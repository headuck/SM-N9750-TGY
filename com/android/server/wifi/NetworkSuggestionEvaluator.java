package com.android.server.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiNetworkSuggestion;
import android.util.LocalLog;
import android.util.Log;
import com.android.server.wifi.NetworkSuggestionEvaluator;
import com.android.server.wifi.WifiNetworkSelector;
import com.android.server.wifi.util.ScanResultUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class NetworkSuggestionEvaluator implements WifiNetworkSelector.NetworkEvaluator {
    private static final String TAG = "NetworkSuggestionEvaluator";
    /* access modifiers changed from: private */
    public final LocalLog mLocalLog;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiNetworkSuggestionsManager mWifiNetworkSuggestionsManager;

    NetworkSuggestionEvaluator(WifiNetworkSuggestionsManager networkSuggestionsManager, WifiConfigManager wifiConfigManager, LocalLog localLog) {
        this.mWifiNetworkSuggestionsManager = networkSuggestionsManager;
        this.mWifiConfigManager = wifiConfigManager;
        this.mLocalLog = localLog;
    }

    public void update(List<ScanDetail> list) {
    }

    public WifiConfiguration evaluateNetworks(List<ScanDetail> scanDetails, WifiConfiguration currentNetwork, String currentBssid, boolean connected, boolean untrustedNetworkAllowed, WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener, boolean bluetoothConnected) {
        MatchMetaInfo matchMetaInfo = new MatchMetaInfo();
        for (int i = 0; i < scanDetails.size(); i++) {
            List<ScanDetail> list = scanDetails;
            ScanDetail scanDetail = scanDetails.get(i);
            ScanResult scanResult = scanDetail.getScanResult();
            if (this.mWifiConfigManager.wasEphemeralNetworkDeleted(ScanResultUtil.createQuotedSSID(scanResult.SSID))) {
                LocalLog localLog = this.mLocalLog;
                localLog.log("Ignoring disabled ephemeral SSID: " + WifiNetworkSelector.toScanId(scanResult));
            } else {
                Set<WifiNetworkSuggestion> matchingNetworkSuggestions = this.mWifiNetworkSuggestionsManager.getNetworkSuggestionsForScanDetail(scanDetail);
                if (matchingNetworkSuggestions != null && !matchingNetworkSuggestions.isEmpty()) {
                    WifiConfiguration wCmConfiguredNetwork = this.mWifiConfigManager.getConfiguredNetwork(((WifiNetworkSuggestion) matchingNetworkSuggestions.stream().findAny().get()).wifiConfiguration.configKey());
                    if (wCmConfiguredNetwork == null || wCmConfiguredNetwork.getNetworkSelectionStatus().isNetworkEnabled() || this.mWifiConfigManager.tryEnableNetwork(wCmConfiguredNetwork.networkId)) {
                        matchMetaInfo.putAll(matchingNetworkSuggestions, wCmConfiguredNetwork, scanDetail);
                    } else {
                        LocalLog localLog2 = this.mLocalLog;
                        localLog2.log("Ignoring blacklisted network: " + WifiNetworkSelector.toNetworkString(wCmConfiguredNetwork));
                    }
                }
            }
        }
        List<ScanDetail> list2 = scanDetails;
        if (matchMetaInfo.isEmpty() != 0) {
            this.mLocalLog.log("did not see any matching network suggestions.");
            return null;
        }
        PerNetworkSuggestionMatchMetaInfo candidate = matchMetaInfo.findConnectableNetworksAndPickBest(onConnectableListener);
        if (candidate != null) {
            return candidate.wCmConfiguredNetwork;
        }
        Log.wtf(TAG, "Unexepectedly got null");
        return null;
    }

    /* access modifiers changed from: private */
    public WifiConfiguration addCandidateToWifiConfigManager(WifiConfiguration wifiConfiguration, int uid, String packageName) {
        wifiConfiguration.ephemeral = true;
        wifiConfiguration.fromWifiNetworkSuggestion = true;
        NetworkUpdateResult result = this.mWifiConfigManager.addOrUpdateNetwork(wifiConfiguration, uid, packageName);
        if (!result.isSuccess()) {
            this.mLocalLog.log("Failed to add network suggestion");
            return null;
        } else if (!this.mWifiConfigManager.updateNetworkSelectionStatus(result.getNetworkId(), 0)) {
            this.mLocalLog.log("Failed to make network suggestion selectable");
            return null;
        } else {
            return this.mWifiConfigManager.getConfiguredNetwork(result.getNetworkId());
        }
    }

    public int getId() {
        return 1;
    }

    public String getName() {
        return TAG;
    }

    private class PerNetworkSuggestionMatchMetaInfo {
        public final ScanDetail matchingScanDetail;
        public WifiConfiguration wCmConfiguredNetwork;
        public final WifiNetworkSuggestion wifiNetworkSuggestion;

        PerNetworkSuggestionMatchMetaInfo(WifiNetworkSuggestion wifiNetworkSuggestion2, @Nullable WifiConfiguration wCmConfiguredNetwork2, ScanDetail matchingScanDetail2) {
            this.wifiNetworkSuggestion = wifiNetworkSuggestion2;
            this.wCmConfiguredNetwork = wCmConfiguredNetwork2;
            this.matchingScanDetail = matchingScanDetail2;
        }
    }

    private class PerAppMatchMetaInfo {
        public final List<PerNetworkSuggestionMatchMetaInfo> networkInfos;

        private PerAppMatchMetaInfo() {
            this.networkInfos = new ArrayList();
        }

        public void put(WifiNetworkSuggestion wifiNetworkSuggestion, WifiConfiguration matchingWifiConfiguration, ScanDetail matchingScanDetail) {
            this.networkInfos.add(new PerNetworkSuggestionMatchMetaInfo(wifiNetworkSuggestion, matchingWifiConfiguration, matchingScanDetail));
        }

        public List<PerNetworkSuggestionMatchMetaInfo> getHighestPriorityNetworks() {
            Map<Integer, List<PerNetworkSuggestionMatchMetaInfo>> matchedNetworkInfosPerPriority = (Map) this.networkInfos.stream().collect(Collectors.toMap(C0333x4cc006.INSTANCE, C0334x4070411.INSTANCE, C0335xb560b5ff.INSTANCE));
            if (!matchedNetworkInfosPerPriority.isEmpty()) {
                return matchedNetworkInfosPerPriority.get(Collections.max(matchedNetworkInfosPerPriority.keySet()));
            }
            Log.wtf(NetworkSuggestionEvaluator.TAG, "Unexepectedly got empty");
            return Collections.EMPTY_LIST;
        }

        static /* synthetic */ List lambda$getHighestPriorityNetworks$2(List v1, List v2) {
            List<PerNetworkSuggestionMatchMetaInfo> concatList = new ArrayList<>(v1);
            concatList.addAll(v2);
            return concatList;
        }
    }

    private class MatchMetaInfo {
        private Map<String, PerAppMatchMetaInfo> mAppInfos;

        private MatchMetaInfo() {
            this.mAppInfos = new HashMap();
        }

        public void putAll(Set<WifiNetworkSuggestion> wifiNetworkSuggestions, WifiConfiguration wCmConfiguredNetwork, ScanDetail matchingScanDetail) {
            for (WifiNetworkSuggestion wifiNetworkSuggestion : wifiNetworkSuggestions) {
                this.mAppInfos.computeIfAbsent(wifiNetworkSuggestion.suggestorPackageName, new Function() {
                    public final Object apply(Object obj) {
                        return NetworkSuggestionEvaluator.MatchMetaInfo.this.lambda$putAll$0$NetworkSuggestionEvaluator$MatchMetaInfo((String) obj);
                    }
                }).put(wifiNetworkSuggestion, wCmConfiguredNetwork, matchingScanDetail);
            }
        }

        public /* synthetic */ PerAppMatchMetaInfo lambda$putAll$0$NetworkSuggestionEvaluator$MatchMetaInfo(String k) {
            return new PerAppMatchMetaInfo();
        }

        public boolean isEmpty() {
            return this.mAppInfos.isEmpty();
        }

        public PerNetworkSuggestionMatchMetaInfo findConnectableNetworksAndPickBest(WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener) {
            List<PerNetworkSuggestionMatchMetaInfo> allMatchedNetworkInfos = new ArrayList<>();
            for (PerAppMatchMetaInfo appInfo : this.mAppInfos.values()) {
                for (PerNetworkSuggestionMatchMetaInfo matchedNetworkInfo : appInfo.getHighestPriorityNetworks()) {
                    if (matchedNetworkInfo.wCmConfiguredNetwork == null) {
                        matchedNetworkInfo.wCmConfiguredNetwork = NetworkSuggestionEvaluator.this.addCandidateToWifiConfigManager(matchedNetworkInfo.wifiNetworkSuggestion.wifiConfiguration, matchedNetworkInfo.wifiNetworkSuggestion.suggestorUid, matchedNetworkInfo.wifiNetworkSuggestion.suggestorPackageName);
                        if (matchedNetworkInfo.wCmConfiguredNetwork != null) {
                            NetworkSuggestionEvaluator.this.mLocalLog.log(String.format("network suggestion candidate %s (new)", new Object[]{WifiNetworkSelector.toNetworkString(matchedNetworkInfo.wCmConfiguredNetwork)}));
                        }
                    } else {
                        NetworkSuggestionEvaluator.this.mLocalLog.log(String.format("network suggestion candidate %s (existing)", new Object[]{WifiNetworkSelector.toNetworkString(matchedNetworkInfo.wCmConfiguredNetwork)}));
                    }
                    allMatchedNetworkInfos.add(matchedNetworkInfo);
                    onConnectableListener.onConnectable(matchedNetworkInfo.matchingScanDetail, matchedNetworkInfo.wCmConfiguredNetwork, 0);
                }
            }
            PerNetworkSuggestionMatchMetaInfo networkInfo = (PerNetworkSuggestionMatchMetaInfo) allMatchedNetworkInfos.stream().max(Comparator.comparing(C0332x32e518c2.INSTANCE)).orElse((Object) null);
            if (networkInfo != null) {
                return networkInfo;
            }
            Log.wtf(NetworkSuggestionEvaluator.TAG, "Unexepectedly got null");
            return null;
        }
    }
}
