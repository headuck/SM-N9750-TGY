package com.android.server.wifi;

import android.content.Context;
import android.os.Looper;

public class WifiNotificationController extends AvailableNetworkNotifier {
    private static final String STORE_DATA_IDENTIFIER = "WifiNotifierBlacklist";
    private static final String TAG = "WifiNotificationController";
    private static final String TOGGLE_SETTINGS_NAME = "wifi_networks_available_notification_on";

    public WifiNotificationController(Context context, Looper looper, FrameworkFacade framework, Clock clock, WifiMetrics wifiMetrics, WifiConfigManager wifiConfigManager, WifiConfigStore wifiConfigStore, ClientModeImpl clientModeImpl, ConnectToNetworkNotificationBuilder connectToNetworkNotificationBuilder) {
        super(TAG, STORE_DATA_IDENTIFIER, TOGGLE_SETTINGS_NAME, 17303299, 9, context, looper, framework, clock, wifiMetrics, wifiConfigManager, wifiConfigStore, clientModeImpl, connectToNetworkNotificationBuilder);
    }
}
