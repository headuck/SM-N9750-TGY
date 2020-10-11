package com.android.server.wifi.iwc.rlengine;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import com.android.server.wifi.hotspot2.anqp.NAIRealmData;
import com.android.server.wifi.iwc.IWCLogFile;
import com.android.server.wifi.iwc.RewardEvent;
import com.android.server.wifi.tcp.WifiTransportLayerUtils;
import java.util.Iterator;

public class RewardManager {
    private static final String TAG = "IWCMonitor.RM";
    public int INITREWARDTIMES = 7;
    private IWCLogFile IWCLog;
    float NReward = -10.0f;
    float PReward = 1.0f;
    float PReward10 = 10.0f;
    float R1Reward = 1.0f;
    float R2Reward = -10.0f;
    float R3Reward = 10.0f;
    public float alpha = 0.8f;
    public float alpha_half = 0.9f;
    public float beta = 0.8f;
    public float gamma = 1.0f;
    public float gamma_dis = 0.98f;
    public int iTimes;
    public boolean mSwitchFlag;
    public String storedCurrentAP;

    public RewardManager(IWCLogFile logFile) {
        this.IWCLog = logFile;
    }

    public void _sendDebugIntent(Context ctx, int kind, String strEvent, String strBss, int idx, QTable qt) {
        Intent intent = new Intent("com.sec.android.IWC_REWARD_EVENT_DEBUG");
        intent.putExtra("kind", kind);
        intent.putExtra("event", strEvent);
        intent.putExtra("bssid", strBss);
        intent.putExtra("tableindex", idx);
        intent.putExtra("lastvalue1", qt.qTable[0][0]);
        intent.putExtra("lastvalue2", qt.qTable[1][0]);
        intent.putExtra("lastvalue3", qt.qTable[2][0]);
        intent.putExtra("ss_poor", qt.getSteadyState());
        intent.putExtra("qai", qt.mLastSNS == 1 ? qt.getState() + 1 : -1);
        ctx.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void sendDebugIntent(Context ctx, String strEvent, String strBss, int idx, QTable qt) {
        _sendDebugIntent(ctx, 1, strEvent, strBss, idx, qt);
    }

    public void updateDebugIntent(Context ctx, String strEvent, String strBss, int idx, QTable qt) {
        _sendDebugIntent(ctx, 4, strEvent, strBss, idx, qt);
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int applyRewards(com.android.server.wifi.iwc.RewardEvent r22, com.android.server.wifi.iwc.rlengine.QTableContainer r23, com.android.server.wifi.iwc.rlengine.RFLInterface r24, long r25) {
        /*
            r21 = this;
            r10 = r21
            r11 = r23
            r12 = r24
            r13 = 0
            java.lang.String r0 = r12.currentApBssid_IN
            r10.storedCurrentAP = r0
            boolean r0 = r12.switchFlag
            r10.mSwitchFlag = r0
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r14 = 3
            r0.setIdInfo(r14)
            java.lang.String r0 = r10.storedCurrentAP
            int r15 = r11.findTable(r0)
            java.lang.String r0 = "IWCMonitor.RM"
            r8 = -1
            if (r15 != r8) goto L_0x0026
            java.lang.String r1 = "ApplyRewards - findTable returned -1"
            android.util.Log.e(r0, r1)
            return r8
        L_0x0026:
            java.util.ArrayList<com.android.server.wifi.iwc.rlengine.QTable> r1 = r11.qTableList
            java.lang.Object r1 = r1.get(r15)
            r9 = r1
            com.android.server.wifi.iwc.rlengine.QTable r9 = (com.android.server.wifi.iwc.rlengine.QTable) r9
            int r7 = r9.getState()
            int r16 = r9.getLastAction()
            int r17 = r9.getLastState()
            int[] r1 = com.android.server.wifi.iwc.rlengine.RewardManager.C05201.$SwitchMap$com$android$server$wifi$iwc$RewardEvent
            int r2 = r22.ordinal()
            r1 = r1[r2]
            r6 = 2
            r5 = 0
            r4 = 1
            switch(r1) {
                case 1: goto L_0x03d8;
                case 2: goto L_0x0399;
                case 3: goto L_0x0371;
                case 4: goto L_0x033c;
                case 5: goto L_0x0306;
                case 6: goto L_0x02d1;
                case 7: goto L_0x0296;
                case 8: goto L_0x025b;
                case 9: goto L_0x0216;
                case 10: goto L_0x01d6;
                case 11: goto L_0x015b;
                case 12: goto L_0x005e;
                case 13: goto L_0x0057;
                case 14: goto L_0x0050;
                default: goto L_0x0049;
            }
        L_0x0049:
            r1 = r4
            r11 = r9
            r19 = r13
            r13 = r7
            goto L_0x0415
        L_0x0050:
            r1 = r4
            r11 = r9
            r19 = r13
            r13 = r7
            goto L_0x0415
        L_0x0057:
            r1 = r4
            r11 = r9
            r19 = r13
            r13 = r7
            goto L_0x0415
        L_0x005e:
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "network_connected cur state : "
            r1.append(r2)
            boolean r2 = r12.snsFlag
            r1.append(r2)
            java.lang.String r2 = " "
            r1.append(r2)
            boolean r3 = r12.aggSnsFlag
            r1.append(r3)
            java.lang.String r3 = "   Saved state : "
            r1.append(r3)
            int r3 = r9.mLastSNS
            r1.append(r3)
            r1.append(r2)
            int r2 = r9.mLastAGG
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.d(r0, r1)
            java.lang.String r1 = r10.storedCurrentAP
            boolean r14 = r12.snsFlag
            boolean r5 = r12.aggSnsFlag
            java.lang.String r3 = "NETWORK_CONNECTED initial value "
            r0 = r21
            r2 = r9
            r11 = r4
            r4 = r17
            r18 = r5
            r5 = r16
            r6 = r14
            r14 = r7
            r7 = r18
            r11 = r9
            r19 = r13
            r13 = r8
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            int r0 = r11.mLastSNS
            if (r0 == r13) goto L_0x0147
            int r0 = r11.mLastAGG
            if (r0 == r13) goto L_0x0147
            boolean r0 = r12.snsFlag
            r1 = 1
            if (r0 != r1) goto L_0x00fd
            boolean r0 = r12.aggSnsFlag
            if (r0 != r1) goto L_0x00fd
            int r0 = r11.mLastSNS
            if (r0 != r1) goto L_0x00c8
            int r0 = r11.mLastAGG
            if (r0 == 0) goto L_0x00cc
        L_0x00c8:
            int r0 = r11.mLastSNS
            if (r0 != 0) goto L_0x0147
        L_0x00cc:
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.NETWORK_CONNECTED_WITH_SNS_ON
            int r13 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r11 = r10.moreAggressiveRewardUpdateCB(r11, r14)
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "SNS ON indirect : M"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 11
            r0.set24HEventAccWithIdx(r1)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            r9 = r11
            goto L_0x014a
        L_0x00fd:
            boolean r0 = r12.snsFlag
            if (r0 != 0) goto L_0x0147
            boolean r0 = r12.aggSnsFlag
            if (r0 != 0) goto L_0x0147
            int r0 = r11.mLastSNS
            r1 = 1
            if (r0 != r1) goto L_0x010e
            int r0 = r11.mLastAGG
            if (r0 == r1) goto L_0x0116
        L_0x010e:
            int r0 = r11.mLastSNS
            if (r0 != r1) goto L_0x0147
            int r0 = r11.mLastAGG
            if (r0 != 0) goto L_0x0147
        L_0x0116:
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.NETWORK_CONNECTED_WITH_SNS_OFF
            int r13 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r11 = r10.lessAggressiveRewardUpdateCB(r11, r14)
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "SNS OFF indirect : L"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 12
            r0.set24HEventAccWithIdx(r1)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            r9 = r11
            goto L_0x014a
        L_0x0147:
            r9 = r11
            r13 = r19
        L_0x014a:
            boolean r0 = r12.snsFlag
            r9.mLastSNS = r0
            boolean r0 = r12.aggSnsFlag
            r9.mLastAGG = r0
            r11 = r9
            r1 = 1
            r20 = r14
            r14 = r13
            r13 = r20
            goto L_0x0417
        L_0x015b:
            r14 = r7
            r11 = r9
            r19 = r13
            r11.mLastSNS = r5
            r11.mLastAGG = r5
            boolean r0 = r12.switchFlag
            r13 = 10
            if (r0 != 0) goto L_0x019b
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.SNS_OFF
            int r19 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r11 = r10.lessAggressiveRewardUpdateCB(r11, r14)
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "SNS OFF : L"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r0.set24HEventAccWithIdx(r13)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            r13 = r14
            r14 = r19
            goto L_0x0417
        L_0x019b:
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.SNS_OFF
            int r19 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r0 = r10.lessAggressiveRewardUpdateCB(r11, r14)
            com.android.server.wifi.iwc.rlengine.QTable r11 = r10.lessAggressiveRewardUpdateCB(r0, r14)
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "SNS OFF : LL"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r0.set24HEventAccWithIdx(r13)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r0.set24HEventAccWithIdx(r13)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            r13 = r14
            r14 = r19
            goto L_0x0417
        L_0x01d6:
            r1 = r4
            r14 = r7
            r11 = r9
            r19 = r13
            r11.mLastSNS = r1
            r11.mLastAGG = r1
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.SNS_ON
            int r13 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r11 = r10.moreAggressiveRewardUpdateCB(r11, r14)
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "SNS ON : M"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 9
            r0.set24HEventAccWithIdx(r1)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            r1 = 1
            r20 = r14
            r14 = r13
            r13 = r20
            goto L_0x0417
        L_0x0216:
            r11 = r9
            r19 = r13
            r13 = r7
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.AUTO_DISCONNECTION
            int r19 = r11.addEvent(r0, r1, r2)
            float[][] r0 = r11.qTable
            r0 = r0[r13]
            float[][] r1 = r11.qTable
            r1 = r1[r13]
            r1 = r1[r5]
            float r2 = r10.gamma_dis
            float r1 = r1 * r2
            r2 = 1036831949(0x3dcccccd, float:0.1)
            float r1 = r1 + r2
            r0[r5] = r1
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "auto_disconnection:M"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 6
            r0.set24HEventAccWithIdx(r1)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r0.setIdInfo(r14)
            r14 = r19
            r1 = 1
            goto L_0x0417
        L_0x025b:
            r11 = r9
            r19 = r13
            r13 = r7
            boolean r0 = r12.edgeFlag
            if (r0 == 0) goto L_0x0293
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.CELLULAR_DATA_OFF
            int r14 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r11 = r10.lessAggressiveRewardUpdateCB(r11, r13)
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "cellular_data_off:L"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 7
            r0.set24HEventAccWithIdx(r1)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            goto L_0x0417
        L_0x0293:
            r1 = 1
            goto L_0x0415
        L_0x0296:
            r11 = r9
            r19 = r13
            r13 = r7
            boolean r0 = r12.edgeFlag
            if (r0 == 0) goto L_0x02ce
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.WIFI_OFF
            int r14 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r11 = r10.moreAggressiveRewardUpdateCB(r11, r13)
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "wifi-off:M"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 5
            r0.set24HEventAccWithIdx(r1)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            goto L_0x0417
        L_0x02ce:
            r1 = 1
            goto L_0x0415
        L_0x02d1:
            r11 = r9
            r19 = r13
            r13 = r7
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.MANUAL_RECONNECTION
            int r14 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r11 = r10.lessAggressiveRewardUpdateCB(r11, r13)
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "Manual_reconection:L"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 4
            r0.set24HEventAccWithIdx(r1)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            r1 = 1
            goto L_0x0417
        L_0x0306:
            r11 = r9
            r19 = r13
            r13 = r7
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.CONNECTION_SWITCHED_TOO_SHORT
            int r19 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r11 = r10.lessAggressiveRewardUpdateCB(r11, r13)
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "connection_switched_too_short:L"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r0.set24HEventAccWithIdx(r14)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            r14 = r19
            r1 = 1
            goto L_0x0417
        L_0x033c:
            r11 = r9
            r19 = r13
            r13 = r7
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.MANUAL_SWITCH_L
            int r14 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r11 = r10.lessAggressiveRewardUpdateCB(r11, r13)
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "Manual_switch_L:L"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r8 = 2
            r0.set24HEventAccWithIdx(r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            r1 = 1
            goto L_0x0417
        L_0x0371:
            r8 = r6
            r11 = r9
            r19 = r13
            r13 = r7
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "Manual_switch_G:X"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r14 = r8
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.set24HEventAccWithIdx(r1)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r0.setIdInfo(r1)
            r1 = 1
            goto L_0x0415
        L_0x0399:
            r14 = r6
            r11 = r9
            r19 = r13
            r13 = r7
            boolean r0 = r12.edgeFlag
            if (r0 == 0) goto L_0x03b3
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.MANUAL_SWITCH
            int r0 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r1 = r10.moreAggressiveHalfRewardUpdateCB(r11, r13)
            r19 = r0
            r11 = r1
        L_0x03b3:
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "Manual_switch:halfM"
            r0 = r21
            r2 = r11
            r4 = r17
            r8 = r5
            r5 = r16
            r14 = r8
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r0.set24HEventAccWithIdx(r14)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            r14 = r19
            r1 = 1
            goto L_0x0417
        L_0x03d8:
            r11 = r9
            r19 = r13
            r13 = r7
            boolean r0 = r12.edgeFlag
            if (r0 == 0) goto L_0x03f1
            boolean r0 = r12.snsFlag
            boolean r1 = r12.aggSnsFlag
            com.android.server.wifi.iwc.RewardEvent r2 = com.android.server.wifi.iwc.RewardEvent.MANUAL_DISCONNECT
            int r0 = r11.addEvent(r0, r1, r2)
            com.android.server.wifi.iwc.rlengine.QTable r1 = r10.moreAggressiveRewardUpdateCB(r11, r13)
            r19 = r0
            r11 = r1
        L_0x03f1:
            java.lang.String r1 = r10.storedCurrentAP
            boolean r6 = r12.snsFlag
            boolean r7 = r12.aggSnsFlag
            java.lang.String r3 = "Manual_disconnect:M"
            r0 = r21
            r2 = r11
            r4 = r17
            r5 = r16
            r8 = r25
            r0.writeLog(r1, r2, r3, r4, r5, r6, r7, r8)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 8
            r0.set24HEventAccWithIdx(r1)
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 1
            r0.setIdInfo(r1)
            r14 = r19
            goto L_0x0417
        L_0x0415:
            r14 = r19
        L_0x0417:
            if (r14 != r1) goto L_0x041f
            com.android.server.wifi.iwc.IWCBDTracking r0 = r12.mBdTracking
            r1 = 2
            r0.setIdInfo(r1)
        L_0x041f:
            return r14
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.iwc.rlengine.RewardManager.applyRewards(com.android.server.wifi.iwc.RewardEvent, com.android.server.wifi.iwc.rlengine.QTableContainer, com.android.server.wifi.iwc.rlengine.RFLInterface, long):int");
    }

    public String getEventTypeString(RewardEvent rwEvent, boolean switchFlag) {
        switch (rwEvent) {
            case MANUAL_DISCONNECT:
                return "Manual_disconnect:M";
            case MANUAL_SWITCH:
                return "Manual_switch:halfM";
            case MANUAL_SWITCH_G:
                return "Manual_switch_G:X";
            case MANUAL_SWITCH_L:
                return "Manual_switch_L:L";
            case CONNECTION_SWITCHED_TOO_SHORT:
                return "connection_switched_too_short:L";
            case MANUAL_RECONNECTION:
                return "Manual_reconection:L";
            case WIFI_OFF:
                return "wifi-off:M";
            case CELLULAR_DATA_OFF:
                return "cellular_data_off:L";
            case AUTO_DISCONNECTION:
                return "auto_disconnection:A";
            case SNS_ON:
                return "SNS ON : M";
            case SNS_OFF:
                if (!switchFlag) {
                    return "SNS OFF : L";
                }
                return "SNS OFF : LL";
            case NETWORK_CONNECTED:
                return "WiFi Network Changed";
            default:
                return WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE;
        }
    }

    public QTable moreAggressiveRewardUpdateCB(QTable qtable, int curState) {
        float discount = 1.0f;
        for (int idxstate = 0; idxstate < qtable.numStates; idxstate++) {
            qtable.qTable[idxstate][0] = this.alpha * qtable.qTable[idxstate][0];
        }
        if (curState == 0) {
            qtable.qTable[curState][0] = qtable.qTable[curState][0] + (this.gamma * 1.0f);
        } else {
            for (int idxstate2 = curState - 1; idxstate2 >= 0; idxstate2--) {
                qtable.qTable[idxstate2][0] = qtable.qTable[idxstate2][0] + (this.gamma * discount);
                discount *= this.beta;
            }
        }
        return qtable;
    }

    public QTable moreAggressiveHalfRewardUpdateCB(QTable qtable, int curState) {
        float discount = 0.5f;
        for (int idxstate = 0; idxstate < qtable.numStates; idxstate++) {
            qtable.qTable[idxstate][0] = this.alpha_half * qtable.qTable[idxstate][0];
        }
        if (curState == 0) {
            qtable.qTable[curState][0] = qtable.qTable[curState][0] + (this.gamma * 0.5f);
        } else {
            for (int idxstate2 = curState - 1; idxstate2 >= 0; idxstate2--) {
                qtable.qTable[idxstate2][0] = qtable.qTable[idxstate2][0] + (this.gamma * discount);
                discount *= this.beta;
            }
        }
        return qtable;
    }

    public QTable lessAggressiveRewardUpdateCB(QTable qtable, int curState) {
        float discount = 1.0f;
        for (int idxstate = 0; idxstate < qtable.numStates; idxstate++) {
            qtable.qTable[idxstate][0] = this.alpha * qtable.qTable[idxstate][0];
        }
        if (curState == qtable.numStates - 1) {
            qtable.qTable[curState][0] = qtable.qTable[curState][0] + this.gamma;
        } else {
            for (int idxstate2 = curState + 1; idxstate2 < qtable.numStates; idxstate2++) {
                qtable.qTable[idxstate2][0] = qtable.qTable[idxstate2][0] + (this.gamma * 1.0f * discount);
                discount *= this.beta;
            }
        }
        return qtable;
    }

    public void writeLog(String AP, QTable tempTable, String event, int state, int action, boolean snsFlag, boolean aggFlag, long timestamp) {
        QTable qTable = tempTable;
        String str = event;
        long j = timestamp;
        String logValue = ("" + String.format("CAP: %s, Event: %s>> ", new Object[]{AP, str})) + String.format("Q-Table: [", new Object[0]);
        for (int i = 0; i < qTable.numStates; i++) {
            logValue = (logValue + String.format(" %f", new Object[]{Float.valueOf(qTable.qTable[i][0])})) + String.format(NAIRealmData.NAI_REALM_STRING_SEPARATOR, new Object[0]);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(logValue);
        sb.append(String.format(" ] >> swflag=" + this.mSwitchFlag + " timestamp =" + j, new Object[0]));
        String logValue2 = sb.toString();
        IWCLogFile iWCLogFile = this.IWCLog;
        if (iWCLogFile != null) {
            iWCLogFile.writeToLogFile("Q-Table reward update", logValue2);
        }
        if (!str.equals("NETWORK_CONNECTED initial value ")) {
            String logValue3 = ("" + String.format("isSteadyState: %b, snsFlag: %b, aggFlag: %b, ", new Object[]{Boolean.valueOf(tempTable.getSteadyState()), Boolean.valueOf(snsFlag), Boolean.valueOf(aggFlag)})) + String.format("EventBuffer: [", new Object[0]);
            Iterator<Integer> iter = qTable.eventBuffer.iterator();
            while (iter.hasNext()) {
                int actionType = iter.next().intValue();
                logValue3 = logValue3 + String.format(" %d", new Object[]{Integer.valueOf(actionType)});
            }
            String logValue4 = (logValue3 + String.format(" ]", new Object[0])) + " timestamp =" + j;
            IWCLogFile iWCLogFile2 = this.IWCLog;
            if (iWCLogFile2 != null) {
                iWCLogFile2.writeToLogFile("Steady State", logValue4);
            }
        }
    }
}
