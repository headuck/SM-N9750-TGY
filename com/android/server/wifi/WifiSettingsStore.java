package com.android.server.wifi;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class WifiSettingsStore {
    static final int WIFI_DISABLED = 0;
    private static final int WIFI_DISABLED_AIRPLANE_ON = 3;
    static final int WIFI_ENABLED = 1;
    private static final int WIFI_ENABLED_AIRPLANE_OVERRIDE = 2;
    private boolean mAirplaneModeOn = false;
    private boolean mCheckSavedStateAtBoot = false;
    private final Context mContext;
    private boolean mCustomScanAlwaysAvailablePolicy = false;
    private int mPersistWifiState = 0;
    private boolean mScanAlwaysAvailable;
    private boolean mScanAlwaysAvailableForAutoWifi = false;

    WifiSettingsStore(Context context) {
        this.mContext = context;
        this.mAirplaneModeOn = getPersistedAirplaneModeOn();
        this.mPersistWifiState = getPersistedWifiState();
        this.mScanAlwaysAvailable = getPersistedScanAlwaysAvailable();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:16:0x001d, code lost:
        return r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0025, code lost:
        return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean isWifiToggleEnabled() {
        /*
            r4 = this;
            monitor-enter(r4)
            boolean r0 = r4.mCheckSavedStateAtBoot     // Catch:{ all -> 0x0026 }
            r1 = 1
            if (r0 != 0) goto L_0x0010
            r4.mCheckSavedStateAtBoot = r1     // Catch:{ all -> 0x0026 }
            boolean r0 = r4.testAndClearWifiSavedState()     // Catch:{ all -> 0x0026 }
            if (r0 == 0) goto L_0x0010
            monitor-exit(r4)
            return r1
        L_0x0010:
            boolean r0 = r4.mAirplaneModeOn     // Catch:{ all -> 0x0026 }
            r2 = 0
            if (r0 == 0) goto L_0x001e
            int r0 = r4.mPersistWifiState     // Catch:{ all -> 0x0026 }
            r3 = 2
            if (r0 != r3) goto L_0x001b
            goto L_0x001c
        L_0x001b:
            r1 = r2
        L_0x001c:
            monitor-exit(r4)
            return r1
        L_0x001e:
            int r0 = r4.mPersistWifiState     // Catch:{ all -> 0x0026 }
            if (r0 == 0) goto L_0x0023
            goto L_0x0024
        L_0x0023:
            r1 = r2
        L_0x0024:
            monitor-exit(r4)
            return r1
        L_0x0026:
            r0 = move-exception
            monitor-exit(r4)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiSettingsStore.isWifiToggleEnabled():boolean");
    }

    public synchronized boolean isAirplaneModeOn() {
        return this.mAirplaneModeOn;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0015, code lost:
        return !r1.mAirplaneModeOn && r1.mScanAlwaysAvailable;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean isScanAlwaysAvailable() {
        /*
            r1 = this;
            monitor-enter(r1)
            boolean r0 = r1.mCustomScanAlwaysAvailablePolicy     // Catch:{ all -> 0x0016 }
            if (r0 == 0) goto L_0x0009
            boolean r0 = r1.mScanAlwaysAvailableForAutoWifi     // Catch:{ all -> 0x0016 }
            monitor-exit(r1)
            return r0
        L_0x0009:
            boolean r0 = r1.mAirplaneModeOn     // Catch:{ all -> 0x0016 }
            if (r0 != 0) goto L_0x0013
            boolean r0 = r1.mScanAlwaysAvailable     // Catch:{ all -> 0x0016 }
            if (r0 == 0) goto L_0x0013
            r0 = 1
            goto L_0x0014
        L_0x0013:
            r0 = 0
        L_0x0014:
            monitor-exit(r1)
            return r0
        L_0x0016:
            r0 = move-exception
            monitor-exit(r1)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiSettingsStore.isScanAlwaysAvailable():boolean");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0022, code lost:
        return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean handleWifiToggled(boolean r3) {
        /*
            r2 = this;
            monitor-enter(r2)
            boolean r0 = r2.mAirplaneModeOn     // Catch:{ all -> 0x0023 }
            r1 = 0
            if (r0 == 0) goto L_0x000e
            boolean r0 = r2.isAirplaneToggleable()     // Catch:{ all -> 0x0023 }
            if (r0 != 0) goto L_0x000e
            monitor-exit(r2)
            return r1
        L_0x000e:
            r0 = 1
            if (r3 == 0) goto L_0x001e
            boolean r1 = r2.mAirplaneModeOn     // Catch:{ all -> 0x0023 }
            if (r1 == 0) goto L_0x001a
            r1 = 2
            r2.persistWifiState(r1)     // Catch:{ all -> 0x0023 }
            goto L_0x0021
        L_0x001a:
            r2.persistWifiState(r0)     // Catch:{ all -> 0x0023 }
            goto L_0x0021
        L_0x001e:
            r2.persistWifiState(r1)     // Catch:{ all -> 0x0023 }
        L_0x0021:
            monitor-exit(r2)
            return r0
        L_0x0023:
            r3 = move-exception
            monitor-exit(r2)
            throw r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiSettingsStore.handleWifiToggled(boolean):boolean");
    }

    /* Debug info: failed to restart local var, previous not found, register: 5 */
    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0048, code lost:
        return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean handleAirplaneModeToggled() {
        /*
            r5 = this;
            monitor-enter(r5)
            boolean r0 = r5.isAirplaneSensitive()     // Catch:{ all -> 0x0049 }
            r1 = 0
            if (r0 != 0) goto L_0x000a
            monitor-exit(r5)
            return r1
        L_0x000a:
            boolean r0 = r5.getPersistedAirplaneModeOn()     // Catch:{ all -> 0x0049 }
            r5.mAirplaneModeOn = r0     // Catch:{ all -> 0x0049 }
            boolean r0 = r5.mAirplaneModeOn     // Catch:{ all -> 0x0049 }
            r2 = 3
            r3 = 2
            r4 = 1
            if (r0 == 0) goto L_0x002c
            int r0 = r5.mPersistWifiState     // Catch:{ all -> 0x0049 }
            if (r0 != r4) goto L_0x0047
            android.content.Context r0 = r5.mContext     // Catch:{ all -> 0x0049 }
            boolean r0 = com.samsung.android.server.wifi.WifiMobileDeviceManager.isAllowToUseWifi(r0, r1)     // Catch:{ all -> 0x0049 }
            if (r0 == 0) goto L_0x0027
            r5.persistWifiState(r2)     // Catch:{ all -> 0x0049 }
            goto L_0x0047
        L_0x0027:
            r5.persistWifiState(r3)     // Catch:{ all -> 0x0049 }
            monitor-exit(r5)
            return r1
        L_0x002c:
            boolean r0 = r5.testAndClearWifiSavedState()     // Catch:{ all -> 0x0049 }
            if (r0 != 0) goto L_0x003a
            int r0 = r5.mPersistWifiState     // Catch:{ all -> 0x0049 }
            if (r0 == r3) goto L_0x003a
            int r0 = r5.mPersistWifiState     // Catch:{ all -> 0x0049 }
            if (r0 != r2) goto L_0x003d
        L_0x003a:
            r5.persistWifiState(r4)     // Catch:{ all -> 0x0049 }
        L_0x003d:
            android.content.Context r0 = r5.mContext     // Catch:{ all -> 0x0049 }
            boolean r0 = com.samsung.android.server.wifi.WifiMobileDeviceManager.isAllowToUseWifi(r0, r4)     // Catch:{ all -> 0x0049 }
            if (r0 != 0) goto L_0x0047
            monitor-exit(r5)
            return r1
        L_0x0047:
            monitor-exit(r5)
            return r4
        L_0x0049:
            r0 = move-exception
            monitor-exit(r5)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiSettingsStore.handleAirplaneModeToggled():boolean");
    }

    /* access modifiers changed from: package-private */
    public synchronized void handleWifiScanAlwaysAvailableToggled() {
        this.mScanAlwaysAvailable = getPersistedScanAlwaysAvailable();
    }

    public void obtainScanAlwaysAvailablePolicy(boolean enable) {
        this.mCustomScanAlwaysAvailablePolicy = enable;
    }

    public boolean isManagedByAutoWifi() {
        return this.mCustomScanAlwaysAvailablePolicy;
    }

    public void setScanAlwaysAvailable(boolean enable) {
        this.mScanAlwaysAvailableForAutoWifi = enable;
    }

    /* access modifiers changed from: package-private */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("mPersistWifiState " + this.mPersistWifiState);
        pw.println("mAirplaneModeOn " + this.mAirplaneModeOn);
    }

    private void persistWifiState(int state) {
        ContentResolver cr = this.mContext.getContentResolver();
        this.mPersistWifiState = state;
        Settings.Global.putInt(cr, "wifi_on", state);
    }

    private boolean isAirplaneSensitive() {
        String airplaneModeRadios = Settings.Global.getString(this.mContext.getContentResolver(), "airplane_mode_radios");
        return airplaneModeRadios == null || airplaneModeRadios.contains("wifi");
    }

    private boolean isAirplaneToggleable() {
        String toggleableRadios = Settings.Global.getString(this.mContext.getContentResolver(), "airplane_mode_toggleable_radios");
        return toggleableRadios != null && toggleableRadios.contains("wifi");
    }

    private boolean testAndClearWifiSavedState() {
        int wifiSavedState = getWifiSavedState();
        if (wifiSavedState == 1) {
            setWifiSavedState(0);
        }
        if (wifiSavedState == 1) {
            return true;
        }
        return false;
    }

    public void setWifiSavedState(int state) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_saved_state", state);
    }

    public int getWifiSavedState() {
        try {
            return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_saved_state");
        } catch (Settings.SettingNotFoundException e) {
            return 0;
        }
    }

    public synchronized void persistWifiApState(int state) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_ap_saved_state", state);
    }

    public synchronized int getPersistedWifiApState() {
        try {
        } catch (Settings.SettingNotFoundException e) {
            return 0;
        }
        return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_ap_saved_state");
    }

    private int getPersistedWifiState() {
        ContentResolver cr = this.mContext.getContentResolver();
        try {
            return Settings.Global.getInt(cr, "wifi_on");
        } catch (Settings.SettingNotFoundException e) {
            Settings.Global.putInt(cr, "wifi_on", 0);
            return 0;
        }
    }

    private boolean getPersistedAirplaneModeOn() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    private boolean getPersistedScanAlwaysAvailable() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) == 1;
    }

    public int getLocationModeSetting(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), "location_mode", 0, -2);
    }
}
