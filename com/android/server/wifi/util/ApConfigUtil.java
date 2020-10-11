package com.android.server.wifi.util;

import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Debug;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.wifi.WifiNative;
import java.util.ArrayList;
import java.util.Random;

public class ApConfigUtil {
    public static final int DEFAULT_AP_BAND = 0;
    public static final int DEFAULT_AP_CHANNEL = 6;
    public static final int ERROR_GENERIC = 2;
    public static final int ERROR_NO_CHANNEL = 1;
    public static final int SUCCESS = 0;
    private static final String TAG = "ApConfigUtil";
    private static final Random sRandom = new Random();

    public static int convertFrequencyToChannel(int frequency) {
        if (frequency >= 2412 && frequency <= 2472) {
            return ((frequency - 2412) / 5) + 1;
        }
        if (frequency == 2484) {
            return 14;
        }
        if (frequency < 5170 || frequency > 5825) {
            return -1;
        }
        return ((frequency - 5170) / 5) + 34;
    }

    public static int chooseApChannel(int apBand, ArrayList<Integer> allowed2GChannels, int[] allowed5GFreqList) {
        if (apBand != 0 && apBand != 1 && apBand != -1) {
            Log.e(TAG, "Invalid band: " + apBand);
            return -1;
        } else if (apBand == 0 || apBand == -1) {
            if (allowed2GChannels != null && allowed2GChannels.size() != 0) {
                return allowed2GChannels.get(sRandom.nextInt(allowed2GChannels.size())).intValue();
            }
            Log.d(TAG, "2GHz allowed channel list not specified");
            return 6;
        } else if (allowed5GFreqList != null && allowed5GFreqList.length > 0) {
            return convertFrequencyToChannel(allowed5GFreqList[sRandom.nextInt(allowed5GFreqList.length)]);
        } else {
            Log.e(TAG, "No available channels on 5GHz band");
            return -1;
        }
    }

    public static int updateApChannelConfig(WifiNative wifiNative, String countryCode, ArrayList<Integer> allowed2GChannels, WifiConfiguration config) {
        if (!wifiNative.isHalStarted()) {
            config.apBand = 0;
            config.apChannel = 6;
            return 0;
        } else if (config.apBand == 1 && countryCode == null) {
            Log.e(TAG, "5GHz band is not allowed without country code");
            return 2;
        } else {
            if (config.apChannel == 0) {
                config.apChannel = chooseApChannel(config.apBand, allowed2GChannels, wifiNative.getChannelsForBand(2));
                if (config.apChannel == -1) {
                    Log.e(TAG, "Failed to get available channel.");
                    return 1;
                }
            }
            return 0;
        }
    }

    public static boolean isWifiApSupport5G(WifiNative wifiNative, String countryCode) {
        String tCountry;
        boolean bSupport5G = false;
        Log.d(TAG, "isWifiApSupport5G() :" + countryCode);
        if (countryCode == null) {
            countryCode = "US";
            Log.i(TAG, "set country code : " + countryCode);
        }
        if (!wifiNative.isHalStarted()) {
            Log.i(TAG, "hal is not started");
            return false;
        }
        if (("eng".equals(Build.TYPE) || Debug.semIsProductDev()) && (tCountry = SystemProperties.get("mhs.country")) != null && !tCountry.equals("")) {
            countryCode = tCountry;
            Log.i(TAG, " DBG country" + countryCode);
        }
        String[] Countries2GOnly = {"BO", "CL", "QA", "KP", "SY"};
        for (String equals : Countries2GOnly) {
            if (countryCode.equals(equals)) {
                Log.i(TAG, "isWifiApSupport5G() 2G only country return false");
                return false;
            }
        }
        Log.i(TAG, "setCountryCode Hal:" + countryCode);
        if (wifiNative.setCountryCodeHal(wifiNative.getSoftApInterfaceName(), countryCode)) {
            int[] allowed5GFreqs = wifiNative.getChannelsForBand(2);
            if (allowed5GFreqs != null) {
                int i = 0;
                while (true) {
                    if (i >= allowed5GFreqs.length) {
                        break;
                    }
                    Log.i(TAG, " supported 5G channels [" + i + "] " + allowed5GFreqs[i]);
                    if (allowed5GFreqs[i] == 5745) {
                        bSupport5G = true;
                        break;
                    }
                    i++;
                }
            }
        } else {
            Log.i(TAG, "setCountryCodeHal() failed");
        }
        Log.i(TAG, "isWifiApSupport5G() return " + bSupport5G);
        return bSupport5G;
    }
}
