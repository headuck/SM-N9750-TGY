package com.samsung.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkStats;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Debug;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import com.android.server.wifi.WifiInjector;
import java.math.BigDecimal;

public class HotspotMobileDataLimit {
    /* access modifiers changed from: private */
    public static final boolean DBG = Debug.semIsProductDev();
    private static final String TAG = "HotspotMobileDataLimit";
    private static final int WIFI_AP_DATA_CHECKING_MS = 1000;
    private ConnectivityManager connectivity;
    private String iface;
    /* access modifiers changed from: private */
    public boolean isAgain;
    /* access modifiers changed from: private */
    public boolean isDataConnected;
    /* access modifiers changed from: private */
    public boolean isReached;
    /* access modifiers changed from: private */
    public boolean isStarted;
    /* access modifiers changed from: private */
    public boolean isWifiApEnabled;
    /* access modifiers changed from: private */
    public CountApData mApData;
    private ContentObserver mApLimitObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            HotspotMobileDataLimit hotspotMobileDataLimit = HotspotMobileDataLimit.this;
            boolean z = false;
            if (Settings.Secure.getInt(hotspotMobileDataLimit.mContext.getContentResolver(), "wifi_ap_mobile_data_limit", 0) == 1) {
                z = true;
            }
            boolean unused = hotspotMobileDataLimit.mDataLimited = z;
            Log.d(HotspotMobileDataLimit.TAG, "Mobile AP data limit change to : " + HotspotMobileDataLimit.this.mDataLimited);
        }
    };
    private ContentObserver mApLimitValueObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            String limit = Settings.Secure.getString(HotspotMobileDataLimit.this.mContext.getContentResolver(), "wifi_ap_mobile_data_limit_value");
            Log.d(HotspotMobileDataLimit.TAG, "APMobileDataLimitValue onChange, new limit : " + limit);
            if (limit != null) {
                BigDecimal unused = HotspotMobileDataLimit.this.mLimitData = new BigDecimal(limit);
            }
        }
    };
    /* access modifiers changed from: private */
    public long mBaseRxBytes;
    /* access modifiers changed from: private */
    public long mBaseTxBytes;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public boolean mDataLimited;
    /* access modifiers changed from: private */
    public BigDecimal mLimitData;
    private ContentObserver mMobileDataObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            boolean DataNetworkEnable = Settings.Global.getInt(HotspotMobileDataLimit.this.mContext.getContentResolver(), "mobile_data", 0) == 1;
            if (HotspotMobileDataLimit.DBG) {
                Log.d(HotspotMobileDataLimit.TAG, "DataNetworkEnable : " + DataNetworkEnable);
            }
            if (true == DataNetworkEnable) {
                boolean unused = HotspotMobileDataLimit.this.isDataConnected = true;
                if (HotspotMobileDataLimit.this.isStarted) {
                    boolean unused2 = HotspotMobileDataLimit.this.isAgain = true;
                    HotspotMobileDataLimit hotspotMobileDataLimit = HotspotMobileDataLimit.this;
                    String unused3 = hotspotMobileDataLimit.mResumeData = Settings.Secure.getString(hotspotMobileDataLimit.mContext.getContentResolver(), "wifi_ap_mobile_data_usage_value");
                    HotspotMobileDataLimit.this.getTetheringBytes();
                    Log.d(HotspotMobileDataLimit.TAG, "Data change to ON, get current TxBytes : " + HotspotMobileDataLimit.this.mBaseTxBytes + ", RxBytes : " + HotspotMobileDataLimit.this.mBaseRxBytes);
                    if (!HotspotMobileDataLimit.this.mApData.isRunning()) {
                        HotspotMobileDataLimit.this.mApData.resume();
                        return;
                    }
                    return;
                }
                return;
            }
            boolean unused4 = HotspotMobileDataLimit.this.isDataConnected = false;
            if (HotspotMobileDataLimit.this.mApData.isRunning()) {
                HotspotMobileDataLimit.this.mApData.pause();
            }
        }
    };
    /* access modifiers changed from: private */
    public INetworkManagementService mNM;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            HotspotMobileDataLimit.this.handleEvent(context, intent);
        }
    };
    /* access modifiers changed from: private */
    public String mResumeData;
    private WifiManager mWifiManager;
    private ContentObserver mWifiSharingObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            boolean isWifiSharing = Settings.Secure.getInt(HotspotMobileDataLimit.this.mContext.getContentResolver(), "wifi_ap_wifi_sharing", 0) == 1;
            if (HotspotMobileDataLimit.DBG) {
                Log.d(HotspotMobileDataLimit.TAG, "isWifiSharing : " + isWifiSharing + ", isWifiApEnabled = " + HotspotMobileDataLimit.this.isWifiApEnabled + ", saveWifiStatus = " + HotspotMobileDataLimit.this.saveWifiStatus);
            }
            if (!isWifiSharing && HotspotMobileDataLimit.this.isWifiApEnabled) {
                HotspotMobileDataLimit hotspotMobileDataLimit = HotspotMobileDataLimit.this;
                String unused = hotspotMobileDataLimit.mResumeData = Settings.Secure.getString(hotspotMobileDataLimit.mContext.getContentResolver(), "wifi_ap_mobile_data_usage_value");
                if (HotspotMobileDataLimit.this.saveWifiStatus) {
                    HotspotMobileDataLimit.this.getTetheringBytes();
                    boolean unused2 = HotspotMobileDataLimit.this.saveWifiStatus = false;
                    boolean unused3 = HotspotMobileDataLimit.this.saveMobileDate = true;
                }
                if (!HotspotMobileDataLimit.this.mApData.isRunning()) {
                    HotspotMobileDataLimit.this.mApData.resume();
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean saveMobileDate;
    /* access modifiers changed from: private */
    public boolean saveWifiStatus;

    public HotspotMobileDataLimit(Context context) {
        this.mContext = context;
        if (WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing()) {
            this.iface = "swlan0";
        } else {
            this.iface = "wlan0";
        }
        this.isReached = false;
        this.isStarted = false;
        this.isAgain = false;
        this.isWifiApEnabled = false;
        this.saveWifiStatus = false;
        this.saveMobileDate = false;
        this.mResumeData = null;
        this.mApData = new CountApData();
        this.mDataLimited = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_mobile_data_limit", 0) != 1 ? false : true;
        String limit = Settings.Secure.getString(this.mContext.getContentResolver(), "wifi_ap_mobile_data_limit_value");
        if (limit != null) {
            this.mLimitData = new BigDecimal(limit);
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("wifi_ap_mobile_data_limit"), false, this.mApLimitObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("wifi_ap_mobile_data_limit_value"), false, this.mApLimitValueObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("mobile_data"), false, this.mMobileDataObserver);
        if (WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing()) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("wifi_ap_wifi_sharing"), false, this.mWifiSharingObserver);
        }
        registerForBroadcasts();
        this.mNM = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED");
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    /* access modifiers changed from: private */
    public void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "handleEvent action : " + action);
        if (action.equals("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED")) {
            if (!this.isStarted) {
                getTetheringBytes();
                this.isStarted = true;
            }
            int ClientNum = intent.getIntExtra("NUM", 0);
            if (WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing() && isWifiSharingEnabled() && isWifiConnected()) {
                Log.d(TAG, "Don't need to start to count ApData, This use the Wi-Fi data");
            } else if (ClientNum > 0) {
                if (!this.mApData.isRunning()) {
                    this.mApData.resume();
                    Log.d(TAG, "start to count ApData");
                }
            } else if (this.mApData.isRunning()) {
                this.mApData.pause();
            }
        } else if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
            int state = intent.getIntExtra("wifi_state", 14);
            if (state == 10) {
                if (this.mApData.isRunning()) {
                    this.mApData.pause();
                }
            } else if (state == 11) {
                this.isWifiApEnabled = false;
                resetApDataLimit();
            } else if (state == 13) {
                this.isWifiApEnabled = true;
                resetApDataUsage();
            }
        } else if (action.equals("android.net.wifi.STATE_CHANGE") && WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing() && isWifiSharingEnabled()) {
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                this.saveWifiStatus = true;
                if (this.mApData.isRunning()) {
                    this.mApData.pause();
                }
            } else if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED && this.isWifiApEnabled) {
                this.saveWifiStatus = false;
                this.saveMobileDate = true;
                this.mResumeData = Settings.Secure.getString(this.mContext.getContentResolver(), "wifi_ap_mobile_data_usage_value");
                getTetheringBytes();
                if (!this.mApData.isRunning()) {
                    this.mApData.resume();
                }
            }
        }
    }

    private void resetApDataLimit() {
        if (DBG) {
            Log.d(TAG, "Mobile AP is disabled, reset Mobile AP Usage data, Limit value reached : " + this.isReached);
        }
        this.isStarted = false;
        this.isReached = false;
        this.isAgain = false;
        this.isWifiApEnabled = false;
        this.saveWifiStatus = false;
        this.saveMobileDate = false;
        this.mResumeData = null;
        this.mApData.pause();
    }

    private void resetApDataUsage() {
        if (DBG) {
            Log.d(TAG, "Mobile AP enabled, reset Mobile AP usage data");
        }
        Settings.Secure.putString(this.mContext.getContentResolver(), "wifi_ap_mobile_data_usage_value", BigDecimal.valueOf(0).toString());
    }

    /* access modifiers changed from: private */
    public void getTetheringBytes() {
        this.mBaseRxBytes = 0;
        this.mBaseTxBytes = 0;
        try {
            NetworkStats.Entry entry = this.mNM.getNetworkStatsTethering(1).getTotal((NetworkStats.Entry) null);
            this.mBaseRxBytes = entry.rxBytes;
            this.mBaseTxBytes = entry.txBytes;
        } catch (RemoteException e) {
        }
    }

    private class CountApData extends Handler {
        private boolean check = false;
        private long mBytes;
        private long mRxBytes;
        private long mTxBytes;

        CountApData() {
        }

        /* access modifiers changed from: package-private */
        public void resume() {
            boolean z = true;
            this.check = true;
            if (!HotspotMobileDataLimit.this.isDataConnected) {
                HotspotMobileDataLimit hotspotMobileDataLimit = HotspotMobileDataLimit.this;
                if (Settings.Global.getInt(hotspotMobileDataLimit.mContext.getContentResolver(), "mobile_data", 0) != 1) {
                    z = false;
                }
                boolean unused = hotspotMobileDataLimit.isDataConnected = z;
            }
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        /* access modifiers changed from: package-private */
        public void pause() {
            this.check = false;
            removeMessages(0);
        }

        /* access modifiers changed from: package-private */
        public boolean isRunning() {
            return this.check;
        }

        public void handleMessage(Message message) {
            if (this.check) {
                if (!HotspotMobileDataLimit.this.isDataConnected && HotspotMobileDataLimit.this.mApData.isRunning()) {
                    HotspotMobileDataLimit.this.mApData.pause();
                }
                long mTx = 0;
                long mRx = 0;
                try {
                    NetworkStats.Entry entry = HotspotMobileDataLimit.this.mNM.getNetworkStatsTethering(1).getTotal((NetworkStats.Entry) null);
                    mTx = entry.txBytes;
                    mRx = entry.rxBytes;
                } catch (RemoteException e) {
                }
                this.mTxBytes = mTx - HotspotMobileDataLimit.this.mBaseTxBytes;
                this.mRxBytes = mRx - HotspotMobileDataLimit.this.mBaseRxBytes;
                this.mBytes = this.mTxBytes + this.mRxBytes;
                BigDecimal usage = new BigDecimal(this.mBytes);
                if ((HotspotMobileDataLimit.this.saveMobileDate || HotspotMobileDataLimit.this.isAgain) && HotspotMobileDataLimit.this.mResumeData != null) {
                    usage = usage.add(new BigDecimal(HotspotMobileDataLimit.this.mResumeData));
                }
                if (HotspotMobileDataLimit.DBG) {
                    Log.d(HotspotMobileDataLimit.TAG, "mBaseTxBytes = " + HotspotMobileDataLimit.this.mBaseTxBytes + ", mBaseRxBytes = " + HotspotMobileDataLimit.this.mBaseRxBytes + ", mTx = " + mTx + ", mRx = " + mRx + ", mTxBytes = " + this.mTxBytes + ", mRxBytes = " + this.mRxBytes + ", mBytes = " + this.mBytes + ", usage = " + usage.toString());
                }
                if (!HotspotMobileDataLimit.this.mDataLimited || HotspotMobileDataLimit.this.mLimitData == null) {
                    Settings.Secure.putString(HotspotMobileDataLimit.this.mContext.getContentResolver(), "wifi_ap_mobile_data_usage_value", usage.toString());
                } else {
                    if (HotspotMobileDataLimit.DBG) {
                        Log.d(HotspotMobileDataLimit.TAG, "Ap Data Limit Value : " + HotspotMobileDataLimit.this.mLimitData.toString());
                    }
                    if (usage.compareTo(HotspotMobileDataLimit.this.mLimitData) >= 0) {
                        boolean unused = HotspotMobileDataLimit.this.isReached = true;
                        Log.d(HotspotMobileDataLimit.TAG, "Mobile AP Limited Data reached, turn off Mobile AP");
                        Settings.Secure.putString(HotspotMobileDataLimit.this.mContext.getContentResolver(), "wifi_ap_mobile_data_usage_value", HotspotMobileDataLimit.this.mLimitData.toString());
                        ((WifiManager) HotspotMobileDataLimit.this.mContext.getSystemService("wifi")).semSetWifiApEnabled((WifiConfiguration) null, false);
                    } else {
                        Settings.Secure.putString(HotspotMobileDataLimit.this.mContext.getContentResolver(), "wifi_ap_mobile_data_usage_value", usage.toString());
                    }
                }
                if (!HotspotMobileDataLimit.this.isReached) {
                    sendEmptyMessageDelayed(0, 1000);
                }
            }
        }
    }

    private boolean isWifiSharingEnabled() {
        try {
            if (Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_wifi_sharing") == 1) {
                return true;
            }
            return Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_wifi_sharing") == 0 ? false : false;
        } catch (Settings.SettingNotFoundException e) {
        }
    }

    private boolean isWifiConnected() {
        ConnectivityManager connectivity2 = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (connectivity2 != null) {
            return connectivity2.getNetworkInfo(1).isConnected();
        }
        return false;
    }
}
