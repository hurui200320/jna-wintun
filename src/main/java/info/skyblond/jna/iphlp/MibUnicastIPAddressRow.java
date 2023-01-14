package info.skyblond.jna.iphlp;

import com.sun.jna.Structure;

@Structure.FieldOrder({
        "Address", "InterfaceLuid", "InterfaceIndex", "PrefixOrigin", "SuffixOrigin",
        "ValidLifetime", "PreferredLifetime", "OnLinkPrefixLength", "SkipAsSource",
        "DadState", "ScopeId", "CreationTimeStamp",
})
public class MibUnicastIPAddressRow extends Structure {
    public SocketAddrINET Address;
    public long InterfaceLuid;
    public int InterfaceIndex;
    public int PrefixOrigin;
    public int SuffixOrigin;
    public int ValidLifetime;
    public int PreferredLifetime;
    /**
     * MASK.
     * E.g: 192.168.0.0/16, OnLinkPrefixLength is 16.
     * */
    public byte OnLinkPrefixLength;
    public byte SkipAsSource;
    public byte DadState;
    public int ScopeId;
    public long CreationTimeStamp;
}
