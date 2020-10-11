package com.sec.android.securestorage;

import com.sec.android.securestorage.SecureStorageJNI;
import java.math.BigInteger;
import java.nio.ByteBuffer;

public class SecureStorageOption {
    private static final int DOUBLE_SIZE = 8;
    private static final int INTEGER_SIZE = 4;
    private static SecureStorageJNI secureStorageJNI = SecureStorageJNI.getInstance();

    private void throwJNIException(String message) throws SecureStorageException {
        throw new SecureStorageException(message);
    }

    private void throwException(String dataName) throws SecureStorageException {
        if (dataName == null) {
            throw new SecureStorageException("Error: input data are incorrect");
        }
    }

    private void throwException(boolean result, String message) throws SecureStorageException {
        if (result) {
            throw new SecureStorageException(message);
        }
    }

    public boolean putOption(String dataName, char[] dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 2);
        bb.asCharBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array(), inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, char dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, BigInteger.valueOf((long) dataBlock).toByteArray(), inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, String dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, dataBlock.getBytes(), inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, byte[] dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, dataBlock, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, byte dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        boolean z = false;
        try {
            result = secureStorageJNI.put(dataName, new byte[]{dataBlock}, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (!result) {
            z = true;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, int[] dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 4);
        bb.asIntBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array(), inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, int dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, BigInteger.valueOf((long) dataBlock).toByteArray(), inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, long[] dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 8);
        bb.asLongBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array(), inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, long dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, BigInteger.valueOf(dataBlock).toByteArray(), inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, double[] dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 8);
        bb.asDoubleBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array(), inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, double dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, (dataBlock + "").getBytes(), inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, boolean[] dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean z = false;
        throwException(dataBlock == null, "Error: input data are incorrect");
        byte[] dataInBytes = new byte[dataBlock.length];
        for (int i = 0; i < dataInBytes.length; i++) {
            if (dataBlock[i]) {
                dataInBytes[i] = 1;
            } else {
                dataInBytes[i] = 0;
            }
        }
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, dataInBytes, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (!result) {
            z = true;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean putOption(String dataName, boolean dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, (dataBlock + "").getBytes(), inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public char[] getCharArrayOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        char[] publicText = new char[(result.length / 2)];
        ByteBuffer buffer = ByteBuffer.wrap(result);
        buffer.rewind();
        int i = 0;
        while (buffer.hasRemaining()) {
            publicText[i] = buffer.getChar();
            i++;
        }
        return publicText;
    }

    public char getCharOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return (char) new BigInteger(result).intValue();
    }

    public byte[] getByteArrayOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return result;
    }

    public byte getByteOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return result[0];
    }

    public int[] getIntArrayOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        int[] publicText = new int[(result.length / 4)];
        ByteBuffer buffer = ByteBuffer.wrap(result);
        buffer.rewind();
        int i = 0;
        while (buffer.hasRemaining()) {
            publicText[i] = buffer.getInt();
            i++;
        }
        return publicText;
    }

    public int getIntOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return new BigInteger(result).intValue();
    }

    public long[] getLongArrayOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        long[] publicText = new long[(result.length / 8)];
        ByteBuffer buffer = ByteBuffer.wrap(result);
        buffer.rewind();
        int i = 0;
        while (buffer.hasRemaining()) {
            publicText[i] = buffer.getLong();
            i++;
        }
        return publicText;
    }

    public long getLongOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return new BigInteger(result).longValue();
    }

    public String getStringOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return new String(result);
    }

    public double[] getDoubleArrayOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        double[] publicText = new double[(result.length / 8)];
        ByteBuffer buffer = ByteBuffer.wrap(result);
        buffer.rewind();
        int i = 0;
        while (buffer.hasRemaining()) {
            publicText[i] = buffer.getDouble();
            i++;
        }
        return publicText;
    }

    public double getDoubleOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return Double.parseDouble(new String(result));
    }

    public boolean[] getBooleanArrayOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        boolean[] publicText = new boolean[result.length];
        for (int i = 0; i < publicText.length; i++) {
            if (result[i] == 1) {
                publicText[i] = true;
            } else {
                publicText[i] = false;
            }
        }
        return publicText;
    }

    public boolean getBooleanOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return Boolean.parseBoolean(new String(result));
    }

    public byte[] encryptOption(byte[] dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.encrypt(dataBlock, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public byte[] decryptOption(byte[] dataBlock, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.decrypt(dataBlock, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: input data are incorrect");
        return result;
    }

    public boolean deleteOption(String dataName, SecureStorageOptionFlag inOptionFlag) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.delete(dataName, inOptionFlag.getFlag());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: input data are incorrect");
        return result;
    }

    public boolean provisionKey(SecureStorageOptionProvisionKey pKey) throws SecureStorageException {
        boolean result = false;
        try {
            result = secureStorageJNI.provisionKey(pKey.getKey());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public boolean isKeyProvisioned() throws SecureStorageException {
        boolean result = false;
        try {
            result = secureStorageJNI.isKeyProvisioned();
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public boolean eraseKey() throws SecureStorageException {
        boolean result = false;
        try {
            result = secureStorageJNI.eraseKey();
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public static boolean isSupported() {
        return secureStorageJNI.isSupported();
    }

    public static int version() {
        return secureStorageJNI.getVersion();
    }

    public static int initialize() {
        return secureStorageJNI.initialize();
    }

    public static class SecureStorageException extends Exception {
        public static final String SECURE_STORAGE_ERROR_AUTHENTICATION = "Error: authentication failure";
        public static final String SECURE_STORAGE_ERROR_INPUT_DATA = "Error: input data are incorrect";
        public static final String SECURE_STORAGE_ERROR_INTERNAL = "Error: internal error";
        private static final long serialVersionUID = 1;

        public SecureStorageException() {
        }

        public SecureStorageException(String detailMessage) {
            super(detailMessage);
        }
    }
}
