package com.android.server.wifi;

import com.android.internal.annotations.VisibleForTesting;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class ByteBufferReader {
    @VisibleForTesting
    public static final int MAXIMUM_INTEGER_SIZE = 8;
    @VisibleForTesting
    public static final int MINIMUM_INTEGER_SIZE = 1;

    public static long readInteger(ByteBuffer payload, ByteOrder byteOrder, int size) {
        if (size < 1 || size > 8) {
            throw new IllegalArgumentException("Invalid size " + size);
        }
        byte[] octets = new byte[size];
        payload.get(octets);
        long value = 0;
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            for (int n = octets.length - 1; n >= 0; n--) {
                value = (value << 8) | ((long) (octets[n] & 255));
            }
        } else {
            for (byte octet : octets) {
                value = (value << 8) | ((long) (octet & 255));
            }
        }
        return value;
    }

    public static String readString(ByteBuffer payload, int length, Charset charset) {
        byte[] octets = new byte[length];
        payload.get(octets);
        return new String(octets, charset);
    }

    public static String readStringWithByteLength(ByteBuffer payload, Charset charset) {
        return readString(payload, payload.get() & 255, charset);
    }
}
