package com.android.server.wifi.iwc.p000le;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/* renamed from: com.android.server.wifi.iwc.le.DataFileControl */
public class DataFileControl<T extends Serializable> {
    private static final String TAG = "IWCLE";

    public T loadObject(String fileDir, String fileName) {
        T obj = null;
        ObjectInputStream ois = null;
        try {
            File file = new File(fileDir + "/" + fileName);
            if (file.exists()) {
                ois = new ObjectInputStream(new FileInputStream(file));
                obj = (Serializable) ois.readObject();
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        } catch (IOException e2) {
            Log.e(TAG, e2.getMessage(), e2);
            if (ois != null) {
                ois.close();
            }
        } catch (ClassNotFoundException e3) {
            Log.e(TAG, e3.getMessage(), e3);
            if (ois != null) {
                ois.close();
            }
        } catch (Throwable th) {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e4) {
                    Log.e(TAG, e4.getMessage(), e4);
                }
            }
            throw th;
        }
        return obj;
    }

    public List<T> loadObjectList(String fileDir, String fileName) {
        List<T> objectList = new ArrayList<>();
        ObjectInputStream ois = null;
        try {
            File file = new File(fileDir + "/" + fileName);
            if (file.exists()) {
                ois = new ObjectInputStream(new FileInputStream(file));
                while (true) {
                    T t = (Serializable) ois.readObject();
                    T obj = t;
                    if (t == null) {
                        break;
                    }
                    objectList.add(obj);
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        } catch (IOException e2) {
            Log.e(TAG, e2.getMessage(), e2);
            if (ois != null) {
                ois.close();
            }
        } catch (ClassNotFoundException e3) {
            Log.e(TAG, e3.getMessage(), e3);
            if (ois != null) {
                ois.close();
            }
        } catch (Throwable th) {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e4) {
                    Log.e(TAG, e4.getMessage(), e4);
                }
            }
            throw th;
        }
        return objectList;
    }

    public boolean saveObject(String fileDir, String fileName, T obj, boolean append) {
        ObjectOutputStream oos;
        ObjectOutputStream oos2 = null;
        try {
            File file = new File(fileDir + "/" + fileName);
            if (!append || !file.exists()) {
                oos = new ObjectOutputStream(new FileOutputStream(file));
            } else {
                oos = new AppendingObjectOutputStream(new FileOutputStream(file, append));
            }
            oos.writeObject(obj);
            oos.flush();
            try {
                oos.close();
                return true;
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                return false;
            }
        } catch (IOException e2) {
            Log.e(TAG, e2.getMessage(), e2);
            if (oos2 != null) {
                try {
                    oos2.close();
                } catch (IOException e3) {
                    Log.e(TAG, e3.getMessage(), e3);
                    return false;
                }
            }
            return false;
        } catch (Throwable e4) {
            if (oos2 != null) {
                try {
                    oos2.close();
                } catch (IOException e5) {
                    Log.e(TAG, e5.getMessage(), e5);
                    return false;
                }
            }
            throw e4;
        }
    }

    public void deleteDataFile(String fileDir, String fileName) {
        File file = new File(fileDir + "/" + fileName);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }

    /* renamed from: com.android.server.wifi.iwc.le.DataFileControl$AppendingObjectOutputStream */
    class AppendingObjectOutputStream extends ObjectOutputStream {
        public AppendingObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        protected AppendingObjectOutputStream() throws IOException, SecurityException {
        }

        /* access modifiers changed from: protected */
        public void writeStreamHeader() throws IOException {
            reset();
        }
    }
}
