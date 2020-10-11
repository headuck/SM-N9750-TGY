package com.android.server.wifi.iwc.rlengine;

import com.android.server.wifi.iwc.IWCLogFile;
import com.android.server.wifi.iwc.RewardEvent;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;

public class QTable {
    public static final long serialVersionUID = 20181101;
    private final IWCLogFile IWCLog;
    public int STEADSTATETHRESHOLD = 4;
    /* access modifiers changed from: private */
    public float discountFactor;
    public ArrayList<Integer> eventBuffer;
    public int eventBufferLimit;
    public int firstIndexToggling = -1;
    public boolean isSteadyState;
    /* access modifiers changed from: private */
    public int lastAction;
    public RewardEvent lastEvent = RewardEvent.AUTO_DISCONNECTION;
    /* access modifiers changed from: private */
    public int lastState;
    public long lastUpdateTime;
    /* access modifiers changed from: private */
    public float learningRate;
    public int mLastAGG;
    public int mLastSNS;
    public int movedFirstIndexToggling = -1;
    public int numActions;
    public int numStates;
    public float[][] qTable;
    /* access modifiers changed from: private */
    public int state;
    public int steakTogglingCnt = 0;
    public int zeroIndexReached = 0;

    public QTable(SavedMembers src, IWCLogFile logFile) {
        this.state = src.state;
        this.lastState = src.lastState;
        this.lastAction = src.lastAction;
        this.learningRate = src.learningRate;
        this.discountFactor = src.discountFactor;
        this.numStates = src.numStates;
        this.numActions = src.numActions;
        this.mLastSNS = src.mLastSNS;
        this.mLastAGG = src.mLastAGG;
        this.isSteadyState = src.isSteadyState;
        this.lastUpdateTime = src.lastUpdateTime;
        this.eventBuffer = src.eventBuffer;
        this.eventBufferLimit = src.eventBufferLimit;
        this.STEADSTATETHRESHOLD = src.STEADSTATETHRESHOLD;
        this.lastEvent = src.lastEvent;
        this.firstIndexToggling = src.firstIndexToggling;
        this.movedFirstIndexToggling = src.movedFirstIndexToggling;
        this.steakTogglingCnt = src.steakTogglingCnt;
        this.zeroIndexReached = src.zeroIndexReached;
        this.qTable = src.qTable;
        this.eventBuffer = src.eventBuffer;
        this.IWCLog = logFile;
    }

    public QTable(float[] param, IWCLogFile logFile) {
        this.state = (int) param[0];
        this.learningRate = param[1];
        this.discountFactor = param[2];
        this.lastAction = 0;
        this.numActions = 3;
        this.numStates = 3;
        this.mLastSNS = -1;
        this.mLastAGG = -1;
        this.lastUpdateTime = 0;
        this.isSteadyState = false;
        this.eventBuffer = new ArrayList<>();
        this.eventBufferLimit = 32;
        this.qTable = (float[][]) Array.newInstance(float.class, new int[]{this.numStates, this.numActions});
        this.IWCLog = logFile;
    }

    public int getBestState() {
        int bestState = this.numStates - 1;
        float bestval = -1000.0f;
        for (int idx = bestState; idx >= 0; idx--) {
            float[][] fArr = this.qTable;
            if (fArr[idx][0] > bestval) {
                bestState = idx;
                bestval = fArr[idx][0];
            }
        }
        return bestState;
    }

    public void setState(int curstate) {
        this.state = curstate;
    }

    public void setLastState(int lastState2) {
        this.lastState = lastState2;
    }

    public int getState() {
        return this.state;
    }

    public int getLastAction() {
        return this.lastAction;
    }

    public int getLastState() {
        return this.lastState;
    }

    public boolean getSteadyState() {
        return this.isSteadyState;
    }

    private void checkConsSNSToggling(RewardEvent eventType) {
        int numRemoveEvent;
        if (eventType == RewardEvent.SNS_OFF || eventType == RewardEvent.SNS_ON) {
            if (this.steakTogglingCnt == 0) {
                this.firstIndexToggling = this.eventBuffer.size() - 1;
                this.movedFirstIndexToggling = this.firstIndexToggling;
                this.steakTogglingCnt++;
            }
            if (this.lastEvent == RewardEvent.SNS_OFF || this.lastEvent == RewardEvent.SNS_ON) {
                int i = this.firstIndexToggling;
                if (i == 0) {
                    int size = this.eventBuffer.size();
                    int i2 = this.steakTogglingCnt;
                    if (size > i2) {
                        this.steakTogglingCnt = i2 + 1;
                    }
                } else if (i != 0 && this.zeroIndexReached != 1) {
                    if (this.movedFirstIndexToggling == 0) {
                        this.zeroIndexReached = 1;
                    }
                    this.steakTogglingCnt++;
                }
            }
        } else {
            int i3 = this.steakTogglingCnt;
            if (i3 > 0) {
                if (i3 >= this.eventBufferLimit) {
                    numRemoveEvent = i3 - 1;
                } else if (i3 % 2 == 0) {
                    numRemoveEvent = this.steakTogglingCnt;
                } else {
                    numRemoveEvent = i3 - 1;
                }
                Iterator<Integer> iter = this.eventBuffer.iterator();
                int iterCnt = 0;
                while (true) {
                    if (!iter.hasNext()) {
                        break;
                    }
                    iter.next();
                    if (iterCnt == this.movedFirstIndexToggling) {
                        while (numRemoveEvent > 0) {
                            iter.remove();
                            numRemoveEvent--;
                            if (iter.hasNext()) {
                                iter.next();
                            }
                        }
                    } else {
                        iterCnt++;
                    }
                }
                this.steakTogglingCnt = 0;
                this.firstIndexToggling = -1;
                this.movedFirstIndexToggling = -1;
                this.zeroIndexReached = 0;
            }
        }
    }

    private boolean checkSteadyState() {
        int y;
        int lStreak;
        int mStreak;
        boolean tempSteadyState = false;
        int mStreak2 = 0;
        int lStreak2 = 0;
        int aStreak = 0;
        int aCnt = 0;
        int lActionIdxLimit = RewardEvent.LESSEVENT_INDEXLIMIT.getValue();
        int mActionIdxLimit = RewardEvent.MOREEVENT_INDEXLIMIT.getValue();
        int oActionIdxLimit = RewardEvent.OTHEREVENT_INDEXLIMIT.getValue();
        if (this.eventBuffer.size() > 0) {
            Iterator<Integer> iter = this.eventBuffer.iterator();
            int longestStreak = 0;
            int mCnt = 0;
            int lCnt = 0;
            while (iter.hasNext()) {
                int agg = iter.next().intValue();
                if (agg == RewardEvent.AUTO_DISCONNECTION.getValue()) {
                    aStreak++;
                    aCnt++;
                    mStreak = mStreak2;
                    lStreak = lStreak2;
                } else if (agg < lActionIdxLimit) {
                    lCnt++;
                    mStreak = mStreak2;
                    lStreak = lStreak2 + 1;
                } else if (agg <= mActionIdxLimit || agg >= oActionIdxLimit) {
                    mStreak = mStreak2;
                    lStreak = lStreak2;
                } else {
                    mCnt++;
                    mStreak = mStreak2 + 1;
                    lStreak = lStreak2;
                }
                if (mStreak > 0 && lStreak == 0) {
                    mStreak2 = mStreak;
                    lStreak2 = lStreak;
                    longestStreak = mStreak;
                } else if (mStreak != 0 || lStreak <= 0) {
                    if (mStreak > 0 && lStreak > 0) {
                        if (mStreak > lStreak) {
                            if (mStreak > longestStreak) {
                                longestStreak = mStreak;
                            }
                            mStreak2 = 0;
                            lStreak2 = lStreak;
                            aStreak = 0;
                        } else if (mStreak < lStreak) {
                            if (lStreak > longestStreak) {
                                longestStreak = lStreak;
                            }
                            mStreak2 = mStreak;
                            lStreak2 = 0;
                            aStreak = 0;
                        } else if (agg > mActionIdxLimit && agg < oActionIdxLimit) {
                            mStreak2 = mStreak;
                            lStreak2 = 0;
                            aStreak = 0;
                        } else if (agg < lActionIdxLimit) {
                            mStreak2 = 0;
                            lStreak2 = lStreak;
                            aStreak = 0;
                        }
                    }
                    mStreak2 = mStreak;
                    lStreak2 = lStreak;
                } else {
                    mStreak2 = mStreak;
                    lStreak2 = lStreak;
                    longestStreak = lStreak;
                }
            }
            int x = longestStreak;
            int y2 = aStreak;
            int numMinLM = lCnt < mCnt ? lCnt : mCnt;
            int numMaxLM = lCnt > mCnt ? lCnt : mCnt;
            int i = longestStreak;
            int i2 = oActionIdxLimit;
            int i3 = mActionIdxLimit;
            int numMaxLM2 = numMaxLM;
            int numMinLM2 = numMinLM;
            int y3 = y2;
            int mCnt2 = mCnt;
            int lCnt2 = lCnt;
            int i4 = lActionIdxLimit;
            writeLog(x, y2, this.STEADSTATETHRESHOLD, this.eventBuffer.size(), this.eventBufferLimit, false, this.isSteadyState, this.firstIndexToggling, this.movedFirstIndexToggling, this.steakTogglingCnt, this.zeroIndexReached, "variable info");
            int i5 = this.STEADSTATETHRESHOLD;
            if (x < i5 - 1) {
                y = y3;
                if (((float) x) + (((float) y) / 4.0f) >= ((float) i5)) {
                    int i6 = numMaxLM2;
                    int i7 = numMinLM2;
                } else {
                    int numMinLM3 = numMinLM2;
                    int numMaxLM3 = numMaxLM2;
                    if (((float) numMinLM3) / ((float) numMaxLM3) > 0.25f || numMinLM3 <= 0 || numMaxLM3 < i5) {
                        tempSteadyState = false;
                    } else {
                        tempSteadyState = true;
                    }
                    int i8 = mCnt2;
                    int i9 = lCnt2;
                    int mCnt3 = y;
                }
            } else {
                int i10 = numMinLM2;
                y = y3;
            }
            tempSteadyState = true;
            int i82 = mCnt2;
            int i92 = lCnt2;
            int mCnt32 = y;
        } else {
            int i11 = mActionIdxLimit;
            int i12 = lActionIdxLimit;
            writeLog(0, 0, this.STEADSTATETHRESHOLD, this.eventBuffer.size(), this.eventBufferLimit, false, this.isSteadyState, this.firstIndexToggling, this.movedFirstIndexToggling, this.steakTogglingCnt, this.zeroIndexReached, "eventBuffer size <= 0");
        }
        return tempSteadyState;
    }

    public int addEvent(boolean snsFlag, boolean aggFlag, RewardEvent eventType) {
        int i;
        RewardEvent rewardEvent = eventType;
        int reachSS = 0;
        if (this.eventBuffer == null) {
            this.eventBuffer = new ArrayList<>();
            writeLog(0, 0, this.STEADSTATETHRESHOLD, -1, this.eventBufferLimit, false, this.isSteadyState, this.firstIndexToggling, this.movedFirstIndexToggling, this.steakTogglingCnt, this.zeroIndexReached, "eventBuffer is null --> new");
        }
        this.eventBuffer.add(Integer.valueOf(eventType.getValue()));
        Iterator<Integer> iter = this.eventBuffer.iterator();
        if (this.eventBuffer.size() > this.eventBufferLimit) {
            if (iter.hasNext()) {
                iter.next();
                iter.remove();
            }
            if (!(this.firstIndexToggling == 0 || (i = this.movedFirstIndexToggling) == -1 || this.zeroIndexReached != 0)) {
                this.movedFirstIndexToggling = i - 1;
            }
        }
        checkConsSNSToggling(rewardEvent);
        if (!this.isSteadyState && checkSteadyState()) {
            this.isSteadyState = true;
            reachSS = 1;
        }
        this.lastEvent = rewardEvent;
        return reachSS;
    }

    public void writeLog(String valueName, String value) {
        IWCLogFile iWCLogFile = this.IWCLog;
        if (iWCLogFile != null) {
            iWCLogFile.writeToLogFile(valueName, value);
        }
    }

    private void writeLog(int x, int y, int thre, int buffSize, int buffLimit, boolean tmpSteadyState, boolean isSteadyState2, int firstIndexToggling2, int movedFirstIndexToggling2, int steakTogglingCnt2, int zeroIndexReached2, String str) {
        writeLog("Steady State Detail", "" + String.format("x: %f, y: %f, STEADSTATETHRESHOLD: %f, buffSize: %d, buffLimit: %d, tmpSteadyState: %b, isSteadyState: %b, first/moved_IndexToggling: %d/%d, steakTogglingCnt: %d, zeroIndexReached= %d, str = %s", new Object[]{Float.valueOf((float) x), Float.valueOf((float) y), Float.valueOf((float) thre), Integer.valueOf(buffSize), Integer.valueOf(buffLimit), Boolean.valueOf(tmpSteadyState), Boolean.valueOf(isSteadyState2), Integer.valueOf(firstIndexToggling2), Integer.valueOf(movedFirstIndexToggling2), Integer.valueOf(steakTogglingCnt2), Integer.valueOf(zeroIndexReached2), str}));
    }

    static class SavedMembers {
        public int STEADSTATETHRESHOLD;
        public float discountFactor;
        public ArrayList<Integer> eventBuffer;
        public int eventBufferLimit;
        public int firstIndexToggling;
        public boolean isSteadyState;
        public int lastAction;
        public RewardEvent lastEvent;
        public int lastState;
        public long lastUpdateTime;
        public float learningRate;
        public int mLastAGG;
        public int mLastSNS;
        public int movedFirstIndexToggling;
        public int numActions;
        public int numStates;
        public float[][] qTable;
        public int state;
        public int steakTogglingCnt;
        public long version;
        public int zeroIndexReached;

        public SavedMembers(QTable src) {
            this.version = 20181101;
            this.state = src.state;
            this.lastState = src.lastState;
            this.lastAction = src.lastAction;
            this.learningRate = src.learningRate;
            this.discountFactor = src.discountFactor;
            this.numStates = src.numStates;
            this.numActions = src.numActions;
            this.mLastSNS = src.mLastSNS;
            this.mLastAGG = src.mLastAGG;
            this.isSteadyState = src.isSteadyState;
            this.lastUpdateTime = src.lastUpdateTime;
            this.eventBuffer = src.eventBuffer;
            this.eventBufferLimit = src.eventBufferLimit;
            this.STEADSTATETHRESHOLD = src.STEADSTATETHRESHOLD;
            this.lastEvent = src.lastEvent;
            this.firstIndexToggling = src.firstIndexToggling;
            this.movedFirstIndexToggling = src.movedFirstIndexToggling;
            this.steakTogglingCnt = src.steakTogglingCnt;
            this.zeroIndexReached = src.zeroIndexReached;
            this.qTable = src.qTable;
        }

        public SavedMembers() {
        }

        public QTable readResolve(IWCLogFile logFile) {
            return new QTable(this, logFile);
        }
    }

    public SavedMembers writeReplace() {
        return new SavedMembers(this);
    }
}
