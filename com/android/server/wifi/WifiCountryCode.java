package com.android.server.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.samsung.android.feature.SemCscFeature;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WifiCountryCode {
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final String TAG = "WifiCountryCode";
    private boolean DBG = false;
    private ConnectivityManager mCm = null;
    private final Context mContext;
    private String mDefaultCountryCode = null;
    private String mDriverCountryCode = null;
    private String mDriverCountryTimestamp = null;
    private int mIsWifiOnly = -1;
    private boolean mNeedToUpdateCountryCode = false;
    private boolean mReady = false;
    private String mReadyTimestamp = null;
    private boolean mRevertCountryCodeOnCellularLoss;
    private String mTelephonyCountryCode = null;
    private String mTelephonyCountryTimestamp = null;
    private final WifiNative mWifiNative;

    public WifiCountryCode(WifiNative wifiNative, Context context, String oemDefaultCountryCode, boolean revertCountryCodeOnCellularLoss) {
        this.mWifiNative = wifiNative;
        this.mContext = context;
        this.mRevertCountryCodeOnCellularLoss = revertCountryCodeOnCellularLoss;
        if (!TextUtils.isEmpty(oemDefaultCountryCode)) {
            this.mDefaultCountryCode = oemDefaultCountryCode.toUpperCase(Locale.US);
        } else if (this.mRevertCountryCodeOnCellularLoss) {
            Log.w(TAG, "config_wifi_revert_country_code_on_cellular_loss is set, but there is no default country code.");
            this.mRevertCountryCodeOnCellularLoss = false;
        }
        if (this.mDefaultCountryCode == null) {
            this.mDefaultCountryCode = SystemProperties.get("ro.csc.countryiso_code", "");
        }
        Log.d(TAG, "mDefaultCountryCode " + this.mDefaultCountryCode + " mRevertCountryCodeOnCellularLoss " + this.mRevertCountryCodeOnCellularLoss);
    }

    public void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.DBG = true;
        } else {
            this.DBG = false;
        }
    }

    public synchronized void airplaneModeEnabled() {
        Log.d(TAG, "Airplane Mode Enabled");
        if (this.mRevertCountryCodeOnCellularLoss) {
            this.mTelephonyCountryCode = null;
        }
    }

    public synchronized void setReadyForChange(boolean ready) {
        if (this.DBG) {
            Log.d(TAG, "Set ready: " + ready + ", update:" + this.mNeedToUpdateCountryCode);
        }
        this.mReady = ready;
        this.mReadyTimestamp = FORMATTER.format(new Date(System.currentTimeMillis()));
        if (!ready) {
            this.mNeedToUpdateCountryCode = false;
        }
        if (this.mReady && this.mNeedToUpdateCountryCode) {
            updateCountryCode();
        }
    }

    public synchronized void setReadyForChangeAndUpdate(boolean ready) {
        if (this.DBG) {
            Log.e(TAG, "Set ready: " + ready + ", update:" + this.mNeedToUpdateCountryCode);
        }
        this.mReady = ready;
        this.mReadyTimestamp = FORMATTER.format(new Date(System.currentTimeMillis()));
        if (this.mReady) {
            updateCountryCode();
        }
    }

    public synchronized boolean setCountryCode(String countryCode) {
        Log.d(TAG, "Receive set country code request: " + countryCode);
        this.mTelephonyCountryTimestamp = FORMATTER.format(new Date(System.currentTimeMillis()));
        if (!TextUtils.isEmpty(countryCode)) {
            this.mTelephonyCountryCode = countryCode.toUpperCase(Locale.US);
        } else if (this.mRevertCountryCodeOnCellularLoss) {
            Log.d(TAG, "Received empty country code, reset to default country code");
            this.mTelephonyCountryCode = null;
        }
        if (this.mReady) {
            updateCountryCode();
        } else {
            Log.d(TAG, "skip update supplicant not ready yet");
        }
        return true;
    }

    public synchronized String getCountryCodeSentToDriver() {
        return this.mDriverCountryCode;
    }

    public synchronized String getCountryCode() {
        return pickCountryCode();
    }

    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("mRevertCountryCodeOnCellularLoss: " + this.mRevertCountryCodeOnCellularLoss);
        pw.println("mDefaultCountryCode: " + this.mDefaultCountryCode);
        pw.println("mDriverCountryCode: " + this.mDriverCountryCode);
        pw.println("mTelephonyCountryCode: " + this.mTelephonyCountryCode);
        pw.println("mTelephonyCountryTimestamp: " + this.mTelephonyCountryTimestamp);
        pw.println("mDriverCountryTimestamp: " + this.mDriverCountryTimestamp);
        pw.println("mReadyTimestamp: " + this.mReadyTimestamp);
        pw.println("mReady: " + this.mReady);
    }

    private void updateCountryCode() {
        String country = pickCountryCode();
        Log.d(TAG, "updateCountryCode to " + country);
        if (country != null) {
            setCountryCodeNative(country, false);
        } else if (isWifiOnly()) {
            String mCountryCodeFromCsc = SystemProperties.get("ro.csc.countryiso_code");
            if (mCountryCodeFromCsc != null && !mCountryCodeFromCsc.isEmpty()) {
                setCountryCodeNative(mCountryCodeFromCsc, false);
            }
        } else {
            Log.e(TAG, "country == null && !isWifiOnly()");
        }
    }

    private String pickCountryCode() {
        String str = this.mTelephonyCountryCode;
        if (str != null) {
            return str;
        }
        String str2 = this.mDefaultCountryCode;
        if (str2 != null) {
            return str2;
        }
        return null;
    }

    public boolean setCountryCodeNative(String country, boolean setDefaultCountry) {
        if (country == null) {
            Log.d(TAG, "[setCountryCodeNative] country == null");
            return false;
        }
        String country2 = country.toUpperCase();
        String str = "";
        if (country2 != null && country2.contains("FACTORY")) {
            Log.d(TAG, "[setCountryCodeNative-FACTORY] WlanTest - setCountryCode() !!");
            country2 = country2.replace("FACTORY", str);
        } else if ("TN".equals(SemCscFeature.getInstance().getString("CountryISO")) || "IL".equals(SemCscFeature.getInstance().getString("CountryISO")) || setDefaultCountry) {
            String temp = SystemProperties.get("ro.csc.countryiso_code");
            if (temp != null) {
                temp = temp.trim();
            }
            if (!TextUtils.isEmpty(temp)) {
                str = temp;
            }
            country2 = str;
            Log.d(TAG, "[setCountryCodeNative] Default CSC Country Code : " + temp);
        }
        this.mDriverCountryTimestamp = FORMATTER.format(new Date(System.currentTimeMillis()));
        WifiNative wifiNative = this.mWifiNative;
        if (wifiNative.setCountryCode(wifiNative.getClientInterfaceName(), country2)) {
            Log.d(TAG, "Succeeded to set country code to: " + country2);
            this.mTelephonyCountryCode = country2.toUpperCase();
            this.mDriverCountryCode = country2;
            return true;
        }
        Log.d(TAG, "Failed to set country code to: " + country2);
        return false;
    }

    public boolean isWifiOnly() {
        if (this.mIsWifiOnly == -1) {
            checkAndSetConnectivityInstance();
            if (this.mCm.isNetworkSupported(0)) {
                this.mIsWifiOnly = 0;
            } else {
                this.mIsWifiOnly = 1;
            }
        }
        if (this.mIsWifiOnly == 1) {
            return true;
        }
        return false;
    }

    private void checkAndSetConnectivityInstance() {
        if (this.mCm == null) {
            this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }
}
