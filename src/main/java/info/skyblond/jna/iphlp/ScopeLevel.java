package info.skyblond.jna.iphlp;

public interface ScopeLevel {
    int scopeLevelInterface = 1;
    int scopeLevelLink = 2;
    int scopeLevelSubnet = 3;
    int scopeLevelAdmin = 4;
    int scopeLevelSite = 5;
    int scopeLevelOrganization = 8;
    int scopeLevelGlobal = 14;
    int scopeLevelCount = 16;
}
