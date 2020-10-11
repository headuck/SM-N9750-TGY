package com.android.server.wifi.tcp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.util.Slog;

public class WifiTransportLayerNotificationManager {
    public static final int NOTIFICATION_TYPE_AUTO_SWITCH = 3;
    public static final int NOTIFICATION_TYPE_DETECTED = 2;
    public static final int NOTIFICATION_TYPE_SUGGESTION = 1;
    private static final String TAG = "WifiTransportLayerNotificationManager";
    private final String NOTIFICATION_CHANNEL_ID_DEFAULT = "WifiTransportLayerNotificationManager_DEFAULT";
    private final String NOTIFICATION_CHANNEL_ID_HIGH = "WifiTransportLayerNotificationManager_HIGH";
    private final int TCP_MONITOR_HUN_ID = 17042697;
    private final int TCP_MONITOR_NOTIFICATION_ID = 17042702;
    private final int TCP_MONITOR_SUGGESTION_ID = 17042706;
    private BroadcastReceiver mBroadcastReceiver;
    /* access modifiers changed from: private */
    public String mChannelNameEmergency;
    /* access modifiers changed from: private */
    public String mChannelNameGeneral;
    /* access modifiers changed from: private */
    public final Context mContext;
    private NotificationManager mNotificationManager;
    private PackageManager mPackageManager;

    public WifiTransportLayerNotificationManager(Context context) {
        this.mContext = context;
        this.mChannelNameGeneral = this.mContext.getResources().getString(17042706) + "(" + this.mContext.getResources().getString(17042695) + ")";
        this.mChannelNameEmergency = this.mContext.getResources().getString(17042706) + "(" + this.mContext.getResources().getString(17042694) + ")";
        initNotificationChannel();
        setBroadCastReceiver();
    }

    private NotificationManager getNotificationManager() {
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        }
        return this.mNotificationManager;
    }

    public void showNotification(int type, int uid, String packageName) {
        if (type == 1) {
            showTcpMonitorSuggestionNotification(true, packageName, uid);
        } else if (type == 2) {
            showTcpMonitorHeadUpNotification(true, uid, packageName);
        } else if (type == 3) {
            showTcpMonitorNotification(true, uid, packageName);
        }
    }

    public void removeNotification(int type) {
        if (type == 1) {
            showTcpMonitorSuggestionNotification(false, (String) null, 0);
        } else if (type == 2) {
            showTcpMonitorHeadUpNotification(false, 0, (String) null);
        } else if (type == 3) {
            showTcpMonitorNotification(false, 0, (String) null);
        }
    }

    public void clearNotificationAll() {
        showTcpMonitorSuggestionNotification(false, (String) null, 0);
        showTcpMonitorHeadUpNotification(false, 0, (String) null);
        showTcpMonitorNotification(false, 0, (String) null);
    }

    private boolean showTcpMonitorNotification(boolean visible, int uid, String packageName) {
        boolean z = visible;
        String str = packageName;
        Slog.d(TAG, "showTcpMonitorNotification: " + z + ", " + uid + ", " + str);
        getNotificationManager().cancel(17042702);
        if (!z) {
            return true;
        }
        try {
            String title = this.mContext.getResources().getString(17042702, new Object[]{getAppLabel(str)});
            String detail = this.mContext.getResources().getString(17042699, new Object[]{getAppLabel(str)});
            Notification.Action actionSettings = new Notification.Action.Builder((Icon) null, this.mContext.getResources().getString(17042705), PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.samsung.android.net.wifi.WIFI_TCP_MONITOR_ACTION_SETTINGS"), 134217728)).build();
            Intent deleteIntent = new Intent("com.samsung.android.net.wifi.WIFI_TCP_MONITOR_DELETE_NOTIFICATION");
            deleteIntent.putExtra("reason", 3);
            Notification notification = new Notification.Builder(this.mContext, "WifiTransportLayerNotificationManager_DEFAULT").setSmallIcon(17304155).setStyle(new Notification.BigTextStyle().bigText(detail)).setAutoCancel(true).setTicker(title).setContentTitle(title).setContentText(detail).addAction(actionSettings).setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, deleteIntent, 134217728)).build();
            notification.when = System.currentTimeMillis();
            getNotificationManager().notify(17042702, notification);
            return true;
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean showTcpMonitorHeadUpNotification(boolean visible, int uid, String packageName) {
        boolean z = visible;
        int i = uid;
        String str = packageName;
        Slog.d(TAG, "showTcpMonitorHeadUpNotification: " + z + ", " + i + ", " + str);
        getNotificationManager().cancel(17042697);
        if (!z) {
            return true;
        }
        try {
            String title = this.mContext.getResources().getString(17042697, new Object[]{getAppLabel(str)});
            String detail = this.mContext.getResources().getString(17042696, new Object[]{getAppLabel(str)});
            Intent useMobileDataIntent = new Intent("com.samsung.android.net.wifi.TCP_MONITOR_ACTION_USE_MOBILE_DATA");
            useMobileDataIntent.putExtra("uid", i);
            useMobileDataIntent.putExtra("packageName", str);
            Notification.Action actionUseMobileData = new Notification.Action.Builder((Icon) null, this.mContext.getResources().getString(17042698), PendingIntent.getBroadcast(this.mContext, 0, useMobileDataIntent, 134217728)).build();
            Intent deleteIntent = new Intent("com.samsung.android.net.wifi.WIFI_TCP_MONITOR_DELETE_NOTIFICATION");
            deleteIntent.putExtra("reason", 2);
            Notification.Builder notiBuilder = new Notification.Builder(this.mContext, "WifiTransportLayerNotificationManager_HIGH").setSmallIcon(17304155).setStyle(new Notification.BigTextStyle().bigText(detail)).setAutoCancel(true).setTicker(title).setContentTitle(title).setContentText(detail).addAction(actionUseMobileData).setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, deleteIntent, 134217728));
            Notification notification = notiBuilder.build();
            Notification.Builder builder = notiBuilder;
            notification.when = System.currentTimeMillis();
            getNotificationManager().notify(17042697, notification);
            return true;
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean showTcpMonitorSuggestionNotification(boolean visible, String packageName, int uid) {
        boolean z = visible;
        String str = packageName;
        Slog.d(TAG, "showTcpMonitorSuggestionNotification: " + z + ", " + str);
        getNotificationManager().cancel(17042706);
        if (!z) {
            return true;
        }
        try {
            String title = this.mContext.getResources().getString(17042706);
            String detail = this.mContext.getResources().getString(17042704, new Object[]{getAppLabel(str)});
            Intent intentSettings = new Intent("com.samsung.android.net.wifi.WIFI_TCP_MONITOR_ACTION_SETTINGS");
            try {
                intentSettings.putExtra("UID", uid);
                PendingIntent pendingContentIntent = PendingIntent.getBroadcast(this.mContext, 0, intentSettings, 134217728);
                Notification.Action actionSettings = new Notification.Action.Builder((Icon) null, this.mContext.getResources().getString(17042705), pendingContentIntent).build();
                Intent deleteIntent = new Intent("com.samsung.android.net.wifi.WIFI_TCP_MONITOR_DELETE_NOTIFICATION");
                deleteIntent.putExtra("reason", 1);
                Notification notification = new Notification.Builder(this.mContext, "WifiTransportLayerNotificationManager_HIGH").setSmallIcon(17304155).setStyle(new Notification.BigTextStyle().bigText(detail)).setAutoCancel(true).setTicker(title).setContentTitle(title).setContentText(detail).addAction(actionSettings).setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, deleteIntent, 134217728)).setContentIntent(pendingContentIntent).build();
                notification.when = System.currentTimeMillis();
                getNotificationManager().notify(17042706, notification);
                return true;
            } catch (Resources.NotFoundException e) {
                e = e;
                e.printStackTrace();
                return false;
            }
        } catch (Resources.NotFoundException e2) {
            e = e2;
            int i = uid;
            e.printStackTrace();
            return false;
        }
    }

    private PackageManager getPackageManager() {
        if (this.mPackageManager == null) {
            this.mPackageManager = this.mContext.getPackageManager();
        }
        return this.mPackageManager;
    }

    private String getAppLabel(String packageName) {
        try {
            return (String) getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(packageName, 128));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    /* access modifiers changed from: private */
    public void initNotificationChannel() {
        NotificationChannel channelHigh = new NotificationChannel("WifiTransportLayerNotificationManager_HIGH", this.mChannelNameEmergency, 4);
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channelHigh);
        notificationManager.createNotificationChannel(new NotificationChannel("WifiTransportLayerNotificationManager_DEFAULT", this.mChannelNameGeneral, 3));
    }

    private void setBroadCastReceiver() {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.LOCALE_CHANGED")) {
                    WifiTransportLayerNotificationManager wifiTransportLayerNotificationManager = WifiTransportLayerNotificationManager.this;
                    String unused = wifiTransportLayerNotificationManager.mChannelNameGeneral = WifiTransportLayerNotificationManager.this.mContext.getResources().getString(17042706) + "(" + WifiTransportLayerNotificationManager.this.mContext.getResources().getString(17042695) + ")";
                    WifiTransportLayerNotificationManager wifiTransportLayerNotificationManager2 = WifiTransportLayerNotificationManager.this;
                    String unused2 = wifiTransportLayerNotificationManager2.mChannelNameEmergency = WifiTransportLayerNotificationManager.this.mContext.getResources().getString(17042706) + "(" + WifiTransportLayerNotificationManager.this.mContext.getResources().getString(17042694) + ")";
                    WifiTransportLayerNotificationManager.this.initNotificationChannel();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }
}
