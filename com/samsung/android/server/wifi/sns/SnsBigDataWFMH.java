package com.samsung.android.server.wifi.sns;

import android.util.Log;

public class SnsBigDataWFMH extends SnsBigDataFeature {
    public static final String KEY_MH_CONNECTION_STATUS = "MHCT";
    public static final String KEY_MH_DAILY_COUNT = "MHDC";
    public static final String KEY_MH_HASH_PRIMARY = "MHHP";
    public static final String KEY_MH_HASH_SECONDARY = "MHHS";
    public static final String KEY_MH_INTERNET_CONNECTION = "MHIC";
    public static final String KEY_MH_OUI_PRIMARY = "MHOP";
    public static final String KEY_MH_OUI_SECONDARY = "MHOS";
    public static final String KEY_MH_SAME_COUNT = "MHSC";
    public static final String KEY_MH_SECURITY_TYPE = "MHST";
    public static final String KEY_MH_SERVER_INDEX = "MHSI";
    public static final String KEY_MH_TYPE = "MTYP";
    private static final String KEY_MH_VERSION = "MVER";
    private static final String[][] WFMH = {new String[]{KEY_MH_VERSION, "2017091800"}, new String[]{KEY_MH_TYPE, "0"}, new String[]{KEY_MH_CONNECTION_STATUS, "-1"}, new String[]{KEY_MH_SECURITY_TYPE, "-1"}, new String[]{KEY_MH_OUI_PRIMARY, "ff:ff:ff"}, new String[]{KEY_MH_OUI_SECONDARY, "ff:ff:ff"}, new String[]{KEY_MH_SAME_COUNT, "-1"}, new String[]{"MHSI", "-1"}, new String[]{KEY_MH_HASH_PRIMARY, "-1"}, new String[]{KEY_MH_HASH_SECONDARY, "-1"}, new String[]{KEY_MH_INTERNET_CONNECTION, "-1"}, new String[]{"MHDC", "-1"}};

    public SnsBigDataWFMH(boolean dqaEnabled, String dqaFeatureName) {
        super(dqaEnabled, dqaFeatureName);
    }

    public SnsBigDataWFMH() {
    }

    public String getJsonFormat() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(getKeyValueStrings(WFMH));
            if (DBG) {
                String str = this.TAG;
                Log.d(str, "getJsonFormat - " + sb.toString());
            }
        } catch (Exception e) {
            if (DBG) {
                String str2 = this.TAG;
                Log.w(str2, "Exception occured on getJsonFormat - " + e);
            }
            e.printStackTrace();
        }
        return sb.toString();
    }
}
