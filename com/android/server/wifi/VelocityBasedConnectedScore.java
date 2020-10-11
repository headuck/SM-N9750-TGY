package com.android.server.wifi;

import android.net.wifi.WifiInfo;
import com.android.server.wifi.util.KalmanFilter;
import com.android.server.wifi.util.Matrix;

public class VelocityBasedConnectedScore extends ConnectedScore {
    private double mEstimatedRateOfRssiChange;
    private final KalmanFilter mFilter;
    private double mFilteredRssi;
    private int mFrequency = 5000;
    private long mLastMillis;
    private double mMinimumPpsForMeasuringSuccess = 2.0d;
    private final ScoringParams mScoringParams;
    private double mThresholdAdjustment;

    public VelocityBasedConnectedScore(ScoringParams scoringParams, Clock clock) {
        super(clock);
        this.mScoringParams = scoringParams;
        this.mFilter = new KalmanFilter();
        this.mFilter.f36mH = new Matrix(2, new double[]{1.0d, 0.0d});
        this.mFilter.f39mR = new Matrix(1, new double[]{1.0d});
    }

    private void setDeltaTimeSeconds(double dt) {
        this.mFilter.f35mF = new Matrix(2, new double[]{1.0d, dt, 0.0d, 1.0d});
        Matrix tG = new Matrix(1, new double[]{0.5d * dt * dt, dt});
        this.mFilter.f38mQ = tG.dotTranspose(tG).dot(new Matrix(2, new double[]{0.02d * 0.02d, 0.0d, 0.0d, 0.02d * 0.02d}));
    }

    public void reset() {
        this.mLastMillis = 0;
        this.mThresholdAdjustment = 0.0d;
        this.mFilter.f40mx = null;
    }

    public void updateUsingRssi(int rssi, long millis, double standardDeviation) {
        int i = rssi;
        long j = millis;
        if (j > 0) {
            long j2 = this.mLastMillis;
            if (j2 <= 0 || j < j2 || this.mFilter.f40mx == null) {
                this.mFilter.f40mx = new Matrix(1, new double[]{(double) i, 0.0d});
                this.mFilter.f37mP = new Matrix(2, new double[]{9.0d * standardDeviation * standardDeviation, 0.0d, 0.0d, 0.0d});
            } else {
                this.mFilter.f39mR.put(0, 0, standardDeviation * standardDeviation);
                setDeltaTimeSeconds(((double) (j - this.mLastMillis)) * 0.001d);
                this.mFilter.predict();
                this.mFilter.update(new Matrix(1, new double[]{(double) i}));
            }
            this.mLastMillis = j;
            this.mFilteredRssi = this.mFilter.f40mx.get(0, 0);
            this.mEstimatedRateOfRssiChange = this.mFilter.f40mx.get(1, 0);
        }
    }

    public void updateUsingWifiInfo(WifiInfo wifiInfo, long millis) {
        int frequency = wifiInfo.getFrequency();
        if (frequency != this.mFrequency) {
            this.mLastMillis = 0;
            this.mFrequency = frequency;
        }
        updateUsingRssi(wifiInfo.getRssi(), millis, this.mDefaultRssiStandardDeviation);
        adjustThreshold(wifiInfo);
    }

    public double getFilteredRssi() {
        return this.mFilteredRssi;
    }

    public double getEstimatedRateOfRssiChange() {
        return this.mEstimatedRateOfRssiChange;
    }

    public double getAdjustedRssiThreshold() {
        return ((double) this.mScoringParams.getExitRssi(this.mFrequency)) + this.mThresholdAdjustment;
    }

    private void adjustThreshold(WifiInfo wifiInfo) {
        if (this.mThresholdAdjustment >= -7.0d && this.mFilteredRssi < getAdjustedRssiThreshold() + 2.0d && Math.abs(this.mEstimatedRateOfRssiChange) < 0.2d) {
            double txSuccessPps = wifiInfo.txSuccessRate;
            double rxSuccessPps = wifiInfo.rxSuccessRate;
            double d = this.mMinimumPpsForMeasuringSuccess;
            if (txSuccessPps >= d && rxSuccessPps >= d) {
                if (txSuccessPps / ((txSuccessPps + wifiInfo.txBadRate) + wifiInfo.txRetriesRate) > 0.2d) {
                    this.mThresholdAdjustment -= 0.5d;
                }
            }
        }
    }

    public int generateScore() {
        if (this.mFilter.f40mx == null) {
            return 51;
        }
        double badRssi = getAdjustedRssiThreshold();
        Matrix x = new Matrix(this.mFilter.f40mx);
        double filteredRssi = x.get(0, 0);
        setDeltaTimeSeconds((double) this.mScoringParams.getHorizonSeconds());
        double forecastRssi = this.mFilter.f35mF.dot(x).get(0, 0);
        if (forecastRssi > filteredRssi) {
            forecastRssi = filteredRssi;
        }
        return ((int) (((double) Math.round(forecastRssi)) - badRssi)) + 50;
    }
}
