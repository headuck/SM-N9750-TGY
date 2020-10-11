package com.android.server.wifi.iwc.rlengine;

import android.util.Log;
import com.android.server.wifi.hotspot2.anqp.NAIRealmData;
import com.android.server.wifi.iwc.IWCLogFile;
import com.android.server.wifi.iwc.RewardEvent;
import java.util.Iterator;

public class RFLearningTop {
    private static final String TAG = "IWCMonitor.RFLearningTop";
    private IWCLogFile IWCLog;
    private String currentAP;
    private int currentState;
    public RFLInterface intf = new RFLInterface();
    private int lastAction;
    private QTableContainer qTables;
    public RewardManager rwManager;

    public RFLearningTop(IWCLogFile logFile) {
        this.qTables = new QTableContainer(logFile);
        this.rwManager = new RewardManager(logFile);
        this.IWCLog = logFile;
        this.currentAP = null;
        this.lastAction = 0;
        this.currentState = 2;
    }

    public void setDefaultQAI() {
        synchronized (this) {
            this.qTables.setDefaultQAI(this.intf);
        }
    }

    public void setDefaultQAI(int forced_qai) {
        synchronized (this) {
            this.qTables.setDefaultQAI(forced_qai);
        }
    }

    public void removeNonSSQtables() {
        this.qTables.removeNonSSQtables();
    }

    public void setCurrentState(int currentState2) {
        this.currentState = currentState2;
    }

    public void algorithmStep() {
        QTableContainer qTableContainer = this.qTables;
        if (qTableContainer == null) {
            Log.e(TAG, "algorithmStep - QTable Container is null");
            return;
        }
        qTableContainer.manageApList();
        printApLists();
        this.currentAP = this.intf.currentApBssid_IN;
        String str = this.currentAP;
        if (str != null) {
            if (this.qTables.findTable(str) == -1) {
                this.qTables.createTable(this.currentAP, this.intf);
            }
            String logValue = "" + String.format("Q-Table: [", new Object[0]);
            int tableIdx = this.qTables.findTable(this.currentAP);
            if (tableIdx == -1) {
                Log.e(TAG, "algorithmStep - FindTable returned -1");
                return;
            }
            QTable tempTable = this.qTables.qTableList.get(tableIdx);
            for (int i = 0; i < tempTable.numStates; i++) {
                for (int j = 0; j < tempTable.numActions; j++) {
                    logValue = logValue + String.format(" %f", new Object[]{Float.valueOf(tempTable.qTable[i][j])});
                }
                logValue = logValue + String.format(NAIRealmData.NAI_REALM_STRING_SEPARATOR, new Object[0]);
            }
            this.currentState = this.qTables.getTableStateCB(this.currentAP);
            String logValue2 = (logValue + String.format(" ] >> ", new Object[0])) + String.format("Action Taken: %d, New State: %d", new Object[]{Integer.valueOf(this.lastAction), Integer.valueOf(this.currentState)});
            IWCLogFile iWCLogFile = this.IWCLog;
            if (iWCLogFile != null) {
                iWCLogFile.writeToLogFile("Q-Table Action Taken", logValue2);
            }
        }
    }

    public synchronized boolean rebase() {
        int tableIdx = this.qTables.findTable(this.intf.currentApBssid_IN);
        if (tableIdx == -1) {
            Log.e(TAG, "updateTable - findTable returned -1");
            return false;
        }
        this.qTables.updateApListAccessTime(this.intf.currentApBssid_IN);
        this.qTables.qTableList.get(tableIdx).lastUpdateTime = System.currentTimeMillis();
        this.qTables.setDefaultQAI(this.intf);
        this.qTables.rebaseQTables();
        return true;
    }

    public void updateDebugIntent(RewardEvent re, String currentBssid_IN, boolean switchFlag) {
        int index = this.qTables.findTable(currentBssid_IN);
        this.rwManager.updateDebugIntent(this.intf.mContext, this.rwManager.getEventTypeString(re, switchFlag), currentBssid_IN, index, this.qTables.qTableList.get(index));
    }

    /* JADX WARNING: Code restructure failed: missing block: B:58:0x0206, code lost:
        return r8;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean updateTable(com.android.server.wifi.iwc.RewardEvent r20, long r21) {
        /*
            r19 = this;
            r1 = r19
            r0 = r20
            monitor-enter(r19)
            r8 = 0
            com.android.server.wifi.iwc.rlengine.RFLInterface r2 = r1.intf     // Catch:{ all -> 0x0207 }
            boolean r2 = r2.edgeFlag     // Catch:{ all -> 0x0207 }
            if (r2 != 0) goto L_0x0012
            com.android.server.wifi.iwc.rlengine.RFLInterface r2 = r1.intf     // Catch:{ all -> 0x0207 }
            boolean r2 = r2.snsOptionChanged     // Catch:{ all -> 0x0207 }
            if (r2 == 0) goto L_0x0205
        L_0x0012:
            com.android.server.wifi.iwc.rlengine.QTableContainer r2 = r1.qTables     // Catch:{ all -> 0x0207 }
            r9 = 0
            if (r2 != 0) goto L_0x0020
            java.lang.String r2 = "IWCMonitor.RFLearningTop"
            java.lang.String r3 = "updateTable - QTable Container is null"
            android.util.Log.e(r2, r3)     // Catch:{ all -> 0x0207 }
            monitor-exit(r19)
            return r9
        L_0x0020:
            com.android.server.wifi.iwc.rlengine.QTableContainer r2 = r1.qTables     // Catch:{ all -> 0x0207 }
            r2.manageApList()     // Catch:{ all -> 0x0207 }
            r19.printApLists()     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r2 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r2 = r2.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            r1.currentAP = r2     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r2 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r2 = r2.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            r10 = -1
            if (r2 == 0) goto L_0x0055
            com.android.server.wifi.iwc.rlengine.QTableContainer r2 = r1.qTables     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r3 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r3 = r3.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            int r2 = r2.findTable(r3)     // Catch:{ all -> 0x0207 }
            if (r2 != r10) goto L_0x0055
            com.android.server.wifi.iwc.rlengine.QTableContainer r2 = r1.qTables     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r3 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r3 = r3.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            r2.addCandidateList(r3)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTableContainer r2 = r1.qTables     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r3 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r3 = r3.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r4 = r1.intf     // Catch:{ all -> 0x0207 }
            r2.createTable(r3, r4)     // Catch:{ all -> 0x0207 }
        L_0x0055:
            int r2 = r1.currentState     // Catch:{ all -> 0x0207 }
            r11 = r2
            r12 = r11
            com.android.server.wifi.iwc.rlengine.RewardManager r2 = r1.rwManager     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTableContainer r4 = r1.qTables     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r5 = r1.intf     // Catch:{ all -> 0x0207 }
            r3 = r20
            r6 = r21
            int r2 = r2.applyRewards(r3, r4, r5, r6)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTableContainer r3 = r1.qTables     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r4 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r4 = r4.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            int r3 = r3.findTable(r4)     // Catch:{ all -> 0x0207 }
            if (r3 != r10) goto L_0x007c
            java.lang.String r4 = "IWCMonitor.RFLearningTop"
            java.lang.String r5 = "updateTable - findTable returned -1"
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x0207 }
            monitor-exit(r19)
            return r9
        L_0x007c:
            com.android.server.wifi.iwc.rlengine.QTableContainer r4 = r1.qTables     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r5 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r5 = r5.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            r4.updateApListAccessTime(r5)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTableContainer r4 = r1.qTables     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r5 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r5 = r5.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            int r4 = r4.getTableStateCB(r5)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTableContainer r5 = r1.qTables     // Catch:{ all -> 0x0207 }
            java.util.ArrayList<com.android.server.wifi.iwc.rlengine.QTable> r5 = r5.qTableList     // Catch:{ all -> 0x0207 }
            java.lang.Object r5 = r5.get(r3)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTable r5 = (com.android.server.wifi.iwc.rlengine.QTable) r5     // Catch:{ all -> 0x0207 }
            r6 = 1
            if (r2 == r6) goto L_0x00a4
            boolean r7 = r5.getSteadyState()     // Catch:{ all -> 0x0207 }
            if (r7 != r6) goto L_0x00a5
            if (r12 == r4) goto L_0x00a5
        L_0x00a4:
            r8 = 1
        L_0x00a5:
            com.android.server.wifi.iwc.rlengine.RewardManager r7 = r1.rwManager     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r10 = r1.intf     // Catch:{ all -> 0x0207 }
            boolean r10 = r10.switchFlag     // Catch:{ all -> 0x0207 }
            java.lang.String r7 = r7.getEventTypeString(r0, r10)     // Catch:{ all -> 0x0207 }
            java.lang.String r10 = "NONE"
            boolean r10 = r7.equals(r10)     // Catch:{ all -> 0x0207 }
            if (r10 != 0) goto L_0x00cb
            com.android.server.wifi.iwc.rlengine.RewardManager r13 = r1.rwManager     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r10 = r1.intf     // Catch:{ all -> 0x0207 }
            android.content.Context r14 = r10.mContext     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r10 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r10 = r10.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            r15 = r7
            r16 = r10
            r17 = r3
            r18 = r5
            r13.sendDebugIntent(r14, r15, r16, r17, r18)     // Catch:{ all -> 0x0207 }
        L_0x00cb:
            com.android.server.wifi.iwc.rlengine.QTableContainer r10 = r1.qTables     // Catch:{ all -> 0x0207 }
            java.lang.String r11 = r1.currentAP     // Catch:{ all -> 0x0207 }
            r10.recordApActivity(r11, r0)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r10 = r1.intf     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.IWCBDTracking r10 = r10.mBdTracking     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r11 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r11 = r11.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            r10.setOUIInfo(r11)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r10 = r1.intf     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.IWCBDTracking r10 = r10.mBdTracking     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r11 = r1.intf     // Catch:{ all -> 0x0207 }
            boolean r11 = r11.switchFlag     // Catch:{ all -> 0x0207 }
            r10.setStateInfo(r11)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r10 = r1.intf     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.IWCBDTracking r10 = r10.mBdTracking     // Catch:{ all -> 0x0207 }
            r10.setQAIInfo(r12, r4)     // Catch:{ all -> 0x0207 }
            java.lang.String r10 = ""
            java.lang.String r11 = ""
            java.util.ArrayList<java.lang.Integer> r13 = r5.eventBuffer     // Catch:{ all -> 0x0207 }
            java.util.Iterator r13 = r13.iterator()     // Catch:{ all -> 0x0207 }
        L_0x00f9:
            boolean r14 = r13.hasNext()     // Catch:{ all -> 0x0207 }
            if (r14 == 0) goto L_0x0125
            java.lang.Object r14 = r13.next()     // Catch:{ all -> 0x0207 }
            java.lang.Integer r14 = (java.lang.Integer) r14     // Catch:{ all -> 0x0207 }
            java.lang.StringBuilder r15 = new java.lang.StringBuilder     // Catch:{ all -> 0x0207 }
            r15.<init>()     // Catch:{ all -> 0x0207 }
            r15.append(r11)     // Catch:{ all -> 0x0207 }
            java.lang.String r9 = " %d"
            java.lang.Object[] r0 = new java.lang.Object[r6]     // Catch:{ all -> 0x0207 }
            r16 = 0
            r0[r16] = r14     // Catch:{ all -> 0x0207 }
            java.lang.String r0 = java.lang.String.format(r9, r0)     // Catch:{ all -> 0x0207 }
            r15.append(r0)     // Catch:{ all -> 0x0207 }
            java.lang.String r0 = r15.toString()     // Catch:{ all -> 0x0207 }
            r11 = r0
            r0 = r20
            r9 = 0
            goto L_0x00f9
        L_0x0125:
            java.lang.String r0 = "%.2f %.2f %.2f"
            r9 = 3
            java.lang.Object[] r9 = new java.lang.Object[r9]     // Catch:{ all -> 0x0207 }
            float[][] r13 = r5.qTable     // Catch:{ all -> 0x0207 }
            r14 = 0
            r13 = r13[r14]     // Catch:{ all -> 0x0207 }
            r13 = r13[r14]     // Catch:{ all -> 0x0207 }
            java.lang.Float r13 = java.lang.Float.valueOf(r13)     // Catch:{ all -> 0x0207 }
            r9[r14] = r13     // Catch:{ all -> 0x0207 }
            float[][] r13 = r5.qTable     // Catch:{ all -> 0x0207 }
            r13 = r13[r6]     // Catch:{ all -> 0x0207 }
            r13 = r13[r14]     // Catch:{ all -> 0x0207 }
            java.lang.Float r13 = java.lang.Float.valueOf(r13)     // Catch:{ all -> 0x0207 }
            r9[r6] = r13     // Catch:{ all -> 0x0207 }
            float[][] r6 = r5.qTable     // Catch:{ all -> 0x0207 }
            r13 = 2
            r6 = r6[r13]     // Catch:{ all -> 0x0207 }
            r14 = 0
            r6 = r6[r14]     // Catch:{ all -> 0x0207 }
            java.lang.Float r6 = java.lang.Float.valueOf(r6)     // Catch:{ all -> 0x0207 }
            r9[r13] = r6     // Catch:{ all -> 0x0207 }
            java.lang.String r0 = java.lang.String.format(r0, r9)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r6 = r1.intf     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.IWCBDTracking r6 = r6.mBdTracking     // Catch:{ all -> 0x0207 }
            r6.setEVInfo(r11)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r6 = r1.intf     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.IWCBDTracking r6 = r6.mBdTracking     // Catch:{ all -> 0x0207 }
            r6.setQTableValueInfo(r0)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r6 = r1.intf     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.IWCBDTracking r6 = r6.mBdTracking     // Catch:{ all -> 0x0207 }
            int r6 = r6.getIdInfo()     // Catch:{ all -> 0x0207 }
            if (r6 != r13) goto L_0x0205
            com.android.server.wifi.iwc.rlengine.RFLInterface r6 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r6 = r6.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            if (r6 == 0) goto L_0x0205
            com.android.server.wifi.iwc.rlengine.QTableContainer r6 = r1.qTables     // Catch:{ all -> 0x0207 }
            java.util.Map<java.lang.String, com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode> r6 = r6.candidateApList     // Catch:{ all -> 0x0207 }
            if (r6 == 0) goto L_0x01a4
            com.android.server.wifi.iwc.rlengine.QTableContainer r6 = r1.qTables     // Catch:{ all -> 0x0207 }
            java.util.Map<java.lang.String, com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode> r6 = r6.candidateApList     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r9 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r9 = r9.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            java.lang.Object r6 = r6.get(r9)     // Catch:{ all -> 0x0207 }
            if (r6 == 0) goto L_0x01a4
            com.android.server.wifi.iwc.rlengine.RFLInterface r6 = r1.intf     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.IWCBDTracking r6 = r6.mBdTracking     // Catch:{ all -> 0x0207 }
            long r9 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTableContainer r13 = r1.qTables     // Catch:{ all -> 0x0207 }
            java.util.Map<java.lang.String, com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode> r13 = r13.candidateApList     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r14 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r14 = r14.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            java.lang.Object r13 = r13.get(r14)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode r13 = (com.android.server.wifi.iwc.rlengine.QTableContainer.ApListNode) r13     // Catch:{ all -> 0x0207 }
            long r13 = r13.firstAdded     // Catch:{ all -> 0x0207 }
            long r9 = r9 - r13
            r6.setSSTakenTimeInfo(r9)     // Catch:{ all -> 0x0207 }
            goto L_0x0205
        L_0x01a4:
            com.android.server.wifi.iwc.rlengine.QTableContainer r6 = r1.qTables     // Catch:{ all -> 0x0207 }
            java.util.Map<java.lang.String, com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode> r6 = r6.coreApList     // Catch:{ all -> 0x0207 }
            if (r6 == 0) goto L_0x01d5
            com.android.server.wifi.iwc.rlengine.QTableContainer r6 = r1.qTables     // Catch:{ all -> 0x0207 }
            java.util.Map<java.lang.String, com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode> r6 = r6.coreApList     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r9 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r9 = r9.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            java.lang.Object r6 = r6.get(r9)     // Catch:{ all -> 0x0207 }
            if (r6 == 0) goto L_0x01d5
            com.android.server.wifi.iwc.rlengine.RFLInterface r6 = r1.intf     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.IWCBDTracking r6 = r6.mBdTracking     // Catch:{ all -> 0x0207 }
            long r9 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTableContainer r13 = r1.qTables     // Catch:{ all -> 0x0207 }
            java.util.Map<java.lang.String, com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode> r13 = r13.coreApList     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r14 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r14 = r14.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            java.lang.Object r13 = r13.get(r14)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode r13 = (com.android.server.wifi.iwc.rlengine.QTableContainer.ApListNode) r13     // Catch:{ all -> 0x0207 }
            long r13 = r13.firstAdded     // Catch:{ all -> 0x0207 }
            long r9 = r9 - r13
            r6.setSSTakenTimeInfo(r9)     // Catch:{ all -> 0x0207 }
            goto L_0x0205
        L_0x01d5:
            com.android.server.wifi.iwc.rlengine.QTableContainer r6 = r1.qTables     // Catch:{ all -> 0x0207 }
            java.util.Map<java.lang.String, com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode> r6 = r6.probationApList     // Catch:{ all -> 0x0207 }
            if (r6 == 0) goto L_0x0205
            com.android.server.wifi.iwc.rlengine.QTableContainer r6 = r1.qTables     // Catch:{ all -> 0x0207 }
            java.util.Map<java.lang.String, com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode> r6 = r6.probationApList     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r9 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r9 = r9.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            java.lang.Object r6 = r6.get(r9)     // Catch:{ all -> 0x0207 }
            if (r6 == 0) goto L_0x0205
            com.android.server.wifi.iwc.rlengine.RFLInterface r6 = r1.intf     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.IWCBDTracking r6 = r6.mBdTracking     // Catch:{ all -> 0x0207 }
            long r9 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTableContainer r13 = r1.qTables     // Catch:{ all -> 0x0207 }
            java.util.Map<java.lang.String, com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode> r13 = r13.probationApList     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.RFLInterface r14 = r1.intf     // Catch:{ all -> 0x0207 }
            java.lang.String r14 = r14.currentApBssid_IN     // Catch:{ all -> 0x0207 }
            java.lang.Object r13 = r13.get(r14)     // Catch:{ all -> 0x0207 }
            com.android.server.wifi.iwc.rlengine.QTableContainer$ApListNode r13 = (com.android.server.wifi.iwc.rlengine.QTableContainer.ApListNode) r13     // Catch:{ all -> 0x0207 }
            long r13 = r13.firstAdded     // Catch:{ all -> 0x0207 }
            long r9 = r9 - r13
            r6.setSSTakenTimeInfo(r9)     // Catch:{ all -> 0x0207 }
        L_0x0205:
            monitor-exit(r19)
            return r8
        L_0x0207:
            r0 = move-exception
            monitor-exit(r19)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.iwc.rlengine.RFLearningTop.updateTable(com.android.server.wifi.iwc.RewardEvent, long):boolean");
    }

    public int removeQtableIfExist(String bssid) {
        QTableContainer qTableContainer = this.qTables;
        if (qTableContainer != null) {
            return qTableContainer.removeQtableIfExist(bssid);
        }
        Log.e(TAG, "removeQtableIfExist - QTable Container is null");
        return -1;
    }

    public void setCurrentAP(String bssid) {
        this.currentAP = bssid;
    }

    public int getCurrentState() {
        return this.currentState;
    }

    public float[][] getCurrentTableStates() {
        QTableContainer qTableContainer;
        int tableIndex;
        String str = this.currentAP;
        if (str != null && (qTableContainer = this.qTables) != null && (tableIndex = qTableContainer.findTable(str)) != -1) {
            return this.qTables.qTableList.get(tableIndex).qTable;
        }
        return new float[][]{new float[]{-1.0f, -1.0f, -1.0f}, new float[]{-1.0f, -1.0f, -1.0f}, new float[]{-1.0f, -1.0f, -1.0f}};
    }

    public int getSteadyStateNum() {
        int res = 0;
        Iterator<QTable> it = this.qTables.qTableList.iterator();
        while (it.hasNext()) {
            if (it.next().getSteadyState()) {
                res++;
            }
        }
        return res;
    }

    public boolean getIsSteadyState(String bssid) {
        QTableContainer qTableContainer;
        int tableIndex;
        if (bssid == null || (qTableContainer = this.qTables) == null || (tableIndex = qTableContainer.findTable(bssid)) == -1) {
            return false;
        }
        return this.qTables.qTableList.get(tableIndex).getSteadyState();
    }

    public String getCurrentAP() {
        return this.currentAP;
    }

    public String getQTableStr() {
        QTableContainer qTableContainer;
        String tmpStr = "";
        String str = this.currentAP;
        if (str == null || (qTableContainer = this.qTables) == null) {
            return "null / null / null";
        }
        int tableIndex = qTableContainer.findTable(str);
        if (tableIndex == -1) {
            return tmpStr;
        }
        QTable tempTable = this.qTables.qTableList.get(tableIndex);
        for (int i = 0; i < tempTable.numStates; i++) {
            if (i == tempTable.numStates - 1) {
                tmpStr = tmpStr + String.format(" %.2f", new Object[]{Float.valueOf(tempTable.qTable[i][0])});
            } else {
                tmpStr = tmpStr + String.format(" %.2f /", new Object[]{Float.valueOf(tempTable.qTable[i][0])});
            }
        }
        return tmpStr;
    }

    public QTableContainer getQtables() {
        return this.qTables;
    }

    public void setQtables(QTableContainer qt) {
        if (qt != null) {
            this.qTables = qt;
        }
    }

    public void printApLists() {
        String value = "Candidate List: [";
        QTableContainer qTableContainer = this.qTables;
        if (qTableContainer == null) {
            Log.e(TAG, "PrintApLists - QTable Container is null");
            return;
        }
        for (String key : qTableContainer.candidateApList.keySet()) {
            value = value + String.format(" <%s;%s;%s> ", new Object[]{key, Integer.valueOf(this.qTables.candidateApList.get(key).activityScore), Long.valueOf(System.currentTimeMillis() - this.qTables.candidateApList.get(key).firstAdded)});
        }
        String value2 = value + "], Core List: [";
        for (String key2 : this.qTables.coreApList.keySet()) {
            value2 = value2 + String.format(" <%s;%s> ", new Object[]{key2, Long.valueOf(this.qTables.coreApList.get(key2).lastAccessed)});
        }
        String value3 = value2 + "], Probation List: [";
        for (String key3 : this.qTables.probationApList.keySet()) {
            value3 = value3 + String.format(" <%s;%s;%s> ", new Object[]{key3, Integer.valueOf(this.qTables.probationApList.get(key3).activityScore), Long.valueOf(System.currentTimeMillis() - this.qTables.probationApList.get(key3).firstAdded)});
        }
        String value4 = value3 + "]";
        IWCLogFile iWCLogFile = this.IWCLog;
        if (iWCLogFile != null) {
            iWCLogFile.writeToLogFile("List of Known APs", value4);
        }
    }

    public int updateQAI() {
        QTableContainer qTableContainer = this.qTables;
        if (qTableContainer == null) {
            Log.e(TAG, "updateQAI - QTable Container is null");
            return -1;
        }
        if (qTableContainer.findTable(this.currentAP) != -1) {
            this.currentState = this.qTables.getTableStateCB(this.currentAP);
        }
        return this.currentState;
    }

    public void printTable() {
        if (this.qTables == null) {
            Log.e(TAG, "PrintTable - QTable Container is null");
            return;
        }
        for (int i = 0; i < this.qTables.qTableList.size(); i++) {
            QTable tempTable = this.qTables.qTableList.get(i);
            String value = (("" + String.format("< %s - %f %f %f >", new Object[]{this.qTables.qTableIndexList.get(i).bssid, Float.valueOf(tempTable.qTable[0][0]), Float.valueOf(tempTable.qTable[1][0]), Float.valueOf(tempTable.qTable[2][0])})) + String.format(" < isSteadyState - %d, mLastSNS - %d, mLastAGG - %d >", new Object[]{Integer.valueOf(tempTable.isSteadyState ? 1 : 0), Integer.valueOf(tempTable.mLastSNS), Integer.valueOf(tempTable.mLastAGG)})) + String.format(" < EventBuffer ", new Object[0]);
            Iterator<Integer> it = tempTable.eventBuffer.iterator();
            while (it.hasNext()) {
                value = value + String.format(" %d", new Object[]{it.next()});
            }
            String value2 = value + String.format(" >", new Object[0]);
            IWCLogFile iWCLogFile = this.IWCLog;
            if (iWCLogFile != null) {
                iWCLogFile.writeToLogFile("Qtable Dump", value2);
            }
        }
    }

    public void printCurrentTable(String bssid) {
        String value = "";
        if (this.qTables == null) {
            Log.e(TAG, "PrintCurrentTable - QTable Container is null");
            return;
        }
        for (int i = 0; i < this.qTables.qTableList.size(); i++) {
            if (this.qTables.qTableIndexList.get(i).bssid.equals(bssid)) {
                QTable tempTable = this.qTables.qTableList.get(i);
                String value2 = ((value + String.format("< %s - %f %f %f >", new Object[]{bssid, Float.valueOf(tempTable.qTable[0][0]), Float.valueOf(tempTable.qTable[1][0]), Float.valueOf(tempTable.qTable[2][0])})) + String.format(" < isSteadyState - %d, mLastSNS - %d, mLastAGG - %d >", new Object[]{Integer.valueOf(tempTable.isSteadyState ? 1 : 0), Integer.valueOf(tempTable.mLastSNS), Integer.valueOf(tempTable.mLastAGG)})) + String.format(" < EventBuffer ", new Object[0]);
                Iterator<Integer> it = tempTable.eventBuffer.iterator();
                while (it.hasNext()) {
                    value2 = value2 + String.format(" %d", new Object[]{it.next()});
                }
                value = value2 + String.format(" >", new Object[0]);
                IWCLogFile iWCLogFile = this.IWCLog;
                if (iWCLogFile != null) {
                    iWCLogFile.writeToLogFile("Qtable Dump(current bssid)", value);
                }
            }
        }
    }

    public void putBssidToConfigKey(String key, String bssid) {
        QTableContainer qTableContainer;
        if (key == null || (qTableContainer = this.qTables) == null) {
            Log.e(TAG, "QTable Container is null or " + key);
            return;
        }
        qTableContainer.putBssidToConfigKey(key, bssid);
    }

    public void removeConfigKey(String key) {
        QTableContainer qTableContainer;
        if (key == null || (qTableContainer = this.qTables) == null) {
            Log.e(TAG, "QTable Container is null or " + key);
            return;
        }
        qTableContainer.removeConfigKey(key);
    }
}
