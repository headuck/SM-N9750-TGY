package com.samsung.android.server.wifi;

import android.app.ActivityOptions;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.UserHandle;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.android.server.wifi.WifiGeofenceDBHelper;
import java.util.UnknownFormatConversionException;

public class SemWifiFrameworkUxUtils {
    public static final String ACTION_WIFI_INFO = "com.samsung.android.net.wifi.SHOW_INFO_MESSAGE";
    private static final int EAP_NOTIFICATION_VERIZON_WIFI_ERROR_ACCESS_ACCOUNT = 32762;
    private static final int EAP_NOTIFICATION_VERIZON_WIFI_ERROR_ACCESS_ACCOUNT2 = 32765;
    private static final int EAP_NOTIFICATION_VERIZON_WIFI_ERROR_ALREADY_CONNECTED = 32763;
    private static final int EAP_NOTIFICATION_VERIZON_WIFI_ERROR_CONNECTING = 32764;
    private static final int EAP_NOTIFICATION_VERIZON_WIFI_ERROR_NOT_AVAILABLE_LOCATION = 32766;
    private static final int EAP_NOTIFICATION_VERIZON_WIFI_ERROR_NOT_SUBSCRIBED = 32761;
    private static final int EAP_NOTIFICATION_VERIZON_WIFI_ERROR_OUTSIDE_COVERAGE = 32760;
    private static final int EXTRA_TYPE_SHARING_LITE = 5;
    private static final int EXTRA_TYPE_WIFI = 1;
    public static final int INFO_TYPE_DISABLE_HOTSPOT = 1;
    public static final int INFO_TYPE_DISCONNECT_TOAST = 30;
    public static final int INFO_TYPE_DPM_HOTSPOT = 3;
    public static final int INFO_TYPE_DPM_WIFI = 2;
    public static final int INFO_TYPE_EDM_HOTSPOT = 10;
    public static final int INFO_TYPE_NETWORK_CONNECT_FAILED = 70;
    public static final int INFO_TYPE_NETWORK_NOT_FOUND = 50;
    public static final int INFO_TYPE_NETWORK_TEMPORARILY_DISABLED = 60;
    public static final int INFO_TYPE_UNABLE_TO_TURNON_IBSS = 6;
    public static final int INFO_TYPE_UNABLE_TO_TURNON_WIFI = 5;
    public static final int INFO_TYPE_UNABLE_TO_TURNON_WIFI_AT_AIRPLANE_MODE = 7;
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    public static final int SIM_GENERAL_FAILURE_AFTER_AUTH = 0;
    public static final int SIM_GENERAL_FAILURE_BEFORE_AUTH = 16384;
    public static final int SIM_NOT_SUBSCRIBED = 1031;
    public static final int SIM_TEMPORARILY_DENIED = 1026;
    private static final String SYSTEM_UI_PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "WifiFrameworkUx";
    public static final int WARN_CONFIRM_TO_DISABLE_HOTSPOT = 4;
    public static final int WARN_CONFIRM_TO_DISABLE_SHARING_LITE = 5;
    public static final int WARN_SIM_REMOVED = 2;
    public static final int WARN_SIM_REMOVED_WHEN_CONNECTED = 3;
    public static final int WARN_WIFI_ENABLED_BY_3RDPARTY_APK = 1;

    public static void sendShowInfoIntentToSettings(Context context, int infoType, int networkId, int reason) {
        Intent intent = new Intent(ACTION_WIFI_INFO);
        intent.putExtra("info_type", infoType);
        intent.putExtra(WifiGeofenceDBHelper.KEY_NETWORKID, networkId);
        intent.putExtra("reason", reason);
        intent.setPackage(SETTINGS_PACKAGE_NAME);
        sendBroadcastToCurrentUser(context, intent);
    }

    public static void sendBroadcastToCurrentUser(Context context, Intent intent) {
        if (context != null) {
            try {
                context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                Log.e(TAG, "sendBroadcastToCurrentUser IllegalStateException occured.");
            }
        }
    }

    public static String removeDoubleQuotes(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    public static void showEapToast(Context context, int eapCode) {
        Log.i(TAG, "showEapToast. eapCode:" + eapCode);
        if (eapCode != 0) {
            if (eapCode == 1031) {
                Toast.makeText(context, 17042622, 1).show();
                return;
            } else if (eapCode != 16384) {
                Toast.makeText(context, 17042742, 1).show();
                return;
            }
        }
        Toast.makeText(context, 17042616, 1).show();
    }

    public static void showEapToastVzw(Context context, int eapCode) {
        Log.i(TAG, "showEapToast for Verizon. eapCode:" + eapCode);
        switch (eapCode) {
            case EAP_NOTIFICATION_VERIZON_WIFI_ERROR_OUTSIDE_COVERAGE /*32760*/:
                Toast.makeText(context, 17042614, 1).show();
                return;
            case EAP_NOTIFICATION_VERIZON_WIFI_ERROR_NOT_SUBSCRIBED /*32761*/:
                Toast.makeText(context, 17042613, 1).show();
                return;
            case EAP_NOTIFICATION_VERIZON_WIFI_ERROR_ACCESS_ACCOUNT /*32762*/:
            case EAP_NOTIFICATION_VERIZON_WIFI_ERROR_ACCESS_ACCOUNT2 /*32765*/:
                Toast.makeText(context, 17042615, 1).show();
                return;
            case EAP_NOTIFICATION_VERIZON_WIFI_ERROR_ALREADY_CONNECTED /*32763*/:
                Toast.makeText(context, 17042611, 1).show();
                return;
            case EAP_NOTIFICATION_VERIZON_WIFI_ERROR_CONNECTING /*32764*/:
                Toast.makeText(context, 17042612, 1).show();
                return;
            case EAP_NOTIFICATION_VERIZON_WIFI_ERROR_NOT_AVAILABLE_LOCATION /*32766*/:
                Toast.makeText(context, 17042621, 1).show();
                return;
            default:
                Toast.makeText(context, 17042618, 1).show();
                return;
        }
    }

    public static void showToast(Context context, int infoType, String extraString) {
        if (infoType == 1) {
            Toast.makeText(context, 17042602, 0).show();
        } else if (infoType == 2) {
            Toast.makeText(context, 17040589, 0).show();
        } else if (infoType == 3) {
            Toast.makeText(context, 17040590, 0).show();
        } else if (infoType == 5) {
            Toast.makeText(context, 17042640, 0).show();
        } else if (infoType == 6) {
            Toast.makeText(context, 17040196, 0).show();
        } else if (infoType == 7) {
            Toast.makeText(context, 17042648, 0).show();
        } else if (infoType == 10) {
            Toast.makeText(context, 17040195, 0).show();
        } else if (infoType == 30) {
            Toast toast = Toast.makeText(context, 17042639, 0);
            ((TextView) toast.getView().findViewById(16908299)).setGravity(17);
            toast.show();
        } else if (infoType == 50 && extraString != null) {
            Resources res = context.getResources();
            String colorText = res.getString(17171465).replaceAll("#ff", "#");
            if ("#DEAD00".equals(colorText)) {
                Toast.makeText(context, res.getString(17042658, new Object[]{extraString}), 0).show();
                return;
            }
            try {
                Toast toast2 = Toast.makeText(context, Html.fromHtml(res.getString(17042658, new Object[]{"<font color=" + colorText + ">" + Html.escapeHtml(extraString) + "</font>"})), 0);
                ((TextView) toast2.getView().findViewById(16908299)).setGravity(17);
                toast2.show();
                Log.d(TAG, "Show NETWORK_NOT_FOUND Toast");
            } catch (UnknownFormatConversionException e) {
                Log.d(TAG, e.toString());
            }
        }
    }

    private static Intent getWifiWarningIntent() {
        Intent intent = new Intent();
        intent.setClassName(SETTINGS_PACKAGE_NAME, "com.samsung.android.settings.wifi.WifiWarning");
        intent.setFlags(343932928);
        return intent;
    }

    private static Intent getWifiWarningDialogIntent(String dialogType) {
        Intent intent = new Intent();
        intent.setClassName(SETTINGS_PACKAGE_NAME, "com.samsung.android.settings.wifi.WifiWarningDialog");
        intent.setFlags(343932928);
        if (dialogType != null) {
            intent.putExtra("dialog_type", dialogType);
        }
        return intent;
    }

    private static void collapsePanels(Context context) {
        StatusBarManager statusBar = (StatusBarManager) context.getSystemService("statusbar");
        if (statusBar != null) {
            statusBar.collapsePanels();
        }
    }

    private static void showUserConfirmDialog(Context context, String callingPackage, int extraType) {
        "com.android.systemui".equals(callingPackage);
        Intent intent = getWifiWarningIntent();
        intent.putExtra("req_type", 0);
        intent.putExtra("extra_type", extraType);
        startActivityOnDisplay(context, intent, 0);
    }

    public static void showWarningDialog(Context context, int type, String[] params) {
        Log.i(TAG, "Show warning dialog, type: " + type);
        if (type == 1) {
            String packageName = params[0];
            String label = packageName;
            PackageManager pm = context.getPackageManager();
            try {
                label = (String) pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0));
            } catch (PackageManager.NameNotFoundException e) {
            }
            Log.i(TAG, "label :" + label);
            Intent intent = getWifiWarningDialogIntent("wlan_enable_warning");
            intent.putExtra("android.intent.extra.PACKAGE_NAME", packageName);
            intent.putExtra("dialog_name", label);
            Log.i(TAG, "WifiManager.setWifiEnabled Prepare to stat activity: WifiWarningDialog");
            try {
                startActivityAsUser(context, intent, "com.android.systemui".equals(packageName) ? UserHandle.CURRENT : new UserHandle(UserHandle.myUserId()));
            } catch (ActivityNotFoundException e2) {
                Log.e(TAG, "ActivityNotFoundException occured.");
            }
        } else if (type == 2) {
            Intent intent2 = getWifiWarningDialogIntent("sim_removed_warning");
            intent2.putExtra("dialog_name", params[0]);
            startActivity(context, intent2);
        } else if (type == 3) {
            Intent intent3 = getWifiWarningDialogIntent("sim_removed_warning_when_connected");
            intent3.putExtra("dialog_name", params[0]);
            startActivity(context, intent3);
        } else if (type == 4) {
            collapsePanels(context);
            showUserConfirmDialog(context, params[0], 1);
        } else if (type == 5) {
            collapsePanels(context);
            showUserConfirmDialog(context, params[0], 5);
        }
    }

    private static void startActivityOnDisplay(Context context, Intent intent, int displayId) {
        try {
            ActivityOptions.makeBasic().setLaunchDisplayId(displayId);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "ActivityNotFoundException occured.");
        }
    }

    private static void startActivityAsUser(Context context, Intent intent, UserHandle userHandle) {
        try {
            context.startActivityAsUser(intent, userHandle);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "ActivityNotFoundException occured.");
        }
    }

    private static void startActivity(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "ActivityNotFoundException occured.");
        }
    }
}
