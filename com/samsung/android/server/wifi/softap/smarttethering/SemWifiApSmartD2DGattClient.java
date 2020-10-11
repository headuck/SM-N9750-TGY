package com.samsung.android.server.wifi.softap.smarttethering;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.wifi.SemWifiApSmartWhiteList;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.iwc.IWCEventManager;
import com.samsung.android.net.wifi.ISemWifiApSmartCallback;
import com.samsung.android.server.wifi.dqa.ReportIdKey;
import com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class SemWifiApSmartD2DGattClient {
    private static final int ST_ADDED_TO_ALLOWED_LIST = 4;
    private static final int ST_BONDING_FAILURE = -6;
    private static final int ST_BONDING_GOINGON = -4;
    private static final int ST_BOND_FAILED = -2;
    private static final int ST_BT_PAIRING = 2;
    private static final int ST_CONNECTION_ALREADY_EXIST = -3;
    private static final int ST_DEVICE_NOT_FOUND = -1;
    private static final int ST_DISCONNECTED = 0;
    private static final int ST_GATT_CONNECTING = 1;
    private static final int ST_GATT_FAILURE = -5;
    private static final int ST_MHS_GATT_CLIENT_TIMEOUT = -7;
    private static final int ST_MHS_GATT_SERVICE_NOT_FOUND = -8;
    private static IntentFilter mSemWifiApSmartD2DGattClientIntentFilter = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
    private final int DISCONNECT_GATT = 12;
    private final int DISPLAY_ADDED_TOAST = 14;
    private final int DISPLAY_FAILED_TO_ADD_TOAST = 15;
    private final String TAG = "SemWifiApSmartD2DGattClient";
    private final int UPDATE_CONNECTION_FAILURES = 13;
    private final int WAIT_FOR_MTU_CALLBACK = 16;
    /* access modifiers changed from: private */
    public HashSet<String> bonedDevicesFromD2D = new HashSet<>();
    /* access modifiers changed from: private */
    public boolean isBondingGoingon = false;
    /* access modifiers changed from: private */
    public BleWorkHandler mBleWorkHandler = null;
    private HandlerThread mBleWorkThread = null;
    private BluetoothAdapter mBluetoothAdapter;
    /* access modifiers changed from: private */
    public BluetoothDevice mBluetoothDevice;
    /* access modifiers changed from: private */
    public BluetoothGattService mBluetoothGattService;
    /* access modifiers changed from: private */
    public boolean mBluetoothIsOn;
    List<ClientD2DInfo> mClientD2DList = new ArrayList();
    /* access modifiers changed from: private */
    public BluetoothGatt mConnectedGatt = null;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public String mD2DClient_MAC;
    List<String> mD2DConnection = new ArrayList();
    HashMap<String, Integer> mFailedBLEConnections = new HashMap<>();
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            LocalLog access$000 = SemWifiApSmartD2DGattClient.this.mLocalLog;
            access$000.log("SemWifiApSmartD2DGattClient:\tonConnectionStateChange " + SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil.getStatusDescription(status) + " " + SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil.getStateDescription(newState));
            if (newState == 2) {
                Log.d("SemWifiApSmartD2DGattClient", "device,connected" + gatt.getDevice() + ",mRequestedToConnect:" + SemWifiApSmartD2DGattClient.this.mRequestedToConnect);
                if (SemWifiApSmartD2DGattClient.this.mRequestedToConnect) {
                    boolean unused = SemWifiApSmartD2DGattClient.this.mRequestedToConnect = false;
                    if (SemWifiApSmartD2DGattClient.this.mBleWorkHandler != null) {
                        SemWifiApSmartD2DGattClient.this.mBleWorkHandler.sendEmptyMessageDelayed(16, 300);
                    }
                }
            } else if (newState == 0) {
                Log.d("SemWifiApSmartD2DGattClient", "device, disconnected" + gatt.getDevice());
                if (status != 0) {
                    String mBTaddr = gatt.getDevice().getAddress();
                    int count = SemWifiApSmartD2DGattClient.this.mFailedBLEConnections.get(mBTaddr).intValue();
                    if (count >= 3) {
                        SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient = SemWifiApSmartD2DGattClient.this;
                        semWifiApSmartD2DGattClient.setConnectionState(SemWifiApSmartD2DGattClient.ST_GATT_FAILURE, semWifiApSmartD2DGattClient.mD2DClient_MAC);
                        SemWifiApSmartD2DGattClient.this.mFailedBLEConnections.remove(mBTaddr);
                        SemWifiApSmartD2DGattClient.this.shutdownclient();
                    } else if (count < 3) {
                        SemWifiApSmartD2DGattClient.this.shutdownclient_1();
                        gatt.refresh();
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                        if (!SemWifiApSmartD2DGattClient.this.tryToConnectToRemoteBLE(gatt.getDevice(), true)) {
                            SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient2 = SemWifiApSmartD2DGattClient.this;
                            semWifiApSmartD2DGattClient2.setConnectionState(SemWifiApSmartD2DGattClient.ST_GATT_FAILURE, semWifiApSmartD2DGattClient2.mD2DClient_MAC);
                            SemWifiApSmartD2DGattClient.this.mFailedBLEConnections.remove(mBTaddr);
                            SemWifiApSmartD2DGattClient.this.shutdownclient();
                        }
                    }
                } else {
                    if (gatt.getDevice() != null) {
                        SemWifiApSmartD2DGattClient.this.mFailedBLEConnections.remove(gatt.getDevice().getAddress());
                    }
                    SemWifiApSmartD2DGattClient.this.shutdownclient();
                }
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            boolean found = false;
            for (BluetoothGattService service : gatt.getServices()) {
                Log.d("SemWifiApSmartD2DGattClient", "Service: " + SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil.lookup(service.getUuid()));
                SemWifiApSmartUtil unused = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil;
                if (SemWifiApSmartUtil.SERVICE_UUID.equals(service.getUuid())) {
                    BluetoothGattService unused2 = SemWifiApSmartD2DGattClient.this.mBluetoothGattService = service;
                    found = true;
                    BluetoothGattService access$1900 = SemWifiApSmartD2DGattClient.this.mBluetoothGattService;
                    SemWifiApSmartUtil unused3 = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil;
                    gatt.readCharacteristic(access$1900.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_D2D_CLIENT_BOND_STATUS));
                }
            }
            if (!found) {
                SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient = SemWifiApSmartD2DGattClient.this;
                semWifiApSmartD2DGattClient.setConnectionState(SemWifiApSmartD2DGattClient.ST_MHS_GATT_SERVICE_NOT_FOUND, semWifiApSmartD2DGattClient.mD2DClient_MAC);
                if (SemWifiApSmartD2DGattClient.this.mBleWorkHandler != null) {
                    SemWifiApSmartD2DGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                }
            }
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            SemWifiApSmartUtil unused = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil;
            if (SemWifiApSmartUtil.CHARACTERISTIC_D2D_CLIENT_BOND_STATUS.equals(characteristic.getUuid())) {
                byte[] mbytes = characteristic.getValue();
                if (mbytes == null || mbytes.length <= 0 || mbytes[0] != 1) {
                    Log.d("SemWifiApSmartD2DGattClient", "remote device is not bonded");
                    SemWifiApSmartD2DGattClient.this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tremote device is not bonded");
                    BluetoothDevice mDevice = gatt.getDevice();
                    if (mDevice != null) {
                        Log.e("SemWifiApSmartD2DGattClient", "device is not bonded at D2D Client side ,so removing the device");
                        mDevice.removeBond();
                    }
                    SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient = SemWifiApSmartD2DGattClient.this;
                    semWifiApSmartD2DGattClient.setConnectionState(SemWifiApSmartD2DGattClient.ST_BONDING_FAILURE, semWifiApSmartD2DGattClient.mD2DClient_MAC);
                    if (SemWifiApSmartD2DGattClient.this.mBleWorkHandler != null) {
                        SemWifiApSmartD2DGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                        return;
                    }
                    return;
                }
                Log.d("SemWifiApSmartD2DGattClient", "Got bond status:" + mbytes[0]);
                LocalLog access$000 = SemWifiApSmartD2DGattClient.this.mLocalLog;
                access$000.log("SemWifiApSmartD2DGattClient:\tGot bond status:" + mbytes[0]);
                BluetoothGattService access$1900 = SemWifiApSmartD2DGattClient.this.mBluetoothGattService;
                SemWifiApSmartUtil unused2 = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil;
                BluetoothGattCharacteristic mtemp = access$1900.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_ACCEPT_INVITATION);
                gatt.setCharacteristicNotification(mtemp, true);
                mtemp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeCharacteristic(mtemp);
                return;
            }
            SemWifiApSmartUtil unused3 = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil;
            if (SemWifiApSmartUtil.CHARACTERISTIC_CLIENT_MAC.equals(characteristic.getUuid())) {
                byte[] mbytes2 = characteristic.getValue();
                if (mbytes2 != null && mbytes2[0] == 1) {
                    String mClientMAC = new String(mbytes2, 1, 17);
                    String mDeviceName = new String(mbytes2, 19, mbytes2[18]);
                    String mClientMAC2 = mClientMAC.toLowerCase();
                    Log.d("SemWifiApSmartD2DGattClient", "got client devicename and MAC" + mClientMAC2 + ":" + mDeviceName);
                    SemWifiApSmartWhiteList.getInstance().addWhiteList(mClientMAC2, mDeviceName);
                    if (SemWifiApSmartD2DGattClient.this.mBleWorkHandler != null) {
                        SemWifiApSmartD2DGattClient.this.mBleWorkHandler.sendEmptyMessageDelayed(14, IWCEventManager.autoDisconnectThreshold);
                    }
                } else if (mbytes2[0] == 0 && SemWifiApSmartD2DGattClient.this.mBleWorkHandler != null) {
                    SemWifiApSmartD2DGattClient.this.mBleWorkHandler.sendEmptyMessageDelayed(15, IWCEventManager.autoDisconnectThreshold);
                }
                if (SemWifiApSmartD2DGattClient.this.mBleWorkHandler != null) {
                    SemWifiApSmartD2DGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                }
            }
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d("SemWifiApSmartD2DGattClient", "requestedToAccept:" + SemWifiApSmartD2DGattClient.this.requestedToAccept + ",onCharacteristicChanged:" + SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil.lookup(characteristic.getUuid()));
            SemWifiApSmartUtil unused = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil;
            if (SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_ACCEPT_INVITATION.equals(characteristic.getUuid()) && SemWifiApSmartD2DGattClient.this.requestedToAccept) {
                boolean unused2 = SemWifiApSmartD2DGattClient.this.requestedToAccept = false;
                BluetoothGattService access$1900 = SemWifiApSmartD2DGattClient.this.mBluetoothGattService;
                SemWifiApSmartUtil unused3 = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil;
                gatt.readCharacteristic(access$1900.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_CLIENT_MAC));
            }
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d("SemWifiApSmartD2DGattClient", "onCharacteristicWrite:" + SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil.lookup(characteristic.getUuid()));
            SemWifiApSmartUtil unused = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil;
            if (SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_ACCEPT_INVITATION.equals(characteristic.getUuid())) {
                BluetoothGattService access$1900 = SemWifiApSmartD2DGattClient.this.mBluetoothGattService;
                SemWifiApSmartUtil unused2 = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil;
                BluetoothGattCharacteristic mtemp = access$1900.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_FAMILY_ID);
                byte[] mdata = new byte[150];
                String mFamilyId = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil.getFamilyID();
                String mDeviceName = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil.getDeviceName();
                String mWiFiMAC = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil.getOwnWifiMac();
                if (mFamilyId == null || mFamilyId.equals("")) {
                    Log.e("SemWifiApSmartD2DGattClient", "family id is null shutting down");
                    if (SemWifiApSmartD2DGattClient.this.mBleWorkHandler != null) {
                        SemWifiApSmartD2DGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                        return;
                    }
                    return;
                }
                Log.d("SemWifiApSmartD2DGattClient", "sending familyid to client");
                byte[] tdata = mFamilyId.getBytes();
                byte[] tdeviceName = mDeviceName.getBytes();
                byte[] tWiFiMAC = mWiFiMAC.getBytes();
                mdata[0] = (byte) tdata.length;
                for (int i = 0; i < tdata.length; i++) {
                    mdata[i + 1] = tdata[i];
                }
                mdata[tdata.length + 1] = (byte) tdeviceName.length;
                for (int i2 = 0; i2 < tdeviceName.length; i2++) {
                    mdata[i2 + 2 + tdata.length] = tdeviceName[i2];
                }
                mdata[tdata.length + 2 + tdeviceName.length] = (byte) tWiFiMAC.length;
                for (int i3 = 0; i3 < tWiFiMAC.length; i3++) {
                    mdata[i3 + 3 + tdata.length + tdeviceName.length] = tWiFiMAC[i3];
                }
                mtemp.setValue(mdata);
                gatt.writeCharacteristic(mtemp);
                return;
            }
            SemWifiApSmartUtil unused3 = SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil;
            if (SemWifiApSmartUtil.CHARACTERISTIC_FAMILY_ID.equals(characteristic.getUuid())) {
                boolean unused4 = SemWifiApSmartD2DGattClient.this.requestedToAccept = true;
            }
        }

        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (mtu == 512) {
                gatt.discoverServices();
                if (SemWifiApSmartD2DGattClient.this.mBleWorkHandler != null && SemWifiApSmartD2DGattClient.this.mBleWorkHandler.hasMessages(16)) {
                    SemWifiApSmartD2DGattClient.this.mBleWorkHandler.removeMessages(16);
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public LocalLog mLocalLog;
    /* access modifiers changed from: private */
    public String mPendingDeviceAddress;
    /* access modifiers changed from: private */
    public boolean mRequestedToConnect = false;
    private SemWifiApSmartD2DGattClientReceiver mSemWifiApSmartD2DGattClientReceiver;
    /* access modifiers changed from: private */
    public SemWifiApSmartD2DMHS mSemWifiApSmartD2DMHS;
    /* access modifiers changed from: private */
    public SemWifiApSmartUtil mSemWifiApSmartUtil;
    private ContentObserver mSemWifiApSmart_D2D_SwitchObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            int i = Settings.Secure.getInt(SemWifiApSmartD2DGattClient.this.mContext.getContentResolver(), "wifi_ap_smart_d2d_mhs", 0);
        }
    };
    /* access modifiers changed from: private */
    public String mSmartAp_BLE_MAC;
    /* access modifiers changed from: private */
    public boolean requestedToAccept;
    private ISemWifiApSmartCallback tSemWifiApSmartCallback;

    public SemWifiApSmartD2DGattClient(Context context, SemWifiApSmartUtil semWifiApSmartUtil, LocalLog tLocalLog) {
        this.mContext = context;
        this.mSemWifiApSmartUtil = semWifiApSmartUtil;
        this.mLocalLog = tLocalLog;
        this.mSemWifiApSmartD2DGattClientReceiver = new SemWifiApSmartD2DGattClientReceiver();
        if (!FactoryTest.isFactoryBinary()) {
            this.mContext.registerReceiver(this.mSemWifiApSmartD2DGattClientReceiver, mSemWifiApSmartD2DGattClientIntentFilter);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("wifi_ap_smart_d2d_mhs"), false, this.mSemWifiApSmart_D2D_SwitchObserver);
            return;
        }
        Log.e("SemWifiApSmartD2DGattClient", "This devices's binary is a factory binary");
    }

    static {
        mSemWifiApSmartD2DGattClientIntentFilter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
        mSemWifiApSmartD2DGattClientIntentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        mSemWifiApSmartD2DGattClientIntentFilter.setPriority(ReportIdKey.ID_PATTERN_MATCHED);
    }

    class BleWorkHandler extends Handler {
        public BleWorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            List<ClientD2DInfo> list;
            int rcount;
            boolean z;
            int rcount2;
            boolean z2;
            Message message = msg;
            Log.i("SemWifiApSmartD2DGattClient", "msg.what:" + message.what);
            boolean z3 = true;
            switch (message.what) {
                case 12:
                    SemWifiApSmartD2DGattClient.this.shutdownclient();
                    return;
                case 13:
                    int rcount3 = 0;
                    List<ClientD2DInfo> list2 = SemWifiApSmartD2DGattClient.this.mClientD2DList;
                    synchronized (list2) {
                        try {
                            List<Integer> mIndexeList = new ArrayList<>();
                            int count = 0;
                            for (ClientD2DInfo var : SemWifiApSmartD2DGattClient.this.mClientD2DList) {
                                try {
                                    if (var.state == SemWifiApSmartD2DGattClient.ST_BONDING_FAILURE || var.state == -2) {
                                        rcount2 = rcount3;
                                        List<ClientD2DInfo> list3 = list2;
                                        z2 = z3;
                                        list = list3;
                                    } else if (var.state == SemWifiApSmartD2DGattClient.ST_GATT_FAILURE) {
                                        rcount2 = rcount3;
                                        List<ClientD2DInfo> list4 = list2;
                                        z2 = z3;
                                        list = list4;
                                    } else {
                                        rcount = rcount3;
                                        try {
                                            list = z3;
                                            if (System.currentTimeMillis() - var.timestamp <= 10000 || var.state >= 0) {
                                                if (var.state == 2) {
                                                    list = list2;
                                                    if (System.currentTimeMillis() - var.timestamp > 40000) {
                                                        Log.e("SemWifiApSmartD2DGattClient", " ST_BT_PAIRING after 30 sec, so cancelling bonding:" + var.clientMAC.substring(9) + ",isBondingGoingon:" + SemWifiApSmartD2DGattClient.this.isBondingGoingon);
                                                        SemWifiApSmartD2DGattClient.this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tST_BT_PAIRING after 30 sec, so cancelling bonding:" + var.clientMAC.substring(9) + ",isBondingGoingon:" + SemWifiApSmartD2DGattClient.this.isBondingGoingon);
                                                        var.state = SemWifiApSmartD2DGattClient.ST_BONDING_FAILURE;
                                                        var.timestamp = System.currentTimeMillis();
                                                        boolean unused = SemWifiApSmartD2DGattClient.this.isBondingGoingon = false;
                                                        rcount3 = rcount + 1;
                                                        try {
                                                            SemWifiApSmartD2DGattClient.this.invokeCallback(var.clientMAC, SemWifiApSmartD2DGattClient.ST_BONDING_FAILURE);
                                                            SemWifiApSmartD2DGattClient.this.mSemWifiApSmartD2DMHS.setBLEPairingFailedHistory(var.clientMAC, new Pair(Long.valueOf(System.currentTimeMillis()), SemWifiApSmartD2DGattClient.this.mSmartAp_BLE_MAC));
                                                            z = true;
                                                            count++;
                                                            boolean z4 = z;
                                                            list2 = list;
                                                            z3 = z4;
                                                        } catch (Throwable th) {
                                                            th = th;
                                                            throw th;
                                                        }
                                                    }
                                                } else {
                                                    list = list2;
                                                }
                                                if (var.state == 1 && SemWifiApSmartD2DGattClient.this.mConnectedGatt == null) {
                                                    Log.e("SemWifiApSmartD2DGattClient", " ST_GATT_CONNECTING and mConnectgatt is null :" + var.clientMAC.substring(9));
                                                    SemWifiApSmartD2DGattClient.this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tST_GATT_CONNECTING and mConnectgatt is null:" + var.clientMAC.substring(9));
                                                    var.state = 0;
                                                    var.timestamp = System.currentTimeMillis();
                                                    mIndexeList.add(Integer.valueOf(count));
                                                    SemWifiApSmartD2DGattClient.this.invokeCallback(var.clientMAC, 0);
                                                    z = true;
                                                } else if (var.state != 1 || System.currentTimeMillis() - var.timestamp <= 35000) {
                                                    if (var.state >= 0) {
                                                        z = true;
                                                        if (var.state != 1) {
                                                            if (var.state == 2) {
                                                            }
                                                        }
                                                    } else {
                                                        z = true;
                                                    }
                                                    rcount3 = rcount + 1;
                                                    count++;
                                                    boolean z42 = z;
                                                    list2 = list;
                                                    z3 = z42;
                                                } else {
                                                    Log.e("SemWifiApSmartD2DGattClient", "mConnectedGatt is not null after 40 sec, so disconnecting gatt");
                                                    SemWifiApSmartD2DGattClient.this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tmConnectedGatt is not null after 40 sec, so disconnecting gatt");
                                                    var.state = SemWifiApSmartD2DGattClient.ST_MHS_GATT_CLIENT_TIMEOUT;
                                                    var.timestamp = System.currentTimeMillis();
                                                    SemWifiApSmartD2DGattClient.this.invokeCallback(var.clientMAC, SemWifiApSmartD2DGattClient.ST_MHS_GATT_CLIENT_TIMEOUT);
                                                    rcount3 = rcount + 1;
                                                    if (SemWifiApSmartD2DGattClient.this.mConnectedGatt != null) {
                                                        SemWifiApSmartD2DGattClient.this.shutdownclient();
                                                    }
                                                    z = true;
                                                    count++;
                                                    boolean z422 = z;
                                                    list2 = list;
                                                    z3 = z422;
                                                }
                                            } else {
                                                mIndexeList.add(Integer.valueOf(count));
                                                list = list2;
                                                z = true;
                                            }
                                            rcount3 = rcount;
                                            count++;
                                            boolean z4222 = z;
                                            list2 = list;
                                            z3 = z4222;
                                        } catch (Throwable th2) {
                                            th = th2;
                                            int i = rcount;
                                            throw th;
                                        }
                                    }
                                    if (!WifiInjector.getInstance().getSemWifiApSmartD2DMHS().getBLEPairingFailedHistory(var.clientMAC)) {
                                        SemWifiApSmartD2DGattClient.this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tUPDATE_CONNECTION_FAILURES: removing," + var.clientMAC.substring(9));
                                        Log.d("SemWifiApSmartD2DGattClient", "UPDATE_CONNECTION_FAILURES: removing, " + var.clientMAC.substring(9));
                                        mIndexeList.add(Integer.valueOf(count));
                                    } else {
                                        rcount2++;
                                    }
                                    rcount3 = rcount2;
                                    count++;
                                    boolean z42222 = z;
                                    list2 = list;
                                    z3 = z42222;
                                } catch (Throwable th3) {
                                    th = th3;
                                    int i2 = rcount3;
                                    list = list2;
                                    throw th;
                                }
                            }
                            rcount = rcount3;
                            list = list2;
                            try {
                                for (Integer intValue : mIndexeList) {
                                    SemWifiApSmartD2DGattClient.this.mClientD2DList.remove(intValue.intValue());
                                }
                                if (mIndexeList.size() > 0) {
                                    SemWifiApSmartD2DGattClient.this.mSemWifiApSmartUtil.sendClientScanResultUpdateIntent("D2D gattclient, update failures");
                                }
                            } catch (IndexOutOfBoundsException e) {
                            }
                            SemWifiApSmartD2DGattClient.this.showD2DClientInfo();
                            if (rcount > 0) {
                                sendEmptyMessageDelayed(13, 10000);
                                return;
                            }
                            return;
                        } catch (Throwable th4) {
                            th = th4;
                            list = list2;
                            throw th;
                        }
                    }
                case 14:
                    Toast.makeText(SemWifiApSmartD2DGattClient.this.mContext, 17042595, 1).show();
                    SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient = SemWifiApSmartD2DGattClient.this;
                    semWifiApSmartD2DGattClient.invokeCallback(semWifiApSmartD2DGattClient.mD2DClient_MAC, 4);
                    SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient2 = SemWifiApSmartD2DGattClient.this;
                    semWifiApSmartD2DGattClient2.setConnectionState(0, semWifiApSmartD2DGattClient2.mD2DClient_MAC);
                    SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient3 = SemWifiApSmartD2DGattClient.this;
                    semWifiApSmartD2DGattClient3.removeDuplicateClientMAC(semWifiApSmartD2DGattClient3.mD2DClient_MAC);
                    return;
                case 15:
                    Toast.makeText(SemWifiApSmartD2DGattClient.this.mContext, 17042596, 1).show();
                    SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient4 = SemWifiApSmartD2DGattClient.this;
                    semWifiApSmartD2DGattClient4.setConnectionState(0, semWifiApSmartD2DGattClient4.mD2DClient_MAC);
                    SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient5 = SemWifiApSmartD2DGattClient.this;
                    semWifiApSmartD2DGattClient5.removeDuplicateClientMAC(semWifiApSmartD2DGattClient5.mD2DClient_MAC);
                    return;
                case 16:
                    Log.d("SemWifiApSmartD2DGattClient", "Device didn't get mtu callback so this device is using default value.");
                    SemWifiApSmartD2DGattClient.this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tDevice didn't get mtu callback so this device is using default value.");
                    if (SemWifiApSmartD2DGattClient.this.mConnectedGatt != null) {
                        SemWifiApSmartD2DGattClient.this.mConnectedGatt.requestMtu(512);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public void handleBootCompleted() {
        Log.d("SemWifiApSmartD2DGattClient", "handleBootCompleted");
        this.mBleWorkThread = new HandlerThread("SemWifiApSmartD2DGattClientHandler");
        this.mBleWorkThread.start();
        this.mBleWorkHandler = new BleWorkHandler(this.mBleWorkThread.getLooper());
        String tlist = Settings.Secure.getString(this.mContext.getContentResolver(), "bonded_device_d2dMHSside");
        if (tlist != null) {
            for (String str : tlist.split("\n")) {
                this.bonedDevicesFromD2D.add(str);
            }
            this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tbonded_device_clientside,booting time :" + tlist);
        }
    }

    class SemWifiApSmartD2DGattClientReceiver extends BroadcastReceiver {
        SemWifiApSmartD2DGattClientReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                int mType = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT", Integer.MIN_VALUE);
                String format = String.format(Locale.US, "%06d", new Object[]{Integer.valueOf(intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE))});
                Log.d("SemWifiApSmartD2DGattClient", "mType: " + mType + " ,device: " + device + " ,mPendingDeviceAddress: " + SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress);
                SemWifiApSmartD2DGattClient.this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tmType: " + mType + " ,device: " + device + " ,mPendingDeviceAddress: " + SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress);
                if (SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress != null && device.getAddress().equals(SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress) && mType == 2) {
                    abortBroadcast();
                    intent.setClassName("com.android.settings", "com.samsung.android.settings.wifi.mobileap.WifiApWarning");
                    intent.setFlags(268435456);
                    intent.setAction(SemWifiApBroadcastReceiver.WIFIAP_WARNING_DIALOG);
                    intent.putExtra(SemWifiApBroadcastReceiver.WIFIAP_WARNING_DIALOG_TYPE, 8);
                    SemWifiApSmartD2DGattClient.this.mContext.startActivity(intent);
                    Log.d("SemWifiApSmartD2DGattClient", "passkeyconfirm dialog");
                }
            } else if (!action.equals("android.bluetooth.adapter.action.STATE_CHANGED") || !SemWifiApSmartD2DGattClient.this.mBluetoothIsOn) {
                if (intent.getAction().equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) {
                    BluetoothDevice device2 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    switch (intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", 10)) {
                        case 10:
                            boolean unused = SemWifiApSmartD2DGattClient.this.isBondingGoingon = false;
                            Log.i("SemWifiApSmartD2DGattClient", "BOND FAILED mPendingDeviceAddress:" + SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress + ",device.getAddress():" + device2.getAddress());
                            if (SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress != null && device2.getAddress().equals(SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress)) {
                                String unused2 = SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress = null;
                                Log.d("SemWifiApSmartD2DGattClient", " client Bonding is failed");
                                SemWifiApSmartD2DGattClient.this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tclient Bonding is failed");
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        SemWifiApSmartD2DGattClient.this.setConnectionState(-2, SemWifiApSmartD2DGattClient.this.mD2DClient_MAC);
                                    }
                                }, 6000);
                                if (SemWifiApSmartD2DGattClient.this.mBleWorkHandler != null) {
                                    SemWifiApSmartD2DGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                                    return;
                                }
                                return;
                            }
                            return;
                        case 11:
                            Log.d("SemWifiApSmartD2DGattClient", "BONDing gOING ON mPendingDeviceAddress:" + SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress + ",device.getAddress():" + device2.getAddress());
                            if (SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress != null && device2.getAddress().equals(SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress)) {
                                boolean unused3 = SemWifiApSmartD2DGattClient.this.isBondingGoingon = true;
                                Log.d("SemWifiApSmartD2DGattClient", " client Bonding is going on");
                                SemWifiApSmartD2DGattClient.this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tclient Bonding is going on");
                                return;
                            }
                            return;
                        case 12:
                            boolean unused4 = SemWifiApSmartD2DGattClient.this.isBondingGoingon = false;
                            Log.i("SemWifiApSmartD2DGattClient", "BONDED mPendingDeviceAddress:" + SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress + ",device.getAddress():" + device2.getAddress());
                            if (SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress != null && device2.getAddress().equals(SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress)) {
                                String unused5 = SemWifiApSmartD2DGattClient.this.mPendingDeviceAddress = null;
                                Log.d("SemWifiApSmartD2DGattClient", "D2D MHS Bonding is done");
                                SemWifiApSmartD2DGattClient.this.bonedDevicesFromD2D.add(device2.getAddress());
                                String tpString = "";
                                Iterator it = SemWifiApSmartD2DGattClient.this.bonedDevicesFromD2D.iterator();
                                while (it.hasNext()) {
                                    tpString = tpString + ((String) it.next()) + "\n";
                                }
                                Settings.Secure.putString(SemWifiApSmartD2DGattClient.this.mContext.getContentResolver(), "bonded_device_d2dMHSside", tpString);
                                SemWifiApSmartD2DGattClient.this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tAdding to bondedd devices :" + device2.getAddress());
                                Log.d("SemWifiApSmartD2DGattClient", ":Adding to bondedd devices :" + tpString);
                                SemWifiApSmartD2DGattClient.this.mLocalLog.log("SemWifiApSmartD2DGattClient\tTrying to create a D2D connection after bonding.");
                                SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient = SemWifiApSmartD2DGattClient.this;
                                semWifiApSmartD2DGattClient.setConnectionState(1, semWifiApSmartD2DGattClient.mD2DClient_MAC);
                                if (!SemWifiApSmartD2DGattClient.this.tryToConnectToRemoteBLE(device2, false)) {
                                    SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient2 = SemWifiApSmartD2DGattClient.this;
                                    semWifiApSmartD2DGattClient2.setConnectionState(SemWifiApSmartD2DGattClient.ST_GATT_FAILURE, semWifiApSmartD2DGattClient2.mD2DClient_MAC);
                                    SemWifiApSmartD2DGattClient.this.mFailedBLEConnections.remove(device2.getAddress());
                                    return;
                                }
                                return;
                            }
                            return;
                        default:
                            return;
                    }
                }
            } else if (SemWifiApSmartD2DGattClient.this.mBluetoothDevice != null && BluetoothAdapter.getDefaultAdapter().getState() == 12) {
                Log.d("SemWifiApSmartD2DGattClient", "ACTION_STATE_CHANGED mBluetoothIsOn " + SemWifiApSmartD2DGattClient.this.mBluetoothIsOn);
                SemWifiApSmartD2DGattClient semWifiApSmartD2DGattClient3 = SemWifiApSmartD2DGattClient.this;
                String unused6 = semWifiApSmartD2DGattClient3.mPendingDeviceAddress = semWifiApSmartD2DGattClient3.mBluetoothDevice.getAddress();
                boolean unused7 = SemWifiApSmartD2DGattClient.this.mBluetoothIsOn = false;
                SemWifiApSmartD2DGattClient.this.mBluetoothDevice.createBond(2);
            }
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 19 */
    /*  JADX ERROR: IndexOutOfBoundsException in pass: RegionMakerVisitor
        java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0
        	at java.base/jdk.internal.util.Preconditions.outOfBounds(Preconditions.java:64)
        	at java.base/jdk.internal.util.Preconditions.outOfBoundsCheckIndex(Preconditions.java:70)
        	at java.base/jdk.internal.util.Preconditions.checkIndex(Preconditions.java:248)
        	at java.base/java.util.Objects.checkIndex(Objects.java:372)
        	at java.base/java.util.ArrayList.get(ArrayList.java:458)
        	at jadx.core.dex.nodes.InsnNode.getArg(InsnNode.java:101)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:611)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverseMonitorExits(RegionMaker.java:619)
        	at jadx.core.dex.visitors.regions.RegionMaker.processMonitorEnter(RegionMaker.java:561)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:133)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:86)
        	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:698)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:123)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:86)
        	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:698)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:123)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:86)
        	at jadx.core.dex.visitors.regions.RegionMaker.processMonitorEnter(RegionMaker.java:598)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:133)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:86)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:49)
        */
    public boolean connectToSmartD2DClient(java.lang.String r20, java.lang.String r21, com.samsung.android.net.wifi.ISemWifiApSmartCallback r22) {
        /*
            r19 = this;
            r7 = r19
            r8 = r20
            r9 = r21
            com.android.server.wifi.WifiInjector r0 = com.android.server.wifi.WifiInjector.getInstance()
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DMHS r0 = r0.getSemWifiApSmartD2DMHS()
            r7.mSemWifiApSmartD2DMHS = r0
            java.util.List<java.lang.String> r10 = r7.mD2DConnection
            monitor-enter(r10)
            android.bluetooth.BluetoothGatt r0 = r7.mConnectedGatt     // Catch:{ all -> 0x01a0 }
            r11 = 0
            if (r0 == 0) goto L_0x0021
            java.lang.String r0 = "SemWifiApSmartD2DGattClient"
            java.lang.String r1 = "mConnectedGatt is not null"
            android.util.Log.e(r0, r1)     // Catch:{ all -> 0x01a0 }
            monitor-exit(r10)     // Catch:{ all -> 0x01a0 }
            return r11
        L_0x0021:
            boolean r0 = r7.isBondingGoingon     // Catch:{ all -> 0x01a0 }
            if (r0 == 0) goto L_0x002e
            java.lang.String r0 = "SemWifiApSmartD2DGattClient"
            java.lang.String r1 = "isBondingGoingon is true"
            android.util.Log.e(r0, r1)     // Catch:{ all -> 0x01a0 }
            monitor-exit(r10)     // Catch:{ all -> 0x01a0 }
            return r11
        L_0x002e:
            java.util.List<com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$ClientD2DInfo> r1 = r7.mClientD2DList     // Catch:{ all -> 0x01a0 }
            monitor-enter(r1)     // Catch:{ all -> 0x01a0 }
            java.util.List<com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$ClientD2DInfo> r0 = r7.mClientD2DList     // Catch:{ all -> 0x0199 }
            java.util.Iterator r0 = r0.iterator()     // Catch:{ all -> 0x0199 }
        L_0x0037:
            boolean r2 = r0.hasNext()     // Catch:{ all -> 0x0199 }
            r12 = 2
            if (r2 == 0) goto L_0x0053
            java.lang.Object r2 = r0.next()     // Catch:{ all -> 0x0199 }
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$ClientD2DInfo r2 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient.ClientD2DInfo) r2     // Catch:{ all -> 0x0199 }
            int r3 = r2.state     // Catch:{ all -> 0x0199 }
            if (r3 != r12) goto L_0x0052
            java.lang.String r0 = "SemWifiApSmartD2DGattClient"
            java.lang.String r3 = "BLE pairing is going on, return"
            android.util.Log.e(r0, r3)     // Catch:{ all -> 0x0199 }
            monitor-exit(r1)     // Catch:{ all -> 0x0199 }
            monitor-exit(r10)     // Catch:{ all -> 0x01a0 }
            return r11
        L_0x0052:
            goto L_0x0037
        L_0x0053:
            monitor-exit(r1)     // Catch:{ all -> 0x0199 }
            r7.mD2DClient_MAC = r9     // Catch:{ all -> 0x01a0 }
            r7.mSmartAp_BLE_MAC = r8     // Catch:{ all -> 0x01a0 }
            java.lang.String r0 = "SemWifiApSmartD2DGattClient"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x01a0 }
            r1.<init>()     // Catch:{ all -> 0x01a0 }
            java.lang.String r2 = "  connectToD2DClient   address:"
            r1.append(r2)     // Catch:{ all -> 0x01a0 }
            r1.append(r8)     // Catch:{ all -> 0x01a0 }
            java.lang.String r2 = ",clientMAC:"
            r1.append(r2)     // Catch:{ all -> 0x01a0 }
            r1.append(r9)     // Catch:{ all -> 0x01a0 }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x01a0 }
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x01a0 }
            android.util.LocalLog r0 = r7.mLocalLog     // Catch:{ all -> 0x01a0 }
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x01a0 }
            r1.<init>()     // Catch:{ all -> 0x01a0 }
            java.lang.String r2 = "SemWifiApSmartD2DGattClient:\tconnectToD2DClient   address:"
            r1.append(r2)     // Catch:{ all -> 0x01a0 }
            r1.append(r8)     // Catch:{ all -> 0x01a0 }
            java.lang.String r2 = ",clientMAC:"
            r1.append(r2)     // Catch:{ all -> 0x01a0 }
            r1.append(r9)     // Catch:{ all -> 0x01a0 }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x01a0 }
            r0.log(r1)     // Catch:{ all -> 0x01a0 }
            r13 = r22
            r7.tSemWifiApSmartCallback = r13     // Catch:{ all -> 0x01a5 }
            android.bluetooth.BluetoothAdapter r0 = android.bluetooth.BluetoothAdapter.getDefaultAdapter()     // Catch:{ all -> 0x01a5 }
            r7.mBluetoothAdapter = r0     // Catch:{ all -> 0x01a5 }
            android.bluetooth.BluetoothAdapter r0 = r7.mBluetoothAdapter     // Catch:{ all -> 0x01a5 }
            if (r0 == 0) goto L_0x018f
            if (r8 != 0) goto L_0x00a6
            goto L_0x018f
        L_0x00a6:
            android.bluetooth.BluetoothAdapter r0 = r7.mBluetoothAdapter     // Catch:{ all -> 0x01a5 }
            android.bluetooth.BluetoothDevice r0 = r0.getRemoteDevice(r8)     // Catch:{ all -> 0x01a5 }
            r14 = r0
            if (r14 != 0) goto L_0x00b8
            java.lang.String r0 = "SemWifiApSmartD2DGattClient"
            java.lang.String r1 = "Device not found. Unable to connect."
            android.util.Log.e(r0, r1)     // Catch:{ all -> 0x01a5 }
            monitor-exit(r10)     // Catch:{ all -> 0x01a5 }
            return r11
        L_0x00b8:
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$BleWorkHandler r0 = r7.mBleWorkHandler     // Catch:{ all -> 0x01a5 }
            if (r0 == 0) goto L_0x00cd
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$BleWorkHandler r0 = r7.mBleWorkHandler     // Catch:{ all -> 0x01a5 }
            r1 = 13
            boolean r0 = r0.hasMessages(r1)     // Catch:{ all -> 0x01a5 }
            if (r0 != 0) goto L_0x00cd
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$BleWorkHandler r0 = r7.mBleWorkHandler     // Catch:{ all -> 0x01a5 }
            r2 = 5000(0x1388, double:2.4703E-320)
            r0.sendEmptyMessageDelayed(r1, r2)     // Catch:{ all -> 0x01a5 }
        L_0x00cd:
            int r0 = r14.getBondState()     // Catch:{ all -> 0x01a5 }
            r15 = 10
            r6 = 1
            if (r0 != r15) goto L_0x0151
            r7.removeDuplicateClientMAC(r9)     // Catch:{ all -> 0x01a5 }
            java.util.List<com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$ClientD2DInfo> r11 = r7.mClientD2DList     // Catch:{ all -> 0x01a5 }
            monitor-enter(r11)     // Catch:{ all -> 0x01a5 }
            java.util.List<com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$ClientD2DInfo> r0 = r7.mClientD2DList     // Catch:{ all -> 0x014e }
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$ClientD2DInfo r4 = new com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$ClientD2DInfo     // Catch:{ all -> 0x014e }
            long r16 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x014e }
            r18 = 2
            r1 = r4
            r2 = r19
            r3 = r21
            r15 = r4
            r4 = r16
            r6 = r18
            r1.<init>(r3, r4, r6)     // Catch:{ all -> 0x014e }
            r0.add(r15)     // Catch:{ all -> 0x014e }
            monitor-exit(r11)     // Catch:{ all -> 0x014e }
            r7.invokeCallback(r9, r12)     // Catch:{ all -> 0x01a5 }
            android.bluetooth.BluetoothAdapter r0 = r7.mBluetoothAdapter     // Catch:{ all -> 0x01a5 }
            if (r0 == 0) goto L_0x0143
            android.bluetooth.BluetoothAdapter r0 = r7.mBluetoothAdapter     // Catch:{ all -> 0x01a5 }
            int r0 = r0.getState()     // Catch:{ all -> 0x01a5 }
            r1 = 10
            if (r0 != r1) goto L_0x0143
            java.lang.String r0 = "SemWifiApSmartD2DGattClient"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x01a5 }
            r1.<init>()     // Catch:{ all -> 0x01a5 }
            java.lang.String r2 = "device is not bonded, enabling BT adapter,mBluetoothIsOn:"
            r1.append(r2)     // Catch:{ all -> 0x01a5 }
            boolean r2 = r7.mBluetoothIsOn     // Catch:{ all -> 0x01a5 }
            r1.append(r2)     // Catch:{ all -> 0x01a5 }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x01a5 }
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x01a5 }
            android.util.LocalLog r0 = r7.mLocalLog     // Catch:{ all -> 0x01a5 }
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x01a5 }
            r1.<init>()     // Catch:{ all -> 0x01a5 }
            java.lang.String r2 = "SemWifiApSmartD2DGattClient:\tdevice is not bonded, enabling BT adapter,mBluetoothIsOn:"
            r1.append(r2)     // Catch:{ all -> 0x01a5 }
            boolean r2 = r7.mBluetoothIsOn     // Catch:{ all -> 0x01a5 }
            r1.append(r2)     // Catch:{ all -> 0x01a5 }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x01a5 }
            r0.log(r1)     // Catch:{ all -> 0x01a5 }
            android.bluetooth.BluetoothAdapter r0 = r7.mBluetoothAdapter     // Catch:{ all -> 0x01a5 }
            r0.enable()     // Catch:{ all -> 0x01a5 }
            r0 = 1
            r7.mBluetoothIsOn = r0     // Catch:{ all -> 0x01a5 }
            r7.mBluetoothDevice = r14     // Catch:{ all -> 0x01a5 }
            goto L_0x018a
        L_0x0143:
            r0 = 1
            java.lang.String r1 = r14.getAddress()     // Catch:{ all -> 0x01a5 }
            r7.mPendingDeviceAddress = r1     // Catch:{ all -> 0x01a5 }
            r14.createBond(r12)     // Catch:{ all -> 0x01a5 }
            goto L_0x018a
        L_0x014e:
            r0 = move-exception
            monitor-exit(r11)     // Catch:{ all -> 0x014e }
            throw r0     // Catch:{ all -> 0x01a5 }
        L_0x0151:
            r0 = r6
            r7.removeDuplicateClientMAC(r9)     // Catch:{ all -> 0x01a5 }
            java.util.List<com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$ClientD2DInfo> r12 = r7.mClientD2DList     // Catch:{ all -> 0x01a5 }
            monitor-enter(r12)     // Catch:{ all -> 0x01a5 }
            java.util.List<com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$ClientD2DInfo> r15 = r7.mClientD2DList     // Catch:{ all -> 0x018c }
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$ClientD2DInfo r6 = new com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient$ClientD2DInfo     // Catch:{ all -> 0x018c }
            long r4 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x018c }
            r16 = 1
            r1 = r6
            r2 = r19
            r3 = r21
            r11 = r6
            r6 = r16
            r1.<init>(r3, r4, r6)     // Catch:{ all -> 0x018c }
            r15.add(r11)     // Catch:{ all -> 0x018c }
            monitor-exit(r12)     // Catch:{ all -> 0x018c }
            r7.invokeCallback(r9, r0)     // Catch:{ all -> 0x01a5 }
            r1 = 0
            boolean r1 = r7.tryToConnectToRemoteBLE(r14, r1)     // Catch:{ all -> 0x01a5 }
            if (r1 != 0) goto L_0x018a
            r1 = -5
            java.lang.String r2 = r7.mD2DClient_MAC     // Catch:{ all -> 0x01a5 }
            r7.setConnectionState(r1, r2)     // Catch:{ all -> 0x01a5 }
            java.util.HashMap<java.lang.String, java.lang.Integer> r1 = r7.mFailedBLEConnections     // Catch:{ all -> 0x01a5 }
            java.lang.String r2 = r14.getAddress()     // Catch:{ all -> 0x01a5 }
            r1.remove(r2)     // Catch:{ all -> 0x01a5 }
        L_0x018a:
            monitor-exit(r10)     // Catch:{ all -> 0x01a5 }
            return r0
        L_0x018c:
            r0 = move-exception
            monitor-exit(r12)     // Catch:{ all -> 0x018c }
            throw r0     // Catch:{ all -> 0x01a5 }
        L_0x018f:
            java.lang.String r0 = "SemWifiApSmartD2DGattClient"
            java.lang.String r1 = "BluetoothAdapter not initialized or unspecified address."
            android.util.Log.e(r0, r1)     // Catch:{ all -> 0x01a5 }
            monitor-exit(r10)     // Catch:{ all -> 0x01a5 }
            r0 = 0
            return r0
        L_0x0199:
            r0 = move-exception
            r13 = r22
        L_0x019c:
            monitor-exit(r1)     // Catch:{ all -> 0x019e }
            throw r0     // Catch:{ all -> 0x01a5 }
        L_0x019e:
            r0 = move-exception
            goto L_0x019c
        L_0x01a0:
            r0 = move-exception
            r13 = r22
        L_0x01a3:
            monitor-exit(r10)     // Catch:{ all -> 0x01a5 }
            throw r0
        L_0x01a5:
            r0 = move-exception
            goto L_0x01a3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient.connectToSmartD2DClient(java.lang.String, java.lang.String, com.samsung.android.net.wifi.ISemWifiApSmartCallback):boolean");
    }

    /* access modifiers changed from: private */
    public void shutdownclient_1() {
        Log.d("SemWifiApSmartD2DGattClient", "shutdownclient_1");
        BluetoothGatt bluetoothGatt = this.mConnectedGatt;
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
        this.mRequestedToConnect = false;
        this.requestedToAccept = false;
        this.mConnectedGatt = null;
    }

    /* access modifiers changed from: private */
    public void shutdownclient() {
        Log.d("SemWifiApSmartD2DGattClient", "shutdownclient");
        BluetoothGatt bluetoothGatt = this.mConnectedGatt;
        if (bluetoothGatt != null) {
            this.mFailedBLEConnections.remove(bluetoothGatt.getDevice().getAddress());
            this.mConnectedGatt.close();
            try {
                Thread.sleep(6000);
            } catch (Exception e) {
            }
        }
        this.mRequestedToConnect = false;
        this.requestedToAccept = false;
        this.mConnectedGatt = null;
    }

    private class ClientD2DInfo {
        public String clientMAC;
        public int state;
        public long timestamp;

        public ClientD2DInfo(String mClientMAC, long mTimeStamp, int mState) {
            this.clientMAC = mClientMAC;
            this.timestamp = mTimeStamp;
            this.state = mState;
        }

        public String toString() {
            return String.format("clientMAC:" + this.clientMAC + ",timestamp:" + this.timestamp + ",state:" + this.state, new Object[0]);
        }
    }

    public int getSmartD2DClientConnectedStatus(String clientmac) {
        if (clientmac == null) {
            return 0;
        }
        synchronized (this.mClientD2DList) {
            for (ClientD2DInfo inf : this.mClientD2DList) {
                if (inf.clientMAC != null && inf.clientMAC.equalsIgnoreCase(clientmac)) {
                    Log.d("SemWifiApSmartD2DGattClient", "getSmartD2DClientConnectedStatus clientmac:" + clientmac + ",:state:" + inf.state);
                    if (inf.state < 0 && System.currentTimeMillis() - inf.timestamp > 45000 && this.mBleWorkHandler != null && !this.mBleWorkHandler.hasMessages(13)) {
                        Log.d("SemWifiApSmartD2DGattClient", "getSmartD2DClientConnectedStatus, specialcase:" + inf.clientMAC);
                        LocalLog localLog = this.mLocalLog;
                        localLog.log("SemWifiApSmartD2DGattClient:\tgetSmartD2DClientConnectedStatus, specialcase:" + inf.clientMAC);
                        this.mBleWorkHandler.sendEmptyMessageDelayed(13, 10);
                    }
                    int i = inf.state;
                    return i;
                }
            }
            return 0;
        }
    }

    /* access modifiers changed from: private */
    public void setConnectionState(int state, String clientmac) {
        Log.d("SemWifiApSmartD2DGattClient", "setConnectionState state " + state + "clientmac" + clientmac);
        LocalLog localLog = this.mLocalLog;
        localLog.log("SemWifiApSmartD2DGattClient:\tsetConnectionState state :" + state + "clientmac" + clientmac);
        if (state == ST_BONDING_FAILURE || state == -2 || state == ST_GATT_FAILURE) {
            this.mSemWifiApSmartD2DMHS.setBLEPairingFailedHistory(clientmac, new Pair(Long.valueOf(System.currentTimeMillis()), this.mSmartAp_BLE_MAC));
        }
        updateClientD2DConnectionStatus(clientmac, state);
        ISemWifiApSmartCallback iSemWifiApSmartCallback = this.tSemWifiApSmartCallback;
        if (iSemWifiApSmartCallback != null) {
            try {
                iSemWifiApSmartCallback.onStateChanged(state, clientmac);
            } catch (Exception e) {
            }
        }
    }

    /* access modifiers changed from: private */
    public void invokeCallback(String clientmac, int state) {
        Log.d("SemWifiApSmartD2DGattClient", "invokeCallback state " + state + "clientmac" + clientmac);
        LocalLog localLog = this.mLocalLog;
        localLog.log("SemWifiApSmartD2DGattClient:\tsinvokeCallback state :" + state + "clientmac" + clientmac);
        ISemWifiApSmartCallback iSemWifiApSmartCallback = this.tSemWifiApSmartCallback;
        if (iSemWifiApSmartCallback != null) {
            try {
                iSemWifiApSmartCallback.onStateChanged(state, clientmac);
            } catch (Exception e) {
            }
        }
    }

    private void updateClientD2DConnectionStatus(String clientmac, int state) {
        synchronized (this.mClientD2DList) {
            Iterator<ClientD2DInfo> it = this.mClientD2DList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ClientD2DInfo inf = it.next();
                if (inf.clientMAC.equalsIgnoreCase(clientmac)) {
                    inf.state = state;
                    Log.d("SemWifiApSmartD2DGattClient", "update state clientmac:" + clientmac + ",state:" + state);
                    inf.timestamp = System.currentTimeMillis();
                    break;
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void showD2DClientInfo() {
        synchronized (this.mClientD2DList) {
            Log.d("SemWifiApSmartD2DGattClient", "================================================");
            this.mLocalLog.log("SemWifiApSmartD2DGattClient:\t================================================");
            Log.d("SemWifiApSmartD2DGattClient", "showing D2D Client states");
            this.mLocalLog.log("SemWifiApSmartD2DGattClient:\tshowing D2D Client states");
            for (ClientD2DInfo var : this.mClientD2DList) {
                Log.d("SemWifiApSmartD2DGattClient", "" + var);
                LocalLog localLog = this.mLocalLog;
                localLog.log("SemWifiApSmartD2DGattClient:\t" + var);
            }
            Log.d("SemWifiApSmartD2DGattClient", "================================================");
            this.mLocalLog.log("SemWifiApSmartD2DGattClient:\t================================================");
        }
    }

    /* access modifiers changed from: package-private */
    public void removeDuplicateClientMAC(String clientmac) {
        synchronized (this.mClientD2DList) {
            int index = -1;
            int count = 0;
            Iterator<ClientD2DInfo> it = this.mClientD2DList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                } else if (it.next().clientMAC.equalsIgnoreCase(clientmac)) {
                    index = count;
                    break;
                } else {
                    count++;
                }
            }
            if (index != -1) {
                this.mClientD2DList.remove(index);
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean tryToConnectToRemoteBLE(BluetoothDevice mDevice, boolean autoConnect) {
        String mBTaddr = mDevice.getAddress();
        int count = 0;
        if (this.mFailedBLEConnections.containsKey(mBTaddr)) {
            count = this.mFailedBLEConnections.get(mBTaddr).intValue();
        }
        int count2 = count + 1;
        this.mFailedBLEConnections.put(mBTaddr, Integer.valueOf(count2));
        Log.e("SemWifiApSmartD2DGattClient", "Trying to create a new connection. attempt:" + count2);
        LocalLog localLog = this.mLocalLog;
        localLog.log("SemWifiApSmartD2DGattClient\tTrying to create a new connection. attempt:" + count2);
        setConnectionState(1, this.mD2DClient_MAC);
        this.mConnectedGatt = mDevice.connectGatt(this.mContext, autoConnect, this.mGattCallback, 2);
        if (this.mConnectedGatt == null) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            Log.e("SemWifiApSmartD2DGattClient", "mConnectedGatt = null, Trying to create a new connection. attempt:" + count2);
            LocalLog localLog2 = this.mLocalLog;
            localLog2.log("SemWifiApSmartD2DGattClient\tmConnectedGatt = null, Trying to create a new connection. attempt:" + count2);
            setConnectionState(1, this.mD2DClient_MAC);
            this.mConnectedGatt = mDevice.connectGatt(this.mContext, true, this.mGattCallback, 2);
            if (this.mConnectedGatt == null) {
                Log.e("SemWifiApSmartD2DGattClient", " mConnectedGatt = null, returning false");
                this.mLocalLog.log("SemWifiApSmartD2DGattClient\tmConnectedGatt = null, returning false");
                return false;
            }
        }
        this.mRequestedToConnect = true;
        return true;
    }
}
