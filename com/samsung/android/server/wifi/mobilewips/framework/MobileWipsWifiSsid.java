package com.samsung.android.server.wifi.mobilewips.framework;

import android.os.Parcel;
import android.os.Parcelable;
import com.samsung.android.net.wifi.OpBrandingLoader;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Locale;

public class MobileWipsWifiSsid implements Parcelable {
    private static final String CHARSET_CN = "gbk";
    private static final String CHARSET_KOR = "ksc5601";
    public static final Parcelable.Creator<MobileWipsWifiSsid> CREATOR = new Parcelable.Creator<MobileWipsWifiSsid>() {
        public MobileWipsWifiSsid createFromParcel(Parcel in) {
            MobileWipsWifiSsid ssid = new MobileWipsWifiSsid();
            int length = in.readInt();
            byte[] b = new byte[length];
            in.readByteArray(b);
            ssid.octets.write(b, 0, length);
            return ssid;
        }

        public MobileWipsWifiSsid[] newArray(int size) {
            return new MobileWipsWifiSsid[size];
        }
    };
    private static final int HEX_RADIX = 16;
    public static final String NONE = "<unknown ssid>";
    private static final String TAG = "MobileWipsWifiSsid";
    private final String CONFIG_CHARSET;
    public final ByteArrayOutputStream octets;

    private MobileWipsWifiSsid() {
        this.octets = new ByteArrayOutputStream(32);
        this.CONFIG_CHARSET = OpBrandingLoader.getInstance().getSupportCharacterSet();
    }

    public static MobileWipsWifiSsid createFromByteArray(byte[] ssid) {
        MobileWipsWifiSsid mobileWipsWifiSsid = new MobileWipsWifiSsid();
        if (ssid != null) {
            mobileWipsWifiSsid.octets.write(ssid, 0, ssid.length);
        }
        return mobileWipsWifiSsid;
    }

    public static MobileWipsWifiSsid createFromAsciiEncoded(String asciiEncoded) {
        MobileWipsWifiSsid a = new MobileWipsWifiSsid();
        a.convertToBytes(asciiEncoded);
        return a;
    }

    public static MobileWipsWifiSsid createFromHex(String hexStr) {
        int val;
        MobileWipsWifiSsid a = new MobileWipsWifiSsid();
        if (hexStr == null) {
            return a;
        }
        if (hexStr.startsWith("0x") || hexStr.startsWith("0X")) {
            hexStr = hexStr.substring(2);
        }
        for (int i = 0; i < hexStr.length() - 1; i += 2) {
            try {
                val = Integer.parseInt(hexStr.substring(i, i + 2), 16);
            } catch (NumberFormatException e) {
                val = 0;
            }
            a.octets.write(val);
        }
        return a;
    }

    private void convertToBytes(String asciiEncoded) {
        int val;
        int i = 0;
        while (i < asciiEncoded.length()) {
            char c = asciiEncoded.charAt(i);
            if (c != '\\') {
                this.octets.write(c);
                i++;
            } else {
                i++;
                char charAt = asciiEncoded.charAt(i);
                if (charAt == '\"') {
                    this.octets.write(34);
                    i++;
                } else if (charAt == '\\') {
                    this.octets.write(92);
                    i++;
                } else if (charAt == 'e') {
                    this.octets.write(27);
                    i++;
                } else if (charAt == 'n') {
                    this.octets.write(10);
                    i++;
                } else if (charAt == 'r') {
                    this.octets.write(13);
                    i++;
                } else if (charAt == 't') {
                    this.octets.write(9);
                    i++;
                } else if (charAt != 'x') {
                    switch (charAt) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                            int val2 = asciiEncoded.charAt(i) - '0';
                            i++;
                            if (asciiEncoded.charAt(i) >= '0' && asciiEncoded.charAt(i) <= '7') {
                                val2 = ((val2 * 8) + asciiEncoded.charAt(i)) - 48;
                                i++;
                            }
                            if (asciiEncoded.charAt(i) >= '0' && asciiEncoded.charAt(i) <= '7') {
                                val2 = ((val2 * 8) + asciiEncoded.charAt(i)) - 48;
                                i++;
                            }
                            this.octets.write(val2);
                            int i2 = val2;
                            break;
                    }
                } else {
                    i++;
                    try {
                        val = Integer.parseInt(asciiEncoded.substring(i, i + 2), 16);
                    } catch (NumberFormatException e) {
                        val = -1;
                    } catch (StringIndexOutOfBoundsException e2) {
                        val = -1;
                    }
                    if (val < 0) {
                        int val3 = Character.digit(asciiEncoded.charAt(i), 16);
                        if (val3 >= 0) {
                            this.octets.write(val3);
                            i++;
                        }
                    } else {
                        this.octets.write(val);
                        i += 2;
                    }
                }
            }
        }
    }

    static boolean isUTF8String(byte[] str, long length) {
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

    static boolean isUCNVString(byte[] str, int length) {
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

    public String toString() {
        String ucnvSsid;
        byte[] ssidBytes = this.octets.toByteArray();
        if (this.octets.size() <= 0 || isArrayAllZeroes(ssidBytes)) {
            return "";
        }
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        CharBuffer out = CharBuffer.allocate(32);
        CoderResult result = decoder.decode(ByteBuffer.wrap(ssidBytes), out, true);
        out.flip();
        if (result.isError()) {
            return NONE;
        }
        String decodedSsid = out.toString();
        int length = this.octets.size();
        if (!CHARSET_CN.equals(this.CONFIG_CHARSET) && !CHARSET_KOR.equals(this.CONFIG_CHARSET)) {
            return out.toString();
        }
        if (isUTF8String(ssidBytes, (long) length) || !isUCNVString(ssidBytes, length)) {
            return out.toString();
        }
        try {
            if (CHARSET_CN.equals(this.CONFIG_CHARSET)) {
                ucnvSsid = new String(ssidBytes, CHARSET_CN);
            } else {
                ucnvSsid = new String(ssidBytes, CHARSET_KOR);
            }
            return ucnvSsid;
        } catch (Exception e) {
            return decodedSsid;
        }
    }

    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof MobileWipsWifiSsid)) {
            return false;
        }
        return Arrays.equals(this.octets.toByteArray(), ((MobileWipsWifiSsid) thatObject).octets.toByteArray());
    }

    public int hashCode() {
        return Arrays.hashCode(this.octets.toByteArray());
    }

    private boolean isArrayAllZeroes(byte[] ssidBytes) {
        for (byte b : ssidBytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isHidden() {
        return isArrayAllZeroes(this.octets.toByteArray());
    }

    public byte[] getOctets() {
        return this.octets.toByteArray();
    }

    public String getHexString() {
        String out = "0x";
        byte[] ssidbytes = getOctets();
        for (int i = 0; i < this.octets.size(); i++) {
            out = out + String.format(Locale.US, "%02x", new Object[]{Byte.valueOf(ssidbytes[i])});
        }
        if (this.octets.size() > 0) {
            return out;
        }
        return null;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (dest != null) {
            dest.writeInt(this.octets.size());
            dest.writeByteArray(this.octets.toByteArray());
        }
    }
}
