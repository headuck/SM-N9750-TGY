package com.sec.android.securestorage;

import com.sec.android.securestorage.SecureStorageOption;

public class SecureStorageOptionProvisionKey {
    public static final int SS_PROVISIONED_KEY_128 = 16;
    public static final int SS_PROVISIONED_KEY_192 = 24;
    public static final int SS_PROVISIONED_KEY_256 = 32;
    private byte[] keyData = null;

    public SecureStorageOptionProvisionKey(byte[] inKey) throws SecureStorageOption.SecureStorageException {
        setKey(inKey);
    }

    public void setKey(byte[] inKey) throws SecureStorageOption.SecureStorageException {
        if (inKey == null) {
            throw new SecureStorageOption.SecureStorageException("Error: input data are incorrect");
        } else if (inKey.length == 16 || inKey.length == 24 || inKey.length == 32) {
            clearKey();
            this.keyData = (byte[]) inKey.clone();
        } else {
            throw new SecureStorageOption.SecureStorageException("Error: input data are incorrect");
        }
    }

    public byte[] getKey() {
        return this.keyData;
    }

    public void clearKey() {
        byte[] bArr = this.keyData;
        if (bArr != null) {
            System.arraycopy(new byte[bArr.length], 0, bArr, 0, bArr.length);
            this.keyData = null;
        }
    }
}
