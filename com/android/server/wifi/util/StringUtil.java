package com.android.server.wifi.util;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import com.android.server.wifi.tcp.WifiTransportLayerUtils;
import java.util.BitSet;

public class StringUtil {
    static final byte ASCII_PRINTABLE_MAX = 126;
    static final byte ASCII_PRINTABLE_MIN = 32;

    public static boolean isAsciiPrintable(byte[] byteArray) {
        if (byteArray == null) {
            return true;
        }
        for (byte b : byteArray) {
            switch (b) {
                case 7:
                case 9:
                case 10:
                case 11:
                case 12:
                    break;
                default:
                    if (b >= 32 && b <= 126) {
                        break;
                    } else {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    public static String makeString(BitSet set, String[] strings) {
        StringBuffer buf = new StringBuffer();
        int nextSetBit = -1;
        BitSet set2 = set.get(0, strings.length);
        while (true) {
            int nextSetBit2 = set2.nextSetBit(nextSetBit + 1);
            nextSetBit = nextSetBit2;
            if (nextSetBit2 == -1) {
                break;
            }
            buf.append(strings[nextSetBit].replace('_', '-'));
            buf.append(' ');
        }
        if (set2.cardinality() > 0) {
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }

    public static String makeStringEapMethod(WifiConfiguration config) {
        if (config.enterpriseConfig.getEapMethod() != -1) {
            return WifiEnterpriseConfig.Eap.strings[config.enterpriseConfig.getEapMethod()];
        }
        return WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE;
    }

    public static String removeDoubleQuotes(String string) {
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }
}
