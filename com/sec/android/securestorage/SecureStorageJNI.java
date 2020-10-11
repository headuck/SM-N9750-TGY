package com.sec.android.securestorage;

public final class SecureStorageJNI {
    private static SecureStorageJNI secureStorage = new SecureStorageJNI();

    public native byte[] decrypt(byte[] bArr) throws SecureStorageExceptionJNI;

    public native byte[] decrypt(byte[] bArr, byte b) throws SecureStorageExceptionJNI;

    public native byte[] decrypt(byte[] bArr, String str) throws SecureStorageExceptionJNI;

    public native boolean delete(String str) throws SecureStorageExceptionJNI;

    public native boolean delete(String str, byte b) throws SecureStorageExceptionJNI;

    public native byte[] encrypt(byte[] bArr) throws SecureStorageExceptionJNI;

    public native byte[] encrypt(byte[] bArr, byte b) throws SecureStorageExceptionJNI;

    public native byte[] encrypt(byte[] bArr, String str) throws SecureStorageExceptionJNI;

    public native boolean eraseKey() throws SecureStorageExceptionJNI;

    public native byte[] get(String str) throws SecureStorageExceptionJNI;

    public native byte[] get(String str, byte b) throws SecureStorageExceptionJNI;

    public native byte[] get(String str, String str2) throws SecureStorageExceptionJNI;

    public native int getVersion();

    public native int initialize();

    public native boolean isKeyProvisioned() throws SecureStorageExceptionJNI;

    public native boolean isSupported();

    public native boolean provisionKey(byte[] bArr) throws SecureStorageExceptionJNI;

    public native boolean put(String str, byte[] bArr) throws SecureStorageExceptionJNI;

    public native boolean put(String str, byte[] bArr, byte b) throws SecureStorageExceptionJNI;

    public native boolean put(String str, byte[] bArr, String str2) throws SecureStorageExceptionJNI;

    static {
        System.loadLibrary("ss_jni.securestorage.samsung");
    }

    private SecureStorageJNI() {
    }

    public static SecureStorageJNI getInstance() {
        return secureStorage;
    }

    public static class SecureStorageExceptionJNI extends Exception {
        private static final long serialVersionUID = 1;

        public SecureStorageExceptionJNI() {
        }

        public SecureStorageExceptionJNI(String detailMessage) {
            super(detailMessage);
        }
    }
}
