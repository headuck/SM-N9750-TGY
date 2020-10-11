package com.android.server.wifi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import com.android.internal.notification.SystemNotificationChannels;

public class WifiConnectivityNotificationManager {
    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args";
    private static final int POOR_CONNECTION_NOTIFICATION_ID = 17042689;
    private static final String PREF_KEY_POOR_NETWORK_DETECTION = "wifi_poor_network_detection";
    private static final String TAG = "WifiConnectivityNotificationManager";
    private final Context mContext;
    private Notification.Builder mWifiPoorConnectionNotificationBuilder = null;

    public WifiConnectivityNotificationManager(Context context) {
        this.mContext = context;
    }

    public void showWifiPoorConnectionNotification(String ssid, int netId, boolean visible) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (!visible) {
            notificationManager.cancel(POOR_CONNECTION_NOTIFICATION_ID);
            return;
        }
        if (ssid != null) {
            int i = netId;
        } else if (netId == -1) {
            return;
        }
        Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_poor_connection_warning", 1);
        String title = this.mContext.getResources().getString(17042693);
        String message = this.mContext.getResources().getString(17042692, new Object[]{ssid});
        Notification.BigTextStyle bigStyle = new Notification.BigTextStyle().bigText(message);
        Intent goAdvancedSettings = new Intent();
        goAdvancedSettings.setClassName("com.android.settings", "com.android.settings.Settings$ConfigureWifiSettingsActivity");
        goAdvancedSettings.addFlags(268468224);
        goAdvancedSettings.putExtra(":settings:fragment_args_key", PREF_KEY_POOR_NETWORK_DETECTION);
        Bundle args = new Bundle();
        args.putString(":settings:fragment_args_key", PREF_KEY_POOR_NETWORK_DETECTION);
        goAdvancedSettings.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        PendingIntent pendingContentIntent = PendingIntent.getActivityAsUser(this.mContext, 0, goAdvancedSettings, 134217728, (Bundle) null, UserHandle.CURRENT);
        Notification.Action actionGoSettings = new Notification.Action.Builder((Icon) null, this.mContext.getResources().getString(17042734), pendingContentIntent).build();
        this.mWifiPoorConnectionNotificationBuilder = null;
        this.mWifiPoorConnectionNotificationBuilder = new Notification.Builder(this.mContext, SystemNotificationChannels.NETWORK_ALERTS).setPriority(1).setSmallIcon(17304155).setAutoCancel(true).setStyle(bigStyle).setWhen(0).setTicker(title).setContentTitle(title).setContentText(message).setContentIntent(pendingContentIntent).addAction(actionGoSettings).setDefaults(1);
        Notification notification = this.mWifiPoorConnectionNotificationBuilder.build();
        notification.when = System.currentTimeMillis();
        notificationManager.notify(POOR_CONNECTION_NOTIFICATION_ID, notification);
    }
}
