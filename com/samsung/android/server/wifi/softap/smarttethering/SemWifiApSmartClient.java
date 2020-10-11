package com.samsung.android.server.wifi.softap.smarttethering;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.samsung.android.emergencymode.SemEmergencyManager;
import com.samsung.android.net.wifi.SemWifiApBleScanResult;
import com.samsung.android.net.wifi.SemWifiApMacInfo;
import com.samsung.android.server.wifi.WifiDevicePolicyManager;
import com.samsung.android.server.wifi.softap.SemWifiApPowerSaveImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SemWifiApSmartClient {
    private static final String ACTION_LOGOUT_ACCOUNTS_COMPLETE = "com.samsung.account.SAMSUNGACCOUNT_SIGNOUT_COMPLETED";
    /* access modifiers changed from: private */
    public static String TAG = "SemWifiApSmartClient";
    private static IntentFilter mSemWifiApSmartClientIntentFilter = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
    /* access modifiers changed from: private */
    public int CLIENT_BLE_UPDATE_SCAN_DATA_INTERVAL = 20000;
    private final int START_ADVERTISE = 10;
    private final int STOP_ADVERTISE = 11;
    private final int UPDATE_BLE_SCAN_RESULT = 13;
    private HashSet<BluetoothDevice> bonedDevicesFromHotspotLive = new HashSet<>();
    /* access modifiers changed from: private */
    public boolean isAdvRunning;
    private boolean isJDMDevice = "in_house".contains("jdm");
    /* access modifiers changed from: private */
    public boolean isStartAdvPending;
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(SemWifiApSmartClient.TAG, "Client Advertise Started.");
        }

        public void onStartFailure(int errorCode) {
            String access$000 = SemWifiApSmartClient.TAG;
            Log.e(access$000, "Client Advertise Failed: " + errorCode);
            LocalLog access$100 = SemWifiApSmartClient.this.mLocalLog;
            access$100.log(SemWifiApSmartClient.TAG + ":Client Advertise Failed: " + errorCode);
        }
    };
    Map<String, Pair<Long, String>> mBLEPairingFailedHashMap = new ConcurrentHashMap();
    /* access modifiers changed from: private */
    public BleWorkHandler mBleWorkHandler = null;
    private HandlerThread mBleWorkThread = null;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /* access modifiers changed from: private */
    public Context mContext;
    private Long mHashBasedD2DFamilyID;
    private Long mHashBasedFamilyID;
    private Long mHashBasedGuid;
    /* access modifiers changed from: private */
    public LocalLog mLocalLog;
    private boolean mNeedAdvertisement;
    private PowerManager mPowerManager;
    private int mScanningCount = 0;
    /* access modifiers changed from: private */
    public List<SemWifiApBleScanResult> mSemWifiApBleScanResults = new ArrayList();
    private SemWifiApSmartClientReceiver mSemWifiApSmartClientReceiver;
    private SemWifiApSmartUtil mSemWifiApSmartUtil;
    private Set<String> mSmartMHSDevices = new HashSet();
    private WifiDevicePolicyManager mWifiDevicePolicyManager;
    HashMap<String, Integer> mlowBatteryHashMap = new HashMap<>();
    /* access modifiers changed from: private */
    public WifiInfo mwifiInfo;
    List<ScanFilter> scanFilters = new ArrayList();

    static {
        mSemWifiApSmartClientIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        mSemWifiApSmartClientIntentFilter.addAction("com.samsung.bluetooth.adapter.action.BLE_STATE_CHANGED");
        mSemWifiApSmartClientIntentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        mSemWifiApSmartClientIntentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        mSemWifiApSmartClientIntentFilter.addAction(ACTION_LOGOUT_ACCOUNTS_COMPLETE);
        mSemWifiApSmartClientIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        mSemWifiApSmartClientIntentFilter.addAction("com.samsung.android.server.wifi.softap.smarttethering.changed");
        mSemWifiApSmartClientIntentFilter.addAction("com.samsung.android.server.wifi.softap.smarttethering.familyid");
        mSemWifiApSmartClientIntentFilter.addAction("com.samsung.android.server.wifi.softap.smarttethering.d2dfamilyid");
        mSemWifiApSmartClientIntentFilter.addAction("android.intent.action.SCREEN_OFF");
        mSemWifiApSmartClientIntentFilter.addAction(SemWifiApPowerSaveImpl.ACTION_SCREEN_OFF_BY_PROXIMITY);
    }

    public SemWifiApSmartClient(Context context, SemWifiApSmartUtil tSemWifiApSmartUtil, LocalLog tLocalLog) {
        this.mContext = context;
        this.mSemWifiApSmartUtil = tSemWifiApSmartUtil;
        this.mSemWifiApSmartClientReceiver = new SemWifiApSmartClientReceiver();
        this.mLocalLog = tLocalLog;
        if (!FactoryTest.isFactoryBinary()) {
            this.mContext.registerReceiver(this.mSemWifiApSmartClientReceiver, mSemWifiApSmartClientIntentFilter);
        } else {
            Log.e(TAG, "This devices's binary is a factory binary");
        }
    }

    public void sendEmptyMessage(int val) {
        BleWorkHandler bleWorkHandler = this.mBleWorkHandler;
        if (bleWorkHandler != null) {
            bleWorkHandler.sendEmptyMessageDelayed(val, 2000);
        } else if (bleWorkHandler == null && val == 10) {
            Log.d(TAG, "START_ADVERTISE with null bleworker");
            this.mNeedAdvertisement = true;
        } else if (this.mBleWorkHandler == null && val == 11) {
            Log.d(TAG, "STOP_ADVERTISE with null bleworker");
            this.mNeedAdvertisement = false;
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 61 */
    /* JADX WARNING: Code restructure failed: missing block: B:143:0x0593, code lost:
        r16 = r7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:146:0x0599, code lost:
        r17 = r4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:149:0x059d, code lost:
        r18 = r5;
        r20 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:152:0x05a3, code lost:
        r56 = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:155:0x05a7, code lost:
        r58 = r14;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:158:0x05c8, code lost:
        r7 = new com.samsung.android.net.wifi.SemWifiApBleScanResult(r9.mDevice, r9.mMHSdeviceType, r9.mBattery, r9.mNetworkType, 2, r2, r9.mUserName, r9.mSSID, r9.mhidden, r9.mSecurity, r9.mTimeStamp, r9.mBLERssi, r9.version);
        r1 = r20;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:160:?, code lost:
        r9.mDevice = r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:161:0x05cd, code lost:
        r3 = r49;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:163:?, code lost:
        r9.mBattery = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:164:0x05d1, code lost:
        r4 = r57;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:166:?, code lost:
        r9.mMHSdeviceType = r4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:167:0x05d5, code lost:
        r5 = r55;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:169:?, code lost:
        r9.mNetworkType = r5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:170:0x05d9, code lost:
        r14 = r58;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:172:?, code lost:
        r9.mUserName = r14;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:173:0x05dd, code lost:
        r6 = r48;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:175:?, code lost:
        r9.mSSID = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:176:0x05e1, code lost:
        r10 = r50;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:178:?, code lost:
        r9.mhidden = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:179:0x05e5, code lost:
        r11 = r56;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:181:?, code lost:
        r9.mSecurity = r11;
        r9.mTimeStamp = java.lang.System.currentTimeMillis();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:182:0x05ef, code lost:
        r12 = r18;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:184:?, code lost:
        r9.mBLERssi = r12;
        r9.version = r17[15];
     */
    /* JADX WARNING: Code restructure failed: missing block: B:185:0x05fb, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:186:0x05fc, code lost:
        r20 = r1;
        r16 = r7;
        r9 = r10;
        r13 = r54;
        r7 = r61;
        r10 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:187:0x0608, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:188:0x0609, code lost:
        r12 = r18;
        r20 = r1;
        r16 = r7;
        r9 = r10;
        r13 = r54;
        r7 = r61;
        r10 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:189:0x0617, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:190:0x0618, code lost:
        r12 = r18;
        r11 = r56;
        r20 = r1;
        r16 = r7;
        r9 = r10;
        r13 = r54;
        r7 = r61;
        r10 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:191:0x0628, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:192:0x0629, code lost:
        r12 = r18;
        r11 = r56;
        r20 = r1;
        r10 = r2;
        r16 = r7;
        r9 = r50;
        r13 = r54;
        r7 = r61;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:193:0x063a, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:194:0x063b, code lost:
        r12 = r18;
        r6 = r48;
        r11 = r56;
        r20 = r1;
        r10 = r2;
        r16 = r7;
        r9 = r50;
        r13 = r54;
        r7 = r61;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:195:0x064e, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:196:0x064f, code lost:
        r12 = r18;
        r6 = r48;
        r11 = r56;
        r14 = r58;
        r20 = r1;
        r10 = r2;
        r16 = r7;
        r9 = r50;
        r13 = r54;
        r7 = r61;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:197:0x0664, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:198:0x0665, code lost:
        r12 = r18;
        r6 = r48;
        r5 = r55;
        r11 = r56;
        r14 = r58;
        r20 = r1;
        r10 = r2;
        r16 = r7;
        r9 = r50;
        r13 = r54;
        r7 = r61;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:199:0x067c, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:200:0x067d, code lost:
        r12 = r18;
        r6 = r48;
        r5 = r55;
        r11 = r56;
        r4 = r57;
        r14 = r58;
        r20 = r1;
        r10 = r2;
        r16 = r7;
        r9 = r50;
        r13 = r54;
        r7 = r61;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:201:0x0696, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:202:0x0697, code lost:
        r12 = r18;
        r6 = r48;
        r3 = r49;
        r5 = r55;
        r11 = r56;
        r4 = r57;
        r14 = r58;
        r20 = r1;
        r10 = r2;
        r16 = r7;
        r9 = r50;
        r13 = r54;
        r7 = r61;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:203:0x06b2, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:204:0x06b3, code lost:
        r12 = r18;
        r6 = r48;
        r3 = r49;
        r5 = r55;
        r11 = r56;
        r4 = r57;
        r14 = r58;
        r7 = r61;
        r10 = r2;
        r9 = r50;
        r13 = r54;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:205:0x06ca, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:206:0x06cb, code lost:
        r12 = r18;
        r6 = r48;
        r3 = r49;
        r5 = r55;
        r11 = r56;
        r4 = r57;
        r7 = r61;
        r10 = r2;
        r9 = r50;
        r13 = r54;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:207:0x06e0, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:208:0x06e1, code lost:
        r11 = r3;
        r12 = r18;
        r6 = r48;
        r3 = r49;
        r5 = r55;
        r4 = r57;
        r7 = r61;
        r10 = r2;
        r9 = r50;
        r13 = r54;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:209:0x06f5, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:210:0x06f6, code lost:
        r11 = r3;
        r12 = r5;
        r1 = r6;
        r6 = r48;
        r3 = r49;
        r5 = r55;
        r4 = r57;
        r7 = r61;
        r20 = r1;
        r10 = r2;
        r9 = r50;
        r13 = r54;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:211:0x070c, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:212:0x070d, code lost:
        r11 = r3;
        r17 = r4;
        r12 = r5;
        r1 = r6;
        r6 = r48;
        r3 = r49;
        r5 = r55;
        r4 = r57;
        r7 = r61;
        r20 = r1;
        r10 = r2;
        r9 = r50;
        r13 = r54;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:223:0x07ad, code lost:
        if (r7 == null) goto L_0x07ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:226:0x07b5, code lost:
        if (r7.mDevice.equalsIgnoreCase(r1) == false) goto L_0x07ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:228:0x07bd, code lost:
        if (r7.mSSID.equals(r6) == false) goto L_0x07ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:230:0x07c1, code lost:
        if (r7.mhidden != r10) goto L_0x07ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:232:0x07c5, code lost:
        if (r7.mSecurity != r11) goto L_0x07ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:234:0x07c9, code lost:
        if (r3 <= 15) goto L_0x07cf;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:236:0x07cd, code lost:
        if (r7.mBattery > 15) goto L_0x07d7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:238:0x07d3, code lost:
        if (r7.mBattery > 15) goto L_0x07ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:239:0x07d5, code lost:
        if (r3 > 15) goto L_0x07ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:241:0x07e0, code lost:
        if (java.lang.Math.abs(r7.mBLERssi - r12) >= 10) goto L_0x07ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:242:0x07e2, code lost:
        if (r14 == null) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:244:0x07ea, code lost:
        if (r7.mUserName.equals(r14) == false) goto L_0x07ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:245:0x07ed, code lost:
        r0 = TAG;
        r8 = new java.lang.StringBuilder();
        r8.append("adv_ssid_length,srp_ssid_length:");
        r9 = r53;
        r8.append(r9);
        r8.append(",");
        r8.append(r54);
        android.util.Log.d(r0, r8.toString());
        android.util.Log.d(TAG, "updated Scanresult data::" + java.util.Arrays.toString(r17));
        r0 = TAG;
        r8 = new java.lang.StringBuilder();
        r8.append(" updated Smart MHS Device with version,");
        r16 = r7;
        r8.append(r17[15]);
        r8.append(",Bt mac:");
        r8.append(r1);
        r8.append(",mBattery:");
        r8.append(r3);
        r8.append(",mNetwork:");
        r8.append(r5);
        r8.append(",SSID:");
        r8.append(r6);
        r8.append(",mMHS_MAC:");
        r8.append(r2);
        r8.append(",mUserName");
        r8.append(r14);
        r8.append(",Security:");
        r8.append(r11);
        r8.append(",mhidden:");
        r8.append(r10);
        r8.append(",timestamp:");
        r53 = r9;
        r50 = r10;
        r8.append(java.lang.System.currentTimeMillis());
        r8.append(",mBLERssi:");
        r8.append(r12);
        r8.append(",mMHSdeviceType:");
        r8.append(r4);
        android.util.Log.d(r0, r8.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:246:0x08a5, code lost:
        r7 = r61;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:248:?, code lost:
        r0 = r7.mLocalLog;
        r8 = new java.lang.StringBuilder();
        r8.append(TAG);
        r8.append(":\tupdated Smart MHS Device with version,");
        r8.append(r17[15]);
        r8.append(",Bt mac:");
        r8.append(r1);
        r8.append(",mBattery:");
        r8.append(r3);
        r8.append(",mNetwork:");
        r8.append(r5);
        r8.append(",SSID:");
        r8.append(r6);
        r8.append(",mMHS_MAC:");
        r8.append(r2);
        r8.append(",mUserName:");
        r8.append(r14);
        r8.append(",Security:");
        r8.append(r11);
        r8.append(",mhidden:");
        r8.append(r50);
        r8.append(",curTimestamp:");
        r20 = r1;
        r10 = r2;
        r8.append(java.lang.System.currentTimeMillis());
        r8.append(",mBLERssi:");
        r8.append(r12);
        r8.append(",mMHSdeviceType:");
        r8.append(r4);
        r0.log(r8.toString());
        r7.mLocalLog.log(TAG + ":\tupdated Scanresult data::" + java.util.Arrays.toString(r17));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:249:0x0947, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:250:0x0948, code lost:
        r7 = r61;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:259:0x0972, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:276:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:277:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:281:?, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void sendScanResultFromScanner(int r62, android.bluetooth.le.ScanResult r63) {
        /*
            r61 = this;
            r1 = r61
            r2 = r62
            android.bluetooth.le.ScanRecord r0 = r63.getScanRecord()     // Catch:{ Exception -> 0x098b }
            r3 = r0
            r0 = 117(0x75, float:1.64E-43)
            byte[] r0 = r3.getManufacturerSpecificData(r0)     // Catch:{ Exception -> 0x098b }
            r4 = r0
            r0 = 0
            int r5 = r63.getRssi()     // Catch:{ Exception -> 0x098b }
            r6 = 0
            r7 = 0
            r8 = 1
            r9 = 50
            r10 = 0
            r11 = 0
            r12 = 0
            r13 = 0
            r14 = 0
            r15 = 1
            if (r2 != r15) goto L_0x002a
            r8 = 1
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r15 = r1.mSemWifiApSmartUtil     // Catch:{ Exception -> 0x098b }
            java.lang.String r15 = r15.getSameUserName()     // Catch:{ Exception -> 0x098b }
            r14 = r15
        L_0x002a:
            r15 = 2
            if (r2 != r15) goto L_0x0050
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r15 = r1.mSemWifiApSmartUtil     // Catch:{ Exception -> 0x098b }
            int r15 = r15.getSamsungAccountCount()     // Catch:{ Exception -> 0x098b }
            if (r15 != 0) goto L_0x0044
            r8 = 3
            android.content.Context r15 = r1.mContext     // Catch:{ Exception -> 0x098b }
            r18 = r0
            r0 = 17040041(0x10402a9, float:2.424648E-38)
            java.lang.String r0 = r15.getString(r0)     // Catch:{ Exception -> 0x098b }
            r14 = r0
            r15 = r8
            goto L_0x0053
        L_0x0044:
            r18 = r0
            r0 = 2
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r8 = r1.mSemWifiApSmartUtil     // Catch:{ Exception -> 0x098b }
            java.lang.String r8 = r8.getUserNameFromFamily(r4)     // Catch:{ Exception -> 0x098b }
            r15 = r0
            r14 = r8
            goto L_0x0053
        L_0x0050:
            r18 = r0
            r15 = r8
        L_0x0053:
            r0 = 12
        L_0x0055:
            r6 = 15
            if (r0 >= r6) goto L_0x0061
            byte r8 = r4[r0]     // Catch:{ Exception -> 0x098b }
            if (r8 == 0) goto L_0x005e
            goto L_0x0061
        L_0x005e:
            int r0 = r0 + 1
            goto L_0x0055
        L_0x0061:
            if (r0 != r6) goto L_0x0064
            return
        L_0x0064:
            r8 = 12
            byte r6 = r4[r8]     // Catch:{ Exception -> 0x098b }
            r6 = r6 & 240(0xf0, float:3.36E-43)
            r8 = 4
            int r6 = r6 >> r8
            r8 = 16
            char r6 = java.lang.Character.forDigit(r6, r8)     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = java.lang.Character.toString(r6)     // Catch:{ Exception -> 0x098b }
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r8.<init>()     // Catch:{ Exception -> 0x098b }
            r8.append(r6)     // Catch:{ Exception -> 0x098b }
            r20 = 12
            byte r20 = r4[r20]     // Catch:{ Exception -> 0x098b }
            r22 = r0
            r19 = 15
            r0 = r20 & 15
            r2 = 16
            char r0 = java.lang.Character.forDigit(r0, r2)     // Catch:{ Exception -> 0x098b }
            java.lang.String r0 = java.lang.Character.toString(r0)     // Catch:{ Exception -> 0x098b }
            r8.append(r0)     // Catch:{ Exception -> 0x098b }
            java.lang.String r0 = r8.toString()     // Catch:{ Exception -> 0x098b }
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r2.<init>()     // Catch:{ Exception -> 0x098b }
            r2.append(r0)     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = ":"
            r2.append(r6)     // Catch:{ Exception -> 0x098b }
            java.lang.String r2 = r2.toString()     // Catch:{ Exception -> 0x098b }
            r0 = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r2.<init>()     // Catch:{ Exception -> 0x098b }
            r2.append(r0)     // Catch:{ Exception -> 0x098b }
            r6 = 13
            byte r8 = r4[r6]     // Catch:{ Exception -> 0x098b }
            r8 = r8 & 240(0xf0, float:3.36E-43)
            r20 = 4
            int r8 = r8 >> 4
            r6 = 16
            char r8 = java.lang.Character.forDigit(r8, r6)     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = java.lang.Character.toString(r8)     // Catch:{ Exception -> 0x098b }
            r2.append(r6)     // Catch:{ Exception -> 0x098b }
            java.lang.String r2 = r2.toString()     // Catch:{ Exception -> 0x098b }
            r0 = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r2.<init>()     // Catch:{ Exception -> 0x098b }
            r2.append(r0)     // Catch:{ Exception -> 0x098b }
            r6 = 13
            byte r6 = r4[r6]     // Catch:{ Exception -> 0x098b }
            r8 = 15
            r6 = r6 & r8
            r8 = 16
            char r6 = java.lang.Character.forDigit(r6, r8)     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = java.lang.Character.toString(r6)     // Catch:{ Exception -> 0x098b }
            r2.append(r6)     // Catch:{ Exception -> 0x098b }
            java.lang.String r2 = r2.toString()     // Catch:{ Exception -> 0x098b }
            r0 = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r2.<init>()     // Catch:{ Exception -> 0x098b }
            r2.append(r0)     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = ":"
            r2.append(r6)     // Catch:{ Exception -> 0x098b }
            java.lang.String r2 = r2.toString()     // Catch:{ Exception -> 0x098b }
            r0 = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r2.<init>()     // Catch:{ Exception -> 0x098b }
            r2.append(r0)     // Catch:{ Exception -> 0x098b }
            r6 = 14
            byte r8 = r4[r6]     // Catch:{ Exception -> 0x098b }
            r8 = r8 & 240(0xf0, float:3.36E-43)
            r20 = 4
            int r8 = r8 >> 4
            r6 = 16
            char r8 = java.lang.Character.forDigit(r8, r6)     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = java.lang.Character.toString(r8)     // Catch:{ Exception -> 0x098b }
            r2.append(r6)     // Catch:{ Exception -> 0x098b }
            java.lang.String r2 = r2.toString()     // Catch:{ Exception -> 0x098b }
            r0 = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r2.<init>()     // Catch:{ Exception -> 0x098b }
            r2.append(r0)     // Catch:{ Exception -> 0x098b }
            r6 = 14
            byte r6 = r4[r6]     // Catch:{ Exception -> 0x098b }
            r8 = 15
            r6 = r6 & r8
            r8 = 16
            char r6 = java.lang.Character.forDigit(r6, r8)     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = java.lang.Character.toString(r6)     // Catch:{ Exception -> 0x098b }
            r2.append(r6)     // Catch:{ Exception -> 0x098b }
            java.lang.String r2 = r2.toString()     // Catch:{ Exception -> 0x098b }
            r0 = r2
            java.lang.String r2 = r0.toLowerCase()     // Catch:{ Exception -> 0x098b }
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r0 = r1.mSemWifiApSmartUtil     // Catch:{ Exception -> 0x098b }
            int r0 = r0.getSamsungAccountCount()     // Catch:{ Exception -> 0x098b }
            if (r0 != 0) goto L_0x0173
            android.content.Context r0 = r1.mContext     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = "smart_tethering_d2d_Wifimac"
            java.lang.String r0 = com.samsung.android.net.wifi.SemWifiApContentProviderHelper.get(r0, r6)     // Catch:{ Exception -> 0x098b }
            boolean r6 = android.text.TextUtils.isEmpty(r0)     // Catch:{ Exception -> 0x098b }
            if (r6 == 0) goto L_0x0162
            return
        L_0x0162:
            java.lang.String r6 = "\n"
            java.lang.String[] r6 = r0.split(r6)     // Catch:{ Exception -> 0x098b }
            java.util.List r8 = java.util.Arrays.asList(r6)     // Catch:{ Exception -> 0x098b }
            boolean r8 = r8.contains(r2)     // Catch:{ Exception -> 0x098b }
            if (r8 != 0) goto L_0x0173
            return
        L_0x0173:
            android.bluetooth.BluetoothDevice r0 = r63.getDevice()     // Catch:{ Exception -> 0x098b }
            java.lang.String r0 = r0.getAddress()     // Catch:{ Exception -> 0x098b }
            r8 = r0
            java.util.Map<java.lang.String, android.util.Pair<java.lang.Long, java.lang.String>> r0 = r1.mBLEPairingFailedHashMap     // Catch:{ Exception -> 0x098b }
            java.lang.Object r0 = r0.get(r2)     // Catch:{ Exception -> 0x098b }
            android.util.Pair r0 = (android.util.Pair) r0     // Catch:{ Exception -> 0x098b }
            r6 = r0
            r23 = -1
            if (r6 == 0) goto L_0x019d
            long r25 = java.lang.System.currentTimeMillis()     // Catch:{ Exception -> 0x098b }
            java.lang.Object r0 = r6.first     // Catch:{ Exception -> 0x098b }
            java.lang.Long r0 = (java.lang.Long) r0     // Catch:{ Exception -> 0x098b }
            long r27 = r0.longValue()     // Catch:{ Exception -> 0x098b }
            long r23 = r25 - r27
            r0 = r11
            r18 = r12
            r11 = r23
            goto L_0x01a2
        L_0x019d:
            r0 = r11
            r18 = r12
            r11 = r23
        L_0x01a2:
            if (r6 == 0) goto L_0x0231
            if (r8 == 0) goto L_0x0231
            r20 = r0
            java.lang.Object r0 = r6.second     // Catch:{ Exception -> 0x098b }
            if (r0 == 0) goto L_0x022c
            java.lang.Object r0 = r6.second     // Catch:{ Exception -> 0x098b }
            boolean r0 = r8.equals(r0)     // Catch:{ Exception -> 0x098b }
            if (r0 == 0) goto L_0x01bb
            r23 = 60000(0xea60, double:2.9644E-319)
            int r0 = (r11 > r23 ? 1 : (r11 == r23 ? 0 : -1))
            if (r0 < 0) goto L_0x01c9
        L_0x01bb:
            java.lang.Object r0 = r6.second     // Catch:{ Exception -> 0x098b }
            boolean r0 = r8.equals(r0)     // Catch:{ Exception -> 0x098b }
            if (r0 != 0) goto L_0x0227
            r23 = 5000(0x1388, double:2.4703E-320)
            int r0 = (r11 > r23 ? 1 : (r11 == r23 ? 0 : -1))
            if (r0 >= 0) goto L_0x0227
        L_0x01c9:
            java.lang.String r0 = TAG     // Catch:{ Exception -> 0x098b }
            r38 = r3
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r3.<init>()     // Catch:{ Exception -> 0x098b }
            r23 = r7
            java.lang.String r7 = "new mBLE_MAC:"
            r3.append(r7)     // Catch:{ Exception -> 0x098b }
            r3.append(r8)     // Catch:{ Exception -> 0x098b }
            java.lang.String r7 = ",diff:"
            r3.append(r7)     // Catch:{ Exception -> 0x098b }
            r3.append(r11)     // Catch:{ Exception -> 0x098b }
            java.lang.String r7 = ",old BLE mac:"
            r3.append(r7)     // Catch:{ Exception -> 0x098b }
            java.lang.Object r7 = r6.second     // Catch:{ Exception -> 0x098b }
            java.lang.String r7 = (java.lang.String) r7     // Catch:{ Exception -> 0x098b }
            r3.append(r7)     // Catch:{ Exception -> 0x098b }
            java.lang.String r3 = r3.toString()     // Catch:{ Exception -> 0x098b }
            android.util.Log.d(r0, r3)     // Catch:{ Exception -> 0x098b }
            android.util.LocalLog r0 = r1.mLocalLog     // Catch:{ Exception -> 0x098b }
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r3.<init>()     // Catch:{ Exception -> 0x098b }
            java.lang.String r7 = TAG     // Catch:{ Exception -> 0x098b }
            r3.append(r7)     // Catch:{ Exception -> 0x098b }
            java.lang.String r7 = ":\tnew mBLE_MAC:"
            r3.append(r7)     // Catch:{ Exception -> 0x098b }
            r3.append(r8)     // Catch:{ Exception -> 0x098b }
            java.lang.String r7 = ",diff:"
            r3.append(r7)     // Catch:{ Exception -> 0x098b }
            r3.append(r11)     // Catch:{ Exception -> 0x098b }
            java.lang.String r7 = ",old BLE mac:"
            r3.append(r7)     // Catch:{ Exception -> 0x098b }
            java.lang.Object r7 = r6.second     // Catch:{ Exception -> 0x098b }
            java.lang.String r7 = (java.lang.String) r7     // Catch:{ Exception -> 0x098b }
            r3.append(r7)     // Catch:{ Exception -> 0x098b }
            java.lang.String r3 = r3.toString()     // Catch:{ Exception -> 0x098b }
            r0.log(r3)     // Catch:{ Exception -> 0x098b }
            return
        L_0x0227:
            r38 = r3
            r23 = r7
            goto L_0x0237
        L_0x022c:
            r38 = r3
            r23 = r7
            goto L_0x0237
        L_0x0231:
            r20 = r0
            r38 = r3
            r23 = r7
        L_0x0237:
            if (r6 == 0) goto L_0x023e
            java.util.Map<java.lang.String, android.util.Pair<java.lang.Long, java.lang.String>> r0 = r1.mBLEPairingFailedHashMap     // Catch:{ Exception -> 0x098b }
            r0.remove(r2)     // Catch:{ Exception -> 0x098b }
        L_0x023e:
            r39 = 0
            r3 = 11
            byte r0 = r4[r3]     // Catch:{ Exception -> 0x098b }
            r41 = r0
            r0 = r41 & 64
            r7 = r41 & -64
            if (r0 != r7) goto L_0x024f
            r0 = 1
            r7 = r0
            goto L_0x025a
        L_0x024f:
            r0 = r41 & -64
            r7 = r41 & -64
            if (r0 != r7) goto L_0x0258
            r0 = 2
            r7 = r0
            goto L_0x025a
        L_0x0258:
            r0 = 0
            r7 = r0
        L_0x025a:
            r0 = r41 & 4
            r42 = r3
            r3 = 4
            if (r0 != r3) goto L_0x0264
            r0 = 1
            r3 = r0
            goto L_0x0265
        L_0x0264:
            r3 = r10
        L_0x0265:
            r0 = r41 & 2
            r10 = 2
            if (r0 != r10) goto L_0x026d
            r0 = 1
            r10 = r0
            goto L_0x026f
        L_0x026d:
            r10 = r20
        L_0x026f:
            r0 = r41 & 8
            r17 = r6
            r6 = r41 & 56
            if (r0 != r6) goto L_0x027b
            r0 = 15
            r9 = r0
            goto L_0x02b6
        L_0x027b:
            r0 = r41 & 16
            r6 = r41 & 56
            if (r0 != r6) goto L_0x0285
            r0 = 30
            r9 = r0
            goto L_0x02b6
        L_0x0285:
            r0 = r41 & 24
            r6 = r41 & 56
            if (r0 != r6) goto L_0x028f
            r0 = 45
            r9 = r0
            goto L_0x02b6
        L_0x028f:
            r0 = r41 & 32
            r6 = r41 & 56
            if (r0 != r6) goto L_0x0299
            r0 = 60
            r9 = r0
            goto L_0x02b6
        L_0x0299:
            r0 = r41 & 40
            r6 = r41 & 56
            if (r0 != r6) goto L_0x02a3
            r0 = 75
            r9 = r0
            goto L_0x02b6
        L_0x02a3:
            r0 = r41 & 48
            r6 = r41 & 56
            if (r0 != r6) goto L_0x02ad
            r0 = 90
            r9 = r0
            goto L_0x02b6
        L_0x02ad:
            r0 = r41 & 56
            r6 = r41 & 56
            if (r0 != r6) goto L_0x02b6
            r0 = 100
            r9 = r0
        L_0x02b6:
            r0 = 34
            byte[] r0 = new byte[r0]     // Catch:{ Exception -> 0x098b }
            r6 = r0
            r0 = 0
            r20 = 17
            r59 = r11
            r12 = r0
            r11 = r18
            r0 = r20
            r20 = r59
        L_0x02c7:
            r18 = r8
            r8 = 24
            if (r0 >= r8) goto L_0x02e0
            byte r8 = r4[r0]     // Catch:{ Exception -> 0x098b }
            if (r8 != 0) goto L_0x02d2
            goto L_0x02e0
        L_0x02d2:
            int r11 = r11 + 1
            int r8 = r12 + 1
            byte r22 = r4[r0]     // Catch:{ Exception -> 0x098b }
            r6[r12] = r22     // Catch:{ Exception -> 0x098b }
            int r0 = r0 + 1
            r12 = r8
            r8 = r18
            goto L_0x02c7
        L_0x02e0:
            r0 = 26
            r8 = r13
            r13 = r0
        L_0x02e4:
            r0 = 51
            if (r13 >= r0) goto L_0x02f9
            byte r0 = r4[r13]     // Catch:{ Exception -> 0x098b }
            if (r0 != 0) goto L_0x02ed
            goto L_0x02f9
        L_0x02ed:
            int r0 = r12 + 1
            byte r22 = r4[r13]     // Catch:{ Exception -> 0x098b }
            r6[r12] = r22     // Catch:{ Exception -> 0x098b }
            int r8 = r8 + 1
            int r13 = r13 + 1
            r12 = r0
            goto L_0x02e4
        L_0x02f9:
            java.lang.String r0 = new java.lang.String     // Catch:{ Exception -> 0x098b }
            r22 = r13
            r13 = 17
            r0.<init>(r4, r13, r11)     // Catch:{ Exception -> 0x098b }
            r43 = r0
            java.lang.String r0 = new java.lang.String     // Catch:{ Exception -> 0x098b }
            r13 = 26
            r0.<init>(r4, r13, r8)     // Catch:{ Exception -> 0x098b }
            r44 = r0
            java.lang.String r0 = new java.lang.String     // Catch:{ Exception -> 0x098b }
            r13 = 0
            r0.<init>(r6, r13, r12)     // Catch:{ Exception -> 0x098b }
            r45 = r0
            r0 = 15
            if (r9 <= r0) goto L_0x0322
            java.util.HashMap<java.lang.String, java.lang.Integer> r0 = r1.mlowBatteryHashMap     // Catch:{ Exception -> 0x098b }
            java.lang.Integer r13 = java.lang.Integer.valueOf(r13)     // Catch:{ Exception -> 0x098b }
            r0.put(r2, r13)     // Catch:{ Exception -> 0x098b }
        L_0x0322:
            android.net.wifi.WifiInfo r0 = r1.mwifiInfo     // Catch:{ Exception -> 0x098b }
            if (r0 == 0) goto L_0x03bd
            java.lang.String r0 = r61.getBssid()     // Catch:{ Exception -> 0x098b }
            java.util.HashMap<java.lang.String, java.lang.Integer> r13 = r1.mlowBatteryHashMap     // Catch:{ Exception -> 0x098b }
            java.lang.Object r13 = r13.get(r2)     // Catch:{ Exception -> 0x098b }
            java.lang.Integer r13 = (java.lang.Integer) r13     // Catch:{ Exception -> 0x098b }
            r23 = r6
            r6 = 15
            if (r9 > r6) goto L_0x03b4
            if (r0 == 0) goto L_0x03b4
            if (r13 == 0) goto L_0x03b4
            int r6 = r13.intValue()     // Catch:{ Exception -> 0x098b }
            if (r6 != 0) goto L_0x03b4
            boolean r6 = r1.islegacy(r2)     // Catch:{ Exception -> 0x098b }
            if (r6 != 0) goto L_0x03b4
            java.util.HashMap<java.lang.String, java.lang.Integer> r6 = r1.mlowBatteryHashMap     // Catch:{ Exception -> 0x098b }
            r24 = r0
            r16 = 1
            java.lang.Integer r0 = java.lang.Integer.valueOf(r16)     // Catch:{ Exception -> 0x098b }
            r6.put(r2, r0)     // Catch:{ Exception -> 0x098b }
            java.lang.String r0 = TAG     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = "Sending low battery intent"
            android.util.Log.d(r0, r6)     // Catch:{ Exception -> 0x098b }
            android.content.Intent r0 = new android.content.Intent     // Catch:{ Exception -> 0x098b }
            r0.<init>()     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = "com.android.settings"
            r16 = r8
            java.lang.String r8 = "com.samsung.android.settings.wifi.mobileap.WifiApWarning"
            r0.setClassName(r6, r8)     // Catch:{ Exception -> 0x098b }
            r6 = 268435456(0x10000000, float:2.5243549E-29)
            r0.setFlags(r6)     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = "com.samsung.android.settings.wifi.mobileap.wifiapwarning"
            r0.setAction(r6)     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = "st_ssid_name"
            r8 = r45
            r0.putExtra(r6, r8)     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = "battery_info"
            r0.putExtra(r6, r9)     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = "wifiap_warning_dialog_type"
            r25 = r11
            r11 = 42
            r0.putExtra(r6, r11)     // Catch:{ Exception -> 0x098b }
            android.content.Context r6 = r1.mContext     // Catch:{ Exception -> 0x098b }
            r6.startActivity(r0)     // Catch:{ Exception -> 0x098b }
            android.util.LocalLog r6 = r1.mLocalLog     // Catch:{ Exception -> 0x098b }
            java.lang.StringBuilder r11 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r11.<init>()     // Catch:{ Exception -> 0x098b }
            r26 = r0
            java.lang.String r0 = TAG     // Catch:{ Exception -> 0x098b }
            r11.append(r0)     // Catch:{ Exception -> 0x098b }
            java.lang.String r0 = ":sending low battery intent mBattery :"
            r11.append(r0)     // Catch:{ Exception -> 0x098b }
            r11.append(r9)     // Catch:{ Exception -> 0x098b }
            java.lang.String r0 = "mMHS_MAC"
            r11.append(r0)     // Catch:{ Exception -> 0x098b }
            r11.append(r2)     // Catch:{ Exception -> 0x098b }
            java.lang.String r0 = r11.toString()     // Catch:{ Exception -> 0x098b }
            r6.log(r0)     // Catch:{ Exception -> 0x098b }
            goto L_0x03c5
        L_0x03b4:
            r24 = r0
            r16 = r8
            r25 = r11
            r8 = r45
            goto L_0x03c5
        L_0x03bd:
            r23 = r6
            r16 = r8
            r25 = r11
            r8 = r45
        L_0x03c5:
            if (r2 == 0) goto L_0x0536
            java.util.Set<java.lang.String> r0 = r1.mSmartMHSDevices     // Catch:{ Exception -> 0x098b }
            boolean r0 = r0.add(r2)     // Catch:{ Exception -> 0x098b }
            if (r0 == 0) goto L_0x0536
            android.bluetooth.BluetoothDevice r0 = r63.getDevice()     // Catch:{ Exception -> 0x098b }
            java.lang.String r0 = r0.getAddress()     // Catch:{ Exception -> 0x098b }
            java.lang.String r6 = TAG     // Catch:{ Exception -> 0x098b }
            java.lang.StringBuilder r11 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r11.<init>()     // Catch:{ Exception -> 0x098b }
            java.lang.String r13 = " Smart MHS Device with version,"
            r11.append(r13)     // Catch:{ Exception -> 0x098b }
            r24 = r12
            r13 = 15
            byte r12 = r4[r13]     // Catch:{ Exception -> 0x098b }
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ", Bt mac:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r0)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mBattery:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r9)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mNetwork:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r7)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",SSID:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r8)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mMHS_MAC:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r2)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mUserName:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r14)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",Security:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r3)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mhidden:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r10)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",curTimestamp:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            long r12 = java.lang.System.currentTimeMillis()     // Catch:{ Exception -> 0x098b }
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mBLERssi:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r5)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = "mMHSdeviceType:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r15)     // Catch:{ Exception -> 0x098b }
            java.lang.String r11 = r11.toString()     // Catch:{ Exception -> 0x098b }
            android.util.Log.d(r6, r11)     // Catch:{ Exception -> 0x098b }
            android.util.LocalLog r6 = r1.mLocalLog     // Catch:{ Exception -> 0x098b }
            java.lang.StringBuilder r11 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x098b }
            r11.<init>()     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = TAG     // Catch:{ Exception -> 0x098b }
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ":\tSmart MHS Device with version,"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r12 = 15
            byte r13 = r4[r12]     // Catch:{ Exception -> 0x098b }
            r11.append(r13)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",Bt mac:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r0)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mBattery:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r9)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mNetwork:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r7)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",SSID:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r8)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mMHS_MAC:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r2)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mUserName:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r14)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",Security:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r3)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mhidden:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r10)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",curTimestamp:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            long r12 = java.lang.System.currentTimeMillis()     // Catch:{ Exception -> 0x098b }
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = ",mBLERssi:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r5)     // Catch:{ Exception -> 0x098b }
            java.lang.String r12 = "mMHSdeviceType:"
            r11.append(r12)     // Catch:{ Exception -> 0x098b }
            r11.append(r15)     // Catch:{ Exception -> 0x098b }
            java.lang.String r11 = r11.toString()     // Catch:{ Exception -> 0x098b }
            r6.log(r11)     // Catch:{ Exception -> 0x098b }
            if (r14 == 0) goto L_0x0519
            com.samsung.android.net.wifi.SemWifiApBleScanResult r26 = new com.samsung.android.net.wifi.SemWifiApBleScanResult     // Catch:{ Exception -> 0x098b }
            android.bluetooth.BluetoothDevice r6 = r63.getDevice()     // Catch:{ Exception -> 0x098b }
            java.lang.String r11 = r6.getAddress()     // Catch:{ Exception -> 0x098b }
            r12 = 2
            long r27 = java.lang.System.currentTimeMillis()     // Catch:{ Exception -> 0x098b }
            r6 = 15
            byte r29 = r4[r6]     // Catch:{ Exception -> 0x098b }
            r45 = r17
            r46 = r23
            r6 = r26
            r13 = r7
            r7 = r11
            r48 = r8
            r11 = r16
            r47 = r18
            r8 = r15
            r49 = r9
            r50 = r10
            r10 = r13
            r54 = r11
            r51 = r20
            r53 = r25
            r11 = r12
            r21 = r24
            r12 = r2
            r55 = r13
            r13 = r14
            r56 = r14
            r14 = r48
            r57 = r15
            r15 = r50
            r16 = r3
            r17 = r27
            r19 = r5
            r20 = r29
            r6.<init>(r7, r8, r9, r10, r11, r12, r13, r14, r15, r16, r17, r19, r20)     // Catch:{ Exception -> 0x098b }
            r6 = r26
            r1.addScanResults(r6)     // Catch:{ Exception -> 0x098b }
            goto L_0x0533
        L_0x0519:
            r55 = r7
            r48 = r8
            r49 = r9
            r50 = r10
            r56 = r14
            r57 = r15
            r54 = r16
            r45 = r17
            r47 = r18
            r51 = r20
            r46 = r23
            r21 = r24
            r53 = r25
        L_0x0533:
            r7 = r1
            goto L_0x098a
        L_0x0536:
            r55 = r7
            r48 = r8
            r49 = r9
            r50 = r10
            r56 = r14
            r57 = r15
            r54 = r16
            r45 = r17
            r47 = r18
            r51 = r20
            r46 = r23
            r53 = r25
            r21 = r12
            if (r2 == 0) goto L_0x0976
            java.util.Set<java.lang.String> r0 = r1.mSmartMHSDevices     // Catch:{ Exception -> 0x098b }
            boolean r0 = r0.add(r2)     // Catch:{ Exception -> 0x098b }
            if (r0 != 0) goto L_0x0976
            android.bluetooth.BluetoothDevice r0 = r63.getDevice()     // Catch:{ Exception -> 0x098b }
            java.lang.String r0 = r0.getAddress()     // Catch:{ Exception -> 0x098b }
            r6 = r0
            r7 = 0
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r8 = r1.mSemWifiApBleScanResults     // Catch:{ Exception -> 0x098b }
            monitor-enter(r8)     // Catch:{ Exception -> 0x098b }
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r0 = r1.mSemWifiApBleScanResults     // Catch:{ all -> 0x0957 }
            java.util.Iterator r0 = r0.iterator()     // Catch:{ all -> 0x0957 }
        L_0x056d:
            boolean r9 = r0.hasNext()     // Catch:{ all -> 0x0957 }
            if (r9 == 0) goto L_0x0799
            java.lang.Object r9 = r0.next()     // Catch:{ all -> 0x077c }
            com.samsung.android.net.wifi.SemWifiApBleScanResult r9 = (com.samsung.android.net.wifi.SemWifiApBleScanResult) r9     // Catch:{ all -> 0x077c }
            java.lang.String r10 = r9.mWifiMac     // Catch:{ all -> 0x077c }
            boolean r10 = r10.equalsIgnoreCase(r2)     // Catch:{ all -> 0x077c }
            if (r10 == 0) goto L_0x0752
            r14 = r56
            if (r14 == 0) goto L_0x0740
            com.samsung.android.net.wifi.SemWifiApBleScanResult r0 = new com.samsung.android.net.wifi.SemWifiApBleScanResult     // Catch:{ all -> 0x0725 }
            java.lang.String r10 = r9.mDevice     // Catch:{ all -> 0x0725 }
            int r11 = r9.mMHSdeviceType     // Catch:{ all -> 0x0725 }
            int r12 = r9.mBattery     // Catch:{ all -> 0x0725 }
            int r13 = r9.mNetworkType     // Catch:{ all -> 0x0725 }
            r28 = 2
            java.lang.String r15 = r9.mUserName     // Catch:{ all -> 0x0725 }
            r16 = r7
            java.lang.String r7 = r9.mSSID     // Catch:{ all -> 0x070c }
            int r1 = r9.mhidden     // Catch:{ all -> 0x070c }
            r17 = r4
            int r4 = r9.mSecurity     // Catch:{ all -> 0x06f5 }
            r18 = r5
            r20 = r6
            long r5 = r9.mTimeStamp     // Catch:{ all -> 0x06e0 }
            r56 = r3
            int r3 = r9.mBLERssi     // Catch:{ all -> 0x06ca }
            r58 = r14
            int r14 = r9.version     // Catch:{ all -> 0x06b2 }
            r23 = r0
            r24 = r10
            r25 = r11
            r26 = r12
            r27 = r13
            r29 = r2
            r30 = r15
            r31 = r7
            r32 = r1
            r33 = r4
            r34 = r5
            r36 = r3
            r37 = r14
            r23.<init>(r24, r25, r26, r27, r28, r29, r30, r31, r32, r33, r34, r36, r37)     // Catch:{ all -> 0x06b2 }
            r7 = r0
            r1 = r20
            r9.mDevice = r1     // Catch:{ all -> 0x0696 }
            r3 = r49
            r9.mBattery = r3     // Catch:{ all -> 0x067c }
            r4 = r57
            r9.mMHSdeviceType = r4     // Catch:{ all -> 0x0664 }
            r5 = r55
            r9.mNetworkType = r5     // Catch:{ all -> 0x064e }
            r14 = r58
            r9.mUserName = r14     // Catch:{ all -> 0x063a }
            r6 = r48
            r9.mSSID = r6     // Catch:{ all -> 0x0628 }
            r10 = r50
            r9.mhidden = r10     // Catch:{ all -> 0x0617 }
            r11 = r56
            r9.mSecurity = r11     // Catch:{ all -> 0x0608 }
            long r12 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x0608 }
            r9.mTimeStamp = r12     // Catch:{ all -> 0x0608 }
            r12 = r18
            r9.mBLERssi = r12     // Catch:{ all -> 0x05fb }
            r0 = 15
            byte r13 = r17[r0]     // Catch:{ all -> 0x05fb }
            r9.version = r13     // Catch:{ all -> 0x05fb }
            goto L_0x07ac
        L_0x05fb:
            r0 = move-exception
            r20 = r1
            r16 = r7
            r9 = r10
            r13 = r54
            r7 = r61
            r10 = r2
            goto L_0x0970
        L_0x0608:
            r0 = move-exception
            r12 = r18
            r20 = r1
            r16 = r7
            r9 = r10
            r13 = r54
            r7 = r61
            r10 = r2
            goto L_0x0970
        L_0x0617:
            r0 = move-exception
            r12 = r18
            r11 = r56
            r20 = r1
            r16 = r7
            r9 = r10
            r13 = r54
            r7 = r61
            r10 = r2
            goto L_0x0970
        L_0x0628:
            r0 = move-exception
            r12 = r18
            r11 = r56
            r20 = r1
            r10 = r2
            r16 = r7
            r9 = r50
            r13 = r54
            r7 = r61
            goto L_0x0970
        L_0x063a:
            r0 = move-exception
            r12 = r18
            r6 = r48
            r11 = r56
            r20 = r1
            r10 = r2
            r16 = r7
            r9 = r50
            r13 = r54
            r7 = r61
            goto L_0x0970
        L_0x064e:
            r0 = move-exception
            r12 = r18
            r6 = r48
            r11 = r56
            r14 = r58
            r20 = r1
            r10 = r2
            r16 = r7
            r9 = r50
            r13 = r54
            r7 = r61
            goto L_0x0970
        L_0x0664:
            r0 = move-exception
            r12 = r18
            r6 = r48
            r5 = r55
            r11 = r56
            r14 = r58
            r20 = r1
            r10 = r2
            r16 = r7
            r9 = r50
            r13 = r54
            r7 = r61
            goto L_0x0970
        L_0x067c:
            r0 = move-exception
            r12 = r18
            r6 = r48
            r5 = r55
            r11 = r56
            r4 = r57
            r14 = r58
            r20 = r1
            r10 = r2
            r16 = r7
            r9 = r50
            r13 = r54
            r7 = r61
            goto L_0x0970
        L_0x0696:
            r0 = move-exception
            r12 = r18
            r6 = r48
            r3 = r49
            r5 = r55
            r11 = r56
            r4 = r57
            r14 = r58
            r20 = r1
            r10 = r2
            r16 = r7
            r9 = r50
            r13 = r54
            r7 = r61
            goto L_0x0970
        L_0x06b2:
            r0 = move-exception
            r12 = r18
            r6 = r48
            r3 = r49
            r5 = r55
            r11 = r56
            r4 = r57
            r14 = r58
            r7 = r61
            r10 = r2
            r9 = r50
            r13 = r54
            goto L_0x0970
        L_0x06ca:
            r0 = move-exception
            r12 = r18
            r6 = r48
            r3 = r49
            r5 = r55
            r11 = r56
            r4 = r57
            r7 = r61
            r10 = r2
            r9 = r50
            r13 = r54
            goto L_0x0970
        L_0x06e0:
            r0 = move-exception
            r11 = r3
            r12 = r18
            r6 = r48
            r3 = r49
            r5 = r55
            r4 = r57
            r7 = r61
            r10 = r2
            r9 = r50
            r13 = r54
            goto L_0x0970
        L_0x06f5:
            r0 = move-exception
            r11 = r3
            r12 = r5
            r1 = r6
            r6 = r48
            r3 = r49
            r5 = r55
            r4 = r57
            r7 = r61
            r20 = r1
            r10 = r2
            r9 = r50
            r13 = r54
            goto L_0x0970
        L_0x070c:
            r0 = move-exception
            r11 = r3
            r17 = r4
            r12 = r5
            r1 = r6
            r6 = r48
            r3 = r49
            r5 = r55
            r4 = r57
            r7 = r61
            r20 = r1
            r10 = r2
            r9 = r50
            r13 = r54
            goto L_0x0970
        L_0x0725:
            r0 = move-exception
            r11 = r3
            r17 = r4
            r12 = r5
            r1 = r6
            r16 = r7
            r6 = r48
            r3 = r49
            r5 = r55
            r4 = r57
            r7 = r61
            r20 = r1
            r10 = r2
            r9 = r50
            r13 = r54
            goto L_0x0970
        L_0x0740:
            r11 = r3
            r17 = r4
            r12 = r5
            r1 = r6
            r16 = r7
            r6 = r48
            r3 = r49
            r10 = r50
            r5 = r55
            r4 = r57
            goto L_0x0765
        L_0x0752:
            r11 = r3
            r17 = r4
            r12 = r5
            r1 = r6
            r16 = r7
            r6 = r48
            r3 = r49
            r10 = r50
            r5 = r55
            r14 = r56
            r4 = r57
        L_0x0765:
            r49 = r3
            r57 = r4
            r55 = r5
            r48 = r6
            r50 = r10
            r3 = r11
            r5 = r12
            r56 = r14
            r7 = r16
            r4 = r17
            r6 = r1
            r1 = r61
            goto L_0x056d
        L_0x077c:
            r0 = move-exception
            r11 = r3
            r17 = r4
            r12 = r5
            r1 = r6
            r16 = r7
            r6 = r48
            r3 = r49
            r5 = r55
            r14 = r56
            r4 = r57
            r7 = r61
            r20 = r1
            r10 = r2
            r9 = r50
            r13 = r54
            goto L_0x0970
        L_0x0799:
            r11 = r3
            r17 = r4
            r12 = r5
            r1 = r6
            r16 = r7
            r6 = r48
            r3 = r49
            r10 = r50
            r5 = r55
            r14 = r56
            r4 = r57
        L_0x07ac:
            monitor-exit(r8)     // Catch:{ all -> 0x094b }
            if (r7 == 0) goto L_0x07ed
            java.lang.String r0 = r7.mDevice     // Catch:{ Exception -> 0x0947 }
            boolean r0 = r0.equalsIgnoreCase(r1)     // Catch:{ Exception -> 0x0947 }
            if (r0 == 0) goto L_0x07ed
            java.lang.String r0 = r7.mSSID     // Catch:{ Exception -> 0x0947 }
            boolean r0 = r0.equals(r6)     // Catch:{ Exception -> 0x0947 }
            if (r0 == 0) goto L_0x07ed
            int r0 = r7.mhidden     // Catch:{ Exception -> 0x0947 }
            if (r0 != r10) goto L_0x07ed
            int r0 = r7.mSecurity     // Catch:{ Exception -> 0x0947 }
            if (r0 != r11) goto L_0x07ed
            r0 = 15
            if (r3 <= r0) goto L_0x07cf
            int r8 = r7.mBattery     // Catch:{ Exception -> 0x0947 }
            if (r8 > r0) goto L_0x07d7
        L_0x07cf:
            int r0 = r7.mBattery     // Catch:{ Exception -> 0x0947 }
            r8 = 15
            if (r0 > r8) goto L_0x07ed
            if (r3 > r8) goto L_0x07ed
        L_0x07d7:
            int r0 = r7.mBLERssi     // Catch:{ Exception -> 0x0947 }
            int r0 = r0 - r12
            int r0 = java.lang.Math.abs(r0)     // Catch:{ Exception -> 0x0947 }
            r8 = 10
            if (r0 >= r8) goto L_0x07ed
            if (r14 == 0) goto L_0x07ec
            java.lang.String r0 = r7.mUserName     // Catch:{ Exception -> 0x0947 }
            boolean r0 = r0.equals(r14)     // Catch:{ Exception -> 0x0947 }
            if (r0 == 0) goto L_0x07ed
        L_0x07ec:
            return
        L_0x07ed:
            java.lang.String r0 = TAG     // Catch:{ Exception -> 0x0947 }
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0947 }
            r8.<init>()     // Catch:{ Exception -> 0x0947 }
            java.lang.String r9 = "adv_ssid_length,srp_ssid_length:"
            r8.append(r9)     // Catch:{ Exception -> 0x0947 }
            r9 = r53
            r8.append(r9)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r13 = ","
            r8.append(r13)     // Catch:{ Exception -> 0x0947 }
            r13 = r54
            r8.append(r13)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r8 = r8.toString()     // Catch:{ Exception -> 0x0947 }
            android.util.Log.d(r0, r8)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r0 = TAG     // Catch:{ Exception -> 0x0947 }
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0947 }
            r8.<init>()     // Catch:{ Exception -> 0x0947 }
            java.lang.String r15 = "updated Scanresult data::"
            r8.append(r15)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r15 = java.util.Arrays.toString(r17)     // Catch:{ Exception -> 0x0947 }
            r8.append(r15)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r8 = r8.toString()     // Catch:{ Exception -> 0x0947 }
            android.util.Log.d(r0, r8)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r0 = TAG     // Catch:{ Exception -> 0x0947 }
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0947 }
            r8.<init>()     // Catch:{ Exception -> 0x0947 }
            java.lang.String r15 = " updated Smart MHS Device with version,"
            r8.append(r15)     // Catch:{ Exception -> 0x0947 }
            r16 = r7
            r15 = 15
            byte r7 = r17[r15]     // Catch:{ Exception -> 0x0947 }
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = ",Bt mac:"
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            r8.append(r1)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = ",mBattery:"
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            r8.append(r3)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = ",mNetwork:"
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            r8.append(r5)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = ",SSID:"
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            r8.append(r6)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = ",mMHS_MAC:"
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            r8.append(r2)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = ",mUserName"
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            r8.append(r14)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = ",Security:"
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            r8.append(r11)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = ",mhidden:"
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            r8.append(r10)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = ",timestamp:"
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            r53 = r9
            r50 = r10
            long r9 = java.lang.System.currentTimeMillis()     // Catch:{ Exception -> 0x0947 }
            r8.append(r9)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = ",mBLERssi:"
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            r8.append(r12)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = ",mMHSdeviceType:"
            r8.append(r7)     // Catch:{ Exception -> 0x0947 }
            r8.append(r4)     // Catch:{ Exception -> 0x0947 }
            java.lang.String r7 = r8.toString()     // Catch:{ Exception -> 0x0947 }
            android.util.Log.d(r0, r7)     // Catch:{ Exception -> 0x0947 }
            r7 = r61
            android.util.LocalLog r0 = r7.mLocalLog     // Catch:{ Exception -> 0x0972 }
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0972 }
            r8.<init>()     // Catch:{ Exception -> 0x0972 }
            java.lang.String r9 = TAG     // Catch:{ Exception -> 0x0972 }
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r9 = ":\tupdated Smart MHS Device with version,"
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            r9 = 15
            byte r9 = r17[r9]     // Catch:{ Exception -> 0x0972 }
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r9 = ",Bt mac:"
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            r8.append(r1)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r9 = ",mBattery:"
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            r8.append(r3)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r9 = ",mNetwork:"
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            r8.append(r5)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r9 = ",SSID:"
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            r8.append(r6)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r9 = ",mMHS_MAC:"
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            r8.append(r2)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r9 = ",mUserName:"
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            r8.append(r14)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r9 = ",Security:"
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            r8.append(r11)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r9 = ",mhidden:"
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            r9 = r50
            r8.append(r9)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r10 = ",curTimestamp:"
            r8.append(r10)     // Catch:{ Exception -> 0x0972 }
            r20 = r1
            r10 = r2
            long r1 = java.lang.System.currentTimeMillis()     // Catch:{ Exception -> 0x0972 }
            r8.append(r1)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r1 = ",mBLERssi:"
            r8.append(r1)     // Catch:{ Exception -> 0x0972 }
            r8.append(r12)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r1 = ",mMHSdeviceType:"
            r8.append(r1)     // Catch:{ Exception -> 0x0972 }
            r8.append(r4)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r1 = r8.toString()     // Catch:{ Exception -> 0x0972 }
            r0.log(r1)     // Catch:{ Exception -> 0x0972 }
            android.util.LocalLog r0 = r7.mLocalLog     // Catch:{ Exception -> 0x0972 }
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0972 }
            r1.<init>()     // Catch:{ Exception -> 0x0972 }
            java.lang.String r2 = TAG     // Catch:{ Exception -> 0x0972 }
            r1.append(r2)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r2 = ":\tupdated Scanresult data::"
            r1.append(r2)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r2 = java.util.Arrays.toString(r17)     // Catch:{ Exception -> 0x0972 }
            r1.append(r2)     // Catch:{ Exception -> 0x0972 }
            java.lang.String r1 = r1.toString()     // Catch:{ Exception -> 0x0972 }
            r0.log(r1)     // Catch:{ Exception -> 0x0972 }
            goto L_0x098a
        L_0x0947:
            r0 = move-exception
            r7 = r61
            goto L_0x098d
        L_0x094b:
            r0 = move-exception
            r20 = r1
            r16 = r7
            r9 = r10
            r13 = r54
            r7 = r61
            r10 = r2
            goto L_0x0970
        L_0x0957:
            r0 = move-exception
            r10 = r2
            r11 = r3
            r17 = r4
            r12 = r5
            r20 = r6
            r16 = r7
            r6 = r48
            r3 = r49
            r9 = r50
            r13 = r54
            r5 = r55
            r14 = r56
            r4 = r57
            r7 = r1
        L_0x0970:
            monitor-exit(r8)     // Catch:{ all -> 0x0974 }
            throw r0     // Catch:{ Exception -> 0x0972 }
        L_0x0972:
            r0 = move-exception
            goto L_0x098d
        L_0x0974:
            r0 = move-exception
            goto L_0x0970
        L_0x0976:
            r7 = r1
            r10 = r2
            r11 = r3
            r17 = r4
            r12 = r5
            r6 = r48
            r3 = r49
            r9 = r50
            r13 = r54
            r5 = r55
            r14 = r56
            r4 = r57
        L_0x098a:
            goto L_0x0997
        L_0x098b:
            r0 = move-exception
            r7 = r1
        L_0x098d:
            java.lang.String r1 = TAG
            java.lang.String r2 = "for log only, Exception occured"
            android.util.Log.e(r1, r2)
            r0.printStackTrace()
        L_0x0997:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartClient.sendScanResultFromScanner(int, android.bluetooth.le.ScanResult):void");
    }

    public boolean setWifiApSmartClient(boolean enable) {
        if (enable) {
            BleWorkHandler bleWorkHandler = this.mBleWorkHandler;
            if (bleWorkHandler != null) {
                bleWorkHandler.removeMessages(11);
                this.mBleWorkHandler.removeMessages(10);
            }
            sendEmptyMessage(10);
            return true;
        }
        BleWorkHandler bleWorkHandler2 = this.mBleWorkHandler;
        if (bleWorkHandler2 != null) {
            bleWorkHandler2.removeMessages(11);
            this.mBleWorkHandler.removeMessages(10);
        }
        sendEmptyMessage(11);
        return true;
    }

    public void handleBootCompleted() {
        Log.d(TAG, "handleBootCompleted");
        LocalLog localLog = this.mLocalLog;
        localLog.log(TAG + ":\t handleBootCompleted");
        this.mBleWorkThread = new HandlerThread("SemWifiApSmartClientBleHandler");
        this.mBleWorkThread.start();
        this.mBleWorkHandler = new BleWorkHandler(this.mBleWorkThread.getLooper());
        if (this.mNeedAdvertisement && this.mBleWorkHandler != null) {
            Log.d(TAG, "need to advertise client packets");
            sendEmptyMessage(10);
        }
    }

    class SemWifiApSmartClientReceiver extends BroadcastReceiver {
        SemWifiApSmartClientReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean isConnected = false;
            if (action.equals("android.intent.action.SCREEN_OFF") || action.equals(SemWifiApPowerSaveImpl.ACTION_SCREEN_OFF_BY_PROXIMITY)) {
                SemWifiApSmartClient.this.mLocalLog.log(SemWifiApSmartClient.TAG + ":\t LCD is oFF,so stop client advertize");
                Log.d(SemWifiApSmartClient.TAG, "LCD is OFF,so stop client advertize");
                SemWifiApSmartClient.this.setWifiApSmartClient(false);
                return;
            }
            boolean z = true;
            if (action.equals("android.net.wifi.STATE_CHANGE")) {
                WifiManager wifiManager = (WifiManager) SemWifiApSmartClient.this.mContext.getSystemService("wifi");
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (networkInfo != null && networkInfo.isConnected()) {
                    isConnected = true;
                }
                if (isConnected) {
                    WifiInfo unused = SemWifiApSmartClient.this.mwifiInfo = wifiManager.getConnectionInfo();
                    SemWifiApSmartClient.this.mLocalLog.log(SemWifiApSmartClient.TAG + ":\t connected to wifi");
                    Log.d(SemWifiApSmartClient.TAG, "connected to wifi:");
                    return;
                }
                WifiInfo unused2 = SemWifiApSmartClient.this.mwifiInfo = null;
            } else if (action.equals(SemWifiApSmartClient.ACTION_LOGOUT_ACCOUNTS_COMPLETE)) {
                SemWifiApSmartClient.this.mLocalLog.log(SemWifiApSmartClient.TAG + ":LOGOUT_COMPLETE");
                Log.d(SemWifiApSmartClient.TAG, "LOGOUT_COMPLETE");
                SemWifiApSmartClient.this.setWifiApSmartClient(false);
            } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (Settings.Global.getInt(SemWifiApSmartClient.this.mContext.getContentResolver(), "airplane_mode_on", 0) == 0) {
                    z = false;
                }
                boolean isAirplaneMode = z;
                Log.d(SemWifiApSmartClient.TAG, "isAirplaneMode:" + isAirplaneMode);
                SemWifiApSmartClient.this.mLocalLog.log(SemWifiApSmartClient.TAG + ":AIRPLANE_MODE" + isAirplaneMode);
                if (isAirplaneMode) {
                    SemWifiApSmartClient.this.setWifiApSmartClient(false);
                    Settings.Secure.putInt(SemWifiApSmartClient.this.mContext.getContentResolver(), "wifi_client_smart_tethering_settings", 0);
                }
            } else if (action.equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                boolean emergencyMode = intent.getBooleanExtra("phoneinECMState", false);
                Log.d(SemWifiApSmartClient.TAG, "emergencyMode:" + emergencyMode);
                SemWifiApSmartClient.this.mLocalLog.log(SemWifiApSmartClient.TAG + ": EMERGENCY" + emergencyMode);
                if (emergencyMode) {
                    SemWifiApSmartClient.this.setWifiApSmartClient(false);
                    Settings.Secure.putInt(SemWifiApSmartClient.this.mContext.getContentResolver(), "wifi_client_smart_tethering_settings", 0);
                }
            } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                int mWifiState = intent.getIntExtra("wifi_state", 4);
                if (mWifiState != 3 && mWifiState == 1) {
                    Log.d(SemWifiApSmartClient.TAG, "stopping adv due to Wi-FI is OFF/");
                    SemWifiApSmartClient.this.mLocalLog.log(SemWifiApSmartClient.TAG + ":\tstopping adv due to Wi-FI is OFF/");
                    SemWifiApSmartClient.this.clearLocalResults();
                    SemWifiApSmartClient.this.setWifiApSmartClient(false);
                    SemWifiApSmartClient.this.mBLEPairingFailedHashMap.clear();
                }
            } else if (action.equals("com.samsung.bluetooth.adapter.action.BLE_STATE_CHANGED")) {
                int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
                if (SemWifiApSmartClient.this.isAdvRunning) {
                    boolean unused3 = SemWifiApSmartClient.this.isStartAdvPending = false;
                }
                if (SemWifiApSmartClient.this.isStartAdvPending && state == 15) {
                    boolean unused4 = SemWifiApSmartClient.this.isStartAdvPending = false;
                    Log.d(SemWifiApSmartClient.TAG, "BLE state:" + state);
                    SemWifiApSmartClient.this.startWifiApSmartClientAdvertize();
                }
            } else if (action.equals("com.samsung.android.server.wifi.softap.smarttethering.familyid") || action.equals("com.samsung.android.server.wifi.softap.smarttethering.d2dfamilyid")) {
                int CST = Settings.Secure.getInt(SemWifiApSmartClient.this.mContext.getContentResolver(), "wifi_client_smart_tethering_settings", 0);
                Log.d(SemWifiApSmartClient.TAG, "familyid intent is received:cst," + CST);
                if (SemWifiApSmartClient.this.isAdvRunning || CST == 1) {
                    Log.d(SemWifiApSmartClient.TAG, "familyid intent is received, so restarting client advertizement,cst:" + CST);
                    SemWifiApSmartClient.this.mLocalLog.log(SemWifiApSmartClient.TAG + ":\tfamilyid intent is received, so restarting client advertizement,cst:" + CST);
                    if (SemWifiApSmartClient.this.mBleWorkHandler != null) {
                        SemWifiApSmartClient.this.mBleWorkHandler.sendEmptyMessage(11);
                    }
                    if (SemWifiApSmartClient.this.mBleWorkHandler != null) {
                        SemWifiApSmartClient.this.mBleWorkHandler.sendEmptyMessageDelayed(10, 1000);
                    }
                }
            }
        }
    }

    class BleWorkHandler extends Handler {
        public BleWorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int num;
            int i = msg.what;
            if (i == 10) {
                int status = SemWifiApSmartClient.this.checkPreConditions();
                if (status == 0) {
                    Settings.Secure.putInt(SemWifiApSmartClient.this.mContext.getContentResolver(), "wifi_client_smart_tethering_settings", 1);
                    SemWifiApSmartClient.this.startWifiApSmartClientAdvertize();
                    sendEmptyMessageDelayed(13, (long) SemWifiApSmartClient.this.CLIENT_BLE_UPDATE_SCAN_DATA_INTERVAL);
                    return;
                }
                String access$000 = SemWifiApSmartClient.TAG;
                Log.e(access$000, "checkPreConditions failed " + status);
                LocalLog access$100 = SemWifiApSmartClient.this.mLocalLog;
                access$100.log(SemWifiApSmartClient.TAG + ":checkPreConditions failed " + status);
            } else if (i == 11) {
                Settings.Secure.putInt(SemWifiApSmartClient.this.mContext.getContentResolver(), "wifi_client_smart_tethering_settings", 0);
                SemWifiApSmartClient.this.stopWifiApSmartClientAdvertize();
            } else if (i == 13) {
                synchronized (SemWifiApSmartClient.this.mSemWifiApBleScanResults) {
                    num = SemWifiApSmartClient.this.mSemWifiApBleScanResults.size();
                }
                if (SemWifiApSmartClient.this.isAdvRunning || num != 0) {
                    SemWifiApSmartClient.this.updateLocalResults();
                    sendEmptyMessageDelayed(13, (long) SemWifiApSmartClient.this.CLIENT_BLE_UPDATE_SCAN_DATA_INTERVAL);
                    return;
                }
                String access$0002 = SemWifiApSmartClient.TAG;
                Log.e(access$0002, "Not updating BLE scan result sScan " + SemWifiApSmartClient.this.isAdvRunning + " size " + num);
                SemWifiApSmartClient.this.clearLocalResults();
                removeMessages(13);
            }
        }
    }

    class SortList implements Comparator<SemWifiApBleScanResult> {
        SortList() {
        }

        public int compare(SemWifiApBleScanResult m1, SemWifiApBleScanResult m2) {
            int bleRSSI1 = SemWifiApSmartClient.this.getRssiRoundOffValue(m1);
            int bleRSSI2 = SemWifiApSmartClient.this.getRssiRoundOffValue(m2);
            if (bleRSSI1 > bleRSSI2) {
                String access$000 = SemWifiApSmartClient.TAG;
                Log.d(access$000, "compare() - bleRSSI1 > bleRSSI2, (" + bleRSSI1 + ">" + bleRSSI2 + ")");
                return -1;
            } else if (bleRSSI1 < bleRSSI2) {
                String access$0002 = SemWifiApSmartClient.TAG;
                Log.d(access$0002, "compare() - bleRSSI1 < bleRSSI2, (" + bleRSSI1 + "<" + bleRSSI2 + ")");
                return 1;
            } else {
                String access$0003 = SemWifiApSmartClient.TAG;
                Log.d(access$0003, "compare() - m1.mSSID < m2.mSSID, (" + m1.mSSID + " is compared with " + m2.mSSID + ")");
                return m1.mSSID.compareTo(m2.mSSID);
            }
        }
    }

    /* access modifiers changed from: private */
    public int getRssiRoundOffValue(SemWifiApBleScanResult ble) {
        int level;
        if (ble.mBLERssi >= -60) {
            level = -60;
        } else if (ble.mBLERssi >= -70) {
            level = -70;
        } else if (ble.mBLERssi >= -80) {
            level = -80;
        } else if (ble.mBLERssi >= -90) {
            level = -90;
        } else {
            level = -100;
        }
        String str = TAG;
        Log.d(str, "getRssiRoundOffValue() - SSID: " + ble.mSSID + "`s BLERssi value internally is set from " + ble.mBLERssi + " to " + level);
        return level;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x009a, code lost:
        r1 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> getWifiApBleScanResults() {
        /*
            r9 = this;
            monitor-enter(r9)
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r0 = r9.mSemWifiApBleScanResults     // Catch:{ all -> 0x009c }
            monitor-enter(r0)     // Catch:{ all -> 0x009c }
            java.util.ArrayList r1 = new java.util.ArrayList     // Catch:{ all -> 0x0097 }
            r1.<init>()     // Catch:{ all -> 0x0097 }
            java.util.ArrayList r2 = new java.util.ArrayList     // Catch:{ all -> 0x0097 }
            r2.<init>()     // Catch:{ all -> 0x0097 }
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartClient$SortList r3 = new com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartClient$SortList     // Catch:{ all -> 0x0097 }
            r3.<init>()     // Catch:{ all -> 0x0097 }
            java.lang.String r4 = TAG     // Catch:{ all -> 0x0097 }
            java.lang.String r5 = "getWifiApBleScanResults() - BLE scan result sort based on Signal Strength and alphabetical order start."
            android.util.Log.i(r4, r5)     // Catch:{ all -> 0x0097 }
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r4 = r9.mSemWifiApBleScanResults     // Catch:{ all -> 0x0097 }
            java.util.Collections.sort(r4, r3)     // Catch:{ all -> 0x0097 }
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r4 = r9.mSemWifiApBleScanResults     // Catch:{ all -> 0x0097 }
            java.util.Iterator r4 = r4.iterator()     // Catch:{ all -> 0x0097 }
        L_0x0025:
            boolean r5 = r4.hasNext()     // Catch:{ all -> 0x0097 }
            if (r5 == 0) goto L_0x0083
            java.lang.Object r5 = r4.next()     // Catch:{ all -> 0x0097 }
            com.samsung.android.net.wifi.SemWifiApBleScanResult r5 = (com.samsung.android.net.wifi.SemWifiApBleScanResult) r5     // Catch:{ all -> 0x0097 }
            int r6 = r5.mBattery     // Catch:{ all -> 0x0097 }
            r7 = 15
            if (r6 > r7) goto L_0x005d
            java.lang.String r6 = TAG     // Catch:{ all -> 0x0097 }
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ all -> 0x0097 }
            r7.<init>()     // Catch:{ all -> 0x0097 }
            java.lang.String r8 = "getWifiApBleScanResults() - adding ble to lowBatteryAccessPoints, wifiMac: "
            r7.append(r8)     // Catch:{ all -> 0x0097 }
            java.lang.String r8 = r5.mWifiMac     // Catch:{ all -> 0x0097 }
            r7.append(r8)     // Catch:{ all -> 0x0097 }
            java.lang.String r8 = ", SSID: "
            r7.append(r8)     // Catch:{ all -> 0x0097 }
            java.lang.String r8 = r5.mSSID     // Catch:{ all -> 0x0097 }
            r7.append(r8)     // Catch:{ all -> 0x0097 }
            java.lang.String r7 = r7.toString()     // Catch:{ all -> 0x0097 }
            android.util.Log.i(r6, r7)     // Catch:{ all -> 0x0097 }
            r2.add(r5)     // Catch:{ all -> 0x0097 }
            goto L_0x0082
        L_0x005d:
            java.lang.String r6 = TAG     // Catch:{ all -> 0x0097 }
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ all -> 0x0097 }
            r7.<init>()     // Catch:{ all -> 0x0097 }
            java.lang.String r8 = "getWifiApBleScanResults() - adding ble to normalAccessPoints, wifiMac: "
            r7.append(r8)     // Catch:{ all -> 0x0097 }
            java.lang.String r8 = r5.mWifiMac     // Catch:{ all -> 0x0097 }
            r7.append(r8)     // Catch:{ all -> 0x0097 }
            java.lang.String r8 = ", SSID: "
            r7.append(r8)     // Catch:{ all -> 0x0097 }
            java.lang.String r8 = r5.mSSID     // Catch:{ all -> 0x0097 }
            r7.append(r8)     // Catch:{ all -> 0x0097 }
            java.lang.String r7 = r7.toString()     // Catch:{ all -> 0x0097 }
            android.util.Log.i(r6, r7)     // Catch:{ all -> 0x0097 }
            r1.add(r5)     // Catch:{ all -> 0x0097 }
        L_0x0082:
            goto L_0x0025
        L_0x0083:
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r4 = r9.mSemWifiApBleScanResults     // Catch:{ all -> 0x0097 }
            r4.clear()     // Catch:{ all -> 0x0097 }
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r4 = r9.mSemWifiApBleScanResults     // Catch:{ all -> 0x0097 }
            r4.addAll(r1)     // Catch:{ all -> 0x0097 }
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r4 = r9.mSemWifiApBleScanResults     // Catch:{ all -> 0x0097 }
            r4.addAll(r2)     // Catch:{ all -> 0x0097 }
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r4 = r9.mSemWifiApBleScanResults     // Catch:{ all -> 0x0097 }
            monitor-exit(r0)     // Catch:{ all -> 0x0097 }
            monitor-exit(r9)
            return r4
        L_0x0097:
            r1 = move-exception
        L_0x0098:
            monitor-exit(r0)     // Catch:{ all -> 0x009a }
            throw r1     // Catch:{ all -> 0x009c }
        L_0x009a:
            r1 = move-exception
            goto L_0x0098
        L_0x009c:
            r0 = move-exception
            monitor-exit(r9)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartClient.getWifiApBleScanResults():java.util.List");
    }

    private byte[] getclientAdvManufactureData() {
        byte[] data = new byte[24];
        for (int i = 0; i < 24; i++) {
            data[i] = 0;
        }
        data[0] = 1;
        data[1] = 18;
        long mguid = this.mSemWifiApSmartUtil.getHashbasedonGuid();
        long familyID = this.mSemWifiApSmartUtil.getHashbasedonFamilyId();
        long mD2DFamilyID = this.mSemWifiApSmartUtil.getHashbasedonD2DFamilyid();
        if (mguid != -1) {
            byte[] guidBytes = SemWifiApSmartUtil.bytesFromLong(Long.valueOf(mguid));
            for (int i2 = 0; i2 < 4; i2++) {
                data[i2 + 2] = guidBytes[i2];
            }
        }
        if (familyID != -1) {
            byte[] familyBytes = SemWifiApSmartUtil.bytesFromLong(Long.valueOf(familyID));
            for (int i3 = 0; i3 < 4; i3++) {
                data[i3 + 2 + 4] = familyBytes[i3];
            }
        } else if (mguid == -1 && mD2DFamilyID != -1) {
            byte[] familyBytes2 = SemWifiApSmartUtil.bytesFromLong(Long.valueOf(mD2DFamilyID));
            for (int i4 = 0; i4 < 4; i4++) {
                data[i4 + 2 + 4] = familyBytes2[i4];
            }
        }
        data[10] = 1;
        return data;
    }

    private String getBssid() {
        WifiInfo wifiInfo = this.mwifiInfo;
        if (wifiInfo == null || wifiInfo.getBSSID() == null) {
            return null;
        }
        return this.mwifiInfo.getBSSID();
    }

    private boolean islegacy(String ScanResultMAC) {
        int state;
        WifiManager mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (mWifiManager == null || ScanResultMAC == null || (state = mWifiManager.getSmartApConnectedStatusFromScanResult(ScanResultMAC)) != 3) {
            return true;
        }
        String str = TAG;
        Log.d(str, "islegacy state" + state + " ScanResultMAC " + ScanResultMAC);
        return false;
    }

    /* access modifiers changed from: package-private */
    public void clearLocalResults() {
        synchronized (this.mSemWifiApBleScanResults) {
            this.mSemWifiApBleScanResults.clear();
            this.mSmartMHSDevices.clear();
        }
    }

    /* access modifiers changed from: private */
    public void updateLocalResults() {
        synchronized (this.mSemWifiApBleScanResults) {
            List<Integer> mInt = new ArrayList<>();
            int i = 0;
            long curTime = System.currentTimeMillis();
            for (SemWifiApBleScanResult res : this.mSemWifiApBleScanResults) {
                if (curTime - res.mTimeStamp > 20000) {
                    mInt.add(Integer.valueOf(i));
                }
                i++;
            }
            try {
                for (Integer intValue : mInt) {
                    int j = intValue.intValue();
                    String mhsmac = this.mSemWifiApBleScanResults.get(j).mWifiMac;
                    if (mhsmac != null) {
                        String str = TAG;
                        Log.d(str, "removed BLE scan result data:" + mhsmac);
                        LocalLog localLog = this.mLocalLog;
                        localLog.log(TAG + ":\tremoved BLE scan result data:" + mhsmac);
                        this.mSmartMHSDevices.remove(mhsmac);
                    }
                    this.mSemWifiApBleScanResults.remove(j);
                }
            } catch (IndexOutOfBoundsException e) {
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setBLEPairingFailedHistory(String mhsmac, Pair<Long, String> mpair) {
        this.mBLEPairingFailedHashMap.put(mhsmac, mpair);
        String str = TAG;
        Log.d(str, "setBLEPairingFailedHistory:" + mhsmac + ",time:" + mpair.first + ",BLE mac:" + ((String) mpair.second));
        LocalLog localLog = this.mLocalLog;
        localLog.log(TAG + "\t:setBLEPairingFailedHistory:" + mhsmac + ",time:" + mpair.first + ",BLE mac:" + ((String) mpair.second));
    }

    /* access modifiers changed from: package-private */
    public boolean getBLEPairingFailedHistory(String mac) {
        boolean ret = false;
        if (this.mBLEPairingFailedHashMap.get(mac) != null) {
            ret = true;
        }
        String str = TAG;
        Log.d(str, "getBLEPairingFailedHistory:" + mac);
        LocalLog localLog = this.mLocalLog;
        localLog.log(TAG + "\t:getBLEPairingFailedHistory:" + mac);
        return ret;
    }

    /* access modifiers changed from: package-private */
    public void removeFromScanResults(int type, String address) {
        synchronized (this.mSemWifiApBleScanResults) {
            int i = 0;
            boolean found = false;
            Iterator<SemWifiApBleScanResult> it = this.mSemWifiApBleScanResults.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                SemWifiApBleScanResult res = it.next();
                i++;
                if (type != 1 || !res.mDevice.equalsIgnoreCase(address)) {
                    if (type == 2 && res.mWifiMac.equalsIgnoreCase(address)) {
                        found = true;
                        break;
                    }
                } else {
                    found = true;
                    break;
                }
            }
            if (found) {
                if (type == 2) {
                    try {
                        this.mSmartMHSDevices.remove(address);
                    } catch (Exception e) {
                    }
                }
                this.mSemWifiApBleScanResults.remove(i - 1);
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("removed at index:");
                sb.append(i - 1);
                sb.append(",mSemWifiApBleScanResults.size():");
                sb.append(this.mSemWifiApBleScanResults.size());
                Log.d(str, sb.toString());
                LocalLog localLog = this.mLocalLog;
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TAG);
                sb2.append("\t:removed at index:");
                sb2.append(i - 1);
                sb2.append(",mSemWifiApBleScanResults.size():");
                sb2.append(this.mSemWifiApBleScanResults.size());
                localLog.log(sb2.toString());
            }
        }
    }

    /* access modifiers changed from: package-private */
    public String usernameFromScanResults(int type, String address) {
        String username;
        synchronized (this.mSemWifiApBleScanResults) {
            int i = 0;
            username = null;
            Iterator<SemWifiApBleScanResult> it = this.mSemWifiApBleScanResults.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                SemWifiApBleScanResult res = it.next();
                i++;
                if (type != 1 || !res.mDevice.equalsIgnoreCase(address)) {
                    if (type == 2 && res.mWifiMac.equalsIgnoreCase(address)) {
                        username = res.mUserName;
                        break;
                    }
                } else {
                    username = res.mUserName;
                    break;
                }
            }
            String str = TAG;
            Log.d(str, "usernameFromScanResults: " + username + " address: " + address);
        }
        return username;
    }

    private void addScanResults(SemWifiApBleScanResult o) {
        synchronized (this.mSemWifiApBleScanResults) {
            this.mSemWifiApBleScanResults.add(o);
        }
    }

    /* access modifiers changed from: package-private */
    public int checkPreConditions() {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (this.mBluetoothAdapter == null) {
            Log.e(TAG, "mBluetoothAdapter==null");
            LocalLog localLog = this.mLocalLog;
            localLog.log(TAG + ":\t mBluetoothAdapter==null");
            return -7;
        } else if (this.isJDMDevice && SemWifiApMacInfo.getInstance().readWifiMacInfo() == null) {
            Log.e(TAG, "JDM MAC address is null");
            LocalLog localLog2 = this.mLocalLog;
            localLog2.log(TAG + ":\t JDM MAC address is null");
            return -6;
        } else if (!this.mSemWifiApSmartUtil.isPackageExists("com.sec.mhs.smarttethering")) {
            Log.e(TAG, "isPackageExists smarttethering == null");
            setWifiApSmartClient(false);
            return -1;
        } else if (((WifiManager) this.mContext.getSystemService("wifi")).getWifiState() != 3) {
            Log.e(TAG, "not starting scanning ,due to Wi-Fi is OFF");
            return -3;
        } else {
            this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothAdapter bluetoothAdapter = this.mBluetoothAdapter;
            if (bluetoothAdapter == null || !bluetoothAdapter.semIsBleEnabled()) {
                LocalLog localLog3 = this.mLocalLog;
                localLog3.log(TAG + ":\t  Preconditions BLE is OFF");
                Log.i(TAG, "Preconditions BLE is OFF");
                SemEmergencyManager em = SemEmergencyManager.getInstance(this.mContext);
                if (em == null || !em.isEmergencyMode()) {
                    boolean isAirplaneMode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0;
                    if (isAirplaneMode) {
                        String str = TAG;
                        Log.d(str, "getAirplaneMode: " + isAirplaneMode);
                        return -5;
                    } else if (this.mSemWifiApSmartUtil.isNearByAutohotspotEnabled()) {
                        return 0;
                    } else {
                        Log.d(TAG, "not isNearByAutohotspotEnabled");
                        LocalLog localLog4 = this.mLocalLog;
                        localLog4.log(TAG + ":\t not isNearByAutohotspotEnabled");
                        return -8;
                    }
                } else {
                    Log.i(TAG, "Do not setWifiApSmartClient in EmergencyMode");
                    return -4;
                }
            } else {
                Log.i(TAG, "Preconditions BLE is ON");
                return 0;
            }
        }
    }

    public void startWifiApSmartClientAdvertize() {
        if (FactoryTest.isFactoryBinary()) {
            Log.e(TAG, "This devices's binary is a factory binary");
            return;
        }
        int status = checkPreConditions();
        if (!this.isAdvRunning && status == 0) {
            this.mHashBasedGuid = Long.valueOf(this.mSemWifiApSmartUtil.getHashbasedonGuid());
            this.mHashBasedFamilyID = Long.valueOf(this.mSemWifiApSmartUtil.getHashbasedonFamilyId());
            this.mHashBasedD2DFamilyID = Long.valueOf(this.mSemWifiApSmartUtil.getHashbasedonD2DFamilyid());
            if (this.mHashBasedGuid.longValue() == -1 && this.mHashBasedD2DFamilyID.longValue() == -1) {
                Log.e(TAG, "mHashBasedGuid == null and mHashBasedD2DFamilyID is -1");
                return;
            }
            this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (this.mSemWifiApSmartUtil.isNearByAutohotspotEnabled() && !this.mBluetoothAdapter.semIsBleEnabled()) {
                this.mBluetoothAdapter.semSetStandAloneBleMode(true);
            }
            this.mBluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
            if (this.mBluetoothLeAdvertiser == null) {
                this.isStartAdvPending = true;
                Log.e(TAG, "mBluetoothLeScanner == null, waiting for isStartAdvPending");
                return;
            }
            this.isAdvRunning = true;
            byte[] tClientData = getclientAdvManufactureData();
            String str = TAG;
            Log.d(str, ": Client startWifiApSmartClientAdvertize,mHashBasedGuid:" + this.mHashBasedGuid + ",mHashBasedFamilyID:" + this.mHashBasedFamilyID + ",mHashBasedD2DFamilyID:" + this.mHashBasedD2DFamilyID + "," + Arrays.toString(tClientData));
            LocalLog localLog = this.mLocalLog;
            localLog.log(TAG + ":\tClient startWifiApSmartClientAdvertize,mHashBasedGuid:" + this.mHashBasedGuid + ",mHashBasedFamilyID:" + this.mHashBasedFamilyID + ",mHashBasedD2DFamilyID:" + this.mHashBasedD2DFamilyID + "," + Arrays.toString(tClientData));
            AdvertiseSettings settings = new AdvertiseSettings.Builder().setAdvertiseMode(2).setConnectable(true).setTimeout(0).setTxPowerLevel(3).build();
            AdvertiseData.Builder builder = new AdvertiseData.Builder();
            SemWifiApSmartUtil semWifiApSmartUtil = this.mSemWifiApSmartUtil;
            this.mBluetoothLeAdvertiser.startAdvertising(settings, builder.addManufacturerData(SemWifiApSmartUtil.MANUFACTURE_ID, tClientData).build(), this.mAdvertiseCallback);
        }
    }

    public void stopWifiApSmartClientAdvertize() {
        AdvertiseCallback advertiseCallback;
        Log.d(TAG, "stopWifiApSmartClientAdvertize");
        if (this.isAdvRunning) {
            BluetoothLeAdvertiser bluetoothLeAdvertiser = this.mBluetoothLeAdvertiser;
            if (!(bluetoothLeAdvertiser == null || (advertiseCallback = this.mAdvertiseCallback) == null)) {
                bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            }
            this.isAdvRunning = false;
            LocalLog localLog = this.mLocalLog;
            localLog.log(TAG + ":\tstopWifiApSmartClientAdvertize");
        }
    }
}
