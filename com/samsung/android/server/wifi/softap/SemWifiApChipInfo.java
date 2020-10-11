package com.samsung.android.server.wifi.softap;

import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SemWifiApChipInfo {
    private static final boolean MHSDBG = ("eng".equals(Build.TYPE) || Debug.semIsProductDev());
    public static String SoftAp_HalFn_getValidChannels = "na";
    public static String SoftAp_HalFn_setCountryCodeHal = "na";
    public static String SoftAp_MaxClient = "na";
    public static String SoftAp_PowerSave = "na";
    public static String SoftAp_Support5g = "na";
    public static String SoftAp_Support5gBasedOnCountry = "na";
    public static String SoftAp_SupportDualInterface = "na";
    public static String SoftAp_SupportWifiSharing = "na";
    private static final String TAG = "SemWifiApChipInfo";
    public static int mCount = 0;
    public static boolean mIsReadSoftApInfo = false;
    public static boolean mSupportWifiSharing = false;
    public static boolean mSupportWifiSharingLite = false;
    private Context mContext;
    private List<String> mMHSDumpLogs = new ArrayList();

    public SemWifiApChipInfo(Context context) {
        this.mContext = context;
    }

    private void addMHSDumpLog(String log) {
        StringBuffer value = new StringBuffer();
        Log.i(TAG, log + " mhs: " + this.mMHSDumpLogs.size());
        value.append(new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Long.valueOf(System.currentTimeMillis())) + " " + log + "\n");
        if (this.mMHSDumpLogs.size() > 100) {
            this.mMHSDumpLogs.remove(0);
        }
        this.mMHSDumpLogs.add(value.toString());
    }

    public String getDumpLogs() {
        StringBuffer retValue = new StringBuffer();
        retValue.append("--WifiApChipInfo\n");
        retValue.append("WifiSharing:" + SoftAp_SupportWifiSharing + "\n");
        retValue.append("maxClient:" + SoftAp_MaxClient + "\n");
        retValue.append("5g:" + SoftAp_Support5g + "\n");
        retValue.append("5gBasedOnCountry:" + SoftAp_Support5gBasedOnCountry + "\n");
        retValue.append("PowerSave:" + SoftAp_PowerSave + "\n");
        retValue.append("DualInterface:" + SoftAp_SupportDualInterface + "\n");
        retValue.append("---softap.info:\n");
        retValue.append(this.mMHSDumpLogs.toString());
        return retValue.toString();
    }

    public String readSoftApInfo(String info) {
        IOException e;
        StringBuilder sb;
        String str = info;
        BufferedReader br = null;
        StringBuffer retValue = new StringBuffer();
        if (str == null) {
            addMHSDumpLog("softap.info path is null.");
            mIsReadSoftApInfo = true;
            return "";
        }
        try {
            br = new BufferedReader(new StringReader(str));
            Object obj = "";
            mIsReadSoftApInfo = true;
            while (true) {
                String readLine = br.readLine();
                String line = readLine;
                if (readLine == null) {
                    break;
                }
                if (line != null) {
                    if (!"".equals(line.trim())) {
                        addMHSDumpLog("[" + line + "]");
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("readSoftApInfo :");
                        sb2.append(line);
                        Log.d(TAG, sb2.toString());
                        retValue.append(line + "\n");
                        if (!line.startsWith("#")) {
                            int index = line.indexOf(61);
                            if (index != -1) {
                                String attr = line.substring(0, index);
                                String value = line.substring(index + 1, line.length());
                                if (attr.equals("DualBandConcurrency")) {
                                    if (value.equals("yes")) {
                                        SoftAp_SupportWifiSharing = "true";
                                    } else {
                                        SoftAp_SupportWifiSharing = "false";
                                    }
                                } else if (attr.equals("5G")) {
                                    if (value.equals("yes")) {
                                        SoftAp_Support5g = "true";
                                    } else {
                                        SoftAp_Support5g = "false";
                                    }
                                } else if (attr.equals("maxClient")) {
                                    SoftAp_MaxClient = value;
                                } else if (attr.equals("HalFn_setCountryCodeHal")) {
                                    if (value.equals("yes")) {
                                        SoftAp_HalFn_setCountryCodeHal = "true";
                                    } else {
                                        SoftAp_HalFn_setCountryCodeHal = "false";
                                    }
                                } else if (attr.equals("HalFn_getValidChannels")) {
                                    if (value.equals("yes")) {
                                        SoftAp_HalFn_getValidChannels = "true";
                                    } else {
                                        SoftAp_HalFn_getValidChannels = "false";
                                    }
                                } else if (attr.equals("PowerSave")) {
                                    if (value.equals("yes")) {
                                        SoftAp_PowerSave = "true";
                                    } else {
                                        SoftAp_PowerSave = "false";
                                    }
                                } else if (attr.equals("DualInterface")) {
                                    if (value.equals("yes")) {
                                        SoftAp_SupportDualInterface = "true";
                                    } else {
                                        SoftAp_SupportDualInterface = "false";
                                    }
                                }
                            }
                        }
                    }
                }
                String str2 = info;
            }
            if (SoftAp_HalFn_setCountryCodeHal.equals("true") && SoftAp_HalFn_getValidChannels.equals("true")) {
                SoftAp_Support5gBasedOnCountry = "true";
            } else if (SoftAp_HalFn_setCountryCodeHal.equals("na") && SoftAp_HalFn_getValidChannels.equals("na")) {
                SoftAp_Support5gBasedOnCountry = "na";
            }
            Settings.Secure.putString(this.mContext.getContentResolver(), "wifi_ap_chip_maxclient", SoftAp_MaxClient);
            Settings.Secure.putString(this.mContext.getContentResolver(), "wifi_ap_chip_support5g", SoftAp_Support5g);
            Settings.Secure.putString(this.mContext.getContentResolver(), "wifi_ap_chip_support5g_baseon_country", SoftAp_Support5gBasedOnCountry);
            checkWifiSharing();
            try {
                br.close();
            } catch (IOException e2) {
                e = e2;
                sb = new StringBuilder();
            }
        } catch (Exception e3) {
            Log.i(TAG, "Exception " + e3);
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e4) {
                    e = e4;
                    sb = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            Throwable th2 = th;
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e5) {
                    Log.i(TAG, "IOException " + e5);
                }
            }
            throw th2;
        }
        return retValue.toString();
        sb.append("IOException ");
        sb.append(e);
        Log.i(TAG, sb.toString());
        return retValue.toString();
    }

    public void checkWifiSharing() {
        int testapilevel;
        int first_api_level = SystemProperties.getInt("ro.product.first_api_level", -1);
        if (MHSDBG && (testapilevel = SystemProperties.getInt("mhs.first_api_level", -1)) != -1) {
            first_api_level = testapilevel;
        }
        if (SoftAp_SupportWifiSharing.equals("true") && SoftAp_SupportDualInterface.equals("true")) {
            mSupportWifiSharing = true;
            mSupportWifiSharingLite = false;
        } else if (SoftAp_SupportWifiSharing.equals("false") && SoftAp_SupportDualInterface.equals("true")) {
            mSupportWifiSharing = true;
            mSupportWifiSharingLite = true;
        } else if (SoftAp_SupportWifiSharing.equals("true")) {
            mSupportWifiSharing = true;
            mSupportWifiSharingLite = false;
        }
        if (mSupportWifiSharingLite || mSupportWifiSharing) {
            SystemProperties.set("wifi.dualconcurrent.interface", "swlan0");
        } else {
            SystemProperties.set("wifi.dualconcurrent.interface", "wlan0");
        }
        SystemProperties.set("wifi.mhs.wifisharinglite", "" + mSupportWifiSharingLite);
        if (MHSDBG) {
            addMHSDumpLog("wifi.dualconcurrent.interface " + SystemProperties.get("wifi.dualconcurrent.interface", "aaaa") + " " + SystemProperties.get("wifi.mhs.wifisharinglite", "aaaa"));
        }
        Log.d(TAG, " checkWifiSharing() " + mSupportWifiSharing + " " + mSupportWifiSharingLite + " " + first_api_level);
    }

    public boolean supportWifiSharing() {
        if (!mIsReadSoftApInfo) {
            addMHSDumpLog("supportWifiSharing() !!! try to use before init !!!! :" + Log.getStackTraceString(new Throwable()));
        }
        if (MHSDBG && SystemProperties.getInt("mhs.first_api_level", -1) != -1) {
            checkWifiSharing();
        }
        return mSupportWifiSharing;
    }

    public boolean supportWifiSharingLite() {
        if (!mIsReadSoftApInfo) {
            addMHSDumpLog("supportWifiSharingLite() !!! try to use before init !!!! :" + Log.getStackTraceString(new Throwable()));
        }
        if (MHSDBG && SystemProperties.getInt("mhs.first_api_level", -1) != -1) {
            checkWifiSharing();
        }
        return mSupportWifiSharingLite;
    }
}
