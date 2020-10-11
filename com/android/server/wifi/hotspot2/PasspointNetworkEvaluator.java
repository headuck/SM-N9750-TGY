package com.android.server.wifi.hotspot2;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Pair;
import com.android.server.wifi.CarrierNetworkConfig;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNetworkSelector;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.HSWanMetricsElement;
import com.android.server.wifi.util.RilUtil;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.TelephonyUtil;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.samsung.android.server.wifi.SemSarManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PasspointNetworkEvaluator implements WifiNetworkSelector.NetworkEvaluator {
    private static final String NAME = "PasspointNetworkEvaluator";
    private static final String SEC_FRIENDLY_NAME = "Samsung Hotspot2.0 Profile";
    private static final String VENDOR_FRIENDLY_NAME = "Vendor Hotspot2.0 Profile";
    private static String mImsi = "00101*";
    private static boolean mIsEnabledNetworkForOpenRoaming = false;
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    private final CarrierNetworkConfig mCarrierNetworkConfig;
    private final Context mContext;
    private final LocalLog mLocalLog;
    private final PasspointManager mPasspointManager;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiInjector mWifiInjector;

    private class PasspointNetworkCandidate {
        PasspointMatch mMatchStatus;
        PasspointProvider mProvider;
        ScanDetail mScanDetail;

        PasspointNetworkCandidate(PasspointProvider provider, PasspointMatch matchStatus, ScanDetail scanDetail) {
            this.mProvider = provider;
            this.mMatchStatus = matchStatus;
            this.mScanDetail = scanDetail;
        }
    }

    public PasspointNetworkEvaluator(Context context, PasspointManager passpointManager, WifiConfigManager wifiConfigManager, LocalLog localLog, CarrierNetworkConfig carrierNetworkConfig, WifiInjector wifiInjector, SubscriptionManager subscriptionManager) {
        this.mContext = context;
        this.mPasspointManager = passpointManager;
        this.mWifiConfigManager = wifiConfigManager;
        this.mLocalLog = localLog;
        this.mCarrierNetworkConfig = carrierNetworkConfig;
        this.mWifiInjector = wifiInjector;
        this.mSubscriptionManager = subscriptionManager;
    }

    private TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = this.mWifiInjector.makeTelephonyManager();
        }
        return this.mTelephonyManager;
    }

    public int getId() {
        return 2;
    }

    public String getName() {
        return NAME;
    }

    public void update(List<ScanDetail> list) {
    }

    public WifiConfiguration evaluateNetworks(List<ScanDetail> scanDetails, WifiConfiguration currentNetwork, String currentBssid, boolean connected, boolean untrustedNetworkAllowed, WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener, boolean bluetoothConnected) {
        PasspointConfiguration passpointconfig;
        HSWanMetricsElement wm;
        WifiConfiguration wifiConfiguration = currentNetwork;
        WifiNetworkSelector.NetworkEvaluator.OnConnectableListener onConnectableListener2 = onConnectableListener;
        if (!this.mPasspointManager.isPasspointEnabled()) {
            localLog("Passpoint is disabled");
            return null;
        }
        this.mPasspointManager.sweepCache();
        List<ScanDetail> filteredScanDetails = (List) scanDetails.stream().filter($$Lambda$PasspointNetworkEvaluator$GeomGkeNP2MEelBL59RV_0T1M8.INSTANCE).filter(new Predicate() {
            public final boolean test(Object obj) {
                return PasspointNetworkEvaluator.this.lambda$evaluateNetworks$1$PasspointNetworkEvaluator((ScanDetail) obj);
            }
        }).collect(Collectors.toList());
        createEphemeralProfileForMatchingAp(filteredScanDetails);
        List<PasspointNetworkCandidate> candidateList = new ArrayList<>();
        for (ScanDetail scanDetail : filteredScanDetails) {
            ScanResult scanResult = scanDetail.getScanResult();
            Map<Constants.ANQPElementType, ANQPElement> anqpElements = this.mPasspointManager.getANQPElements(scanDetail.getScanResult());
            if (OpBrandingLoader.Vendor.SKT == mOpBranding) {
                if (!scanDetail.getNetworkDetail().isInternet()) {
                    localLog("Passpoint network, ScanDetail: " + scanDetail + " has internet accessibility false. Skip");
                } else if (anqpElements != null && anqpElements.size() > 0 && (wm = (HSWanMetricsElement) anqpElements.get(Constants.ANQPElementType.HSWANMetrics)) != null && wm.isCapped()) {
                    this.mPasspointManager.forceRequestAnqp(scanDetail.getScanResult());
                    localLog("Passpoint network, ScanDetail: " + scanDetail + " has WAN capped. Skip and Request ANQP for update");
                }
            }
            WifiConfiguration configuredNetwork = this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail, true);
            if (configuredNetwork != null) {
                if (configuredNetwork.getNetworkSelectionStatus().isDisabledByReason(19)) {
                    localLog("Passpoint network " + configuredNetwork.configKey() + " is unstable AP. Skip");
                } else if (!RilUtil.isWifiOnly(this.mContext) && !SemSarManager.isRfTestMode() && ((scanResult.is24GHz() && scanResult.level < configuredNetwork.entryRssi24GHz) || (scanResult.is5GHz() && scanResult.level < configuredNetwork.entryRssi5GHz))) {
                    localLog("Passpoint network " + configuredNetwork.configKey() + " has entryRssi (" + configuredNetwork.entryRssi24GHz + ", " + configuredNetwork.entryRssi5GHz + "). And current scan result has freq = " + scanResult.frequency + " and rssi = " + scanResult.level + ". Skip");
                }
            }
            Pair<PasspointProvider, PasspointMatch> bestProvider = this.mPasspointManager.matchProvider(scanResult);
            if (bestProvider != null && ((!((PasspointProvider) bestProvider.first).isSimCredential() || TelephonyUtil.isSimPresent(this.mSubscriptionManager)) && (passpointconfig = ((PasspointProvider) bestProvider.first).getConfig()) != null)) {
                if (((PasspointProvider) bestProvider.first).isSimCredential() && passpointconfig.getHomeSp().isVendorSpecificSsid() && !this.mPasspointManager.isVendorSimUseable()) {
                    localLog("Passpoint bestProvider has no MCC, MNC of vndor. Skip");
                } else if (this.mWifiConfigManager.getAutoConnectCarrierApEnabled() || !passpointconfig.getHomeSp().isVendorSpecificSsid() || RilUtil.isMptcpEnabled(this.mContext)) {
                    candidateList.add(new PasspointNetworkCandidate((PasspointProvider) bestProvider.first, (PasspointMatch) bestProvider.second, scanDetail));
                } else {
                    localLog("Passpoint bestProvider has a PasspointConfiguration from carrier. autoConnectCarrierAp is disabled. MPTCP is disabled. Skip");
                }
            }
        }
        if (candidateList.isEmpty()) {
            localLog("No suitable Passpoint network found");
            return null;
        }
        PasspointNetworkCandidate bestNetwork = findBestNetwork(candidateList, wifiConfiguration == null ? null : wifiConfiguration.SSID, bluetoothConnected);
        if (bestNetwork == null) {
            localLog("Passpoint's bestNetwork is null");
            return null;
        } else if (wifiConfiguration == null || !TextUtils.equals(wifiConfiguration.SSID, ScanResultUtil.createQuotedSSID(bestNetwork.mScanDetail.getSSID()))) {
            WifiConfiguration config = createWifiConfigForProvider(bestNetwork);
            if (config != null) {
                onConnectableListener2.onConnectable(bestNetwork.mScanDetail, config, 0);
                localLog("Passpoint network to connect to: " + config.SSID);
            }
            return config;
        } else {
            localLog("Staying with current Passpoint network " + wifiConfiguration.SSID);
            this.mWifiConfigManager.setNetworkCandidateScanResult(wifiConfiguration.networkId, bestNetwork.mScanDetail.getScanResult(), 0);
            this.mWifiConfigManager.updateScanDetailForNetwork(wifiConfiguration.networkId, bestNetwork.mScanDetail);
            onConnectableListener2.onConnectable(bestNetwork.mScanDetail, wifiConfiguration, 0);
            return wifiConfiguration;
        }
    }

    public /* synthetic */ boolean lambda$evaluateNetworks$1$PasspointNetworkEvaluator(ScanDetail s) {
        if (!this.mWifiConfigManager.wasEphemeralNetworkDeleted(ScanResultUtil.createQuotedSSID(s.getScanResult().SSID))) {
            return true;
        }
        LocalLog localLog = this.mLocalLog;
        localLog.log("Ignoring disabled the SSID of Passpoint AP: " + WifiNetworkSelector.toScanId(s.getScanResult()));
        return false;
    }

    private void createEphemeralProfileForMatchingAp(List<ScanDetail> filteredScanDetails) {
        PasspointConfiguration carrierConfig;
        TelephonyManager telephonyManager = getTelephonyManager();
        if (telephonyManager != null && TelephonyUtil.getCarrierType(telephonyManager) == 0 && this.mCarrierNetworkConfig.isCarrierEncryptionInfoAvailable()) {
            if (!this.mPasspointManager.hasCarrierProvider(telephonyManager.createForSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId()).getSimOperator())) {
                int eapMethod = this.mPasspointManager.findEapMethodFromNAIRealmMatchedWithCarrier(filteredScanDetails);
                if (Utils.isCarrierEapMethod(eapMethod) && (carrierConfig = this.mPasspointManager.createEphemeralPasspointConfigForCarrier(eapMethod)) != null) {
                    this.mPasspointManager.installEphemeralPasspointConfigForCarrier(carrierConfig);
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:5:0x0029, code lost:
        r4 = r3.getHomeSp().isOAuthEnabled();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private android.net.wifi.WifiConfiguration createWifiConfigForProvider(com.android.server.wifi.hotspot2.PasspointNetworkEvaluator.PasspointNetworkCandidate r17) {
        /*
            r16 = this;
            r0 = r16
            r1 = r17
            com.android.server.wifi.hotspot2.PasspointProvider r2 = r1.mProvider
            android.net.wifi.WifiConfiguration r2 = r2.getWifiConfig()
            com.android.server.wifi.ScanDetail r3 = r1.mScanDetail
            java.lang.String r3 = r3.getSSID()
            java.lang.String r3 = com.android.server.wifi.util.ScanResultUtil.createQuotedSSID(r3)
            r2.SSID = r3
            com.android.server.wifi.hotspot2.PasspointMatch r3 = r1.mMatchStatus
            com.android.server.wifi.hotspot2.PasspointMatch r4 = com.android.server.wifi.hotspot2.PasspointMatch.HomeProvider
            r5 = 1
            if (r3 != r4) goto L_0x001f
            r2.isHomeProviderNetwork = r5
        L_0x001f:
            com.android.server.wifi.hotspot2.PasspointProvider r3 = r1.mProvider
            android.net.wifi.hotspot2.PasspointConfiguration r3 = r3.getConfig()
            r4 = 0
            r6 = 0
            if (r3 == 0) goto L_0x003b
            android.net.wifi.hotspot2.pps.HomeSp r7 = r3.getHomeSp()
            boolean r4 = r7.isOAuthEnabled()
            if (r4 == 0) goto L_0x003b
            android.net.wifi.hotspot2.pps.HomeSp r7 = r3.getHomeSp()
            java.lang.String r6 = r7.getOAuthProvider()
        L_0x003b:
            com.android.server.wifi.WifiConfigManager r7 = r0.mWifiConfigManager
            java.lang.String r8 = r2.configKey()
            android.net.wifi.WifiConfiguration r7 = r7.getConfiguredNetwork((java.lang.String) r8)
            r8 = 1010(0x3f2, float:1.415E-42)
            r9 = 0
            r10 = 0
            if (r7 == 0) goto L_0x00c3
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r11 = r7.getNetworkSelectionStatus()
            java.lang.String r12 = "Current configuration for the Passpoint AP "
            if (r4 == 0) goto L_0x0097
            boolean r13 = r0.hasOAuthProvider(r6)
            if (r13 == 0) goto L_0x006e
            boolean r13 = mIsEnabledNetworkForOpenRoaming
            if (r13 != 0) goto L_0x0097
            boolean r13 = r11.isNetworkEnabled()
            if (r13 != 0) goto L_0x006b
            com.android.server.wifi.WifiConfigManager r13 = r0.mWifiConfigManager
            int r14 = r7.networkId
            r13.enableNetwork(r14, r10, r8)
        L_0x006b:
            mIsEnabledNetworkForOpenRoaming = r5
            goto L_0x0097
        L_0x006e:
            boolean r5 = r11.isNetworkEnabled()
            if (r5 == 0) goto L_0x007b
            com.android.server.wifi.WifiConfigManager r5 = r0.mWifiConfigManager
            int r13 = r7.networkId
            r5.disableNetwork(r13, r8)
        L_0x007b:
            mIsEnabledNetworkForOpenRoaming = r10
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r12)
            java.lang.String r8 = r2.SSID
            r5.append(r8)
            java.lang.String r8 = " does not have OAuth, skip this candidate"
            r5.append(r8)
            java.lang.String r5 = r5.toString()
            r0.localLog(r5)
            return r9
        L_0x0097:
            boolean r5 = r11.isNetworkEnabled()
            if (r5 != 0) goto L_0x00c2
            com.android.server.wifi.WifiConfigManager r5 = r0.mWifiConfigManager
            int r8 = r7.networkId
            boolean r5 = r5.tryEnableNetwork((int) r8)
            if (r5 == 0) goto L_0x00a8
            return r7
        L_0x00a8:
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            r8.append(r12)
            java.lang.String r10 = r2.SSID
            r8.append(r10)
            java.lang.String r10 = " is disabled, skip this candidate"
            r8.append(r10)
            java.lang.String r8 = r8.toString()
            r0.localLog(r8)
            return r9
        L_0x00c2:
            return r7
        L_0x00c3:
            com.android.server.wifi.WifiConfigManager r11 = r0.mWifiConfigManager
            int r11 = r11.increaseAndGetPriority()
            r2.priority = r11
            r11 = 0
            r12 = 0
            if (r3 == 0) goto L_0x01f4
            android.net.wifi.hotspot2.pps.HomeSp r13 = r3.getHomeSp()
            boolean r13 = r13.isAutoReconnectEnabled()
            r2.semAutoReconnect = r13
            android.net.wifi.hotspot2.pps.HomeSp r13 = r3.getHomeSp()
            boolean r13 = r13.isVendorSpecificSsid()
            if (r13 == 0) goto L_0x010f
            android.net.wifi.hotspot2.pps.Credential r13 = r3.getCredential()
            android.net.wifi.hotspot2.pps.Credential$SimCredential r13 = r13.getSimCredential()
            if (r13 == 0) goto L_0x010f
            android.net.wifi.hotspot2.pps.Credential r13 = r3.getCredential()
            android.net.wifi.hotspot2.pps.Credential$SimCredential r13 = r13.getSimCredential()
            java.lang.String r13 = r13.getImsi()
            boolean r14 = android.text.TextUtils.isEmpty(r13)
            if (r14 != 0) goto L_0x010f
            java.lang.String r14 = mImsi
            boolean r14 = android.text.TextUtils.equals(r14, r13)
            if (r14 != 0) goto L_0x010f
            mImsi = r13
            r11 = 1
            java.lang.String r14 = "IMSI of Passpoint config is changed. So network must be enabled"
            r0.localLog(r14)
        L_0x010f:
            java.lang.String r13 = r2.providerFriendlyName
            java.lang.String r14 = "Vendor Hotspot2.0 Profile"
            boolean r13 = r14.equals(r13)
            if (r13 != 0) goto L_0x0123
            java.lang.String r13 = r2.providerFriendlyName
            java.lang.String r14 = "Samsung Hotspot2.0 Profile"
            boolean r13 = r14.equals(r13)
            if (r13 == 0) goto L_0x01b8
        L_0x0123:
            r13 = 0
            com.samsung.android.net.wifi.OpBrandingLoader$Vendor r14 = com.samsung.android.net.wifi.OpBrandingLoader.Vendor.SKT
            com.samsung.android.net.wifi.OpBrandingLoader$Vendor r15 = mOpBranding
            if (r14 != r15) goto L_0x0161
            r14 = 0
            com.android.server.wifi.hotspot2.PasspointManager r15 = r0.mPasspointManager
            com.android.server.wifi.ScanDetail r9 = r1.mScanDetail
            android.net.wifi.ScanResult r9 = r9.getScanResult()
            java.util.Map r9 = r15.getANQPElements(r9)
            if (r9 == 0) goto L_0x0161
            int r14 = r9.size()
            if (r14 <= 0) goto L_0x0161
            com.android.server.wifi.hotspot2.anqp.Constants$ANQPElementType r14 = com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType.HSFriendlyName
            java.lang.Object r14 = r9.get(r14)
            com.android.server.wifi.hotspot2.anqp.HSFriendlyNameElement r14 = (com.android.server.wifi.hotspot2.anqp.HSFriendlyNameElement) r14
            if (r14 == 0) goto L_0x0161
            java.util.List r15 = r14.getNames()
            boolean r15 = r15.isEmpty()
            if (r15 != 0) goto L_0x0161
            java.util.List r15 = r14.getNames()
            java.lang.Object r15 = r15.get(r10)
            com.android.server.wifi.hotspot2.anqp.I18Name r15 = (com.android.server.wifi.hotspot2.anqp.I18Name) r15
            java.lang.String r13 = r15.getText()
        L_0x0161:
            boolean r9 = android.text.TextUtils.isEmpty(r13)
            java.lang.String r14 = " was replaced by "
            java.lang.String r15 = "The Friendlyname of "
            if (r9 != 0) goto L_0x018a
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            r9.append(r15)
            java.lang.String r15 = r2.providerFriendlyName
            r9.append(r15)
            r9.append(r14)
            r9.append(r13)
            java.lang.String r9 = r9.toString()
            r0.localLog(r9)
            r2.providerFriendlyName = r13
            r9 = 1
            r12 = r9
            goto L_0x01b8
        L_0x018a:
            java.lang.String r9 = r2.SSID
            boolean r9 = android.text.TextUtils.isEmpty(r9)
            if (r9 != 0) goto L_0x01b8
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            r9.append(r15)
            java.lang.String r15 = r2.providerFriendlyName
            r9.append(r15)
            r9.append(r14)
            java.lang.String r14 = r2.SSID
            r9.append(r14)
            java.lang.String r9 = r9.toString()
            r0.localLog(r9)
            java.lang.String r9 = r2.SSID
            java.lang.String r9 = com.android.server.wifi.util.StringUtil.removeDoubleQuotes(r9)
            r2.providerFriendlyName = r9
            r9 = 1
            r12 = r9
        L_0x01b8:
            if (r12 == 0) goto L_0x01f4
            r9 = 0
            android.net.wifi.hotspot2.pps.HomeSp r13 = r3.getHomeSp()
            java.lang.String r14 = r2.providerFriendlyName
            r13.setFriendlyName(r14)
            com.android.server.wifi.hotspot2.PasspointManager r13 = r0.mPasspointManager
            com.android.server.wifi.hotspot2.PasspointProvider r14 = r1.mProvider
            int r14 = r14.getCreatorUid()
            com.android.server.wifi.hotspot2.PasspointProvider r15 = r1.mProvider
            java.lang.String r15 = r15.getPackageName()
            boolean r9 = r13.addOrUpdateProvider(r3, r14, r15)
            if (r9 == 0) goto L_0x01f4
            java.lang.StringBuilder r13 = new java.lang.StringBuilder
            r13.<init>()
            java.lang.String r14 = "FriendlyName of Passpoint config updated to "
            r13.append(r14)
            android.net.wifi.hotspot2.pps.HomeSp r14 = r3.getHomeSp()
            java.lang.String r14 = r14.getFriendlyName()
            r13.append(r14)
            java.lang.String r13 = r13.toString()
            r0.localLog(r13)
        L_0x01f4:
            com.android.server.wifi.hotspot2.PasspointMatch r9 = r1.mMatchStatus
            com.android.server.wifi.hotspot2.PasspointMatch r13 = com.android.server.wifi.hotspot2.PasspointMatch.HomeProvider
            if (r9 != r13) goto L_0x01fc
            r2.isHomeProviderNetwork = r5
        L_0x01fc:
            com.android.server.wifi.WifiConfigManager r5 = r0.mWifiConfigManager
            com.android.server.wifi.NetworkUpdateResult r5 = r5.addOrUpdateNetwork(r2, r8)
            boolean r9 = r5.isSuccess()
            if (r9 != 0) goto L_0x020f
            java.lang.String r8 = "Failed to add passpoint network"
            r0.localLog(r8)
            r8 = 0
            return r8
        L_0x020f:
            int r9 = r2.semAutoReconnect
            if (r9 != 0) goto L_0x0232
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            java.lang.String r9 = "AutoReconnect of "
            r8.append(r9)
            java.lang.String r9 = r2.configKey()
            r8.append(r9)
            java.lang.String r9 = " Passpoint network is disabled"
            r8.append(r9)
            java.lang.String r8 = r8.toString()
            r0.localLog(r8)
            r8 = 0
            return r8
        L_0x0232:
            com.android.server.wifi.WifiConfigManager r9 = r0.mWifiConfigManager
            int r13 = r5.getNetworkId()
            android.net.wifi.WifiConfiguration r9 = r9.getConfiguredNetwork((int) r13)
            if (r11 != 0) goto L_0x025b
            if (r9 == 0) goto L_0x025b
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r13 = r9.getNetworkSelectionStatus()
            if (r13 == 0) goto L_0x025b
            boolean r14 = r13.isNetworkEnabled()
            if (r14 != 0) goto L_0x025b
            r14 = 19
            boolean r14 = r13.isDisabledByReason(r14)
            if (r14 == 0) goto L_0x025b
            java.lang.String r8 = "Selected Passpoint network is unstable AP"
            r0.localLog(r8)
            r8 = 0
            return r8
        L_0x025b:
            if (r4 == 0) goto L_0x028f
            com.android.server.wifi.WifiInjector r13 = com.android.server.wifi.WifiInjector.getInstance()
            com.samsung.android.server.wifi.dqa.SemWifiIssueDetector r13 = r13.getIssueDetector()
            if (r13 == 0) goto L_0x0282
            android.os.Bundle r14 = new android.os.Bundle
            r14.<init>()
            android.net.wifi.hotspot2.pps.HomeSp r15 = r3.getHomeSp()
            java.lang.String r15 = r15.getFqdn()
            java.lang.String r8 = "fqdn"
            r14.putString(r8, r15)
            r8 = 106(0x6a, float:1.49E-43)
            android.os.Message r8 = r13.obtainMessage(r10, r8, r10, r14)
            r13.sendMessage(r8)
        L_0x0282:
            boolean r8 = r0.hasOAuthProvider(r6)
            if (r8 != 0) goto L_0x028f
            java.lang.String r8 = "Do not create WifiConfig, the user did not agree to use OpenRoaming."
            r0.localLog(r8)
            r8 = 0
            return r8
        L_0x028f:
            com.android.server.wifi.WifiConfigManager r8 = r0.mWifiConfigManager
            int r13 = r5.getNetworkId()
            r14 = 1010(0x3f2, float:1.415E-42)
            r8.enableNetwork(r13, r10, r14)
            com.android.server.wifi.WifiConfigManager r8 = r0.mWifiConfigManager
            int r13 = r5.getNetworkId()
            com.android.server.wifi.ScanDetail r14 = r1.mScanDetail
            android.net.wifi.ScanResult r14 = r14.getScanResult()
            r8.setNetworkCandidateScanResult(r13, r14, r10)
            com.android.server.wifi.WifiConfigManager r8 = r0.mWifiConfigManager
            int r10 = r5.getNetworkId()
            com.android.server.wifi.ScanDetail r13 = r1.mScanDetail
            r8.updateScanDetailForNetwork(r10, r13)
            com.android.server.wifi.WifiConfigManager r8 = r0.mWifiConfigManager
            int r10 = r5.getNetworkId()
            android.net.wifi.WifiConfiguration r8 = r8.getConfiguredNetwork((int) r10)
            return r8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.hotspot2.PasspointNetworkEvaluator.createWifiConfigForProvider(com.android.server.wifi.hotspot2.PasspointNetworkEvaluator$PasspointNetworkCandidate):android.net.wifi.WifiConfiguration");
    }

    private PasspointNetworkCandidate findBestNetwork(List<PasspointNetworkCandidate> networkList, String currentNetworkSsid, boolean bluetoothConnected) {
        PasspointNetworkCandidate bestCandidate = null;
        int bestScore = Integer.MIN_VALUE;
        StringBuffer scoreHistory = new StringBuffer();
        for (PasspointNetworkCandidate candidate : networkList) {
            ScanDetail scanDetail = candidate.mScanDetail;
            PasspointMatch match = candidate.mMatchStatus;
            localLog("Cnadidate Passpoint network : " + scanDetail.getNetworkDetail());
            boolean isActiveNetwork = TextUtils.equals(currentNetworkSsid, ScanResultUtil.createQuotedSSID(scanDetail.getSSID()));
            int score = PasspointNetworkScore.calculateScore(match == PasspointMatch.HomeProvider, scanDetail, this.mPasspointManager.getANQPElements(scanDetail.getScanResult()), isActiveNetwork, scoreHistory, bluetoothConnected);
            PasspointConfiguration passpointconfig = candidate.mProvider.getConfig();
            if (passpointconfig != null) {
                boolean isOAuthEnabled = passpointconfig.getHomeSp().isOAuthEnabled();
                if (isOAuthEnabled) {
                    String oAuthProvider = passpointconfig.getHomeSp().getOAuthProvider();
                    PasspointConfiguration passpointConfiguration = passpointconfig;
                    if (this.mWifiConfigManager.getConfiguredNetwork(candidate.mProvider.getWifiConfig().configKey()) != null && isOAuthEnabled && !hasOAuthProvider(oAuthProvider)) {
                        localLog("Do not candidate as the Best Passpoint network, the user did not agree to use OpenRoaming.");
                    }
                }
            }
            if (score > bestScore) {
                bestCandidate = candidate;
                bestScore = score;
            }
        }
        String str = currentNetworkSsid;
        if (scoreHistory.length() > 0) {
            localLog("\n" + scoreHistory.toString());
        }
        if (!(bestCandidate == null || bestCandidate.mScanDetail == null)) {
            localLog("Best Passpoint network " + bestCandidate.mScanDetail.getSSID() + " provided by " + bestCandidate.mProvider.getConfig().getHomeSp().getFqdn());
        }
        return bestCandidate;
    }

    private void localLog(String log) {
        this.mLocalLog.log(log);
    }

    private boolean hasOAuthProvider(String oAuthProvider) {
        String values = Settings.Global.getString(this.mContext.getContentResolver(), "sem_wifi_allowed_oauth_provider");
        if (TextUtils.isEmpty(values)) {
            return false;
        }
        if (!values.contains("[" + oAuthProvider + "]")) {
            return false;
        }
        return true;
    }
}
