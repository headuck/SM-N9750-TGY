package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemMHPS extends BaseBigDataItem {
    private static final String KEY_MH_PSMODE = "mh_pss";
    private static final String KEY_MH_PS_USED_TIME = "mh_pst";
    private static final String[][] MHPS = {new String[]{KEY_MH_PSMODE, ""}, new String[]{KEY_MH_PS_USED_TIME, ""}};

    public BigDataItemMHPS(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return getKeyValueStrings(MHPS);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = MHPS;
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
