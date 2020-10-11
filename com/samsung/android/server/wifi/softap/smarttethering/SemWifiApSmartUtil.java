package com.samsung.android.server.wifi.softap.smarttethering;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.icu.text.Transliterator;
import android.net.ConnectivityManager;
import android.net.InterfaceConfiguration;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.INetworkManagementService;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import com.android.server.wifi.WifiInjector;
import com.samsung.android.net.wifi.SemWifiApContentProviderHelper;
import com.samsung.android.net.wifi.SemWifiApMacInfo;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class SemWifiApSmartUtil {
    public static final int AES_ENC = 1;
    public static final int ALLOWED_USER = 3;
    public static final byte BLE_BATT_0 = 0;
    public static final byte BLE_BATT_1 = 8;
    public static final byte BLE_BATT_2 = 16;
    public static final byte BLE_BATT_3 = 24;
    public static final byte BLE_BATT_4 = 32;
    public static final byte BLE_BATT_5 = 40;
    public static final byte BLE_BATT_6 = 48;
    public static final byte BLE_BATT_7 = 56;
    public static final byte BLE_BATT_MASK = 56;
    public static final byte BLE_HIDDEN = 2;
    public static final byte BLE_LTE_5G = -64;
    public static final byte BLE_NOTCONNECTED = 0;
    public static final byte BLE_NW_MASK = -64;
    public static final int BLE_PACKET_SIZE = 24;
    public static final int BLE_SCAN_RSP_SIZE = 27;
    public static final byte BLE_WIFI = 64;
    public static final byte BLE_WPA2 = 4;
    public static UUID CHARACTERISTIC_AUTH_STATUS = UUID.fromString("7c7bdb04-27a2-11e9-ab14-d663bd873d93");
    public static UUID CHARACTERISTIC_CLIENT_MAC = UUID.fromString("369a01a7-fcd9-48ad-b642-11e93092d3d4");
    public static UUID CHARACTERISTIC_D2D_CLIENT_BOND_STATUS = UUID.fromString("e9a6d869-d2bc-4fc6-9386-323c8c8b2d96");
    public static UUID CHARACTERISTIC_ENCRYPTED_AUTH_ID = UUID.fromString("7c7bd829-27a2-11e9-ab14-d663bd873d93");
    public static UUID CHARACTERISTIC_FAMILY_ID = UUID.fromString("c6c333ec-4e53-407c-b00a-20dd347fe8fd");
    public static UUID CHARACTERISTIC_MHS_BOND_STATUS = UUID.fromString("7c7bdb04-27b2-11e9-ab14-d663bd873d93");
    public static UUID CHARACTERISTIC_MHS_SIDE_GET_TIME = UUID.fromString("c323b908-f4f9-11e9-802a-5aa538984bd8");
    public static UUID CHARACTERISTIC_MHS_STATUS_UUID = UUID.fromString("7c7bd820-27a2-11e9-ab14-d663bd873d93");
    public static UUID CHARACTERISTIC_MHS_VER_UPDATE = UUID.fromString("c323b4e4-f4f9-11e9-802a-5aa538984bd8");
    public static UUID CHARACTERISTIC_NOTIFY_ACCEPT_INVITATION = UUID.fromString("d11aa5ef-de73-46ce-867f-e381c961245f");
    public static UUID CHARACTERISTIC_NOTIFY_MHS_ENABLED = UUID.fromString("7f7cdbf4-27a2-11f9-ab14-d663bd873d93");
    public static final int CMD_CLIENT_ADV = 1;
    public static final int CMD_CLIENT_D2D_ADV = 4;
    public static final int CMD_MHS_ADV = 2;
    public static final int CMD_MHS_D2D_ADV = 3;
    public static final int CONNECTED_5G = 2;
    public static final int CONNECTED_LTE = 3;
    public static final int CONNECTED_UNKWON = 0;
    public static final int CONNECTED_WIFI = 1;
    public static UUID D2D_SERVICE_UUID = UUID.fromString("cb50061d-ab7e-4edd-9f07-ad2986b6c684");
    public static final int HASH_FAMILYID_SIZE = 4;
    public static final int HASH_GUID_SIZE = 4;
    public static final int MANUFACTURE_ID = 117;
    public static final int MHS_MACID_SIZE = 3;
    public static final int SAME_FAMILY = 2;
    public static final int SAME_USER = 1;
    public static final int SERVICE_ID = 18;
    public static UUID SERVICE_UUID = UUID.fromString("7c7bcc5e-27a2-11e9-ab14-d663bd873d93");
    public static int SIM_CARD_ERROR = -2;
    private static final String TAG = "SemWifiApSmartUtil";
    public static final int VERSION_ID = 1;
    private static HashMap<UUID, String> attributes = new HashMap<>();
    private static int mIsSamsungDevice = -1;
    private static String mSamsungDeviceName = null;
    private final String AUTO_HOTSPOT_ACCEPT;
    private final int BUFFER_SIZE;
    private boolean DBG;
    private boolean isJDMDevice;
    private Context mContext;
    private LocalLog mLocalLog;
    private INetworkManagementService mNwService;
    private String mPkgVer;
    String mWifiChipMAC;
    private WifiManager mWifiManager;
    private char[] mapping_MAC;

    static {
        attributes.put(SERVICE_UUID, "Smart Tethering Service");
        attributes.put(CHARACTERISTIC_MHS_STATUS_UUID, "Smart Tethering MHS status");
        attributes.put(CHARACTERISTIC_NOTIFY_MHS_ENABLED, "Smart Tethering notify MHS status");
        attributes.put(CHARACTERISTIC_AUTH_STATUS, "read auth status from MHS");
        attributes.put(CHARACTERISTIC_ENCRYPTED_AUTH_ID, "send  auth info from client");
        attributes.put(CHARACTERISTIC_MHS_BOND_STATUS, "read bond status from MHS");
        attributes.put(D2D_SERVICE_UUID, "Smart Tethering D2D Service");
        attributes.put(CHARACTERISTIC_FAMILY_ID, "Smart Tethering FAMILY ID");
        attributes.put(CHARACTERISTIC_CLIENT_MAC, "Smart Tethering Get client Wi-Fi mac");
        attributes.put(CHARACTERISTIC_NOTIFY_ACCEPT_INVITATION, "Smart Tethering notify family group invitation status");
        attributes.put(CHARACTERISTIC_MHS_VER_UPDATE, "Smart Tethering mhs ver update");
        attributes.put(CHARACTERISTIC_MHS_SIDE_GET_TIME, "Smart Tethering get MHS side system time");
    }

    public SemWifiApSmartUtil(Context context, LocalLog tLocalLog) {
        this.DBG = "eng".equals(Build.TYPE) || Debug.semIsProductDev();
        this.isJDMDevice = "in_house".contains("jdm");
        this.mNwService = null;
        this.mWifiChipMAC = null;
        this.AUTO_HOTSPOT_ACCEPT = "/data/misc/wifi_hostapd/smart_tethering.accept";
        this.BUFFER_SIZE = 64;
        this.mPkgVer = null;
        this.mapping_MAC = new char[]{'5', '7', '9', '4', '3', '0', '8', '1', '6', '2', 'f', 'e', 'd', 'c', 'b', 'a'};
        this.mContext = context;
        this.mLocalLog = tLocalLog;
        if (Settings.Secure.getString(this.mContext.getContentResolver(), "smart_samsung_account_name") != null) {
            Settings.Secure.putString(this.mContext.getContentResolver(), "smart_samsung_account_name", (String) null);
        }
        mSamsungDeviceName = getHostNameFromDeviceName();
        Settings.Secure.putInt(this.mContext.getContentResolver(), "smart_tethering_db_ver", 1);
        int mst = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_smart_tethering_settings", 0);
        int family_saved = Settings.Secure.getInt(this.mContext.getContentResolver(), "autohotspot_family_sharing_saved_state", 0);
        int autohotspot_saved = Settings.Secure.getInt(this.mContext.getContentResolver(), "autohotspot_saved_state", 0);
        Log.d(TAG, "Intial value of mst:" + mst + ",family_saved:" + family_saved + ",autohotspot_saved" + autohotspot_saved);
        LocalLog localLog = this.mLocalLog;
        localLog.log("SemWifiApSmartUtil:\t Intial value of mst:" + mst + ",family_saved:" + family_saved + ",autohotspot_saved" + autohotspot_saved);
    }

    public void handleBootCompleted() {
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "smart_tethering_db_migration", 0) != 1) {
            Settings.Secure.putInt(this.mContext.getContentResolver(), "smart_tethering_db_migration", 1);
            Log.d(TAG, "handleBootCompleted: migration data");
            this.mLocalLog.log("SemWifiApSmartUtil:\thandleBootCompleted: migration data");
            String str = Settings.Secure.getString(this.mContext.getContentResolver(), "smart_tethering_guid");
            if (!TextUtils.isEmpty(str)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_guid", str);
                Settings.Secure.putString(this.mContext.getContentResolver(), "smart_tethering_guid", (String) null);
            }
            String str2 = Settings.Secure.getString(this.mContext.getContentResolver(), "smart_tethering_user_name");
            if (!TextUtils.isEmpty(str2)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_user_name", str2);
                Settings.Secure.putString(this.mContext.getContentResolver(), "smart_tethering_user_name", (String) null);
            }
            String str3 = Settings.Secure.getString(this.mContext.getContentResolver(), "smart_tethering_user_profile_name");
            if (!TextUtils.isEmpty(str3)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_user_profile_name", str3);
                Settings.Secure.putString(this.mContext.getContentResolver(), "smart_tethering_user_profile_name", (String) null);
            }
            String str4 = Settings.Secure.getString(this.mContext.getContentResolver(), "smart_tethering_family_user_names");
            if (!TextUtils.isEmpty(str4)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_family_user_names", str4);
                Settings.Secure.putString(this.mContext.getContentResolver(), "smart_tethering_family_user_names", (String) null);
            }
            String str5 = Settings.Secure.getString(this.mContext.getContentResolver(), "smart_tethering_family_guids");
            if (!TextUtils.isEmpty(str5)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_family_guids", str5);
                Settings.Secure.putString(this.mContext.getContentResolver(), "smart_tethering_family_guids", (String) null);
            }
            String str6 = Settings.Secure.getString(this.mContext.getContentResolver(), "smart_tethering_familyid");
            if (!TextUtils.isEmpty(str6)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_familyid", str6);
                Settings.Secure.putString(this.mContext.getContentResolver(), "smart_tethering_familyid", (String) null);
            }
            String str7 = Settings.Secure.getString(this.mContext.getContentResolver(), "smart_tethering_user_icon");
            if (!TextUtils.isEmpty(str7)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_user_icon", str7);
                Settings.Secure.putString(this.mContext.getContentResolver(), "smart_tethering_user_icon", (String) null);
            }
            for (int i = 0; i < 6; i++) {
                ContentResolver contentResolver = this.mContext.getContentResolver();
                String str8 = Settings.Secure.getString(contentResolver, "smart_tethering_family_icons_" + i);
                if (!TextUtils.isEmpty(str8)) {
                    Context context = this.mContext;
                    SemWifiApContentProviderHelper.insert(context, "smart_tethering_family_icons_" + i, str8);
                    ContentResolver contentResolver2 = this.mContext.getContentResolver();
                    Settings.Secure.putString(contentResolver2, "smart_tethering_family_icons_" + i, (String) null);
                }
            }
            String str9 = Settings.Secure.getString(this.mContext.getContentResolver(), "smart_tethering_d2dfamilyid");
            if (!TextUtils.isEmpty(str9)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_d2dfamilyid", str9);
                Settings.Secure.putString(this.mContext.getContentResolver(), "smart_tethering_d2dfamilyid", (String) null);
            }
            String str10 = Settings.Secure.getString(this.mContext.getContentResolver(), "smart_tethering_AES_keys");
            if (!TextUtils.isEmpty(str10)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_AES_keys", str10);
                Settings.Secure.putString(this.mContext.getContentResolver(), "smart_tethering_AES_keys", (String) null);
            }
            String str11 = Settings.Secure.getString(this.mContext.getContentResolver(), "smart_tethering_d2d_Wifimac");
            if (!TextUtils.isEmpty(str11)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_d2d_Wifimac", str11);
                Settings.Secure.putString(this.mContext.getContentResolver(), "smart_tethering_d2d_Wifimac", (String) null);
            }
            String str12 = Settings.Secure.getString(this.mContext.getContentResolver(), "smart_tethering_latest_sa_error_code");
            if (!TextUtils.isEmpty(str12)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_latest_sa_error_code", str12);
                Settings.Secure.putString(this.mContext.getContentResolver(), "smart_tethering_latest_sa_error_code", (String) null);
            }
            long mLong = Settings.Secure.getLong(this.mContext.getContentResolver(), "hash_value_based_on_guid", -1);
            if (mLong != -1) {
                Context context2 = this.mContext;
                SemWifiApContentProviderHelper.insert(context2, "hash_value_based_on_guid", mLong + "");
                Settings.Secure.putLong(this.mContext.getContentResolver(), "hash_value_based_on_guid", -1);
            }
            long mLong2 = Settings.Secure.getLong(this.mContext.getContentResolver(), "hash_value_based_on_familyid", -1);
            if (mLong2 != -1) {
                Context context3 = this.mContext;
                SemWifiApContentProviderHelper.insert(context3, "hash_value_based_on_familyid", mLong2 + "");
                Settings.Secure.putLong(this.mContext.getContentResolver(), "hash_value_based_on_familyid", -1);
            }
            long mLong3 = Settings.Secure.getLong(this.mContext.getContentResolver(), "smart_tethering_sim_value", -1);
            if (mLong3 != -1) {
                Context context4 = this.mContext;
                SemWifiApContentProviderHelper.insert(context4, "smart_tethering_sim_value", mLong3 + "");
                Settings.Secure.putLong(this.mContext.getContentResolver(), "smart_tethering_sim_value", -1);
            }
            long mLong4 = Settings.Secure.getLong(this.mContext.getContentResolver(), "hash_value_based_on_d2dFamilyid", -1);
            if (mLong4 != -1) {
                Context context5 = this.mContext;
                SemWifiApContentProviderHelper.insert(context5, "hash_value_based_on_d2dFamilyid", mLong4 + "");
                Settings.Secure.putLong(this.mContext.getContentResolver(), "hash_value_based_on_d2dFamilyid", -1);
            }
            long mLong5 = Settings.Global.getLong(this.mContext.getContentResolver(), "smart_tethering_last_access_Token", -1);
            if (mLong5 != -1) {
                Context context6 = this.mContext;
                SemWifiApContentProviderHelper.insert(context6, "smart_tethering_last_access_Token", mLong5 + "");
                Settings.Global.putLong(this.mContext.getContentResolver(), "smart_tethering_last_access_Token", -1);
            }
            int count = Settings.Secure.getInt(this.mContext.getContentResolver(), "smart_tethering_family_count", 0);
            if (count != 0) {
                Context context7 = this.mContext;
                SemWifiApContentProviderHelper.insert(context7, "smart_tethering_family_count", count + "");
                Settings.Secure.putInt(this.mContext.getContentResolver(), "smart_tethering_family_count", 0);
            }
            int count2 = Settings.Secure.getInt(this.mContext.getContentResolver(), "smart_tethering_family_sharing_service_registered", 0);
            if (count2 != 0) {
                Context context8 = this.mContext;
                SemWifiApContentProviderHelper.insert(context8, "smart_tethering_family_sharing_service_registered", count2 + "");
                Settings.Secure.putInt(this.mContext.getContentResolver(), "smart_tethering_family_sharing_service_registered", 0);
            }
            long mLong6 = Settings.Global.getLong(this.mContext.getContentResolver(), "smart_tethering_access_Token_count", 0);
            if (mLong6 != 0) {
                Context context9 = this.mContext;
                SemWifiApContentProviderHelper.insert(context9, "smart_tethering_access_Token_count", mLong6 + "");
                Settings.Global.putLong(this.mContext.getContentResolver(), "smart_tethering_access_Token_count", 0);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public String lookup(UUID uuid) {
        String name = attributes.get(uuid);
        return name == null ? uuid.toString() : name;
    }

    /* access modifiers changed from: package-private */
    public long getHashbasedonGuid() {
        String mGuid = SemWifiApContentProviderHelper.get(this.mContext, "hash_value_based_on_guid");
        if (TextUtils.isEmpty(mGuid)) {
            return -1;
        }
        return Long.parseLong(mGuid);
    }

    /* access modifiers changed from: package-private */
    public long getHashbasedonFamilyId() {
        String familyId = SemWifiApContentProviderHelper.get(this.mContext, "hash_value_based_on_familyid");
        if (TextUtils.isEmpty(familyId)) {
            return -1;
        }
        return Long.parseLong(familyId);
    }

    /* access modifiers changed from: package-private */
    public String getSamsungAccount() {
        Account[] samsungAccnts = ((AccountManager) this.mContext.getSystemService("account")).getAccountsByType("com.osp.app.signin");
        if (samsungAccnts.length != 0) {
            Log.d(TAG, "getSamsungAccount:" + samsungAccnts[0]);
            LocalLog localLog = this.mLocalLog;
            localLog.log("SemWifiApSmartUtil:\tgetSamsungAccount:" + samsungAccnts[0]);
            return samsungAccnts[0].name;
        }
        LocalLog localLog2 = this.mLocalLog;
        localLog2.log("SemWifiApSmartUtil,getSamsungAccount:" + null);
        Log.e(TAG, "getSamsungAccount:null");
        return null;
    }

    public String getMHSMacFromInterface() {
        String str = null;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (this.mWifiManager.getWifiApState() != 13) {
            return null;
        }
        this.mNwService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        try {
            InterfaceConfiguration ifcg = this.mNwService.getInterfaceConfig(this.mWifiManager.getWifiApInterfaceName());
            if (ifcg != null) {
                str = ifcg.getHardwareAddress();
            }
        } catch (Exception e) {
        }
        if (str != null && this.DBG) {
            LocalLog localLog = this.mLocalLog;
            localLog.log("SemWifiApSmartUtil:\tgetMHSMacFromInterface is:" + getBytesOfMACForLog(str));
            Log.i(TAG, "getMHSMacFromInterface is:" + getBytesOfMACForLog(str));
        }
        return str;
    }

    /* access modifiers changed from: package-private */
    public String getOwnWifiMac() {
        String str = this.mWifiChipMAC;
        if (this.mWifiChipMAC == null) {
            if (this.isJDMDevice) {
                str = SemWifiApMacInfo.getInstance().readWifiMacInfo();
            } else {
                str = WifiInjector.getInstance().getWifiNative().getVendorConnFileInfo(0);
            }
            this.mWifiChipMAC = str;
        }
        if (str == null) {
            Log.e(TAG, " getOwnWifiMac is null  ");
            str = "02:00:00:00:00:00";
        } else {
            Log.d(TAG, "chipset information is macAddress " + getBytesOfMACForLog(str));
        }
        LocalLog localLog = this.mLocalLog;
        localLog.log("SemWifiApSmartUtil:\tgetOwnWifiMac is: " + getBytesOfMACForLog(str));
        return str;
    }

    /* access modifiers changed from: package-private */
    public byte[] getClientMACbytes() {
        String str = this.mWifiChipMAC;
        byte[] bt = new byte[6];
        if (this.mWifiChipMAC == null) {
            if (this.isJDMDevice) {
                str = SemWifiApMacInfo.getInstance().readWifiMacInfo();
            } else {
                str = WifiInjector.getInstance().getWifiNative().getVendorConnFileInfo(0);
            }
            this.mWifiChipMAC = str;
        }
        if (str == null) {
            Log.e(TAG, " getWiFiMACbytes is null  ");
            str = "02:00:00:00:00:00";
        } else {
            Log.d(TAG, "chipset information is macAddress " + getBytesOfMACForLog(str));
        }
        Log.d(TAG, "getClientMACbytes is:" + getBytesOfMACForLog(str));
        String[] str1 = str.split(":");
        int length = str1.length;
        int i = 0;
        int i2 = 0;
        while (i2 < length) {
            String sr = str1[i2];
            bt[i] = (byte) ((Character.digit(sr.charAt(0), 16) << 4) | Character.digit(sr.charAt(1), 16));
            i2++;
            i++;
        }
        return bt;
    }

    /* access modifiers changed from: package-private */
    public String getActualMACFrom_mappedMAC(String mappedmac) {
        int len = mappedmac.length();
        String mappedmac2 = mappedmac.toLowerCase();
        String ret = "";
        for (int i = 0; i < len; i++) {
            if (mappedmac2.charAt(i) == ':') {
                ret = ret + ":";
            } else if ('0' > mappedmac2.charAt(i) || '9' < mappedmac2.charAt(i)) {
                ret = ret + this.mapping_MAC[(mappedmac2.charAt(i) - 'a') + 10];
            } else {
                ret = ret + this.mapping_MAC[mappedmac2.charAt(i) - '0'];
            }
        }
        return ret;
    }

    /* access modifiers changed from: package-private */
    public byte[] getmappedClientMACbytes() {
        String str = this.mWifiChipMAC;
        byte[] bt = new byte[6];
        if (this.mWifiChipMAC == null) {
            if (this.isJDMDevice) {
                str = SemWifiApMacInfo.getInstance().readWifiMacInfo();
            } else {
                str = WifiInjector.getInstance().getWifiNative().getVendorConnFileInfo(0);
            }
            this.mWifiChipMAC = str;
        }
        if (str == null) {
            Log.e(TAG, " getmappedClientMACbytes is null  ");
            str = "02:00:00:00:00:00";
        } else {
            Log.d(TAG, "chipset information is macAddress " + getBytesOfMACForLog(str));
        }
        String str2 = str.toLowerCase();
        int len = str2.length();
        char[] chars = str2.toCharArray();
        for (int i = 0; i < len; i++) {
            if (chars[i] != ':') {
                char ch = chars[i];
                if ('0' > ch || '9' < ch) {
                    chars[i] = this.mapping_MAC[(ch - 'a') + 10];
                } else {
                    chars[i] = this.mapping_MAC[ch - '0'];
                }
            }
        }
        String[] str1 = new String(chars).split(":");
        int length = str1.length;
        int i2 = 0;
        int i3 = 0;
        while (i3 < length) {
            String sr = str1[i3];
            bt[i2] = (byte) ((Character.digit(sr.charAt(0), 16) << 4) | Character.digit(sr.charAt(1), 16));
            i3++;
            i2++;
        }
        return bt;
    }

    /* access modifiers changed from: package-private */
    public byte[] getBlutoothMACbytes() {
        String str = BluetoothAdapter.getDefaultAdapter().getAddress();
        if (str == null) {
            Log.e(TAG, " getBTMACbytes is null  ");
            str = "02:00:00:00:00:00";
        }
        byte[] bt = new byte[12];
        int i = 0;
        for (String sr : str.split(":")) {
            byte[] mbt = sr.getBytes();
            int i2 = i + 1;
            bt[i] = mbt[0];
            i = i2 + 1;
            bt[i2] = mbt[1];
        }
        return bt;
    }

    static byte[] bytesFromLong(Long value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value.longValue()).array();
    }

    static long convertToLong(byte[] array) {
        return ByteBuffer.wrap(array).getLong();
    }

    public boolean isPackageExists(String targetPackage) {
        try {
            boolean z = false;
            PackageInfo info = this.mContext.getPackageManager().getPackageInfo(targetPackage, 0);
            if (!(info == null || info.applicationInfo == null)) {
                z = true;
            }
            boolean hasPkg = z;
            if (hasPkg) {
                boolean hasPkg2 = info.applicationInfo.enabled;
                this.mPkgVer = Long.toString(info.getLongVersionCode());
                return hasPkg2;
            }
            this.mPkgVer = null;
            Log.d(TAG, "isPackageExists | package is not system app or not available");
            return hasPkg;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Package not found : " + targetPackage);
            return false;
        }
    }

    public static boolean isSamsungDevice() {
        if (mIsSamsungDevice == -1) {
            mIsSamsungDevice = 0;
            if ("samsung".equals(Build.MANUFACTURER)) {
                try {
                    if (!Build.MODEL.startsWith("Nexus")) {
                        mIsSamsungDevice = 1;
                    }
                } catch (Exception e) {
                    Log.d(TAG, " Exception isSamsungDevice" + e);
                }
            }
        }
        if (mIsSamsungDevice == 1) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void removeWifiMACFromRegisteredList(String tWifiMAC) {
        long ident = Binder.clearCallingIdentity();
        try {
            String mD2DWifiAMC = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_d2d_Wifimac");
            String mretList = "";
            Log.d(TAG, " tWifiMAC:" + tWifiMAC);
            Log.d(TAG, " mD2DWifiAMC:" + mD2DWifiAMC);
            if (!TextUtils.isEmpty(mD2DWifiAMC)) {
                String[] splitMAC = mD2DWifiAMC.split("\n");
                int len = Arrays.asList(splitMAC).indexOf(tWifiMAC);
                Log.d(TAG, " len:" + len);
                if (len != -1) {
                    for (int i = 0; i < splitMAC.length; i++) {
                        if (!splitMAC[i].equalsIgnoreCase(tWifiMAC)) {
                            Log.d(TAG, " splitMAC[i]:" + splitMAC[i]);
                            mretList = splitMAC[i] + "\n" + mretList;
                        }
                    }
                    Log.d(TAG, " removed mac:" + tWifiMAC);
                    this.mLocalLog.log("SemWifiApSmartUtil:\tremoved mac:" + tWifiMAC + ",mretList:" + mretList);
                    if (splitMAC.length == 1) {
                        mretList = null;
                    }
                    SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_d2d_Wifimac", mretList);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: finally extract failed */
    public String getHostNameFromDeviceName() {
        long ident = Binder.clearCallingIdentity();
        new StringBuffer();
        String hostname = "Samsung Device";
        try {
            String settingsDeviceName = Settings.Global.getString(this.mContext.getContentResolver(), "device_name");
            if (settingsDeviceName != null) {
                String hostname2 = Transliterator.getInstance("Any-latin; nfd; [:nonspacing mark:] remove; nfc").transliterate(settingsDeviceName).replaceAll("[^[[a-z][A-Z][0-9][ ][-]]]", "").replaceAll(" ", "-");
                if (hostname2.length() > 0) {
                    if (hostname2.charAt(0) == '-') {
                        hostname2 = hostname2.replaceFirst("-+", "");
                    }
                }
                if (hostname2.length() > 0 && hostname2.charAt(hostname2.length() - 1) == '-') {
                    hostname2 = replaceLast(hostname2);
                }
                hostname = hostname2.replaceAll("-+", "-");
            }
            Binder.restoreCallingIdentity(ident);
            Log.d(TAG, "hostname = " + hostname);
            LocalLog localLog = this.mLocalLog;
            localLog.log("SemWifiApSmartUtil:\thostname = " + hostname);
            return hostname;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    private String replaceLast(String str) {
        return new StringBuffer(new StringBuffer(str).reverse().toString().replaceFirst("-+", "")).reverse().toString();
    }

    public String getDeviceName() {
        String mSamsungDeviceName2 = null;
        long ident = Binder.clearCallingIdentity();
        try {
            String systemDeviceName = Settings.System.getString(this.mContext.getContentResolver(), "device_name");
            if (systemDeviceName != null) {
                mSamsungDeviceName2 = systemDeviceName;
            }
            String globalDeviceName = Settings.Global.getString(this.mContext.getContentResolver(), "device_name");
            if (globalDeviceName != null && mSamsungDeviceName2 == null) {
                mSamsungDeviceName2 = globalDeviceName;
            }
            if (mSamsungDeviceName2 == null) {
                mSamsungDeviceName2 = Build.MODEL == null ? "Samsung Mobile" : Build.MODEL;
            }
            return mSamsungDeviceName2;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public byte getNetworkType() {
        NetworkCapabilities nc;
        byte type;
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        ServiceState ss = ((TelephonyManager) this.mContext.getSystemService("phone")).getServiceState();
        Network activeNetwork = cm.getActiveNetwork();
        if (ss == null || activeNetwork == null || (nc = cm.getNetworkCapabilities(activeNetwork)) == null) {
            return 0;
        }
        if (nc.hasTransport(1)) {
            Log.i(TAG, "getNetworkType :isWiFi");
            return 1;
        } else if (nc.hasTransport(0)) {
            Log.i(TAG, "getNetworkType :isMobile");
            if (ss.getNrBearerStatus() == 1) {
                type = 2;
            } else if (ServiceState.isLte(ss.getRilMobileDataRadioTechnology())) {
                type = 3;
            } else {
                type = 0;
            }
            LocalLog localLog = this.mLocalLog;
            localLog.log("SemWifiApSmartUtil:\tgetNetworkType :isMobile" + type);
            return type;
        } else {
            Log.i(TAG, "getNetworkType :No Network");
            return 0;
        }
    }

    /* access modifiers changed from: package-private */
    public String getSameUserName() {
        String mEncString = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_user_name");
        if (TextUtils.isEmpty(mEncString)) {
            return null;
        }
        return AES.decrypt(mEncString, getAccountName());
    }

    /* access modifiers changed from: package-private */
    public String getGuid() {
        long ident = Binder.clearCallingIdentity();
        String mDecString = null;
        try {
            String mGuid = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_guid");
            if (!TextUtils.isEmpty(mGuid)) {
                mDecString = AES.decrypt(mGuid, getAccountName());
            }
            return mDecString;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: package-private */
    public String getFamilyID() {
        long ident = Binder.clearCallingIdentity();
        String mDecString = null;
        try {
            String mFamilyid = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_familyid");
            if (!TextUtils.isEmpty(mFamilyid)) {
                mDecString = AES.decrypt(mFamilyid, getAccountName());
            }
            return mDecString;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: package-private */
    public String getD2DWifiMac() {
        long ident = Binder.clearCallingIdentity();
        try {
            return SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_d2d_Wifimac");
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: package-private */
    public void putD2DWifiMac(String val) {
        long ident = Binder.clearCallingIdentity();
        try {
            SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_d2d_Wifimac", val);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: package-private */
    public void putD2DFamilyID(String id) {
        long ident = Binder.clearCallingIdentity();
        try {
            putHashbasedonD2DFamilyid(generateHashKey(id));
            SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_d2dfamilyid", id);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public String getD2DFamilyID() {
        long ident = Binder.clearCallingIdentity();
        try {
            String mstr = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_d2dfamilyid");
            Binder.restoreCallingIdentity(ident);
            if (TextUtils.isEmpty(mstr)) {
                return null;
            }
            return mstr;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    /* access modifiers changed from: package-private */
    public void putHashbasedonD2DFamilyid(long id) {
        long ident = Binder.clearCallingIdentity();
        try {
            Context context = this.mContext;
            SemWifiApContentProviderHelper.insert(context, "hash_value_based_on_d2dFamilyid", "" + id);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public long getHashbasedonD2DFamilyid() {
        long ident = Binder.clearCallingIdentity();
        try {
            String mstr = SemWifiApContentProviderHelper.get(this.mContext, "hash_value_based_on_d2dFamilyid");
            Binder.restoreCallingIdentity(ident);
            if (TextUtils.isEmpty(mstr)) {
                return -1;
            }
            return Long.parseLong(mstr);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    /* access modifiers changed from: package-private */
    public void checkAndAddD2DFamilyMAC(String m_ST_D2D_WiFiMAC) {
        long ident = Binder.clearCallingIdentity();
        try {
            String mlistMAC = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_d2d_Wifimac");
            String m_ST_D2D_WiFiMAC2 = m_ST_D2D_WiFiMAC.substring(9);
            if (TextUtils.isEmpty(mlistMAC)) {
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_d2d_Wifimac", m_ST_D2D_WiFiMAC2);
            } else if (!Arrays.asList(mlistMAC.split("\n")).contains(m_ST_D2D_WiFiMAC2)) {
                Log.d(TAG, "added D2D AutoHotspot MAC");
                SemWifiApContentProviderHelper.insert(this.mContext, "smart_tethering_d2d_Wifimac", mlistMAC + "\n" + m_ST_D2D_WiFiMAC2);
            } else {
                Log.d(TAG, "same D2D AutoHotspot MAC");
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: package-private */
    public String getlegacySSID() {
        WifiConfiguration mConfig = WifiInjector.getInstance().getWifiApConfigStore().getApConfiguration();
        LocalLog localLog = this.mLocalLog;
        localLog.log("SemWifiApSmartUtil:\tgetlegacySSID:" + mConfig.SSID);
        return mConfig.SSID;
    }

    /* access modifiers changed from: package-private */
    public boolean getlegacySSIDHidden() {
        WifiConfiguration mConfig = WifiInjector.getInstance().getWifiApConfigStore().getApConfiguration();
        LocalLog localLog = this.mLocalLog;
        localLog.log("SemWifiApSmartUtil:\tgetlegacySSIDHidden:" + mConfig.hiddenSSID);
        return mConfig.hiddenSSID;
    }

    /* access modifiers changed from: package-private */
    public String getlegacyPassword() {
        WifiConfiguration mConfig = WifiInjector.getInstance().getWifiApConfigStore().getApConfiguration();
        LocalLog localLog = this.mLocalLog;
        localLog.log("SemWifiApSmartUtil:\tmConfig.getAuthType():" + mConfig.getAuthType());
        if (mConfig.getAuthType() != 0) {
            return mConfig.preSharedKey;
        }
        LocalLog localLog2 = this.mLocalLog;
        localLog2.log("SemWifiApSmartUtil:\tgetlegacyPassword:" + null);
        return null;
    }

    /* access modifiers changed from: package-private */
    public byte getSecurityType() {
        WifiConfiguration mConfig = WifiInjector.getInstance().getWifiApConfigStore().getApConfiguration();
        if (mConfig.getAuthType() == 25) {
            this.mLocalLog.log("SemWifiApSmartUtil:\tisWPA3Enabled");
            return 1;
        } else if (mConfig.getAuthType() != 26) {
            return 0;
        } else {
            this.mLocalLog.log("SemWifiApSmartUtil:\tisWPA3 transition Enabled");
            return 2;
        }
    }

    /* access modifiers changed from: package-private */
    public String getStatusDescription(int status) {
        if (status == 0) {
            return "SUCCESS";
        }
        return "Unknown Status " + status;
    }

    /* access modifiers changed from: package-private */
    public String getStateDescription(int state) {
        if (state == 0) {
            return "Disconnected";
        }
        if (state == 1) {
            return "Connecting";
        }
        if (state == 2) {
            return "Connected";
        }
        if (state == 3) {
            return "Disconnecting";
        }
        return "Unknown State " + state;
    }

    /* access modifiers changed from: package-private */
    public boolean isConnected() {
        NetworkInfo info = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getActiveNetworkInfo();
        if (info != null) {
            Log.i(TAG, "isConnected : " + info.isConnected());
        }
        return info != null && info.isConnected();
    }

    /* access modifiers changed from: package-private */
    public boolean checkIfActiveNetworkHasInternet() {
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }
        NetworkCapabilities nc = cm.getNetworkCapabilities(activeNetwork);
        return nc != null && nc.hasCapability(16);
    }

    public String getPassword() {
        String temp = "";
        Random rand = new Random();
        for (int i = 0; i < 15; i++) {
            temp = temp + rand.nextInt(10);
        }
        return temp;
    }

    public void startSmartTetheringApk(boolean isfamily, boolean isServerCall, String receivedFamilyId) {
        if (!checkIfActiveNetworkHasInternet()) {
            Log.d(TAG, "can't start apk, due to internet not available");
            return;
        }
        LocalLog localLog = this.mLocalLog;
        localLog.log("SemWifiApSmartUtil:\tstartSmartTetheringApk:,isfamily:" + isfamily + ",isServerCall:" + isServerCall + ",receivedFamilyId:" + receivedFamilyId);
        Log.d(TAG, "startSmartTetheringApk:,isfamily:" + isfamily + ",isServerCall:" + isServerCall + ",receivedFamilyId:" + receivedFamilyId);
        Intent qintent = new Intent();
        qintent.setClassName("com.sec.mhs.smarttethering", "com.sec.mhs.smarttethering.SemWifiApBleSAService");
        qintent.putExtra("family", isfamily);
        qintent.putExtra("server_call", isServerCall);
        if (receivedFamilyId != null && receivedFamilyId.startsWith("FMLY")) {
            qintent.putExtra("group_id", receivedFamilyId);
        }
        try {
            this.mContext.startService(qintent);
        } catch (Exception e) {
            Log.e(TAG, "can't start service com.sec.mhs.smarttethering ");
        }
    }

    public void stopSmartTetheringApk() {
        Intent qintent = new Intent();
        qintent.setClassName("com.sec.mhs.smarttethering", "com.sec.mhs.smarttethering.SemWifiApBleSAService");
        try {
            this.mContext.stopService(qintent);
        } catch (Exception e) {
            Log.e(TAG, "can't stop service com.sec.mhs.smarttethering ");
        }
    }

    /* access modifiers changed from: package-private */
    public void putHashbasedonGuid(Long val) {
        Context context = this.mContext;
        SemWifiApContentProviderHelper.insert(context, "hash_value_based_on_guid", "" + val);
    }

    /* access modifiers changed from: package-private */
    public void putHashbasedonFamilyId(Long val) {
        Context context = this.mContext;
        SemWifiApContentProviderHelper.insert(context, "hash_value_based_on_familyid", "" + val);
    }

    /* access modifiers changed from: package-private */
    public int getSamsungAccountCount() {
        long ident = Binder.clearCallingIdentity();
        try {
            return ((AccountManager) this.mContext.getSystemService("account")).getAccountsByType("com.osp.app.signin").length;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: package-private */
    public String getAccountName() {
        long ident = Binder.clearCallingIdentity();
        String name = null;
        try {
            Account[] accountArr = AccountManager.get(this.mContext).getAccountsByType("com.osp.app.signin");
            if (accountArr.length > 0) {
                name = accountArr[0].name;
            }
            return name;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: package-private */
    public long generateHashKey(String guid) {
        if (guid == null) {
            return -1;
        }
        long hash = 0;
        for (int j = 0; j < guid.length(); j++) {
            hash = ((31 * hash) + ((long) guid.charAt(j))) % 9223372036854775806L;
        }
        if (hash < 0) {
            return hash * -1;
        }
        return hash;
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public boolean validateGuidInFamilyUsers(String rguid) {
        String mDec_family_user_guids;
        if (TextUtils.isEmpty(rguid)) {
            return true;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            String mEnc_family_user_guids = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_family_guids");
            Binder.restoreCallingIdentity(ident);
            if (TextUtils.isEmpty(mEnc_family_user_guids) || (mDec_family_user_guids = AES.decrypt(mEnc_family_user_guids, getAccountName())) == null) {
                return false;
            }
            for (String mGuid : mDec_family_user_guids.split("\n")) {
                if (rguid.equals(mGuid)) {
                    Log.i(TAG, "validateGuidInFamilyUsers true");
                    return true;
                }
            }
            return false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    /* access modifiers changed from: package-private */
    public String getUserNameFromFamily(byte[] inguid) {
        int i;
        int i2;
        String mEnc_family_user_names;
        String mEnc_family_user_names2 = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_family_user_names");
        String mEnc_family_user_guids = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_family_guids");
        if (TextUtils.isEmpty(mEnc_family_user_names2)) {
            i = 17042607;
        } else if (TextUtils.isEmpty(mEnc_family_user_guids)) {
            String str = mEnc_family_user_names2;
            i = 17042607;
        } else {
            String mDec_family_user_names = AES.decrypt(mEnc_family_user_names2, getAccountName());
            String mDec_family_user_guids = AES.decrypt(mEnc_family_user_guids, getAccountName());
            if (mDec_family_user_names == null) {
                i2 = 17042607;
            } else if (mDec_family_user_guids == null) {
                String str2 = mEnc_family_user_names2;
                i2 = 17042607;
            } else {
                String[] mDec_family_user_names_list = mDec_family_user_names.split("\n");
                String[] mDec_family_user_guids_list = mDec_family_user_guids.split("\n");
                int count = 0;
                boolean found = false;
                int length = mDec_family_user_guids_list.length;
                int i3 = 0;
                while (true) {
                    if (i3 >= length) {
                        break;
                    }
                    byte[] guidBytes = bytesFromLong(Long.valueOf(generateHashKey(mDec_family_user_guids_list[i3])));
                    int i4 = 0;
                    while (true) {
                        if (i4 >= 4) {
                            mEnc_family_user_names = mEnc_family_user_names2;
                            break;
                        }
                        mEnc_family_user_names = mEnc_family_user_names2;
                        if (inguid[i4 + 2] != guidBytes[i4]) {
                            break;
                        }
                        i4++;
                        mEnc_family_user_names2 = mEnc_family_user_names;
                    }
                    if (i4 == 4) {
                        found = true;
                        break;
                    }
                    count++;
                    i3++;
                    mEnc_family_user_names2 = mEnc_family_user_names;
                }
                if (found) {
                    return mDec_family_user_names_list[count];
                }
                return this.mContext.getString(17042607);
            }
            return this.mContext.getString(i2);
        }
        return this.mContext.getString(i);
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:11:?, code lost:
        r2.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0065, code lost:
        r1 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0066, code lost:
        r1.printStackTrace();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean verifyInSmartApWhiteList(java.lang.String r9) {
        /*
            r8 = this;
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "verifyInSmartApWhiteList in_mac:"
            r0.append(r1)
            r0.append(r9)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "SemWifiApSmartUtil"
            android.util.Log.d(r1, r0)
            r0 = 0
            r2 = 0
            java.io.BufferedReader r3 = new java.io.BufferedReader     // Catch:{ IOException -> 0x006c }
            java.io.FileReader r4 = new java.io.FileReader     // Catch:{ IOException -> 0x006c }
            java.lang.String r5 = "/data/misc/wifi_hostapd/smart_tethering.accept"
            r4.<init>(r5)     // Catch:{ IOException -> 0x006c }
            r5 = 64
            r3.<init>(r4, r5)     // Catch:{ IOException -> 0x006c }
            r2 = r3
        L_0x0027:
            java.lang.String r3 = r2.readLine()     // Catch:{ IOException -> 0x006c }
            r4 = r3
            if (r3 == 0) goto L_0x0060
            r3 = 0
            r5 = 0
            java.lang.String r6 = "#"
            boolean r6 = r4.startsWith(r6)     // Catch:{ IOException -> 0x006c }
            if (r6 == 0) goto L_0x005f
            r6 = 1
            java.lang.String r6 = r4.substring(r6)     // Catch:{ IOException -> 0x006c }
            r5 = r6
            java.lang.String r6 = r2.readLine()     // Catch:{ IOException -> 0x006c }
            r3 = r6
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x006c }
            r6.<init>()     // Catch:{ IOException -> 0x006c }
            java.lang.String r7 = "mac:"
            r6.append(r7)     // Catch:{ IOException -> 0x006c }
            r6.append(r3)     // Catch:{ IOException -> 0x006c }
            java.lang.String r6 = r6.toString()     // Catch:{ IOException -> 0x006c }
            android.util.Log.d(r1, r6)     // Catch:{ IOException -> 0x006c }
            boolean r6 = r3.equalsIgnoreCase(r9)     // Catch:{ IOException -> 0x006c }
            if (r6 == 0) goto L_0x005f
            r0 = 1
            goto L_0x0060
        L_0x005f:
            goto L_0x0027
        L_0x0060:
            r2.close()     // Catch:{ IOException -> 0x0065 }
            goto L_0x007a
        L_0x0065:
            r1 = move-exception
            r1.printStackTrace()
            goto L_0x007a
        L_0x006a:
            r1 = move-exception
            goto L_0x007b
        L_0x006c:
            r1 = move-exception
            r1.printStackTrace()     // Catch:{ all -> 0x006a }
            if (r2 == 0) goto L_0x007a
            r2.close()     // Catch:{ IOException -> 0x0076 }
            goto L_0x007a
        L_0x0076:
            r1 = move-exception
            r1.printStackTrace()
        L_0x007a:
            return r0
        L_0x007b:
            if (r2 == 0) goto L_0x0085
            r2.close()     // Catch:{ IOException -> 0x0081 }
            goto L_0x0085
        L_0x0081:
            r3 = move-exception
            r3.printStackTrace()
        L_0x0085:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil.verifyInSmartApWhiteList(java.lang.String):boolean");
    }

    /* access modifiers changed from: package-private */
    public String getSmartApWhiteList() {
        String ret = "";
        BufferedReader buf = null;
        try {
            BufferedReader buf2 = new BufferedReader(new FileReader("/data/misc/wifi_hostapd/smart_tethering.accept"), 64);
            while (true) {
                String readLine = buf2.readLine();
                String bufReadLine = readLine;
                if (readLine == null) {
                    try {
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (bufReadLine.startsWith("#")) {
                    String name = bufReadLine.substring(1);
                    String mac = buf2.readLine();
                    ret = ret + name + "," + mac.substring(9) + "\n";
                }
            }
            buf2.close();
        } catch (IOException e2) {
            e2.printStackTrace();
            if (buf != null) {
                try {
                    buf.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
        } catch (Throwable th) {
            if (buf != null) {
                try {
                    buf.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }
            throw th;
        }
        return ret;
    }

    /* access modifiers changed from: package-private */
    public boolean checkIfActiveInternetOfWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        return false;
    }

    /* access modifiers changed from: package-private */
    public void SetUserTypefromGattServer(int type) {
        long ident = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_smart_tethering_user_type", type);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: package-private */
    public void sendClientScanResultUpdateIntent(String function) {
        Log.i(TAG, "update has been called from:" + function);
        Intent tintent = new Intent();
        tintent.setAction("com.samsung.android.server.wifi.softap.smarttethering.d2dClientUpdate");
        this.mContext.sendBroadcastAsUser(tintent, UserHandle.ALL);
    }

    /* access modifiers changed from: package-private */
    public String getAESKey(long mCurrentTime) {
        long ident = Binder.clearCallingIdentity();
        String mAESKey = null;
        try {
            String aes_array = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_AES_keys");
            if (TextUtils.isEmpty(aes_array)) {
                Log.e(TAG, " AES keys are null");
                return null;
            }
            String[] mAESKeys = aes_array.split("\n");
            int i = 0;
            while (true) {
                if (i >= mAESKeys.length) {
                    break;
                } else if (Long.parseLong(mAESKeys[i]) - mCurrentTime > 120000) {
                    Log.d(TAG, " getting AES");
                    mAESKey = mAESKeys[i + 1];
                    break;
                } else {
                    i += 2;
                }
            }
            Binder.restoreCallingIdentity(ident);
            return mAESKey;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean Check_MHS_AES_Key() {
        long ident = Binder.clearCallingIdentity();
        String mAESKey = null;
        try {
            String aes_array = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_AES_keys");
            if (TextUtils.isEmpty(aes_array)) {
                Log.e(TAG, " AES keys are null");
                return false;
            }
            String[] mAESKeys = aes_array.split("\n");
            int i = 0;
            while (true) {
                if (i >= mAESKeys.length) {
                    break;
                } else if (Long.parseLong(mAESKeys[i]) - System.currentTimeMillis() > 259200000) {
                    Log.d(TAG, " getting AES");
                    mAESKey = mAESKeys[i + 1];
                    break;
                } else {
                    i += 2;
                }
            }
            Binder.restoreCallingIdentity(ident);
            if (mAESKey != null) {
                return true;
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isEncryptionCanbeUsed(int version, int userType) {
        return version == 1 && userType == 1 && getAESKey(System.currentTimeMillis()) != null;
    }

    /* access modifiers changed from: package-private */
    public boolean isNearByAutohotspotEnabled() {
        boolean isNearByEnabled = Settings.System.getInt(this.mContext.getContentResolver(), "nearby_scanning_enabled", 0) == 1;
        int mst = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_smart_tethering_settings", 0);
        if (isNearByEnabled || mst == 1) {
            return true;
        }
        return false;
    }

    /* JADX INFO: finally extract failed */
    public String getDumpLogs() {
        long ident = Binder.clearCallingIdentity();
        StringBuffer retValue = new StringBuffer();
        try {
            retValue.append("--Hotspot Live --\n");
            StringBuilder sb = new StringBuilder();
            sb.append("smart_tethering_db_ver: ");
            sb.append(Settings.Secure.getInt(this.mContext.getContentResolver(), "smart_tethering_db_ver", 0));
            sb.append("\n");
            retValue.append(sb.toString());
            retValue.append("mPkgVer: " + this.mPkgVer + "\n");
            if (this.DBG) {
                retValue.append("getHostNameFromDeviceName: " + getHostNameFromDeviceName() + "\n");
                retValue.append("getDeviceName: " + getDeviceName() + "\n");
                retValue.append("smart_tethering_guid: " + getGuid() + "\n");
                retValue.append("smart_tethering_user_name: " + getSameUserName() + "\n");
                retValue.append("smart_tethering_familyid: " + getFamilyID() + "\n");
                retValue.append("getHashbasedonGuid(): " + getHashbasedonGuid() + "\n");
                retValue.append(" getHashbasedonFamilyId: " + getHashbasedonFamilyId() + "\n");
                retValue.append(" SamAccount: " + getSamsungAccount() + "\n");
                retValue.append(" getD2DFamilyID: " + getD2DFamilyID() + "\n");
                retValue.append(" getHashbasedonD2DFamilyid: " + getHashbasedonD2DFamilyid() + "\n");
                retValue.append(" d2d_wifi_mac: " + SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_d2d_Wifimac") + "\n");
                retValue.append(" smart_tethering_family_count: " + SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_family_count") + "\n");
                for (String str : getAllAesKeysAsStingArray()) {
                    retValue.append(" AES key: " + generateHashKey(str) + "\n");
                }
                retValue.append("SmartApWhiteList:\n " + getSmartApWhiteList() + "\n");
            }
            Binder.restoreCallingIdentity(ident);
            return retValue.toString();
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    private String[] getAllAesKeysAsStingArray() {
        String aesDbString = SemWifiApContentProviderHelper.get(this.mContext, "smart_tethering_AES_keys");
        if (!TextUtils.isEmpty(aesDbString)) {
            return aesDbString.split("\n");
        }
        Log.e(TAG, "getAllAesKesAsArray() - AES keys are null");
        return new String[0];
    }

    private String getBytesOfMACForLog(String mac) {
        if (this.DBG) {
            return mac;
        }
        return mac.substring(mac.length() - 5);
    }
}
