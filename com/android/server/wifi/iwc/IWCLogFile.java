package com.android.server.wifi.iwc;

import android.app.ActivityManager;
import android.icu.text.SimpleDateFormat;
import android.util.LocalLog;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

public class IWCLogFile extends IWCFile {
    private static final String FILE_NAME_COUNTER = "iwc_log_name_counter";
    private static final String LOG_NAME_OLD = "iwc_dump_old.txt";
    private static final String LOG_PATH = "/data/log/wifi/iwc/";
    private static final int MAX_SIZE = 4194304;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private final String TAG;
    private String mFilePath_old = "/data/log/wifi/iwc/iwc_dump_old.txt";
    private final LocalLog mLocalLog;

    public /* bridge */ /* synthetic */ BufferedReader getBufferedReader() throws IOException {
        return super.getBufferedReader();
    }

    public /* bridge */ /* synthetic */ long getSize() {
        return super.getSize();
    }

    public /* bridge */ /* synthetic */ boolean isFileExists() {
        return super.isFileExists();
    }

    public /* bridge */ /* synthetic */ String readFile() throws IOException {
        return super.readFile();
    }

    public /* bridge */ /* synthetic */ void writeDataAppend(String str) {
        super.writeDataAppend(str);
    }

    public IWCLogFile(String filePath, String tag) {
        super(filePath);
        File fcnt = new File("/data/log/wifi/iwc/iwc_log_name_counter");
        if (!fcnt.exists()) {
            try {
                fcnt.createNewFile();
                setFileNameCounter(0);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        this.TAG = tag;
        this.mLocalLog = new LocalLog(ActivityManager.isLowRamDeviceStatic() ? 512 : 1024);
    }

    public byte[] readData() {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        File f = new File(this.mFilePath);
        int size = (int) getSize();
        if (size <= 0) {
            return null;
        }
        byte[] bytes = new byte[size];
        try {
            fis = new FileInputStream(f);
            bis = new BufferedInputStream(fis);
            bis.read(bytes, 0, bytes.length);
            try {
                bis.close();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e2) {
            e2.printStackTrace();
            if (bis != null) {
                bis.close();
            }
            if (fis != null) {
                fis.close();
            }
        } catch (Throwable th) {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                    throw th;
                }
            }
            if (fis != null) {
                fis.close();
            }
            throw th;
        }
        return bytes;
    }

    public byte[] readData_old() {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        File f = new File(this.mFilePath_old);
        int size = (int) getSize_old();
        if (size <= 0) {
            return null;
        }
        byte[] bytes = new byte[size];
        try {
            fis = new FileInputStream(f);
            bis = new BufferedInputStream(fis);
            bis.read(bytes, 0, bytes.length);
            try {
                bis.close();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e2) {
            e2.printStackTrace();
            if (bis != null) {
                bis.close();
            }
            if (fis != null) {
                fis.close();
            }
        } catch (Throwable th) {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                    throw th;
                }
            }
            if (fis != null) {
                fis.close();
            }
            throw th;
        }
        return bytes;
    }

    public void writeData(String data) {
        try {
            byte[] byteArray = data.getBytes(Charset.forName("UTF-8"));
            File f = new File(this.mFilePath);
            if (!f.exists()) {
                File pf = f.getParentFile();
                if (pf != null) {
                    pf.mkdirs();
                }
                f.createNewFile();
            }
            if (f.length() + ((long) byteArray.length) > 4194304) {
                File nf = new File("/data/log/wifi/iwc/iwc_dump_old.txt");
                if (nf.exists()) {
                    nf.delete();
                }
                f.renameTo(nf);
                new File(this.mFilePath).createNewFile();
                this.mFilePath_old = "/data/log/wifi/iwc/iwc_dump_old.txt";
            }
            Files.write(Paths.get(this.mFilePath, new String[0]), byteArray, new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND});
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e2) {
            e2.printStackTrace();
        } catch (SecurityException e3) {
            e3.printStackTrace();
        }
    }

    private void localLog(String s) {
        LocalLog localLog = this.mLocalLog;
        if (localLog != null) {
            localLog.log(s);
        }
    }

    public void writeToLogFile(String valueName, String value) {
        localLog(valueName + ", " + value);
        String log = valueName + ": " + value + "\n";
        Log.e(this.TAG + ".File", log);
        writeData("[" + dateFormat.format(new Date()) + "] " + log);
    }

    public long getSize_old() {
        return new File(this.mFilePath_old).length();
    }

    public void delete() {
        File f = new File(this.mFilePath);
        File f_old = new File(this.mFilePath_old);
        if (f.exists()) {
            f.delete();
        }
        if (f_old.exists()) {
            f_old.delete();
        }
    }

    public int getFileNameCounter() {
        int cnt = 0;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        DataInputStream in = null;
        try {
            FileInputStream fis2 = new FileInputStream("/data/log/wifi/iwc/iwc_log_name_counter");
            BufferedInputStream bis2 = new BufferedInputStream(fis2);
            DataInputStream in2 = new DataInputStream(bis2);
            cnt = in2.readInt();
            try {
                in2.close();
                bis2.close();
                fis2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e2) {
            e2.printStackTrace();
            if (in != null) {
                in.close();
            }
            if (bis != null) {
                bis.close();
            }
            if (fis != null) {
                fis.close();
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                    throw th;
                }
            }
            if (bis != null) {
                bis.close();
            }
            if (fis != null) {
                fis.close();
            }
            throw th;
        }
        return cnt;
    }

    /* Debug info: failed to restart local var, previous not found, register: 4 */
    /* JADX WARNING: Code restructure failed: missing block: B:11:?, code lost:
        r0.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0024, code lost:
        throw r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x001b, code lost:
        r2 = move-exception;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setFileNameCounter(int r5) {
        /*
            r4 = this;
            java.io.DataOutputStream r0 = new java.io.DataOutputStream     // Catch:{ IOException -> 0x0025 }
            java.io.BufferedOutputStream r1 = new java.io.BufferedOutputStream     // Catch:{ IOException -> 0x0025 }
            java.io.FileOutputStream r2 = new java.io.FileOutputStream     // Catch:{ IOException -> 0x0025 }
            java.lang.String r3 = "/data/log/wifi/iwc/iwc_log_name_counter"
            r2.<init>(r3)     // Catch:{ IOException -> 0x0025 }
            r1.<init>(r2)     // Catch:{ IOException -> 0x0025 }
            r0.<init>(r1)     // Catch:{ IOException -> 0x0025 }
            r0.writeInt(r5)     // Catch:{ all -> 0x0019 }
            r0.close()     // Catch:{ IOException -> 0x0025 }
            goto L_0x0029
        L_0x0019:
            r1 = move-exception
            throw r1     // Catch:{ all -> 0x001b }
        L_0x001b:
            r2 = move-exception
            r0.close()     // Catch:{ all -> 0x0020 }
            goto L_0x0024
        L_0x0020:
            r3 = move-exception
            r1.addSuppressed(r3)     // Catch:{ IOException -> 0x0025 }
        L_0x0024:
            throw r2     // Catch:{ IOException -> 0x0025 }
        L_0x0025:
            r0 = move-exception
            r0.printStackTrace()
        L_0x0029:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.iwc.IWCLogFile.setFileNameCounter(int):void");
    }

    public void dumpLocalLog(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mLocalLog.dump(fd, pw, args);
    }
}
