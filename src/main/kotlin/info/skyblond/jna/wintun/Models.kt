package info.skyblond.jna.wintun

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32Util
import java.net.Inet6Address
import java.net.InetAddress

/**
 * A pointer represent a wintun adapter handler.
 * */
typealias WintunAdapterHandler = Pointer

/**
 * A pointer represent a wintun session handler.
 * */
typealias WintunSessionHandler = Pointer

/**
 * Indicate something wrong with native side.
 * Like the lib complains the arguments.
 * */
class NativeException(msg: String, err: Int) : Exception(
    "$msg: err $err, ${Kernel32Util.formatMessageFromLastErrorCode(err)}"
)

/**
 * Representing an adapter ip address.
 * */
data class AdapterIPAddress(
    /**
     * The ip, can be [InetAddress] or [Inet6Address].
     * */
    val ip: InetAddress,
    /**
     * The prefix length. For example, 127.0.0.1/8 -> prefixLength = 8.
     *
     * For [InetAddress], it must not greater than 32;
     * For [Inet6Address], it must not greater than 128.
     * A value of 255 means illegal value.
     * */
    val prefixLength: UByte,
    /**
     * The max time in second that this ip address is valid.
     * 0xFFFFFFFF is considered to be infinite.
     * */
    val validLifeTime: UInt = 0xFFFFFFFFu,
    /**
     * The preferred  time in second that this ip address is valid.
     * 0xFFFFFFFF is considered to be infinite.
     * */
    val preferredLifeTime: UInt = 0xFFFFFFFFu,
    /**
     * The time stamp when the IP address was created.
     * */
    val creationTimeStamp: Long = -1
)
