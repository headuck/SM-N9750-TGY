package com.samsung.android.server.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Debug;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.samsung.android.net.wifi.OpBrandingLoader;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class UnstableApController {
    /* access modifiers changed from: private */
    public static final boolean DBG = Debug.semIsProductDev();
    private static final int DISABLED_ASSOC_REJECT = 2;
    private static final int DISABLED_UNSTABLE_AP = 19;
    private static final int DISABLED_WRONG_PASSWORD = 13;
    private static final int DISCONNECT_REASON_BEACON_LOSS = 0;
    private static final String TAG = "WifiUnstableAp";
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    private final UnstableApAdapter mAdapter;
    private int mAuthFailCounter = 0;
    private final DeauthAttackDetector mDeauthAttackDetector = new DeauthAttackDetector();
    private boolean mIsSimCardReady;
    private String mLastAuthFailBssid;
    private final HashMap<Integer, UnstableApInfo> mUnstableAps = new HashMap<>();

    public interface UnstableApAdapter {
        void addToBlackList(String str);

        void enableNetwork(int i);

        WifiConfiguration getNetwork(int i);

        void updateUnstableApNetwork(int i, int i2);
    }

    public UnstableApController(UnstableApAdapter adapter) {
        this.mAdapter = adapter;
        clearAll();
    }

    public void clearAll() {
        this.mUnstableAps.clear();
    }

    public void setSimCardState(boolean isSimCardReady) {
        this.mIsSimCardReady = isSimCardReady;
    }

    public boolean isUnstableAp(String bssid) {
        if (this.mUnstableAps.size() <= 0) {
            return false;
        }
        for (Integer intValue : this.mUnstableAps.keySet()) {
            UnstableApInfo unstableApInfo = this.mUnstableAps.get(Integer.valueOf(intValue.intValue()));
            if (unstableApInfo != null && unstableApInfo.isUnstableAp() && unstableApInfo.contains(bssid) && !unstableApInfo.canEnable()) {
                return true;
            }
        }
        return false;
    }

    public void l2Connected(int networkId) {
        this.mDeauthAttackDetector.l2Connected(networkId);
    }

    public boolean disconnectWithAuthFail(int networkId, String bssid, int rssi, int disableReason, boolean isConnectedState, boolean isHotspotAp) {
        String str = bssid;
        int i = networkId;
        WifiConfiguration config = this.mAdapter.getNetwork(networkId);
        boolean ret = this.mDeauthAttackDetector.checkDeauthAttack(config, str, rssi);
        if (ret && config != null) {
            disconnect(bssid, -50, config, 0, false);
        }
        if (isConnectedState || !isHotspotAp) {
            String str2 = this.mLastAuthFailBssid;
            if (str2 == null || !str2.equals(str)) {
                this.mLastAuthFailBssid = str;
                this.mAuthFailCounter = 1;
            } else {
                this.mAuthFailCounter++;
            }
            if (!ret && this.mAuthFailCounter >= 3) {
                this.mAuthFailCounter = 0;
                if (disconnect(bssid, rssi, config, disableReason, true) || ret) {
                    return true;
                }
                return false;
            } else if (config == null || (!isKoreaVendorAp(config) && !isVendorPasspointAp(config))) {
                return ret;
            } else {
                if (disconnect(bssid, rssi, config, 0, true) || ret) {
                    return true;
                }
                return false;
            }
        } else if (disconnect(bssid, -50, config, 0, false) || ret) {
            return true;
        } else {
            return false;
        }
    }

    public boolean disconnect(String bssid, int rssi, WifiConfiguration config, int reason) {
        return disconnect(bssid, rssi, config, reason, true);
    }

    private boolean disconnect(String bssid, int rssi, WifiConfiguration config, int reason, boolean ignoreMHS) {
        if (!isDisconnectedWithUnstableApReason(reason)) {
            return false;
        }
        if (!this.mIsSimCardReady) {
            Log.d(TAG, "SIM card is not ready");
            return false;
        } else if (config == null) {
            if (DBG) {
                Log.d(TAG, "disconnected unstable network - do not add (config is null)");
            }
            return false;
        } else if (ignoreMHS && isHotspotAp(config)) {
            if (DBG) {
                Log.d(TAG, "disconnected unstable network - do not add (it's samsung mobile hotspot)");
            }
            return false;
        } else if (config.allowedKeyManagement.get(0) || config.allowedKeyManagement.get(1) || config.allowedKeyManagement.get(8) || config.allowedKeyManagement.get(4) || config.allowedKeyManagement.get(22) || ((config.allowedKeyManagement.get(0) && config.wepKeys != null && config.wepKeys[0] != null) || isKoreaVendorAp(config) || isVendorPasspointAp(config))) {
            return attemptDetectUnstableAp(bssid, rssi, config);
        } else {
            if (DBG) {
                Log.d(TAG, "disconnected unstable network - do not add (others reason)");
            }
            return false;
        }
    }

    private boolean isDisconnectedWithUnstableApReason(int disconnectReason) {
        if (disconnectReason == 0 || disconnectReason == 100 || disconnectReason == 193) {
            Log.d(TAG, "disconnected reason : beacon loss");
            return true;
        } else if (disconnectReason >= 4 && disconnectReason <= 7) {
            return true;
        } else {
            if (disconnectReason == 12) {
                return false;
            }
            if ((disconnectReason < 10 || disconnectReason > 22) && disconnectReason != 34) {
                return false;
            }
            return true;
        }
    }

    private boolean hasEverConnectedAp(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        return config.getNetworkSelectionStatus().getHasEverConnected();
    }

    private boolean isHotspotAp(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        return config.semSamsungSpecificFlags.get(1);
    }

    private boolean isKoreaVendorAp(WifiConfiguration config) {
        if (!config.semIsVendorSpecificSsid || 1 != mOpBranding.getCountry()) {
            return false;
        }
        return true;
    }

    static boolean isVendorPasspointAp(WifiConfiguration config) {
        if (!config.semIsVendorSpecificSsid || !config.isPasspoint()) {
            return false;
        }
        return true;
    }

    public void resetUnstableApInfo(int networkId, String bssid) {
        UnstableApInfo unstableApInfo = this.mUnstableAps.get(Integer.valueOf(networkId));
        if (unstableApInfo != null) {
            if (bssid == null) {
                bssid = new String("00:00:00:00:00:00");
            }
            unstableApInfo.forceReset(bssid);
        }
    }

    public void verifyAll(List<ScanResult> scanResults) {
        for (Integer intValue : this.mUnstableAps.keySet()) {
            int networkId = intValue.intValue();
            UnstableApInfo unstableApInfo = this.mUnstableAps.get(Integer.valueOf(networkId));
            if (unstableApInfo != null && unstableApInfo.isUnstableAp()) {
                if (unstableApInfo.canEnable() || unstableApInfo.checkRssiAndReenable(scanResults)) {
                    unstableApInfo.reset((String) null);
                    WifiConfiguration config = this.mAdapter.getNetwork(networkId);
                    if (config != null && config.getNetworkSelectionStatus().isDisabledByReason(19)) {
                        Log.i(TAG, "reenable unstable network id:" + networkId);
                        this.mAdapter.enableNetwork(networkId);
                    }
                }
            }
        }
    }

    private boolean attemptDetectUnstableAp(String bssid, int rssi, WifiConfiguration config) {
        List<String> blackList;
        if (bssid == null) {
            bssid = new String("00:00:00:00:00:00");
        }
        UnstableApInfo unstableApInfo = this.mUnstableAps.get(Integer.valueOf(config.networkId));
        if (unstableApInfo == null || !unstableApInfo.isMatched(config)) {
            if (DBG) {
                Log.d(TAG, "disconnected unstable network - add new item id:" + config.networkId);
            }
            UnstableApInfo unstableApInfo2 = new UnstableApInfo(config, bssid, rssi);
            unstableApInfo2.addAndCheck(bssid, rssi);
            if (this.mUnstableAps.containsKey(Integer.valueOf(config.networkId))) {
                this.mUnstableAps.remove(Integer.valueOf(config.networkId));
            }
            this.mUnstableAps.put(Integer.valueOf(config.networkId), unstableApInfo2);
            return false;
        }
        if (DBG) {
            Log.d(TAG, "disconnected unstable network id:" + config.networkId + ", unstable:" + unstableApInfo.isUnstableAp());
        }
        int targetDisableReason = 19;
        if (isHotspotAp(config)) {
            targetDisableReason = 2;
        }
        boolean doNotReplaceDisableReason = false;
        WifiConfiguration.NetworkSelectionStatus networkStatus = config.getNetworkSelectionStatus();
        if (!networkStatus.isNetworkEnabled() && !networkStatus.isDisabledByReason(19)) {
            doNotReplaceDisableReason = true;
        }
        if (unstableApInfo.isUnstableAp()) {
            unstableApInfo.updateTime(bssid);
            if (!doNotReplaceDisableReason) {
                this.mAdapter.updateUnstableApNetwork(config.networkId, targetDisableReason);
                Log.i(TAG, "disable unstable network id:" + config.networkId);
            }
            return true;
        } else if (!unstableApInfo.canAddCounter(bssid)) {
            if (DBG) {
                Log.d(TAG, "disconnected unstable network - reset");
            }
            unstableApInfo.reset(bssid);
            unstableApInfo.addAndCheck(bssid, rssi);
            return false;
        } else if (unstableApInfo.addAndCheck(bssid, rssi)) {
            unstableApInfo.updateTime(bssid);
            if (!doNotReplaceDisableReason) {
                this.mAdapter.updateUnstableApNetwork(config.networkId, targetDisableReason);
                Log.i(TAG, "disable unstable network id:" + config.networkId);
            }
            return true;
        } else if (!unstableApInfo.isRoamAp() || (blackList = unstableApInfo.getBlackList()) == null || blackList.size() <= 0) {
            return false;
        } else {
            for (String bssidItem : blackList) {
                if (DBG) {
                    Log.i(TAG, "add unstable bssid-" + bssidItem + " to blacklist");
                }
                this.mAdapter.addToBlackList(bssid);
            }
            return false;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println(dump());
    }

    public String dump() {
        StringBuffer sb = new StringBuffer();
        int unstableApSize = this.mUnstableAps.size();
        sb.append("UnstableAps " + unstableApSize);
        if (unstableApSize > 0) {
            for (Integer intValue : this.mUnstableAps.keySet()) {
                int networkId = intValue.intValue();
                UnstableApInfo unstableApInfo = this.mUnstableAps.get(Integer.valueOf(networkId));
                if (unstableApInfo != null) {
                    sb.append("\n");
                    sb.append(" - unstable item netid:" + networkId + ", details:" + unstableApInfo);
                }
            }
        }
        return sb.toString();
    }

    private static class DeauthAttackDetector {
        private int mLastDeauthCount = 0;
        private int mLastL2ConnectedNetworkId = -1;
        private long mLastL2ConnectedTime = 0;

        DeauthAttackDetector() {
        }

        /* access modifiers changed from: package-private */
        public void l2Connected(int networkId) {
            if (networkId != this.mLastL2ConnectedNetworkId) {
                if (UnstableApController.DBG) {
                    Log.i(UnstableApController.TAG, "reset deauth counter");
                }
                this.mLastDeauthCount = 0;
                this.mLastL2ConnectedNetworkId = networkId;
            }
            this.mLastL2ConnectedTime = SystemClock.elapsedRealtime();
        }

        /* access modifiers changed from: package-private */
        public boolean checkDeauthAttack(WifiConfiguration config, String bssid, int rssi) {
            long diff = SystemClock.elapsedRealtime() - this.mLastL2ConnectedTime;
            if (diff > 0 && diff < 700) {
                this.mLastDeauthCount++;
                Log.i(UnstableApController.TAG, "detecting deauth attack, counter:" + this.mLastDeauthCount);
                if (this.mLastDeauthCount > 6) {
                    if (config != null) {
                        return true;
                    }
                    if (UnstableApController.DBG) {
                        Log.e(UnstableApController.TAG, "can't get Wi-Fi config, init counter");
                    }
                    this.mLastDeauthCount = 0;
                }
            } else if (diff > 10000) {
                this.mLastDeauthCount = 0;
            }
            return false;
        }
    }

    private static class UnstableApInfo {
        private static final int ENABLE_RSSI_LEVEL = -70;
        private static final int MAX_DISCONNECTED_WITH_REASON_ZERO_COUNT = 3;
        private static final long MAX_DISCONNECTED_WITH_REASON_ZERO_TIME = 300000;
        private static final long MAX_UNSTABLE_AP_DISABLED_DURATION_FOR_VENDORAP = 3600000;
        private static final int MIN_RSSI_LEVEL = -75;
        private static final long MIN_UNSTABLE_AP_DISABLED_DURATION = 900000;
        private static final long MIN_UNSTABLE_AP_DISABLED_DURATION_FOR_VENDORAP = 1200000;
        private final BitSet mAllowKeyMgmt;
        private final HashMap<String, ApInfo> mBssids = new HashMap<>();
        private final boolean mIsVendorAp;
        private final String mSsid;

        UnstableApInfo(WifiConfiguration config, String bssid, int rssi) {
            if (bssid != null) {
                ApInfo info = new ApInfo();
                info.level = rssi;
                this.mBssids.put(bssid, info);
            }
            this.mSsid = removeDoubleQuotes(config.SSID);
            this.mAllowKeyMgmt = (BitSet) config.allowedKeyManagement.clone();
            this.mIsVendorAp = config.semIsVendorSpecificSsid || UnstableApController.isVendorPasspointAp(config);
            reset((String) null);
        }

        /* access modifiers changed from: package-private */
        public boolean isMatched(WifiConfiguration config) {
            String str;
            if (config == null || (str = this.mSsid) == null || !str.equals(removeDoubleQuotes(config.SSID)) || this.mAllowKeyMgmt == null || !config.allowedKeyManagement.equals(this.mAllowKeyMgmt)) {
                return false;
            }
            return true;
        }

        /* access modifiers changed from: package-private */
        public boolean contains(String bssid) {
            if (this.mBssids.containsKey(bssid)) {
                return true;
            }
            return false;
        }

        /* access modifiers changed from: package-private */
        public boolean isUnstableAp() {
            if (isRoamAp()) {
                return false;
            }
            if (this.mIsVendorAp) {
                int counter = 0;
                for (ApInfo apInfo : this.mBssids.values()) {
                    counter += apInfo.counter;
                }
                if (counter >= 3) {
                    return true;
                }
            } else {
                for (ApInfo apInfo2 : this.mBssids.values()) {
                    if (apInfo2.counter >= 3) {
                        return true;
                    }
                }
            }
            return false;
        }

        /* access modifiers changed from: package-private */
        public boolean isRoamAp() {
            if (!this.mIsVendorAp && this.mBssids.size() >= 2) {
                return true;
            }
            return false;
        }

        /* access modifiers changed from: package-private */
        public boolean canEnable() {
            if (isRoamAp()) {
                return true;
            }
            for (ApInfo apInfo : this.mBssids.values()) {
                if (SystemClock.elapsedRealtime() - apInfo.time < getTimeLimit()) {
                    return false;
                }
            }
            return true;
        }

        /* access modifiers changed from: package-private */
        public boolean checkRssiAndReenable(List<ScanResult> scanResult) {
            if (scanResult == null || this.mIsVendorAp) {
                return false;
            }
            for (ScanResult scanItem : scanResult) {
                String bssid = scanItem.BSSID;
                if (this.mBssids.containsKey(bssid)) {
                    ApInfo apInfo = this.mBssids.get(bssid);
                    if (scanItem.capabilities.contains("SEC80")) {
                        apInfo.isMobileHotspot = true;
                    } else {
                        apInfo.isMobileHotspot = false;
                    }
                    if (apInfo.level < -75 && scanItem.level >= ENABLE_RSSI_LEVEL) {
                        return true;
                    }
                } else if (getMatchingSsidSecureConfig(scanItem)) {
                    ApInfo apInfo2 = new ApInfo();
                    if (scanItem.capabilities.contains("SEC80")) {
                        apInfo2.isMobileHotspot = true;
                    }
                    apInfo2.level = scanItem.level;
                    this.mBssids.put(scanItem.BSSID, apInfo2);
                    return true;
                }
            }
            return false;
        }

        private long getTimeLimit() {
            if (!this.mIsVendorAp) {
                return MIN_UNSTABLE_AP_DISABLED_DURATION;
            }
            for (ApInfo info : this.mBssids.values()) {
                if (info.totalDisabledCounter >= 2) {
                    return 3600000;
                }
            }
            return MIN_UNSTABLE_AP_DISABLED_DURATION_FOR_VENDORAP;
        }

        private boolean getMatchingSsidSecureConfig(ScanResult scanResult) {
            if (scanResult.SSID != null && scanResult.SSID.equals(this.mSsid)) {
                if (this.mAllowKeyMgmt.get(1) && scanResult.capabilities.contains("PSK")) {
                    return true;
                }
                if (this.mAllowKeyMgmt.get(2) && scanResult.capabilities.contains("EAP")) {
                    return true;
                }
                if (this.mAllowKeyMgmt.get(24) && scanResult.capabilities.contains("CCKM")) {
                    return true;
                }
                if (this.mAllowKeyMgmt.get(22) && scanResult.capabilities.contains("WAPI-PSK")) {
                    return true;
                }
                if (this.mAllowKeyMgmt.get(23) && scanResult.capabilities.contains("WAPI-CERT")) {
                    return true;
                }
                if ((!this.mAllowKeyMgmt.get(8) || !scanResult.capabilities.contains("SAE")) && !this.mAllowKeyMgmt.get(0)) {
                    return false;
                }
                return true;
            }
            return false;
        }

        private static String removeDoubleQuotes(String string) {
            if (TextUtils.isEmpty(string)) {
                return "";
            }
            int length = string.length();
            if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
                return string.substring(1, length - 1);
            }
            return string;
        }

        /* access modifiers changed from: package-private */
        public boolean addAndCheck(String bssid, int rssi) {
            if (bssid != null) {
                if (this.mBssids.containsKey(bssid)) {
                    ApInfo apInfo = this.mBssids.get(bssid);
                    apInfo.level = rssi;
                    apInfo.counter++;
                } else {
                    ApInfo apInfo2 = new ApInfo();
                    apInfo2.level = rssi;
                    apInfo2.counter = 1;
                    this.mBssids.put(bssid, apInfo2);
                }
            }
            return isUnstableAp();
        }

        /* access modifiers changed from: package-private */
        public boolean canAddCounter(String bssid) {
            if (this.mIsVendorAp) {
                return true;
            }
            if (bssid == null || !this.mBssids.containsKey(bssid) || SystemClock.elapsedRealtime() - this.mBssids.get(bssid).time >= MAX_DISCONNECTED_WITH_REASON_ZERO_TIME) {
                return false;
            }
            return true;
        }

        /* access modifiers changed from: package-private */
        public List<String> getBlackList() {
            ArrayList<String> blackList = null;
            if (this.mBssids.size() >= 2) {
                blackList = new ArrayList<>();
                for (String bssid : this.mBssids.keySet()) {
                    ApInfo info = this.mBssids.get(bssid);
                    if (info != null && info.counter >= 3) {
                        blackList.add(bssid);
                    }
                }
            }
            if (blackList != null) {
                Iterator<String> it = blackList.iterator();
                while (it.hasNext()) {
                    ApInfo info2 = this.mBssids.get(it.next());
                    info2.counter--;
                }
            }
            return blackList;
        }

        /* access modifiers changed from: package-private */
        public void updateTime(String bssid) {
            if (bssid != null && this.mBssids.containsKey(bssid)) {
                this.mBssids.get(bssid).time = SystemClock.elapsedRealtime();
            }
        }

        /* access modifiers changed from: package-private */
        public void forceReset(String bssid) {
            if (this.mIsVendorAp) {
                for (ApInfo info : this.mBssids.values()) {
                    info.forceReset();
                }
            } else if (this.mBssids.containsKey(bssid)) {
                this.mBssids.get(bssid).forceReset();
            }
        }

        /* access modifiers changed from: package-private */
        public void reset(String bssid) {
            if (bssid == null) {
                for (ApInfo info : this.mBssids.values()) {
                    if (this.mIsVendorAp) {
                        info.totalDisabledCounter++;
                    }
                    if (info.counter >= 3) {
                        info.totalDisabledCounter++;
                    }
                    info.counter = 0;
                    info.time = SystemClock.elapsedRealtime();
                }
            } else if (this.mBssids.containsKey(bssid)) {
                ApInfo info2 = this.mBssids.get(bssid);
                if (info2.counter >= 3) {
                    info2.totalDisabledCounter++;
                }
                info2.counter = 0;
                info2.time = SystemClock.elapsedRealtime();
            }
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.mSsid);
            if (this.mIsVendorAp) {
                sb.append("[VendorAp]");
            }
            sb.append(" ");
            for (String key : this.mBssids.keySet()) {
                sb.append("[");
                ApInfo apInfo = this.mBssids.get(key);
                sb.append(key.substring((key.length() / 2) + 1));
                sb.append(", cnt:");
                sb.append(apInfo.counter);
                sb.append(", total:");
                sb.append(apInfo.totalDisabledCounter);
                sb.append(", rssi:");
                sb.append(apInfo.level);
                if (apInfo.isMobileHotspot) {
                    sb.append(",SEC80");
                }
                sb.append("]");
            }
            return sb.toString();
        }

        private static class ApInfo {
            public int counter = 0;
            public boolean isMobileHotspot = false;
            public int level = -100;
            public long time = 0;
            public int totalDisabledCounter = 0;

            public void forceReset() {
                this.counter = 0;
                this.totalDisabledCounter = 0;
                this.time = 0;
            }
        }
    }
}
