package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanTxType {
    public static final int BROADCAST = 0;
    public static final int UNICAST = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "BROADCAST";
        }
        if (o == 1) {
            return "UNICAST";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("BROADCAST");
        if ((o & 1) == 1) {
            list.add("UNICAST");
            flipped = 0 | 1;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}
