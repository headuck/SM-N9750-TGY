package com.samsung.android.server.wifi.sns;

import android.util.Log;
import com.android.server.wifi.tcp.WifiTransportLayerUtils;

public class SnsBigDataTCPE extends SnsBigDataFeature {
    public static final String KEY_SNS_TCP_ALGORESULT = "TCAL";
    public static final String KEY_SNS_TCP_AP_CONNECTION_COUNT = "TCPF";
    public static final String KEY_SNS_TCP_AP_CONNECTION_TIME = "TCPG";
    public static final String KEY_SNS_TCP_AP_DETECTED_COUNT = "TCUT";
    public static final String KEY_SNS_TCP_AP_PACKAGE_DETECTED_COUNT = "TCPC";
    public static final String KEY_SNS_TCP_AUTO_SWITCH_ENABLED = "TCID";
    public static final String KEY_SNS_TCP_CATEGORY = "TCST";
    public static final String KEY_SNS_TCP_ESTABLISHED = "TCPE";
    public static final String KEY_SNS_TCP_FREQUENCY = "TCFR";
    public static final String KEY_SNS_TCP_LASTACK = "TCPL";
    public static final String KEY_SNS_TCP_LINKSPEED = "TCLS";
    public static final String KEY_SNS_TCP_LOSS = "TCLO";
    public static final String KEY_SNS_TCP_PACKAGENAME = "TCPN";
    public static final String KEY_SNS_TCP_PACKAGE_DETECTED_COUNT = "TCPW";
    public static final String KEY_SNS_TCP_QCRESULT = "TCQC";
    public static final String KEY_SNS_TCP_RECEIVEDPACKETS = "TCRX";
    public static final String KEY_SNS_TCP_RETRANSMISSION = "TCPR";
    public static final String KEY_SNS_TCP_RSSI = "TCRS";
    public static final String KEY_SNS_TCP_SYN = "TCPS";
    public static final String KEY_SNS_TCP_TIME = "TCTM";
    public static final String KEY_SNS_TCP_TRANSMITTEDPACKETS = "TCTX";
    private static final String KEY_SNS_VERSION = "SVER";
    private static final String[][] TCPE = {new String[]{KEY_SNS_VERSION, "2020061700"}, new String[]{KEY_SNS_TCP_TIME, "123"}, new String[]{KEY_SNS_TCP_QCRESULT, "0"}, new String[]{KEY_SNS_TCP_ALGORESULT, "NoBlocking"}, new String[]{KEY_SNS_TCP_PACKAGENAME, "0"}, new String[]{"TCPE", "0"}, new String[]{KEY_SNS_TCP_SYN, "0"}, new String[]{KEY_SNS_TCP_RETRANSMISSION, "0"}, new String[]{KEY_SNS_TCP_LASTACK, "0"}, new String[]{KEY_SNS_TCP_RSSI, "0"}, new String[]{KEY_SNS_TCP_LINKSPEED, "0"}, new String[]{KEY_SNS_TCP_TRANSMITTEDPACKETS, "0"}, new String[]{KEY_SNS_TCP_RECEIVEDPACKETS, "0"}, new String[]{KEY_SNS_TCP_LOSS, "0.0"}, new String[]{KEY_SNS_TCP_FREQUENCY, "0"}, new String[]{KEY_SNS_TCP_CATEGORY, WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE}, new String[]{KEY_SNS_TCP_PACKAGE_DETECTED_COUNT, "0"}, new String[]{KEY_SNS_TCP_AP_PACKAGE_DETECTED_COUNT, "0"}, new String[]{KEY_SNS_TCP_AP_DETECTED_COUNT, "0"}, new String[]{KEY_SNS_TCP_AP_CONNECTION_COUNT, "0"}, new String[]{KEY_SNS_TCP_AP_CONNECTION_TIME, "0"}, new String[]{KEY_SNS_TCP_AUTO_SWITCH_ENABLED, "0"}};
    public static final int USER_ACTION_NOTIFICATION_DELETE_HUN = 2;
    public static final int USER_ACTION_NOTIFICATION_DELETE_NORMAL = 3;
    public static final int USER_ACTION_NOTIFICATION_DELETE_SUGGESTION = 1;
    public static final int USER_ACTION_SETTINGS = 4;
    public static final int USER_ACTION_STOP_USE_MOBILE_DATA = 6;
    public static final int USER_ACTION_USE_MOBILE_DATA = 5;
    public static final int USER_ACTION_WIFI_DISCONNECTED = 7;
    public String mTcpAlgorithmResult;
    public int mTcpApConnectionCount;
    public int mTcpApConnectionTime;
    public int mTcpApDetectedCount;
    public int mTcpApFrequency;
    public int mTcpApPackageDetectedCount;
    public int mTcpAutoSwitchEnabled;
    public String mTcpCategory;
    public int mTcpEstablished;
    public int mTcpLastAck;
    public int mTcpLinkSpeed;
    public double mTcpLoss;
    public int mTcpPackageDetectedCount;
    public String mTcpPackageName;
    public int mTcpQcResults;
    public int mTcpRetransmission;
    public int mTcpRssi;
    public long mTcpRx;
    public int mTcpSyn;
    public long mTcpTime;
    public long mTcpTx;

    public SnsBigDataTCPE() {
        initialize();
    }

    public SnsBigDataTCPE(boolean dqaEnabled, String dqaFeatureName) {
        super(dqaEnabled, dqaFeatureName);
    }

    public void initialize() {
        this.mTcpTime = 0;
        this.mTcpQcResults = 0;
        this.mTcpAlgorithmResult = null;
        this.mTcpPackageName = null;
        this.mTcpEstablished = 0;
        this.mTcpSyn = 0;
        this.mTcpRetransmission = 0;
        this.mTcpLastAck = 0;
        this.mTcpRssi = 0;
        this.mTcpLinkSpeed = 0;
        this.mTcpTx = 0;
        this.mTcpRx = 0;
        this.mTcpLoss = 0.0d;
        this.mTcpApFrequency = 0;
        this.mTcpCategory = null;
        this.mTcpPackageDetectedCount = 0;
        this.mTcpApPackageDetectedCount = 0;
        this.mTcpApDetectedCount = 0;
        this.mTcpApConnectionCount = 0;
        this.mTcpApConnectionTime = 0;
        this.mTcpAutoSwitchEnabled = 0;
    }

    public void addOrUpdateAllValue() {
        addOrUpdateValue(KEY_SNS_TCP_TIME, this.mTcpTime);
        addOrUpdateValue(KEY_SNS_TCP_QCRESULT, this.mTcpQcResults);
        addOrUpdateValue(KEY_SNS_TCP_ALGORESULT, this.mTcpAlgorithmResult);
        addOrUpdateValue(KEY_SNS_TCP_PACKAGENAME, this.mTcpPackageName);
        addOrUpdateValue("TCPE", this.mTcpEstablished);
        addOrUpdateValue(KEY_SNS_TCP_SYN, this.mTcpSyn);
        addOrUpdateValue(KEY_SNS_TCP_RETRANSMISSION, this.mTcpRetransmission);
        addOrUpdateValue(KEY_SNS_TCP_LASTACK, this.mTcpLastAck);
        addOrUpdateValue(KEY_SNS_TCP_RSSI, this.mTcpRssi);
        addOrUpdateValue(KEY_SNS_TCP_LINKSPEED, this.mTcpLinkSpeed);
        addOrUpdateValue(KEY_SNS_TCP_TRANSMITTEDPACKETS, this.mTcpTx);
        addOrUpdateValue(KEY_SNS_TCP_RECEIVEDPACKETS, this.mTcpRx);
        addOrUpdateValue(KEY_SNS_TCP_LOSS, this.mTcpLoss);
        addOrUpdateValue(KEY_SNS_TCP_FREQUENCY, this.mTcpApFrequency);
        addOrUpdateValue(KEY_SNS_TCP_CATEGORY, this.mTcpCategory);
        addOrUpdateValue(KEY_SNS_TCP_PACKAGE_DETECTED_COUNT, this.mTcpPackageDetectedCount);
        addOrUpdateValue(KEY_SNS_TCP_AP_PACKAGE_DETECTED_COUNT, this.mTcpApPackageDetectedCount);
        addOrUpdateValue(KEY_SNS_TCP_AP_DETECTED_COUNT, this.mTcpApDetectedCount);
        addOrUpdateValue(KEY_SNS_TCP_AP_CONNECTION_COUNT, this.mTcpApConnectionCount);
        addOrUpdateValue(KEY_SNS_TCP_AP_CONNECTION_TIME, this.mTcpApConnectionTime);
        addOrUpdateValue(KEY_SNS_TCP_AUTO_SWITCH_ENABLED, this.mTcpAutoSwitchEnabled);
    }

    public String getJsonFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append(getKeyValueStrings(TCPE));
        if (DBG) {
            String str = this.TAG;
            Log.d(str, "getJsonFormat - " + sb.toString());
        }
        return sb.toString();
    }

    public void putKeyValueString(String[] key) {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                TCPE[i][1] = key[i];
            }
        }
    }
}
