package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.MacAddress;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.INetworkRequestMatchCallback;
import android.net.wifi.INetworkRequestUserSelectionCallback;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.WorkSource;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.wifi.NetworkRequestStoreData;
import com.android.server.wifi.WifiNetworkFactory;
import com.android.server.wifi.util.ExternalCallbackTracker;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class WifiNetworkFactory extends NetworkFactory {
    @VisibleForTesting
    public static final int NETWORK_CONNECTION_TIMEOUT_MS = 30000;
    @VisibleForTesting
    public static final int PERIODIC_SCAN_INTERVAL_MS = 10000;
    @VisibleForTesting
    private static final int SCORE_FILTER = 60;
    private static final String TAG = "WifiNetworkFactory";
    @VisibleForTesting
    public static final String UI_START_INTENT_ACTION = "com.android.settings.wifi.action.NETWORK_REQUEST";
    @VisibleForTesting
    public static final String UI_START_INTENT_CATEGORY = "android.intent.category.DEFAULT";
    @VisibleForTesting
    public static final String UI_START_INTENT_EXTRA_APP_NAME = "com.android.settings.wifi.extra.APP_NAME";
    @VisibleForTesting
    public static final String UI_START_INTENT_EXTRA_REQUEST_IS_FOR_SINGLE_NETWORK = "com.android.settings.wifi.extra.REQUEST_IS_FOR_SINGLE_NETWORK";
    @VisibleForTesting
    public static final int USER_SELECTED_NETWORK_CONNECT_RETRY_MAX = 3;
    /* access modifiers changed from: private */
    public List<ScanResult> mActiveMatchedScanResults;
    /* access modifiers changed from: private */
    public NetworkRequest mActiveSpecificNetworkRequest;
    private WifiNetworkSpecifier mActiveSpecificNetworkRequestSpecifier;
    private final ActivityManager mActivityManager;
    private final AlarmManager mAlarmManager;
    private final AppOpsManager mAppOpsManager;
    private final Clock mClock;
    private NetworkRequest mConnectedSpecificNetworkRequest;
    private WifiNetworkSpecifier mConnectedSpecificNetworkRequestSpecifier;
    private final ConnectionTimeoutAlarmListener mConnectionTimeoutAlarmListener;
    private boolean mConnectionTimeoutSet = false;
    private final Context mContext;
    private int mGenericConnectionReqCount = 0;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mHasNewDataToSerialize = false;
    private boolean mIsPeriodicScanPaused = false;
    private final Handler.Callback mNetworkConnectionTriggerCallback = new Handler.Callback() {
        public final boolean handleMessage(Message message) {
            return WifiNetworkFactory.this.lambda$new$0$WifiNetworkFactory(message);
        }
    };
    private boolean mPendingConnectionSuccess = false;
    private final PeriodicScanAlarmListener mPeriodicScanTimerListener;
    private boolean mPeriodicScanTimerSet = false;
    private final ExternalCallbackTracker<INetworkRequestMatchCallback> mRegisteredCallbacks;
    private final NetworkFactoryScanListener mScanListener;
    private final WifiScanner.ScanSettings mScanSettings;
    private final Messenger mSrcMessenger;
    /* access modifiers changed from: private */
    public final Map<String, Set<AccessPoint>> mUserApprovedAccessPointMap = new HashMap();
    /* access modifiers changed from: private */
    public WifiConfiguration mUserSelectedNetwork;
    private int mUserSelectedNetworkConnectRetryCount;
    /* access modifiers changed from: private */
    public boolean mVerboseLoggingEnabled = false;
    /* access modifiers changed from: private */
    public final WifiConfigManager mWifiConfigManager;
    private final WifiConfigStore mWifiConfigStore;
    private final WifiConnectivityManager mWifiConnectivityManager;
    private boolean mWifiEnabled = false;
    private final WifiInjector mWifiInjector;
    /* access modifiers changed from: private */
    public final WifiMetrics mWifiMetrics;
    private final WifiPermissionsUtil mWifiPermissionsUtil;
    private WifiScanner mWifiScanner;

    public static class AccessPoint {
        public final MacAddress bssid;
        public final int networkType;
        public final String ssid;

        AccessPoint(String ssid2, MacAddress bssid2, int networkType2) {
            this.ssid = ssid2;
            this.bssid = bssid2;
            this.networkType = networkType2;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.ssid, this.bssid, Integer.valueOf(this.networkType)});
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AccessPoint)) {
                return false;
            }
            AccessPoint other = (AccessPoint) obj;
            if (!TextUtils.equals(this.ssid, other.ssid) || !Objects.equals(this.bssid, other.bssid) || this.networkType != other.networkType) {
                return false;
            }
            return true;
        }

        public String toString() {
            return "AccessPoint: " + this.ssid + ", " + this.bssid + ", " + this.networkType;
        }
    }

    private class NetworkFactoryScanListener implements WifiScanner.ScanListener {
        private NetworkFactoryScanListener() {
        }

        public void onSuccess() {
            if (WifiNetworkFactory.this.mVerboseLoggingEnabled) {
                Log.d(WifiNetworkFactory.TAG, "Scan request succeeded");
            }
        }

        public void onFailure(int reason, String description) {
            Log.e(WifiNetworkFactory.TAG, "Scan failure received. reason: " + reason + ", description: " + description);
            WifiNetworkFactory.this.scheduleNextPeriodicScan();
        }

        public void onResults(WifiScanner.ScanData[] scanDatas) {
            if (WifiNetworkFactory.this.mVerboseLoggingEnabled) {
                Log.d(WifiNetworkFactory.TAG, "Scan results received");
            }
            if (scanDatas.length != 1) {
                Log.wtf(WifiNetworkFactory.TAG, "Found more than 1 batch of scan results, Ignoring...");
                return;
            }
            ScanResult[] scanResults = scanDatas[0].getResults();
            if (WifiNetworkFactory.this.mVerboseLoggingEnabled) {
                Log.v(WifiNetworkFactory.TAG, "Received " + scanResults.length + " scan results");
            }
            List<ScanResult> matchedScanResults = WifiNetworkFactory.this.getNetworksMatchingActiveNetworkRequest(scanResults);
            if (WifiNetworkFactory.this.mActiveMatchedScanResults == null) {
                WifiNetworkFactory.this.mWifiMetrics.incrementNetworkRequestApiMatchSizeHistogram(matchedScanResults.size());
            }
            List unused = WifiNetworkFactory.this.mActiveMatchedScanResults = matchedScanResults;
            ScanResult approvedScanResult = null;
            if (WifiNetworkFactory.this.isActiveRequestForSingleAccessPoint()) {
                approvedScanResult = WifiNetworkFactory.this.m51x1456ab16();
            }
            if (approvedScanResult == null || WifiNetworkFactory.this.mWifiConfigManager.wasEphemeralNetworkDeleted(ScanResultUtil.createQuotedSSID(approvedScanResult.SSID))) {
                if (WifiNetworkFactory.this.mVerboseLoggingEnabled) {
                    Log.v(WifiNetworkFactory.TAG, "No approved access points found in matching scan results. Sending match callback");
                }
                WifiNetworkFactory.this.sendNetworkRequestMatchCallbacksForActiveRequest(matchedScanResults);
                WifiNetworkFactory.this.scheduleNextPeriodicScan();
                return;
            }
            Log.v(WifiNetworkFactory.TAG, "Approved access point found in matching scan results. Triggering connect " + approvedScanResult);
            WifiNetworkFactory.this.handleConnectToNetworkUserSelectionInternal(ScanResultUtil.createNetworkFromScanResult(approvedScanResult));
            WifiNetworkFactory.this.mWifiMetrics.incrementNetworkRequestApiNumUserApprovalBypass();
        }

        public void onFullResult(ScanResult fullScanResult) {
        }

        public void onPeriodChanged(int periodInMs) {
        }
    }

    private class PeriodicScanAlarmListener implements AlarmManager.OnAlarmListener {
        private PeriodicScanAlarmListener() {
        }

        public void onAlarm() {
            WifiNetworkFactory.this.startScan();
        }
    }

    private class ConnectionTimeoutAlarmListener implements AlarmManager.OnAlarmListener {
        private ConnectionTimeoutAlarmListener() {
        }

        public void onAlarm() {
            Log.e(WifiNetworkFactory.TAG, "Timed-out connecting to network");
            WifiNetworkFactory wifiNetworkFactory = WifiNetworkFactory.this;
            wifiNetworkFactory.handleNetworkConnectionFailure(wifiNetworkFactory.mUserSelectedNetwork);
        }
    }

    private class NetworkFactoryUserSelectionCallback extends INetworkRequestUserSelectionCallback.Stub {
        private final NetworkRequest mNetworkRequest;

        NetworkFactoryUserSelectionCallback(NetworkRequest networkRequest) {
            this.mNetworkRequest = networkRequest;
        }

        public void select(WifiConfiguration wifiConfiguration) {
            WifiNetworkFactory.this.mHandler.post(new Runnable(wifiConfiguration) {
                private final /* synthetic */ WifiConfiguration f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WifiNetworkFactory.NetworkFactoryUserSelectionCallback.this.mo3901xa5b862d6(this.f$1);
                }
            });
        }

        /* renamed from: lambda$select$0$WifiNetworkFactory$NetworkFactoryUserSelectionCallback */
        public /* synthetic */ void mo3901xa5b862d6(WifiConfiguration wifiConfiguration) {
            if (WifiNetworkFactory.this.mActiveSpecificNetworkRequest != this.mNetworkRequest) {
                Log.e(WifiNetworkFactory.TAG, "Stale callback select received");
            } else {
                WifiNetworkFactory.this.handleConnectToNetworkUserSelection(wifiConfiguration);
            }
        }

        public void reject() {
            WifiNetworkFactory.this.mHandler.post(new Runnable() {
                public final void run() {
                    WifiNetworkFactory.NetworkFactoryUserSelectionCallback.this.mo3900x3e73f072();
                }
            });
        }

        /* renamed from: lambda$reject$1$WifiNetworkFactory$NetworkFactoryUserSelectionCallback */
        public /* synthetic */ void mo3900x3e73f072() {
            if (WifiNetworkFactory.this.mActiveSpecificNetworkRequest != this.mNetworkRequest) {
                Log.e(WifiNetworkFactory.TAG, "Stale callback reject received");
            } else {
                WifiNetworkFactory.this.handleRejectUserSelection();
            }
        }
    }

    public /* synthetic */ boolean lambda$new$0$WifiNetworkFactory(Message msg) {
        switch (msg.what) {
            case 151554:
                Log.e(TAG, "Failed to trigger network connection");
                handleNetworkConnectionFailure(this.mUserSelectedNetwork);
                return true;
            case 151555:
                if (!this.mVerboseLoggingEnabled) {
                    return true;
                }
                Log.v(TAG, "Triggered network connection");
                return true;
            default:
                Log.e(TAG, "Unknown message " + msg.what);
                return true;
        }
    }

    private class NetworkRequestDataSource implements NetworkRequestStoreData.DataSource {
        private NetworkRequestDataSource() {
        }

        public Map<String, Set<AccessPoint>> toSerialize() {
            boolean unused = WifiNetworkFactory.this.mHasNewDataToSerialize = false;
            return WifiNetworkFactory.this.mUserApprovedAccessPointMap;
        }

        public void fromDeserialized(Map<String, Set<AccessPoint>> approvedAccessPointMap) {
            WifiNetworkFactory.this.mUserApprovedAccessPointMap.putAll(approvedAccessPointMap);
        }

        public void reset() {
            WifiNetworkFactory.this.mUserApprovedAccessPointMap.clear();
        }

        public boolean hasNewDataToSerialize() {
            return WifiNetworkFactory.this.mHasNewDataToSerialize;
        }
    }

    /* JADX WARNING: Illegal instructions before constructor call */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public WifiNetworkFactory(android.os.Looper r17, android.content.Context r18, android.net.NetworkCapabilities r19, android.app.ActivityManager r20, android.app.AlarmManager r21, android.app.AppOpsManager r22, com.android.server.wifi.Clock r23, com.android.server.wifi.WifiInjector r24, com.android.server.wifi.WifiConnectivityManager r25, com.android.server.wifi.WifiConfigManager r26, com.android.server.wifi.WifiConfigStore r27, com.android.server.wifi.util.WifiPermissionsUtil r28, com.android.server.wifi.WifiMetrics r29) {
        /*
            r16 = this;
            r0 = r16
            r1 = r17
            r2 = r18
            r3 = r24
            r4 = r27
            java.lang.String r5 = "WifiNetworkFactory"
            r6 = r19
            r0.<init>(r1, r2, r5, r6)
            java.util.HashMap r5 = new java.util.HashMap
            r5.<init>()
            r0.mUserApprovedAccessPointMap = r5
            r5 = 0
            r0.mGenericConnectionReqCount = r5
            r0.mVerboseLoggingEnabled = r5
            r0.mPeriodicScanTimerSet = r5
            r0.mConnectionTimeoutSet = r5
            r0.mIsPeriodicScanPaused = r5
            r0.mPendingConnectionSuccess = r5
            r0.mWifiEnabled = r5
            r0.mHasNewDataToSerialize = r5
            com.android.server.wifi.-$$Lambda$WifiNetworkFactory$FWSc-VL5YZolV22WpOQkmaAHmpU r5 = new com.android.server.wifi.-$$Lambda$WifiNetworkFactory$FWSc-VL5YZolV22WpOQkmaAHmpU
            r5.<init>()
            r0.mNetworkConnectionTriggerCallback = r5
            r0.mContext = r2
            r5 = r20
            r0.mActivityManager = r5
            r7 = r21
            r0.mAlarmManager = r7
            r8 = r22
            r0.mAppOpsManager = r8
            r9 = r23
            r0.mClock = r9
            android.os.Handler r10 = new android.os.Handler
            r10.<init>(r1)
            r0.mHandler = r10
            r0.mWifiInjector = r3
            r10 = r25
            r0.mWifiConnectivityManager = r10
            r11 = r26
            r0.mWifiConfigManager = r11
            r0.mWifiConfigStore = r4
            r12 = r28
            r0.mWifiPermissionsUtil = r12
            r13 = r29
            r0.mWifiMetrics = r13
            android.net.wifi.WifiScanner$ScanSettings r14 = new android.net.wifi.WifiScanner$ScanSettings
            r14.<init>()
            r0.mScanSettings = r14
            android.net.wifi.WifiScanner$ScanSettings r14 = r0.mScanSettings
            r15 = 2
            r14.type = r15
            r15 = 7
            r14.band = r15
            r15 = 1
            r14.reportEvents = r15
            com.android.server.wifi.WifiNetworkFactory$NetworkFactoryScanListener r14 = new com.android.server.wifi.WifiNetworkFactory$NetworkFactoryScanListener
            r15 = 0
            r14.<init>()
            r0.mScanListener = r14
            com.android.server.wifi.WifiNetworkFactory$PeriodicScanAlarmListener r14 = new com.android.server.wifi.WifiNetworkFactory$PeriodicScanAlarmListener
            r14.<init>()
            r0.mPeriodicScanTimerListener = r14
            com.android.server.wifi.WifiNetworkFactory$ConnectionTimeoutAlarmListener r14 = new com.android.server.wifi.WifiNetworkFactory$ConnectionTimeoutAlarmListener
            r14.<init>()
            r0.mConnectionTimeoutAlarmListener = r14
            com.android.server.wifi.util.ExternalCallbackTracker r14 = new com.android.server.wifi.util.ExternalCallbackTracker
            android.os.Handler r15 = r0.mHandler
            r14.<init>(r15)
            r0.mRegisteredCallbacks = r14
            android.os.Messenger r14 = new android.os.Messenger
            android.os.Handler r15 = new android.os.Handler
            android.os.Handler$Callback r2 = r0.mNetworkConnectionTriggerCallback
            r15.<init>(r1, r2)
            r14.<init>(r15)
            r0.mSrcMessenger = r14
            com.android.server.wifi.WifiNetworkFactory$NetworkRequestDataSource r2 = new com.android.server.wifi.WifiNetworkFactory$NetworkRequestDataSource
            r14 = 0
            r2.<init>()
            com.android.server.wifi.NetworkRequestStoreData r2 = r3.makeNetworkRequestStoreData(r2)
            r4.registerStoreData(r2)
            r2 = 60
            r0.setScoreFilter(r2)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiNetworkFactory.<init>(android.os.Looper, android.content.Context, android.net.NetworkCapabilities, android.app.ActivityManager, android.app.AlarmManager, android.app.AppOpsManager, com.android.server.wifi.Clock, com.android.server.wifi.WifiInjector, com.android.server.wifi.WifiConnectivityManager, com.android.server.wifi.WifiConfigManager, com.android.server.wifi.WifiConfigStore, com.android.server.wifi.util.WifiPermissionsUtil, com.android.server.wifi.WifiMetrics):void");
    }

    private void saveToStore() {
        this.mHasNewDataToSerialize = true;
        if (!this.mWifiConfigManager.saveToStore(true)) {
            Log.w(TAG, "Failed to save to store");
        }
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
    }

    public void addCallback(IBinder binder, INetworkRequestMatchCallback callback, int callbackIdentifier) {
        if (this.mActiveSpecificNetworkRequest == null) {
            Log.wtf(TAG, "No valid network request. Ignoring callback registration");
            try {
                callback.onAbort();
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to invoke network request abort callback " + callback, e);
            }
        } else if (!this.mRegisteredCallbacks.add(binder, callback, callbackIdentifier)) {
            Log.e(TAG, "Failed to add callback");
        } else {
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Adding callback. Num callbacks: " + this.mRegisteredCallbacks.getNumCallbacks());
            }
            try {
                callback.onUserSelectionCallbackRegistration(new NetworkFactoryUserSelectionCallback(this.mActiveSpecificNetworkRequest));
            } catch (RemoteException e2) {
                Log.e(TAG, "Unable to invoke user selection registration callback " + callback, e2);
            }
        }
    }

    public void removeCallback(int callbackIdentifier) {
        this.mRegisteredCallbacks.remove(callbackIdentifier);
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Removing callback. Num callbacks: " + this.mRegisteredCallbacks.getNumCallbacks());
        }
    }

    private boolean canNewRequestOverrideExistingRequest(WifiNetworkSpecifier newRequest, WifiNetworkSpecifier existingRequest) {
        if (existingRequest == null || this.mWifiPermissionsUtil.checkNetworkSettingsPermission(newRequest.requestorUid) || isRequestFromForegroundApp(newRequest.requestorPackageName) || !isRequestFromForegroundApp(existingRequest.requestorPackageName)) {
            return true;
        }
        Log.e(TAG, "Already processing request from a foreground app " + existingRequest.requestorPackageName + ". Rejecting request from " + newRequest.requestorPackageName);
        return false;
    }

    /* access modifiers changed from: package-private */
    public boolean isRequestWithNetworkSpecifierValid(NetworkRequest networkRequest) {
        if (!(networkRequest.networkCapabilities.getNetworkSpecifier() instanceof WifiNetworkSpecifier)) {
            Log.e(TAG, "Invalid network specifier mentioned. Rejecting");
            return false;
        } else if (!networkRequest.hasCapability(12)) {
            return true;
        } else {
            Log.e(TAG, "Request with wifi network specifier cannot contain NET_CAPABILITY_INTERNET. Rejecting");
            return false;
        }
    }

    public boolean acceptRequest(NetworkRequest networkRequest, int score) {
        NetworkSpecifier ns = networkRequest.networkCapabilities.getNetworkSpecifier();
        if (ns != null) {
            if (!isRequestWithNetworkSpecifierValid(networkRequest)) {
                releaseRequestAsUnfulfillableByAnyFactory(networkRequest);
                return false;
            } else if (!this.mWifiEnabled) {
                Log.e(TAG, "Wifi off. Rejecting");
                return false;
            } else {
                WifiNetworkSpecifier wns = (WifiNetworkSpecifier) ns;
                if (!WifiConfigurationUtil.validateNetworkSpecifier(wns)) {
                    Log.e(TAG, "Invalid network specifier. Rejecting request from " + wns.requestorPackageName);
                    releaseRequestAsUnfulfillableByAnyFactory(networkRequest);
                    return false;
                }
                try {
                    this.mAppOpsManager.checkPackage(wns.requestorUid, wns.requestorPackageName);
                    if (!this.mWifiPermissionsUtil.checkNetworkSettingsPermission(wns.requestorUid) && !isRequestFromForegroundAppOrService(wns.requestorPackageName)) {
                        Log.e(TAG, "Request not from foreground app or service. Rejecting request from " + wns.requestorPackageName);
                        releaseRequestAsUnfulfillableByAnyFactory(networkRequest);
                        return false;
                    } else if (!canNewRequestOverrideExistingRequest(wns, this.mActiveSpecificNetworkRequestSpecifier)) {
                        Log.e(TAG, "Request cannot override active request. Rejecting request from " + wns.requestorPackageName);
                        releaseRequestAsUnfulfillableByAnyFactory(networkRequest);
                        return false;
                    } else if (!canNewRequestOverrideExistingRequest(wns, this.mConnectedSpecificNetworkRequestSpecifier)) {
                        Log.e(TAG, "Request cannot override connected request. Rejecting request from " + wns.requestorPackageName);
                        releaseRequestAsUnfulfillableByAnyFactory(networkRequest);
                        return false;
                    } else if (this.mVerboseLoggingEnabled) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Accepted network request with specifier from fg ");
                        sb.append(isRequestFromForegroundApp(wns.requestorPackageName) ? "app" : "service");
                        Log.v(TAG, sb.toString());
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "Invalid uid/package name " + wns.requestorPackageName + ", " + wns.requestorPackageName, e);
                    releaseRequestAsUnfulfillableByAnyFactory(networkRequest);
                    return false;
                }
            }
        }
        if (!this.mVerboseLoggingEnabled) {
            return true;
        }
        Log.v(TAG, "Accepted network request " + networkRequest);
        return true;
    }

    /* access modifiers changed from: protected */
    public void needNetworkFor(NetworkRequest networkRequest, int score) {
        NetworkSpecifier ns = networkRequest.networkCapabilities.getNetworkSpecifier();
        if (ns == null) {
            int i = this.mGenericConnectionReqCount + 1;
            this.mGenericConnectionReqCount = i;
            if (i == 1) {
                this.mWifiConnectivityManager.setTrustedConnectionAllowed(true);
            }
        } else if (!isRequestWithNetworkSpecifierValid(networkRequest)) {
            releaseRequestAsUnfulfillableByAnyFactory(networkRequest);
        } else if (!this.mWifiEnabled) {
            Log.e(TAG, "Wifi off. Rejecting");
        } else {
            retrieveWifiScanner();
            setupForActiveRequest();
            this.mActiveSpecificNetworkRequest = new NetworkRequest(networkRequest);
            WifiNetworkSpecifier wns = (WifiNetworkSpecifier) ns;
            this.mActiveSpecificNetworkRequestSpecifier = new WifiNetworkSpecifier(wns.ssidPatternMatcher, wns.bssidPatternMatcher, wns.wifiConfiguration, wns.requestorUid, wns.requestorPackageName);
            this.mWifiMetrics.incrementNetworkRequestApiNumRequest();
            startUi();
            startPeriodicScans();
        }
    }

    /* access modifiers changed from: protected */
    public void releaseNetworkFor(NetworkRequest networkRequest) {
        NetworkSpecifier ns = networkRequest.networkCapabilities.getNetworkSpecifier();
        if (ns == null) {
            int i = this.mGenericConnectionReqCount;
            if (i == 0) {
                Log.e(TAG, "No valid network request to release");
                return;
            }
            int i2 = i - 1;
            this.mGenericConnectionReqCount = i2;
            if (i2 == 0) {
                this.mWifiConnectivityManager.setTrustedConnectionAllowed(false);
            }
        } else if (!(ns instanceof WifiNetworkSpecifier)) {
            Log.e(TAG, "Invalid network specifier mentioned. Ignoring");
        } else if (!this.mWifiEnabled) {
            Log.e(TAG, "Wifi off. Ignoring");
        } else if (this.mActiveSpecificNetworkRequest == null && this.mConnectedSpecificNetworkRequest == null) {
            Log.e(TAG, "Network release received with no active/connected request. Ignoring");
        } else if (Objects.equals(this.mActiveSpecificNetworkRequest, networkRequest)) {
            Log.i(TAG, "App released request, cancelling " + this.mActiveSpecificNetworkRequest);
            teardownForActiveRequest();
        } else if (Objects.equals(this.mConnectedSpecificNetworkRequest, networkRequest)) {
            Log.i(TAG, "App released request, cancelling " + this.mConnectedSpecificNetworkRequest);
            teardownForConnectedNetwork();
        } else {
            Log.e(TAG, "Network specifier does not match the active/connected request. Ignoring");
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        WifiNetworkFactory.super.dump(fd, pw, args);
        pw.println("WifiNetworkFactory: mGenericConnectionReqCount " + this.mGenericConnectionReqCount);
        pw.println("WifiNetworkFactory: mActiveSpecificNetworkRequest " + this.mActiveSpecificNetworkRequest);
        pw.println("WifiNetworkFactory: mUserApprovedAccessPointMap " + this.mUserApprovedAccessPointMap);
    }

    public boolean hasConnectionRequests() {
        return (this.mGenericConnectionReqCount <= 0 && this.mActiveSpecificNetworkRequest == null && this.mConnectedSpecificNetworkRequest == null) ? false : true;
    }

    public Pair<Integer, String> getSpecificNetworkRequestUidAndPackageName(WifiConfiguration connectedNetwork) {
        if (this.mUserSelectedNetwork == null || connectedNetwork == null) {
            return Pair.create(-1, "");
        }
        if (!isUserSelectedNetwork(connectedNetwork)) {
            Log.w(TAG, "Connected to unknown network " + connectedNetwork + ". Ignoring...");
            return Pair.create(-1, "");
        }
        WifiNetworkSpecifier wifiNetworkSpecifier = this.mConnectedSpecificNetworkRequestSpecifier;
        if (wifiNetworkSpecifier != null) {
            return Pair.create(Integer.valueOf(wifiNetworkSpecifier.requestorUid), this.mConnectedSpecificNetworkRequestSpecifier.requestorPackageName);
        }
        WifiNetworkSpecifier wifiNetworkSpecifier2 = this.mActiveSpecificNetworkRequestSpecifier;
        if (wifiNetworkSpecifier2 != null) {
            return Pair.create(Integer.valueOf(wifiNetworkSpecifier2.requestorUid), this.mActiveSpecificNetworkRequestSpecifier.requestorPackageName);
        }
        return Pair.create(-1, "");
    }

    private int addNetworkToWifiConfigManager(WifiConfiguration network) {
        WifiConfiguration existingSavedNetwork = this.mWifiConfigManager.getConfiguredNetwork(network.configKey());
        if (existingSavedNetwork != null) {
            return existingSavedNetwork.networkId;
        }
        NetworkUpdateResult networkUpdateResult = this.mWifiConfigManager.addOrUpdateNetwork(network, this.mActiveSpecificNetworkRequestSpecifier.requestorUid, this.mActiveSpecificNetworkRequestSpecifier.requestorPackageName);
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Added network to config manager " + networkUpdateResult.netId);
        }
        return networkUpdateResult.netId;
    }

    private void connectToNetwork(WifiConfiguration network) {
        cancelConnectionTimeout();
        int networkId = addNetworkToWifiConfigManager(network);
        this.mWifiMetrics.setNominatorForNetwork(networkId, 7);
        Message msg = Message.obtain();
        msg.what = 151553;
        msg.arg1 = networkId;
        msg.replyTo = this.mSrcMessenger;
        this.mWifiInjector.getClientModeImpl().sendMessage(msg);
        scheduleConnectionTimeout();
    }

    /* access modifiers changed from: private */
    public void handleConnectToNetworkUserSelectionInternal(WifiConfiguration network) {
        this.mWifiConnectivityManager.setSpecificNetworkRequestInProgress(true);
        WifiConfiguration networkToConnect = new WifiConfiguration(this.mActiveSpecificNetworkRequestSpecifier.wifiConfiguration);
        networkToConnect.SSID = network.SSID;
        networkToConnect.BSSID = findBestBssidFromActiveMatchedScanResultsForNetwork(network);
        networkToConnect.ephemeral = true;
        networkToConnect.fromWifiNetworkSpecifier = true;
        this.mUserSelectedNetwork = networkToConnect;
        this.mWifiInjector.getClientModeImpl().disconnectCommand();
        connectToNetwork(networkToConnect);
        this.mPendingConnectionSuccess = true;
    }

    /* access modifiers changed from: private */
    public void handleConnectToNetworkUserSelection(WifiConfiguration network) {
        Log.d(TAG, "User initiated connect to network: " + network.SSID);
        cancelPeriodicScans();
        handleConnectToNetworkUserSelectionInternal(network);
        addNetworkToUserApprovedAccessPointMap(this.mUserSelectedNetwork);
    }

    /* access modifiers changed from: private */
    public void handleRejectUserSelection() {
        Log.w(TAG, "User dismissed notification, cancelling " + this.mActiveSpecificNetworkRequest);
        teardownForActiveRequest();
        this.mWifiMetrics.incrementNetworkRequestApiNumUserReject();
    }

    private boolean isUserSelectedNetwork(WifiConfiguration config) {
        if (TextUtils.equals(this.mUserSelectedNetwork.SSID, config.SSID) && Objects.equals(this.mUserSelectedNetwork.allowedKeyManagement, config.allowedKeyManagement)) {
            return true;
        }
        return false;
    }

    public void handleConnectionAttemptEnded(int failureCode, WifiConfiguration network) {
        if (failureCode == 1) {
            handleNetworkConnectionSuccess(network);
        } else {
            handleNetworkConnectionFailure(network);
        }
    }

    private void handleNetworkConnectionSuccess(WifiConfiguration connectedNetwork) {
        if (this.mUserSelectedNetwork != null && connectedNetwork != null && this.mPendingConnectionSuccess) {
            if (!isUserSelectedNetwork(connectedNetwork)) {
                Log.w(TAG, "Connected to unknown network " + connectedNetwork + ". Ignoring...");
                return;
            }
            Log.d(TAG, "Connected to network " + this.mUserSelectedNetwork);
            for (INetworkRequestMatchCallback callback : this.mRegisteredCallbacks.getCallbacks()) {
                try {
                    callback.onUserSelectionConnectSuccess(this.mUserSelectedNetwork);
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to invoke network request connect failure callback " + callback, e);
                }
            }
            setupForConnectedRequest();
            this.mWifiMetrics.incrementNetworkRequestApiNumConnectSuccess();
        }
    }

    /* access modifiers changed from: private */
    public void handleNetworkConnectionFailure(WifiConfiguration failedNetwork) {
        if (this.mUserSelectedNetwork != null && failedNetwork != null && this.mPendingConnectionSuccess) {
            if (!isUserSelectedNetwork(failedNetwork)) {
                Log.w(TAG, "Connection failed to unknown network " + failedNetwork + ". Ignoring...");
                return;
            }
            Log.w(TAG, "Failed to connect to network " + this.mUserSelectedNetwork);
            int i = this.mUserSelectedNetworkConnectRetryCount;
            this.mUserSelectedNetworkConnectRetryCount = i + 1;
            if (i < 3) {
                Log.i(TAG, "Retrying connection attempt, attempt# " + this.mUserSelectedNetworkConnectRetryCount);
                connectToNetwork(this.mUserSelectedNetwork);
                return;
            }
            Log.e(TAG, "Connection failures, cancelling " + this.mUserSelectedNetwork);
            for (INetworkRequestMatchCallback callback : this.mRegisteredCallbacks.getCallbacks()) {
                try {
                    callback.onUserSelectionConnectFailure(this.mUserSelectedNetwork);
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to invoke network request connect failure callback " + callback, e);
                }
            }
            teardownForActiveRequest();
        }
    }

    public void handleScreenStateChanged(boolean screenOn) {
        if (this.mActiveSpecificNetworkRequest != null && this.mUserSelectedNetwork == null) {
            if (screenOn) {
                if (this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "Resuming scans on screen on");
                }
                startScan();
                this.mIsPeriodicScanPaused = false;
                return;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Pausing scans on screen off");
            }
            cancelPeriodicScans();
            this.mIsPeriodicScanPaused = true;
        }
    }

    public void setWifiState(boolean enabled) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "setWifiState " + enabled);
        }
        if (enabled) {
            reevaluateAllRequests();
        } else {
            if (this.mActiveSpecificNetworkRequest != null) {
                Log.w(TAG, "Wifi off, cancelling " + this.mActiveSpecificNetworkRequest);
                teardownForActiveRequest();
            }
            if (this.mConnectedSpecificNetworkRequest != null) {
                Log.w(TAG, "Wifi off, cancelling " + this.mConnectedSpecificNetworkRequest);
                teardownForConnectedNetwork();
            }
        }
        this.mWifiEnabled = enabled;
    }

    private void cleanupActiveRequest() {
        for (INetworkRequestMatchCallback callback : this.mRegisteredCallbacks.getCallbacks()) {
            try {
                callback.onAbort();
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to invoke network request abort callback " + callback, e);
            }
        }
        NetworkRequest networkRequest = this.mActiveSpecificNetworkRequest;
        if (networkRequest != null) {
            releaseRequestAsUnfulfillableByAnyFactory(networkRequest);
        }
        this.mActiveSpecificNetworkRequest = null;
        this.mActiveSpecificNetworkRequestSpecifier = null;
        this.mUserSelectedNetwork = null;
        this.mUserSelectedNetworkConnectRetryCount = 0;
        this.mIsPeriodicScanPaused = false;
        this.mActiveMatchedScanResults = null;
        this.mPendingConnectionSuccess = false;
        cancelPeriodicScans();
        cancelConnectionTimeout();
        this.mRegisteredCallbacks.clear();
    }

    private void setupForActiveRequest() {
        if (this.mActiveSpecificNetworkRequest != null) {
            cleanupActiveRequest();
        }
    }

    private void teardownForActiveRequest() {
        cleanupActiveRequest();
        if (this.mConnectedSpecificNetworkRequest == null) {
            this.mWifiConnectivityManager.setSpecificNetworkRequestInProgress(false);
        }
    }

    private void setupForConnectedRequest() {
        this.mConnectedSpecificNetworkRequest = this.mActiveSpecificNetworkRequest;
        this.mConnectedSpecificNetworkRequestSpecifier = this.mActiveSpecificNetworkRequestSpecifier;
        this.mActiveSpecificNetworkRequest = null;
        this.mActiveSpecificNetworkRequestSpecifier = null;
        this.mPendingConnectionSuccess = false;
        cancelConnectionTimeout();
    }

    private void teardownForConnectedNetwork() {
        Log.i(TAG, "Disconnecting from network on reset");
        this.mWifiInjector.getClientModeImpl().disconnectCommand();
        this.mConnectedSpecificNetworkRequest = null;
        this.mConnectedSpecificNetworkRequestSpecifier = null;
        if (this.mActiveSpecificNetworkRequest == null) {
            this.mWifiConnectivityManager.setSpecificNetworkRequestInProgress(false);
        }
    }

    private boolean isRequestFromForegroundAppOrService(String requestorPackageName) {
        try {
            return this.mActivityManager.getPackageImportance(requestorPackageName) <= 125;
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to check the app state", e);
            return false;
        }
    }

    private boolean isRequestFromForegroundApp(String requestorPackageName) {
        try {
            return this.mActivityManager.getPackageImportance(requestorPackageName) <= 100;
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to check the app state", e);
            return false;
        }
    }

    private void retrieveWifiScanner() {
        if (this.mWifiScanner == null) {
            this.mWifiScanner = this.mWifiInjector.getWifiScanner();
            Preconditions.checkNotNull(this.mWifiScanner);
        }
    }

    private void startPeriodicScans() {
        if (this.mActiveSpecificNetworkRequestSpecifier == null) {
            Log.e(TAG, "Periodic scan triggered when there is no active network request. Ignoring...");
            return;
        }
        WifiNetworkSpecifier wns = this.mActiveSpecificNetworkRequestSpecifier;
        if (wns.wifiConfiguration.hiddenSSID) {
            WifiScanner.ScanSettings scanSettings = this.mScanSettings;
            scanSettings.hiddenNetworks = new WifiScanner.ScanSettings.HiddenNetwork[1];
            scanSettings.hiddenNetworks[0] = new WifiScanner.ScanSettings.HiddenNetwork(NativeUtil.addEnclosingQuotes(wns.ssidPatternMatcher.getPath()));
        }
        startScan();
    }

    private void cancelPeriodicScans() {
        if (this.mPeriodicScanTimerSet) {
            this.mAlarmManager.cancel(this.mPeriodicScanTimerListener);
            this.mPeriodicScanTimerSet = false;
        }
        this.mScanSettings.hiddenNetworks = null;
    }

    /* access modifiers changed from: private */
    public void scheduleNextPeriodicScan() {
        if (this.mIsPeriodicScanPaused) {
            Log.e(TAG, "Scan triggered when periodic scanning paused. Ignoring...");
            return;
        }
        this.mAlarmManager.set(2, 10000 + this.mClock.getElapsedSinceBootMillis(), TAG, this.mPeriodicScanTimerListener, this.mHandler);
        this.mPeriodicScanTimerSet = true;
    }

    /* access modifiers changed from: private */
    public void startScan() {
        if (this.mActiveSpecificNetworkRequestSpecifier == null) {
            Log.e(TAG, "Scan triggered when there is no active network request. Ignoring...");
            return;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Starting the next scan for " + this.mActiveSpecificNetworkRequestSpecifier);
        }
        this.mWifiScanner.startScan(this.mScanSettings, this.mScanListener, new WorkSource(this.mActiveSpecificNetworkRequestSpecifier.requestorUid));
    }

    private boolean doesScanResultMatchWifiNetworkSpecifier(WifiNetworkSpecifier wns, ScanResult scanResult) {
        if (wns.ssidPatternMatcher.match(scanResult.SSID) && MacAddress.fromString(scanResult.BSSID).matches((MacAddress) wns.bssidPatternMatcher.first, (MacAddress) wns.bssidPatternMatcher.second) && ScanResultMatchInfo.getNetworkType(wns.wifiConfiguration) == ScanResultMatchInfo.getNetworkType(scanResult)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public List<ScanResult> getNetworksMatchingActiveNetworkRequest(ScanResult[] scanResults) {
        if (this.mActiveSpecificNetworkRequestSpecifier == null) {
            Log.e(TAG, "Scan results received with no active network request. Ignoring...");
            return new ArrayList();
        }
        List<ScanResult> matchedScanResults = new ArrayList<>();
        WifiNetworkSpecifier wns = this.mActiveSpecificNetworkRequestSpecifier;
        for (ScanResult scanResult : scanResults) {
            if (doesScanResultMatchWifiNetworkSpecifier(wns, scanResult)) {
                matchedScanResults.add(scanResult);
            }
        }
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "List of scan results matching the active request " + matchedScanResults);
        }
        return matchedScanResults;
    }

    /* access modifiers changed from: private */
    public void sendNetworkRequestMatchCallbacksForActiveRequest(List<ScanResult> matchedScanResults) {
        if (this.mRegisteredCallbacks.getNumCallbacks() == 0) {
            Log.e(TAG, "No callback registered for sending network request matches. Ignoring...");
            return;
        }
        for (INetworkRequestMatchCallback callback : this.mRegisteredCallbacks.getCallbacks()) {
            try {
                callback.onMatch(matchedScanResults);
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to invoke network request match callback " + callback, e);
            }
        }
    }

    private void cancelConnectionTimeout() {
        if (this.mConnectionTimeoutSet) {
            this.mAlarmManager.cancel(this.mConnectionTimeoutAlarmListener);
            this.mConnectionTimeoutSet = false;
        }
    }

    private void scheduleConnectionTimeout() {
        this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + 30000, TAG, this.mConnectionTimeoutAlarmListener, this.mHandler);
        this.mConnectionTimeoutSet = true;
    }

    private CharSequence getAppName(String packageName) {
        try {
            CharSequence appName = this.mContext.getPackageManager().getApplicationLabel(this.mContext.getPackageManager().getApplicationInfo(packageName, 0));
            if (appName != null) {
                return appName;
            }
            return "";
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to find app name for " + packageName);
            return "";
        }
    }

    private void startUi() {
        Intent intent = new Intent();
        intent.setAction(UI_START_INTENT_ACTION);
        intent.addCategory(UI_START_INTENT_CATEGORY);
        intent.setFlags(272629760);
        intent.putExtra(UI_START_INTENT_EXTRA_APP_NAME, getAppName(this.mActiveSpecificNetworkRequestSpecifier.requestorPackageName));
        intent.putExtra(UI_START_INTENT_EXTRA_REQUEST_IS_FOR_SINGLE_NETWORK, isActiveRequestForSingleNetwork());
        this.mContext.startActivityAsUser(intent, UserHandle.getUserHandleForUid(this.mActiveSpecificNetworkRequestSpecifier.requestorUid));
    }

    /* access modifiers changed from: private */
    public boolean isActiveRequestForSingleAccessPoint() {
        WifiNetworkSpecifier wifiNetworkSpecifier = this.mActiveSpecificNetworkRequestSpecifier;
        if (wifiNetworkSpecifier != null && wifiNetworkSpecifier.ssidPatternMatcher.getType() == 0 && Objects.equals(this.mActiveSpecificNetworkRequestSpecifier.bssidPatternMatcher.second, MacAddress.BROADCAST_ADDRESS)) {
            return true;
        }
        return false;
    }

    private boolean isActiveRequestForSingleNetwork() {
        WifiNetworkSpecifier wifiNetworkSpecifier = this.mActiveSpecificNetworkRequestSpecifier;
        if (wifiNetworkSpecifier == null) {
            return false;
        }
        if (wifiNetworkSpecifier.ssidPatternMatcher.getType() != 0 && !Objects.equals(this.mActiveSpecificNetworkRequestSpecifier.bssidPatternMatcher.second, MacAddress.BROADCAST_ADDRESS)) {
            return false;
        }
        return true;
    }

    private String findBestBssidFromActiveMatchedScanResultsForNetwork(WifiConfiguration network) {
        List<ScanResult> list;
        if (this.mActiveSpecificNetworkRequestSpecifier == null || (list = this.mActiveMatchedScanResults) == null) {
            return null;
        }
        ScanResult selectedScanResult = (ScanResult) list.stream().filter(new Predicate(network) {
            private final /* synthetic */ WifiConfiguration f$0;

            {
                this.f$0 = r1;
            }

            public final boolean test(Object obj) {
                return Objects.equals(ScanResultMatchInfo.fromScanResult((ScanResult) obj), ScanResultMatchInfo.fromWifiConfiguration(this.f$0));
            }
        }).max(Comparator.comparing($$Lambda$WifiNetworkFactory$CeLllDmgSLUEXADpBNicCUsuAQ.INSTANCE)).orElse((Object) null);
        if (selectedScanResult == null) {
            Log.wtf(TAG, "Expected to find at least one matching scan result");
            return null;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Best bssid selected for the request " + selectedScanResult);
        }
        return selectedScanResult.BSSID;
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:4:0x000a, code lost:
        r0 = r0.requestorPackageName;
     */
    /* renamed from: findUserApprovedAccessPointForActiveRequestFromActiveMatchedScanResults */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public android.net.wifi.ScanResult m51x1456ab16() {
        /*
            r10 = this;
            android.net.wifi.WifiNetworkSpecifier r0 = r10.mActiveSpecificNetworkRequestSpecifier
            r1 = 0
            if (r0 == 0) goto L_0x0067
            java.util.List<android.net.wifi.ScanResult> r2 = r10.mActiveMatchedScanResults
            if (r2 != 0) goto L_0x000a
            goto L_0x0067
        L_0x000a:
            java.lang.String r0 = r0.requestorPackageName
            java.util.Map<java.lang.String, java.util.Set<com.android.server.wifi.WifiNetworkFactory$AccessPoint>> r2 = r10.mUserApprovedAccessPointMap
            java.lang.Object r2 = r2.get(r0)
            java.util.Set r2 = (java.util.Set) r2
            if (r2 != 0) goto L_0x0017
            return r1
        L_0x0017:
            java.util.List<android.net.wifi.ScanResult> r3 = r10.mActiveMatchedScanResults
            java.util.Iterator r3 = r3.iterator()
        L_0x001d:
            boolean r4 = r3.hasNext()
            if (r4 == 0) goto L_0x0066
            java.lang.Object r4 = r3.next()
            android.net.wifi.ScanResult r4 = (android.net.wifi.ScanResult) r4
            com.android.server.wifi.ScanResultMatchInfo r5 = com.android.server.wifi.ScanResultMatchInfo.fromScanResult(r4)
            com.android.server.wifi.WifiNetworkFactory$AccessPoint r6 = new com.android.server.wifi.WifiNetworkFactory$AccessPoint
            java.lang.String r7 = r4.SSID
            java.lang.String r8 = r4.BSSID
            android.net.MacAddress r8 = android.net.MacAddress.fromString(r8)
            int r9 = r5.networkType
            r6.<init>(r7, r8, r9)
            boolean r7 = r2.contains(r6)
            if (r7 == 0) goto L_0x0065
            boolean r1 = r10.mVerboseLoggingEnabled
            if (r1 == 0) goto L_0x0064
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r3 = "Found "
            r1.append(r3)
            r1.append(r6)
            java.lang.String r3 = " in user approved access point for "
            r1.append(r3)
            r1.append(r0)
            java.lang.String r1 = r1.toString()
            java.lang.String r3 = "WifiNetworkFactory"
            android.util.Log.v(r3, r1)
        L_0x0064:
            return r4
        L_0x0065:
            goto L_0x001d
        L_0x0066:
            return r1
        L_0x0067:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiNetworkFactory.m51x1456ab16():android.net.wifi.ScanResult");
    }

    private void addNetworkToUserApprovedAccessPointMap(WifiConfiguration network) {
        if (this.mActiveSpecificNetworkRequestSpecifier != null && this.mActiveMatchedScanResults != null) {
            Set<AccessPoint> newUserApprovedAccessPoints = new HashSet<>();
            for (ScanResult scanResult : this.mActiveMatchedScanResults) {
                ScanResultMatchInfo fromScanResult = ScanResultMatchInfo.fromScanResult(scanResult);
                if (fromScanResult.equals(ScanResultMatchInfo.fromWifiConfiguration(network))) {
                    newUserApprovedAccessPoints.add(new AccessPoint(scanResult.SSID, MacAddress.fromString(scanResult.BSSID), fromScanResult.networkType));
                }
            }
            if (!newUserApprovedAccessPoints.isEmpty()) {
                String requestorPackageName = this.mActiveSpecificNetworkRequestSpecifier.requestorPackageName;
                Set<AccessPoint> approvedAccessPoints = this.mUserApprovedAccessPointMap.get(requestorPackageName);
                if (approvedAccessPoints == null) {
                    approvedAccessPoints = new HashSet<>();
                    this.mUserApprovedAccessPointMap.put(requestorPackageName, approvedAccessPoints);
                    this.mWifiMetrics.incrementNetworkRequestApiNumApps();
                }
                if (this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "Adding " + newUserApprovedAccessPoints + " to user approved access point for " + requestorPackageName);
                }
                approvedAccessPoints.addAll(newUserApprovedAccessPoints);
                saveToStore();
            }
        }
    }

    public void removeUserApprovedAccessPointsForApp(String packageName) {
        Iterator<Map.Entry<String, Set<AccessPoint>>> iter = this.mUserApprovedAccessPointMap.entrySet().iterator();
        while (iter.hasNext()) {
            if (packageName.equals(iter.next().getKey())) {
                Log.i(TAG, "Removing all approved access points for " + packageName);
                iter.remove();
            }
        }
        saveToStore();
    }

    public void clear() {
        this.mUserApprovedAccessPointMap.clear();
        Log.i(TAG, "Cleared all internal state");
        saveToStore();
    }
}
