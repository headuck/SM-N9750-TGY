package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import com.samsung.android.server.wifi.SemWifiConstants;
import java.util.ArrayList;
import java.util.Objects;

public final class StaLinkLayerStats {
    public StaLinkLayerIfaceStats iface = new StaLinkLayerIfaceStats();
    public ArrayList<StaLinkLayerRadioStats> radios = new ArrayList<>();
    public long timeStampInMs;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != StaLinkLayerStats.class) {
            return false;
        }
        StaLinkLayerStats other = (StaLinkLayerStats) otherObject;
        if (HidlSupport.deepEquals(this.iface, other.iface) && HidlSupport.deepEquals(this.radios, other.radios) && this.timeStampInMs == other.timeStampInMs) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(this.iface)), Integer.valueOf(HidlSupport.deepHashCode(this.radios)), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.timeStampInMs)))});
    }

    public final String toString() {
        return "{" + ".iface = " + this.iface + ", .radios = " + this.radios + ", .timeStampInMs = " + this.timeStampInMs + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(160), 0);
    }

    public static final ArrayList<StaLinkLayerStats> readVectorFromParcel(HwParcel parcel) {
        ArrayList<StaLinkLayerStats> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * SemWifiConstants.ROUTER_OUI_TYPE), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            StaLinkLayerStats _hidl_vec_element = new StaLinkLayerStats();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * SemWifiConstants.ROUTER_OUI_TYPE));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        HwParcel hwParcel = parcel;
        HwBlob hwBlob = _hidl_blob;
        this.iface.readEmbeddedFromParcel(hwParcel, hwBlob, _hidl_offset + 0);
        int _hidl_vec_size = hwBlob.getInt32(_hidl_offset + 136 + 8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 32), _hidl_blob.handle(), _hidl_offset + 136 + 0, true);
        this.radios.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            StaLinkLayerRadioStats _hidl_vec_element = new StaLinkLayerRadioStats();
            _hidl_vec_element.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 32));
            this.radios.add(_hidl_vec_element);
        }
        this.timeStampInMs = hwBlob.getInt64(_hidl_offset + 152);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(SemWifiConstants.ROUTER_OUI_TYPE);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<StaLinkLayerStats> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * SemWifiConstants.ROUTER_OUI_TYPE);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * SemWifiConstants.ROUTER_OUI_TYPE));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        this.iface.writeEmbeddedToBlob(_hidl_blob, _hidl_offset + 0);
        int _hidl_vec_size = this.radios.size();
        _hidl_blob.putInt32(_hidl_offset + 136 + 8, _hidl_vec_size);
        _hidl_blob.putBool(_hidl_offset + 136 + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.radios.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 32));
        }
        _hidl_blob.putBlob(136 + _hidl_offset + 0, childBlob);
        _hidl_blob.putInt64(152 + _hidl_offset, this.timeStampInMs);
    }
}
