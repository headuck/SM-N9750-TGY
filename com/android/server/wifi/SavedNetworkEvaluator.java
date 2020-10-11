package com.android.server.wifi;

import android.content.Context;
import android.net.RssiCurve;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiNetworkSelector;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.util.GeneralUtil;
import com.android.server.wifi.util.RilUtil;
import com.android.server.wifi.util.TelephonyUtil;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.samsung.android.server.wifi.SemSarManager;
import java.util.Iterator;
import java.util.List;

public class SavedNetworkEvaluator implements WifiNetworkSelector.NetworkEvaluator {
    private static final int BLUETOOTH_CONNECTION_ACTIVE_PENALTY = 40;
    private static final RssiCurve CHANNEL_UTILIZATION_SCORES = new RssiCurve(0, 85, new byte[]{1, 0, -1});
    private static final RssiCurve ESTIMATED_AIR_TIME_FRACTION_SCORES = new RssiCurve(0, 85, new byte[]{-1, 0, 1});
    @VisibleForTesting
    public static final int LAST_SELECTION_AWARD_DECAY_MSEC = 60000;
    private static final String NAME = "SavedNetworkEvaluator";
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    private final int mBand5GHzAward;
    private final int mBand5GHzExtraAward;
    private final int mBluetoothConnectionActivePenalty;
    private final Clock mClock;
    private final WifiConnectivityHelper mConnectivityHelper;
    private final Context mContext;
    private final boolean mIsSupportBssidWhitelist;
    private final int mLastSelectionAward;
    private final LocalLog mLocalLog;
    private final int mRssiScoreOffset;
    private final int mRssiScoreSlope;
    private final int mSameBssidAward;
    private final int mSameNetworkAward;
    private final ScoringParams mScoringParams;
    private final int mSecurityAward;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager = ((TelephonyManager) this.mContext.getSystemService("phone"));
    private final WifiConfigManager mWifiConfigManager;
    private final WifiMetrics mWifiMetrics;

    SavedNetworkEvaluator(Context context, ScoringParams scoringParams, WifiConfigManager configManager, Clock clock, LocalLog localLog, WifiConnectivityHelper connectivityHelper, SubscriptionManager subscriptionManager, WifiMetrics wifiMetrics) {
        this.mContext = context;
        this.mScoringParams = scoringParams;
        this.mWifiConfigManager = configManager;
        this.mClock = clock;
        this.mLocalLog = localLog;
        this.mConnectivityHelper = connectivityHelper;
        this.mSubscriptionManager = subscriptionManager;
        this.mWifiMetrics = wifiMetrics;
        this.mRssiScoreSlope = context.getResources().getInteger(17694978);
        this.mRssiScoreOffset = context.getResources().getInteger(17694977);
        this.mSameBssidAward = context.getResources().getInteger(17694979);
        this.mSameNetworkAward = context.getResources().getInteger(17694989);
        this.mLastSelectionAward = context.getResources().getInteger(17694975);
        this.mSecurityAward = context.getResources().getInteger(17694980);
        this.mBand5GHzAward = context.getResources().getInteger(17694972);
        this.mBand5GHzExtraAward = this.mBand5GHzAward / 2;
        this.mBluetoothConnectionActivePenalty = 40;
        this.mIsSupportBssidWhitelist = OpBrandingLoader.Vendor.KOO == mOpBranding || OpBrandingLoader.Vendor.SKT == mOpBranding || OpBrandingLoader.Vendor.KTT == mOpBranding || OpBrandingLoader.Vendor.LGU == mOpBranding;
    }

    private void localLog(String log) {
        this.mLocalLog.log(log);
    }

    public int getId() {
        return 0;
    }

    public String getName() {
        return NAME;
    }

    public void update(List<ScanDetail> list) {
    }

    private int calculateBssidScore(ScanDetail scanDetail, WifiConfiguration network, WifiConfiguration currentNetwork, String currentBssid, StringBuffer sbuf, boolean bluetoothConnected) {
        int estimatedAirTimeFraction;
        WifiConfiguration wifiConfiguration = network;
        WifiConfiguration wifiConfiguration2 = currentNetwork;
        String str = currentBssid;
        StringBuffer stringBuffer = sbuf;
        ScanResult scanResult = scanDetail.getScanResult();
        NetworkDetail networkDetail = scanDetail.getNetworkDetail();
        boolean is5GHz = scanResult.is5GHz();
        stringBuffer.append("[ ");
        stringBuffer.append(scanResult.SSID);
        stringBuffer.append(" ");
        stringBuffer.append(scanResult.BSSID);
        stringBuffer.append(" RSSI:");
        stringBuffer.append(scanResult.level);
        stringBuffer.append(" ] ");
        int rssiSaturationThreshold = this.mScoringParams.getGoodRssi(scanResult.frequency);
        int score = 0 + ((this.mRssiScoreOffset + Math.min(scanResult.level, rssiSaturationThreshold)) * this.mRssiScoreSlope);
        stringBuffer.append(" RSSI score: ");
        stringBuffer.append(score);
        stringBuffer.append(",");
        if (is5GHz) {
            score += this.mBand5GHzAward;
            stringBuffer.append(" 5GHz bonus: ");
            stringBuffer.append(this.mBand5GHzAward);
            stringBuffer.append(",");
            if (scanResult.channelWidth >= 2) {
                score += this.mBand5GHzAward;
                stringBuffer.append(" Channel bandwidth bonus: " + this.mBand5GHzAward);
                stringBuffer.append(",");
            }
            if (networkDetail.getWifiMode() > 5) {
                score += this.mBand5GHzAward;
                stringBuffer.append(" Mode 11ax bonus: " + this.mBand5GHzAward);
                stringBuffer.append(",");
            }
            if (GeneralUtil.isDomesticModel() && GeneralUtil.isDomesticDfsChannel(scanResult.frequency)) {
                score += this.mBand5GHzExtraAward;
                stringBuffer.append(" DFS channel bonus: " + this.mBand5GHzExtraAward);
                stringBuffer.append(",");
            }
        }
        int lastUserSelectedNetworkId = this.mWifiConfigManager.getLastSelectedNetwork();
        if (lastUserSelectedNetworkId == -1 || lastUserSelectedNetworkId != wifiConfiguration.networkId) {
            int i = rssiSaturationThreshold;
        } else {
            long timeDifference = this.mClock.getElapsedSinceBootMillis() - this.mWifiConfigManager.getLastSelectedTimeStamp();
            if (timeDifference > 0) {
                boolean z = is5GHz;
                int i2 = rssiSaturationThreshold;
                int decay = (int) (timeDifference / 60000);
                int i3 = decay;
                int bonus = Math.max(this.mLastSelectionAward - decay, 0);
                score += bonus;
                stringBuffer.append(" User selection ");
                stringBuffer.append(timeDifference);
                stringBuffer.append(" ms ago, bonus: ");
                stringBuffer.append(bonus);
                stringBuffer.append(",");
            } else {
                int i4 = rssiSaturationThreshold;
            }
        }
        if (wifiConfiguration2 != null && wifiConfiguration.networkId == wifiConfiguration2.networkId) {
            score += this.mSameNetworkAward;
            stringBuffer.append(" Same network bonus: ");
            stringBuffer.append(this.mSameNetworkAward);
            stringBuffer.append(",");
            if (this.mConnectivityHelper.isFirmwareRoamingSupported() && str != null && !str.equals(scanResult.BSSID)) {
                score += this.mSameBssidAward;
                stringBuffer.append(" Equivalent BSSID bonus: ");
                stringBuffer.append(this.mSameBssidAward);
                stringBuffer.append(",");
            }
        }
        if (str != null && str.equals(scanResult.BSSID)) {
            score += this.mSameBssidAward;
            stringBuffer.append(" Same BSSID bonus: ");
            stringBuffer.append(this.mSameBssidAward);
            stringBuffer.append(",");
        }
        if (!WifiConfigurationUtil.isConfigForOpenNetwork(network)) {
            score += this.mSecurityAward;
            stringBuffer.append(" Secure network bonus: ");
            stringBuffer.append(this.mSecurityAward);
            stringBuffer.append(",");
        }
        if (bluetoothConnected && scanResult.is24GHz()) {
            score -= this.mBluetoothConnectionActivePenalty;
            stringBuffer.append(" Active Bluetooth connection penalty: -" + this.mBluetoothConnectionActivePenalty);
            stringBuffer.append(",");
        }
        if (networkDetail != null) {
            score += CHANNEL_UTILIZATION_SCORES.lookupScore(networkDetail.getChannelUtilization());
            stringBuffer.append(" Channel utilization bonus: ");
            stringBuffer.append(CHANNEL_UTILIZATION_SCORES.lookupScore(networkDetail.getChannelUtilization()));
            stringBuffer.append(",");
        }
        if (!(networkDetail == null || (estimatedAirTimeFraction = networkDetail.getEstimatedAirTimeFraction(1)) == -1)) {
            score += ESTIMATED_AIR_TIME_FRACTION_SCORES.lookupScore(estimatedAirTimeFraction);
            stringBuffer.append(" Estimated air fractional time (AC_BE) bonus: ");
            stringBuffer.append(ESTIMATED_AIR_TIME_FRACTION_SCORES.lookupScore(estimatedAirTimeFraction));
            stringBuffer.append(",");
        }
        stringBuffer.append(" ## Total score: ");
        stringBuffer.append(score);
        stringBuffer.append("\n");
        return score;
    }

    public WifiConfiguration evaluateNetworks(List<ScanDetail> scanDetails, WifiConfiguration currentNetwork, String currentBssid, boolean connected, boolean untrustedNetworkAllowed, WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener, boolean bluetoothConnected) {
        Iterator<ScanDetail> it;
        StringBuffer scoreHistory = new StringBuffer();
        Iterator<ScanDetail> it2 = scanDetails.iterator();
        int highestScore = Integer.MIN_VALUE;
        ScanResult scanResultCandidate = null;
        WifiConfiguration candidate = null;
        while (it2.hasNext() != 0) {
            ScanDetail scanDetail = it2.next();
            ScanResult scanResult = scanDetail.getScanResult();
            WifiConfiguration network = this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail);
            if (network == null) {
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener2 = onConnectableListener;
                it = it2;
            } else if (network.isPasspoint()) {
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener3 = onConnectableListener;
                it = it2;
            } else if (network.isEphemeral()) {
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener4 = onConnectableListener;
                it = it2;
            } else if (network.semAutoReconnect == 0) {
                localLog("Network: " + WifiNetworkSelector.toNetworkString(network) + " has autoReconnect false. Skip");
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener5 = onConnectableListener;
                it = it2;
            } else if (!this.mWifiConfigManager.getAutoConnectCarrierApEnabled() && network.semIsVendorSpecificSsid && !RilUtil.isMptcpEnabled(this.mContext)) {
                localLog("Network: " + WifiNetworkSelector.toNetworkString(network) + " is carrier AP. autoConnectCarrierAp is disabled.  Mptcp is disabled. Skip");
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener6 = onConnectableListener;
                it = it2;
            } else if (this.mIsSupportBssidWhitelist && (("iptime".equals(network.getPrintableSsid()) || "iptime5G".equals(network.getPrintableSsid())) && WifiConfigurationUtil.isConfigForOpenNetwork(network) && network.bssidWhitelist != null && network.bssidWhitelist.size() > 0 && !network.bssidWhitelist.containsBssid(scanResult.BSSID))) {
                localLog("[KOR] Network: " + WifiNetworkSelector.toNetworkString(network) + " has BSSID whitelist and " + scanResult.BSSID + " is not contained in the whitelist. Skip");
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener7 = onConnectableListener;
                it = it2;
            } else if (OpBrandingLoader.Vendor.ATT == mOpBranding && !this.mWifiConfigManager.getNetworkAutoConnectEnabled() && "attwifi".equals(network.getPrintableSsid())) {
                localLog("[ATT] Network: " + WifiNetworkSelector.toNetworkString(network) + " has ATT mNetworkAutoConnectEnabled false. Skip");
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener8 = onConnectableListener;
                it = it2;
            } else if (OpBrandingLoader.Vendor.ATT == mOpBranding && this.mWifiMetrics.getScanCount() < 3) {
                localLog("[ATT] Network: " + WifiNetworkSelector.toNetworkString(network) + " is skipped because of low scan count.");
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener9 = onConnectableListener;
                it = it2;
            } else if (OpBrandingLoader.Vendor.SKT == mOpBranding && network.semIsVendorSpecificSsid && scanDetail.getNetworkDetail().hasInterworking() && !scanDetail.getNetworkDetail().isInternet()) {
                localLog("[SKT] Network: " + WifiNetworkSelector.toNetworkString(network) + " has internet accessibility false. Skip");
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener10 = onConnectableListener;
                it = it2;
            } else if (RilUtil.isWifiOnly(this.mContext) || SemSarManager.isRfTestMode() || ((!scanResult.is24GHz() || scanResult.level >= network.entryRssi24GHz) && (!scanResult.is5GHz() || scanResult.level >= network.entryRssi5GHz))) {
                WifiConfiguration.NetworkSelectionStatus status = network.getNetworkSelectionStatus();
                status.setSeenInLastQualifiedNetworkSelection(true);
                if (!status.isNetworkEnabled()) {
                    WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener11 = onConnectableListener;
                    it = it2;
                } else if (network.BSSID != null && !network.BSSID.equals("any") && !network.BSSID.equals(scanResult.BSSID)) {
                    localLog("Network " + WifiNetworkSelector.toNetworkString(network) + " has specified BSSID " + network.BSSID + ". Skip " + scanResult.BSSID);
                    WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener12 = onConnectableListener;
                    it = it2;
                } else if (!TelephonyUtil.isSimConfig(network) || TelephonyUtil.isSimPresent(this.mSubscriptionManager)) {
                    if (TelephonyUtil.isSimConfig(network) && this.mContext != null) {
                        if (this.mTelephonyManager == null) {
                            localLog("Network: " + WifiNetworkSelector.toNetworkString(network) + " - mTelephonyManager is null");
                        } else if (!TelephonyUtil.isVendorApUsimUseable(network.getPrintableSsid(), this.mTelephonyManager)) {
                            localLog("Network: " + WifiNetworkSelector.toNetworkString(network) + " has VendorApUsimUseable false and EAP-SIM/AKA/AKA_PRIME. Skip");
                            WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener13 = onConnectableListener;
                            it = it2;
                        }
                    }
                    it = it2;
                    String str = "Network ";
                    int score = calculateBssidScore(scanDetail, network, currentNetwork, currentBssid, scoreHistory, bluetoothConnected);
                    if (score > status.getCandidateScore() || (score == status.getCandidateScore() && status.getCandidate() != null && scanResult.level > status.getCandidate().level)) {
                        this.mWifiConfigManager.setNetworkCandidateScanResult(network.networkId, scanResult, score);
                    }
                    if (network.useExternalScores) {
                        localLog(str + WifiNetworkSelector.toNetworkString(network) + " has external score.");
                        WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener14 = onConnectableListener;
                    } else {
                        onConnectableListener.onConnectable(scanDetail, this.mWifiConfigManager.getConfiguredNetwork(network.networkId), score);
                        if (score > highestScore || (score == highestScore && scanResultCandidate != null && scanResult.level > scanResultCandidate.level)) {
                            int highestScore2 = score;
                            ScanResult scanResultCandidate2 = scanResult;
                            this.mWifiConfigManager.setNetworkCandidateScanResult(network.networkId, scanResultCandidate2, highestScore2);
                            highestScore = highestScore2;
                            scanResultCandidate = scanResultCandidate2;
                            candidate = this.mWifiConfigManager.getConfiguredNetwork(network.networkId);
                        }
                        it2 = it;
                    }
                } else {
                    localLog("Network: " + WifiNetworkSelector.toNetworkString(network) + " has EAP SIM/AKA/AKA' security type and SIM is not present. Skip");
                    WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener15 = onConnectableListener;
                    it = it2;
                }
            } else {
                localLog("Network: " + WifiNetworkSelector.toNetworkString(network) + " has entryRssi (" + network.entryRssi24GHz + ", " + network.entryRssi5GHz + "). And current scan result has freq = " + scanResult.frequency + " and rssi = " + scanResult.level + ". Skip");
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener16 = onConnectableListener;
                it = it2;
            }
            it2 = it;
        }
        WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener17 = onConnectableListener;
        if (scoreHistory.length() > 0) {
            localLog("\n" + scoreHistory.toString());
        }
        if (scanResultCandidate == null) {
            localLog("did not see any good candidates.");
        }
        return candidate;
    }
}
