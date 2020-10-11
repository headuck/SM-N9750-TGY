package com.samsung.android.net.wifi.wifiguider;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IWifiGuiderService extends IInterface {
    void clearHistory() throws RemoteException;

    List<String> getDiagnosisResults() throws RemoteException;

    boolean runDiagnosis(String str) throws RemoteException;

    void setEnabled(boolean z) throws RemoteException;

    public static class Default implements IWifiGuiderService {
        public boolean runDiagnosis(String diagnosisName) throws RemoteException {
            return false;
        }

        public List<String> getDiagnosisResults() throws RemoteException {
            return null;
        }

        public void clearHistory() throws RemoteException {
        }

        public void setEnabled(boolean enabled) throws RemoteException {
        }

        public IBinder asBinder() {
            return null;
        }
    }

    public static abstract class Stub extends Binder implements IWifiGuiderService {
        private static final String DESCRIPTOR = "com.samsung.android.net.wifi.wifiguider.IWifiGuiderService";
        static final int TRANSACTION_clearHistory = 3;
        static final int TRANSACTION_getDiagnosisResults = 2;
        static final int TRANSACTION_runDiagnosis = 1;
        static final int TRANSACTION_setEnabled = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWifiGuiderService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IWifiGuiderService)) {
                return new Proxy(obj);
            }
            return (IWifiGuiderService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 1) {
                data.enforceInterface(DESCRIPTOR);
                boolean _result = runDiagnosis(data.readString());
                reply.writeNoException();
                reply.writeInt(_result);
                return true;
            } else if (code == 2) {
                data.enforceInterface(DESCRIPTOR);
                List<String> _result2 = getDiagnosisResults();
                reply.writeNoException();
                reply.writeStringList(_result2);
                return true;
            } else if (code == 3) {
                data.enforceInterface(DESCRIPTOR);
                clearHistory();
                reply.writeNoException();
                return true;
            } else if (code == 4) {
                data.enforceInterface(DESCRIPTOR);
                setEnabled(data.readInt() != 0);
                reply.writeNoException();
                return true;
            } else if (code != 1598968902) {
                return super.onTransact(code, data, reply, flags);
            } else {
                reply.writeString(DESCRIPTOR);
                return true;
            }
        }

        private static class Proxy implements IWifiGuiderService {
            public static IWifiGuiderService sDefaultImpl;
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

            public boolean runDiagnosis(String diagnosisName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(diagnosisName);
                    boolean z = false;
                    if (!this.mRemote.transact(1, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().runDiagnosis(diagnosisName);
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

            public List<String> getDiagnosisResults() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!this.mRemote.transact(2, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getDiagnosisResults();
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

            public void clearHistory() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (this.mRemote.transact(3, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().clearHistory();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled ? 1 : 0);
                    if (this.mRemote.transact(4, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().setEnabled(enabled);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IWifiGuiderService impl) {
            if (Proxy.sDefaultImpl != null || impl == null) {
                return false;
            }
            Proxy.sDefaultImpl = impl;
            return true;
        }

        public static IWifiGuiderService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
