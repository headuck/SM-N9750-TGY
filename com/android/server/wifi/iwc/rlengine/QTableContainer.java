package com.android.server.wifi.iwc.rlengine;

import android.util.Log;
import com.android.server.wifi.iwc.IWCLogFile;
import com.android.server.wifi.iwc.RewardEvent;
import com.android.server.wifi.p2p.common.DefaultImageRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class QTableContainer {
    public static final float[] PARAM = {2.0f, 0.5f, 0.5f};
    private static final String TAG = "IWCMonitor.QTableContainer";
    public static final long serialVersionUID = 20200326;
    private IWCLogFile IWCLog;
    public int activityScore_AUTODISC;
    public int activityScore_CELLULAR_OFF;
    public int activityScore_DROP_TOO_SHORT;
    public int activityScore_MANUAL_DISC;
    public int activityScore_MANUAL_RECONNECTION;
    public int activityScore_MANUAL_SWITCH;
    public int activityScore_SNS_ONOFF;
    public int activityScore_WIFI_OFF;
    public Map<String, Set<Long>> bssidPerSsidList;
    public Map<String, ApListNode> candidateApList;
    public int candidateListActivityThreshold;
    public int candidateListMemberLimit;
    public long candidateListTimeThreshold;
    public Map<String, ApListNode> coreApList;
    public int coreListMemberLimit;
    public int mDefaultQAI;
    public int mForcedlyQAISet;
    public Map<String, ApListNode> probationApList;
    public int probationListActivityThreshold;
    public long probationListTimeThreshold;
    public ArrayList<IndexNode> qTableIndexList;
    public ArrayList<QTable> qTableList;
    public ArrayList<QTableInfoForSort> sortInfoList;
    public boolean useProbationList;

    public static class IndexNode {
        public String bssid;

        IndexNode(String id) {
            this.bssid = id;
        }
    }

    public static class ApListNode {
        public int activityScore;
        public long firstAdded;
        public long lastAccessed;

        ApListNode(long time) {
            this.firstAdded = time;
            this.activityScore = 0;
            this.lastAccessed = time;
        }

        public ApListNode(int a1, long a2, long a3) {
            this.firstAdded = a2;
            this.activityScore = a1;
            this.lastAccessed = a3;
        }
    }

    public static class QTableInfoForSort {
        /* access modifiers changed from: private */
        public long curState;
        /* access modifiers changed from: private */
        public long lastUpdateTime;

        QTableInfoForSort(long Time, int state) {
            this.lastUpdateTime = Time;
            this.curState = (long) state;
        }
    }

    public QTableContainer(IWCLogFile logFile) {
        this.mDefaultQAI = -1;
        this.mForcedlyQAISet = -1;
        this.qTableList = new ArrayList<>();
        this.qTableIndexList = new ArrayList<>();
        this.coreApList = new ConcurrentHashMap();
        this.candidateApList = new ConcurrentHashMap();
        this.probationApList = new ConcurrentHashMap();
        this.sortInfoList = new ArrayList<>();
        this.bssidPerSsidList = new ConcurrentHashMap();
        initQTableContainer(logFile);
    }

    public QTableContainer(SavedMembers src, IWCLogFile logFile) {
        this.mDefaultQAI = -1;
        this.mForcedlyQAISet = -1;
        this.mDefaultQAI = src.qai;
        this.mForcedlyQAISet = src.forcedqai;
        this.qTableList = (ArrayList) Optional.ofNullable(src.qTableList).orElse(new ArrayList());
        this.qTableIndexList = (ArrayList) Optional.ofNullable(src.qTableIndexList).orElse(new ArrayList());
        this.coreApList = (Map) Optional.ofNullable(src.coreApList).orElse(new ConcurrentHashMap());
        this.candidateApList = (Map) Optional.ofNullable(src.candidateApList).orElse(new ConcurrentHashMap());
        this.probationApList = (Map) Optional.ofNullable(src.probationApList).orElse(new ConcurrentHashMap());
        this.bssidPerSsidList = (Map) Optional.ofNullable(src.bssidPerSsidList).orElse(new ConcurrentHashMap());
        initQTableContainer(logFile);
    }

    private void initQTableContainer(IWCLogFile logFile) {
        this.candidateListTimeThreshold = 5184000000L;
        this.probationListTimeThreshold = 5184000000L;
        this.candidateListActivityThreshold = 4;
        this.probationListActivityThreshold = 2;
        this.candidateListMemberLimit = 100;
        this.coreListMemberLimit = 20;
        this.activityScore_WIFI_OFF = 2;
        this.activityScore_MANUAL_RECONNECTION = 2;
        this.activityScore_MANUAL_SWITCH = 2;
        this.activityScore_MANUAL_DISC = 2;
        this.activityScore_CELLULAR_OFF = 2;
        this.activityScore_SNS_ONOFF = 2;
        this.activityScore_DROP_TOO_SHORT = 1;
        this.activityScore_AUTODISC = 1;
        this.useProbationList = true;
        this.IWCLog = logFile;
    }

    public void createTable(String bssid, RFLInterface intf) {
        this.qTableIndexList.add(new IndexNode(bssid));
        QTable newTable = new QTable(PARAM, this.IWCLog);
        int i = this.mDefaultQAI;
        if (i == 1) {
            newTable.qTable[0][0] = 4.0f;
            newTable.qTable[1][0] = 2.0f;
            newTable.qTable[2][0] = 0.0f;
            newTable.setState(0);
        } else if (i == 2) {
            newTable.qTable[0][0] = 2.0f;
            newTable.qTable[1][0] = 4.0f;
            newTable.qTable[2][0] = 2.0f;
            newTable.setState(1);
        } else if (i == 3) {
            newTable.qTable[0][0] = 0.0f;
            newTable.qTable[1][0] = 2.0f;
            newTable.qTable[2][0] = 4.0f;
            newTable.setState(2);
        } else {
            newTable.qTable[0][0] = 0.0f;
            newTable.qTable[1][0] = 0.0f;
            newTable.qTable[2][0] = 0.0f;
            newTable.setState(2);
            Log.e(TAG, "Wrong Default QAI");
        }
        newTable.setLastState(2);
        this.qTableList.add(newTable);
    }

    public void setDefaultQAI(int forced_qai) {
        this.mDefaultQAI = forced_qai;
        this.mForcedlyQAISet = this.mDefaultQAI;
        Log.d(TAG, "setDefaultQAI forcedly, default QAI set to " + this.mDefaultQAI);
        writeIWCLog("setDefaultQAI(int forced_qai)", "" + String.format("setDefaultQAI forcedly, default QAI set to %d", new Object[]{Integer.valueOf(this.mDefaultQAI)}));
    }

    public void setDefaultQAI(RFLInterface intf) {
        int qai = getAverageQAI();
        if (intf.snsOptionChanged) {
            this.mForcedlyQAISet = -1;
        }
        if (qai == -1) {
            int i = this.mForcedlyQAISet;
            if (i > 0) {
                this.mDefaultQAI = i;
                Log.d(TAG, "Default QAI is calculated by mForcedlyQAISet");
                writeIWCLog("setDefaultQAI(RFLInterface intf)", "" + String.format("Default QAI is calculated by mForcedlyQAISet", new Object[0]));
            } else {
                if (intf.aggSnsFlag) {
                    this.mDefaultQAI = 1;
                } else if (intf.snsFlag) {
                    this.mDefaultQAI = 2;
                } else if (!intf.snsFlag) {
                    this.mDefaultQAI = 3;
                }
                Log.d(TAG, "Default QAI is calculated by SNS Option based");
                writeIWCLog("setDefaultQAI(RFLInterface intf)", "" + String.format("Default QAI is calculated by SNS Option based", new Object[0]));
            }
        } else {
            this.mForcedlyQAISet = -1;
            this.mDefaultQAI = qai;
            Log.d(TAG, "Default QAI is calculated by AverageQAI");
            writeIWCLog("setDefaultQAI(RFLInterface intf)", "" + String.format("Default QAI is calculated by AverageQAI", new Object[0]));
        }
        Log.d(TAG, "Default QAI was set " + this.mDefaultQAI);
        writeIWCLog("setDefaultQAI(RFLInterface intf)", "" + String.format("Default QAI was set %d", new Object[]{Integer.valueOf(this.mDefaultQAI)}));
    }

    public int getAverageQAI() {
        float[] pVal = {DefaultImageRequest.OFFSET_DEFAULT, DefaultImageRequest.OFFSET_DEFAULT, DefaultImageRequest.OFFSET_DEFAULT};
        int lastN = 0;
        int orderIdx = 0;
        if (this.qTableList == null) {
            return -1;
        }
        if (this.sortInfoList == null) {
            this.sortInfoList = new ArrayList<>();
            Log.e(TAG, "sortInfoList null --> new");
        }
        for (int i = 0; i < this.qTableList.size(); i++) {
            QTable tempTable = this.qTableList.get(i);
            if (tempTable.getSteadyState()) {
                this.sortInfoList.add(new QTableInfoForSort(tempTable.lastUpdateTime, tempTable.getState()));
                lastN++;
            }
        }
        Collections.sort(this.sortInfoList, new Comparator<QTableInfoForSort>() {
            public int compare(QTableInfoForSort first, QTableInfoForSort second) {
                if (first.lastUpdateTime < second.lastUpdateTime) {
                    return -1;
                }
                if (first.lastUpdateTime > second.lastUpdateTime) {
                    return 1;
                }
                return 0;
            }
        });
        for (int i2 = 0; i2 < this.sortInfoList.size(); i2++) {
            orderIdx++;
            if (this.sortInfoList.get(i2).curState == 0) {
                pVal[0] = pVal[0] + ((float) Math.pow((double) 0.504f, (double) (lastN - orderIdx)));
            } else if (this.sortInfoList.get(i2).curState == 1) {
                pVal[1] = pVal[1] + ((float) Math.pow((double) 0.504f, (double) (lastN - orderIdx)));
            } else if (this.sortInfoList.get(i2).curState == 2) {
                pVal[2] = pVal[2] + ((float) Math.pow((double) 0.504f, (double) (lastN - orderIdx)));
            }
        }
        float refVal = pVal[0];
        int AverageQAI = 1;
        for (int i3 = 1; i3 < 3; i3++) {
            if (refVal < pVal[i3]) {
                refVal = pVal[i3];
                AverageQAI = i3 + 1;
            }
        }
        if (lastN == 0) {
            AverageQAI = -1;
        }
        this.sortInfoList.clear();
        return AverageQAI;
    }

    public void rebaseQTables() {
        ArrayList<QTable> arrayList = this.qTableList;
        if (arrayList != null) {
            int numRemovedTable = 0;
            int cntSStable = 0;
            for (int i = arrayList.size() - 1; i >= 0; i--) {
                if (this.qTableList.get(i).getSteadyState()) {
                    cntSStable++;
                }
            }
            if (cntSStable != 0) {
                Iterator<QTable> qTableListIter = this.qTableList.iterator();
                Iterator<IndexNode> qTableIndexListIter = this.qTableIndexList.iterator();
                while (qTableListIter.hasNext()) {
                    QTable tempTable = qTableListIter.next();
                    IndexNode tempIdxNode = qTableIndexListIter.next();
                    if (tempIdxNode == null) {
                        Log.e(TAG, "rebaseQTables: qTableListIter is null");
                        return;
                    } else if (!tempTable.getSteadyState() && tempTable.eventBuffer.size() < 2) {
                        String bss = tempIdxNode.bssid;
                        if (this.candidateApList.get(bss) != null) {
                            this.candidateApList.remove(bss);
                        } else if (this.coreApList.get(bss) != null) {
                            this.coreApList.remove(bss);
                        } else if (this.probationApList.get(bss) != null) {
                            this.probationApList.remove(bss);
                        }
                        qTableListIter.remove();
                        qTableIndexListIter.remove();
                        numRemovedTable++;
                    }
                }
                writeIWCLog("Rebase QTables", "" + String.format("Num removed tables: %d", new Object[]{Integer.valueOf(numRemovedTable)}));
            }
        }
    }

    public void removeNonSSQtables() {
        ArrayList<QTable> arrayList = this.qTableList;
        if (arrayList != null) {
            int numRemovedTable = 0;
            Iterator<QTable> qTableListIter = arrayList.iterator();
            Iterator<IndexNode> qTableIndexListIter = this.qTableIndexList.iterator();
            while (qTableListIter.hasNext()) {
                QTable tempTable = qTableListIter.next();
                IndexNode tempIdxNode = qTableIndexListIter.next();
                if (tempIdxNode == null) {
                    Log.e(TAG, "removeNonSSQtables: qTableListIter is null");
                    return;
                } else if (!tempTable.getSteadyState()) {
                    String bss = tempIdxNode.bssid;
                    if (this.candidateApList.get(bss) != null) {
                        this.candidateApList.remove(bss);
                    } else if (this.coreApList.get(bss) != null) {
                        this.coreApList.remove(bss);
                    } else if (this.probationApList.get(bss) != null) {
                        this.probationApList.remove(bss);
                    }
                    qTableListIter.remove();
                    qTableIndexListIter.remove();
                    numRemovedTable++;
                }
            }
            writeIWCLog("removeNonSSQtables QTables", "" + String.format("Num removed tables: %d", new Object[]{Integer.valueOf(numRemovedTable)}));
        }
    }

    public int getTableStateCB(String bssid) {
        int tableIndex = findTable(bssid);
        if (tableIndex == -1) {
            Log.e(TAG, "getTableStateCB - findTable returned -1");
            return 2;
        }
        QTable tempTable = this.qTableList.get(tableIndex);
        int bestState = tempTable.getBestState();
        tempTable.setLastState(tempTable.getState());
        tempTable.setState(bestState);
        return bestState;
    }

    public int findTable(String bssid) {
        for (int i = 0; i < this.qTableList.size(); i++) {
            if (this.qTableIndexList.get(i).bssid.equals(bssid)) {
                return i;
            }
        }
        return -1;
    }

    private ArrayList<QTable> findTables(String bssid) {
        ArrayList<QTable> qTablesSingleAp = new ArrayList<>();
        for (int i = 0; i < this.qTableList.size(); i++) {
            if (this.qTableIndexList.get(i).bssid.equals(bssid)) {
                qTablesSingleAp.add(this.qTableList.get(i));
            }
        }
        return qTablesSingleAp;
    }

    public void removeBssidTables(String bssid) {
        this.qTableList.removeAll(findTables(bssid));
        ArrayList<IndexNode> removeIndices = new ArrayList<>();
        Iterator<IndexNode> it = this.qTableIndexList.iterator();
        while (it.hasNext()) {
            IndexNode node = it.next();
            if (node.bssid.equals(bssid)) {
                removeIndices.add(node);
            }
        }
        this.qTableIndexList.removeAll(removeIndices);
    }

    public void updateApListAccessTime(String bssid) {
        if (bssid == null) {
            Log.e(TAG, "updateApListAccessTime - bssid is null");
        } else if (this.coreApList.containsKey(bssid)) {
            ApListNode tempNode = this.coreApList.get(bssid);
            tempNode.lastAccessed = System.currentTimeMillis();
            this.coreApList.put(bssid, tempNode);
        } else if (this.candidateApList.containsKey(bssid)) {
            ApListNode tempNode2 = this.candidateApList.get(bssid);
            tempNode2.lastAccessed = System.currentTimeMillis();
            this.candidateApList.put(bssid, tempNode2);
        } else if (this.probationApList.containsKey(bssid)) {
            ApListNode tempNode3 = this.probationApList.get(bssid);
            tempNode3.lastAccessed = System.currentTimeMillis();
            this.probationApList.put(bssid, tempNode3);
        }
    }

    public void addCandidateList(String bssid) {
        if (this.candidateApList.get(bssid) == null && this.coreApList.get(bssid) == null && this.probationApList.get(bssid) == null) {
            checkOverfilled();
            this.candidateApList.put(bssid, new ApListNode(System.currentTimeMillis()));
        }
    }

    private void checkOverfilled() {
        if (this.candidateApList.size() > this.candidateListMemberLimit) {
            String.valueOf(this.candidateApList.keySet().toArray()[0]);
            int tableIndex = findTable(String.valueOf(this.candidateApList.keySet().toArray()[0]));
            if (tableIndex == -1) {
                Log.e(TAG, "checkOverfilled - findTable returned -1 -> 1st element in candidateApList is not found in qTableIndexList");
                return;
            }
            int leastNumEvents = this.qTableList.get(tableIndex).eventBuffer.size();
            long furthestLastAccessed = System.currentTimeMillis();
            String bssidToRemove = String.valueOf(this.candidateApList.keySet().toArray()[0]);
            for (String bssid : this.candidateApList.keySet()) {
                if (bssid == null) {
                    Log.e(TAG, "checkOverfilled - bssid in candidateApList.keySet() is null -> abnormal candidate AP list management");
                } else {
                    int tableIndex2 = findTable(bssid);
                    if (tableIndex2 == -1) {
                        Log.e(TAG, "checkOverfilled - findTable returned -1 -> QTable(" + bssid + ") is not found in qTableIndexList");
                    } else {
                        QTable tempTable = this.qTableList.get(tableIndex2);
                        if (tempTable.eventBuffer.size() < leastNumEvents || (tempTable.eventBuffer.size() == leastNumEvents && this.candidateApList.get(bssid).lastAccessed < furthestLastAccessed)) {
                            bssidToRemove = bssid;
                            leastNumEvents = tempTable.eventBuffer.size();
                            furthestLastAccessed = this.candidateApList.get(bssid).lastAccessed;
                        }
                    }
                }
            }
            if (this.candidateApList.get(bssidToRemove) != null) {
                this.candidateApList.remove(bssidToRemove);
                writeLog(bssidToRemove, "CandidateAPList");
                removeBssidTables(bssidToRemove);
                Log.e(TAG, "Candidate list is overfilled. Remove " + bssidToRemove);
            }
        }
    }

    public void manageApList() {
        checkOverfilled();
        for (String bssid : this.candidateApList.keySet()) {
            if (bssid == null) {
                Log.e(TAG, "manageApList - bssid in candidateApList.keySet() is null -> abnormal candidate AP list management");
            } else if (this.candidateApList.get(bssid) != null && System.currentTimeMillis() - this.candidateApList.get(bssid).firstAdded > this.candidateListTimeThreshold) {
                if (this.candidateApList.get(bssid).activityScore >= this.candidateListActivityThreshold) {
                    moveToCoreList(bssid);
                } else {
                    this.candidateApList.remove(bssid);
                    writeLog(bssid, "CandidateList");
                    removeBssidTables(bssid);
                }
            }
        }
        if (this.useProbationList) {
            for (String bssid2 : this.probationApList.keySet()) {
                if (bssid2 == null) {
                    Log.e(TAG, "manageApList - bssid in probationApList.keySet() is null -> abnormal Probation AP list management");
                } else if (this.probationApList.get(bssid2) != null && System.currentTimeMillis() - this.probationApList.get(bssid2).firstAdded > this.probationListTimeThreshold) {
                    if (this.probationApList.get(bssid2).activityScore >= this.probationListActivityThreshold) {
                        moveToCoreList(bssid2);
                    } else {
                        this.probationApList.remove(bssid2);
                        writeLog(bssid2, "ProbationList");
                        removeBssidTables(bssid2);
                    }
                }
            }
        }
    }

    private void moveToCoreList(String bssid) {
        if (bssid == null) {
            Log.e(TAG, "moveToCoreList - bssid is null");
            return;
        }
        ApListNode temp = new ApListNode(System.currentTimeMillis());
        if (this.candidateApList.get(bssid) != null) {
            temp.lastAccessed = this.candidateApList.get(bssid).lastAccessed;
            this.coreApList.put(bssid, temp);
            this.candidateApList.remove(bssid);
            writeLog(bssid, "CandidateList->CoreList");
        } else if (this.probationApList.get(bssid) != null) {
            temp.lastAccessed = this.probationApList.get(bssid).lastAccessed;
            this.coreApList.put(bssid, temp);
            this.probationApList.remove(bssid);
            writeLog(bssid, "ProbationList->CoreList");
        }
        if (this.coreApList.size() > this.coreListMemberLimit) {
            long furthestLastAccessed = System.currentTimeMillis();
            String.valueOf(this.coreApList.keySet().toArray()[0]);
            String bssidToRemove = String.valueOf(this.coreApList.keySet().toArray()[0]);
            for (String tmpbssid : this.coreApList.keySet()) {
                if (tmpbssid == null) {
                    Log.e(TAG, "moveToCoreList - bssid in coreApList.keySet() is null -> abnormal core AP list management");
                } else if (this.coreApList.get(tmpbssid) != null && this.coreApList.get(tmpbssid).lastAccessed < furthestLastAccessed) {
                    bssidToRemove = tmpbssid;
                    furthestLastAccessed = this.coreApList.get(tmpbssid).lastAccessed;
                }
            }
            if (this.useProbationList) {
                ApListNode temp2 = new ApListNode(System.currentTimeMillis());
                temp2.lastAccessed = this.coreApList.get(bssidToRemove).lastAccessed;
                this.probationApList.put(bssidToRemove, temp2);
                this.coreApList.remove(bssidToRemove);
                writeLog(bssidToRemove, "CoreList->ProbationList");
                return;
            }
            this.coreApList.remove(bssidToRemove);
            writeLog(bssidToRemove, "CoreList");
            removeBssidTables(bssidToRemove);
        }
    }

    public void recordApActivity(String bssid, RewardEvent event) {
        if (bssid == null) {
            Log.e(TAG, "RecordApActivity - bssid is null");
        } else if (this.candidateApList.get(bssid) != null) {
            ApListNode tempNode = this.candidateApList.get(bssid);
            switch (event) {
                case MANUAL_SWITCH:
                case MANUAL_SWITCH_G:
                case MANUAL_SWITCH_L:
                    tempNode.activityScore += this.activityScore_MANUAL_SWITCH;
                    this.candidateApList.put(bssid, tempNode);
                    return;
                case MANUAL_RECONNECTION:
                    tempNode.activityScore += this.activityScore_MANUAL_RECONNECTION;
                    this.candidateApList.put(bssid, tempNode);
                    return;
                case WIFI_OFF:
                    tempNode.activityScore += this.activityScore_WIFI_OFF;
                    this.candidateApList.put(bssid, tempNode);
                    return;
                case MANUAL_DISCONNECT:
                    tempNode.activityScore += this.activityScore_MANUAL_DISC;
                    this.candidateApList.put(bssid, tempNode);
                    return;
                case CONNECTION_SWITCHED_TOO_SHORT:
                    tempNode.activityScore += this.activityScore_DROP_TOO_SHORT;
                    this.candidateApList.put(bssid, tempNode);
                    return;
                case CELLULAR_DATA_OFF:
                    tempNode.activityScore += this.activityScore_CELLULAR_OFF;
                    this.candidateApList.put(bssid, tempNode);
                    return;
                case AUTO_DISCONNECTION:
                    tempNode.activityScore += this.activityScore_AUTODISC;
                    this.candidateApList.put(bssid, tempNode);
                    return;
                case SNS_ON:
                case SNS_OFF:
                    tempNode.activityScore += this.activityScore_SNS_ONOFF;
                    this.candidateApList.put(bssid, tempNode);
                    return;
                default:
                    return;
            }
        } else if (this.useProbationList && this.probationApList.get(bssid) != null) {
            ApListNode tempNode2 = this.probationApList.get(bssid);
            switch (event) {
                case MANUAL_SWITCH:
                case MANUAL_SWITCH_G:
                case MANUAL_SWITCH_L:
                    tempNode2.activityScore += this.activityScore_MANUAL_SWITCH;
                    this.probationApList.put(bssid, tempNode2);
                    return;
                case MANUAL_RECONNECTION:
                    tempNode2.activityScore += this.activityScore_MANUAL_RECONNECTION;
                    this.probationApList.put(bssid, tempNode2);
                    return;
                case WIFI_OFF:
                    tempNode2.activityScore += this.activityScore_WIFI_OFF;
                    this.probationApList.put(bssid, tempNode2);
                    return;
                case MANUAL_DISCONNECT:
                    tempNode2.activityScore += this.activityScore_MANUAL_DISC;
                    this.probationApList.put(bssid, tempNode2);
                    return;
                case CONNECTION_SWITCHED_TOO_SHORT:
                    tempNode2.activityScore += this.activityScore_DROP_TOO_SHORT;
                    this.probationApList.put(bssid, tempNode2);
                    return;
                case CELLULAR_DATA_OFF:
                    tempNode2.activityScore += this.activityScore_CELLULAR_OFF;
                    this.probationApList.put(bssid, tempNode2);
                    return;
                case AUTO_DISCONNECTION:
                    tempNode2.activityScore += this.activityScore_AUTODISC;
                    this.probationApList.put(bssid, tempNode2);
                    return;
                case SNS_ON:
                case SNS_OFF:
                    tempNode2.activityScore += this.activityScore_SNS_ONOFF;
                    this.probationApList.put(bssid, tempNode2);
                    return;
                default:
                    return;
            }
        }
    }

    public void writeIWCLog(String valueName, String value) {
        IWCLogFile iWCLogFile = this.IWCLog;
        if (iWCLogFile != null) {
            iWCLogFile.writeToLogFile(valueName, value);
        }
    }

    public void writeLog(String bssid, String apListChangeType) {
        String logValue = ("" + String.format("bssid: %s %s >> ", new Object[]{bssid, apListChangeType})) + String.format("Candidate List: [", new Object[0]);
        for (String key : this.candidateApList.keySet()) {
            logValue = logValue + String.format(" <%s;%s;%s> ", new Object[]{key, Integer.valueOf(this.candidateApList.get(key).activityScore), Long.valueOf(System.currentTimeMillis() - this.candidateApList.get(key).firstAdded)});
        }
        String logValue2 = logValue + "], Core List: [";
        for (String key2 : this.coreApList.keySet()) {
            logValue2 = logValue2 + String.format(" <%s;%s> ", new Object[]{key2, Long.valueOf(this.coreApList.get(key2).lastAccessed)});
        }
        String logValue3 = logValue2 + "], Probation List: [";
        for (String key3 : this.probationApList.keySet()) {
            logValue3 = logValue3 + String.format(" <%s;%s;%s> ", new Object[]{key3, Integer.valueOf(this.probationApList.get(key3).activityScore), Long.valueOf(System.currentTimeMillis() - this.probationApList.get(key3).firstAdded)});
        }
        writeIWCLog("AP List Change", logValue3 + "]");
    }

    public static class SavedMembers {
        public Map<String, Set<Long>> bssidPerSsidList;
        public Map<String, ApListNode> candidateApList;
        public Map<String, ApListNode> coreApList;
        public int forcedqai;
        public Map<String, ApListNode> probationApList;
        public ArrayList<IndexNode> qTableIndexList;
        public ArrayList<QTable> qTableList;
        public int qai;
        public long version;

        public SavedMembers(QTableContainer src) {
            this.version = QTableContainer.serialVersionUID;
            this.qai = src.mDefaultQAI;
            this.forcedqai = src.mForcedlyQAISet;
            this.qTableList = src.qTableList;
            this.qTableIndexList = src.qTableIndexList;
            this.coreApList = src.coreApList;
            this.candidateApList = src.candidateApList;
            this.probationApList = src.probationApList;
            this.bssidPerSsidList = src.bssidPerSsidList;
        }

        public SavedMembers() {
        }

        public QTableContainer readResolve(IWCLogFile logFile) {
            return new QTableContainer(this, logFile);
        }
    }

    public SavedMembers writeReplace() {
        return new SavedMembers(this);
    }

    public int removeQtableIfExist(String bssid) {
        int index = findTable(bssid);
        if (!(bssid == null || index == -1)) {
            removeBssidTables(bssid);
            this.candidateApList.remove(bssid);
            this.probationApList.remove(bssid);
            this.coreApList.remove(bssid);
        }
        return index;
    }

    public static Long convertBssid(String bssid) {
        if (bssid == null) {
            return -1L;
        }
        return Long.valueOf(bssid.replaceAll(":", ""), 16);
    }

    public static String toMacString(long mac) {
        if (mac > 281474976710655L || mac < 0) {
            throw new IllegalArgumentException("mac out of range");
        }
        StringBuffer m = new StringBuffer(Long.toString(mac, 16));
        while (m.length() < 12) {
            m.insert(0, "0");
        }
        for (int j = m.length() - 2; j >= 2; j -= 2) {
            m.insert(j, ":");
        }
        return m.toString().toLowerCase();
    }

    public Set<Long> putBssidToConfigKey(String configKey, String bssid) {
        Long macAddress = convertBssid(bssid);
        if (macAddress.longValue() == -1) {
            return null;
        }
        Set<Long> v = this.bssidPerSsidList.get(configKey);
        if (v != null) {
            v.add(macAddress);
        } else {
            v = new HashSet<>();
            v.add(macAddress);
        }
        return this.bssidPerSsidList.put(configKey, v);
    }

    public Set<Long> removeConfigKey(String configKey) {
        Set<Long> bssids = this.bssidPerSsidList.get(configKey);
        if (bssids == null) {
            return null;
        }
        bssids.forEach(new Consumer() {
            public final void accept(Object obj) {
                QTableContainer.this.lambda$removeConfigKey$0$QTableContainer((Long) obj);
            }
        });
        return this.bssidPerSsidList.remove(configKey);
    }

    public /* synthetic */ void lambda$removeConfigKey$0$QTableContainer(Long v) {
        removeQtableIfExist(toMacString(v.longValue()));
    }
}
