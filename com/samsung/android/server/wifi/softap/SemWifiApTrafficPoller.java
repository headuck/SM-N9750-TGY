package com.samsung.android.server.wifi.softap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;
import com.samsung.android.os.SemDvfsManager;

public class SemWifiApTrafficPoller {
    private static final int CHECK_TO_DEALY_FIRST_TIME = 100;
    private static final int CHECK_TO_DEALY_TIME = 3000;
    private static final int DefaultMode = 0;
    private static final int PrimaryMode = 1;
    private static final int SET_CHANGE_PCIE_CORE = 3;
    private static final int SET_WIFIAP_L1SS_CTRL = 0;
    private static final int START_TRAFFIC_CHECK = 1;
    private static final int STOP_TRAFFIC_CHECK = 2;
    private static final int SecondaryMode = 2;
    private static final String TAG = "SemWifiApTrafficPoller";
    private static final int mAffinityBoosterThreshod = 45875200;
    private static final int mDefaultCoreBoosterIndex = Integer.parseInt("0");
    private static final int mHotspotL1ssDisableThreshold = (((Integer.parseInt("30") * 1024) * 1024) / 8);
    private static IntentFilter mSemWifiApTrafficPollerIntentFilter;
    private int NumOfClientsConnected;
    /* access modifiers changed from: private */
    public String mApInterfaceName;
    private Context mContext;
    /* access modifiers changed from: private */
    public int mCurrenAffinityMode = 0;
    /* access modifiers changed from: private */
    public boolean mCurrentL1ssModeValue = false;
    private SemDvfsManager mDefaultCpuCoreBooster = null;
    /* access modifiers changed from: private */
    public boolean mHotspotEnabled = false;
    private SemDvfsManager mLpm = null;
    private long mMaxPreRxBytes;
    private long mMaxPreTxBytes;
    private long mMaxRxBytes;
    private long mMaxTxBytes;
    /* access modifiers changed from: private */
    public boolean mNeedBooster = false;
    private long mRxBytesInterface1;
    private final BroadcastReceiver mSemWifiApTrafficPollerReceiver;
    /* access modifiers changed from: private */
    public SemWifiApTrafficPollerWorkHandler mSemWifiApTrafficPollerWorkHandler = null;
    private HandlerThread mSemWifiApTrafficPollerWorkThread = null;
    private long mTxBytesInterface1;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;
    private WifiNative mWifiNative;
    private long preRxBytesInterface1 = 0;
    /* access modifiers changed from: private */
    public long preTxBytesInterface1 = 0;

    public SemWifiApTrafficPoller(Context context, WifiNative wifinative) {
        this.mContext = context;
        this.mWifiNative = wifinative;
        mSemWifiApTrafficPollerIntentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
        mSemWifiApTrafficPollerIntentFilter.addAction("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED");
        this.mSemWifiApTrafficPollerReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(SemWifiApTrafficPoller.TAG, "onReceive :" + action);
                if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                    int state = intent.getIntExtra("wifi_state", 14);
                    WifiManager unused = SemWifiApTrafficPoller.this.mWifiManager = (WifiManager) context.getSystemService("wifi");
                    int wifiState = SemWifiApTrafficPoller.this.mWifiManager.getWifiState();
                    if (state == 13) {
                        if (WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing() || WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharingLite()) {
                            String unused2 = SemWifiApTrafficPoller.this.mApInterfaceName = "swlan0";
                        } else {
                            String unused3 = SemWifiApTrafficPoller.this.mApInterfaceName = "wlan0";
                        }
                        boolean unused4 = SemWifiApTrafficPoller.this.mHotspotEnabled = true;
                        Log.d(SemWifiApTrafficPoller.TAG, "setPcieIrqAffinity -> DefaultMode, Reason: - Turned on hotspot  check needbooster: " + SemWifiApTrafficPoller.this.mNeedBooster);
                        SemWifiApTrafficPoller.this.setPcieIrqAffinity(0);
                        int unused5 = SemWifiApTrafficPoller.this.mCurrenAffinityMode = 0;
                    } else if (state == 11 || state == 14) {
                        boolean unused6 = SemWifiApTrafficPoller.this.mHotspotEnabled = false;
                        Log.d(SemWifiApTrafficPoller.TAG, "setPcieIrqAffinity -> DefaultMode, Reason: - Turned off hotspot  check needbooster: " + SemWifiApTrafficPoller.this.mNeedBooster);
                        SemWifiApTrafficPoller.this.setPcieIrqAffinity(0);
                        boolean unused7 = SemWifiApTrafficPoller.this.mCurrentL1ssModeValue = false;
                        long unused8 = SemWifiApTrafficPoller.this.preTxBytesInterface1 = 0;
                        boolean unused9 = SemWifiApTrafficPoller.this.mNeedBooster = false;
                    }
                } else if (!intent.getAction().equals("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED")) {
                } else {
                    if (intent.getIntExtra("NUM", 0) > 0) {
                        if (!SemWifiApTrafficPoller.this.mNeedBooster) {
                            boolean unused10 = SemWifiApTrafficPoller.this.mNeedBooster = true;
                            if (SemWifiApTrafficPoller.this.mSemWifiApTrafficPollerWorkHandler != null) {
                                SemWifiApTrafficPoller.this.mSemWifiApTrafficPollerWorkHandler.removeMessages(1);
                                SemWifiApTrafficPoller.this.mSemWifiApTrafficPollerWorkHandler.sendEmptyMessageDelayed(1, 3000);
                            }
                        }
                    } else if (SemWifiApTrafficPoller.this.mNeedBooster) {
                        boolean unused11 = SemWifiApTrafficPoller.this.mNeedBooster = false;
                        if (SemWifiApTrafficPoller.this.mSemWifiApTrafficPollerWorkHandler != null) {
                            SemWifiApTrafficPoller.this.mSemWifiApTrafficPollerWorkHandler.removeMessages(1);
                        }
                        if (SemWifiApTrafficPoller.this.mCurrenAffinityMode != 0) {
                            Log.d(SemWifiApTrafficPoller.TAG, "setPcieIrqAffinity -> DefaultMode, Reason: hotspot with no client");
                            SemWifiApTrafficPoller.this.setPcieIrqAffinity(0);
                            int unused12 = SemWifiApTrafficPoller.this.mCurrenAffinityMode = 0;
                        }
                    }
                }
            }
        };
        this.mLpm = SemDvfsManager.createInstance(context, "MHS_L1SS", 26);
        Context context2 = this.mContext;
        if (context2 != null) {
            context2.registerReceiver(this.mSemWifiApTrafficPollerReceiver, mSemWifiApTrafficPollerIntentFilter);
        }
    }

    class SemWifiApTrafficPollerWorkHandler extends Handler {
        public SemWifiApTrafficPollerWorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                SemWifiApTrafficPoller.this.determineMaximumTpHotspot();
                Log.d(SemWifiApTrafficPoller.TAG, "received START_TRAFFIC_CHECK");
            } else if (i == 2) {
                Log.d(SemWifiApTrafficPoller.TAG, "received STOP_TRAFFIC_CHECK");
            } else if (i == 3) {
                Log.d(SemWifiApTrafficPoller.TAG, "received SET_CHANGE_PCIE_CORE");
            }
        }
    }

    public void handleBootCompleted() {
        Log.d(TAG, "first on mSemWifiApTrafficPollerWorkThread start");
        this.mSemWifiApTrafficPollerWorkThread = new HandlerThread(TAG);
        this.mSemWifiApTrafficPollerWorkThread.start();
        this.mSemWifiApTrafficPollerWorkHandler = new SemWifiApTrafficPollerWorkHandler(this.mSemWifiApTrafficPollerWorkThread.getLooper());
    }

    public void determineMaximumTpHotspot() {
        long mMaxTraffic = 0;
        if (this.mHotspotEnabled) {
            if (!TextUtils.isEmpty(this.mApInterfaceName)) {
                this.mTxBytesInterface1 = TrafficStats.getTxBytes(this.mApInterfaceName);
                this.mRxBytesInterface1 = TrafficStats.getRxBytes(this.mApInterfaceName);
                long j = this.mTxBytesInterface1;
                long j2 = this.preTxBytesInterface1;
                long j3 = this.mRxBytesInterface1;
                long j4 = this.preRxBytesInterface1;
                mMaxTraffic = (j - j2) + (j3 - j4);
                this.mMaxTxBytes = j - j2;
                this.mMaxRxBytes = j3 - j4;
            }
            if (this.preTxBytesInterface1 != 0) {
                long j5 = this.mMaxTxBytes;
                if (j5 > 0) {
                    long j6 = this.mMaxRxBytes;
                    if (j6 > 0) {
                        if (j5 + j6 > ((long) mHotspotL1ssDisableThreshold)) {
                            if (!this.mCurrentL1ssModeValue && this.mLpm != null) {
                                Log.d(TAG, "Over mHotspotL1ssDisableThreshold" + mHotspotL1ssDisableThreshold + "Mbps");
                                this.mLpm.acquire();
                                this.mCurrentL1ssModeValue = true;
                            }
                        } else if (this.mCurrentL1ssModeValue && this.mLpm != null) {
                            Log.d(TAG, "Less mHotspotL1ssDisableThreshold " + mHotspotL1ssDisableThreshold + "Mbps");
                            this.mLpm.release();
                            this.mCurrentL1ssModeValue = false;
                        }
                    }
                }
            }
            if (this.preTxBytesInterface1 != 0) {
                if (mMaxTraffic > 137625600 && this.mCurrenAffinityMode == 0) {
                    Log.d(TAG, "setPcieIrqAffinity -> SecondaryMode, Reason: - High Throughput");
                    setPcieIrqAffinity(2);
                    this.mCurrenAffinityMode = 2;
                } else if (mMaxTraffic <= 45875200 && this.mCurrenAffinityMode != 0) {
                    Log.d(TAG, "setPcieIrqAffinity -> PrimaryMode, Reason: - Low Throughput, check needbooster: " + this.mNeedBooster);
                    setPcieIrqAffinity(0);
                    this.mCurrenAffinityMode = 0;
                }
            }
            this.preTxBytesInterface1 = this.mTxBytesInterface1;
            this.preRxBytesInterface1 = this.mRxBytesInterface1;
            this.mSemWifiApTrafficPollerWorkHandler.sendEmptyMessageDelayed(1, 3000);
        }
    }

    /* access modifiers changed from: private */
    public void setPcieIrqAffinity(int mMOde) {
        Log.d(TAG, "setPcieIrqAffinity " + mMOde);
        this.mWifiNative.setHotspotPcieIrqAffinity(mMOde);
    }
}
