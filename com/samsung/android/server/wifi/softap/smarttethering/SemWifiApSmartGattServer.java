package com.samsung.android.server.wifi.softap.smarttethering;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.SemDeviceInfo;
import android.net.Uri;
import android.net.wifi.SemWifiApSmartWhiteList;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.widget.Toast;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.net.wifi.SemWifiApContentProviderHelper;
import com.samsung.android.server.wifi.dqa.ReportIdKey;
import com.sec.android.app.CscFeatureTagSetting;
import com.sec.android.app.CscFeatureTagWifi;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class SemWifiApSmartGattServer {
    private static final String ACTION_LOGOUT_ACCOUNTS_COMPLETE = "com.samsung.account.SAMSUNGACCOUNT_SIGNOUT_COMPLETED";
    private static final int BLE_PACKET_SIZE_LIMIT_FOR_DEVICE_NAME = 34;
    public static final String CONFIGOPBRANDINGFORMOBILEAP = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDINGFORMOBILEAP, "ALL");
    private static final String TAG = "SemWifiApSmartGattServer";
    private static final String WIFIAP_WARNING_CLASS = "com.samsung.android.settings.wifi.mobileap.WifiApWarning";
    private static final String WIFIAP_WARNING_DIALOG = "com.samsung.android.settings.wifi.mobileap.wifiapwarning";
    private static final String WIFIAP_WARNING_DIALOG_TYPE = "wifiap_warning_dialog_type";
    private static IntentFilter mSemWifiApSmartGattServerIntentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
    private final int COMMAND_ENABLE_HOTSPOT = 2;
    private final int DISPLAY_JOINED_NEW_FAMILYID_TOAST = 5;
    private final int DISPLAY_NO_UPDATE_FAMILYID_TOAST = 4;
    private final int SEND_NOTIFICATION = 8;
    private final int START_HOTSPOT_ENABLED_TIMEOUT_WITHOUT_CLIENT = 9;
    private final int START_HOTSPOT_ENABLED_TIME_WITHOUT_CLIENT = 60000;
    private final int START_HOTSPOT_ENABLING_TIME = 60000;
    private final int START_HOTSPOT_ENABLING_TIMEOUT = 1;
    private final int STORE_BONDED_ADDRESS = 6;
    private final int WAIT_ACCEPT_INVITATION = 7;
    /* access modifiers changed from: private */
    public HashSet<String> bonedDevicesFromHotspotLive = new HashSet<>();
    /* access modifiers changed from: private */
    public boolean isAutoHotspotServerSet;
    private boolean isJDMDevice = "in_house".contains("jdm");
    /* access modifiers changed from: private */
    public boolean isMHSEnabledSmartly = false;
    /* access modifiers changed from: private */
    public boolean isMHSEnabledViaIntent = false;
    /* access modifiers changed from: private */
    public boolean isWaitingForAcceptStatus;
    /* access modifiers changed from: private */
    public boolean isWaitingForMHSStatus;
    HashMap<String, Integer> mAuthDevices = new HashMap<>();
    /* access modifiers changed from: private */
    public BleWorkHandler mBleWorkHandler = null;
    private HandlerThread mBleWorkThread = null;
    /* access modifiers changed from: private */
    public boolean mBluetoothIsOn = false;
    private BluetoothManager mBluetoothManager;
    /* access modifiers changed from: private */
    public String mBondingAddress;
    HashMap<String, ClientVer> mClientConnections = new HashMap<>();
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public String mFamilyID;
    public BluetoothGattServer mGattServer;
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.d(SemWifiApSmartGattServer.TAG, "onConnectionStateChange " + SemWifiApSmartGattServer.this.mSemWifiApSmartUtil.getStatusDescription(status) + " " + SemWifiApSmartGattServer.this.mSemWifiApSmartUtil.getStateDescription(newState));
            if (newState == 2) {
                SemWifiApSmartGattServer.this.mAuthDevices.put(device.getAddress(), 0);
                SemWifiApSmartGattServer.this.mClientConnections.put(device.getAddress(), new ClientVer());
                Log.d(SemWifiApSmartGattServer.TAG, "connected device:" + device);
                String unused = SemWifiApSmartGattServer.this.mBondingAddress = device.getAddress();
                int unused2 = SemWifiApSmartGattServer.this.mVersion = 0;
                int unused3 = SemWifiApSmartGattServer.this.mUserType = -1;
                LocalLog access$400 = SemWifiApSmartGattServer.this.mLocalLog;
                access$400.log("SemWifiApSmartGattServer:\tGattServer connected device:" + SemWifiApSmartGattServer.this.mBondingAddress);
            } else if (newState == 0) {
                SemWifiApSmartGattServer.this.mAuthDevices.remove(device.getAddress());
                SemWifiApSmartGattServer.this.mClientConnections.remove(device.getAddress());
                Log.d(SemWifiApSmartGattServer.TAG, "disconnected device:" + device);
                LocalLog access$4002 = SemWifiApSmartGattServer.this.mLocalLog;
                access$4002.log("SemWifiApSmartGattServer:\tGattServer disconnected device:" + device);
                String unused4 = SemWifiApSmartGattServer.this.mBondingAddress = null;
            }
        }

        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.d(SemWifiApSmartGattServer.TAG, "onServiceAdded:" + service.getUuid());
            LocalLog access$400 = SemWifiApSmartGattServer.this.mLocalLog;
            access$400.log("SemWifiApSmartGattServer:\tonServiceAdded:" + service.getUuid());
        }

        /* JADX WARNING: Code restructure failed: missing block: B:50:0x027f, code lost:
            if (r5 == 1) goto L_0x0294;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:96:0x04ed, code lost:
            if (r5 == 1) goto L_0x04ef;
         */
        /* JADX WARNING: Removed duplicated region for block: B:82:0x041b  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice r18, int r19, int r20, android.bluetooth.BluetoothGattCharacteristic r21) {
            /*
                r17 = this;
                r0 = r17
                super.onCharacteristicReadRequest(r18, r19, r20, r21)
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "onCharacteristicReadRequest:: "
                r1.append(r2)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r2 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r2 = r2.mSemWifiApSmartUtil
                java.util.UUID r3 = r21.getUuid()
                java.lang.String r2 = r2.lookup(r3)
                r1.append(r2)
                java.lang.String r1 = r1.toString()
                java.lang.String r2 = "SemWifiApSmartGattServer"
                android.util.Log.d(r2, r1)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r1.mSemWifiApSmartUtil
                java.util.UUID r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.CHARACTERISTIC_D2D_CLIENT_BOND_STATUS
                java.util.UUID r3 = r21.getUuid()
                boolean r1 = r1.equals(r3)
                java.lang.String r3 = "Sent bond status "
                r4 = 10
                r5 = 12
                r6 = 0
                r7 = 1
                if (r1 == 0) goto L_0x0072
                byte[] r1 = new byte[r4]
                r1[r6] = r6
                int r4 = r18.getBondState()
                if (r4 != r5) goto L_0x004e
                r1[r6] = r7
            L_0x004e:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.bluetooth.BluetoothGattServer r8 = r4.mGattServer
                r11 = 0
                r12 = 0
                r9 = r18
                r10 = r19
                r13 = r1
                r8.sendResponse(r9, r10, r11, r12, r13)
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                r4.append(r3)
                byte r3 = r1[r6]
                r4.append(r3)
                java.lang.String r3 = r4.toString()
                android.util.Log.d(r2, r3)
                goto L_0x05cc
            L_0x0072:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r1.mSemWifiApSmartUtil
                java.util.UUID r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.CHARACTERISTIC_CLIENT_MAC
                java.util.UUID r8 = r21.getUuid()
                boolean r1 = r1.equals(r8)
                if (r1 == 0) goto L_0x00fe
                r1 = 150(0x96, float:2.1E-43)
                byte[] r1 = new byte[r1]
                int r3 = r18.getBondState()
                if (r3 != r5) goto L_0x00ee
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r3 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r3 = r3.mSemWifiApSmartUtil
                java.lang.String r3 = r3.getDeviceName()
                byte[] r3 = r3.getBytes()
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r4 = r4.mSemWifiApSmartUtil
                java.lang.String r4 = r4.getOwnWifiMac()
                byte[] r4 = r4.getBytes()
                r1[r6] = r6
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                boolean r5 = r5.isWaitingForAcceptStatus
                if (r5 != 0) goto L_0x00b5
                r1[r6] = r7
            L_0x00b5:
                r5 = 0
            L_0x00b6:
                int r6 = r4.length
                if (r5 >= r6) goto L_0x00c2
                int r6 = r5 + 1
                byte r7 = r4[r5]
                r1[r6] = r7
                int r5 = r5 + 1
                goto L_0x00b6
            L_0x00c2:
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r6 = "device length:"
                r5.append(r6)
                int r6 = r3.length
                r5.append(r6)
                java.lang.String r5 = r5.toString()
                android.util.Log.d(r2, r5)
                r2 = 18
                int r5 = r3.length
                byte r5 = (byte) r5
                r1[r2] = r5
                r2 = 0
            L_0x00de:
                r5 = 34
                if (r2 >= r5) goto L_0x00ee
                int r5 = r3.length
                if (r2 >= r5) goto L_0x00ee
                int r5 = r2 + 19
                byte r6 = r3[r2]
                r1[r5] = r6
                int r2 = r2 + 1
                goto L_0x00de
            L_0x00ee:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r2 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.bluetooth.BluetoothGattServer r8 = r2.mGattServer
                r11 = 0
                r12 = 0
                r9 = r18
                r10 = r19
                r13 = r1
                r8.sendResponse(r9, r10, r11, r12, r13)
                goto L_0x05cc
            L_0x00fe:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r1.mSemWifiApSmartUtil
                java.util.UUID r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.CHARACTERISTIC_MHS_SIDE_GET_TIME
                java.util.UUID r8 = r21.getUuid()
                boolean r1 = r1.equals(r8)
                java.lang.String r8 = ""
                if (r1 == 0) goto L_0x018f
                long r3 = java.lang.System.currentTimeMillis()
                java.lang.Long r1 = java.lang.Long.valueOf(r3)
                r3 = 0
                if (r1 != 0) goto L_0x0124
                long r4 = java.lang.System.currentTimeMillis()
                java.lang.Long r1 = java.lang.Long.valueOf(r4)
            L_0x0124:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.util.LocalLog r4 = r4.mLocalLog
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r6 = "SemWifiApSmartGattServer:\tsystem time is :"
                r5.append(r6)
                r5.append(r1)
                java.lang.String r5 = r5.toString()
                r4.log(r5)
                if (r1 == 0) goto L_0x016b
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                r4.append(r8)
                r4.append(r1)
                java.lang.String r4 = r4.toString()
                byte[] r3 = r4.getBytes()
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r6 = "sending mhs time:"
                r5.append(r6)
                java.lang.String r6 = java.util.Arrays.toString(r3)
                r5.append(r6)
                java.lang.String r5 = r5.toString()
                android.util.Log.i(r2, r5)
            L_0x016b:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.bluetooth.BluetoothGattServer r6 = r4.mGattServer
                r9 = 0
                r10 = 0
                r7 = r18
                r8 = r19
                r11 = r3
                r6.sendResponse(r7, r8, r9, r10, r11)
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                java.lang.String r5 = "Sent mhs time "
                r4.append(r5)
                r4.append(r1)
                java.lang.String r4 = r4.toString()
                android.util.Log.e(r2, r4)
                goto L_0x05cc
            L_0x018f:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r1.mSemWifiApSmartUtil
                java.util.UUID r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.CHARACTERISTIC_MHS_BOND_STATUS
                java.util.UUID r9 = r21.getUuid()
                boolean r1 = r1.equals(r9)
                if (r1 == 0) goto L_0x01f7
                byte[] r1 = new byte[r4]
                r1[r6] = r6
                int r4 = r18.getBondState()
                if (r4 != r5) goto L_0x01d3
                r1[r6] = r7
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r4 = r4.mBleWorkHandler
                if (r4 == 0) goto L_0x01d3
                android.os.Message r4 = new android.os.Message
                r4.<init>()
                r5 = 6
                r4.what = r5
                java.lang.String r5 = r18.getAddress()
                r4.obj = r5
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r5 = r5.mBleWorkHandler
                if (r5 == 0) goto L_0x01d3
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r5 = r5.mBleWorkHandler
                r5.sendMessage(r4)
            L_0x01d3:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.bluetooth.BluetoothGattServer r9 = r4.mGattServer
                r12 = 0
                r13 = 0
                r10 = r18
                r11 = r19
                r14 = r1
                r9.sendResponse(r10, r11, r12, r13, r14)
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                r4.append(r3)
                byte r3 = r1[r6]
                r4.append(r3)
                java.lang.String r3 = r4.toString()
                android.util.Log.d(r2, r3)
                goto L_0x05cc
            L_0x01f7:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r1.mSemWifiApSmartUtil
                java.util.UUID r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.CHARACTERISTIC_AUTH_STATUS
                java.util.UUID r3 = r21.getUuid()
                boolean r1 = r1.equals(r3)
                java.lang.String r3 = "wifi"
                if (r1 == 0) goto L_0x0475
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r4 = r1.mSemWifiApSmartUtil
                java.lang.String r4 = r4.getlegacySSID()
                java.lang.String unused = r1.mSSID = r4
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r4 = r1.mSemWifiApSmartUtil
                java.lang.String r4 = r4.getlegacyPassword()
                java.lang.String unused = r1.mPassword = r4
                r1 = 200(0xc8, float:2.8E-43)
                byte[] r1 = new byte[r1]
                java.util.Arrays.fill(r1, r6)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r8 = r4.mClientConnections
                java.lang.String r9 = r18.getAddress()
                java.lang.Object r8 = r8.get(r9)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r8 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r8
                int r8 = r8.mVersion
                int unused = r4.mVersion = r8
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r8 = r4.mClientConnections
                java.lang.String r9 = r18.getAddress()
                java.lang.Object r8 = r8.get(r9)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r8 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r8
                int r8 = r8.mUserType
                int unused = r4.mUserType = r8
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r4 = r4.mClientConnections
                java.lang.String r8 = r18.getAddress()
                java.lang.Object r4 = r4.get(r8)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r4 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r4
                java.lang.String r4 = r4.mAESKey
                int r8 = r18.getBondState()
                if (r8 == r5) goto L_0x0294
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r5 = r5.mVersion
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r9.mSemWifiApSmartUtil
                if (r5 != r7) goto L_0x0282
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r5 = r5.mUserType
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r9.mSemWifiApSmartUtil
                if (r5 != r7) goto L_0x0282
                goto L_0x0294
            L_0x0282:
                java.lang.String r3 = "client device is not bonded"
                android.util.Log.e(r2, r3)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r3 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.util.LocalLog r3 = r3.mLocalLog
                java.lang.String r5 = "SemWifiApSmartGattServer:\tclient device is not bonded"
                r3.log(r5)
                goto L_0x03b2
            L_0x0294:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, java.lang.Integer> r5 = r5.mAuthDevices
                java.lang.String r9 = r18.getAddress()
                java.lang.Object r5 = r5.get(r9)
                java.lang.Integer r5 = (java.lang.Integer) r5
                int r5 = r5.intValue()
                byte r5 = (byte) r5
                r1[r6] = r5
                byte r5 = r1[r6]
                java.lang.String r9 = "mWifiMAC:"
                r10 = 9
                if (r5 != r7) goto L_0x0377
                r5 = 0
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r11 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r11 = r11.mSSID
                if (r11 == 0) goto L_0x02c5
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r11 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r11 = r11.mSSID
                byte[] r11 = r11.getBytes()
                int r5 = r11.length
            L_0x02c5:
                r11 = 0
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r12 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r12 = r12.mPassword
                if (r12 == 0) goto L_0x02d9
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r12 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r12 = r12.mPassword
                byte[] r12 = r12.getBytes()
                int r11 = r12.length
            L_0x02d9:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r12 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r12 = r12.mSemWifiApSmartUtil
                java.lang.String r12 = r12.getOwnWifiMac()
                java.lang.String r12 = r12.toLowerCase()
                java.lang.String r10 = r12.substring(r10)
                java.lang.StringBuilder r12 = new java.lang.StringBuilder
                r12.<init>()
                r12.append(r9)
                r12.append(r10)
                java.lang.String r9 = r12.toString()
                android.util.Log.e(r2, r9)
                byte[] r9 = r10.getBytes()
                r12 = 0
            L_0x0302:
                int r13 = r9.length
                if (r12 >= r13) goto L_0x0311
                int r13 = r5 + 3
                int r13 = r13 + r11
                int r13 = r13 + r7
                int r13 = r13 + r12
                byte r14 = r9[r12]
                r1[r13] = r14
                int r12 = r12 + 1
                goto L_0x0302
            L_0x0311:
                byte r12 = (byte) r5
                r1[r7] = r12
                r12 = 0
            L_0x0315:
                if (r12 >= r5) goto L_0x032a
                int r13 = r12 + 2
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r14 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r14 = r14.mSSID
                byte[] r14 = r14.getBytes()
                byte r14 = r14[r12]
                r1[r13] = r14
                int r12 = r12 + 1
                goto L_0x0315
            L_0x032a:
                int r12 = r5 + 2
                byte r13 = (byte) r11
                r1[r12] = r13
                r12 = 0
            L_0x0330:
                if (r12 >= r11) goto L_0x0346
                int r13 = r12 + 3
                int r13 = r13 + r5
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r14 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r14 = r14.mPassword
                byte[] r14 = r14.getBytes()
                byte r14 = r14[r12]
                r1[r13] = r14
                int r12 = r12 + 1
                goto L_0x0330
            L_0x0346:
                int r12 = r5 + 3
                int r12 = r12 + r11
                r1[r12] = r6
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r12 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.content.Context r12 = r12.mContext
                java.lang.Object r3 = r12.getSystemService(r3)
                android.net.wifi.WifiManager r3 = (android.net.wifi.WifiManager) r3
                int r12 = r3.getWifiApState()
                r13 = 13
                if (r12 != r13) goto L_0x0364
                int r13 = r5 + 3
                int r13 = r13 + r11
                r1[r13] = r7
            L_0x0364:
                int r13 = r5 + 3
                int r13 = r13 + r11
                int r13 = r13 + r7
                int r13 = r13 + 8
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r14 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r14 = r14.mSemWifiApSmartUtil
                byte r14 = r14.getSecurityType()
                r1[r13] = r14
                goto L_0x03b1
            L_0x0377:
                byte r3 = r1[r6]
                if (r3 != 0) goto L_0x03b1
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r3 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r3 = r3.mSemWifiApSmartUtil
                java.lang.String r3 = r3.getOwnWifiMac()
                java.lang.String r3 = r3.toLowerCase()
                java.lang.String r3 = r3.substring(r10)
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                r5.append(r9)
                r5.append(r3)
                java.lang.String r5 = r5.toString()
                android.util.Log.e(r2, r5)
                byte[] r5 = r3.getBytes()
                r9 = 0
            L_0x03a4:
                int r10 = r5.length
                if (r9 >= r10) goto L_0x03b0
                int r10 = r9 + 1
                byte r11 = r5[r9]
                r1[r10] = r11
                int r9 = r9 + 1
                goto L_0x03a4
            L_0x03b0:
                goto L_0x03b2
            L_0x03b1:
            L_0x03b2:
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r5 = "Sent Auth status "
                r3.append(r5)
                byte r5 = r1[r6]
                r3.append(r5)
                java.lang.String r5 = ",mVersion:"
                r3.append(r5)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r5 = r5.mVersion
                r3.append(r5)
                java.lang.String r5 = ",mUserType:"
                r3.append(r5)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r5 = r5.mUserType
                r3.append(r5)
                java.lang.String r5 = ",bonded_state:"
                r3.append(r5)
                r3.append(r8)
                java.lang.String r3 = r3.toString()
                android.util.Log.d(r2, r3)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r3 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.util.LocalLog r3 = r3.mLocalLog
                java.lang.StringBuilder r9 = new java.lang.StringBuilder
                r9.<init>()
                java.lang.String r10 = "SemWifiApSmartGattServer:\tSent Auth status "
                r9.append(r10)
                byte r6 = r1[r6]
                r9.append(r6)
                r9.append(r5)
                r9.append(r8)
                java.lang.String r5 = r9.toString()
                r3.log(r5)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r3 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r3 = r3.mVersion
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r5.mSemWifiApSmartUtil
                if (r3 != r7) goto L_0x0465
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r3 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r3 = r3.mUserType
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r5.mSemWifiApSmartUtil
                if (r3 != r7) goto L_0x0465
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r5 = "Using AES:"
                r3.append(r5)
                r3.append(r4)
                java.lang.String r3 = r3.toString()
                android.util.Log.d(r2, r3)
                java.lang.String r3 = new java.lang.String
                r3.<init>(r1)
                java.lang.String r3 = com.samsung.android.server.wifi.softap.smarttethering.AES.encrypt(r3, r4)
                byte[] r1 = r3.getBytes()
                if (r1 != 0) goto L_0x0450
                java.lang.String r3 = " Encryption can't be null"
                android.util.Log.e(r2, r3)
            L_0x0450:
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r5 = " Encryption length:"
                r3.append(r5)
                int r5 = r1.length
                r3.append(r5)
                java.lang.String r3 = r3.toString()
                android.util.Log.d(r2, r3)
            L_0x0465:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r2 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.bluetooth.BluetoothGattServer r9 = r2.mGattServer
                r12 = 0
                r13 = 0
                r10 = r18
                r11 = r19
                r14 = r1
                r9.sendResponse(r10, r11, r12, r13, r14)
                goto L_0x05cc
            L_0x0475:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r1.mSemWifiApSmartUtil
                java.util.UUID r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.CHARACTERISTIC_MHS_STATUS_UUID
                java.util.UUID r4 = r21.getUuid()
                boolean r1 = r1.equals(r4)
                if (r1 == 0) goto L_0x05cc
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r1 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.content.Context r1 = r1.mContext
                java.lang.Object r1 = r1.getSystemService(r3)
                android.net.wifi.WifiManager r1 = (android.net.wifi.WifiManager) r1
                r3 = 50
                byte[] r3 = new byte[r3]
                r3[r6] = r6
                r4 = 0
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r10 = r9.mClientConnections
                java.lang.String r11 = r18.getAddress()
                java.lang.Object r10 = r10.get(r11)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r10 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r10
                int r10 = r10.mVersion
                int unused = r9.mVersion = r10
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r10 = r9.mClientConnections
                java.lang.String r11 = r18.getAddress()
                java.lang.Object r10 = r10.get(r11)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r10 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r10
                int r10 = r10.mUserType
                int unused = r9.mUserType = r10
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r9 = r9.mClientConnections
                java.lang.String r10 = r18.getAddress()
                java.lang.Object r9 = r9.get(r10)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r9 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r9
                java.lang.String r15 = r9.mAESKey
                int r9 = r18.getBondState()
                if (r9 == r5) goto L_0x04ef
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r5 = r5.mVersion
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r9.mSemWifiApSmartUtil
                if (r5 != r7) goto L_0x057e
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r5 = r5.mUserType
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r9.mSemWifiApSmartUtil
                if (r5 != r7) goto L_0x057e
            L_0x04ef:
                int r5 = r1.getWifiApState()
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                boolean r9 = r9.isMHSEnabledViaIntent
                if (r9 == 0) goto L_0x057e
                r3[r6] = r7
                int r4 = r1.semGetWifiApChannel()
                r9 = 0
                if (r4 < r7) goto L_0x052b
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                r10.append(r8)
                r10.append(r4)
                java.lang.String r8 = r10.toString()
                int r9 = r8.length()
                byte r10 = (byte) r9
                r3[r7] = r10
                r10 = 0
            L_0x051b:
                if (r10 >= r9) goto L_0x052a
                int r11 = r10 + 2
                byte[] r12 = r8.getBytes()
                byte r12 = r12[r10]
                r3[r11] = r12
                int r10 = r10 + 1
                goto L_0x051b
            L_0x052a:
                goto L_0x052d
            L_0x052b:
                r3[r7] = r6
            L_0x052d:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r8 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r8 = r8.mSemWifiApSmartUtil
                java.lang.String r8 = r8.getOwnWifiMac()
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r10 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r10 = r10.mSemWifiApSmartUtil
                java.lang.String r10 = r10.getMHSMacFromInterface()
                r11 = 0
                r12 = 0
                if (r10 == 0) goto L_0x0549
                int r12 = r10.length()
            L_0x0549:
                if (r8 == 0) goto L_0x054f
                int r11 = r8.length()
            L_0x054f:
                int r13 = r9 + 2
                byte r14 = (byte) r11
                r3[r13] = r14
                r13 = 0
            L_0x0555:
                if (r13 >= r11) goto L_0x0566
                int r14 = r9 + 2
                int r14 = r14 + r7
                int r14 = r14 + r13
                byte[] r16 = r8.getBytes()
                byte r16 = r16[r13]
                r3[r14] = r16
                int r13 = r13 + 1
                goto L_0x0555
            L_0x0566:
                int r7 = r9 + 3
                int r7 = r7 + r11
                byte r13 = (byte) r12
                r3[r7] = r13
                r7 = 0
            L_0x056d:
                if (r7 >= r12) goto L_0x057e
                int r13 = r11 + 4
                int r13 = r13 + r9
                int r13 = r13 + r7
                byte[] r14 = r10.getBytes()
                byte r14 = r14[r7]
                r3[r13] = r14
                int r7 = r7 + 1
                goto L_0x056d
            L_0x057e:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.bluetooth.BluetoothGattServer r9 = r5.mGattServer
                r12 = 0
                r13 = 0
                r10 = r18
                r11 = r19
                r14 = r3
                r9.sendResponse(r10, r11, r12, r13, r14)
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r7 = "Sent MHS status "
                r5.append(r7)
                byte r7 = r3[r6]
                r5.append(r7)
                java.lang.String r7 = ", mhsChannel:"
                r5.append(r7)
                r5.append(r4)
                java.lang.String r5 = r5.toString()
                android.util.Log.d(r2, r5)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r2 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.util.LocalLog r2 = r2.mLocalLog
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r8 = "SemWifiApSmartGattServer:\tSent MHS status "
                r5.append(r8)
                byte r6 = r3[r6]
                r5.append(r6)
                r5.append(r7)
                r5.append(r4)
                java.lang.String r5 = r5.toString()
                r2.log(r5)
            L_0x05cc:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.C08131.onCharacteristicReadRequest(android.bluetooth.BluetoothDevice, int, int, android.bluetooth.BluetoothGattCharacteristic):void");
        }

        /* JADX WARNING: Code restructure failed: missing block: B:144:0x0480, code lost:
            if (r3 == 1) goto L_0x048d;
         */
        /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r11v27, types: [byte] */
        /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r12v3, types: [byte] */
        /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r12v7, types: [byte] */
        /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r14v3, types: [byte] */
        /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r15v3, types: [byte] */
        /* JADX WARNING: Removed duplicated region for block: B:117:0x03e9  */
        /* JADX WARNING: Removed duplicated region for block: B:119:0x03ec  */
        /* JADX WARNING: Removed duplicated region for block: B:125:0x03f9  */
        /* JADX WARNING: Removed duplicated region for block: B:131:0x040a  */
        /* JADX WARNING: Removed duplicated region for block: B:138:0x0431  */
        /* JADX WARNING: Removed duplicated region for block: B:141:0x0467  */
        /* JADX WARNING: Removed duplicated region for block: B:147:0x048f  */
        /* JADX WARNING: Removed duplicated region for block: B:195:0x07ef  */
        /* JADX WARNING: Removed duplicated region for block: B:90:0x02ac  */
        /* JADX WARNING: Removed duplicated region for block: B:91:0x02b9  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice r29, int r30, android.bluetooth.BluetoothGattCharacteristic r31, boolean r32, boolean r33, int r34, byte[] r35) {
            /*
                r28 = this;
                r1 = r28
                r8 = r35
                super.onCharacteristicWriteRequest(r29, r30, r31, r32, r33, r34, r35)
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r2 = "onCharacteristicWriteRequest:"
                r0.append(r2)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r2 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r2 = r2.mSemWifiApSmartUtil
                java.util.UUID r3 = r31.getUuid()
                java.lang.String r2 = r2.lookup(r3)
                r0.append(r2)
                java.lang.String r0 = r0.toString()
                java.lang.String r2 = "SemWifiApSmartGattServer"
                android.util.Log.d(r2, r0)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r0.mSemWifiApSmartUtil
                java.util.UUID r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.CHARACTERISTIC_FAMILY_ID
                java.util.UUID r3 = r31.getUuid()
                boolean r0 = r0.equals(r3)
                r3 = 12
                r4 = 3
                r6 = 2
                r7 = 0
                java.lang.Integer r9 = java.lang.Integer.valueOf(r7)
                r10 = 1
                if (r0 == 0) goto L_0x02ee
                r0 = 1
                int r9 = r8.length
                r11 = 0
                r12 = 0
                if (r9 != 0) goto L_0x004d
                r0 = 0
            L_0x004d:
                if (r0 == 0) goto L_0x0051
                byte r11 = r8[r7]
            L_0x0051:
                if (r0 == 0) goto L_0x0058
                int r13 = r11 + 2
                if (r9 >= r13) goto L_0x0058
                r0 = 0
            L_0x0058:
                if (r0 == 0) goto L_0x005e
                int r13 = r11 + 1
                byte r12 = r8[r13]
            L_0x005e:
                r13 = 17
                if (r0 == 0) goto L_0x006b
                int r14 = r12 + r11
                int r14 = r14 + r13
                int r14 = r14 + r6
                if (r9 >= r14) goto L_0x006b
                r0 = 0
                r14 = r0
                goto L_0x006c
            L_0x006b:
                r14 = r0
            L_0x006c:
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r15 = "family ID valid:"
                r0.append(r15)
                r0.append(r14)
                java.lang.String r15 = ",rValueLength:"
                r0.append(r15)
                r0.append(r9)
                java.lang.String r15 = ",rDeviceNamelength:"
                r0.append(r15)
                r0.append(r12)
                java.lang.String r0 = r0.toString()
                android.util.Log.e(r2, r0)
                int r0 = r29.getBondState()
                if (r0 != r3) goto L_0x02db
                if (r14 == 0) goto L_0x02db
                java.lang.String r0 = new java.lang.String
                byte r3 = r8[r7]
                r0.<init>(r8, r10, r3)
                r3 = r0
                java.lang.String r0 = new java.lang.String
                byte r15 = r8[r7]
                int r15 = r15 + r6
                byte r6 = r8[r7]
                int r6 = r6 + r10
                byte r6 = r8[r6]
                r0.<init>(r8, r15, r6)
                r6 = r0
                java.lang.String r0 = new java.lang.String
                byte r15 = r8[r7]
                byte r16 = r8[r7]
                int r16 = r16 + 1
                byte r16 = r8[r16]
                int r15 = r15 + r16
                int r15 = r15 + r4
                r0.<init>(r8, r15, r13)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r4 = r4.mSemWifiApSmartUtil
                r15 = r6
                long r5 = r4.generateHashKey(r3)
                java.lang.String r4 = r0.toLowerCase()
                r0 = 0
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r13 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r13 = r13.mSemWifiApSmartUtil
                java.lang.String r13 = r13.getD2DWifiMac()
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r10 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                boolean unused = r10.isWaitingForAcceptStatus = r7
                boolean r7 = android.text.TextUtils.isEmpty(r13)
                java.lang.String r10 = "\n"
                r18 = r0
                r0 = 9
                if (r7 != 0) goto L_0x0100
                java.lang.String[] r7 = r13.split(r10)
                r19 = r9
                java.util.List r9 = java.util.Arrays.asList(r7)
                r20 = r7
                java.lang.String r7 = r4.substring(r0)
                boolean r7 = r9.contains(r7)
                r18 = r7
                goto L_0x0102
            L_0x0100:
                r19 = r9
            L_0x0102:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r7 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r7 = r7.mSemWifiApSmartUtil
                java.lang.String r7 = r7.getD2DFamilyID()
                long r20 = android.os.Binder.clearCallingIdentity()
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x02d3 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r9 = r9.mSemWifiApSmartUtil     // Catch:{ all -> 0x02d3 }
                int r9 = r9.getSamsungAccountCount()     // Catch:{ all -> 0x02d3 }
                if (r9 == 0) goto L_0x013b
                java.lang.String r0 = " device logged in with samsung account, so D2DFamilyID will not be saved"
                android.util.Log.d(r2, r0)     // Catch:{ all -> 0x0135 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x0135 }
                android.util.LocalLog r0 = r0.mLocalLog     // Catch:{ all -> 0x0135 }
                java.lang.String r2 = "SemWifiApSmartGattServer\t:device logged in with samsung account, so D2DFamilyID will not be saved "
                r0.log(r2)     // Catch:{ all -> 0x0135 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x0135 }
                r2 = 1
                boolean unused = r0.isWaitingForAcceptStatus = r2     // Catch:{ all -> 0x0135 }
                r9 = r13
                goto L_0x0200
            L_0x0135:
                r0 = move-exception
                r17 = r3
                r9 = r13
                goto L_0x02d7
            L_0x013b:
                if (r7 == 0) goto L_0x0210
                boolean r9 = r7.isEmpty()     // Catch:{ all -> 0x020a }
                if (r9 == 0) goto L_0x0146
                r9 = r13
                goto L_0x0211
            L_0x0146:
                if (r18 != 0) goto L_0x01a0
                boolean r9 = r7.equals(r3)     // Catch:{ all -> 0x020a }
                if (r9 == 0) goto L_0x01a0
                if (r13 != 0) goto L_0x0156
                java.lang.String r0 = r4.substring(r0)     // Catch:{ all -> 0x0135 }
                r13 = r0
                goto L_0x016d
            L_0x0156:
                java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ all -> 0x0135 }
                r9.<init>()     // Catch:{ all -> 0x0135 }
                r9.append(r13)     // Catch:{ all -> 0x0135 }
                r9.append(r10)     // Catch:{ all -> 0x0135 }
                java.lang.String r0 = r4.substring(r0)     // Catch:{ all -> 0x0135 }
                r9.append(r0)     // Catch:{ all -> 0x0135 }
                java.lang.String r0 = r9.toString()     // Catch:{ all -> 0x0135 }
                r13 = r0
            L_0x016d:
                java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ all -> 0x0135 }
                r0.<init>()     // Catch:{ all -> 0x0135 }
                java.lang.String r9 = "added D2D AutoHotspot MAC2:"
                r0.append(r9)     // Catch:{ all -> 0x0135 }
                r0.append(r13)     // Catch:{ all -> 0x0135 }
                java.lang.String r0 = r0.toString()     // Catch:{ all -> 0x0135 }
                android.util.Log.d(r2, r0)     // Catch:{ all -> 0x0135 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x0135 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r0 = r0.mSemWifiApSmartUtil     // Catch:{ all -> 0x0135 }
                r0.putD2DWifiMac(r13)     // Catch:{ all -> 0x0135 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x0135 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r0 = r0.mBleWorkHandler     // Catch:{ all -> 0x0135 }
                if (r0 == 0) goto L_0x019c
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x0135 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r0 = r0.mBleWorkHandler     // Catch:{ all -> 0x0135 }
                r2 = 4
                r0.sendEmptyMessage(r2)     // Catch:{ all -> 0x0135 }
            L_0x019c:
                r17 = r3
                goto L_0x0298
            L_0x01a0:
                r9 = r13
                boolean r0 = r7.equals(r3)     // Catch:{ all -> 0x0205 }
                if (r0 != 0) goto L_0x01ee
                android.content.Intent r0 = new android.content.Intent     // Catch:{ all -> 0x0205 }
                r0.<init>()     // Catch:{ all -> 0x0205 }
                java.lang.String r10 = "com.android.settings"
                java.lang.String r13 = "com.samsung.android.settings.wifi.mobileap.WifiApWarning"
                r0.setClassName(r10, r13)     // Catch:{ all -> 0x0205 }
                r10 = 268435456(0x10000000, float:2.5243549E-29)
                r0.setFlags(r10)     // Catch:{ all -> 0x0205 }
                java.lang.String r10 = "com.samsung.android.settings.wifi.mobileap.wifiapwarning"
                r0.setAction(r10)     // Catch:{ all -> 0x0205 }
                java.lang.String r10 = "wifiap_warning_dialog_type"
                r13 = 43
                r0.putExtra(r10, r13)     // Catch:{ all -> 0x0205 }
                java.lang.String r10 = "mD2DFamilyID"
                r0.putExtra(r10, r3)     // Catch:{ all -> 0x0205 }
                java.lang.String r10 = "mD2DDeviceName"
                r0.putExtra(r10, r15)     // Catch:{ all -> 0x0205 }
                java.lang.String r10 = "mD2DHashFamily"
                r0.putExtra(r10, r5)     // Catch:{ all -> 0x0205 }
                java.lang.String r10 = "mD2DDeviceWiFiMAC"
                r0.putExtra(r10, r4)     // Catch:{ all -> 0x0205 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r10 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x0205 }
                android.content.Context r10 = r10.mContext     // Catch:{ all -> 0x0205 }
                r10.startActivity(r0)     // Catch:{ all -> 0x0205 }
                java.lang.String r10 = "D2D Family dialog"
                android.util.Log.d(r2, r10)     // Catch:{ all -> 0x0205 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r2 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x0205 }
                r10 = 1
                boolean unused = r2.isWaitingForAcceptStatus = r10     // Catch:{ all -> 0x0205 }
                goto L_0x0200
            L_0x01ee:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x0205 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r0 = r0.mBleWorkHandler     // Catch:{ all -> 0x0205 }
                if (r0 == 0) goto L_0x0200
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x0205 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r0 = r0.mBleWorkHandler     // Catch:{ all -> 0x0205 }
                r2 = 4
                r0.sendEmptyMessage(r2)     // Catch:{ all -> 0x0205 }
            L_0x0200:
                r17 = r3
                r13 = r9
                goto L_0x0298
            L_0x0205:
                r0 = move-exception
                r17 = r3
                goto L_0x02d7
            L_0x020a:
                r0 = move-exception
                r9 = r13
                r17 = r3
                goto L_0x02d7
            L_0x0210:
                r9 = r13
            L_0x0211:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r13 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x02cf }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r13 = r13.mSemWifiApSmartUtil     // Catch:{ all -> 0x02cf }
                r13.putD2DFamilyID(r3)     // Catch:{ all -> 0x02cf }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r13 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x02cf }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r13 = r13.mSemWifiApSmartUtil     // Catch:{ all -> 0x02cf }
                r13.putHashbasedonD2DFamilyid(r5)     // Catch:{ all -> 0x02cf }
                android.content.Intent r13 = new android.content.Intent     // Catch:{ all -> 0x02cf }
                r13.<init>()     // Catch:{ all -> 0x02cf }
                java.lang.String r0 = "com.samsung.android.server.wifi.softap.smarttethering.d2dfamilyid"
                r13.setAction(r0)     // Catch:{ all -> 0x02cf }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x02cf }
                android.content.Context r0 = r0.mContext     // Catch:{ all -> 0x02cf }
                r0.sendBroadcast(r13)     // Catch:{ all -> 0x02cf }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x02cf }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r0 = r0.mBleWorkHandler     // Catch:{ all -> 0x02cf }
                if (r0 == 0) goto L_0x024b
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x02cf }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r0 = r0.mBleWorkHandler     // Catch:{ all -> 0x02cf }
                r17 = r3
                r3 = 5
                r0.sendEmptyMessage(r3)     // Catch:{ all -> 0x0294 }
                goto L_0x024d
            L_0x024b:
                r17 = r3
            L_0x024d:
                if (r18 != 0) goto L_0x0296
                if (r9 != 0) goto L_0x0259
                r0 = 9
                java.lang.String r0 = r4.substring(r0)     // Catch:{ all -> 0x0294 }
                r3 = r0
                goto L_0x0272
            L_0x0259:
                java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ all -> 0x0294 }
                r0.<init>()     // Catch:{ all -> 0x0294 }
                r0.append(r9)     // Catch:{ all -> 0x0294 }
                r0.append(r10)     // Catch:{ all -> 0x0294 }
                r3 = 9
                java.lang.String r3 = r4.substring(r3)     // Catch:{ all -> 0x0294 }
                r0.append(r3)     // Catch:{ all -> 0x0294 }
                java.lang.String r0 = r0.toString()     // Catch:{ all -> 0x0294 }
                r3 = r0
            L_0x0272:
                java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ all -> 0x0291 }
                r0.<init>()     // Catch:{ all -> 0x0291 }
                java.lang.String r9 = "added D2D AutoHotspot MAC1:"
                r0.append(r9)     // Catch:{ all -> 0x0291 }
                r0.append(r3)     // Catch:{ all -> 0x0291 }
                java.lang.String r0 = r0.toString()     // Catch:{ all -> 0x0291 }
                android.util.Log.d(r2, r0)     // Catch:{ all -> 0x0291 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this     // Catch:{ all -> 0x0291 }
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r0 = r0.mSemWifiApSmartUtil     // Catch:{ all -> 0x0291 }
                r0.putD2DWifiMac(r3)     // Catch:{ all -> 0x0291 }
                r13 = r3
                goto L_0x0297
            L_0x0291:
                r0 = move-exception
                r9 = r3
                goto L_0x02d7
            L_0x0294:
                r0 = move-exception
                goto L_0x02d7
            L_0x0296:
                r13 = r9
            L_0x0297:
            L_0x0298:
                android.os.Binder.restoreCallingIdentity(r20)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                boolean r0 = r0.isWaitingForAcceptStatus
                if (r0 == 0) goto L_0x02b9
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r0 = r0.mBleWorkHandler
                if (r0 == 0) goto L_0x02b9
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r0 = r0.mBleWorkHandler
                r2 = 7
                r9 = 30000(0x7530, double:1.4822E-319)
                r0.sendEmptyMessageDelayed(r2, r9)
                goto L_0x02dd
            L_0x02b9:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r0 = r0.mBleWorkHandler
                if (r0 == 0) goto L_0x02dd
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r0 = r0.mBleWorkHandler
                r2 = 8
                r9 = 100
                r0.sendEmptyMessageDelayed(r2, r9)
                goto L_0x02dd
            L_0x02cf:
                r0 = move-exception
                r17 = r3
                goto L_0x02d7
            L_0x02d3:
                r0 = move-exception
                r17 = r3
                r9 = r13
            L_0x02d7:
                android.os.Binder.restoreCallingIdentity(r20)
                throw r0
            L_0x02db:
                r19 = r9
            L_0x02dd:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.bluetooth.BluetoothGattServer r2 = r0.mGattServer
                r5 = 0
                r6 = 0
                r3 = r29
                r4 = r30
                r7 = r35
                r2.sendResponse(r3, r4, r5, r6, r7)
                goto L_0x089a
            L_0x02ee:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r0.mSemWifiApSmartUtil
                java.util.UUID r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.CHARACTERISTIC_MHS_VER_UPDATE
                java.util.UUID r5 = r31.getUuid()
                boolean r0 = r0.equals(r5)
                if (r0 == 0) goto L_0x0353
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r0 = r0.mClientConnections
                java.lang.String r2 = r29.getAddress()
                java.lang.Object r0 = r0.get(r2)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r0 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r0
                byte r2 = r8[r7]
                r0.mVersion = r2
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r0 = r0.mClientConnections
                java.lang.String r2 = r29.getAddress()
                java.lang.Object r0 = r0.get(r2)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r0 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r0
                r2 = 1
                byte r2 = r8[r2]
                r0.mUserType = r2
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r0 = r0.mClientConnections
                java.lang.String r2 = r29.getAddress()
                java.lang.Object r0 = r0.get(r2)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r0 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r0
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r2 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r2 = r2.mSemWifiApSmartUtil
                long r3 = java.lang.System.currentTimeMillis()
                java.lang.String r2 = r2.getAESKey(r3)
                r0.mAESKey = r2
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.bluetooth.BluetoothGattServer r2 = r0.mGattServer
                r5 = 0
                r6 = 0
                r3 = r29
                r4 = r30
                r7 = r35
                r2.sendResponse(r3, r4, r5, r6, r7)
                goto L_0x089a
            L_0x0353:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r0.mSemWifiApSmartUtil
                java.util.UUID r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.CHARACTERISTIC_ENCRYPTED_AUTH_ID
                java.util.UUID r5 = r31.getUuid()
                boolean r0 = r0.equals(r5)
                if (r0 == 0) goto L_0x0855
                r0 = 0
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r10 = r5.mClientConnections
                java.lang.String r11 = r29.getAddress()
                java.lang.Object r10 = r10.get(r11)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r10 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r10
                int r10 = r10.mVersion
                int unused = r5.mVersion = r10
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r10 = r5.mClientConnections
                java.lang.String r11 = r29.getAddress()
                java.lang.Object r10 = r10.get(r11)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r10 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r10
                int r10 = r10.mUserType
                int unused = r5.mUserType = r10
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer> r5 = r5.mClientConnections
                java.lang.String r10 = r29.getAddress()
                java.lang.Object r5 = r5.get(r10)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$ClientVer r5 = (com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.ClientVer) r5
                java.lang.String r5 = r5.mAESKey
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r10 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r10 = r10.mVersion
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r11 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r11.mSemWifiApSmartUtil
                r11 = 1
                if (r10 != r11) goto L_0x03df
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r10 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r10 = r10.mUserType
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r12 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r12.mSemWifiApSmartUtil
                if (r10 != r11) goto L_0x03df
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                java.lang.String r11 = "Using AES:"
                r10.append(r11)
                r10.append(r5)
                java.lang.String r10 = r10.toString()
                android.util.Log.d(r2, r10)
                java.lang.String r10 = new java.lang.String
                r10.<init>(r8)
                java.lang.String r10 = com.samsung.android.server.wifi.softap.smarttethering.AES.decrypt(r10, r5)
                byte[] r0 = r10.getBytes()
                if (r0 != 0) goto L_0x03e1
                java.lang.String r10 = " decryption can't be null"
                android.util.Log.e(r2, r10)
                goto L_0x03e1
            L_0x03df:
                r0 = r35
            L_0x03e1:
                int r10 = r0.length
                r11 = 1
                r12 = 0
                r14 = 0
                r15 = 0
                r13 = 4
                if (r10 >= r13) goto L_0x03ea
                r11 = 0
            L_0x03ea:
                if (r11 == 0) goto L_0x03f0
                r16 = 1
                byte r15 = r0[r16]
            L_0x03f0:
                if (r11 == 0) goto L_0x03f7
                int r13 = r15 + 3
                if (r10 >= r13) goto L_0x03f7
                r11 = 0
            L_0x03f7:
                if (r11 == 0) goto L_0x03fd
                int r13 = r15 + 2
                byte r12 = r0[r13]
            L_0x03fd:
                if (r11 == 0) goto L_0x0408
                int r13 = r12 + r15
                r16 = 4
                int r13 = r13 + 4
                if (r10 >= r13) goto L_0x0408
                r11 = 0
            L_0x0408:
                if (r11 == 0) goto L_0x0413
                int r13 = r15 + 2
                r16 = 1
                int r13 = r13 + 1
                int r13 = r13 + r12
                byte r14 = r0[r13]
            L_0x0413:
                if (r11 == 0) goto L_0x041f
                int r13 = r12 + r15
                r16 = 4
                int r13 = r13 + 4
                int r13 = r13 + r14
                if (r10 >= r13) goto L_0x041f
                r11 = 0
            L_0x041f:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r13 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.content.Context r13 = r13.mContext
                android.content.ContentResolver r13 = r13.getContentResolver()
                java.lang.String r4 = "wifi_ap_smart_tethering_settings"
                int r4 = android.provider.Settings.Secure.getInt(r13, r4, r7)
                if (r4 != 0) goto L_0x044d
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r13 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.util.LocalLog r13 = r13.mLocalLog
                java.lang.String r6 = "SemWifiApSmartGattServer:\tAutoHotspot switch is OFF, so making auth 0"
                r13.log(r6)
                java.lang.String r6 = "AutoHotspot switch is OFF, so making auth 0"
                android.util.Log.d(r2, r6)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r6 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, java.lang.Integer> r6 = r6.mAuthDevices
                java.lang.String r13 = r29.getAddress()
                r6.put(r13, r9)
                r11 = 0
            L_0x044d:
                java.lang.StringBuilder r6 = new java.lang.StringBuilder
                r6.<init>()
                java.lang.String r13 = "AuthID valid:"
                r6.append(r13)
                r6.append(r11)
                java.lang.String r6 = r6.toString()
                android.util.Log.e(r2, r6)
                int r6 = r29.getBondState()
                if (r6 == r3) goto L_0x048d
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r3 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r3 = r3.mVersion
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r6 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r6.mSemWifiApSmartUtil
                r6 = 1
                if (r3 != r6) goto L_0x0483
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r3 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r3 = r3.mUserType
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r13 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r13.mSemWifiApSmartUtil
                if (r3 != r6) goto L_0x0483
                goto L_0x048d
            L_0x0483:
                r24 = r4
                r25 = r5
                r26 = r10
                r27 = r11
                goto L_0x07f7
            L_0x048d:
                if (r11 == 0) goto L_0x07ef
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r3 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                byte r6 = r0[r7]
                int unused = r3.mUsertype = r6
                r3 = 1
                byte r15 = r0[r3]
                java.lang.String r3 = new java.lang.String
                r6 = 2
                r3.<init>(r0, r6, r15)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r6 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, java.lang.Integer> r6 = r6.mAuthDevices
                java.lang.String r13 = r29.getAddress()
                r6.put(r13, r9)
                int r6 = r15 + 2
                byte r12 = r0[r6]
                java.lang.String r6 = new java.lang.String
                int r13 = r15 + 2
                r7 = 1
                int r13 = r13 + r7
                r6.<init>(r0, r13, r12)
                int r13 = r15 + 2
                int r13 = r13 + r7
                int r13 = r13 + r12
                byte r14 = r0[r13]
                java.lang.String r13 = new java.lang.String
                int r17 = r15 + 2
                int r17 = r17 + 1
                int r17 = r17 + r12
                r24 = r4
                int r4 = r17 + 1
                r13.<init>(r0, r4, r14)
                r4 = r13
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r13 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r13 = r13.mUsertype
                r25 = r5
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r5.mSemWifiApSmartUtil
                if (r13 != r7) goto L_0x0574
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r7 = r5.mSemWifiApSmartUtil
                java.lang.String r7 = r7.getGuid()
                java.lang.String unused = r5.mGuid = r7
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r5 = r5.mGuid
                if (r5 == 0) goto L_0x050a
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, java.lang.Integer> r5 = r5.mAuthDevices
                java.lang.String r7 = r29.getAddress()
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r9 = r9.mGuid
                boolean r9 = r9.equals(r3)
                java.lang.Integer r9 = java.lang.Integer.valueOf(r9)
                r5.put(r7, r9)
            L_0x050a:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.util.LocalLog r5 = r5.mLocalLog
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                java.lang.String r9 = "SemWifiApSmartGattServer:\t same user ,device:"
                r7.append(r9)
                java.lang.String r9 = r29.getAddress()
                r7.append(r9)
                java.lang.String r9 = ",mGuid:"
                r7.append(r9)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r13 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r13 = r13.mGuid
                r7.append(r13)
                java.lang.String r13 = ",Remote mGuid:"
                r7.append(r13)
                r7.append(r3)
                java.lang.String r7 = r7.toString()
                r5.log(r7)
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r7 = "SAME_User device:"
                r5.append(r7)
                java.lang.String r7 = r29.getAddress()
                r5.append(r7)
                r5.append(r9)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r7 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r7 = r7.mGuid
                r5.append(r7)
                java.lang.String r7 = ",Remote mGuid:"
                r5.append(r7)
                r5.append(r3)
                java.lang.String r5 = r5.toString()
                android.util.Log.d(r2, r5)
                r20 = r4
                r19 = r6
                r26 = r10
                r27 = r11
                goto L_0x0782
            L_0x0574:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r5 = r5.mUsertype
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r7 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r7.mSemWifiApSmartUtil
                java.lang.String r7 = ",Remote family id:"
                java.lang.String r13 = ",mFamilyID:"
                r19 = r6
                java.lang.String r6 = "Family:"
                r8 = 2
                if (r5 != r8) goto L_0x0674
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r8 = r5.mSemWifiApSmartUtil
                java.lang.String r8 = r8.getFamilyID()
                java.lang.String unused = r5.mFamilyID = r8
                int r5 = r15 + 2
                r8 = 1
                int r5 = r5 + r8
                int r5 = r5 + r12
                int r5 = r5 + r8
                int r5 = r5 + r14
                byte r5 = r0[r5]
                java.lang.String r8 = new java.lang.String
                int r16 = r15 + 2
                r17 = 1
                int r16 = r16 + 1
                int r16 = r16 + r12
                int r16 = r16 + 1
                int r16 = r16 + r14
                r26 = r10
                int r10 = r16 + 1
                r8.<init>(r0, r10, r5)
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                r27 = r11
                java.lang.String r11 = "mRemoteGuidLength:"
                r10.append(r11)
                r10.append(r8)
                java.lang.String r11 = ","
                r10.append(r11)
                r10.append(r5)
                java.lang.String r10 = r10.toString()
                android.util.Log.i(r2, r10)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r10 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r10 = r10.mFamilyID
                if (r10 == 0) goto L_0x0608
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r10 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, java.lang.Integer> r10 = r10.mAuthDevices
                java.lang.String r11 = r29.getAddress()
                r16 = r5
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r5 = r5.mFamilyID
                boolean r5 = r5.equals(r3)
                if (r5 == 0) goto L_0x05ff
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r5 = r5.mSemWifiApSmartUtil
                boolean r5 = r5.validateGuidInFamilyUsers(r8)
                if (r5 == 0) goto L_0x05ff
                r5 = 1
                goto L_0x0600
            L_0x05ff:
                r5 = 0
            L_0x0600:
                java.lang.Integer r5 = java.lang.Integer.valueOf(r5)
                r10.put(r11, r5)
                goto L_0x060a
            L_0x0608:
                r16 = r5
            L_0x060a:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.content.Context r5 = r5.mContext
                android.content.ContentResolver r5 = r5.getContentResolver()
                java.lang.String r10 = "wifi_ap_smart_tethering_settings_with_family"
                r11 = 0
                int r5 = android.provider.Settings.Secure.getInt(r5, r10, r11)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r10 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.util.LocalLog r10 = r10.mLocalLog
                java.lang.StringBuilder r11 = new java.lang.StringBuilder
                r11.<init>()
                r20 = r8
                java.lang.String r8 = "SemWifiApSmartGattServer:\t same family   device:"
                r11.append(r8)
                java.lang.String r8 = r29.getAddress()
                r11.append(r8)
                r11.append(r6)
                r11.append(r5)
                r11.append(r13)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r6 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r6 = r6.mFamilyID
                r11.append(r6)
                r11.append(r7)
                r11.append(r3)
                java.lang.String r6 = r11.toString()
                r10.log(r6)
                if (r5 != 0) goto L_0x0670
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r6 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.util.LocalLog r6 = r6.mLocalLog
                java.lang.String r7 = "SemWifiApSmartGattServer:\tfamily is not supported, so making auth 0"
                r6.log(r7)
                java.lang.String r6 = "family is not supported, so making auth 0"
                android.util.Log.d(r2, r6)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r6 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, java.lang.Integer> r6 = r6.mAuthDevices
                java.lang.String r7 = r29.getAddress()
                r6.put(r7, r9)
            L_0x0670:
                r20 = r4
                goto L_0x0782
            L_0x0674:
                r26 = r10
                r27 = r11
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r5 = r5.mUsertype
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r8 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r8.mSemWifiApSmartUtil
                r8 = 3
                if (r5 != r8) goto L_0x0780
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r8 = r5.mSemWifiApSmartUtil
                java.lang.String r8 = r8.getFamilyID()
                java.lang.String unused = r5.mFamilyID = r8
                r5 = 0
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r8 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r8 = r8.mFamilyID
                if (r8 == 0) goto L_0x06a6
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r8 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r8 = r8.mFamilyID
                boolean r5 = r8.equals(r3)
            L_0x06a6:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r8 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r8 = r8.mSemWifiApSmartUtil
                java.lang.String r10 = r4.toLowerCase()
                boolean r8 = r8.verifyInSmartApWhiteList(r10)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r10 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r10 = r10.mFamilyID
                if (r10 == 0) goto L_0x06d7
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r10 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, java.lang.Integer> r10 = r10.mAuthDevices
                java.lang.String r11 = r29.getAddress()
                if (r8 == 0) goto L_0x06cb
                if (r5 == 0) goto L_0x06cb
                r16 = 1
                goto L_0x06cd
            L_0x06cb:
                r16 = 0
            L_0x06cd:
                r20 = r4
                java.lang.Integer r4 = java.lang.Integer.valueOf(r16)
                r10.put(r11, r4)
                goto L_0x06d9
            L_0x06d7:
                r20 = r4
            L_0x06d9:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.content.Context r4 = r4.mContext
                android.content.ContentResolver r4 = r4.getContentResolver()
                java.lang.String r10 = "wifi_ap_smart_tethering_settings_with_family"
                r11 = 0
                int r4 = android.provider.Settings.Secure.getInt(r4, r10, r11)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r10 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.util.LocalLog r10 = r10.mLocalLog
                java.lang.StringBuilder r11 = new java.lang.StringBuilder
                r11.<init>()
                r16 = r5
                java.lang.String r5 = "SemWifiApSmartGattServer:\t same allowed user   device:"
                r11.append(r5)
                java.lang.String r5 = r29.getAddress()
                r11.append(r5)
                r11.append(r6)
                r11.append(r4)
                r11.append(r13)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r5 = r5.mFamilyID
                r11.append(r5)
                r11.append(r7)
                r11.append(r3)
                java.lang.String r5 = ",isInWhiteList"
                r11.append(r5)
                r11.append(r8)
                java.lang.String r5 = r11.toString()
                r10.log(r5)
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r10 = " same allowed user   device:"
                r5.append(r10)
                java.lang.String r10 = r29.getAddress()
                r5.append(r10)
                r5.append(r6)
                r5.append(r4)
                r5.append(r13)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r6 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.lang.String r6 = r6.mFamilyID
                r5.append(r6)
                r5.append(r7)
                r5.append(r3)
                java.lang.String r6 = ",isInWhiteList"
                r5.append(r6)
                r5.append(r8)
                java.lang.String r5 = r5.toString()
                android.util.Log.d(r2, r5)
                if (r4 != 0) goto L_0x0782
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.util.LocalLog r5 = r5.mLocalLog
                java.lang.String r6 = "SemWifiApSmartGattServer:\tfamily is not supported, so making auth 0"
                r5.log(r6)
                java.lang.String r5 = "family is not supported, so making auth 0"
                android.util.Log.d(r2, r5)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, java.lang.Integer> r5 = r5.mAuthDevices
                java.lang.String r6 = r29.getAddress()
                r5.put(r6, r9)
                goto L_0x0782
            L_0x0780:
                r20 = r4
            L_0x0782:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r4 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                java.util.HashMap<java.lang.String, java.lang.Integer> r4 = r4.mAuthDevices
                java.lang.String r5 = r29.getAddress()
                java.lang.Object r4 = r4.get(r5)
                java.lang.Integer r4 = (java.lang.Integer) r4
                int r4 = r4.intValue()
                r5 = 1
                if (r4 != r5) goto L_0x07f7
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r5 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.content.Context r5 = r5.mContext
                java.lang.String r6 = "wifi"
                java.lang.Object r5 = r5.getSystemService(r6)
                android.net.wifi.WifiManager r5 = (android.net.wifi.WifiManager) r5
                int r6 = r5.getWifiApState()
                r7 = 0
                r8 = 10
                if (r6 != r8) goto L_0x07b0
                r7 = 400(0x190, float:5.6E-43)
            L_0x07b0:
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                java.lang.String r9 = "Enabling Hotspot state: "
                r8.append(r9)
                r8.append(r6)
                java.lang.String r9 = " interval "
                r8.append(r9)
                r8.append(r7)
                java.lang.String r8 = r8.toString()
                android.util.Log.d(r2, r8)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r8 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil r8 = r8.mSemWifiApSmartUtil
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r9 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                int r9 = r9.mUsertype
                r8.SetUserTypefromGattServer(r9)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r8 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r8 = r8.mBleWorkHandler
                if (r8 == 0) goto L_0x07f7
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r8 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer$BleWorkHandler r8 = r8.mBleWorkHandler
                long r9 = (long) r7
                r11 = 2
                r8.sendEmptyMessageDelayed(r11, r9)
                goto L_0x07f7
            L_0x07ef:
                r24 = r4
                r25 = r5
                r26 = r10
                r27 = r11
            L_0x07f7:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r3 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.bluetooth.BluetoothGattServer r3 = r3.mGattServer
                r21 = 0
                r22 = 0
                r18 = r3
                r19 = r29
                r20 = r30
                r23 = r0
                r18.sendResponse(r19, r20, r21, r22, r23)
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "reveived Auth: "
                r3.append(r4)
                r4 = 0
                byte r5 = r0[r4]
                r3.append(r5)
                java.lang.String r4 = ",device.getBondState():"
                r3.append(r4)
                int r5 = r29.getBondState()
                r3.append(r5)
                java.lang.String r3 = r3.toString()
                android.util.Log.d(r2, r3)
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r2 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.util.LocalLog r2 = r2.mLocalLog
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r5 = "SemWifiApSmartGattServer\treveived Auth: "
                r3.append(r5)
                r5 = 0
                byte r5 = r0[r5]
                r3.append(r5)
                r3.append(r4)
                int r4 = r29.getBondState()
                r3.append(r4)
                java.lang.String r3 = r3.toString()
                r2.log(r3)
                goto L_0x089a
            L_0x0855:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r0.mSemWifiApSmartUtil
                java.util.UUID r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_MHS_ENABLED
                java.util.UUID r2 = r31.getUuid()
                boolean r0 = r0.equals(r2)
                if (r0 == 0) goto L_0x0878
                if (r33 == 0) goto L_0x089a
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.bluetooth.BluetoothGattServer r2 = r0.mGattServer
                r5 = 0
                r6 = 0
                r3 = r29
                r4 = r30
                r7 = r35
                r2.sendResponse(r3, r4, r5, r6, r7)
                goto L_0x089a
            L_0x0878:
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil unused = r0.mSemWifiApSmartUtil
                java.util.UUID r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_ACCEPT_INVITATION
                java.util.UUID r2 = r31.getUuid()
                boolean r0 = r0.equals(r2)
                if (r0 == 0) goto L_0x089a
                if (r33 == 0) goto L_0x089a
                com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer r0 = com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.this
                android.bluetooth.BluetoothGattServer r2 = r0.mGattServer
                r5 = 0
                r6 = 0
                r3 = r29
                r4 = r30
                r7 = r35
                r2.sendResponse(r3, r4, r5, r6, r7)
            L_0x089a:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer.C08131.onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice, int, android.bluetooth.BluetoothGattCharacteristic, boolean, boolean, int, byte[]):void");
        }

        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            Log.d(SemWifiApSmartGattServer.TAG, "Our gatt server onMtuChanged. " + mtu);
            LocalLog access$400 = SemWifiApSmartGattServer.this.mLocalLog;
            access$400.log("SemWifiApSmartGattServer:\tOur gatt server onMtuChanged. " + mtu);
        }
    };
    public BluetoothGattService mGattService = null;
    /* access modifiers changed from: private */
    public String mGuid;
    /* access modifiers changed from: private */
    public boolean mIsNotClientConnected;
    /* access modifiers changed from: private */
    public LocalLog mLocalLog;
    private NotificationManager mNotificationManager;
    /* access modifiers changed from: private */
    public String mPassword;
    /* access modifiers changed from: private */
    public Intent mPenditIntent;
    private String[] mProvisionApp;
    /* access modifiers changed from: private */
    public String mSSID;
    private SemWifiApSmartGattServerBroadcastReceiver mSemWifiApSmartGattServerBroadcastReceiver;
    /* access modifiers changed from: private */
    public SemWifiApSmartUtil mSemWifiApSmartUtil;
    private Set<String> mTempSynchronized = new HashSet();
    private final String mTetheringProvisionApp = SemCscFeature.getInstance().getString(CscFeatureTagSetting.TAG_CSCFEATURE_SETTING_CONFIGMOBILEHOTSPOTPROVISIONAPP);
    /* access modifiers changed from: private */
    public int mUserType;
    /* access modifiers changed from: private */
    public int mUsertype;
    /* access modifiers changed from: private */
    public int mVersion;
    private WifiAwareManager mWifiAwareManager = null;
    private WifiP2pManager mWifiP2pManager = null;

    static {
        mSemWifiApSmartGattServerIntentFilter.addAction("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED");
        mSemWifiApSmartGattServerIntentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        mSemWifiApSmartGattServerIntentFilter.addAction("com.samsung.bluetooth.adapter.action.BLE_STATE_CHANGED");
        mSemWifiApSmartGattServerIntentFilter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
        mSemWifiApSmartGattServerIntentFilter.addAction(ACTION_LOGOUT_ACCOUNTS_COMPLETE);
        mSemWifiApSmartGattServerIntentFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        mSemWifiApSmartGattServerIntentFilter.addAction("com.samsung.android.server.wifi.softap.smarttethering.AcceptPopUp");
        mSemWifiApSmartGattServerIntentFilter.addAction("com.samsung.android.net.wifi.WIFI_DIALOG_CANCEL_ACTION");
        mSemWifiApSmartGattServerIntentFilter.setPriority(ReportIdKey.ID_PATTERN_MATCHED);
    }

    public SemWifiApSmartGattServer(Context context, SemWifiApSmartUtil tSemWifiApSmartUtil, LocalLog obj) {
        this.mContext = context;
        this.mSemWifiApSmartUtil = tSemWifiApSmartUtil;
        this.mSemWifiApSmartGattServerBroadcastReceiver = new SemWifiApSmartGattServerBroadcastReceiver();
        if (!FactoryTest.isFactoryBinary()) {
            this.mContext.registerReceiver(this.mSemWifiApSmartGattServerBroadcastReceiver, mSemWifiApSmartGattServerIntentFilter);
        } else {
            Log.e(TAG, "This devices's binary is a factory binary");
        }
        this.mLocalLog = obj;
    }

    public void handleBootCompleted() {
        this.mBleWorkThread = new HandlerThread("SemWifiApSmartGattServerHandler");
        this.mBleWorkThread.start();
        this.mBleWorkHandler = new BleWorkHandler(this.mBleWorkThread.getLooper());
        String tlist = Settings.Secure.getString(this.mContext.getContentResolver(), "bonded_device_mhsside");
        if (tlist != null) {
            for (String str : tlist.split("\n")) {
                this.bonedDevicesFromHotspotLive.add(str);
            }
        }
    }

    class SemWifiApSmartGattServerBroadcastReceiver extends BroadcastReceiver {
        SemWifiApSmartGattServerBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Intent intent2 = intent;
            String action = intent.getAction();
            if (intent.getAction().equals("com.samsung.android.server.wifi.softap.smarttethering.AcceptPopUp")) {
                if (SemWifiApSmartGattServer.this.isWaitingForAcceptStatus) {
                    Boolean isAccepted = Boolean.valueOf(intent2.getBooleanExtra("accepted", false));
                    Log.d(SemWifiApSmartGattServer.TAG, "Accepted popup:" + isAccepted);
                    if (isAccepted.booleanValue()) {
                        boolean unused = SemWifiApSmartGattServer.this.isWaitingForAcceptStatus = false;
                    }
                    if (SemWifiApSmartGattServer.this.mBleWorkHandler != null) {
                        SemWifiApSmartGattServer.this.mBleWorkHandler.removeMessages(7);
                    }
                    SemWifiApSmartGattServer.this.notifyConnectedDevices(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_ACCEPT_INVITATION);
                }
            } else if (intent.getAction().equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) {
                BluetoothDevice device = (BluetoothDevice) intent2.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                switch (intent2.getIntExtra("android.bluetooth.device.extra.BOND_STATE", 10)) {
                    case 10:
                        if (SemWifiApSmartGattServer.this.mBondingAddress != null && device.getAddress().equals(SemWifiApSmartGattServer.this.mBondingAddress)) {
                            String unused2 = SemWifiApSmartGattServer.this.mBondingAddress = null;
                            LocalLog access$400 = SemWifiApSmartGattServer.this.mLocalLog;
                            access$400.log("SemWifiApSmartGattServer:\tBonding is failed:" + device.getAddress());
                            Log.d(SemWifiApSmartGattServer.TAG, "Bonding is failed");
                            return;
                        }
                        return;
                    case 11:
                        Log.d(SemWifiApSmartGattServer.TAG, "Bonding is going on");
                        LocalLog access$4002 = SemWifiApSmartGattServer.this.mLocalLog;
                        access$4002.log("SemWifiApSmartGattServer:\tBonding is goingon:" + device.getAddress());
                        return;
                    case 12:
                        Log.d(SemWifiApSmartGattServer.TAG, "Bonding is done,mBondingAddress" + SemWifiApSmartGattServer.this.mBondingAddress);
                        Log.d(SemWifiApSmartGattServer.TAG, "Bonding is done,device.getAddress()" + device.getAddress());
                        LocalLog access$4003 = SemWifiApSmartGattServer.this.mLocalLog;
                        access$4003.log("SemWifiApSmartGattServer:\tBonding is done,mBondingAddress" + SemWifiApSmartGattServer.this.mBondingAddress);
                        LocalLog access$4004 = SemWifiApSmartGattServer.this.mLocalLog;
                        access$4004.log("SemWifiApSmartGattServer:\tBonding is done,device.getAddress()" + device.getAddress());
                        return;
                    default:
                        return;
                }
            } else if (action.equals(SemWifiApSmartGattServer.ACTION_LOGOUT_ACCOUNTS_COMPLETE)) {
                int wifiApState = ((WifiManager) SemWifiApSmartGattServer.this.mContext.getSystemService("wifi")).getWifiApState();
                new Message().what = 3;
                Iterator<String> tempIt = SemWifiApSmartGattServer.this.bonedDevicesFromHotspotLive.iterator();
                while (tempIt != null && tempIt.hasNext()) {
                    String tempDevice = tempIt.next();
                    BluetoothDevice device2 = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(tempDevice);
                    Log.d(SemWifiApSmartGattServer.TAG, "delete device " + tempDevice);
                    if (device2 != null && device2.getBondState() == 12) {
                        device2.removeBond();
                        Log.d(SemWifiApSmartGattServer.TAG, ":smarttethering remove device " + tempDevice);
                        LocalLog access$4005 = SemWifiApSmartGattServer.this.mLocalLog;
                        access$4005.log("SemWifiApSmartGattServer:\tsmarttethering remove device " + tempDevice);
                    }
                }
                SemWifiApSmartGattServer.this.bonedDevicesFromHotspotLive.clear();
                Settings.Secure.putString(SemWifiApSmartGattServer.this.mContext.getContentResolver(), "bonded_device_mhsside", (String) null);
            } else if (!action.equals("android.bluetooth.adapter.action.STATE_CHANGED") || !SemWifiApSmartGattServer.this.mBluetoothIsOn) {
                if (action.equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
                    BluetoothDevice device3 = (BluetoothDevice) intent2.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    int mType = intent2.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT", Integer.MIN_VALUE);
                    int mPasskey = intent2.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE);
                    String format = String.format(Locale.US, "%06d", new Object[]{Integer.valueOf(mPasskey)});
                    Log.d(SemWifiApSmartGattServer.TAG, "mType:" + mType + ",device:" + device3 + ",mBondingAddress:" + SemWifiApSmartGattServer.this.mBondingAddress + "isAutoHotspotServerSet :" + SemWifiApSmartGattServer.this.isAutoHotspotServerSet);
                    LocalLog access$4006 = SemWifiApSmartGattServer.this.mLocalLog;
                    access$4006.log("SemWifiApSmartGattServer\tACTION_PAIRING_REQUEST PAIRING Type:" + mType + ",device:" + device3 + ",mBondingAddress:" + SemWifiApSmartGattServer.this.mBondingAddress + "isAutoHotspotServerSet :" + SemWifiApSmartGattServer.this.isAutoHotspotServerSet);
                    if (SemWifiApSmartGattServer.this.isAutoHotspotServerSet && SemWifiApSmartGattServer.this.mBondingAddress != null && device3.getAddress().equals(SemWifiApSmartGattServer.this.mBondingAddress)) {
                        if (mType == 3) {
                            device3.setPairingConfirmation(true);
                            abortBroadcast();
                        } else if (mType == 2) {
                            abortBroadcast();
                            intent2.setClassName("com.android.settings", SemWifiApSmartGattServer.WIFIAP_WARNING_CLASS);
                            intent2.setFlags(268435456);
                            intent2.setAction("com.samsung.android.settings.wifi.mobileap.wifiapwarning");
                            intent2.putExtra("wifiap_warning_dialog_type", 8);
                            if (BluetoothAdapter.getDefaultAdapter() == null || BluetoothAdapter.getDefaultAdapter().getState() != 10) {
                                SemWifiApSmartGattServer.this.mContext.startActivity(intent2);
                            } else {
                                BluetoothAdapter.getDefaultAdapter().enable();
                                boolean unused3 = SemWifiApSmartGattServer.this.mBluetoothIsOn = true;
                                Intent unused4 = SemWifiApSmartGattServer.this.mPenditIntent = intent2;
                            }
                            Log.d(SemWifiApSmartGattServer.TAG, "passkeyconfirm dialog");
                            Intent tintent = new Intent();
                            tintent.setAction("com.samsung.android.server.wifi.softap.smarttethering.collapseQuickPanel");
                            SemWifiApSmartGattServer.this.mContext.sendBroadcast(tintent);
                        }
                    }
                } else if (action.equals("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED")) {
                    int ClientNum = intent2.getIntExtra("NUM", 0);
                    String event = intent2.getStringExtra("EVENT");
                    WifiManager wifiManager = (WifiManager) SemWifiApSmartGattServer.this.mContext.getSystemService("wifi");
                    int state = wifiManager.getWifiApState();
                    Log.d(SemWifiApSmartGattServer.TAG, "event" + event + "Client Num" + ClientNum + "isMhsEnabledsmartly" + SemWifiApSmartGattServer.this.isMHSEnabledSmartly + "state" + state + "mAuthDevices size" + SemWifiApSmartGattServer.this.mAuthDevices.size());
                    if (state == 13 && SemWifiApSmartGattServer.this.isMHSEnabledSmartly && SemWifiApSmartGattServer.this.mIsNotClientConnected) {
                        Log.d(SemWifiApSmartGattServer.TAG, "Client is connected so remove START_HOTSPOT_ENABLED_TIMEOUT_WITHOUT_CLIENT");
                        SemWifiApSmartGattServer.this.mLocalLog.log("SemWifiApSmartGattServer:\tClient is connected so remove START_HOTSPOT_ENABLED_TIMEOUT_WITHOUT_CLIENT");
                        if (SemWifiApSmartGattServer.this.mBleWorkHandler != null) {
                            SemWifiApSmartGattServer.this.mBleWorkHandler.removeMessages(9);
                        }
                        boolean unused5 = SemWifiApSmartGattServer.this.mIsNotClientConnected = false;
                    }
                    if (event != null && event.equals("sta_leave") && ClientNum == 0 && state == 13 && SemWifiApSmartGattServer.this.isMHSEnabledSmartly) {
                        Log.e(SemWifiApSmartGattServer.TAG, "Disabling Smart MHS");
                        SemWifiApSmartGattServer.this.mLocalLog.log("SemWifiApSmartGattServer:\tGattServer sta_leave  ClientNum == 0 stopSoftAp");
                        wifiManager.semSetWifiApEnabled((WifiConfiguration) null, false);
                    }
                } else if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                    int state2 = intent2.getIntExtra("wifi_state", 0);
                    if (state2 == 13) {
                        if (SemWifiApSmartGattServer.this.isWaitingForMHSStatus) {
                            boolean unused6 = SemWifiApSmartGattServer.this.isWaitingForMHSStatus = false;
                            SemWifiApSmartGattServer.this.notifyConnectedDevices(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_MHS_ENABLED);
                            if (SemWifiApSmartGattServer.this.mBleWorkHandler != null) {
                                SemWifiApSmartGattServer.this.mBleWorkHandler.removeMessages(1);
                            }
                        }
                        if (SemWifiApSmartGattServer.this.isMHSEnabledSmartly && SemWifiApSmartGattServer.this.mBleWorkHandler != null) {
                            if (SemWifiApSmartGattServer.this.mBleWorkHandler.hasMessages(9)) {
                                SemWifiApSmartGattServer.this.mBleWorkHandler.removeMessages(9);
                            }
                            SemWifiApSmartGattServer.this.mBleWorkHandler.sendEmptyMessageDelayed(9, 60000);
                            boolean unused7 = SemWifiApSmartGattServer.this.mIsNotClientConnected = true;
                        }
                        boolean unused8 = SemWifiApSmartGattServer.this.isMHSEnabledViaIntent = true;
                        Log.d(SemWifiApSmartGattServer.TAG, "Hotspot Enabled..");
                        SemWifiApSmartGattServer.this.mLocalLog.log("SemWifiApSmartGattServer:\tHotspot Enabled.. ");
                    } else if (state2 == 11 || state2 == 14) {
                        if (SemWifiApSmartGattServer.this.isWaitingForMHSStatus) {
                            boolean unused9 = SemWifiApSmartGattServer.this.isWaitingForMHSStatus = false;
                            if (SemWifiApSmartGattServer.this.mBleWorkHandler != null) {
                                SemWifiApSmartGattServer.this.mBleWorkHandler.removeMessages(1);
                                SemWifiApSmartGattServer.this.mBleWorkHandler.removeMessages(9);
                            }
                            SemWifiApSmartGattServer.this.notifyConnectedDevices(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_MHS_ENABLED);
                            LocalLog access$4007 = SemWifiApSmartGattServer.this.mLocalLog;
                            access$4007.log("SemWifiApSmartGattServer\tHotspot disabled. state " + state2 + " isWaitingForMHSStatus " + SemWifiApSmartGattServer.this.isWaitingForMHSStatus + " isMHSEnabledSmartly " + SemWifiApSmartGattServer.this.isMHSEnabledSmartly);
                        }
                        boolean unused10 = SemWifiApSmartGattServer.this.isMHSEnabledSmartly = false;
                        boolean unused11 = SemWifiApSmartGattServer.this.isMHSEnabledViaIntent = false;
                        boolean unused12 = SemWifiApSmartGattServer.this.mIsNotClientConnected = false;
                    }
                } else if (action.equals("com.samsung.android.net.wifi.WIFI_DIALOG_CANCEL_ACTION") && intent2.getIntExtra("called_dialog", -1) == 2) {
                    Log.d(SemWifiApSmartGattServer.TAG, "Hotspot Enabled cancelled..");
                    SemWifiApSmartGattServer.this.mLocalLog.log("SemWifiApSmartGattServer:\tHotspot Enabled cancelled.. ");
                    if (SemWifiApSmartGattServer.this.isWaitingForMHSStatus) {
                        boolean unused13 = SemWifiApSmartGattServer.this.isWaitingForMHSStatus = false;
                        if (SemWifiApSmartGattServer.this.mBleWorkHandler != null) {
                            SemWifiApSmartGattServer.this.mBleWorkHandler.removeMessages(1);
                            SemWifiApSmartGattServer.this.mBleWorkHandler.removeMessages(9);
                        }
                        SemWifiApSmartGattServer.this.notifyConnectedDevices(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_MHS_ENABLED);
                        LocalLog access$4008 = SemWifiApSmartGattServer.this.mLocalLog;
                        access$4008.log("SemWifiApSmartGattServer\tHotspot enabling cancelled. isWaitingForMHSStatus " + SemWifiApSmartGattServer.this.isWaitingForMHSStatus + " isMHSEnabledSmartly " + SemWifiApSmartGattServer.this.isMHSEnabledSmartly);
                    }
                    boolean unused14 = SemWifiApSmartGattServer.this.isMHSEnabledSmartly = false;
                    boolean unused15 = SemWifiApSmartGattServer.this.isMHSEnabledViaIntent = false;
                    boolean unused16 = SemWifiApSmartGattServer.this.mIsNotClientConnected = false;
                }
            } else if (SemWifiApSmartGattServer.this.mPenditIntent != null && BluetoothAdapter.getDefaultAdapter().getState() == 12) {
                Log.d(SemWifiApSmartGattServer.TAG, "ACTION_STATE_CHANGED passkeyconfirm dialog, mBluetoothIsOn : " + SemWifiApSmartGattServer.this.mBluetoothIsOn);
                LocalLog access$4009 = SemWifiApSmartGattServer.this.mLocalLog;
                access$4009.log("SemWifiApSmartGattServer:\tACTION_STATE_CHANGED passkeyconfirm dialog, mBluetoothIsOn : " + SemWifiApSmartGattServer.this.mBluetoothIsOn);
                SemWifiApSmartGattServer.this.mContext.startActivity(SemWifiApSmartGattServer.this.mPenditIntent);
                boolean unused17 = SemWifiApSmartGattServer.this.mBluetoothIsOn = false;
            }
        }
    }

    class BleWorkHandler extends Handler {
        public BleWorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Log.e(SemWifiApSmartGattServer.TAG, "Got message:" + msg.what);
            switch (msg.what) {
                case 1:
                    Log.i(SemWifiApSmartGattServer.TAG, "Got message START_HOTSPOT_ENABLING_TIMEOUT: isWaitingForMHSStatus " + SemWifiApSmartGattServer.this.isWaitingForMHSStatus);
                    SemWifiApSmartGattServer.this.mLocalLog.log("SemWifiApSmartGattServer:\tGot message START_HOTSPOT_ENABLING_TIMEOUT: isWaitingForMHSStatus " + SemWifiApSmartGattServer.this.isWaitingForMHSStatus);
                    if (SemWifiApSmartGattServer.this.isWaitingForMHSStatus) {
                        boolean unused = SemWifiApSmartGattServer.this.isWaitingForMHSStatus = false;
                        SemWifiApSmartGattServer.this.notifyConnectedDevices(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_MHS_ENABLED);
                        return;
                    }
                    return;
                case 2:
                    Log.i(SemWifiApSmartGattServer.TAG, "Got message COMMAND_ENABLE_HOTSPOT");
                    SemWifiApSmartGattServer.this.mLocalLog.log("SemWifiApSmartGattServer:\tGot message COMMAND_ENABLE_HOTSPOT");
                    WifiManager wifiManager = (WifiManager) SemWifiApSmartGattServer.this.mContext.getSystemService("wifi");
                    int state = wifiManager.getWifiApState();
                    SemWifiApSmartGattServer semWifiApSmartGattServer = SemWifiApSmartGattServer.this;
                    String unused2 = semWifiApSmartGattServer.mSSID = semWifiApSmartGattServer.mSemWifiApSmartUtil.getlegacySSID();
                    SemWifiApSmartGattServer semWifiApSmartGattServer2 = SemWifiApSmartGattServer.this;
                    String unused3 = semWifiApSmartGattServer2.mPassword = semWifiApSmartGattServer2.mSemWifiApSmartUtil.getlegacyPassword();
                    if (state == 13 || state == 12) {
                        SemWifiApSmartGattServer.this.notifyConnectedDevices(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_MHS_ENABLED);
                        SemWifiApSmartGattServer.this.mLocalLog.log("SemWifiApSmartGattServer:\tMHS already Enabled");
                        return;
                    }
                    boolean unused4 = SemWifiApSmartGattServer.this.isWaitingForMHSStatus = true;
                    boolean unused5 = SemWifiApSmartGattServer.this.isMHSEnabledSmartly = true;
                    if (SemWifiApSmartGattServer.this.preProvisioning() || wifiManager.isWifiSharingLiteSupported()) {
                        Intent startDialogIntent = new Intent();
                        startDialogIntent.setClassName("com.android.settings", SemWifiApSmartGattServer.WIFIAP_WARNING_CLASS);
                        startDialogIntent.setFlags(268435456);
                        startDialogIntent.setAction("com.samsung.android.settings.wifi.mobileap.wifiapwarning");
                        startDialogIntent.putExtra("wifiap_warning_dialog_type", 5);
                        SemWifiApSmartGattServer.this.mContext.startActivity(startDialogIntent);
                        SemWifiApSmartGattServer.this.mLocalLog.log("SemWifiApSmartGattServer:\tenableHotspot start wifiapwarning SoftAp state :" + state + ",mSSID:" + SemWifiApSmartGattServer.this.mSSID);
                    } else {
                        wifiManager.semSetWifiApEnabled((WifiConfiguration) null, true);
                        SemWifiApSmartGattServer.this.mLocalLog.log("SemWifiApSmartGattServer\tenableHotspot startSoftAp state :" + state + ",mSSID:" + SemWifiApSmartGattServer.this.mSSID);
                    }
                    if (SemWifiApSmartGattServer.this.mBleWorkHandler != null) {
                        SemWifiApSmartGattServer.this.mBleWorkHandler.sendEmptyMessageDelayed(1, 60000);
                        return;
                    }
                    return;
                case 4:
                    Toast.makeText(SemWifiApSmartGattServer.this.mContext, 17042594, 1).show();
                    return;
                case 5:
                    Toast.makeText(SemWifiApSmartGattServer.this.mContext, 17042598, 1).show();
                    return;
                case 6:
                    String device = (String) msg.obj;
                    if (device != null && SemWifiApSmartGattServer.this.bonedDevicesFromHotspotLive.add(device)) {
                        String tpString = "";
                        Iterator it = SemWifiApSmartGattServer.this.bonedDevicesFromHotspotLive.iterator();
                        while (it.hasNext()) {
                            tpString = tpString + ((String) it.next()) + "\n";
                        }
                        Settings.Secure.putString(SemWifiApSmartGattServer.this.mContext.getContentResolver(), "bonded_device_mhsside", tpString);
                        Log.i(SemWifiApSmartGattServer.TAG, "Adding to bondedd devices:" + device);
                        SemWifiApSmartGattServer.this.mLocalLog.log("SemWifiApSmartGattServer:\tAdding to bondedd devices :" + tpString);
                        return;
                    }
                    return;
                case 7:
                    if (SemWifiApSmartGattServer.this.isWaitingForAcceptStatus) {
                        SemWifiApSmartGattServer.this.notifyConnectedDevices(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_ACCEPT_INVITATION);
                        return;
                    }
                    return;
                case 8:
                    SemWifiApSmartGattServer.this.notifyConnectedDevices(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_ACCEPT_INVITATION);
                    return;
                case 9:
                    WifiManager mWifiManager = (WifiManager) SemWifiApSmartGattServer.this.mContext.getSystemService("wifi");
                    int mState = mWifiManager.getWifiApState();
                    Log.d(SemWifiApSmartGattServer.TAG, "isMhsEnabledsmartly" + SemWifiApSmartGattServer.this.isMHSEnabledSmartly + " mState" + mState);
                    if (mState == 13 && SemWifiApSmartGattServer.this.isMHSEnabledSmartly) {
                        Log.e(SemWifiApSmartGattServer.TAG, "Disabling Smart MHS");
                        SemWifiApSmartGattServer.this.mLocalLog.log("SemWifiApSmartGattServer:\tGattServer START_HOTSPOT_ENABLED_TIMEOUT_WITHOUT_CLIENT stopSoftAp");
                        mWifiManager.semSetWifiApEnabled((WifiConfiguration) null, false);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public boolean isMHSEnabledSmart() {
        return this.isMHSEnabledSmartly;
    }

    public boolean setGattServer() {
        synchronized (this.mTempSynchronized) {
            if (this.mGattServer != null) {
                return true;
            }
            Log.d(TAG, "mGattServer is null");
            this.mBluetoothManager = (BluetoothManager) this.mContext.getSystemService("bluetooth");
            this.mGattServer = this.mBluetoothManager.openGattServer(this.mContext, this.mGattServerCallback, 2);
            if (this.mGattServer != null) {
                Log.d(TAG, "calling initGattServer");
                this.mLocalLog.log("SemWifiApSmartGattServer:\tcalling initGattServer");
                boolean initGattServer = initGattServer();
                return initGattServer;
            }
            Log.d(TAG, "failed to set GattServer in  initGattServer");
            this.mLocalLog.log("SemWifiApSmartGattServer:\tfailed to set GattServer in  initGattServer");
            return false;
        }
    }

    public void removeGattServer() {
        synchronized (this.mTempSynchronized) {
            Log.d(TAG, "trying to close mGattServer and remove mGattService");
            this.mLocalLog.log("SemWifiApSmartGattServer:\ttrying to close mGattServer and remove mGattService");
            if (this.mGattServer != null) {
                if (this.mGattService != null) {
                    boolean ret = this.mGattServer.removeService(this.mGattService);
                    this.isAutoHotspotServerSet = false;
                    Log.d(TAG, "remove mGattService:" + ret);
                    LocalLog localLog = this.mLocalLog;
                    localLog.log("SemWifiApSmartGattServer:\tmGattService removed:" + ret);
                }
                this.mGattServer.close();
                Log.d(TAG, "close mGattServer:");
                this.mLocalLog.log("SemWifiApSmartGattServer:\tmGattServer closed:");
                this.mGattServer = null;
            }
        }
    }

    private boolean initGattServer() {
        if (this.mGattService == null) {
            Log.d(TAG, "Creating autoHotspot GattService");
            this.mLocalLog.log("SemWifiApSmartGattServer:\tCreating autoHotspot GattService");
            this.mGattService = new BluetoothGattService(SemWifiApSmartUtil.SERVICE_UUID, 0);
            BluetoothGattCharacteristic mhs_auth_status = new BluetoothGattCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_AUTH_STATUS, 2, 1);
            BluetoothGattCharacteristic mhs_status_characteristic = new BluetoothGattCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_STATUS_UUID, 2, 1);
            BluetoothGattCharacteristic mhs_bond_status = new BluetoothGattCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_BOND_STATUS, 2, 1);
            BluetoothGattCharacteristic auth_encrypted_status = new BluetoothGattCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_ENCRYPTED_AUTH_ID, 10, 17);
            BluetoothGattCharacteristic mhs_ver_update = new BluetoothGattCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_VER_UPDATE, 10, 17);
            BluetoothGattCharacteristic mhs_side_get_time = new BluetoothGattCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_MHS_SIDE_GET_TIME, 2, 1);
            BluetoothGattCharacteristic mNotifyMHSStatus = new BluetoothGattCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_MHS_ENABLED, 26, 17);
            this.mGattService.addCharacteristic(mhs_auth_status);
            this.mGattService.addCharacteristic(mhs_status_characteristic);
            this.mGattService.addCharacteristic(auth_encrypted_status);
            this.mGattService.addCharacteristic(mhs_bond_status);
            this.mGattService.addCharacteristic(mNotifyMHSStatus);
            this.mGattService.addCharacteristic(mhs_ver_update);
            this.mGattService.addCharacteristic(mhs_side_get_time);
            BluetoothGattCharacteristic read_client_devicename = new BluetoothGattCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_CLIENT_MAC, 2, 1);
            BluetoothGattCharacteristic read_client_bond_status = new BluetoothGattCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_D2D_CLIENT_BOND_STATUS, 2, 1);
            BluetoothGattCharacteristic send_family_id = new BluetoothGattCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_FAMILY_ID, 10, 17);
            this.mGattService.addCharacteristic(read_client_devicename);
            this.mGattService.addCharacteristic(send_family_id);
            this.mGattService.addCharacteristic(read_client_bond_status);
            this.mGattService.addCharacteristic(new BluetoothGattCharacteristic(SemWifiApSmartUtil.CHARACTERISTIC_NOTIFY_ACCEPT_INVITATION, 26, 17));
        }
        BluetoothGattServer bluetoothGattServer = this.mGattServer;
        if (bluetoothGattServer != null) {
            boolean ret = bluetoothGattServer.addService(this.mGattService);
            if (ret) {
                this.isAutoHotspotServerSet = true;
                this.mLocalLog.log("SemWifiApSmartGattServer:\tGattServer Added Custom Server to GattServer");
                Log.d(TAG, "Added Custom Server to GattServer");
            } else {
                this.mLocalLog.log("SemWifiApSmartGattServer:\t failed to add GattServer Custom Server to GattServer");
                Log.d(TAG, "failed to add Custom Server to GattServer");
            }
            return ret;
        }
        this.mLocalLog.log("SemWifiApSmartGattServer:\tmGattServer is null in initGattServer");
        Log.d(TAG, "GattServer is null in initGattServer");
        return false;
    }

    /* access modifiers changed from: private */
    public void notifyConnectedDevices(UUID mUUID) {
        for (Map.Entry m : this.mAuthDevices.entrySet()) {
            BluetoothGattServer bluetoothGattServer = this.mGattServer;
            if (!(bluetoothGattServer == null || bluetoothGattServer.getService(SemWifiApSmartUtil.SERVICE_UUID) == null)) {
                BluetoothGattCharacteristic readCharacteristic = this.mGattServer.getService(SemWifiApSmartUtil.SERVICE_UUID).getCharacteristic(mUUID);
                Random rand = new Random();
                readCharacteristic.setValue(new byte[]{(byte) rand.nextInt(10), (byte) rand.nextInt(10)});
                Log.d(TAG, "notifyConnectedDevices");
                this.mLocalLog.log("SemWifiApSmartGattServer:\tnotifyConnectedDevices");
                this.mGattServer.notifyCharacteristicChanged(BluetoothAdapter.getDefaultAdapter().getRemoteDevice((String) m.getKey()), readCharacteristic, false);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isProvisioningNeeded() {
        String[] strArr;
        if (!isProvisioningCheck()) {
            return false;
        }
        this.mProvisionApp = this.mContext.getResources().getStringArray(17236154);
        if ((("ATT".equals(CONFIGOPBRANDINGFORMOBILEAP) || "VZW".equals(CONFIGOPBRANDINGFORMOBILEAP) || "TMO".equals(CONFIGOPBRANDINGFORMOBILEAP) || "NEWCO".equals(CONFIGOPBRANDINGFORMOBILEAP)) && (SystemProperties.getBoolean("net.tethering.noprovisioning", false) || (strArr = this.mProvisionApp) == null || strArr.length != 2)) || TextUtils.isEmpty(this.mTetheringProvisionApp) || this.mProvisionApp.length != 2) {
            return false;
        }
        return true;
    }

    private boolean isProvisioningCheck() {
        if (SystemProperties.get("Provisioning.disable").equals("1")) {
            return false;
        }
        return true;
    }

    private boolean isP2pEnabled() {
        this.mWifiP2pManager = (WifiP2pManager) this.mContext.getSystemService("wifip2p");
        WifiP2pManager wifiP2pManager = this.mWifiP2pManager;
        if (wifiP2pManager == null) {
            return false;
        }
        return wifiP2pManager.isWifiP2pEnabled();
    }

    private boolean isP2pConnected() {
        if (this.mWifiP2pManager == null) {
            this.mWifiP2pManager = (WifiP2pManager) this.mContext.getSystemService("wifip2p");
        }
        WifiP2pManager wifiP2pManager = this.mWifiP2pManager;
        if (wifiP2pManager == null) {
            Log.i(TAG, "isP2pConnected() : mWifiP2pManager is null");
            return false;
        }
        boolean ret = wifiP2pManager.isWifiP2pConnected();
        Log.i(TAG, "isP2pConnected() : " + ret);
        return ret;
    }

    private boolean isNanEnabled() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
            this.mWifiAwareManager = (WifiAwareManager) this.mContext.getSystemService("wifiaware");
        }
        WifiAwareManager wifiAwareManager = this.mWifiAwareManager;
        if (wifiAwareManager == null) {
            return false;
        }
        return wifiAwareManager.isEnabled();
    }

    private boolean isWirelessDexEnabled() {
        SemDeviceInfo info = ((DisplayManager) this.mContext.getSystemService("display")).semGetActiveDevice();
        if (info != null && info.isWirelessDexMode()) {
            return true;
        }
        Bundle extras = new Bundle(2);
        extras.putString("key", "wireless_dex_scan_device");
        extras.putString("def", "false");
        String ret = "false";
        try {
            Bundle result = this.mContext.getContentResolver().call(Uri.parse("content://com.sec.android.desktopmode.uiservice.SettingsProvider/settings"), "getSettings", (String) null, extras);
            if (result != null) {
                ret = result.getString("wireless_dex_scan_device");
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to get settings", e);
        }
        return ret.equals("true");
    }

    /* access modifiers changed from: private */
    public boolean preProvisioning() {
        if (this.isJDMDevice) {
            Log.i(TAG, " JDM device");
            return true;
        } else if (SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_ENABLEWARNINGPOPUP4DATABATTERYUSAGE)) {
            Log.i(TAG, " Low battery: failed");
            return true;
        } else if (Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_display_on", 0) == 1) {
            Log.i(TAG, " SMARTVIEW_DISABLE: failed");
            this.mLocalLog.log("SemWifiApSmartGattServer:\tSMARTVIEW_DISABLE: failed");
            return true;
        } else if (isP2pConnected()) {
            Log.i(TAG, " isP2pConnected: failed");
            this.mLocalLog.log("SemWifiApSmartGattServer:\tisP2pConnected: failed");
            return true;
        } else if (isNanEnabled()) {
            Log.i(TAG, " isNanEnabled: failed");
            this.mLocalLog.log("SemWifiApSmartGattServer:\tisNanEnabled: failed");
            return true;
        } else if (isWirelessDexEnabled()) {
            Log.i(TAG, " WirelessDex: failed");
            this.mLocalLog.log("SemWifiApSmartGattServer:\t WirelessDex: failed");
            return true;
        } else if (isProvisioningNeeded()) {
            Log.i(TAG, " ProvisioningNeeded ");
            this.mLocalLog.log("SemWifiApSmartGattServer:\tProvisioningNeeded ");
            return true;
        } else {
            WifiManager wm = (WifiManager) this.mContext.getSystemService("wifi");
            if (!wm.isWifiSharingSupported() || wm.isWifiSharingLiteSupported() || Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_first_time_wifi_sharing_dialog", 0) != 0) {
                TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
                boolean isRoaming = tm.isNetworkRoaming();
                String iso = tm.getNetworkCountryIso();
                if (!"VZW".equals(CONFIGOPBRANDINGFORMOBILEAP) || wm.getWifiApState() != 11 || !isRoaming || "us".equals(iso)) {
                    return false;
                }
                Log.i(TAG, "vzw roaming popup");
                this.mLocalLog.log("SemWifiApSmartGattServer:\tvzw roaming popup");
                return true;
            }
            Log.i(TAG, " show wifisharing fist popup");
            this.mLocalLog.log("SemWifiApSmartGattServer:\tshow wifisharing fist popup");
            return true;
        }
    }

    public void factoryReset() {
        Log.d(TAG, "network reset settings ");
        long ident = Binder.clearCallingIdentity();
        try {
            SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_d2d_Wifimac", (String) null);
            this.mSemWifiApSmartUtil.putD2DFamilyID((String) null);
            this.mSemWifiApSmartUtil.putHashbasedonD2DFamilyid(-1);
            Intent tintent = new Intent();
            tintent.setAction("com.samsung.android.server.wifi.softap.smarttethering.d2dfamilyid");
            this.mContext.sendBroadcast(tintent);
            SemWifiApSmartWhiteList.getInstance().resetWhitelist();
            Iterator<String> tempIt = this.bonedDevicesFromHotspotLive.iterator();
            if (BluetoothAdapter.getDefaultAdapter() != null) {
                while (tempIt != null && tempIt.hasNext()) {
                    String tempDevice = tempIt.next();
                    BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(tempDevice);
                    Log.d(TAG, "delete device " + tempDevice);
                    if (device != null && device.getBondState() == 12) {
                        device.removeBond();
                        Log.d(TAG, ":smarttethering remove device " + tempDevice);
                        LocalLog localLog = this.mLocalLog;
                        localLog.log("SemWifiApSmartGattServer:\tsmarttethering remove device " + tempDevice);
                    }
                }
                this.bonedDevicesFromHotspotLive.clear();
                Settings.Secure.putString(this.mContext.getContentResolver(), "bonded_device_mhsside", (String) null);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private static class ClientVer {
        public String mAESKey = "";
        public int mUserType = -1;
        public int mVersion = 0;

        public String toString() {
            return String.format("mVersion:" + this.mVersion + ",mUserType:" + this.mUserType + ",mAESKey:" + this.mAESKey, new Object[0]);
        }
    }
}
