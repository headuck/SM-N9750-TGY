package com.samsung.android.server.wifi;

import android.app.ActivityManager;
import android.os.Debug;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.LocalLog;
import android.util.Log;
import com.samsung.android.service.vaultkeeper.VaultKeeperManager;
import com.sec.android.securestorage.SecureStorage;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class WifiPasswordManager {
    private static final int IV_LENGTH = 12;
    private static final int MAX_FAIL_COUNTER = 5;
    private static final int MAX_RETRY_COUNTER = 3;
    private static final boolean NO_SHIP = Debug.semIsProductDev();
    private static final String SS_KEY = "wifiConfig";
    private static final String TAG = "WifiPasswordManager";
    private static final int TAG_LENGTH = 16;
    private static final String USER_VAULT_NAME = "WIFI";
    private static byte[] mSeedKey;
    private static WifiPasswordManager sInstance;
    private boolean mCanAccessDataFiles;
    private int mFailCounter;
    private boolean mIsSupported;
    private LocalLog mLocalLog;
    private SecureStorage mSecureStorage;
    private VaultKeeperManager mVkm;

    public static synchronized WifiPasswordManager getInstance() {
        WifiPasswordManager wifiPasswordManager;
        synchronized (WifiPasswordManager.class) {
            if (sInstance == null) {
                sInstance = new WifiPasswordManager();
            }
            wifiPasswordManager = sInstance;
        }
        return wifiPasswordManager;
    }

    private WifiPasswordManager() {
        if (SecureStorage.isSupported()) {
            this.mSecureStorage = new SecureStorage();
        } else {
            this.mVkm = VaultKeeperManager.getInstance(USER_VAULT_NAME);
        }
        this.mLocalLog = new LocalLog(ActivityManager.isLowRamDeviceStatic() ? 32 : 64);
        this.mIsSupported = true;
    }

    public boolean isSupported() {
        return this.mIsSupported;
    }

    private boolean isSeedKeyReady() {
        byte[] bArr = mSeedKey;
        return (bArr == null || bArr.length == 0) ? false : true;
    }

    private byte[] makeRandomByte(int number) {
        byte[] key = new byte[number];
        new SecureRandom().nextBytes(key);
        return key;
    }

    private boolean isPossibleDataAccess() {
        boolean possibleToAccess;
        boolean z = this.mCanAccessDataFiles;
        if (z) {
            return z;
        }
        String cryptoType = SystemProperties.get("ro.crypto.type", "none");
        logi("ro.crypto.type is  " + cryptoType);
        String voldDecrypt = SystemProperties.get("vold.decrypt", "none");
        logi("vold.decrypt is " + voldDecrypt);
        if ("block".equals(cryptoType)) {
            possibleToAccess = "trigger_restart_framework".equals(voldDecrypt);
        } else {
            possibleToAccess = true;
        }
        logi("possible to access data : " + possibleToAccess);
        this.mCanAccessDataFiles = possibleToAccess;
        return possibleToAccess;
    }

    /* Debug info: failed to restart local var, previous not found, register: 4 */
    private synchronized boolean setupSeedKey() {
        if (!this.mIsSupported) {
            return false;
        }
        if (!isPossibleDataAccess()) {
            return false;
        }
        if (this.mFailCounter > 5) {
            return false;
        }
        int counter = 0;
        while (true) {
            if (counter < 3) {
                if (counter == 2) {
                    logi("KEY maybe not exist, generate new KEY");
                    if (!generateKey()) {
                        loge("Failed to push new generated key");
                        break;
                    }
                }
                if (getSeedKey()) {
                    return true;
                }
                counter++;
            }
        }
        this.mFailCounter++;
        loge("setup seed key failure " + this.mFailCounter);
        return false;
    }

    private boolean getSeedKey() {
        long startTime = 0;
        if (NO_SHIP) {
            startTime = SystemClock.elapsedRealtime();
        }
        String str = "";
        if (this.mSecureStorage != null) {
            try {
                if (NO_SHIP) {
                    logi("try to get seed KEY from secure storage");
                }
                mSeedKey = hexStringToByteArray(this.mSecureStorage.getString(SS_KEY));
                if (isSeedKeyReady()) {
                    this.mFailCounter = 0;
                    StringBuilder sb = new StringBuilder();
                    sb.append("get KEY : success");
                    if (NO_SHIP) {
                        str = " duration:" + (SystemClock.elapsedRealtime() - startTime);
                    }
                    sb.append(str);
                    logi(sb.toString());
                    return true;
                }
            } catch (SecureStorage.SecureStorageException e) {
                mSeedKey = null;
                if ("Error: input data are incorrect".equals(e.getMessage())) {
                    loge("setupSeedKey: SS error SECURE_STORAGE_ERROR_INPUT_DATA " + e);
                } else if ("Error: authentication failure".equals(e.getMessage())) {
                    loge("setupSeedKey: SS error SECURE_STORAGE_ERROR_AUTHENTICATION " + e);
                } else if ("Error: internal error".equals(e.getMessage())) {
                    loge("setupSeedKey: SS error SECURE_STORAGE_ERROR_INTERNAL " + e);
                } else {
                    loge("setupSeedKey: SS error else " + e);
                }
            }
            loge("get seed KEY failure");
        } else {
            VaultKeeperManager vaultKeeperManager = this.mVkm;
            if (vaultKeeperManager != null) {
                mSeedKey = vaultKeeperManager.read(2);
                if (isSeedKeyReady()) {
                    this.mFailCounter = 0;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("get KEY : success");
                    if (NO_SHIP) {
                        str = " duration:" + (SystemClock.elapsedRealtime() - startTime);
                    }
                    sb2.append(str);
                    logi(sb2.toString());
                    return true;
                }
                loge("get seed KEY failure. error vkm code: " + this.mVkm.getErrorCode());
            }
        }
        return false;
    }

    private boolean generateKey() {
        long startTime = 0;
        if (NO_SHIP) {
            startTime = SystemClock.elapsedRealtime();
        }
        String str = "";
        if (this.mSecureStorage != null) {
            String randomString = null;
            for (int i = 0; i < 3; i++) {
                randomString = byteArrayToHexString(makeRandomByte(16));
                if (randomString == null || randomString.length() == 0) {
                    loge("generate KEY failure : can't make random string. retry " + i);
                }
            }
            if (randomString == null || randomString.length() == 0) {
                loge("generate KEY failure : can't make random string.");
                return false;
            }
            if (NO_SHIP) {
                logi("random string ready, push new KEY");
            }
            try {
                this.mSecureStorage.put(SS_KEY, randomString);
                StringBuilder sb = new StringBuilder();
                sb.append("generate KEY success");
                if (NO_SHIP) {
                    str = " duration:" + (SystemClock.elapsedRealtime() - startTime);
                }
                sb.append(str);
                logi(sb.toString());
                return true;
            } catch (SecureStorage.SecureStorageException e) {
                if ("Error: input data are incorrect".equals(e.getMessage())) {
                    loge("generateKey: SS error SECURE_STORAGE_ERROR_INPUT_DATA " + e);
                } else if ("Error: authentication failure".equals(e.getMessage())) {
                    loge("generateKey: SS error SECURE_STORAGE_ERROR_AUTHENTICATION " + e);
                } else if ("Error: internal error".equals(e.getMessage())) {
                    loge("generateKey: SS error SECURE_STORAGE_ERROR_INTERNAL " + e);
                } else {
                    loge("generateKey: SS error else " + e);
                }
                loge("generate KEY failure");
            }
        } else {
            String randomString2 = this.mVkm;
            if (randomString2 != null) {
                int result = randomString2.write(2, makeRandomByte(16));
                if (NO_SHIP) {
                    logi("push new generated KEY, result:" + result);
                }
                if (result == 0) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("generate KEY success");
                    if (NO_SHIP) {
                        str = " duration:" + (SystemClock.elapsedRealtime() - startTime);
                    }
                    sb2.append(str);
                    logi(sb2.toString());
                    return true;
                }
                loge("generate KEY failure. vkm result: " + result);
            }
            return false;
        }
    }

    public String encrypt(String password) {
        if (!this.mIsSupported || password == null || password.length() == 0) {
            return password;
        }
        if (isSeedKeyReady() || setupSeedKey()) {
            try {
                byte[] iv = new byte[12];
                new SecureRandom().nextBytes(iv);
                SecretKeySpec keySpec = new SecretKeySpec(mSeedKey, "AES");
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(1, keySpec, gcmParameterSpec);
                byte[] encryption = cipher.doFinal(password.getBytes());
                byte[] output = new byte[(encryption.length + 12)];
                for (int i = 0; i < 12; i++) {
                    output[i] = cipher.getIV()[i];
                }
                for (int i2 = 0; i2 < encryption.length; i2++) {
                    output[i2 + 12] = encryption[i2];
                }
                return byteArrayToHexString(output);
            } catch (Exception e) {
                loge("encrypt failure : exception occurs " + e);
                return "";
            }
        } else {
            loge("encrypt failure : seed is not ready, failCount: " + this.mFailCounter);
            return password;
        }
    }

    public String decrypt(String password) {
        if (!this.mIsSupported || password == null || password.length() == 0) {
            return password;
        }
        if (isSeedKeyReady() || setupSeedKey()) {
            int length = password.length();
            if (length > 1 && password.charAt(0) == '\"' && password.charAt(length - 1) == '\"') {
                if (NO_SHIP) {
                    logi("decrypt ignore : password is not encrypted");
                }
                return password;
            }
            try {
                byte[] bytePassword = hexStringToByteArray(password);
                byte[] iv = new byte[12];
                byte[] encryption = new byte[(bytePassword.length - 12)];
                for (int i = 0; i < 12; i++) {
                    iv[i] = bytePassword[i];
                }
                for (int i2 = 0; i2 < encryption.length; i2++) {
                    encryption[i2] = bytePassword[i2 + 12];
                }
                SecretKeySpec keySpec = new SecretKeySpec(mSeedKey, "AES");
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(2, keySpec, new GCMParameterSpec(128, iv));
                return new String(cipher.doFinal(encryption));
            } catch (NegativeArraySizeException e) {
                loge("decrypt failure : exception occurs " + e);
                return password;
            } catch (Exception e2) {
                loge("decrypt failure : exception occurs " + e2);
                return password;
            }
        } else {
            loge("decrypt failure : seed is not ready, failCount: " + this.mFailCounter);
            return password;
        }
    }

    private String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X", new Object[]{Integer.valueOf(bytes[i] & 255)}));
        }
        return sb.toString();
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[(len / 2)];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private void loge(String s) {
        if (NO_SHIP) {
            Log.e(TAG, s);
        }
        LocalLog localLog = this.mLocalLog;
        if (localLog != null) {
            localLog.log(s);
        }
    }

    private void logi(String s) {
        if (NO_SHIP) {
            Log.i(TAG, s);
        }
        LocalLog localLog = this.mLocalLog;
        if (localLog != null) {
            localLog.log(s);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of WifiPasswordManager");
        pw.println(" - supported: " + this.mIsSupported);
        StringBuilder sb = new StringBuilder();
        sb.append("   (secure storage: ");
        String str = "false";
        sb.append(this.mSecureStorage == null ? str : "true");
        sb.append(")");
        pw.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("   (vaultkeeper: ");
        if (this.mVkm != null) {
            str = "true";
        }
        sb2.append(str);
        sb2.append(")");
        pw.println(sb2.toString());
        pw.println(" - fail counter: " + this.mFailCounter);
        LocalLog localLog = this.mLocalLog;
        if (localLog != null) {
            localLog.dump(fd, pw, args);
        }
    }
}
