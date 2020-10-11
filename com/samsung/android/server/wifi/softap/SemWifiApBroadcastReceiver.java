package com.samsung.android.server.wifi.softap;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.WifiInjector;
import com.samsung.android.feature.SemCscFeature;
import com.sec.android.app.CscFeatureTagSetting;
import com.sec.android.app.CscFeatureTagWifi;

public class SemWifiApBroadcastReceiver {
    public static final String ADVANCED_WIFI_SHARING_NOTI = "com.samsung.intent.action.ADVANCED_WIFI_SHARING_NOTIFICATION";
    public static final String AP_STA_24GHZ_DISCONNECTED = "com.samsung.actoin.24GHZ_AP_STA_DISCONNECTED";
    /* access modifiers changed from: private */
    public static final String CONFIGOPBRANDINGFORMOBILEAP = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDINGFORMOBILEAP, "ALL");
    private static final int DATA_REACH = 17042599;
    public static final int DIALOG_HOTSPOT_24GHZ_AP_STA_DISCONNECT = 51;
    public static final int DIALOG_HOTSPOT_NO_DATA = 1;
    public static final int DIALOG_HOTSPOT_PROVISIONING_REQUEST = 6;
    public static final int DIALOG_NAI_MISMATCH = 2;
    public static final int DIALOG_TETHERING_DENIED = 3;
    public static final int DIALOG_WIFI_AP_ENABLE_WARNING = 5;
    public static final int DIALOG_WIFI_ENABLE_WARNING = 4;
    public static final int DIALOG_WIFI_P2P_ENABLE_WARNING = 50;
    static final String INTENT_KEY_ICC_STATE = "ss";
    static final String INTENT_VALUE_ICC_IMSI = "IMSI";
    public static final String START_PROVISIONING = "com.samsung.intent.action.START_PROVISIONING";
    private static final String TAG = "SemWifiApBroadcastReceiver";
    public static final String WIFIAP_MODEMNAI_MISSMATH = "com.samsung.intent.action.MIP_ERROR";
    public static final String WIFIAP_PROVISION_DIALOG_TYPE = "wifiap_provision_dialog_type";
    public static final String WIFIAP_TETHERING_DENIED = "com.samsung.android.intent.action.TETHERING_DENIED";
    public static final String WIFIAP_TETHERING_FAILED = "com.samsung.android.intent.action.TETHERING_FAILED";
    public static final String WIFIAP_WARNING_DIALOG = "com.samsung.android.settings.wifi.mobileap.wifiapwarning";
    public static final String WIFIAP_WARNING_DIALOG_TYPE = "wifiap_warning_dialog_type";
    static String currentMccMnc = "";
    private static boolean isRegistered = false;
    /* access modifiers changed from: private */
    public static long mBaseTxBytes = 0;
    /* access modifiers changed from: private */
    public static final String mHotspotActionForSimStatus = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGHOTSPOTACTIONFORSIMSTATUS);
    /* access modifiers changed from: private */
    public boolean bIsFirstCall = false;
    /* access modifiers changed from: private */
    public boolean bUseMobileData = false;
    /* access modifiers changed from: private */
    public long mAmountMobileRxBytes;
    /* access modifiers changed from: private */
    public long mAmountMobileTxBytes;
    /* access modifiers changed from: private */
    public long mAmountTimeOfMobileData;
    /* access modifiers changed from: private */
    public long mBaseRxBytes;
    /* access modifiers changed from: private */
    public Context mContext = null;
    private final IntentFilter mFilter;
    private NotificationManager mNotificationManager = null;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* JADX WARNING: Removed duplicated region for block: B:100:0x037f  */
        /* JADX WARNING: Removed duplicated region for block: B:74:0x02af  */
        /* JADX WARNING: Removed duplicated region for block: B:75:0x02b2  */
        /* JADX WARNING: Removed duplicated region for block: B:78:0x02c2  */
        /* JADX WARNING: Removed duplicated region for block: B:79:0x02c5  */
        /* JADX WARNING: Removed duplicated region for block: B:84:0x02e0  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(android.content.Context r37, android.content.Intent r38) {
            /*
                r36 = this;
                r1 = r36
                r2 = r37
                r3 = r38
                java.lang.String r4 = r38.getAction()
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r5 = "Received : "
                r0.append(r5)
                r0.append(r4)
                java.lang.String r0 = r0.toString()
                java.lang.String r5 = "SemWifiApBroadcastReceiver"
                android.util.Log.d(r5, r0)
                java.lang.String r0 = "com.samsung.intent.action.MIP_ERROR"
                boolean r6 = r4.equals(r0)
                r8 = 13
                java.lang.String r9 = "com.samsung.android.intent.action.TETHERING_DENIED"
                r10 = 0
                java.lang.String r11 = "wifi"
                r12 = 1
                r13 = 0
                if (r6 != 0) goto L_0x065a
                boolean r6 = r4.equals(r9)
                if (r6 != 0) goto L_0x065a
                java.lang.String r6 = "com.samsung.android.intent.action.TETHERING_FAILED"
                boolean r6 = r4.equals(r6)
                if (r6 == 0) goto L_0x004b
                r34 = r5
                r5 = r2
                r2 = r34
                r35 = r4
                r4 = r3
                r3 = r35
                goto L_0x0664
            L_0x004b:
                java.lang.String r0 = "com.samsung.actoin.24GHZ_AP_STA_DISCONNECTED"
                boolean r0 = r4.equals(r0)
                if (r0 == 0) goto L_0x0067
                java.lang.String r0 = "Sending the dialog type51"
                android.util.Log.d(r5, r0)
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                r5 = 51
                r0.showHotspotErrorDialog(r2, r5, r3)
                r5 = r2
                r34 = r4
                r4 = r3
                r3 = r34
                goto L_0x0789
            L_0x0067:
                java.lang.String r0 = "com.nttdocomo.intent.action.SHOW_WPSDIALOG"
                boolean r0 = r4.equals(r0)
                if (r0 == 0) goto L_0x007c
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                r0.startWifiApSettings(r2)
                r5 = r2
                r34 = r4
                r4 = r3
                r3 = r34
                goto L_0x0789
            L_0x007c:
                java.lang.String r0 = "com.samsung.intent.action.START_PROVISIONING"
                boolean r0 = r4.equals(r0)
                if (r0 == 0) goto L_0x00e2
                java.lang.Object r0 = r2.getSystemService(r11)
                android.net.wifi.WifiManager r0 = (android.net.wifi.WifiManager) r0
                int r6 = r0.getWifiApState()
                if (r6 != r8) goto L_0x00da
                int r6 = r0.getProvisionSuccess()
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r7 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                boolean r7 = r7.isProvisioningNeeded(r2)
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                java.lang.String r9 = "check Start provisioning as wifi disconnected getProvisionSuccess "
                r8.append(r9)
                r8.append(r6)
                java.lang.String r9 = "isProvisioningNeeded "
                r8.append(r9)
                r8.append(r7)
                java.lang.String r9 = "wifisharing "
                r8.append(r9)
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r9 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                boolean r9 = r9.isWifiSharingEnabled(r2)
                r8.append(r9)
                java.lang.String r8 = r8.toString()
                android.util.Log.d(r5, r8)
                if (r7 == 0) goto L_0x00da
                if (r6 == r12) goto L_0x00da
                java.lang.String r5 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.CONFIGOPBRANDINGFORMOBILEAP
                java.lang.String r8 = "VZW"
                boolean r5 = r8.equals(r5)
                if (r5 != 0) goto L_0x00da
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r5 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                r8 = 6
                r5.startHotspotProvisioningRequestWifiSharing(r2, r8)
            L_0x00da:
                r5 = r2
                r34 = r4
                r4 = r3
                r3 = r34
                goto L_0x0789
            L_0x00e2:
                java.lang.String r0 = "com.samsung.intent.action.ADVANCED_WIFI_SHARING_NOTIFICATION"
                boolean r0 = r0.equals(r4)
                if (r0 == 0) goto L_0x0112
                java.lang.String r0 = "NOTIFICATION_TASK"
                int r0 = r3.getIntExtra(r0, r13)
                if (r0 != 0) goto L_0x00f8
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r5 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                r5.clearWifiScanListNotification(r2)
                goto L_0x010a
            L_0x00f8:
                if (r0 != r12) goto L_0x010a
                android.content.res.Resources r5 = r37.getResources()
                r6 = 17039545(0x10400b9, float:2.424509E-38)
                java.lang.String r5 = r5.getString(r6)
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r6 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                r6.showWifiScanListNotification(r2, r5)
            L_0x010a:
                r5 = r2
                r34 = r4
                r4 = r3
                r3 = r34
                goto L_0x0789
            L_0x0112:
                java.lang.String r0 = "android.net.wifi.WIFI_AP_STATE_CHANGED"
                boolean r0 = r0.equals(r4)
                java.lang.String r6 = "wifi_ap_max_client_number"
                if (r0 == 0) goto L_0x0587
                r0 = 14
                java.lang.String r8 = "wifi_state"
                int r8 = r3.getIntExtra(r8, r0)
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r9 = "onreceive WIFI_AP_STATE_CHANGED_ACTION] apState : "
                r0.append(r9)
                r0.append(r8)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r5, r0)
                com.android.server.wifi.WifiInjector r0 = com.android.server.wifi.WifiInjector.getInstance()
                com.samsung.android.server.wifi.softap.SemWifiApChipInfo r0 = r0.getSemWifiApChipInfo()
                boolean r0 = r0.supportWifiSharing()
                if (r0 == 0) goto L_0x014a
                java.lang.String r0 = "swlan0"
                r9 = r0
                goto L_0x014d
            L_0x014a:
                java.lang.String r0 = "wlan0"
                r9 = r0
            L_0x014d:
                r14 = 0
                java.lang.String r0 = " "
                switch(r8) {
                    case 10: goto L_0x038f;
                    case 11: goto L_0x01fe;
                    case 12: goto L_0x0154;
                    case 13: goto L_0x017c;
                    case 14: goto L_0x0173;
                    default: goto L_0x0154;
                }
            L_0x0154:
                r27 = r4
                r2 = r5
                r16 = r8
                r26 = r9
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r3 = "unhandled apState : "
                r0.append(r3)
                r3 = r16
                r0.append(r3)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r2, r0)
                goto L_0x057f
            L_0x0173:
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                r0.setRvfMode(r2, r13)
                r27 = r4
                goto L_0x057f
            L_0x017c:
                long r6 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.mBaseTxBytes     // Catch:{ Exception -> 0x01e5 }
                int r0 = (r6 > r14 ? 1 : (r6 == r14 ? 0 : -1))
                if (r0 != 0) goto L_0x01e1
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x01e5 }
                r0.resetParameterForHotspotLogging()     // Catch:{ Exception -> 0x01e5 }
                long r6 = java.lang.System.currentTimeMillis()     // Catch:{ Exception -> 0x01e5 }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x01e5 }
                long r10 = java.lang.System.currentTimeMillis()     // Catch:{ Exception -> 0x01e5 }
                long unused = r0.mTimeOfStartMobileAp = r10     // Catch:{ Exception -> 0x01e5 }
                long r10 = android.net.TrafficStats.getTxBytes(r9)     // Catch:{ Exception -> 0x01e5 }
                long unused = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.mBaseTxBytes = r10     // Catch:{ Exception -> 0x01e5 }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x01e5 }
                long r10 = android.net.TrafficStats.getRxBytes(r9)     // Catch:{ Exception -> 0x01e5 }
                long unused = r0.mBaseRxBytes = r10     // Catch:{ Exception -> 0x01e5 }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x01e5 }
                android.telephony.TelephonyManager r0 = r0.mTelephonyManagerForHotspot     // Catch:{ Exception -> 0x01e5 }
                if (r0 != 0) goto L_0x01c6
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x01e5 }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r10 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x01e5 }
                android.content.Context r10 = r10.mContext     // Catch:{ Exception -> 0x01e5 }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r11 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x01e5 }
                android.content.Context unused = r11.mContext     // Catch:{ Exception -> 0x01e5 }
                java.lang.String r11 = "phone"
                java.lang.Object r10 = r10.getSystemService(r11)     // Catch:{ Exception -> 0x01e5 }
                android.telephony.TelephonyManager r10 = (android.telephony.TelephonyManager) r10     // Catch:{ Exception -> 0x01e5 }
                android.telephony.TelephonyManager unused = r0.mTelephonyManagerForHotspot = r10     // Catch:{ Exception -> 0x01e5 }
            L_0x01c6:
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x01e5 }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver$1$1 r10 = new com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver$1$1     // Catch:{ Exception -> 0x01e5 }
                r10.<init>()     // Catch:{ Exception -> 0x01e5 }
                android.telephony.PhoneStateListener unused = r0.mTelephonyPhoneStateListener = r10     // Catch:{ Exception -> 0x01e5 }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x01e5 }
                android.telephony.TelephonyManager r0 = r0.mTelephonyManagerForHotspot     // Catch:{ Exception -> 0x01e5 }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r10 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x01e5 }
                android.telephony.PhoneStateListener r10 = r10.mTelephonyPhoneStateListener     // Catch:{ Exception -> 0x01e5 }
                r11 = 64
                r0.listen(r10, r11)     // Catch:{ Exception -> 0x01e5 }
            L_0x01e1:
                r27 = r4
                goto L_0x057f
            L_0x01e5:
                r0 = move-exception
                java.lang.StringBuilder r6 = new java.lang.StringBuilder
                r6.<init>()
                java.lang.String r7 = "Error in getting wlan0 interface config:"
                r6.append(r7)
                r6.append(r0)
                java.lang.String r6 = r6.toString()
                android.util.Log.e(r5, r6)
                r27 = r4
                goto L_0x057f
            L_0x01fe:
                java.lang.Object r10 = r2.getSystemService(r11)
                android.net.wifi.WifiManager r10 = (android.net.wifi.WifiManager) r10
                if (r10 == 0) goto L_0x0384
                java.lang.String r11 = "not_support"
                java.lang.String r14 = "not_support"
                java.lang.String r15 = "not_support"
                com.android.server.wifi.WifiInjector r16 = com.android.server.wifi.WifiInjector.getInstance()
                com.samsung.android.server.wifi.softap.SemWifiApChipInfo r16 = r16.getSemWifiApChipInfo()
                boolean r16 = r16.supportWifiSharing()
                if (r16 == 0) goto L_0x021f
                java.lang.String r16 = "swlan0"
                r17 = r16
                goto L_0x0223
            L_0x021f:
                java.lang.String r16 = "wlan0"
                r17 = r16
            L_0x0223:
                android.content.ContentResolver r7 = r37.getContentResolver()
                com.samsung.android.feature.SemCscFeature r12 = com.samsung.android.feature.SemCscFeature.getInstance()
                r13 = 1200(0x4b0, float:1.682E-42)
                r20 = r11
                java.lang.String r11 = "CscFeature_Wifi_ConfigMobileApDefaultTimeOut"
                int r11 = r12.getInteger(r11, r13)
                int r11 = r11 / 60
                java.lang.String r12 = "wifi_ap_timeout_setting"
                int r7 = android.provider.Settings.Secure.getInt(r7, r12, r11)
                android.content.ContentResolver r11 = r37.getContentResolver()
                com.android.server.wifi.WifiInjector r12 = com.android.server.wifi.WifiInjector.getInstance()
                com.samsung.android.server.wifi.softap.SemWifiApChipInfo r12 = r12.getSemWifiApChipInfo()
                boolean r12 = r12.supportWifiSharing()
                java.lang.String r13 = "wifi_ap_wifi_sharing"
                if (r12 == 0) goto L_0x027b
                r12 = 10
                r21 = r14
                int r14 = android.provider.Settings.Secure.getInt(r11, r13, r12)
                if (r14 != r12) goto L_0x0278
                java.lang.StringBuilder r14 = new java.lang.StringBuilder
                r14.<init>()
                r22 = r15
                java.lang.String r15 = "Wifi Sharing first time provider value "
                r14.append(r15)
                int r12 = android.provider.Settings.Secure.getInt(r11, r13, r12)
                r14.append(r12)
                java.lang.String r12 = r14.toString()
                android.util.Log.d(r5, r12)
                java.lang.String r5 = "-1"
                goto L_0x02a1
            L_0x0278:
                r22 = r15
                goto L_0x027f
            L_0x027b:
                r21 = r14
                r22 = r15
            L_0x027f:
                com.android.server.wifi.WifiInjector r5 = com.android.server.wifi.WifiInjector.getInstance()
                com.samsung.android.server.wifi.softap.SemWifiApChipInfo r5 = r5.getSemWifiApChipInfo()
                boolean r5 = r5.supportWifiSharing()
                if (r5 == 0) goto L_0x029f
                android.content.ContentResolver r5 = r37.getContentResolver()
                r12 = 0
                int r5 = android.provider.Settings.Secure.getInt(r5, r13, r12)
                r12 = 1
                if (r5 != r12) goto L_0x029c
                java.lang.String r5 = "sharing_on"
                goto L_0x029e
            L_0x029c:
                java.lang.String r5 = "sharing_off"
            L_0x029e:
                goto L_0x02a1
            L_0x029f:
                r5 = r20
            L_0x02a1:
                android.content.ContentResolver r12 = r37.getContentResolver()
                java.lang.String r13 = "wifi_ap_pmf_checked"
                r14 = 0
                int r12 = android.provider.Settings.Secure.getInt(r12, r13, r14)
                r13 = 1
                if (r12 != r13) goto L_0x02b2
                java.lang.String r12 = "pmf_on"
                goto L_0x02b4
            L_0x02b2:
                java.lang.String r12 = "pmf_off"
            L_0x02b4:
                android.content.ContentResolver r13 = r37.getContentResolver()
                java.lang.String r14 = "wifi_ap_powersave_mode_checked"
                r15 = 0
                int r13 = android.provider.Settings.Secure.getInt(r13, r14, r15)
                r14 = 1
                if (r13 != r14) goto L_0x02c5
                java.lang.String r13 = "power_save_mode_on"
                goto L_0x02c7
            L_0x02c5:
                java.lang.String r13 = "power_save_mode_off"
            L_0x02c7:
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r14 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                android.content.Context r14 = r14.mContext
                android.content.ContentResolver r14 = r14.getContentResolver()
                r15 = 0
                int r6 = android.provider.Settings.Secure.getInt(r14, r6, r15)
                android.net.wifi.WifiConfiguration r14 = r10.getWifiApConfiguration()
                if (r14 == 0) goto L_0x037f
                java.lang.String r15 = r14.SSID
                if (r15 == 0) goto L_0x037f
                java.lang.String r15 = "CustomSSID"
                java.lang.String r18 = "All"
                r19 = r10
                int r10 = r14.macaddrAcl
                r20 = r11
                r11 = 3
                if (r10 == r11) goto L_0x02ef
                java.lang.String r18 = "Only"
            L_0x02ef:
                r10 = r18
                java.lang.String r11 = r14.SSID
                r16 = r15
                java.lang.String r15 = "Android"
                boolean r11 = r11.startsWith(r15)
                if (r11 != 0) goto L_0x0329
                java.lang.String r11 = r14.SSID
                java.lang.String r15 = "Verizon"
                boolean r11 = r11.startsWith(r15)
                if (r11 != 0) goto L_0x0329
                java.lang.String r11 = r14.SSID
                java.lang.String r15 = "Samsung"
                boolean r11 = r11.startsWith(r15)
                if (r11 != 0) goto L_0x0329
                java.lang.String r11 = r14.SSID
                java.lang.String r15 = "Galaxy"
                boolean r11 = r11.startsWith(r15)
                if (r11 != 0) goto L_0x0329
                java.lang.String r11 = r14.SSID
                java.lang.String r15 = "SM-"
                boolean r11 = r11.startsWith(r15)
                if (r11 == 0) goto L_0x0326
                goto L_0x0329
            L_0x0326:
                r15 = r16
                goto L_0x032b
            L_0x0329:
                java.lang.String r15 = "DefaultSSID"
            L_0x032b:
                java.lang.String r11 = ""
                r16 = r11
                java.lang.StringBuilder r11 = new java.lang.StringBuilder
                r11.<init>()
                r2 = r17
                r11.append(r2)
                r11.append(r0)
                r11.append(r15)
                r11.append(r0)
                boolean r2 = r14.hiddenSSID
                r11.append(r2)
                r11.append(r0)
                int r2 = r14.apChannel
                r11.append(r2)
                r11.append(r0)
                r11.append(r10)
                r11.append(r0)
                r11.append(r6)
                r11.append(r0)
                r11.append(r5)
                r11.append(r0)
                r11.append(r7)
                r11.append(r0)
                r11.append(r12)
                r11.append(r0)
                r11.append(r13)
                java.lang.String r0 = r11.toString()
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                java.lang.String r11 = "MHSI"
                r2.callSecBigdataApi(r11, r0)
                goto L_0x0386
            L_0x037f:
                r19 = r10
                r20 = r11
                goto L_0x0386
            L_0x0384:
                r19 = r10
            L_0x0386:
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                r0.callInitBigdataVariables()
                r27 = r4
                goto L_0x057f
            L_0x038f:
                java.lang.String r2 = ""
                long r6 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.mBaseTxBytes     // Catch:{ Exception -> 0x0560 }
                int r6 = (r6 > r14 ? 1 : (r6 == r14 ? 0 : -1))
                if (r6 == 0) goto L_0x0547
                long r6 = java.lang.System.currentTimeMillis()     // Catch:{ Exception -> 0x053c }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r10 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x053c }
                long r10 = r10.mTimeOfStartMobileAp     // Catch:{ Exception -> 0x053c }
                long r10 = r6 - r10
                r12 = 60000(0xea60, double:2.9644E-319)
                long r10 = r10 / r12
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r12 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x053c }
                boolean r12 = r12.bUseMobileData     // Catch:{ Exception -> 0x053c }
                if (r12 == 0) goto L_0x0413
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r12 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0407 }
                long r12 = r12.mStartTimeOfMobileData     // Catch:{ Exception -> 0x0407 }
                long r12 = r6 - r12
                r16 = 60000(0xea60, double:2.9644E-319)
                long r12 = r12 / r16
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r14 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0407 }
                long r14 = r14.mAmountTimeOfMobileData     // Catch:{ Exception -> 0x0407 }
                long r14 = r14 + r12
                long r20 = android.net.TrafficStats.getTxBytes(r9)     // Catch:{ Exception -> 0x0407 }
                long r22 = android.net.TrafficStats.getRxBytes(r9)     // Catch:{ Exception -> 0x0407 }
                r18 = r2
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x03fd }
                long r24 = r2.mAmountMobileTxBytes     // Catch:{ Exception -> 0x03fd }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x03fd }
                long r26 = r2.mAmountMobileRxBytes     // Catch:{ Exception -> 0x03fd }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x03fd }
                r28 = r6
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r6 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x03fd }
                long r6 = r6.mTempMobileTxBytes     // Catch:{ Exception -> 0x03fd }
                long r6 = r20 - r6
                long r6 = r24 + r6
                long unused = r2.mAmountMobileTxBytes = r6     // Catch:{ Exception -> 0x03fd }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x03fd }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r6 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x03fd }
                long r6 = r6.mTempMobileRxBytes     // Catch:{ Exception -> 0x03fd }
                long r6 = r22 - r6
                long r6 = r26 + r6
                long unused = r2.mAmountMobileRxBytes = r6     // Catch:{ Exception -> 0x03fd }
                goto L_0x041e
            L_0x03fd:
                r0 = move-exception
                r27 = r4
                r2 = r5
                r16 = r8
                r26 = r9
                goto L_0x056a
            L_0x0407:
                r0 = move-exception
                r18 = r2
                r27 = r4
                r2 = r5
                r16 = r8
                r26 = r9
                goto L_0x056a
            L_0x0413:
                r18 = r2
                r28 = r6
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0533 }
                long r6 = r2.mAmountTimeOfMobileData     // Catch:{ Exception -> 0x0533 }
                r14 = r6
            L_0x041e:
                long r6 = android.net.TrafficStats.getTxBytes(r9)     // Catch:{ Exception -> 0x0533 }
                long r12 = android.net.TrafficStats.getRxBytes(r9)     // Catch:{ Exception -> 0x0533 }
                long r20 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.mBaseTxBytes     // Catch:{ Exception -> 0x0533 }
                long r20 = r6 - r20
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0533 }
                long r22 = r2.mBaseRxBytes     // Catch:{ Exception -> 0x0533 }
                long r22 = r12 - r22
                r24 = r6
                long r6 = r20 + r22
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0533 }
                long r26 = r2.mAmountMobileTxBytes     // Catch:{ Exception -> 0x0533 }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0533 }
                long r30 = r2.mAmountMobileRxBytes     // Catch:{ Exception -> 0x0533 }
                long r26 = r26 + r30
                r30 = r12
                long r12 = r6 - r26
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0533 }
                java.lang.String r2 = r2.convertBytesToMegaByte(r6)     // Catch:{ Exception -> 0x0533 }
                r26 = r9
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r9 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x052c }
                java.lang.String r9 = r9.convertBytesToMegaByte(r12)     // Catch:{ Exception -> 0x052c }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r3 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x052c }
                java.lang.String r3 = r3.convertMinute(r10)     // Catch:{ Exception -> 0x052c }
                r27 = r4
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r4 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0527 }
                java.lang.String r4 = r4.convertMinute(r14)     // Catch:{ Exception -> 0x0527 }
                long r32 = r10 - r14
                r16 = 0
                int r16 = (r32 > r16 ? 1 : (r32 == r16 ? 0 : -1))
                if (r16 < 0) goto L_0x04f0
                r16 = r8
                java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x04ec }
                r8.<init>()     // Catch:{ Exception -> 0x04ec }
                r8.append(r3)     // Catch:{ Exception -> 0x04ec }
                r8.append(r0)     // Catch:{ Exception -> 0x04ec }
                r8.append(r4)     // Catch:{ Exception -> 0x04ec }
                r8.append(r0)     // Catch:{ Exception -> 0x04ec }
                r17 = r3
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r3 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x04ec }
                r33 = r4
                r32 = r5
                long r4 = r10 - r14
                java.lang.String r3 = r3.convertMinute(r4)     // Catch:{ Exception -> 0x055c }
                r8.append(r3)     // Catch:{ Exception -> 0x055c }
                r8.append(r0)     // Catch:{ Exception -> 0x055c }
                r8.append(r10)     // Catch:{ Exception -> 0x055c }
                r8.append(r0)     // Catch:{ Exception -> 0x055c }
                r8.append(r14)     // Catch:{ Exception -> 0x055c }
                r8.append(r0)     // Catch:{ Exception -> 0x055c }
                long r3 = r10 - r14
                r8.append(r3)     // Catch:{ Exception -> 0x055c }
                r8.append(r0)     // Catch:{ Exception -> 0x055c }
                r8.append(r2)     // Catch:{ Exception -> 0x055c }
                r8.append(r0)     // Catch:{ Exception -> 0x055c }
                r8.append(r9)     // Catch:{ Exception -> 0x055c }
                r8.append(r0)     // Catch:{ Exception -> 0x055c }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r3 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x055c }
                long r4 = r6 - r12
                java.lang.String r3 = r3.convertBytesToMegaByte(r4)     // Catch:{ Exception -> 0x055c }
                r8.append(r3)     // Catch:{ Exception -> 0x055c }
                r8.append(r0)     // Catch:{ Exception -> 0x055c }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r3 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x055c }
                java.lang.String r3 = r3.convertBytesToMegaByteForLogging(r6)     // Catch:{ Exception -> 0x055c }
                r8.append(r3)     // Catch:{ Exception -> 0x055c }
                r8.append(r0)     // Catch:{ Exception -> 0x055c }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r3 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x055c }
                java.lang.String r3 = r3.convertBytesToMegaByteForLogging(r12)     // Catch:{ Exception -> 0x055c }
                r8.append(r3)     // Catch:{ Exception -> 0x055c }
                r8.append(r0)     // Catch:{ Exception -> 0x055c }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x055c }
                long r3 = r6 - r12
                java.lang.String r0 = r0.convertBytesToMegaByteForLogging(r3)     // Catch:{ Exception -> 0x055c }
                r8.append(r0)     // Catch:{ Exception -> 0x055c }
                java.lang.String r0 = r8.toString()     // Catch:{ Exception -> 0x055c }
                r3 = r0
                goto L_0x04fa
            L_0x04ec:
                r0 = move-exception
                r2 = r5
                goto L_0x056a
            L_0x04f0:
                r17 = r3
                r33 = r4
                r32 = r5
                r16 = r8
                r0 = 0
                r3 = r0
            L_0x04fa:
                if (r3 == 0) goto L_0x0503
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0521 }
                java.lang.String r4 = "MHSS"
                r0.callSecBigdataApi(r4, r3)     // Catch:{ Exception -> 0x0521 }
            L_0x0503:
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0521 }
                android.telephony.PhoneStateListener r0 = r0.mTelephonyPhoneStateListener     // Catch:{ Exception -> 0x0521 }
                if (r0 == 0) goto L_0x051b
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0521 }
                android.telephony.TelephonyManager r0 = r0.mTelephonyManagerForHotspot     // Catch:{ Exception -> 0x0521 }
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r4 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0521 }
                android.telephony.PhoneStateListener r4 = r4.mTelephonyPhoneStateListener     // Catch:{ Exception -> 0x0521 }
                r5 = 0
                r0.listen(r4, r5)     // Catch:{ Exception -> 0x0521 }
            L_0x051b:
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this     // Catch:{ Exception -> 0x0521 }
                r0.resetParameterForHotspotLogging()     // Catch:{ Exception -> 0x0521 }
                goto L_0x0559
            L_0x0521:
                r0 = move-exception
                r18 = r3
                r2 = r32
                goto L_0x056a
            L_0x0527:
                r0 = move-exception
                r16 = r8
                r2 = r5
                goto L_0x056a
            L_0x052c:
                r0 = move-exception
                r27 = r4
                r16 = r8
                r2 = r5
                goto L_0x056a
            L_0x0533:
                r0 = move-exception
                r27 = r4
                r16 = r8
                r26 = r9
                r2 = r5
                goto L_0x056a
            L_0x053c:
                r0 = move-exception
                r18 = r2
                r27 = r4
                r16 = r8
                r26 = r9
                r2 = r5
                goto L_0x056a
            L_0x0547:
                r18 = r2
                r27 = r4
                r32 = r5
                r16 = r8
                r26 = r9
                java.lang.String r0 = "unnormal status of interface"
                r2 = r32
                android.util.Log.d(r2, r0)     // Catch:{ Exception -> 0x055a }
                r3 = 0
            L_0x0559:
                goto L_0x057f
            L_0x055a:
                r0 = move-exception
                goto L_0x056a
            L_0x055c:
                r0 = move-exception
                r2 = r32
                goto L_0x056a
            L_0x0560:
                r0 = move-exception
                r18 = r2
                r27 = r4
                r2 = r5
                r16 = r8
                r26 = r9
            L_0x056a:
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "Error in getting wlan0 interface config:"
                r3.append(r4)
                r3.append(r0)
                java.lang.String r3 = r3.toString()
                android.util.Log.e(r2, r3)
            L_0x057f:
                r5 = r37
                r4 = r38
                r3 = r27
                goto L_0x0789
            L_0x0587:
                r27 = r4
                r2 = r5
                java.lang.String r0 = "com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED"
                r3 = r27
                boolean r0 = r0.equals(r3)
                if (r0 == 0) goto L_0x05d5
                java.lang.String r0 = "NUM"
                r4 = r38
                r5 = 0
                int r0 = r4.getIntExtra(r0, r5)
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r7 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                android.content.Context r7 = r7.mContext
                android.content.ContentResolver r7 = r7.getContentResolver()
                int r5 = android.provider.Settings.Secure.getInt(r7, r6, r5)
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                java.lang.String r8 = "ClientNum from WIFI_AP_STA_STATUS_CHANGED_ACTION = "
                r7.append(r8)
                r7.append(r0)
                java.lang.String r7 = r7.toString()
                android.util.Log.i(r2, r7)
                if (r0 >= 0) goto L_0x05c2
                r0 = 0
            L_0x05c2:
                if (r0 <= r5) goto L_0x05d1
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                android.content.Context r2 = r2.mContext
                android.content.ContentResolver r2 = r2.getContentResolver()
                android.provider.Settings.Secure.putInt(r2, r6, r0)
            L_0x05d1:
                r5 = r37
                goto L_0x0789
            L_0x05d5:
                r4 = r38
                java.lang.String r0 = "android.intent.action.SIM_STATE_CHANGED"
                boolean r0 = r3.equals(r0)
                if (r0 == 0) goto L_0x0656
                java.lang.String r0 = "SimCheck.disable"
                java.lang.String r0 = android.os.SystemProperties.get(r0)
                java.lang.String r5 = "1"
                boolean r0 = r0.equals(r5)
                if (r0 == 0) goto L_0x05ee
                return
            L_0x05ee:
                r5 = r37
                java.lang.Object r0 = r5.getSystemService(r11)
                android.net.wifi.WifiManager r0 = (android.net.wifi.WifiManager) r0
                if (r0 == 0) goto L_0x0789
                int r6 = r0.getWifiApState()
                java.lang.String r7 = "ss"
                java.lang.String r7 = r4.getStringExtra(r7)
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                java.lang.String r9 = " INTENT_KEY_ICC_STATE state : "
                r8.append(r9)
                r8.append(r7)
                java.lang.String r8 = r8.toString()
                android.util.Log.i(r2, r8)
                java.lang.String r8 = "ABSENT"
                boolean r8 = r8.equals(r7)
                if (r8 == 0) goto L_0x062d
                r8 = 11
                if (r6 == r8) goto L_0x0789
                java.lang.String r8 = "INTENT_VALUE_ICC_ABSENT received, disable wifi hotspot"
                android.util.Log.i(r2, r8)
                r2 = 0
                r0.semSetWifiApEnabled(r10, r2)
                goto L_0x0789
            L_0x062d:
                java.lang.String r8 = "LOADED"
                boolean r8 = r8.equals(r7)
                if (r8 == 0) goto L_0x0789
                java.lang.String r8 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.mHotspotActionForSimStatus
                if (r8 == 0) goto L_0x0789
                java.lang.String r8 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.mHotspotActionForSimStatus
                java.lang.String r9 = "turn off"
                boolean r8 = r8.equals(r9)
                if (r8 == 0) goto L_0x0789
                r8 = 11
                if (r6 == r8) goto L_0x0789
                java.lang.String r8 = "INTENT_VALUE_ICC_LOADED received, disable wifi hotspot"
                android.util.Log.e(r2, r8)
                r2 = 0
                r0.semSetWifiApEnabled(r10, r2)
                goto L_0x0789
            L_0x0656:
                r5 = r37
                goto L_0x0789
            L_0x065a:
                r34 = r5
                r5 = r2
                r2 = r34
                r35 = r4
                r4 = r3
                r3 = r35
            L_0x0664:
                java.lang.String r6 = "ro.csc.sales_code"
                java.lang.String r6 = android.os.SystemProperties.get(r6)
                java.lang.String r7 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.CONFIGOPBRANDINGFORMOBILEAP
                java.lang.String r12 = "SPRINT"
                boolean r7 = r12.equals(r7)
                if (r7 != 0) goto L_0x069c
                boolean r7 = r12.equals(r6)
                if (r7 != 0) goto L_0x069c
                java.lang.String r7 = "SPR"
                boolean r7 = r7.equals(r6)
                if (r7 != 0) goto L_0x069c
                java.lang.String r7 = "XAS"
                boolean r7 = r7.equals(r6)
                if (r7 != 0) goto L_0x069c
                java.lang.String r7 = "VMU"
                boolean r7 = r7.equals(r6)
                if (r7 != 0) goto L_0x069c
                java.lang.String r7 = "BST"
                boolean r7 = r7.equals(r6)
                if (r7 == 0) goto L_0x0788
            L_0x069c:
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r7 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                int r7 = r7.getRvfMode(r5)
                r12 = 1
                if (r7 == r12) goto L_0x0788
                java.lang.String r7 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.CONFIGOPBRANDINGFORMOBILEAP
                java.lang.String r12 = "ALL"
                boolean r7 = r12.equals(r7)
                if (r7 == 0) goto L_0x0745
                java.lang.Object r0 = r5.getSystemService(r11)
                r7 = r0
                android.net.wifi.WifiManager r7 = (android.net.wifi.WifiManager) r7
                int r9 = r7.getWifiApState()
                r0 = 12
                if (r9 == r0) goto L_0x06c3
                if (r9 == r8) goto L_0x06c3
                return
            L_0x06c3:
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r8 = "Mobile AP is disabled by [USA OPEN (SPR)] don't : "
                r0.append(r8)
                r0.append(r9)
                java.lang.String r0 = r0.toString()
                android.util.Log.i(r2, r0)
                r8 = 0
                r7.semSetWifiApEnabled(r10, r8)
                r10 = 600(0x258, double:2.964E-321)
                java.lang.Thread.sleep(r10)     // Catch:{ InterruptedException -> 0x06e1 }
                goto L_0x06f8
            L_0x06e1:
                r0 = move-exception
                r8 = r0
                r0 = r8
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                java.lang.String r10 = "Error InterruptedException "
                r8.append(r10)
                r8.append(r0)
                java.lang.String r8 = r8.toString()
                android.util.Log.i(r2, r8)
            L_0x06f8:
                com.android.server.wifi.WifiInjector r0 = com.android.server.wifi.WifiInjector.getInstance()
                com.samsung.android.server.wifi.softap.SemWifiApChipInfo r0 = r0.getSemWifiApChipInfo()
                boolean r0 = r0.supportWifiSharing()
                if (r0 == 0) goto L_0x071c
                com.android.server.wifi.WifiInjector r0 = com.android.server.wifi.WifiInjector.getInstance()
                com.samsung.android.server.wifi.softap.SemWifiApChipInfo r0 = r0.getSemWifiApChipInfo()
                boolean r0 = r0.supportWifiSharing()
                if (r0 == 0) goto L_0x0744
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                boolean r0 = r0.isWifiSharingEnabled(r5)
                if (r0 != 0) goto L_0x0744
            L_0x071c:
                r8 = 0
                android.content.ContentResolver r0 = r37.getContentResolver()     // Catch:{ SettingNotFoundException -> 0x0729 }
                java.lang.String r10 = "wifi_saved_state"
                int r0 = android.provider.Settings.Secure.getInt(r0, r10)     // Catch:{ SettingNotFoundException -> 0x0729 }
                r8 = r0
                goto L_0x072f
            L_0x0729:
                r0 = move-exception
                java.lang.String r10 = "SettingNotFoundException"
                android.util.Log.i(r2, r10)
            L_0x072f:
                r10 = 1
                if (r8 != r10) goto L_0x0744
                java.lang.String r0 = "Need to enabled Wifi since provision dialog got dismissed in onPause"
                android.util.Log.d(r2, r0)
                r7.setWifiEnabled(r10)
                android.content.ContentResolver r0 = r37.getContentResolver()
                java.lang.String r2 = "wifi_saved_state"
                r10 = 0
                android.provider.Settings.Secure.putInt(r0, r2, r10)
            L_0x0744:
                goto L_0x0788
            L_0x0745:
                boolean r0 = r3.equals(r0)
                if (r0 == 0) goto L_0x0776
                java.lang.String r0 = "CODE"
                java.lang.String r0 = r4.getStringExtra(r0)
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                java.lang.String r8 = "mipErrorCode : "
                r7.append(r8)
                r7.append(r0)
                java.lang.String r7 = r7.toString()
                android.util.Log.i(r2, r7)
                if (r0 == 0) goto L_0x0775
                java.lang.String r2 = "67"
                boolean r2 = r0.equals(r2)
                if (r2 == 0) goto L_0x0775
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r2 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                r7 = 2
                r2.showHotspotErrorDialog(r5, r7, r4)
            L_0x0775:
                goto L_0x0788
            L_0x0776:
                boolean r0 = r3.equals(r9)
                if (r0 == 0) goto L_0x0783
                com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.this
                r2 = 3
                r0.showHotspotErrorDialog(r5, r2, r4)
                goto L_0x0788
            L_0x0783:
                java.lang.String r0 = "do NOT turn off MHS when DIALOG_HOTSPOT_NO_DATA , spr new requirement!!!!"
                android.util.Log.i(r2, r0)
            L_0x0788:
            L_0x0789:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver.C07911.onReceive(android.content.Context, android.content.Intent):void");
        }
    };
    /* access modifiers changed from: private */
    public long mStartTimeOfMobileData;
    /* access modifiers changed from: private */
    public TelephonyManager mTelephonyManagerForHotspot = null;
    /* access modifiers changed from: private */
    public PhoneStateListener mTelephonyPhoneStateListener = null;
    /* access modifiers changed from: private */
    public long mTempMobileRxBytes;
    /* access modifiers changed from: private */
    public long mTempMobileTxBytes;
    /* access modifiers changed from: private */
    public long mTimeOfStartMobileAp;

    public SemWifiApBroadcastReceiver(Context context) {
        this.mContext = context;
        this.mFilter = new IntentFilter();
        this.mFilter.addAction(WIFIAP_MODEMNAI_MISSMATH);
        this.mFilter.addAction(WIFIAP_TETHERING_DENIED);
        this.mFilter.addAction(START_PROVISIONING);
        this.mFilter.addAction(WIFIAP_TETHERING_FAILED);
        this.mFilter.addAction(AP_STA_24GHZ_DISCONNECTED);
        this.mFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        this.mFilter.addAction("com.nttdocomo.intent.action.SHOW_WPSDIALOG");
        this.mFilter.addAction(ADVANCED_WIFI_SHARING_NOTI);
        this.mFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mFilter.addAction("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED");
        Log.d(TAG, " SemWifiApBroadcastReceiver intialized");
    }

    public void startTracking() {
        if (!isRegistered) {
            this.mContext.registerReceiver(this.mReceiver, this.mFilter);
            Log.d(TAG, " SemWifiApBroadcastReceiver startTracking");
        }
    }

    public void stopTracking() {
        isRegistered = false;
        Log.d(TAG, " SemWifiApBroadcastReceiver stopTracking");
        callInitBigdataVariables();
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    /* access modifiers changed from: private */
    public int getRvfMode(Context context) {
        Log.i(TAG, "getRvfMode");
        WifiManager mWifiManager = (WifiManager) context.getSystemService("wifi");
        if (mWifiManager == null) {
            return 0;
        }
        Message msg = new Message();
        msg.what = 28;
        return mWifiManager.callSECApi(msg);
    }

    /* access modifiers changed from: private */
    public boolean isWifiSharingEnabled(Context context) {
        try {
            if (Settings.Secure.getInt(context.getContentResolver(), "wifi_ap_wifi_sharing") == 1) {
                Log.i(TAG, "Returning true");
                return true;
            }
            if (Settings.Secure.getInt(context.getContentResolver(), "wifi_ap_wifi_sharing") == 0) {
                Log.i(TAG, "Returning false");
                return false;
            }
            return false;
        } catch (Settings.SettingNotFoundException e1) {
            Log.i(TAG, "Error in getting provider value" + e1);
        }
    }

    /* access modifiers changed from: private */
    public void startWifiApSettings(Context context) {
        Intent wifiApIntent = new Intent();
        wifiApIntent.setAction("com.samsung.settings.WIFI_AP_SETTINGS");
        wifiApIntent.setFlags(276824064);
        context.startActivity(wifiApIntent);
    }

    /* access modifiers changed from: private */
    public void showHotspotErrorDialog(Context context, int DialogType, Intent intent) {
        Log.i(TAG, "[showHotspotErrorDialog] DialogType : " + DialogType);
        WifiManager mWifiManager = (WifiManager) context.getSystemService("wifi");
        int extra_type = intent.getIntExtra("extra_type", -1);
        int req_type = intent.getIntExtra("req_type", -1);
        if (mWifiManager != null) {
            int wifiApState = mWifiManager.getWifiApState();
            if (DialogType == 4) {
                if (req_type == 0 && extra_type == 1 && WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing() && isWifiSharingEnabled(context)) {
                    return;
                }
                if (!(wifiApState == 12 || wifiApState == 13 || extra_type + req_type == 3 || extra_type == 4)) {
                    return;
                }
            } else if (DialogType == 5) {
                if (wifiApState == 12 || wifiApState == 13) {
                    return;
                }
            } else if (DialogType != 50 || !WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing() || !isWifiSharingEnabled(context)) {
                if (DialogType == 51) {
                    if (!(wifiApState == 12 || wifiApState == 13)) {
                        Log.i(TAG, "Wifi AP is not enabled during DIALOG_HOTSPOT_24GHZ_AP_STA_DISCONNECT");
                        return;
                    }
                } else if (wifiApState == 12 || wifiApState == 13) {
                    Log.i(TAG, "Mobile AP is disabled by [showHotspotErrorDialog] : " + wifiApState);
                    mWifiManager.semSetWifiApEnabled((WifiConfiguration) null, false);
                } else {
                    return;
                }
            } else if (!(wifiApState == 12 || wifiApState == 13)) {
                Log.i(TAG, "Wifi AP is not enabled");
                return;
            }
            StatusBarManager statusBar = (StatusBarManager) context.getSystemService("statusbar");
            if (statusBar != null) {
                statusBar.collapsePanels();
            }
            Intent startDialogIntent = new Intent();
            startDialogIntent.setClassName("com.android.settings", "com.samsung.android.settings.wifi.mobileap.WifiApWarning");
            startDialogIntent.setFlags(268435456);
            startDialogIntent.setAction(WIFIAP_WARNING_DIALOG);
            startDialogIntent.putExtra(WIFIAP_WARNING_DIALOG_TYPE, DialogType);
            startDialogIntent.putExtra("req_type", req_type);
            startDialogIntent.putExtra("extra_type", extra_type);
            context.startActivity(startDialogIntent);
        }
    }

    /* access modifiers changed from: private */
    public boolean isProvisioningNeeded(Context context) {
        String[] mProvisionApp = context.getResources().getStringArray(17236154);
        if ("ATT".equals(CONFIGOPBRANDINGFORMOBILEAP) || "VZW".equals(CONFIGOPBRANDINGFORMOBILEAP) || "TMO".equals(CONFIGOPBRANDINGFORMOBILEAP) || "NEWCO".equals(CONFIGOPBRANDINGFORMOBILEAP)) {
            String tetheringProvisionApp = SemCscFeature.getInstance().getString(CscFeatureTagSetting.TAG_CSCFEATURE_SETTING_CONFIGMOBILEHOTSPOTPROVISIONAPP);
            if (SystemProperties.getBoolean("net.tethering.noprovisioning", false) || mProvisionApp == null || mProvisionApp.length != 2 || TextUtils.isEmpty(tetheringProvisionApp)) {
                return false;
            }
        }
        if (isWifiSharingEnabled(context)) {
            if (isWifiConnected(context)) {
                Log.d(TAG, "Wifi is connected so skip provisioning for Wifi Sharing");
                return false;
            }
            Log.d(TAG, "Wifi is not connected so dont skip provisioning for Wifi Sharing");
        }
        if (mProvisionApp.length == 2) {
            return true;
        }
        return false;
    }

    private boolean isWifiConnected(Context context) {
        boolean isWifiConnected = false;
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            boolean z = true;
            if (activeNetworkInfo.getType() != 1) {
                z = false;
            }
            isWifiConnected = z;
        }
        Log.d(TAG, "isWifiConnected : " + isWifiConnected);
        return isWifiConnected;
    }

    /* access modifiers changed from: private */
    public void startHotspotProvisioningRequestWifiSharing(Context context, int DialogType) {
        Log.d(TAG, "startHotspotProvisioningRequest for Wifi Sharing");
        Intent startDialogIntent = new Intent();
        startDialogIntent.setClassName("com.android.settings", "com.samsung.android.settings.wifi.mobileap.WifiApWarning");
        startDialogIntent.setFlags(268435456);
        startDialogIntent.setAction(WIFIAP_WARNING_DIALOG);
        startDialogIntent.putExtra(WIFIAP_WARNING_DIALOG_TYPE, DialogType);
        startDialogIntent.putExtra(WIFIAP_PROVISION_DIALOG_TYPE, DialogType);
        context.startActivity(startDialogIntent);
    }

    /* access modifiers changed from: private */
    public void callSecBigdataApi(String feature, String data) {
        Message msg = new Message();
        msg.what = 77;
        Bundle args = new Bundle();
        args.putBoolean("bigdata", true);
        args.putString("feature", feature);
        Log.d(TAG, "Bigdata logging " + data);
        args.putString("data", data);
        msg.obj = args;
        ((WifiManager) this.mContext.getSystemService("wifi")).callSECApi(msg);
    }

    /* access modifiers changed from: private */
    public void resetParameterForHotspotLogging() {
        this.mTelephonyPhoneStateListener = null;
        this.mAmountMobileTxBytes = 0;
        this.mAmountMobileRxBytes = 0;
        this.mTempMobileRxBytes = 0;
        this.mTempMobileTxBytes = 0;
        this.mAmountTimeOfMobileData = 0;
        this.mTempMobileTxBytes = 0;
        this.mTempMobileRxBytes = 0;
        this.bIsFirstCall = false;
        mBaseTxBytes = 0;
        this.mBaseRxBytes = 0;
        this.mTelephonyManagerForHotspot = null;
    }

    /* access modifiers changed from: private */
    public void callInitBigdataVariables() {
        Log.d(TAG, "callInitBigdataVariables() ");
        Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_max_client_number", 0);
    }

    /* access modifiers changed from: private */
    public String convertBytesToMegaByte(long tempValue) {
        long valueOfDevided = tempValue / 1048576;
        if (valueOfDevided >= ((long) 500)) {
            return "over" + 500 + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.9d) {
            return (((double) 500) * 0.9d) + "~" + 500 + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.8d) {
            return (((double) 500) * 0.8d) + "~" + (((double) 500) * 0.9d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.7d) {
            return (((double) 500) * 0.7d) + "~" + (((double) 500) * 0.8d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.6d) {
            return (((double) 500) * 0.6d) + "~" + (((double) 500) * 0.7d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.5d) {
            return (((double) 500) * 0.5d) + "~" + (((double) 500) * 0.6d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.4d) {
            return (((double) 500) * 0.4d) + "~" + (((double) 500) * 0.5d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.3d) {
            return (((double) 500) * 0.3d) + "~" + (((double) 500) * 0.4d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.2d) {
            return (((double) 500) * 0.2d) + "~" + (((double) 500) * 0.3d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.1d) {
            return (((double) 500) * 0.1d) + "~" + (((double) 500) * 0.2d) + "MB";
        } else {
            return "0~" + (((double) 500) * 0.1d) + "MB";
        }
    }

    /* access modifiers changed from: private */
    public String convertBytesToMegaByteForLogging(long tempValue) {
        return "" + (tempValue / 1048576);
    }

    /* access modifiers changed from: private */
    public String convertMinute(long tempValue) {
        long valueOfDevided = tempValue;
        if (valueOfDevided >= 120) {
            return (valueOfDevided / 60) + "hour";
        } else if (valueOfDevided >= 100) {
            return "100~120";
        } else {
            if (valueOfDevided >= 80) {
                return "80~100";
            }
            if (valueOfDevided >= 60) {
                return "60~80";
            }
            if (valueOfDevided >= 40) {
                return "40~60";
            }
            if (valueOfDevided >= 20) {
                return "20~40";
            }
            return "0~20";
        }
    }

    /* access modifiers changed from: private */
    public void setRvfMode(Context context, int mode) {
        Message msg = new Message();
        msg.what = 27;
        Bundle b = new Bundle();
        b.putInt("mode", mode);
        msg.obj = b;
        ((WifiManager) context.getSystemService("wifi")).callSECApi(msg);
    }

    /* access modifiers changed from: package-private */
    public void showLimitDataReachNotification(Context context) {
        String title = context.getResources().getString(17042609);
        String message = context.getResources().getString(17040046);
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.wifi.mobileap.WifiApSettings");
        intent.setFlags(335544320);
        PendingIntent pi = PendingIntent.getActivityAsUser(context, 0, intent, 0, (Bundle) null, UserHandle.CURRENT);
        Notification.Builder mNotiBuilder = new Notification.Builder(context);
        mNotiBuilder.setWhen(0).setOngoing(false).setAutoCancel(true).setColor(context.getColor(17170460)).setVisibility(1).setCategory("status").setSmallIcon(17304239).setContentTitle(title).setContentText(message).setContentIntent(pi);
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        }
        this.mNotificationManager.notifyAsUser((String) null, DATA_REACH, mNotiBuilder.build(), UserHandle.CURRENT);
    }

    /* access modifiers changed from: package-private */
    public void clearLimitDataReachNotification(Context context) {
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        }
        this.mNotificationManager.cancelAsUser((String) null, DATA_REACH, UserHandle.ALL);
    }

    /* access modifiers changed from: package-private */
    public void showWifiScanListNotification(Context context, String message) {
        Context context2 = context;
        String str = message;
        String title = context.getResources().getString(17042593);
        String str2 = title;
        NotificationChannel mChannel = new NotificationChannel("wifi_sharing_channel", str2, 4);
        mChannel.setDescription(title);
        mChannel.enableLights(true);
        mChannel.setLightColor(-65536);
        mChannel.enableVibration(true);
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.wifi.WifiSettings");
        intent.setFlags(335544320);
        String str3 = "com.android.settings";
        Intent intent2 = intent;
        String str4 = str2;
        PendingIntent pi = PendingIntent.getActivityAsUser(context, 0, intent, 0, (Bundle) null, UserHandle.CURRENT);
        Intent advWSIntent = new Intent(ADVANCED_WIFI_SHARING_NOTI);
        advWSIntent.setPackage(str3);
        advWSIntent.putExtra("NOTIFICATION_TASK", 0);
        PendingIntent piDismiss = PendingIntent.getBroadcast(context2, 0, advWSIntent, 0);
        Intent wifiSettingsIntent = new Intent();
        wifiSettingsIntent.setPackage(str3);
        wifiSettingsIntent.setClassName(str3, "com.android.settings.wifi.WifiSettings");
        wifiSettingsIntent.setFlags(335544320);
        PendingIntent piWifiSettings = PendingIntent.getActivity(context2, 0, wifiSettingsIntent, 0);
        Notification.Builder mNotiBuilder = new Notification.Builder(context2);
        Intent intent3 = advWSIntent;
        mNotiBuilder.setWhen(0).setOngoing(false).setAutoCancel(true).setColor(context2.getColor(17170460)).setVisibility(1).setCategory("status").setSmallIcon(17301642).setContentTitle(title).setContentText(str).setChannelId("wifi_sharing_channel").setPriority(2).addAction(17301642, context.getResources().getString(17039360), piDismiss).addAction(17301642, context.getResources().getString(17039767), piWifiSettings).setStyle(new Notification.BigTextStyle().bigText(str)).setTimeoutAfter(20000).setContentIntent(pi);
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) context2.getSystemService("notification");
        }
        this.mNotificationManager.createNotificationChannel(mChannel);
        this.mNotificationManager.notifyAsUser((String) null, 17042593, mNotiBuilder.build(), UserHandle.CURRENT);
        Log.d(TAG, "showWifiScanListNotification");
    }

    /* access modifiers changed from: package-private */
    public void clearWifiScanListNotification(Context context) {
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        }
        this.mNotificationManager.cancelAsUser((String) null, 17042593, UserHandle.ALL);
        Log.d(TAG, "clearWifiScanListNotification");
    }
}
