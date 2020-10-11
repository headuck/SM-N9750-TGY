package com.samsung.android.server.wifi.bigdata;

import com.android.server.wifi.util.TelephonyUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class WifiChipInfo {
    private static final String CID_INFO_MURATA = "murata";
    private static final String CID_INFO_MURATAFEM1 = "MURATAFEM1";
    private static final String CID_INFO_MURATAFEM2 = "MURATAFEM2";
    private static final String CID_INFO_MURATAFEM3 = "MURATAFEM3";
    private static final String CID_INFO_SAMSUNG = "SAMSUNG";
    private static final String CID_INFO_SAMSUNGVE = "SAMSUNGVE";
    private static final String CID_INFO_SEMCO = "SEMCO";
    private static final String CID_INFO_SEMCO3RD = "SEMCO3RD";
    private static final String CID_INFO_SEMCOSH = "SEMCOSH";
    private static final String CID_INFO_WISOL = "WISOL";
    private static final String CID_INFO_WISOLFEM1 = "WISOLFEM1";
    private static final String KEY_CHIPSET_VENDOR_NAME = "ld_cnm";
    private static final String KEY_CID_INFO = "Cid_Info";
    private static final String KEY_DRIVER_VERSION = "ld_drv";
    private static final String KEY_FIRMWARE_VERSION = "ld_fwv";
    private static final String KEY_MAC_ADDRESS = "mac_add";
    private static final String NULL_STRING = "null";
    private static final String STRING_NOT_READY = "not ready";
    private static final String WIFI_VER_PREFIX_BRCM = "HD_ver";
    private static final String WIFI_VER_PREFIX_MAVL = "received";
    private static final String WIFI_VER_PREFIX_MTK = "ediatek";
    private static final String WIFI_VER_PREFIX_QCA = "FW:";
    private static final String WIFI_VER_PREFIX_QCOM = "CNSS";
    private static final String WIFI_VER_PREFIX_SLSI = "rv_ver:";
    private static final String WIFI_VER_PREFIX_SPRTRM = "is 0x";
    private static String mChipsetName;
    private static String mCidInfo;
    private static WifiChipInfo sInstance;
    private String mDriverVer = NULL_STRING;
    private String mFirmwareVer = NULL_STRING;
    private String mFirmwareVerFactory = NULL_STRING;
    private boolean mIsReady;
    private boolean mIsReceivedArpResponse = false;
    private String mMacAddress;
    private boolean mNetworkIpConflict = false;
    private String mWifiVerInfoString = NULL_STRING;

    private WifiChipInfo() {
        mChipsetName = NULL_STRING;
        mCidInfo = NULL_STRING;
        this.mIsReady = false;
    }

    public static synchronized WifiChipInfo getInstance() {
        WifiChipInfo wifiChipInfo;
        synchronized (WifiChipInfo.class) {
            if (sInstance == null) {
                sInstance = new WifiChipInfo();
            }
            wifiChipInfo = sInstance;
        }
        return wifiChipInfo;
    }

    public boolean isReady() {
        return this.mIsReady;
    }

    public void updateChipInfos(String cidInfo, String wifiVerInfo) {
        if (wifiVerInfo != null && wifiVerInfo.length() != 0) {
            this.mWifiVerInfoString = wifiVerInfo;
            this.mFirmwareVer = setFirmwareVer(wifiVerInfo, false);
            this.mDriverVer = setDriverVer(wifiVerInfo);
            mChipsetName = setChipsetName(wifiVerInfo);
            mCidInfo = setCidInfo(cidInfo);
            this.mIsReady = true;
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(convertToQuotedString(KEY_FIRMWARE_VERSION) + ":");
        sb.append(convertToQuotedString(this.mFirmwareVer) + ",");
        sb.append(convertToQuotedString(KEY_DRIVER_VERSION) + ":");
        sb.append(convertToQuotedString(this.mDriverVer) + ",");
        sb.append(convertToQuotedString(KEY_CHIPSET_VENDOR_NAME) + ":");
        sb.append(convertToQuotedString(mChipsetName));
        return sb.toString();
    }

    private static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    public String getFirmwareVer(boolean factorymode) {
        if (!factorymode) {
            return this.mFirmwareVer;
        }
        if (!this.mIsReady) {
            return STRING_NOT_READY;
        }
        return setFirmwareVer(this.mWifiVerInfoString, true);
    }

    /*  JADX ERROR: NullPointerException in pass: CodeShrinkVisitor
        java.lang.NullPointerException
        	at jadx.core.dex.instructions.args.InsnArg.wrapInstruction(InsnArg.java:118)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.inline(CodeShrinkVisitor.java:146)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.shrinkBlock(CodeShrinkVisitor.java:71)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.shrinkMethod(CodeShrinkVisitor.java:43)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.visit(CodeShrinkVisitor.java:35)
        */
    private java.lang.String setFirmwareVer(java.lang.String r17, boolean r18) {
        /*
            r16 = this;
            r1 = r17
            r2 = r18
            java.lang.String r0 = "|"
            java.lang.String r3 = "driver version is "
            java.lang.String r4 = "version"
            java.lang.String r5 = "CNSS"
            java.lang.String r6 = "File Close error"
            java.lang.String r7 = "error"
            if (r1 == 0) goto L_0x02a8
            int r8 = r17.length()
            if (r8 != 0) goto L_0x001a
            goto L_0x02a8
        L_0x001a:
            r8 = 0
            r9 = 0
            r10 = 0
            r11 = 0
            r12 = 0
            java.io.BufferedReader r13 = new java.io.BufferedReader     // Catch:{ IOException -> 0x028c }
            java.io.StringReader r14 = new java.io.StringReader     // Catch:{ IOException -> 0x028c }
            r14.<init>(r1)     // Catch:{ IOException -> 0x028c }
            r13.<init>(r14)     // Catch:{ IOException -> 0x028c }
            r8 = r13
            java.lang.String r13 = r8.readLine()     // Catch:{ IOException -> 0x028c }
            r9 = r13
            if (r9 == 0) goto L_0x027e
            java.lang.String r13 = "HD_ver"
            boolean r13 = r9.contains(r13)     // Catch:{ IOException -> 0x028c }
            java.lang.String r15 = "NG"
            r14 = 1
            if (r13 == 0) goto L_0x0093
            java.lang.String r0 = r8.readLine()     // Catch:{ IOException -> 0x028c }
            r9 = r0
            if (r9 == 0) goto L_0x0086
            int r0 = r9.indexOf(r4)     // Catch:{ IOException -> 0x028c }
            r11 = r0
            r0 = -1
            if (r11 == r0) goto L_0x0082
            int r0 = r4.length()     // Catch:{ IOException -> 0x028c }
            int r0 = r0 + r11
            int r11 = r0 + 1
            java.lang.String r0 = " "
            int r0 = r9.indexOf(r0, r11)     // Catch:{ IOException -> 0x028c }
            r12 = r0
            if (r2 != r14) goto L_0x0072
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x028c }
            r0.<init>()     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = "BR"
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = r9.substring(r11, r12)     // Catch:{ IOException -> 0x028c }
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r0 = r0.toString()     // Catch:{ IOException -> 0x028c }
            r3 = r0
            goto L_0x0077
        L_0x0072:
            java.lang.String r0 = r9.substring(r11, r12)     // Catch:{ IOException -> 0x028c }
            r3 = r0
        L_0x0077:
            r8.close()     // Catch:{ IOException -> 0x007e }
            return r3
        L_0x007e:
            r0 = move-exception
            r4 = r0
            r0 = r4
            return r6
        L_0x0082:
            r0 = r15
            r10 = r0
            goto L_0x0263
        L_0x0086:
            r10 = r15
            java.lang.String r0 = "file was damaged, it need check !"
            r8.close()     // Catch:{ IOException -> 0x008f }
            return r0
        L_0x008f:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r6
        L_0x0093:
            boolean r4 = r9.contains(r5)     // Catch:{ IOException -> 0x028c }
            java.lang.String r13 = "QC"
            if (r4 == 0) goto L_0x00d5
            int r0 = r9.indexOf(r5)     // Catch:{ IOException -> 0x028c }
            r11 = r0
            r0 = -1
            if (r11 == r0) goto L_0x00d1
            java.lang.String r0 = "CNSS-PR-"
            int r0 = r0.length()     // Catch:{ IOException -> 0x028c }
            int r11 = r11 + r0
            if (r2 != r14) goto L_0x00c1
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x028c }
            r0.<init>()     // Catch:{ IOException -> 0x028c }
            r0.append(r13)     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = r9.substring(r11)     // Catch:{ IOException -> 0x028c }
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r0 = r0.toString()     // Catch:{ IOException -> 0x028c }
            r3 = r0
            goto L_0x00c6
        L_0x00c1:
            java.lang.String r0 = r9.substring(r11)     // Catch:{ IOException -> 0x028c }
            r3 = r0
        L_0x00c6:
            r8.close()     // Catch:{ IOException -> 0x00cd }
            return r3
        L_0x00cd:
            r0 = move-exception
            r4 = r0
            r0 = r4
            return r6
        L_0x00d1:
            r0 = r15
            r10 = r0
            goto L_0x0263
        L_0x00d5:
            java.lang.String r4 = "FW:"
            boolean r4 = r9.contains(r4)     // Catch:{ IOException -> 0x028c }
            java.lang.String r5 = "FW"
            if (r4 == 0) goto L_0x0121
            int r0 = r9.indexOf(r5)     // Catch:{ IOException -> 0x028c }
            r11 = r0
            r0 = -1
            if (r11 == r0) goto L_0x011d
            int r0 = r5.length()     // Catch:{ IOException -> 0x028c }
            int r0 = r0 + r11
            int r11 = r0 + 1
            java.lang.String r0 = "HW"
            int r0 = r9.indexOf(r0)     // Catch:{ IOException -> 0x028c }
            int r12 = r0 + -2
            if (r2 != r14) goto L_0x010d
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x028c }
            r0.<init>()     // Catch:{ IOException -> 0x028c }
            r0.append(r13)     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = r9.substring(r11, r12)     // Catch:{ IOException -> 0x028c }
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r0 = r0.toString()     // Catch:{ IOException -> 0x028c }
            r3 = r0
            goto L_0x0112
        L_0x010d:
            java.lang.String r0 = r9.substring(r11, r12)     // Catch:{ IOException -> 0x028c }
            r3 = r0
        L_0x0112:
            r8.close()     // Catch:{ IOException -> 0x0119 }
            return r3
        L_0x0119:
            r0 = move-exception
            r4 = r0
            r0 = r4
            return r6
        L_0x011d:
            r0 = r15
            r10 = r0
            goto L_0x0263
        L_0x0121:
            java.lang.String r4 = "received"
            boolean r4 = r9.contains(r4)     // Catch:{ IOException -> 0x028c }
            r13 = 0
            if (r4 == 0) goto L_0x016e
            java.lang.String r0 = ".p"
            int r0 = r9.indexOf(r0)     // Catch:{ IOException -> 0x028c }
            int r11 = r0 + 1
            r0 = -1
            if (r11 == r0) goto L_0x016a
            java.lang.String r0 = r9.substring(r11)     // Catch:{ IOException -> 0x028c }
            r9 = r0
            java.lang.String r0 = "-"
            int r0 = r9.indexOf(r0)     // Catch:{ IOException -> 0x028c }
            r12 = r0
            if (r2 != r14) goto L_0x015a
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x028c }
            r0.<init>()     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = "MV"
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = r9.substring(r13, r12)     // Catch:{ IOException -> 0x028c }
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r0 = r0.toString()     // Catch:{ IOException -> 0x028c }
            r3 = r0
            goto L_0x015f
        L_0x015a:
            java.lang.String r0 = r9.substring(r13, r12)     // Catch:{ IOException -> 0x028c }
            r3 = r0
        L_0x015f:
            r8.close()     // Catch:{ IOException -> 0x0166 }
            return r3
        L_0x0166:
            r0 = move-exception
            r4 = r0
            r0 = r4
            return r6
        L_0x016a:
            r0 = r15
            r10 = r0
            goto L_0x0263
        L_0x016e:
            java.lang.String r4 = "is 0x"
            boolean r4 = r9.contains(r4)     // Catch:{ IOException -> 0x028c }
            if (r4 == 0) goto L_0x01b8
            int r0 = r9.indexOf(r3)     // Catch:{ IOException -> 0x028c }
            int r3 = r3.length()     // Catch:{ IOException -> 0x028c }
            int r0 = r0 + r3
            int r11 = r0 + 1
            r0 = -1
            if (r11 == r0) goto L_0x01b4
            java.lang.String r0 = "] ["
            int r0 = r9.indexOf(r0)     // Catch:{ IOException -> 0x028c }
            r12 = r0
            if (r2 != r14) goto L_0x01a4
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x028c }
            r0.<init>()     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = "SP"
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = r9.substring(r13, r12)     // Catch:{ IOException -> 0x028c }
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r0 = r0.toString()     // Catch:{ IOException -> 0x028c }
            r3 = r0
            goto L_0x01a9
        L_0x01a4:
            java.lang.String r0 = r9.substring(r13, r12)     // Catch:{ IOException -> 0x028c }
            r3 = r0
        L_0x01a9:
            r8.close()     // Catch:{ IOException -> 0x01b0 }
            return r3
        L_0x01b0:
            r0 = move-exception
            r4 = r0
            r0 = r4
            return r6
        L_0x01b4:
            r0 = r15
            r10 = r0
            goto L_0x0263
        L_0x01b8:
            java.lang.String r3 = "rv_ver:"
            boolean r3 = r9.contains(r3)     // Catch:{ IOException -> 0x028c }
            if (r3 == 0) goto L_0x0205
            java.lang.String r3 = r8.readLine()     // Catch:{ IOException -> 0x028c }
            r9 = r3
            int r3 = r9.indexOf(r0)     // Catch:{ IOException -> 0x028c }
            int r11 = r3 + 1
            r3 = -1
            if (r11 == r3) goto L_0x0201
            java.lang.String r3 = r9.substring(r11)     // Catch:{ IOException -> 0x028c }
            r9 = r3
            int r0 = r9.indexOf(r0)     // Catch:{ IOException -> 0x028c }
            r12 = r0
            if (r2 != r14) goto L_0x01f1
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x028c }
            r0.<init>()     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = "LS"
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = r9.substring(r13, r12)     // Catch:{ IOException -> 0x028c }
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r0 = r0.toString()     // Catch:{ IOException -> 0x028c }
            r3 = r0
            goto L_0x01f6
        L_0x01f1:
            java.lang.String r0 = r9.substring(r13, r12)     // Catch:{ IOException -> 0x028c }
            r3 = r0
        L_0x01f6:
            r8.close()     // Catch:{ IOException -> 0x01fd }
            return r3
        L_0x01fd:
            r0 = move-exception
            r4 = r0
            r0 = r4
            return r6
        L_0x0201:
            r0 = r15
            r10 = r0
            goto L_0x0263
        L_0x0205:
            java.lang.String r0 = "ediatek"
            boolean r0 = r9.contains(r0)     // Catch:{ IOException -> 0x028c }
            if (r0 == 0) goto L_0x0261
            java.lang.String r0 = r8.readLine()     // Catch:{ IOException -> 0x028c }
            r9 = r0
            java.lang.String r0 = r8.readLine()     // Catch:{ IOException -> 0x028c }
            r9 = r0
            int r0 = r9.indexOf(r5)     // Catch:{ IOException -> 0x028c }
            r11 = r0
            r0 = -1
            if (r11 == r0) goto L_0x025e
            int r0 = r9.length()     // Catch:{ IOException -> 0x028c }
            r3 = 15
            if (r0 <= r3) goto L_0x022e
            int r0 = r9.length()     // Catch:{ IOException -> 0x028c }
            int r0 = r0 - r3
            r11 = r0
            goto L_0x0235
        L_0x022e:
            java.lang.String r0 = "FW_VER: "
            int r0 = r0.length()     // Catch:{ IOException -> 0x028c }
            int r11 = r11 + r0
        L_0x0235:
            if (r2 != r14) goto L_0x024e
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x028c }
            r0.<init>()     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = "MT"
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r3 = r9.substring(r11)     // Catch:{ IOException -> 0x028c }
            r0.append(r3)     // Catch:{ IOException -> 0x028c }
            java.lang.String r0 = r0.toString()     // Catch:{ IOException -> 0x028c }
            r3 = r0
            goto L_0x0253
        L_0x024e:
            java.lang.String r0 = r9.substring(r11)     // Catch:{ IOException -> 0x028c }
            r3 = r0
        L_0x0253:
            r8.close()     // Catch:{ IOException -> 0x025a }
            return r3
        L_0x025a:
            r0 = move-exception
            r4 = r0
            r0 = r4
            return r6
        L_0x025e:
            r0 = r15
            r10 = r0
            goto L_0x0263
        L_0x0261:
            r0 = r15
            r10 = r0
        L_0x0263:
            boolean r0 = r15.equals(r10)     // Catch:{ IOException -> 0x028c }
            if (r0 == 0) goto L_0x0274
            r8.close()     // Catch:{ IOException -> 0x0270 }
            return r9
        L_0x0270:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r6
        L_0x0274:
            r8.close()     // Catch:{ IOException -> 0x027a }
            return r7
        L_0x027a:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r6
        L_0x027e:
            java.lang.String r0 = "file is null .. !"
            r8.close()     // Catch:{ IOException -> 0x0286 }
            return r0
        L_0x0286:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r6
        L_0x028a:
            r0 = move-exception
            goto L_0x029c
        L_0x028c:
            r0 = move-exception
            r3 = r0
            java.lang.String r0 = "exception"
            if (r8 == 0) goto L_0x029a
            r8.close()     // Catch:{ IOException -> 0x0296 }
            goto L_0x029a
        L_0x0296:
            r0 = move-exception
            r4 = r0
            r0 = r4
            return r6
        L_0x029a:
            return r0
        L_0x029c:
            if (r8 == 0) goto L_0x02a6
            r8.close()     // Catch:{ IOException -> 0x02a2 }
            goto L_0x02a6
        L_0x02a2:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r6
        L_0x02a6:
            throw r0
        L_0x02a8:
            return r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.bigdata.WifiChipInfo.setFirmwareVer(java.lang.String, boolean):java.lang.String");
    }

    public String getDriverVer() {
        return this.mDriverVer;
    }

    /*  JADX ERROR: NullPointerException in pass: CodeShrinkVisitor
        java.lang.NullPointerException
        	at jadx.core.dex.instructions.args.InsnArg.wrapInstruction(InsnArg.java:118)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.inline(CodeShrinkVisitor.java:146)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.shrinkBlock(CodeShrinkVisitor.java:71)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.shrinkMethod(CodeShrinkVisitor.java:43)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.visit(CodeShrinkVisitor.java:35)
        */
    private java.lang.String setDriverVer(java.lang.String r17) {
        /*
            r16 = this;
            r1 = r17
            java.lang.String r0 = "drv_ver:"
            java.lang.String r2 = "cp version is "
            java.lang.String r3 = "-GPL"
            java.lang.String r4 = "SW"
            java.lang.String r5 = "v"
            java.lang.String r6 = "HD_ver:"
            java.lang.String r7 = "File Close error"
            java.lang.String r8 = "error"
            if (r1 == 0) goto L_0x01c7
            int r9 = r17.length()
            if (r9 != 0) goto L_0x001c
            goto L_0x01c7
        L_0x001c:
            r9 = 0
            r10 = 0
            r11 = 0
            r12 = 0
            r13 = 0
            java.io.BufferedReader r14 = new java.io.BufferedReader     // Catch:{ IOException -> 0x01ab }
            java.io.StringReader r15 = new java.io.StringReader     // Catch:{ IOException -> 0x01ab }
            r15.<init>(r1)     // Catch:{ IOException -> 0x01ab }
            r14.<init>(r15)     // Catch:{ IOException -> 0x01ab }
            r9 = r14
            java.lang.String r14 = r9.readLine()     // Catch:{ IOException -> 0x01ab }
            r10 = r14
            if (r10 == 0) goto L_0x019d
            java.lang.String r14 = "HD_ver"
            boolean r14 = r10.contains(r14)     // Catch:{ IOException -> 0x01ab }
            java.lang.String r15 = "NG"
            if (r14 == 0) goto L_0x0067
            int r0 = r10.indexOf(r6)     // Catch:{ IOException -> 0x01ab }
            r12 = r0
            r0 = -1
            if (r12 == r0) goto L_0x0063
            int r0 = r6.length()     // Catch:{ IOException -> 0x01ab }
            int r0 = r0 + r12
            int r12 = r0 + 1
            java.lang.String r0 = " "
            int r0 = r10.indexOf(r0, r12)     // Catch:{ IOException -> 0x01ab }
            r13 = r0
            java.lang.String r0 = r10.substring(r12, r13)     // Catch:{ IOException -> 0x01ab }
            r2 = r0
            r9.close()     // Catch:{ IOException -> 0x005f }
            return r2
        L_0x005f:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r7
        L_0x0063:
            r0 = r15
            r11 = r0
            goto L_0x0182
        L_0x0067:
            java.lang.String r6 = "CNSS"
            boolean r6 = r10.contains(r6)     // Catch:{ IOException -> 0x01ab }
            if (r6 == 0) goto L_0x0097
            int r0 = r10.indexOf(r5)     // Catch:{ IOException -> 0x01ab }
            r12 = r0
            r0 = -1
            if (r12 == r0) goto L_0x0093
            int r0 = r5.length()     // Catch:{ IOException -> 0x01ab }
            int r12 = r12 + r0
            java.lang.String r0 = " CNSS"
            int r0 = r10.indexOf(r0)     // Catch:{ IOException -> 0x01ab }
            r13 = r0
            java.lang.String r0 = r10.substring(r12, r13)     // Catch:{ IOException -> 0x01ab }
            r2 = r0
            r9.close()     // Catch:{ IOException -> 0x008f }
            return r2
        L_0x008f:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r7
        L_0x0093:
            r0 = r15
            r11 = r0
            goto L_0x0182
        L_0x0097:
            java.lang.String r5 = "FW:"
            boolean r5 = r10.contains(r5)     // Catch:{ IOException -> 0x01ab }
            if (r5 == 0) goto L_0x00ca
            int r0 = r10.indexOf(r4)     // Catch:{ IOException -> 0x01ab }
            r12 = r0
            r0 = -1
            if (r12 == r0) goto L_0x00c6
            int r0 = r4.length()     // Catch:{ IOException -> 0x01ab }
            int r0 = r0 + r12
            int r12 = r0 + 1
            java.lang.String r0 = "FW"
            int r0 = r10.indexOf(r0)     // Catch:{ IOException -> 0x01ab }
            int r13 = r0 + -2
            java.lang.String r0 = r10.substring(r12, r13)     // Catch:{ IOException -> 0x01ab }
            r2 = r0
            r9.close()     // Catch:{ IOException -> 0x00c2 }
            return r2
        L_0x00c2:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r7
        L_0x00c6:
            r0 = r15
            r11 = r0
            goto L_0x0182
        L_0x00ca:
            java.lang.String r4 = "received"
            boolean r4 = r10.contains(r4)     // Catch:{ IOException -> 0x01ab }
            if (r4 == 0) goto L_0x00f4
            int r0 = r10.indexOf(r3)     // Catch:{ IOException -> 0x01ab }
            int r12 = r0 + -4
            r0 = -1
            if (r12 == r0) goto L_0x00f0
            int r0 = r10.indexOf(r3)     // Catch:{ IOException -> 0x01ab }
            r13 = r0
            java.lang.String r0 = r10.substring(r12, r13)     // Catch:{ IOException -> 0x01ab }
            r2 = r0
            r9.close()     // Catch:{ IOException -> 0x00ec }
            return r2
        L_0x00ec:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r7
        L_0x00f0:
            r0 = r15
            r11 = r0
            goto L_0x0182
        L_0x00f4:
            java.lang.String r3 = "is 0x"
            boolean r3 = r10.contains(r3)     // Catch:{ IOException -> 0x01ab }
            if (r3 == 0) goto L_0x0125
            int r0 = r10.indexOf(r2)     // Catch:{ IOException -> 0x01ab }
            int r2 = r2.length()     // Catch:{ IOException -> 0x01ab }
            int r12 = r0 + r2
            r0 = -1
            if (r12 == r0) goto L_0x0121
            java.lang.String r0 = "date"
            int r0 = r10.indexOf(r0)     // Catch:{ IOException -> 0x01ab }
            int r13 = r0 + -2
            java.lang.String r0 = r10.substring(r12, r13)     // Catch:{ IOException -> 0x01ab }
            r2 = r0
            r9.close()     // Catch:{ IOException -> 0x011d }
            return r2
        L_0x011d:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r7
        L_0x0121:
            r0 = r15
            r11 = r0
            goto L_0x0182
        L_0x0125:
            java.lang.String r2 = "rv_ver:"
            boolean r2 = r10.contains(r2)     // Catch:{ IOException -> 0x01ab }
            if (r2 == 0) goto L_0x014f
            int r2 = r10.indexOf(r0)     // Catch:{ IOException -> 0x01ab }
            r12 = r2
            r2 = -1
            if (r12 == r2) goto L_0x014c
            int r0 = r0.length()     // Catch:{ IOException -> 0x01ab }
            int r0 = r0 + r12
            int r12 = r0 + 1
            java.lang.String r0 = r10.substring(r12)     // Catch:{ IOException -> 0x01ab }
            r2 = r0
            r9.close()     // Catch:{ IOException -> 0x0148 }
            return r2
        L_0x0148:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r7
        L_0x014c:
            r0 = r15
            r11 = r0
            goto L_0x0182
        L_0x014f:
            java.lang.String r0 = "ediatek"
            boolean r0 = r10.contains(r0)     // Catch:{ IOException -> 0x01ab }
            if (r0 == 0) goto L_0x0180
            java.lang.String r0 = r9.readLine()     // Catch:{ IOException -> 0x01ab }
            r10 = r0
            java.lang.String r0 = "DRIVER_VER"
            int r0 = r10.indexOf(r0)     // Catch:{ IOException -> 0x01ab }
            r12 = r0
            r0 = -1
            if (r12 == r0) goto L_0x017d
            java.lang.String r0 = "DRIVER_VER: "
            int r0 = r0.length()     // Catch:{ IOException -> 0x01ab }
            int r12 = r12 + r0
            java.lang.String r0 = r10.substring(r12)     // Catch:{ IOException -> 0x01ab }
            r2 = r0
            r9.close()     // Catch:{ IOException -> 0x0179 }
            return r2
        L_0x0179:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r7
        L_0x017d:
            r0 = r15
            r11 = r0
            goto L_0x0182
        L_0x0180:
            r0 = r15
            r11 = r0
        L_0x0182:
            boolean r0 = r15.equals(r11)     // Catch:{ IOException -> 0x01ab }
            if (r0 == 0) goto L_0x0193
            r9.close()     // Catch:{ IOException -> 0x018f }
            return r10
        L_0x018f:
            r0 = move-exception
            r2 = r0
            r0 = r2
            return r7
        L_0x0193:
            r9.close()     // Catch:{ IOException -> 0x0199 }
            return r8
        L_0x0199:
            r0 = move-exception
            r2 = r0
            r0 = r2
            return r7
        L_0x019d:
            java.lang.String r0 = "file is null .. !"
            r9.close()     // Catch:{ IOException -> 0x01a5 }
            return r0
        L_0x01a5:
            r0 = move-exception
            r2 = r0
            r0 = r2
            return r7
        L_0x01a9:
            r0 = move-exception
            goto L_0x01bb
        L_0x01ab:
            r0 = move-exception
            r2 = r0
            java.lang.String r0 = "exception"
            if (r9 == 0) goto L_0x01b9
            r9.close()     // Catch:{ IOException -> 0x01b5 }
            goto L_0x01b9
        L_0x01b5:
            r0 = move-exception
            r3 = r0
            r0 = r3
            return r7
        L_0x01b9:
            return r0
        L_0x01bb:
            if (r9 == 0) goto L_0x01c5
            r9.close()     // Catch:{ IOException -> 0x01c1 }
            goto L_0x01c5
        L_0x01c1:
            r0 = move-exception
            r2 = r0
            r0 = r2
            return r7
        L_0x01c5:
            throw r0
        L_0x01c7:
            return r8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.bigdata.WifiChipInfo.setDriverVer(java.lang.String):java.lang.String");
    }

    private String setCidInfo(String infoString) {
        if (infoString == null || infoString.length() == 0) {
            return NULL_STRING;
        }
        if (infoString.charAt(infoString.length() - 1) == 0) {
            infoString = infoString.replace(TelephonyUtil.DEFAULT_EAP_PREFIX, "");
        }
        return infoString.trim();
    }

    public String getCidInfo() {
        return mCidInfo;
    }

    public String getCidInfoForKeyValueType() {
        StringBuffer sb = new StringBuffer();
        sb.append(convertToQuotedString(KEY_CID_INFO) + ":");
        sb.append(convertToQuotedString(mCidInfo));
        return sb.toString();
    }

    public static String getChipsetName() {
        return mChipsetName;
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String getChipsetNameHumanReadable() {
        /*
            java.lang.String r0 = mChipsetName
            int r1 = r0.hashCode()
            switch(r1) {
                case 49: goto L_0x0046;
                case 50: goto L_0x003c;
                case 51: goto L_0x0032;
                case 52: goto L_0x0028;
                case 53: goto L_0x001e;
                case 54: goto L_0x0014;
                case 55: goto L_0x000a;
                default: goto L_0x0009;
            }
        L_0x0009:
            goto L_0x0050
        L_0x000a:
            java.lang.String r1 = "7"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L_0x0009
            r0 = 6
            goto L_0x0051
        L_0x0014:
            java.lang.String r1 = "6"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L_0x0009
            r0 = 5
            goto L_0x0051
        L_0x001e:
            java.lang.String r1 = "5"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L_0x0009
            r0 = 4
            goto L_0x0051
        L_0x0028:
            java.lang.String r1 = "4"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L_0x0009
            r0 = 3
            goto L_0x0051
        L_0x0032:
            java.lang.String r1 = "3"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L_0x0009
            r0 = 2
            goto L_0x0051
        L_0x003c:
            java.lang.String r1 = "2"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L_0x0009
            r0 = 1
            goto L_0x0051
        L_0x0046:
            java.lang.String r1 = "1"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L_0x0009
            r0 = 0
            goto L_0x0051
        L_0x0050:
            r0 = -1
        L_0x0051:
            switch(r0) {
                case 0: goto L_0x0069;
                case 1: goto L_0x0066;
                case 2: goto L_0x0063;
                case 3: goto L_0x0060;
                case 4: goto L_0x005d;
                case 5: goto L_0x005a;
                case 6: goto L_0x0057;
                default: goto L_0x0054;
            }
        L_0x0054:
            java.lang.String r0 = "Unknown"
            return r0
        L_0x0057:
            java.lang.String r0 = "MTK"
            return r0
        L_0x005a:
            java.lang.String r0 = "S.LSI"
            return r0
        L_0x005d:
            java.lang.String r0 = "Spreadtrum"
            return r0
        L_0x0060:
            java.lang.String r0 = "Marvell"
            return r0
        L_0x0063:
            java.lang.String r0 = "QCA"
            return r0
        L_0x0066:
            java.lang.String r0 = "Qualcomm"
            return r0
        L_0x0069:
            java.lang.String r0 = "Broadcom"
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.bigdata.WifiChipInfo.getChipsetNameHumanReadable():java.lang.String");
    }

    private static String setChipsetName(String wifiVerInfo) {
        if (wifiVerInfo == null || wifiVerInfo.length() == 0) {
            return "error";
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new StringReader(wifiVerInfo));
            String verString = br.readLine();
            if (verString == null) {
                try {
                    br.close();
                    return "91";
                } catch (IOException e) {
                    return "93";
                }
            } else if (verString.contains(WIFI_VER_PREFIX_BRCM)) {
                try {
                    br.close();
                    return "1";
                } catch (IOException e2) {
                    return "93";
                }
            } else if (verString.contains(WIFI_VER_PREFIX_QCOM)) {
                try {
                    br.close();
                    return "2";
                } catch (IOException e3) {
                    return "93";
                }
            } else if (verString.contains(WIFI_VER_PREFIX_QCA)) {
                try {
                    br.close();
                    return "3";
                } catch (IOException e4) {
                    return "93";
                }
            } else if (verString.contains(WIFI_VER_PREFIX_MAVL)) {
                try {
                    br.close();
                    return "4";
                } catch (IOException e5) {
                    return "93";
                }
            } else if (verString.contains(WIFI_VER_PREFIX_SPRTRM)) {
                try {
                    br.close();
                    return "5";
                } catch (IOException e6) {
                    return "93";
                }
            } else if (verString.contains(WIFI_VER_PREFIX_SLSI)) {
                try {
                    br.close();
                    return "6";
                } catch (IOException e7) {
                    return "93";
                }
            } else if (verString.contains(WIFI_VER_PREFIX_MTK)) {
                try {
                    br.close();
                    return "7";
                } catch (IOException e8) {
                    return "93";
                }
            } else if ("NG".equals("NG")) {
                try {
                    br.close();
                    return "90";
                } catch (IOException e9) {
                    return "93";
                }
            } else {
                try {
                    br.close();
                    return "94";
                } catch (IOException e10) {
                    return "93";
                }
            }
        } catch (IOException e11) {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e12) {
                    return "93";
                }
            }
            return "92";
        }
    }

    public String readWifiVersion() {
        return this.mWifiVerInfoString;
    }

    public String getMacAddress() {
        return this.mMacAddress;
    }

    public void setMacAddress(String macAddress) {
        this.mMacAddress = macAddress;
    }

    public void setDuplicatedIpDetect(boolean enable) {
        this.mNetworkIpConflict = enable;
    }

    public boolean getDuplicatedIpDetect() {
        return this.mNetworkIpConflict;
    }

    public void setArpResult(boolean enable) {
        this.mIsReceivedArpResponse = enable;
    }

    public boolean getArpResult() {
        return this.mIsReceivedArpResponse;
    }
}
