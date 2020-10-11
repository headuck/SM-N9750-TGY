package com.android.server.wifi.hotspot2.anqp.eap;

import com.android.internal.annotations.VisibleForTesting;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class VendorSpecificAuth extends AuthParam {
    private final byte[] mData;

    @VisibleForTesting
    public VendorSpecificAuth(byte[] data) {
        super(221);
        this.mData = data;
    }

    public static VendorSpecificAuth parse(ByteBuffer payload, int length) {
        byte[] data = new byte[length];
        payload.get(data);
        return new VendorSpecificAuth(data);
    }

    public byte[] getData() {
        return this.mData;
    }

    public boolean equals(Object thatObject) {
        if (thatObject == this) {
            return true;
        }
        if (!(thatObject instanceof VendorSpecificAuth)) {
            return false;
        }
        return Arrays.equals(this.mData, ((VendorSpecificAuth) thatObject).mData);
    }

    public int hashCode() {
        return Arrays.hashCode(this.mData);
    }

    public String toString() {
        return "VendorSpecificAuth{mData=" + Arrays.toString(this.mData) + "}";
    }
}
