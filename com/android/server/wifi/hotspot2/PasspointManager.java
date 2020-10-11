package com.android.server.wifi.hotspot2;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiSsid;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.Clock;
import com.android.server.wifi.IMSIParameter;
import com.android.server.wifi.SIMAccessor;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.WifiConfigStore;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiKeyStore;
import com.android.server.wifi.WifiMetrics;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.PasspointConfigSharedStoreData;
import com.android.server.wifi.hotspot2.PasspointConfigUserStoreData;
import com.android.server.wifi.hotspot2.PasspointEventHandler;
import com.android.server.wifi.hotspot2.PasspointManager;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.HSOsuProvidersElement;
import com.android.server.wifi.hotspot2.anqp.NAIRealmElement;
import com.android.server.wifi.hotspot2.anqp.OsuProviderInfo;
import com.android.server.wifi.util.InformationElementUtil;
import com.android.server.wifi.util.TelephonyUtil;
import com.samsung.android.net.wifi.OpBrandingLoader;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PasspointManager {
    private static final String TAG = "PasspointManager";
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    private static PasspointManager sPasspointManager;
    /* access modifiers changed from: private */
    public final AnqpCache mAnqpCache;
    /* access modifiers changed from: private */
    public final ANQPRequestManager mAnqpRequestManager;
    /* access modifiers changed from: private */
    public final AppOpsManager mAppOps;
    private final Map<String, AppOpsChangedListener> mAppOpsChangedListenerPerApp = new HashMap();
    private final CertificateVerifier mCertVerifier;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private boolean mIsPasspointEnabled;
    private boolean mIsVendorSimUseable;
    private final WifiKeyStore mKeyStore;
    private final LocalLog mLocalLog;
    private final PasspointObjectFactory mObjectFactory;
    private final PasspointEventHandler mPasspointEventHandler;
    private final PasspointProvisioner mPasspointProvisioner;
    /* access modifiers changed from: private */
    public long mProviderIndex;
    /* access modifiers changed from: private */
    public final Map<String, PasspointProvider> mProviders;
    private boolean mRequestANQPEnabled = true;
    private final SIMAccessor mSimAccessor;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;
    private boolean mVerboseLoggingEnabled = false;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiInjector mWifiInjector;
    private final WifiMetrics mWifiMetrics;

    private class CallbackHandler implements PasspointEventHandler.Callbacks {
        private final Context mContext;

        CallbackHandler(Context context) {
            this.mContext = context;
        }

        public void onANQPResponse(long bssid, Map<Constants.ANQPElementType, ANQPElement> anqpElements) {
            ANQPNetworkKey anqpKey = PasspointManager.this.mAnqpRequestManager.onRequestCompleted(bssid, anqpElements != null);
            if (anqpElements != null && anqpKey != null) {
                PasspointManager.this.mAnqpCache.addEntry(anqpKey, anqpElements);
            }
        }

        public void onIconResponse(long bssid, String fileName, byte[] data) {
            Intent intent = new Intent("android.net.wifi.action.PASSPOINT_ICON");
            intent.addFlags(67108864);
            intent.putExtra("android.net.wifi.extra.BSSID_LONG", bssid);
            intent.putExtra("android.net.wifi.extra.FILENAME", fileName);
            if (data != null) {
                intent.putExtra("android.net.wifi.extra.ICON", Icon.createWithData(data, 0, data.length));
            }
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.ACCESS_WIFI_STATE");
        }

        public void onWnmFrameReceived(WnmData event) {
            Intent intent;
            if (event.isDeauthEvent()) {
                intent = new Intent("android.net.wifi.action.PASSPOINT_DEAUTH_IMMINENT");
                intent.addFlags(67108864);
                intent.putExtra("android.net.wifi.extra.BSSID_LONG", event.getBssid());
                intent.putExtra("android.net.wifi.extra.URL", event.getUrl());
                intent.putExtra("android.net.wifi.extra.ESS", event.isEss());
                intent.putExtra("android.net.wifi.extra.DELAY", event.getDelay());
            } else {
                intent = new Intent("android.net.wifi.action.PASSPOINT_SUBSCRIPTION_REMEDIATION");
                intent.addFlags(67108864);
                intent.putExtra("android.net.wifi.extra.BSSID_LONG", event.getBssid());
                intent.putExtra("android.net.wifi.extra.SUBSCRIPTION_REMEDIATION_METHOD", event.getMethod());
                intent.putExtra("android.net.wifi.extra.URL", event.getUrl());
            }
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.ACCESS_WIFI_STATE");
        }
    }

    private class UserDataSourceHandler implements PasspointConfigUserStoreData.DataSource {
        private UserDataSourceHandler() {
        }

        public List<PasspointProvider> getProviders() {
            List<PasspointProvider> providers = new ArrayList<>();
            for (Map.Entry<String, PasspointProvider> entry : PasspointManager.this.mProviders.entrySet()) {
                providers.add(entry.getValue());
            }
            return providers;
        }

        public void setProviders(List<PasspointProvider> providers) {
            PasspointManager.this.mProviders.clear();
            for (PasspointProvider provider : providers) {
                PasspointManager.this.mProviders.put(provider.getConfig().getHomeSp().getFqdn(), provider);
                if (provider.getPackageName() != null) {
                    PasspointManager.this.startTrackingAppOpsChange(provider.getPackageName(), provider.getCreatorUid());
                }
            }
        }
    }

    private class SharedDataSourceHandler implements PasspointConfigSharedStoreData.DataSource {
        private SharedDataSourceHandler() {
        }

        public long getProviderIndex() {
            return PasspointManager.this.mProviderIndex;
        }

        public void setProviderIndex(long providerIndex) {
            long unused = PasspointManager.this.mProviderIndex = providerIndex;
        }
    }

    private final class AppOpsChangedListener implements AppOpsManager.OnOpChangedListener {
        private final String mPackageName;
        private final int mUid;

        AppOpsChangedListener(String packageName, int uid) {
            this.mPackageName = packageName;
            this.mUid = uid;
        }

        public void onOpChanged(String op, String packageName) {
            PasspointManager.this.mHandler.post(new Runnable(packageName, op) {
                private final /* synthetic */ String f$1;
                private final /* synthetic */ String f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    PasspointManager.AppOpsChangedListener.this.lambda$onOpChanged$0$PasspointManager$AppOpsChangedListener(this.f$1, this.f$2);
                }
            });
        }

        public /* synthetic */ void lambda$onOpChanged$0$PasspointManager$AppOpsChangedListener(String packageName, String op) {
            if (this.mPackageName.equals(packageName) && "android:change_wifi_state".equals(op)) {
                try {
                    PasspointManager.this.mAppOps.checkPackage(this.mUid, this.mPackageName);
                    if (PasspointManager.this.mAppOps.unsafeCheckOpNoThrow("android:change_wifi_state", this.mUid, this.mPackageName) == 1) {
                        Log.i(PasspointManager.TAG, "User disallowed change wifi state for " + packageName);
                        PasspointManager.this.removePasspointProviderWithPackage(this.mPackageName);
                    }
                } catch (SecurityException e) {
                    Log.wtf(PasspointManager.TAG, "Invalid uid/package" + packageName);
                }
            }
        }
    }

    public void removePasspointProviderWithPackage(String packageName) {
        stopTrackingAppOpsChange(packageName);
        for (Map.Entry<String, PasspointProvider> entry : getPasspointProviderWithPackage(packageName).entrySet()) {
            String fqdn = entry.getValue().getConfig().getHomeSp().getFqdn();
            removeProvider(fqdn);
            disconnectIfPasspointNetwork(fqdn);
        }
    }

    private Map<String, PasspointProvider> getPasspointProviderWithPackage(String packageName) {
        return (Map) this.mProviders.entrySet().stream().filter(new Predicate(packageName) {
            private final /* synthetic */ String f$0;

            {
                this.f$0 = r1;
            }

            public final boolean test(Object obj) {
                return TextUtils.equals(this.f$0, ((PasspointProvider) ((Map.Entry) obj).getValue()).getPackageName());
            }
        }).collect(Collectors.toMap($$Lambda$PasspointManager$pKR4KzimmVxw_e4yzicg7QJrJ1c.INSTANCE, $$Lambda$PasspointManager$WfXcPLClLXFUI2CymehBp9oUwqE.INSTANCE));
    }

    static /* synthetic */ String lambda$getPasspointProviderWithPackage$1(Map.Entry entry) {
        return (String) entry.getKey();
    }

    static /* synthetic */ PasspointProvider lambda$getPasspointProviderWithPackage$2(Map.Entry entry) {
        return (PasspointProvider) entry.getValue();
    }

    /* access modifiers changed from: private */
    public void startTrackingAppOpsChange(String packageName, int uid) {
        if (!this.mAppOpsChangedListenerPerApp.containsKey(packageName)) {
            AppOpsChangedListener appOpsChangedListener = new AppOpsChangedListener(packageName, uid);
            this.mAppOps.startWatchingMode("android:change_wifi_state", packageName, appOpsChangedListener);
            this.mAppOpsChangedListenerPerApp.put(packageName, appOpsChangedListener);
        }
    }

    private void stopTrackingAppOpsChange(String packageName) {
        AppOpsChangedListener appOpsChangedListener = this.mAppOpsChangedListenerPerApp.remove(packageName);
        if (appOpsChangedListener == null) {
            Log.wtf(TAG, "No app ops listener found for " + packageName);
            return;
        }
        this.mAppOps.stopWatchingMode(appOpsChangedListener);
    }

    private void disconnectIfPasspointNetwork(String fqdn) {
        WifiConfiguration currentConfiguration = this.mWifiInjector.getClientModeImpl().getCurrentWifiConfiguration();
        if (currentConfiguration != null && currentConfiguration.isPasspoint() && TextUtils.equals(currentConfiguration.FQDN, fqdn)) {
            Log.i(TAG, "Disconnect current Passpoint network for " + fqdn + "because the profile was removed");
            this.mWifiInjector.getClientModeImpl().disconnectCommand();
        }
    }

    public PasspointManager(Context context, WifiInjector wifiInjector, Handler handler, WifiNative wifiNative, WifiKeyStore keyStore, Clock clock, SIMAccessor simAccessor, PasspointObjectFactory objectFactory, WifiConfigManager wifiConfigManager, WifiConfigStore wifiConfigStore, WifiMetrics wifiMetrics, TelephonyManager telephonyManager, SubscriptionManager subscriptionManager, LocalLog localLog) {
        Context context2 = context;
        WifiNative wifiNative2 = wifiNative;
        Clock clock2 = clock;
        PasspointObjectFactory passpointObjectFactory = objectFactory;
        WifiConfigStore wifiConfigStore2 = wifiConfigStore;
        WifiMetrics wifiMetrics2 = wifiMetrics;
        this.mPasspointEventHandler = passpointObjectFactory.makePasspointEventHandler(wifiNative2, new CallbackHandler(context2));
        this.mWifiInjector = wifiInjector;
        this.mHandler = handler;
        this.mKeyStore = keyStore;
        this.mSimAccessor = simAccessor;
        this.mObjectFactory = passpointObjectFactory;
        this.mProviders = new HashMap();
        this.mAnqpCache = passpointObjectFactory.makeAnqpCache(clock2);
        this.mAnqpRequestManager = passpointObjectFactory.makeANQPRequestManager(this.mPasspointEventHandler, clock2);
        this.mCertVerifier = objectFactory.makeCertificateVerifier();
        this.mWifiConfigManager = wifiConfigManager;
        this.mWifiMetrics = wifiMetrics2;
        this.mProviderIndex = 0;
        this.mTelephonyManager = telephonyManager;
        this.mSubscriptionManager = subscriptionManager;
        wifiConfigStore2.registerStoreData(passpointObjectFactory.makePasspointConfigUserStoreData(this.mKeyStore, this.mSimAccessor, new UserDataSourceHandler()));
        wifiConfigStore2.registerStoreData(passpointObjectFactory.makePasspointConfigSharedStoreData(new SharedDataSourceHandler()));
        this.mPasspointProvisioner = passpointObjectFactory.makePasspointProvisioner(context2, wifiNative2, this, wifiMetrics2);
        this.mAppOps = (AppOpsManager) context2.getSystemService("appops");
        this.mIsPasspointEnabled = false;
        this.mLocalLog = localLog;
        sPasspointManager = this;
    }

    public void initializeProvisioner(Looper looper) {
        this.mPasspointProvisioner.init(looper);
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
        this.mPasspointProvisioner.enableVerboseLogging(verbose);
    }

    public boolean addOrUpdateProvider(PasspointConfiguration config, int uid, String packageName) {
        long existingProviderId;
        long newProviderId;
        int i = uid;
        this.mWifiMetrics.incrementNumPasspointProviderInstallation();
        if (config == null) {
            Log.e(TAG, "Configuration not provided");
            return false;
        } else if (!config.validate()) {
            Log.e(TAG, "Invalid configuration");
            return false;
        } else {
            X509Certificate[] x509Certificates = config.getCredential().getCaCertificates();
            if (config.getUpdateIdentifier() == Integer.MIN_VALUE && x509Certificates != null) {
                try {
                    for (X509Certificate certificate : x509Certificates) {
                        this.mCertVerifier.verifyCaCert(certificate);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to verify CA certificate: " + e.getMessage());
                    return false;
                }
            }
            if (this.mProviders.containsKey(config.getHomeSp().getFqdn())) {
                existingProviderId = this.mProviders.get(config.getHomeSp().getFqdn()).getProviderId();
            } else {
                existingProviderId = 0;
            }
            if (existingProviderId != 0) {
                newProviderId = existingProviderId;
            } else {
                long newProviderId2 = this.mProviderIndex;
                this.mProviderIndex = 1 + newProviderId2;
                newProviderId = newProviderId2;
            }
            PasspointProvider newProvider = this.mObjectFactory.makePasspointProvider(config, this.mKeyStore, this.mSimAccessor, newProviderId, uid, packageName);
            if (!newProvider.installCertsAndKeys()) {
                Log.e(TAG, "Failed to install certificates and keys to keystore");
                return false;
            }
            if (this.mProviders.containsKey(config.getHomeSp().getFqdn())) {
                Log.d(TAG, "Replacing configuration for " + config.getHomeSp().getFqdn());
                if (existingProviderId != 0) {
                    Log.d(TAG, "Skip uninstallCertsAndKeys");
                } else {
                    this.mProviders.get(config.getHomeSp().getFqdn()).uninstallCertsAndKeys();
                }
                this.mProviders.remove(config.getHomeSp().getFqdn());
            }
            this.mProviders.put(config.getHomeSp().getFqdn(), newProvider);
            this.mWifiConfigManager.saveToStore(true);
            if (newProvider.getPackageName() != null) {
                startTrackingAppOpsChange(newProvider.getPackageName(), i);
            }
            Log.d(TAG, "Added/updated Passpoint configuration: " + config.getHomeSp().getFqdn() + " by " + i);
            this.mWifiMetrics.incrementNumPasspointProviderInstallSuccess();
            return true;
        }
    }

    public int findEapMethodFromNAIRealmMatchedWithCarrier(List<ScanDetail> scanDetails) {
        if (!TelephonyUtil.isSimPresent(this.mSubscriptionManager)) {
            return -1;
        }
        if (scanDetails == null) {
            return -1;
        }
        if (scanDetails.isEmpty()) {
            return -1;
        }
        String mccMnc = this.mTelephonyManager.createForSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId()).getSimOperator();
        if (mccMnc == null) {
            return -1;
        }
        if (mccMnc.length() < 5) {
            return -1;
        }
        String domain = Utils.getRealmForMccMnc(mccMnc);
        if (domain == null) {
            return -1;
        }
        for (ScanDetail scanDetail : scanDetails) {
            if (scanDetail.getNetworkDetail().isInterworking()) {
                ScanResult scanResult = scanDetail.getScanResult();
                InformationElementUtil.RoamingConsortium roamingConsortium = InformationElementUtil.getRoamingConsortiumIE(scanResult.informationElements);
                InformationElementUtil.Vsa vsa = InformationElementUtil.getHS2VendorSpecificIE(scanResult.informationElements);
                try {
                    long bssid = Utils.parseMac(scanResult.BSSID);
                    ANQPNetworkKey anqpKey = ANQPNetworkKey.buildKey(scanResult.SSID, bssid, scanResult.hessid, vsa.anqpDomainID);
                    ANQPData anqpEntry = this.mAnqpCache.getEntry(anqpKey);
                    if (anqpEntry == null) {
                        this.mAnqpRequestManager.requestANQPElements(bssid, anqpKey, roamingConsortium.anqpOICount > 0, vsa.hsRelease == NetworkDetail.HSRelease.R2);
                        Log.d(TAG, "ANQP entry not found for: " + anqpKey);
                    } else {
                        int eapMethod = ANQPMatcher.getCarrierEapMethodFromMatchingNAIRealm(domain, (NAIRealmElement) anqpEntry.getElements().get(Constants.ANQPElementType.ANQPNAIRealm));
                        if (eapMethod != -1) {
                            return eapMethod;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid BSSID provided in the scan result: " + scanResult.BSSID);
                }
            }
        }
        return -1;
    }

    public PasspointConfiguration createEphemeralPasspointConfigForCarrier(int eapMethod) {
        String mccMnc = this.mTelephonyManager.createForSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId()).getSimOperator();
        if (mccMnc == null || mccMnc.length() < 5) {
            Log.e(TAG, "invalid length of mccmnc");
            return null;
        } else if (!Utils.isCarrierEapMethod(eapMethod)) {
            Log.e(TAG, "invalid eapMethod type");
            return null;
        } else {
            String domain = Utils.getRealmForMccMnc(mccMnc);
            if (domain == null) {
                Log.e(TAG, "can't make a home domain name using " + mccMnc);
                return null;
            }
            PasspointConfiguration config = new PasspointConfiguration();
            HomeSp homeSp = new HomeSp();
            homeSp.setFqdn(domain);
            homeSp.setFriendlyName(this.mTelephonyManager.createForSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId()).getSimOperatorName());
            config.setHomeSp(homeSp);
            Credential credential = new Credential();
            credential.setRealm(domain);
            Credential.SimCredential simCredential = new Credential.SimCredential();
            simCredential.setImsi(mccMnc + "*");
            simCredential.setEapType(eapMethod);
            credential.setSimCredential(simCredential);
            config.setCredential(credential);
            if (config.validate()) {
                return config;
            }
            Log.e(TAG, "Transient PasspointConfiguration is not a valid format: " + config);
            return null;
        }
    }

    public boolean hasCarrierProvider(String mccmnc) {
        String domain = Utils.getRealmForMccMnc(mccmnc);
        if (domain == null) {
            Log.e(TAG, "can't make a home domain name using " + mccmnc);
            return false;
        }
        for (Map.Entry<String, PasspointProvider> provider : this.mProviders.entrySet()) {
            if (provider.getValue().getConfig().getCredential().getSimCredential() != null) {
                if (domain.equals(provider.getKey())) {
                    return true;
                }
                IMSIParameter imsiParameter = provider.getValue().getImsiParameter();
                if (imsiParameter != null && imsiParameter.matchesMccMnc(mccmnc)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean installEphemeralPasspointConfigForCarrier(PasspointConfiguration config) {
        if (config == null) {
            Log.e(TAG, "PasspointConfiguration for carrier is null");
            return false;
        } else if (!TelephonyUtil.isSimPresent(this.mSubscriptionManager)) {
            Log.e(TAG, "Sim is not presented on the device");
            return false;
        } else {
            Credential.SimCredential simCredential = config.getCredential().getSimCredential();
            if (simCredential == null || simCredential.getImsi() == null) {
                Log.e(TAG, "This is not for a carrier configuration using EAP-SIM/AKA/AKA'");
                return false;
            } else if (!config.validate()) {
                Log.e(TAG, "It is not a valid format for Passpoint Configuration with EAP-SIM/AKA/AKA'");
                return false;
            } else {
                String imsi = simCredential.getImsi();
                if (imsi.length() < 6) {
                    Log.e(TAG, "Invalid IMSI length: " + imsi.length());
                    return false;
                }
                int index = imsi.indexOf("*");
                if (index == -1) {
                    Log.e(TAG, "missing * in imsi");
                    return false;
                } else if (hasCarrierProvider(imsi.substring(0, index))) {
                    Log.e(TAG, "It is already in the Provider list");
                    return false;
                } else {
                    PasspointObjectFactory passpointObjectFactory = this.mObjectFactory;
                    WifiKeyStore wifiKeyStore = this.mKeyStore;
                    SIMAccessor sIMAccessor = this.mSimAccessor;
                    long j = this.mProviderIndex;
                    this.mProviderIndex = 1 + j;
                    PasspointProvider newProvider = passpointObjectFactory.makePasspointProvider(config, wifiKeyStore, sIMAccessor, j, 1010, (String) null);
                    newProvider.setEphemeral(true);
                    Log.d(TAG, "installed PasspointConfiguration for carrier : " + config.getHomeSp().getFriendlyName());
                    this.mProviders.put(config.getHomeSp().getFqdn(), newProvider);
                    this.mWifiConfigManager.saveToStore(true);
                    return true;
                }
            }
        }
    }

    public boolean removeProvider(String fqdn) {
        this.mWifiMetrics.incrementNumPasspointProviderUninstallation();
        if (!this.mProviders.containsKey(fqdn)) {
            Log.e(TAG, "Config doesn't exist");
            return false;
        }
        this.mProviders.get(fqdn).uninstallCertsAndKeys();
        String packageName = this.mProviders.get(fqdn).getPackageName();
        this.mProviders.remove(fqdn);
        this.mWifiConfigManager.saveToStore(true);
        if (this.mAppOpsChangedListenerPerApp.containsKey(packageName) && getPasspointProviderWithPackage(packageName).size() == 0) {
            stopTrackingAppOpsChange(packageName);
        }
        Log.d(TAG, "Removed Passpoint configuration: " + fqdn);
        this.mWifiMetrics.incrementNumPasspointProviderUninstallSuccess();
        return true;
    }

    public static void clearInternalData() {
        PasspointManager passpointManager = sPasspointManager;
        if (passpointManager == null) {
            Log.e(TAG, "PasspointManager have not been initialized yet");
        } else {
            passpointManager.clearProviders();
        }
    }

    private void clearProviders() {
        this.mProviders.clear();
        this.mProviderIndex = 0;
    }

    public void removeEphemeralProviders() {
        this.mProviders.entrySet().removeIf(new Predicate() {
            public final boolean test(Object obj) {
                return PasspointManager.this.lambda$removeEphemeralProviders$3$PasspointManager((Map.Entry) obj);
            }
        });
    }

    public /* synthetic */ boolean lambda$removeEphemeralProviders$3$PasspointManager(Map.Entry entry) {
        PasspointProvider provider = (PasspointProvider) entry.getValue();
        if (provider == null || !provider.isEphemeral()) {
            return false;
        }
        this.mWifiConfigManager.removePasspointConfiguredNetwork((String) entry.getKey());
        return true;
    }

    public List<PasspointConfiguration> getProviderConfigs() {
        List<PasspointConfiguration> configs = new ArrayList<>();
        for (Map.Entry<String, PasspointProvider> entry : this.mProviders.entrySet()) {
            configs.add(entry.getValue().getConfig());
        }
        return configs;
    }

    public Pair<PasspointProvider, PasspointMatch> matchProvider(ScanResult scanResult) {
        String str;
        List<Pair<PasspointProvider, PasspointMatch>> allMatches = getAllMatchedProviders(scanResult);
        if (allMatches == null) {
            return null;
        }
        Pair<PasspointProvider, PasspointMatch> bestMatch = null;
        long matchProviderId = 0;
        Iterator<Pair<PasspointProvider, PasspointMatch>> it = allMatches.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Pair<PasspointProvider, PasspointMatch> match = it.next();
            if (!((PasspointProvider) match.first).getConfig().getHomeSp().isAutoReconnectEnabled()) {
                localLog("Fqdn :" + ((PasspointProvider) match.first).getConfig().getHomeSp().getFqdn() + " AutoReconnect is false, skip ");
            } else if (match.second == PasspointMatch.HomeProvider) {
                bestMatch = match;
                break;
            } else if (match.second == PasspointMatch.RoamingProvider && matchProviderId <= ((PasspointProvider) match.first).getProviderId()) {
                bestMatch = match;
                matchProviderId = ((PasspointProvider) match.first).getProviderId();
            }
        }
        if (bestMatch != null) {
            Object[] objArr = new Object[3];
            objArr[0] = scanResult.SSID;
            objArr[1] = ((PasspointProvider) bestMatch.first).getConfig().getHomeSp().getFqdn();
            if (bestMatch.second == PasspointMatch.HomeProvider) {
                str = "Home Provider";
            } else {
                str = "Roaming Provider";
            }
            objArr[2] = str;
            Log.d(TAG, String.format("matchProvider : Matched %s to %s as %s", objArr));
        } else if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "No service provider found for " + scanResult.SSID);
        }
        return bestMatch;
    }

    public List<Pair<PasspointProvider, PasspointMatch>> getAllMatchedProviders(ScanResult scanResult) {
        String str;
        HomeSp homesp;
        ScanResult scanResult2 = scanResult;
        List<Pair<PasspointProvider, PasspointMatch>> allMatches = new ArrayList<>();
        InformationElementUtil.RoamingConsortium roamingConsortium = InformationElementUtil.getRoamingConsortiumIE(scanResult2.informationElements);
        InformationElementUtil.Vsa vsa = InformationElementUtil.getHS2VendorSpecificIE(scanResult2.informationElements);
        try {
            long bssid = Utils.parseMac(scanResult2.BSSID);
            ANQPNetworkKey anqpKey = ANQPNetworkKey.buildKey(scanResult2.SSID, bssid, scanResult2.hessid, vsa.anqpDomainID);
            ANQPData anqpEntry = this.mAnqpCache.getEntry(anqpKey);
            if (anqpEntry == null) {
                if (this.mRequestANQPEnabled) {
                    this.mAnqpRequestManager.requestANQPElements(bssid, anqpKey, roamingConsortium.anqpOICount > 0, true);
                    Log.d(TAG, "ANQP entry not found for: " + anqpKey);
                } else {
                    Log.d(TAG, "ANQP entry not found for: " + anqpKey + ", but mRequestANQPEnabled is false. Not allowed to send ANQP request.");
                }
                return allMatches;
            }
            for (Map.Entry<String, PasspointProvider> entry : this.mProviders.entrySet()) {
                PasspointProvider provider = entry.getValue();
                if (!(provider == null || (homesp = provider.getConfig().getHomeSp()) == null || TextUtils.isEmpty(homesp.getFqdn()))) {
                    PasspointMatch matchStatus = provider.match(anqpEntry.getElements(), roamingConsortium);
                    if (matchStatus == PasspointMatch.HomeProvider || matchStatus == PasspointMatch.RoamingProvider) {
                        allMatches.add(Pair.create(provider, matchStatus));
                    }
                }
            }
            if (allMatches.size() != 0) {
                for (Pair<PasspointProvider, PasspointMatch> match : allMatches) {
                    Object[] objArr = new Object[3];
                    objArr[0] = scanResult2.SSID;
                    objArr[1] = ((PasspointProvider) match.first).getConfig().getHomeSp().getFqdn();
                    if (match.second == PasspointMatch.HomeProvider) {
                        str = "Home Provider";
                    } else {
                        str = "Roaming Provider";
                    }
                    objArr[2] = str;
                    Log.d(TAG, String.format("getAllMatchedProviders : Matched %s to %s as %s", objArr));
                }
            } else if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "No service providers found for " + scanResult2.SSID);
            }
            return allMatches;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid BSSID provided in the scan result: " + scanResult2.BSSID);
            return allMatches;
        }
    }

    public static boolean addLegacyPasspointConfig(WifiConfiguration config) {
        if (sPasspointManager == null) {
            Log.e(TAG, "PasspointManager have not been initialized yet");
            return false;
        }
        Log.d(TAG, "Installing legacy Passpoint configuration: " + config.FQDN);
        return sPasspointManager.addWifiConfig(config);
    }

    public void sweepCache() {
        this.mAnqpCache.sweep();
    }

    public void notifyANQPDone(AnqpEvent anqpEvent) {
        this.mPasspointEventHandler.notifyANQPDone(anqpEvent);
    }

    public void notifyIconDone(IconEvent iconEvent) {
        this.mPasspointEventHandler.notifyIconDone(iconEvent);
    }

    public void receivedWnmFrame(WnmData data) {
        this.mPasspointEventHandler.notifyWnmFrameReceived(data);
    }

    public boolean queryPasspointIcon(long bssid, String fileName) {
        return this.mPasspointEventHandler.requestIcon(bssid, fileName);
    }

    public Map<Constants.ANQPElementType, ANQPElement> getANQPElements(ScanResult scanResult) {
        InformationElementUtil.Vsa vsa = InformationElementUtil.getHS2VendorSpecificIE(scanResult.informationElements);
        try {
            ANQPData anqpEntry = this.mAnqpCache.getEntry(ANQPNetworkKey.buildKey(scanResult.SSID, Utils.parseMac(scanResult.BSSID), scanResult.hessid, vsa.anqpDomainID));
            if (anqpEntry != null) {
                return anqpEntry.getElements();
            }
            return new HashMap();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid BSSID provided in the scan result: " + scanResult.BSSID);
            return new HashMap();
        }
    }

    public Map<String, Map<Integer, List<ScanResult>>> getAllMatchingFqdnsForScanResults(List<ScanResult> scanResults) {
        if (scanResults == null) {
            Log.e(TAG, "Attempt to get matching config for a null ScanResults");
            return new HashMap();
        } else if (!this.mIsPasspointEnabled) {
            Log.e(TAG, "Attempt to get matching config when Passpoint is disabled");
            return new HashMap();
        } else if (this.mProviders.size() == 0) {
            Log.e(TAG, "Attempt to get matching config when Providers size is zero");
            return new HashMap();
        } else {
            Map<String, Map<Integer, List<ScanResult>>> configs = new HashMap<>();
            for (ScanResult scanResult : scanResults) {
                if (scanResult.isPasspointNetwork()) {
                    for (Pair<PasspointProvider, PasspointMatch> matchedProvider : getAllMatchedProviders(scanResult)) {
                        WifiConfiguration config = ((PasspointProvider) matchedProvider.first).getWifiConfig();
                        int type = 0;
                        if (!config.isHomeProviderNetwork) {
                            type = 1;
                        }
                        Map<Integer, List<ScanResult>> scanResultsPerNetworkType = configs.get(config.FQDN);
                        if (scanResultsPerNetworkType == null) {
                            scanResultsPerNetworkType = new HashMap<>();
                            configs.put(config.FQDN, scanResultsPerNetworkType);
                        }
                        List<ScanResult> matchingScanResults = scanResultsPerNetworkType.get(Integer.valueOf(type));
                        if (matchingScanResults == null) {
                            matchingScanResults = new ArrayList<>();
                            scanResultsPerNetworkType.put(Integer.valueOf(type), matchingScanResults);
                        }
                        matchingScanResults.add(scanResult);
                    }
                }
            }
            return configs;
        }
    }

    public Map<OsuProvider, List<ScanResult>> getMatchingOsuProviders(List<ScanResult> scanResults) {
        if (scanResults == null) {
            Log.e(TAG, "Attempt to retrieve OSU providers for a null ScanResult");
            return new HashMap();
        }
        Map<OsuProvider, List<ScanResult>> osuProviders = new HashMap<>();
        for (ScanResult scanResult : scanResults) {
            if (scanResult.isPasspointNetwork()) {
                Map<Constants.ANQPElementType, ANQPElement> anqpElements = getANQPElements(scanResult);
                if (anqpElements.containsKey(Constants.ANQPElementType.HSOSUProviders)) {
                    for (OsuProviderInfo info : ((HSOsuProvidersElement) anqpElements.get(Constants.ANQPElementType.HSOSUProviders)).getProviders()) {
                        OsuProvider provider = new OsuProvider((WifiSsid) null, info.getFriendlyNames(), info.getServiceDescription(), info.getServerUri(), info.getNetworkAccessIdentifier(), info.getMethodList(), (Icon) null);
                        List<ScanResult> matchingScanResults = osuProviders.get(provider);
                        if (matchingScanResults == null) {
                            matchingScanResults = new ArrayList<>();
                            osuProviders.put(provider, matchingScanResults);
                        }
                        matchingScanResults.add(scanResult);
                    }
                }
            }
        }
        return osuProviders;
    }

    public Map<OsuProvider, PasspointConfiguration> getMatchingPasspointConfigsForOsuProviders(List<OsuProvider> osuProviders) {
        String osuFriendlyName;
        Map<OsuProvider, PasspointConfiguration> matchingPasspointConfigs = new HashMap<>();
        List<PasspointConfiguration> passpointConfigurations = getProviderConfigs();
        for (OsuProvider osuProvider : osuProviders) {
            Map<String, String> friendlyNamesForOsuProvider = osuProvider.getFriendlyNameList();
            if (friendlyNamesForOsuProvider != null) {
                for (PasspointConfiguration passpointConfiguration : passpointConfigurations) {
                    Map<String, String> serviceFriendlyNamesForPpsMo = passpointConfiguration.getServiceFriendlyNames();
                    if (serviceFriendlyNamesForPpsMo != null) {
                        Iterator<Map.Entry<String, String>> it = serviceFriendlyNamesForPpsMo.entrySet().iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            Map.Entry<String, String> entry = it.next();
                            String lang = entry.getKey();
                            String friendlyName = entry.getValue();
                            if (friendlyName != null && (osuFriendlyName = friendlyNamesForOsuProvider.get(lang)) != null && friendlyName.equals(osuFriendlyName)) {
                                matchingPasspointConfigs.put(osuProvider, passpointConfiguration);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return matchingPasspointConfigs;
    }

    public List<WifiConfiguration> getWifiConfigsForPasspointProfiles(List<String> fqdnList) {
        Set<String> fqdnSet = new HashSet<>();
        fqdnSet.addAll(fqdnList);
        List<WifiConfiguration> configs = new ArrayList<>();
        for (String fqdn : fqdnSet) {
            PasspointProvider provider = this.mProviders.get(fqdn);
            if (provider != null) {
                configs.add(provider.getWifiConfig());
            }
        }
        return configs;
    }

    public void onPasspointNetworkConnected(String fqdn) {
        PasspointProvider provider = this.mProviders.get(fqdn);
        if (provider == null) {
            Log.e(TAG, "Passpoint network connected without provider: " + fqdn);
        } else if (!provider.getHasEverConnected()) {
            provider.setHasEverConnected(true);
        }
    }

    public void updateMetrics() {
        int numProviders = this.mProviders.size();
        int numConnectedProviders = 0;
        for (Map.Entry<String, PasspointProvider> entry : this.mProviders.entrySet()) {
            if (entry.getValue().getHasEverConnected()) {
                numConnectedProviders++;
            }
        }
        this.mWifiMetrics.updateSavedPasspointProfilesInfo(this.mProviders);
        this.mWifiMetrics.updateSavedPasspointProfiles(numProviders, numConnectedProviders);
    }

    public void dump(PrintWriter pw) {
        pw.println("Dump of PasspointManager");
        pw.println("mRequestANQPEnabled " + this.mRequestANQPEnabled);
        pw.println("mIsPasspointEnabled " + this.mIsPasspointEnabled);
        pw.println("PasspointManager - Providers Begin ---");
        for (Map.Entry<String, PasspointProvider> entry : this.mProviders.entrySet()) {
            pw.println(entry.getValue());
        }
        pw.println("PasspointManager - Providers End ---");
        pw.println("PasspointManager - Next provider ID to be assigned " + this.mProviderIndex);
        this.mAnqpCache.dump(pw);
        this.mAnqpRequestManager.dump(pw);
    }

    private boolean addWifiConfig(WifiConfiguration wifiConfig) {
        PasspointConfiguration passpointConfig;
        WifiConfiguration wifiConfiguration = wifiConfig;
        if (wifiConfiguration == null || (passpointConfig = PasspointProvider.convertFromWifiConfig(wifiConfig)) == null) {
            return false;
        }
        WifiEnterpriseConfig enterpriseConfig = wifiConfiguration.enterpriseConfig;
        String caCertificateAliasSuffix = enterpriseConfig.getCaCertificateAlias();
        String clientCertAndKeyAliasSuffix = enterpriseConfig.getClientCertificateAlias();
        if (passpointConfig.getCredential().getUserCredential() == null || !TextUtils.isEmpty(caCertificateAliasSuffix)) {
            if (passpointConfig.getCredential().getCertCredential() != null) {
                if (TextUtils.isEmpty(caCertificateAliasSuffix)) {
                    Log.e(TAG, "Missing CA certificate for Certificate credential");
                    return false;
                } else if (TextUtils.isEmpty(clientCertAndKeyAliasSuffix)) {
                    Log.e(TAG, "Missing client certificate and key for certificate credential");
                    return false;
                }
            }
            WifiKeyStore wifiKeyStore = this.mKeyStore;
            SIMAccessor sIMAccessor = this.mSimAccessor;
            long j = this.mProviderIndex;
            this.mProviderIndex = 1 + j;
            PasspointConfiguration passpointConfiguration = passpointConfig;
            WifiEnterpriseConfig wifiEnterpriseConfig = enterpriseConfig;
            this.mProviders.put(passpointConfig.getHomeSp().getFqdn(), new PasspointProvider(passpointConfiguration, wifiKeyStore, sIMAccessor, j, wifiConfiguration.creatorUid, (String) null, Arrays.asList(new String[]{enterpriseConfig.getCaCertificateAlias()}), enterpriseConfig.getClientCertificateAlias(), enterpriseConfig.getClientCertificateAlias(), (String) null, false, false));
            return true;
        }
        Log.e(TAG, "Missing CA Certificate for user credential");
        return false;
    }

    public boolean startSubscriptionProvisioning(int callingUid, OsuProvider provider, IProvisioningCallback callback) {
        return this.mPasspointProvisioner.startSubscriptionProvisioning(callingUid, provider, callback);
    }

    public void setRequestANQPEnabled(boolean enable) {
        this.mRequestANQPEnabled = enable;
    }

    public boolean isPasspointEnabled() {
        return this.mIsPasspointEnabled;
    }

    public void setPasspointEnabled(boolean enabled) {
        this.mIsPasspointEnabled = enabled;
        if (!enabled) {
            if (OpBrandingLoader.Vendor.ATT == mOpBranding) {
                localLog("Passpoint, skip to clear AnqpCache");
            } else {
                this.mAnqpRequestManager.clearANQPRequests();
                this.mAnqpCache.clearAnqpCache();
            }
        }
        localLog("Passpoint, setHotspot20Enabled: " + enabled);
    }

    public boolean isVendorSimUseable() {
        return this.mIsVendorSimUseable;
    }

    public void setVendorSimUseable(boolean enabled) {
        this.mIsVendorSimUseable = enabled;
        localLog("Passpoint, setVendorUsimState : " + enabled);
    }

    public void forceRequestAnqp(ScanResult scanResult) {
        if (!this.mRequestANQPEnabled) {
            Log.e(TAG, "forceRequestAnqp, ANQP request was ignored for concurrent mode ");
            return;
        }
        InformationElementUtil.RoamingConsortium roamingConsortium = InformationElementUtil.getRoamingConsortiumIE(scanResult.informationElements);
        InformationElementUtil.Vsa vsa = InformationElementUtil.getHS2VendorSpecificIE(scanResult.informationElements);
        long bssid = Utils.parseMac(scanResult.BSSID);
        if (!this.mAnqpRequestManager.requestANQPElements(bssid, ANQPNetworkKey.buildKey(scanResult.SSID, bssid, scanResult.hessid, vsa.anqpDomainID), roamingConsortium.anqpOICount > 0, true)) {
            Log.e(TAG, "forceRequestAnqp, ANQP request was fail ");
        }
    }

    public static void requestANQPElements(ScanResult scanResult) {
        PasspointManager passpointManager = sPasspointManager;
        if (passpointManager == null) {
            Log.e(TAG, "requestANQPElements, have not been initialized yet");
        } else {
            passpointManager.forceRequestAnqp(scanResult);
        }
    }

    public int getSavedProvidersSize() {
        return this.mProviders.size();
    }

    private void localLog(String log) {
        LocalLog localLog = this.mLocalLog;
        if (localLog != null) {
            localLog.log(log);
        }
    }
}
