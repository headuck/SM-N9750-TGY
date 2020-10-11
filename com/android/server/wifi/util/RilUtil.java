package com.android.server.wifi.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.SystemProperties;
import android.provider.Settings;
import com.samsung.android.feature.SemCscFeature;
import com.sec.android.app.CscFeatureTagRIL;

public class RilUtil {
    public static final String TAG = "RilUtil";

    private static boolean isMptcpSupported() {
        if (!SemCscFeature.getInstance().getBoolean(CscFeatureTagRIL.TAG_CSCFEATURE_RIL_SUPPORTMPTCP, false) || !"1".equals(SystemProperties.get("ro.supportmodel.mptcp"))) {
            return false;
        }
        return true;
    }

    public static boolean isMptcpEnabled(Context context) {
        if (!isMptcpSupported() || Settings.System.getInt(context.getContentResolver(), "mptcp_value_internal", 0) != 1) {
            return false;
        }
        return true;
    }

    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (cm == null || !cm.isNetworkSupported(0)) {
            return true;
        }
        return false;
    }
}
