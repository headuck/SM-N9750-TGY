package com.android.server.wifi.tcp;

import android.content.Context;
import android.os.Debug;
import android.util.Log;

public class WifiPackageInfo {
    private static final boolean DBG = Debug.semIsProductDev();
    private static final String TAG = "WifiPackageInfo";
    public static final int WIFI_APPLICATION_CATEGORY_NONE_QUERY_MAX = 3;
    public static final int WIFI_DATA_USAGE_HIGH = 3;
    public static final int WIFI_DATA_USAGE_LOW = 1;
    public static final int WIFI_DATA_USAGE_MID = 2;
    public static final int WIFI_DATA_USAGE_NONE = 0;
    public static final int WIFI_USAGE_PATTERN_BROWSER = 3;
    public static final int WIFI_USAGE_PATTERN_CHAT = 2;
    public static final int WIFI_USAGE_PATTERN_NONE = 0;
    public static final int WIFI_USAGE_PATTERN_RADIO = 1;
    public static final int WIFI_USAGE_PATTERN_STREAMING = 4;
    private String mCategory = WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE;
    private int mCategoryUpdateFailCount = 0;
    private int mDataUsage = 0;
    private int mDetectedCount = 0;
    private boolean mHasInternetPermission = false;
    private boolean mIsBrowsingApp = false;
    private boolean mIsChattingApp = false;
    private boolean mIsGamingApp = false;
    private boolean mIsLaunchableApp = false;
    private boolean mIsSwitchable = false;
    private boolean mIsSystemApp = false;
    private boolean mIsVoip = false;
    private final String mPackageName;
    private final int mUid;
    private int mUsagePattern = 0;

    public WifiPackageInfo(Context context, int uid, String packageName) {
        this.mPackageName = packageName;
        this.mUid = uid;
        this.mCategory = WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE;
        this.mIsChattingApp = WifiTransportLayerUtils.isChatApp(packageName);
        this.mIsVoip = false;
        this.mIsGamingApp = WifiTransportLayerUtils.isSemGamePackage(packageName) || this.mCategory == WifiTransportLayerUtils.CATEGORY_PLAYSTORE_GAME;
        this.mIsBrowsingApp = WifiTransportLayerUtils.isBrowserApp(context, packageName);
        this.mIsSystemApp = WifiTransportLayerUtils.isSystemApp(context, packageName);
        if (this.mIsSystemApp) {
            this.mCategory = WifiTransportLayerUtils.CATEGORY_PLAYSTORE_SYSTEM;
        }
        this.mIsLaunchableApp = WifiTransportLayerUtils.isLauchablePackage(context, packageName);
        updateSwitchable();
        this.mDetectedCount = 0;
        this.mDataUsage = 0;
        this.mUsagePattern = 0;
        this.mCategoryUpdateFailCount = 0;
        this.mHasInternetPermission = WifiTransportLayerUtils.hasPermission(context, packageName, "android.permission.INTERNET");
        if (DBG) {
            Log.d(TAG, "CREATED - " + toString());
        }
    }

    public WifiPackageInfo(int uid, String packageName, String category, boolean chattingApp, boolean voip, boolean game, boolean browsing, boolean systemApp, boolean launchable, boolean switchable, int detectedCount, int dataUsage, int usagePattern, int categoryUpdateFailCount, boolean hasInternetPermission) {
        this.mUid = uid;
        this.mPackageName = packageName;
        this.mCategory = category;
        this.mIsChattingApp = chattingApp;
        this.mIsVoip = voip;
        this.mIsGamingApp = game;
        this.mIsBrowsingApp = browsing;
        this.mIsSystemApp = systemApp;
        this.mIsLaunchableApp = launchable;
        this.mIsSwitchable = switchable;
        this.mDetectedCount = detectedCount;
        this.mDataUsage = dataUsage;
        this.mUsagePattern = usagePattern;
        this.mCategoryUpdateFailCount = categoryUpdateFailCount;
        this.mHasInternetPermission = hasInternetPermission;
    }

    public WifiPackageInfo(WifiPackageInfo info) {
        this.mUid = info.getUid();
        this.mPackageName = info.getPackageName();
        this.mCategory = info.getCategory();
        this.mIsChattingApp = info.isChatApp();
        this.mIsVoip = info.isVoip();
        this.mIsGamingApp = info.isGamingApp();
        this.mIsBrowsingApp = info.isBrowsingApp();
        this.mIsSystemApp = info.isSystemApp();
        this.mIsLaunchableApp = info.isLaunchable();
        this.mIsSwitchable = info.isSwitchable();
        this.mDetectedCount = info.getDetectedCount();
        this.mDataUsage = info.getDataUsage();
        this.mUsagePattern = info.getUsagePattern();
        this.mCategoryUpdateFailCount = info.getCategoryUpdateFailCount();
        this.mHasInternetPermission = info.hasInternetPermission();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("UID:");
        sb.append(this.mUid);
        sb.append(", PackageName:");
        sb.append(this.mPackageName);
        sb.append(", Category:");
        sb.append(this.mCategory);
        sb.append(", ChattingApp:");
        sb.append(this.mIsChattingApp);
        sb.append(", VoIP:");
        sb.append(this.mIsVoip);
        sb.append(", Game:");
        sb.append(this.mIsGamingApp);
        sb.append(", Browsing:");
        sb.append(this.mIsBrowsingApp);
        sb.append(", SystemApp:");
        sb.append(this.mIsSystemApp);
        sb.append(", Launchable:");
        sb.append(this.mIsLaunchableApp);
        sb.append(", Switchable:");
        sb.append(this.mIsSwitchable);
        sb.append(", DetectedCount:");
        sb.append(this.mDetectedCount);
        sb.append(", DataUsage:");
        sb.append(this.mDataUsage);
        sb.append(", UsagePattern:");
        sb.append(this.mUsagePattern);
        sb.append(", CategoryUpdateFailCount:");
        sb.append(this.mCategoryUpdateFailCount);
        sb.append(", HasInternetPermission:");
        sb.append(this.mHasInternetPermission);
        return sb.toString();
    }

    public int getUid() {
        return this.mUid;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public boolean isChatApp() {
        return this.mIsChattingApp;
    }

    public boolean isVoip() {
        return this.mIsVoip;
    }

    public void setIsVoip(boolean isVoip) {
        this.mIsVoip = isVoip;
    }

    public boolean isGamingApp() {
        return this.mIsGamingApp;
    }

    public boolean isBrowsingApp() {
        return this.mIsBrowsingApp;
    }

    public boolean isSystemApp() {
        return this.mIsSystemApp;
    }

    public String getCategory() {
        return this.mCategory;
    }

    public void setCategory(String category) {
        this.mCategory = category;
        updateSwitchable();
    }

    public boolean isSwitchable() {
        return this.mIsSwitchable;
    }

    private void updateSwitchable() {
        this.mIsSwitchable = getSwitchable();
    }

    private boolean getSwitchable() {
        if (isChatApp()) {
            return true;
        }
        if (!isLaunchable() || isSystemApp() || isBrowsingApp() || isGamingApp() || isSkipCategory(getCategory())) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isSkipCategory(java.lang.String r4) {
        /*
            r3 = this;
            int r0 = r4.hashCode()
            r1 = 1
            r2 = 0
            switch(r0) {
                case -1833998801: goto L_0x0050;
                case -201031457: goto L_0x0046;
                case -135275590: goto L_0x003c;
                case 2180082: goto L_0x0032;
                case 2402104: goto L_0x0028;
                case 289768878: goto L_0x001e;
                case 1381037124: goto L_0x0014;
                case 2066319421: goto L_0x000a;
                default: goto L_0x0009;
            }
        L_0x0009:
            goto L_0x005a
        L_0x000a:
            java.lang.String r0 = "FAILED"
            boolean r0 = r4.equals(r0)
            if (r0 == 0) goto L_0x0009
            r0 = r2
            goto L_0x005b
        L_0x0014:
            java.lang.String r0 = "MAPS_AND_NAVIGATION"
            boolean r0 = r4.equals(r0)
            if (r0 == 0) goto L_0x0009
            r0 = 4
            goto L_0x005b
        L_0x001e:
            java.lang.String r0 = "VIDEO_PLAYERS"
            boolean r0 = r4.equals(r0)
            if (r0 == 0) goto L_0x0009
            r0 = 6
            goto L_0x005b
        L_0x0028:
            java.lang.String r0 = "NONE"
            boolean r0 = r4.equals(r0)
            if (r0 == 0) goto L_0x0009
            r0 = r1
            goto L_0x005b
        L_0x0032:
            java.lang.String r0 = "GAME"
            boolean r0 = r4.equals(r0)
            if (r0 == 0) goto L_0x0009
            r0 = 3
            goto L_0x005b
        L_0x003c:
            java.lang.String r0 = "FINANCE"
            boolean r0 = r4.equals(r0)
            if (r0 == 0) goto L_0x0009
            r0 = 2
            goto L_0x005b
        L_0x0046:
            java.lang.String r0 = "AUTO_AND_VEHICLES"
            boolean r0 = r4.equals(r0)
            if (r0 == 0) goto L_0x0009
            r0 = 7
            goto L_0x005b
        L_0x0050:
            java.lang.String r0 = "SYSTEM"
            boolean r0 = r4.equals(r0)
            if (r0 == 0) goto L_0x0009
            r0 = 5
            goto L_0x005b
        L_0x005a:
            r0 = -1
        L_0x005b:
            switch(r0) {
                case 0: goto L_0x005f;
                case 1: goto L_0x005f;
                case 2: goto L_0x005f;
                case 3: goto L_0x005f;
                case 4: goto L_0x005f;
                case 5: goto L_0x005f;
                case 6: goto L_0x005f;
                case 7: goto L_0x005f;
                default: goto L_0x005e;
            }
        L_0x005e:
            return r2
        L_0x005f:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r2 = "isSkipCategory - skip:"
            r0.append(r2)
            r0.append(r4)
            java.lang.String r0 = r0.toString()
            java.lang.String r2 = "WifiPackageInfo"
            android.util.Log.d(r2, r0)
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiPackageInfo.isSkipCategory(java.lang.String):boolean");
    }

    public boolean isLaunchable() {
        return this.mIsLaunchableApp;
    }

    public int getDetectedCount() {
        return this.mDetectedCount;
    }

    public void setDetectedCount(int detectedCount) {
        this.mDetectedCount = detectedCount;
    }

    public int getDataUsage() {
        return this.mDataUsage;
    }

    public int getUsagePattern() {
        return this.mUsagePattern;
    }

    public void updatePackageInfo(Context context) {
        if (DBG) {
            Log.d(TAG, "updatePackageInfo");
        }
        if (!this.mIsChattingApp) {
            this.mIsChattingApp = WifiTransportLayerUtils.isChatApp(this.mPackageName);
        }
        boolean z = false;
        if (!this.mIsGamingApp) {
            this.mIsGamingApp = WifiTransportLayerUtils.isSemGamePackage(this.mPackageName) || this.mCategory == WifiTransportLayerUtils.CATEGORY_PLAYSTORE_GAME;
        }
        if (this.mIsBrowsingApp || WifiTransportLayerUtils.isBrowserApp(context, this.mPackageName)) {
            z = true;
        }
        this.mIsBrowsingApp = z;
        this.mIsSystemApp = WifiTransportLayerUtils.isSystemApp(context, this.mPackageName);
        if (this.mIsSystemApp) {
            this.mCategory = WifiTransportLayerUtils.CATEGORY_PLAYSTORE_SYSTEM;
        }
        this.mIsLaunchableApp = WifiTransportLayerUtils.isLauchablePackage(context, this.mPackageName);
        updateSwitchable();
        this.mHasInternetPermission = WifiTransportLayerUtils.hasPermission(context, this.mPackageName, "android.permission.INTERNET");
    }

    public int getCategoryUpdateFailCount() {
        return this.mCategoryUpdateFailCount;
    }

    public void addCategoryUpdateFailCount() {
        this.mCategoryUpdateFailCount++;
    }

    public boolean hasInternetPermission() {
        return this.mHasInternetPermission;
    }
}
