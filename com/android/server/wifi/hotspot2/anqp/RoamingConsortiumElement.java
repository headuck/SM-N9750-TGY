package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RoamingConsortiumElement extends ANQPElement {
    @VisibleForTesting
    public static final int MAXIMUM_OI_LENGTH = 8;
    @VisibleForTesting
    public static final int MINIMUM_OI_LENGTH = 1;
    private final List<Long> mOIs;

    @VisibleForTesting
    public RoamingConsortiumElement(List<Long> ois) {
        super(Constants.ANQPElementType.ANQPRoamingConsortium);
        this.mOIs = ois;
    }

    public static RoamingConsortiumElement parse(ByteBuffer payload) throws ProtocolException {
        List<Long> OIs = new ArrayList<>();
        while (payload.hasRemaining()) {
            int length = payload.get() & 255;
            if (length < 1 || length > 8) {
                throw new ProtocolException("Bad OI length: " + length);
            }
            OIs.add(Long.valueOf(ByteBufferReader.readInteger(payload, ByteOrder.BIG_ENDIAN, length)));
        }
        return new RoamingConsortiumElement(OIs);
    }

    public List<Long> getOIs() {
        return Collections.unmodifiableList(this.mOIs);
    }

    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof RoamingConsortiumElement)) {
            return false;
        }
        return this.mOIs.equals(((RoamingConsortiumElement) thatObject).mOIs);
    }

    public int hashCode() {
        return this.mOIs.hashCode();
    }

    public String toString() {
        return "RoamingConsortium{mOis=[" + Utils.roamingConsortiumsToString((Collection<Long>) this.mOIs) + "]}";
    }
}
