package com.samsung.android.server.wifi.softap.smarttethering;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.WifiInjector;
import com.samsung.android.emergencymode.SemEmergencyManager;
import com.samsung.android.net.wifi.SemWifiApBleScanResult;
import com.samsung.android.net.wifi.SemWifiApMacInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SemWifiApSmartD2DMHS {
    /* access modifiers changed from: private */
    public static String TAG = "SemWifiApSmartD2DMHS";
    private static IntentFilter mSemWifiApSmartD2DMHSIntentFilter = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
    private final int CLIENT_BLE_UPDATE_SCAN_DATA_INTERVAL = 20000;
    private final int RESTART_ADVERTISE = 4;
    public final int START_ADVERTISE = 1;
    public final int STOP_ADVERTISE = 2;
    private final int UPDATE_BLE_SCAN_RESULT = 3;
    /* access modifiers changed from: private */
    public boolean isAdvRunning;
    private boolean isJDMDevice = "in_house".contains("jdm");
    /* access modifiers changed from: private */
    public boolean isStartAdvPending;
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(SemWifiApSmartD2DMHS.TAG, "MHS D2D Advertise Started.");
        }

        public void onStartFailure(int errorCode) {
            String access$100 = SemWifiApSmartD2DMHS.TAG;
            Log.e(access$100, "MHS D2D Advertise Failed: " + errorCode + ",restarting after 1 sec");
            LocalLog access$200 = SemWifiApSmartD2DMHS.this.mLocalLog;
            access$200.log(SemWifiApSmartD2DMHS.TAG + ":\tMHS D2D Advertise Failed: " + errorCode + ",restarting after 1 sec");
            boolean unused = SemWifiApSmartD2DMHS.this.isAdvRunning = false;
            if (SemWifiApSmartD2DMHS.this.mBleWorkHandler != null) {
                SemWifiApSmartD2DMHS.this.mBleWorkHandler.sendEmptyMessageDelayed(4, 1000);
            }
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
    /* access modifiers changed from: private */
    public LocalLog mLocalLog;
    /* access modifiers changed from: private */
    public List<SemWifiApBleScanResult> mSemWifiApBleScanResults = new ArrayList();
    private SemWifiApSmartD2DMHSReceiver mSemWifiApSmartD2DMHSReceiver;
    private SemWifiApSmartUtil mSemWifiApSmartUtil;
    private Set<String> mSmartD2DClientDevices = new HashSet();

    static {
        mSemWifiApSmartD2DMHSIntentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        mSemWifiApSmartD2DMHSIntentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        mSemWifiApSmartD2DMHSIntentFilter.addAction("com.samsung.bluetooth.adapter.action.BLE_STATE_CHANGED");
        mSemWifiApSmartD2DMHSIntentFilter.addAction("com.samsung.android.server.wifi.softap.smarttethering.startD2DMHS");
    }

    public SemWifiApSmartD2DMHS(Context context, SemWifiApSmartUtil tSemWifiApSmartUtil, LocalLog tLocalLog) {
        this.mSemWifiApSmartUtil = tSemWifiApSmartUtil;
        this.mLocalLog = tLocalLog;
        this.mContext = context;
        this.mSemWifiApSmartD2DMHSReceiver = new SemWifiApSmartD2DMHSReceiver();
        if (!FactoryTest.isFactoryBinary()) {
            this.mContext.registerReceiver(this.mSemWifiApSmartD2DMHSReceiver, mSemWifiApSmartD2DMHSIntentFilter);
        } else {
            Log.e(TAG, "This devices's binary is a factory binary");
        }
    }

    public void handleBootCompleted() {
        Log.d(TAG, "handleBootCompleted");
        LocalLog localLog = this.mLocalLog;
        localLog.log(TAG + ":\t handleBootCompleted");
        this.mBleWorkThread = new HandlerThread("SemWifiApSmartD2DMHSBleHandler");
        this.mBleWorkThread.start();
        this.mBleWorkHandler = new BleWorkHandler(this.mBleWorkThread.getLooper());
    }

    class SemWifiApSmartD2DMHSReceiver extends BroadcastReceiver {
        SemWifiApSmartD2DMHSReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean z = true;
            if (action.equals("com.samsung.android.server.wifi.softap.smarttethering.startD2DMHS")) {
                if (intent.getIntExtra("status", -1) == 1) {
                    SemWifiApSmartD2DMHS.this.sendEmptyMessage(1);
                } else {
                    SemWifiApSmartD2DMHS.this.sendEmptyMessage(2);
                }
            } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (Settings.Global.getInt(SemWifiApSmartD2DMHS.this.mContext.getContentResolver(), "airplane_mode_on", 0) == 0) {
                    z = false;
                }
                boolean isAirplaneMode = z;
                Log.d(SemWifiApSmartD2DMHS.TAG, "isAirplaneMode:" + isAirplaneMode);
                SemWifiApSmartD2DMHS.this.mLocalLog.log(SemWifiApSmartD2DMHS.TAG + ":AIRPLANE_MODE" + isAirplaneMode);
                if (isAirplaneMode) {
                    SemWifiApSmartD2DMHS.this.sendEmptyMessage(2);
                }
            } else if (action.equals("com.samsung.bluetooth.adapter.action.BLE_STATE_CHANGED")) {
                int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
                if (SemWifiApSmartD2DMHS.this.isAdvRunning) {
                    boolean unused = SemWifiApSmartD2DMHS.this.isStartAdvPending = false;
                }
                if (SemWifiApSmartD2DMHS.this.isStartAdvPending && state == 15) {
                    boolean unused2 = SemWifiApSmartD2DMHS.this.isStartAdvPending = false;
                    Log.d(SemWifiApSmartD2DMHS.TAG, "BLE is ON, starting advertizement");
                    SemWifiApSmartD2DMHS.this.mLocalLog.log(SemWifiApSmartD2DMHS.TAG + ":\t BLE is ON, starting advertizement");
                    SemWifiApSmartD2DMHS.this.sendEmptyMessage(1);
                }
            } else if (action.equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                boolean emergencyMode = intent.getBooleanExtra("phoneinECMState", false);
                Log.d(SemWifiApSmartD2DMHS.TAG, "emergencyMode:" + emergencyMode);
                SemWifiApSmartD2DMHS.this.mLocalLog.log(SemWifiApSmartD2DMHS.TAG + ": EMERGENCY" + emergencyMode);
                if (emergencyMode) {
                    SemWifiApSmartD2DMHS.this.sendEmptyMessage(2);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public int checkPreConditions() {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (this.mBluetoothAdapter == null) {
            Log.e(TAG, "mBluetoothAdapter==null");
            LocalLog localLog = this.mLocalLog;
            localLog.log(TAG + ":\t mBluetoothAdapter==null");
            return -5;
        } else if (this.isJDMDevice && SemWifiApMacInfo.getInstance().readWifiMacInfo() == null) {
            Log.e(TAG, "JDM MAC address is null");
            LocalLog localLog2 = this.mLocalLog;
            localLog2.log(TAG + ":\t JDM MAC address is null");
            return -4;
        } else if (!this.mSemWifiApSmartUtil.isPackageExists("com.sec.mhs.smarttethering")) {
            Log.e(TAG, "isPackageExists smarttethering == null");
            return -1;
        } else {
            this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothAdapter bluetoothAdapter = this.mBluetoothAdapter;
            if (bluetoothAdapter == null || !bluetoothAdapter.semIsBleEnabled()) {
                SemEmergencyManager em = SemEmergencyManager.getInstance(this.mContext);
                if (em == null || !em.isEmergencyMode()) {
                    boolean isAirplaneMode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0;
                    if (isAirplaneMode) {
                        String str = TAG;
                        Log.d(str, "getAirplaneMode: " + isAirplaneMode);
                        return -3;
                    } else if (this.mSemWifiApSmartUtil.isNearByAutohotspotEnabled()) {
                        return 0;
                    } else {
                        Log.d(TAG, "not isNearByAutohotspotEnabled");
                        LocalLog localLog3 = this.mLocalLog;
                        localLog3.log(TAG + ":\t not isNearByAutohotspotEnabled");
                        return -6;
                    }
                } else {
                    Log.i(TAG, "Do not setWifiApSmartClient in EmergencyMode");
                    return -2;
                }
            } else {
                Log.i(TAG, "Preconditions BLE is ON");
                LocalLog localLog4 = this.mLocalLog;
                localLog4.log(TAG + ":\t  Preconditions BLE is ON");
                return 0;
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
            if (i == 1) {
                int status = SemWifiApSmartD2DMHS.this.checkPreConditions();
                if (status == 0) {
                    SemWifiApSmartD2DMHS.this.clearLocalResults();
                    Settings.Secure.putInt(SemWifiApSmartD2DMHS.this.mContext.getContentResolver(), "wifi_ap_smart_d2d_mhs", 1);
                    SemWifiApSmartD2DMHS.this.startWifiApSmartD2DMHSAdvertize();
                    sendEmptyMessageDelayed(3, 20000);
                    return;
                }
                String access$100 = SemWifiApSmartD2DMHS.TAG;
                Log.e(access$100, "checkPreConditions failed " + status);
                LocalLog access$200 = SemWifiApSmartD2DMHS.this.mLocalLog;
                access$200.log(SemWifiApSmartD2DMHS.TAG + ":\tcheckPreConditions failed " + status);
            } else if (i == 2) {
                Settings.Secure.putInt(SemWifiApSmartD2DMHS.this.mContext.getContentResolver(), "wifi_ap_smart_d2d_mhs", 0);
                SemWifiApSmartD2DMHS.this.stopWifiApSmartD2DMHSAdvertize();
                SemWifiApSmartD2DMHS.this.clearLocalResults();
            } else if (i == 3) {
                synchronized (SemWifiApSmartD2DMHS.this.mSemWifiApBleScanResults) {
                    num = SemWifiApSmartD2DMHS.this.mSemWifiApBleScanResults.size();
                }
                if (SemWifiApSmartD2DMHS.this.isAdvRunning || num != 0) {
                    SemWifiApSmartD2DMHS.this.updateLocalResults();
                    sendEmptyMessageDelayed(3, 20000);
                    return;
                }
                String access$1002 = SemWifiApSmartD2DMHS.TAG;
                Log.e(access$1002, "Not updating BLE scan result isAdvRunning " + SemWifiApSmartD2DMHS.this.isAdvRunning + " ,size: " + num);
                SemWifiApSmartD2DMHS.this.clearLocalResults();
                removeMessages(3);
            } else if (i == 4 && Settings.Secure.getInt(SemWifiApSmartD2DMHS.this.mContext.getContentResolver(), "wifi_ap_smart_d2d_mhs", 0) == 1) {
                int status2 = SemWifiApSmartD2DMHS.this.checkPreConditions();
                if (status2 == 0) {
                    SemWifiApSmartD2DMHS.this.clearLocalResults();
                    SemWifiApSmartD2DMHS.this.startWifiApSmartD2DMHSAdvertize();
                    sendEmptyMessageDelayed(3, 20000);
                    return;
                }
                String access$1003 = SemWifiApSmartD2DMHS.TAG;
                Log.e(access$1003, "checkPreConditions failed " + status2);
                LocalLog access$2002 = SemWifiApSmartD2DMHS.this.mLocalLog;
                access$2002.log(SemWifiApSmartD2DMHS.TAG + ":\tcheckPreConditions failed " + status2);
            }
        }
    }

    public boolean semWifiApBleD2DMhsRole(boolean enable) {
        if (enable) {
            sendEmptyMessage(1);
        } else if (!enable) {
            sendEmptyMessage(2);
        }
        return true;
    }

    public void sendEmptyMessage(int val) {
        BleWorkHandler bleWorkHandler = this.mBleWorkHandler;
        if (bleWorkHandler != null) {
            bleWorkHandler.sendEmptyMessage(val);
        }
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:112:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x041d, code lost:
        if (r10.equalsIgnoreCase(r12.mDevice) == false) goto L_0x0445;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x0425, code lost:
        if (r13.equalsIgnoreCase(r12.mSSID) == false) goto L_0x0445;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x0427, code lost:
        if (r14 != null) goto L_0x042b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x0429, code lost:
        if (r4 != null) goto L_0x0445;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x042b, code lost:
        if (r14 == null) goto L_0x042f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x042d, code lost:
        if (r4 == null) goto L_0x0445;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:0x042f, code lost:
        if (r14 == null) goto L_0x043d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x0431, code lost:
        if (r4 == null) goto L_0x043d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x0437, code lost:
        if (r14.equals(r4) != false) goto L_0x043a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x043a, code lost:
        r16 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x043d, code lost:
        r16 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x0440, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x0441, code lost:
        r16 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:85:?, code lost:
        r1.mSemWifiApSmartUtil.sendClientScanResultUpdateIntent("D2DMHS ScanResult event");
        r0 = TAG;
        r15 = new java.lang.StringBuilder();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:86:0x0453, code lost:
        r16 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:88:?, code lost:
        r15.append("updating mClientDeviceName:");
        r15.append(r13);
        r15.append(",BT_MAC:");
        r15.append(r10);
        r15.append(",mD2D_ClientMAC");
        r15.append(r5);
        android.util.Log.d(r0, r15.toString());
        r1.mLocalLog.log(TAG + "\t updating mClientDeviceName:" + r13 + ",BT_MAC:" + r10 + ",mD2D_ClientMAC" + r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:89:0x049f, code lost:
        r12.mUserName = r4;
        r12.mDevice = r10;
        r12.mSSID = r13;
        r12.mTimeStamp = java.lang.System.currentTimeMillis();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void sendScanResultFromScanner(android.bluetooth.le.ScanResult r33) {
        /*
            r32 = this;
            r1 = r32
            android.bluetooth.le.ScanRecord r2 = r33.getScanRecord()
            r0 = 117(0x75, float:1.64E-43)
            byte[] r3 = r2.getManufacturerSpecificData(r0)
            r0 = 0
            r4 = 2
            byte r5 = r3[r4]
            r5 = r5 & 240(0xf0, float:3.36E-43)
            r6 = 4
            int r5 = r5 >> r6
            r7 = 16
            char r5 = java.lang.Character.forDigit(r5, r7)
            java.lang.String r0 = java.lang.Character.toString(r5)
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            byte r8 = r3[r4]
            r8 = r8 & 15
            char r8 = java.lang.Character.forDigit(r8, r7)
            java.lang.String r8 = java.lang.Character.toString(r8)
            r5.append(r8)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            java.lang.String r8 = ":"
            r5.append(r8)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            r8 = 3
            byte r9 = r3[r8]
            r9 = r9 & 240(0xf0, float:3.36E-43)
            int r9 = r9 >> r6
            char r9 = java.lang.Character.forDigit(r9, r7)
            java.lang.String r9 = java.lang.Character.toString(r9)
            r5.append(r9)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            byte r9 = r3[r8]
            r9 = r9 & 15
            char r9 = java.lang.Character.forDigit(r9, r7)
            java.lang.String r9 = java.lang.Character.toString(r9)
            r5.append(r9)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            java.lang.String r9 = ":"
            r5.append(r9)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            byte r9 = r3[r6]
            r9 = r9 & 240(0xf0, float:3.36E-43)
            int r9 = r9 >> r6
            char r9 = java.lang.Character.forDigit(r9, r7)
            java.lang.String r9 = java.lang.Character.toString(r9)
            r5.append(r9)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            byte r9 = r3[r6]
            r9 = r9 & 15
            char r9 = java.lang.Character.forDigit(r9, r7)
            java.lang.String r9 = java.lang.Character.toString(r9)
            r5.append(r9)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            java.lang.String r9 = ":"
            r5.append(r9)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            r9 = 5
            byte r10 = r3[r9]
            r10 = r10 & 240(0xf0, float:3.36E-43)
            int r10 = r10 >> r6
            char r10 = java.lang.Character.forDigit(r10, r7)
            java.lang.String r10 = java.lang.Character.toString(r10)
            r5.append(r10)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            byte r9 = r3[r9]
            r9 = r9 & 15
            char r9 = java.lang.Character.forDigit(r9, r7)
            java.lang.String r9 = java.lang.Character.toString(r9)
            r5.append(r9)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            java.lang.String r9 = ":"
            r5.append(r9)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            r9 = 6
            byte r10 = r3[r9]
            r10 = r10 & 240(0xf0, float:3.36E-43)
            int r10 = r10 >> r6
            char r10 = java.lang.Character.forDigit(r10, r7)
            java.lang.String r10 = java.lang.Character.toString(r10)
            r5.append(r10)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            byte r10 = r3[r9]
            r10 = r10 & 15
            char r10 = java.lang.Character.forDigit(r10, r7)
            java.lang.String r10 = java.lang.Character.toString(r10)
            r5.append(r10)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            java.lang.String r10 = ":"
            r5.append(r10)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            r10 = 7
            byte r11 = r3[r10]
            r11 = r11 & 240(0xf0, float:3.36E-43)
            int r11 = r11 >> r6
            char r11 = java.lang.Character.forDigit(r11, r7)
            java.lang.String r11 = java.lang.Character.toString(r11)
            r5.append(r11)
            java.lang.String r0 = r5.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            byte r10 = r3[r10]
            r10 = r10 & 15
            char r7 = java.lang.Character.forDigit(r10, r7)
            java.lang.String r7 = java.lang.Character.toString(r7)
            r5.append(r7)
            java.lang.String r0 = r5.toString()
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r5 = r1.mSemWifiApSmartUtil
            java.lang.String r5 = r5.getActualMACFrom_mappedMAC(r0)
            android.bluetooth.BluetoothDevice r0 = r33.getDevice()
            java.lang.String r7 = r0.getAddress()
            java.util.Map<java.lang.String, android.util.Pair<java.lang.Long, java.lang.String>> r0 = r1.mBLEPairingFailedHashMap
            java.lang.Object r0 = r0.get(r5)
            r15 = r0
            android.util.Pair r15 = (android.util.Pair) r15
            r10 = -1
            if (r15 == 0) goto L_0x01d0
            long r12 = java.lang.System.currentTimeMillis()
            java.lang.Object r0 = r15.first
            java.lang.Long r0 = (java.lang.Long) r0
            long r16 = r0.longValue()
            long r10 = r12 - r16
            r13 = r10
            goto L_0x01d1
        L_0x01d0:
            r13 = r10
        L_0x01d1:
            if (r15 == 0) goto L_0x0250
            if (r7 == 0) goto L_0x0250
            java.lang.Object r0 = r15.second
            if (r0 == 0) goto L_0x0250
            java.lang.Object r0 = r15.second
            boolean r0 = r7.equals(r0)
            if (r0 == 0) goto L_0x01e8
            r10 = 60000(0xea60, double:2.9644E-319)
            int r0 = (r13 > r10 ? 1 : (r13 == r10 ? 0 : -1))
            if (r0 < 0) goto L_0x01f6
        L_0x01e8:
            java.lang.Object r0 = r15.second
            boolean r0 = r7.equals(r0)
            if (r0 != 0) goto L_0x0250
            r10 = 5000(0x1388, double:2.4703E-320)
            int r0 = (r13 > r10 ? 1 : (r13 == r10 ? 0 : -1))
            if (r0 >= 0) goto L_0x0250
        L_0x01f6:
            java.lang.String r0 = TAG
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r6 = "new mBLE_MAC:"
            r4.append(r6)
            r4.append(r7)
            java.lang.String r6 = ",diff:"
            r4.append(r6)
            r4.append(r13)
            java.lang.String r6 = ",old BLE mac:"
            r4.append(r6)
            java.lang.Object r6 = r15.second
            java.lang.String r6 = (java.lang.String) r6
            r4.append(r6)
            java.lang.String r4 = r4.toString()
            android.util.Log.d(r0, r4)
            android.util.LocalLog r0 = r1.mLocalLog
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r6 = TAG
            r4.append(r6)
            java.lang.String r6 = ":\tnew mBLE_MAC:"
            r4.append(r6)
            r4.append(r7)
            java.lang.String r6 = ",diff:"
            r4.append(r6)
            r4.append(r13)
            java.lang.String r6 = ",old BLE mac:"
            r4.append(r6)
            java.lang.Object r6 = r15.second
            java.lang.String r6 = (java.lang.String) r6
            r4.append(r6)
            java.lang.String r4 = r4.toString()
            r0.log(r4)
            return
        L_0x0250:
            if (r15 == 0) goto L_0x0257
            java.util.Map<java.lang.String, android.util.Pair<java.lang.Long, java.lang.String>> r0 = r1.mBLEPairingFailedHashMap
            r0.remove(r5)
        L_0x0257:
            r0 = 0
            r10 = 0
            r11 = 8
            byte r11 = r3[r11]
            r12 = 0
            r8 = 1
            if (r11 != r8) goto L_0x02c0
            java.lang.String r11 = new java.lang.String
            r9 = 11
            r11.<init>(r3, r9, r6)
            r0 = r11
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r9 = r1.mSemWifiApSmartUtil
            long r18 = r9.getHashbasedonFamilyId()
            r20 = -1
            int r9 = (r18 > r20 ? 1 : (r18 == r20 ? 0 : -1))
            if (r9 == 0) goto L_0x02bd
            java.lang.Long r9 = java.lang.Long.valueOf(r18)
            byte[] r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.bytesFromLong(r9)
            byte[] r11 = new byte[r6]
            byte r20 = r9[r12]
            r11[r12] = r20
            byte r20 = r9[r4]
            r11[r8] = r20
            byte r8 = r9[r6]
            r11[r4] = r8
            r4 = 6
            byte r4 = r9[r4]
            r8 = 3
            r11[r8] = r4
            java.lang.String r4 = new java.lang.String
            r4.<init>(r11, r12, r6)
            r10 = r4
            boolean r4 = r10.equals(r0)
            if (r4 == 0) goto L_0x02ba
            android.net.wifi.SemWifiApSmartWhiteList r4 = android.net.wifi.SemWifiApSmartWhiteList.getInstance()
            boolean r4 = r4.isContains(r5)
            if (r4 == 0) goto L_0x02ba
            java.util.Set<java.lang.String> r4 = r1.mSmartD2DClientDevices
            boolean r4 = r4.contains(r5)
            if (r4 == 0) goto L_0x02b9
            java.lang.String r4 = TAG
            java.lang.String r6 = "Same familyID"
            android.util.Log.i(r4, r6)
            r1.removeFromScanResults(r5)
        L_0x02b9:
            return
        L_0x02ba:
            r4 = r0
            r6 = r10
            goto L_0x02c2
        L_0x02bd:
            r4 = r0
            r6 = r10
            goto L_0x02c2
        L_0x02c0:
            r4 = r0
            r6 = r10
        L_0x02c2:
            r0 = 50
            byte[] r8 = new byte[r0]
            r0 = 0
            r9 = 15
        L_0x02c9:
            r10 = 24
            if (r9 >= r10) goto L_0x02df
            int r10 = r3.length
            if (r9 >= r10) goto L_0x02df
            byte r10 = r3[r9]
            if (r10 != 0) goto L_0x02d5
            goto L_0x02df
        L_0x02d5:
            int r10 = r0 + 1
            byte r11 = r3[r9]
            r8[r0] = r11
            int r9 = r9 + 1
            r0 = r10
            goto L_0x02c9
        L_0x02df:
            r9 = 26
        L_0x02e1:
            r11 = r0
            r0 = 51
            if (r9 >= r0) goto L_0x02f7
            int r0 = r3.length
            if (r9 >= r0) goto L_0x02f7
            byte r0 = r3[r9]
            if (r0 != 0) goto L_0x02ee
            goto L_0x02f7
        L_0x02ee:
            int r0 = r11 + 1
            byte r10 = r3[r9]
            r8[r11] = r10
            int r9 = r9 + 1
            goto L_0x02e1
        L_0x02f7:
            android.util.LocalLog r0 = r1.mLocalLog
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            java.lang.String r12 = TAG
            r10.append(r12)
            java.lang.String r12 = ":\tmScanResultData:"
            r10.append(r12)
            java.lang.String r12 = java.util.Arrays.toString(r3)
            r10.append(r12)
            java.lang.String r10 = r10.toString()
            r0.log(r10)
            java.lang.String r0 = TAG
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            java.lang.String r12 = "mScanResultData:"
            r10.append(r12)
            java.lang.String r12 = java.util.Arrays.toString(r3)
            r10.append(r12)
            java.lang.String r10 = r10.toString()
            android.util.Log.d(r0, r10)
            java.lang.String r0 = new java.lang.String
            r10 = 0
            r0.<init>(r8, r10, r11)
            r12 = r0
            if (r5 == 0) goto L_0x03df
            java.util.Set<java.lang.String> r0 = r1.mSmartD2DClientDevices
            boolean r0 = r0.add(r5)
            if (r0 == 0) goto L_0x03df
            android.bluetooth.BluetoothDevice r0 = r33.getDevice()
            java.lang.String r0 = r0.getAddress()
            com.samsung.android.net.wifi.SemWifiApBleScanResult r25 = new com.samsung.android.net.wifi.SemWifiApBleScanResult
            r16 = 0
            r17 = 0
            r18 = 0
            r19 = 2
            r20 = 0
            r21 = 0
            long r22 = java.lang.System.currentTimeMillis()
            r24 = 0
            r26 = 0
            r10 = r25
            r27 = r11
            r11 = r0
            r28 = r12
            r12 = r16
            r29 = r13
            r13 = r17
            r14 = r18
            r31 = r15
            r15 = r19
            r16 = r5
            r17 = r4
            r18 = r28
            r19 = r20
            r20 = r21
            r21 = r22
            r23 = r24
            r24 = r26
            r10.<init>(r11, r12, r13, r14, r15, r16, r17, r18, r19, r20, r21, r23, r24)
            java.lang.String r11 = TAG
            java.lang.StringBuilder r12 = new java.lang.StringBuilder
            r12.<init>()
            java.lang.String r13 = "adding mClientDeviceName:"
            r12.append(r13)
            r13 = r28
            r12.append(r13)
            java.lang.String r14 = ",BT_MAC:"
            r12.append(r14)
            r12.append(r0)
            java.lang.String r14 = ",mD2D_ClientMAC"
            r12.append(r14)
            r12.append(r5)
            java.lang.String r12 = r12.toString()
            android.util.Log.d(r11, r12)
            android.util.LocalLog r11 = r1.mLocalLog
            java.lang.StringBuilder r12 = new java.lang.StringBuilder
            r12.<init>()
            java.lang.String r14 = TAG
            r12.append(r14)
            java.lang.String r14 = "\tadding mClientDeviceName:"
            r12.append(r14)
            r12.append(r13)
            java.lang.String r14 = ",BT_MAC:"
            r12.append(r14)
            r12.append(r0)
            java.lang.String r14 = ",mD2D_ClientMAC"
            r12.append(r14)
            r12.append(r5)
            java.lang.String r12 = r12.toString()
            r11.log(r12)
            r1.addScanResults(r10)
            r16 = r2
            goto L_0x04c1
        L_0x03df:
            r27 = r11
            r29 = r13
            r31 = r15
            r13 = r12
            if (r5 == 0) goto L_0x04bf
            java.util.Set<java.lang.String> r0 = r1.mSmartD2DClientDevices
            boolean r0 = r0.add(r5)
            if (r0 != 0) goto L_0x04bf
            android.bluetooth.BluetoothDevice r0 = r33.getDevice()
            java.lang.String r10 = r0.getAddress()
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r11 = r1.mSemWifiApBleScanResults
            monitor-enter(r11)
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r0 = r1.mSemWifiApBleScanResults     // Catch:{ all -> 0x04b8 }
            java.util.Iterator r0 = r0.iterator()     // Catch:{ all -> 0x04b8 }
        L_0x0401:
            boolean r12 = r0.hasNext()     // Catch:{ all -> 0x04b8 }
            if (r12 == 0) goto L_0x04b4
            java.lang.Object r12 = r0.next()     // Catch:{ all -> 0x04b8 }
            com.samsung.android.net.wifi.SemWifiApBleScanResult r12 = (com.samsung.android.net.wifi.SemWifiApBleScanResult) r12     // Catch:{ all -> 0x04b8 }
            java.lang.String r14 = r12.mUserName     // Catch:{ all -> 0x04b8 }
            java.lang.String r15 = r12.mWifiMac     // Catch:{ all -> 0x04b8 }
            boolean r15 = r15.equalsIgnoreCase(r5)     // Catch:{ all -> 0x04b8 }
            if (r15 == 0) goto L_0x04ac
            java.lang.String r0 = r12.mDevice     // Catch:{ all -> 0x04b8 }
            boolean r0 = r10.equalsIgnoreCase(r0)     // Catch:{ all -> 0x04b8 }
            if (r0 == 0) goto L_0x0445
            java.lang.String r0 = r12.mSSID     // Catch:{ all -> 0x0440 }
            boolean r0 = r13.equalsIgnoreCase(r0)     // Catch:{ all -> 0x0440 }
            if (r0 == 0) goto L_0x0445
            if (r14 != 0) goto L_0x042b
            if (r4 != 0) goto L_0x0445
        L_0x042b:
            if (r14 == 0) goto L_0x042f
            if (r4 == 0) goto L_0x0445
        L_0x042f:
            if (r14 == 0) goto L_0x043d
            if (r4 == 0) goto L_0x043d
            boolean r0 = r14.equals(r4)     // Catch:{ all -> 0x0440 }
            if (r0 != 0) goto L_0x043a
            goto L_0x0445
        L_0x043a:
            r16 = r2
            goto L_0x049f
        L_0x043d:
            r16 = r2
            goto L_0x049f
        L_0x0440:
            r0 = move-exception
            r16 = r2
            goto L_0x04bb
        L_0x0445:
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r0 = r1.mSemWifiApSmartUtil     // Catch:{ all -> 0x04b8 }
            java.lang.String r15 = "D2DMHS ScanResult event"
            r0.sendClientScanResultUpdateIntent(r15)     // Catch:{ all -> 0x04b8 }
            java.lang.String r0 = TAG     // Catch:{ all -> 0x04b8 }
            java.lang.StringBuilder r15 = new java.lang.StringBuilder     // Catch:{ all -> 0x04b8 }
            r15.<init>()     // Catch:{ all -> 0x04b8 }
            r16 = r2
            java.lang.String r2 = "updating mClientDeviceName:"
            r15.append(r2)     // Catch:{ all -> 0x04bd }
            r15.append(r13)     // Catch:{ all -> 0x04bd }
            java.lang.String r2 = ",BT_MAC:"
            r15.append(r2)     // Catch:{ all -> 0x04bd }
            r15.append(r10)     // Catch:{ all -> 0x04bd }
            java.lang.String r2 = ",mD2D_ClientMAC"
            r15.append(r2)     // Catch:{ all -> 0x04bd }
            r15.append(r5)     // Catch:{ all -> 0x04bd }
            java.lang.String r2 = r15.toString()     // Catch:{ all -> 0x04bd }
            android.util.Log.d(r0, r2)     // Catch:{ all -> 0x04bd }
            android.util.LocalLog r0 = r1.mLocalLog     // Catch:{ all -> 0x04bd }
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x04bd }
            r2.<init>()     // Catch:{ all -> 0x04bd }
            java.lang.String r15 = TAG     // Catch:{ all -> 0x04bd }
            r2.append(r15)     // Catch:{ all -> 0x04bd }
            java.lang.String r15 = "\t updating mClientDeviceName:"
            r2.append(r15)     // Catch:{ all -> 0x04bd }
            r2.append(r13)     // Catch:{ all -> 0x04bd }
            java.lang.String r15 = ",BT_MAC:"
            r2.append(r15)     // Catch:{ all -> 0x04bd }
            r2.append(r10)     // Catch:{ all -> 0x04bd }
            java.lang.String r15 = ",mD2D_ClientMAC"
            r2.append(r15)     // Catch:{ all -> 0x04bd }
            r2.append(r5)     // Catch:{ all -> 0x04bd }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x04bd }
            r0.log(r2)     // Catch:{ all -> 0x04bd }
        L_0x049f:
            r12.mUserName = r4     // Catch:{ all -> 0x04bd }
            r12.mDevice = r10     // Catch:{ all -> 0x04bd }
            r12.mSSID = r13     // Catch:{ all -> 0x04bd }
            long r0 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x04bd }
            r12.mTimeStamp = r0     // Catch:{ all -> 0x04bd }
            goto L_0x04b6
        L_0x04ac:
            r16 = r2
            r1 = r32
            r2 = r16
            goto L_0x0401
        L_0x04b4:
            r16 = r2
        L_0x04b6:
            monitor-exit(r11)     // Catch:{ all -> 0x04bd }
            goto L_0x04c1
        L_0x04b8:
            r0 = move-exception
            r16 = r2
        L_0x04bb:
            monitor-exit(r11)     // Catch:{ all -> 0x04bd }
            throw r0
        L_0x04bd:
            r0 = move-exception
            goto L_0x04bb
        L_0x04bf:
            r16 = r2
        L_0x04c1:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DMHS.sendScanResultFromScanner(android.bluetooth.le.ScanResult):void");
    }

    /* access modifiers changed from: private */
    public void clearLocalResults() {
        synchronized (this.mSemWifiApBleScanResults) {
            this.mSemWifiApBleScanResults.clear();
            this.mSmartD2DClientDevices.clear();
            this.mSemWifiApSmartUtil.sendClientScanResultUpdateIntent("D2DMHS ClearLocalResults");
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
                    String clientmac = this.mSemWifiApBleScanResults.get(j).mWifiMac;
                    if (clientmac != null) {
                        String str = TAG;
                        Log.d(str, "removed:" + clientmac);
                        this.mSmartD2DClientDevices.remove(clientmac);
                    }
                    this.mSemWifiApBleScanResults.remove(j);
                }
                if (mInt.size() > 0) {
                    this.mSemWifiApSmartUtil.sendClientScanResultUpdateIntent("D2DMHS updateLocalResults");
                }
            } catch (IndexOutOfBoundsException e) {
            }
        }
    }

    private void removeFromScanResults(String mac) {
        synchronized (this.mSemWifiApBleScanResults) {
            int j = -1;
            int count = 0;
            for (SemWifiApBleScanResult res : this.mSemWifiApBleScanResults) {
                if (res.mWifiMac.equals(mac)) {
                    j = count;
                }
                count++;
            }
            if (j != -1) {
                this.mSmartD2DClientDevices.remove(mac);
                this.mSemWifiApBleScanResults.remove(j);
            }
        }
    }

    private void addScanResults(SemWifiApBleScanResult o) {
        synchronized (this.mSemWifiApBleScanResults) {
            this.mSemWifiApBleScanResults.add(o);
            this.mSemWifiApSmartUtil.sendClientScanResultUpdateIntent("D2D MHS addScanResults");
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:14:0x000c, code lost:
        r1 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> getWifiApBleD2DScanResults() {
        /*
            r2 = this;
            monitor-enter(r2)
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r0 = r2.mSemWifiApBleScanResults     // Catch:{ all -> 0x000e }
            monitor-enter(r0)     // Catch:{ all -> 0x000e }
            java.util.List<com.samsung.android.net.wifi.SemWifiApBleScanResult> r1 = r2.mSemWifiApBleScanResults     // Catch:{ all -> 0x0009 }
            monitor-exit(r0)     // Catch:{ all -> 0x0009 }
            monitor-exit(r2)
            return r1
        L_0x0009:
            r1 = move-exception
        L_0x000a:
            monitor-exit(r0)     // Catch:{ all -> 0x000c }
            throw r1     // Catch:{ all -> 0x000e }
        L_0x000c:
            r1 = move-exception
            goto L_0x000a
        L_0x000e:
            r0 = move-exception
            monitor-exit(r2)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DMHS.getWifiApBleD2DScanResults():java.util.List");
    }

    private byte[] getmhsD2DManufactureData() {
        byte[] data = new byte[24];
        for (int i = 0; i < 24; i++) {
            data[i] = 0;
        }
        data[0] = 1;
        data[1] = 18;
        data[10] = 3;
        return data;
    }

    public void startWifiApSmartD2DMHSAdvertize() {
        if (FactoryTest.isFactoryBinary()) {
            Log.e(TAG, "This devices's binary is a factory binary");
            return;
        }
        int status = checkPreConditions();
        String str = TAG;
        Log.d(str, " startWifiApSmartD2DMHSAdvertize : status:" + status + ",isAdvRunning:" + this.isAdvRunning);
        if (!this.isAdvRunning && status == 0) {
            this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (this.mSemWifiApSmartUtil.isNearByAutohotspotEnabled() && !this.mBluetoothAdapter.semIsBleEnabled()) {
                this.mBluetoothAdapter.semSetStandAloneBleMode(true);
            }
            this.mBluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
            if (this.mBluetoothLeAdvertiser == null) {
                this.isStartAdvPending = true;
                Log.e(TAG, "mBluetoothLeAdvertiser == null, waiting for isStartAdvPending");
                return;
            }
            this.isAdvRunning = true;
            WifiInjector.getInstance().getSemWifiApSmartBleScanner().startBleScanning();
            AdvertiseSettings settings = new AdvertiseSettings.Builder().setAdvertiseMode(2).setConnectable(true).setTimeout(0).setTxPowerLevel(3).build();
            AdvertiseData.Builder builder = new AdvertiseData.Builder();
            SemWifiApSmartUtil semWifiApSmartUtil = this.mSemWifiApSmartUtil;
            AdvertiseData data = builder.addManufacturerData(SemWifiApSmartUtil.MANUFACTURE_ID, getmhsD2DManufactureData()).build();
            LocalLog localLog = this.mLocalLog;
            localLog.log(TAG + ":\tstartWifiApSmartD2DMHSAdvertize" + Arrays.toString(getmhsD2DManufactureData()));
            String str2 = TAG;
            Log.d(str2, "Started startWifiApSmartD2DMHSAdvertize with " + Arrays.toString(getmhsD2DManufactureData()));
            this.mBluetoothLeAdvertiser.startAdvertising(settings, data, this.mAdvertiseCallback);
        }
    }

    /* access modifiers changed from: package-private */
    public void stopWifiApSmartD2DMHSAdvertize() {
        AdvertiseCallback advertiseCallback;
        if (this.isAdvRunning) {
            Log.d(TAG, "stopWifiApSmartD2DMHSAdvertize");
            BluetoothLeAdvertiser bluetoothLeAdvertiser = this.mBluetoothLeAdvertiser;
            if (!(bluetoothLeAdvertiser == null || (advertiseCallback = this.mAdvertiseCallback) == null)) {
                bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            }
            this.isAdvRunning = false;
            this.mBLEPairingFailedHashMap.clear();
            LocalLog localLog = this.mLocalLog;
            localLog.log(TAG + ":stopWifiApSmartD2DMHSAdvertize");
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
}
