package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemSI5G extends BaseBigDataItem {
    private static final String KEY_AP_OUI = "ap_oui";
    private static final String KEY_FREQUENCY_24GHZ = "fq2g";
    private static final String KEY_FREQUENCY_5GHZ = "fq5g";
    private static final String KEY_RSSI_24GHZ = "rs2g";
    private static final String KEY_RSSI_5GHZ = "rs5g";
    private static final String KEY_RSSI_DIFF = "rsdf";
    private static final String[][] SI5G = {new String[]{KEY_AP_OUI, "00:00:00"}, new String[]{KEY_RSSI_DIFF, "0"}, new String[]{KEY_RSSI_24GHZ, "0"}, new String[]{KEY_RSSI_5GHZ, "0"}, new String[]{KEY_FREQUENCY_24GHZ, "0"}, new String[]{KEY_FREQUENCY_5GHZ, "0"}};

    public BigDataItemSI5G(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return null;
    }

    public String getJsonFormatFor(int type) {
        if (type != 2) {
            return null;
        }
        WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
        return wifiChipInfo.toString() + "," + getKeyValueStrings(SI5G);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = SI5G;
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
