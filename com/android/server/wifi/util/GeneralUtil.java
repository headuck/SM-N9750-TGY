package com.android.server.wifi.util;

import android.util.Log;
import com.samsung.android.feature.SemCscFeature;

public class GeneralUtil {
    private static final String TAG = "GeneralUtil";
    private static String mCountryCode;

    public static final class Mutable<E> {
        public E value;

        public Mutable() {
            this.value = null;
        }

        public Mutable(E value2) {
            this.value = value2;
        }
    }

    private static String readCountryCode() {
        String str = mCountryCode;
        if (str != null && !str.isEmpty()) {
            return mCountryCode;
        }
        mCountryCode = SemCscFeature.getInstance().getString("CountryISO");
        Log.d(TAG, "readCountryCode(): country=" + mCountryCode);
        return mCountryCode;
    }

    public static boolean isDomesticModel() {
        return "KR".equalsIgnoreCase(readCountryCode());
    }

    public static boolean isDomesticDfsChannel(int frequency) {
        return (5250 < frequency && frequency < 5330) || (5490 < frequency && frequency < 5710);
    }
}
