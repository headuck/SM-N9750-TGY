package com.android.server.wifi;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.SsidSetStoreData;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.android.server.wifi.util.ScanResultUtil;
import com.samsung.android.feature.SemCscFeature;
import com.sec.android.app.CscFeatureTagWifi;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

public class AvailableNetworkNotifier {
    private static final String CONFIG_OP_BRANDING = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDING);
    @VisibleForTesting
    static final int DEFAULT_REPEAT_DELAY_SEC = 60;
    private static final int STATE_CONNECTED_NOTIFICATION = 3;
    private static final int STATE_CONNECTING_IN_NOTIFICATION = 2;
    private static final int STATE_CONNECT_FAILED_NOTIFICATION = 4;
    private static final int STATE_NO_NOTIFICATION = 0;
    private static final int STATE_SHOWING_RECOMMENDATION_NOTIFICATION = 1;
    public static final boolean SUPPORT_NOTIFICATION_MENU = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_SUPPORTNOTIFICATIONMENU);
    private static final int TIME_TO_SHOW_CONNECTED_MILLIS = 5000;
    private static final int TIME_TO_SHOW_CONNECTING_MILLIS = 10000;
    private static final int TIME_TO_SHOW_FAILED_MILLIS = 5000;
    private static final String VendorNotificationStyle = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGWIFINOTIFICATIONSTYLE);
    private static final boolean WIFI_SCREENCONNECTEDINFO = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISPLAYSSIDSTATUSBARINFO);
    public static final int WIFI_STATE_CONNECTED = 1;
    public static final int WIFI_STATE_DISCONNECTED = 2;
    /* access modifiers changed from: private */
    public final Set<String> mBlacklistedSsids = new ArraySet();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (AvailableNetworkNotifier.this.mTag.equals(intent.getExtra(ConnectToNetworkNotificationBuilder.AVAILABLE_NETWORK_NOTIFIER_TAG))) {
                String action = intent.getAction();
                char c = 65535;
                switch (action.hashCode()) {
                    case -1692061185:
                        if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_CONNECT_TO_NETWORK)) {
                            c = 1;
                            break;
                        }
                        break;
                    case -1140661470:
                        if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK)) {
                            c = 2;
                            break;
                        }
                        break;
                    case 303648504:
                        if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE)) {
                            c = 3;
                            break;
                        }
                        break;
                    case 1260970165:
                        if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_USER_DISMISSED_NOTIFICATION)) {
                            c = 0;
                            break;
                        }
                        break;
                }
                if (c == 0) {
                    AvailableNetworkNotifier.this.handleUserDismissedAction();
                } else if (c == 1) {
                    AvailableNetworkNotifier.this.handleConnectToNetworkAction();
                } else if (c == 2) {
                    AvailableNetworkNotifier.this.handleSeeAllNetworksAction();
                } else if (c != 3) {
                    Log.e(AvailableNetworkNotifier.this.mTag, "Unknown action " + intent.getAction());
                } else {
                    AvailableNetworkNotifier.this.handlePickWifiNetworkAfterConnectFailure();
                }
            }
        }
    };
    private final ClientModeImpl mClientModeImpl;
    private final Clock mClock;
    private final WifiConfigManager mConfigManager;
    private final Handler.Callback mConnectionStateCallback = new Handler.Callback() {
        public final boolean handleMessage(Message message) {
            return AvailableNetworkNotifier.this.lambda$new$0$AvailableNetworkNotifier(message);
        }
    };
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public final FrameworkFacade mFrameworkFacade;
    private final Handler mHandler;
    private final int mNominatorId;
    private final ConnectToNetworkNotificationBuilder mNotificationBuilder;
    private final long mNotificationRepeatDelay;
    private long mNotificationRepeatTime;
    private ScanResult mRecommendedNetwork;
    private boolean mScreenOn;
    /* access modifiers changed from: private */
    public boolean mSettingEnabled;
    private final Messenger mSrcMessenger;
    private int mState = 0;
    private final String mStoreDataIdentifier;
    private final int mSystemMessageNotificationId;
    /* access modifiers changed from: private */
    public final String mTag;
    /* access modifiers changed from: private */
    public final String mToggleSettingsName;
    /* access modifiers changed from: private */
    public final WifiMetrics mWifiMetrics;

    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
    }

    public AvailableNetworkNotifier(String tag, String storeDataIdentifier, String toggleSettingsName, int notificationIdentifier, int nominatorId, Context context, Looper looper, FrameworkFacade framework, Clock clock, WifiMetrics wifiMetrics, WifiConfigManager wifiConfigManager, WifiConfigStore wifiConfigStore, ClientModeImpl clientModeImpl, ConnectToNetworkNotificationBuilder connectToNetworkNotificationBuilder) {
        Context context2 = context;
        Looper looper2 = looper;
        this.mTag = tag;
        this.mStoreDataIdentifier = storeDataIdentifier;
        this.mToggleSettingsName = toggleSettingsName;
        this.mSystemMessageNotificationId = notificationIdentifier;
        this.mNominatorId = nominatorId;
        this.mContext = context2;
        this.mHandler = new Handler(looper2);
        this.mFrameworkFacade = framework;
        this.mWifiMetrics = wifiMetrics;
        this.mClock = clock;
        this.mConfigManager = wifiConfigManager;
        this.mClientModeImpl = clientModeImpl;
        this.mNotificationBuilder = connectToNetworkNotificationBuilder;
        this.mScreenOn = false;
        this.mSrcMessenger = new Messenger(new Handler(looper2, this.mConnectionStateCallback));
        wifiConfigStore.registerStoreData(new SsidSetStoreData(this.mStoreDataIdentifier, new AvailableNetworkNotifierStoreData()));
        this.mNotificationRepeatDelay = ((long) this.mFrameworkFacade.getIntegerSetting(context2, "wifi_networks_available_repeat_delay", 60)) * 1000;
        NotificationEnabledSettingObserver settingObserver = new NotificationEnabledSettingObserver(this.mHandler);
        settingObserver.register();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_USER_DISMISSED_NOTIFICATION);
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_CONNECT_TO_NETWORK);
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK);
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE);
        NotificationEnabledSettingObserver notificationEnabledSettingObserver = settingObserver;
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter, (String) null, this.mHandler);
    }

    public /* synthetic */ boolean lambda$new$0$AvailableNetworkNotifier(Message msg) {
        switch (msg.what) {
            case 151554:
                handleConnectionAttemptFailedToSend();
                return true;
            case 151555:
                return true;
            default:
                Log.e("AvailableNetworkNotifier", "Unknown message " + msg.what);
                return true;
        }
    }

    public void clearPendingNotification(boolean resetRepeatTime) {
        if (resetRepeatTime) {
            this.mNotificationRepeatTime = 0;
        }
        if (this.mState != 0) {
            getNotificationManager().cancel(this.mSystemMessageNotificationId);
            if (this.mRecommendedNetwork != null) {
                String str = this.mTag;
                Log.d(str, "Notification with state=" + this.mState + " was cleared for recommended network: \"" + this.mRecommendedNetwork.SSID + "\"");
            }
            this.mState = 0;
            this.mRecommendedNetwork = null;
        }
    }

    private boolean isControllerEnabled() {
        return this.mSettingEnabled && !UserManager.get(this.mContext).hasUserRestriction("no_config_wifi", UserHandle.CURRENT);
    }

    public void handleScanResults(List<ScanDetail> availableNetworks) {
        if (!isControllerEnabled()) {
            clearPendingNotification(true);
        } else if (availableNetworks.isEmpty() && this.mState == 1) {
            clearPendingNotification(false);
        } else if (this.mState == 0 && this.mClock.getWallClockMillis() < this.mNotificationRepeatTime) {
        } else {
            if (this.mState != 0 || this.mScreenOn) {
                int i = this.mState;
                if (i != 0 && i != 1) {
                    return;
                }
                if ("WifiNotificationController".equals(this.mTag)) {
                    int numOpenNetworks = availableNetworks.size();
                    if (numOpenNetworks > 0) {
                        postInitialNotification(numOpenNetworks);
                    } else {
                        clearPendingNotification(false);
                    }
                } else {
                    ScanResult recommendation = recommendNetwork(availableNetworks);
                    if (recommendation != null) {
                        postInitialNotification(recommendation);
                    } else {
                        clearPendingNotification(false);
                    }
                }
            }
        }
    }

    public void handlePasspointConnected(int state, WifiInfo wifiInfo) {
        WifiConfiguration wifiConfiguration;
        if ("SKT".equals(CONFIG_OP_BRANDING)) {
            getNotificationManager().cancel("HOTSPOT20_CONNECTION_NOTIFICATION", 1);
            if (state != 1 || (wifiConfiguration = this.mClientModeImpl.getCurrentWifiConfiguration()) == null || !wifiConfiguration.isPasspoint()) {
                return;
            }
            if (wifiConfiguration.isHomeProviderNetwork) {
                Log.i(this.mTag, "handlePasspointConnected, Passpoint is HomeProvider network.");
            } else {
                getNotificationManager().notify("HOTSPOT20_CONNECTION_NOTIFICATION", 1, this.mNotificationBuilder.createHotspot20ConnectedNotification(this.mTag, wifiConfiguration, wifiInfo.getVenueName()));
            }
        }
    }

    public ScanResult recommendNetwork(List<ScanDetail> networks) {
        ScanResult result = null;
        int highestRssi = Integer.MIN_VALUE;
        for (ScanDetail scanDetail : networks) {
            ScanResult scanResult = scanDetail.getScanResult();
            if (scanResult.level > highestRssi) {
                result = scanResult;
                highestRssi = scanResult.level;
            }
        }
        if (result == null || !this.mBlacklistedSsids.contains(result.SSID)) {
            return result;
        }
        return null;
    }

    public void handleScreenStateChanged(boolean screenOn) {
        this.mScreenOn = screenOn;
    }

    public void handleWifiConnected(String ssid) {
        removeNetworkFromBlacklist(ssid);
        if (this.mState != 2) {
            clearPendingNotification(true);
            return;
        }
        ScanResult scanResult = this.mRecommendedNetwork;
        if (scanResult != null) {
            postNotification(this.mNotificationBuilder.createNetworkConnectedNotification(this.mTag, scanResult));
            String str = this.mTag;
            Log.d(str, "User connected to recommended network: \"" + this.mRecommendedNetwork.SSID + "\"");
        }
        this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 3);
        this.mState = 3;
        this.mHandler.postDelayed(new Runnable() {
            public final void run() {
                AvailableNetworkNotifier.this.lambda$handleWifiConnected$1$AvailableNetworkNotifier();
            }
        }, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
    }

    public /* synthetic */ void lambda$handleWifiConnected$1$AvailableNetworkNotifier() {
        if (this.mState == 3) {
            clearPendingNotification(true);
        }
    }

    public void handleConnectionFailure() {
        if (this.mState == 2) {
            postNotification(this.mNotificationBuilder.createNetworkFailedNotification(this.mTag));
            if (this.mRecommendedNetwork != null) {
                String str = this.mTag;
                Log.d(str, "User failed to connect to recommended network: \"" + this.mRecommendedNetwork.SSID + "\"");
                this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 4);
            }
            this.mState = 4;
            this.mHandler.postDelayed(new Runnable() {
                public final void run() {
                    AvailableNetworkNotifier.this.lambda$handleConnectionFailure$2$AvailableNetworkNotifier();
                }
            }, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
        }
    }

    public /* synthetic */ void lambda$handleConnectionFailure$2$AvailableNetworkNotifier() {
        if (this.mState == 4) {
            clearPendingNotification(false);
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) this.mContext.getSystemService("notification");
    }

    private void postInitialNotification(ScanResult recommendedNetwork) {
        ScanResult scanResult = this.mRecommendedNetwork;
        if (scanResult == null || !TextUtils.equals(scanResult.SSID, recommendedNetwork.SSID)) {
            postNotification(this.mNotificationBuilder.createConnectToAvailableNetworkNotification(this.mTag, recommendedNetwork));
            if (this.mState == 0) {
                this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 1);
            } else {
                this.mWifiMetrics.incrementNumNetworkRecommendationUpdates(this.mTag);
            }
            this.mState = 1;
            this.mRecommendedNetwork = recommendedNetwork;
            this.mNotificationRepeatTime = this.mClock.getWallClockMillis() + this.mNotificationRepeatDelay;
        }
    }

    private void postInitialNotification(int numOpenNetworks) {
        if ("VZW".equals(VendorNotificationStyle)) {
            postNotification(this.mNotificationBuilder.createVZWOpenNetworkAvailableNotification(this.mTag));
        } else {
            postNotification(this.mNotificationBuilder.createOpenNetworkAvailableNotification(this.mTag, numOpenNetworks));
        }
        if (this.mState == 0) {
            this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 1);
        } else {
            this.mWifiMetrics.incrementNumNetworkRecommendationUpdates(this.mTag);
        }
        this.mState = 1;
        this.mNotificationRepeatTime = this.mClock.getWallClockMillis() + this.mNotificationRepeatDelay;
    }

    private void postNotification(Notification notification) {
        getNotificationManager().notify(this.mSystemMessageNotificationId, notification);
    }

    /* access modifiers changed from: private */
    public void handleConnectToNetworkAction() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 2);
        if (this.mState == 1) {
            ScanResult scanResult = this.mRecommendedNetwork;
            if (scanResult != null) {
                postNotification(this.mNotificationBuilder.createNetworkConnectingNotification(this.mTag, scanResult));
                this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 2);
                String str = this.mTag;
                Log.d(str, "User initiated connection to recommended network: \"" + this.mRecommendedNetwork.SSID + "\"");
                NetworkUpdateResult result = this.mConfigManager.addOrUpdateNetwork(createRecommendedNetworkConfig(this.mRecommendedNetwork), 1010);
                if (result.isSuccess()) {
                    this.mWifiMetrics.setNominatorForNetwork(result.netId, this.mNominatorId);
                    Message msg = Message.obtain();
                    msg.what = 151553;
                    msg.arg1 = result.netId;
                    msg.obj = null;
                    msg.replyTo = this.mSrcMessenger;
                    this.mClientModeImpl.sendMessage(msg);
                    addNetworkToBlacklist(this.mRecommendedNetwork.SSID);
                }
            }
            this.mState = 2;
            this.mHandler.postDelayed(new Runnable() {
                public final void run() {
                    AvailableNetworkNotifier.this.lambda$handleConnectToNetworkAction$3$AvailableNetworkNotifier();
                }
            }, 10000);
        }
    }

    public /* synthetic */ void lambda$handleConnectToNetworkAction$3$AvailableNetworkNotifier() {
        if (this.mState == 2) {
            handleConnectionFailure();
        }
    }

    private void addNetworkToBlacklist(String ssid) {
        this.mBlacklistedSsids.add(ssid);
        this.mWifiMetrics.setNetworkRecommenderBlacklistSize(this.mTag, this.mBlacklistedSsids.size());
        this.mConfigManager.saveToStore(false);
        String str = this.mTag;
        Log.d(str, "Network is added to the network notification blacklist: \"" + ssid + "\"");
    }

    private void removeNetworkFromBlacklist(String ssid) {
        if (ssid != null && this.mBlacklistedSsids.remove(ssid)) {
            this.mWifiMetrics.setNetworkRecommenderBlacklistSize(this.mTag, this.mBlacklistedSsids.size());
            this.mConfigManager.saveToStore(false);
            String str = this.mTag;
            Log.d(str, "Network is removed from the network notification blacklist: \"" + ssid + "\"");
        }
    }

    /* access modifiers changed from: package-private */
    public WifiConfiguration createRecommendedNetworkConfig(ScanResult recommendedNetwork) {
        return ScanResultUtil.createNetworkFromScanResult(recommendedNetwork);
    }

    /* access modifiers changed from: private */
    public void handleSeeAllNetworksAction() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 3);
        startWifiSettings();
    }

    private void startWifiSettings() {
        this.mContext.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        this.mContext.startActivity(new Intent("android.settings.WIFI_SETTINGS").addFlags(268435456));
        clearPendingNotification(false);
    }

    public void handleConnectionAttemptFailedToSend() {
        handleConnectionFailure();
        this.mWifiMetrics.incrementNumNetworkConnectMessageFailedToSend(this.mTag);
    }

    /* access modifiers changed from: private */
    public void handlePickWifiNetworkAfterConnectFailure() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 4);
        startWifiSettings();
    }

    /* access modifiers changed from: private */
    public void handleUserDismissedAction() {
        ScanResult scanResult;
        String str = this.mTag;
        Log.d(str, "User dismissed notification with state=" + this.mState);
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 1);
        if (this.mState == 1 && (scanResult = this.mRecommendedNetwork) != null) {
            addNetworkToBlacklist(scanResult.SSID);
        }
        resetStateAndDelayNotification();
    }

    private void resetStateAndDelayNotification() {
        this.mState = 0;
        this.mNotificationRepeatTime = System.currentTimeMillis() + this.mNotificationRepeatDelay;
        this.mRecommendedNetwork = null;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println(this.mTag + ": ");
        pw.println("mSettingEnabled " + this.mSettingEnabled);
        pw.println("currentTime: " + this.mClock.getWallClockMillis());
        pw.println("mNotificationRepeatTime: " + this.mNotificationRepeatTime);
        pw.println("mState: " + this.mState);
        pw.println("mBlacklistedSsids: " + this.mBlacklistedSsids.toString());
    }

    private class AvailableNetworkNotifierStoreData implements SsidSetStoreData.DataSource {
        private AvailableNetworkNotifierStoreData() {
        }

        public Set<String> getSsids() {
            return new ArraySet(AvailableNetworkNotifier.this.mBlacklistedSsids);
        }

        public void setSsids(Set<String> ssidList) {
            AvailableNetworkNotifier.this.mBlacklistedSsids.addAll(ssidList);
            AvailableNetworkNotifier.this.mWifiMetrics.setNetworkRecommenderBlacklistSize(AvailableNetworkNotifier.this.mTag, AvailableNetworkNotifier.this.mBlacklistedSsids.size());
        }
    }

    private class NotificationEnabledSettingObserver extends ContentObserver {
        NotificationEnabledSettingObserver(Handler handler) {
            super(handler);
        }

        public void register() {
            AvailableNetworkNotifier.this.mFrameworkFacade.registerContentObserver(AvailableNetworkNotifier.this.mContext, Settings.Global.getUriFor(AvailableNetworkNotifier.this.mToggleSettingsName), true, this);
            boolean unused = AvailableNetworkNotifier.this.mSettingEnabled = getValue();
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            boolean unused = AvailableNetworkNotifier.this.mSettingEnabled = getValue();
            AvailableNetworkNotifier.this.clearPendingNotification(true);
        }

        private boolean getValue() {
            boolean z = true;
            if (AvailableNetworkNotifier.this.mFrameworkFacade.getIntegerSetting(AvailableNetworkNotifier.this.mContext, AvailableNetworkNotifier.this.mToggleSettingsName, 1) != 1) {
                z = false;
            }
            boolean enabled = z;
            AvailableNetworkNotifier.this.mWifiMetrics.setIsWifiNetworksAvailableNotificationEnabled(AvailableNetworkNotifier.this.mTag, enabled);
            Log.d(AvailableNetworkNotifier.this.mTag, "Settings toggle enabled=" + enabled);
            if (AvailableNetworkNotifier.SUPPORT_NOTIFICATION_MENU) {
                return enabled;
            }
            return false;
        }
    }
}
