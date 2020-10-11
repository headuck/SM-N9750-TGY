package com.samsung.android.server.wifi.softap.smarttethering;

import android.app.NotificationManager;
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
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.iwc.IWCEventManager;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.net.wifi.SemWifiApContentProviderHelper;
import com.samsung.android.server.wifi.dqa.ReportIdKey;
import com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver;
import com.sec.android.app.CscFeatureTagWifi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class SemWifiApSmartGattClient {
    private static final String ACTION_LOGOUT_ACCOUNTS_COMPLETE = "com.samsung.account.SAMSUNGACCOUNT_SIGNOUT_COMPLETED";
    private static final String CONFIG_OP_BRANDING = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDING);
    private static final int CONNECTION_DELAY_SEC = 5000;
    private static final int ST_AUTH_FAILED = -3;
    private static final int ST_BONDING_FAILURE = -10;
    private static final int ST_BONDING_GOINGON = -5;
    private static final int ST_BOND_FAILED = -2;
    private static final int ST_CONNECTION_ALREADY_EXIST = -4;
    private static final int ST_DEVICE_NOT_FOUND = -1;
    private static final int ST_GATT_CONNECTING = 1;
    private static final int ST_GATT_FAILURE = -7;
    private static final int ST_MHS_ENABLING_FAILURE = -8;
    private static final int ST_MHS_GATT_CLIENT_TIMEOUT = -12;
    private static final int ST_MHS_GATT_SERVICE_NOT_FOUND = -13;
    private static final int ST_MHS_USERNAME_FAILED = -9;
    private static final int ST_MHS_WIFI_CONNECTION_TIMEOUT = -11;
    private static final int ST_WIFI_CONNECTED = 3;
    private static final int ST_WIFI_CONNECTING = 2;
    private static final int ST_WIFI_DISCONNECTED = 0;
    private static IntentFilter mSemWifiApSmartGattClientIntentFilter = new IntentFilter("android.net.wifi.SCAN_RESULTS");
    private final int DISCONNECT_GATT = 12;
    private final int DISCONNECT_HOTSPOT = 14;
    private final int GATT_CONNECTION_TIMEOUT = 45000;
    private final int GATT_TRANSACTION_TIMEOUT = 60000;
    private final int GENERATE_CONNECT_WIFI = 11;
    private final int MHS_CONNECTION_TIMEOUT = 25000;
    /* access modifiers changed from: private */
    public String TAG = "SemWifiApSmartGattClient";
    private final int UPDATE_CONNECTION_FAILURES = 18;
    private final int UPDATE_CONNECTION_FAILURES_TIMER = 5000;
    private final int WAIT_FOR_MTU_CALLBACK = 19;
    /* access modifiers changed from: private */
    public HashSet<String> bonedDevicesFromHotspotLive = new HashSet<>();
    /* access modifiers changed from: private */
    public boolean isBondingGoingon = false;
    private boolean isShowingDisConnectNotification = false;
    /* access modifiers changed from: private */
    public String mAESKey = null;
    /* access modifiers changed from: private */
    public BleWorkHandler mBleWorkHandler = null;
    private HandlerThread mBleWorkThread = null;
    /* access modifiers changed from: private */
    public BluetoothAdapter mBluetoothAdapter;
    /* access modifiers changed from: private */
    public BluetoothDevice mBluetoothDevice;
    /* access modifiers changed from: private */
    public BluetoothGattService mBluetoothGattService;
    /* access modifiers changed from: private */
    public boolean mBluetoothIsOn = false;
    /* access modifiers changed from: private */
    public BluetoothGatt mConnectedGatt = null;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public long mDelayStartFrom = -1;
    HashMap<String, Integer> mFailedBLEConnections = new HashMap<>();
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            LocalLog access$100 = SemWifiApSmartGattClient.this.mLocalLog;
            access$100.log(SemWifiApSmartGattClient.this.TAG + ":\tonConnectionStateChange " + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getStatusDescription(status) + " " + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getStateDescription(newState));
            String access$000 = SemWifiApSmartGattClient.this.TAG;
            Log.d(access$000, "onConnectionStateChange: " + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getStatusDescription(status) + " " + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getStateDescription(newState));
            if (newState == 2) {
                String access$0002 = SemWifiApSmartGattClient.this.TAG;
                Log.d(access$0002, "device,connected" + gatt.getDevice());
                if (SemWifiApSmartGattClient.this.mBleWorkHandler != null) {
                    SemWifiApSmartGattClient.this.mBleWorkHandler.sendEmptyMessageDelayed(19, 300);
                }
            } else if (newState == 0) {
                String access$0003 = SemWifiApSmartGattClient.this.TAG;
                Log.d(access$0003, "device, disconnected" + gatt.getDevice());
                if (status != 0) {
                    String mBTaddr = gatt.getDevice().getAddress();
                    Integer count = SemWifiApSmartGattClient.this.mFailedBLEConnections.get(mBTaddr);
                    if (count == null) {
                        SemWifiApSmartGattClient semWifiApSmartGattClient = SemWifiApSmartGattClient.this;
                        semWifiApSmartGattClient.setConnectionState(SemWifiApSmartGattClient.ST_GATT_FAILURE, semWifiApSmartGattClient.mSmartAp_WiFi_MAC);
                        SemWifiApSmartGattClient.this.mFailedBLEConnections.remove(mBTaddr);
                        SemWifiApSmartGattClient.this.shutdownclient();
                    } else if (count.intValue() >= 3) {
                        SemWifiApSmartGattClient semWifiApSmartGattClient2 = SemWifiApSmartGattClient.this;
                        semWifiApSmartGattClient2.setConnectionState(SemWifiApSmartGattClient.ST_GATT_FAILURE, semWifiApSmartGattClient2.mSmartAp_WiFi_MAC);
                        SemWifiApSmartGattClient.this.mFailedBLEConnections.remove(mBTaddr);
                        SemWifiApSmartGattClient.this.shutdownclient();
                    } else if (count.intValue() < 3) {
                        SemWifiApSmartGattClient.this.shutdownclient_1();
                        gatt.refresh();
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                        if (gatt.getDevice().getBondState() == 12) {
                            if (!SemWifiApSmartGattClient.this.tryToConnectToRemoteBLE(gatt.getDevice(), true)) {
                                SemWifiApSmartGattClient semWifiApSmartGattClient3 = SemWifiApSmartGattClient.this;
                                semWifiApSmartGattClient3.setConnectionState(SemWifiApSmartGattClient.ST_GATT_FAILURE, semWifiApSmartGattClient3.mSmartAp_WiFi_MAC);
                                SemWifiApSmartGattClient.this.mFailedBLEConnections.remove(mBTaddr);
                                SemWifiApSmartGattClient.this.shutdownclient();
                            }
                        } else if (!SemWifiApSmartGattClient.this.tryToConnectToRemoteBLE(gatt.getDevice(), false)) {
                            SemWifiApSmartGattClient semWifiApSmartGattClient4 = SemWifiApSmartGattClient.this;
                            semWifiApSmartGattClient4.setConnectionState(SemWifiApSmartGattClient.ST_GATT_FAILURE, semWifiApSmartGattClient4.mSmartAp_WiFi_MAC);
                            SemWifiApSmartGattClient.this.mFailedBLEConnections.remove(mBTaddr);
                            SemWifiApSmartGattClient.this.shutdownclient();
                        }
                    }
                } else {
                    if (gatt.getDevice() != null) {
                        SemWifiApSmartGattClient.this.mFailedBLEConnections.remove(gatt.getDevice().getAddress());
                    }
                    SemWifiApSmartGattClient.this.shutdownclient();
                }
            }
        }

        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (mtu == 512) {
                gatt.discoverServices();
                if (SemWifiApSmartGattClient.this.mBleWorkHandler != null && SemWifiApSmartGattClient.this.mBleWorkHandler.hasMessages(19)) {
                    SemWifiApSmartGattClient.this.mBleWorkHandler.removeMessages(19);
                }
            }
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String access$000 = SemWifiApSmartGattClient.this.TAG;
            Log.d(access$000, "onCharacteristicChanged:" + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.lookup(characteristic.getUuid()));
            LocalLog access$100 = SemWifiApSmartGattClient.this.mLocalLog;
            access$100.log(SemWifiApSmartGattClient.this.TAG + "\tonCharacteristicChanged:" + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.lookup(characteristic.getUuid()));
            SemWifiApSmartUtil unused = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
            if (SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_MHS_ENABLED.equals(characteristic.getUuid()) && SemWifiApSmartGattClient.this.requestedToEnableMHS) {
                boolean unused2 = SemWifiApSmartGattClient.this.requestedToEnableMHS = false;
                BluetoothGattService access$1200 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                SemWifiApSmartUtil unused3 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                gatt.readCharacteristic(access$1200.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_STATUS_UUID));
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            String access$000 = SemWifiApSmartGattClient.this.TAG;
            Log.d(access$000, "onServicesDiscovered:" + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getStatusDescription(status));
            boolean found = false;
            for (BluetoothGattService service : gatt.getServices()) {
                String access$0002 = SemWifiApSmartGattClient.this.TAG;
                Log.d(access$0002, "Service: " + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.lookup(service.getUuid()));
                SemWifiApSmartUtil unused = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                if (SemWifiApSmartUtil.SERVICE_UUID.equals(service.getUuid())) {
                    Log.i(SemWifiApSmartGattClient.this.TAG, "Service: mSemWifiApSmartUtil.SERVICE_UUID");
                    LocalLog access$100 = SemWifiApSmartGattClient.this.mLocalLog;
                    access$100.log(SemWifiApSmartGattClient.this.TAG + "\tService: mSemWifiApSmartUtil.SERVICE_UUID");
                    BluetoothGattService unused2 = SemWifiApSmartGattClient.this.mBluetoothGattService = service;
                    found = true;
                    if (SemWifiApSmartGattClient.this.mAESKey != null) {
                        Log.i(SemWifiApSmartGattClient.this.TAG, "read CHARACTERISTIC_MHS_SIDE_GET_TIME");
                        BluetoothGattService access$1200 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                        SemWifiApSmartUtil unused3 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                        gatt.readCharacteristic(access$1200.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_SIDE_GET_TIME));
                    } else {
                        Log.i(SemWifiApSmartGattClient.this.TAG, "read CHARACTERISTIC_MHS_BOND_STATUS");
                        BluetoothGattService access$12002 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                        SemWifiApSmartUtil unused4 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                        gatt.readCharacteristic(access$12002.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_BOND_STATUS));
                    }
                }
            }
            if (!found) {
                SemWifiApSmartGattClient semWifiApSmartGattClient = SemWifiApSmartGattClient.this;
                semWifiApSmartGattClient.setConnectionState(SemWifiApSmartGattClient.ST_MHS_GATT_SERVICE_NOT_FOUND, semWifiApSmartGattClient.mSmartAp_WiFi_MAC);
                if (SemWifiApSmartGattClient.this.mBleWorkHandler != null) {
                    SemWifiApSmartGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                }
            }
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] mDeviceNametemp;
            byte[] temp;
            byte[] temp2;
            byte[] temp3;
            BluetoothGatt bluetoothGatt = gatt;
            int i = status;
            super.onCharacteristicWrite(gatt, characteristic, status);
            String access$000 = SemWifiApSmartGattClient.this.TAG;
            Log.d(access$000, "onCharacteristicWrite:" + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.lookup(characteristic.getUuid()) + ",status:" + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getStatusDescription(i));
            LocalLog access$100 = SemWifiApSmartGattClient.this.mLocalLog;
            access$100.log(SemWifiApSmartGattClient.this.TAG + "\tonCharacteristicWrite:" + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.lookup(characteristic.getUuid()) + ",status:" + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getStatusDescription(i));
            SemWifiApSmartUtil unused = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
            if (SemWifiApSmartUtil.CHARACTERISTIC_MHS_VER_UPDATE.equals(characteristic.getUuid())) {
                BluetoothGattService access$1200 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                SemWifiApSmartUtil unused2 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                BluetoothGattCharacteristic mtemp = access$1200.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_MHS_ENABLED);
                bluetoothGatt.setCharacteristicNotification(mtemp, true);
                mtemp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeCharacteristic(mtemp);
                return;
            }
            SemWifiApSmartUtil unused3 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
            if (SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_MHS_ENABLED.equals(characteristic.getUuid())) {
                BluetoothGattService access$12002 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                SemWifiApSmartUtil unused4 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                BluetoothGattCharacteristic mtemp2 = access$12002.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_ENCRYPTED_AUTH_ID);
                String mDeviceName = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getHostNameFromDeviceName();
                String mDeviceWifiMAC = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getOwnWifiMac();
                String mFamilyGuid = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getGuid();
                if (mFamilyGuid == null) {
                    mFamilyGuid = "";
                }
                int length = mFamilyGuid.length();
                if (mDeviceName == null) {
                    mDeviceNametemp = "".getBytes();
                } else {
                    mDeviceNametemp = mDeviceName.getBytes();
                }
                byte[] mDeviceWifiMACtemp = mDeviceWifiMAC.getBytes();
                byte[] mGuidBytes = mFamilyGuid.getBytes();
                if (SemWifiApSmartGattClient.this.mUserType == 1) {
                    Log.d(SemWifiApSmartGattClient.this.TAG, "Autheticating for same GUID");
                    LocalLog access$1002 = SemWifiApSmartGattClient.this.mLocalLog;
                    access$1002.log(SemWifiApSmartGattClient.this.TAG + ":\tAutheticating for same GUID");
                    if (SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getGuid() == null) {
                        temp3 = "".getBytes();
                    } else {
                        temp3 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getGuid().getBytes();
                    }
                    byte[] mdata = new byte[(temp3.length + 10 + mDeviceNametemp.length + mDeviceWifiMACtemp.length)];
                    mdata[0] = 1;
                    mdata[1] = (byte) temp3.length;
                    for (int i2 = 0; i2 < temp3.length; i2++) {
                        mdata[i2 + 2] = temp3[i2];
                    }
                    mdata[temp3.length + 2] = (byte) mDeviceNametemp.length;
                    for (int i3 = 0; i3 < mDeviceNametemp.length; i3++) {
                        mdata[i3 + 2 + temp3.length + 1] = mDeviceNametemp[i3];
                    }
                    mdata[temp3.length + 3 + mDeviceNametemp.length] = (byte) mDeviceWifiMACtemp.length;
                    for (int i4 = 0; i4 < mDeviceWifiMACtemp.length; i4++) {
                        mdata[i4 + 4 + temp3.length + mDeviceNametemp.length] = mDeviceWifiMACtemp[i4];
                    }
                    if (SemWifiApSmartGattClient.this.mAESKey != null) {
                        String access$0002 = SemWifiApSmartGattClient.this.TAG;
                        Log.d(access$0002, "Using AES:" + SemWifiApSmartGattClient.this.mAESKey);
                        byte[] mAESdata = AES.encrypt(new String(mdata), SemWifiApSmartGattClient.this.mAESKey).getBytes();
                        if (mAESdata == null) {
                            Log.e(SemWifiApSmartGattClient.this.TAG, " Encryption can't be null");
                        }
                        String access$0003 = SemWifiApSmartGattClient.this.TAG;
                        Log.d(access$0003, "Encrypted size is" + mAESdata.length + "," + new String(mAESdata));
                        mtemp2.setValue(mAESdata);
                    } else {
                        mtemp2.setValue(mdata);
                    }
                } else if (SemWifiApSmartGattClient.this.mUserType == 2) {
                    Log.d(SemWifiApSmartGattClient.this.TAG, "Autheticating for same family ID");
                    LocalLog access$1003 = SemWifiApSmartGattClient.this.mLocalLog;
                    access$1003.log(SemWifiApSmartGattClient.this.TAG + ":\tAutheticating for same family ID");
                    if (SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getFamilyID() == null) {
                        temp2 = "".getBytes();
                    } else {
                        temp2 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getFamilyID().getBytes();
                    }
                    byte[] mdata2 = new byte[(temp2.length + 10 + mDeviceNametemp.length + mDeviceWifiMACtemp.length + mGuidBytes.length)];
                    mdata2[0] = 2;
                    mdata2[1] = (byte) temp2.length;
                    for (int i5 = 0; i5 < temp2.length; i5++) {
                        mdata2[i5 + 2] = temp2[i5];
                    }
                    mdata2[temp2.length + 2] = (byte) mDeviceNametemp.length;
                    for (int i6 = 0; i6 < mDeviceNametemp.length; i6++) {
                        mdata2[i6 + 2 + temp2.length + 1] = mDeviceNametemp[i6];
                    }
                    mdata2[temp2.length + 3 + mDeviceNametemp.length] = (byte) mDeviceWifiMACtemp.length;
                    for (int i7 = 0; i7 < mDeviceWifiMACtemp.length; i7++) {
                        mdata2[i7 + 4 + temp2.length + mDeviceNametemp.length] = mDeviceWifiMACtemp[i7];
                    }
                    mdata2[temp2.length + 4 + mDeviceNametemp.length + mDeviceWifiMACtemp.length] = (byte) mGuidBytes.length;
                    for (int i8 = 0; i8 < mGuidBytes.length; i8++) {
                        mdata2[i8 + 5 + temp2.length + mDeviceNametemp.length + mDeviceWifiMACtemp.length] = mGuidBytes[i8];
                    }
                    mtemp2.setValue(mdata2);
                } else if (SemWifiApSmartGattClient.this.mUserType == 3) {
                    Log.d(SemWifiApSmartGattClient.this.TAG, "Autheticating for Allowed User");
                    LocalLog access$1004 = SemWifiApSmartGattClient.this.mLocalLog;
                    access$1004.log(SemWifiApSmartGattClient.this.TAG + ":\tAutheticating for Allowed User");
                    if (SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getD2DFamilyID() == null) {
                        temp = "".getBytes();
                    } else {
                        temp = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getD2DFamilyID().getBytes();
                    }
                    byte[] mdata3 = new byte[(temp.length + 10 + mDeviceNametemp.length + mDeviceWifiMACtemp.length)];
                    mdata3[0] = 3;
                    mdata3[1] = (byte) temp.length;
                    for (int i9 = 0; i9 < temp.length; i9++) {
                        mdata3[i9 + 2] = temp[i9];
                    }
                    mdata3[temp.length + 2] = (byte) mDeviceNametemp.length;
                    for (int i10 = 0; i10 < mDeviceNametemp.length; i10++) {
                        mdata3[i10 + 2 + temp.length + 1] = mDeviceNametemp[i10];
                    }
                    mdata3[temp.length + 3 + mDeviceNametemp.length] = (byte) mDeviceWifiMACtemp.length;
                    for (int i11 = 0; i11 < mDeviceWifiMACtemp.length; i11++) {
                        mdata3[i11 + 4 + temp.length + mDeviceNametemp.length] = mDeviceWifiMACtemp[i11];
                    }
                    mtemp2.setValue(mdata3);
                }
                String access$0004 = SemWifiApSmartGattClient.this.TAG;
                Log.d(access$0004, "Write Characterstic:" + mDeviceName);
                bluetoothGatt.writeCharacteristic(mtemp2);
                return;
            }
            SemWifiApSmartUtil unused5 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
            if (SemWifiApSmartUtil.CHARACTERISTIC_ENCRYPTED_AUTH_ID.equals(characteristic.getUuid())) {
                BluetoothGattService access$12003 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                SemWifiApSmartUtil unused6 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                bluetoothGatt.readCharacteristic(access$12003.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_AUTH_STATUS));
            }
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BluetoothGatt bluetoothGatt = gatt;
            int i = status;
            super.onCharacteristicRead(gatt, characteristic, status);
            String access$000 = SemWifiApSmartGattClient.this.TAG;
            Log.d(access$000, "onCharacteristicRead:" + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.lookup(characteristic.getUuid()) + ",status:" + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getStatusDescription(i));
            LocalLog access$100 = SemWifiApSmartGattClient.this.mLocalLog;
            access$100.log(SemWifiApSmartGattClient.this.TAG + ":\tonCharacteristicRead:" + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.lookup(characteristic.getUuid()) + ",status:" + SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getStatusDescription(i));
            SemWifiApSmartUtil unused = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
            if (SemWifiApSmartUtil.CHARACTERISTIC_MHS_SIDE_GET_TIME.equals(characteristic.getUuid())) {
                byte[] mbytes = characteristic.getValue();
                String access$0002 = SemWifiApSmartGattClient.this.TAG;
                Log.i(access$0002, "received mhs time:" + Arrays.toString(mbytes));
                if (mbytes != null) {
                    long mhs_time = Long.parseLong(new String(mbytes));
                    SemWifiApSmartGattClient semWifiApSmartGattClient = SemWifiApSmartGattClient.this;
                    String unused2 = semWifiApSmartGattClient.mAESKey = semWifiApSmartGattClient.mSemWifiApSmartUtil.getAESKey(mhs_time);
                    String access$0003 = SemWifiApSmartGattClient.this.TAG;
                    Log.d(access$0003, "received mhs_time" + mhs_time + ",mAESKey:" + SemWifiApSmartGattClient.this.mAESKey);
                }
                if (mbytes == null || SemWifiApSmartGattClient.this.mAESKey == null) {
                    Log.e(SemWifiApSmartGattClient.this.TAG, "Time mismatch ocured,need to establish 1.0 connection");
                    boolean unused3 = SemWifiApSmartGattClient.this.mTimeMismatchOccured = true;
                    if (gatt.getDevice().getBondState() != 10) {
                        BluetoothGattService access$1200 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                        SemWifiApSmartUtil unused4 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                        bluetoothGatt.readCharacteristic(access$1200.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_BOND_STATUS));
                    } else if (SemWifiApSmartGattClient.this.mBluetoothAdapter == null || SemWifiApSmartGattClient.this.mBluetoothAdapter.getState() != 10) {
                        String unused5 = SemWifiApSmartGattClient.this.mPendingDeviceAddress = gatt.getDevice().getAddress();
                        gatt.getDevice().createBond(2);
                    } else {
                        String access$0004 = SemWifiApSmartGattClient.this.TAG;
                        Log.d(access$0004, "device is not bonded, enabling BT adapter,mBluetoothIsOn:" + SemWifiApSmartGattClient.this.mBluetoothIsOn);
                        LocalLog access$1002 = SemWifiApSmartGattClient.this.mLocalLog;
                        access$1002.log(SemWifiApSmartGattClient.this.TAG + ":\tdevice is not bonded, enabling BT adapter,mBluetoothIsOn:" + SemWifiApSmartGattClient.this.mBluetoothIsOn);
                        SemWifiApSmartGattClient.this.mBluetoothAdapter.enable();
                        boolean unused6 = SemWifiApSmartGattClient.this.mBluetoothIsOn = true;
                        BluetoothDevice unused7 = SemWifiApSmartGattClient.this.mBluetoothDevice = gatt.getDevice();
                    }
                } else {
                    BluetoothGattService access$12002 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                    SemWifiApSmartUtil unused8 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                    BluetoothGattCharacteristic mtemp = access$12002.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_VER_UPDATE);
                    SemWifiApSmartUtil unused9 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                    SemWifiApSmartUtil unused10 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                    mtemp.setValue(new byte[]{1, 1});
                    String access$0005 = SemWifiApSmartGattClient.this.TAG;
                    Log.d(access$0005, "Write Characterstic version:" + SemWifiApSmartGattClient.this.mversion);
                    bluetoothGatt.writeCharacteristic(mtemp);
                }
            } else {
                SemWifiApSmartUtil unused11 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                if (SemWifiApSmartUtil.CHARACTERISTIC_MHS_BOND_STATUS.equals(characteristic.getUuid())) {
                    byte[] mbytes2 = characteristic.getValue();
                    if (mbytes2 == null || mbytes2.length <= 0 || mbytes2[0] != 1) {
                        BluetoothDevice mDevice = gatt.getDevice();
                        if (mDevice != null) {
                            Log.e(SemWifiApSmartGattClient.this.TAG, "device is not bonded at MHS side ,so removing the device");
                            LocalLog access$1003 = SemWifiApSmartGattClient.this.mLocalLog;
                            access$1003.log(SemWifiApSmartGattClient.this.TAG + ":\tdevice is not bonded at MHS side ,so removing the device");
                            mDevice.removeBond();
                        }
                        SemWifiApSmartGattClient semWifiApSmartGattClient2 = SemWifiApSmartGattClient.this;
                        semWifiApSmartGattClient2.setConnectionState(SemWifiApSmartGattClient.ST_BONDING_FAILURE, semWifiApSmartGattClient2.mSmartAp_WiFi_MAC);
                        if (SemWifiApSmartGattClient.this.mBleWorkHandler != null) {
                            SemWifiApSmartGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                            return;
                        }
                        return;
                    }
                    String access$0006 = SemWifiApSmartGattClient.this.TAG;
                    Log.d(access$0006, "Got bond status:" + mbytes2[0]);
                    LocalLog access$1004 = SemWifiApSmartGattClient.this.mLocalLog;
                    access$1004.log(SemWifiApSmartGattClient.this.TAG + ":\tGot bond status:" + mbytes2[0]);
                    BluetoothGattService access$12003 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                    SemWifiApSmartUtil unused12 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                    BluetoothGattCharacteristic mtemp2 = access$12003.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_MHS_ENABLED);
                    bluetoothGatt.setCharacteristicNotification(mtemp2, true);
                    mtemp2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.writeCharacteristic(mtemp2);
                    return;
                }
                SemWifiApSmartUtil unused13 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                if (SemWifiApSmartUtil.CHARACTERISTIC_AUTH_STATUS.equals(characteristic.getUuid())) {
                    byte[] mbytes3 = characteristic.getValue();
                    if (SemWifiApSmartGattClient.this.mAESKey != null) {
                        String access$0007 = SemWifiApSmartGattClient.this.TAG;
                        Log.d(access$0007, "Using AES:" + SemWifiApSmartGattClient.this.mAESKey);
                        if (mbytes3 != null) {
                            mbytes3 = AES.decrypt(new String(mbytes3), SemWifiApSmartGattClient.this.mAESKey).getBytes();
                        }
                        if (mbytes3 == null) {
                            Log.e(SemWifiApSmartGattClient.this.TAG, " decryption can't be null");
                        }
                    }
                    if (mbytes3 == null || mbytes3.length <= 0 || mbytes3[0] != 1) {
                        boolean unused14 = SemWifiApSmartGattClient.this.requestedToEnableMHS = false;
                        Log.d(SemWifiApSmartGattClient.this.TAG, "Auth failed");
                        LocalLog access$1005 = SemWifiApSmartGattClient.this.mLocalLog;
                        access$1005.log(SemWifiApSmartGattClient.this.TAG + ":\tAuth failed");
                        SemWifiApSmartGattClient semWifiApSmartGattClient3 = SemWifiApSmartGattClient.this;
                        semWifiApSmartGattClient3.setConnectionState(-3, semWifiApSmartGattClient3.mSmartAp_WiFi_MAC);
                        if (SemWifiApSmartGattClient.this.mBleWorkHandler != null) {
                            SemWifiApSmartGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                        }
                        int access$3200 = SemWifiApSmartGattClient.this.mUserType;
                        SemWifiApSmartUtil unused15 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                        if (access$3200 == 2) {
                            Log.d(SemWifiApSmartGattClient.this.TAG, "Auth failed for family user");
                            LocalLog access$1006 = SemWifiApSmartGattClient.this.mLocalLog;
                            access$1006.log(SemWifiApSmartGattClient.this.TAG + ":\tAuth failed for family user");
                        }
                        if (SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getSamsungAccountCount() == 0) {
                            Log.d(SemWifiApSmartGattClient.this.TAG, "Allowed type, auth failed, so removing wifi mac");
                            LocalLog access$1007 = SemWifiApSmartGattClient.this.mLocalLog;
                            access$1007.log(SemWifiApSmartGattClient.this.TAG + ":\tAllowed type, auth failed, so removing wifi mac");
                            SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.removeWifiMACFromRegisteredList(new String(mbytes3, 1, 8));
                            String mD2DWifiAMC = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getD2DWifiMac();
                            if (mD2DWifiAMC == null || mD2DWifiAMC.isEmpty()) {
                                SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.putHashbasedonD2DFamilyid(-1);
                                SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.putD2DFamilyID((String) null);
                                Log.d(SemWifiApSmartGattClient.this.TAG, "Allowed type, no registered D2D MHS found, so removed D2DfamilyID");
                                LocalLog access$1008 = SemWifiApSmartGattClient.this.mLocalLog;
                                access$1008.log(SemWifiApSmartGattClient.this.TAG + ":\tAllowed type, no registered D2D MHS found, so removed D2DfamilyID");
                                Intent tintent = new Intent();
                                tintent.setAction("com.samsung.android.server.wifi.softap.smarttethering.d2dfamilyid");
                                SemWifiApSmartGattClient.this.mContext.sendBroadcast(tintent);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    String access$0008 = SemWifiApSmartGattClient.this.TAG;
                    Log.d(access$0008, "Got Auth status:" + mbytes3[0]);
                    LocalLog access$1009 = SemWifiApSmartGattClient.this.mLocalLog;
                    access$1009.log(SemWifiApSmartGattClient.this.TAG + ":\tGot Auth status:" + mbytes3[0]);
                    boolean unused16 = SemWifiApSmartGattClient.this.requestedToEnableMHS = true;
                    byte mSSIDlength = mbytes3[1];
                    String unused17 = SemWifiApSmartGattClient.this.mSSID = new String(mbytes3, 2, mSSIDlength);
                    byte mPasswordLength = mbytes3[mSSIDlength + 2];
                    String unused18 = SemWifiApSmartGattClient.this.mPassword = null;
                    if (mPasswordLength != 0) {
                        String unused19 = SemWifiApSmartGattClient.this.mPassword = new String(mbytes3, mSSIDlength + 2 + 1, mPasswordLength);
                    }
                    byte mhs_status = mbytes3[mSSIDlength + 3 + mPasswordLength];
                    int unused20 = SemWifiApSmartGattClient.this.mWPA3Mode = mbytes3[mSSIDlength + 3 + mPasswordLength + 1 + 8];
                    String access$0009 = SemWifiApSmartGattClient.this.TAG;
                    Log.d(access$0009, "mSSID:" + SemWifiApSmartGattClient.this.mSSID + "mhs_status:" + mhs_status + ",mWPA3Mode:" + SemWifiApSmartGattClient.this.mWPA3Mode + ",mSSIDlength:" + mSSIDlength + ",mPasswordLength:" + mPasswordLength + ",mbytes.length:" + mbytes3.length);
                    String access$00010 = SemWifiApSmartGattClient.this.TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("bytes:");
                    sb.append(Arrays.toString(mbytes3));
                    Log.d(access$00010, sb.toString());
                    LocalLog access$10010 = SemWifiApSmartGattClient.this.mLocalLog;
                    access$10010.log(SemWifiApSmartGattClient.this.TAG + ":\tmSSID:" + SemWifiApSmartGattClient.this.mSSID + "mhs_status:" + mhs_status + ",mWPA3Mode:" + SemWifiApSmartGattClient.this.mWPA3Mode + ",mSSIDlength:" + mSSIDlength + ",mPasswordLength:" + mPasswordLength + ",mbytes.length:" + mbytes3.length);
                    if (mhs_status == 1) {
                        BluetoothGattService access$12004 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                        SemWifiApSmartUtil unused21 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                        bluetoothGatt.readCharacteristic(access$12004.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_STATUS_UUID));
                        return;
                    }
                    return;
                }
                SemWifiApSmartUtil unused22 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                if (SemWifiApSmartUtil.CHARACTERISTIC_MHS_STATUS_UUID.equals(characteristic.getUuid())) {
                    byte[] mStatus = characteristic.getValue();
                    if (mStatus != null) {
                        LocalLog access$10011 = SemWifiApSmartGattClient.this.mLocalLog;
                        access$10011.log(SemWifiApSmartGattClient.this.TAG + ":\tGot MHS status:" + Arrays.toString(mStatus));
                        String access$00011 = SemWifiApSmartGattClient.this.TAG;
                        Log.d(access$00011, "Got MHS status:" + Arrays.toString(mStatus));
                    } else if (SemWifiApSmartGattClient.this.mhs_read_status_retry > 0) {
                        LocalLog access$10012 = SemWifiApSmartGattClient.this.mLocalLog;
                        access$10012.log(SemWifiApSmartGattClient.this.TAG + ":\tmhs_read_status_retry");
                        Log.d(SemWifiApSmartGattClient.this.TAG, "mhs_read_status_retry");
                        SemWifiApSmartGattClient.access$3510(SemWifiApSmartGattClient.this);
                        BluetoothGattService access$12005 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                        SemWifiApSmartUtil unused23 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                        bluetoothGatt.readCharacteristic(access$12005.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_STATUS_UUID));
                        return;
                    }
                    if (mStatus == null || mStatus[0] != 1) {
                        SemWifiApSmartGattClient semWifiApSmartGattClient4 = SemWifiApSmartGattClient.this;
                        semWifiApSmartGattClient4.setConnectionState(SemWifiApSmartGattClient.ST_MHS_ENABLING_FAILURE, semWifiApSmartGattClient4.mSmartAp_WiFi_MAC);
                    } else {
                        if (mStatus.length > 2) {
                            byte channelStrLength = mStatus[1];
                            String channelStr = new String(mStatus, 2, channelStrLength);
                            if (channelStr.isEmpty()) {
                                Log.d(SemWifiApSmartGattClient.this.TAG, "Got MHS channel: 0");
                            } else {
                                try {
                                    int unused24 = SemWifiApSmartGattClient.this.mMhsFreq = SemWifiApSmartGattClient.this.channelToFreq(Integer.parseInt(channelStr));
                                    String access$00012 = SemWifiApSmartGattClient.this.TAG;
                                    Log.d(access$00012, "Got MHS channel:" + SemWifiApSmartGattClient.this.mMhsFreq);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                            byte clientMACLength = mStatus[channelStrLength + 2];
                            byte mhsMACLength = mStatus[channelStrLength + 3 + clientMACLength];
                            String clientmac = new String(mStatus, channelStrLength + 2 + 1, clientMACLength);
                            String mhsmac = new String(mStatus, channelStrLength + 4 + clientMACLength, mhsMACLength);
                            String access$00013 = SemWifiApSmartGattClient.this.TAG;
                            Log.d(access$00013, "Got MHS channel:" + SemWifiApSmartGattClient.this.mMhsFreq + ",clientmac:" + clientmac + ",mhsmac:" + mhsmac);
                            LocalLog access$10013 = SemWifiApSmartGattClient.this.mLocalLog;
                            access$10013.log(SemWifiApSmartGattClient.this.TAG + "\tGot MHS channel:" + SemWifiApSmartGattClient.this.mMhsFreq + ",clientmac:" + clientmac + ",mhsmac:" + mhsmac);
                            synchronized (SemWifiApSmartGattClient.this.mSmartMHSList) {
                                if (!clientmac.equals("") && !mhsmac.equals("")) {
                                    Iterator<SmartMHSInfo> it = SemWifiApSmartGattClient.this.mSmartMHSList.iterator();
                                    while (true) {
                                        if (!it.hasNext()) {
                                            break;
                                        }
                                        SmartMHSInfo inf = it.next();
                                        if (clientmac.substring(9).toLowerCase().equals(inf.clientMAC)) {
                                            inf.MHS_MAC = mhsmac.toLowerCase();
                                            inf.state = 2;
                                            Log.e(SemWifiApSmartGattClient.this.TAG, "updated MHS MAC in Smart MHS list");
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (SemWifiApSmartGattClient.this.mBleWorkHandler != null) {
                            SemWifiApSmartGattClient.this.mBleWorkHandler.sendEmptyMessage(11);
                        }
                    }
                    if (SemWifiApSmartGattClient.this.mBleWorkHandler != null) {
                        SemWifiApSmartGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                    }
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public LocalLog mLocalLog;
    /* access modifiers changed from: private */
    public int mMhsFreq = -1;
    private NotificationManager mNotificationManager;
    /* access modifiers changed from: private */
    public String mPassword;
    /* access modifiers changed from: private */
    public String mPendingDeviceAddress;
    /* access modifiers changed from: private */
    public String mSSID;
    private WifiManager.SemWifiApSmartCallback mSemWifiApSmartCallbackImpl;
    private SemWifiApSmartClient mSemWifiApSmartClient;
    private SemWifiApSmartGattClientReceiver mSemWifiApSmartGattClientReceiver;
    /* access modifiers changed from: private */
    public SemWifiApSmartUtil mSemWifiApSmartUtil;
    private String mSmartAp_BLE_MAC;
    /* access modifiers changed from: private */
    public String mSmartAp_WiFi_MAC;
    List<SmartMHSInfo> mSmartMHSList = new ArrayList();
    /* access modifiers changed from: private */
    public boolean mTimeMismatchOccured = false;
    /* access modifiers changed from: private */
    public String mUserName;
    /* access modifiers changed from: private */
    public int mUserType;
    /* access modifiers changed from: private */
    public int mWPA3Mode = 0;
    private PowerManager.WakeLock mWakeLock;
    /* access modifiers changed from: private */
    public int mhideSSID;
    /* access modifiers changed from: private */
    public int mhs_read_status_retry = 2;
    /* access modifiers changed from: private */
    public int mversion;
    /* access modifiers changed from: private */
    public boolean mwaitingToConnect = false;
    /* access modifiers changed from: private */
    public boolean requestedToEnableMHS;
    private WifiManager.SemWifiApSmartCallback tSemWifiApSmartCallback;
    /* access modifiers changed from: private */
    public int tryingToRetry = -1;

    static /* synthetic */ int access$2110(SemWifiApSmartGattClient x0) {
        int i = x0.tryingToRetry;
        x0.tryingToRetry = i - 1;
        return i;
    }

    static /* synthetic */ int access$3510(SemWifiApSmartGattClient x0) {
        int i = x0.mhs_read_status_retry;
        x0.mhs_read_status_retry = i - 1;
        return i;
    }

    static {
        mSemWifiApSmartGattClientIntentFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        mSemWifiApSmartGattClientIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        mSemWifiApSmartGattClientIntentFilter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
        mSemWifiApSmartGattClientIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        mSemWifiApSmartGattClientIntentFilter.addAction(ACTION_LOGOUT_ACCOUNTS_COMPLETE);
        mSemWifiApSmartGattClientIntentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        mSemWifiApSmartGattClientIntentFilter.setPriority(ReportIdKey.ID_PATTERN_MATCHED);
    }

    public SemWifiApSmartGattClient(Context context, SemWifiApSmartUtil semWifiApSmartUtil, LocalLog tLocalLog) {
        this.mContext = context;
        this.mSemWifiApSmartUtil = semWifiApSmartUtil;
        this.mLocalLog = tLocalLog;
        this.mSemWifiApSmartClient = WifiInjector.getInstance().getSemWifiApSmartClient();
        this.mSemWifiApSmartGattClientReceiver = new SemWifiApSmartGattClientReceiver();
        if (!FactoryTest.isFactoryBinary()) {
            this.mContext.registerReceiver(this.mSemWifiApSmartGattClientReceiver, mSemWifiApSmartGattClientIntentFilter);
        } else {
            Log.e(this.TAG, "This devices's binary is a factory binary");
        }
    }

    public void registerSemWifiApSmartCallback(WifiManager.SemWifiApSmartCallback callback) {
        this.tSemWifiApSmartCallback = callback;
    }

    public void handleBootCompleted() {
        Log.d(this.TAG, "handleBootCompleted");
        this.mBleWorkThread = new HandlerThread("SemWifiApSmartGattClientBleHandler");
        this.mBleWorkThread.start();
        this.mBleWorkHandler = new BleWorkHandler(this.mBleWorkThread.getLooper());
        String tlist = Settings.Secure.getString(this.mContext.getContentResolver(), "bonded_device_clientside");
        if (tlist != null) {
            for (String str : tlist.split("\n")) {
                this.bonedDevicesFromHotspotLive.add(str);
            }
            this.mLocalLog.log(this.TAG + ":\tbonded_device_clientside,booting time :" + tlist);
        }
    }

    class SemWifiApSmartGattClientReceiver extends BroadcastReceiver {
        SemWifiApSmartGattClientReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            boolean z;
            Intent intent2 = intent;
            String action = intent.getAction();
            boolean z2 = false;
            if (action.equals(SemWifiApSmartGattClient.ACTION_LOGOUT_ACCOUNTS_COMPLETE)) {
                SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tLOGOUT_COMPLETE");
                Log.d(SemWifiApSmartGattClient.this.TAG, "shutdownClient due to LOGOUT /");
                boolean unused = SemWifiApSmartGattClient.this.mwaitingToConnect = false;
                int unused2 = SemWifiApSmartGattClient.this.mMhsFreq = -1;
                if (SemWifiApSmartGattClient.this.mBleWorkHandler != null) {
                    SemWifiApSmartGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                }
                Iterator<String> tempIt = SemWifiApSmartGattClient.this.bonedDevicesFromHotspotLive.iterator();
                while (tempIt != null && tempIt.hasNext()) {
                    BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(tempIt.next());
                    if (device != null && device.getBondState() == 12) {
                        device.removeBond();
                        Log.d(SemWifiApSmartGattClient.this.TAG, ":smarttethering.removeBond :" + device.getAddress());
                        SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tsmarttethering.removeBond :" + device.getAddress());
                    }
                }
                SemWifiApSmartGattClient.this.bonedDevicesFromHotspotLive.clear();
                Settings.Secure.putString(SemWifiApSmartGattClient.this.mContext.getContentResolver(), "bonded_device_clientside", (String) null);
                return;
            }
            int i = 8;
            boolean wpa3_isSecure = true;
            if (action.equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
                BluetoothDevice device2 = (BluetoothDevice) intent2.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                int mType = intent2.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT", Integer.MIN_VALUE);
                String format = String.format(Locale.US, "%06d", new Object[]{Integer.valueOf(intent2.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE))});
                Log.d(SemWifiApSmartGattClient.this.TAG, "mType: " + mType + " ,device: " + device2 + " ,mPendingDeviceAddress: " + SemWifiApSmartGattClient.this.mPendingDeviceAddress);
                SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tmType: " + mType + " ,device: " + device2 + " ,mPendingDeviceAddress: " + SemWifiApSmartGattClient.this.mPendingDeviceAddress);
                if (SemWifiApSmartGattClient.this.mPendingDeviceAddress != null && device2.getAddress().equals(SemWifiApSmartGattClient.this.mPendingDeviceAddress) && mType == 2) {
                    Intent tintent = new Intent();
                    tintent.setAction("com.samsung.android.server.wifi.softap.smarttethering.collapseQuickPanel");
                    SemWifiApSmartGattClient.this.mContext.sendBroadcast(tintent);
                    abortBroadcast();
                    intent2.setClassName("com.android.settings", "com.samsung.android.settings.wifi.mobileap.WifiApWarning");
                    intent2.setFlags(268435456);
                    intent2.setAction(SemWifiApBroadcastReceiver.WIFIAP_WARNING_DIALOG);
                    intent2.putExtra(SemWifiApBroadcastReceiver.WIFIAP_WARNING_DIALOG_TYPE, 8);
                    SemWifiApSmartGattClient.this.mContext.startActivity(intent2);
                    Log.d(SemWifiApSmartGattClient.this.TAG, "passkeyconfirm dialog");
                }
            } else if (intent.getAction().equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) {
                BluetoothDevice device3 = (BluetoothDevice) intent2.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                switch (intent2.getIntExtra("android.bluetooth.device.extra.BOND_STATE", 10)) {
                    case 10:
                        boolean unused3 = SemWifiApSmartGattClient.this.isBondingGoingon = false;
                        if (SemWifiApSmartGattClient.this.mPendingDeviceAddress != null && device3.getAddress().equals(SemWifiApSmartGattClient.this.mPendingDeviceAddress)) {
                            String unused4 = SemWifiApSmartGattClient.this.mPendingDeviceAddress = null;
                            Log.d(SemWifiApSmartGattClient.this.TAG, " client Bonding is failed");
                            SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tclient Bonding is failed");
                            new Handler().postDelayed(new Runnable() {
                                public void run() {
                                    SemWifiApSmartGattClient.this.setConnectionState(-2, SemWifiApSmartGattClient.this.mSmartAp_WiFi_MAC);
                                    if (SemWifiApSmartGattClient.this.mBleWorkHandler != null) {
                                        SemWifiApSmartGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                                    }
                                }
                            }, 6000);
                            return;
                        }
                        return;
                    case 11:
                        if (SemWifiApSmartGattClient.this.mPendingDeviceAddress != null && device3.getAddress().equals(SemWifiApSmartGattClient.this.mPendingDeviceAddress)) {
                            boolean unused5 = SemWifiApSmartGattClient.this.isBondingGoingon = true;
                            Log.d(SemWifiApSmartGattClient.this.TAG, " client Bonding is going on");
                            return;
                        }
                        return;
                    case 12:
                        Log.d(SemWifiApSmartGattClient.this.TAG, "client Bonding is done");
                        boolean unused6 = SemWifiApSmartGattClient.this.isBondingGoingon = false;
                        if (device3 != null && SemWifiApSmartGattClient.this.mPendingDeviceAddress != null && device3.getAddress().equals(SemWifiApSmartGattClient.this.mPendingDeviceAddress)) {
                            String unused7 = SemWifiApSmartGattClient.this.mPendingDeviceAddress = null;
                            SemWifiApSmartGattClient.this.bonedDevicesFromHotspotLive.add(device3.getAddress());
                            String tpString = "";
                            Iterator it = SemWifiApSmartGattClient.this.bonedDevicesFromHotspotLive.iterator();
                            while (it.hasNext()) {
                                tpString = tpString + ((String) it.next()) + "\n";
                            }
                            Settings.Secure.putString(SemWifiApSmartGattClient.this.mContext.getContentResolver(), "bonded_device_clientside", tpString);
                            SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\t\tAdding to bondedd devices :" + device3.getAddress());
                            Log.d(SemWifiApSmartGattClient.this.TAG, ":Adding to bondedd devices :" + tpString);
                            if (SemWifiApSmartGattClient.this.mTimeMismatchOccured) {
                                Log.d(SemWifiApSmartGattClient.this.TAG, "mTimeMismatchOccured is true after bonding");
                                SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\t mTimeMismatchOccured is true after bonding :");
                                boolean unused8 = SemWifiApSmartGattClient.this.mTimeMismatchOccured = false;
                                if (SemWifiApSmartGattClient.this.mConnectedGatt != null) {
                                    BluetoothGatt access$1000 = SemWifiApSmartGattClient.this.mConnectedGatt;
                                    BluetoothGattService access$1200 = SemWifiApSmartGattClient.this.mBluetoothGattService;
                                    SemWifiApSmartUtil unused9 = SemWifiApSmartGattClient.this.mSemWifiApSmartUtil;
                                    access$1000.readCharacteristic(access$1200.getCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_BOND_STATUS));
                                    return;
                                }
                                Log.d(SemWifiApSmartGattClient.this.TAG, "mTimeMismatchOccured is true after bonding, but gattconnection is null");
                                SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\t mTimeMismatchOccured is true after bonding, but gattconnection is null");
                                SemWifiApSmartGattClient.this.shutdownclient();
                                return;
                            } else if (!SemWifiApSmartGattClient.this.tryToConnectToRemoteBLE(device3, false)) {
                                SemWifiApSmartGattClient semWifiApSmartGattClient = SemWifiApSmartGattClient.this;
                                semWifiApSmartGattClient.setConnectionState(SemWifiApSmartGattClient.ST_GATT_FAILURE, semWifiApSmartGattClient.mSmartAp_WiFi_MAC);
                                SemWifiApSmartGattClient.this.mFailedBLEConnections.remove(device3.getAddress());
                                return;
                            } else {
                                return;
                            }
                        } else {
                            return;
                        }
                    default:
                        return;
                }
            } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                int mWifiState = intent2.getIntExtra("wifi_state", 4);
                if (mWifiState == 1 || mWifiState == 4) {
                    Log.d(SemWifiApSmartGattClient.this.TAG, "shutdownClient due to Wi-FI is OFF/");
                    SemWifiApContentProviderHelper.insert(SemWifiApSmartGattClient.this.mContext, "smart_tethering_GattClient_username", (String) null);
                    boolean unused10 = SemWifiApSmartGattClient.this.mwaitingToConnect = false;
                    int unused11 = SemWifiApSmartGattClient.this.mMhsFreq = -1;
                    if (SemWifiApSmartGattClient.this.mBleWorkHandler != null) {
                        SemWifiApSmartGattClient.this.mBleWorkHandler.sendEmptyMessage(12);
                    }
                    synchronized (SemWifiApSmartGattClient.this.mSmartMHSList) {
                        SemWifiApSmartGattClient.this.mSmartMHSList.clear();
                    }
                }
            } else if (!action.equals("android.net.wifi.SCAN_RESULTS")) {
                boolean z3 = true;
                if (!action.equals("android.bluetooth.adapter.action.STATE_CHANGED") || !SemWifiApSmartGattClient.this.mBluetoothIsOn) {
                    if (action.equals("android.net.wifi.STATE_CHANGE")) {
                        WifiManager wifiManager = (WifiManager) SemWifiApSmartGattClient.this.mContext.getSystemService("wifi");
                        int i2 = Settings.Secure.getInt(SemWifiApSmartGattClient.this.mContext.getContentResolver(), "wifi_client_smart_tethering_settings", 0);
                        NetworkInfo networkInfo = (NetworkInfo) intent2.getParcelableExtra("networkInfo");
                        boolean isConnected = networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED;
                        if (networkInfo == null || networkInfo.getDetailedState() != NetworkInfo.DetailedState.DISCONNECTED) {
                            z3 = false;
                        }
                        boolean isDisconnected = z3;
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        String mBSSID = wifiInfo == null ? null : wifiInfo.getBSSID();
                        if (isConnected) {
                            if (wifiInfo.getSSID().equals("\"" + SemWifiApSmartGattClient.this.mSSID + "\"")) {
                                synchronized (SemWifiApSmartGattClient.this.mSmartMHSList) {
                                    Iterator<SmartMHSInfo> it2 = SemWifiApSmartGattClient.this.mSmartMHSList.iterator();
                                    while (true) {
                                        if (!it2.hasNext()) {
                                            break;
                                        }
                                        SmartMHSInfo inf = it2.next();
                                        if (mBSSID != null && inf.state == 2 && inf.MHS_MAC != null && inf.MHS_MAC.equalsIgnoreCase(mBSSID)) {
                                            inf.state = 3;
                                            SemWifiApSmartGattClient.this.invokeCallback(inf.clientMAC, 3);
                                            Log.e(SemWifiApSmartGattClient.this.TAG, "updated status to WIFI connected in the SmartMHSlist");
                                            SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tupdated status to WIFI connected in the SmartMHSlist");
                                            SemWifiApSmartGattClient.this.showSmartMHSInfo();
                                            break;
                                        }
                                    }
                                }
                                Log.d(SemWifiApSmartGattClient.this.TAG, "NETWORK_STATE_CHANGED_ACTION isConnected:" + isConnected + ", mSSID:" + SemWifiApSmartGattClient.this.mSSID + ", startPartialScan 1,6,11,149 one more time to update wifi list quickly");
                                SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tNETWORK_STATE_CHANGED_ACTION isConnected:" + isConnected + ", mSSID:" + SemWifiApSmartGattClient.this.mSSID + ", startPartialScan 1,6,11,149 one more time to update wifi list quickly");
                                int[] freqs = {2412, 2437, 2462, 5745};
                                if (!wifiManager.semStartPartialChannelScan(freqs)) {
                                    wifiManager.semStartPartialChannelScan(freqs);
                                }
                            }
                        } else if (isDisconnected) {
                            Log.d(SemWifiApSmartGattClient.this.TAG, "isDisconnected: true");
                            synchronized (SemWifiApSmartGattClient.this.mSmartMHSList) {
                                Iterator<SmartMHSInfo> it3 = SemWifiApSmartGattClient.this.mSmartMHSList.iterator();
                                while (true) {
                                    if (!it3.hasNext()) {
                                        break;
                                    }
                                    SmartMHSInfo inf2 = it3.next();
                                    if (inf2.state == 3) {
                                        inf2.state = 0;
                                        SemWifiApSmartGattClient.this.invokeCallback(inf2.clientMAC, 0);
                                        Log.e(SemWifiApSmartGattClient.this.TAG, "updated status to WIFI disconnected in the SmartMHSlist:" + inf2.MHS_MAC.substring(9));
                                        SemWifiApContentProviderHelper.insert(SemWifiApSmartGattClient.this.mContext, "smart_tethering_GattClient_username", (String) null);
                                        SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tupdated status to WIFI disconnected in the SmartMHSlist:" + inf2.MHS_MAC.substring(9));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else if (SemWifiApSmartGattClient.this.mBluetoothDevice != null && BluetoothAdapter.getDefaultAdapter().getState() == 12) {
                    Log.d(SemWifiApSmartGattClient.this.TAG, "ACTION_STATE_CHANGED mBluetoothIsOn " + SemWifiApSmartGattClient.this.mBluetoothIsOn);
                    SemWifiApSmartGattClient semWifiApSmartGattClient2 = SemWifiApSmartGattClient.this;
                    String unused12 = semWifiApSmartGattClient2.mPendingDeviceAddress = semWifiApSmartGattClient2.mBluetoothDevice.getAddress();
                    SemWifiApSmartGattClient.this.mBluetoothDevice.createBond(2);
                    boolean unused13 = SemWifiApSmartGattClient.this.mBluetoothIsOn = false;
                }
            } else if (SemWifiApSmartGattClient.this.mwaitingToConnect) {
                boolean scanUpdated = intent2.getBooleanExtra("resultsUpdated", true);
                WifiManager wifiManager2 = (WifiManager) SemWifiApSmartGattClient.this.mContext.getSystemService("wifi");
                List<ScanResult> results = wifiManager2.getScanResults();
                Log.d(SemWifiApSmartGattClient.this.TAG, "SCAN_RESULTS_AVAILABLE_ACTION received, mwaitingToConnect:" + SemWifiApSmartGattClient.this.mwaitingToConnect + ", scanUpdated:" + scanUpdated);
                boolean canConnectNow = true;
                if ("JP".equals(SystemProperties.get("ro.csc.country_code")) && SemWifiApSmartGattClient.this.mDelayStartFrom > 0) {
                    long tGap = System.currentTimeMillis() - SemWifiApSmartGattClient.this.mDelayStartFrom;
                    Log.d(SemWifiApSmartGattClient.this.TAG, "SCAN_RESULTS_AVAILABLE_ACTION received, tGap:" + tGap + " for Japan");
                    if (tGap > RttServiceImpl.HAL_RANGING_TIMEOUT_MS) {
                        long unused14 = SemWifiApSmartGattClient.this.mDelayStartFrom = -1;
                        Log.d(SemWifiApSmartGattClient.this.TAG, "SCAN_RESULTS_AVAILABLE_ACTION received, keep going connection process for Japan");
                    } else {
                        canConnectNow = false;
                        Log.d(SemWifiApSmartGattClient.this.TAG, "SCAN_RESULTS_AVAILABLE_ACTION received, skip this scan results and wait more for Japan");
                    }
                }
                if (results != null && results.size() > 0 && canConnectNow) {
                    Log.d(SemWifiApSmartGattClient.this.TAG, "SCAN_RESULTS_AVAILABLE_ACTION received, results.size():" + results.size());
                    WifiInfo wifiInfo2 = wifiManager2.getConnectionInfo();
                    if (wifiInfo2 != null) {
                        if (wifiInfo2.getSSID().equals("\"" + SemWifiApSmartGattClient.this.mSSID + "\"")) {
                            boolean z4 = scanUpdated;
                            List<ScanResult> list = results;
                        }
                    }
                    Iterator<ScanResult> it4 = results.iterator();
                    while (true) {
                        if (!it4.hasNext()) {
                            List<ScanResult> list2 = results;
                            break;
                        } else if (it4.next().SSID.equals(SemWifiApSmartGattClient.this.mSSID)) {
                            boolean unused15 = SemWifiApSmartGattClient.this.mwaitingToConnect = z2;
                            int unused16 = SemWifiApSmartGattClient.this.mMhsFreq = -1;
                            List<WifiConfiguration> list3 = wifiManager2.getConfiguredNetworks();
                            Iterator<WifiConfiguration> it5 = list3.iterator();
                            while (true) {
                                if (!it5.hasNext()) {
                                    List<WifiConfiguration> list4 = list3;
                                    List<ScanResult> list5 = results;
                                    break;
                                }
                                WifiConfiguration i3 = it5.next();
                                boolean wpa2_isSecure = i3.allowedKeyManagement.get(wpa3_isSecure);
                                boolean wpa3_isSecure2 = i3.allowedKeyManagement.get(i);
                                boolean scanUpdated2 = scanUpdated;
                                boolean isOpen = i3.allowedKeyManagement.get(0);
                                String access$000 = SemWifiApSmartGattClient.this.TAG;
                                List<WifiConfiguration> list6 = list3;
                                StringBuilder sb = new StringBuilder();
                                List<ScanResult> results2 = results;
                                sb.append("isOpen");
                                sb.append(isOpen);
                                sb.append(",wpa2_isSecure=");
                                sb.append(wpa2_isSecure);
                                sb.append(",wpa3_isSecure=");
                                sb.append(wpa3_isSecure2);
                                Log.d(access$000, sb.toString());
                                if ((SemWifiApSmartGattClient.this.mPassword != null || !isOpen) && (SemWifiApSmartGattClient.this.mPassword == null || !wpa2_isSecure || SemWifiApSmartGattClient.this.mWPA3Mode != 0)) {
                                    if (SemWifiApSmartGattClient.this.mPassword == null || !wpa3_isSecure2) {
                                        z = true;
                                        wpa3_isSecure = z;
                                        scanUpdated = scanUpdated2;
                                        list3 = list6;
                                        results = results2;
                                        i = 8;
                                    } else if (SemWifiApSmartGattClient.this.mWPA3Mode != 1) {
                                        z = true;
                                        wpa3_isSecure = z;
                                        scanUpdated = scanUpdated2;
                                        list3 = list6;
                                        results = results2;
                                        i = 8;
                                    }
                                }
                                if (i3.SSID != null) {
                                    if (i3.SSID.equals("\"" + SemWifiApSmartGattClient.this.mSSID + "\"")) {
                                        wifiManager2.disconnect();
                                        Log.d(SemWifiApSmartGattClient.this.TAG, "Scan resullts Connecting to MHS:" + SemWifiApSmartGattClient.this.mSSID + ",i.networkId:" + i3.networkId);
                                        SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tScan resullts Connecting to MHS:" + SemWifiApSmartGattClient.this.mSSID + ",i.networkId:" + i3.networkId);
                                        wifiManager2.enableNetwork(i3.networkId, true);
                                        Log.d(SemWifiApSmartGattClient.this.TAG, "reconnect");
                                        wifiManager2.reconnect();
                                        break;
                                    }
                                }
                                z = true;
                                wpa3_isSecure = z;
                                scanUpdated = scanUpdated2;
                                list3 = list6;
                                results = results2;
                                i = 8;
                            }
                        } else {
                            results = results;
                            z2 = false;
                            i = 8;
                        }
                    }
                } else {
                    List<ScanResult> list7 = results;
                }
                if (SemWifiApSmartGattClient.this.mwaitingToConnect && SemWifiApSmartGattClient.this.tryingToRetry != 0) {
                    Log.d(SemWifiApSmartGattClient.this.TAG, "SCAN_RESULTS_AVAILABLE_ACTION doesn't have " + SemWifiApSmartGattClient.this.mSSID + " tryingToRetry : " + SemWifiApSmartGattClient.this.tryingToRetry);
                    SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tSCAN_RESULTS_AVAILABLE_ACTION doesn't have " + SemWifiApSmartGattClient.this.mSSID + ",starting partial scan tryingToRetry : " + SemWifiApSmartGattClient.this.tryingToRetry);
                    SemWifiApSmartGattClient.this.startPartialScanAfterSleep(50);
                    SemWifiApSmartGattClient.access$2110(SemWifiApSmartGattClient.this);
                }
            }
        }
    }

    public int getSmartApConnectedStatus(String mBSSID) {
        if (mBSSID == null) {
            return 0;
        }
        synchronized (this.mSmartMHSList) {
            for (SmartMHSInfo inf : this.mSmartMHSList) {
                if (inf.MHS_MAC != null && inf.MHS_MAC.equalsIgnoreCase(mBSSID)) {
                    String str = this.TAG;
                    Log.d(str, "getSmartApConnectedStatus mhs_mac " + mBSSID + ",::" + inf.state);
                    int i = inf.state;
                    return i;
                }
            }
            return 0;
        }
    }

    public int getSmartApConnectedStatusFromScanResult(String mClientMAC) {
        if (mClientMAC == null) {
            return 0;
        }
        synchronized (this.mSmartMHSList) {
            for (SmartMHSInfo inf : this.mSmartMHSList) {
                if (inf.clientMAC != null && inf.clientMAC.equalsIgnoreCase(mClientMAC)) {
                    String str = this.TAG;
                    Log.d(str, "getSmartApConnectedStatusFromScanResult client MAC:" + mClientMAC + ":" + inf.state);
                    int i = inf.state;
                    return i;
                }
            }
            return 0;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:50:0x0178, code lost:
        r16 = r10;
        r1.mSmartMHSList.add(new com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient.SmartMHSInfo(r24, (java.lang.String) null, java.lang.System.currentTimeMillis(), 1));
        setConnectionState(1, r12);
        r0 = r1.mBleWorkHandler;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x0195, code lost:
        if (r0 == null) goto L_0x01a6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x019d, code lost:
        if (r0.hasMessages(18) != false) goto L_0x01a6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x019f, code lost:
        r1.mBleWorkHandler.sendEmptyMessageDelayed(18, com.android.server.wifi.rtt.RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x01a6, code lost:
        r1.mAESKey = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:0x01af, code lost:
        if (r1.mSemWifiApSmartUtil.isEncryptionCanbeUsed(r13, r3) == false) goto L_0x01bd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:57:0x01b1, code lost:
        r1.mAESKey = r1.mSemWifiApSmartUtil.getAESKey(java.lang.System.currentTimeMillis());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x01bd, code lost:
        if (r13 == 0) goto L_0x01c8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:60:0x01c1, code lost:
        if (r1.mAESKey != null) goto L_0x01c4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:61:0x01c4, code lost:
        r7 = r16;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:0x01ce, code lost:
        if (r16.getBondState() != 10) goto L_0x0261;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:64:0x01d0, code lost:
        r0 = r1.mBluetoothAdapter;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x01d2, code lost:
        if (r0 == null) goto L_0x021b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x01d8, code lost:
        if (r0.getState() != 10) goto L_0x021b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x01da, code lost:
        android.util.Log.d(r1.TAG, "device is not bonded, enabling BT adapter,mBluetoothIsOn:" + r1.mBluetoothIsOn);
        r1.mLocalLog.log(r1.TAG + ":\tdevice is not bonded, enabling BT adapter,mBluetoothIsOn:" + r1.mBluetoothIsOn);
        r1.mBluetoothAdapter.enable();
        r1.mBluetoothIsOn = true;
        r1.mBluetoothDevice = r16;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:0x021b, code lost:
        r7 = r16;
        android.util.Log.d(r1.TAG, "device is not bonded:" + r7.getBondState());
        r1.mLocalLog.log(r1.TAG + ":\tdevice is not bonded:" + r7.getBondState());
        r1.mPendingDeviceAddress = r7.getAddress();
        r7.createBond(2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x0261, code lost:
        r7 = r16;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x0268, code lost:
        if (tryToConnectToRemoteBLE(r7, false) != false) goto L_0x0277;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x026a, code lost:
        setConnectionState(ST_GATT_FAILURE, r12);
        r1.mFailedBLEConnections.remove(r7.getAddress());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x0277, code lost:
        return true;
     */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0035  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean connectToSmartMHS(java.lang.String r20, int r21, int r22, int r23, java.lang.String r24, java.lang.String r25, int r26) {
        /*
            r19 = this;
            r1 = r19
            r2 = r20
            r3 = r21
            r4 = r22
            r5 = r23
            r12 = r24
            r13 = r26
            android.bluetooth.BluetoothGatt r0 = r1.mConnectedGatt
            r14 = 0
            if (r0 == 0) goto L_0x001b
            java.lang.String r0 = r1.TAG
            java.lang.String r6 = "mConnectedGatt is not null"
            android.util.Log.e(r0, r6)
            return r14
        L_0x001b:
            boolean r0 = r1.isBondingGoingon
            if (r0 == 0) goto L_0x0027
            java.lang.String r0 = r1.TAG
            java.lang.String r6 = "isBondingGoingon is true"
            android.util.Log.e(r0, r6)
            return r14
        L_0x0027:
            java.util.List<com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient$SmartMHSInfo> r0 = r1.mSmartMHSList
            java.util.Iterator r0 = r0.iterator()
        L_0x002d:
            boolean r6 = r0.hasNext()
            r15 = 2
            r11 = 1
            if (r6 == 0) goto L_0x005e
            java.lang.Object r6 = r0.next()
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient$SmartMHSInfo r6 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient.SmartMHSInfo) r6
            int r7 = r6.state
            if (r7 == r11) goto L_0x0045
            int r7 = r6.state
            if (r7 != r15) goto L_0x0044
            goto L_0x0045
        L_0x0044:
            goto L_0x002d
        L_0x0045:
            java.lang.String r0 = r1.TAG
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "Gatt connecting is going on, return:"
            r7.append(r8)
            int r8 = r6.state
            r7.append(r8)
            java.lang.String r7 = r7.toString()
            android.util.Log.e(r0, r7)
            return r14
        L_0x005e:
            r1.mhs_read_status_retry = r15
            r1.mUserType = r3
            r1.mhideSSID = r4
            r9 = r25
            r1.mUserName = r9
            r1.mversion = r13
            java.lang.String r0 = r1.TAG
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "connectToSmartMHS   mversion:"
            r6.append(r7)
            r6.append(r13)
            java.lang.String r7 = ",address:"
            r6.append(r7)
            r6.append(r2)
            java.lang.String r7 = ",mUserType:"
            r6.append(r7)
            r6.append(r3)
            java.lang.String r7 = ",mHidden:"
            r6.append(r7)
            r6.append(r4)
            java.lang.String r7 = ",mSecurity:"
            r6.append(r7)
            r6.append(r5)
            java.lang.String r7 = ",mSmartAp_WiFi_MAC:"
            r6.append(r7)
            r6.append(r12)
            java.lang.String r6 = r6.toString()
            android.util.Log.d(r0, r6)
            android.util.LocalLog r0 = r1.mLocalLog
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = r1.TAG
            r6.append(r7)
            java.lang.String r7 = ":\tconnectToSmartMHS   mversion:"
            r6.append(r7)
            r6.append(r13)
            java.lang.String r7 = ",address:"
            r6.append(r7)
            r6.append(r2)
            java.lang.String r7 = ",mUserType:"
            r6.append(r7)
            r6.append(r3)
            java.lang.String r7 = ",mHidden:"
            r6.append(r7)
            r6.append(r4)
            java.lang.String r7 = ",mSecurity:"
            r6.append(r7)
            r6.append(r5)
            java.lang.String r7 = ",mSmartAp_WiFi_MAC:"
            r6.append(r7)
            r6.append(r12)
            java.lang.String r6 = r6.toString()
            r0.log(r6)
            android.bluetooth.BluetoothAdapter r0 = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            r1.mBluetoothAdapter = r0
            android.bluetooth.BluetoothAdapter r0 = r1.mBluetoothAdapter
            if (r0 == 0) goto L_0x027e
            if (r2 != 0) goto L_0x00f9
            goto L_0x027e
        L_0x00f9:
            android.bluetooth.BluetoothDevice r10 = r0.getRemoteDevice(r2)
            if (r10 != 0) goto L_0x0107
            java.lang.String r0 = r1.TAG
            java.lang.String r6 = "Device not found. Unable to connect."
            android.util.Log.e(r0, r6)
            return r14
        L_0x0107:
            r1.mSmartAp_WiFi_MAC = r12
            r1.mSmartAp_BLE_MAC = r2
            java.util.List<com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient$SmartMHSInfo> r6 = r1.mSmartMHSList
            monitor-enter(r6)
            java.util.ArrayList r0 = new java.util.ArrayList     // Catch:{ all -> 0x0278 }
            r0.<init>()     // Catch:{ all -> 0x0278 }
            r7 = r0
            r0 = 0
            java.util.List<com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient$SmartMHSInfo> r8 = r1.mSmartMHSList     // Catch:{ all -> 0x0278 }
            java.util.Iterator r8 = r8.iterator()     // Catch:{ all -> 0x0278 }
            r16 = r0
        L_0x011d:
            boolean r0 = r8.hasNext()     // Catch:{ all -> 0x0278 }
            if (r0 == 0) goto L_0x0156
            java.lang.Object r0 = r8.next()     // Catch:{ all -> 0x0172 }
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient$SmartMHSInfo r0 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient.SmartMHSInfo) r0     // Catch:{ all -> 0x0172 }
            java.lang.String r11 = r0.clientMAC     // Catch:{ all -> 0x0172 }
            boolean r11 = r11.equals(r12)     // Catch:{ all -> 0x0172 }
            r14 = 3
            if (r11 == 0) goto L_0x013e
            int r11 = r0.state     // Catch:{ all -> 0x0172 }
            if (r11 == r14) goto L_0x013e
            java.lang.Integer r11 = java.lang.Integer.valueOf(r16)     // Catch:{ all -> 0x0172 }
            r7.add(r11)     // Catch:{ all -> 0x0172 }
            goto L_0x0150
        L_0x013e:
            int r11 = r0.state     // Catch:{ all -> 0x0172 }
            if (r11 == r14) goto L_0x0150
            java.lang.Integer r11 = java.lang.Integer.valueOf(r16)     // Catch:{ all -> 0x0172 }
            r7.add(r11)     // Catch:{ all -> 0x0172 }
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartClient r11 = r1.mSemWifiApSmartClient     // Catch:{ all -> 0x0172 }
            java.lang.String r14 = r0.clientMAC     // Catch:{ all -> 0x0172 }
            r11.removeFromScanResults(r15, r14)     // Catch:{ all -> 0x0172 }
        L_0x0150:
            int r16 = r16 + 1
            r11 = 1
            r14 = 0
            goto L_0x011d
        L_0x0156:
            java.util.Iterator r0 = r7.iterator()     // Catch:{ IndexOutOfBoundsException -> 0x0176 }
        L_0x015a:
            boolean r8 = r0.hasNext()     // Catch:{ IndexOutOfBoundsException -> 0x0176 }
            if (r8 == 0) goto L_0x0171
            java.lang.Object r8 = r0.next()     // Catch:{ IndexOutOfBoundsException -> 0x0176 }
            java.lang.Integer r8 = (java.lang.Integer) r8     // Catch:{ IndexOutOfBoundsException -> 0x0176 }
            int r8 = r8.intValue()     // Catch:{ IndexOutOfBoundsException -> 0x0176 }
            java.util.List<com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient$SmartMHSInfo> r11 = r1.mSmartMHSList     // Catch:{ IndexOutOfBoundsException -> 0x0176 }
            r11.remove(r8)     // Catch:{ IndexOutOfBoundsException -> 0x0176 }
            goto L_0x015a
        L_0x0171:
            goto L_0x0177
        L_0x0172:
            r0 = move-exception
            r7 = r10
            goto L_0x027a
        L_0x0176:
            r0 = move-exception
        L_0x0177:
            monitor-exit(r6)     // Catch:{ all -> 0x0278 }
            java.util.List<com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient$SmartMHSInfo> r0 = r1.mSmartMHSList
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient$SmartMHSInfo r14 = new com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient$SmartMHSInfo
            r8 = 0
            long r17 = java.lang.System.currentTimeMillis()
            r11 = 1
            r6 = r14
            r7 = r24
            r16 = r10
            r9 = r17
            r15 = 1
            r6.<init>(r7, r8, r9, r11)
            r0.add(r14)
            r1.setConnectionState(r15, r12)
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient$BleWorkHandler r0 = r1.mBleWorkHandler
            if (r0 == 0) goto L_0x01a6
            r6 = 18
            boolean r0 = r0.hasMessages(r6)
            if (r0 != 0) goto L_0x01a6
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient$BleWorkHandler r0 = r1.mBleWorkHandler
            r7 = 5000(0x1388, double:2.4703E-320)
            r0.sendEmptyMessageDelayed(r6, r7)
        L_0x01a6:
            r0 = 0
            r1.mAESKey = r0
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r0 = r1.mSemWifiApSmartUtil
            boolean r0 = r0.isEncryptionCanbeUsed(r13, r3)
            if (r0 == 0) goto L_0x01bd
            com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r0 = r1.mSemWifiApSmartUtil
            long r6 = java.lang.System.currentTimeMillis()
            java.lang.String r0 = r0.getAESKey(r6)
            r1.mAESKey = r0
        L_0x01bd:
            if (r13 == 0) goto L_0x01c8
            java.lang.String r0 = r1.mAESKey
            if (r0 != 0) goto L_0x01c4
            goto L_0x01c8
        L_0x01c4:
            r7 = r16
            goto L_0x0263
        L_0x01c8:
            int r0 = r16.getBondState()
            r6 = 10
            if (r0 != r6) goto L_0x0261
            android.bluetooth.BluetoothAdapter r0 = r1.mBluetoothAdapter
            if (r0 == 0) goto L_0x021b
            int r0 = r0.getState()
            if (r0 != r6) goto L_0x021b
            java.lang.String r0 = r1.TAG
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "device is not bonded, enabling BT adapter,mBluetoothIsOn:"
            r6.append(r7)
            boolean r7 = r1.mBluetoothIsOn
            r6.append(r7)
            java.lang.String r6 = r6.toString()
            android.util.Log.d(r0, r6)
            android.util.LocalLog r0 = r1.mLocalLog
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = r1.TAG
            r6.append(r7)
            java.lang.String r7 = ":\tdevice is not bonded, enabling BT adapter,mBluetoothIsOn:"
            r6.append(r7)
            boolean r7 = r1.mBluetoothIsOn
            r6.append(r7)
            java.lang.String r6 = r6.toString()
            r0.log(r6)
            android.bluetooth.BluetoothAdapter r0 = r1.mBluetoothAdapter
            r0.enable()
            r1.mBluetoothIsOn = r15
            r7 = r16
            r1.mBluetoothDevice = r7
            goto L_0x0277
        L_0x021b:
            r7 = r16
            java.lang.String r0 = r1.TAG
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r8 = "device is not bonded:"
            r6.append(r8)
            int r8 = r7.getBondState()
            r6.append(r8)
            java.lang.String r6 = r6.toString()
            android.util.Log.d(r0, r6)
            android.util.LocalLog r0 = r1.mLocalLog
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r8 = r1.TAG
            r6.append(r8)
            java.lang.String r8 = ":\tdevice is not bonded:"
            r6.append(r8)
            int r8 = r7.getBondState()
            r6.append(r8)
            java.lang.String r6 = r6.toString()
            r0.log(r6)
            java.lang.String r0 = r7.getAddress()
            r1.mPendingDeviceAddress = r0
            r6 = 2
            r7.createBond(r6)
            goto L_0x0277
        L_0x0261:
            r7 = r16
        L_0x0263:
            r6 = 0
            boolean r0 = r1.tryToConnectToRemoteBLE(r7, r6)
            if (r0 != 0) goto L_0x0277
            r0 = -7
            r1.setConnectionState(r0, r12)
            java.util.HashMap<java.lang.String, java.lang.Integer> r0 = r1.mFailedBLEConnections
            java.lang.String r6 = r7.getAddress()
            r0.remove(r6)
        L_0x0277:
            return r15
        L_0x0278:
            r0 = move-exception
            r7 = r10
        L_0x027a:
            monitor-exit(r6)     // Catch:{ all -> 0x027c }
            throw r0
        L_0x027c:
            r0 = move-exception
            goto L_0x027a
        L_0x027e:
            java.lang.String r0 = r1.TAG
            java.lang.String r6 = "BluetoothAdapter not initialized or unspecified address."
            android.util.Log.e(r0, r6)
            r6 = 0
            return r6
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient.connectToSmartMHS(java.lang.String, int, int, int, java.lang.String, java.lang.String, int):boolean");
    }

    class BleWorkHandler extends Handler {
        public BleWorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Message message = msg;
            Log.e(SemWifiApSmartGattClient.this.TAG, "Got message:" + message.what);
            int i = message.what;
            if (i == 11) {
                Log.e(SemWifiApSmartGattClient.this.TAG, "Got message: GENERATE_CONNECT_WIFI");
                SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tGot message: GENERATE_CONNECT_WIFI");
                WifiManager wifiManager = (WifiManager) SemWifiApSmartGattClient.this.mContext.getSystemService("wifi");
                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                if (SemWifiApSmartGattClient.this.mWPA3Mode == 1) {
                    Iterator<WifiConfiguration> it = list.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        WifiConfiguration i2 = it.next();
                        boolean wpa3_isSecure = i2.allowedKeyManagement.get(8);
                        if (i2.SSID != null && wpa3_isSecure && SemWifiApSmartGattClient.this.removeDoubleQuotes(i2.SSID).equals(SemWifiApSmartGattClient.this.mSSID)) {
                            SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tremoving old WPA3");
                            Log.e(SemWifiApSmartGattClient.this.TAG, "removing old WPA3");
                            wifiManager.removeNetwork(i2.networkId);
                            break;
                        }
                    }
                } else if (SemWifiApSmartGattClient.this.mWPA3Mode == 2) {
                    Iterator<WifiConfiguration> it2 = list.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        }
                        WifiConfiguration i3 = it2.next();
                        if (i3.SSID != null && SemWifiApSmartGattClient.this.removeDoubleQuotes(i3.SSID).equals(SemWifiApSmartGattClient.this.mSSID)) {
                            boolean wpa3_isSecure2 = i3.allowedKeyManagement.get(8);
                            boolean wpa2_isSecure = i3.allowedKeyManagement.get(1);
                            if (wpa3_isSecure2) {
                                SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tremoving old WPA3, in WPA2/3");
                                Log.e(SemWifiApSmartGattClient.this.TAG, "removing old WPA3, in WPA2/3");
                                int unused = SemWifiApSmartGattClient.this.mWPA3Mode = 1;
                                wifiManager.removeNetwork(i3.networkId);
                                break;
                            } else if (wpa2_isSecure) {
                                SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\t WPA2, in WPA2/3");
                                Log.e(SemWifiApSmartGattClient.this.TAG, "WPA2, in WPA2/3");
                                int unused2 = SemWifiApSmartGattClient.this.mWPA3Mode = 0;
                                break;
                            }
                        }
                    }
                }
                if (SemWifiApSmartGattClient.this.mWPA3Mode == 2) {
                    SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tnot found any profile with same SSID, WPA2/3");
                    Log.e(SemWifiApSmartGattClient.this.TAG, "not found any profile with same SSID, WPA2/3");
                    int unused3 = SemWifiApSmartGattClient.this.mWPA3Mode = 1;
                }
                WifiConfiguration conf = new WifiConfiguration();
                conf.SSID = "\"" + SemWifiApSmartGattClient.this.mSSID + "\"";
                if (SemWifiApSmartGattClient.this.mPassword != null) {
                    conf.preSharedKey = "\"" + SemWifiApSmartGattClient.this.mPassword + "\"";
                    if (SemWifiApSmartGattClient.this.mWPA3Mode == 1) {
                        Log.d(SemWifiApSmartGattClient.this.TAG, "connect to WPA3 access Point");
                        SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tconnect to WPA3 access Point");
                        conf.allowedKeyManagement.set(8);
                        conf.requirePMF = true;
                    } else {
                        conf.allowedKeyManagement.set(1);
                    }
                } else {
                    conf.allowedKeyManagement.set(0);
                }
                if (SemWifiApSmartGattClient.this.mhideSSID == 1) {
                    conf.hiddenSSID = true;
                }
                conf.semSamsungSpecificFlags.set(1);
                if (SemWifiApSmartGattClient.this.mSemWifiApSmartUtil.getNetworkType() == 1) {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    Log.d(SemWifiApSmartGattClient.this.TAG, "checking for Same SSID: " + wifiInfo.getSSID() + ",mSSID:\"" + SemWifiApSmartGattClient.this.mSSID + "\"");
                    if (wifiInfo.getSSID().equals("\"" + SemWifiApSmartGattClient.this.mSSID + "\"")) {
                        return;
                    }
                }
                if (SemWifiApSmartGattClient.this.mUserName == null) {
                    SemWifiApSmartGattClient semWifiApSmartGattClient = SemWifiApSmartGattClient.this;
                    semWifiApSmartGattClient.setConnectionState(-9, semWifiApSmartGattClient.mSmartAp_WiFi_MAC);
                    Log.e(SemWifiApSmartGattClient.this.TAG, "connecting to mUserName==null ST_MHS_USERNAME_FAILED");
                    return;
                }
                SemWifiApContentProviderHelper.insert(SemWifiApSmartGattClient.this.mContext, "smart_tethering_GattClient_username", SemWifiApSmartGattClient.this.mUserName);
                SemWifiApSmartGattClient semWifiApSmartGattClient2 = SemWifiApSmartGattClient.this;
                semWifiApSmartGattClient2.setConnectionState(2, semWifiApSmartGattClient2.mSmartAp_WiFi_MAC);
                if (SemWifiApSmartGattClient.this.mhideSSID == 1) {
                    Log.e(SemWifiApSmartGattClient.this.TAG, "connecting to hiddenSSID");
                    boolean unused4 = SemWifiApSmartGattClient.this.mwaitingToConnect = false;
                    int unused5 = SemWifiApSmartGattClient.this.mMhsFreq = -1;
                    wifiManager.connect(conf, new WifiManager.ActionListener() {
                        public void onSuccess() {
                            Log.i(SemWifiApSmartGattClient.this.TAG, "onSuccess");
                        }

                        public void onFailure(int reason) {
                            String access$000 = SemWifiApSmartGattClient.this.TAG;
                            Log.i(access$000, "onFailure : " + reason);
                        }
                    });
                    return;
                }
                boolean unused6 = SemWifiApSmartGattClient.this.mwaitingToConnect = true;
                int unused7 = SemWifiApSmartGattClient.this.tryingToRetry = 10;
                int netId = wifiManager.addNetwork(conf);
                Log.d(SemWifiApSmartGattClient.this.TAG, "trying to Connect to: " + SemWifiApSmartGattClient.this.mSSID + ",netId:" + netId);
                long unused8 = SemWifiApSmartGattClient.this.mDelayStartFrom = -1;
                if ("JP".equals(SystemProperties.get("ro.csc.country_code"))) {
                    long unused9 = SemWifiApSmartGattClient.this.mDelayStartFrom = System.currentTimeMillis();
                    Log.d(SemWifiApSmartGattClient.this.TAG, "disableNetwork netId:" + netId + ", start delay " + 5000 + " for Japan from " + SemWifiApSmartGattClient.this.mDelayStartFrom);
                    wifiManager.disableNetwork(netId);
                }
                SemWifiApSmartGattClient.this.startPartialScanAfterSleep(0);
            } else if (i == 12) {
                SemWifiApSmartGattClient.this.shutdownclient();
            } else if (i == 14) {
                WifiManager twifiManager = (WifiManager) SemWifiApSmartGattClient.this.mContext.getSystemService("wifi");
                WifiInfo wifiInfo2 = twifiManager.getConnectionInfo();
                Log.d(SemWifiApSmartGattClient.this.TAG, " wifiInfo.getNetworkId(): " + wifiInfo2.getNetworkId() + ",wifiInfo.getBSSID():" + wifiInfo2.getBSSID() + ",status:" + SemWifiApSmartGattClient.this.getSmartApConnectedStatus(wifiInfo2.getBSSID()));
                if (!(wifiInfo2.getNetworkId() == -1 || wifiInfo2.getBSSID() == null || SemWifiApSmartGattClient.this.getSmartApConnectedStatus(wifiInfo2.getBSSID()) != 3)) {
                    Log.e(SemWifiApSmartGattClient.this.TAG, "Disconnecting Wifi as device is smartly connected and device is loggedout");
                    SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tDisconnecting Wifi as device is smartly connected and device is loggedout");
                    twifiManager.removeNetwork(wifiInfo2.getNetworkId());
                }
                SemWifiApSmartGattClient.this.mSmartMHSList.clear();
            } else if (i == 18) {
                int rcount = 0;
                synchronized (SemWifiApSmartGattClient.this.mSmartMHSList) {
                    List<Integer> mIndexeList = new ArrayList<>();
                    int count = 0;
                    for (SmartMHSInfo var : SemWifiApSmartGattClient.this.mSmartMHSList) {
                        if (!(var.state == SemWifiApSmartGattClient.ST_BONDING_FAILURE || var.state == -2)) {
                            if (var.state != SemWifiApSmartGattClient.ST_GATT_FAILURE) {
                                if (System.currentTimeMillis() - var.timestamp > 10000 && var.state < 0) {
                                    mIndexeList.add(Integer.valueOf(count));
                                    count++;
                                } else if (var.state == 1 && !SemWifiApSmartGattClient.this.isBondingGoingon && System.currentTimeMillis() - var.timestamp > 60000) {
                                    Log.e(SemWifiApSmartGattClient.this.TAG, " BLE transactions going on more than 60 sec, disconnecting gatt");
                                    SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tBLE transactions going on more than 60 sec, disconnecting gatt");
                                    var.state = SemWifiApSmartGattClient.ST_MHS_GATT_CLIENT_TIMEOUT;
                                    var.timestamp = System.currentTimeMillis();
                                    SemWifiApSmartGattClient.this.invokeCallback(var.clientMAC, SemWifiApSmartGattClient.ST_MHS_GATT_CLIENT_TIMEOUT);
                                    rcount++;
                                    if (SemWifiApSmartGattClient.this.mConnectedGatt != null) {
                                        SemWifiApSmartGattClient.this.shutdownclient();
                                    }
                                    count++;
                                } else if (var.state == 1 && System.currentTimeMillis() - var.timestamp > 45000) {
                                    if (SemWifiApSmartGattClient.this.mConnectedGatt != null) {
                                        Log.e(SemWifiApSmartGattClient.this.TAG, "mConnectedGatt is not null after 45 sec, so disconnecting gatt");
                                        SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tmConnectedGatt is not null after 45 sec, so disconnecting gatt");
                                        var.state = SemWifiApSmartGattClient.ST_MHS_GATT_CLIENT_TIMEOUT;
                                        var.timestamp = System.currentTimeMillis();
                                        SemWifiApSmartGattClient.this.invokeCallback(var.clientMAC, SemWifiApSmartGattClient.ST_MHS_GATT_CLIENT_TIMEOUT);
                                        rcount++;
                                        SemWifiApSmartGattClient.this.shutdownclient();
                                    } else if (SemWifiApSmartGattClient.this.mConnectedGatt == null) {
                                        Log.e(SemWifiApSmartGattClient.this.TAG, "mConnectedGatt is null after 45 sec,but state is Gatt Connecting ");
                                        SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tmConnectedGatt is null after 45 sec,but state is Gatt Connecting");
                                        var.state = SemWifiApSmartGattClient.ST_MHS_GATT_CLIENT_TIMEOUT;
                                        var.timestamp = System.currentTimeMillis();
                                        SemWifiApSmartGattClient.this.invokeCallback(var.clientMAC, SemWifiApSmartGattClient.ST_MHS_GATT_CLIENT_TIMEOUT);
                                        SemWifiApSmartGattClient.this.shutdownclient();
                                        rcount++;
                                    }
                                    count++;
                                } else if (var.state == 2 && System.currentTimeMillis() - var.timestamp > IWCEventManager.reconTimeThreshold) {
                                    boolean unused10 = SemWifiApSmartGattClient.this.mwaitingToConnect = false;
                                    Log.e(SemWifiApSmartGattClient.this.TAG, "Wifi connection timeout after 45 sec, so dont try to connect");
                                    SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tWifi connection timeout after 45 sec, so dont try to connect");
                                    var.state = SemWifiApSmartGattClient.ST_MHS_WIFI_CONNECTION_TIMEOUT;
                                    var.timestamp = System.currentTimeMillis();
                                    SemWifiApSmartGattClient.this.invokeCallback(var.clientMAC, SemWifiApSmartGattClient.ST_MHS_WIFI_CONNECTION_TIMEOUT);
                                    rcount++;
                                    count++;
                                } else if (var.state < 0 || var.state == 1 || var.state == 2) {
                                    rcount++;
                                    count++;
                                } else {
                                    count++;
                                }
                            }
                        }
                        if (!WifiInjector.getInstance().getSemWifiApSmartClient().getBLEPairingFailedHistory(var.clientMAC)) {
                            mIndexeList.add(Integer.valueOf(count));
                        } else {
                            rcount++;
                        }
                        count++;
                    }
                    try {
                        for (Integer intValue : mIndexeList) {
                            SemWifiApSmartGattClient.this.mSmartMHSList.remove(intValue.intValue());
                        }
                    } catch (IndexOutOfBoundsException e) {
                    }
                    SemWifiApSmartGattClient.this.showSmartMHSInfo();
                }
                if (rcount > 0) {
                    sendEmptyMessageDelayed(18, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
                }
            } else if (i == 19) {
                Log.d(SemWifiApSmartGattClient.this.TAG, "Device didn't get mtu callback so this device is using default value.");
                SemWifiApSmartGattClient.this.mLocalLog.log(SemWifiApSmartGattClient.this.TAG + ":\tDevice didn't get mtu callback so this device is using default value.");
                if (SemWifiApSmartGattClient.this.mConnectedGatt != null) {
                    SemWifiApSmartGattClient.this.mConnectedGatt.requestMtu(512);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public int channelToFreq(int channel) {
        if (channel < 1 || channel > 165) {
            return -1;
        }
        return (channel <= 14 ? 2407 : 5000) + (channel * 5);
    }

    /* access modifiers changed from: private */
    public void startPartialScanAfterSleep(int sleepTime) {
        String str = this.TAG;
        Log.d(str, "startPartialScanAfterSleep() trying to semStartPartialChannelScan after sleep " + sleepTime + " mMhsFreq:" + this.mMhsFreq);
        LocalLog localLog = this.mLocalLog;
        localLog.log(this.TAG + ":\tstartPartialScanAfterSleep() trying to semStartPartialChannelScan after sleep " + sleepTime + " mMhsFreq:" + this.mMhsFreq);
        try {
            Thread.sleep((long) sleepTime);
        } catch (Exception e) {
        }
        int i = this.mMhsFreq;
        int[] freqs = {i};
        if (i == -1) {
            freqs = new int[]{2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447, 2452, 2457, 2462, 5745};
        }
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (wifiManager != null && !wifiManager.semStartPartialChannelScan(freqs)) {
            wifiManager.semStartPartialChannelScan(freqs);
        }
    }

    /* access modifiers changed from: private */
    public void setConnectionState(int state, String MhsMac) {
        String str = this.TAG;
        Log.d(str, "setConnectionState state " + state + "MhsMac" + MhsMac);
        LocalLog localLog = this.mLocalLog;
        localLog.log(this.TAG + ":\tsetConnectionState state :" + state + "MhsMac" + MhsMac);
        if (state == ST_BONDING_FAILURE || state == -2 || state == ST_GATT_FAILURE) {
            WifiInjector.getInstance().getSemWifiApSmartClient().setBLEPairingFailedHistory(MhsMac, new Pair(Long.valueOf(System.currentTimeMillis()), this.mSmartAp_BLE_MAC));
        }
        updateSmartMHSConnectionStatus(MhsMac, state);
        WifiManager.SemWifiApSmartCallback semWifiApSmartCallback = this.tSemWifiApSmartCallback;
        if (semWifiApSmartCallback != null) {
            try {
                semWifiApSmartCallback.onStateChanged(state, MhsMac);
            } catch (Exception e) {
            }
        }
    }

    /* access modifiers changed from: private */
    public void shutdownclient_1() {
        Log.d(this.TAG, "shutdownclient_1");
        this.requestedToEnableMHS = false;
        BluetoothGatt bluetoothGatt = this.mConnectedGatt;
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
        this.isBondingGoingon = false;
        this.mConnectedGatt = null;
        this.mTimeMismatchOccured = false;
    }

    /* access modifiers changed from: private */
    public void shutdownclient() {
        Log.d(this.TAG, "shutdownclient");
        this.requestedToEnableMHS = false;
        BluetoothGatt bluetoothGatt = this.mConnectedGatt;
        if (bluetoothGatt != null) {
            this.mFailedBLEConnections.remove(bluetoothGatt.getDevice().getAddress());
            this.mConnectedGatt.close();
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
        this.isBondingGoingon = false;
        this.mConnectedGatt = null;
        this.mTimeMismatchOccured = false;
    }

    private static class SmartMHSInfo {
        public String MHS_MAC;
        public String clientMAC;
        public int state;
        public long timestamp;

        public SmartMHSInfo(String mClientMAC, String tMHS_MAC, long mTimeStamp, int mState) {
            this.clientMAC = mClientMAC;
            this.MHS_MAC = tMHS_MAC;
            this.timestamp = mTimeStamp;
            this.state = mState;
        }

        public String toString() {
            return String.format("clientMAC:" + this.clientMAC + ",MHS_MAC:" + this.MHS_MAC + ",timestamp:" + this.timestamp + ",state:" + this.state, new Object[0]);
        }
    }

    /* access modifiers changed from: package-private */
    public void showSmartMHSInfo() {
        synchronized (this.mSmartMHSList) {
            for (SmartMHSInfo var : this.mSmartMHSList) {
                String str = this.TAG;
                Log.d(str, "" + var);
            }
        }
    }

    private void updateSmartMHSConnectionStatus(String clientmac, int state) {
        synchronized (this.mSmartMHSList) {
            for (SmartMHSInfo inf : this.mSmartMHSList) {
                if (inf.clientMAC.equals(clientmac)) {
                    inf.state = state;
                    inf.timestamp = System.currentTimeMillis();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void invokeCallback(String clientmac, int state) {
        String str = this.TAG;
        Log.d(str, "invokeCallback state " + state + "clientmac" + clientmac);
        LocalLog localLog = this.mLocalLog;
        localLog.log(this.TAG + ":\tsinvokeCallback state :" + state + "clientmac" + clientmac);
        WifiManager.SemWifiApSmartCallback semWifiApSmartCallback = this.tSemWifiApSmartCallback;
        if (semWifiApSmartCallback != null) {
            try {
                semWifiApSmartCallback.onStateChanged(state, clientmac);
            } catch (Exception e) {
            }
        }
    }

    public void factoryReset() {
        long ident = Binder.clearCallingIdentity();
        Log.d(this.TAG, "factoryReset is called");
        LocalLog localLog = this.mLocalLog;
        localLog.log(this.TAG + ":\tfactoryReset is called");
        try {
            Iterator<String> tempIt = this.bonedDevicesFromHotspotLive.iterator();
            if (BluetoothAdapter.getDefaultAdapter() != null) {
                while (tempIt != null && tempIt.hasNext()) {
                    BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(tempIt.next());
                    if (device != null && device.getBondState() == 12) {
                        device.removeBond();
                        String str = this.TAG;
                        Log.d(str, ":factoryReset smarttethering.removeBond :" + device.getAddress());
                        LocalLog localLog2 = this.mLocalLog;
                        localLog2.log(this.TAG + ":\tfactoryReset smarttethering.removeBond :" + device.getAddress());
                    }
                }
                this.bonedDevicesFromHotspotLive.clear();
                Settings.Secure.putString(this.mContext.getContentResolver(), "bonded_device_clientside", (String) null);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
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
        String str = this.TAG;
        Log.e(str, "Trying to create a new connection. attempt:" + count2);
        LocalLog localLog = this.mLocalLog;
        localLog.log(this.TAG + "\tTrying to create a new connection. attempt:" + count2);
        setConnectionState(1, this.mSmartAp_WiFi_MAC);
        this.mConnectedGatt = mDevice.connectGatt(this.mContext, autoConnect, this.mGattCallback, 2);
        if (this.mConnectedGatt == null) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            String str2 = this.TAG;
            Log.e(str2, "mConnectedGatt = null, Trying to create a new connection. attempt:" + count2);
            LocalLog localLog2 = this.mLocalLog;
            localLog2.log(this.TAG + "\tmConnectedGatt = null, Trying to create a new connection. attempt:" + count2);
            setConnectionState(1, this.mSmartAp_WiFi_MAC);
            this.mConnectedGatt = mDevice.connectGatt(this.mContext, true, this.mGattCallback, 2);
            if (this.mConnectedGatt == null) {
                Log.e(this.TAG, " mConnectedGatt = null, returning false");
                LocalLog localLog3 = this.mLocalLog;
                localLog3.log(this.TAG + "\tmConnectedGatt = null, returning false");
                return false;
            }
        }
        return true;
    }

    /* access modifiers changed from: private */
    public String removeDoubleQuotes(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }
}
