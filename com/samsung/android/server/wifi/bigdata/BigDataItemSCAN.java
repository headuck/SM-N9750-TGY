package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemSCAN extends BaseBigDataItem {
    private static final String KEY_SC_APP_NAME = "sc_app";
    private static final String KEY_SC_CACHED = "scca";
    private static final String KEY_SC_COUNT = "sc_cnt";
    private static final String KEY_SC_DELAYED = "scde";
    private static final String KEY_SC_FULL = "scfu";
    private static final String KEY_SC_P_1_6_11CH_ONLY = "sc16";
    private static final String KEY_SC_P_24_ONLY = "sc24";
    private static final String KEY_SC_P_DFS = "scpd";
    private static final String[][] SCAN = {new String[]{KEY_SC_APP_NAME, ""}, new String[]{KEY_SC_COUNT, "0"}, new String[]{KEY_SC_DELAYED, "0"}, new String[]{KEY_SC_CACHED, "0"}, new String[]{KEY_SC_FULL, "0"}, new String[]{KEY_SC_P_24_ONLY, "0"}, new String[]{KEY_SC_P_1_6_11CH_ONLY, "0"}, new String[]{KEY_SC_P_DFS, "0"}};

    public BigDataItemSCAN(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return getKeyValueStrings(SCAN);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = SCAN;
            if (length == strArr.length) {
                putValues(strArr, array);
                return true;
            }
        }
        if (!this.mLogMessages) {
            return false;
        }
        String str = this.TAG;
        Log.e(str, "can't pase bigdata extra - data:" + data);
        return false;
    }
}
