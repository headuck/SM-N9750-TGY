package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemMHSI extends BaseBigDataItem {
    private static final String KEY_MH_ALLOWED = "mh_all";
    private static final String KEY_MH_AP_TIMEOUT = "mh_apt";
    private static final String KEY_MH_CHANNEL = "mh_chn";
    private static final String KEY_MH_CONNECTED_MAX_CLIENT = "mh_max";
    private static final String KEY_MH_HIDDEN = "mh_hdd";
    private static final String KEY_MH_IFACE = "mh_ifa";
    private static final String KEY_MH_PMF = "mh_pmf";
    private static final String KEY_MH_POWER_SAVE_MODE = "mh_psm";
    private static final String KEY_MH_SSID_TYPE = "mh_typ";
    private static final String KEY_MH_WIFI_AP_WIFI_SHARING = "mh_swi";
    private static final String[][] MHSI = {new String[]{KEY_MH_IFACE, ""}, new String[]{KEY_MH_SSID_TYPE, ""}, new String[]{KEY_MH_HIDDEN, ""}, new String[]{KEY_MH_CHANNEL, ""}, new String[]{KEY_MH_ALLOWED, ""}, new String[]{KEY_MH_CONNECTED_MAX_CLIENT, ""}, new String[]{KEY_MH_WIFI_AP_WIFI_SHARING, ""}, new String[]{KEY_MH_AP_TIMEOUT, ""}, new String[]{KEY_MH_PMF, ""}, new String[]{KEY_MH_POWER_SAVE_MODE, ""}};

    public BigDataItemMHSI(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return getKeyValueStrings(MHSI);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = MHSI;
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
