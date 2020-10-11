package com.samsung.android.server.wifi.dqa;

import android.os.Bundle;
import android.util.Log;
import com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager;
import java.util.ArrayList;
import java.util.Collection;

public class PatternWifiConnecting extends WifiIssuePattern {
    private static final int BASE_RSSI_CONDITION = -70;
    private static final int CATEGORY_ID_CAN_NOT_CHANGE_WIFI_STATE = 2;
    private static final int CATEGORY_ID_CONNECT_FAIL_ASSOC_REJECT = 5;
    private static final int CATEGORY_ID_CONNECT_FAIL_AUTH_FAIL = 6;
    private static final int CATEGORY_ID_CONNECT_FAIL_DHCP_REASON = 7;
    private static final int CATEGORY_ID_HIDL_PROBLEM = 9;
    private static final int CATEGORY_ID_SCAN_FAIL = 8;
    private static final int CATEGORY_ID_SYSTEM_PROBLEM = 1;
    private static final int CATEGORY_ID_UNSTABLE_AP = 4;
    private static final long MAX_DURATION_FOR_DETECTING_WIFI_ONOFF_ISSUE = 1500;
    private static final String TAG = "PatternWifiConnecting";
    private static final String UNKNOWN = "unknown";
    private String mBssid;
    private int mCategoryId;
    private int mFrequency;
    private int mHangReason;
    private int mKeyMgmt;
    private int mLastProceedMessage;
    private String mLastProceedState;
    private int mLastReportId;
    private ReportData mLastTriedToConnectReport;
    private long mLastUpdatedWifiStateTime;
    private int mNumAssociation;
    private String mOui;
    private String mPackageName;
    private int mReason;
    private int mRssi;
    private String mSsid;
    private int mSupplicantDisconnectCount;
    private final String mVersion = "0.6";

    public PatternWifiConnecting() {
        initValues();
    }

    public Collection<Integer> getAssociatedKeys() {
        Collection<Integer> ret = new ArrayList<>();
        ret.add(7);
        ret.add(10);
        ret.add(201);
        ret.add(11);
        ret.add(Integer.valueOf(ReportIdKey.ID_SCAN_FAIL));
        return ret;
    }

    public boolean isAssociated(int reportId, ReportData reportData) {
        this.mLastReportId = reportId;
        if (reportId != 7) {
            if (reportId != 201) {
                if (reportId == 401 || reportId == 10 || reportId == 11) {
                    return true;
                }
            } else if (reportData.mTime - this.mLastUpdatedWifiStateTime >= MAX_DURATION_FOR_DETECTING_WIFI_ONOFF_ISSUE || isApiCalledBySystemApk((String) getValue(reportData, ReportIdKey.KEY_CALL_BY, ""))) {
                this.mLastUpdatedWifiStateTime = reportData.mTime;
            } else {
                this.mLastUpdatedWifiStateTime = reportData.mTime;
                return true;
            }
            return false;
        }
        int counter = ((Integer) getValue(reportData, ReportIdKey.KEY_COUNT, -1)).intValue();
        Log.i(TAG, "isAssociated counter:" + counter);
        return counter != -1 ? counter >= 1 : ((Integer) getValue(reportData, "reason", -1)).intValue() != -1 ? true : true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:102:0x0401  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean matches() {
        /*
            r19 = this;
            r0 = r19
            r19.initValues()
            int r1 = r0.mLastReportId
            java.lang.String r2 = "oui"
            r3 = -200(0xffffffffffffff38, float:NaN)
            java.lang.Integer r3 = java.lang.Integer.valueOf(r3)
            java.lang.String r4 = "rssi"
            r5 = 2
            r6 = 7
            java.lang.String r7 = ""
            java.lang.String r8 = "matched category id : "
            java.lang.String r9 = "unknown"
            java.lang.String r10 = "reason"
            r11 = -1
            java.lang.Integer r12 = java.lang.Integer.valueOf(r11)
            r13 = 1
            java.lang.String r14 = "PatternWifiConnecting"
            r15 = 0
            java.lang.Integer r15 = java.lang.Integer.valueOf(r15)
            if (r1 != r6) goto L_0x007b
            com.samsung.android.server.wifi.dqa.ReportData r1 = r0.getLastIndexOfData(r6)
            if (r1 == 0) goto L_0x0079
            r0.mCategoryId = r13
            java.lang.String r2 = "count"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r2, r15)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r0.mSupplicantDisconnectCount = r2
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r10, r12)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r0.mHangReason = r2
            java.lang.String r2 = "pstate"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r2, r9)
            java.lang.String r2 = (java.lang.String) r2
            r0.mLastProceedState = r2
            java.lang.String r2 = "pmsg"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r2, r15)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r0.mLastProceedMessage = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r2.append(r8)
            int r3 = r0.mCategoryId
            r2.append(r3)
            java.lang.String r2 = r2.toString()
            android.util.Log.i(r14, r2)
            return r13
        L_0x0079:
            goto L_0x01d4
        L_0x007b:
            r6 = 17
            if (r1 != r6) goto L_0x00c6
            r1 = 17
            com.samsung.android.server.wifi.dqa.ReportData r1 = r0.getLastIndexOfData(r1)
            if (r1 == 0) goto L_0x00c4
            r2 = 9
            r0.mCategoryId = r2
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r10, r12)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r0.mHangReason = r2
            java.lang.String r2 = "pstate"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r2, r9)
            java.lang.String r2 = (java.lang.String) r2
            r0.mLastProceedState = r2
            java.lang.String r2 = "pmsg"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r2, r15)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r0.mLastProceedMessage = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r2.append(r8)
            int r3 = r0.mCategoryId
            r2.append(r3)
            java.lang.String r2 = r2.toString()
            android.util.Log.i(r14, r2)
            return r13
        L_0x00c4:
            goto L_0x01d4
        L_0x00c6:
            r6 = 10
            if (r1 != r6) goto L_0x0124
            r1 = 10
            com.samsung.android.server.wifi.dqa.ReportData r1 = r0.getLastIndexOfData(r1)
            if (r1 == 0) goto L_0x0122
            r5 = 4
            r0.mCategoryId = r5
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r8)
            int r6 = r0.mCategoryId
            r5.append(r6)
            java.lang.String r5 = r5.toString()
            android.util.Log.i(r14, r5)
            r5 = 200(0xc8, float:2.8E-43)
            com.samsung.android.server.wifi.dqa.ReportData r5 = r0.getLastIndexOfData(r5)
            if (r5 == 0) goto L_0x0121
            java.lang.Object r3 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r4, r3)
            java.lang.Integer r3 = (java.lang.Integer) r3
            int r3 = r3.intValue()
            r0.mRssi = r3
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r2, r9)
            java.lang.String r2 = (java.lang.String) r2
            r0.mOui = r2
            java.lang.String r2 = "wpaSecureType"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r2, r15)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r0.mKeyMgmt = r2
            java.lang.String r2 = "freqeuncy"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r2, r15)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r0.mFrequency = r2
        L_0x0121:
            return r13
        L_0x0122:
            goto L_0x01d4
        L_0x0124:
            r6 = 201(0xc9, float:2.82E-43)
            if (r1 != r6) goto L_0x0193
            com.samsung.android.server.wifi.dqa.ReportData r1 = r0.getLastIndexOfData(r6)
            com.samsung.android.server.wifi.dqa.ReportData r6 = r0.getLastIndexOfData(r6, r5)
            if (r1 == 0) goto L_0x0190
            if (r6 == 0) goto L_0x0190
            java.lang.String r11 = "wifiState"
            java.lang.Object r11 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r6, (java.lang.String) r11, r15)
            java.lang.Integer r11 = (java.lang.Integer) r11
            int r11 = r11.intValue()
            java.lang.String r13 = "wifiState"
            java.lang.Object r13 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r13, r15)
            java.lang.Integer r13 = (java.lang.Integer) r13
            int r13 = r13.intValue()
            if (r11 == r13) goto L_0x018d
            java.lang.String r5 = "callBy"
            java.lang.Object r5 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r6, (java.lang.String) r5, r7)
            java.lang.String r5 = (java.lang.String) r5
            r18 = r6
            java.lang.String r6 = "callBy"
            java.lang.Object r6 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r6, r7)
            java.lang.String r6 = (java.lang.String) r6
            r0.mPackageName = r6
            if (r5 == 0) goto L_0x01d3
            java.lang.String r6 = r0.mPackageName
            boolean r6 = r5.equals(r6)
            if (r6 != 0) goto L_0x01d3
            java.lang.String r6 = r0.mPackageName
            boolean r6 = r0.isApiCalledBySystemApk(r6)
            if (r6 != 0) goto L_0x01d3
            r2 = 2
            r0.mCategoryId = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r2.append(r8)
            int r3 = r0.mCategoryId
            r2.append(r3)
            java.lang.String r2 = r2.toString()
            android.util.Log.i(r14, r2)
            r2 = 1
            return r2
        L_0x018d:
            r18 = r6
            goto L_0x01d3
        L_0x0190:
            r18 = r6
            goto L_0x01d3
        L_0x0193:
            r5 = 401(0x191, float:5.62E-43)
            if (r1 != r5) goto L_0x01d3
            r1 = 401(0x191, float:5.62E-43)
            com.samsung.android.server.wifi.dqa.ReportData r1 = r0.getLastIndexOfData(r1)
            if (r1 == 0) goto L_0x01d4
            r2 = 8
            r0.mCategoryId = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r2.append(r8)
            int r3 = r0.mCategoryId
            r2.append(r3)
            java.lang.String r2 = r2.toString()
            android.util.Log.i(r14, r2)
            java.lang.String r2 = "count"
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r2, r15)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r0.mSupplicantDisconnectCount = r2
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r10, r12)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            r0.mHangReason = r2
            r2 = 1
            return r2
        L_0x01d3:
        L_0x01d4:
            r1 = 0
            com.samsung.android.server.wifi.dqa.ReportData r5 = r0.mLastTriedToConnectReport
            if (r5 == 0) goto L_0x03f5
            java.lang.String r6 = "netid"
            java.lang.Object r8 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r6, r12)
            java.lang.Integer r8 = (java.lang.Integer) r8
            int r8 = r8.intValue()
            r11 = -1
            if (r8 != r11) goto L_0x01f0
            java.lang.String r2 = "invalid network ID"
            android.util.Log.i(r14, r2)
            r13 = r1
            goto L_0x03f6
        L_0x01f0:
            java.lang.String r11 = "numAssoc"
            java.lang.Object r11 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r11, r15)
            java.lang.Integer r11 = (java.lang.Integer) r11
            int r11 = r11.intValue()
            r0.mNumAssociation = r11
            int r11 = r0.mNumAssociation
            if (r11 != 0) goto L_0x0207
            java.lang.String r11 = "first time connection"
            android.util.Log.i(r14, r11)
        L_0x0207:
            java.lang.String r11 = "isLinkDebouncing"
            java.lang.Object r11 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r11, r15)
            java.lang.Integer r11 = (java.lang.Integer) r11
            int r11 = r11.intValue()
            r13 = 1
            if (r11 != r13) goto L_0x021e
            java.lang.String r2 = "it's link debouncing connection"
            android.util.Log.i(r14, r2)
            r13 = r1
            goto L_0x03f6
        L_0x021e:
            r11 = 2
            com.samsung.android.server.wifi.dqa.ReportData r11 = r0.getLastIndexOfData(r11)
            if (r11 == 0) goto L_0x0247
            r13 = r1
            r16 = r2
            long r1 = r11.mTime
            r17 = r3
            r18 = r4
            long r3 = r5.mTime
            int r1 = (r1 > r3 ? 1 : (r1 == r3 ? 0 : -1))
            if (r1 <= 0) goto L_0x024e
            java.lang.Object r1 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r11, (java.lang.String) r6, r12)
            java.lang.Integer r1 = (java.lang.Integer) r1
            int r1 = r1.intValue()
            if (r1 != r8) goto L_0x024e
            java.lang.String r2 = "network is connected"
            android.util.Log.i(r14, r2)
            goto L_0x03f6
        L_0x0247:
            r13 = r1
            r16 = r2
            r17 = r3
            r18 = r4
        L_0x024e:
            java.lang.String r1 = "ssid"
            java.lang.Object r1 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r1, r9)
            java.lang.String r1 = (java.lang.String) r1
            r0.mSsid = r1
            r1 = 14
            com.samsung.android.server.wifi.dqa.ReportData r1 = r0.getLastIndexOfData(r1)
            if (r1 == 0) goto L_0x0331
            java.lang.Object r2 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r6, r12)
            java.lang.Integer r2 = (java.lang.Integer) r2
            int r2 = r2.intValue()
            if (r8 != r2) goto L_0x0310
            java.lang.String r3 = "bssid"
            java.lang.Object r3 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r3, r7)
            java.lang.String r3 = (java.lang.String) r3
            r0.mBssid = r3
            java.lang.String r3 = r0.mBssid
            java.lang.String r4 = "any"
            boolean r3 = r4.equals(r3)
            if (r3 == 0) goto L_0x02fe
            r3 = 202(0xca, float:2.83E-43)
            com.samsung.android.server.wifi.dqa.ReportData r3 = r0.getLastIndexOfData(r3)
            if (r3 == 0) goto L_0x02ca
            r6 = r17
            r4 = r18
            java.lang.Object r4 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r3, (java.lang.String) r4, r6)
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            r0.mRssi = r4
            r4 = r16
            java.lang.Object r4 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r3, (java.lang.String) r4, r9)
            java.lang.String r4 = (java.lang.String) r4
            r0.mOui = r4
            java.lang.Object r4 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r3, (java.lang.String) r10, r15)
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            r0.mReason = r4
            java.lang.String r4 = "wpaSecureType"
            java.lang.Object r4 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r3, (java.lang.String) r4, r15)
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            r0.mKeyMgmt = r4
            java.lang.String r4 = "freqeuncy"
            java.lang.Object r4 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r3, (java.lang.String) r4, r15)
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            r0.mFrequency = r4
        L_0x02ca:
            int r4 = r0.mRssi
            r6 = -70
            if (r4 >= r6) goto L_0x02e8
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r6 = "weak signal "
            r4.append(r6)
            int r6 = r0.mRssi
            r4.append(r6)
            java.lang.String r4 = r4.toString()
            android.util.Log.i(r14, r4)
            goto L_0x03f6
        L_0x02e8:
            r4 = 5
            r0.mCategoryId = r4
            int r4 = r0.mReason
            if (r4 != 0) goto L_0x02fb
            java.lang.Object r4 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r10, r15)
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            r0.mReason = r4
        L_0x02fb:
            r4 = 1
            goto L_0x03f7
        L_0x02fe:
            r3 = r16
            r4 = r18
            r16 = r1
            r1 = r17
            r17 = r2
            java.lang.String r2 = "assoc.rejected (auto connection)"
            android.util.Log.d(r14, r2)
            r18 = r11
            goto L_0x033b
        L_0x0310:
            r3 = r16
            r4 = r18
            r16 = r1
            r1 = r17
            r17 = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r18 = r11
            java.lang.String r11 = "assoc.rejected but network id is mismatched. try:"
            r2.append(r11)
            r2.append(r8)
            java.lang.String r2 = r2.toString()
            android.util.Log.d(r14, r2)
            goto L_0x033b
        L_0x0331:
            r3 = r16
            r4 = r18
            r16 = r1
            r18 = r11
            r1 = r17
        L_0x033b:
            r2 = 200(0xc8, float:2.8E-43)
            com.samsung.android.server.wifi.dqa.ReportData r2 = r0.getLastIndexOfData(r2)
            if (r2 == 0) goto L_0x0373
            java.lang.Object r1 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r2, (java.lang.String) r4, r1)
            java.lang.Integer r1 = (java.lang.Integer) r1
            int r1 = r1.intValue()
            r0.mRssi = r1
            java.lang.Object r1 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r2, (java.lang.String) r3, r9)
            java.lang.String r1 = (java.lang.String) r1
            r0.mOui = r1
            java.lang.String r1 = "wpaSecureType"
            java.lang.Object r1 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r2, (java.lang.String) r1, r15)
            java.lang.Integer r1 = (java.lang.Integer) r1
            int r1 = r1.intValue()
            r0.mKeyMgmt = r1
            java.lang.String r1 = "freqeuncy"
            java.lang.Object r1 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r2, (java.lang.String) r1, r15)
            java.lang.Integer r1 = (java.lang.Integer) r1
            int r1 = r1.intValue()
            r0.mFrequency = r1
        L_0x0373:
            int r1 = r0.mRssi
            r3 = -70
            if (r1 >= r3) goto L_0x0390
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r3 = "weak signal "
            r1.append(r3)
            int r3 = r0.mRssi
            r1.append(r3)
            java.lang.String r1 = r1.toString()
            android.util.Log.i(r14, r1)
            goto L_0x03f6
        L_0x0390:
            r1 = 15
            com.samsung.android.server.wifi.dqa.ReportData r1 = r0.getLastIndexOfData(r1)
            if (r1 == 0) goto L_0x03d3
            java.lang.Object r3 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r6, r12)
            java.lang.Integer r3 = (java.lang.Integer) r3
            int r3 = r3.intValue()
            if (r8 != r3) goto L_0x03bf
            r4 = 6
            r0.mCategoryId = r4
            java.lang.String r4 = "bssid"
            java.lang.Object r4 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r5, (java.lang.String) r4, r7)
            java.lang.String r4 = (java.lang.String) r4
            r0.mBssid = r4
            java.lang.Object r4 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r1, (java.lang.String) r10, r15)
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            r0.mReason = r4
            r4 = 1
            goto L_0x03f7
        L_0x03bf:
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r6 = "auth.failed but network id is mismatched. try:"
            r4.append(r6)
            r4.append(r8)
            java.lang.String r4 = r4.toString()
            android.util.Log.d(r14, r4)
        L_0x03d3:
            r3 = 300(0x12c, float:4.2E-43)
            com.samsung.android.server.wifi.dqa.ReportData r3 = r0.getLastIndexOfData(r3)
            if (r3 == 0) goto L_0x03f6
            r4 = 1
            java.lang.Integer r6 = java.lang.Integer.valueOf(r4)
            java.lang.String r7 = "dhcpResult"
            java.lang.Object r6 = getValue((com.samsung.android.server.wifi.dqa.ReportData) r3, (java.lang.String) r7, r6)
            java.lang.Integer r6 = (java.lang.Integer) r6
            int r6 = r6.intValue()
            if (r6 == r4) goto L_0x03f6
            r0.mReason = r6
            r4 = 7
            r0.mCategoryId = r4
            r4 = 1
            goto L_0x03f7
        L_0x03f5:
            r13 = r1
        L_0x03f6:
            r4 = r13
        L_0x03f7:
            r1 = 11
            com.samsung.android.server.wifi.dqa.ReportData r1 = r0.getLastIndexOfData(r1)
            r0.mLastTriedToConnectReport = r1
            if (r4 == 0) goto L_0x0417
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "pattern matched categoryId:"
            r1.append(r2)
            int r2 = r0.mCategoryId
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.i(r14, r1)
        L_0x0417:
            return r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.dqa.PatternWifiConnecting.matches():boolean");
    }

    public Bundle getBigDataParams() {
        Bundle bigDataBundle = getBigDataBundle(WifiBigDataLogManager.FEATURE_ISSUE_DETECTOR_DISC2, getParams());
        bigDataBundle.putInt(ReportIdKey.KEY_CATEGORY_ID, this.mCategoryId);
        return bigDataBundle;
    }

    private void initValues() {
        this.mCategoryId = 0;
        this.mLastProceedMessage = 0;
        this.mLastProceedState = UNKNOWN;
        this.mHangReason = -1;
        this.mSupplicantDisconnectCount = 0;
        this.mReason = 0;
        this.mSsid = UNKNOWN;
        this.mBssid = UNKNOWN;
        this.mOui = UNKNOWN;
        this.mNumAssociation = 0;
        this.mRssi = -200;
        this.mKeyMgmt = 0;
        this.mFrequency = 0;
        this.mPackageName = UNKNOWN;
    }

    private String getParams() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.mCategoryId);
        sb.append(" ");
        sb.append(this.mLastProceedState);
        sb.append(" ");
        sb.append(this.mLastProceedMessage);
        sb.append(" ");
        sb.append(this.mHangReason);
        sb.append(" ");
        sb.append(this.mSupplicantDisconnectCount);
        sb.append(" ");
        sb.append(this.mReason);
        sb.append(" ");
        sb.append(this.mNumAssociation);
        sb.append(" ");
        sb.append(this.mRssi);
        sb.append(" ");
        sb.append(this.mOui);
        sb.append(" ");
        sb.append(this.mKeyMgmt);
        sb.append(" ");
        sb.append(this.mFrequency);
        sb.append(" ");
        sb.append(this.mPackageName);
        sb.append(" ");
        sb.append("0.6");
        sb.append(" ");
        sb.append(this.mSsid);
        sb.append(" ");
        sb.append(this.mBssid);
        return sb.toString();
    }

    public String getPatternId() {
        return "disc2";
    }
}
