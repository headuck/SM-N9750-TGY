package com.samsung.android.server.wifi;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.scontext.SContextAnyMotionDetector;
import android.hardware.scontext.SContextEvent;
import android.hardware.scontext.SContextListener;
import android.hardware.scontext.SContextManager;
import android.os.Debug;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

public class SemSmdMotionDetector {
    /* access modifiers changed from: private */
    public static final boolean DBG = Debug.semIsProductDev();
    private static final long MAX_REGISTERATION_SENSOR_TIME = 360000;
    private static final long MAX_WAIT_TIME_SENSOR_STATUS = 1500;
    private static final String TAG = "WifiScanController";
    private static SemSmdMotionDetector sInstance = null;
    private boolean mCheckSMDSetting = false;
    private Context mContext;
    private HandlerThread mHandlerThread;
    private boolean mIsAnyMotionDetectorSupported = false;
    /* access modifiers changed from: private */
    public boolean mIsMoved = true;
    /* access modifiers changed from: private */
    public volatile boolean mIsRegisteredSMDListener = false;
    private long mLastNLPScanRequestTimeForSMDRegi = 0;
    private long mLastRequestTimeForGettingSMDStatus = 0;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (!SemSmdMotionDetector.this.mIsRegisteredSMDListener) {
                return;
            }
            if (!SemSmdMotionDetector.this.isLocationEnabled()) {
                SemSmdMotionDetector.this.unregisterSensorMonitor();
            } else if (SemSmdMotionDetector.this.getPersistedAirplaneModeOn()) {
                SemSmdMotionDetector.this.unregisterSensorMonitor();
            }
        }
    };
    private final SContextListener mSContextListener = new SContextListener() {
        public void onSContextChanged(SContextEvent sContextEvent) {
            SContextAnyMotionDetector anyMotionDetector;
            if (sContextEvent.scontext.getType() == 50 && (anyMotionDetector = sContextEvent.getAnyMotionDetectorContext()) != null) {
                int action = anyMotionDetector.getAction();
                if (action == 0) {
                    if (SemSmdMotionDetector.DBG) {
                        Log.d(SemSmdMotionDetector.TAG, "SMD detect : none");
                    }
                    boolean unused = SemSmdMotionDetector.this.mIsMoved = false;
                } else if (action == 1) {
                    if (SemSmdMotionDetector.DBG) {
                        Log.d(SemSmdMotionDetector.TAG, "SMD detect : action");
                    }
                    boolean unused2 = SemSmdMotionDetector.this.mIsMoved = true;
                }
            }
        }
    };
    private SContextManager mSContextManager = null;

    public static synchronized SemSmdMotionDetector getInstance(Context context) {
        SemSmdMotionDetector semSmdMotionDetector;
        synchronized (SemSmdMotionDetector.class) {
            if (sInstance == null) {
                sInstance = new SemSmdMotionDetector(context);
            }
            semSmdMotionDetector = sInstance;
        }
        return semSmdMotionDetector;
    }

    private SemSmdMotionDetector(Context context) {
        this.mContext = context;
        if (context.getPackageManager().hasSystemFeature("com.sec.feature.sensorhub")) {
            this.mSContextManager = (SContextManager) context.getSystemService("scontext");
            SContextManager sContextManager = this.mSContextManager;
            if (sContextManager != null && sContextManager.isAvailableService(50)) {
                this.mIsAnyMotionDetectorSupported = true;
                this.mCheckSMDSetting = true;
                this.mHandlerThread = new HandlerThread("AnyMotionDetector_wifi");
                this.mHandlerThread.start();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.location.MODE_CHANGED");
                filter.addAction("android.intent.action.AIRPLANE_MODE");
                context.registerReceiver(this.mReceiver, filter);
            }
        }
    }

    public boolean isSupported() {
        return this.mIsAnyMotionDetectorSupported;
    }

    public boolean isEnabled() {
        return this.mIsAnyMotionDetectorSupported && this.mCheckSMDSetting;
    }

    public void startMonitoring() {
        registerSensorMonitor();
    }

    public void stopMonitoring() {
        unregisterSensorMonitor();
    }

    public void resetTimer(long now) {
        this.mLastNLPScanRequestTimeForSMDRegi = now;
    }

    public void checkAndStopMonitoring() {
        if (this.mIsAnyMotionDetectorSupported && this.mIsRegisteredSMDListener) {
            long diff = SystemClock.elapsedRealtime() - this.mLastNLPScanRequestTimeForSMDRegi;
            Log.i(TAG, "checkAndStopMonitoring diff:" + diff);
            if (diff > MAX_REGISTERATION_SENSOR_TIME) {
                unregisterSensorMonitor();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("SMD listener state : ");
            sb.append(this.mIsRegisteredSMDListener ? "registered" : "unregistered");
            Log.i(TAG, sb.toString());
        }
    }

    public boolean setEnable(boolean enabled) {
        if (!this.mIsAnyMotionDetectorSupported) {
            return false;
        }
        this.mCheckSMDSetting = enabled;
        if (enabled || !this.mIsRegisteredSMDListener) {
            return true;
        }
        unregisterSensorMonitor();
        return true;
    }

    private boolean registerSensorMonitor() {
        if (!this.mIsAnyMotionDetectorSupported || !this.mCheckSMDSetting || this.mIsRegisteredSMDListener) {
            return false;
        }
        if (!isLocationEnabled()) {
            Log.i(TAG, "location is disabled");
            return false;
        } else if (getPersistedAirplaneModeOn()) {
            Log.i(TAG, "airplain mode enabled");
            return false;
        } else {
            Log.i(TAG, "register SMD listener");
            this.mIsRegisteredSMDListener = true;
            this.mLastNLPScanRequestTimeForSMDRegi = SystemClock.elapsedRealtime();
            this.mSContextManager.registerListener(this.mSContextListener, 50, this.mHandlerThread.getLooper());
            return true;
        }
    }

    /* access modifiers changed from: private */
    public void unregisterSensorMonitor() {
        if (this.mIsAnyMotionDetectorSupported && this.mIsRegisteredSMDListener) {
            Log.i(TAG, "unregister SMD listener");
            this.mSContextManager.unregisterListener(this.mSContextListener, 50);
            this.mIsRegisteredSMDListener = false;
        }
    }

    public boolean getMovingStatus() {
        if (this.mIsRegisteredSMDListener) {
            long now = SystemClock.elapsedRealtime();
            long diff = now - this.mLastRequestTimeForGettingSMDStatus;
            if (diff < MAX_WAIT_TIME_SENSOR_STATUS) {
                if (DBG) {
                    Log.i(TAG, "ignore to call AMD api. diff: " + diff);
                }
                return this.mIsMoved;
            }
            this.mIsMoved = true;
            this.mSContextManager.requestToUpdate(this.mSContextListener, 50);
            this.mLastRequestTimeForGettingSMDStatus = now;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            return this.mIsMoved;
        }
        startMonitoring();
        return true;
    }

    /* access modifiers changed from: private */
    public boolean isLocationEnabled() {
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "location_mode", 0, ActivityManager.getCurrentUser()) != 0) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean getPersistedAirplaneModeOn() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }
}
