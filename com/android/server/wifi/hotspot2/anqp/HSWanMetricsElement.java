package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HSWanMetricsElement extends ANQPElement {
    @VisibleForTesting
    public static final int AT_CAPACITY_MASK = 8;
    @VisibleForTesting
    public static final int EXPECTED_BUFFER_SIZE = 13;
    public static final int LINK_STATUS_DOWN = 2;
    @VisibleForTesting
    public static final int LINK_STATUS_MASK = 3;
    public static final int LINK_STATUS_RESERVED = 0;
    public static final int LINK_STATUS_TEST = 3;
    public static final int LINK_STATUS_UP = 1;
    private static final int MAX_LOAD = 256;
    @VisibleForTesting
    public static final int SYMMETRIC_LINK_MASK = 4;
    private final boolean mCapped;
    private final int mDownlinkLoad;
    private final long mDownlinkSpeed;
    private final int mLMD;
    private final int mStatus;
    private final boolean mSymmetric;
    private final int mUplinkLoad;
    private final long mUplinkSpeed;

    @VisibleForTesting
    public HSWanMetricsElement(int status, boolean symmetric, boolean capped, long downlinkSpeed, long uplinkSpeed, int downlinkLoad, int uplinkLoad, int lmd) {
        super(Constants.ANQPElementType.HSWANMetrics);
        this.mStatus = status;
        this.mSymmetric = symmetric;
        this.mCapped = capped;
        this.mDownlinkSpeed = downlinkSpeed;
        this.mUplinkSpeed = uplinkSpeed;
        this.mDownlinkLoad = downlinkLoad;
        this.mUplinkLoad = uplinkLoad;
        this.mLMD = lmd;
    }

    public static HSWanMetricsElement parse(ByteBuffer payload) throws ProtocolException {
        ByteBuffer byteBuffer = payload;
        if (payload.remaining() == 13) {
            int wanInfo = payload.get() & 255;
            int status = wanInfo & 3;
            boolean capped = false;
            boolean symmetric = (wanInfo & 4) != 0;
            if ((wanInfo & 8) != 0) {
                capped = true;
            }
            long downlinkSpeed = ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 4) & Constants.INT_MASK;
            long uplinkSpeed = ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 4) & Constants.INT_MASK;
            byte b = payload.get() & 255;
            byte b2 = payload.get() & 255;
            byte b3 = b2;
            byte b4 = b;
            return new HSWanMetricsElement(status, symmetric, capped, downlinkSpeed, uplinkSpeed, b, b2, ((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & 65535);
        }
        throw new ProtocolException("Unexpected buffer size: " + payload.remaining());
    }

    public int getStatus() {
        return this.mStatus;
    }

    public boolean isSymmetric() {
        return this.mSymmetric;
    }

    public boolean isCapped() {
        return this.mCapped;
    }

    public long getDownlinkSpeed() {
        return this.mDownlinkSpeed;
    }

    public long getUplinkSpeed() {
        return this.mUplinkSpeed;
    }

    public int getDownlinkLoad() {
        return this.mDownlinkLoad;
    }

    public int getUplinkLoad() {
        return this.mUplinkLoad;
    }

    public int getLMD() {
        return this.mLMD;
    }

    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof HSWanMetricsElement)) {
            return false;
        }
        HSWanMetricsElement that = (HSWanMetricsElement) thatObject;
        if (this.mStatus == that.mStatus && this.mSymmetric == that.mSymmetric && this.mCapped == that.mCapped && this.mDownlinkSpeed == that.mDownlinkSpeed && this.mUplinkSpeed == that.mUplinkSpeed && this.mDownlinkLoad == that.mDownlinkLoad && this.mUplinkLoad == that.mUplinkLoad && this.mLMD == that.mLMD) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (int) (((long) this.mStatus) + this.mDownlinkSpeed + this.mUplinkSpeed + ((long) this.mDownlinkLoad) + ((long) this.mUplinkLoad) + ((long) this.mLMD));
    }

    public String toString() {
        return String.format("HSWanMetrics{mStatus=%s, mSymmetric=%s, mCapped=%s, mDlSpeed=%d, mUlSpeed=%d, mDlLoad=%f, mUlLoad=%f, mLMD=%d}", new Object[]{Integer.valueOf(this.mStatus), Boolean.valueOf(this.mSymmetric), Boolean.valueOf(this.mCapped), Long.valueOf(this.mDownlinkSpeed), Long.valueOf(this.mUplinkSpeed), Double.valueOf((((double) this.mDownlinkLoad) * 100.0d) / 256.0d), Double.valueOf((((double) this.mUplinkLoad) * 100.0d) / 256.0d), Integer.valueOf(this.mLMD)});
    }
}
