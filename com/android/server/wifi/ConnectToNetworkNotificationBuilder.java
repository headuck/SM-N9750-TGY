package com.android.server.wifi;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import com.android.internal.notification.SystemNotificationChannels;

public class ConnectToNetworkNotificationBuilder {
    public static final String ACTION_CONNECT_TO_NETWORK = "com.android.server.wifi.ConnectToNetworkNotification.CONNECT_TO_NETWORK";
    public static final String ACTION_PICK_WIFI_NETWORK = "com.android.server.wifi.ConnectToNetworkNotification.PICK_WIFI_NETWORK";
    public static final String ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE = "com.android.server.wifi.ConnectToNetworkNotification.PICK_NETWORK_AFTER_FAILURE";
    public static final String ACTION_USER_DISMISSED_NOTIFICATION = "com.android.server.wifi.ConnectToNetworkNotification.USER_DISMISSED_NOTIFICATION";
    public static final String AVAILABLE_NETWORK_NOTIFIER_TAG = "com.android.server.wifi.ConnectToNetworkNotification.AVAILABLE_NETWORK_NOTIFIER_TAG";
    private Context mContext;
    private FrameworkFacade mFrameworkFacade;
    private Resources mResources;

    public ConnectToNetworkNotificationBuilder(Context context, FrameworkFacade framework) {
        this.mContext = context;
        this.mResources = context.getResources();
        this.mFrameworkFacade = framework;
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x0028  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x004b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public android.app.Notification createConnectToAvailableNetworkNotification(java.lang.String r8, android.net.wifi.ScanResult r9) {
        /*
            r7 = this;
            int r0 = r8.hashCode()
            r1 = 594918769(0x2375bd71, float:1.3321592E-17)
            r2 = 1
            if (r0 == r1) goto L_0x001a
            r1 = 2017428693(0x783f84d5, float:1.5537857E34)
            if (r0 == r1) goto L_0x0010
        L_0x000f:
            goto L_0x0024
        L_0x0010:
            java.lang.String r0 = "WifiOpenNetworkNotifier"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto L_0x000f
            r0 = 0
            goto L_0x0025
        L_0x001a:
            java.lang.String r0 = "WifiCarrierNetworkNotifier"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto L_0x000f
            r0 = r2
            goto L_0x0025
        L_0x0024:
            r0 = -1
        L_0x0025:
            r1 = 0
            if (r0 == 0) goto L_0x004b
            if (r0 == r2) goto L_0x0041
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r2 = "Unknown network notifier."
            r0.append(r2)
            r0.append(r8)
            java.lang.String r0 = r0.toString()
            java.lang.String r2 = "ConnectToNetworkNotificationBuilder"
            android.util.Log.wtf(r2, r0)
            return r1
        L_0x0041:
            android.content.Context r0 = r7.mContext
            r2 = 17042627(0x1040cc3, float:2.4253727E-38)
            java.lang.CharSequence r0 = r0.getText(r2)
            goto L_0x0055
        L_0x004b:
            android.content.Context r0 = r7.mContext
            r2 = 17042630(0x1040cc6, float:2.4253735E-38)
            java.lang.CharSequence r0 = r0.getText(r2)
        L_0x0055:
            android.app.Notification$Action$Builder r2 = new android.app.Notification$Action$Builder
            android.content.res.Resources r3 = r7.mResources
            r4 = 17042626(0x1040cc2, float:2.4253724E-38)
            java.lang.CharSequence r3 = r3.getText(r4)
            java.lang.String r4 = "com.android.server.wifi.ConnectToNetworkNotification.CONNECT_TO_NETWORK"
            android.app.PendingIntent r4 = r7.getPrivateBroadcast(r4, r8)
            r2.<init>(r1, r3, r4)
            android.app.Notification$Action r2 = r2.build()
            android.app.Notification$Action$Builder r3 = new android.app.Notification$Action$Builder
            android.content.res.Resources r4 = r7.mResources
            r5 = 17042625(0x1040cc1, float:2.4253721E-38)
            java.lang.CharSequence r4 = r4.getText(r5)
            java.lang.String r5 = "com.android.server.wifi.ConnectToNetworkNotification.PICK_WIFI_NETWORK"
            android.app.PendingIntent r6 = r7.getPrivateBroadcast(r5, r8)
            r3.<init>(r1, r4, r6)
            android.app.Notification$Action r1 = r3.build()
            java.lang.String r3 = r9.SSID
            android.app.Notification$Builder r3 = r7.createNotificationBuilder(r0, r3, r8)
            android.app.PendingIntent r4 = r7.getPrivateBroadcast(r5, r8)
            android.app.Notification$Builder r3 = r3.setContentIntent(r4)
            android.app.Notification$Builder r3 = r3.addAction(r2)
            android.app.Notification$Builder r3 = r3.addAction(r1)
            android.app.Notification r3 = r3.build()
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.ConnectToNetworkNotificationBuilder.createConnectToAvailableNetworkNotification(java.lang.String, android.net.wifi.ScanResult):android.app.Notification");
    }

    public Notification createOpenNetworkAvailableNotification(String notifierTag, int numNetworks) {
        Notification.Action build = new Notification.Action.Builder((Icon) null, this.mResources.getText(17042626), getPrivateBroadcast(ACTION_CONNECT_TO_NETWORK, notifierTag)).build();
        return createNotificationBuilder(this.mContext.getResources().getQuantityText(18153512, numNetworks), this.mContext.getResources().getQuantityText(18153513, numNetworks), notifierTag).addAction(new Notification.Action.Builder((Icon) null, this.mResources.getText(17042625), getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, notifierTag)).build()).setContentIntent(getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, notifierTag)).setAutoCancel(true).build();
    }

    public Notification createVZWOpenNetworkAvailableNotification(String notifierTag) {
        Notification.Action build = new Notification.Action.Builder((Icon) null, this.mResources.getText(17042626), getPrivateBroadcast(ACTION_CONNECT_TO_NETWORK, notifierTag)).build();
        return createNotificationBuilder(this.mContext.getText(17042681), this.mContext.getText(17042682), notifierTag).addAction(new Notification.Action.Builder((Icon) null, this.mResources.getText(17042625), getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, notifierTag)).build()).setContentIntent(getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, notifierTag)).setSmallIcon(17304253).setAutoCancel(true).build();
    }

    public Notification createNetworkConnectingNotification(String notifierTag, ScanResult network) {
        return createNotificationBuilder(this.mContext.getText(17042632), network.SSID, notifierTag).setProgress(0, 0, true).build();
    }

    public Notification createNetworkConnectedNotification(String notifierTag, ScanResult network) {
        return createNotificationBuilder(this.mContext.getText(17042631), network.SSID, notifierTag).build();
    }

    public Notification createNetworkFailedNotification(String notifierTag) {
        return createNotificationBuilder(this.mContext.getText(17042633), this.mContext.getText(17042628), notifierTag).setContentIntent(getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE, notifierTag)).setAutoCancel(true).build();
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0028  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x002d A[RETURN] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getNotifierRequestCode(java.lang.String r5) {
        /*
            r4 = this;
            int r0 = r5.hashCode()
            r1 = 594918769(0x2375bd71, float:1.3321592E-17)
            r2 = 0
            r3 = 1
            if (r0 == r1) goto L_0x001b
            r1 = 2017428693(0x783f84d5, float:1.5537857E34)
            if (r0 == r1) goto L_0x0011
        L_0x0010:
            goto L_0x0025
        L_0x0011:
            java.lang.String r0 = "WifiOpenNetworkNotifier"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x0010
            r0 = r2
            goto L_0x0026
        L_0x001b:
            java.lang.String r0 = "WifiCarrierNetworkNotifier"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x0010
            r0 = r3
            goto L_0x0026
        L_0x0025:
            r0 = -1
        L_0x0026:
            if (r0 == 0) goto L_0x002d
            if (r0 == r3) goto L_0x002b
            return r2
        L_0x002b:
            r0 = 2
            return r0
        L_0x002d:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.ConnectToNetworkNotificationBuilder.getNotifierRequestCode(java.lang.String):int");
    }

    private Notification.Builder createNotificationBuilder(CharSequence title, CharSequence content, String extraData) {
        return this.mFrameworkFacade.makeNotificationBuilder(this.mContext, SystemNotificationChannels.NETWORK_AVAILABLE).setSmallIcon(17304154).setTicker(title).setContentTitle(title).setContentText(content).setDeleteIntent(getPrivateBroadcast(ACTION_USER_DISMISSED_NOTIFICATION, extraData)).setShowWhen(false).setLocalOnly(true).setColor(this.mResources.getColor(17170460, this.mContext.getTheme()));
    }

    public Notification createHotspot20ConnectedNotification(String notifierTag, WifiConfiguration wifiConfiguration, String venueName) {
        String title;
        String message;
        if (venueName == null) {
            title = String.format(this.mContext.getResources().getString(17042646), new Object[]{wifiConfiguration.SSID});
        } else {
            title = String.format(this.mContext.getResources().getString(17042647), new Object[]{wifiConfiguration.SSID, venueName});
        }
        if (wifiConfiguration.providerFriendlyName != null) {
            message = String.format(this.mContext.getResources().getString(17042644, new Object[]{wifiConfiguration.providerFriendlyName}), new Object[0]);
        } else {
            message = String.format(this.mContext.getResources().getString(17042645, new Object[]{wifiConfiguration.providerFriendlyName}), new Object[0]);
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.Settings$ConfigureWifiSettingsActivity");
        intent.addFlags(268468224);
        return createNotificationBuilder(title, message, notifierTag).setSmallIcon(17302392).setContentIntent(this.mFrameworkFacade.getActivity(this.mContext, 0, intent, 134217728)).setAutoCancel(true).build();
    }

    private PendingIntent getPrivateBroadcast(String action, String extraData) {
        Intent intent = new Intent(action).setPackage("android");
        int requestCode = 0;
        if (extraData != null) {
            intent.putExtra(AVAILABLE_NETWORK_NOTIFIER_TAG, extraData);
            requestCode = getNotifierRequestCode(extraData);
        }
        return this.mFrameworkFacade.getBroadcast(this.mContext, requestCode, intent, 134217728);
    }
}
