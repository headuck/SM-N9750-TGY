package com.android.server.wifi;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import com.android.internal.notification.SystemNotificationChannels;

public class WakeupNotificationFactory {
    public static final String ACTION_DISMISS_NOTIFICATION = "com.android.server.wifi.wakeup.DISMISS_NOTIFICATION";
    public static final String ACTION_OPEN_WIFI_PREFERENCES = "com.android.server.wifi.wakeup.OPEN_WIFI_PREFERENCES";
    public static final String ACTION_OPEN_WIFI_SETTINGS = "com.android.server.wifi.wakeup.OPEN_WIFI_SETTINGS";
    public static final String ACTION_TURN_OFF_WIFI_WAKE = "com.android.server.wifi.wakeup.TURN_OFF_WIFI_WAKE";
    public static final int ONBOARD_ID = 43;
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;

    WakeupNotificationFactory(Context context, FrameworkFacade frameworkFacade) {
        this.mContext = context;
        this.mFrameworkFacade = frameworkFacade;
    }

    public Notification createOnboardingNotification() {
        CharSequence title = this.mContext.getText(17042748);
        CharSequence content = this.mContext.getText(17042747);
        CharSequence disableText = this.mContext.getText(17042746);
        return this.mFrameworkFacade.makeNotificationBuilder(this.mContext, SystemNotificationChannels.NETWORK_STATUS).setSmallIcon(17302996).setTicker(title).setContentTitle(title).setContentText(content).setContentIntent(getPrivateBroadcast(ACTION_OPEN_WIFI_PREFERENCES)).setDeleteIntent(getPrivateBroadcast(ACTION_DISMISS_NOTIFICATION)).addAction(new Notification.Action.Builder((Icon) null, disableText, getPrivateBroadcast(ACTION_TURN_OFF_WIFI_WAKE)).build()).setShowWhen(false).setLocalOnly(true).setColor(this.mContext.getResources().getColor(17170460, this.mContext.getTheme())).build();
    }

    private PendingIntent getPrivateBroadcast(String action) {
        return this.mFrameworkFacade.getBroadcast(this.mContext, 0, new Intent(action).setPackage("android"), 134217728);
    }
}
