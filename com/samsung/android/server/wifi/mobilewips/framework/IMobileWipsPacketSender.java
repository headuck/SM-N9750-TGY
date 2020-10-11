package com.samsung.android.server.wifi.mobilewips.framework;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IMobileWipsPacketSender extends IInterface {
    boolean pingTcp(byte[] bArr, byte[] bArr2, int i, int i2, int i3) throws RemoteException;

    List<String> sendArp(int i, byte[] bArr, byte[] bArr2, String str) throws RemoteException;

    List<String> sendArpToSniffing(int i, byte[] bArr, byte[] bArr2, String str) throws RemoteException;

    int sendDhcp(int i, byte[] bArr, int i2, String str) throws RemoteException;

    byte[] sendDns(long[] jArr, byte[] bArr, byte[] bArr2, byte[] bArr3, String str, boolean z) throws RemoteException;

    boolean sendDnsQueries(long[] jArr, byte[] bArr, byte[] bArr2, String str, List<String> list, int i) throws RemoteException;

    List<String> sendIcmp(int i, byte[] bArr, byte[] bArr2, String str) throws RemoteException;

    boolean sendTcp(int i, byte[] bArr, byte[] bArr2, String str) throws RemoteException;

    public static class Default implements IMobileWipsPacketSender {
        public List<String> sendArp(int timeoutMillis, byte[] gateway, byte[] myAddr, String myMac) throws RemoteException {
            return null;
        }

        public List<String> sendArpToSniffing(int timeoutMillis, byte[] gateway, byte[] myAddr, String myMac) throws RemoteException {
            return null;
        }

        public List<String> sendIcmp(int timeoutMillis, byte[] gateway, byte[] myAddr, String dstMac) throws RemoteException {
            return null;
        }

        public int sendDhcp(int timeoutMillis, byte[] myAddr, int equalOption, String equalString) throws RemoteException {
            return 0;
        }

        public byte[] sendDns(long[] timeoutMillis, byte[] srcAddr, byte[] dstAddr, byte[] dnsMessage, String dstMac, boolean isUDP) throws RemoteException {
            return null;
        }

        public boolean sendDnsQueries(long[] timeoutMillis, byte[] srcAddr, byte[] dstAddr, String dstMac, List<String> list, int tcpIndex) throws RemoteException {
            return false;
        }

        public boolean sendTcp(int timeoutMillis, byte[] gateway, byte[] myAddr, String myMac) throws RemoteException {
            return false;
        }

        public boolean pingTcp(byte[] srcAddr, byte[] dstAddr, int dstPort, int ttl, int timeoutMillis) throws RemoteException {
            return false;
        }

        public IBinder asBinder() {
            return null;
        }
    }

    public static abstract class Stub extends Binder implements IMobileWipsPacketSender {
        private static final String DESCRIPTOR = "com.samsung.android.server.wifi.mobilewips.framework.IMobileWipsPacketSender";
        static final int TRANSACTION_pingTcp = 8;
        static final int TRANSACTION_sendArp = 1;
        static final int TRANSACTION_sendArpToSniffing = 2;
        static final int TRANSACTION_sendDhcp = 4;
        static final int TRANSACTION_sendDns = 5;
        static final int TRANSACTION_sendDnsQueries = 6;
        static final int TRANSACTION_sendIcmp = 3;
        static final int TRANSACTION_sendTcp = 7;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMobileWipsPacketSender asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IMobileWipsPacketSender)) {
                return new Proxy(obj);
            }
            return (IMobileWipsPacketSender) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            if (i != 1598968902) {
                switch (i) {
                    case 1:
                        parcel.enforceInterface(DESCRIPTOR);
                        List<String> _result = sendArp(data.readInt(), data.createByteArray(), data.createByteArray(), data.readString());
                        reply.writeNoException();
                        parcel2.writeStringList(_result);
                        return true;
                    case 2:
                        parcel.enforceInterface(DESCRIPTOR);
                        List<String> _result2 = sendArpToSniffing(data.readInt(), data.createByteArray(), data.createByteArray(), data.readString());
                        reply.writeNoException();
                        parcel2.writeStringList(_result2);
                        return true;
                    case 3:
                        parcel.enforceInterface(DESCRIPTOR);
                        List<String> _result3 = sendIcmp(data.readInt(), data.createByteArray(), data.createByteArray(), data.readString());
                        reply.writeNoException();
                        parcel2.writeStringList(_result3);
                        return true;
                    case 4:
                        parcel.enforceInterface(DESCRIPTOR);
                        int _result4 = sendDhcp(data.readInt(), data.createByteArray(), data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 5:
                        parcel.enforceInterface(DESCRIPTOR);
                        byte[] _result5 = sendDns(data.createLongArray(), data.createByteArray(), data.createByteArray(), data.createByteArray(), data.readString(), data.readInt() != 0);
                        reply.writeNoException();
                        parcel2.writeByteArray(_result5);
                        return true;
                    case 6:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean _result6 = sendDnsQueries(data.createLongArray(), data.createByteArray(), data.createByteArray(), data.readString(), data.createStringArrayList(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 7:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean _result7 = sendTcp(data.readInt(), data.createByteArray(), data.createByteArray(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 8:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean _result8 = pingTcp(data.createByteArray(), data.createByteArray(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            } else {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
        }

        private static class Proxy implements IMobileWipsPacketSender {
            public static IMobileWipsPacketSender sDefaultImpl;
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

            public List<String> sendArp(int timeoutMillis, byte[] gateway, byte[] myAddr, String myMac) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(timeoutMillis);
                    _data.writeByteArray(gateway);
                    _data.writeByteArray(myAddr);
                    _data.writeString(myMac);
                    if (!this.mRemote.transact(1, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().sendArp(timeoutMillis, gateway, myAddr, myMac);
                    }
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> sendArpToSniffing(int timeoutMillis, byte[] gateway, byte[] myAddr, String myMac) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(timeoutMillis);
                    _data.writeByteArray(gateway);
                    _data.writeByteArray(myAddr);
                    _data.writeString(myMac);
                    if (!this.mRemote.transact(2, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().sendArpToSniffing(timeoutMillis, gateway, myAddr, myMac);
                    }
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> sendIcmp(int timeoutMillis, byte[] gateway, byte[] myAddr, String dstMac) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(timeoutMillis);
                    _data.writeByteArray(gateway);
                    _data.writeByteArray(myAddr);
                    _data.writeString(dstMac);
                    if (!this.mRemote.transact(3, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().sendIcmp(timeoutMillis, gateway, myAddr, dstMac);
                    }
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int sendDhcp(int timeoutMillis, byte[] myAddr, int equalOption, String equalString) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(timeoutMillis);
                    _data.writeByteArray(myAddr);
                    _data.writeInt(equalOption);
                    _data.writeString(equalString);
                    if (!this.mRemote.transact(4, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().sendDhcp(timeoutMillis, myAddr, equalOption, equalString);
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] sendDns(long[] timeoutMillis, byte[] srcAddr, byte[] dstAddr, byte[] dnsMessage, String dstMac, boolean isUDP) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeLongArray(timeoutMillis);
                    } catch (Throwable th) {
                        th = th;
                        byte[] bArr = srcAddr;
                        byte[] bArr2 = dstAddr;
                        byte[] bArr3 = dnsMessage;
                        String str = dstMac;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeByteArray(srcAddr);
                        try {
                            _data.writeByteArray(dstAddr);
                            try {
                                _data.writeByteArray(dnsMessage);
                            } catch (Throwable th2) {
                                th = th2;
                                String str2 = dstMac;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            byte[] bArr32 = dnsMessage;
                            String str22 = dstMac;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        byte[] bArr22 = dstAddr;
                        byte[] bArr322 = dnsMessage;
                        String str222 = dstMac;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeString(dstMac);
                        _data.writeInt(isUDP ? 1 : 0);
                    } catch (Throwable th5) {
                        th = th5;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        if (this.mRemote.transact(5, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                            _reply.readException();
                            byte[] _result = _reply.createByteArray();
                            _reply.recycle();
                            _data.recycle();
                            return _result;
                        }
                        byte[] sendDns = Stub.getDefaultImpl().sendDns(timeoutMillis, srcAddr, dstAddr, dnsMessage, dstMac, isUDP);
                        _reply.recycle();
                        _data.recycle();
                        return sendDns;
                    } catch (Throwable th6) {
                        th = th6;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    long[] jArr = timeoutMillis;
                    byte[] bArr4 = srcAddr;
                    byte[] bArr222 = dstAddr;
                    byte[] bArr3222 = dnsMessage;
                    String str2222 = dstMac;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public boolean sendDnsQueries(long[] timeoutMillis, byte[] srcAddr, byte[] dstAddr, String dstMac, List<String> dnsMessages, int tcpIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeLongArray(timeoutMillis);
                    } catch (Throwable th) {
                        th = th;
                        byte[] bArr = srcAddr;
                        byte[] bArr2 = dstAddr;
                        String str = dstMac;
                        List<String> list = dnsMessages;
                        int i = tcpIndex;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeByteArray(srcAddr);
                        try {
                            _data.writeByteArray(dstAddr);
                            try {
                                _data.writeString(dstMac);
                            } catch (Throwable th2) {
                                th = th2;
                                List<String> list2 = dnsMessages;
                                int i2 = tcpIndex;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            String str2 = dstMac;
                            List<String> list22 = dnsMessages;
                            int i22 = tcpIndex;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        byte[] bArr22 = dstAddr;
                        String str22 = dstMac;
                        List<String> list222 = dnsMessages;
                        int i222 = tcpIndex;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeStringList(dnsMessages);
                        try {
                            _data.writeInt(tcpIndex);
                            boolean z = false;
                            if (this.mRemote.transact(6, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                                _reply.readException();
                                if (_reply.readInt() != 0) {
                                    z = true;
                                }
                                boolean _status = z;
                                _reply.recycle();
                                _data.recycle();
                                return _status;
                            }
                            boolean sendDnsQueries = Stub.getDefaultImpl().sendDnsQueries(timeoutMillis, srcAddr, dstAddr, dstMac, dnsMessages, tcpIndex);
                            _reply.recycle();
                            _data.recycle();
                            return sendDnsQueries;
                        } catch (Throwable th5) {
                            th = th5;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        int i2222 = tcpIndex;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    long[] jArr = timeoutMillis;
                    byte[] bArr3 = srcAddr;
                    byte[] bArr222 = dstAddr;
                    String str222 = dstMac;
                    List<String> list2222 = dnsMessages;
                    int i22222 = tcpIndex;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public boolean sendTcp(int timeoutMillis, byte[] gateway, byte[] myAddr, String myMac) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(timeoutMillis);
                    _data.writeByteArray(gateway);
                    _data.writeByteArray(myAddr);
                    _data.writeString(myMac);
                    boolean z = false;
                    if (!this.mRemote.transact(7, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().sendTcp(timeoutMillis, gateway, myAddr, myMac);
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

            public boolean pingTcp(byte[] srcAddr, byte[] dstAddr, int dstPort, int ttl, int timeoutMillis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeByteArray(srcAddr);
                    } catch (Throwable th) {
                        th = th;
                        byte[] bArr = dstAddr;
                        int i = dstPort;
                        int i2 = ttl;
                        int i3 = timeoutMillis;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeByteArray(dstAddr);
                        try {
                            _data.writeInt(dstPort);
                            try {
                                _data.writeInt(ttl);
                            } catch (Throwable th2) {
                                th = th2;
                                int i32 = timeoutMillis;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            int i22 = ttl;
                            int i322 = timeoutMillis;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        int i4 = dstPort;
                        int i222 = ttl;
                        int i3222 = timeoutMillis;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(timeoutMillis);
                        try {
                            boolean z = false;
                            if (this.mRemote.transact(8, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                                _reply.readException();
                                if (_reply.readInt() != 0) {
                                    z = true;
                                }
                                boolean _status = z;
                                _reply.recycle();
                                _data.recycle();
                                return _status;
                            }
                            boolean pingTcp = Stub.getDefaultImpl().pingTcp(srcAddr, dstAddr, dstPort, ttl, timeoutMillis);
                            _reply.recycle();
                            _data.recycle();
                            return pingTcp;
                        } catch (Throwable th5) {
                            th = th5;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    byte[] bArr2 = srcAddr;
                    byte[] bArr3 = dstAddr;
                    int i42 = dstPort;
                    int i2222 = ttl;
                    int i32222 = timeoutMillis;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }
        }

        public static boolean setDefaultImpl(IMobileWipsPacketSender impl) {
            if (Proxy.sDefaultImpl != null || impl == null) {
                return false;
            }
            Proxy.sDefaultImpl = impl;
            return true;
        }

        public static IMobileWipsPacketSender getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
