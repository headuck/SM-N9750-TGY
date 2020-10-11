package com.samsung.android.server.wifi.share;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.samsung.android.mcf.McfAdapter;
import com.samsung.android.mcf.McfDevice;
import com.samsung.android.mcf.McfSubscriber;
import com.samsung.android.mcf.SubscribeCallback;
import com.samsung.android.mcf.discovery.KeepDeviceCallback;
import com.samsung.android.mcf.discovery.McfAdvertiseCallback;
import com.samsung.android.mcf.discovery.McfAdvertiseData;
import com.samsung.android.mcf.discovery.McfDeviceDiscoverCallback;
import com.samsung.android.mcf.discovery.McfScanData;
import com.samsung.android.server.wifi.share.McfController;
import com.samsung.android.server.wifi.share.McfDataUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

class SemSubscriberManager {
    private static final String DEVICE_NAME_ME = "-ME---";
    private static final int MAX_KEEP_GATT_CONNECTION_SIZE = 3;
    private static final int MODE_PASSWORD = 1;
    private static final int MODE_QOS = 0;
    private static final String TAG = "WifiProfileShare.McfSub";
    private static final boolean mFlagShowDataLog = true;
    /* access modifiers changed from: private */
    public ISubscriberCallback mCallback;
    private int mCountPostAdvertise = 0;
    private final Object mHistoryLock = new Object();
    /* access modifiers changed from: private */
    public boolean mIsNetworkEnabled;
    /* access modifiers changed from: private */
    public boolean mIsPasswordAdvertiseTriggered;
    private boolean mIsPasswordLowLatency;
    /* access modifiers changed from: private */
    public boolean mIsPostAdvertiseStarted;
    private boolean mIsQosLowLatency;
    private boolean mIsScanTriggered;
    /* access modifiers changed from: private */
    public final Map<String, KeepGattDeviceInfo> mKeepGattDeviceList = new HashMap();
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    /* access modifiers changed from: private */
    public McfSubscriber mMcfSubscriber;
    private int mMode = 0;
    private McfAdvertiseCallback mPassMcfAdvertiseCallback = new McfAdvertiseCallback() {
        public void onAdvertiseStarted(int i) {
            Log.d(SemSubscriberManager.TAG, "-ME--- mPassMcfAdvertiseCallback, onAdvertiseStarted ");
        }

        public void onAdvertiseStopped(int i) {
            Log.d(SemSubscriberManager.TAG, "-ME--- onPassAdvertiseStopped");
        }
    };
    private final List<String> mPasswordConfirmHistory = new ArrayList();
    /* access modifiers changed from: private */
    public McfDataUtil.McfData mPasswordData;
    private McfDeviceDiscoverCallback mPasswordDiscoverCallback = new McfDeviceDiscoverCallback() {
        public void onDeviceDiscovered(McfDevice mcfDevice, int i) {
            if (mcfDevice == null) {
                Log.e(SemSubscriberManager.TAG, "-ME--- onDeviceDiscovered(pass), mcfDevice is null");
                return;
            }
            String deviceId = mcfDevice.getDeviceID();
            JSONObject jsonObject = mcfDevice.getContentsJson();
            if (jsonObject == null) {
                Log.e(SemSubscriberManager.TAG, deviceId + " json is null");
                return;
            }
            McfController.AdvertiseState state = McfController.AdvertiseState.NONE;
            if (jsonObject.has("state")) {
                try {
                    state = McfController.AdvertiseState.valueOf((String) jsonObject.get("state"));
                } catch (JSONException e) {
                    Log.e(SemSubscriberManager.TAG, "-ME--- can not get state");
                }
            }
            Log.d(SemSubscriberManager.TAG, deviceId + " received(pass) message state: " + state.name() + ", postAdv:" + SemSubscriberManager.this.mIsPostAdvertiseStarted);
            int i2 = C07747.f59x6b45a697[state.ordinal()];
            boolean z = true;
            if (i2 == 1) {
                Log.i(SemSubscriberManager.TAG, deviceId + " delivered password data");
                McfDevice unused = SemSubscriberManager.this.mReceivedPasswordMcfDevice = mcfDevice;
                SemSubscriberManager.this.callbackToClient(mcfDevice);
                SemSubscriberManager.this.stopScan();
                SemSubscriberManager.this.addConfirmHistory(deviceId, jsonObject);
            } else if (i2 == 2) {
                Log.i(SemSubscriberManager.TAG, deviceId + " rejected");
                SemSubscriberManager.this.removeKeepDevice(mcfDevice);
                if (!SemSubscriberManager.this.mIsPasswordAdvertiseTriggered) {
                    SemSubscriberManager semSubscriberManager = SemSubscriberManager.this;
                    semSubscriberManager.startAdvertise(semSubscriberManager.mPasswordData);
                }
                SemSubscriberManager.this.addConfirmHistory(deviceId, jsonObject);
            } else if (i2 != 3) {
                Log.e(SemSubscriberManager.TAG, deviceId + " unhandled state: " + state.name());
            } else if (!jsonObject.has("configKey")) {
            } else {
                if (mcfDevice.isInContact() != 1) {
                    Log.d(SemSubscriberManager.TAG, deviceId + " not exist in my contact list");
                } else if (SemSubscriberManager.this.isAlreadyConfirmed(deviceId, jsonObject)) {
                    Log.d(SemSubscriberManager.TAG, deviceId + " already confirmed before");
                } else {
                    ArrayList<String> configKeyFromCaster = new ArrayList<>();
                    try {
                        configKeyFromCaster.add(jsonObject.get("configKey").toString());
                        if (jsonObject.has(McfDataUtil.McfData.JSON_CONFIGKEY_HOTSPOT)) {
                            configKeyFromCaster.add(jsonObject.get(McfDataUtil.McfData.JSON_CONFIGKEY_HOTSPOT).toString());
                        }
                    } catch (JSONException e2) {
                        Log.e(SemSubscriberManager.TAG, deviceId + " json:configKey parsing error");
                    }
                    if (configKeyFromCaster.isEmpty() || SemSubscriberManager.this.mPasswordData == null) {
                        Log.e(SemSubscriberManager.TAG, deviceId + " config key is null");
                        return;
                    }
                    int configCheckCount = 0;
                    Iterator<String> it = configKeyFromCaster.iterator();
                    while (it.hasNext()) {
                        if (TextUtils.equals(it.next(), SemSubscriberManager.this.mPasswordData.getConfigKey())) {
                            configCheckCount++;
                        }
                    }
                    if (configCheckCount == 0) {
                        Log.d(SemSubscriberManager.TAG, deviceId + " not matched configKey req:" + configKeyFromCaster);
                        return;
                    }
                    Log.i(SemSubscriberManager.TAG, deviceId + " hello my friend! authentication completed.authType:" + mcfDevice.getAdditionalAuthType());
                    synchronized (SemSubscriberManager.this.mLock) {
                        if (SemSubscriberManager.this.mKeepGattDeviceList.size() >= 3) {
                            if (!SemSubscriberManager.this.mKeepGattDeviceList.containsKey(deviceId)) {
                                Log.d(SemSubscriberManager.TAG, deviceId + " skip to add, keep device list is full");
                                if (SemSubscriberManager.this.mIsPasswordAdvertiseTriggered && SemSubscriberManager.this.mKeepGattDeviceList.size() >= 3) {
                                    SemSubscriberManager.this.stopAdvertise();
                                }
                            }
                        }
                        KeepDeviceCallback callback = new KeepDeviceCallback() {
                            public void onKeepDeviceStateCallback(McfDevice mcfDevice, int status) {
                                C07726.super.onKeepDeviceStateCallback(mcfDevice, status);
                                if (mcfDevice == null) {
                                    Log.e(SemSubscriberManager.TAG, "-ME--- onKeepDeviceStateCallback, mcf device is null");
                                    return;
                                }
                                Log.d(SemSubscriberManager.TAG, mcfDevice.getDeviceID() + " onKeepDeviceStateCallback  status: " + status);
                                if (status == 1) {
                                    SemSubscriberManager.this.removeKeepDevice(mcfDevice);
                                }
                            }
                        };
                        SemSubscriberManager.this.mKeepGattDeviceList.put(deviceId, new KeepGattDeviceInfo(mcfDevice, callback));
                        SemSubscriberManager.this.mMcfSubscriber.keepDiscoveredDevice(mcfDevice, true, callback);
                        Log.v(SemSubscriberManager.TAG, deviceId + " add to keep device list, size: " + SemSubscriberManager.this.mKeepGattDeviceList.size());
                        if (SemSubscriberManager.this.mCallback != null) {
                            ISubscriberCallback access$1500 = SemSubscriberManager.this.mCallback;
                            if (SemSubscriberManager.this.mKeepGattDeviceList.size() <= 0) {
                                z = false;
                            }
                            access$1500.onFoundDevicesForPassword(z);
                        }
                        if (SemSubscriberManager.this.mIsPostAdvertiseStarted) {
                            Log.d(SemSubscriberManager.TAG, deviceId + " request password");
                            SemSubscriberManager.this.postStartScanForPassword(false);
                        }
                        SemSubscriberManager.this.stopAdvertise();
                    }
                }
            }
        }

        public void onDeviceRemoved(McfDevice mcfDevice, int i) {
            if (mcfDevice == null) {
                Log.e(SemSubscriberManager.TAG, "-ME--- mPasswordDiscoverCallback::onDeviceRemoved mcf device is null");
                return;
            }
            String deviceId = mcfDevice.getDeviceID();
            Log.d(SemSubscriberManager.TAG, deviceId + " onDeviceRemoved");
            SemSubscriberManager.this.removeKeepDevice(mcfDevice);
        }
    };
    private McfAdvertiseCallback mQoSMcfAdvertiseCallback = new McfAdvertiseCallback() {
        public void onAdvertiseStarted(int i) {
            Log.d(SemSubscriberManager.TAG, "-ME--- mQosMcfAdvertiseCallback, onAdvertiseStarted");
        }

        public void onAdvertiseStopped(int i) {
            Log.d(SemSubscriberManager.TAG, "-ME--- onQosAdvertiseStopped ");
        }
    };
    private McfDeviceDiscoverCallback mQoSMcfDeviceDiscoverCallback = new McfDeviceDiscoverCallback() {
        public void onDeviceDiscovered(McfDevice mcfDevice, int i) {
            if (mcfDevice == null) {
                Log.e(SemSubscriberManager.TAG, "-ME--- onDeviceDiscovered, mcfDevice is null");
                return;
            }
            Log.v(SemSubscriberManager.TAG, mcfDevice.getDeviceID() + " delivered qos data");
            SemSubscriberManager.this.callbackToClient(mcfDevice);
        }

        public void onDeviceRemoved(McfDevice mcfDevice, int i) {
            if (mcfDevice == null) {
                Log.e(SemSubscriberManager.TAG, "-ME--- onDeviceRemoved, mcfDevice is null");
                return;
            }
            Log.v(SemSubscriberManager.TAG, mcfDevice.getDeviceID() + " onDeviceRemoved");
        }
    };
    /* access modifiers changed from: private */
    public McfDevice mReceivedPasswordMcfDevice;
    private SubscribeCallback mSubscribeCallback = new SubscribeCallback() {
        public void onMcfServiceStateChanged(int status, int i1) {
            SemSubscriberManager.super.onMcfServiceStateChanged(status, i1);
            if (SemSubscriberManager.this.mMcfSubscriber != null && 2 == status && 1 == i1) {
                SemSubscriberManager semSubscriberManager = SemSubscriberManager.this;
                boolean unused = semSubscriberManager.mIsNetworkEnabled = semSubscriberManager.mMcfSubscriber.isNetworkEnabled(1);
            }
            Log.i(SemSubscriberManager.TAG, "-ME--- onMcfServiceStateChanged, status : " + status + " network:" + SemSubscriberManager.this.mIsNetworkEnabled);
        }
    };

    SemSubscriberManager() {
        Log.d(TAG, "-ME--- SemSubscriberManager !");
    }

    /* access modifiers changed from: package-private */
    public void openSubscriber(McfAdapter adapter) {
        if (adapter == null) {
            Log.e(TAG, "-ME--- openSubscriber, adapter is null");
            return;
        }
        try {
            this.mMcfSubscriber = adapter.getSubscriber(4, this.mSubscribeCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (this.mMcfSubscriber == null) {
            Log.e(TAG, "-ME--- openSubscriber failed");
        }
        Log.d(TAG, "-ME--- openSubscriber");
    }

    /* access modifiers changed from: package-private */
    public void closeSubscriber(McfAdapter adapter) {
        if (this.mMcfSubscriber == null) {
            Log.d(TAG, "-ME--- closeSubscriber, already closed");
            return;
        }
        if (this.mIsNetworkEnabled) {
            this.mIsNetworkEnabled = false;
        }
        if (adapter != null) {
            try {
                adapter.closeSubscriber(4);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        this.mMcfSubscriber = null;
    }

    /* access modifiers changed from: package-private */
    public void closeSubscriber() {
        Log.d(TAG, "-ME--- closeSubscriber start");
        if (this.mIsPasswordAdvertiseTriggered) {
            stopAdvertise();
        }
        if (this.mIsScanTriggered) {
            stopScan();
        }
        if (this.mIsPostAdvertiseStarted) {
            this.mIsPostAdvertiseStarted = false;
            stopPostAdvertise();
        }
        if (isPasswordMode()) {
            if (this.mKeepGattDeviceList.isEmpty()) {
                startAdvertise(McfDataUtil.getMcfDataForCancelingPassword(""));
            } else {
                closePasswordSessionToKeepDevices();
            }
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                Log.e(TAG, "-ME--- interrupt");
            }
            if (this.mKeepGattDeviceList.isEmpty()) {
                stopAdvertise();
            } else {
                clearKeepDeviceList();
            }
        }
        this.mCountPostAdvertise = 0;
        Log.d(TAG, "-ME--- closeSubscriber done");
    }

    /* access modifiers changed from: package-private */
    public boolean isOpened() {
        return this.mMcfSubscriber != null;
    }

    /* access modifiers changed from: package-private */
    public boolean isNetworkEnable() {
        McfSubscriber mcfSubscriber = this.mMcfSubscriber;
        if (mcfSubscriber != null) {
            this.mIsNetworkEnabled = mcfSubscriber.isNetworkEnabled(1);
        }
        return this.mIsNetworkEnabled;
    }

    private void setMode(int mode, ISubscriberCallback callback) {
        this.mMode = mode;
        this.mCallback = callback;
    }

    private boolean isPasswordMode() {
        return this.mMode == 1;
    }

    private boolean isQoSMode() {
        return this.mMode == 0;
    }

    private String getModeString() {
        if (isQoSMode()) {
            return "QoS Mode";
        }
        return "Password Mode";
    }

    /* access modifiers changed from: package-private */
    public void startScanForQoS(ISubscriberCallback callback) {
        setMode(0, callback);
        startScan();
        startAdvertise((McfDataUtil.McfData) null);
    }

    /* access modifiers changed from: package-private */
    public void preStartScanForPassword(McfDataUtil.McfData reqPwdInfo, ISubscriberCallback callback) {
        this.mPasswordData = reqPwdInfo;
        setMode(1, callback);
        startScan();
        startAdvertise(reqPwdInfo);
        Log.d(TAG, "-ME--- preStartScanForPassword data:" + reqPwdInfo);
    }

    /* access modifiers changed from: package-private */
    public void postStartScanForPassword(boolean initialize) {
        Log.d(TAG, "-ME--- postStartScanForPassword init:" + initialize + " cnt:" + this.mCountPostAdvertise);
        this.mIsPostAdvertiseStarted = true;
        if (!initialize || this.mCountPostAdvertise <= 0) {
            this.mCountPostAdvertise++;
            if (!this.mKeepGattDeviceList.isEmpty()) {
                synchronized (this.mLock) {
                    Log.v(TAG, "-ME--- postStartScanForPassword keep gatt device:" + this.mKeepGattDeviceList.size());
                    for (KeepGattDeviceInfo deviceCallback : this.mKeepGattDeviceList.values()) {
                        startAdvertiseTo(deviceCallback, this.mPasswordData, McfController.AdvertiseState.REQUEST);
                    }
                }
            } else {
                Log.d(TAG, "-ME--- There are no keep device in list, wait to response");
            }
            this.mReceivedPasswordMcfDevice = null;
            return;
        }
        clearKeepDeviceList();
        preStartScanForPassword(this.mPasswordData, this.mCallback);
    }

    /* access modifiers changed from: package-private */
    public void stopPostAdvertise() {
        Log.d(TAG, "-ME--- stopPostAdvertise");
        this.mIsPostAdvertiseStarted = false;
        synchronized (this.mLock) {
            for (KeepGattDeviceInfo deviceCallback : this.mKeepGattDeviceList.values()) {
                if (deviceCallback.getAdvertiseStarted()) {
                    Log.d(TAG, deviceCallback.getMcfDevice().getDeviceID() + " stop advertise");
                    this.mMcfSubscriber.stopAdvertise(deviceCallback.getMcfAdvertiseCallback());
                    deviceCallback.setAdvertiseStarted(false);
                }
            }
        }
    }

    private void closePasswordSessionToKeepDevices() {
        synchronized (this.mLock) {
            for (KeepGattDeviceInfo deviceCallback : this.mKeepGattDeviceList.values()) {
                if (this.mReceivedPasswordMcfDevice == null || !deviceCallback.getMcfDevice().toString().equals(this.mReceivedPasswordMcfDevice.toString())) {
                    startAdvertiseTo(deviceCallback, this.mPasswordData, McfController.AdvertiseState.CLOSE);
                } else {
                    Log.d(TAG, deviceCallback.getMcfDevice().toString() + " pass me password data ! skip close start advertise");
                }
            }
        }
    }

    private void clearKeepDeviceList() {
        Log.d(TAG, "-ME--- clearKeepDeviceList");
        synchronized (this.mLock) {
            for (KeepGattDeviceInfo deviceCallback : this.mKeepGattDeviceList.values()) {
                McfDevice mcfDevice = deviceCallback.getMcfDevice();
                Log.v(TAG, mcfDevice.getDeviceID() + " clear keep");
                this.mMcfSubscriber.keepDiscoveredDevice(mcfDevice, false, deviceCallback.getKeepDeviceCallback());
                if (deviceCallback.getAdvertiseStarted()) {
                    this.mMcfSubscriber.stopAdvertise(deviceCallback.getMcfAdvertiseCallback());
                }
            }
            this.mKeepGattDeviceList.clear();
        }
    }

    /* access modifiers changed from: package-private */
    public void setScanMode(boolean qosLowLatency, boolean passwordLowLatency) {
        this.mIsQosLowLatency = qosLowLatency;
        this.mIsPasswordLowLatency = passwordLowLatency;
    }

    private void startScan() {
        String str;
        if (!isOpened()) {
            Log.e(TAG, "-ME--- startScan failed, subscriber is not opened");
            return;
        }
        if (this.mIsScanTriggered) {
            Log.d(TAG, "-ME--- startScan already triggered.");
            stopScan();
        }
        McfScanData.Builder builder = new McfScanData.Builder().setScanData(4, isPasswordMode(), isQoSMode());
        if (isQoSMode()) {
            builder.setContentsFilter(1);
        }
        if ((isQoSMode() && this.mIsQosLowLatency) || (isPasswordMode() && this.mIsPasswordLowLatency)) {
            builder.setScanMode(3);
        }
        this.mIsScanTriggered = true;
        this.mMcfSubscriber.startScan(builder.build(), getDeviceDiscoverCallback());
        StringBuilder sb = new StringBuilder();
        sb.append("-ME--- start Scan mode:");
        sb.append(getModeString());
        if (this.mMode == 0) {
            str = " extend, lowLatency: " + this.mIsQosLowLatency;
        } else {
            str = " general, lowLatency: " + this.mIsPasswordLowLatency;
        }
        sb.append(str);
        Log.d(TAG, sb.toString());
    }

    /* access modifiers changed from: private */
    public void stopScan() {
        this.mIsScanTriggered = false;
        if (this.mMcfSubscriber != null) {
            Log.d(TAG, "-ME--- stopScan");
            this.mMcfSubscriber.stopScan(getDeviceDiscoverCallback());
        }
    }

    /* access modifiers changed from: private */
    public void startAdvertise(McfDataUtil.McfData reqInfo) {
        if (!isOpened()) {
            Log.e(TAG, "-ME--- startAdvertise failed, subscriber is not opened");
            return;
        }
        if (this.mIsPasswordAdvertiseTriggered) {
            stopAdvertise();
        }
        McfAdvertiseData.Builder builder = new McfAdvertiseData.Builder().setAdvertiseData(4, isPasswordMode(), isQoSMode());
        if (reqInfo != null) {
            builder.setByteContent(reqInfo.getByteArrayForSharing());
        }
        Log.d(TAG, "-ME--- startAdvertise mode:" + getModeString() + " all");
        this.mMcfSubscriber.startAdvertise(builder.build(), getAdvertiseCallback());
        this.mIsPasswordAdvertiseTriggered = true;
    }

    private void startAdvertiseTo(KeepGattDeviceInfo deviceCallback, McfDataUtil.McfData data, McfController.AdvertiseState state) {
        if (!isOpened()) {
            Log.e(TAG, "-ME--- startAdvertiseTo failed, subscriber is not opened");
            return;
        }
        if (deviceCallback.getAdvertiseStarted()) {
            this.mMcfSubscriber.stopAdvertise(deviceCallback.getMcfAdvertiseCallback());
        }
        McfDevice targetDevice = deviceCallback.getMcfDevice();
        McfAdvertiseCallback emptyCallback = new McfAdvertiseCallback() {
        };
        deviceCallback.setAdvertiseCallback(emptyCallback);
        McfAdvertiseData advData = new McfAdvertiseData.Builder().setAdvertiseData(4, isPasswordMode(), isQoSMode()).setByteContent(data.getByteArrayForSharing()).setJsonContent(data.getPasswordJsonData(state.name(), false)).setTargetDevice(targetDevice).build();
        Log.d(TAG, targetDevice.getDeviceID() + " sending message state:" + state.name() + " data:" + data);
        this.mMcfSubscriber.startAdvertise(advData, emptyCallback);
        deviceCallback.setAdvertiseStarted(true);
    }

    /* access modifiers changed from: private */
    public void stopAdvertise() {
        this.mIsPasswordAdvertiseTriggered = false;
        if (isOpened()) {
            Log.d(TAG, "-ME--- stopAdvertise for " + getModeString());
            this.mMcfSubscriber.stopAdvertise(getAdvertiseCallback());
        }
    }

    private McfAdvertiseCallback getAdvertiseCallback() {
        if (isQoSMode()) {
            return this.mQoSMcfAdvertiseCallback;
        }
        return this.mPassMcfAdvertiseCallback;
    }

    /* access modifiers changed from: private */
    public void callbackToClient(McfDevice mcfDevice) {
        ISubscriberCallback iSubscriberCallback;
        ISubscriberCallback iSubscriberCallback2;
        String deviceId = mcfDevice.getDeviceID();
        StringBuilder sb = new StringBuilder();
        sb.append(deviceId);
        sb.append(" callbackToClient, ");
        sb.append(isQoSMode() ? "qos" : McfDataUtil.McfData.JSON_PASSWORD);
        sb.append(" data");
        Log.d(TAG, sb.toString());
        if (isQoSMode()) {
            McfDataUtil.McfData qosData = McfDataUtil.getMcfDataForQoS(mcfDevice.getContentsByte());
            if (qosData == null || (iSubscriberCallback2 = this.mCallback) == null) {
                Log.e(TAG, deviceId + " delivered qos data is null");
                return;
            }
            iSubscriberCallback2.onQoSDataDelivered(qosData);
            return;
        }
        McfDataUtil.McfData mMcfData = McfDataUtil.getMcfData(mcfDevice.getContentsJson());
        if (mMcfData == null || (iSubscriberCallback = this.mCallback) == null) {
            Log.e(TAG, deviceId + " delivered password data is null");
            return;
        }
        iSubscriberCallback.onPasswordDelivered(mMcfData);
    }

    private McfDeviceDiscoverCallback getDeviceDiscoverCallback() {
        if (isQoSMode()) {
            return this.mQoSMcfDeviceDiscoverCallback;
        }
        return this.mPasswordDiscoverCallback;
    }

    /* access modifiers changed from: private */
    public void removeKeepDevice(McfDevice mcfDevice) {
        synchronized (this.mLock) {
            Iterator<KeepGattDeviceInfo> iterator = this.mKeepGattDeviceList.values().iterator();
            while (true) {
                if (!iterator.hasNext()) {
                    break;
                }
                KeepGattDeviceInfo keptDevice = iterator.next();
                if (keptDevice.mcfDevice.equals(mcfDevice)) {
                    Log.i(TAG, mcfDevice.getDeviceID() + " removed from keep list");
                    iterator.remove();
                    this.mMcfSubscriber.keepDiscoveredDevice(mcfDevice, false, keptDevice.getKeepDeviceCallback());
                    break;
                }
            }
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 5 */
    /* access modifiers changed from: private */
    public void addConfirmHistory(String deviceId, JSONObject jsonObject) {
        if (jsonObject.has(McfDataUtil.McfData.JSON_START_AT)) {
            try {
                long startAt = jsonObject.getLong(McfDataUtil.McfData.JSON_START_AT);
                if (startAt != 0) {
                    synchronized (this.mHistoryLock) {
                        List<String> list = this.mPasswordConfirmHistory;
                        list.add(deviceId + startAt);
                        if (this.mPasswordConfirmHistory.size() > 20) {
                            this.mPasswordConfirmHistory.remove(0);
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "-ME--- can not get json:startAt");
            }
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 5 */
    /* access modifiers changed from: private */
    public boolean isAlreadyConfirmed(String deviceId, JSONObject jsonObject) {
        if (!jsonObject.has(McfDataUtil.McfData.JSON_START_AT)) {
            return false;
        }
        try {
            long startAt = jsonObject.getLong(McfDataUtil.McfData.JSON_START_AT);
            synchronized (this.mHistoryLock) {
                List<String> list = this.mPasswordConfirmHistory;
                if (list.contains(deviceId + startAt)) {
                    return true;
                }
                return false;
            }
        } catch (JSONException e) {
            Log.e(TAG, deviceId + " json:startAt parsing error");
            return false;
        }
    }

    /* renamed from: com.samsung.android.server.wifi.share.SemSubscriberManager$7 */
    static /* synthetic */ class C07747 {

        /* renamed from: $SwitchMap$com$samsung$android$server$wifi$share$McfController$AdvertiseState */
        static final /* synthetic */ int[] f59x6b45a697 = new int[McfController.AdvertiseState.values().length];

        static {
            try {
                f59x6b45a697[McfController.AdvertiseState.ACCEPT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                f59x6b45a697[McfController.AdvertiseState.REJECT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                f59x6b45a697[McfController.AdvertiseState.AUTHENTICATION.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private static class KeepGattDeviceInfo {
        private boolean advertiseStarted;
        private KeepDeviceCallback keepDeviceCallback;
        private McfAdvertiseCallback mcfAdvertiseCallback;
        /* access modifiers changed from: private */
        public McfDevice mcfDevice;

        KeepGattDeviceInfo(McfDevice mcfDevice2, KeepDeviceCallback keepDeviceCallback2) {
            this.mcfDevice = mcfDevice2;
            this.keepDeviceCallback = keepDeviceCallback2;
        }

        /* access modifiers changed from: package-private */
        public void setAdvertiseCallback(McfAdvertiseCallback mcfAdvertiseCallback2) {
            this.mcfAdvertiseCallback = mcfAdvertiseCallback2;
        }

        /* access modifiers changed from: package-private */
        public McfDevice getMcfDevice() {
            return this.mcfDevice;
        }

        /* access modifiers changed from: package-private */
        public KeepDeviceCallback getKeepDeviceCallback() {
            return this.keepDeviceCallback;
        }

        /* access modifiers changed from: package-private */
        public McfAdvertiseCallback getMcfAdvertiseCallback() {
            return this.mcfAdvertiseCallback;
        }

        /* access modifiers changed from: package-private */
        public boolean getAdvertiseStarted() {
            return this.advertiseStarted;
        }

        /* access modifiers changed from: package-private */
        public void setAdvertiseStarted(boolean start) {
            this.advertiseStarted = start;
        }
    }
}
