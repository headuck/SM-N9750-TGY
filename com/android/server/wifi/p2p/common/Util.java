package com.android.server.wifi.p2p.common;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class Util {
    public static int DISABLED = 0;
    public static int ENABLED = 1;
    public static String KEY_ALLOW_TO_CONNECT = "quick_connect_allow_connect";
    public static String KEY_CONTACT_ONLY = "quick_connect_contact_only";
    private static final int LENGTH_PHONE_NUMBER = 8;
    public static String MODEL = null;
    private static final String[] OWNER_NUMBER_PROJ = {"data1"};
    private static final String TAG = "WifiP2pUtil";

    public static String getDeviceName(Context context) {
        String deviceName = Settings.System.getString(context.getContentResolver(), "device_name");
        if (deviceName == null) {
            deviceName = Settings.Global.getString(context.getContentResolver(), "device_name");
        }
        if (deviceName != null) {
            return deviceName;
        }
        if (Build.MODEL != null) {
            return Build.MODEL;
        }
        return "Samsung Mobile";
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0033, code lost:
        if (r1 != null) goto L_0x0035;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0035, code lost:
        r1.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x003f, code lost:
        if (r1 == null) goto L_0x0042;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0042, code lost:
        return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String retrieveContact(android.content.Context r10, java.lang.String r11) {
        /*
            java.lang.String r0 = "display_name"
            r1 = 0
            r2 = 0
            android.content.ContentResolver r3 = r10.getContentResolver()     // Catch:{ Exception -> 0x003b }
            if (r3 == 0) goto L_0x0033
            android.net.Uri r4 = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI     // Catch:{ Exception -> 0x003b }
            java.lang.String r5 = android.net.Uri.encode(r11)     // Catch:{ Exception -> 0x003b }
            android.net.Uri r5 = android.net.Uri.withAppendedPath(r4, r5)     // Catch:{ Exception -> 0x003b }
            java.lang.String[] r6 = new java.lang.String[]{r0}     // Catch:{ Exception -> 0x003b }
            r7 = 0
            r8 = 0
            r9 = 0
            r4 = r3
            android.database.Cursor r4 = r4.query(r5, r6, r7, r8, r9)     // Catch:{ Exception -> 0x003b }
            r1 = r4
            if (r1 == 0) goto L_0x0033
            boolean r4 = r1.moveToNext()     // Catch:{ Exception -> 0x003b }
            if (r4 == 0) goto L_0x0033
            int r0 = r1.getColumnIndex(r0)     // Catch:{ Exception -> 0x003b }
            java.lang.String r0 = r1.getString(r0)     // Catch:{ Exception -> 0x003b }
            r2 = r0
        L_0x0033:
            if (r1 == 0) goto L_0x0042
        L_0x0035:
            r1.close()
            goto L_0x0042
        L_0x0039:
            r0 = move-exception
            goto L_0x0043
        L_0x003b:
            r0 = move-exception
            r0.printStackTrace()     // Catch:{ all -> 0x0039 }
            if (r1 == 0) goto L_0x0042
            goto L_0x0035
        L_0x0042:
            return r2
        L_0x0043:
            if (r1 == 0) goto L_0x0048
            r1.close()
        L_0x0048:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.common.Util.retrieveContact(android.content.Context, java.lang.String):java.lang.String");
    }

    public static String getMyMobileNumber(Context context) {
        String phoneNumber = getOwnerSystemNumber(context);
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            return phoneNumber;
        }
        String phoneNumber2 = getOwnerContactNumber(context.getContentResolver(), 2);
        if (phoneNumber2 != null && !phoneNumber2.isEmpty()) {
            return phoneNumber2;
        }
        String phoneNumber3 = getAccountNumber(context);
        if (phoneNumber3 == null || phoneNumber3.isEmpty()) {
            return null;
        }
        return phoneNumber3;
    }

    public static String cutNumber(String number) {
        String phoneNumber = number.replaceAll("[^\\d]", "");
        if (phoneNumber == null || phoneNumber.length() <= 8) {
            return phoneNumber;
        }
        return phoneNumber.substring(phoneNumber.length() - 8);
    }

    private static String getAccountNumber(Context context) {
        AccountManager am = AccountManager.get(context);
        String phoneNumber = null;
        if (am != null) {
            for (Account ac : am.getAccounts()) {
                String str = ac.name;
                if (ac.type.equals("com.whatsapp")) {
                    phoneNumber = ac.name;
                }
            }
        }
        return phoneNumber;
    }

    private static String getOwnerSystemNumber(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService("phone");
        String number = null;
        if (manager != null) {
            number = manager.getLine1Number();
        }
        if (number == null || number.length() != 10 || !number.startsWith("000000")) {
            return number;
        }
        Log.d(TAG, "CONTACT_Info - getOwnerSystemNumber : this is not normal");
        return null;
    }

    private static String getOwnerContactNumber(ContentResolver resolver, int numberType) {
        String phoneNumber = null;
        Cursor c = null;
        final CancellationSignal mCancelSignal = new CancellationSignal();
        Thread mCancelThread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(500);
                    if (mCancelSignal != null) {
                        mCancelSignal.cancel();
                    }
                } catch (InterruptedException e) {
                    Log.i(Util.TAG, "query was done or stopped. mCancelThread called Interrupt()");
                }
            }
        };
        mCancelThread.start();
        try {
            Cursor c2 = resolver.query(Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, "data"), OWNER_NUMBER_PROJ, "mimetype = 'vnd.android.cursor.item/phone_v2' AND data2= ?", new String[]{String.valueOf(numberType)}, (String) null, mCancelSignal);
            if (c2 != null && c2.moveToFirst()) {
                phoneNumber = c2.getString(0);
            }
            if (c2 != null) {
                try {
                    c2.close();
                } catch (Exception e) {
                    Log.e(TAG, "getOwnerContactNumber - Exception while closing : " + e);
                }
            }
        } catch (Exception e2) {
            Log.e(TAG, "mCancelSignal is executed");
            Log.e(TAG, "getOwnerContactNumber - Exception : " + e2);
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e3) {
                    Log.e(TAG, "getOwnerContactNumber - Exception while closing : " + e3);
                }
            }
        } catch (Throwable th) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e4) {
                    Log.e(TAG, "getOwnerContactNumber - Exception while closing : " + e4);
                }
            }
            mCancelThread.interrupt();
            throw th;
        }
        mCancelThread.interrupt();
        return phoneNumber;
    }

    public static String byteToString(byte[] object) {
        if (object == null) {
            return null;
        }
        StringBuilder hex = new StringBuilder(object.length * 2);
        for (byte b : object) {
            hex.append("0123456789abcdef".charAt((b & 240) >> 4));
            hex.append("0123456789abcdef".charAt(b & 15));
        }
        return hex.toString();
    }

    public static byte[] stringToByte(String hex) {
        if (hex == null || hex.length() == 0) {
            return null;
        }
        byte[] raw = new byte[(hex.length() / 2)];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) Integer.parseInt(hex.substring(i * 2, (i * 2) + 2), 16);
        }
        return raw;
    }

    public static String stringToHexString(String string) {
        String hexString = "";
        for (byte b : string.getBytes()) {
            hexString = (hexString + Integer.toString((b & 240) >> 4, 16)) + Integer.toString(b & 15, 16);
        }
        return hexString;
    }

    public static String hexStringToString(String hexString) {
        byte[] hexBytes = new byte[(hexString.length() / 2)];
        int j = 0;
        int i = 0;
        while (i < hexString.length()) {
            hexBytes[j] = Byte.parseByte(hexString.substring(i, i + 2), 16);
            i += 2;
            j++;
        }
        return new String(hexBytes);
    }

    public static boolean isMac(String str) {
        if (Pattern.compile("^([0-9a-fA-F][0-9a-fA-F]:){5}([0-9a-fA-F][0-9a-fA-F])$").matcher(str).find()) {
            return true;
        }
        return false;
    }

    public static String getTopActivityName(Context context) {
        ActivityManager activityMgr = (ActivityManager) context.getSystemService("activity");
        if (activityMgr == null) {
            return "unknown";
        }
        try {
            List<ActivityManager.RunningTaskInfo> tasks = activityMgr.getRunningTasks(1);
            if (tasks == null || tasks.isEmpty()) {
                return "unknown";
            }
            return tasks.get(0).topActivity.getClassName();
        } catch (Exception e) {
            Log.e(TAG, "getTopActivityName - exception : " + e);
            return "unknown";
        }
    }

    public static String getTopProcessName(Context context) {
        ActivityManager activityMgr = (ActivityManager) context.getSystemService("activity");
        if (activityMgr == null) {
            return "unknown";
        }
        try {
            List<ActivityManager.RunningAppProcessInfo> processInfo = activityMgr.getRunningAppProcesses();
            if (processInfo == null || processInfo.isEmpty()) {
                return "unknown";
            }
            return processInfo.get(0).processName;
        } catch (Exception e) {
            Log.e(TAG, "getTopProcessName - exception : " + e);
            return "unknown";
        }
    }

    public static boolean isScreenLocked(Context context) {
        if (((KeyguardManager) context.getSystemService("keyguard")).inKeyguardRestrictedInputMode()) {
            return true;
        }
        return false;
    }

    public static int countOccurrences(String input, String word) {
        int count = 0;
        while (Pattern.compile(word).matcher(input).find()) {
            count++;
        }
        return count;
    }

    public static int countContact(ArrayList<Uri> uris) {
        int count = 0;
        Iterator<Uri> it = uris.iterator();
        while (it.hasNext()) {
            String uriString = it.next().toString();
            if (uriString.contains("as_multi_vcard")) {
                count += uriString.split("%3A").length;
            } else {
                count++;
            }
        }
        Log.d(TAG, "countContact - count : " + count);
        if (count == 0) {
            return 1;
        }
        return count;
    }

    public static int countWebpage(ArrayList<Uri> uris) {
        int count = 0;
        Iterator<Uri> it = uris.iterator();
        while (it.hasNext()) {
            String uriString = it.next().toString();
            count = count + countOccurrences(uriString, "http://") + countOccurrences(uriString, "https://");
        }
        Log.d(TAG, "countWebpage - count : " + count);
        if (count == 0) {
            return 1;
        }
        return count;
    }

    public static boolean isLocaleRTL() {
        return isLocaleRTL(Locale.getDefault());
    }

    public static boolean isLocaleRTL(Locale locale) {
        String iso639 = locale.getLanguage();
        return "ar".equals(iso639) || "fa".equals(iso639) || "he".equals(iso639) || "ur".equals(iso639) || "yi".equals(iso639) || "iw".equals(iso639) || "ji".equals(iso639);
    }

    public static int getDefaultPhotoBackgroundColor(long contactId) {
        if (contactId < 1) {
            return 17170759;
        }
        int i = (int) (contactId % 4);
        if (i == 1) {
            return 17170760;
        }
        if (i == 2) {
            return 17170761;
        }
        if (i != 3) {
            return 17170759;
        }
        return 17170762;
    }

    public static float getCornerRadius(Resources res, int height) {
        return ((float) height) / 2.0f;
    }

    public static int dpTopx(Context context, int dp) {
        return (int) TypedValue.applyDimension(1, (float) dp, context.getResources().getDisplayMetrics());
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
