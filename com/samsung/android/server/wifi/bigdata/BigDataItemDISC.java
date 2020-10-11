package com.samsung.android.server.wifi.bigdata;

class BigDataItemDISC extends BaseBigDataItem {
    private static final String[][] DISC = {new String[]{KEY_AP_SECURE_TYPE, ""}, new String[]{KEY_WPA_STATE, ""}, new String[]{KEY_AP_SCAN_COUNT_TOTAL, "0"}, new String[]{KEY_AP_SCAN_COUNT_SAME_CHANNEL, "0"}, new String[]{KEY_AP_DISCONNECT_REASON, "0"}, new String[]{KEY_AP_LOCALLY_GENERATED, "0"}, new String[]{"DUNO", "0"}, new String[]{KEY_AP_OUI, ""}, new String[]{KEY_AP_CHANNEL, ""}, new String[]{KEY_AP_BANDWIDTH, ""}, new String[]{KEY_AP_RSSI, ""}, new String[]{KEY_AP_DATA_RATE, ""}, new String[]{KEY_AP_80211MODE, ""}, new String[]{KEY_AP_ANTENNA, ""}, new String[]{KEY_AP_MU_MIMO, ""}, new String[]{KEY_AP_PASSPOINT, ""}, new String[]{KEY_AP_SNR, ""}, new String[]{KEY_AP_NOISE, ""}, new String[]{KEY_AP_AKM, ""}, new String[]{KEY_AP_ROAMING_COUNT, ""}, new String[]{KEY_AP_11KV, "0"}, new String[]{KEY_AP_11KV_IE, "0"}, new String[]{KEY_AP_ROAMING_FULLS_SCAN_COUNT, "0"}, new String[]{KEY_AP_ROAMING_PARTIAL_SCAN_COUNT, "0"}, new String[]{KEY_AP_ADPS_DISCONNECT, "0"}, new String[]{KEY_CHIPSET_OUIS, ""}};
    static final String KEY_ADPS_STATE = "adps";
    private static final String KEY_AP_11KV = "11KV";
    private static final String KEY_AP_11KV_IE = "KVIE";
    private static final String KEY_AP_80211MODE = "ap_mod";
    private static final String KEY_AP_ADPS_DISCONNECT = "adps_dis";
    private static final String KEY_AP_AKM = "ap_akm";
    private static final String KEY_AP_ANTENNA = "ap_ant";
    static final String KEY_AP_BANDWIDTH = "ap_bdw";
    static final String KEY_AP_BT_CONNECTION = "bt_cnt";
    static final String KEY_AP_CHANNEL = "ap_chn";
    static final String KEY_AP_CONN_DURATION = "apdr";
    private static final String KEY_AP_DATA_RATE = "ap_drt";
    static final String KEY_AP_DISCONNECT_REASON = "cn_rsn";
    static final String KEY_AP_INTERNAL_REASON = "cn_irs";
    static final String KEY_AP_INTERNAL_TYPE = "apwe";
    static final String KEY_AP_LOCALLY_GENERATED = "aplo";
    static final String KEY_AP_MAX_DATA_RATE = "max_drt";
    private static final String KEY_AP_MU_MIMO = "ap_mmo";
    private static final String KEY_AP_NOISE = "ap_nos";
    static final String KEY_AP_OUI = "ap_oui";
    private static final String KEY_AP_PASSPOINT = "ap_pas";
    private static final String KEY_AP_ROAMING_COUNT = "ap_rct";
    private static final String KEY_AP_ROAMING_FULLS_SCAN_COUNT = "rfs_cnt";
    private static final String KEY_AP_ROAMING_PARTIAL_SCAN_COUNT = "rps_cnt";
    static final String KEY_AP_ROAMING_TRIGGER = "cn_rom";
    static final String KEY_AP_RSSI = "ap_rsi";
    private static final String KEY_AP_SCAN_COUNT_SAME_CHANNEL = "ap_snt";
    private static final String KEY_AP_SCAN_COUNT_TOTAL = "ap_stc";
    static final String KEY_AP_SECURE_TYPE = "ap_sec";
    private static final String KEY_AP_SNR = "ap_snr";
    private static final String KEY_CHIPSET_OUIS = "chipset_ouis";
    private static final String KEY_VER = "bver";
    static final String KEY_WPA_STATE = "wpst";
    private static final String PARM_VERSION = "5";
    private int mMaxDataRate = 0;

    public BigDataItemDISC(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
        resetTime();
        return wifiChipInfo.toString() + "," + getKeyValueStrings(DISC) + "," + getKeyValueString(KEY_AP_ROAMING_TRIGGER, "0") + "," + getKeyValueString(KEY_AP_INTERNAL_REASON, "0") + "," + getKeyValueString(KEY_AP_MAX_DATA_RATE, "0") + "," + getKeyValueString(KEY_AP_BT_CONNECTION, "0") + "," + getKeyValueString(KEY_AP_INTERNAL_TYPE, "0") + "," + getKeyValueString(KEY_ADPS_STATE, "0") + "," + getKeyValueString(KEY_VER, PARM_VERSION) + "," + getDurationTimeKeyValueString(KEY_AP_CONN_DURATION);
    }

    public void addOrUpdateValue(String key, int value) {
        if (KEY_AP_MAX_DATA_RATE.equals(key)) {
            if (value < this.mMaxDataRate) {
                value = this.mMaxDataRate;
            } else {
                this.mMaxDataRate = value;
            }
        }
        super.addOrUpdateValue(key, value);
    }

    public void clearData() {
        this.mMaxDataRate = 0;
        super.clearData();
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x0058  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0067  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean parseData(java.lang.String r8) {
        /*
            r7 = this;
            java.lang.String[] r0 = r7.getArray(r8)
            java.lang.String r1 = "-1"
            r2 = 0
            r3 = 1
            if (r0 == 0) goto L_0x002a
            int r4 = r0.length
            java.lang.String[][] r5 = DISC
            int r6 = r5.length
            int r6 = r6 - r3
            if (r4 != r6) goto L_0x002a
            int r4 = r5.length
            java.lang.String[] r4 = new java.lang.String[r4]
            int r5 = r0.length
            int r5 = r5 - r3
            java.lang.System.arraycopy(r0, r2, r4, r2, r5)
            java.lang.String[][] r5 = DISC
            int r6 = r5.length
            int r6 = r6 + -2
            r4[r6] = r1
            int r1 = r5.length
            int r1 = r1 - r3
            int r5 = r0.length
            int r5 = r5 - r3
            r5 = r0[r5]
            r4[r1] = r5
            r0 = r4
        L_0x0029:
            goto L_0x0056
        L_0x002a:
            if (r0 == 0) goto L_0x0029
            int r4 = r0.length
            java.lang.String[][] r5 = DISC
            int r6 = r5.length
            int r6 = r6 + -3
            if (r4 != r6) goto L_0x0029
            int r4 = r5.length
            java.lang.String[] r4 = new java.lang.String[r4]
            int r5 = r0.length
            int r5 = r5 - r3
            java.lang.System.arraycopy(r0, r2, r4, r2, r5)
            java.lang.String[][] r5 = DISC
            int r6 = r5.length
            int r6 = r6 + -4
            r4[r6] = r1
            int r6 = r5.length
            int r6 = r6 + -3
            r4[r6] = r1
            int r6 = r5.length
            int r6 = r6 + -2
            r4[r6] = r1
            int r1 = r5.length
            int r1 = r1 - r3
            int r5 = r0.length
            int r5 = r5 - r3
            r5 = r0[r5]
            r4[r1] = r5
            r0 = r4
        L_0x0056:
            if (r0 == 0) goto L_0x0063
            int r1 = r0.length
            java.lang.String[][] r4 = DISC
            int r5 = r4.length
            if (r1 == r5) goto L_0x005f
            goto L_0x0063
        L_0x005f:
            r7.putValues(r4, r0)
            return r3
        L_0x0063:
            boolean r1 = r7.mLogMessages
            if (r1 == 0) goto L_0x007d
            java.lang.String r1 = r7.TAG
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "can't pase bigdata extra - data:"
            r3.append(r4)
            r3.append(r8)
            java.lang.String r3 = r3.toString()
            android.util.Log.e(r1, r3)
        L_0x007d:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.bigdata.BigDataItemDISC.parseData(java.lang.String):boolean");
    }

    public boolean isAvailableLogging(int type) {
        if (type == 1) {
            return true;
        }
        return super.isAvailableLogging(type);
    }
}
