package com.samsung.android.server.wifi.share;

import android.content.Context;
import android.util.Log;
import com.samsung.android.mcf.McfAdapter;
import com.samsung.android.server.wifi.share.McfDataUtil;
import java.util.List;

class McfController {
    private static final int SUCCESS = 0;
    private static final String TAG = "WifiProfileShare.MCF";
    private final Context mContext;
    /* access modifiers changed from: private */
    public IMcfServiceState mListener;
    /* access modifiers changed from: private */
    public McfAdapter mMcfAdapter = null;
    private McfAdapter.McfAdapterListener mMcfAdapterListener = new McfAdapter.McfAdapterListener() {
        public void onServiceConnected(McfAdapter mcfAdapter) {
            Log.d(McfController.TAG, "onServiceConnected");
            McfController.this.mSemCasterManager.closeCaster(McfController.this.mMcfAdapter);
            McfController.this.mSemSubscriberManager.closeSubscriber(McfController.this.mMcfAdapter);
            McfAdapter unused = McfController.this.mMcfAdapter = mcfAdapter;
            McfController.this.mSemCasterManager.openCaster(McfController.this.mMcfAdapter);
            McfController.this.mSemSubscriberManager.openSubscriber(McfController.this.mMcfAdapter);
            if (McfController.this.mListener != null) {
                McfController.this.mListener.onServiceConnected();
            }
        }

        public void onServiceDisconnected() {
            Log.d(McfController.TAG, "onServiceDisconnected");
            McfController.this.mSemCasterManager.closeCaster(McfController.this.mMcfAdapter);
            McfController.this.mSemSubscriberManager.closeSubscriber(McfController.this.mMcfAdapter);
            McfAdapter unused = McfController.this.mMcfAdapter = null;
        }

        public void onServiceRemoteException() {
            Log.d(McfController.TAG, "onServiceRemoteException  mcf server is null");
        }
    };
    /* access modifiers changed from: private */
    public final SemCasterManager mSemCasterManager;
    /* access modifiers changed from: private */
    public final SemSubscriberManager mSemSubscriberManager;

    enum AdvertiseState {
        NONE,
        DEVICE_DETECTED,
        AUTHENTICATION,
        GATT_CONNECTED,
        REQUEST,
        ACCEPT,
        REJECT,
        CLOSE
    }

    McfController(Context context) {
        this.mContext = context;
        this.mSemCasterManager = new SemCasterManager();
        this.mSemSubscriberManager = new SemSubscriberManager();
    }

    /* access modifiers changed from: package-private */
    public boolean isServiceBound() {
        return this.mMcfAdapter != null;
    }

    /* access modifiers changed from: package-private */
    public boolean bindMcfService(IMcfServiceState listener) {
        this.mListener = listener;
        if (isServiceBound()) {
            return true;
        }
        try {
            boolean result = McfAdapter.bindService(this.mContext, this.mMcfAdapterListener);
            if (!result) {
                Log.e(TAG, "failed to bind mcf service");
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public void unbindMcfService() {
        if (isServiceBound()) {
            this.mMcfAdapter.unbindService();
            this.mMcfAdapter = null;
        }
    }

    /* access modifiers changed from: package-private */
    public void stopAllCasterMode() {
        this.mSemCasterManager.closeCaster();
    }

    /* access modifiers changed from: package-private */
    public boolean startCaster(McfDataUtil.McfData qosData, List<McfDataUtil.McfData> pwdData, ICasterCallback callback) {
        if (!this.mSemCasterManager.isOpened() || !this.mSemCasterManager.isNetworkEnable()) {
            Log.e(TAG, "failed to start caster, not opened");
            return false;
        }
        if (isValidQoSData(qosData)) {
            this.mSemCasterManager.startScanForQoS(qosData);
        }
        if (pwdData == null || pwdData.size() == 0) {
            return true;
        }
        this.mSemCasterManager.startScanForPassword(pwdData, callback);
        return true;
    }

    /* access modifiers changed from: package-private */
    public void checkAndUpdatePasswordData(List<McfDataUtil.McfData> pwdData, ICasterCallback callback) {
        if (!this.mSemCasterManager.isOpened() || !this.mSemCasterManager.isNetworkEnable()) {
            Log.e(TAG, "failed to update password data, not opened");
        } else if (pwdData == null || pwdData.size() == 0) {
            if (this.mSemCasterManager.isEnabledSharingPassword()) {
                this.mSemCasterManager.stopScanForPassword();
            }
        } else if (this.mSemCasterManager.isEnabledSharingPassword()) {
            this.mSemCasterManager.updatePasswordDate(pwdData);
        } else {
            this.mSemCasterManager.startScanForPassword(pwdData, callback);
        }
    }

    private boolean isValidQoSData(McfDataUtil.McfData qosData) {
        if (qosData == null || qosData.getByteArrayForSharing()[3] == 0) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public void checkAndUpdateQoSData(McfDataUtil.McfData qosData) {
        int success = -1;
        if (this.mSemCasterManager.isOpened() && this.mSemCasterManager.isNetworkEnable()) {
            if (isValidQoSData(qosData)) {
                if (this.mSemCasterManager.isEnabledQoSSharing()) {
                    success = this.mSemCasterManager.updateQoSData(qosData.getByteArrayForSharing());
                } else {
                    this.mSemCasterManager.startScanForQoS(qosData);
                    success = 0;
                }
            } else if (this.mSemCasterManager.isEnabledQoSSharing()) {
                this.mSemCasterManager.stopScanForQoS();
            }
        }
        if (success == 0) {
            Log.d(TAG, "success update QosData !");
        } else {
            Log.d(TAG, "Fail update QosData");
        }
    }

    /* access modifiers changed from: package-private */
    public void setUserConfirm(boolean isAccept, String userData) {
        this.mSemCasterManager.sendPasswordData(isAccept, userData);
    }

    /* access modifiers changed from: package-private */
    public void clearUserRequestPasswordHistory() {
        Log.v(TAG, "clearUserConfirmHistory ");
        this.mSemCasterManager.clearUserRequestPasswordHistory();
    }

    /* access modifiers changed from: package-private */
    public boolean startSubscriberForQoS(ISubscriberCallback callback) {
        if (!this.mSemSubscriberManager.isOpened() || !this.mSemSubscriberManager.isNetworkEnable()) {
            Log.e(TAG, "startSubscriberForQoS failed");
            return false;
        }
        Log.d(TAG, "startSubscriberForQoS");
        this.mSemSubscriberManager.startScanForQoS(callback);
        return true;
    }

    /* access modifiers changed from: package-private */
    public void stopSubscriberModeForQoS() {
        this.mSemSubscriberManager.closeSubscriber();
    }

    /* access modifiers changed from: package-private */
    public boolean startSubscriberForPassword(McfDataUtil.McfData pwdReqInfo, ISubscriberCallback callback) {
        if (!this.mSemSubscriberManager.isOpened() || !this.mSemSubscriberManager.isNetworkEnable()) {
            Log.e(TAG, "startSubscriberForPassword failed");
            return false;
        }
        this.mSemSubscriberManager.preStartScanForPassword(pwdReqInfo, callback);
        return true;
    }

    /* access modifiers changed from: package-private */
    public void startSubscriberForPassword() {
        if (!this.mSemSubscriberManager.isOpened() || !this.mSemSubscriberManager.isNetworkEnable()) {
            Log.e(TAG, "failed to start subscriber for password");
        } else {
            this.mSemSubscriberManager.postStartScanForPassword(true);
        }
    }

    /* access modifiers changed from: package-private */
    public void stopSubscriberModeForPassword() {
        if (!this.mSemSubscriberManager.isOpened() || !this.mSemSubscriberManager.isNetworkEnable()) {
            Log.d(TAG, "skip to stop subscriber for password, not opened");
        } else {
            this.mSemSubscriberManager.closeSubscriber();
        }
    }

    /* access modifiers changed from: package-private */
    public void stopPostAdvertise() {
        if (!this.mSemSubscriberManager.isOpened() || !this.mSemSubscriberManager.isNetworkEnable()) {
            Log.v(TAG, "skip to stop post advertise packet, not opened");
        } else {
            this.mSemSubscriberManager.stopPostAdvertise();
        }
    }

    /* access modifiers changed from: package-private */
    public void setScanMode(boolean isLowLatencyForCasterPassword, boolean isLowLatencyForSubscriberQos, boolean isLowLatencyForSubscriberPassword) {
        Log.d(TAG, "setScanModeToLowLatency isLowLatencyForCasterPassword : " + isLowLatencyForCasterPassword + ", subsQosLatency : " + isLowLatencyForSubscriberQos + ", subsPassLatency : " + isLowLatencyForSubscriberPassword);
        this.mSemCasterManager.setScanMode(isLowLatencyForCasterPassword);
        this.mSemSubscriberManager.setScanMode(isLowLatencyForSubscriberQos, isLowLatencyForSubscriberPassword);
    }
}
