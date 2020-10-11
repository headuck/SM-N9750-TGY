package android.hardware.wifi.supplicant.V1_2;

import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIfaceCallback;
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
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface ISupplicantP2pIface extends android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface {
    public static final String kInterfaceName = "android.hardware.wifi.supplicant@1.2::ISupplicantP2pIface";

    SupplicantStatus addGroup_1_2(ArrayList<Byte> arrayList, String str, boolean z, int i, byte[] bArr, boolean z2) throws RemoteException;

    IHwBinder asBinder();

    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    DebugInfo getDebugInfo() throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    SupplicantStatus setMacRandomization(boolean z) throws RemoteException;

    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISupplicantP2pIface asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ISupplicantP2pIface)) {
            return (ISupplicantP2pIface) iface;
        }
        ISupplicantP2pIface proxy = new Proxy(binder);
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

    static ISupplicantP2pIface castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static ISupplicantP2pIface getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static ISupplicantP2pIface getService(boolean retry) throws RemoteException {
        return getService("default", retry);
    }

    static ISupplicantP2pIface getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static ISupplicantP2pIface getService() throws RemoteException {
        return getService("default");
    }

    public static final class Proxy implements ISupplicantP2pIface {
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
                return "[class or subclass of android.hardware.wifi.supplicant@1.2::ISupplicantP2pIface]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        public void getName(ISupplicantIface.getNameCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getType(ISupplicantIface.getTypeCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void addNetwork(ISupplicantIface.addNetworkCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, ISupplicantNetwork.asInterface(_hidl_reply.readStrongBinder()));
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus removeNetwork(int id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt32(id);
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

        public void getNetwork(int id, ISupplicantIface.getNetworkCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt32(id);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, ISupplicantNetwork.asInterface(_hidl_reply.readStrongBinder()));
            } finally {
                _hidl_reply.release();
            }
        }

        public void listNetworks(ISupplicantIface.listNetworksCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsDeviceName(String name) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(name);
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

        public SupplicantStatus setWpsDeviceType(byte[] type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(8);
            byte[] _hidl_array_item_0 = type;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 8) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
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

        public SupplicantStatus setWpsManufacturer(String manufacturer) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(manufacturer);
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

        public SupplicantStatus setWpsModelName(String modelName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(modelName);
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

        public SupplicantStatus setWpsModelNumber(String modelNumber) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(modelNumber);
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

        public SupplicantStatus setWpsSerialNumber(String serialNumber) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(serialNumber);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsConfigMethods(short configMethods) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt16(configMethods);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus registerCallback(ISupplicantP2pIfaceCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
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

        public void getDeviceAddress(ISupplicantP2pIface.getDeviceAddressCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(15, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                byte[] _hidl_out_deviceAddress = new byte[6];
                _hidl_reply.readBuffer(6).copyToInt8Array(0, _hidl_out_deviceAddress, 6);
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_deviceAddress);
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setSsidPostfix(ArrayList<Byte> postfix) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8Vector(postfix);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(16, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setGroupIdle(String groupIfName, int timeoutInSec) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            _hidl_request.writeInt32(timeoutInSec);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(17, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setPowerSave(String groupIfName, boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(18, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus find(int timeoutInSec) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(timeoutInSec);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(19, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus stopFind() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(20, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus flush() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(21, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void connect(byte[] peerAddress, int provisionMethod, String preSelectedPin, boolean joinExistingGroup, boolean persistent, int goIntent, ISupplicantP2pIface.connectCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = peerAddress;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeInt32(provisionMethod);
            _hidl_request.writeString(preSelectedPin);
            _hidl_request.writeBool(joinExistingGroup);
            _hidl_request.writeBool(persistent);
            _hidl_request.writeInt32(goIntent);
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

        public SupplicantStatus cancelConnect() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(23, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus provisionDiscovery(byte[] peerAddress, int provisionMethod) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = peerAddress;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeInt32(provisionMethod);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(24, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus addGroup(boolean persistent, int persistentNetworkId) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeBool(persistent);
            _hidl_request.writeInt32(persistentNetworkId);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(25, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus removeGroup(String groupIfName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(26, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus reject(byte[] peerAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = peerAddress;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(27, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus invite(String groupIfName, byte[] goDeviceAddress, byte[] peerAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = goDeviceAddress;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            HwBlob _hidl_blob2 = new HwBlob(6);
            byte[] _hidl_array_item_02 = peerAddress;
            if (_hidl_array_item_02 == null || _hidl_array_item_02.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob2.putInt8Array(0, _hidl_array_item_02);
            _hidl_request.writeBuffer(_hidl_blob2);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(28, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus reinvoke(int persistentNetworkId, byte[] peerAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(persistentNetworkId);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = peerAddress;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(29, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus configureExtListen(int periodInMillis, int intervalInMillis) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(periodInMillis);
            _hidl_request.writeInt32(intervalInMillis);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(30, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setListenChannel(int channel, int operatingClass) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(channel);
            _hidl_request.writeInt32(operatingClass);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(31, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setDisallowedFrequencies(ArrayList<ISupplicantP2pIface.FreqRange> ranges) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            ISupplicantP2pIface.FreqRange.writeVectorToParcel(_hidl_request, ranges);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(32, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getSsid(byte[] peerAddress, ISupplicantP2pIface.getSsidCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = peerAddress;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(33, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getGroupCapability(byte[] peerAddress, ISupplicantP2pIface.getGroupCapabilityCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = peerAddress;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(34, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus addBonjourService(ArrayList<Byte> query, ArrayList<Byte> response) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8Vector(query);
            _hidl_request.writeInt8Vector(response);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(35, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus removeBonjourService(ArrayList<Byte> query) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8Vector(query);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(36, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus addUpnpService(int version, String serviceName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(version);
            _hidl_request.writeString(serviceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(37, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus removeUpnpService(int version, String serviceName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(version);
            _hidl_request.writeString(serviceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(38, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus flushServices() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(39, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void requestServiceDiscovery(byte[] peerAddress, ArrayList<Byte> query, ISupplicantP2pIface.requestServiceDiscoveryCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = peerAddress;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeInt8Vector(query);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(40, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt64());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus cancelServiceDiscovery(long identifier) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt64(identifier);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(41, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setMiracastMode(byte mode) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8(mode);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(42, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus startWpsPbc(String groupIfName, byte[] bssid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = bssid;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(43, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus startWpsPinKeypad(String groupIfName, String pin) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            _hidl_request.writeString(pin);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(44, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void startWpsPinDisplay(String groupIfName, byte[] bssid, ISupplicantP2pIface.startWpsPinDisplayCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = bssid;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(45, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus cancelWps(String groupIfName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(46, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus enableWfd(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(47, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWfdDeviceInfo(byte[] info) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = info;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(48, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void createNfcHandoverRequestMessage(ISupplicantP2pIface.createNfcHandoverRequestMessageCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(49, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public void createNfcHandoverSelectMessage(ISupplicantP2pIface.createNfcHandoverSelectMessageCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(50, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus reportNfcHandoverResponse(ArrayList<Byte> request) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8Vector(request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(51, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus reportNfcHandoverInitiation(ArrayList<Byte> select) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8Vector(select);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(52, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus saveConfig() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(53, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus addGroup_1_2(ArrayList<Byte> ssid, String pskPassphrase, boolean persistent, int freq, byte[] peerAddress, boolean joinExistingGroup) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8Vector(ssid);
            _hidl_request.writeString(pskPassphrase);
            _hidl_request.writeBool(persistent);
            _hidl_request.writeInt32(freq);
            HwBlob _hidl_blob = new HwBlob(6);
            byte[] _hidl_array_item_0 = peerAddress;
            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeBool(joinExistingGroup);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(54, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setMacRandomization(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(55, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
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

    public static abstract class Stub extends HwBinder implements ISupplicantP2pIface {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(new String[]{ISupplicantP2pIface.kInterfaceName, android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName, ISupplicantIface.kInterfaceName, IBase.kInterfaceName}));
        }

        public void debug(NativeHandle fd, ArrayList<String> arrayList) {
        }

        public final String interfaceDescriptor() {
            return ISupplicantP2pIface.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[][]{new byte[]{18, 2, 17, 55, 31, -35, 41, -5, 19, 72, 55, 7, 29, 67, 42, SemWifiApSmartUtil.BLE_BATT_6, 45, 123, 96, -23, -71, 90, -10, 17, -35, -115, -34, -122, -67, 31, 119, -18}, new byte[]{73, 7, 65, 3, 56, -59, -24, -37, -18, -60, -75, -19, -62, 96, -114, -93, 35, -11, 86, 25, 69, -8, -127, 10, -8, SemWifiApSmartUtil.BLE_BATT_3, SemWifiApSmartUtil.BLE_BATT_2, -60, 123, 1, -111, -124}, new byte[]{53, -70, 123, -51, -15, -113, 36, -88, 102, -89, -27, 66, -107, 72, -16, 103, 104, -69, SemWifiApSmartUtil.BLE_BATT_4, -94, 87, -9, 91, SemWifiApSmartUtil.BLE_BATT_2, -93, -105, -60, -40, 37, -17, -124, 56}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, 87, 19, -109, 36, -72, 59, SemWifiApSmartUtil.BLE_BATT_3, -54, 76}}));
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
            if (ISupplicantP2pIface.kInterfaceName.equals(descriptor)) {
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
            final HwParcel hwParcel2 = _hidl_reply;
            boolean _hidl_is_oneway = false;
            boolean _hidl_is_oneway2 = true;
            switch (_hidl_code) {
                case 1:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    getName(new ISupplicantIface.getNameCallback() {
                        public void onValues(SupplicantStatus status, String name) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeString(name);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 2:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    getType(new ISupplicantIface.getTypeCallback() {
                        public void onValues(SupplicantStatus status, int type) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32(type);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 3:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    addNetwork(new ISupplicantIface.addNetworkCallback() {
                        public void onValues(SupplicantStatus status, ISupplicantNetwork network) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeStrongBinder(network == null ? null : network.asBinder());
                            hwParcel2.send();
                        }
                    });
                    return;
                case 4:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status = removeNetwork(_hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 5:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    getNetwork(_hidl_request.readInt32(), new ISupplicantIface.getNetworkCallback() {
                        public void onValues(SupplicantStatus status, ISupplicantNetwork network) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeStrongBinder(network == null ? null : network.asBinder());
                            hwParcel2.send();
                        }
                    });
                    return;
                case 6:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    listNetworks(new ISupplicantIface.listNetworksCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Integer> networkIds) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32Vector(networkIds);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 7:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status2 = setWpsDeviceName(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 8:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    byte[] type = new byte[8];
                    hwParcel.readBuffer(8).copyToInt8Array(0, type, 8);
                    SupplicantStatus _hidl_out_status3 = setWpsDeviceType(type);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status3.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 9:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status4 = setWpsManufacturer(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status4.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 10:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status5 = setWpsModelName(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status5.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 11:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status6 = setWpsModelNumber(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status6.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 12:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status7 = setWpsSerialNumber(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status7.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 13:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status8 = setWpsConfigMethods(_hidl_request.readInt16());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status8.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 14:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status9 = registerCallback(ISupplicantP2pIfaceCallback.asInterface(_hidl_request.readStrongBinder()));
                    hwParcel2.writeStatus(0);
                    _hidl_out_status9.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 15:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    getDeviceAddress(new ISupplicantP2pIface.getDeviceAddressCallback() {
                        public void onValues(SupplicantStatus status, byte[] deviceAddress) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            HwBlob _hidl_blob = new HwBlob(6);
                            byte[] _hidl_array_item_0 = deviceAddress;
                            if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
                                throw new IllegalArgumentException("Array element is not of the expected length");
                            }
                            _hidl_blob.putInt8Array(0, _hidl_array_item_0);
                            hwParcel2.writeBuffer(_hidl_blob);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 16:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status10 = setSsidPostfix(_hidl_request.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status10.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 17:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status11 = setGroupIdle(_hidl_request.readString(), _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status11.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 18:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status12 = setPowerSave(_hidl_request.readString(), _hidl_request.readBool());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status12.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 19:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status13 = find(_hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status13.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 20:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status14 = stopFind();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status14.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case ISupplicantStaIfaceCallback.ReasonCode.UNSUPPORTED_RSN_IE_VERSION:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status15 = flush();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status15.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 22:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    byte[] peerAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress, 6);
                    connect(peerAddress, _hidl_request.readInt32(), _hidl_request.readString(), _hidl_request.readBool(), _hidl_request.readBool(), _hidl_request.readInt32(), new ISupplicantP2pIface.connectCallback() {
                        public void onValues(SupplicantStatus status, String generatedPin) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeString(generatedPin);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 23:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status16 = cancelConnect();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status16.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 24:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    byte[] peerAddress2 = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress2, 6);
                    SupplicantStatus _hidl_out_status17 = provisionDiscovery(peerAddress2, _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status17.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 25:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status18 = addGroup(_hidl_request.readBool(), _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status18.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 26:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status19 = removeGroup(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status19.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 27:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    byte[] peerAddress3 = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress3, 6);
                    SupplicantStatus _hidl_out_status20 = reject(peerAddress3);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status20.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 28:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    String groupIfName = _hidl_request.readString();
                    byte[] goDeviceAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, goDeviceAddress, 6);
                    byte[] peerAddress4 = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress4, 6);
                    SupplicantStatus _hidl_out_status21 = invite(groupIfName, goDeviceAddress, peerAddress4);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status21.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 29:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    int persistentNetworkId = _hidl_request.readInt32();
                    byte[] peerAddress5 = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress5, 6);
                    SupplicantStatus _hidl_out_status22 = reinvoke(persistentNetworkId, peerAddress5);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status22.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 30:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status23 = configureExtListen(_hidl_request.readInt32(), _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status23.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 31:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status24 = setListenChannel(_hidl_request.readInt32(), _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status24.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 32:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status25 = setDisallowedFrequencies(ISupplicantP2pIface.FreqRange.readVectorFromParcel(_hidl_request));
                    hwParcel2.writeStatus(0);
                    _hidl_out_status25.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 33:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    byte[] peerAddress6 = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress6, 6);
                    getSsid(peerAddress6, new ISupplicantP2pIface.getSsidCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> ssid) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt8Vector(ssid);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 34:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    byte[] peerAddress7 = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress7, 6);
                    getGroupCapability(peerAddress7, new ISupplicantP2pIface.getGroupCapabilityCallback() {
                        public void onValues(SupplicantStatus status, int capabilities) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32(capabilities);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 35:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status26 = addBonjourService(_hidl_request.readInt8Vector(), _hidl_request.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status26.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 36:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status27 = removeBonjourService(_hidl_request.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status27.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 37:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status28 = addUpnpService(_hidl_request.readInt32(), _hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status28.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 38:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status29 = removeUpnpService(_hidl_request.readInt32(), _hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status29.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 39:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status30 = flushServices();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status30.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 40:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    byte[] peerAddress8 = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress8, 6);
                    requestServiceDiscovery(peerAddress8, _hidl_request.readInt8Vector(), new ISupplicantP2pIface.requestServiceDiscoveryCallback() {
                        public void onValues(SupplicantStatus status, long identifier) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt64(identifier);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 41:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status31 = cancelServiceDiscovery(_hidl_request.readInt64());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status31.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 42:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status32 = setMiracastMode(_hidl_request.readInt8());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status32.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 43:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    String groupIfName2 = _hidl_request.readString();
                    byte[] bssid = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, bssid, 6);
                    SupplicantStatus _hidl_out_status33 = startWpsPbc(groupIfName2, bssid);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status33.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 44:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status34 = startWpsPinKeypad(_hidl_request.readString(), _hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status34.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 45:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    String groupIfName3 = _hidl_request.readString();
                    byte[] bssid2 = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, bssid2, 6);
                    startWpsPinDisplay(groupIfName3, bssid2, new ISupplicantP2pIface.startWpsPinDisplayCallback() {
                        public void onValues(SupplicantStatus status, String generatedPin) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeString(generatedPin);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 46:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status35 = cancelWps(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status35.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 47:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status36 = enableWfd(_hidl_request.readBool());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status36.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 48:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    byte[] info = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, info, 6);
                    SupplicantStatus _hidl_out_status37 = setWfdDeviceInfo(info);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status37.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 49:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    createNfcHandoverRequestMessage(new ISupplicantP2pIface.createNfcHandoverRequestMessageCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> request) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt8Vector(request);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 50:
                    if (_hidl_flags != false && true) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    createNfcHandoverSelectMessage(new ISupplicantP2pIface.createNfcHandoverSelectMessageCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> select) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt8Vector(select);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 51:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status38 = reportNfcHandoverResponse(_hidl_request.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status38.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 52:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status39 = reportNfcHandoverInitiation(_hidl_request.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status39.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 53:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status40 = saveConfig();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status40.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 54:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pIface.kInterfaceName);
                    ArrayList<Byte> ssid = _hidl_request.readInt8Vector();
                    String pskPassphrase = _hidl_request.readString();
                    boolean persistent = _hidl_request.readBool();
                    int freq = _hidl_request.readInt32();
                    byte[] peerAddress9 = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress9, 6);
                    byte[] bArr = peerAddress9;
                    SupplicantStatus _hidl_out_status41 = addGroup_1_2(ssid, pskPassphrase, persistent, freq, peerAddress9, _hidl_request.readBool());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status41.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 55:
                    if (_hidl_flags == false || !true) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status42 = setMacRandomization(_hidl_request.readBool());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status42.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                default:
                    switch (_hidl_code) {
                        case 256067662:
                            if (_hidl_flags == false || !true) {
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
                            if (_hidl_flags == false || !true) {
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
                            if (_hidl_flags == false || !true) {
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
                            if (_hidl_flags == false || !true) {
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
