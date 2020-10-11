package android.hardware.wifi.V1_3;

import android.hardware.wifi.V1_0.WifiChannelInfo;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WifiChannelStats {
    public int ccaBusyTimeInMs;
    public WifiChannelInfo channel = new WifiChannelInfo();
    public int onTimeInMs;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != WifiChannelStats.class) {
            return false;
        }
        WifiChannelStats other = (WifiChannelStats) otherObject;
        if (HidlSupport.deepEquals(this.channel, other.channel) && this.onTimeInMs == other.onTimeInMs && this.ccaBusyTimeInMs == other.ccaBusyTimeInMs) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(this.channel)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.onTimeInMs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.ccaBusyTimeInMs)))});
    }

    public final String toString() {
        return "{" + ".channel = " + this.channel + ", .onTimeInMs = " + this.onTimeInMs + ", .ccaBusyTimeInMs = " + this.ccaBusyTimeInMs + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(24), 0);
    }

    public static final ArrayList<WifiChannelStats> readVectorFromParcel(HwParcel parcel) {
        ArrayList<WifiChannelStats> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 24), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            WifiChannelStats _hidl_vec_element = new WifiChannelStats();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 24));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.channel.readEmbeddedFromParcel(parcel, _hidl_blob, 0 + _hidl_offset);
        this.onTimeInMs = _hidl_blob.getInt32(16 + _hidl_offset);
        this.ccaBusyTimeInMs = _hidl_blob.getInt32(20 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(24);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<WifiChannelStats> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 24);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 24));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        this.channel.writeEmbeddedToBlob(_hidl_blob, 0 + _hidl_offset);
        _hidl_blob.putInt32(16 + _hidl_offset, this.onTimeInMs);
        _hidl_blob.putInt32(20 + _hidl_offset, this.ccaBusyTimeInMs);
    }
}
