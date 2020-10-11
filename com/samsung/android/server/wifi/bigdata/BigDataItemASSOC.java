package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemASSOC extends BaseBigDataItem {
    private static final String[][] ASSOC = {new String[]{KEY_AP_ASSOC_REJECT, ""}, new String[]{"DUNO", "0"}, new String[]{KEY_AP_SECURE_TYPE, ""}, new String[]{KEY_AP_SCAN_COUNT_TOTAL, ""}, new String[]{KEY_AP_SCAN_COUNT_SAME_CHANNEL, ""}, new String[]{KEY_AP_CHANNEL, ""}, new String[]{KEY_AP_RSSI, ""}, new String[]{KEY_AP_OUI, ""}};
    private static final String KEY_AP_ASSOC_DISABLE = "cn_dis";
    private static final String KEY_AP_ASSOC_REJECT = "cn_sts";
    private static final String KEY_AP_CHANNEL = "ap_chn";
    private static final String KEY_AP_OUI = "ap_oui";
    private static final String KEY_AP_RSSI = "ap_rsi";
    private static final String KEY_AP_SCAN_COUNT_SAME_CHANNEL = "ap_snt";
    private static final String KEY_AP_SCAN_COUNT_TOTAL = "ap_stc";
    private static final String KEY_AP_SECURE_TYPE = "ap_sec";

    public BigDataItemASSOC(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
        return wifiChipInfo.toString() + "," + getKeyValueStrings(ASSOC) + "," + getKeyValueString(KEY_AP_ASSOC_DISABLE, "0");
    }

    public boolean parseData(String data) {
        String parseData;
        if (data.indexOf("assoc_reject.status") >= 0) {
            parseData = data.substring("assoc_reject.status".length()).trim();
        } else {
            parseData = data.trim();
        }
        String[] array = getArray(parseData);
        if (array != null) {
            int length = array.length;
            String[][] strArr = ASSOC;
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
