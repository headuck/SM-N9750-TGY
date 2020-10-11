package com.samsung.android.server.wifi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Debug;
import android.os.Process;
import android.util.Log;

public class WifiMobileDeviceManager {
    private static final boolean DBG = Debug.semIsProductDev();
    public static final int EDM_FALSE = 0;
    public static final int EDM_NULL = -1;
    public static final int EDM_TRUE = 1;
    private static final String TAG = "WifiMobileDeviceManager";
    private static boolean mAllowToWifiStateChangeOnce = false;

    public static int getEnterprisePolicyEnabled(Context context, String edmUri, String projectionArgs) {
        Cursor cr = context.getContentResolver().query(Uri.parse(edmUri), (String[]) null, projectionArgs, (String[]) null, (String) null);
        if (cr == null) {
            return -1;
        }
        try {
            cr.moveToFirst();
            if (cr.getString(cr.getColumnIndex(projectionArgs)).equals("true")) {
                return 1;
            }
            cr.close();
            return 0;
        } catch (Exception e) {
            return -1;
        } finally {
            cr.close();
        }
    }

    public static int getEnterprisePolicyEnabled(Context context, String edmUri, String projectionArgs, String[] selectionArgs) {
        Cursor cr = context.getContentResolver().query(Uri.parse(edmUri), (String[]) null, projectionArgs, selectionArgs, (String) null);
        if (cr == null) {
            return -1;
        }
        try {
            cr.moveToFirst();
            if (cr.getString(cr.getColumnIndex(projectionArgs)).equals("true")) {
                return 1;
            }
            cr.close();
            return 0;
        } catch (Exception e) {
            return -1;
        } finally {
            cr.close();
        }
    }

    public static int getEnterprisePolicyEnabledInt(Context context, String edmUri, String projectionArgs, String[] selectionArgs) {
        int result = 0;
        Cursor cr = context.getContentResolver().query(Uri.parse(edmUri), (String[]) null, projectionArgs, selectionArgs, (String) null);
        if (cr != null) {
            try {
                cr.moveToFirst();
                result = cr.getInt(cr.getColumnIndex(projectionArgs));
            } catch (Exception e) {
            } catch (Throwable th) {
                cr.close();
                throw th;
            }
            cr.close();
        }
        return result;
    }

    public static void setEnterprisePolicyInt(Context context, String edmUri, String projectionArgs, int value) {
        Uri uri = Uri.parse(edmUri);
        ContentValues cv = new ContentValues();
        cv.put("API", projectionArgs);
        cv.put("flag", Integer.valueOf(value));
        context.getContentResolver().insert(uri, cv);
    }

    public static String getEnterprisePolicyStringValue(Context context, String edmUri, String projectionArgs, String[] selectionArgs) {
        Cursor cr = context.getContentResolver().query(Uri.parse(edmUri), (String[]) null, projectionArgs, selectionArgs, (String) null);
        if (cr == null) {
            return null;
        }
        try {
            cr.moveToFirst();
            return cr.getString(cr.getColumnIndex(projectionArgs));
        } catch (Exception e) {
            return null;
        } finally {
            cr.close();
        }
    }

    public static void auditLog(Context context, int group, boolean outcome, String component, String msg) {
        try {
            Uri uri = Uri.parse("content://com.sec.knox.provider/AuditLog");
            ContentValues cv = new ContentValues();
            cv.put("severity", 5);
            cv.put("group", Integer.valueOf(group));
            cv.put("outcome", Boolean.valueOf(outcome));
            cv.put("uid", Integer.valueOf(Process.myPid()));
            cv.put("component", component);
            cv.put("message", msg);
            context.getContentResolver().insert(uri, cv);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.toString());
        }
    }

    public static void allowToStateChange(boolean allowed) {
        mAllowToWifiStateChangeOnce = allowed;
    }

    public static boolean isAllowToUseWifi(Context context, boolean enabled) {
        String[] selectionArgs = {"false"};
        if (enabled && getEnterprisePolicyEnabled(context, "content://com.sec.knox.provider/RestrictionPolicy4", "isWifiEnabled", selectionArgs) == 0) {
            Log.w(TAG, "Wi-Fi is not allowed by Restriction Policy");
            return false;
        } else if (mAllowToWifiStateChangeOnce) {
            mAllowToWifiStateChangeOnce = false;
            return true;
        } else if (getEnterprisePolicyEnabled(context, "content://com.sec.knox.provider2/WifiPolicy", "isWifiStateChangeAllowed") != 0) {
            return true;
        } else {
            Log.w(TAG, "Wi-Fi state change is not allowed by Wifi Policy");
            return false;
        }
    }
}
