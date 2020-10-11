package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import com.android.server.wifi.WifiNetworkSelector;
import com.android.server.wifi.util.RilUtil;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.TelephonyUtil;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.samsung.android.server.wifi.SemSarManager;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class CarrierNetworkEvaluator implements WifiNetworkSelector.NetworkEvaluator {
    private static final String TAG = "CarrierNetworkEvaluator";
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    private final CarrierNetworkConfig mCarrierNetworkConfig;
    private final Context mContext;
    private final LocalLog mLocalLog;
    private TelephonyManager mTelephonyManager;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiInjector mWifiInjector;
    private final WifiMetrics mWifiMetrics;

    public CarrierNetworkEvaluator(Context context, WifiConfigManager wifiConfigManager, CarrierNetworkConfig carrierNetworkConfig, LocalLog localLog, WifiInjector wifiInjector, WifiMetrics wifiMetrics) {
        this.mContext = context;
        this.mWifiConfigManager = wifiConfigManager;
        this.mCarrierNetworkConfig = carrierNetworkConfig;
        this.mLocalLog = localLog;
        this.mWifiInjector = wifiInjector;
        this.mWifiMetrics = wifiMetrics;
    }

    private TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = this.mWifiInjector.makeTelephonyManager();
        }
        return this.mTelephonyManager;
    }

    public int getId() {
        return 3;
    }

    public String getName() {
        return TAG;
    }

    public void update(List<ScanDetail> list) {
    }

    public WifiConfiguration evaluateNetworks(List<ScanDetail> scanDetails, WifiConfiguration currentNetwork, String currentBssid, boolean connected, boolean untrustedNetworkAllowed, WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener, boolean bluetoothConnected) {
        if (!this.mCarrierNetworkConfig.isCarrierEncryptionInfoAvailable()) {
            return null;
        }
        int currentMaxRssi = Integer.MIN_VALUE;
        WifiConfiguration configWithMaxRssi = null;
        for (ScanDetail scanDetail : scanDetails) {
            ScanResult scanResult = scanDetail.getScanResult();
            if (!ScanResultUtil.isScanResultForEapNetwork(scanResult)) {
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener2 = onConnectableListener;
            } else if (!this.mCarrierNetworkConfig.isCarrierNetwork(scanResult.SSID)) {
                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener3 = onConnectableListener;
            } else {
                int eapType = this.mCarrierNetworkConfig.getNetworkEapType(scanResult.SSID);
                if (!TelephonyUtil.isSimEapMethod(eapType)) {
                    LocalLog localLog = this.mLocalLog;
                    localLog.log("CarrierNetworkEvaluator: eapType is not a carrier eap method: " + eapType);
                    WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener4 = onConnectableListener;
                } else if (this.mWifiConfigManager.wasEphemeralNetworkDeleted(ScanResultUtil.createQuotedSSID(scanResult.SSID))) {
                    LocalLog localLog2 = this.mLocalLog;
                    localLog2.log("CarrierNetworkEvaluator: Ignoring disabled ephemeral SSID: " + WifiNetworkSelector.toScanId(scanResult));
                    WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener5 = onConnectableListener;
                } else {
                    WifiConfiguration config = ScanResultUtil.createNetworkFromScanResult(scanResult);
                    if (config.enterpriseConfig == null) {
                        config.enterpriseConfig = new WifiEnterpriseConfig();
                    }
                    config.enterpriseConfig.setEapMethod(eapType);
                    WifiConfiguration existingNetwork = this.mWifiConfigManager.getConfiguredNetwork(config.configKey());
                    if (existingNetwork == null || existingNetwork.getNetworkSelectionStatus().isNetworkEnabled() || this.mWifiConfigManager.tryEnableNetwork(existingNetwork.networkId)) {
                        if (existingNetwork != null) {
                            config.semAutoReconnect = existingNetwork.semAutoReconnect;
                        }
                        config.semIsVendorSpecificSsid = true;
                        NetworkUpdateResult result = this.mWifiConfigManager.addOrUpdateNetwork(config, 1010);
                        if (!result.isSuccess()) {
                            LocalLog localLog3 = this.mLocalLog;
                            localLog3.log("CarrierNetworkEvaluator: Failed to add carrier network: " + config);
                            WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener6 = onConnectableListener;
                        } else if (!this.mWifiConfigManager.enableNetwork(result.getNetworkId(), false, 1010)) {
                            LocalLog localLog4 = this.mLocalLog;
                            localLog4.log("CarrierNetworkEvaluator: Failed to enable carrier network: " + config);
                            WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener7 = onConnectableListener;
                        } else if (!this.mWifiConfigManager.setNetworkCandidateScanResult(result.getNetworkId(), scanResult, 0)) {
                            LocalLog localLog5 = this.mLocalLog;
                            localLog5.log("CarrierNetworkEvaluator: Failed to set network candidate for carrier network: " + config);
                            WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener8 = onConnectableListener;
                        } else if (config.semAutoReconnect == 0) {
                            LocalLog localLog6 = this.mLocalLog;
                            localLog6.log("CarrierNetworkEvaluator: Ignoring autoReconnect false network: " + WifiNetworkSelector.toNetworkString(config));
                            WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener9 = onConnectableListener;
                        } else if (!this.mWifiConfigManager.getAutoConnectCarrierApEnabled() && config.semIsVendorSpecificSsid && !RilUtil.isMptcpEnabled(this.mContext)) {
                            LocalLog localLog7 = this.mLocalLog;
                            localLog7.log("CarrierNetworkEvaluator: Ignoring this network: " + WifiNetworkSelector.toNetworkString(config) + " because both autoConnectCarrierAp and Mptcp are disabled.");
                            WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener10 = onConnectableListener;
                        } else if (OpBrandingLoader.Vendor.ATT == mOpBranding && !this.mWifiConfigManager.getNetworkAutoConnectEnabled() && "attwifi".equals(config.getPrintableSsid())) {
                            LocalLog localLog8 = this.mLocalLog;
                            localLog8.log("CarrierNetworkEvaluator: [ATT] Ignoring this network: " + WifiNetworkSelector.toNetworkString(config) + " because ATT mNetworkAutoConnectEnabled is false.");
                            WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener11 = onConnectableListener;
                        } else if (OpBrandingLoader.Vendor.ATT == mOpBranding && this.mWifiMetrics.getScanCount() < 3) {
                            LocalLog localLog9 = this.mLocalLog;
                            localLog9.log("CarrierNetworkEvaluator: [ATT] Ignoring this network: " + WifiNetworkSelector.toNetworkString(config) + " because of low scan count.");
                            WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener12 = onConnectableListener;
                        } else if (OpBrandingLoader.Vendor.SKT == mOpBranding && config.semIsVendorSpecificSsid && scanDetail.getNetworkDetail().hasInterworking() && !scanDetail.getNetworkDetail().isInternet()) {
                            LocalLog localLog10 = this.mLocalLog;
                            localLog10.log("CarrierNetworkEvaluator: [SKT] Ignoring this network: " + WifiNetworkSelector.toNetworkString(config) + " because of internet accessibility false.");
                            WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener13 = onConnectableListener;
                        } else if (RilUtil.isWifiOnly(this.mContext) || SemSarManager.isRfTestMode() || ((!scanResult.is24GHz() || scanResult.level >= config.entryRssi24GHz) && (!scanResult.is5GHz() || scanResult.level >= config.entryRssi5GHz))) {
                            WifiConfiguration config2 = this.mWifiConfigManager.getConfiguredNetwork(result.getNetworkId());
                            WifiConfiguration.NetworkSelectionStatus nss = null;
                            if (config2 != null) {
                                nss = config2.getNetworkSelectionStatus();
                            }
                            if (nss == null) {
                                LocalLog localLog11 = this.mLocalLog;
                                localLog11.log("CarrierNetworkEvaluator: null network selection status for: " + config2);
                                WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener14 = onConnectableListener;
                            } else {
                                if (nss.getCandidate() != null && nss.getCandidate().level < scanResult.level) {
                                    this.mWifiConfigManager.updateScanDetailForNetwork(result.getNetworkId(), scanDetail);
                                }
                                onConnectableListener.onConnectable(scanDetail, config2, 0);
                                if (scanResult.level > currentMaxRssi) {
                                    configWithMaxRssi = config2;
                                    currentMaxRssi = scanResult.level;
                                }
                            }
                        } else {
                            LocalLog localLog12 = this.mLocalLog;
                            localLog12.log("CarrierNetworkEvaluator: Ignoring this network: " + WifiNetworkSelector.toNetworkString(config) + " because it has entryRssi (" + config.entryRssi24GHz + ", " + config.entryRssi5GHz + "). And current scan result has freq = " + scanResult.frequency + " and rssi = " + scanResult.level + ".");
                            WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener15 = onConnectableListener;
                        }
                    } else {
                        LocalLog localLog13 = this.mLocalLog;
                        localLog13.log("CarrierNetworkEvaluator: Ignoring blacklisted network: " + WifiNetworkSelector.toNetworkString(existingNetwork));
                        WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener16 = onConnectableListener;
                    }
                }
            }
        }
        WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener17 = onConnectableListener;
        return configWithMaxRssi;
    }
}
