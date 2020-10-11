package com.android.server.wifi.iwc;

import android.util.Log;

public class IWCDataRateTraceData {
    private static final int DURATION_FOR_SET_HDR = 5;
    private static final int DURATION_FOR_SET_LDR = 25;
    private static final int DURATION_FOR_UNSET_HDR = 2;
    private static final int DURATION_FOR_UNSET_LDR = 10;
    private static final long HDR_SET_THRESHOLD = 1250000;
    private static final long HDR_UNSET_THRESHOLD = 1250000;
    public static final int IWC_SAVER_STATE_DISABLED = 0;
    public static final int IWC_SAVER_STATE_HDR_ENABLED = 1;
    public static final int IWC_SAVER_STATE_LDR_ENABLED = 2;
    private static final long LDR_SET_THRESHOLD = 125000;
    private static final long LDR_UNSET_THRESHOLD = 125000;
    private static final int MAX_RX_BUF_SIZE = 30;
    private static final int MAX_TX_BUF_SIZE = 30;
    private static final String TAG = "IWCMonitor.IWCDataRateTraceData";
    private boolean DBG = false;
    private int mOverCntForHDR;
    private int mOverCntForLDR;
    private long[] mRxBuf = new long[30];
    private int mRxBufIdx;
    private long[] mTxBuf = new long[30];
    private int mTxBufIdx;
    private int mUnderCntForHDR;
    private int mUnderCntForLDR;

    public IWCDataRateTraceData() {
        resetBufsAndCnts();
    }

    public void setDebugMode(boolean dbg) {
        this.DBG = dbg;
        Log.d(TAG, "Debug mode - " + this.DBG);
    }

    public void resetBufsAndCnts() {
        for (int i = 0; i < 30; i++) {
            this.mTxBuf[i] = 0;
        }
        for (int i2 = 0; i2 < 30; i2++) {
            this.mRxBuf[i2] = 0;
        }
        this.mTxBufIdx = 0;
        this.mRxBufIdx = 0;
        this.mOverCntForLDR = 0;
        this.mOverCntForHDR = 0;
        this.mUnderCntForLDR = 0;
        this.mUnderCntForHDR = 0;
    }

    private long getTxValueWithIdx(int idx) {
        int tempIdx;
        int i = this.mTxBufIdx;
        if (i <= 0 || (i - idx) - 1 < 0) {
            tempIdx = ((this.mTxBufIdx - idx) - 1) + 30;
        } else {
            tempIdx = (i - idx) - 1;
        }
        return this.mTxBuf[tempIdx];
    }

    private long getRxValueWithIdx(int idx) {
        int tempIdx;
        int i = this.mRxBufIdx;
        if (i <= 0 || (i - idx) - 1 < 0) {
            tempIdx = ((this.mRxBufIdx - idx) - 1) + 30;
        } else {
            tempIdx = (i - idx) - 1;
        }
        return this.mRxBuf[tempIdx];
    }

    private void setTxValueWithIdx(long txDiff) {
        if (this.mTxBufIdx >= 30) {
            this.mTxBufIdx = 0;
        }
        long[] jArr = this.mTxBuf;
        int i = this.mTxBufIdx;
        this.mTxBufIdx = i + 1;
        jArr[i] = txDiff;
    }

    private void setRxValueWithIdx(long rxDiff) {
        if (this.mRxBufIdx >= 30) {
            this.mRxBufIdx = 0;
        }
        long[] jArr = this.mRxBuf;
        int i = this.mRxBufIdx;
        this.mRxBufIdx = i + 1;
        jArr[i] = rxDiff;
    }

    private void updateAvgForHDR(int curState) {
        long txAvgForSet;
        int i;
        int i2 = curState;
        long rxAvgForSet = 0;
        long txSumForSet = 0;
        long rxSumForSet = 0;
        long txAvgForUnset = 0;
        long rxAvgForUnset = 0;
        if (i2 == 0 || i2 == 2) {
            for (int i3 = 0; i3 < 5; i3++) {
                txSumForSet += getTxValueWithIdx(i3);
                rxSumForSet += getRxValueWithIdx(i3);
            }
            long txAvgForSet2 = txSumForSet / 5;
            rxAvgForSet = rxSumForSet / 5;
            if (txAvgForSet2 > 1250000) {
                i = 0;
            } else if (rxAvgForSet > 1250000) {
                i = 0;
            } else {
                i = 0;
                this.mOverCntForHDR = 0;
                this.mUnderCntForHDR = i;
                txAvgForSet = txAvgForSet2;
            }
            this.mOverCntForHDR++;
            this.mUnderCntForHDR = i;
            txAvgForSet = txAvgForSet2;
        } else {
            long rxSumForUnset = 0;
            long txSumForUnset = 0;
            for (int i4 = 0; i4 < 2; i4++) {
                txSumForUnset += getTxValueWithIdx(i4);
                rxSumForUnset += getRxValueWithIdx(i4);
            }
            txAvgForUnset = txSumForUnset / 2;
            rxAvgForUnset = rxSumForUnset / 2;
            if (txAvgForUnset >= 1250000 || rxAvgForUnset >= 1250000) {
                this.mUnderCntForHDR = 0;
            } else {
                this.mUnderCntForHDR++;
            }
            this.mOverCntForHDR = 0;
            long j = txSumForUnset;
            txAvgForSet = 0;
            long j2 = rxSumForUnset;
        }
        long txAvgForSet3 = txSumForSet;
        if (this.DBG || i2 != 0) {
            Log.d(TAG, "updateAvgForHDR - OverCnt: " + this.mOverCntForHDR + ", UnderCnt: " + this.mUnderCntForHDR + ", txAvgForSet: " + txAvgForSet + ", rxAvgForSet: " + rxAvgForSet + ", txAvgForUnset: " + txAvgForUnset + ", rxAvgForUnset: " + rxAvgForUnset);
        }
    }

    private void updateAvgForLDR(int curState) {
        long txAvgForSet;
        int i;
        int i2;
        int i3 = curState;
        long rxAvgForSet = 0;
        long txSumForSet = 0;
        long txAvgForUnset = 0;
        long rxAvgForUnset = 0;
        long txSumForUnset = 0;
        long rxSumForUnset = 0;
        if (i3 == 0) {
            long rxSumForSet = 0;
            long txSumForSet2 = 0;
            for (int i4 = 0; i4 < 25; i4++) {
                txSumForSet2 += getTxValueWithIdx(i4);
                rxSumForSet += getRxValueWithIdx(i4);
            }
            long txAvgForSet2 = txSumForSet2 / 25;
            rxAvgForSet = rxSumForSet / 25;
            if (txAvgForSet2 > 125000 || rxAvgForSet > 125000) {
                this.mOverCntForLDR++;
            } else {
                this.mOverCntForLDR = 0;
            }
            this.mUnderCntForLDR = 0;
            txSumForSet = txSumForSet2;
            txAvgForSet = txAvgForSet2;
            long j = rxSumForSet;
        } else if (i3 == 1) {
            for (int i5 = 0; i5 < 2; i5++) {
                txSumForUnset += getTxValueWithIdx(i5);
                rxSumForUnset += getRxValueWithIdx(i5);
            }
            txAvgForUnset = txSumForUnset / 2;
            rxAvgForUnset = rxSumForUnset / 2;
            if (txAvgForUnset >= 125000 || rxAvgForUnset >= 125000) {
                i2 = 0;
                this.mUnderCntForLDR = 0;
            } else {
                this.mUnderCntForLDR = 10;
                i2 = 0;
            }
            this.mOverCntForLDR = i2;
            txAvgForSet = 0;
        } else {
            for (int i6 = 0; i6 < 10; i6++) {
                txSumForUnset += getTxValueWithIdx(i6);
                rxSumForUnset += getRxValueWithIdx(i6);
            }
            txAvgForUnset = txSumForUnset / 10;
            rxAvgForUnset = rxSumForUnset / 10;
            if (txAvgForUnset >= 125000 || rxAvgForUnset >= 125000) {
                i = 0;
                this.mUnderCntForLDR = 0;
            } else {
                this.mUnderCntForLDR++;
                i = 0;
            }
            this.mOverCntForLDR = i;
            txAvgForSet = 0;
        }
        long txAvgForSet3 = txSumForSet;
        if (this.DBG || i3 != 0) {
            Log.d(TAG, "updateAvgForLDR - OverCnt: " + this.mOverCntForLDR + ", UnderCnt: " + this.mUnderCntForLDR + ", txAvgForSet: " + txAvgForSet + ", rxAvgForSet: " + rxAvgForSet + ", txAvgForUnset: " + txAvgForUnset + ", rxAvgForUnset: " + rxAvgForUnset);
        }
    }

    public int getNextState(int curState, long txDiff, long rxDiff) {
        int nextState;
        setTxValueWithIdx(txDiff);
        setRxValueWithIdx(rxDiff);
        updateAvgForLDR(curState);
        updateAvgForHDR(curState);
        if (curState != 0) {
            if (curState != 1) {
                if (curState != 2) {
                    Log.d(TAG, "Current State(" + curState + ") was wrong... Set to disabled");
                    nextState = 0;
                } else if (this.mOverCntForHDR > 4) {
                    nextState = 1;
                } else if (this.mUnderCntForLDR > 9) {
                    nextState = 0;
                } else {
                    nextState = 2;
                }
            } else if (this.mUnderCntForHDR <= 1) {
                nextState = 1;
            } else if (this.mUnderCntForLDR < 9) {
                nextState = 2;
            } else {
                nextState = 0;
            }
        } else if (this.mOverCntForHDR > 4) {
            nextState = 1;
        } else if (this.mOverCntForLDR > 24) {
            nextState = 2;
        } else {
            nextState = 0;
        }
        if (this.DBG || curState != nextState) {
            Log.d(TAG, "Current State: " + curState + ", Next State: " + nextState);
        }
        return nextState;
    }
}
