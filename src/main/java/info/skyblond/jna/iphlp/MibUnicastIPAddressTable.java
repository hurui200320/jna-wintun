package info.skyblond.jna.iphlp;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Might be wrong, but is good enough to set MTU.
 */
@Structure.FieldOrder({"NumEntries", "Table"})
public class MibUnicastIPAddressTable extends Structure {
    public int NumEntries;
    public MibUnicastIPAddressRow[] Table = new MibUnicastIPAddressRow[1];

    public MibUnicastIPAddressTable() {
        super();
    }

    public MibUnicastIPAddressTable(Pointer p) {
        super(p);
        this.Table = new MibUnicastIPAddressRow[p.getInt(0)];
        read();
    }
}
