package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemONOF extends BaseBigDataItem {
    private static final String KEY_IS_CONNECTED = "isCo";
    private static final String KEY_IS_SNS_ENABLED = "isSN";
    private static final String KEY_ON_APP = "on_app";
    private static final String KEY_ON_CONFIG_COUNT = "ONCN";
    private static final String KEY_ON_CONFIG_COUNT_OPEN = "ONC2";
    private static final String KEY_ON_CONIFG_COUNT_FAVORITE = "ONC3";
    private static final String KEY_ON_DURATION = "fDUR";
    private static final String KEY_ON_FOREGROUND_APP = "ONFG";
    private static final String KEY_ON_STATE = "on_enb";
    private static final String[][] ONOF = {new String[]{KEY_ON_STATE, ""}, new String[]{KEY_ON_APP, ""}, new String[]{KEY_IS_CONNECTED, "0"}, new String[]{KEY_IS_SNS_ENABLED, "1"}, new String[]{KEY_ON_CONFIG_COUNT, "0"}, new String[]{KEY_ON_CONFIG_COUNT_OPEN, "0"}, new String[]{KEY_ON_CONIFG_COUNT_FAVORITE, "0"}, new String[]{KEY_ON_FOREGROUND_APP, "x"}};
    public static final long delayTimeMillis = 30000;

    public BigDataItemONOF(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
        return wifiChipInfo.toString() + "," + wifiChipInfo.getCidInfoForKeyValueType() + "," + getKeyValueStrings(ONOF) + "," + getDurationTimeKeyValueString(KEY_ON_DURATION);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = ONOF;
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

    public boolean isAvailableLogging(int type) {
        if (type == 1) {
            return true;
        }
        return super.isAvailableLogging(type);
    }
}
