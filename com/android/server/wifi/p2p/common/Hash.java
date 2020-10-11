package com.android.server.wifi.p2p.common;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Hash {
    private static final String[] CONTACT_NUMBER = {"data1"};
    static int LONG_BYTES = 8;
    private static final String TAG = "Hash";
    static Checksum checksum = new CRC32();
    static final byte[] key = "2309851Cdgewlk3E".getBytes();

    /* renamed from: m */
    private static long f30m;
    private static long mV0;
    private static final long mV0_init = (bytesLEtoLong(key, 0) ^ 8317987319222330741L);
    private static long mV1;
    private static final long mV1_init = (bytesLEtoLong(key, 8) ^ 7237128888997146477L);
    private static long mV2;
    private static final long mV2_init = (bytesLEtoLong(key, 0) ^ 7816392313619706465L);
    private static long mV3;
    private static final long mV3_init = (bytesLEtoLong(key, 8) ^ 8387220255154660723L);
    private static int m_idx = 0;
    private static byte msg_byte_counter = 0;

    private static void initialize() {
        mV0 = mV0_init;
        mV1 = mV1_init;
        mV2 = mV2_init;
        mV3 = mV3_init;
        msg_byte_counter = 0;
    }

    private static void updateHash(byte b) {
        msg_byte_counter = (byte) (msg_byte_counter + 1);
        long j = f30m;
        int i = m_idx;
        f30m = j | ((((long) b) & 255) << (i * 8));
        m_idx = i + 1;
        if (m_idx >= LONG_BYTES) {
            mV3 ^= f30m;
            siphash_round();
            siphash_round();
            mV0 ^= f30m;
            m_idx = 0;
            f30m = 0;
        }
    }

    private static long finish() {
        byte msgLenMod256 = msg_byte_counter;
        while (m_idx < LONG_BYTES - 1) {
            updateHash((byte) 0);
        }
        updateHash(msgLenMod256);
        mV2 ^= 255;
        siphash_round();
        siphash_round();
        siphash_round();
        siphash_round();
        mV0 = ((mV0 ^ mV1) ^ mV2) ^ mV3;
        return mV0;
    }

    private static long hash(byte[] data) {
        initialize();
        for (byte updateHash : data) {
            updateHash(updateHash);
        }
        return finish();
    }

    private static long rotateLeft(long l, int shift) {
        return (l << shift) | (l >>> (64 - shift));
    }

    private static void siphash_round() {
        long j = mV0;
        long j2 = mV1;
        mV0 = j + j2;
        mV2 += mV3;
        mV1 = rotateLeft(j2, 13);
        mV3 = rotateLeft(mV3, 16);
        long j3 = mV1;
        long j4 = mV0;
        mV1 = j3 ^ j4;
        mV3 ^= mV2;
        mV0 = rotateLeft(j4, 32);
        long j5 = mV2;
        long j6 = mV1;
        mV2 = j5 + j6;
        mV0 += mV3;
        mV1 = rotateLeft(j6, 17);
        mV3 = rotateLeft(mV3, 21);
        long j7 = mV1;
        long j8 = mV2;
        mV1 = j7 ^ j8;
        mV3 ^= mV0;
        mV2 = rotateLeft(j8, 32);
    }

    private static long bytesLEtoLong(byte[] b, int offset) {
        if (b.length - offset >= 8) {
            long m = 0;
            for (int i = 0; i < 8; i++) {
                m |= (((long) b[i + offset]) & 255) << (i * 8);
            }
            return m;
        }
        throw new IllegalArgumentException("Less then 8 bytes starting from offset:" + offset);
    }

    private static byte[] longToBytes(long m) {
        return new byte[]{(byte) ((int) ((m >>> 56) & 255)), (byte) ((int) ((m >>> 48) & 255)), (byte) ((int) ((m >>> 40) & 255))};
    }

    private static String longToString(long m) {
        StringBuilder sb = new StringBuilder(18);
        for (int i = 7; i >= 0; i--) {
            sb.append(String.format("%02x", new Object[]{Byte.valueOf((byte) ((int) ((m >>> (i * 8)) & 255)))}));
        }
        return sb.toString().toLowerCase(Locale.ENGLISH);
    }

    public static byte[] getDataCheckByte(String num) {
        byte[] b = {0, 0};
        if (!"00000000".equals(num)) {
            byte[] byteArray = num.getBytes();
            checksum.update(byteArray, 0, byteArray.length);
            long csValue = checksum.getValue();
            checksum.reset();
            b[0] = (byte) ((int) ((csValue >>> 8) & 255));
            b[1] = (byte) ((int) (csValue & 255));
        }
        return b;
    }

    public static String getDataCheckString(String num) {
        if ("00000000".equals(num)) {
            return "0000";
        }
        byte[] byteArray = num.getBytes();
        checksum.update(byteArray, 0, byteArray.length);
        long csValue = checksum.getValue();
        checksum.reset();
        String val = longToString(csValue);
        return val.substring(val.length() - 4).toLowerCase(Locale.ENGLISH);
    }

    public static String getSipHashString(String value) {
        return longToString(hash(value.getBytes()));
    }

    public static byte[] getSipHashByte(String value) {
        return longToBytes(hash(value.getBytes()));
    }

    public static String retrieveDB(Context mContext, String numberhash, String crc) {
        Cursor c = null;
        InputStream clsInputStream = null;
        String findContact = null;
        Log.d(TAG, " hash retrieveDB hash : " + numberhash);
        try {
            ContentResolver contentResolver = mContext.getContentResolver();
            Uri uri = ContactsContract.Data.CONTENT_URI;
            String[] strArr = CONTACT_NUMBER;
            c = contentResolver.query(uri, strArr, "mimetype='vnd.android.cursor.item/phone_v2' AND data12 LIKE '" + numberhash + "%'", (String[]) null, (String) null);
            if (c != null) {
                while (c.moveToNext()) {
                    String data = c.getString(c.getColumnIndex("data1"));
                    String crcValue = getDataCheckString(Util.cutNumber(data));
                    if (crc.equals(crcValue)) {
                        Log.d(TAG, " hash retrieveDB - CHECK  : true  -- " + crc + "!=" + crcValue);
                        findContact = data;
                    } else {
                        Log.d(TAG, " hash retrieveDB - CHECK  : false  -- " + crc + "!=" + crcValue);
                    }
                }
                Log.d(TAG, " hash retrieveDB - failed to cursor moveToNext");
                c.close();
            } else {
                Log.d(TAG, " hash retrieveDB - CHECK :false - cursor is null");
            }
            if (c != null) {
                c.close();
            }
            if (clsInputStream != null) {
                try {
                    clsInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e2) {
            e2.printStackTrace();
            if (c != null) {
                c.close();
            }
            if (clsInputStream != null) {
                clsInputStream.close();
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            if (clsInputStream != null) {
                try {
                    clsInputStream.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            throw th;
        }
        return findContact;
    }
}
