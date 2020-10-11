package com.samsung.android.server.wifi.mobilewips.framework;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class MobileWipsFrameworkUtil {
    private static final boolean A_DBG = false;
    private static final String TAG = "MobileWipsFrameworkUtil::Util";

    private MobileWipsFrameworkUtil() {
    }

    public static String ipToString(byte[] ip) {
        String ipAddr = "";
        if (ip == null) {
            return null;
        }
        for (int i = 0; i < ip.length; i++) {
            ipAddr = ipAddr + Integer.toString(ip[i] & 255);
            if (i == ip.length - 1) {
                break;
            }
            ipAddr = ipAddr + ".";
        }
        return ipAddr;
    }

    public static int ipToInt(byte[] ip) {
        int ipAddr = 0;
        if (ip == null) {
            return 0;
        }
        for (int i = 0; i < ip.length; i++) {
            ipAddr |= (ip[i] & 255) << (i * 8);
        }
        return ipAddr;
    }

    public static String macToString(byte[] mac) {
        String macAddr = "";
        if (mac == null) {
            return null;
        }
        for (int i = 0; i < mac.length; i++) {
            String hexString = "0" + Integer.toHexString(mac[i]);
            macAddr = macAddr + hexString.substring(hexString.length() - 2);
            if (i != mac.length - 1) {
                macAddr = macAddr + ":";
            }
        }
        return macAddr;
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    public static String byteArrayToHexString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", new Object[]{Integer.valueOf(bytes[i] & 255)}));
        }
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        if (s.length() % 2 != 0) {
            s = "0" + s;
        }
        byte[] data = new byte[(s.length() / 2)];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ((Character.digit(s.charAt(i * 2), 16) << 4) + Character.digit(s.charAt((i * 2) + 1), 16));
        }
        return data;
    }

    public static boolean compareByteArray(byte[] _data1, byte[] _data2) {
        if (_data1 == null || _data2 == null) {
            return false;
        }
        return Arrays.equals(_data1, _data2);
    }

    public static boolean compareString(String _data1, String _data2) {
        if (_data1 == null || _data2 == null || !_data1.equals(_data2)) {
            return false;
        }
        return true;
    }

    public static int byteArrayToInt(byte[] bytes) {
        if (bytes != null && bytes.length == 2) {
            return ((bytes[0] << 8) & 65280) | (bytes[1] & 255);
        }
        return -1;
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    public static void byteArrayToReverseByteArray(byte[] bytes) {
        if (bytes != null) {
            int j = bytes.length - 1;
            for (int i = 0; j > i; i++) {
                byte tmp = bytes[j];
                bytes[j] = bytes[i];
                bytes[i] = tmp;
                j--;
            }
        }
    }

    public static void insertDumplog(int attack_type, String reason_code) {
    }

    public static WifiConfiguration getSpecificNetwork(WifiManager wifiManager, int netID) {
        List<WifiConfiguration> configlist;
        if (!(wifiManager == null || (configlist = wifiManager.getConfiguredNetworks()) == null)) {
            for (WifiConfiguration wificonfig : configlist) {
                if (wificonfig.networkId == netID) {
                    return wificonfig;
                }
            }
        }
        return null;
    }
}
