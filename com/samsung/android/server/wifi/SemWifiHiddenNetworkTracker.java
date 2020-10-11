package com.samsung.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.SystemClock;
import android.util.Log;
import com.android.server.wifi.ScanResultMatchInfo;
import java.util.Iterator;
import java.util.List;

public class SemWifiHiddenNetworkTracker {
    private static final long MAX_DURATION = 10000;
    private static final String TAG = "WifiHiddenTracker";
    /* access modifiers changed from: private */
    public WifiHiddenNetworkAdapter mAdapter;
    private Context mContext;
    /* access modifiers changed from: private */
    public WifiConfiguration mHiddenConfig;
    /* access modifiers changed from: private */
    public boolean mIsTracking;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (SemWifiHiddenNetworkTracker.this.mIsTracking) {
                boolean hiddenApScanned = false;
                Iterator<ScanResult> it = SemWifiHiddenNetworkTracker.this.mAdapter.getScanResults().iterator();
                while (true) {
                    if (it.hasNext()) {
                        if (ScanResultMatchInfo.fromScanResult(it.next()).equals(ScanResultMatchInfo.fromWifiConfiguration(SemWifiHiddenNetworkTracker.this.mHiddenConfig))) {
                            hiddenApScanned = true;
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (hiddenApScanned) {
                    return;
                }
                if (SystemClock.elapsedRealtime() - SemWifiHiddenNetworkTracker.this.mStartTrackingAt > 10000) {
                    Log.d(SemWifiHiddenNetworkTracker.TAG, "failed to connect hidden AP, show toast");
                    SemWifiHiddenNetworkTracker.this.showAddNetworkFailNoti();
                    return;
                }
                Log.d(SemWifiHiddenNetworkTracker.TAG, "hidden AP is not scanned yet, wait next scan");
            }
        }
    };
    /* access modifiers changed from: private */
    public long mStartTrackingAt;

    public interface WifiHiddenNetworkAdapter {
        List<ScanResult> getScanResults();
    }

    public SemWifiHiddenNetworkTracker(Context context, WifiHiddenNetworkAdapter adapter) {
        this.mContext = context;
        this.mAdapter = adapter;
    }

    public void startTracking(WifiConfiguration config) {
        if (config == null) {
            Log.e(TAG, "tracking config is null, failed to start");
            return;
        }
        Log.d(TAG, "start tracking hidden AP connection");
        this.mIsTracking = true;
        this.mHiddenConfig = config;
        this.mStartTrackingAt = SystemClock.elapsedRealtime();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    public void stopTracking() {
        if (this.mIsTracking) {
            Log.d(TAG, "stop tracking");
            this.mIsTracking = false;
            this.mContext.unregisterReceiver(this.mReceiver);
        }
    }

    /* access modifiers changed from: private */
    public void showAddNetworkFailNoti() {
        if (this.mIsTracking) {
            SemWifiFrameworkUxUtils.showToast(this.mContext, 50, SemWifiFrameworkUxUtils.removeDoubleQuotes(this.mHiddenConfig.SSID));
        } else {
            Log.d(TAG, "ignore to show the toast");
        }
        stopTracking();
    }
}
