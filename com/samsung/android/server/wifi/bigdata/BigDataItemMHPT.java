package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemMHPT extends BaseBigDataItem {
    private static final String KEY_MHOT_NTV = "mh_ntv";
    private static final String KEY_MHPT_ETV = "mh_etv";
    private static final String KEY_MHPT_PTV = "mh_ptv";
    private static final String[][] MHPT = {new String[]{KEY_MHOT_NTV, ""}, new String[]{KEY_MHPT_PTV, ""}, new String[]{KEY_MHPT_ETV, ""}};

    public BigDataItemMHPT(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return getKeyValueStrings(MHPT);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = MHPT;
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
