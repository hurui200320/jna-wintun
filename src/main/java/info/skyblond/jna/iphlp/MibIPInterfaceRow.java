package info.skyblond.jna.iphlp;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinNT;

/**
 * Might be wrong, but is good enough to set MTU.
 * */
@Structure.FieldOrder({
        "Family", "InterfaceLuid", "InterfaceIndex", "MaxReassemblySize", "InterfaceIdentifier",
        "MinRouterAdvertisementInterval", "MaxRouterAdvertisementInterval", "AdvertisingEnabled",
        "ForwardingEnabled", "WeakHostSend", "WeakHostReceive", "UseAutomaticMetric",
        "UseNeighborUnreachabilityDetection", "ManagedAddressConfigurationSupported",
        "OtherStatefulConfigurationSupported", "AdvertiseDefaultRoute", "RouterDiscoveryBehavior",
        "DadTransmits", "BaseReachableTime", "RetransmitTime", "PathMtuDiscoveryTimeout",
        "LinkLocalAddressBehavior", "LinkLocalAddressTimeout", "ZoneIndices", "SitePrefixLength",
        "Metric", "NlMtu", "Connected", "SupportsWakeUpPatterns", "SupportsNeighborDiscovery",
        "SupportsRouterDiscovery", "ReachableTime", "TransmitOffload", "ReceiveOffload",
        "DisableDefaultRoutes"})
public class MibIPInterfaceRow extends Structure {
    public int Family;
    public long InterfaceLuid;
    public int InterfaceIndex;
    public int MaxReassemblySize;
    public long InterfaceIdentifier;
    public int MinRouterAdvertisementInterval;
    public int MaxRouterAdvertisementInterval;
    public byte AdvertisingEnabled;
    public byte ForwardingEnabled;
    public byte WeakHostSend;
    public byte WeakHostReceive;
    public byte UseAutomaticMetric;
    public byte UseNeighborUnreachabilityDetection;
    public byte ManagedAddressConfigurationSupported;
    public byte OtherStatefulConfigurationSupported;
    public byte AdvertiseDefaultRoute;
    public int RouterDiscoveryBehavior;
    public int DadTransmits;
    public int BaseReachableTime;
    public int RetransmitTime;
    public int PathMtuDiscoveryTimeout;
    public int LinkLocalAddressBehavior;
    public int LinkLocalAddressTimeout;
    public int[] ZoneIndices = new int[ScopeLevel.scopeLevelCount];
    public int SitePrefixLength;
    public int Metric;
    public int NlMtu;
    public byte Connected;
    public byte SupportsWakeUpPatterns;
    public byte SupportsNeighborDiscovery;
    public byte SupportsRouterDiscovery;
    public int ReachableTime;
    /**
     * 0:NlChecksumSupported
     * 1:NlOptionsSupported
     * 2:TlDatagramChecksumSupported
     * 3:TlStreamChecksumSupported
     * 4:TlStreamOptionsSupported
     * 5:FastPathCompatible
     * 6:TlLargeSendOffloadSupported
     * 7:TlGiantSendOffloadSupported
     */
    public byte TransmitOffload;
    /**
     * 0:NlChecksumSupported
     * 1:NlOptionsSupported
     * 2:TlDatagramChecksumSupported
     * 3:TlStreamChecksumSupported
     * 4:TlStreamOptionsSupported
     * 5:FastPathCompatible
     * 6:TlLargeSendOffloadSupported
     * 7:TlGiantSendOffloadSupported
     */
    public byte ReceiveOffload;
    public byte DisableDefaultRoutes;
}
