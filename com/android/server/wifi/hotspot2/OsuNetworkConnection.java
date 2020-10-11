package com.android.server.wifi.hotspot2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

public class OsuNetworkConnection {
    private static final String TAG = "PasspointOsuNetworkConnection";
    private static final int TIMEOUT_MS = 10000;
    /* access modifiers changed from: private */
    public Callbacks mCallbacks;
    /* access modifiers changed from: private */
    public boolean mConnected = false;
    private ConnectivityCallbacks mConnectivityCallbacks;
    private ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public Network mNetwork = null;
    /* access modifiers changed from: private */
    public int mNetworkId = -1;
    /* access modifiers changed from: private */
    public boolean mVerboseLoggingEnabled = false;
    /* access modifiers changed from: private */
    public boolean mWifiEnabled = false;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;

    public interface Callbacks {
        void onConnected(Network network);

        void onDisconnected();

        void onTimeOut();

        void onWifiDisabled();

        void onWifiEnabled();
    }

    public OsuNetworkConnection(Context context) {
        this.mContext = context;
    }

    public void init(Handler handler) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    int state = intent.getIntExtra("wifi_state", 4);
                    if (state == 1 && OsuNetworkConnection.this.mWifiEnabled) {
                        boolean unused = OsuNetworkConnection.this.mWifiEnabled = false;
                        if (OsuNetworkConnection.this.mCallbacks != null) {
                            OsuNetworkConnection.this.mCallbacks.onWifiDisabled();
                        }
                    }
                    if (state == 3 && !OsuNetworkConnection.this.mWifiEnabled) {
                        boolean unused2 = OsuNetworkConnection.this.mWifiEnabled = true;
                        if (OsuNetworkConnection.this.mCallbacks != null) {
                            OsuNetworkConnection.this.mCallbacks.onWifiEnabled();
                        }
                    }
                }
            }
        };
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mContext.registerReceiver(receiver, filter, (String) null, handler);
        this.mWifiEnabled = this.mWifiManager.isWifiEnabled();
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mConnectivityCallbacks = new ConnectivityCallbacks();
        this.mHandler = handler;
    }

    public void disconnectIfNeeded() {
        if (this.mNetworkId >= 0) {
            this.mConnectivityManager.unregisterNetworkCallback(this.mConnectivityCallbacks);
            this.mWifiManager.removeNetwork(this.mNetworkId);
            this.mNetworkId = -1;
            this.mNetwork = null;
            this.mConnected = false;
        } else if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "No connection to tear down");
        }
    }

    public void setEventCallback(Callbacks callbacks) {
        this.mCallbacks = callbacks;
    }

    public boolean connect(WifiSsid ssid, String nai, String friendlyName) {
        if (this.mConnected) {
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Connect called twice");
            }
            return true;
        } else if (!this.mWifiEnabled) {
            Log.w(TAG, "Wifi is not enabled");
            return false;
        } else {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = "\"" + ssid.toString() + "\"";
            config.noInternetAccessExpected = true;
            config.osu = true;
            config.ephemeral = true;
            config.providerFriendlyName = friendlyName;
            if (TextUtils.isEmpty(nai)) {
                config.allowedKeyManagement.set(0);
                this.mNetworkId = this.mWifiManager.addNetwork(config);
                if (this.mNetworkId < 0) {
                    Log.e(TAG, "Unable to add network");
                    return false;
                }
                this.mConnectivityManager.requestNetwork(new NetworkRequest.Builder().addTransportType(1).removeCapability(14).build(), this.mConnectivityCallbacks, this.mHandler, 10000);
                if (!this.mWifiManager.enableNetwork(this.mNetworkId, true)) {
                    Log.e(TAG, "Unable to enable network " + this.mNetworkId);
                    disconnectIfNeeded();
                    return false;
                }
                if (this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "Current network ID " + this.mNetworkId);
                }
                return true;
            }
            Log.w(TAG, "OSEN not supported");
            return false;
        }
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
    }

    private class ConnectivityCallbacks extends ConnectivityManager.NetworkCallback {
        private ConnectivityCallbacks() {
        }

        public void onAvailable(Network network) {
            WifiInfo wifiInfo = OsuNetworkConnection.this.mWifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                Log.w(OsuNetworkConnection.TAG, "wifiInfo is not valid");
            } else if (OsuNetworkConnection.this.mNetworkId < 0 || OsuNetworkConnection.this.mNetworkId != wifiInfo.getNetworkId()) {
                Log.w(OsuNetworkConnection.TAG, "Irrelevant network available notification for netId: " + wifiInfo.getNetworkId());
            } else {
                Network unused = OsuNetworkConnection.this.mNetwork = network;
                boolean unused2 = OsuNetworkConnection.this.mConnected = true;
            }
        }

        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            if (OsuNetworkConnection.this.mVerboseLoggingEnabled) {
                Log.v(OsuNetworkConnection.TAG, "onLinkPropertiesChanged for network=" + network + " isProvisioned?" + linkProperties.isProvisioned());
            }
            if (OsuNetworkConnection.this.mNetwork == null) {
                Log.w(OsuNetworkConnection.TAG, "ignore onLinkPropertyChanged event for null network");
            } else if (linkProperties.isProvisioned() && OsuNetworkConnection.this.mCallbacks != null) {
                OsuNetworkConnection.this.mCallbacks.onConnected(network);
            }
        }

        public void onUnavailable() {
            if (OsuNetworkConnection.this.mVerboseLoggingEnabled) {
                Log.v(OsuNetworkConnection.TAG, "onUnvailable ");
            }
            if (OsuNetworkConnection.this.mCallbacks != null) {
                OsuNetworkConnection.this.mCallbacks.onTimeOut();
            }
        }

        public void onLost(Network network) {
            if (OsuNetworkConnection.this.mVerboseLoggingEnabled) {
                Log.v(OsuNetworkConnection.TAG, "onLost " + network);
            }
            if (network != OsuNetworkConnection.this.mNetwork) {
                Log.w(OsuNetworkConnection.TAG, "Irrelevant network lost notification");
            } else if (OsuNetworkConnection.this.mCallbacks != null) {
                OsuNetworkConnection.this.mCallbacks.onDisconnected();
            }
        }
    }
}
