package com.samsung.android.server.wifi.mobilewips.client;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import com.samsung.android.server.wifi.mobilewips.framework.IMobileWipsFramework;
import com.samsung.android.server.wifi.mobilewips.framework.IMobileWipsPacketSender;
import com.samsung.android.server.wifi.mobilewips.framework.MobileWipsScanResult;
import java.util.List;

public interface IMobileWipsService extends IInterface {
    void broadcastBcnEventAbort(String str, int i) throws RemoteException;

    void broadcastBcnIntervalEvent(String str, String str2, String str3, int i, int i2, long j, long j2) throws RemoteException;

    boolean checkMWIPS(String str, int i) throws RemoteException;

    void onDnsResponses(List<String> list, String str) throws RemoteException;

    void onScanResults(List<MobileWipsScanResult> list) throws RemoteException;

    boolean registerCallback(IMobileWipsFramework iMobileWipsFramework) throws RemoteException;

    boolean registerPacketSender(IMobileWipsPacketSender iMobileWipsPacketSender) throws RemoteException;

    void sendMessage(Message message) throws RemoteException;

    boolean setCurrentBss(String str, int i, byte[] bArr) throws RemoteException;

    boolean unregisterCallback(IMobileWipsFramework iMobileWipsFramework) throws RemoteException;

    boolean unregisterPacketSender(IMobileWipsPacketSender iMobileWipsPacketSender) throws RemoteException;

    void updateWifiChipInfo(String str, String str2) throws RemoteException;

    public static class Default implements IMobileWipsService {
        public boolean registerCallback(IMobileWipsFramework callback) throws RemoteException {
            return false;
        }

        public boolean unregisterCallback(IMobileWipsFramework callback) throws RemoteException {
            return false;
        }

        public void broadcastBcnIntervalEvent(String iface, String ssid, String bssid, int channel, int beaconInterval, long timestamp, long systemtime) throws RemoteException {
        }

        public void broadcastBcnEventAbort(String iface, int abortReason) throws RemoteException {
        }

        public boolean checkMWIPS(String bssid, int freq) throws RemoteException {
            return false;
        }

        public void sendMessage(Message msg) throws RemoteException {
        }

        public void updateWifiChipInfo(String id, String value) throws RemoteException {
        }

        public boolean setCurrentBss(String bssid, int freq, byte[] ies) throws RemoteException {
            return false;
        }

        public void onScanResults(List<MobileWipsScanResult> list) throws RemoteException {
        }

        public void onDnsResponses(List<String> list, String dstMac) throws RemoteException {
        }

        public boolean registerPacketSender(IMobileWipsPacketSender packetSender) throws RemoteException {
            return false;
        }

        public boolean unregisterPacketSender(IMobileWipsPacketSender packetSender) throws RemoteException {
            return false;
        }

        public IBinder asBinder() {
            return null;
        }
    }

    public static abstract class Stub extends Binder implements IMobileWipsService {
        private static final String DESCRIPTOR = "com.samsung.android.server.wifi.mobilewips.client.IMobileWipsService";
        static final int TRANSACTION_broadcastBcnEventAbort = 4;
        static final int TRANSACTION_broadcastBcnIntervalEvent = 3;
        static final int TRANSACTION_checkMWIPS = 5;
        static final int TRANSACTION_onDnsResponses = 10;
        static final int TRANSACTION_onScanResults = 9;
        static final int TRANSACTION_registerCallback = 1;
        static final int TRANSACTION_registerPacketSender = 11;
        static final int TRANSACTION_sendMessage = 6;
        static final int TRANSACTION_setCurrentBss = 8;
        static final int TRANSACTION_unregisterCallback = 2;
        static final int TRANSACTION_unregisterPacketSender = 12;
        static final int TRANSACTION_updateWifiChipInfo = 7;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMobileWipsService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IMobileWipsService)) {
                return new Proxy(obj);
            }
            return (IMobileWipsService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Message _arg0;
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            if (i != 1598968902) {
                switch (i) {
                    case 1:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean _result = registerCallback(IMobileWipsFramework.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 2:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean _result2 = unregisterCallback(IMobileWipsFramework.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 3:
                        parcel.enforceInterface(DESCRIPTOR);
                        broadcastBcnIntervalEvent(data.readString(), data.readString(), data.readString(), data.readInt(), data.readInt(), data.readLong(), data.readLong());
                        reply.writeNoException();
                        return true;
                    case 4:
                        parcel.enforceInterface(DESCRIPTOR);
                        broadcastBcnEventAbort(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 5:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean _result3 = checkMWIPS(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 6:
                        parcel.enforceInterface(DESCRIPTOR);
                        if (data.readInt() != 0) {
                            _arg0 = (Message) Message.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg0 = null;
                        }
                        sendMessage(_arg0);
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(DESCRIPTOR);
                        updateWifiChipInfo(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean _result4 = setCurrentBss(data.readString(), data.readInt(), data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 9:
                        parcel.enforceInterface(DESCRIPTOR);
                        onScanResults(parcel.createTypedArrayList(MobileWipsScanResult.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(DESCRIPTOR);
                        onDnsResponses(data.createStringArrayList(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean _result5 = registerPacketSender(IMobileWipsPacketSender.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 12:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean _result6 = unregisterPacketSender(IMobileWipsPacketSender.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            } else {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
        }

        private static class Proxy implements IMobileWipsService {
            public static IMobileWipsService sDefaultImpl;
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public boolean registerCallback(IMobileWipsFramework callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    if (!this.mRemote.transact(1, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().registerCallback(callback);
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status = z;
                    _reply.recycle();
                    _data.recycle();
                    return _status;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean unregisterCallback(IMobileWipsFramework callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    if (!this.mRemote.transact(2, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().unregisterCallback(callback);
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status = z;
                    _reply.recycle();
                    _data.recycle();
                    return _status;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void broadcastBcnIntervalEvent(String iface, String ssid, String bssid, int channel, int beaconInterval, long timestamp, long systemtime) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeString(iface);
                    } catch (Throwable th) {
                        th = th;
                        String str = ssid;
                        String str2 = bssid;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeString(ssid);
                    } catch (Throwable th2) {
                        th = th2;
                        String str22 = bssid;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeString(bssid);
                        _data.writeInt(channel);
                        _data.writeInt(beaconInterval);
                        _data.writeLong(timestamp);
                        _data.writeLong(systemtime);
                        if (this.mRemote.transact(3, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                            _reply.readException();
                            _reply.recycle();
                            _data.recycle();
                            return;
                        }
                        Stub.getDefaultImpl().broadcastBcnIntervalEvent(iface, ssid, bssid, channel, beaconInterval, timestamp, systemtime);
                        _reply.recycle();
                        _data.recycle();
                    } catch (Throwable th3) {
                        th = th3;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    String str3 = iface;
                    String str4 = ssid;
                    String str222 = bssid;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public void broadcastBcnEventAbort(String iface, int abortReason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(abortReason);
                    if (this.mRemote.transact(4, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().broadcastBcnEventAbort(iface, abortReason);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean checkMWIPS(String bssid, int freq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(bssid);
                    _data.writeInt(freq);
                    boolean z = false;
                    if (!this.mRemote.transact(5, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().checkMWIPS(bssid, freq);
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status = z;
                    _reply.recycle();
                    _data.recycle();
                    return _status;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void sendMessage(Message msg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (msg != null) {
                        _data.writeInt(1);
                        msg.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (this.mRemote.transact(6, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().sendMessage(msg);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateWifiChipInfo(String id, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(id);
                    _data.writeString(value);
                    if (this.mRemote.transact(7, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().updateWifiChipInfo(id, value);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setCurrentBss(String bssid, int freq, byte[] ies) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(bssid);
                    _data.writeInt(freq);
                    _data.writeByteArray(ies);
                    boolean z = false;
                    if (!this.mRemote.transact(8, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().setCurrentBss(bssid, freq, ies);
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status = z;
                    _reply.recycle();
                    _data.recycle();
                    return _status;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onScanResults(List<MobileWipsScanResult> scanResults) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(scanResults);
                    if (this.mRemote.transact(9, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().onScanResults(scanResults);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onDnsResponses(List<String> dnsResponses, String dstMac) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(dnsResponses);
                    _data.writeString(dstMac);
                    if (this.mRemote.transact(10, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().onDnsResponses(dnsResponses, dstMac);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean registerPacketSender(IMobileWipsPacketSender packetSender) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(packetSender != null ? packetSender.asBinder() : null);
                    boolean z = false;
                    if (!this.mRemote.transact(11, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().registerPacketSender(packetSender);
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status = z;
                    _reply.recycle();
                    _data.recycle();
                    return _status;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean unregisterPacketSender(IMobileWipsPacketSender packetSender) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(packetSender != null ? packetSender.asBinder() : null);
                    boolean z = false;
                    if (!this.mRemote.transact(12, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().unregisterPacketSender(packetSender);
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status = z;
                    _reply.recycle();
                    _data.recycle();
                    return _status;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IMobileWipsService impl) {
            if (Proxy.sDefaultImpl != null || impl == null) {
                return false;
            }
            Proxy.sDefaultImpl = impl;
            return true;
        }

        public static IMobileWipsService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
