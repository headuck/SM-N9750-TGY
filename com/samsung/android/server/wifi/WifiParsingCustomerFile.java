package com.samsung.android.server.wifi;

import android.util.LocalLog;
import android.util.Log;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class WifiParsingCustomerFile {
    private static final String TAG = "WifiDefaultApController.Customer";
    private static WifiParsingCustomerFile instance;
    private static File mFilePathDefaultAp = new File("/data/misc/wifi/default_ap.conf");
    private static File mFilePathGeneralNwInfo = new File("/data/misc/wifi/generalinfo_nw.conf");
    private final LocalLog mLocalLog = new LocalLog(256);
    private CscParser mParser = null;

    private void WifiParsingCustomerFile() {
    }

    public static WifiParsingCustomerFile getInstance() {
        if (instance == null) {
            instance = new WifiParsingCustomerFile();
        }
        return instance;
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r14v3, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r13v11, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r14v4, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r13v12, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r14v6, resolved type: org.w3c.dom.NodeList} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r13v16, resolved type: int} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r13v17, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r14v7, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r14v8, resolved type: org.w3c.dom.NodeList} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r13v18, resolved type: int} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r14v18, resolved type: org.w3c.dom.NodeList} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r13v48, resolved type: int} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r14v19, resolved type: org.w3c.dom.NodeList} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r14v20, resolved type: org.w3c.dom.NodeList} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r13v49, resolved type: int} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r13v50, resolved type: int} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r14v21, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r13v51, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r13v52, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r13v53, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r14v22, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r14v23, resolved type: java.lang.String} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x039c  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0291  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void getCustomerFile() {
        /*
            r46 = this;
            r1 = r46
            java.lang.String r2 = "}\n"
            java.lang.String r3 = "    networkname="
            java.lang.String r4 = "network={\n"
            java.lang.String r5 = "\n"
            java.lang.String r6 = "\""
            r0 = 0
            r7 = 0
            r8 = 0
            r9 = 0
            r10 = 0
            r11 = 0
            r12 = 0
            r13 = 0
            java.lang.String r14 = "Settings."
            java.lang.String r15 = "WifiProfile"
            r16 = r7
            java.lang.String r7 = "WifiSSID"
            r17 = r8
            java.lang.String r8 = "WifiKeyMgmt"
            r18 = r9
            java.lang.String r9 = "WifiPSK"
            r19 = r10
            java.lang.String r10 = "WifiHiddenSSID"
            r20 = r11
            java.lang.String r11 = "WifiEAPMethod"
            r21 = r12
            java.lang.String r12 = "NetworkName"
            r22 = r13
            java.lang.String r13 = "GeneralInfo."
            r23 = r13
            java.lang.String r13 = "NetworkInfo"
            r24 = r13
            java.lang.String r13 = "MCCMNC"
            r25 = r13
            java.lang.String r13 = "NetworkName"
            r26 = 0
            r27 = 0
            r28 = 0
            r29 = 0
            r30 = 0
            r31 = 0
            r32 = r0
            com.samsung.android.server.wifi.CscParser r0 = new com.samsung.android.server.wifi.CscParser
            r33 = r13
            java.lang.String r13 = com.samsung.android.server.wifi.CscParser.getCustomerPath()
            r0.<init>((java.lang.String) r13)
            r1.mParser = r0
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r13 = "getCustomerFile: PATH: "
            r0.append(r13)
            java.lang.String r13 = com.samsung.android.server.wifi.CscParser.getCustomerPath()
            r0.append(r13)
            java.lang.String r0 = r0.toString()
            r1.logi(r0)
            com.samsung.android.server.wifi.CscParser r0 = r1.mParser
            org.w3c.dom.Node r13 = r0.search(r14)
            com.samsung.android.server.wifi.CscParser r0 = r1.mParser
            r34 = r14
            org.w3c.dom.NodeList r14 = r0.searchList(r13, r15)
            if (r14 != 0) goto L_0x0089
            java.lang.String r0 = "getCustomerFile: No WifiProfileNodeList to setup"
            r1.loge(r0)
            return
        L_0x0089:
            r35 = r13
            int r13 = r14.getLength()
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r28 = r15
            java.lang.String r15 = "getCustomerFile: parsing WifiProfile from customer.xml file, number: "
            r0.append(r15)
            r0.append(r13)
            java.lang.String r0 = r0.toString()
            r1.logd(r0)
            java.lang.String[] r15 = new java.lang.String[r13]
            r32 = r2
            java.lang.String[] r2 = new java.lang.String[r13]
            r16 = r3
            java.lang.String[] r3 = new java.lang.String[r13]
            r19 = r5
            java.lang.String[] r5 = new java.lang.String[r13]
            r20 = r6
            java.lang.String[] r6 = new java.lang.String[r13]
            r17 = r4
            java.lang.String[] r4 = new java.lang.String[r13]
            r0 = 0
            r36 = r26
        L_0x00bf:
            if (r0 >= r13) goto L_0x0151
            r18 = r13
            org.w3c.dom.Node r13 = r14.item(r0)
            r26 = r14
            com.samsung.android.server.wifi.CscParser r14 = r1.mParser
            org.w3c.dom.Node r14 = r14.search(r13, r7)
            r37 = r7
            com.samsung.android.server.wifi.CscParser r7 = r1.mParser
            org.w3c.dom.Node r7 = r7.search(r13, r8)
            r38 = r8
            com.samsung.android.server.wifi.CscParser r8 = r1.mParser
            org.w3c.dom.Node r8 = r8.search(r13, r9)
            r39 = r9
            com.samsung.android.server.wifi.CscParser r9 = r1.mParser
            org.w3c.dom.Node r9 = r9.search(r13, r10)
            r40 = r10
            com.samsung.android.server.wifi.CscParser r10 = r1.mParser
            org.w3c.dom.Node r10 = r10.search(r13, r11)
            r41 = r11
            com.samsung.android.server.wifi.CscParser r11 = r1.mParser
            org.w3c.dom.Node r11 = r11.search(r13, r12)
            if (r14 == 0) goto L_0x0104
            r42 = r12
            com.samsung.android.server.wifi.CscParser r12 = r1.mParser
            java.lang.String r12 = r12.getValue(r14)
            r15[r0] = r12
            goto L_0x0106
        L_0x0104:
            r42 = r12
        L_0x0106:
            if (r7 == 0) goto L_0x0110
            com.samsung.android.server.wifi.CscParser r12 = r1.mParser
            java.lang.String r12 = r12.getValue(r7)
            r2[r0] = r12
        L_0x0110:
            if (r8 == 0) goto L_0x011a
            com.samsung.android.server.wifi.CscParser r12 = r1.mParser
            java.lang.String r12 = r12.getValue(r8)
            r3[r0] = r12
        L_0x011a:
            if (r9 == 0) goto L_0x0124
            com.samsung.android.server.wifi.CscParser r12 = r1.mParser
            java.lang.String r12 = r12.getValue(r9)
            r5[r0] = r12
        L_0x0124:
            if (r10 == 0) goto L_0x012e
            com.samsung.android.server.wifi.CscParser r12 = r1.mParser
            java.lang.String r12 = r12.getValue(r10)
            r6[r0] = r12
        L_0x012e:
            if (r11 == 0) goto L_0x0138
            com.samsung.android.server.wifi.CscParser r12 = r1.mParser
            java.lang.String r12 = r12.getValue(r11)
            r4[r0] = r12
        L_0x0138:
            r7 = r36
            int r36 = r7 + 1
            int r0 = r0 + 1
            r13 = r18
            r14 = r26
            r7 = r37
            r8 = r38
            r9 = r39
            r10 = r40
            r11 = r41
            r12 = r42
            goto L_0x00bf
        L_0x0151:
            r37 = r7
            r38 = r8
            r39 = r9
            r40 = r10
            r41 = r11
            r42 = r12
            r18 = r13
            r26 = r14
            r7 = r36
            r8 = 0
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ NullPointerException -> 0x026b }
            r0.<init>()     // Catch:{ NullPointerException -> 0x026b }
            r0.setLength(r8)     // Catch:{ NullPointerException -> 0x026b }
            java.lang.String r9 = "getCustomerFile: get Wifi default ap information"
            r1.logd(r9)     // Catch:{ NullPointerException -> 0x026b }
            r9 = 0
        L_0x0172:
            if (r9 >= r7) goto L_0x0250
            r10 = r17
            r0.append(r10)     // Catch:{ NullPointerException -> 0x024c }
            r11 = r15[r9]     // Catch:{ NullPointerException -> 0x024c }
            if (r11 == 0) goto L_0x01a1
            java.lang.String r11 = "    ssid="
            r0.append(r11)     // Catch:{ NullPointerException -> 0x019a }
            r11 = r20
            r0.append(r11)     // Catch:{ NullPointerException -> 0x0195 }
            r12 = r15[r9]     // Catch:{ NullPointerException -> 0x0195 }
            r0.append(r12)     // Catch:{ NullPointerException -> 0x0195 }
            r0.append(r11)     // Catch:{ NullPointerException -> 0x0195 }
            r12 = r19
            r0.append(r12)     // Catch:{ NullPointerException -> 0x0248 }
            goto L_0x01a5
        L_0x0195:
            r0 = move-exception
            r12 = r19
            goto L_0x0249
        L_0x019a:
            r0 = move-exception
            r12 = r19
            r11 = r20
            goto L_0x0249
        L_0x01a1:
            r12 = r19
            r11 = r20
        L_0x01a5:
            r13 = r5[r9]     // Catch:{ NullPointerException -> 0x0248 }
            if (r13 == 0) goto L_0x01b6
            java.lang.String r13 = "    scan_ssid="
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0248 }
            r13 = r5[r9]     // Catch:{ NullPointerException -> 0x0248 }
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0248 }
            r0.append(r12)     // Catch:{ NullPointerException -> 0x0248 }
        L_0x01b6:
            r13 = r2[r9]     // Catch:{ NullPointerException -> 0x0248 }
            if (r13 == 0) goto L_0x01c7
            java.lang.String r13 = "    key_mgmt="
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0248 }
            r13 = r2[r9]     // Catch:{ NullPointerException -> 0x0248 }
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0248 }
            r0.append(r12)     // Catch:{ NullPointerException -> 0x0248 }
        L_0x01c7:
            r13 = r3[r9]     // Catch:{ NullPointerException -> 0x0248 }
            if (r13 == 0) goto L_0x01de
            java.lang.String r13 = "    psk="
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0248 }
            r0.append(r11)     // Catch:{ NullPointerException -> 0x0248 }
            r13 = r3[r9]     // Catch:{ NullPointerException -> 0x0248 }
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0248 }
            r0.append(r11)     // Catch:{ NullPointerException -> 0x0248 }
            r0.append(r12)     // Catch:{ NullPointerException -> 0x0248 }
        L_0x01de:
            r13 = r6[r9]     // Catch:{ NullPointerException -> 0x0248 }
            if (r13 == 0) goto L_0x0219
            java.lang.String r13 = "    eap="
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0248 }
            java.lang.String r13 = "sim"
            r14 = r6[r9]     // Catch:{ NullPointerException -> 0x0248 }
            boolean r13 = r13.equals(r14)     // Catch:{ NullPointerException -> 0x0248 }
            if (r13 == 0) goto L_0x01f7
            java.lang.String r13 = "SIM"
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0248 }
            goto L_0x0216
        L_0x01f7:
            java.lang.String r13 = "aka"
            r14 = r6[r9]     // Catch:{ NullPointerException -> 0x0248 }
            boolean r13 = r13.equals(r14)     // Catch:{ NullPointerException -> 0x0248 }
            if (r13 == 0) goto L_0x0207
            java.lang.String r13 = "AKA"
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0248 }
            goto L_0x0216
        L_0x0207:
            java.lang.String r13 = "akaprime"
            r14 = r6[r9]     // Catch:{ NullPointerException -> 0x0248 }
            boolean r13 = r13.equals(r14)     // Catch:{ NullPointerException -> 0x0248 }
            if (r13 == 0) goto L_0x0216
            java.lang.String r13 = "AKA'"
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0248 }
        L_0x0216:
            r0.append(r12)     // Catch:{ NullPointerException -> 0x0248 }
        L_0x0219:
            r13 = r4[r9]     // Catch:{ NullPointerException -> 0x0248 }
            if (r13 == 0) goto L_0x0233
            r13 = r16
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0231 }
            r0.append(r11)     // Catch:{ NullPointerException -> 0x0231 }
            r14 = r4[r9]     // Catch:{ NullPointerException -> 0x0231 }
            r0.append(r14)     // Catch:{ NullPointerException -> 0x0231 }
            r0.append(r11)     // Catch:{ NullPointerException -> 0x0231 }
            r0.append(r12)     // Catch:{ NullPointerException -> 0x0231 }
            goto L_0x0235
        L_0x0231:
            r0 = move-exception
            goto L_0x0274
        L_0x0233:
            r13 = r16
        L_0x0235:
            r14 = r32
            r0.append(r14)     // Catch:{ NullPointerException -> 0x0269 }
            int r9 = r9 + 1
            r17 = r10
            r20 = r11
            r19 = r12
            r16 = r13
            r32 = r14
            goto L_0x0172
        L_0x0248:
            r0 = move-exception
        L_0x0249:
            r13 = r16
            goto L_0x0274
        L_0x024c:
            r0 = move-exception
            r13 = r16
            goto L_0x0270
        L_0x0250:
            r13 = r16
            r10 = r17
            r12 = r19
            r11 = r20
            r14 = r32
            java.lang.String r9 = r0.toString()     // Catch:{ NullPointerException -> 0x0269 }
            r1.createDefaultApFile(r9)     // Catch:{ NullPointerException -> 0x0269 }
            java.lang.String r9 = r0.toString()     // Catch:{ NullPointerException -> 0x0269 }
            r1.logi(r9)     // Catch:{ NullPointerException -> 0x0269 }
            goto L_0x027b
        L_0x0269:
            r0 = move-exception
            goto L_0x0276
        L_0x026b:
            r0 = move-exception
            r13 = r16
            r10 = r17
        L_0x0270:
            r12 = r19
            r11 = r20
        L_0x0274:
            r14 = r32
        L_0x0276:
            java.lang.String r9 = "getCustomerFile: WIFI Profile -NullPointerException"
            r1.loge(r9)
        L_0x027b:
            com.samsung.android.server.wifi.CscParser r0 = r1.mParser
            r9 = r23
            org.w3c.dom.Node r8 = r0.search(r9)
            com.samsung.android.server.wifi.CscParser r0 = r1.mParser
            r17 = r2
            r19 = r3
            r2 = r24
            org.w3c.dom.NodeList r3 = r0.searchList(r8, r2)
            if (r3 == 0) goto L_0x039c
            r24 = r2
            int r2 = r3.getLength()
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r20 = r5
            java.lang.String r5 = "getCustomerFile: GeneralInfo, number : "
            r0.append(r5)
            r0.append(r2)
            java.lang.String r0 = r0.toString()
            r1.logd(r0)
            int r0 = r18 * r2
            java.lang.String[] r5 = new java.lang.String[r0]
            int r0 = r18 * r2
            r23 = r6
            java.lang.String[] r6 = new java.lang.String[r0]
            r0 = 0
            r32 = r8
            r8 = r27
        L_0x02bc:
            if (r0 >= r2) goto L_0x0330
            r29 = r2
            org.w3c.dom.Node r2 = r3.item(r0)
            r36 = r3
            com.samsung.android.server.wifi.CscParser r3 = r1.mParser
            r43 = r9
            r9 = r25
            org.w3c.dom.Node r3 = r3.search(r2, r9)
            com.samsung.android.server.wifi.CscParser r9 = r1.mParser
            r44 = r15
            r15 = r33
            org.w3c.dom.Node r9 = r9.search(r2, r15)
            r21 = 0
            r45 = r21
            r21 = r8
            r8 = r45
        L_0x02e3:
            if (r8 >= r7) goto L_0x031d
            r22 = r4[r8]
            if (r22 == 0) goto L_0x0312
            r22 = r2
            r2 = r4[r8]
            r33 = r4
            com.samsung.android.server.wifi.CscParser r4 = r1.mParser
            java.lang.String r4 = r4.getValue(r9)
            boolean r2 = r2.equals(r4)
            if (r2 == 0) goto L_0x0316
            if (r3 == 0) goto L_0x0305
            com.samsung.android.server.wifi.CscParser r2 = r1.mParser
            java.lang.String r2 = r2.getValue(r3)
            r5[r21] = r2
        L_0x0305:
            if (r9 == 0) goto L_0x030f
            com.samsung.android.server.wifi.CscParser r2 = r1.mParser
            java.lang.String r2 = r2.getValue(r9)
            r6[r21] = r2
        L_0x030f:
            int r21 = r21 + 1
            goto L_0x0316
        L_0x0312:
            r22 = r2
            r33 = r4
        L_0x0316:
            int r8 = r8 + 1
            r2 = r22
            r4 = r33
            goto L_0x02e3
        L_0x031d:
            r22 = r2
            r33 = r4
            int r0 = r0 + 1
            r8 = r21
            r2 = r29
            r3 = r36
            r9 = r43
            r33 = r15
            r15 = r44
            goto L_0x02bc
        L_0x0330:
            r29 = r2
            r36 = r3
            r43 = r9
            r44 = r15
            r15 = r33
            r33 = r4
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ NullPointerException -> 0x0391 }
            r0.<init>()     // Catch:{ NullPointerException -> 0x0391 }
            r2 = 0
            r0.setLength(r2)     // Catch:{ NullPointerException -> 0x0391 }
            java.lang.String r2 = "getCustomerFile: get GeneralInfo NetworkInfo"
            r1.logd(r2)     // Catch:{ NullPointerException -> 0x0391 }
            r2 = 0
        L_0x034b:
            if (r2 >= r8) goto L_0x0382
            r0.append(r10)     // Catch:{ NullPointerException -> 0x0391 }
            r3 = r6[r2]     // Catch:{ NullPointerException -> 0x0391 }
            if (r3 == 0) goto L_0x0365
            r0.append(r13)     // Catch:{ NullPointerException -> 0x0391 }
            r0.append(r11)     // Catch:{ NullPointerException -> 0x0391 }
            r3 = r6[r2]     // Catch:{ NullPointerException -> 0x0391 }
            r0.append(r3)     // Catch:{ NullPointerException -> 0x0391 }
            r0.append(r11)     // Catch:{ NullPointerException -> 0x0391 }
            r0.append(r12)     // Catch:{ NullPointerException -> 0x0391 }
        L_0x0365:
            r3 = r5[r2]     // Catch:{ NullPointerException -> 0x0391 }
            if (r3 == 0) goto L_0x037c
            java.lang.String r3 = "    mccmnc="
            r0.append(r3)     // Catch:{ NullPointerException -> 0x0391 }
            r0.append(r11)     // Catch:{ NullPointerException -> 0x0391 }
            r3 = r5[r2]     // Catch:{ NullPointerException -> 0x0391 }
            r0.append(r3)     // Catch:{ NullPointerException -> 0x0391 }
            r0.append(r11)     // Catch:{ NullPointerException -> 0x0391 }
            r0.append(r12)     // Catch:{ NullPointerException -> 0x0391 }
        L_0x037c:
            r0.append(r14)     // Catch:{ NullPointerException -> 0x0391 }
            int r2 = r2 + 1
            goto L_0x034b
        L_0x0382:
            java.lang.String r2 = r0.toString()     // Catch:{ NullPointerException -> 0x0391 }
            r1.createGeneralNetworkFile(r2)     // Catch:{ NullPointerException -> 0x0391 }
            java.lang.String r2 = r0.toString()     // Catch:{ NullPointerException -> 0x0391 }
            r1.logi(r2)     // Catch:{ NullPointerException -> 0x0391 }
            goto L_0x0397
        L_0x0391:
            r0 = move-exception
            java.lang.String r2 = "getCustomerFile: GeneralInfo -NullPointerException"
            r1.loge(r2)
        L_0x0397:
            r21 = r5
            r22 = r6
            goto L_0x03b0
        L_0x039c:
            r24 = r2
            r36 = r3
            r20 = r5
            r23 = r6
            r32 = r8
            r43 = r9
            r44 = r15
            r15 = r33
            r33 = r4
            r8 = r27
        L_0x03b0:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.WifiParsingCustomerFile.getCustomerFile():void");
    }

    private void createDefaultApFile(String wifiDefaultApProfile) {
        if (wifiDefaultApProfile == null) {
            loge("createDefaultApFile: createDefaultApFile is null");
            return;
        }
        if (mFilePathDefaultAp.exists()) {
            logd("createDefaultApFile: delete default_ap.conf file");
            mFilePathDefaultAp.delete();
        }
        if (mFilePathGeneralNwInfo.exists()) {
            logd("createDefaultApFile: delete generalinfo_nw.conf file");
            mFilePathGeneralNwInfo.delete();
        }
        if (wifiDefaultApProfile.length() == 0) {
            logi("createDefaultApFile: WifiProfile is empty");
            return;
        }
        FileOutputStream fw = null;
        try {
            mFilePathDefaultAp.createNewFile();
            fw = new FileOutputStream(mFilePathDefaultAp, true);
            fw.write(wifiDefaultApProfile.getBytes());
            try {
                fw.close();
            } catch (IOException e2) {
                loge(e2.toString());
            }
        } catch (FileNotFoundException e) {
            loge("WiFi Profile File Create Not Found ");
            if (fw != null) {
                fw.close();
            }
        } catch (IOException e3) {
            e3.printStackTrace();
            if (fw != null) {
                fw.close();
            }
        } catch (Throwable th) {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e22) {
                    loge(e22.toString());
                }
            }
            throw th;
        }
    }

    private void createGeneralNetworkFile(String generalInfoNw) {
        logd("String Matched General Info List \n" + generalInfoNw);
        if (mFilePathGeneralNwInfo.exists()) {
            logd("GeneralInfo file delete is called");
            mFilePathGeneralNwInfo.delete();
        }
        if (generalInfoNw == null) {
            loge("createGeneralNetworkFile: generalInfoNw is null");
        } else if (generalInfoNw.length() == 0) {
            logi("Settings.Secure.WIFI_GENERALINFO_NWINFO is empty");
        } else {
            FileOutputStream generalFW = null;
            try {
                mFilePathGeneralNwInfo.createNewFile();
                generalFW = new FileOutputStream(mFilePathGeneralNwInfo, true);
                generalFW.write(generalInfoNw.getBytes());
                try {
                    generalFW.close();
                } catch (IOException e2) {
                    loge(e2.toString());
                }
            } catch (FileNotFoundException e) {
                loge("GeneralNwInfo File Create Not Found ");
                if (generalFW != null) {
                    generalFW.close();
                }
            } catch (IOException e3) {
                e3.printStackTrace();
                if (generalFW != null) {
                    generalFW.close();
                }
            } catch (Throwable th) {
                if (generalFW != null) {
                    try {
                        generalFW.close();
                    } catch (IOException e22) {
                        loge(e22.toString());
                    }
                }
                throw th;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void loge(String s) {
        Log.e(TAG, s);
        this.mLocalLog.log(s);
    }

    /* access modifiers changed from: protected */
    public void logd(String s) {
        Log.d(TAG, s);
        this.mLocalLog.log(s);
    }

    /* access modifiers changed from: protected */
    public void logi(String s) {
        Log.i(TAG, s);
        this.mLocalLog.log(s);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("==== Customer File Dump ====");
        this.mLocalLog.dump(fd, pw, args);
    }
}
