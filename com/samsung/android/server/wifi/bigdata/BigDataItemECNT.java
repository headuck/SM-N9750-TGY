package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemECNT extends BaseBigDataItem {
    private static final String[][] ECNT = {new String[]{KEY_UNIQ_VALUE, "0"}, new String[]{KEY_VER, "0"}, new String[]{KEY_COOK, "0"}, new String[]{KEY_C01, "0"}, new String[]{KEY_C02, "0"}, new String[]{KEY_C03, "0"}, new String[]{KEY_RAW, "0"}};
    private static final String KEY_C01 = "C01";
    private static final String KEY_C02 = "C02";
    private static final String KEY_C03 = "C03";
    private static final String KEY_COOK = "COOK";
    private static final String KEY_RAW = "RAW";
    private static final String KEY_UID = "UID";
    static final String KEY_UNIQ_VALUE = "DUNO";
    private static final String KEY_VER = "VER";

    public BigDataItemECNT(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return null;
    }

    public String getJsonFormatFor(int type) {
        if (type != 2) {
            return null;
        }
        return getKeyValueStrings(ECNT);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = ECNT;
            if (length == strArr.length) {
                putValues(strArr, array, true);
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

    public String getHitType() {
        return BaseBigDataItem.HIT_TYPE_ONCE_A_DAY;
    }

    public boolean isAvailableLogging(int type) {
        if (type == 2) {
            return true;
        }
        return false;
    }
}
