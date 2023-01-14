package info.skyblond.jna.wintun

import com.sun.jna.WString
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.IPHlpAPI
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinError
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference
import info.skyblond.jna.iphlp.*
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Represent a Wintun adapter.
 * Create or open the tun device when initializing.
 *
 * Note: You need run as admin to create tun.
 *
 * NOT thread-safe.
 * */
class WintunAdapter(
    /**
     * The name of the tun adapter
     * */
    val name: String,
    /**
     * The type of the tun adapter.
     * Null for open existing one.
     * Required when creating new adapter.
     * */
    type: String?,
    /**
     * The guid of the tun device.
     * Required when creating new adapter.
     * Null for system decide, aka the GUID is chosen by the system at random,
     * and hence a new NLA entry is created for each new adapter.
     * */
    guid: String? = null
) : AutoCloseable {

    private val wintunLib: WintunLib = WintunLib.INSTANCE
    private val ipHelperLib: ExtendedIPHlpAPI = ExtendedIPHlpAPI.INSTANCE

    /**
     * Open a existing tun device.
     * */
    constructor(name: String) : this(name, null, null)

    private val adapter: WintunAdapterHandler

    init {
        adapter = if (type == null) { // open
            wintunLib.WintunOpenAdapter(WString(name))
                ?: throw NativeException("Failed to open tun device `$name`", Kernel32.INSTANCE.GetLastError())
        } else { // create
            wintunLib.WintunCreateAdapter(WString(name), WString(type), guid?.let { Guid.GUID.fromString(it) })
                ?: throw NativeException(
                    "Failed to create tun device `$name` (type: $type)",
                    Kernel32.INSTANCE.GetLastError()
                )
        }
    }

    /**
     * Get the LUID of this adapter.
     * */
    private fun getLuid(): Long {
        val result = LongByReference()
        wintunLib.WintunGetAdapterLUID(adapter, result)
        return result.value
    }

    /**
     * List all ip address related to this adapter.
     *
     * @param ipFamily Must be [IPHlpAPI.AF_INET], [IPHlpAPI.AF_INET6] or [IPHlpAPI.AF_UNSPEC]
     * @return List of [AdapterIPAddress], representing an IP.
     * */
    fun listAssociatedAddresses(ipFamily: Int): List<AdapterIPAddress> {
        val pointerByReference = PointerByReference()
        val err = ipHelperLib.GetUnicastIpAddressTable(ipFamily, pointerByReference)
        // something wrong
        if (err != WinError.NO_ERROR && err != WinError.ERROR_NOT_FOUND)
            throw NativeException("Failed to list unicast ip addresses", err)
        // no ip, return empty list
        if (err != WinError.NO_ERROR) return emptyList()
        // parsing pointer
        val table = MibUnicastIPAddressTable(pointerByReference.value)
        check(table.NumEntries == table.Table.size) {
            "MIB_UNICASTIPADDRESS_TABLE size not match. Expect ${table.NumEntries}, actual: ${table.Table.size}"
        }
        val luid = getLuid()
        val result = table.Table
            .filter { it.InterfaceLuid == luid }
            .map {
                it.Address.setType(Int::class.java)
                val ip = when (it.Address.si_family) {
                    IPHlpAPI.AF_INET -> {
                        val v4 = it.Address.getTypedValue(SocketAddrIn::class.java) as SocketAddrIn
                        Inet4Address.getByAddress(v4.sin_addr.copyOf())
                    }

                    IPHlpAPI.AF_INET6 -> {
                        val v6 = it.Address.getTypedValue(SocketAddrIn6::class.java) as SocketAddrIn6
                        Inet6Address.getByAddress(v6.sin6_addr.copyOf())
                    }

                    else -> error("Unknown si family: ${it.Address.si_family}")
                }
                AdapterIPAddress(
                    ip = ip,
                    prefixLength = it.OnLinkPrefixLength.toUByte(),
                    validLifeTime = it.ValidLifetime.toUInt(),
                    preferredLifeTime = it.PreferredLifetime.toUInt(),
                    creationTimeStamp = it.CreationTimeStamp
                )
            }
        ipHelperLib.FreeMibTable(table.pointer)
        return result
    }

    /**
     * Create and initialize a [MibUnicastIPAddressRow], fill the luid and ip.
     * */
    private fun createMibUnicastIpAddressRow(address: InetAddress): MibUnicastIPAddressRow {
        val row = MibUnicastIPAddressRow()
        ipHelperLib.InitializeUnicastIpAddressEntry(row)
        row.InterfaceLuid = getLuid()
        when (address) {
            is Inet4Address -> {
                row.Address.setType(SocketAddrIn::class.java)
                row.Address.Ipv4.sin_family = IPHlpAPI.AF_INET.toShort()
                row.Address.Ipv4.sin_port = 0
                row.Address.Ipv4.sin_addr = address.address
            }

            is Inet6Address -> {
                row.Address.setType(SocketAddrIn6::class.java)
                row.Address.Ipv6.sin6_family = IPHlpAPI.AF_INET6.toShort()
                row.Address.Ipv6.sin6_port = 0
                row.Address.Ipv6.sin6_addr = address.address
            }
        }
        return row
    }

    /**
     * Add an [AdapterIPAddress] to this adapter.
     *
     * @return true if created, false means address already exists.
     * */
    fun associateIp(adapterIPAddress: AdapterIPAddress): Boolean {
        // create a new row
        val row = createMibUnicastIpAddressRow(adapterIPAddress.ip)
        row.OnLinkPrefixLength = adapterIPAddress.prefixLength.toByte()
        row.ValidLifetime = adapterIPAddress.validLifeTime.toInt()
        row.PreferredLifetime = adapterIPAddress.preferredLifeTime.toInt()
        return ipHelperLib.CreateUnicastIpAddressEntry(row).let { err ->
            if (err != WinError.NO_ERROR && err != WinError.ERROR_OBJECT_ALREADY_EXISTS)
                throw NativeException("Failed to create new MIB_UNICASTIPADDRESS_ROW", err)
            err == WinError.NO_ERROR // false: duplicated
        }
    }

    /**
     * Update the key. The [AdapterIPAddress.ip] must not change.
     *
     * @return ture if changed, false means ip not found.
     * */
    fun updateIp(adapterIPAddress: AdapterIPAddress): Boolean {
        // create a new row
        val row = createMibUnicastIpAddressRow(adapterIPAddress.ip)
        // fetch the latest
        ipHelperLib.GetUnicastIpAddressEntry(row).let { err ->
            if (err != WinError.NO_ERROR && err != WinError.ERROR_NOT_FOUND)
                throw NativeException("Failed to get unicast ip entry", err)
            if (err != WinError.NO_ERROR) return false // not found
        }
        row.PreferredLifetime = adapterIPAddress.preferredLifeTime.toInt()
        row.OnLinkPrefixLength = adapterIPAddress.prefixLength.toByte()
        row.ValidLifetime = adapterIPAddress.validLifeTime.toInt()
        ipHelperLib.SetUnicastIpAddressEntry(row).let { err ->
            if (err != WinError.NO_ERROR)
                throw NativeException("Failed to update MIB_UNICASTIPADDRESS_ROW", err)
        }
        return true
    }

    /**
     * Remove an ip from the adapter
     * */
    fun dissociateIp(ip: InetAddress) {
        val row = createMibUnicastIpAddressRow(ip)
        ipHelperLib.DeleteUnicastIpAddressEntry(row).let { err ->
            if (err != WinError.NO_ERROR)
                throw NativeException("Failed deleting ip", err)
        }
    }

    /**
     * Get the MTU of this adapter on the given ipFamily.
     *
     * @param ipFamily Must be [IPHlpAPI.AF_INET] or [IPHlpAPI.AF_INET6]
     * */
    fun getMTU(ipFamily: Int): UInt {
        val ipInterfaceRow = MibIPInterfaceRow()
        ipHelperLib.InitializeIpInterfaceEntry(ipInterfaceRow)
        ipInterfaceRow.InterfaceLuid = getLuid()
        ipInterfaceRow.Family = ipFamily
        ipHelperLib.GetIpInterfaceEntry(ipInterfaceRow).let { err ->
            if (err != WinError.NO_ERROR)
                throw NativeException("Failed getting MIB_IPINTERFACE_ROW", err)
        }
        return ipInterfaceRow.NlMtu.toUInt()
    }

    /**
     * Set the MTU of this adapter on the given ipFamily.
     *
     * @param ipFamily Must be [IPHlpAPI.AF_INET] or [IPHlpAPI.AF_INET6]
     * @param mtu The new mtu value. Although it's [UInt], the range is [UShort].
     * */
    fun setMTU(ipFamily: Int, mtu: UInt) {
        val ipInterfaceRow = MibIPInterfaceRow()
        ipHelperLib.InitializeIpInterfaceEntry(ipInterfaceRow)
        ipInterfaceRow.Family = ipFamily
        ipInterfaceRow.InterfaceLuid = getLuid()
        ipHelperLib.GetIpInterfaceEntry(ipInterfaceRow).let { err ->
            if (err != WinError.NO_ERROR)
                throw NativeException("Failed getting MIB_IPINTERFACE_ROW", err)
        }
        ipInterfaceRow.NlMtu = mtu.toInt()
        ipHelperLib.SetIpInterfaceEntry(ipInterfaceRow).let { err ->
            if (err != WinError.NO_ERROR)
                throw NativeException("Failed setting new MTU", err)
        }
    }

    /**
     * Create a new session associated with this adapter, so you can read/write
     * ip packets.
     *
     * @param capacity Ring capacity of the adapter, must in range of [WintunLib.WINTUN_MIN_RING_CAPACITY]
     * and [WintunLib.WINTUN_MAX_RING_CAPACITY]
     * */
    fun newSession(capacity: Int): WintunSession {
        require(capacity in WintunLib.WINTUN_MIN_RING_CAPACITY..WintunLib.WINTUN_MAX_RING_CAPACITY) {
            "The ring capacity must not smaller than ${WintunLib.WINTUN_MIN_RING_CAPACITY}, and must not bigger than ${WintunLib.WINTUN_MAX_RING_CAPACITY}"
        }
        val handler = wintunLib.WintunStartSession(adapter, capacity)
            ?: throw NativeException("Failed to create session (size: $capacity)", Kernel32.INSTANCE.GetLastError())
        return WintunSession(wintunLib, handler, capacity)
    }

    override fun close() {
        wintunLib.WintunCloseAdapter(adapter)
    }
}
