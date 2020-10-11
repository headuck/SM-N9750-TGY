package com.samsung.android.server.wifi.mobilewips.framework;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.WorkSource;
import android.util.Log;
import com.android.server.wifi.WifiInjector;
import java.util.ArrayList;
import java.util.List;

public class MobileWipsWifiScanner {
    private static final String TAG = "MobileWips::scanner";
    /* access modifiers changed from: private */
    public static boolean mStarted = false;
    private Context mContext = null;
    private PartialScanListener mPartialScanListener = null;
    private HandlerThread mScannerThread;
    private WifiScanner mWifiScanner = null;
    private ServiceHandler mWipsScannerHandler;
    private WorkSource mWorkSource = null;

    public MobileWipsWifiScanner(Context context) {
        this.mContext = context;
        this.mScannerThread = new HandlerThread(TAG);
        this.mScannerThread.start();
        this.mWipsScannerHandler = new ServiceHandler(this.mScannerThread.getLooper());
        this.mPartialScanListener = new PartialScanListener();
        this.mWorkSource = new WorkSource(1000);
    }

    public void start() {
        mStarted = true;
    }

    public void stop() {
        mStarted = false;
        WifiScanner wifiScanner = this.mWifiScanner;
        if (wifiScanner != null) {
            wifiScanner.stopScan(this.mPartialScanListener);
        }
        this.mWipsScannerHandler.removeCallbacksAndMessages((Object) null);
    }

    final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 34:
                    Bundle bundleEvent = msg.getData();
                    String karmaSsid = bundleEvent.getString("karma");
                    String fKarmaSsid = bundleEvent.getString("fKarma");
                    String fKarmaOriSsid = bundleEvent.getString("fkarmaOri");
                    MobileWipsWifiScanner.this.doPartialScan(bundleEvent.getInt("freq"), karmaSsid, fKarmaSsid, fKarmaOriSsid);
                    return;
                default:
                    return;
            }
        }
    }

    public void sendMessage(Message msg) {
        this.mWipsScannerHandler.sendMessage(msg);
    }

    public void sendEmptyMessageDelayed(int id, int delay) {
        this.mWipsScannerHandler.sendEmptyMessageDelayed(id, (long) delay);
    }

    public void removeMessages(int id) {
        this.mWipsScannerHandler.removeMessages(id);
    }

    public void sendEmptyMessage(int id) {
        Message msg = Message.obtain();
        msg.what = id;
        sendMessage(msg);
    }

    public ServiceHandler getHandler() {
        return this.mWipsScannerHandler;
    }

    /* access modifiers changed from: private */
    public void doPartialScan(int freq, String karmaSsid, String fKarmaSsid, String fKarmaOriginSsid) {
        if (!mStarted) {
            Log.e(TAG, "scanner stoped");
            return;
        }
        String packageName = this.mContext.getOpPackageName();
        WifiScanner.ScanSettings settings = new WifiScanner.ScanSettings();
        settings.type = 2;
        settings.band = 0;
        settings.channels = new WifiScanner.ChannelSpec[1];
        settings.channels[0] = new WifiScanner.ChannelSpec(freq);
        settings.reportEvents = 3;
        settings.semPackageName = packageName;
        List<WifiScanner.ScanSettings.HiddenNetwork> hiddenList = new ArrayList<>();
        WifiScanner.ScanSettings.HiddenNetwork hiddenKarma = new WifiScanner.ScanSettings.HiddenNetwork("\"" + karmaSsid + "\"");
        WifiScanner.ScanSettings.HiddenNetwork hiddenFilterKarma = new WifiScanner.ScanSettings.HiddenNetwork(fKarmaSsid);
        WifiScanner.ScanSettings.HiddenNetwork hiddenFilterKarmaOriginal = new WifiScanner.ScanSettings.HiddenNetwork(fKarmaOriginSsid);
        hiddenList.add(hiddenKarma);
        hiddenList.add(hiddenFilterKarma);
        hiddenList.add(hiddenFilterKarmaOriginal);
        settings.hiddenNetworks = (WifiScanner.ScanSettings.HiddenNetwork[]) hiddenList.toArray(new WifiScanner.ScanSettings.HiddenNetwork[hiddenList.size()]);
        if (this.mWifiScanner == null) {
            this.mWifiScanner = WifiInjector.getInstance().getWifiScanner();
        }
        WifiScanner wifiScanner = this.mWifiScanner;
        if (wifiScanner != null) {
            wifiScanner.startScan(settings, this.mPartialScanListener, this.mWorkSource);
            Log.d(TAG, "doScanInternal started");
        }
    }

    private static class PartialScanListener implements WifiScanner.ScanListener {
        private PartialScanListener() {
        }

        public void onSuccess() {
            Log.d(MobileWipsWifiScanner.TAG, "Partial scan success");
        }

        public void onFailure(int reason, String description) {
            if (!MobileWipsWifiScanner.mStarted) {
                Log.e(MobileWipsWifiScanner.TAG, "scanner stoped");
                return;
            }
            Message msg = new Message();
            msg.what = 35;
            MobileWipsFrameworkService.getInstance().sendMessage(msg);
        }

        public void onResults(WifiScanner.ScanData[] scanDatas) {
            if (!MobileWipsWifiScanner.mStarted) {
                Log.e(MobileWipsWifiScanner.TAG, "scanner stoped");
            } else if (scanDatas == null) {
                Log.e(MobileWipsWifiScanner.TAG, "scanDatas null");
            } else if (scanDatas.length != 1) {
                Log.wtf(MobileWipsWifiScanner.TAG, "Found more than 1 batch of scan results, Failing...");
            } else {
                Log.d(MobileWipsWifiScanner.TAG, "onResults");
                List<MobileWipsScanResult> scanResults = new ArrayList<>();
                try {
                    for (ScanResult scanResult : scanDatas[0].getResults()) {
                        Parcel p = Parcel.obtain();
                        scanResult.writeToParcel(p, 0);
                        p.setDataPosition(0);
                        scanResults.add(MobileWipsScanResult.CREATOR.createFromParcel(p));
                    }
                } catch (Exception e) {
                    Log.e(MobileWipsWifiScanner.TAG, e.toString());
                }
                MobileWipsFrameworkService.getInstance().onScanResults(scanResults);
            }
        }

        public void onFullResult(ScanResult fullScanResult) {
        }

        public void onPeriodChanged(int periodInMs) {
        }
    }
}
