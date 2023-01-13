package info.skyblond.jna.wintun

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32Util

/**
 * A pointer represent a wintun adapter handler.
 * */
internal typealias WintunAdapterHandler = Pointer

/**
 * A pointer represent a wintun session handler.
 * */
internal typealias WintunSessionHandler = Pointer

/**
 * Indicate something wrong with native side.
 * Like the lib complains the arguments.
 * */
class NativeException(msg: String, err: Int) : Exception(
    "$msg: err $err, ${Kernel32Util.formatMessageFromLastErrorCode(err)}"
)

