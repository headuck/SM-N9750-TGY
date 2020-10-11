package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemMHOF extends BaseBigDataItem {
    private static final String KEY_MH_APP = "mh_onf";
    private static final String KEY_MH_STATE = "mh_pkg";
    private static final String[][] MHOF = {new String[]{KEY_MH_APP, ""}, new String[]{KEY_MH_STATE, ""}};

    public BigDataItemMHOF(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return getKeyValueStrings(MHOF);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = MHOF;
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
