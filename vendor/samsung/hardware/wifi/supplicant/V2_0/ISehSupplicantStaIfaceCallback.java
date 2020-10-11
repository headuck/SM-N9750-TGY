package vendor.samsung.hardware.wifi.supplicant.V2_0;

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
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface ISehSupplicantStaIfaceCallback extends IBase {
    public static final String kInterfaceName = "vendor.samsung.hardware.wifi.supplicant@2.0::ISehSupplicantStaIfaceCallback";

    IHwBinder asBinder();

    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    DebugInfo getDebugInfo() throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void onBeaconEventAbort(int i) throws RemoteException;

    void onBeaconInterval(String str, String str2, int i, int i2, long j, long j2) throws RemoteException;

    void onBssidPruned(String str, String str2, int i, int i2, int i3) throws RemoteException;

    void onDriverHang(String str, String str2) throws RemoteException;

    void onEapEvent(int i, String str, String str2) throws RemoteException;

    void onNotifyBigdata(String str, String str2) throws RemoteException;

    void onRoamingChannelListUpdate(String str, ArrayList<Integer> arrayList) throws RemoteException;

    void ping() throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISehSupplicantStaIfaceCallback asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ISehSupplicantStaIfaceCallback)) {
            return (ISehSupplicantStaIfaceCallback) iface;
        }
        ISehSupplicantStaIfaceCallback proxy = new Proxy(binder);
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

    static ISehSupplicantStaIfaceCallback castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static ISehSupplicantStaIfaceCallback getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static ISehSupplicantStaIfaceCallback getService(boolean retry) throws RemoteException {
        return getService("default", retry);
    }

    static ISehSupplicantStaIfaceCallback getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static ISehSupplicantStaIfaceCallback getService() throws RemoteException {
        return getService("default");
    }

    public static final class EapEventType {
        public static final int ANONYMOUS_IDENTITY_UPDATED = 1;
        public static final int DEAUTH_8021X_AUTH_FAILED = 2;
        public static final int EAP_FAILURE = 3;
        public static final int ERROR_MESSAGE = 4;
        public static final int LOGGING = 5;
        public static final int NOTIFICATION = 7;
        public static final int NO_CREDENTIALS = 6;
        public static final int SUCCESS = 8;
        public static final int TLS_ALERT = 9;
        public static final int TLS_CERT_ERROR = 10;
        public static final int TLS_HANDSHAKE_FAIL = 11;

        public static final String toString(int o) {
            if (o == 1) {
                return "ANONYMOUS_IDENTITY_UPDATED";
            }
            if (o == 2) {
                return "DEAUTH_8021X_AUTH_FAILED";
            }
            if (o == 3) {
                return "EAP_FAILURE";
            }
            if (o == 4) {
                return "ERROR_MESSAGE";
            }
            if (o == 5) {
                return "LOGGING";
            }
            if (o == 6) {
                return "NO_CREDENTIALS";
            }
            if (o == 7) {
                return "NOTIFICATION";
            }
            if (o == 8) {
                return "SUCCESS";
            }
            if (o == 9) {
                return "TLS_ALERT";
            }
            if (o == 10) {
                return "TLS_CERT_ERROR";
            }
            if (o == 11) {
                return "TLS_HANDSHAKE_FAIL";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList<>();
            int flipped = 0;
            if ((o & 1) == 1) {
                list.add("ANONYMOUS_IDENTITY_UPDATED");
                flipped = 0 | 1;
            }
            if ((o & 2) == 2) {
                list.add("DEAUTH_8021X_AUTH_FAILED");
                flipped |= 2;
            }
            if ((o & 3) == 3) {
                list.add("EAP_FAILURE");
                flipped |= 3;
            }
            if ((o & 4) == 4) {
                list.add("ERROR_MESSAGE");
                flipped |= 4;
            }
            if ((o & 5) == 5) {
                list.add("LOGGING");
                flipped |= 5;
            }
            if ((o & 6) == 6) {
                list.add("NO_CREDENTIALS");
                flipped |= 6;
            }
            if ((o & 7) == 7) {
                list.add("NOTIFICATION");
                flipped |= 7;
            }
            if ((o & 8) == 8) {
                list.add("SUCCESS");
                flipped |= 8;
            }
            if ((o & 9) == 9) {
                list.add("TLS_ALERT");
                flipped |= 9;
            }
            if ((o & 10) == 10) {
                list.add("TLS_CERT_ERROR");
                flipped |= 10;
            }
            if ((o & 11) == 11) {
                list.add("TLS_HANDSHAKE_FAIL");
                flipped |= 11;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    public static final class Proxy implements ISehSupplicantStaIfaceCallback {
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
                return "[class or subclass of vendor.samsung.hardware.wifi.supplicant@2.0::ISehSupplicantStaIfaceCallback]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        public void onNotifyBigdata(String feature, String params) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaIfaceCallback.kInterfaceName);
            _hidl_request.writeString(feature);
            _hidl_request.writeString(params);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public void onEapEvent(int event, String notification, String msg) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaIfaceCallback.kInterfaceName);
            _hidl_request.writeInt32(event);
            _hidl_request.writeString(notification);
            _hidl_request.writeString(msg);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public void onDriverHang(String state, String msg) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaIfaceCallback.kInterfaceName);
            _hidl_request.writeString(state);
            _hidl_request.writeString(msg);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public void onBeaconInterval(String ssid, String bssid, int channel, int beacon_interval, long timestamp, long system_time) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaIfaceCallback.kInterfaceName);
            _hidl_request.writeString(ssid);
            _hidl_request.writeString(bssid);
            _hidl_request.writeInt32(channel);
            _hidl_request.writeInt32(beacon_interval);
            _hidl_request.writeInt64(timestamp);
            _hidl_request.writeInt64(system_time);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public void onBeaconEventAbort(int abort_reason) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaIfaceCallback.kInterfaceName);
            _hidl_request.writeInt32(abort_reason);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public void onBssidPruned(String ssid, String bssid, int rssi, int reason, int timeRemaining) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaIfaceCallback.kInterfaceName);
            _hidl_request.writeString(ssid);
            _hidl_request.writeString(bssid);
            _hidl_request.writeInt32(rssi);
            _hidl_request.writeInt32(reason);
            _hidl_request.writeInt32(timeRemaining);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public void onRoamingChannelListUpdate(String ssid, ArrayList<Integer> channel_list) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISehSupplicantStaIfaceCallback.kInterfaceName);
            _hidl_request.writeString(ssid);
            _hidl_request.writeInt32Vector(channel_list);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
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

    public static abstract class Stub extends HwBinder implements ISehSupplicantStaIfaceCallback {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(new String[]{ISehSupplicantStaIfaceCallback.kInterfaceName, IBase.kInterfaceName}));
        }

        public void debug(NativeHandle fd, ArrayList<String> arrayList) {
        }

        public final String interfaceDescriptor() {
            return ISehSupplicantStaIfaceCallback.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[][]{new byte[]{56, -61, 39, -110, -54, -49, 41, 104, -54, 114, -77, 85, -38, -18, 26, -50, 58, 112, 34, -18, 84, 84, 98, 38, 101, -79, 89, -36, 107, 7, 88, 111}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, 87, 19, -109, 36, -72, 59, SemWifiApSmartUtil.BLE_BATT_3, -54, 76}}));
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
            if (ISehSupplicantStaIfaceCallback.kInterfaceName.equals(descriptor)) {
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

        public void onTransact(int _hidl_code, HwParcel _hidl_request, HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            HwParcel hwParcel = _hidl_request;
            HwParcel hwParcel2 = _hidl_reply;
            boolean _hidl_is_oneway = false;
            boolean _hidl_is_oneway2 = true;
            switch (_hidl_code) {
                case 1:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (!_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISehSupplicantStaIfaceCallback.kInterfaceName);
                    onNotifyBigdata(_hidl_request.readString(), _hidl_request.readString());
                    return;
                case 2:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (!_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISehSupplicantStaIfaceCallback.kInterfaceName);
                    onEapEvent(_hidl_request.readInt32(), _hidl_request.readString(), _hidl_request.readString());
                    return;
                case 3:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (!_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISehSupplicantStaIfaceCallback.kInterfaceName);
                    onDriverHang(_hidl_request.readString(), _hidl_request.readString());
                    return;
                case 4:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (!_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISehSupplicantStaIfaceCallback.kInterfaceName);
                    onBeaconInterval(_hidl_request.readString(), _hidl_request.readString(), _hidl_request.readInt32(), _hidl_request.readInt32(), _hidl_request.readInt64(), _hidl_request.readInt64());
                    return;
                case 5:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (!_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISehSupplicantStaIfaceCallback.kInterfaceName);
                    onBeaconEventAbort(_hidl_request.readInt32());
                    return;
                case 6:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (!_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISehSupplicantStaIfaceCallback.kInterfaceName);
                    onBssidPruned(_hidl_request.readString(), _hidl_request.readString(), _hidl_request.readInt32(), _hidl_request.readInt32(), _hidl_request.readInt32());
                    return;
                case 7:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (!_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISehSupplicantStaIfaceCallback.kInterfaceName);
                    onRoamingChannelListUpdate(_hidl_request.readString(), _hidl_request.readInt32Vector());
                    return;
                default:
                    switch (_hidl_code) {
                        case 256067662:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            ArrayList<String> _hidl_out_descriptors = interfaceChain();
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeStringVector(_hidl_out_descriptors);
                            _hidl_reply.send();
                            return;
                        case 256131655:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            debug(_hidl_request.readNativeHandle(), _hidl_request.readStringVector());
                            hwParcel2.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 256136003:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            String _hidl_out_descriptor = interfaceDescriptor();
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeString(_hidl_out_descriptor);
                            _hidl_reply.send();
                            return;
                        case 256398152:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                            hwParcel2.writeStatus(0);
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
                            hwParcel2.writeBuffer(_hidl_blob);
                            _hidl_reply.send();
                            return;
                        case 256462420:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (!_hidl_is_oneway) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            setHALInstrumentation();
                            return;
                        case 256660548:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (_hidl_is_oneway) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            return;
                        case 256921159:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            ping();
                            hwParcel2.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 257049926:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            DebugInfo _hidl_out_info = getDebugInfo();
                            hwParcel2.writeStatus(0);
                            _hidl_out_info.writeToParcel(hwParcel2);
                            _hidl_reply.send();
                            return;
                        case 257120595:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (!_hidl_is_oneway) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            notifySyspropsChanged();
                            return;
                        case 257250372:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (_hidl_is_oneway) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
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
