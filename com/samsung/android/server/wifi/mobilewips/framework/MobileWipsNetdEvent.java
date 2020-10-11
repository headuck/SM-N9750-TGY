package com.samsung.android.server.wifi.mobilewips.framework;

import android.app.admin.ConnectEvent;
import android.app.admin.DnsEvent;
import android.app.admin.NetworkEvent;
import android.content.Context;
import android.net.IIpConnectivityMetrics;
import android.net.INetdEventCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.server.net.BaseNetdEventCallback;

public class MobileWipsNetdEvent {
    private static final boolean DETAIL_LOGGING = false;
    static final String NW_EVENT_KEY = "network_event";
    static final int NW_RECEIVE_EVENT_MSG = 1;
    private static final String TAG = "MobileWips::netd";
    /* access modifiers changed from: private */
    public Context mContext = null;
    private IIpConnectivityMetrics mIpConnectivityMetrics = IIpConnectivityMetrics.Stub.asInterface(ServiceManager.getService("connmetrics"));
    private boolean mIsCallbackRegistered = false;
    /* access modifiers changed from: private */
    public boolean mIsEnabled = false;
    private final INetdEventCallback mNetdEventCallback = new BaseNetdEventCallback() {
        public void onDnsEvent(int netId, int eventType, int returnCode, String hostname, String[] ipAddresses, int ipAddressesCount, long timestamp, int uid) {
            if (!MobileWipsNetdEvent.this.mIsEnabled) {
                int i = uid;
            } else if (MobileWipsNetdEvent.this.mPaused) {
                int i2 = uid;
            } else if (ipAddressesCount != 0) {
                sendNetworkEvent(new DnsEvent(hostname, ipAddresses, ipAddressesCount, MobileWipsNetdEvent.this.mContext.getPackageManager().getNameForUid(uid), timestamp));
            }
        }

        public void onConnectEvent(String ipAddr, int port, long timestamp, int uid) {
            if (MobileWipsNetdEvent.this.mIsEnabled && !MobileWipsNetdEvent.this.mPaused) {
                sendNetworkEvent(new ConnectEvent(ipAddr, port, MobileWipsNetdEvent.this.mContext.getPackageManager().getNameForUid(uid), timestamp));
            }
        }

        private void sendNetworkEvent(NetworkEvent event) {
            if (event != null) {
                Message msg = new Message();
                if (event instanceof ConnectEvent) {
                    msg.what = 38;
                } else if (event instanceof DnsEvent) {
                    msg.what = 37;
                }
                Bundle bundle = new Bundle();
                bundle.putParcelable(MobileWipsNetdEvent.NW_EVENT_KEY, event);
                msg.setData(bundle);
                MobileWipsFrameworkService.getInstance().getHandler().sendMessage(msg);
            }
        }
    };
    private ServiceHandler mNetdEventHandler;
    private HandlerThread mNetdThread;
    private HandlerThread mNetworkHandlerThread;
    /* access modifiers changed from: private */
    public boolean mPaused = true;

    public MobileWipsNetdEvent(Context context) {
        this.mContext = context;
        this.mNetdThread = new HandlerThread(TAG);
        this.mNetdThread.start();
        this.mNetdEventHandler = new ServiceHandler(this.mNetdThread.getLooper());
    }

    private static class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
        }
    }

    public void sendMessage(Message msg) {
        this.mNetdEventHandler.sendMessage(msg);
    }

    public void sendEmptyMessageDelayed(int id, int delay) {
        this.mNetdEventHandler.sendEmptyMessageDelayed(id, (long) delay);
    }

    public void removeMessages(int id) {
        this.mNetdEventHandler.removeMessages(id);
    }

    public void sendEmptyMessage(int id) {
        Message msg = Message.obtain();
        msg.what = id;
        sendMessage(msg);
    }

    public ServiceHandler getHandler() {
        return this.mNetdEventHandler;
    }

    public boolean setNetdEventStatus(int value) {
        Log.d(TAG, "setNetdEventStatus " + value);
        switch (value) {
            case 43:
                return checkSetNetdEventCallback(true);
            case 44:
                return checkSetNetdEventCallback(false);
            case 45:
                this.mPaused = true;
                return false;
            case 46:
                this.mPaused = false;
                return false;
            case 47:
                this.mIsEnabled = true;
                return false;
            case 48:
                this.mIsEnabled = false;
                return false;
            default:
                return false;
        }
    }

    private boolean checkSetNetdEventCallback(boolean enable) {
        if (enable == this.mIsCallbackRegistered) {
            Log.d(TAG, "Netd event callback registeration (" + enable + ") not changed.");
            return true;
        }
        if (this.mIpConnectivityMetrics == null) {
            this.mIpConnectivityMetrics = IIpConnectivityMetrics.Stub.asInterface(ServiceManager.getService("connmetrics"));
            if (this.mIpConnectivityMetrics == null) {
                Log.e(TAG, "Failed to register callback with IIpConnectivityMetrics.");
                return false;
            }
        }
        if (enable) {
            try {
                if (!this.mIpConnectivityMetrics.addNetdEventCallback(3, this.mNetdEventCallback)) {
                    return false;
                }
                Log.d(TAG, "addNetdEventCallback success");
                this.mIsCallbackRegistered = true;
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to make remote calls to (un)register the callback: ", e);
                return false;
            }
        } else if (!this.mIpConnectivityMetrics.removeNetdEventCallback(3)) {
            return false;
        } else {
            Log.d(TAG, "removeNetdEventCallback success");
            this.mIsCallbackRegistered = false;
            return true;
        }
    }
}
