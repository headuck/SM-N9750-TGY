package com.android.server.wifi;

import android.content.Context;
import android.net.NetworkKey;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.sec.enterprise.WifiPolicyCache;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.wifi.WifiCandidates;
import com.android.server.wifi.WifiNetworkSelector;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.util.ScanResultUtil;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService;
import com.sec.android.app.CscFeatureTagWifi;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WifiNetworkSelector {
    private static final String CONFIG_SECURE_SVC_INTEGRATION = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGSECURESVCINTEGRATION);
    private static final boolean CSC_SUPPORT_ASSOCIATED_NETWORK_SELECTION = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_SUPPORTASSOCIATEDNETWORKSELECTION, false);
    private static final int ID_PREFIX = 42;
    private static final int ID_SUFFIX_MOD = 1000000;
    private static final long INVALID_TIME_STAMP = Long.MIN_VALUE;
    @VisibleForTesting
    public static final int LAST_USER_SELECTION_DECAY_TO_ZERO_MS = 28800000;
    @VisibleForTesting
    public static final int LAST_USER_SELECTION_SUFFICIENT_MS = 30000;
    public static final int LEGACY_CANDIDATE_SCORER_EXP_ID = 0;
    @VisibleForTesting
    public static final int MINIMUM_NETWORK_SELECTION_INTERVAL_MS = 6000;
    private static final int MIN_SCORER_EXP_ID = 42000000;
    public static final String PRESET_CANDIDATE_SCORER_NAME = "CompatibilityScorer";
    private static final String TAG = "WifiNetworkSelector";
    @VisibleForTesting
    public static final int WIFI_POOR_SCORE = 40;
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    private final Map<String, WifiCandidates.CandidateScorer> mCandidateScorers = new ArrayMap();
    private final Clock mClock;
    private final List<Pair<ScanDetail, WifiConfiguration>> mConnectableNetworks = new ArrayList();
    private final Context mContext;
    private final boolean mEnableAutoJoinWhenAssociated;
    private final List<NetworkEvaluator> mEvaluators = new ArrayList(3);
    private List<ScanDetail> mFilteredNetworks = new ArrayList();
    private boolean mIsEnhancedOpenSupported;
    private boolean mIsEnhancedOpenSupportedInitialized = false;
    private long mLastNetworkSelectionTimeStamp = INVALID_TIME_STAMP;
    private final LocalLog mLocalLog;
    private final ScoringParams mScoringParams;
    private final int mStayOnNetworkMinimumRxRate;
    private final int mStayOnNetworkMinimumTxRate;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiMetrics mWifiMetrics;
    private final WifiNative mWifiNative;
    private final WifiScoreCard mWifiScoreCard;

    public interface NetworkEvaluator {
        public static final int EVALUATOR_ID_CARRIER = 3;
        public static final int EVALUATOR_ID_PASSPOINT = 2;
        public static final int EVALUATOR_ID_SAVED = 0;
        public static final int EVALUATOR_ID_SCORED = 4;
        public static final int EVALUATOR_ID_SUGGESTION = 1;

        @Retention(RetentionPolicy.SOURCE)
        public @interface EvaluatorId {
        }

        public interface OnConnectableListener {
            void onConnectable(ScanDetail scanDetail, WifiConfiguration wifiConfiguration, int i);
        }

        WifiConfiguration evaluateNetworks(List<ScanDetail> list, WifiConfiguration wifiConfiguration, String str, boolean z, boolean z2, OnConnectableListener onConnectableListener, boolean z3);

        int getId();

        String getName();

        void update(List<ScanDetail> list);
    }

    private void localLog(String log) {
        this.mLocalLog.log(log);
    }

    private boolean isCurrentNetworkSufficient(WifiInfo wifiInfo, List<ScanDetail> scanDetails) {
        if (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
            localLog("No current connected network.");
            return false;
        }
        localLog("Current connected network: " + wifiInfo.getSSID() + " , ID: " + wifiInfo.getNetworkId());
        int currentRssi = wifiInfo.getRssi();
        boolean hasQualifiedRssi = currentRssi > this.mScoringParams.getSufficientRssi(wifiInfo.getFrequency());
        boolean hasActiveStream = wifiInfo.txSuccessRate > ((double) this.mStayOnNetworkMinimumTxRate) || wifiInfo.rxSuccessRate > ((double) this.mStayOnNetworkMinimumRxRate);
        if (!hasQualifiedRssi || !hasActiveStream) {
            WifiConfiguration network = this.mWifiConfigManager.getConfiguredNetwork(wifiInfo.getNetworkId());
            if (network == null) {
                localLog("Current network was removed.");
                return false;
            } else if (this.mWifiConfigManager.getLastSelectedNetwork() == network.networkId && this.mClock.getElapsedSinceBootMillis() - this.mWifiConfigManager.getLastSelectedTimeStamp() <= 30000) {
                localLog("Current network is recently user-selected.");
                return true;
            } else if (network.osu) {
                return true;
            } else {
                if (wifiInfo.isEphemeral()) {
                    localLog("Current network is an ephemeral one.");
                    return false;
                } else if (wifiInfo.is24GHz() && is5GHzNetworkAvailable(scanDetails)) {
                    localLog("Current network is 2.4GHz. 5GHz networks available.");
                    return false;
                } else if (!hasQualifiedRssi) {
                    localLog("Current network RSSI[" + currentRssi + "]-acceptable but not qualified.");
                    return false;
                } else if (WifiConfigurationUtil.isConfigForOpenNetwork(network)) {
                    localLog("Current network is a open one.");
                    return false;
                } else if (network.numNoInternetAccessReports <= 0 || network.noInternetAccessExpected) {
                    return true;
                } else {
                    localLog("Current network has [" + network.numNoInternetAccessReports + "] no-internet access reports.");
                    return false;
                }
            }
        } else {
            localLog("Stay on current network because of good RSSI and ongoing traffic");
            return true;
        }
    }

    private boolean is5GHzNetworkAvailable(List<ScanDetail> scanDetails) {
        for (ScanDetail detail : scanDetails) {
            if (detail.getScanResult().is5GHz()) {
                return true;
            }
        }
        return false;
    }

    private boolean isNetworkSelectionNeeded(List<ScanDetail> scanDetails, WifiInfo wifiInfo, boolean connected, boolean disconnected) {
        if (scanDetails.size() == 0) {
            localLog("Empty connectivity scan results. Skip network selection.");
            return false;
        } else if (connected) {
            if (!this.mEnableAutoJoinWhenAssociated) {
                localLog("Switching networks in connected state is not allowed. Skip network selection.");
                return false;
            }
            if (this.mLastNetworkSelectionTimeStamp != INVALID_TIME_STAMP) {
                long gap = this.mClock.getElapsedSinceBootMillis() - this.mLastNetworkSelectionTimeStamp;
                if (gap < 6000) {
                    localLog("Too short since last network selection: " + gap + " ms. Skip network selection.");
                    return false;
                }
            }
            if (isCurrentNetworkSufficient(wifiInfo, scanDetails)) {
                localLog("Current connected network already sufficient. Skip network selection.");
                return false;
            }
            localLog("Current connected network is not sufficient.");
            return true;
        } else if (disconnected) {
            return true;
        } else {
            localLog("ClientModeImpl is in neither CONNECTED nor DISCONNECTED state. Skip network selection.");
            return false;
        }
    }

    public static String toScanId(ScanResult scanResult) {
        if (scanResult == null) {
            return "NULL";
        }
        return String.format("%s:%s", new Object[]{scanResult.SSID, scanResult.BSSID});
    }

    public static String toNetworkString(WifiConfiguration network) {
        if (network == null) {
            return null;
        }
        return network.SSID + ":" + network.networkId;
    }

    public boolean isSignalTooWeak(ScanResult scanResult) {
        return scanResult.level < this.mScoringParams.getEntryRssi(scanResult.frequency);
    }

    private List<ScanDetail> filterScanResults(List<ScanDetail> scanDetails, HashSet<String> bssidBlacklist, boolean isConnected, String currentBssid) {
        StringBuffer noValidSsid;
        StringBuffer lowRssi;
        StringBuffer blacklistedBssid;
        String str = currentBssid;
        ArrayList<NetworkKey> unscoredNetworks = new ArrayList<>();
        List<ScanDetail> validScanDetails = new ArrayList<>();
        StringBuffer noValidSsid2 = new StringBuffer();
        StringBuffer blacklistedBssid2 = new StringBuffer();
        StringBuffer lowRssi2 = new StringBuffer();
        StringBuffer maliciousBssid = new StringBuffer();
        StringBuffer mboAssociationDisallowedBssid = new StringBuffer();
        StringBuffer blockedByMDM = new StringBuffer();
        WifiPolicyCache mWifiPolicyCache = WifiPolicyCache.getInstance(this.mContext);
        boolean scanResultsHaveCurrentBssid = false;
        Iterator<ScanDetail> it = scanDetails.iterator();
        while (it.hasNext()) {
            ScanDetail scanDetail = it.next();
            ScanResult scanResult = scanDetail.getScanResult();
            ArrayList<NetworkKey> unscoredNetworks2 = unscoredNetworks;
            if (TextUtils.isEmpty(scanResult.SSID)) {
                noValidSsid2.append(scanResult.BSSID);
                noValidSsid2.append(" / ");
                unscoredNetworks = unscoredNetworks2;
            } else {
                if (scanResult.BSSID.equals(str)) {
                    scanResultsHaveCurrentBssid = true;
                }
                String scanId = toScanId(scanResult);
                boolean scanResultsHaveCurrentBssid2 = scanResultsHaveCurrentBssid;
                Iterator<ScanDetail> it2 = it;
                if (bssidBlacklist.contains(scanResult.BSSID)) {
                    blacklistedBssid2.append(scanId);
                    blacklistedBssid2.append(" / ");
                    noValidSsid = noValidSsid2;
                    blacklistedBssid = blacklistedBssid2;
                    lowRssi = lowRssi2;
                } else {
                    blacklistedBssid = blacklistedBssid2;
                    if (isSignalTooWeak(scanResult)) {
                        lowRssi2.append(scanId);
                        lowRssi2.append("(");
                        lowRssi2.append(scanResult.is24GHz() ? "2.4GHz" : "5GHz");
                        lowRssi2.append(")");
                        lowRssi2.append(scanResult.level);
                        lowRssi2.append(" / ");
                        noValidSsid = noValidSsid2;
                        lowRssi = lowRssi2;
                    } else {
                        MobileWipsFrameworkService mwfs = MobileWipsFrameworkService.getInstance();
                        if (mwfs != null) {
                            lowRssi = lowRssi2;
                            noValidSsid = noValidSsid2;
                            if (mwfs.checkMWIPS(scanResult.BSSID, scanResult.frequency)) {
                                maliciousBssid.append(scanId);
                                maliciousBssid.append(" / ");
                            }
                        } else {
                            noValidSsid = noValidSsid2;
                            lowRssi = lowRssi2;
                        }
                        NetworkDetail networkDetail = scanDetail.getNetworkDetail();
                        if (networkDetail != null) {
                            MobileWipsFrameworkService mobileWipsFrameworkService = mwfs;
                            if (networkDetail.getMboAssociationDisallowedReasonCode() != -1) {
                                mboAssociationDisallowedBssid.append(scanId);
                                mboAssociationDisallowedBssid.append("(");
                                mboAssociationDisallowedBssid.append(networkDetail.getMboAssociationDisallowedReasonCode());
                                mboAssociationDisallowedBssid.append(")");
                                mboAssociationDisallowedBssid.append(" / ");
                            }
                        }
                        if (!mWifiPolicyCache.isNetworkAllowed(ScanResultUtil.createNetworkFromScanResult(scanResult), false)) {
                            blockedByMDM.append(scanId);
                            blockedByMDM.append(" / ");
                        } else {
                            validScanDetails.add(scanDetail);
                        }
                    }
                }
                unscoredNetworks = unscoredNetworks2;
                scanResultsHaveCurrentBssid = scanResultsHaveCurrentBssid2;
                it = it2;
                blacklistedBssid2 = blacklistedBssid;
                lowRssi2 = lowRssi;
                noValidSsid2 = noValidSsid;
            }
        }
        StringBuffer noValidSsid3 = noValidSsid2;
        StringBuffer blacklistedBssid3 = blacklistedBssid2;
        StringBuffer lowRssi3 = lowRssi2;
        if (!isConnected || scanResultsHaveCurrentBssid) {
            if (noValidSsid3.length() != 0) {
                localLog("Networks filtered out due to invalid SSID: " + noValidSsid3);
            }
            if (blacklistedBssid3.length() != 0) {
                localLog("Networks filtered out due to blacklist: " + blacklistedBssid3);
            }
            if (lowRssi3.length() != 0) {
                localLog("Networks filtered out due to low signal strength: " + lowRssi3);
            }
            if (maliciousBssid.length() != 0) {
                localLog("Networks filtered out due to malicious: " + maliciousBssid);
            }
            if (mboAssociationDisallowedBssid.length() != 0) {
                localLog("Networks filtered out due to MBO association disallowed: " + mboAssociationDisallowedBssid);
            }
            if (blockedByMDM.length() != 0) {
                localLog("Networks filtered out due to MDM policy: " + blockedByMDM);
            }
            return validScanDetails;
        }
        localLog("Current connected BSSID " + str + " is not in the scan results. Skip network selection.");
        validScanDetails.clear();
        return validScanDetails;
    }

    private boolean isEnhancedOpenSupported() {
        if (this.mIsEnhancedOpenSupportedInitialized) {
            return this.mIsEnhancedOpenSupported;
        }
        boolean z = true;
        this.mIsEnhancedOpenSupportedInitialized = true;
        WifiNative wifiNative = this.mWifiNative;
        if ((wifiNative.getSupportedFeatureSet(wifiNative.getClientInterfaceName()) & 536870912) == 0) {
            z = false;
        }
        this.mIsEnhancedOpenSupported = z;
        return this.mIsEnhancedOpenSupported;
    }

    public List<ScanDetail> getFilteredScanDetailsForOpenUnsavedNetworks() {
        List<ScanDetail> openUnsavedNetworks = new ArrayList<>();
        boolean enhancedOpenSupported = isEnhancedOpenSupported();
        for (ScanDetail scanDetail : this.mFilteredNetworks) {
            ScanResult scanResult = scanDetail.getScanResult();
            if (ScanResultUtil.isScanResultForOpenNetwork(scanResult) && ((!ScanResultUtil.isScanResultForOweNetwork(scanResult) || enhancedOpenSupported) && this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail) == null)) {
                openUnsavedNetworks.add(scanDetail);
            }
        }
        return openUnsavedNetworks;
    }

    public List<ScanDetail> getFilteredScanDetailsForCarrierUnsavedNetworks(CarrierNetworkConfig carrierConfig) {
        List<ScanDetail> carrierUnsavedNetworks = new ArrayList<>();
        for (ScanDetail scanDetail : this.mFilteredNetworks) {
            ScanResult scanResult = scanDetail.getScanResult();
            if (ScanResultUtil.isScanResultForEapNetwork(scanResult) && carrierConfig.isCarrierNetwork(scanResult.SSID) && this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail) == null) {
                carrierUnsavedNetworks.add(scanDetail);
            }
        }
        return carrierUnsavedNetworks;
    }

    public List<Pair<ScanDetail, WifiConfiguration>> getConnectableScanDetails() {
        return this.mConnectableNetworks;
    }

    public boolean setUserConnectChoice(int netId) {
        localLog("userSelectNetwork: network ID=" + netId);
        WifiConfiguration selected = this.mWifiConfigManager.getConfiguredNetwork(netId);
        if (selected == null || selected.SSID == null) {
            localLog("userSelectNetwork: Invalid configuration with nid=" + netId);
            return false;
        }
        if (!selected.getNetworkSelectionStatus().isNetworkEnabled()) {
            this.mWifiConfigManager.updateNetworkSelectionStatus(netId, 0);
        }
        return setLegacyUserConnectChoice(selected);
    }

    private boolean setLegacyUserConnectChoice(WifiConfiguration selected) {
        Iterator<WifiConfiguration> it;
        List<WifiConfiguration> configuredNetworks;
        Iterator<WifiConfiguration> it2;
        List<WifiConfiguration> configuredNetworks2;
        boolean change = false;
        String key = selected.configKey();
        long currentTime = this.mClock.getWallClockMillis();
        List<WifiConfiguration> configuredNetworks3 = this.mWifiConfigManager.getConfiguredNetworks();
        Iterator<WifiConfiguration> it3 = configuredNetworks3.iterator();
        while (it3.hasNext()) {
            WifiConfiguration network = it3.next();
            WifiConfiguration.NetworkSelectionStatus status = network.getNetworkSelectionStatus();
            if (network.networkId == selected.networkId) {
                configuredNetworks = configuredNetworks3;
                it = it3;
            } else if (key.equals(network.configKey())) {
                configuredNetworks = configuredNetworks3;
                it = it3;
            } else {
                if (OpBrandingLoader.Vendor.ATT == mOpBranding) {
                    WifiConfiguration connectChoice = this.mWifiConfigManager.getConfiguredNetwork(status.getConnectChoice());
                    if (connectChoice == null || !connectChoice.isOpenNetwork()) {
                        configuredNetworks2 = configuredNetworks3;
                        it2 = it3;
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("[ATT] Remove user selection preference of ");
                        sb.append(status.getConnectChoice());
                        sb.append(" Set Time: ");
                        configuredNetworks2 = configuredNetworks3;
                        it2 = it3;
                        sb.append(status.getConnectChoiceTimestamp());
                        sb.append(" from ");
                        sb.append(network.SSID);
                        sb.append(" : ");
                        sb.append(network.networkId);
                        localLog(sb.toString());
                        this.mWifiConfigManager.clearNetworkConnectChoice(network.networkId);
                        change = true;
                    }
                    if (selected.isOpenNetwork()) {
                        localLog("[ATT] Do not setNetworkConnectChoice because an open network is selected");
                        configuredNetworks3 = configuredNetworks2;
                        it3 = it2;
                    }
                } else {
                    configuredNetworks2 = configuredNetworks3;
                    it2 = it3;
                }
                if (status.getSeenInLastQualifiedNetworkSelection() && !key.equals(status.getConnectChoice())) {
                    localLog("Add key: " + key + " Set Time: " + currentTime + " to " + toNetworkString(network));
                    this.mWifiConfigManager.setNetworkConnectChoice(network.networkId, key, currentTime);
                    change = true;
                }
                configuredNetworks3 = configuredNetworks2;
                it3 = it2;
            }
            if (status.getConnectChoice() != null) {
                localLog("Remove user selection preference of " + status.getConnectChoice() + " Set Time: " + status.getConnectChoiceTimestamp() + " from " + network.SSID + " : " + network.networkId);
                this.mWifiConfigManager.clearNetworkConnectChoice(network.networkId);
                change = true;
                key = key;
                configuredNetworks3 = configuredNetworks;
                it3 = it;
                currentTime = currentTime;
            } else {
                long j = currentTime;
                configuredNetworks3 = configuredNetworks;
                it3 = it;
            }
        }
        return change;
    }

    private void updateConfiguredNetworks() {
        List<WifiConfiguration> configuredNetworks = this.mWifiConfigManager.getConfiguredNetworks();
        if (configuredNetworks.size() == 0) {
            localLog("No configured networks.");
            return;
        }
        StringBuffer sbuf = new StringBuffer();
        for (WifiConfiguration network : configuredNetworks) {
            this.mWifiConfigManager.tryEnableNetwork(network.networkId);
            this.mWifiConfigManager.clearNetworkCandidateScanResult(network.networkId);
            WifiConfiguration.NetworkSelectionStatus status = network.getNetworkSelectionStatus();
            if (!status.isNetworkEnabled()) {
                sbuf.append("  ");
                sbuf.append(toNetworkString(network));
                sbuf.append(" ");
                for (int index = 1; index < 21; index++) {
                    int count = status.getDisableReasonCounter(index);
                    if (count > 0) {
                        sbuf.append("reason=");
                        sbuf.append(WifiConfiguration.NetworkSelectionStatus.getNetworkDisableReasonString(index));
                        sbuf.append(", count=");
                        sbuf.append(count);
                        sbuf.append("; ");
                    }
                }
                sbuf.append("\n");
            }
        }
        if (sbuf.length() > 0) {
            localLog("Disabled configured networks:");
            localLog(sbuf.toString());
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0164, code lost:
        r7 = r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private android.net.wifi.WifiConfiguration overrideCandidateWithUserConnectChoice(android.net.wifi.WifiConfiguration r15) {
        /*
            r14 = this;
            java.lang.Object r0 = com.android.internal.util.Preconditions.checkNotNull(r15)
            android.net.wifi.WifiConfiguration r0 = (android.net.wifi.WifiConfiguration) r0
            r1 = r15
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r2 = r15.getNetworkSelectionStatus()
            android.net.wifi.ScanResult r2 = r2.getCandidate()
            r3 = 0
        L_0x0010:
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r4 = r0.getNetworkSelectionStatus()
            java.lang.String r4 = r4.getConnectChoice()
            java.lang.String r5 = " : "
            r6 = 1
            if (r4 == 0) goto L_0x0164
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r4 = r0.getNetworkSelectionStatus()
            java.lang.String r4 = r4.getConnectChoice()
            java.lang.String r7 = r0.configKey()
            boolean r7 = r4.equals(r7)
            java.lang.String r8 = "While user choice adjust, clear user selection preference of "
            java.lang.String r9 = " from "
            java.lang.String r10 = " Set Time: "
            if (r7 == 0) goto L_0x007d
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r7 = r0.getNetworkSelectionStatus()
            java.lang.String r11 = "Tries to self connect choice.."
            r14.localLog(r11)
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            r11.append(r8)
            java.lang.String r8 = r7.getConnectChoice()
            r11.append(r8)
            r11.append(r10)
            long r12 = r7.getConnectChoiceTimestamp()
            r11.append(r12)
            r11.append(r9)
            java.lang.String r8 = r0.SSID
            r11.append(r8)
            r11.append(r5)
            int r8 = r0.networkId
            r11.append(r8)
            java.lang.String r8 = r11.toString()
            r14.localLog(r8)
            com.android.server.wifi.WifiConfigManager r8 = r14.mWifiConfigManager
            int r9 = r0.networkId
            r8.clearNetworkConnectChoice(r9)
            com.android.server.wifi.WifiConfigManager r8 = r14.mWifiConfigManager
            r8.saveToStore(r6)
            goto L_0x0164
        L_0x007d:
            int r7 = r3 + 1
            int r11 = com.android.server.wifi.WifiConfigManager.CSC_DEFAULT_MAX_NETWORKS_FOR_CURRENT_USER
            if (r3 <= r11) goto L_0x00cb
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r3 = r0.getNetworkSelectionStatus()
            java.lang.String r11 = "Too long chain of connect choice.."
            r14.localLog(r11)
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            r11.append(r8)
            java.lang.String r8 = r3.getConnectChoice()
            r11.append(r8)
            r11.append(r10)
            long r12 = r3.getConnectChoiceTimestamp()
            r11.append(r12)
            r11.append(r9)
            java.lang.String r8 = r0.SSID
            r11.append(r8)
            r11.append(r5)
            int r8 = r0.networkId
            r11.append(r8)
            java.lang.String r8 = r11.toString()
            r14.localLog(r8)
            com.android.server.wifi.WifiConfigManager r8 = r14.mWifiConfigManager
            int r9 = r0.networkId
            r8.clearNetworkConnectChoice(r9)
            com.android.server.wifi.WifiConfigManager r8 = r14.mWifiConfigManager
            r8.saveToStore(r6)
            goto L_0x0165
        L_0x00cb:
            com.samsung.android.net.wifi.OpBrandingLoader$Vendor r3 = com.samsung.android.net.wifi.OpBrandingLoader.Vendor.ATT
            com.samsung.android.net.wifi.OpBrandingLoader$Vendor r8 = mOpBranding
            if (r3 != r8) goto L_0x0128
            com.android.server.wifi.WifiConfigManager r3 = r14.mWifiConfigManager
            android.net.wifi.WifiConfiguration r3 = r3.getConfiguredNetwork((java.lang.String) r4)
            if (r3 == 0) goto L_0x0128
            boolean r8 = r3.isOpenNetwork()
            if (r8 == 0) goto L_0x0128
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r8 = r0.getNetworkSelectionStatus()
            java.lang.String r11 = "[ATT] Open network of connect choice.."
            r14.localLog(r11)
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            java.lang.String r12 = "[ATT] While user choice adjust, clear user selection preference of "
            r11.append(r12)
            java.lang.String r12 = r8.getConnectChoice()
            r11.append(r12)
            r11.append(r10)
            long r12 = r8.getConnectChoiceTimestamp()
            r11.append(r12)
            r11.append(r9)
            java.lang.String r9 = r0.SSID
            r11.append(r9)
            r11.append(r5)
            int r9 = r0.networkId
            r11.append(r9)
            java.lang.String r9 = r11.toString()
            r14.localLog(r9)
            com.android.server.wifi.WifiConfigManager r9 = r14.mWifiConfigManager
            int r10 = r0.networkId
            r9.clearNetworkConnectChoice(r10)
            com.android.server.wifi.WifiConfigManager r9 = r14.mWifiConfigManager
            r9.saveToStore(r6)
            goto L_0x0165
        L_0x0128:
            com.android.server.wifi.WifiConfigManager r3 = r14.mWifiConfigManager
            android.net.wifi.WifiConfiguration r0 = r3.getConfiguredNetwork((java.lang.String) r4)
            if (r0 == 0) goto L_0x014a
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r3 = r0.getNetworkSelectionStatus()
            android.net.wifi.ScanResult r5 = r3.getCandidate()
            if (r5 == 0) goto L_0x0146
            boolean r5 = r3.isNetworkEnabled()
            if (r5 == 0) goto L_0x0146
            android.net.wifi.ScanResult r2 = r3.getCandidate()
            r15 = r0
        L_0x0146:
            r3 = r7
            goto L_0x0010
        L_0x014a:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r8 = "Connect choice: "
            r3.append(r8)
            r3.append(r4)
            java.lang.String r8 = " has no corresponding saved config."
            r3.append(r8)
            java.lang.String r3 = r3.toString()
            r14.localLog(r3)
            goto L_0x0165
        L_0x0164:
            r7 = r3
        L_0x0165:
            if (r15 == r1) goto L_0x01a8
            int r3 = r2.level
            com.android.server.wifi.ScoringParams r4 = r14.mScoringParams
            int r8 = r2.frequency
            int r4 = r4.getSufficientRssi(r8)
            if (r3 <= r4) goto L_0x0174
            goto L_0x0175
        L_0x0174:
            r6 = 0
        L_0x0175:
            r3 = r6
            if (r3 == 0) goto L_0x01a2
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r6 = "After user selection adjustment, the final candidate is:"
            r4.append(r6)
            java.lang.String r6 = toNetworkString(r15)
            r4.append(r6)
            r4.append(r5)
            java.lang.String r5 = r2.BSSID
            r4.append(r5)
            java.lang.String r4 = r4.toString()
            r14.localLog(r4)
            com.android.server.wifi.WifiMetrics r4 = r14.mWifiMetrics
            int r5 = r15.networkId
            r6 = 8
            r4.setNominatorForNetwork(r5, r6)
            goto L_0x01a8
        L_0x01a2:
            r15 = r1
            java.lang.String r4 = "After user selection adjustment, the final candidate is not changed due to low rssi"
            r14.localLog(r4)
        L_0x01a8:
            return r15
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiNetworkSelector.overrideCandidateWithUserConnectChoice(android.net.wifi.WifiConfiguration):android.net.wifi.WifiConfiguration");
    }

    public WifiConfiguration selectNetwork(List<ScanDetail> scanDetails, HashSet<String> bssidBlacklist, WifiInfo wifiInfo, boolean connected, boolean disconnected, boolean untrustedNetworkAllowed, boolean bluetoothConnected) {
        int selectedNetworkId;
        int activeExperimentId;
        int networkId;
        ScanDetail scanDetail;
        List<ScanDetail> list = scanDetails;
        WifiInfo wifiInfo2 = wifiInfo;
        boolean z = connected;
        this.mFilteredNetworks.clear();
        this.mConnectableNetworks.clear();
        if (scanDetails.size() == 0) {
            localLog("Empty connectivity scan result");
            return null;
        }
        WifiConfiguration currentNetwork = this.mWifiConfigManager.getConfiguredNetwork(wifiInfo.getNetworkId());
        String currentBssid = wifiInfo.getBSSID();
        if (!isNetworkSelectionNeeded(list, wifiInfo2, z, disconnected)) {
            return null;
        }
        updateConfiguredNetworks();
        for (NetworkEvaluator registeredEvaluator : this.mEvaluators) {
            registeredEvaluator.update(list);
        }
        this.mFilteredNetworks = filterScanResults(list, bssidBlacklist, z && wifiInfo2.score >= 40, currentBssid);
        if (this.mFilteredNetworks.size() == 0) {
            return null;
        }
        int lastUserSelectedNetworkId = this.mWifiConfigManager.getLastSelectedNetwork();
        double lastSelectionWeight = calculateLastSelectionWeight();
        ArraySet<Integer> mNetworkIds = new ArraySet<>();
        WifiCandidates wifiCandidates = new WifiCandidates(this.mWifiScoreCard);
        if (currentNetwork != null) {
            wifiCandidates.setCurrent(currentNetwork.networkId, currentBssid);
        }
        WifiConfiguration selectedNetwork = null;
        for (NetworkEvaluator registeredEvaluator2 : this.mEvaluators) {
            localLog("About to run " + registeredEvaluator2.getName() + " :");
            ArrayList arrayList = new ArrayList(this.mFilteredNetworks);
            String str = TAG;
            WifiConfiguration selectedNetwork2 = selectedNetwork;
            WifiCandidates wifiCandidates2 = wifiCandidates;
            ArraySet<Integer> mNetworkIds2 = mNetworkIds;
            WifiConfiguration choice = registeredEvaluator2.evaluateNetworks(arrayList, currentNetwork, currentBssid, connected, untrustedNetworkAllowed, new NetworkEvaluator.OnConnectableListener(mNetworkIds, lastUserSelectedNetworkId, wifiCandidates, registeredEvaluator2, lastSelectionWeight) {
                private final /* synthetic */ ArraySet f$1;
                private final /* synthetic */ int f$2;
                private final /* synthetic */ WifiCandidates f$3;
                private final /* synthetic */ WifiNetworkSelector.NetworkEvaluator f$4;
                private final /* synthetic */ double f$5;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                    this.f$5 = r6;
                }

                public final void onConnectable(ScanDetail scanDetail, WifiConfiguration wifiConfiguration, int i) {
                    WifiNetworkSelector.this.lambda$selectNetwork$0$WifiNetworkSelector(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, scanDetail, wifiConfiguration, i);
                }
            }, bluetoothConnected);
            if (choice != null && !mNetworkIds2.contains(Integer.valueOf(choice.networkId))) {
                Log.wtf(str, registeredEvaluator2.getName() + " failed to report choice with noConnectibleListener");
            }
            if (selectedNetwork2 != null || choice == null) {
                selectedNetwork = selectedNetwork2;
            } else {
                WifiConfiguration selectedNetwork3 = choice;
                localLog(registeredEvaluator2.getName() + " selects " + toNetworkString(selectedNetwork3));
                selectedNetwork = selectedNetwork3;
            }
            List<ScanDetail> list2 = scanDetails;
            HashSet<String> hashSet = bssidBlacklist;
            WifiInfo wifiInfo3 = wifiInfo;
            mNetworkIds = mNetworkIds2;
            wifiCandidates = wifiCandidates2;
            boolean z2 = connected;
        }
        String str2 = TAG;
        WifiConfiguration selectedNetwork4 = selectedNetwork;
        WifiCandidates wifiCandidates3 = wifiCandidates;
        ArraySet<Integer> mNetworkIds3 = mNetworkIds;
        if (this.mConnectableNetworks.size() != wifiCandidates3.size()) {
            localLog("Connectable: " + this.mConnectableNetworks.size() + " Candidates: " + wifiCandidates3.size());
        }
        Collection<Collection<WifiCandidates.Candidate>> groupedCandidates = wifiCandidates3.getGroupedCandidates();
        for (Collection<WifiCandidates.Candidate> group : groupedCandidates) {
            WifiCandidates.Candidate best = null;
            for (WifiCandidates.Candidate candidate : group) {
                if (best == null || candidate.getEvaluatorId() < best.getEvaluatorId() || (candidate.getEvaluatorId() == best.getEvaluatorId() && candidate.getEvaluatorScore() > best.getEvaluatorScore())) {
                    best = candidate;
                }
            }
            if (!(best == null || (scanDetail = best.getScanDetail()) == null)) {
                this.mWifiConfigManager.setNetworkCandidateScanResult(best.getNetworkConfigId(), scanDetail.getScanResult(), best.getEvaluatorScore());
            }
        }
        ArrayMap<Integer, Integer> experimentNetworkSelections = new ArrayMap<>();
        if (selectedNetwork4 == null) {
            selectedNetworkId = -1;
        } else {
            selectedNetworkId = selectedNetwork4.networkId;
        }
        int legacySelectedNetworkId = selectedNetworkId;
        boolean legacyOverrideWanted = true;
        WifiCandidates.CandidateScorer activeScorer = getActiveCandidateScorer();
        Iterator<WifiCandidates.CandidateScorer> it = this.mCandidateScorers.values().iterator();
        int selectedNetworkId2 = selectedNetworkId;
        while (it.hasNext() != 0) {
            WifiCandidates.CandidateScorer candidateScorer = it.next();
            Iterator<WifiCandidates.CandidateScorer> it2 = it;
            WifiCandidates wifiCandidates4 = wifiCandidates3;
            try {
                WifiCandidates.ScoredCandidate choice2 = wifiCandidates4.choose(candidateScorer);
                wifiCandidates3 = wifiCandidates4;
                if (choice2.candidateKey == null) {
                    networkId = -1;
                } else {
                    networkId = choice2.candidateKey.networkId;
                }
                String chooses = " would choose ";
                if (candidateScorer == activeScorer) {
                    chooses = " chooses ";
                    legacyOverrideWanted = candidateScorer.userConnectChoiceOverrideWanted();
                    selectedNetworkId2 = networkId;
                }
                int selectedNetworkId3 = selectedNetworkId2;
                boolean legacyOverrideWanted2 = legacyOverrideWanted;
                String chooses2 = chooses;
                boolean legacyOverrideWanted3 = legacyOverrideWanted2;
                String id = candidateScorer.getIdentifier();
                WifiCandidates.CandidateScorer candidateScorer2 = candidateScorer;
                int expid = experimentIdFromIdentifier(id);
                WifiConfiguration selectedNetwork5 = selectedNetwork4;
                StringBuilder sb = new StringBuilder();
                sb.append(id);
                sb.append(chooses2);
                sb.append(networkId);
                String str3 = chooses2;
                sb.append(" score ");
                sb.append(choice2.value);
                sb.append("+/-");
                sb.append(choice2.err);
                sb.append(" expid ");
                sb.append(expid);
                localLog(sb.toString());
                experimentNetworkSelections.put(Integer.valueOf(expid), Integer.valueOf(networkId));
                it = it2;
                selectedNetworkId2 = selectedNetworkId3;
                legacyOverrideWanted = legacyOverrideWanted3;
                selectedNetwork4 = selectedNetwork5;
                currentNetwork = currentNetwork;
                mNetworkIds3 = mNetworkIds3;
            } catch (RuntimeException e) {
                WifiCandidates.CandidateScorer candidateScorer3 = candidateScorer;
                wifiCandidates3 = wifiCandidates4;
                Log.wtf(str2, "Exception running a CandidateScorer", e);
                it = it2;
                selectedNetwork4 = selectedNetwork4;
                currentNetwork = currentNetwork;
                mNetworkIds3 = mNetworkIds3;
            }
        }
        ArraySet<Integer> arraySet = mNetworkIds3;
        WifiConfiguration wifiConfiguration = currentNetwork;
        if (activeScorer == null) {
            activeExperimentId = 0;
        } else {
            activeExperimentId = experimentIdFromIdentifier(activeScorer.getIdentifier());
        }
        experimentNetworkSelections.put(0, Integer.valueOf(legacySelectedNetworkId));
        for (Map.Entry<Integer, Integer> entry : experimentNetworkSelections.entrySet()) {
            int experimentId = entry.getKey().intValue();
            if (experimentId != activeExperimentId) {
                this.mWifiMetrics.logNetworkSelectionDecision(experimentId, activeExperimentId, selectedNetworkId2 == entry.getValue().intValue(), groupedCandidates.size());
                experimentNetworkSelections = experimentNetworkSelections;
            }
        }
        WifiConfiguration selectedNetwork6 = this.mWifiConfigManager.getConfiguredNetwork(selectedNetworkId2);
        if (selectedNetwork6 == null || !legacyOverrideWanted) {
            return selectedNetwork6;
        }
        WifiConfiguration selectedNetwork7 = overrideCandidateWithUserConnectChoice(selectedNetwork6);
        this.mLastNetworkSelectionTimeStamp = this.mClock.getElapsedSinceBootMillis();
        return selectedNetwork7;
    }

    public /* synthetic */ void lambda$selectNetwork$0$WifiNetworkSelector(ArraySet mNetworkIds, int lastUserSelectedNetworkId, WifiCandidates wifiCandidates, NetworkEvaluator registeredEvaluator, double lastSelectionWeight, ScanDetail scanDetail, WifiConfiguration config, int score) {
        WifiConfiguration wifiConfiguration = config;
        if (wifiConfiguration != null) {
            this.mConnectableNetworks.add(Pair.create(scanDetail, config));
            ArraySet arraySet = mNetworkIds;
            mNetworkIds.add(Integer.valueOf(wifiConfiguration.networkId));
            if (wifiConfiguration.networkId == lastUserSelectedNetworkId) {
                wifiCandidates.add(scanDetail, config, registeredEvaluator.getId(), score, lastSelectionWeight);
                WifiCandidates wifiCandidates2 = wifiCandidates;
                ScanDetail scanDetail2 = scanDetail;
                int i = score;
            } else {
                WifiCandidates wifiCandidates3 = wifiCandidates;
                wifiCandidates.add(scanDetail, wifiConfiguration, registeredEvaluator.getId(), score);
            }
            this.mWifiMetrics.setNominatorForNetwork(wifiConfiguration.networkId, evaluatorIdToNominatorId(registeredEvaluator.getId()));
            return;
        }
        ArraySet arraySet2 = mNetworkIds;
        int i2 = lastUserSelectedNetworkId;
        WifiCandidates wifiCandidates4 = wifiCandidates;
        ScanDetail scanDetail3 = scanDetail;
        int i3 = score;
    }

    private static int evaluatorIdToNominatorId(int evaluatorId) {
        if (evaluatorId == 0) {
            return 2;
        }
        if (evaluatorId == 1) {
            return 3;
        }
        if (evaluatorId == 2) {
            return 4;
        }
        if (evaluatorId == 3) {
            return 5;
        }
        if (evaluatorId == 4) {
            return 6;
        }
        Log.e(TAG, "UnrecognizedEvaluatorId" + evaluatorId);
        return 0;
    }

    private double calculateLastSelectionWeight() {
        if (this.mWifiConfigManager.getLastSelectedNetwork() != -1) {
            return Math.min(Math.max(1.0d - (((double) (this.mClock.getElapsedSinceBootMillis() - this.mWifiConfigManager.getLastSelectedTimeStamp())) / 2.88E7d), 0.0d), 1.0d);
        }
        return 0.0d;
    }

    private WifiCandidates.CandidateScorer getActiveCandidateScorer() {
        int i;
        WifiCandidates.CandidateScorer ans = this.mCandidateScorers.get(PRESET_CANDIDATE_SCORER_NAME);
        int overrideExperimentId = this.mScoringParams.getExperimentIdentifier();
        if (overrideExperimentId >= MIN_SCORER_EXP_ID) {
            Iterator<WifiCandidates.CandidateScorer> it = this.mCandidateScorers.values().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                WifiCandidates.CandidateScorer candidateScorer = it.next();
                if (experimentIdFromIdentifier(candidateScorer.getIdentifier()) == overrideExperimentId) {
                    ans = candidateScorer;
                    break;
                }
            }
        }
        if (ans == null) {
            Log.wtf(TAG, "CompatibilityScorer is not registered!");
        }
        WifiMetrics wifiMetrics = this.mWifiMetrics;
        if (ans == null) {
            i = 0;
        } else {
            i = experimentIdFromIdentifier(ans.getIdentifier());
        }
        wifiMetrics.setNetworkSelectorExperimentId(i);
        return ans;
    }

    public void registerNetworkEvaluator(NetworkEvaluator evaluator) {
        this.mEvaluators.add((NetworkEvaluator) Preconditions.checkNotNull(evaluator));
    }

    public void registerCandidateScorer(WifiCandidates.CandidateScorer candidateScorer) {
        String name = ((WifiCandidates.CandidateScorer) Preconditions.checkNotNull(candidateScorer)).getIdentifier();
        if (name != null) {
            this.mCandidateScorers.put(name, candidateScorer);
        }
    }

    public void unregisterCandidateScorer(WifiCandidates.CandidateScorer candidateScorer) {
        String name = ((WifiCandidates.CandidateScorer) Preconditions.checkNotNull(candidateScorer)).getIdentifier();
        if (name != null) {
            this.mCandidateScorers.remove(name);
        }
    }

    public static int experimentIdFromIdentifier(String id) {
        return MIN_SCORER_EXP_ID + (((int) (((long) id.hashCode()) & 2147483647L)) % ID_SUFFIX_MOD);
    }

    WifiNetworkSelector(Context context, WifiScoreCard wifiScoreCard, ScoringParams scoringParams, WifiConfigManager configManager, Clock clock, LocalLog localLog, WifiMetrics wifiMetrics, WifiNative wifiNative) {
        this.mContext = context;
        this.mWifiConfigManager = configManager;
        this.mClock = clock;
        this.mWifiScoreCard = wifiScoreCard;
        this.mScoringParams = scoringParams;
        this.mLocalLog = localLog;
        this.mWifiMetrics = wifiMetrics;
        this.mWifiNative = wifiNative;
        this.mEnableAutoJoinWhenAssociated = CSC_SUPPORT_ASSOCIATED_NETWORK_SELECTION;
        this.mStayOnNetworkMinimumTxRate = context.getResources().getInteger(17694994);
        this.mStayOnNetworkMinimumRxRate = context.getResources().getInteger(17694993);
    }
}
