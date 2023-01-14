package info.skyblond.jna.iphlp;

import com.sun.jna.Union;

public class SocketAddrINET extends Union {
    public SocketAddrIn Ipv4;
    public SocketAddrIn6 Ipv6;
    public int si_family; // TODO might be short?
}
