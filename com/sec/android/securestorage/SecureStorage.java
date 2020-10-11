package com.sec.android.securestorage;

import com.android.server.wifi.hotspot2.soap.SppConstants;
import com.samsung.android.util.LibraryVersionQuery;
import com.sec.android.securestorage.SecureStorageJNI;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

public class SecureStorage implements LibraryVersionQuery {
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

    public boolean put(String dataName, char[] dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 2);
        bb.asCharBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, char[] dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        throwException(password == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 2);
        bb.asCharBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array(), password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, char dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, BigInteger.valueOf((long) dataBlock).toByteArray());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, char dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, BigInteger.valueOf((long) dataBlock).toByteArray(), password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, String dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, dataBlock.getBytes("UTF-8"));
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, String dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        throwException(password == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, dataBlock.getBytes("UTF-8"), password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, byte[] dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, dataBlock);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, byte[] dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        throwException(password == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, dataBlock, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, byte dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        boolean z = false;
        try {
            result = secureStorageJNI.put(dataName, new byte[]{dataBlock});
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (!result) {
            z = true;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, byte dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, new byte[]{dataBlock}, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, int[] dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 4);
        bb.asIntBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, int[] dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        throwException(password == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 4);
        bb.asIntBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array(), password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, int dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, BigInteger.valueOf((long) dataBlock).toByteArray());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, int dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, BigInteger.valueOf((long) dataBlock).toByteArray(), password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, long[] dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 8);
        bb.asLongBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, long[] dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        throwException(password == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 8);
        bb.asLongBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array(), password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, long dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, BigInteger.valueOf(dataBlock).toByteArray());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, long dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, BigInteger.valueOf(dataBlock).toByteArray(), password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, double[] dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 8);
        bb.asDoubleBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array());
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, double[] dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        throwException(password == null, "Error: input data are incorrect");
        ByteBuffer bb = ByteBuffer.allocate(dataBlock.length * 8);
        bb.asDoubleBuffer().put(dataBlock);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, bb.array(), password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, double dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, (dataBlock + "").getBytes("UTF-8"));
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, double dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, (dataBlock + "").getBytes("UTF-8"), password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, boolean[] dataBlock) throws SecureStorageException {
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
            result = secureStorageJNI.put(dataName, dataInBytes);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (!result) {
            z = true;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, boolean[] dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = false;
        throwException(dataBlock == null, "Error: input data are incorrect");
        throwException(password == null, "Error: input data are incorrect");
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
            result = secureStorageJNI.put(dataName, dataInBytes, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (!result) {
            z = true;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, boolean dataBlock) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, (dataBlock + "").getBytes("UTF-8"));
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
        throwException(!result, "Error: internal error");
        return result;
    }

    public boolean put(String dataName, boolean dataBlock, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        boolean result = false;
        try {
            result = secureStorageJNI.put(dataName, (dataBlock + "").getBytes("UTF-8"), password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
        if (result) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public char[] getCharArray(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
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

    public char[] getCharArray(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = false;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result == null) {
            z = true;
        }
        throwException(z, "Error: input data are incorrect");
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

    public char getChar(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return (char) new BigInteger(result).intValue();
    }

    public char getChar(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: input data are incorrect");
        return (char) new BigInteger(result).intValue();
    }

    public byte[] getByteArray(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return result;
    }

    public byte[] getByteArray(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: input data are incorrect");
        return result;
    }

    public byte getByte(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return result[0];
    }

    public byte getByte(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: input data are incorrect");
        return result[0];
    }

    public int[] getIntArray(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
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

    public int[] getIntArray(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = false;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result == null) {
            z = true;
        }
        throwException(z, "Error: input data are incorrect");
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

    public int getInt(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return new BigInteger(result).intValue();
    }

    public int getInt(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: input data are incorrect");
        return new BigInteger(result).intValue();
    }

    public long[] getLongArray(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
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

    public long[] getLongArray(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = false;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result == null) {
            z = true;
        }
        throwException(z, "Error: input data are incorrect");
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

    public long getLong(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        return new BigInteger(result).longValue();
    }

    public long getLong(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: input data are incorrect");
        return new BigInteger(result).longValue();
    }

    public String getString(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        try {
            return new String(result, "UTF-8");
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
    }

    public String getString(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: input data are incorrect");
        try {
            return new String(result, "UTF-8");
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
    }

    public double[] getDoubleArray(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
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

    public double[] getDoubleArray(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = false;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result == null) {
            z = true;
        }
        throwException(z, "Error: input data are incorrect");
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

    public double getDouble(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        try {
            return Double.parseDouble(new String(result, "UTF-8"));
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
    }

    public double getDouble(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: input data are incorrect");
        try {
            return Double.parseDouble(new String(result, "UTF-8"));
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
    }

    public boolean[] getBooleanArray(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
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

    public boolean[] getBooleanArray(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
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

    public boolean getBoolean(String dataName) throws SecureStorageException {
        throwException(dataName);
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(result == null, "Error: input data are incorrect");
        try {
            return Boolean.parseBoolean(new String(result, "UTF-8"));
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
    }

    public boolean getBoolean(String dataName, String password) throws SecureStorageException {
        throwException(dataName);
        boolean z = true;
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.get(dataName, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: input data are incorrect");
        try {
            return Boolean.parseBoolean(new String(result, "UTF-8"));
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        }
    }

    public byte[] encrypt(byte[] dataBlock) throws SecureStorageException {
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.encrypt(dataBlock);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public byte[] encrypt(byte[] dataBlock, String password) throws SecureStorageException {
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.encrypt(dataBlock, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: internal error");
        return result;
    }

    public byte[] decrypt(byte[] dataBlock) throws SecureStorageException {
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.decrypt(dataBlock);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: input data are incorrect");
        return result;
    }

    public byte[] decrypt(byte[] dataBlock, String password) throws SecureStorageException {
        boolean z = true;
        throwException(dataBlock == null, "Error: input data are incorrect");
        throwException(password == null, "Error: input data are incorrect");
        byte[] result = null;
        try {
            result = secureStorageJNI.decrypt(dataBlock, password);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        if (result != null) {
            z = false;
        }
        throwException(z, "Error: input data are incorrect");
        return result;
    }

    public boolean delete(String dataName) throws SecureStorageException {
        throwException(dataName);
        boolean result = false;
        try {
            result = secureStorageJNI.delete(dataName);
        } catch (SecureStorageJNI.SecureStorageExceptionJNI e) {
            throwJNIException(e.getMessage());
        }
        throwException(!result, "Error: input data are incorrect");
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

    public String getLibraryVersion() {
        return new String(SppConstants.SUPPORTED_SPP_VERSION);
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
