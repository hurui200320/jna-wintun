package info.skyblond.jna.wintun

import com.sun.jna.WString
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Kernel32

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
     * The underlying lib
     * */
    private val lib: WintunLib,
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

    /**
     * Open a existing tun device.
     * */
    constructor(lib: WintunLib, name: String) : this(lib, name, null, null)

    private val adapter: WintunAdapterHandler

    init {
        adapter = if (type == null) { // open
            lib.WintunOpenAdapter(WString(name))
                ?: throw NativeException("Failed to open tun device `$name`", Kernel32.INSTANCE.GetLastError())
        } else { // create
            lib.WintunCreateAdapter(WString(name), WString(type), guid?.let { Guid.GUID.fromString(it) })
                ?: throw NativeException(
                    "Failed to create tun device `$name` (type: $type)",
                    Kernel32.INSTANCE.GetLastError()
                )
        }
    }

    // TODO adapter get/set/clear ip

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
        val handler = lib.WintunStartSession(adapter, capacity)
            ?: throw NativeException("Failed to create session (size: $capacity)", Kernel32.INSTANCE.GetLastError())
        return WintunSession(lib, handler, capacity)
    }

    override fun close() {
        lib.WintunCloseAdapter(adapter)
    }
}
