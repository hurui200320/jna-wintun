package info.skyblond.jna.iphlp

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.IPHlpAPI
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.W32APIOptions

/**
 * The original [IPHlpAPI] is not enough.
 * Here is some extended methods that helps you
 * manage interface ip addresses
 * */
@Suppress("FunctionName")
interface ExtendedIPHlpAPI : IPHlpAPI {
    companion object {
        @JvmStatic
        val INSTANCE: ExtendedIPHlpAPI =
            Native.load("IPHlpAPI", ExtendedIPHlpAPI::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }

    // ------------------------ START Interface related ------------------------

    /**
     * Initializes the members of an [MibIPInterfaceRow] entry with default values.
     *
     * You must initialize the object using this function, before using it.
     * */
    fun InitializeIpInterfaceEntry(row: MibIPInterfaceRow)

    /**
     * Retrieves IP information for the specified interface on the local computer.
     *
     * @param row A pointer to a [MibIPInterfaceRow] structure that, on successful return,
     * receives information for an interface on the local computer. On input, the
     * [MibIPInterfaceRow.InterfaceLuid] or [MibIPInterfaceRow.InterfaceIndex] member of the
     * [MibIPInterfaceRow] must be set to the interface for which to retrieve information.
     *
     * @return If the function succeeds, the return value is [com.sun.jna.platform.win32.WinError.NO_ERROR].
     * If the function fails, the return value is one of the following error codes:
     * + [com.sun.jna.platform.win32.WinError.ERROR_FILE_NOT_FOUND]: The interface LUID
     * or interface index doesn't exist.
     * + [com.sun.jna.platform.win32.WinError.ERROR_INVALID_PARAMETER]:  This error is
     * returned if a NULL pointer is passed in the Row parameter, the [MibIPInterfaceRow.Family]
     * is not [IPHlpAPI.AF_INET] or [IPHlpAPI.AF_INET6], or both the InterfaceLuid or
     * InterfaceIndex were unspecified.
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_FOUND]: This error is returned
     * if the InterfaceLuid or InterfaceIndex does not match the IP address family.
     * + other: Use [com.sun.jna.platform.win32.Kernel32Util.getLastErrorMessage] to obtain
     * */
    fun GetIpInterfaceEntry(row: MibIPInterfaceRow): Int

    /**
     * Sets the properties of an IP interface on the local computer.
     *
     * @param row A pointer to a [MibIPInterfaceRow] structure entry for an interface.
     * On input, the [MibIPInterfaceRow.Family] must be set to [IPHlpAPI.AF_INET6]
     * or [IPHlpAPI.AF_INET] and the [MibIPInterfaceRow.InterfaceLuid] or the
     * [MibIPInterfaceRow.InterfaceIndex] must be specified. On a successful return,
     * the [MibIPInterfaceRow.InterfaceLuid] is filled in if [MibIPInterfaceRow.InterfaceIndex]
     * was specified.
     *
     * @return If the function succeeds, the return value is [com.sun.jna.platform.win32.WinError.NO_ERROR].
     * If the function fails, the return value is one of the following error codes:
     * + [com.sun.jna.platform.win32.WinError.ERROR_ACCESS_DENIED]: Access is denied. Need run as admin.
     * + see [GetIpInterfaceEntry]
     * */
    fun SetIpInterfaceEntry(row: MibIPInterfaceRow): Int


    // ------------------------- END Interface related -------------------------

    // ------------------------ START UnicastIP related ------------------------

    /**
     * Retrieves the unicast IP address table on the local computer.
     *
     * @param family Must be [IPHlpAPI.AF_INET], [IPHlpAPI.AF_INET6] or [IPHlpAPI.AF_UNSPEC]
     * @param table The pointer of the pointer to [info.skyblond.jna.iphlp.MibUnicastIPAddressTable].
     * The pointer must be released by [FreeMibTable].
     * @return If the function succeeds, the return value is [com.sun.jna.platform.win32.WinError.NO_ERROR].
     * If the function fails, the return value is one of the following error codes:
     * + [com.sun.jna.platform.win32.WinError.ERROR_INVALID_PARAMETER]: Invalid family.
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_ENOUGH_MEMORY]: Insufficient memory.
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_FOUND]: No unicast ip found.
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_SUPPORTED]: Missing IP stack.
     * + other
     * */
    fun GetUnicastIpAddressTable(family: Int, table: PointerByReference): Int

    /**
     * Frees the buffer allocated by the functions that return tables of network
     * interfaces, addresses, and routes.
     *
     * @param pointer The pointer to free.
     * */
    fun FreeMibTable(pointer: Pointer)

    /**
     * Initializes a [MibUnicastIPAddressRow] structure with default values for
     * an unicast IP address entry on the local computer.
     *
     * You must initialize the object using this function, before using it.
     * */
    fun InitializeUnicastIpAddressEntry(row: MibUnicastIPAddressRow)

    /**
     * Retrieves information for an existing unicast IP address entry on the local computer.
     *
     * @param row A pointer to a [MibUnicastIPAddressRow] structure entry for an
     * unicast IP address entry. The [MibUnicastIPAddressRow.InterfaceLuid] or [MibUnicastIPAddressRow.InterfaceIndex]
     * must be initialized, and the [MibUnicastIPAddressRow.Address] must be a
     * valid IPv4 or IPv6 Address. On successful return, this structure will be
     * updated with the properties for an existing unicast IP address.
     *
     * @return If the function succeeds, the return value is [com.sun.jna.platform.win32.WinError.NO_ERROR].
     * If the function fails, the return value is one of the following error codes:
     * + [com.sun.jna.platform.win32.WinError.ERROR_FILE_NOT_FOUND]: Invalid luid or index.
     * + [com.sun.jna.platform.win32.WinError.ERROR_INVALID_PARAMETER]: Missing luid and index, or invalid address.
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_FOUND]: Adapter (luid or index) not match the ip.
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_SUPPORTED]: Missing IP stack.
     * + other
     * */
    fun GetUnicastIpAddressEntry(row: MibUnicastIPAddressRow): Int

    /**
     * Adds a new unicast IP address entry on the local computer.
     *
     * @param row A pointer to [MibUnicastIPAddressRow].
     * @return If the function succeeds, the return value is [com.sun.jna.platform.win32.WinError.NO_ERROR].
     * If the function fails, the return value is one of the following error codes:
     * + [com.sun.jna.platform.win32.WinError.ERROR_ACCESS_DENIED]: Need run as admin.
     * + [com.sun.jna.platform.win32.WinError.ERROR_INVALID_PARAMETER]: Wrong parameter,
     * see https://learn.microsoft.com/en-us/windows/win32/api/netioapi/nf-netioapi-createunicastipaddressentry#return-value
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_FOUND]: Adapter (luid or index) not found.
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_SUPPORTED]: Missing IP stack.
     * + [com.sun.jna.platform.win32.WinError.ERROR_OBJECT_ALREADY_EXISTS]: The address already exists.
     * + other
     * */
    fun CreateUnicastIpAddressEntry(row: MibUnicastIPAddressRow): Int

    /**
     * Sets/updates the properties of an existing unicast IP address entry on the local computer.
     *
     * @param row A pointer to [MibUnicastIPAddressRow].
     *
     * @return If the function succeeds, the return value is [com.sun.jna.platform.win32.WinError.NO_ERROR].
     * If the function fails, the return value is one of the following error codes:
     * + [com.sun.jna.platform.win32.WinError.ERROR_ACCESS_DENIED]: Need run as admin.
     * + [com.sun.jna.platform.win32.WinError.ERROR_INVALID_PARAMETER]: Wrong parameter,
     * see https://learn.microsoft.com/en-us/windows/win32/api/netioapi/nf-netioapi-setunicastipaddressentry#return-value
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_FOUND]: Adapter (luid or index) not found.
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_SUPPORTED]: Missing IP stack.
     * + other
     * */
    fun SetUnicastIpAddressEntry(row: MibUnicastIPAddressRow): Int

    /**
     * Deletes an existing unicast IP address entry on the local computer.
     *
     * @param row A pointer to [MibUnicastIPAddressRow].
     *
     * @return If the function succeeds, the return value is [com.sun.jna.platform.win32.WinError.NO_ERROR].
     * If the function fails, the return value is one of the following error codes:
     * + [com.sun.jna.platform.win32.WinError.ERROR_ACCESS_DENIED]: Need run as admin.
     * + [com.sun.jna.platform.win32.WinError.ERROR_INVALID_PARAMETER]: Wrong parameter,
     * see https://learn.microsoft.com/en-us/windows/win32/api/netioapi/nf-netioapi-deleteunicastipaddressentry#return-value
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_FOUND]: Adapter (luid or index) not found.
     * + [com.sun.jna.platform.win32.WinError.ERROR_NOT_SUPPORTED]: Missing IP stack.
     * + other
     * */
    fun DeleteUnicastIpAddressEntry(row: MibUnicastIPAddressRow): Int

    // ------------------------- END UnicastIP related -------------------------
}
