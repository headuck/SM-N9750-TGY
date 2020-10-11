package com.samsung.android.server.wifi;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.server.wifi.FrameworkFacade;

public class WifiPickerController {
    private static final String TAG = "WifiPickerController";
    private int mChinaConnectionType;
    private boolean mChinaDoNotShowAgain;
    private final Context mContext;
    private boolean mRunning;

    public WifiPickerController(Context context, FrameworkFacade frameworkFacade) {
        this.mContext = context;
    }

    public void setEnable(boolean enabled) {
        this.mRunning = enabled;
        if (enabled) {
            Log.i(TAG, "start tracking");
        } else {
            Log.i(TAG, "stop straking");
        }
    }

    public void showPickerDialogIfNecessary() {
        if (this.mRunning) {
            this.mRunning = false;
            startPickerDialog();
        }
    }

    private void startPickerDialog() {
        Log.i(TAG, "starting Wi-Fi picker dialog");
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.samsung.android.settings.wifi.WifiPickerDialog");
        intent.addFlags(268468224);
        try {
            this.mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "can not show Wi-Fi picker dialog " + e.toString());
        }
    }
}
