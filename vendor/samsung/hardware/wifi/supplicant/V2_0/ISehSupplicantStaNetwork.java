package vendor.samsung.hardware.wifi.supplicant.V2_0;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.NativeHandle;
import android.os.RemoteException;
import com.android.server.wifi.tcp.WifiTransportLayerUtils;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantNetwork;

public interface ISehSupplicantStaNetwork extends ISehSupplicantNetwork {
    public static final String kInterfaceName = "vendor.samsung.hardware.wifi.supplicant@2.0::ISehSupplicantStaNetwork";

    @FunctionalInterface
    public interface getAutoReconnectValueCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getBssCallback {
        void onValues(SupplicantStatus supplicantStatus, BssParam bssParam);
    }

    @FunctionalInterface
    public interface getEapMethodCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getEapPacFileCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getEapPhase1MethodCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getSimIndexCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getVendorSsidValueCallback {
        void onValues(SupplicantStatus supplicantStatus, boolean z);
    }

    @FunctionalInterface
    public interface getWapiAsCertCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    @FunctionalInterface
    public interface getWapiCertFormatCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getWapiPskTypeCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getWapiUserCertCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    IHwBinder asBinder();

    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    void getAutoReconnectValue(getAutoReconnectValueCallback getautoreconnectvaluecallback) throws RemoteException;

    void getBss(getBssCallback getbsscallback) throws RemoteException;

    DebugInfo getDebugInfo() throws RemoteException;

    void getEapMethod(getEapMethodCallback geteapmethodcallback) throws RemoteException;

    void getEapPacFile(getEapPacFileCallback geteappacfilecallback) throws RemoteException;

    void getEapPhase1Method(getEapPhase1MethodCallback geteapphase1methodcallback) throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getSimIndex(getSimIndexCallback getsimindexcallback) throws RemoteException;

    void getVendorSsidValue(getVendorSsidValueCallback getvendorssidvaluecallback) throws RemoteException;

    void getWapiAsCert(getWapiAsCertCallback getwapiascertcallback) throws RemoteException;

    void getWapiCertFormat(getWapiCertFormatCallback getwapicertformatcallback) throws RemoteException;

    void getWapiPskType(getWapiPskTypeCallback getwapipsktypecallback) throws RemoteException;

    void getWapiUserCert(getWapiUserCertCallback getwapiusercertcallback) throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    SupplicantStatus setEapMethod(int i) throws RemoteException;

    SupplicantStatus setEapPacFile(String str) throws RemoteException;

    SupplicantStatus setEapPhase1Method(int i) throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    SupplicantStatus setSimIndex(int i) throws RemoteException;

    SupplicantStatus setVendorSsid(boolean z) throws RemoteException;

    SupplicantStatus setWapiAsCert(String str) throws RemoteException;

    SupplicantStatus setWapiCertFormat(int i) throws RemoteException;

    SupplicantStatus setWapiPskType(int i) throws RemoteException;

    SupplicantStatus setWapiUserCert(String str) throws RemoteException;

    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISehSupplicantStaNetwork asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ISehSupplicantStaNetwork)) {
            return (ISehSupplicantStaNetwork) iface;
        }
        ISehSupplicantStaNetwork proxy = new Proxy(binder);
        try {
            Iterator<String> it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                if (it.next().equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    static ISehSupplicantStaNetwork castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static ISehSupplicantStaNetwork getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static ISehSupplicantStaNetwork getService(boolean retry) throws RemoteException {
        return getService("default", retry);
    }

    static ISehSupplicantStaNetwork getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static ISehSupplicantStaNetwork getService() throws RemoteException {
        return getService("default");
    }

    public static final class EapMethod {
        public static final int AKA = 5;
        public static final int AKA_PRIME = 6;
        public static final int FAST = 18;
        public static final int LEAP = 19;
        public static final int PEAP = 0;
        public static final int PWD = 3;
        public static final int SIM = 4;
        public static final int TLS = 1;
        public static final int TTLS = 2;
        public static final int WFA_UNAUTH_TLS = 7;

        public static final String toString(int o) {
            if (o == 0) {
                return "PEAP";
            }
            if (o == 1) {
                return "TLS";
            }
            if (o == 2) {
                return "TTLS";
            }
            if (o == 3) {
                return "PWD";
            }
            if (o == 4) {
                return "SIM";
            }
            if (o == 5) {
                return "AKA";
            }
            if (o == 6) {
                return "AKA_PRIME";
            }
            if (o == 7) {
                return "WFA_UNAUTH_TLS";
            }
            if (o == 18) {
                return "FAST";
            }
            if (o == 19) {
                return "LEAP";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList<>();
            int flipped = 0;
            list.add("PEAP");
            if ((o & 1) == 1) {
                list.add("TLS");
                flipped = 0 | 1;
            }
            if ((o & 2) == 2) {
                list.add("TTLS");
                flipped |= 2;
            }
            if ((o & 3) == 3) {
                list.add("PWD");
                flipped |= 3;
            }
            if ((o & 4) == 4) {
                list.add("SIM");
                flipped |= 4;
            }
            if ((o & 5) == 5) {
                list.add("AKA");
                flipped |= 5;
            }
            if ((o & 6) == 6) {
                list.add("AKA_PRIME");
                flipped |= 6;
            }
            if ((o & 7) == 7) {
                list.add("WFA_UNAUTH_TLS");
                flipped |= 7;
            }
            if ((o & 18) == 18) {
                list.add("FAST");
                flipped |= 18;
            }
            if ((o & 19) == 19) {
                list.add("LEAP");
                flipped |= 19;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    public static final class EapPhase1Method {
        public static final int ALLOW_AUTHENTICATED = 2;
        public static final int ALLOW_BOTH = 3;
        public static final int ALLOW_UNAUTHENTICATED = 1;
        public static final int DISABLE = 0;
        public static final int NONE = 4;

        public static final String toString(int o) {
            if (o == 0) {
                return "DISABLE";
            }
            if (o == 1) {
                return "ALLOW_UNAUTHENTICATED";
            }
            if (o == 2) {
                return "ALLOW_AUTHENTICATED";
            }
            if (o == 3) {
                return "ALLOW_BOTH";
            }
            if (o == 4) {
                return WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE;
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList<>();
            int flipped = 0;
            list.add("DISABLE");
            if ((o & 1) == 1) {
                list.add("ALLOW_UNAUTHENTICATED");
                flipped = 0 | 1;
            }
            if ((o & 2) == 2) {
                list.add("ALLOW_AUTHENTICATED");
                flipped |= 2;
            }
            if ((o & 3) == 3) {
                list.add("ALLOW_BOTH");
                flipped |= 3;
            }
            if ((o & 4) == 4) {
                list.add(WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE);
                flipped |= 4;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    public static final class KeyMgmtMask {
        public static final int CCKM = 16384;
        public static final int FILS_SHA256 = 262144;
        public static final int FT_SAE = 2048;
        public static final int OWE = 4194304;
        public static final int SAE = 1024;
        public static final int WAPI_CERT = 8192;
        public static final int WAPI_PSK = 4096;

        public static final String toString(int o) {
            if (o == 1024) {
                return "SAE";
            }
            if (o == 2048) {
                return "FT_SAE";
            }
            if (o == 4096) {
                return "WAPI_PSK";
            }
            if (o == 8192) {
                return "WAPI_CERT";
            }
            if (o == 16384) {
                return "CCKM";
            }
            if (o == 262144) {
                return "FILS_SHA256";
            }
            if (o == 4194304) {
                return "OWE";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList<>();
            int flipped = 0;
            if ((o & 1024) == 1024) {
                list.add("SAE");
                flipped = 0 | 1024;
            }
            if ((o & 2048) == 2048) {
                list.add("FT_SAE");
                flipped |= 2048;
            }
            if ((o & 4096) == 4096) {
                list.add("WAPI_PSK");
                flipped |= 4096;
            }
            if ((o & 8192) == 8192) {
                list.add("WAPI_CERT");
                flipped |= 8192;
            }
            if ((o & 16384) == 16384) {
                list.add("CCKM");
                flipped |= 16384;
            }
            if ((o & 262144) == 262144) {
                list.add("FILS_SHA256");
                flipped |= 262144;
            }
            if ((o & 4194304) == 4194304) {
                list.add("OWE");
                flipped |= 4194304;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    public static final class ProtoMask {
        public static final int WAPI_PROTO = 4;

        public static final String toString(int o) {
            if (o == 4) {
                return "WAPI_PROTO";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList<>();
            int flipped = 0;
            if ((o & 4) == 4) {
                list.add("WAPI_PROTO");
                flipped = 0 | 4;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    public static final class WapiPskType {
        public static final int ASCII = 0;
        public static final int HEXADECIMAL = 1;

        public static final String toString(int o) {
            if (o == 0) {
                return "ASCII";
            }
            if (o == 1) {
                return "HEXADECIMAL";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList<>();
            int flipped = 0;
            list.add("ASCII");
            if ((o & 1) == 1) {
                list.add("HEXADECIMAL");
                flipped = 0 | 1;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    public static final class WapiCertFormat {
        public static final int GBW = 2;
        public static final int X509 = 1;

        public static final String toString(int o) {
            if (o == 1) {
                return "X509";
            }
            if (o == 2) {
                return "GBW";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList<>();
            int flipped = 0;
            if ((o & 1) == 1) {
                list.add("X509");
                flipped = 0 | 1;
            }
            if ((o & 2) == 2) {
                list.add("GBW");
                flipped |= 2;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    public static final class BssParam {
        public byte[] age = new byte[8];
        public byte[] bssid = new byte[6];
        public int freq;
        public ArrayList<Byte> ieData = new ArrayList<>();
        public ArrayList<Byte> ssid = new ArrayList<>();
        public byte[] tsf = new byte[8];

        public final boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            }
            if (otherObject == null || otherObject.getClass() != BssParam.class) {
                return false;
            }
            BssParam other = (BssParam) otherObject;
            if (HidlSupport.deepEquals(this.bssid, other.bssid) && HidlSupport.deepEquals(this.ssid, other.ssid) && HidlSupport.deepEquals(this.tsf, other.tsf) && HidlSupport.deepEquals(this.age, other.age) && HidlSupport.deepEquals(this.ieData, other.ieData) && this.freq == other.freq) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(this.bssid)), Integer.valueOf(HidlSupport.deepHashCode(this.ssid)), Integer.valueOf(HidlSupport.deepHashCode(this.tsf)), Integer.valueOf(HidlSupport.deepHashCode(this.age)), Integer.valueOf(HidlSupport.deepHashCode(this.ieData)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.freq)))});
        }

        public final String toString() {
            return "{" + ".bssid = " + Arrays.toString(this.bssid) + ", .ssid = " + this.ssid + ", .tsf = " + Arrays.toString(this.tsf) + ", .age = " + Arrays.toString(this.age) + ", .ieData = " + this.ieData + ", .freq = " + this.freq + "}";
        }

        public final void readFromParcel(HwParcel parcel) {
            readEmbeddedFromParcel(parcel, parcel.readBuffer(64), 0);
        }

        public static final ArrayList<BssParam> readVectorFromParcel(HwParcel parcel) {
            ArrayList<BssParam> _hidl_vec = new ArrayList<>();
            HwBlob _hidl_blob = parcel.readBuffer(16);
            int _hidl_vec_size = _hidl_blob.getInt32(8);
            HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 64), _hidl_blob.handle(), 0, true);
            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                BssParam _hidl_vec_element = new BssParam();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 64));
                _hidl_vec.add(_hidl_vec_element);
            }
            return _hidl_vec;
        }

        public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
            HwBlob hwBlob = _hidl_blob;
            hwBlob.copyToInt8Array(_hidl_offset + 0, this.bssid, 6);
            int _hidl_vec_size = hwBlob.getInt32(_hidl_offset + 8 + 8);
            HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 1), _hidl_blob.handle(), _hidl_offset + 8 + 0, true);
            this.ssid.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                this.ssid.add(Byte.valueOf(childBlob.getInt8((long) (_hidl_index_0 * 1))));
            }
            hwBlob.copyToInt8Array(_hidl_offset + 24, this.tsf, 8);
            hwBlob.copyToInt8Array(_hidl_offset + 32, this.age, 8);
            int _hidl_vec_size2 = hwBlob.getInt32(_hidl_offset + 40 + 8);
            HwBlob childBlob2 = parcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 1), _hidl_blob.handle(), _hidl_offset + 40 + 0, true);
            this.ieData.clear();
            for (int _hidl_index_02 = 0; _hidl_index_02 < _hidl_vec_size2; _hidl_index_02++) {
                this.ieData.add(Byte.valueOf(childBlob2.getInt8((long) (_hidl_index_02 * 1))));
            }
            this.freq = hwBlob.getInt32(_hidl_offset + 56);
        }

        public final void writeToParcel(HwParcel parcel) {
            HwBlob _hidl_blob = new HwBlob(64);
            writeEmbeddedToBlob(_hidl_blob, 0);
            parcel.writeBuffer(_hidl_blob);
        }

        public static final void writeVectorToParcel(HwParcel parcel, ArrayList<BssParam> _hidl_vec) {
            HwBlob _hidl_blob = new HwBlob(16);
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(8, _hidl_vec_size);
            _hidl_blob.putBool(12, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 64);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 64));
            }
            _hidl_blob.putBlob(0, childBlob);
            parcel.writeBuffer(_hidl_blob);
        }

        public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
            HwBlob hwBlob = _hidl_blob;
            long _hidl_array_offset_0 = _hidl_offset + 0;
            byte[] _hidl_array_item_0 = this.bssid;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            hwBlob.putInt8Array(_hidl_array_offset_0, _hidl_array_item_0);
            int _hidl_vec_size = this.ssid.size();
            hwBlob.putInt32(_hidl_offset + 8 + 8, _hidl_vec_size);
            hwBlob.putBool(_hidl_offset + 8 + 12, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 1);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                childBlob.putInt8((long) (_hidl_index_0 * 1), this.ssid.get(_hidl_index_0).byteValue());
            }
            hwBlob.putBlob(_hidl_offset + 8 + 0, childBlob);
            long _hidl_array_offset_02 = _hidl_offset + 24;
            byte[] _hidl_array_item_02 = this.tsf;
            if (_hidl_array_item_02 == null || _hidl_array_item_02.length != 8) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            hwBlob.putInt8Array(_hidl_array_offset_02, _hidl_array_item_02);
            long _hidl_array_offset_03 = _hidl_offset + 32;
            byte[] _hidl_array_item_03 = this.age;
            if (_hidl_array_item_03 == null || _hidl_array_item_03.length != 8) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            hwBlob.putInt8Array(_hidl_array_offset_03, _hidl_array_item_03);
            int _hidl_vec_size2 = this.ieData.size();
            hwBlob.putInt32(_hidl_offset + 40 + 8, _hidl_vec_size2);
            hwBlob.putBool(_hidl_offset + 40 + 12, false);
            HwBlob childBlob2 = new HwBlob(_hidl_vec_size2 * 1);
            for (int _hidl_index_02 = 0; _hidl_index_02 < _hidl_vec_size2; _hidl_index_02++) {
                childBlob2.putInt8((long) (_hidl_index_02 * 1), this.ieData.get(_hidl_index_02).byteValue());
            }
            hwBlob.putBlob(_hidl_offset + 40 + 0, childBlob2);
            hwBlob.putInt32(_hidl_offset + 56, this.freq);
        }
    }

    public static final class Proxy implements ISehSupplicantStaNetwork {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.samsung.hardware.wifi.supplicant@2.0::ISehSupplicantStaNetwork]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        public void getId(ISehSupplicantNetwork.getIdCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getInterfaceName(ISehSupplicantNetwork.getInterfaceNameCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getType(ISehSupplicantNetwork.getTypeCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setVendorSsid(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setEapMethod(int method) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(method);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setEapPhase1Method(int method) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(method);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setEapPacFile(String pac_file) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(pac_file);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWapiPskType(int type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(type);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWapiCertFormat(int format) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(format);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWapiAsCert(String path) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(path);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWapiUserCert(String path) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeString(path);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getVendorSsidValue(getVendorSsidValueCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readBool());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getAutoReconnectValue(getAutoReconnectValueCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setSimIndex(int index) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            _hidl_request.writeInt32(index);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(14, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getSimIndex(getSimIndexCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(15, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapMethod(getEapMethodCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(16, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapPhase1Method(getEapPhase1MethodCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(17, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getEapPacFile(getEapPacFileCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(18, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getWapiPskType(getWapiPskTypeCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(19, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getWapiCertFormat(getWapiCertFormatCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(20, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getWapiAsCert(getWapiAsCertCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(21, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getWapiUserCert(getWapiUserCertCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(22, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getBss(getBssCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(23, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                BssParam _hidl_out_bssid = new BssParam();
                _hidl_out_bssid.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_bssid);
            } finally {
                _hidl_reply.release();
            }
        }

        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256067662, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                return _hidl_reply.readStringVector();
            } finally {
                _hidl_reply.release();
            }
        }

        public void debug(NativeHandle fd, ArrayList<String> options) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            _hidl_request.writeNativeHandle(fd);
            _hidl_request.writeStringVector(options);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256131655, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public String interfaceDescriptor() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256136003, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                return _hidl_reply.readString();
            } finally {
                _hidl_reply.release();
            }
        }

        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256398152, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<byte[]> _hidl_out_hashchain = new ArrayList<>();
                HwBlob _hidl_blob = _hidl_reply.readBuffer(16);
                int _hidl_vec_size = _hidl_blob.getInt32(8);
                HwBlob childBlob = _hidl_reply.readEmbeddedBuffer((long) (_hidl_vec_size * 32), _hidl_blob.handle(), 0, true);
                _hidl_out_hashchain.clear();
                for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                    byte[] _hidl_vec_element = new byte[32];
                    childBlob.copyToInt8Array((long) (_hidl_index_0 * 32), _hidl_vec_element, 32);
                    _hidl_out_hashchain.add(_hidl_vec_element);
                }
                return _hidl_out_hashchain;
            } finally {
                _hidl_reply.release();
            }
        }

        public void setHALInstrumentation() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256462420, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        public void ping() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256921159, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257049926, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                DebugInfo _hidl_out_info = new DebugInfo();
                _hidl_out_info.readFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        public void notifySyspropsChanged() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257120595, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    public static abstract class Stub extends HwBinder implements ISehSupplicantStaNetwork {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(new String[]{ISehSupplicantStaNetwork.kInterfaceName, ISehSupplicantNetwork.kInterfaceName, IBase.kInterfaceName}));
        }

        public void debug(NativeHandle fd, ArrayList<String> arrayList) {
        }

        public final String interfaceDescriptor() {
            return ISehSupplicantStaNetwork.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[][]{new byte[]{60, -115, 70, 120, -104, -87, 107, 85, 41, -108, -97, 37, Byte.MAX_VALUE, 125, -51, 88, -83, -74, 0, 68, Byte.MIN_VALUE, 120, 9, 15, -116, 15, 111, 13, 126, 31, 35, -47}, new byte[]{-45, 78, 52, -55, 46, 44, 97, 88, 19, 103, -56, 66, 122, -120, 68, 126, -28, 9, -33, 33, -45, -53, -94, -80, 58, -78, 50, 80, 88, 79, -37, 77}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, 87, 19, -109, 36, -72, 59, SemWifiApSmartUtil.BLE_BATT_3, -54, 76}}));
        }

        public final void setHALInstrumentation() {
        }

        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        public final void ping() {
        }

        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0;
            info.arch = 0;
            return info;
        }

        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (ISehSupplicantStaNetwork.kInterfaceName.equals(descriptor)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String serviceName) throws RemoteException {
            registerService(serviceName);
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        public void onTransact(int _hidl_code, HwParcel _hidl_request, final HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            boolean _hidl_is_oneway = false;
            boolean _hidl_is_oneway2 = true;
            switch (_hidl_code) {
                case 1:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantNetwork.kInterfaceName);
                    getId(new ISehSupplicantNetwork.getIdCallback() {
                        public void onValues(SupplicantStatus status, int id) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(id);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 2:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantNetwork.kInterfaceName);
                    getInterfaceName(new ISehSupplicantNetwork.getInterfaceNameCallback() {
                        public void onValues(SupplicantStatus status, String name) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(name);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 3:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantNetwork.kInterfaceName);
                    getType(new ISehSupplicantNetwork.getTypeCallback() {
                        public void onValues(SupplicantStatus status, int type) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(type);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 4:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    SupplicantStatus _hidl_out_status = setVendorSsid(_hidl_request.readBool());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 5:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    SupplicantStatus _hidl_out_status2 = setEapMethod(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status2.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 6:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    SupplicantStatus _hidl_out_status3 = setEapPhase1Method(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status3.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 7:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    SupplicantStatus _hidl_out_status4 = setEapPacFile(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status4.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 8:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    SupplicantStatus _hidl_out_status5 = setWapiPskType(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status5.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 9:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    SupplicantStatus _hidl_out_status6 = setWapiCertFormat(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status6.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 10:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    SupplicantStatus _hidl_out_status7 = setWapiAsCert(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status7.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 11:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    SupplicantStatus _hidl_out_status8 = setWapiUserCert(_hidl_request.readString());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status8.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 12:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    getVendorSsidValue(new getVendorSsidValueCallback() {
                        public void onValues(SupplicantStatus status, boolean enabled) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeBool(enabled);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 13:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    getAutoReconnectValue(new getAutoReconnectValueCallback() {
                        public void onValues(SupplicantStatus status, int enabled) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(enabled);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 14:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    SupplicantStatus _hidl_out_status9 = setSimIndex(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_out_status9.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 15:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    getSimIndex(new getSimIndexCallback() {
                        public void onValues(SupplicantStatus status, int index) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(index);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 16:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    getEapMethod(new getEapMethodCallback() {
                        public void onValues(SupplicantStatus status, int method) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(method);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 17:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    getEapPhase1Method(new getEapPhase1MethodCallback() {
                        public void onValues(SupplicantStatus status, int method) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(method);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 18:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    getEapPacFile(new getEapPacFileCallback() {
                        public void onValues(SupplicantStatus status, String pac_file) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(pac_file);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 19:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    getWapiPskType(new getWapiPskTypeCallback() {
                        public void onValues(SupplicantStatus status, int type) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(type);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 20:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    getWapiCertFormat(new getWapiCertFormatCallback() {
                        public void onValues(SupplicantStatus status, int format) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeInt32(format);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case ISupplicantStaIfaceCallback.ReasonCode.UNSUPPORTED_RSN_IE_VERSION:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    getWapiAsCert(new getWapiAsCertCallback() {
                        public void onValues(SupplicantStatus status, String path) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(path);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 22:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    getWapiUserCert(new getWapiUserCertCallback() {
                        public void onValues(SupplicantStatus status, String path) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            _hidl_reply.writeString(path);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 23:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(ISehSupplicantStaNetwork.kInterfaceName);
                    getBss(new getBssCallback() {
                        public void onValues(SupplicantStatus status, BssParam bssid) {
                            _hidl_reply.writeStatus(0);
                            status.writeToParcel(_hidl_reply);
                            bssid.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                default:
                    switch (_hidl_code) {
                        case 256067662:
                            if (_hidl_flags == false || !true) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ArrayList<String> _hidl_out_descriptors = interfaceChain();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeStringVector(_hidl_out_descriptors);
                            _hidl_reply.send();
                            return;
                        case 256131655:
                            if (_hidl_flags == false || !true) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            debug(_hidl_request.readNativeHandle(), _hidl_request.readStringVector());
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 256136003:
                            if (_hidl_flags == false || !true) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            String _hidl_out_descriptor = interfaceDescriptor();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeString(_hidl_out_descriptor);
                            _hidl_reply.send();
                            return;
                        case 256398152:
                            if (_hidl_flags == false || !true) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                            _hidl_reply.writeStatus(0);
                            HwBlob _hidl_blob = new HwBlob(16);
                            int _hidl_vec_size = _hidl_out_hashchain.size();
                            _hidl_blob.putInt32(8, _hidl_vec_size);
                            _hidl_blob.putBool(12, false);
                            HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
                            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                                long _hidl_array_offset_1 = (long) (_hidl_index_0 * 32);
                                byte[] _hidl_array_item_1 = _hidl_out_hashchain.get(_hidl_index_0);
                                if (_hidl_array_item_1 == null || _hidl_array_item_1.length != 32) {
                                    throw new IllegalArgumentException("Array element is not of the expected length");
                                }
                                childBlob.putInt8Array(_hidl_array_offset_1, _hidl_array_item_1);
                            }
                            _hidl_blob.putBlob(0, childBlob);
                            _hidl_reply.writeBuffer(_hidl_blob);
                            _hidl_reply.send();
                            return;
                        case 256462420:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (!_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            setHALInstrumentation();
                            return;
                        case 256660548:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            return;
                        case 256921159:
                            if (_hidl_flags == false || !true) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ping();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 257049926:
                            if (_hidl_flags == false || !true) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            DebugInfo _hidl_out_info = getDebugInfo();
                            _hidl_reply.writeStatus(0);
                            _hidl_out_info.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                            return;
                        case 257120595:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (!_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            notifySyspropsChanged();
                            return;
                        case 257250372:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            return;
                        default:
                            return;
                    }
            }
        }
    }
}
