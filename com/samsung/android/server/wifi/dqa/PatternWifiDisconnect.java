package com.samsung.android.server.wifi.dqa;

import android.os.Bundle;
import android.util.Log;
import com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PatternWifiDisconnect extends WifiIssuePattern {
    private static final long ASSOC_CHECK_TIME_DIFF = 800;
    private static final int ISSUE_TYPE_DISCONNECT_AP_REASON = 3;
    private static final int ISSUE_TYPE_DISCONNECT_BY_3RDPARTY_APK = 1;
    private static final int ISSUE_TYPE_DISCONNECT_BY_SNS = 7;
    private static final int ISSUE_TYPE_DISCONNECT_DHCP_FAILED = 6;
    private static final int ISSUE_TYPE_DISCONNECT_LCD_OFF_STATE = 2;
    private static final int ISSUE_TYPE_DISCONNECT_NO_INTERNET_IP_GW = 5;
    private static final int ISSUE_TYPE_DISCONNECT_STATE_ILLEGAL = 4;
    private static final int ISSUE_TYPE_DISCONNECT_STRONG_SIGNAL = 0;
    private static final int ISSUE_TYPE_SYSTEM_PROBLEM = 8;
    private static final String TAG = "PatternWifiDisc";
    private int adps = 0;
    private int apdr = 0;
    private String appName = null;
    private int apwe = 0;
    private final String aver = "2.9";
    private String bssid = "00:20:00:00:00:00";
    private int dhcp = 0;
    private int dhfs = 0;
    private int disconnectReason = 0;
    private int freq = -120;
    private String gateway = "0.0.0.0";

    /* renamed from: ip */
    private String f51ip = "0.0.0.0";
    private int isct = 0;
    private int locallyGenerated = 0;
    private int mLastAssociatedId = 0;
    private long mLastAssocitatedTime = 0;
    private String oui = "00:00:00";
    private int pprem = 0;
    private int prem = 0;
    private String pres = "default";
    private int rssi = 0;
    private int scrs = 0;
    private int slpp = 2;
    private String ssid = "default";
    private int uwrs = -1;
    private int wpaState = 0;

    public Collection<Integer> getAssociatedKeys() {
        Collection<Integer> ret = new ArrayList<>();
        ret.add(1);
        ret.add(200);
        return ret;
    }

    public boolean isAssociated(int reportId, ReportData reportData) {
        boolean ret = false;
        if (reportId == 1 || reportId == 200) {
            long reportTime = reportData.mTime;
            int i = this.mLastAssociatedId;
            if (i != 0) {
                long diff = reportTime - this.mLastAssocitatedTime;
                if (i != reportId) {
                    if (diff >= 0 && diff < ASSOC_CHECK_TIME_DIFF) {
                        if (DBG) {
                            Log.i(TAG, "associated diff:" + diff);
                        }
                        ret = true;
                    } else if (DBG) {
                        Log.i(TAG, "not associated diff:" + diff);
                    }
                }
            }
            this.mLastAssociatedId = reportId;
            this.mLastAssocitatedTime = reportTime;
        }
        return ret;
    }

    private void resetData() {
        this.f51ip = "0.0.0.0";
        this.gateway = "0.0.0.0";
        this.isct = -1;
        this.oui = "00:00:00";
        this.ssid = "default";
        this.bssid = "00:20:00:00:00:00";
        this.rssi = 0;
        this.freq = -120;
        this.apwe = 0;
        this.dhcp = 0;
        this.apdr = 0;
        this.adps = 0;
        this.slpp = 2;
        this.scrs = 0;
        this.pres = "default";
        this.prem = 0;
        this.pprem = 0;
        this.uwrs = -1;
        this.dhfs = 0;
        this.disconnectReason = 0;
        this.locallyGenerated = 0;
        this.wpaState = 0;
        this.appName = null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:204:0x0564  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean matches() {
        /*
            r23 = this;
            r1 = r23
            r2 = 0
            r23.resetData()
            boolean r0 = DBG
            java.lang.String r3 = "PatternWifiDisc"
            if (r0 == 0) goto L_0x0011
            java.lang.String r0 = "check pattern disc1"
            android.util.Log.d(r3, r0)
        L_0x0011:
            r4 = 1
            com.samsung.android.server.wifi.dqa.ReportData r5 = r1.getLastIndexOfData(r4)
            r6 = 0
            if (r5 != 0) goto L_0x001f
            java.lang.String r0 = "not exit report: disconnect transtion"
            android.util.Log.e(r3, r0)
            return r6
        L_0x001f:
            r0 = 200(0xc8, float:2.8E-43)
            com.samsung.android.server.wifi.dqa.ReportData r7 = r1.getLastIndexOfData(r0)
            if (r7 != 0) goto L_0x002d
            java.lang.String r0 = "not exist report: bigdata disconnect"
            android.util.Log.e(r3, r0)
            return r6
        L_0x002d:
            android.os.Bundle r0 = r7.mData
            java.lang.Integer r8 = java.lang.Integer.valueOf(r6)
            java.lang.String r9 = "apTypeInt"
            java.lang.Object r0 = getValue((android.os.Bundle) r0, (java.lang.String) r9, r8)
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r0 = r0.intValue()
            r1.apwe = r0
            int r0 = r1.apwe
            r8 = 3
            if (r0 != r8) goto L_0x004c
            java.lang.String r0 = "AP is mobile hotspot"
            android.util.Log.d(r3, r0)
            return r6
        L_0x004c:
            java.lang.String r9 = "0.0.0.0"
            java.lang.String r0 = "ip"
            java.lang.Object r0 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r0, r9)
            java.lang.String r0 = (java.lang.String) r0
            r1.f51ip = r0
            java.lang.Integer r0 = java.lang.Integer.valueOf(r6)
            java.lang.String r10 = "networkPrefix"
            java.lang.Object r0 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r10, r0)
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r10 = r0.intValue()
            java.lang.String r0 = "gw"
            java.lang.Object r0 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r0, r9)
            java.lang.String r0 = (java.lang.String) r0
            r1.gateway = r0
            java.lang.String r0 = r1.f51ip
            r11 = 5
            if (r0 == 0) goto L_0x00b5
            java.lang.String r12 = r1.gateway
            if (r12 == 0) goto L_0x00b5
            java.net.InetAddress r0 = java.net.InetAddress.getByName(r0)     // Catch:{ UnknownHostException -> 0x0094 }
            java.lang.String r12 = r1.gateway     // Catch:{ UnknownHostException -> 0x0094 }
            java.net.InetAddress r12 = java.net.InetAddress.getByName(r12)     // Catch:{ UnknownHostException -> 0x0094 }
            boolean r13 = r1.isInRange(r0, r12, r10)     // Catch:{ UnknownHostException -> 0x0094 }
            if (r13 != 0) goto L_0x0093
            r1.isct = r11     // Catch:{ UnknownHostException -> 0x0094 }
            r2 = 1
            java.lang.String r13 = "ip and gateway is wrong "
            android.util.Log.d(r3, r13)     // Catch:{ UnknownHostException -> 0x0094 }
        L_0x0093:
            goto L_0x00b5
        L_0x0094:
            r0 = move-exception
            java.lang.StringBuilder r12 = new java.lang.StringBuilder
            r12.<init>()
            java.lang.String r13 = "Fail to get InetAddress from IP and Gateway!! IP : "
            r12.append(r13)
            java.lang.String r13 = r1.f51ip
            r12.append(r13)
            java.lang.String r13 = " Getway : "
            r12.append(r13)
            java.lang.String r13 = r1.gateway
            r12.append(r13)
            java.lang.String r12 = r12.toString()
            android.util.Log.d(r3, r12)
        L_0x00b5:
            android.os.Bundle r0 = r7.mData
            r12 = -120(0xffffffffffffff88, float:NaN)
            java.lang.Integer r12 = java.lang.Integer.valueOf(r12)
            java.lang.String r13 = "rssi"
            java.lang.Object r0 = getValue((android.os.Bundle) r0, (java.lang.String) r13, r12)
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r0 = r0.intValue()
            r1.rssi = r0
            if (r2 != 0) goto L_0x00db
            int r0 = r1.rssi
            r12 = -60
            if (r0 < r12) goto L_0x00d5
            if (r0 < 0) goto L_0x00db
        L_0x00d5:
            java.lang.String r0 = "weak signal"
            android.util.Log.d(r3, r0)
            return r6
        L_0x00db:
            r0 = 6
            com.samsung.android.server.wifi.dqa.ReportData r12 = r1.getLastIndexOfData(r0)
            if (r12 == 0) goto L_0x00e8
            java.lang.String r0 = "disconnected by sleep policy"
            android.util.Log.d(r3, r0)
            return r6
        L_0x00e8:
            r13 = 8
            com.samsung.android.server.wifi.dqa.ReportData r13 = r1.getLastIndexOfData(r13)
            if (r13 == 0) goto L_0x0108
            java.lang.Integer r14 = java.lang.Integer.valueOf(r6)
            java.lang.String r15 = "state"
            java.lang.Object r14 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r13, (java.lang.String) r15, r14)
            java.lang.Integer r14 = (java.lang.Integer) r14
            int r14 = r14.intValue()
            if (r14 != r4) goto L_0x0108
            java.lang.String r0 = "airplane mode is enabled"
            android.util.Log.d(r3, r0)
            return r6
        L_0x0108:
            r14 = 9
            com.samsung.android.server.wifi.dqa.ReportData r14 = r1.getLastIndexOfData(r14)
            if (r14 == 0) goto L_0x0128
            java.lang.Integer r15 = java.lang.Integer.valueOf(r6)
            java.lang.String r0 = "state"
            java.lang.Object r0 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r14, (java.lang.String) r0, r15)
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r0 = r0.intValue()
            if (r0 != r4) goto L_0x0128
            java.lang.String r4 = "emergency mode is enabled"
            android.util.Log.d(r3, r4)
            return r6
        L_0x0128:
            int r0 = r1.isct
            r15 = -1
            if (r0 != r15) goto L_0x012f
            r1.isct = r6
        L_0x012f:
            r0 = 1
            java.lang.Integer r2 = java.lang.Integer.valueOf(r8)
            java.lang.String r8 = "disconnectReason"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r7, (java.lang.String) r8, r2)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r1.disconnectReason = r2
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)
            java.lang.String r8 = "locallyGenerated"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r7, (java.lang.String) r8, r2)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r1.locallyGenerated = r2
            java.lang.Integer r2 = java.lang.Integer.valueOf(r6)
            java.lang.String r8 = "wpaState"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r7, (java.lang.String) r8, r2)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r1.wpaState = r2
            r2 = 2
            java.lang.Integer r8 = java.lang.Integer.valueOf(r2)
            java.lang.String r15 = "sleep_policy"
            java.lang.Object r8 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r15, r8)
            java.lang.Integer r8 = (java.lang.Integer) r8
            int r8 = r8.intValue()
            r1.slpp = r8
            java.lang.Integer r8 = java.lang.Integer.valueOf(r6)
            java.lang.String r15 = "screen_on"
            java.lang.Object r8 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r15, r8)
            java.lang.Integer r8 = (java.lang.Integer) r8
            int r8 = r8.intValue()
            r1.scrs = r8
            java.lang.Integer r8 = java.lang.Integer.valueOf(r6)
            java.lang.String r15 = "pmsg"
            java.lang.Object r8 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r15, r8)
            java.lang.Integer r8 = (java.lang.Integer) r8
            int r8 = r8.intValue()
            r1.prem = r8
            java.lang.Integer r8 = java.lang.Integer.valueOf(r6)
            java.lang.String r15 = "ppmsg"
            java.lang.Object r8 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r15, r8)
            java.lang.Integer r8 = (java.lang.Integer) r8
            int r8 = r8.intValue()
            r1.pprem = r8
            java.lang.Integer r8 = java.lang.Integer.valueOf(r6)
            java.lang.String r15 = "conn_duration"
            java.lang.Object r8 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r15, r8)
            java.lang.Integer r8 = (java.lang.Integer) r8
            int r8 = r8.intValue()
            r1.apdr = r8
            int r8 = r1.slpp
            if (r8 != r2) goto L_0x01cc
            int r8 = r1.scrs
            if (r8 != 0) goto L_0x01cc
            r1.isct = r2
            r0 = 1
        L_0x01cc:
            int r8 = r1.locallyGenerated
            if (r8 != r4) goto L_0x04ab
            int r8 = r1.isct
            if (r8 == r11) goto L_0x04ab
            r2 = 201(0xc9, float:2.82E-43)
            com.samsung.android.server.wifi.dqa.ReportData r2 = r1.getLastIndexOfData(r2)
            java.lang.String r8 = "user tirggered"
            if (r2 == 0) goto L_0x022b
            android.os.Bundle r11 = r2.mData
            java.lang.Integer r15 = java.lang.Integer.valueOf(r6)
            java.lang.String r6 = "wifiState"
            java.lang.Object r6 = getValue((android.os.Bundle) r11, (java.lang.String) r6, r15)
            java.lang.Integer r6 = (java.lang.Integer) r6
            int r6 = r6.intValue()
            if (r6 != 0) goto L_0x022b
            android.os.Bundle r11 = r2.mData
            java.lang.String r15 = "callBy"
            java.lang.String r4 = "com.android.settings"
            java.lang.Object r4 = getValue((android.os.Bundle) r11, (java.lang.String) r15, r4)
            java.lang.String r4 = (java.lang.String) r4
            r1.appName = r4
            java.lang.String r4 = r1.appName
            boolean r4 = r1.isApiCalledBySystemApk(r4)
            if (r4 != 0) goto L_0x0226
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r8 = "WifiManager.setWifiEnabled(false) api was called by "
            r4.append(r8)
            java.lang.String r8 = r1.appName
            r4.append(r8)
            java.lang.String r4 = r4.toString()
            android.util.Log.d(r3, r4)
            r4 = 1
            r1.isct = r4
            r0 = 1
            r18 = r10
            goto L_0x0562
        L_0x0226:
            android.util.Log.d(r3, r8)
            r3 = 0
            return r3
        L_0x022b:
            r4 = 100
            com.samsung.android.server.wifi.dqa.ReportData r4 = r1.getLastIndexOfData(r4)
            java.lang.String r6 = " api was called by "
            java.lang.String r11 = "WifiManager."
            java.lang.String r15 = "apiName"
            if (r4 == 0) goto L_0x0285
            r16 = r0
            android.os.Bundle r0 = r4.mData
            r17 = r2
            java.lang.String r2 = "callBy"
            r18 = r10
            java.lang.String r10 = "com.android.settings"
            java.lang.Object r0 = getValue((android.os.Bundle) r0, (java.lang.String) r2, r10)
            java.lang.String r0 = (java.lang.String) r0
            r1.appName = r0
            java.lang.String r0 = r1.appName
            boolean r0 = r1.isApiCalledBySystemApk(r0)
            if (r0 != 0) goto L_0x0280
            android.os.Bundle r0 = r4.mData
            java.lang.String r2 = "disconnect"
            java.lang.Object r0 = getValue((android.os.Bundle) r0, (java.lang.String) r15, r2)
            java.lang.String r0 = (java.lang.String) r0
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r2.append(r11)
            r2.append(r0)
            r2.append(r6)
            java.lang.String r6 = r1.appName
            r2.append(r6)
            java.lang.String r2 = r2.toString()
            android.util.Log.d(r3, r2)
            r2 = 1
            r1.isct = r2
            r2 = 1
            r0 = r2
            goto L_0x0562
        L_0x0280:
            android.util.Log.d(r3, r8)
            r2 = 0
            return r2
        L_0x0285:
            r16 = r0
            r17 = r2
            r18 = r10
            r0 = 101(0x65, float:1.42E-43)
            com.samsung.android.server.wifi.dqa.ReportData r0 = r1.getLastIndexOfData(r0)
            if (r0 == 0) goto L_0x02db
            android.os.Bundle r2 = r0.mData
            java.lang.String r10 = "callBy"
            r19 = r4
            java.lang.String r4 = "com.android.settings"
            java.lang.Object r2 = getValue((android.os.Bundle) r2, (java.lang.String) r10, r4)
            java.lang.String r2 = (java.lang.String) r2
            r1.appName = r2
            java.lang.String r2 = r1.appName
            boolean r2 = r1.isApiCalledBySystemApk(r2)
            if (r2 != 0) goto L_0x02d6
            android.os.Bundle r2 = r0.mData
            java.lang.String r4 = "disable"
            java.lang.Object r2 = getValue((android.os.Bundle) r2, (java.lang.String) r15, r4)
            java.lang.String r2 = (java.lang.String) r2
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            r4.append(r11)
            r4.append(r2)
            r4.append(r6)
            java.lang.String r6 = r1.appName
            r4.append(r6)
            java.lang.String r4 = r4.toString()
            android.util.Log.d(r3, r4)
            r4 = 1
            r1.isct = r4
            r4 = 1
            r0 = r4
            goto L_0x0562
        L_0x02d6:
            android.util.Log.d(r3, r8)
            r2 = 0
            return r2
        L_0x02db:
            r19 = r4
            r2 = 102(0x66, float:1.43E-43)
            com.samsung.android.server.wifi.dqa.ReportData r2 = r1.getLastIndexOfData(r2)
            if (r2 == 0) goto L_0x036a
            android.os.Bundle r4 = r2.mData
            java.lang.String r10 = "callBy"
            r20 = r0
            java.lang.String r0 = "com.android.settings"
            java.lang.Object r0 = getValue((android.os.Bundle) r4, (java.lang.String) r10, r0)
            java.lang.String r0 = (java.lang.String) r0
            r1.appName = r0
            java.lang.String r0 = r1.appName
            if (r0 == 0) goto L_0x0341
            java.lang.String r4 = "unknown"
            boolean r0 = r4.equals(r0)
            if (r0 == 0) goto L_0x0341
            android.os.Bundle r0 = r2.mData
            java.lang.String r4 = "callUid"
            java.lang.String r10 = "android.uid.system"
            java.lang.Object r0 = getValue((android.os.Bundle) r0, (java.lang.String) r4, r10)
            java.lang.String r0 = (java.lang.String) r0
            boolean r4 = r1.isApiCalledBySystemUid(r0)
            if (r4 != 0) goto L_0x033c
            android.os.Bundle r4 = r2.mData
            java.lang.String r8 = "remove"
            java.lang.Object r4 = getValue((android.os.Bundle) r4, (java.lang.String) r15, r8)
            java.lang.String r4 = (java.lang.String) r4
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            r8.append(r11)
            r8.append(r4)
            r8.append(r6)
            r8.append(r0)
            java.lang.String r6 = r8.toString()
            android.util.Log.d(r3, r6)
            r6 = 1
            r1.isct = r6
            r6 = 1
            r0 = r6
            goto L_0x0562
        L_0x033c:
            android.util.Log.d(r3, r8)
            r3 = 0
            return r3
        L_0x0341:
            java.lang.String r0 = r1.appName
            boolean r0 = r1.isApiCalledBySystemApk(r0)
            if (r0 != 0) goto L_0x0365
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r4 = "WifiManager.Remove api was called by "
            r0.append(r4)
            java.lang.String r4 = r1.appName
            r0.append(r4)
            java.lang.String r0 = r0.toString()
            android.util.Log.d(r3, r0)
            r4 = 1
            r1.isct = r4
            r0 = 1
            goto L_0x0562
        L_0x0365:
            android.util.Log.d(r3, r8)
            r3 = 0
            return r3
        L_0x036a:
            r20 = r0
            r0 = 103(0x67, float:1.44E-43)
            com.samsung.android.server.wifi.dqa.ReportData r0 = r1.getLastIndexOfData(r0)
            if (r0 == 0) goto L_0x03b6
            android.os.Bundle r4 = r0.mData
            java.lang.String r10 = "callUid"
            r21 = r2
            java.lang.String r2 = "android.uid.system"
            java.lang.Object r2 = getValue((android.os.Bundle) r4, (java.lang.String) r10, r2)
            java.lang.String r2 = (java.lang.String) r2
            boolean r4 = r1.isApiCalledBySystemUid(r2)
            if (r4 != 0) goto L_0x03b1
            android.os.Bundle r4 = r0.mData
            java.lang.String r8 = "connect"
            java.lang.Object r4 = getValue((android.os.Bundle) r4, (java.lang.String) r15, r8)
            java.lang.String r4 = (java.lang.String) r4
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            r8.append(r11)
            r8.append(r4)
            r8.append(r6)
            r8.append(r2)
            java.lang.String r6 = r8.toString()
            android.util.Log.d(r3, r6)
            r6 = 1
            r1.isct = r6
            r6 = 1
            r0 = r6
            goto L_0x0562
        L_0x03b1:
            android.util.Log.d(r3, r8)
            r3 = 0
            return r3
        L_0x03b6:
            r21 = r2
            r2 = 104(0x68, float:1.46E-43)
            com.samsung.android.server.wifi.dqa.ReportData r2 = r1.getLastIndexOfData(r2)
            if (r2 == 0) goto L_0x0402
            android.os.Bundle r4 = r2.mData
            java.lang.String r10 = "callUid"
            r22 = r0
            java.lang.String r0 = "android.uid.system"
            java.lang.Object r0 = getValue((android.os.Bundle) r4, (java.lang.String) r10, r0)
            java.lang.String r0 = (java.lang.String) r0
            boolean r4 = r1.isApiCalledBySystemUid(r0)
            if (r4 != 0) goto L_0x03fd
            android.os.Bundle r4 = r2.mData
            java.lang.String r8 = "startWps"
            java.lang.Object r4 = getValue((android.os.Bundle) r4, (java.lang.String) r15, r8)
            java.lang.String r4 = (java.lang.String) r4
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            r8.append(r11)
            r8.append(r4)
            r8.append(r6)
            r8.append(r0)
            java.lang.String r6 = r8.toString()
            android.util.Log.d(r3, r6)
            r6 = 1
            r1.isct = r6
            r6 = 1
            r0 = r6
            goto L_0x0562
        L_0x03fd:
            android.util.Log.d(r3, r8)
            r3 = 0
            return r3
        L_0x0402:
            r22 = r0
            r0 = 7
            com.samsung.android.server.wifi.dqa.ReportData r4 = r1.getLastIndexOfData(r0)
            if (r4 == 0) goto L_0x0412
            r0 = 8
            r1.isct = r0
            r0 = 1
            goto L_0x0562
        L_0x0412:
            int r0 = r1.prem
            r6 = 131215(0x2008f, float:1.83871E-40)
            if (r0 != r6) goto L_0x0420
            java.lang.String r0 = "start connect"
            android.util.Log.d(r3, r0)
            r3 = 0
            return r3
        L_0x0420:
            r6 = 5
            com.samsung.android.server.wifi.dqa.ReportData r0 = r1.getLastIndexOfData(r6)
            if (r0 == 0) goto L_0x045a
            android.os.Bundle r6 = r0.mData
            r8 = -1
            java.lang.Integer r10 = java.lang.Integer.valueOf(r8)
            java.lang.String r8 = "unwanted_reason"
            java.lang.Object r6 = getValue((android.os.Bundle) r6, (java.lang.String) r8, r10)
            java.lang.Integer r6 = (java.lang.Integer) r6
            int r6 = r6.intValue()
            r1.uwrs = r6
            int r6 = r1.prem
            r8 = 131216(0x20090, float:1.83873E-40)
            if (r6 != r8) goto L_0x045a
            int r6 = r1.uwrs
            r8 = -1
            if (r6 == r8) goto L_0x045a
            int r6 = r1.apdr
            if (r6 != 0) goto L_0x0453
            java.lang.String r6 = "unwanted network"
            android.util.Log.d(r3, r6)
            r3 = 0
            return r3
        L_0x0453:
            r6 = 7
            r1.isct = r6
            r6 = 1
            r0 = r6
            goto L_0x0562
        L_0x045a:
            int r6 = r1.prem
            r8 = 147462(0x24006, float:2.06638E-40)
            if (r6 != r8) goto L_0x0472
            int r6 = r1.uwrs
            r8 = 1
            if (r6 != r8) goto L_0x0472
            int r6 = r1.disconnectReason
            r8 = 3
            if (r6 != r8) goto L_0x0472
            r6 = 7
            r1.isct = r6
            r6 = 1
            r0 = r6
            goto L_0x0562
        L_0x0472:
            r6 = 300(0x12c, float:4.2E-43)
            com.samsung.android.server.wifi.dqa.ReportData r6 = r1.getLastIndexOfData(r6)
            if (r6 == 0) goto L_0x04a9
            android.os.Bundle r8 = r6.mData
            r10 = 0
            java.lang.Integer r11 = java.lang.Integer.valueOf(r10)
            java.lang.String r10 = "dhcpResult"
            java.lang.Object r8 = getValue((android.os.Bundle) r8, (java.lang.String) r10, r11)
            java.lang.Integer r8 = (java.lang.Integer) r8
            int r8 = r8.intValue()
            r1.dhfs = r8
            int r8 = r1.prem
            r10 = 131273(0x200c9, float:1.83953E-40)
            if (r8 == r10) goto L_0x049b
            r10 = 131211(0x2008b, float:1.83866E-40)
            if (r8 != r10) goto L_0x04a9
        L_0x049b:
            int r8 = r1.dhfs
            if (r8 == 0) goto L_0x04a9
            r10 = 1
            if (r8 == r10) goto L_0x04a9
            r8 = 6
            r1.isct = r8
            r8 = 1
            r0 = r8
            goto L_0x0562
        L_0x04a9:
            goto L_0x0540
        L_0x04ab:
            r16 = r0
            r18 = r10
            int r0 = r1.isct
            if (r0 == 0) goto L_0x04b5
            if (r0 != r2) goto L_0x0540
        L_0x04b5:
            int r0 = r1.disconnectReason
            r2 = 4
            r4 = 3
            if (r0 != r4) goto L_0x04bf
            r1.isct = r2
            goto L_0x0540
        L_0x04bf:
            r4 = 6
            if (r0 != r4) goto L_0x04d1
            int r0 = r1.wpaState
            r4 = 5
            if (r0 < r4) goto L_0x0540
            java.lang.String r0 = "disconnected reason=6 illegal state"
            android.util.Log.d(r3, r0)
            r1.isct = r2
            r0 = 1
            goto L_0x0562
        L_0x04d1:
            r4 = 7
            if (r0 != r4) goto L_0x04e3
            int r0 = r1.wpaState
            r4 = 6
            if (r0 >= r4) goto L_0x0540
            java.lang.String r0 = "disconnected reason=7 illegal state"
            android.util.Log.d(r3, r0)
            r1.isct = r2
            r0 = 1
            goto L_0x0562
        L_0x04e3:
            r4 = 15
            if (r0 != r4) goto L_0x04f6
            int r0 = r1.wpaState
            r4 = 7
            if (r0 == r4) goto L_0x0540
            java.lang.String r0 = "disconnected reason=15 illegal state"
            android.util.Log.d(r3, r0)
            r1.isct = r2
            r0 = 1
            goto L_0x0562
        L_0x04f6:
            r4 = 16
            if (r0 != r4) goto L_0x0509
            int r0 = r1.wpaState
            r4 = 8
            if (r0 == r4) goto L_0x0540
            java.lang.String r0 = "disconnected reason=16 illegal state"
            android.util.Log.d(r3, r0)
            r1.isct = r2
            r0 = 1
            goto L_0x0562
        L_0x0509:
            if (r0 == r2) goto L_0x0543
            r2 = 5
            if (r0 == r2) goto L_0x0543
            r2 = 22
            if (r0 == r2) goto L_0x0543
            r2 = 10
            if (r0 == r2) goto L_0x0543
            r2 = 11
            if (r0 == r2) goto L_0x0543
            r2 = 13
            if (r0 == r2) goto L_0x0543
            r2 = 14
            if (r0 == r2) goto L_0x0543
            r2 = 17
            if (r0 == r2) goto L_0x0543
            r2 = 18
            if (r0 == r2) goto L_0x0543
            r2 = 19
            if (r0 == r2) goto L_0x0543
            r2 = 20
            if (r0 == r2) goto L_0x0543
            r2 = 21
            if (r0 != r2) goto L_0x0537
            goto L_0x0543
        L_0x0537:
            if (r0 != 0) goto L_0x0540
            java.lang.String r0 = "maybe beacon loss"
            android.util.Log.d(r3, r0)
            r2 = 0
            return r2
        L_0x0540:
            r0 = r16
            goto L_0x0562
        L_0x0543:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r2 = "disconnected reason="
            r0.append(r2)
            int r2 = r1.disconnectReason
            r0.append(r2)
            java.lang.String r2 = " maybe AP side issue"
            r0.append(r2)
            java.lang.String r0 = r0.toString()
            android.util.Log.d(r3, r0)
            r2 = 3
            r1.isct = r2
            r0 = 1
        L_0x0562:
            if (r0 == 0) goto L_0x05f4
            int r2 = r1.isct
            r4 = -1
            if (r2 != r4) goto L_0x0570
            java.lang.String r2 = "invalid isct value"
            android.util.Log.e(r3, r2)
            r2 = 0
            return r2
        L_0x0570:
            r2 = 0
            android.os.Bundle r4 = r7.mData
            java.lang.String r6 = "oui"
            java.lang.String r8 = "00:00:00"
            java.lang.Object r4 = getValue((android.os.Bundle) r4, (java.lang.String) r6, r8)
            java.lang.String r4 = (java.lang.String) r4
            r1.oui = r4
            android.os.Bundle r4 = r7.mData
            java.lang.Integer r6 = java.lang.Integer.valueOf(r2)
            java.lang.String r2 = "freqeuncy"
            java.lang.Object r2 = getValue((android.os.Bundle) r4, (java.lang.String) r2, r6)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r1.freq = r2
            java.lang.String r2 = "gw"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r2, r9)
            java.lang.String r2 = (java.lang.String) r2
            r1.gateway = r2
            java.lang.String r2 = "ip"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r2, r9)
            java.lang.String r2 = (java.lang.String) r2
            r1.f51ip = r2
            r2 = 0
            java.lang.Integer r4 = java.lang.Integer.valueOf(r2)
            java.lang.String r6 = "internalReason"
            java.lang.Object r4 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r7, (java.lang.String) r6, r4)
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            r1.dhcp = r4
            java.lang.Integer r2 = java.lang.Integer.valueOf(r2)
            java.lang.String r4 = "adpsState"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r4, r2)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r1.adps = r2
            java.lang.String r2 = "pstate"
            java.lang.String r4 = " "
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r2, r4)
            java.lang.String r2 = (java.lang.String) r2
            r1.pres = r2
            java.lang.String r2 = "ssid"
            java.lang.String r4 = "default"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r2, r4)
            java.lang.String r2 = (java.lang.String) r2
            java.lang.String r2 = removeDoubleQuotes(r2)
            r1.ssid = r2
            java.lang.String r2 = "bssid"
            java.lang.String r4 = "00:20:00:00:00:00"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r2, r4)
            java.lang.String r2 = (java.lang.String) r2
            r1.bssid = r2
        L_0x05f4:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r4 = "matches return "
            r2.append(r4)
            r2.append(r0)
            java.lang.String r4 = " isct:"
            r2.append(r4)
            int r4 = r1.isct
            r2.append(r4)
            java.lang.String r2 = r2.toString()
            android.util.Log.d(r3, r2)
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.dqa.PatternWifiDisconnect.matches():boolean");
    }

    private boolean isApiCalledBySystemUid(String callUid) {
        if (callUid != null && callUid.contains("android.uid.system")) {
            return true;
        }
        return false;
    }

    public String getParams() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.oui);
        sb.append(" ");
        sb.append(this.freq);
        sb.append(" ");
        sb.append(this.rssi);
        sb.append(" ");
        sb.append(this.wpaState);
        sb.append(" ");
        sb.append(this.locallyGenerated);
        sb.append(" ");
        sb.append(this.disconnectReason);
        sb.append(" ");
        sb.append(this.dhcp);
        sb.append(" ");
        sb.append(this.apdr);
        sb.append(" ");
        sb.append(this.isct);
        sb.append(" ");
        sb.append(this.dhfs);
        sb.append(" ");
        sb.append(this.adps);
        sb.append(" ");
        sb.append(this.slpp);
        sb.append(" ");
        sb.append(this.scrs);
        sb.append(" ");
        sb.append(this.pres);
        sb.append(" ");
        sb.append(this.prem);
        sb.append(" ");
        sb.append(this.pprem);
        sb.append(" ");
        sb.append(this.uwrs);
        sb.append(" ");
        sb.append("2.9");
        sb.append(" ");
        sb.append(this.apwe);
        sb.append(" ");
        String str = this.appName;
        if (str == null || str.length() <= 0) {
            sb.append("null");
        } else {
            sb.append(this.appName);
        }
        sb.append(" ");
        sb.append(this.ssid);
        sb.append(" ");
        sb.append(this.bssid);
        sb.append(" ");
        sb.append(this.gateway);
        sb.append(" ");
        sb.append(this.f51ip);
        Log.d(TAG, "============================================================================ ");
        Log.d(TAG, sb.toString());
        Log.d(TAG, "============================================================================ ");
        return sb.toString();
    }

    public Bundle getBigDataParams() {
        Bundle bigDataBundle = getBigDataBundle(WifiBigDataLogManager.FEATURE_ISSUE_DETECTOR_DISC1, getParams());
        bigDataBundle.putInt(ReportIdKey.KEY_CATEGORY_ID, this.isct);
        return bigDataBundle;
    }

    public String getPatternId() {
        return "disc1";
    }

    static String removeDoubleQuotes(String string) {
        if (string == null) {
            return "unknown.ssid";
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private boolean isInRange(InetAddress ipAddress, InetAddress gatewayAddress, int prefix) throws UnknownHostException {
        int targetSize;
        ByteBuffer maskBuffer;
        if (ipAddress.getAddress().length == 4) {
            maskBuffer = ByteBuffer.allocate(4).putInt(-1);
            targetSize = 4;
        } else {
            maskBuffer = ByteBuffer.allocate(16).putLong(-1).putLong(-1);
            targetSize = 16;
        }
        BigInteger mask = new BigInteger(1, maskBuffer.array()).not().shiftRight(prefix);
        BigInteger startIp = new BigInteger(1, ByteBuffer.wrap(ipAddress.getAddress()).array()).and(mask);
        BigInteger endIp = startIp.add(mask.not());
        byte[] startIpArr = toBytes(startIp.toByteArray(), targetSize);
        byte[] endIpArr = toBytes(endIp.toByteArray(), targetSize);
        InetAddress startAddress = InetAddress.getByAddress(startIpArr);
        InetAddress endAddress = InetAddress.getByAddress(endIpArr);
        BigInteger start = new BigInteger(1, startAddress.getAddress());
        BigInteger end = new BigInteger(1, endAddress.getAddress());
        ByteBuffer byteBuffer = maskBuffer;
        BigInteger target = new BigInteger(1, gatewayAddress.getAddress());
        int st = start.compareTo(target);
        int te = target.compareTo(end);
        BigInteger bigInteger = end;
        return (st == -1 || st == 0) && (te == -1 || te == 0);
    }

    private byte[] toBytes(byte[] array, int targetSize) {
        int counter = 0;
        List<Byte> newArr = new ArrayList<>();
        while (counter < targetSize && (array.length - 1) - counter >= 0) {
            newArr.add(0, Byte.valueOf(array[(array.length - 1) - counter]));
            counter++;
        }
        int size = newArr.size();
        for (int i = 0; i < targetSize - size; i++) {
            newArr.add(0, (byte) 0);
        }
        byte[] ret = new byte[newArr.size()];
        for (int i2 = 0; i2 < newArr.size(); i2++) {
            ret[i2] = newArr.get(i2).byteValue();
        }
        return ret;
    }
}
