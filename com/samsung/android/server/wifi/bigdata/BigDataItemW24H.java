package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemW24H extends BaseBigDataItem {
    private static final String KEY_ADPS_STATE = "adps";
    private static final String KEY_ALWAYS_ALLOW_SCAN = "AASm";
    private static final String KEY_ASNS_MODE = "ASNS";
    private static final String KEY_AUTO_WIFI_MODE = "Auto";
    private static final String KEY_FAVORITE_AP_COUNT = "cFav";
    private static final String KEY_HANG_COUNT = "Hang";
    private static final String KEY_LAA_ACTIVE_STATE = "laaA";
    private static final String KEY_LAA_ENTER_STATE = "laaE";
    private static final String KEY_SAVED_CONFIG_COUNT = "SCoC";
    private static final String KEY_SCAN_COUNTER_2GHZ = "S2gh";
    private static final String KEY_SCAN_COUNTER_ALL = "SAll";
    private static final String KEY_SCAN_COUNTER_CACHED = "SCac";
    private static final String KEY_SCAN_COUNTER_NLP = "SNlp";
    private static final String KEY_SNS_MODE = "SNSm";
    private static final String KEY_WIFI_SAFE_MODE = "slpp";
    private static final String KEY_WIFI_STATE = "STAT";
    private static final String KEY_WPS_COMPLETED_COUNT = "WpsC";
    private static final String KEY_WPS_FAILED_COUNT = "WpsF";
    private static final String KEY_WPS_START_COUNT = "WpsS";
    private static final String[][] W24H = {new String[]{KEY_WIFI_STATE, "1"}, new String[]{KEY_ALWAYS_ALLOW_SCAN, "0"}, new String[]{KEY_AUTO_WIFI_MODE, "0"}, new String[]{KEY_FAVORITE_AP_COUNT, "0"}, new String[]{KEY_SNS_MODE, "0"}, new String[]{KEY_ASNS_MODE, "0"}, new String[]{KEY_WIFI_SAFE_MODE, "0"}, new String[]{KEY_SCAN_COUNTER_ALL, "0"}, new String[]{KEY_SCAN_COUNTER_NLP, "0"}, new String[]{KEY_SCAN_COUNTER_CACHED, "0"}, new String[]{KEY_SCAN_COUNTER_2GHZ, "0"}, new String[]{KEY_WPS_START_COUNT, "0"}, new String[]{KEY_WPS_FAILED_COUNT, "0"}, new String[]{KEY_WPS_COMPLETED_COUNT, "0"}, new String[]{KEY_HANG_COUNT, "0"}, new String[]{KEY_ADPS_STATE, "0"}, new String[]{KEY_SAVED_CONFIG_COUNT, "0"}, new String[]{KEY_LAA_ENTER_STATE, "0"}, new String[]{KEY_LAA_ACTIVE_STATE, "0"}};

    public BigDataItemW24H(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
        return wifiChipInfo.toString() + "," + getKeyValueStrings(W24H);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = W24H;
            if (length == strArr.length) {
                putValues(strArr, array);
                return true;
            }
        }
        if (!this.mLogMessages) {
            return false;
        }
        String str = this.TAG;
        Log.e(str, "can't parse bigdata extra - data:" + data);
        return false;
    }
}
