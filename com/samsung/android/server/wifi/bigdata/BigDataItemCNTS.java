package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemCNTS extends BaseBigDataItem {
    private static final String[][] CNTS = {new String[]{KEY_UNIQ_VALUE, "0"}, new String[]{KEY_DCNT, "0"}, new String[]{KEY_CLCK, "0"}, new String[]{KEY_DATE, "0"}, new String[]{KEY_TIME, "0"}, new String[]{"bsid", "00:00:00:00:00:00"}, new String[]{KEY_OPMD, "0x0000"}, new String[]{KEY_OUI, "00-00-00"}, new String[]{KEY_OUI, "00-00-00"}, new String[]{KEY_OUI, "00-00-00"}, new String[]{KEY_LQCM, "0"}, new String[]{KEY_RSSI, "0"}, new String[]{KEY_SNR, "0"}, new String[]{"adps", "0"}, new String[]{KEY_PMFL, "0x00"}, new String[]{KEY_PMST, "0"}, new String[]{KEY_SLEN, "0"}, new String[]{KEY_BLMX, "0"}, new String[]{KEY_BLCT, "0"}, new String[]{KEY_D12G, "0"}, new String[]{KEY_O62G, "0"}, new String[]{KEY_M02G, "0"}, new String[]{KEY_O65G, "0"}, new String[]{KEY_M05G, "0"}, new String[]{KEY_TR02, "0"}, new String[]{KEY_TF03, "0"}, new String[]{KEY_TP08, "0"}, new String[]{KEY_RP09, "0"}, new String[]{KEY_TA0A, "0"}, new String[]{KEY_TR0B, "0"}, new String[]{KEY_TC0C, "0"}, new String[]{KEY_TB0D, "0"}, new String[]{KEY_TU0E, "0"}, new String[]{KEY_RR0F, "0"}, new String[]{KEY_TR10, "0"}, new String[]{KEY_RS12, "0"}, new String[]{KEY_RB13, "0"}, new String[]{KEY_RC14, "0"}, new String[]{KEY_RN15, "0"}, new String[]{KEY_BB16, "0"}, new String[]{KEY_BR17, "0"}, new String[]{KEY_RB18, "0"}, new String[]{KEY_RF19, "0"}, new String[]{KEY_RF1A, "0"}, new String[]{KEY_RF1B, "0"}, new String[]{KEY_RR1C, "0"}, new String[]{KEY_RC1D, "0"}, new String[]{KEY_RA1E, "0"}, new String[]{KEY_RB1F, "0"}, new String[]{KEY_RB20, "0"}, new String[]{KEY_RD21, "0"}, new String[]{KEY_RB22, "0"}, new String[]{KEY_RD23, "0"}, new String[]{KEY_RD24, "0"}, new String[]{KEY_RR25, "0"}, new String[]{KEY_RC26, "0"}, new String[]{KEY_RD27, "0"}};
    private static final String KEY_BB16 = "BB16";
    private static final String KEY_BLCT = "BLCT";
    private static final String KEY_BLMX = "BLMX";
    private static final String KEY_BR17 = "BR17";
    private static final String KEY_CLCK = "CLCK";
    private static final String KEY_D12G = "D12G";
    private static final String KEY_DATE = "DATE";
    private static final String KEY_DCNT = "DCNT";
    private static final String KEY_LQCM = "LQCM";
    private static final String KEY_M02G = "M02G";
    private static final String KEY_M05G = "M05G";
    private static final String KEY_O62G = "O62G";
    private static final String KEY_O65G = "O65G";
    private static final String KEY_OPMD = "OPMD";
    private static final String KEY_OUI = "OUI";
    private static final String KEY_PMFL = "PMFL";
    private static final String KEY_PMST = "PMST";
    private static final String KEY_RA1E = "RA1E";
    private static final String KEY_RB13 = "RB13";
    private static final String KEY_RB18 = "RB18";
    private static final String KEY_RB1F = "RB1F";
    private static final String KEY_RB20 = "RB20";
    private static final String KEY_RB22 = "RB22";
    private static final String KEY_RC14 = "RC14";
    private static final String KEY_RC1D = "RC1D";
    private static final String KEY_RC26 = "RC26";
    private static final String KEY_RD21 = "RD21";
    private static final String KEY_RD23 = "RD23";
    private static final String KEY_RD24 = "RD24";
    private static final String KEY_RD27 = "RD27";
    private static final String KEY_RF19 = "RF19";
    private static final String KEY_RF1A = "RF1A";
    private static final String KEY_RF1B = "RF1B";
    private static final String KEY_RN15 = "RN15";
    private static final String KEY_RP09 = "RP09";
    private static final String KEY_RR0F = "RR0F";
    private static final String KEY_RR1C = "RR1C";
    private static final String KEY_RR25 = "RR25";
    private static final String KEY_RS12 = "RS12";
    private static final String KEY_RSSI = "RSSI";
    private static final String KEY_SLEN = "SLEN";
    private static final String KEY_SNR = "SNR";
    private static final String KEY_TA0A = "TA0A";
    private static final String KEY_TB0D = "TB0D";
    private static final String KEY_TC0C = "TC0C";
    private static final String KEY_TF03 = "TF03";
    private static final String KEY_TIME = "TIME";
    private static final String KEY_TP08 = "TP08";
    private static final String KEY_TR02 = "TR02";
    private static final String KEY_TR0B = "TR0B";
    private static final String KEY_TR10 = "TR10";
    private static final String KEY_TU0E = "TU0E";
    static final String KEY_UNIQ_VALUE = "DUNO";

    public BigDataItemCNTS(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return null;
    }

    public String getJsonFormatFor(int type) {
        if (type == 2) {
            WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
            StringBuilder sb = new StringBuilder();
            sb.append(wifiChipInfo.toString());
            sb.append(",");
            int i = 0;
            while (true) {
                String[][] strArr = CNTS;
                if (i >= strArr.length) {
                    return sb.toString();
                }
                if (!(i == 5 || i == 8 || i == 9)) {
                    sb.append(getKeyValueString(strArr[i][0], strArr[i][1]));
                    if (i != CNTS.length - 1) {
                        sb.append(",");
                    }
                }
                i++;
            }
        } else if (type != 3) {
            return null;
        } else {
            StringBuilder sb2 = new StringBuilder();
            String[][] strArr2 = CNTS;
            sb2.append(getKeyValueString(strArr2[5][0], strArr2[5][1]));
            return sb2.toString();
        }
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = CNTS;
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
        if (type == 2 || type == 3) {
            return true;
        }
        return false;
    }
}
