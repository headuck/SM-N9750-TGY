package com.sec.android.securestorage;

public final class SecureStorageOptionFlag {
    public static final byte SS_OPTION_CUSTOM_KERNEL_DISALLOW = 1;
    public static final byte SS_OPTION_DEFAULT = 0;
    public static final byte SS_OPTION_PROVISIONED_KEY = 2;
    private byte OptionFlag;

    public SecureStorageOptionFlag(byte inFlag) {
        this.OptionFlag = 0;
        setFlag(inFlag);
    }

    public SecureStorageOptionFlag() {
        this.OptionFlag = 0;
        this.OptionFlag = 0;
    }

    public void setFlag(byte inFlag) {
        this.OptionFlag = inFlag;
    }

    public byte getFlag() {
        return this.OptionFlag;
    }
}
