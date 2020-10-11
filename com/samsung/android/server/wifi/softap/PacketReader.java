package com.samsung.android.server.wifi.softap;

import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import java.io.FileDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import libcore.io.IoUtils;

public abstract class PacketReader {
    public static final int DEFAULT_RECV_BUF_SIZE = 2048;
    private static final int FD_EVENTS = 5;
    private static final int UNREGISTER_THIS_FD = 0;
    private FileDescriptor mFd;
    private final Handler mHandler;
    private final byte[] mPacket;
    private long mPacketsReceived;
    private final MessageQueue mQueue;

    /* access modifiers changed from: protected */
    public abstract FileDescriptor createFd();

    protected static void closeFd(FileDescriptor fd) {
        IoUtils.closeQuietly(fd);
    }

    protected PacketReader(Handler h) {
        this(h, 2048);
    }

    protected PacketReader(Handler h, int recvbufsize) {
        this.mHandler = h;
        this.mQueue = this.mHandler.getLooper().getQueue();
        this.mPacket = new byte[Math.max(recvbufsize, 2048)];
    }

    public final void start() {
        if (onCorrectThread()) {
            createAndRegisterFd();
        } else {
            this.mHandler.post(new Runnable() {
                public final void run() {
                    PacketReader.this.lambda$start$0$PacketReader();
                }
            });
        }
    }

    public /* synthetic */ void lambda$start$0$PacketReader() {
        logError("start() called from off-thread", (Exception) null);
        createAndRegisterFd();
    }

    public final void stop() {
        if (onCorrectThread()) {
            unregisterAndDestroyFd();
        } else {
            this.mHandler.post(new Runnable() {
                public final void run() {
                    PacketReader.this.lambda$stop$1$PacketReader();
                }
            });
        }
    }

    public /* synthetic */ void lambda$stop$1$PacketReader() {
        logError("stop() called from off-thread", (Exception) null);
        unregisterAndDestroyFd();
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public final int recvBufSize() {
        return this.mPacket.length;
    }

    public final long numPacketsReceived() {
        return this.mPacketsReceived;
    }

    /* access modifiers changed from: protected */
    public int readPacket(FileDescriptor fd, byte[] packetBuffer) throws Exception {
        return Os.read(fd, packetBuffer, 0, packetBuffer.length);
    }

    /* access modifiers changed from: protected */
    public void handlePacket(byte[] recvbuf, int length) {
    }

    /* access modifiers changed from: protected */
    public void logError(String msg, Exception e) {
    }

    /* access modifiers changed from: protected */
    public void onStart() {
    }

    /* access modifiers changed from: protected */
    public void onStop() {
    }

    private void createAndRegisterFd() {
        if (this.mFd == null) {
            try {
                this.mFd = createFd();
                if (this.mFd != null) {
                    IoUtils.setBlocking(this.mFd, false);
                }
                FileDescriptor fileDescriptor = this.mFd;
                if (fileDescriptor != null) {
                    this.mQueue.addOnFileDescriptorEventListener(fileDescriptor, 5, new MessageQueue.OnFileDescriptorEventListener() {
                        public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                            if (PacketReader.this.isRunning() && PacketReader.this.handleInput()) {
                                return 5;
                            }
                            PacketReader.this.unregisterAndDestroyFd();
                            return 0;
                        }
                    });
                    onStart();
                }
            } catch (Exception e) {
                logError("Failed to create socket: ", e);
                closeFd(this.mFd);
                this.mFd = null;
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isRunning() {
        FileDescriptor fileDescriptor = this.mFd;
        return fileDescriptor != null && fileDescriptor.valid();
    }

    /* access modifiers changed from: private */
    public boolean handleInput() {
        while (isRunning()) {
            try {
                int bytesRead = readPacket(this.mFd, this.mPacket);
                if (bytesRead < 1) {
                    if (isRunning()) {
                        logError("Socket closed, exiting", (Exception) null);
                    }
                    return false;
                }
                this.mPacketsReceived++;
                try {
                    handlePacket(this.mPacket, bytesRead);
                } catch (Exception e) {
                    logError("handlePacket error: ", e);
                    return false;
                }
            } catch (ErrnoException e2) {
                if (e2.errno == OsConstants.EAGAIN) {
                    return true;
                }
                if (e2.errno != OsConstants.EINTR) {
                    if (!isRunning()) {
                        return false;
                    }
                    logError("readPacket error: ", e2);
                    return false;
                }
            } catch (Exception e3) {
                if (!isRunning()) {
                    return false;
                }
                logError("readPacket error: ", e3);
                return false;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void unregisterAndDestroyFd() {
        FileDescriptor fileDescriptor = this.mFd;
        if (fileDescriptor != null) {
            this.mQueue.removeOnFileDescriptorEventListener(fileDescriptor);
            closeFd(this.mFd);
            this.mFd = null;
            onStop();
        }
    }

    private boolean onCorrectThread() {
        return this.mHandler.getLooper() == Looper.myLooper();
    }

    public static Object getMethod(Object original, String className, String methodName, Object... args) throws Exception {
        try {
            Method method = Class.forName(className).getDeclaredMethod(methodName, getObjectType(args));
            method.setAccessible(true);
            return method.invoke(original, args);
        } catch (Exception e) {
            e.printStackTrace();
            if (!(e instanceof InvocationTargetException)) {
                return null;
            }
            throw ((Exception) e.getCause());
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v1, resolved type: java.lang.Class<?>[]} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.Class<?>[] getObjectType(java.lang.Object... r4) {
        /*
            int r0 = r4.length
            java.lang.Class[] r0 = new java.lang.Class[r0]
            java.lang.String r1 = "android.system.PacketSocketAddress"
            java.lang.Class r1 = java.lang.Class.forName(r1)     // Catch:{ Exception -> 0x0087 }
            r2 = 0
        L_0x000a:
            int r3 = r4.length     // Catch:{ Exception -> 0x0087 }
            if (r2 >= r3) goto L_0x0085
            r3 = r4[r2]     // Catch:{ Exception -> 0x0087 }
            boolean r3 = r3 instanceof java.lang.Integer     // Catch:{ Exception -> 0x0087 }
            if (r3 == 0) goto L_0x0019
            java.lang.Class r3 = java.lang.Integer.TYPE     // Catch:{ Exception -> 0x0087 }
            r0[r2] = r3     // Catch:{ Exception -> 0x0087 }
            goto L_0x0082
        L_0x0019:
            r3 = r4[r2]     // Catch:{ Exception -> 0x0087 }
            boolean r3 = r3 instanceof java.lang.Long     // Catch:{ Exception -> 0x0087 }
            if (r3 == 0) goto L_0x0024
            java.lang.Class r3 = java.lang.Long.TYPE     // Catch:{ Exception -> 0x0087 }
            r0[r2] = r3     // Catch:{ Exception -> 0x0087 }
            goto L_0x0082
        L_0x0024:
            r3 = r4[r2]     // Catch:{ Exception -> 0x0087 }
            boolean r3 = r3 instanceof java.lang.Boolean     // Catch:{ Exception -> 0x0087 }
            if (r3 == 0) goto L_0x002f
            java.lang.Class r3 = java.lang.Boolean.TYPE     // Catch:{ Exception -> 0x0087 }
            r0[r2] = r3     // Catch:{ Exception -> 0x0087 }
            goto L_0x0082
        L_0x002f:
            r3 = r4[r2]     // Catch:{ Exception -> 0x0087 }
            boolean r3 = r3 instanceof java.lang.Float     // Catch:{ Exception -> 0x0087 }
            if (r3 == 0) goto L_0x003a
            java.lang.Class r3 = java.lang.Float.TYPE     // Catch:{ Exception -> 0x0087 }
            r0[r2] = r3     // Catch:{ Exception -> 0x0087 }
            goto L_0x0082
        L_0x003a:
            r3 = r4[r2]     // Catch:{ Exception -> 0x0087 }
            boolean r3 = r3 instanceof java.lang.Double     // Catch:{ Exception -> 0x0087 }
            if (r3 == 0) goto L_0x0045
            java.lang.Class r3 = java.lang.Double.TYPE     // Catch:{ Exception -> 0x0087 }
            r0[r2] = r3     // Catch:{ Exception -> 0x0087 }
            goto L_0x0082
        L_0x0045:
            r3 = r4[r2]     // Catch:{ Exception -> 0x0087 }
            boolean r3 = r3 instanceof java.lang.Short     // Catch:{ Exception -> 0x0087 }
            if (r3 == 0) goto L_0x0050
            java.lang.Class r3 = java.lang.Short.TYPE     // Catch:{ Exception -> 0x0087 }
            r0[r2] = r3     // Catch:{ Exception -> 0x0087 }
            goto L_0x0082
        L_0x0050:
            r3 = r4[r2]     // Catch:{ Exception -> 0x0087 }
            boolean r3 = r3 instanceof java.lang.Byte     // Catch:{ Exception -> 0x0087 }
            if (r3 == 0) goto L_0x005b
            java.lang.Class r3 = java.lang.Byte.TYPE     // Catch:{ Exception -> 0x0087 }
            r0[r2] = r3     // Catch:{ Exception -> 0x0087 }
            goto L_0x0082
        L_0x005b:
            r3 = r4[r2]     // Catch:{ Exception -> 0x0087 }
            boolean r3 = r3 instanceof java.net.Inet4Address     // Catch:{ Exception -> 0x0087 }
            if (r3 != 0) goto L_0x007e
            r3 = r4[r2]     // Catch:{ Exception -> 0x0087 }
            boolean r3 = r3 instanceof java.net.Inet6Address     // Catch:{ Exception -> 0x0087 }
            if (r3 == 0) goto L_0x0068
            goto L_0x007e
        L_0x0068:
            r3 = r4[r2]     // Catch:{ Exception -> 0x0087 }
            java.lang.Class r3 = r3.getClass()     // Catch:{ Exception -> 0x0087 }
            if (r3 != r1) goto L_0x0075
            java.lang.Class<java.net.SocketAddress> r3 = java.net.SocketAddress.class
            r0[r2] = r3     // Catch:{ Exception -> 0x0087 }
            goto L_0x0082
        L_0x0075:
            r3 = r4[r2]     // Catch:{ Exception -> 0x0087 }
            java.lang.Class r3 = r3.getClass()     // Catch:{ Exception -> 0x0087 }
            r0[r2] = r3     // Catch:{ Exception -> 0x0087 }
            goto L_0x0082
        L_0x007e:
            java.lang.Class<java.net.InetAddress> r3 = java.net.InetAddress.class
            r0[r2] = r3     // Catch:{ Exception -> 0x0087 }
        L_0x0082:
            int r2 = r2 + 1
            goto L_0x000a
        L_0x0085:
            return r0
        L_0x0087:
            r1 = move-exception
            r2 = 0
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.PacketReader.getObjectType(java.lang.Object[]):java.lang.Class[]");
    }
}
