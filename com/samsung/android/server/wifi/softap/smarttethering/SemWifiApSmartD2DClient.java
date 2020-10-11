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
import android.database.ContentObserver;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.LocalLog;
import android.util.Log;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.samsung.android.emergencymode.SemEmergencyManager;
import com.samsung.android.net.wifi.SemWifiApMacInfo;
import java.util.Arrays;

public class SemWifiApSmartD2DClient {
    private static final String ACTION_LOGIN_ACCOUNTS_COMPLETE = "com.samsung.account.SAMSUNGACCOUNT_SIGNIN_COMPLETED";
    public static final int START_ADVERTISE = 1;
    public static final int STOP_ADVERTISE = 2;
    /* access modifiers changed from: private */
    public static String TAG = "SemWifiApSmartD2DClient";
    private static IntentFilter mSemWifiApSmartD2DClientIntentFilter = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
    /* access modifiers changed from: private */
    public boolean isAdvRunning;
    private boolean isJDMDevice = "in_house".contains("jdm");
    /* access modifiers changed from: private */
    public boolean isStartAdvPending;
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(SemWifiApSmartD2DClient.TAG, "client D2D Advertise Started.");
        }

        public void onStartFailure(int errorCode) {
            String access$100 = SemWifiApSmartD2DClient.TAG;
            Log.e(access$100, "client D2D Advertise Failed: " + errorCode);
            LocalLog access$200 = SemWifiApSmartD2DClient.this.mLocalLog;
            access$200.log(SemWifiApSmartD2DClient.TAG + ":\tclient D2D Advertise Failed: " + errorCode);
        }
    };
    /* access modifiers changed from: private */
    public BleWorkHandler mBleWorkHandler = null;
    private HandlerThread mBleWorkThread = null;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public LocalLog mLocalLog;
    private boolean mNeedAdvertisement;
    private SemWifiApSmartD2DClientReceiver mSemWifiApSmartD2DClientReceiver;
    private ContentObserver mSemWifiApSmartDeviceNameObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            if (SemWifiApSmartD2DClient.this.isAdvRunning) {
                Log.d(SemWifiApSmartD2DClient.TAG, "mSemWifiApSmartDeviceNameObserver");
                LocalLog access$200 = SemWifiApSmartD2DClient.this.mLocalLog;
                access$200.log(SemWifiApSmartD2DClient.TAG + ":\tmSemWifiApSmartDeviceNameObserver");
                if (SemWifiApSmartD2DClient.this.mBleWorkHandler != null) {
                    SemWifiApSmartD2DClient.this.mBleWorkHandler.sendEmptyMessage(2);
                    SemWifiApSmartD2DClient.this.mBleWorkHandler.sendEmptyMessageDelayed(1, 1000);
                }
            }
        }
    };
    private SemWifiApSmartUtil mSemWifiApSmartUtil;

    static {
        mSemWifiApSmartD2DClientIntentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        mSemWifiApSmartD2DClientIntentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        mSemWifiApSmartD2DClientIntentFilter.addAction("com.samsung.bluetooth.adapter.action.BLE_STATE_CHANGED");
        mSemWifiApSmartD2DClientIntentFilter.addAction(ACTION_LOGIN_ACCOUNTS_COMPLETE);
        mSemWifiApSmartD2DClientIntentFilter.addAction("com.samsung.android.server.wifi.softap.smarttethering.d2dfamilyid");
    }

    public SemWifiApSmartD2DClient(Context context, SemWifiApSmartUtil tSemWifiApSmartUtil, LocalLog tLocalLog) {
        this.mSemWifiApSmartUtil = tSemWifiApSmartUtil;
        this.mLocalLog = tLocalLog;
        this.mContext = context;
        this.mSemWifiApSmartD2DClientReceiver = new SemWifiApSmartD2DClientReceiver();
        this.mContext.registerReceiver(this.mSemWifiApSmartD2DClientReceiver, mSemWifiApSmartD2DClientIntentFilter);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("device_name"), false, this.mSemWifiApSmartDeviceNameObserver);
    }

    public void handleBootCompleted() {
        Log.d(TAG, "handleBootCompleted");
        LocalLog localLog = this.mLocalLog;
        localLog.log(TAG + ":\t handleBootCompleted");
        this.mBleWorkThread = new HandlerThread("SemWifiApSmartD2DClientBleHandler");
        this.mBleWorkThread.start();
        this.mBleWorkHandler = new BleWorkHandler(this.mBleWorkThread.getLooper());
        if (this.mNeedAdvertisement && this.mBleWorkHandler != null) {
            Log.d(TAG, "need to advertise client packets");
            sendEmptyMessage(1);
        }
    }

    class SemWifiApSmartD2DClientReceiver extends BroadcastReceiver {
        SemWifiApSmartD2DClientReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean z = true;
            if (action.equals("com.samsung.android.server.wifi.softap.smarttethering.d2dfamilyid")) {
                if (SemWifiApSmartD2DClient.this.isAdvRunning) {
                    Log.d(SemWifiApSmartD2DClient.TAG, "d2dfamilyid intent received");
                    SemWifiApSmartD2DClient.this.mLocalLog.log(SemWifiApSmartD2DClient.TAG + ":\td2dfamilyid intent received");
                    if (SemWifiApSmartD2DClient.this.mBleWorkHandler != null) {
                        SemWifiApSmartD2DClient.this.mBleWorkHandler.sendEmptyMessage(2);
                        SemWifiApSmartD2DClient.this.mBleWorkHandler.sendEmptyMessageDelayed(1, 1000);
                    }
                }
            } else if (action.equals(SemWifiApSmartD2DClient.ACTION_LOGIN_ACCOUNTS_COMPLETE)) {
                if (SemWifiApSmartD2DClient.this.isAdvRunning) {
                    SemWifiApSmartD2DClient.this.sendEmptyMessage(2);
                }
            } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (Settings.Global.getInt(SemWifiApSmartD2DClient.this.mContext.getContentResolver(), "airplane_mode_on", 0) == 0) {
                    z = false;
                }
                boolean isAirplaneMode = z;
                Log.d(SemWifiApSmartD2DClient.TAG, "isAirplaneMode:" + isAirplaneMode);
                SemWifiApSmartD2DClient.this.mLocalLog.log(SemWifiApSmartD2DClient.TAG + ":\tAIRPLANE_MODE:" + isAirplaneMode);
                if (isAirplaneMode) {
                    SemWifiApSmartD2DClient.this.sendEmptyMessage(2);
                }
            } else if (action.equals("com.samsung.bluetooth.adapter.action.BLE_STATE_CHANGED")) {
                int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
                if (SemWifiApSmartD2DClient.this.isAdvRunning) {
                    boolean unused = SemWifiApSmartD2DClient.this.isStartAdvPending = false;
                }
                if (SemWifiApSmartD2DClient.this.isStartAdvPending && state == 15) {
                    boolean unused2 = SemWifiApSmartD2DClient.this.isStartAdvPending = false;
                }
                if (state == 16) {
                    Log.d(SemWifiApSmartD2DClient.TAG, "BLE is OFF, stopping advertizement");
                    SemWifiApSmartD2DClient.this.mLocalLog.log(SemWifiApSmartD2DClient.TAG + ":\t BLE is OFF, stopping advertizement");
                    SemWifiApSmartD2DClient.this.stopWifiApSmartD2DclientAdvertize();
                }
            } else if (action.equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                boolean emergencyMode = intent.getBooleanExtra("phoneinECMState", false);
                Log.d(SemWifiApSmartD2DClient.TAG, "emergencyMode:" + emergencyMode);
                SemWifiApSmartD2DClient.this.mLocalLog.log(SemWifiApSmartD2DClient.TAG + ": EMERGENCY" + emergencyMode);
                if (emergencyMode) {
                    SemWifiApSmartD2DClient.this.sendEmptyMessage(2);
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
            return -6;
        } else if (this.isJDMDevice && SemWifiApMacInfo.getInstance().readWifiMacInfo() == null) {
            Log.e(TAG, "JDM MAC address is null");
            LocalLog localLog2 = this.mLocalLog;
            localLog2.log(TAG + ":\t JDM MAC address is null");
            return -5;
        } else if (!this.mSemWifiApSmartUtil.isPackageExists("com.sec.mhs.smarttethering")) {
            Log.e(TAG, "isPackageExists smarttethering == null");
            return -1;
        } else if (this.mSemWifiApSmartUtil.getSamsungAccountCount() != 0) {
            Log.e(TAG, "Samsung account is loggedin, so can't start advertizsement");
            return -4;
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
                        return -7;
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
            int i = msg.what;
            if (i == 1) {
                int status = SemWifiApSmartD2DClient.this.checkPreConditions();
                if (status != 0 || SemWifiApSmartD2DClient.this.isAdvRunning) {
                    String access$100 = SemWifiApSmartD2DClient.TAG;
                    Log.e(access$100, "checkPreConditions failed " + status);
                    LocalLog access$200 = SemWifiApSmartD2DClient.this.mLocalLog;
                    access$200.log(SemWifiApSmartD2DClient.TAG + ":\tcheckPreConditions failed " + status);
                    return;
                }
                SemWifiApSmartD2DClient.this.startWifiApSmartD2DclientAdvertize();
            } else if (i == 2) {
                SemWifiApSmartD2DClient.this.stopWifiApSmartD2DclientAdvertize();
            }
        }
    }

    public boolean semWifiApBleD2DClientRole(boolean enable) {
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
            bleWorkHandler.sendEmptyMessageDelayed(val, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
        } else if (bleWorkHandler == null && val == 1) {
            Log.d(TAG, "START_ADVERTISE with null bleworker");
            this.mNeedAdvertisement = true;
        } else if (this.mBleWorkHandler == null && val == 2) {
            Log.d(TAG, "STOP_ADVERTISE with null bleworker");
            this.mNeedAdvertisement = false;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isAdvertising() {
        return this.isAdvRunning;
    }

    private byte[] getClientD2DManufactureData() {
        byte[] data = new byte[24];
        for (int i = 0; i < 24; i++) {
            data[i] = 0;
        }
        data[0] = 1;
        data[1] = 18;
        byte[] mbt = this.mSemWifiApSmartUtil.getmappedClientMACbytes();
        for (int i2 = 2; i2 < 8; i2++) {
            data[i2] = mbt[i2 - 2];
        }
        data[10] = 4;
        String mdeviceName = this.mSemWifiApSmartUtil.getDeviceName();
        byte[] mbt2 = mdeviceName.getBytes();
        int devicelength = mbt2.length;
        if (devicelength > 34) {
            Log.e(TAG, "client name is more than 34 characters");
            LocalLog localLog = this.mLocalLog;
            localLog.log(TAG + ":\tclient name is more than 34 characters");
            int cbytes = 0;
            int len = mdeviceName.length();
            int i3 = 0;
            while (true) {
                if (i3 >= len) {
                    break;
                }
                cbytes += mdeviceName.substring(i3, i3 + 1).getBytes().length;
                if (cbytes > 34) {
                    mbt2 = mdeviceName.substring(0, i3).getBytes();
                    devicelength = mbt2.length;
                    break;
                }
                i3++;
            }
        }
        int i4 = 0;
        while (i4 < 9 && i4 < devicelength) {
            data[11 + i4 + 4] = mbt2[i4];
            i4++;
        }
        long mExistingD2D = this.mSemWifiApSmartUtil.getHashbasedonD2DFamilyid();
        if (mExistingD2D != -1) {
            data[8] = 1;
            byte[] mExistingD2DBytes = SemWifiApSmartUtil.bytesFromLong(Long.valueOf(mExistingD2D));
            data[11] = mExistingD2DBytes[0];
            data[11 + 1] = mExistingD2DBytes[2];
            data[11 + 2] = mExistingD2DBytes[4];
            data[11 + 3] = mExistingD2DBytes[6];
        }
        return data;
    }

    private byte[] getClientD2DScanResponseData() {
        byte[] data = new byte[27];
        for (int i = 0; i < 27; i++) {
            data[i] = 0;
        }
        data[0] = 1;
        data[1] = 18;
        String mdeviceName = this.mSemWifiApSmartUtil.getDeviceName();
        byte[] mbt = mdeviceName.getBytes();
        int devicelength = mbt.length;
        if (devicelength > 34) {
            int cbytes = 0;
            int len = mdeviceName.length();
            int i2 = 0;
            while (true) {
                if (i2 >= len) {
                    break;
                }
                cbytes += mdeviceName.substring(i2, i2 + 1).getBytes().length;
                if (cbytes > 34) {
                    mbt = mdeviceName.substring(0, i2).getBytes();
                    devicelength = mbt.length;
                    break;
                }
                i2++;
            }
        }
        int len2 = 9;
        int i3 = 2;
        while (i3 < 27 && len2 < devicelength) {
            data[i3] = mbt[len2];
            i3++;
            len2++;
        }
        return data;
    }

    public void startWifiApSmartD2DclientAdvertize() {
        if (FactoryTest.isFactoryBinary()) {
            Log.e(TAG, "This devices's binary is a factory binary");
            return;
        }
        int status = checkPreConditions();
        String str = TAG;
        Log.d(str, " startWifiApSmartD2DClientAdvertize : status:" + status + ",isAdvRunning:" + this.isAdvRunning);
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
            byte[] mClientD2DManuFatureData = getClientD2DManufactureData();
            byte[] mClientD2DSRPData = getClientD2DScanResponseData();
            WifiInjector.getInstance().getSemWifiApSmartBleScanner().startBleScanning();
            AdvertiseSettings settings = new AdvertiseSettings.Builder().setAdvertiseMode(2).setConnectable(true).setTimeout(0).setTxPowerLevel(3).build();
            AdvertiseData.Builder builder = new AdvertiseData.Builder();
            SemWifiApSmartUtil semWifiApSmartUtil = this.mSemWifiApSmartUtil;
            AdvertiseData data = builder.addManufacturerData(SemWifiApSmartUtil.MANUFACTURE_ID, mClientD2DManuFatureData).build();
            AdvertiseData.Builder builder2 = new AdvertiseData.Builder();
            SemWifiApSmartUtil semWifiApSmartUtil2 = this.mSemWifiApSmartUtil;
            AdvertiseData srpdata = builder2.addManufacturerData(SemWifiApSmartUtil.MANUFACTURE_ID, mClientD2DSRPData).build();
            LocalLog localLog = this.mLocalLog;
            localLog.log(TAG + ":\tstartWifiApSmartClientMHSAdvertize" + Arrays.toString(mClientD2DManuFatureData));
            String str2 = TAG;
            Log.d(str2, "Started startWifiApSmartD2DClientAdvertize with " + Arrays.toString(mClientD2DManuFatureData));
            LocalLog localLog2 = this.mLocalLog;
            localLog2.log(TAG + ":\tgetClientD2DScanResponseData:" + Arrays.toString(mClientD2DSRPData));
            String str3 = TAG;
            Log.d(str3, "getClientD2DScanResponseData:" + Arrays.toString(mClientD2DSRPData));
            this.mBluetoothLeAdvertiser.startAdvertising(settings, data, srpdata, this.mAdvertiseCallback);
        }
    }

    /* access modifiers changed from: package-private */
    public void stopWifiApSmartD2DclientAdvertize() {
        AdvertiseCallback advertiseCallback;
        if (this.isAdvRunning) {
            Log.d(TAG, "stopWifiApSmartD2DClientAdvertize");
            BluetoothLeAdvertiser bluetoothLeAdvertiser = this.mBluetoothLeAdvertiser;
            if (!(bluetoothLeAdvertiser == null || (advertiseCallback = this.mAdvertiseCallback) == null)) {
                bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            }
            this.isAdvRunning = false;
            LocalLog localLog = this.mLocalLog;
            localLog.log(TAG + ":stopWifiApSmartD2DClientAdvertize");
        }
    }
}
