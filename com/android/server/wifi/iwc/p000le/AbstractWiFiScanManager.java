package com.android.server.wifi.iwc.p000le;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.util.List;
import java.util.Map;

/* renamed from: com.android.server.wifi.iwc.le.AbstractWiFiScanManager */
public abstract class AbstractWiFiScanManager implements WiFiScanManager, WiFiStatusListener, WiFiScanResultListener {
    protected static final int MSG_CAP_CHANGED = 3;
    protected static final int MSG_GOODLINK = 6;
    protected static final int MSG_POORLINK = 5;
    protected static final int MSG_START = 1;
    protected static final String TAG = "IWCLE";
    protected boolean isInitialized = false;
    protected boolean isMonitoring = false;
    protected long lastScanTimestamp;
    protected CapInfo mCapInfo = null;
    protected ChannelCache mChannelCache;
    protected WorkerHandler mHandler;
    protected WiFiScanResultListener mListener;
    protected HandlerThread mWiFiScanManagerThread = null;
    protected WiFiScanner mWiFiScanner;
    protected WiFiStatusObserver mWiFiStatusObserver;
    protected long minScanInterval;
    protected boolean wifiScanDoneInThisPoorLinkPeriod = false;

    /* access modifiers changed from: protected */
    public abstract void createComponents();

    public AbstractWiFiScanManager() {
        createComponents();
    }

    public void initialize(Context context, Bundle param) {
        Log.d(TAG, "AbstractWiFiScanManager:initialize() called");
        if (!this.isInitialized) {
            this.lastScanTimestamp = -1;
            this.minScanInterval = param.getLong(LEGlobalParams.KEY_MIN_SCAN_INTERVAL, 10000);
            this.mWiFiStatusObserver.initialize(context, param);
            this.mWiFiScanner.initialize(context, param);
            this.mChannelCache.initialize(param);
        }
    }

    public void finalize() {
    }

    public boolean isMonitoring() {
        return this.isMonitoring;
    }

    public void startMonitoring(WiFiScanResultListener listener) {
        Log.d(TAG, "AbstractWiFiScanManager:startMonitoring() called");
        stopMonitoring();
        this.mListener = listener;
        this.mWiFiScanManagerThread = new HandlerThread("localizer-wifiscanmanager");
        this.mWiFiScanManagerThread.start();
        this.mHandler = new WorkerHandler(this.mWiFiScanManagerThread.getLooper());
        WorkerHandler workerHandler = this.mHandler;
        workerHandler.sendMessage(workerHandler.obtainMessage(1));
        this.isMonitoring = true;
    }

    public void stopMonitoring() {
        Log.d(TAG, "AbstractWiFiScanManager:stopMonitoring() called");
        if (this.isMonitoring) {
            this.mWiFiStatusObserver.stopMonitoring();
            HandlerThread handlerThread = this.mWiFiScanManagerThread;
            if (handlerThread != null) {
                this.mHandler = null;
                handlerThread.quitSafely();
            }
            this.isMonitoring = false;
        }
    }

    public void onCapStatusUpdated(CapInfo capInfo) {
        Log.d(TAG, "AbstractWiFiScanManager onCapStatusUpdated");
        WorkerHandler workerHandler = this.mHandler;
        if (workerHandler != null) {
            workerHandler.sendMessage(workerHandler.obtainMessage(3, capInfo));
        }
    }

    public void onStatusUpdated(int statusType) {
        Log.d(TAG, "AbstractWiFiScanManager onStatusUpdated");
        WorkerHandler workerHandler = this.mHandler;
        if (workerHandler == null) {
            return;
        }
        if (statusType == 1) {
            workerHandler.sendMessage(workerHandler.obtainMessage(5));
        } else if (statusType == 2) {
            workerHandler.sendMessage(workerHandler.obtainMessage(6));
        }
    }

    public void onWiFiScanResultAcquired(CapInfo capInfo, Map<String, Double> scanResult, List<Integer> scannedChList) {
        Log.d(TAG, "AbstractWiFiScanManager onWiFiScanAcquired");
        if (capInfo != null && capInfo.bssid != null) {
            Log.d(TAG, "Scan result acquired : " + scanResult.size());
            this.mChannelCache.setChannelListIfNotExist(capInfo.bssid, scannedChList);
            this.mListener.onWiFiScanResultAcquired(capInfo, scanResult, scannedChList);
        }
    }

    /* renamed from: com.android.server.wifi.iwc.le.AbstractWiFiScanManager$WorkerHandler */
    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            boolean z = true;
            if (i == 1) {
                AbstractWiFiScanManager.this.mWiFiStatusObserver.startMonitoring(AbstractWiFiScanManager.this);
            } else if (i == 3) {
                AbstractWiFiScanManager.this.mCapInfo = (CapInfo) msg.obj;
            } else if (i == 5) {
                Log.d(AbstractWiFiScanManager.TAG, "AbstractWiFiScanManager MSG_POORLINK");
                long curTimestamp = System.currentTimeMillis();
                long interval = curTimestamp - AbstractWiFiScanManager.this.lastScanTimestamp;
                if (AbstractWiFiScanManager.this.mCapInfo == null || AbstractWiFiScanManager.this.wifiScanDoneInThisPoorLinkPeriod || interval <= AbstractWiFiScanManager.this.minScanInterval) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("AbstractWiFiScanManager MSG_POORLINK WiFi Scan is not requested ");
                    sb.append(AbstractWiFiScanManager.this.mCapInfo != null);
                    sb.append(",");
                    sb.append(!AbstractWiFiScanManager.this.wifiScanDoneInThisPoorLinkPeriod);
                    sb.append(",");
                    if (interval <= AbstractWiFiScanManager.this.minScanInterval) {
                        z = false;
                    }
                    sb.append(z);
                    Log.d(AbstractWiFiScanManager.TAG, sb.toString());
                } else {
                    Log.d(AbstractWiFiScanManager.TAG, "AbstractWiFiScanManager MSG_POORLINK WiFi Scan is requested");
                    WiFiScanner wiFiScanner = AbstractWiFiScanManager.this.mWiFiScanner;
                    AbstractWiFiScanManager abstractWiFiScanManager = AbstractWiFiScanManager.this;
                    wiFiScanner.requestScan(abstractWiFiScanManager, abstractWiFiScanManager.mCapInfo, AbstractWiFiScanManager.this.mChannelCache.getChannelList(AbstractWiFiScanManager.this.mCapInfo.bssid));
                    AbstractWiFiScanManager abstractWiFiScanManager2 = AbstractWiFiScanManager.this;
                    abstractWiFiScanManager2.lastScanTimestamp = curTimestamp;
                    abstractWiFiScanManager2.wifiScanDoneInThisPoorLinkPeriod = true;
                }
            } else if (i == 6) {
                AbstractWiFiScanManager.this.wifiScanDoneInThisPoorLinkPeriod = false;
                Log.d(AbstractWiFiScanManager.TAG, "AbstractWiFiScanManager MSG_GOODLINK getting out of poor link status");
            }
            super.handleMessage(msg);
        }
    }
}
