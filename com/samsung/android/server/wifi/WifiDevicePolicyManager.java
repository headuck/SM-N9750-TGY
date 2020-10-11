package com.samsung.android.server.wifi;

import android.app.ActivityManagerNative;
import android.app.admin.IDevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.IWifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.server.enterprise.EnterpriseService;
import com.android.server.enterprise.wifi.WifiPolicy;
import com.android.server.wifi.ClientModeImpl;
import com.samsung.android.knox.ContextInfo;

public class WifiDevicePolicyManager {
    private static final String TAG = "WifiDevicePolicyManager";
    private static WifiDevicePolicyManager sInstance;
    private final ClientModeImpl mClientModeImpl;
    private ConnectivityManager mCm = null;
    private final Context mContext;
    private final IDevicePolicyManager mDPM;
    private final BroadcastReceiver mDPMReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Slog.i(WifiDevicePolicyManager.TAG, "onReceive " + action);
            boolean wifiPolicyChanged = intent.getBooleanExtra("isWifiChanged", false);
            boolean tUserSwitch = action.equals("android.intent.action.USER_SWITCHED");
            Slog.i(WifiDevicePolicyManager.TAG, "Receive IP Policy Intent - policy changed : " + wifiPolicyChanged + " User Switch : " + tUserSwitch);
            if (wifiPolicyChanged || tUserSwitch) {
                WifiDevicePolicyManager.this.handleSecurityPolicy();
            }
            WifiDevicePolicyManager.this.handleSecurityPolicyMHS();
        }
    };
    private int mIsWifiOnly = -1;
    private WifiPolicy mWifiPolicy = null;
    private final IWifiManager.Stub mWifiService;

    public static synchronized WifiDevicePolicyManager init(Context context, IWifiManager.Stub service, ClientModeImpl clientModeImpl) {
        WifiDevicePolicyManager wifiDevicePolicyManager;
        synchronized (WifiDevicePolicyManager.class) {
            if (sInstance == null) {
                sInstance = new WifiDevicePolicyManager(context, service, clientModeImpl);
            }
            wifiDevicePolicyManager = sInstance;
        }
        return wifiDevicePolicyManager;
    }

    private WifiDevicePolicyManager(Context context, IWifiManager.Stub service, ClientModeImpl clientModeImpl) {
        this.mContext = context;
        this.mWifiService = service;
        this.mClientModeImpl = clientModeImpl;
        this.mDPM = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        registerReceiver();
    }

    private void checkAndSetConnectivityInstance() {
        if (this.mCm == null) {
            this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    public boolean isAllowToUseWifi() {
        if (isAllowToUseWifiDpm()) {
            return true;
        }
        showInfoMessage(2);
        return false;
    }

    private boolean isAllowToUseWifiDpm() {
        boolean allowWifi = false;
        int userId = 0;
        try {
            userId = ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed getting userId using ActivityManagerNative", e);
        } catch (SecurityException e2) {
            Log.w(TAG, "Failed getting userId using ActivityManagerNative", e2);
        }
        try {
            allowWifi = this.mDPM.semGetAllowWifi((ComponentName) null, userId);
        } catch (Exception e3) {
            Log.w(TAG, "Failed getting Wi-Fi policy from DEVICE_POLICY_SERVICE");
        }
        if (allowWifi) {
            return true;
        }
        Log.i(TAG, "Not allow to use Wi-Fi (DPM)");
        return false;
    }

    public boolean isAllowToUseHotspot() {
        if (isWifiOnly()) {
            Slog.e(TAG, "Do not accept turn on Wifi hotspot in Wifi model");
            return false;
        } else if (!isAllowInternetSharingDpm()) {
            showInfoMessage(3);
            return false;
        } else if (!isAllowToUseHotspot3lm()) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isAllowInternetSharingDpm() {
        boolean allowAp = false;
        int userId = 0;
        try {
            userId = ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed getting userId using ActivityManagerNative", e);
        } catch (SecurityException e2) {
            Log.w(TAG, "Failed getting userId using ActivityManagerNative", e2);
        }
        try {
            allowAp = this.mDPM.semGetAllowInternetSharing((ComponentName) null, userId);
        } catch (Exception e3) {
            Log.w(TAG, "Failed getting Hotspot policy from DEVICE_POLICY_SERVICE");
        }
        if (allowAp) {
            return true;
        }
        Log.i(TAG, "Not allow to use Hotspot (DPM)");
        return false;
    }

    public boolean isWifiOnly() {
        if (this.mIsWifiOnly == -1) {
            checkAndSetConnectivityInstance();
            if (this.mCm.isNetworkSupported(0)) {
                this.mIsWifiOnly = 0;
            } else {
                this.mIsWifiOnly = 1;
            }
        }
        if (this.mIsWifiOnly == 1) {
            return true;
        }
        return false;
    }

    private boolean isAllowToUseHotspot3lm() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "tethering_blocked", 0) != 1;
    }

    private void showInfoMessage(int type) {
        this.mClientModeImpl.showToastMsg(type, (String) null);
    }

    /* access modifiers changed from: private */
    public void handleSecurityPolicy() {
        boolean allowWifi = isAllowToUseWifi();
        Slog.i(TAG, "handleSecurityPolicy()     allowWifi (" + allowWifi + ")");
        if (!allowWifi) {
            try {
                if (this.mWifiService.getWifiEnabledState() != 1) {
                    this.mWifiService.setWifiEnabled(this.mContext.getOpPackageName(), allowWifi);
                }
            } catch (RemoteException e) {
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleSecurityPolicyMHS() {
        boolean allowWifiAp = isAllowInternetSharingDpm();
        Slog.i(TAG, "handleSecurityPolicyMHS()   allowWifiAp (" + allowWifiAp + ")");
        if (!allowWifiAp) {
            try {
                if (this.mWifiService.getWifiApEnabledState() == 13) {
                    this.mWifiService.stopSoftAp();
                    showInfoMessage(3);
                }
            } catch (RemoteException e) {
            }
        }
    }

    public boolean isOpenWifiApAllowed() {
        this.mWifiPolicy = (WifiPolicy) EnterpriseService.getPolicyService("wifi_policy");
        WifiPolicy wifiPolicy = this.mWifiPolicy;
        if (wifiPolicy == null || wifiPolicy.isOpenWifiApAllowed(new ContextInfo(Binder.getCallingUid()))) {
            return true;
        }
        showInfoMessage(10);
        return false;
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiverAsUser(this.mDPMReceiver, UserHandle.ALL, filter, (String) null, (Handler) null);
    }
}
