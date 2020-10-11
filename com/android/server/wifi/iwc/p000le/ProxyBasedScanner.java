package com.android.server.wifi.iwc.p000le;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.iwc.p000le.ProxyBasedScanner;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* renamed from: com.android.server.wifi.iwc.le.ProxyBasedScanner */
public class ProxyBasedScanner implements WiFiScanner {
    private static final String TAG = "IWCLE";
    private static final Map<Integer, Integer> mapChToFreq = new HashMap();
    /* access modifiers changed from: private */
    public static final Map<Integer, Integer> mapFreqToCh = new HashMap();
    /* access modifiers changed from: private */
    public CapInfo capInfo = null;
    /* access modifiers changed from: private */
    public boolean isScanStarted;
    /* access modifiers changed from: private */
    public List<Integer> lastScanChList = null;
    private Context mContext = null;
    /* access modifiers changed from: private */
    public WiFiScanResultListener mListener = null;
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (ProxyBasedScanner.this.isScanStarted && ProxyBasedScanner.this.mListener != null && intent.getAction().equals("android.net.wifi.SCAN_RESULTS")) {
                Map<String, Double> rssi = new HashMap<>();
                List<Integer> scannedChList = new ArrayList<>();
                Map<Integer, Integer> mapChAPNum = new HashMap<>();
                for (ScanResult result : ProxyBasedScanner.this.mWifiInjector.getScanRequestProxy().getScanResults()) {
                    int ch = ((Integer) ProxyBasedScanner.mapFreqToCh.getOrDefault(Integer.valueOf(result.frequency), -1)).intValue();
                    if (ProxyBasedScanner.this.lastScanChList != null && ch > 0 && ProxyBasedScanner.this.lastScanChList.contains(Integer.valueOf(ch))) {
                        rssi.put(result.BSSID, Double.valueOf((double) result.level));
                        int apNum = mapChAPNum.getOrDefault(Integer.valueOf(ch), 0).intValue();
                        if (apNum == 0) {
                            scannedChList.add(Integer.valueOf(ch));
                        }
                        mapChAPNum.put(Integer.valueOf(ch), Integer.valueOf(apNum + 1));
                    }
                }
                Log.d(ProxyBasedScanner.TAG, "" + rssi.size() + "APs are scanned");
                if (scannedChList.size() > 5) {
                    scannedChList.sort(new Comparator(mapChAPNum) {
                        private final /* synthetic */ Map f$0;

                        {
                            this.f$0 = r1;
                        }

                        public final int compare(Object obj, Object obj2) {
                            return ProxyBasedScanner.C05171.lambda$onReceive$0(this.f$0, (Integer) obj, (Integer) obj2);
                        }
                    });
                    scannedChList = scannedChList.subList(0, 5);
                }
                ProxyBasedScanner.this.mListener.onWiFiScanResultAcquired(ProxyBasedScanner.this.capInfo, rssi, scannedChList);
                List unused = ProxyBasedScanner.this.lastScanChList = null;
                boolean unused2 = ProxyBasedScanner.this.isScanStarted = false;
            }
        }

        static /* synthetic */ int lambda$onReceive$0(Map mapChAPNum, Integer o1, Integer o2) {
            return ((Integer) mapChAPNum.get(o2)).intValue() - ((Integer) mapChAPNum.get(o1)).intValue();
        }
    };
    /* access modifiers changed from: private */
    public WifiInjector mWifiInjector;
    private String packageName = null;

    static {
        mapChToFreq.put(1, 2412);
        mapChToFreq.put(2, 2417);
        mapChToFreq.put(3, 2422);
        mapChToFreq.put(4, 2427);
        mapChToFreq.put(5, 2432);
        mapChToFreq.put(6, 2437);
        mapChToFreq.put(7, 2442);
        mapChToFreq.put(8, 2447);
        mapChToFreq.put(9, 2452);
        mapChToFreq.put(10, 2457);
        mapChToFreq.put(11, 2462);
        mapChToFreq.put(12, 2467);
        mapChToFreq.put(13, 2472);
        mapChToFreq.put(14, 2484);
        for (Map.Entry<Integer, Integer> entry : mapChToFreq.entrySet()) {
            mapFreqToCh.put(entry.getValue(), entry.getKey());
        }
    }

    public void initialize(Context context, Bundle param) {
        this.mContext = context;
        this.mWifiInjector = WifiInjector.getInstance();
        this.packageName = this.mContext.getOpPackageName();
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.net.wifi.SCAN_RESULTS"));
        this.isScanStarted = false;
    }

    public void requestScan(WiFiScanResultListener listener, CapInfo capInfo2, List<Integer> chList) {
        if (!this.isScanStarted && this.mContext != null) {
            this.mListener = listener;
            this.capInfo = capInfo2;
            this.lastScanChList = chList;
            this.mWifiInjector.getScanRequestProxy().startScan(Binder.getCallingUid(), this.packageName, convertToFreq(chList));
            this.isScanStarted = true;
        }
    }

    private Set<Integer> convertToFreq(List<Integer> chList) {
        Set<Integer> freqSet = new HashSet<>();
        for (Integer ch : chList) {
            freqSet.add(mapChToFreq.get(ch));
        }
        return freqSet;
    }
}
