package com.samsung.android.server.wifi.softap.smarttethering;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Debug;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import android.util.Log;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.samsung.android.emergencymode.SemEmergencyManager;
import com.samsung.android.net.wifi.SemWifiApMacInfo;
import com.samsung.android.server.wifi.WifiDevicePolicyManager;
import java.util.Arrays;

public class SemWifiApSmartMHS {
    public static final int START_ADVERTISE = 10;
    public static final int STOP_ADVERTISE = 11;
    public static final int STOP_LOW_BATTERY_ADVERTISE = 12;
    private static final String TAG = "SemWifiApSmartMHS";
    private static final IntentFilter mSemWifiApSmartMHSIntentFilter = new IntentFilter("android.intent.action.BATTERY_CHANGED");
    private boolean DBG;
    private boolean isAdvStoppedbecauseOfInternet;
    /* access modifiers changed from: private */
    public boolean isAdvStoppedbecauseOfSIMRemoval;
    /* access modifiers changed from: private */
    public boolean isAdveretizing;
    private boolean isJDMDevice;
    /* access modifiers changed from: private */
    public boolean islowBattery;
    private AdvertiseCallback mAdvertiseCallback;
    /* access modifiers changed from: private */
    public byte mBatteryPct;
    /* access modifiers changed from: private */
    public BleWorkHandler mBleWorkHandler;
    private HandlerThread mBleWorkThread;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /* access modifiers changed from: private */
    public Context mContext;
    private BluetoothGattServer mGattServer;
    private Long mHashBasedFamilyID;
    private Long mHashBasedGuid;
    /* access modifiers changed from: private */
    public LocalLog mLocalLog;
    private PowerManager mPowerManager;
    private SemWifiApSmartMHSBroadcastReceiver mSemWifiApSmartMHSBroadcastReceiver;
    private SemWifiApSmartUtil mSemWifiApSmartUtil;
    private PowerManager.WakeLock mWakeLock;
    private WifiDevicePolicyManager mWifiDevicePolicyManager;
    private WifiManager mWifiManager;

    public SemWifiApSmartMHS(Context context, SemWifiApSmartUtil tSemWifiApBleUtil, WifiDevicePolicyManager sWifiDevicePolicyManager, LocalLog tLocalLog) {
        this.DBG = "eng".equals(Build.TYPE) || Debug.semIsProductDev();
        this.mBatteryPct = 75;
        this.islowBattery = false;
        this.mBleWorkHandler = null;
        this.mBleWorkThread = null;
        this.isAdvStoppedbecauseOfSIMRemoval = false;
        this.isAdvStoppedbecauseOfInternet = false;
        this.isJDMDevice = "in_house".contains("jdm");
        this.mAdvertiseCallback = new AdvertiseCallback() {
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(SemWifiApSmartMHS.TAG, "MHS Advertise Started.");
            }

            public void onStartFailure(int errorCode) {
                Log.e(SemWifiApSmartMHS.TAG, "MHS Advertise Failed: " + errorCode);
                LocalLog access$000 = SemWifiApSmartMHS.this.mLocalLog;
                access$000.log("SemWifiApSmartMHS:\tMHS Advertise Failed: " + errorCode);
            }
        };
        this.mContext = context;
        this.mSemWifiApSmartUtil = tSemWifiApBleUtil;
        this.mSemWifiApSmartMHSBroadcastReceiver = new SemWifiApSmartMHSBroadcastReceiver();
        this.mLocalLog = tLocalLog;
        this.mWifiDevicePolicyManager = sWifiDevicePolicyManager;
        if (!FactoryTest.isFactoryBinary()) {
            this.mContext.registerReceiver(this.mSemWifiApSmartMHSBroadcastReceiver, mSemWifiApSmartMHSIntentFilter);
        } else {
            Log.e(TAG, "This devices's binary is a factory binary");
        }
    }

    static {
        mSemWifiApSmartMHSIntentFilter.addAction("android.net.conn.INET_CONDITION_ACTION");
        mSemWifiApSmartMHSIntentFilter.addAction("com.samsung.android.server.wifi.softap.smarttethering.familyid");
        mSemWifiApSmartMHSIntentFilter.addAction("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED");
        mSemWifiApSmartMHSIntentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        mSemWifiApSmartMHSIntentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        mSemWifiApSmartMHSIntentFilter.addAction("com.samsung.android.server.wifi.softap.smarttethering.ssid_changed");
        mSemWifiApSmartMHSIntentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        mSemWifiApSmartMHSIntentFilter.addAction("com.samsung.bluetooth.adapter.action.BLE_STATE_CHANGED");
        mSemWifiApSmartMHSIntentFilter.addAction("android.intent.action.TIME_SET");
    }

    /* access modifiers changed from: package-private */
    public void sendEmptyMessage(int val) {
        BleWorkHandler bleWorkHandler = this.mBleWorkHandler;
        if (bleWorkHandler == null) {
            return;
        }
        if (val != 10) {
            bleWorkHandler.sendEmptyMessage(val);
        } else if (val == 10 && !this.isAdveretizing) {
            bleWorkHandler.sendEmptyMessageDelayed(10, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
        }
    }

    public boolean setWifiApSmartMHS(boolean enable) {
        Log.d(TAG, "setWifiApSmartMHS is [" + enable + "]");
        int val = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_smart_tethering_settings", 0);
        LocalLog localLog = this.mLocalLog;
        localLog.log("SemWifiApSmartMHS:\tSmartMHS WIFI_AP_SMART_TETHERING: " + val + "enable :" + enable);
        if (enable || !this.isAdveretizing) {
            return true;
        }
        BleWorkHandler bleWorkHandler = this.mBleWorkHandler;
        if (bleWorkHandler != null) {
            bleWorkHandler.removeCallbacksAndMessages((Object) null);
        }
        sendEmptyMessage(11);
        return true;
    }

    public void handleBootCompleted() {
        Log.d(TAG, "handleBootCompleted");
        this.mBleWorkThread = new HandlerThread("SemWifiApSmartMHSBleHandler");
        this.mBleWorkThread.start();
        this.mBleWorkHandler = new BleWorkHandler(this.mBleWorkThread.getLooper());
    }

    class SemWifiApSmartMHSBroadcastReceiver extends BroadcastReceiver {
        SemWifiApSmartMHSBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Intent intent2 = intent;
            String action = intent.getAction();
            if (action.equals("android.intent.action.TIME_SET")) {
                Log.d(SemWifiApSmartMHS.TAG, " System time changed");
                SemWifiApSmartMHS.this.mLocalLog.log("SemWifiApSmartMHS:\tSystem time changed");
                if (SemWifiApSmartMHS.this.isAdveretizing) {
                    if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                        SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessage(11);
                    }
                    if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                        SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessageDelayed(10, 500);
                    }
                }
            } else if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                String state = intent2.getStringExtra("ss");
                if ("ABSENT".equals(state)) {
                    if (SemWifiApSmartMHS.this.isAdveretizing) {
                        Log.d(SemWifiApSmartMHS.TAG, "SIM card removed");
                        SemWifiApSmartMHS.this.mLocalLog.log("SemWifiApSmartMHS:\tSIM card removed");
                        boolean unused = SemWifiApSmartMHS.this.isAdvStoppedbecauseOfSIMRemoval = true;
                        if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                            SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessage(11);
                        }
                    }
                } else if ("READY".equals(state) && !SemWifiApSmartMHS.this.isAdveretizing && SemWifiApSmartMHS.this.isAdvStoppedbecauseOfSIMRemoval) {
                    Log.d(SemWifiApSmartMHS.TAG, "SIM card inserted");
                    SemWifiApSmartMHS.this.mLocalLog.log("SemWifiApSmartMHS:\tSIM card inserted");
                    boolean unused2 = SemWifiApSmartMHS.this.isAdvStoppedbecauseOfSIMRemoval = false;
                }
            } else if (action.equals("com.samsung.bluetooth.adapter.action.BLE_STATE_CHANGED")) {
                int state2 = intent2.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
                Log.d(SemWifiApSmartMHS.TAG, "action BLE state:" + action + "::" + state2);
                LocalLog access$000 = SemWifiApSmartMHS.this.mLocalLog;
                access$000.log("SemWifiApSmartMHS:\taction BLE state:" + action + "::" + state2);
                if (state2 == 16) {
                    Log.d(SemWifiApSmartMHS.TAG, "BLE is turned off");
                    SemWifiApSmartMHS.this.mLocalLog.log("SemWifiApSmartMHS:\tBLE is turned off");
                    if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                        SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessage(11);
                    }
                }
            } else if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                int level = intent2.getIntExtra("level", -1);
                byte unused3 = SemWifiApSmartMHS.this.mBatteryPct = (byte) ((level * 100) / intent2.getIntExtra("scale", -1));
                int val = Settings.Secure.getInt(SemWifiApSmartMHS.this.mContext.getContentResolver(), "wifi_ap_smart_tethering_settings", 0);
                if (val == 1 && !SemWifiApSmartMHS.this.islowBattery && SemWifiApSmartMHS.this.mBatteryPct <= 15) {
                    Log.d(SemWifiApSmartMHS.TAG, "low battery:" + SemWifiApSmartMHS.this.mBatteryPct + " ,,isAdveretizing: " + SemWifiApSmartMHS.this.isAdveretizing);
                    boolean unused4 = SemWifiApSmartMHS.this.islowBattery = true;
                    LocalLog access$0002 = SemWifiApSmartMHS.this.mLocalLog;
                    access$0002.log("SemWifiApSmartMHS:\tlow battery pct:  " + SemWifiApSmartMHS.this.mBatteryPct + ",, isAdveretizing: " + SemWifiApSmartMHS.this.isAdveretizing);
                    if (SemWifiApSmartMHS.this.isAdveretizing) {
                        if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                            SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessage(11);
                        }
                        if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                            SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessageDelayed(10, 1000);
                        }
                    } else if (SemWifiApSmartMHS.this.mBleWorkHandler != null && WifiInjector.getInstance().getSemWifiApSmartBleScanner().registerAutoHotspotGattServer()) {
                        SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessageDelayed(10, 2);
                        SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessageDelayed(12, 60000);
                    }
                } else if (val == 1 && SemWifiApSmartMHS.this.mBleWorkHandler != null && SemWifiApSmartMHS.this.islowBattery && SemWifiApSmartMHS.this.mBatteryPct > 15) {
                    Log.d(SemWifiApSmartMHS.TAG, "charged from low battery:" + SemWifiApSmartMHS.this.mBatteryPct + " ,,isAdveretizing:" + SemWifiApSmartMHS.this.isAdveretizing);
                    boolean unused5 = SemWifiApSmartMHS.this.islowBattery = false;
                    LocalLog access$0003 = SemWifiApSmartMHS.this.mLocalLog;
                    access$0003.log("SemWifiApSmartMHS:\t change from low battery pct:  " + SemWifiApSmartMHS.this.mBatteryPct + " ,,isAdveretizing: " + SemWifiApSmartMHS.this.isAdveretizing);
                    if (SemWifiApSmartMHS.this.isAdveretizing) {
                        if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                            SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessage(11);
                        }
                        if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                            SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessageDelayed(10, 1000);
                        }
                    } else if (SemWifiApSmartMHS.this.mBleWorkHandler != null && WifiInjector.getInstance().getSemWifiApSmartBleScanner().registerAutoHotspotGattServer()) {
                        SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessageDelayed(10, 2);
                        SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessageDelayed(12, 60000);
                    }
                }
            } else if (action.equals("android.net.conn.INET_CONDITION_ACTION")) {
                int i = Settings.Secure.getInt(SemWifiApSmartMHS.this.mContext.getContentResolver(), "wifi_ap_smart_tethering_settings_with_family", 0);
                Settings.Secure.getInt(SemWifiApSmartMHS.this.mContext.getContentResolver(), "wifi_ap_smart_tethering_settings", 0);
            } else if (action.equals("com.samsung.android.server.wifi.softap.smarttethering.ssid_changed")) {
                Log.d(SemWifiApSmartMHS.TAG, " restarting advertisement,because of SSID changed");
                int val2 = Settings.Secure.getInt(SemWifiApSmartMHS.this.mContext.getContentResolver(), "wifi_ap_smart_tethering_settings", 0);
                SemWifiApSmartMHS.this.mLocalLog.log("SemWifiApSmartMHS:\tSSID/Password is changed, so restarting adv");
                if (val2 == 1 && SemWifiApSmartMHS.this.isAdveretizing) {
                    if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                        SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessage(11);
                    }
                    if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                        SemWifiApSmartMHS.this.mBleWorkHandler.sendEmptyMessageDelayed(10, 1000);
                    }
                }
            }
        }
    }

    public boolean isMHSAdvertizing() {
        return this.isAdveretizing;
    }

    private void acquireWakeLock() {
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
            this.mWakeLock = this.mPowerManager.newWakeLock(1, TAG);
        }
        PowerManager.WakeLock wakeLock = this.mWakeLock;
        if (wakeLock != null && !wakeLock.isHeld()) {
            Log.i(TAG, "acquireWakeLock");
            this.mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        PowerManager.WakeLock wakeLock = this.mWakeLock;
        if (wakeLock != null && wakeLock.isHeld()) {
            Log.i(TAG, "releaseWakeLock");
            this.mWakeLock.release();
        }
    }

    private class BleWorkHandler extends Handler {
        public BleWorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 10:
                    if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                        SemWifiApSmartMHS.this.mBleWorkHandler.removeMessages(10);
                    }
                    SemWifiApSmartMHS.this.startAdvertizing();
                    return;
                case 11:
                    if (SemWifiApSmartMHS.this.mBleWorkHandler != null) {
                        SemWifiApSmartMHS.this.mBleWorkHandler.removeMessages(11);
                    }
                    SemWifiApSmartMHS.this.stopAdvertizing();
                    return;
                case 12:
                    Log.e(SemWifiApSmartMHS.TAG, "stop advertising which was started because of low battery");
                    SemWifiApSmartMHS.this.mLocalLog.log("SemWifiApSmartMHS:\tstop advertising which was started because of low battery");
                    WifiInjector.getInstance().getSemWifiApSmartBleScanner().unregisterAutoHotspotGattServer();
                    SemWifiApSmartMHS.this.stopAdvertizing();
                    return;
                default:
                    return;
            }
        }
    }

    private boolean simCheck() {
        if ((!this.DBG || !SystemProperties.get("SimCheck.disable").equals("1")) && ((TelephonyManager) this.mContext.getSystemService("phone")).getSimState() != 5 && !"LOADED".equals(SystemProperties.get("gsm.sim.state")) && !"READY".equals(SystemProperties.get("gsm.sim.state"))) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public int checkPreConditions() {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (this.mBluetoothAdapter == null) {
            Log.e(TAG, "mBluetoothAdapter == null");
            this.mLocalLog.log("SemWifiApSmartMHS:\t mBluetoothAdapter == null");
            return -10;
        } else if (!this.isJDMDevice || SemWifiApMacInfo.getInstance().readWifiMacInfo() != null) {
            this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            if (!this.mSemWifiApSmartUtil.isPackageExists("com.sec.mhs.smarttethering")) {
                Log.e(TAG, "isPackageExists smarttethering null");
                this.mLocalLog.log("SemWifiApSmartMHS:\tisPackageExists smarttethering null");
                Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_smart_tethering_settings", 0);
                return -1;
            } else if (!simCheck()) {
                Log.e(TAG, "Simcard not present");
                this.mLocalLog.log("SemWifiApSmartMHS:\tSimcard not present");
                Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_smart_tethering_settings", 0);
                SemWifiApSmartUtil semWifiApSmartUtil = this.mSemWifiApSmartUtil;
                return SemWifiApSmartUtil.SIM_CARD_ERROR;
            } else if (((UserManager) this.mContext.getSystemService("user")).hasUserRestriction("no_config_tethering") || UserHandle.myUserId() != 0) {
                Log.e(TAG, "Do not setWifiApSmartMHS in DISALLOW_CONFIG_TETHERING");
                this.mLocalLog.log("SemWifiApSmartMHS:\tDo not setWifiApSmartMHS in DISALLOW_CONFIG_TETHERING");
                return -8;
            } else if (!this.mWifiDevicePolicyManager.isAllowToUseHotspot()) {
                Log.e(TAG, "Do not setWifiApSmartMHS in EmergencyMode");
                this.mLocalLog.log("SemWifiApSmartMHS:\tDo not setWifiApSmartMHS in EmergencyMode");
                return -6;
            } else if (this.mWifiManager.getWifiApConfiguration().allowedKeyManagement.get(0) && !this.mWifiDevicePolicyManager.isOpenWifiApAllowed()) {
                Log.e(TAG, "Do not setWifiApSmartMHS  in Open N/W");
                this.mLocalLog.log("SemWifiApSmartMHS:\tDo not setWifiApSmartMHS  in Open N/W");
                return -7;
            } else if (!this.mSemWifiApSmartUtil.isNearByAutohotspotEnabled()) {
                Log.d(TAG, "not isNearByAutohotspotEnabled");
                this.mLocalLog.log("SemWifiApSmartMHS:\t not isNearByAutohotspotEnabled");
                return -11;
            } else {
                this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothAdapter bluetoothAdapter = this.mBluetoothAdapter;
                if (bluetoothAdapter == null || !bluetoothAdapter.semIsBleEnabled()) {
                    boolean isAirplaneMode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0;
                    if (isAirplaneMode) {
                        Log.e(TAG, "getAirplaneMode: " + isAirplaneMode);
                        LocalLog localLog = this.mLocalLog;
                        localLog.log("SemWifiApSmartMHS:\tgetAirplaneMode: " + isAirplaneMode);
                        return -4;
                    }
                    SemEmergencyManager em = SemEmergencyManager.getInstance(this.mContext);
                    if (em == null || !em.isEmergencyMode()) {
                        return 0;
                    }
                    Log.e(TAG, "Do not setWifiApSmartMHS in EmergencyMode");
                    this.mLocalLog.log("SemWifiApSmartMHS:\tDo not setWifiApSmartMHS in EmergencyMode");
                    return -5;
                }
                Log.i(TAG, "Preconditions BLE is ON");
                this.mLocalLog.log("SemWifiApSmartMHS:\t  Preconditions BLE is ON");
                return 0;
            }
        } else {
            Log.e(TAG, "JDM MAC address is null");
            this.mLocalLog.log("SemWifiApSmartMHS:\t JDM MAC address is null");
            return -9;
        }
    }

    private byte[] getMHSAdvManufactureData() {
        byte b;
        byte[] advData = new byte[24];
        for (int idx = 0; idx < 24; idx++) {
            advData[idx] = 0;
        }
        advData[0] = 1;
        advData[1] = 18;
        Long mguid = Long.valueOf(this.mSemWifiApSmartUtil.getHashbasedonGuid());
        Long familyID = Long.valueOf(this.mSemWifiApSmartUtil.getHashbasedonFamilyId());
        int mFamilySupport = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_smart_tethering_settings_with_family", 0);
        if (mguid.longValue() != -1) {
            byte[] guidBytes = SemWifiApSmartUtil.bytesFromLong(mguid);
            for (int i = 0; i < 4; i++) {
                advData[i + 2] = guidBytes[i];
            }
        }
        if (familyID.longValue() != -1 && mFamilySupport == 1) {
            byte[] familyBytes = SemWifiApSmartUtil.bytesFromLong(familyID);
            for (int i2 = 0; i2 < 4; i2++) {
                advData[i2 + 2 + 4] = familyBytes[i2];
            }
        }
        advData[10] = 2;
        int nwtype = this.mSemWifiApSmartUtil.getNetworkType();
        if (nwtype == 1) {
            advData[11] = (byte) (advData[11] | SemWifiApSmartUtil.BLE_WIFI);
        } else if (nwtype == 2) {
            advData[11] = (byte) (advData[11] | -64);
        } else if (nwtype == 3) {
            advData[11] = (byte) (advData[11] | -64);
        }
        if (this.mSemWifiApSmartUtil.getlegacyPassword() != null) {
            advData[11] = (byte) (4 | advData[11]);
        }
        if (this.mSemWifiApSmartUtil.getlegacySSIDHidden()) {
            advData[11] = (byte) (2 | advData[11]);
        }
        byte b2 = this.mBatteryPct;
        if (b2 < 6 || b2 > 15) {
            byte b3 = this.mBatteryPct;
            if (b3 < 16 || b3 > 30) {
                byte b4 = this.mBatteryPct;
                if (b4 < 31 || b4 > 45) {
                    byte b5 = this.mBatteryPct;
                    if (b5 < 46 || b5 > 60) {
                        byte b6 = this.mBatteryPct;
                        if (b6 < 61 || b6 > 75) {
                            byte b7 = this.mBatteryPct;
                            if (b7 < 76 || b7 > 90) {
                                byte b8 = this.mBatteryPct;
                                if (b8 >= 91 && b8 <= 100) {
                                    advData[11] = (byte) (advData[11] | 56);
                                }
                            } else {
                                advData[11] = (byte) (advData[11] | SemWifiApSmartUtil.BLE_BATT_6);
                            }
                        } else {
                            advData[11] = (byte) (advData[11] | SemWifiApSmartUtil.BLE_BATT_5);
                        }
                    } else {
                        advData[11] = (byte) (advData[11] | SemWifiApSmartUtil.BLE_BATT_4);
                    }
                } else {
                    advData[11] = (byte) (24 | advData[11]);
                }
            } else {
                advData[11] = (byte) (advData[11] | SemWifiApSmartUtil.BLE_BATT_2);
            }
        } else {
            advData[11] = (byte) (advData[11] | 8);
        }
        byte[] mbt = this.mSemWifiApSmartUtil.getClientMACbytes();
        for (int i3 = 0; i3 < 3; i3++) {
            advData[i3 + 11 + 1] = mbt[i3 + 3];
        }
        if (this.mSemWifiApSmartUtil.Check_MHS_AES_Key()) {
            SemWifiApSmartUtil semWifiApSmartUtil = this.mSemWifiApSmartUtil;
            b = 1;
        } else {
            b = 0;
        }
        advData[15] = b;
        byte[] name = this.mSemWifiApSmartUtil.getlegacySSID().getBytes();
        int len = name.length;
        int i4 = 0;
        while (i4 < len && i4 < 7) {
            advData[i4 + 17] = name[i4];
            i4++;
        }
        Log.d(TAG, "mBatteryPct:" + this.mBatteryPct);
        Log.d(TAG, "advData:" + Arrays.toString(advData));
        this.mLocalLog.log("SemWifiApSmartMHS:\t SmartMHS startAdvertizing mBatteryPct : " + this.mBatteryPct + ",advData:" + Arrays.toString(advData));
        return advData;
    }

    private byte[] getScanResponseData() {
        byte[] respData = new byte[27];
        for (int idx = 0; idx < 27; idx++) {
            respData[idx] = 0;
        }
        respData[0] = 1;
        respData[1] = 18;
        byte[] name = this.mSemWifiApSmartUtil.getlegacySSID().getBytes();
        byte nameLen = (byte) name.length;
        byte len = 7;
        for (int index = 2; index < 27 && len < nameLen; index++) {
            respData[index] = name[len];
            len = (byte) (len + 1);
        }
        Log.d(TAG, "respData:" + Arrays.toString(respData));
        LocalLog localLog = this.mLocalLog;
        localLog.log("SemWifiApSmartMHS:\tSmartMHS getScanResponseData respData:" + Arrays.toString(respData));
        return respData;
    }

    /* access modifiers changed from: private */
    public void startAdvertizing() {
        if (!this.isAdveretizing) {
            int checkprecond = checkPreConditions();
            if (checkprecond < 0) {
                Log.d(TAG, "preconditions failed :" + checkprecond);
                Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_smart_tethering_settings", 0);
                LocalLog localLog = this.mLocalLog;
                localLog.log("SemWifiApSmartMHS:\tSmartMHS startAdvertizing failed checkprecond: " + checkprecond);
                return;
            }
            this.isAdveretizing = true;
            AdvertiseSettings settings = new AdvertiseSettings.Builder().setAdvertiseMode(2).setConnectable(true).setTimeout(0).setTxPowerLevel(3).build();
            AdvertiseData.Builder builder = new AdvertiseData.Builder();
            SemWifiApSmartUtil semWifiApSmartUtil = this.mSemWifiApSmartUtil;
            AdvertiseData data = builder.addManufacturerData(SemWifiApSmartUtil.MANUFACTURE_ID, getMHSAdvManufactureData()).build();
            AdvertiseData.Builder builder2 = new AdvertiseData.Builder();
            SemWifiApSmartUtil semWifiApSmartUtil2 = this.mSemWifiApSmartUtil;
            AdvertiseData scanResponseData = builder2.addManufacturerData(SemWifiApSmartUtil.MANUFACTURE_ID, getScanResponseData()).setIncludeDeviceName(false).setIncludeTxPowerLevel(false).build();
            Log.d(TAG, "Starting MHS BLE advertising");
            this.mLocalLog.log("SemWifiApSmartMHS:\tStarting MHS BLE advertising ");
            this.mBluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
            BluetoothLeAdvertiser bluetoothLeAdvertiser = this.mBluetoothLeAdvertiser;
            if (bluetoothLeAdvertiser == null) {
                Log.d(TAG, "mBluetoothLeAdvertiser is null");
                this.mLocalLog.log("SemWifiApSmartMHS:\tmBluetoothLeAdvertiser is null ");
                return;
            }
            bluetoothLeAdvertiser.startAdvertising(settings, data, scanResponseData, this.mAdvertiseCallback);
        }
    }

    /* access modifiers changed from: private */
    public void stopAdvertizing() {
        if (this.isAdveretizing) {
            Log.d(TAG, "stopAdvertizing");
            BluetoothLeAdvertiser bluetoothLeAdvertiser = this.mBluetoothLeAdvertiser;
            if (bluetoothLeAdvertiser != null) {
                bluetoothLeAdvertiser.stopAdvertising(this.mAdvertiseCallback);
            }
            this.isAdveretizing = false;
            this.mLocalLog.log("SemWifiApSmartMHS:\tstopped advertizing");
        }
    }
}
