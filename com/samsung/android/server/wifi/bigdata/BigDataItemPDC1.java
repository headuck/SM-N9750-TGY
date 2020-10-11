package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemPDC1 extends BaseBigDataItem {
    private static final String KEY_ALGORITH_VERSION = "aver";
    private static final String KEY_CRASH_PACAKAGE = "cpkg";
    private static final String KEY_DHCP_FAIL_REASON = "dhfs";
    private static final String KEY_ISSUE_DETECTOR_CATEGORY = "isct";
    private static final String KEY_LAST_PROCESS_STATE = "pres";
    private static final String KEY_PRE_PRE_PROCESS_MSG = "pprem";
    private static final String KEY_PRE_PROCESS_MSG = "prem";
    static final String KEY_PRIVATE_BSSID = "bsid";
    private static final String KEY_PRIVATE_GATEWAY = "apgw";
    private static final String KEY_PRIVATE_IP = "apip";
    private static final String KEY_PRIVATE_SSID = "ssid";
    private static final String KEY_SCREEN_STATE = "scrs";
    private static final String KEY_SLEEP_POLICY = "slpp";
    private static final String KEY_UNWANTED_REASON = "uwrs";
    private static final String[][] PDC1 = {new String[]{"ap_oui", "00:00:00"}, new String[]{"ap_chn", "0"}, new String[]{"ap_rsi", "0"}, new String[]{"wpst", "0"}, new String[]{"aplo", "0"}, new String[]{"cn_rsn", "0"}, new String[]{"cn_irs", "0"}, new String[]{"apdr", "0"}, new String[]{KEY_ISSUE_DETECTOR_CATEGORY, "0"}, new String[]{KEY_DHCP_FAIL_REASON, "0"}, new String[]{"adps", "0"}, new String[]{KEY_SLEEP_POLICY, "0"}, new String[]{KEY_SCREEN_STATE, "1"}, new String[]{KEY_LAST_PROCESS_STATE, "0"}, new String[]{KEY_PRE_PROCESS_MSG, "0"}, new String[]{KEY_PRE_PRE_PROCESS_MSG, "0"}, new String[]{KEY_UNWANTED_REASON, "0"}, new String[]{KEY_ALGORITH_VERSION, "unknown"}, new String[]{"apwe", "0"}, new String[]{KEY_CRASH_PACAKAGE, "null"}, new String[]{"ssid", "unknown"}, new String[]{KEY_PRIVATE_BSSID, "00:00:00:00:00:00"}, new String[]{KEY_PRIVATE_GATEWAY, "0.0.0.0"}, new String[]{KEY_PRIVATE_IP, "0.0.0.0"}};

    public BigDataItemPDC1(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return null;
    }

    public String getJsonFormatFor(int type) {
        int lastIndexOfCommonData = PDC1.length - 4;
        if (type == 2) {
            WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
            StringBuilder sb = new StringBuilder();
            sb.append(wifiChipInfo.toString());
            sb.append(",");
            for (int i = 0; i < lastIndexOfCommonData; i++) {
                String[][] strArr = PDC1;
                sb.append(getKeyValueString(strArr[i][0], strArr[i][1]));
                if (i != lastIndexOfCommonData - 1) {
                    sb.append(",");
                }
            }
            return sb.toString();
        } else if (type != 3) {
            return null;
        } else {
            StringBuilder sb2 = new StringBuilder();
            int i2 = lastIndexOfCommonData;
            while (true) {
                String[][] strArr2 = PDC1;
                if (i2 >= strArr2.length) {
                    return sb2.toString();
                }
                sb2.append(getKeyValueString(strArr2[i2][0], strArr2[i2][1]));
                if (i2 != PDC1.length - 1) {
                    sb2.append(",");
                }
                i2++;
            }
        }
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = PDC1;
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
        return BaseBigDataItem.HIT_TYPE_IMMEDIATLY;
    }

    public boolean isAvailableLogging(int type) {
        if (type == 2 || type == 3) {
            return true;
        }
        return false;
    }
}
