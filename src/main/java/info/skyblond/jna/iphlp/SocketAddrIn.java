package info.skyblond.jna.iphlp;

import com.sun.jna.Structure;

@Structure.FieldOrder({"sin_family", "sin_port", "sin_addr", "sin_zero"})
public class SocketAddrIn extends Structure {
    public short sin_family;
    /**
     * unsigned: 0~65535
     */
    public short sin_port;
    /**
     * IPv4 address
     */
    public byte[] sin_addr = new byte[4];
    public byte[] sin_zero = new byte[8];
}
