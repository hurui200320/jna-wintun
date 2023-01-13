package info.skyblond.jna.wintun

import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference

/**
 * Raw JNA binding for lib wintun.
 * */
@Suppress("FunctionName")
interface WintunLib : Library {

    // constants
    companion object {
        /**
         * Minimum ring capacity: 128KiB
         * */
        const val WINTUN_MIN_RING_CAPACITY: Int = 0x20000

        /**
         * Maximum ring capacity: 64MiB
         */
        const val WINTUN_MAX_RING_CAPACITY: Int = 0x4000000

        /**
         * Maximum IP packet size: 65535 bytes
         */
        const val WINTUN_MAX_IP_PACKET_SIZE: Int = 0xFFFF
    }


    // ----------------------------- START Adapter -----------------------------

    /**
     * Creates a new Wintun adapter.
     *
     * If the function succeeds, the return value is the adapter handle.
     * Must be released with [WintunCloseAdapter]. If the function
     * fails, the return value is NULL. To get extended error information, call
     * [Kernel32.GetLastError].
     *
     * @param name The requested name of the adapter. Zero-terminated string of
     * up to MAX_ADAPTER_NAME-1 characters (normally 128-1).
     * @param tunnelType Name of the adapter tunnel type. Zero-terminated string
     * of up to MAX_ADAPTER_NAME-1 characters (normally 128-1).
     * @param requestedGUID The GUID of the created network adapter, which
     * then influences NLA generation deterministically. If it is set to NULL,
     * the GUID is chosen by the system at random, and hence a new NLA entry is
     * created for each new adapter. It is called "requested" GUID because the
     * API it uses is completely undocumented, and so there could be minor
     * interesting complications with its usage.
     */
    fun WintunCreateAdapter(
        name: WString,
        tunnelType: WString,
        requestedGUID: Guid.GUID?
    ): WintunAdapterHandler?

    /**
     * Opens an existing Wintun adapter.
     *
     * If the function succeeds, the return value is the adapter handle.
     * Must be released with [WintunCloseAdapter]. If the function
     * fails, the return value is NULL. To get extended error information, call
     * [Kernel32.GetLastError].
     *
     * @param name The requested name of the adapter. Zero-terminated string of
     * up to MAX_ADAPTER_NAME-1 characters.
     */
    fun WintunOpenAdapter(name: WString): WintunAdapterHandler?

    /**
     * Releases Wintun adapter resources and, if adapter was created with
     * [WintunLib.WintunCreateAdapter], removes adapter.
     *
     * @param adapter Adapter handle obtained with [WintunLib.WintunCreateAdapter]
     * or [WintunLib.WintunOpenAdapter].
     */
    fun WintunCloseAdapter(adapter: WintunAdapterHandler)

    /**
     * Deletes the Wintun driver if there are no more adapters in use.
     *
     * If the function succeeds, the return value is true.
     * If the function fails, the return value is false.
     * To get extended error information, call [Kernel32.GetLastError].
     */
    fun WintunDeleteDriver(): Boolean

    /**
     * Returns the LUID of the adapter.
     *
     * @param adapter Adapter handle obtained with WintunCreateAdapter or WintunOpenAdapter
     * @param liud Pointer to LUID to receive adapter LUID.
     */
    fun WintunGetAdapterLUID(adapter: WintunAdapterHandler, liud: WinNT.LUID)

    // ------------------------------ END Adapter ------------------------------


    // ----------------------------- START Session -----------------------------

    /**
     * Starts Wintun session.
     *
     * Return Wintun session handle. Must be released with [WintunEndSession].
     * If the function fails, the return value is NULL.
     * To get extended error information, call [Kernel32.GetLastError].
     *
     * @param adapter Adapter handle obtained with WintunOpenAdapter or WintunCreateAdapter
     * @param capacity Rings capacity. Must be between [WINTUN_MIN_RING_CAPACITY]
     * and [WINTUN_MAX_RING_CAPACITY] (incl.). Must be a power of two.
     *
     */
    fun WintunStartSession(adapter: WintunAdapterHandler, capacity: Int): WintunSessionHandler?

    /**
     * Ends Wintun session.
     *
     * @param session Wintun session handle obtained with WintunStartSession
     */
    fun WintunEndSession(session: WintunSessionHandler)

    /**
     * Gets Wintun session's read-wait event handle.
     *
     * Get the pointer to receive event handle to wait for available data when reading.
     * Should [WintunReceivePacket] return [com.sun.jna.platform.win32.WinError.ERROR_NO_MORE_ITEMS]
     * (after spinning on it for a while under heavy load), wait for this event
     * to become signaled before retrying [WintunReceivePacket]. Do not call
     * CloseHandle on this event - it is managed by the session.
     *
     * @param session Wintun session handle obtained with [WintunStartSession]
     */
    fun WintunGetReadWaitEvent(session: WintunSessionHandler): WinNT.HANDLE

    /**
     * Retrieves one packet. After the packet content is consumed, call
     * [WintunReleaseReceivePacket] with Packet returned from this function to
     * release internal buffer. This function is thread-safe.
     *
     * Return pointer to layer 3 IPv4 or IPv6 packet. Client may modify its content
     * at will. If the function fails, the return value is NULL. To get extended
     * error information, call [Kernel32.GetLastError]. Possible errors include the
     * following:
     * + [com.sun.jna.platform.win32.WinError.ERROR_HANDLE_EOF]: Wintun adapter is terminating
     * + [com.sun.jna.platform.win32.WinError.ERROR_NO_MORE_ITEMS]: Wintun buffer is exhausted
     * + [com.sun.jna.platform.win32.WinError.ERROR_INVALID_DATA]: ERROR_INVALID_DATA
     *
     * @param session Wintun session handle obtained with WintunStartSession
     * @param packetSize Pointer to receive packet size.
     * @see [Kernel32.WaitForSingleObject] to "wait" for more packets.
     */
    fun WintunReceivePacket(session: WintunSessionHandler, packetSize: IntByReference): Pointer?

    /**
     * Releases internal buffer after the received packet has been processed by the client.
     * This function is thread-safe.
     *
     * @param session Wintun session handle obtained with WintunStartSession
     * @param packet Packet obtained with WintunReceivePacket
     * */
    fun WintunReleaseReceivePacket(session: WintunSessionHandler, packet: Pointer)

    /**
     * Allocates memory for a packet to send. After the memory is filled with
     * packet data, call [WintunSendPacket] to send and release internal buffer.
     * [WintunAllocateSendPacket] is thread-safe and the [WintunAllocateSendPacket] order of
     * calls define the packet sending order.
     *
     * Returns pointer to memory where to prepare layer 3 IPv4 or IPv6 packet for
     * sending. If the function fails, the return value is NULL. To get extended
     * error information, call [Kernel32.GetLastError]. Possible errors include the
     * following:
     * + [com.sun.jna.platform.win32.WinError.ERROR_HANDLE_EOF]: Wintun adapter is terminating
     * + [com.sun.jna.platform.win32.WinError.ERROR_BUFFER_OVERFLOW]: Wintun buffer is full
     *
     * @param session Wintun session handle obtained with WintunStartSession
     * @param packetSize Exact packet size. Must be less or equal to [WINTUN_MAX_IP_PACKET_SIZE].
     */
    fun WintunAllocateSendPacket(session: WintunSessionHandler, packetSize: Int): Pointer?

    /**
     * Sends the packet and releases internal buffer. [WintunSendPacket] is thread-safe,
     * but the [WintunAllocateSendPacket] order of calls define the packet sending order.
     * This means the packet is not guaranteed to be sent in the [WintunSendPacket] yet.
     *
     * @param session Wintun session handle obtained with WintunStartSession
     * @param packet Packet obtained with WintunAllocateSendPacket
     */
    fun WintunSendPacket(session: WintunSessionHandler, packet: Pointer)

    // ------------------------------ END Session ------------------------------

    /**
     * Determines the version of the Wintun driver currently loaded.
     *
     * If the function succeeds, the return value is the version number.
     * If the function fails, the return value is zero. To get extended
     * error information, call [Kernel32.GetLastError]. Possible errors
     * include the following:
     * + [com.sun.jna.platform.win32.WinError.ERROR_FILE_NOT_FOUND]: Wintun not loaded
     * */
    fun WintunGetRunningDriverVersion(): Long
}
