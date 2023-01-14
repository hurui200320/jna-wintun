package info.skyblond.jna.iphlp;

import com.sun.jna.Structure;

@Structure.FieldOrder({
        "sin6_family", "sin6_port", "sin6_flowinfo", "sin6_addr", "sin6_scope_id"
})
public class SocketAddrIn6 extends Structure {
    public short sin6_family;
    /**
     * unsigned: 0~65535
     */
    public short sin6_port;
    public int sin6_flowinfo;
    public byte[] sin6_addr = new byte[16];
    public int sin6_scope_id;
}
