package com.android.server.wifi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Log;
import com.android.server.wifi.WifiEleGeoMagnetic;
import com.android.server.wifi.iwc.IWCEventManager;
import com.samsung.android.hardware.context.SemContextManager;
import com.samsung.android.server.wifi.SemSarManager;
import java.util.Timer;
import java.util.TimerTask;

public class WifiEleStateTracker {
    private static final int ELE_CHECK_BEACON_MISS = 2;
    private static final int ELE_CHECK_BEACON_NONE = 0;
    private static final int ELE_CHECK_BEACON_SUDDEN_DROP = 1;
    public static final int ELE_CHECK_RESULT_DETECTED_GEOMAGNETIC = 1;
    public static final int ELE_CHECK_RESULT_DETECTED_NONE = 0;
    public static final int ELE_CHECK_RESULT_DETECTED_RSSI = 2;
    private static final int ELE_EXPIRE_COUNT = 180;
    private static final int ELE_MINIMUM_RSSI = -70;
    private static final int ELE_PREVIOUS_CHECK_CNT = 6;
    private static final String TAG = "WifiEleStateTracker";
    private static boolean mEleIsScanRunning = false;
    private static long mEleStepExpireMSTime = 180000;
    private static WifiPedometerChecker mWifiPedometerChecker = null;
    private boolean mBlockUntilNewAssoc = false;
    private Context mContext;
    private boolean mEleAggTxBadDetection = false;
    private int mEleBcnCheckingPrevState = 0;
    private int mEleBcnCheckingState = 0;
    private int mEleBcnDropExpireCnt = 0;
    private int mEleBcnHistoryCnt = 0;
    private int mEleBcnMissExpireCnt = 0;
    private int mEleBigSignalChangeExpirationCnt = 0;
    /* access modifiers changed from: private */
    public boolean mEleBlockRoamConnection = false;
    /* access modifiers changed from: private */
    public Timer mEleBlockRoamTimer = null;
    private boolean mEleDetectionPending = false;
    private int mEleDoorOpenCheckCount = 0;
    private boolean mEleEnableMobileRssiPolling = false;
    private int mEleExpireCount = ELE_EXPIRE_COUNT;
    private boolean mEleGeoAvailable = false;
    private boolean mEleGeoEnabled = false;
    WifiEleGeoMagnetic mEleGeoMagnetic = null;
    WifiEleGeoMagnetic.OnEleDetectInterface mEleGeoMagneticAction = null;
    private int mEleInvalidByLegacyDelayCheckCnt = 0;
    private boolean mEleIsStepPending = false;
    private boolean mElePollingSkip = true;
    private int mElePrevBcnCnt = -1;
    private int[] mElePrevBcnDiff = new int[6];
    private int mElePrevBcnDropCond = 0;
    private boolean[] mElePrevGeoMagneticChanges = new boolean[6];
    private int[] mElePrevMobileRssi = new int[6];
    private boolean[] mElePrevStepState = new boolean[6];
    private int mElePrevTxBadCnt = -1;
    private int[] mElePrevWifiRssi = new int[6];
    private long mEleRecentStepCntChangeTime = 0;
    private boolean[] mEleScanHistory = new boolean[6];
    private int mEleStableCount = 3;
    private int mEleStepExpirationCnt = 0;
    private int mEleTwoBigSignalChangeExpirationCnt = 0;
    private boolean[] mEleTxBadHistory = new boolean[6];
    private boolean mGeomagneticEleState = false;
    private boolean mPrevGeomagneticEleState = false;
    private long mPrevStepCnt = 0;
    private String mPreviousBssid = "";
    private boolean mScreenOffResetRequired = false;
    private final SemSarManager mSemSarManager;
    /* access modifiers changed from: private */
    public WifiConnectivityMonitor mWifiConnectivityMonitor;
    private WifiNative mWifiNative;

    public WifiEleStateTracker(Context context, WifiNative wifiNative, WifiConnectivityMonitor wificonnectivityMonitor) {
        this.mContext = context;
        this.mWifiNative = wifiNative;
        this.mWifiConnectivityMonitor = wificonnectivityMonitor;
        mWifiPedometerChecker = new WifiPedometerChecker(this.mContext);
        this.mSemSarManager = new SemSarManager(this.mContext, this.mWifiNative);
        this.mEleGeoMagnetic = new WifiEleGeoMagnetic(this.mContext);
        this.mEleGeoMagneticAction = new WifiEleGeoMagnetic.OnEleDetectInterface() {
            public void onEleDetect() {
                WifiEleStateTracker.this.onEleGeoMagneticChange(true);
            }

            public void onEleNotDetect() {
                WifiEleStateTracker.this.onEleGeoMagneticChange(false);
            }
        };
        WifiEleGeoMagnetic wifiEleGeoMagnetic = this.mEleGeoMagnetic;
        if (wifiEleGeoMagnetic != null && wifiEleGeoMagnetic.getAPISupportFlag() && this.mEleGeoMagneticAction != null) {
            this.mEleGeoAvailable = true;
        }
    }

    public static boolean checkPedometerSensorAvailable(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        if (sensorManager == null || sensorManager.getDefaultSensor(19) == null) {
            return false;
        }
        Log.d(TAG, "STEP_COUNTER type available");
        return true;
    }

    public void registerEleGeomagneticListener() {
        if (this.mEleGeoAvailable && !this.mEleGeoEnabled) {
            Log.d(TAG, " registerEleGeomagneticListener");
            this.mEleGeoMagnetic.registerEleDetector(this.mEleGeoMagneticAction);
            this.mEleGeoEnabled = true;
            Log.d(TAG, " registerEleDetector done");
        }
    }

    public void unregisterEleGeomagneticListener() {
        if (this.mEleGeoAvailable && this.mEleGeoEnabled) {
            Log.d(TAG, " unregisterEleGeomagneticListener");
            this.mEleGeoEnabled = false;
            this.mEleGeoMagnetic.unregisterEleDetector();
            Log.d(TAG, " unregisterEleDetector done");
        }
    }

    public void onEleGeoMagneticChange(boolean detected) {
        if (detected) {
            setGeomagneticEleState(true);
            Log.d(TAG, " GEO Ele TRUE");
            return;
        }
        setGeomagneticEleState(false);
        Log.d(TAG, " GEO Ele FALSE");
    }

    public void setGeomagneticEleState(boolean state) {
        this.mGeomagneticEleState = state;
    }

    public static void setScanState(boolean isStart) {
        mEleIsScanRunning = isStart;
    }

    public void setEleAggTxBadDetection(boolean enable) {
        this.mEleAggTxBadDetection = enable;
    }

    public boolean getElePollingEnabled() {
        return this.mElePollingSkip;
    }

    public void setElePollingSkip(boolean skip) {
        this.mElePollingSkip = skip;
    }

    public boolean checkEleValidBlockState() {
        return this.mEleBlockRoamConnection;
    }

    public void clearEleValidBlockFlag() {
        this.mEleBlockRoamConnection = false;
    }

    public void delayedEleCheckDisable() {
        this.mEleExpireCount = 6;
        this.mEleInvalidByLegacyDelayCheckCnt = 6;
    }

    private void runEleBlockRoamTimer() {
        if (this.mEleBlockRoamTimer != null) {
            Log.i(TAG, "mEleBlockRoamTimer timer cancled");
            this.mEleBlockRoamTimer.cancel();
        }
        this.mEleBlockRoamTimer = new Timer();
        this.mEleBlockRoamTimer.schedule(new TimerTask() {
            public void run() {
                Log.i(WifiEleStateTracker.TAG, "mEleBlockRoamTimer timer expired - enable Roam network valid transition");
                Timer unused = WifiEleStateTracker.this.mEleBlockRoamTimer = null;
                if (WifiEleStateTracker.this.mEleBlockRoamConnection) {
                    WifiEleStateTracker.this.mWifiConnectivityMonitor.enableRecoveryFromEle();
                    Log.d(WifiEleStateTracker.TAG, "CheckEleEnvironment enableRecoveryFromEle delivered ");
                    boolean unused2 = WifiEleStateTracker.this.mEleBlockRoamConnection = false;
                }
            }
        }, IWCEventManager.reconTimeThreshold);
    }

    public void getCurrentStepCnt() {
        WifiPedometerChecker wifiPedometerChecker = mWifiPedometerChecker;
        if (wifiPedometerChecker != null && wifiPedometerChecker.checkPedometerEnabled()) {
            this.mPrevStepCnt = mWifiPedometerChecker.getCurrentTotalStepCnt();
        }
    }

    public void checkStepCntChangeForGeoMagneticSensor() {
        WifiPedometerChecker wifiPedometerChecker;
        if (this.mPrevStepCnt != 0 && (wifiPedometerChecker = mWifiPedometerChecker) != null && wifiPedometerChecker.checkPedometerEnabled() && this.mPrevStepCnt != mWifiPedometerChecker.getCurrentTotalStepCnt()) {
            Log.d(TAG, "Pedometer step movement detected! enable GeoMagnetic Sensor!");
            this.mEleRecentStepCntChangeTime = SystemClock.elapsedRealtime();
            registerEleGeomagneticListener();
        }
    }

    public void enableEleMobileRssiPolling(boolean enable, boolean removeBlock) {
        Log.i(TAG, "enableEleMobileRssiPolling : " + enable);
        enableMobileRssiPolling(enable);
        if (!enable) {
            unregisterEleGeomagneticListener();
        }
        if (removeBlock) {
            this.mBlockUntilNewAssoc = false;
        }
    }

    public boolean getEleCheckDoorOpenState() {
        return this.mEleBlockRoamConnection;
    }

    public boolean getEleCheckEnabled() {
        return this.mEleEnableMobileRssiPolling;
    }

    public void screenSet(boolean isOn) {
        if (isOn) {
            this.mScreenOffResetRequired = true;
        } else {
            this.mScreenOffResetRequired = false;
        }
    }

    public boolean getScreenOffResetRequired() {
        return this.mScreenOffResetRequired;
    }

    public void checkEleDoorOpen(int mobileRssi, int runningBcnCount, int wifiRssi) {
        Log.i(TAG, "CheckEleEnvironment - CheckEleDoorOpen");
        if (this.mEleBcnHistoryCnt == 6) {
            if (this.mEleDoorOpenCheckCount % 6 == 0) {
                printEleFactorsHistory();
            }
            this.mEleDoorOpenCheckCount++;
            if (checkAllStepStateFalseByStationary() && checkDoorOpenMobileSignal(mobileRssi)) {
                Log.i(TAG, "CheckEleEnvironment - door open signal detected");
                runEleBlockRoamTimer();
            }
            shiftEleFactors(mobileRssi, wifiRssi, 0, false, 0, this.mEleIsStepPending, false);
            this.mEleIsStepPending = false;
        }
    }

    private void shiftEleFactors(int mobileRssi, int wifiRssi, int bcnDiff, boolean geoChange, int eleStableCount, boolean stepPending, boolean txBadIncrease) {
        int[] iArr = this.mElePrevMobileRssi;
        System.arraycopy(iArr, 1, iArr, 0, iArr.length - 1);
        int[] iArr2 = this.mElePrevMobileRssi;
        iArr2[iArr2.length - 1] = mobileRssi;
        int[] iArr3 = this.mElePrevWifiRssi;
        System.arraycopy(iArr3, 1, iArr3, 0, iArr3.length - 1);
        int[] iArr4 = this.mElePrevWifiRssi;
        iArr4[iArr4.length - 1] = wifiRssi;
        if (bcnDiff != -1) {
            int i = this.mEleBcnHistoryCnt;
            if (i < 6 && eleStableCount == 0) {
                this.mEleBcnHistoryCnt = i + 1;
            }
            int[] iArr5 = this.mElePrevBcnDiff;
            System.arraycopy(iArr5, 1, iArr5, 0, iArr5.length - 1);
            int[] iArr6 = this.mElePrevBcnDiff;
            iArr6[iArr6.length - 1] = bcnDiff;
        }
        boolean[] zArr = this.mEleScanHistory;
        System.arraycopy(zArr, 1, zArr, 0, zArr.length - 1);
        boolean[] zArr2 = this.mEleScanHistory;
        zArr2[zArr2.length - 1] = mEleIsScanRunning;
        boolean[] zArr3 = this.mEleTxBadHistory;
        System.arraycopy(zArr3, 1, zArr3, 0, zArr3.length - 1);
        boolean[] zArr4 = this.mEleTxBadHistory;
        zArr4[zArr4.length - 1] = txBadIncrease;
        boolean[] zArr5 = this.mElePrevGeoMagneticChanges;
        System.arraycopy(zArr5, 1, zArr5, 0, zArr5.length - 1);
        boolean[] zArr6 = this.mElePrevGeoMagneticChanges;
        zArr6[zArr6.length - 1] = geoChange;
        boolean[] zArr7 = this.mElePrevStepState;
        System.arraycopy(zArr7, 1, zArr7, 0, zArr7.length - 1);
        boolean[] zArr8 = this.mElePrevStepState;
        zArr8[zArr8.length - 1] = stepPending;
    }

    private boolean checkDoorOpenMobileSignal(int currentMobileRssi) {
        for (int baseIndex = 3; baseIndex < 6; baseIndex++) {
            if (currentMobileRssi - this.mElePrevMobileRssi[baseIndex] > 10) {
                return true;
            }
        }
        return false;
    }

    private void enableMobileRssiPolling(boolean enabled) {
        if (enabled) {
            this.mEleExpireCount = ELE_EXPIRE_COUNT;
        }
        if (!this.mEleEnableMobileRssiPolling && enabled) {
            resetEleParameters(0, false, true);
        }
        if (enabled && !this.mEleEnableMobileRssiPolling) {
            Log.i(TAG, "enableMobileRssiPolling true");
        } else if (!enabled && this.mEleEnableMobileRssiPolling) {
            unregisterEleGeomagneticListener();
            this.mWifiConnectivityMonitor.eleCheckFinished();
            Log.i(TAG, "enableMobileRssiPolling false");
        }
        this.mEleEnableMobileRssiPolling = enabled;
    }

    public void resetEleParameters(int stableCount, boolean newBssid, boolean resetHistory) {
        Log.i(TAG, "resetEleParameters");
        if (resetHistory) {
            for (int x = 0; x < 6; x++) {
                this.mElePrevMobileRssi[x] = 0;
                this.mElePrevWifiRssi[x] = 0;
                this.mElePrevBcnDiff[x] = 0;
                this.mEleScanHistory[x] = false;
                this.mEleTxBadHistory[x] = false;
                this.mElePrevGeoMagneticChanges[x] = false;
                this.mElePrevStepState[x] = false;
            }
        }
        this.mElePrevBcnCnt = -1;
        this.mElePrevTxBadCnt = -1;
        this.mEleBcnHistoryCnt = 0;
        this.mEleStableCount = stableCount;
        this.mEleBcnMissExpireCnt = 0;
        this.mElePollingSkip = false;
        this.mGeomagneticEleState = false;
        this.mEleDetectionPending = false;
        this.mEleInvalidByLegacyDelayCheckCnt = 0;
        this.mEleBigSignalChangeExpirationCnt = 0;
        this.mEleTwoBigSignalChangeExpirationCnt = 0;
        this.mEleBcnCheckingPrevState = 0;
        this.mEleBcnDropExpireCnt = 0;
        this.mEleStepExpirationCnt = 0;
        if (newBssid) {
            this.mBlockUntilNewAssoc = false;
        }
    }

    private int getPrevNonZeroBcnCnt() {
        int previousNonZeroBeaconCnt = 0;
        if (this.mEleBcnHistoryCnt != 0) {
            for (int x = 5; x >= 6 - this.mEleBcnHistoryCnt; x--) {
                if (this.mElePrevBcnDiff[x] > 0) {
                    previousNonZeroBeaconCnt++;
                }
            }
        }
        return previousNonZeroBeaconCnt;
    }

    private boolean checkScanHistory() {
        for (int x = 0; x < 6; x++) {
            if (this.mEleScanHistory[x]) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPrevHalfTxBad() {
        for (int x = 0; x < 3; x++) {
            if (this.mEleTxBadHistory[x]) {
                return true;
            }
        }
        return false;
    }

    private int getPrevStableBcnCnt() {
        int previousNonZeroBeaconCnt = 0;
        if (this.mEleBcnHistoryCnt != 0) {
            for (int x = 5; x >= 6 - this.mEleBcnHistoryCnt; x--) {
                if (this.mElePrevBcnDiff[x] > 0) {
                    previousNonZeroBeaconCnt++;
                }
            }
        }
        return previousNonZeroBeaconCnt;
    }

    private int getPrevAverBcnCnt() {
        int totalBeaconCount = 0;
        if (this.mEleBcnHistoryCnt == 0) {
            return 0;
        }
        int x = 5;
        while (true) {
            int i = this.mEleBcnHistoryCnt;
            if (x < 6 - i) {
                return totalBeaconCount / i;
            }
            totalBeaconCount += this.mElePrevBcnDiff[x];
            x--;
        }
    }

    private int getMaxBcnDiff() {
        int maxBcnDiff = 0;
        if (this.mEleBcnHistoryCnt == 0) {
            return 0;
        }
        for (int x = 5; x >= 6 - this.mEleBcnHistoryCnt; x--) {
            int[] iArr = this.mElePrevBcnDiff;
            if (maxBcnDiff < iArr[x]) {
                maxBcnDiff = iArr[x];
            }
        }
        return maxBcnDiff;
    }

    private boolean checkAllStepStateFalseByStationary() {
        if (this.mEleIsStepPending) {
            return false;
        }
        for (int x = 1; x < 6; x++) {
            if (this.mElePrevStepState[x]) {
                return false;
            }
        }
        return true;
    }

    private boolean checkMobileSignalChange() {
        boolean bMobileBigLoss = false;
        for (int baseIndex = 1; baseIndex < 5; baseIndex++) {
            int index = baseIndex;
            while (true) {
                if (index >= 6) {
                    break;
                }
                int[] iArr = this.mElePrevMobileRssi;
                int diffValue = iArr[0] - iArr[index];
                if (diffValue >= 9 || diffValue <= -9) {
                    bMobileBigLoss = true;
                } else {
                    index++;
                }
            }
            bMobileBigLoss = true;
        }
        return bMobileBigLoss;
    }

    private boolean checkWifiSignalChange() {
        boolean bWifiBigLoss = false;
        for (int baseIndex = 1; baseIndex < 5; baseIndex++) {
            int index = baseIndex;
            while (true) {
                if (index >= 6) {
                    break;
                }
                int[] iArr = this.mElePrevWifiRssi;
                if (iArr[0] - iArr[index] >= 9) {
                    bWifiBigLoss = true;
                    break;
                }
                index++;
            }
        }
        return bWifiBigLoss;
    }

    private int eleDetected(int bcnDiff, int mobileRssi, int wifiRssi) {
        int retVal;
        WifiManager wifiManager;
        boolean bBssidChange = false;
        long currentTime = SystemClock.elapsedRealtime();
        if (this.mEleStepExpirationCnt <= 0) {
            long j = this.mEleRecentStepCntChangeTime;
            if (j <= 0 || currentTime - j >= mEleStepExpireMSTime) {
                Log.i(TAG, "Ele detection ignored, limit time passed : " + ((currentTime - this.mEleRecentStepCntChangeTime) / 1000));
                return 0;
            }
        }
        Log.i(TAG, "CheckEleEnvironment Elapsed seconds after last WALK : " + (currentTime - this.mEleRecentStepCntChangeTime) + " mEleStepExpirationCnt : " + this.mEleStepExpirationCnt);
        Context context = this.mContext;
        if (!(context == null || (wifiManager = (WifiManager) context.getSystemService("wifi")) == null)) {
            String currentBssid = wifiManager.getConnectionInfo().getBSSID();
            Log.i(TAG, "CheckEleEnvironment detected BSSID : " + currentBssid + " previousBSSID :" + this.mPreviousBssid);
            if (!currentBssid.equals("") && !this.mPreviousBssid.equals("") && currentBssid.equals(this.mPreviousBssid)) {
                bBssidChange = true;
            }
            this.mPreviousBssid = currentBssid;
        }
        if (!bBssidChange && this.mBlockUntilNewAssoc) {
            Log.i(TAG, "CheckEleEnvironment - mBlockUntilNewAssoc is ture. it could be continous ELE pattern.");
        } else if (this.mSemSarManager.isBodySarGrip()) {
            Log.i(TAG, "CheckEleEnvironment - isBodySarGrip true. eleDetection blocked.");
        } else {
            this.mBlockUntilNewAssoc = true;
            if (this.mGeomagneticEleState) {
                retVal = 1;
            } else {
                retVal = 2;
            }
            Log.i(TAG, "CheckEleEnvironment detection BD : " + this.mElePrevBcnDiff[0] + " " + this.mElePrevBcnDiff[1] + " " + this.mElePrevBcnDiff[2] + " " + this.mElePrevBcnDiff[3] + " " + this.mElePrevBcnDiff[4] + " " + this.mElePrevBcnDiff[5] + " " + bcnDiff + " MD : " + this.mElePrevMobileRssi[0] + " " + this.mElePrevMobileRssi[1] + " " + this.mElePrevMobileRssi[2] + " " + this.mElePrevMobileRssi[3] + " " + this.mElePrevMobileRssi[4] + " " + this.mElePrevMobileRssi[5] + " " + mobileRssi + " WD : " + this.mElePrevWifiRssi[0] + " " + this.mElePrevWifiRssi[1] + " " + this.mElePrevWifiRssi[2] + " " + this.mElePrevWifiRssi[3] + " " + this.mElePrevWifiRssi[4] + " " + this.mElePrevWifiRssi[5] + " " + wifiRssi + " GC : " + this.mElePrevGeoMagneticChanges[0] + " " + this.mElePrevGeoMagneticChanges[1] + " " + this.mElePrevGeoMagneticChanges[2] + " " + this.mElePrevGeoMagneticChanges[3] + " " + this.mElePrevGeoMagneticChanges[4] + " " + this.mElePrevGeoMagneticChanges[5] + " SC : " + this.mElePrevStepState[0] + " " + this.mElePrevStepState[1] + " " + this.mElePrevStepState[2] + " " + this.mElePrevStepState[3] + " " + this.mElePrevStepState[4] + " " + this.mElePrevStepState[5] + "  SH : " + this.mEleScanHistory[0] + " " + this.mEleScanHistory[1] + " " + this.mEleScanHistory[2] + " " + this.mEleScanHistory[3] + " " + this.mEleScanHistory[4] + " " + this.mEleScanHistory[5] + "  TB : " + this.mEleTxBadHistory[0] + " " + this.mEleTxBadHistory[1] + " " + this.mEleTxBadHistory[2] + " " + this.mEleTxBadHistory[3] + " " + this.mEleTxBadHistory[4] + " " + this.mEleTxBadHistory[5]);
            enableMobileRssiPolling(false);
            this.mEleBlockRoamConnection = true;
            runEleBlockRoamTimer();
            this.mEleDoorOpenCheckCount = 0;
            return retVal;
        }
        return 0;
    }

    private void printEleFactorsHistory() {
        Log.i(TAG, "CheckEleEnvironment BD : " + this.mElePrevBcnDiff[0] + " " + this.mElePrevBcnDiff[1] + " " + this.mElePrevBcnDiff[2] + " " + this.mElePrevBcnDiff[3] + " " + this.mElePrevBcnDiff[4] + " " + this.mElePrevBcnDiff[5] + " MD : " + this.mElePrevMobileRssi[0] + " " + this.mElePrevMobileRssi[1] + " " + this.mElePrevMobileRssi[2] + " " + this.mElePrevMobileRssi[3] + " " + this.mElePrevMobileRssi[4] + " " + this.mElePrevMobileRssi[5] + " WD : " + this.mElePrevWifiRssi[0] + " " + this.mElePrevWifiRssi[1] + " " + this.mElePrevWifiRssi[2] + " " + this.mElePrevWifiRssi[3] + " " + this.mElePrevWifiRssi[4] + " " + this.mElePrevWifiRssi[5] + " GC : " + this.mElePrevGeoMagneticChanges[0] + " " + this.mElePrevGeoMagneticChanges[1] + " " + this.mElePrevGeoMagneticChanges[2] + " " + this.mElePrevGeoMagneticChanges[3] + " " + this.mElePrevGeoMagneticChanges[4] + " " + this.mElePrevGeoMagneticChanges[5] + " SC : " + this.mElePrevStepState[0] + " " + this.mElePrevStepState[1] + " " + this.mElePrevStepState[2] + " " + this.mElePrevStepState[3] + " " + this.mElePrevStepState[4] + " " + this.mElePrevStepState[5] + " SH : " + this.mEleScanHistory[0] + " " + this.mEleScanHistory[1] + " " + this.mEleScanHistory[2] + " " + this.mEleScanHistory[3] + " " + this.mEleScanHistory[4] + " " + this.mEleScanHistory[5] + " TB : " + this.mEleTxBadHistory[0] + " " + this.mEleTxBadHistory[1] + " " + this.mEleTxBadHistory[2] + " " + this.mEleTxBadHistory[3] + " " + this.mEleTxBadHistory[4] + " " + this.mEleTxBadHistory[5]);
    }

    private int getBcnCheckingState(int previousNonZeroBeaconCnt, int previousStableBeaconCnt, int prevAverBcnCnt, int bcnDiff, boolean txbadIncrease) {
        int checkingState = 0;
        if (previousNonZeroBeaconCnt == 6) {
            Log.d(TAG, "CheckEleEnvironment - previousNonZeroBeaconCnt Non Zero Beacon Count Condition!");
            if (bcnDiff == 0 && previousStableBeaconCnt >= 4) {
                checkingState = 2;
                this.mEleBcnMissExpireCnt = 5;
            } else if (this.mEleBcnCheckingPrevState != 1) {
                Log.i(TAG, "CheckEleEnvironment - Sudden Drop checking prevAverBcnCnt : " + prevAverBcnCnt);
                if (prevAverBcnCnt >= 9) {
                    if (bcnDiff <= 3) {
                        this.mElePrevBcnDropCond = 3;
                        checkingState = 1;
                    }
                } else if (prevAverBcnCnt >= 7) {
                    if (bcnDiff <= 2) {
                        this.mElePrevBcnDropCond = 2;
                        checkingState = 1;
                    }
                } else if (prevAverBcnCnt >= 5 && getMaxBcnDiff() <= 7 && !checkScanHistory() && txbadIncrease && bcnDiff <= 1) {
                    this.mElePrevBcnDropCond = 1;
                    checkingState = 1;
                }
            } else if (this.mElePrevBcnDropCond >= bcnDiff) {
                Log.d(TAG, "CheckEleEnvironment - Sudden Drop continue!");
                checkingState = 1;
                this.mEleBcnDropExpireCnt--;
            } else {
                Log.d(TAG, "CheckEleEnvironment - Sudden Drop finished.");
                this.mEleBcnDropExpireCnt = 0;
            }
        } else if (bcnDiff != 0 || this.mEleBcnMissExpireCnt <= 0) {
            this.mEleDetectionPending = false;
        } else {
            Log.d(TAG, "CheckEleEnvironment - already in beacon miss in progress!  mEleBcnMissExpireCnt : " + this.mEleBcnMissExpireCnt);
            checkingState = 2;
            this.mEleBcnMissExpireCnt = this.mEleBcnMissExpireCnt - 1;
        }
        if (bcnDiff == 0) {
            this.mEleBcnDropExpireCnt = 0;
        }
        this.mEleBcnCheckingPrevState = checkingState;
        return checkingState;
    }

    private boolean checkGeoMagneticRecentChange() {
        if (!this.mPrevGeomagneticEleState && this.mGeomagneticEleState) {
            return true;
        }
        for (int x = 1; x < 6; x++) {
            if (this.mElePrevGeoMagneticChanges[x]) {
                return true;
            }
        }
        return false;
    }

    private boolean getWifiRssiChangeWithTimeDiff(int wifiRssi, int secondsScope, int diffCond) {
        int difference = 0;
        int index = 6 - secondsScope;
        int[] iArr = this.mElePrevWifiRssi;
        if (iArr[index] == 0) {
            return false;
        }
        if (iArr[index] != 0) {
            difference = (-wifiRssi) - (-iArr[index]);
        }
        if (difference < diffCond) {
            return false;
        }
        Log.i(TAG, "CheckEleEnvironment - getWifiRssiChangeWithTimeDiff - wifiRssi : " + wifiRssi + " secondsCope :" + secondsScope + " diffCond :" + diffCond);
        return true;
    }

    private boolean getMobileRssiChangeWithTimeDiff(int mobileRssi, int secondsScope, int diffCond) {
        int difference = 0;
        int index = 6 - secondsScope;
        int[] iArr = this.mElePrevMobileRssi;
        if (iArr[index] == 0) {
            return false;
        }
        if (iArr[index] != 0) {
            difference = (-mobileRssi) - (-iArr[index]);
        }
        if (difference < diffCond) {
            return false;
        }
        Log.i(TAG, "CheckEleEnvironment - getMobileRssiChangeWithTimeDiff - mobileRssi : " + mobileRssi + " secondsCope :" + secondsScope + " diffCond :" + diffCond);
        return true;
    }

    private boolean checkContiousMobileRssiDecrease(int mobileRssi) {
        int[] iArr = this.mElePrevMobileRssi;
        if (iArr[0] == 0 || iArr[0] < iArr[1] || iArr[1] < iArr[2] || iArr[2] < iArr[3] || iArr[3] < iArr[4] || iArr[4] < iArr[5] || iArr[5] < mobileRssi) {
            return false;
        }
        Log.i(TAG, "CheckEleEnvironment - checkContiousMobileRssiDecrease true");
        return true;
    }

    private int checkEleEnvironmentConfirmFactors(int bcnDiff, int mobileRssi, int wifiRssi, int previousNonZeroBeaconCnt, int previousStableBeaconCnt, int prevAverBcnCnt, boolean txbadIncrease) {
        int i = mobileRssi;
        int i2 = wifiRssi;
        boolean bTwoBigSignalChangeWithStationary = false;
        boolean bStationaryForFiveSeconds = checkAllStepStateFalseByStationary();
        if (bStationaryForFiveSeconds) {
            boolean bMobileBigChange = checkMobileSignalChange();
            boolean bWifiBigChange = checkWifiSignalChange();
            if (bMobileBigChange && bWifiBigChange) {
                bTwoBigSignalChangeWithStationary = true;
                this.mEleTwoBigSignalChangeExpirationCnt = 4;
                Log.i(TAG, "CheckEleEnvironment - bTwoBigSignalChangeWithStationary!!");
            }
            if (bMobileBigChange || bWifiBigChange) {
                this.mEleBigSignalChangeExpirationCnt = 4;
                Log.i(TAG, "CheckEleEnvironment - bBigSignalLossDetected!!");
                if (this.mEleInvalidByLegacyDelayCheckCnt > 0) {
                    Log.i(TAG, "CheckEleEnvironment - Ele detection with prev Invalid and big signal change");
                    return eleDetected(bcnDiff, mobileRssi, wifiRssi);
                }
                boolean z = bTwoBigSignalChangeWithStationary;
            } else {
                boolean z2 = bTwoBigSignalChangeWithStationary;
            }
        }
        if (bcnDiff != 0 || !bStationaryForFiveSeconds) {
            int i3 = previousNonZeroBeaconCnt;
            int i4 = previousStableBeaconCnt;
            int i5 = prevAverBcnCnt;
        } else {
            Log.d(TAG, "CheckEleEnvironment - bcnDiff Zero! mEleExpireCount : " + this.mEleExpireCount + " wifiRssi : " + i2 + " mElePrevWifiRssi[0] : " + this.mElePrevWifiRssi[0] + " previousNonZeroBeaconCnt : " + previousNonZeroBeaconCnt + " previousStableBeaconCnt : " + previousStableBeaconCnt + " prevAverBcnCnt : " + prevAverBcnCnt);
            if (this.mEleTwoBigSignalChangeExpirationCnt > 0) {
                Log.i(TAG, "CheckEleEnvironment - Ele detection by bTwoBigSignalChangeWithStationary");
                return eleDetected(bcnDiff, mobileRssi, wifiRssi);
            } else if (this.mEleBigSignalChangeExpirationCnt > 0 && this.mGeomagneticEleState) {
                return eleDetected(bcnDiff, mobileRssi, wifiRssi);
            }
        }
        this.mEleBcnCheckingState = getBcnCheckingState(previousNonZeroBeaconCnt, previousStableBeaconCnt, prevAverBcnCnt, bcnDiff, txbadIncrease);
        int i6 = this.mEleBcnCheckingState;
        if (i6 != 2 && i6 != 1) {
            return 0;
        }
        Log.i(TAG, "CheckEleEnvironment - Beacon loss checking starteded!");
        if ((!this.mGeomagneticEleState || !checkGeoMagneticRecentChange()) && !this.mEleDetectionPending && ((this.mEleBigSignalChangeExpirationCnt <= 0 || !bStationaryForFiveSeconds) && !getMobileRssiChangeWithTimeDiff(i, 3, 5) && !getMobileRssiChangeWithTimeDiff(i, 4, 6) && ((!getMobileRssiChangeWithTimeDiff(i, 6, 8) || i2 >= this.mElePrevWifiRssi[0]) && ((!checkContiousMobileRssiDecrease(i) || !getMobileRssiChangeWithTimeDiff(i, 2, 3) || !getWifiRssiChangeWithTimeDiff(i2, 2, 5)) && (!getMobileRssiChangeWithTimeDiff(i, 6, 11) || i2 > this.mElePrevWifiRssi[0]))))) {
            return 0;
        }
        if (this.mEleAggTxBadDetection && txbadIncrease && (!checkPrevHalfTxBad() || bcnDiff == 0)) {
            Log.i(TAG, "CheckEleEnvironment - Ele detected! with txbad, bGigSignal Expire Cnt : " + this.mEleBigSignalChangeExpirationCnt);
            return eleDetected(bcnDiff, mobileRssi, wifiRssi);
        } else if ((mEleIsScanRunning || this.mEleIsStepPending) && !this.mGeomagneticEleState) {
            this.mEleDetectionPending = true;
            if (mEleIsScanRunning) {
                Log.i(TAG, "CheckEleEnvironment - Beacon loss ignored by ScanRunning");
                return 0;
            }
            Log.i(TAG, "CheckEleEnvironment - Beacon loss ignored by Step Cnt Pending");
            return 0;
        } else {
            Log.i(TAG, "CheckEleEnvironment - Ele detected! bBigSignal Expire Cnt : " + this.mEleBigSignalChangeExpirationCnt);
            return eleDetected(bcnDiff, mobileRssi, wifiRssi);
        }
    }

    public void registerElePedometer() {
        WifiPedometerChecker wifiPedometerChecker = mWifiPedometerChecker;
        if (wifiPedometerChecker != null && wifiPedometerChecker.mIsEnabled) {
            mWifiPedometerChecker.registerPedometer();
        }
    }

    public void unregisterElePedometer() {
        WifiPedometerChecker wifiPedometerChecker = mWifiPedometerChecker;
        if (wifiPedometerChecker != null && wifiPedometerChecker.mIsEnabled) {
            mWifiPedometerChecker.unregisterPedometer();
        }
    }

    public int checkEleEnvironment(int mobileRssi, int runningBcnCount, int wifiRssi, int txBad) {
        int bcnDiff;
        boolean bTxBadIncrease;
        int bcnDiff2;
        int i = runningBcnCount;
        int i2 = wifiRssi;
        int i3 = txBad;
        int retVal = 0;
        int i4 = this.mEleExpireCount;
        if (i4 % 6 == 0 || i4 == 1) {
            printEleFactorsHistory();
        }
        int i5 = this.mEleStepExpirationCnt;
        if (i5 > 0) {
            this.mEleStepExpirationCnt = i5 - 1;
        }
        int i6 = this.mEleExpireCount;
        this.mEleExpireCount = i6 - 1;
        if (i6 == 0) {
            Log.i(TAG, "CheckEleEnvironment - finished by expiration count");
            enableMobileRssiPolling(false);
            return 0;
        }
        int previousNonZeroBeaconCnt = getPrevNonZeroBcnCnt();
        int previousStableBeaconCnt = getPrevStableBcnCnt();
        int prevAverBcnCnt = getPrevAverBcnCnt();
        int i7 = this.mElePrevBcnCnt;
        if (i7 == -1) {
            bcnDiff = -1;
        } else if (i < 0) {
            resetEleParameters(1, false, true);
            return 0;
        } else {
            int bcnDiff3 = i - i7;
            if (bcnDiff3 <= -1) {
                Log.e(TAG, "CheckEleEnvironment - Abnormal beacon cnt : " + bcnDiff3);
                bcnDiff3 = prevAverBcnCnt;
            }
            if (bcnDiff3 > 13) {
                bcnDiff3 = 13;
            }
            if (bcnDiff3 > 0) {
                this.mEleDetectionPending = false;
            }
            bcnDiff = bcnDiff3;
        }
        int i8 = this.mElePrevTxBadCnt;
        if (i8 == -1) {
            bTxBadIncrease = false;
        } else if (i8 < i3) {
            bTxBadIncrease = true;
        } else {
            bTxBadIncrease = false;
        }
        this.mElePrevTxBadCnt = i3;
        if (i2 > ELE_MINIMUM_RSSI || this.mEleBcnHistoryCnt < 6 || bcnDiff >= 4 || i2 > this.mElePrevWifiRssi[0]) {
            bcnDiff2 = bcnDiff;
        } else {
            bcnDiff2 = bcnDiff;
            retVal = checkEleEnvironmentConfirmFactors(bcnDiff, mobileRssi, wifiRssi, previousNonZeroBeaconCnt, previousStableBeaconCnt, prevAverBcnCnt, bTxBadIncrease);
        }
        shiftEleFactors(mobileRssi, wifiRssi, bcnDiff2, !this.mPrevGeomagneticEleState && this.mGeomagneticEleState, this.mEleStableCount, this.mEleIsStepPending, bTxBadIncrease);
        this.mElePrevBcnCnt = i;
        this.mPrevGeomagneticEleState = this.mGeomagneticEleState;
        int i9 = this.mEleStableCount;
        if (i9 != 0) {
            this.mEleStableCount = i9 - 1;
        }
        int i10 = this.mEleInvalidByLegacyDelayCheckCnt;
        if (i10 != 0) {
            this.mEleInvalidByLegacyDelayCheckCnt = i10 - 1;
        }
        int i11 = this.mEleBigSignalChangeExpirationCnt;
        if (i11 != 0) {
            this.mEleBigSignalChangeExpirationCnt = i11 - 1;
        }
        int i12 = this.mEleTwoBigSignalChangeExpirationCnt;
        if (i12 != 0) {
            this.mEleTwoBigSignalChangeExpirationCnt = i12 - 1;
        }
        this.mEleIsStepPending = false;
        return retVal;
    }

    public void setEleRecentStep() {
        this.mEleIsStepPending = true;
        this.mEleStepExpirationCnt = ELE_EXPIRE_COUNT;
    }

    public boolean checkEleCheckValidTiming() {
        if (this.mEleExpireCount > 5) {
            return true;
        }
        return false;
    }

    public void checkNeedRecoverFromEle() {
        if (this.mEleBlockRoamConnection) {
            Log.d(TAG, "mEleBlockRoamConnection to false by Pedometer.");
            clearEleValidBlockFlag();
            this.mWifiConnectivityMonitor.enableRecoveryFromEle();
        }
    }

    public void checkNeedGeomagnetic() {
        if (this.mEleGeoAvailable && !this.mEleGeoEnabled && checkEleCheckValidTiming()) {
            Log.d(TAG, "registerEleGeomagnetic sensor by Pedometer Walk.");
            registerEleGeomagneticListener();
        }
    }

    private class WifiPedometerChecker implements SensorEventListener {
        private Context mContext;
        /* access modifiers changed from: private */
        public boolean mIsEnabled = false;
        private boolean mListenerEnabled = false;
        private SemContextManager mSemContextManager = null;
        private SensorManager mSensorManager = null;
        private Sensor mStepCountSensor = null;
        private long mTotalStepCount;

        public WifiPedometerChecker(Context context) {
            this.mContext = context;
            this.mSensorManager = (SensorManager) context.getSystemService("sensor");
            SensorManager sensorManager = this.mSensorManager;
            if (sensorManager != null) {
                this.mStepCountSensor = sensorManager.getDefaultSensor(19);
                if (this.mStepCountSensor != null) {
                    this.mIsEnabled = true;
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == 19) {
                this.mTotalStepCount = (long) event.values[0];
                Log.d(WifiEleStateTracker.TAG, "onSensorChanged mTotalStepCount : " + this.mTotalStepCount);
                WifiEleStateTracker.this.setEleRecentStep();
                WifiEleStateTracker.this.checkNeedRecoverFromEle();
                WifiEleStateTracker.this.checkNeedGeomagnetic();
            }
        }

        public long getCurrentTotalStepCnt() {
            Log.d(WifiEleStateTracker.TAG, "pedometer.getCurrentTotalStepCnt() : " + this.mTotalStepCount);
            return this.mTotalStepCount;
        }

        public boolean checkPedometerEnabled() {
            return this.mListenerEnabled;
        }

        public void unregisterPedometer() {
            if (this.mIsEnabled && this.mListenerEnabled) {
                this.mSensorManager.unregisterListener(this, this.mStepCountSensor);
                this.mListenerEnabled = false;
            }
        }

        public void registerPedometer() {
            if (this.mIsEnabled && !this.mListenerEnabled) {
                this.mSensorManager.registerListener(this, this.mStepCountSensor, 3);
                this.mListenerEnabled = true;
            }
        }
    }
}
