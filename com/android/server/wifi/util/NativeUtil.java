package com.android.server.wifi.util;

import android.text.TextUtils;
import com.android.server.wifi.ByteBufferReader;
import com.samsung.android.net.wifi.OpBrandingLoader;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import libcore.util.HexEncoding;

public class NativeUtil {
    public static final byte[] ANY_MAC_BYTES = {0, 0, 0, 0, 0, 0};
    private static final String ANY_MAC_STR = "any";
    private static final String CHARSET_CN = "gbk";
    private static final String CHARSET_KOR = "ksc5601";
    private static final String CONFIG_CHARSET = OpBrandingLoader.getInstance().getSupportCharacterSet();
    public static final int IEEE80211_MAX_SSID_LEN = 32;
    private static final int MAC_LENGTH = 6;
    private static final int MAC_OUI_LENGTH = 3;
    private static final int MAC_STR_LENGTH = 17;
    private static final int SSID_BYTES_MAX_LEN = 32;
    private static final int SSID_BYTES_MAX_LEN_FOR_KOR_CN = 48;

    public static ArrayList<Byte> stringToByteArrayList(String str) {
        if (str != null) {
            try {
                ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(str));
                byte[] byteArray = new byte[encoded.remaining()];
                encoded.get(byteArray);
                return byteArrayToArrayList(byteArray);
            } catch (CharacterCodingException cce) {
                throw new IllegalArgumentException("cannot be utf-8 encoded", cce);
            }
        } else {
            throw new IllegalArgumentException("null string");
        }
    }

    public static String stringFromByteArrayList(ArrayList<Byte> byteArrayList) {
        if (byteArrayList != null) {
            byte[] byteArray = new byte[byteArrayList.size()];
            int i = 0;
            Iterator<Byte> it = byteArrayList.iterator();
            while (it.hasNext()) {
                byteArray[i] = it.next().byteValue();
                i++;
            }
            return new String(byteArray, StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("null byte array list");
    }

    public static byte[] stringToByteArray(String str) {
        if (str != null) {
            return str.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("null string");
    }

    public static String stringFromByteArray(byte[] byteArray) {
        if (byteArray != null) {
            return new String(byteArray);
        }
        throw new IllegalArgumentException("null byte array");
    }

    public static byte[] macAddressToByteArray(String macStr) {
        if (TextUtils.isEmpty(macStr) || "any".equals(macStr)) {
            return ANY_MAC_BYTES;
        }
        String cleanMac = macStr.replace(":", "");
        if (cleanMac.length() == 12) {
            return HexEncoding.decode(cleanMac.toCharArray(), false);
        }
        throw new IllegalArgumentException("invalid mac string length: " + cleanMac);
    }

    public static String macAddressFromByteArray(byte[] macArray) {
        if (macArray == null) {
            throw new IllegalArgumentException("null mac bytes");
        } else if (macArray.length == 6) {
            StringBuilder sb = new StringBuilder(17);
            for (int i = 0; i < macArray.length; i++) {
                if (i != 0) {
                    sb.append(":");
                }
                sb.append(new String(HexEncoding.encode(macArray, i, 1)));
            }
            return sb.toString().toLowerCase();
        } else {
            throw new IllegalArgumentException("invalid macArray length: " + macArray.length);
        }
    }

    public static byte[] macAddressOuiToByteArray(String macStr) {
        if (macStr != null) {
            String cleanMac = macStr.replace(":", "");
            if (cleanMac.length() == 6) {
                return HexEncoding.decode(cleanMac.toCharArray(), false);
            }
            throw new IllegalArgumentException("invalid mac oui string length: " + cleanMac);
        }
        throw new IllegalArgumentException("null mac string");
    }

    public static Long macAddressToLong(byte[] macArray) {
        if (macArray == null) {
            throw new IllegalArgumentException("null mac bytes");
        } else if (macArray.length == 6) {
            try {
                return Long.valueOf(ByteBufferReader.readInteger(ByteBuffer.wrap(macArray), ByteOrder.BIG_ENDIAN, macArray.length));
            } catch (IllegalArgumentException | BufferUnderflowException e) {
                throw new IllegalArgumentException("invalid macArray");
            }
        } else {
            throw new IllegalArgumentException("invalid macArray length: " + macArray.length);
        }
    }

    public static String removeEnclosingQuotes(String quotedStr) {
        int length = quotedStr.length();
        if (length >= 2 && quotedStr.charAt(0) == '\"' && quotedStr.charAt(length - 1) == '\"') {
            return quotedStr.substring(1, length - 1);
        }
        return quotedStr;
    }

    public static String addEnclosingQuotes(String str) {
        return "\"" + str + "\"";
    }

    public static ArrayList<Byte> hexOrQuotedStringToBytes(String str) {
        if (str != null) {
            int length = str.length();
            if (length > 1 && str.charAt(0) == '\"' && str.charAt(length - 1) == '\"') {
                return stringToByteArrayList(str.substring(1, str.length() - 1));
            }
            return byteArrayToArrayList(hexStringToByteArray(str));
        }
        throw new IllegalArgumentException("null string");
    }

    public static String bytesToHexOrQuotedString(ArrayList<Byte> bytes) {
        if (bytes != null) {
            byte[] byteArray = byteArrayFromArrayList(bytes);
            if (!bytes.contains((byte) 0)) {
                try {
                    CharBuffer decoded = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(byteArray));
                    return "\"" + decoded.toString() + "\"";
                } catch (CharacterCodingException e) {
                }
            }
            return hexStringFromByteArray(byteArray);
        }
        throw new IllegalArgumentException("null ssid bytes");
    }

    public static ArrayList<Byte> decodeSsid(String ssidStr) {
        ArrayList<Byte> ssidBytes = hexOrQuotedStringToBytes(ssidStr);
        if (CHARSET_CN.equals(CONFIG_CHARSET) || CHARSET_KOR.equals(CONFIG_CHARSET)) {
            if (ssidBytes.size() > 48) {
                throw new IllegalArgumentException("ssid bytes size out of range: " + ssidBytes.size());
            }
        } else if (ssidBytes.size() > 32) {
            throw new IllegalArgumentException("ssid bytes size out of range: " + ssidBytes.size());
        }
        return ssidBytes;
    }

    public static String encodeSsid(ArrayList<Byte> ssidBytes) {
        if (ssidBytes.size() <= 32) {
            return bytesToHexOrQuotedString(ssidBytes);
        }
        throw new IllegalArgumentException("ssid bytes size out of range: " + ssidBytes.size());
    }

    public static ArrayList<Byte> byteArrayToArrayList(byte[] bytes) {
        ArrayList<Byte> byteList = new ArrayList<>();
        for (byte valueOf : bytes) {
            byteList.add(Byte.valueOf(valueOf));
        }
        return byteList;
    }

    public static byte[] byteArrayFromArrayList(ArrayList<Byte> bytes) {
        byte[] byteArray = new byte[bytes.size()];
        int i = 0;
        Iterator<Byte> it = bytes.iterator();
        while (it.hasNext()) {
            byteArray[i] = it.next().byteValue();
            i++;
        }
        return byteArray;
    }

    public static byte[] hexStringToByteArray(String hexStr) {
        if (hexStr != null) {
            return HexEncoding.decode(hexStr.toCharArray(), false);
        }
        throw new IllegalArgumentException("null hex string");
    }

    public static String hexStringFromByteArray(byte[] bytes) {
        if (bytes != null) {
            return new String(HexEncoding.encode(bytes)).toLowerCase();
        }
        throw new IllegalArgumentException("null hex bytes");
    }

    public static String wpsDevTypeStringFromByteArray(byte[] devType) {
        byte[] a = devType;
        int x = ((a[0] & 255) << 8) | (a[1] & 255);
        return String.format("%d-%s-%d", new Object[]{Integer.valueOf(x), new String(HexEncoding.encode(Arrays.copyOfRange(devType, 2, 6))), Integer.valueOf(((a[6] & 255) << 8) | (a[7] & 255))});
    }

    public static boolean isUTF8String(byte[] str, long length) {
        int nBytes;
        int nBytes2 = 0;
        boolean bAllAscii = true;
        for (int i = 0; ((long) i) < length; i++) {
            char chr = (char) (str[i] & 255);
            if ((chr & 128) != 0) {
                bAllAscii = false;
            }
            if (nBytes2 == 0) {
                if (chr < 128) {
                    continue;
                } else {
                    if (chr >= 252 && chr <= 253) {
                        nBytes = 6;
                    } else if (chr >= 248) {
                        nBytes = 5;
                    } else if (chr >= 240) {
                        nBytes = 4;
                    } else if (chr >= 224) {
                        nBytes = 3;
                    } else if (chr < 192) {
                        return false;
                    } else {
                        nBytes = 2;
                    }
                    nBytes2 = nBytes - 1;
                }
            } else if ((chr & 192) != 128) {
                return false;
            } else {
                nBytes2--;
            }
        }
        if (nBytes2 > 0 || bAllAscii) {
            return false;
        }
        return true;
    }

    public static boolean isUCNVString(byte[] str, int length) {
        boolean isAllASCII = true;
        int i = 0;
        while (i < length) {
            char byte1 = (char) (str[i] & 255);
            if (byte1 >= 129 && byte1 < 255 && i + 1 < length) {
                char byte2 = (char) (str[i + 1] & 255);
                if (byte2 < '@' || byte2 >= 255 || byte2 == 127) {
                    return false;
                }
                isAllASCII = false;
                i++;
            } else if (byte1 >= 128) {
                return false;
            }
            i++;
        }
        return !isAllASCII;
    }

    public static String hexStringFromString(String str) {
        byte[] bytes = stringToByteArray(str);
        byte[] encrypted_bytes = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            encrypted_bytes[i] = (byte) ((~bytes[i]) ^ 128);
        }
        return hexStringFromByteArray(encrypted_bytes);
    }
}
