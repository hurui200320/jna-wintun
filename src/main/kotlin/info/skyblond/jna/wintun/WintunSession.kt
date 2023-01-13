package info.skyblond.jna.wintun

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.ptr.IntByReference
import java.io.EOFException

/**
 * Represent a wintun session.
 *
 * NOT thread-safe.
 * */
class WintunSession(
    private val lib: WintunLib,
    private val session: WintunSessionHandler,
    val capacity: Int
) : AutoCloseable {
    private val readEvent: HANDLE = lib.WintunGetReadWaitEvent(session)

    /**
     * Read an IP packet from session.
     *
     * If the packet is available, return immediately.
     * If not, wait [awaitTimeMs] ms and try again.
     *
     * @throws EOFException if the session or adapter is closed
     * @throws NativeException with err code 13 if there is invalid data
     * @return the IP packet in [ByteArray], or null if no data available
     * @see [Kernel32.WaitForSingleObject]
     * */
    @Throws(NativeException::class, EOFException::class)
    fun readPacket(awaitTimeMs: UInt = WinBase.INFINITE.toUInt()): ByteArray? {
        val packetSizePointer = IntByReference()
        // read once
        val pointer = lib.WintunReceivePacket(session, packetSizePointer)
        val kernel32 = Kernel32.INSTANCE

        if (pointer != null) {// we got data
            val packet = pointer.getByteArray(0, packetSizePointer.value).copyOf()
            lib.WintunReleaseReceivePacket(session, pointer)
            return packet
        } else {
            when (val err = kernel32.GetLastError()) {
                WinError.ERROR_HANDLE_EOF -> throw EOFException()
                WinError.ERROR_NO_MORE_ITEMS -> {
                    // nop, handle it later
                }

                WinError.ERROR_INVALID_DATA -> throw NativeException("Invalid data when reading session", err)
                else -> throw NativeException("Error when reading session", err)
            }
            // now it's waiting time
            return if (awaitTimeMs != 0u) {
                kernel32.WaitForSingleObject(readEvent, awaitTimeMs.toInt())
                // do it again
                readPacket(0u)
            } else {// no wait
                null
            }
        }
    }

    /**
     * Send an IP packet.
     * */
    @Throws(NativeException::class)
    fun writePacket(packet: ByteArray, offset: Int = 0, len: Int = packet.size) {
        require(len <= WintunLib.WINTUN_MAX_IP_PACKET_SIZE) {
            "IP packet to big. Must not bigger than ${WintunLib.WINTUN_MAX_IP_PACKET_SIZE}"
        }
        val p = lib.WintunAllocateSendPacket(session, packet.size)
            ?: throw NativeException("Failed to allocate packet to send", Kernel32.INSTANCE.GetLastError())
        p.write(0, packet, offset, len)
        lib.WintunSendPacket(session, p)
    }

    override fun close() {
        lib.WintunEndSession(session)
    }
}
