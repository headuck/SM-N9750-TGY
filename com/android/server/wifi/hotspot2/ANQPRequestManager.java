package com.android.server.wifi.hotspot2;

import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.Clock;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ANQPRequestManager {
    @VisibleForTesting
    public static final int BASE_HOLDOFF_TIME_MILLISECONDS = 10000;
    @VisibleForTesting
    public static final int MAX_HOLDOFF_COUNT = 6;
    private static final List<Constants.ANQPElementType> R1_ANQP_BASE_SET = Arrays.asList(new Constants.ANQPElementType[]{Constants.ANQPElementType.ANQPVenueName, Constants.ANQPElementType.ANQPIPAddrAvailability, Constants.ANQPElementType.ANQPNAIRealm, Constants.ANQPElementType.ANQP3GPPNetwork, Constants.ANQPElementType.ANQPDomName});
    private static final List<Constants.ANQPElementType> R2_ANQP_BASE_SET = Arrays.asList(new Constants.ANQPElementType[]{Constants.ANQPElementType.HSFriendlyName, Constants.ANQPElementType.HSWANMetrics, Constants.ANQPElementType.HSConnCapability, Constants.ANQPElementType.HSOSUProviders});
    private static final String TAG = "ANQPRequestManager";
    /* access modifiers changed from: private */
    public final Clock mClock;
    private final Map<Long, HoldOffInfo> mHoldOffInfo = new HashMap();
    private final PasspointEventHandler mPasspointHandler;
    private final Map<Long, ANQPNetworkKey> mPendingQueries = new HashMap();

    private class HoldOffInfo {
        public int holdOffCount;
        public long holdOffExpirationTime;

        private HoldOffInfo() {
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            long currentTime = ANQPRequestManager.this.mClock.getElapsedSinceBootMillis();
            sb.append(" holdOffCount, ");
            sb.append(Integer.toString(this.holdOffCount));
            sb.append(" Not allowed to send ANQP request for ");
            sb.append(Utils.toHMS(this.holdOffExpirationTime - currentTime));
            sb.append(" ");
            return sb.toString();
        }
    }

    public ANQPRequestManager(PasspointEventHandler handler, Clock clock) {
        this.mPasspointHandler = handler;
        this.mClock = clock;
    }

    public boolean requestANQPElements(long bssid, ANQPNetworkKey anqpNetworkKey, boolean rcOIs, boolean hsReleaseR2) {
        if (!canSendRequestNow(bssid) || !this.mPasspointHandler.requestANQP(bssid, getRequestElementIDs(rcOIs, hsReleaseR2))) {
            return false;
        }
        updateHoldOffInfo(bssid);
        this.mPendingQueries.put(Long.valueOf(bssid), anqpNetworkKey);
        return true;
    }

    public ANQPNetworkKey onRequestCompleted(long bssid, boolean success) {
        if (success) {
            this.mHoldOffInfo.remove(Long.valueOf(bssid));
        }
        return this.mPendingQueries.remove(Long.valueOf(bssid));
    }

    private boolean canSendRequestNow(long bssid) {
        long currentTime = this.mClock.getElapsedSinceBootMillis();
        HoldOffInfo info = this.mHoldOffInfo.get(Long.valueOf(bssid));
        if (info == null || info.holdOffExpirationTime <= currentTime) {
            return true;
        }
        Log.d(TAG, "Not allowed to send ANQP request to " + bssid + " for another " + ((info.holdOffExpirationTime - currentTime) / 1000) + " seconds");
        return false;
    }

    private void updateHoldOffInfo(long bssid) {
        HoldOffInfo info = this.mHoldOffInfo.get(Long.valueOf(bssid));
        if (info == null) {
            info = new HoldOffInfo();
            this.mHoldOffInfo.put(Long.valueOf(bssid), info);
        }
        info.holdOffExpirationTime = this.mClock.getElapsedSinceBootMillis() + ((long) ((1 << info.holdOffCount) * 10000));
        if (info.holdOffCount < 6) {
            info.holdOffCount++;
        }
    }

    private static List<Constants.ANQPElementType> getRequestElementIDs(boolean rcOIs, boolean hsReleaseR2) {
        List<Constants.ANQPElementType> requestList = new ArrayList<>();
        requestList.addAll(R1_ANQP_BASE_SET);
        if (rcOIs) {
            requestList.add(Constants.ANQPElementType.ANQPRoamingConsortium);
        }
        if (hsReleaseR2) {
            requestList.addAll(R2_ANQP_BASE_SET);
        }
        return requestList;
    }

    public void dump(PrintWriter out) {
        out.println("ANQPRequestManager - HoldOffInfo");
        for (Map.Entry<Long, HoldOffInfo> entry : this.mHoldOffInfo.entrySet()) {
            out.println(Utils.macToString(entry.getKey().longValue()) + ": " + entry.getValue());
        }
    }

    public void clearANQPRequests() {
        this.mHoldOffInfo.clear();
        this.mPendingQueries.clear();
    }
}
