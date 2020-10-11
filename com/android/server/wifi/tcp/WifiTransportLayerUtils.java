package com.android.server.wifi.tcp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Debug;
import android.util.Log;
import com.samsung.android.game.SemGameManager;
import java.util.ArrayList;

public class WifiTransportLayerUtils {
    public static final String CATEGORY_PLAYSTORE_ART_AND_DESIGN = "ART_AND_DESIGN";
    public static final String CATEGORY_PLAYSTORE_AUTO_AND_VEHICLES = "AUTO_AND_VEHICLES";
    public static final String CATEGORY_PLAYSTORE_BEAUTY = "BEAUTY";
    public static final String CATEGORY_PLAYSTORE_BOOKS_AND_REFERENCE = "BOOKS_AND_REFERENCE";
    public static final String CATEGORY_PLAYSTORE_BUSINESS = "BUSINESS";
    public static final String CATEGORY_PLAYSTORE_COMICS = "COMICS";
    public static final String CATEGORY_PLAYSTORE_COMMUNICATION = "COMMUNICATION";
    public static final String CATEGORY_PLAYSTORE_DATING = "DATING";
    public static final String CATEGORY_PLAYSTORE_EDUCATION = "EDUCATION";
    public static final String CATEGORY_PLAYSTORE_ENTERTAINMENT = "ENTERTAINMENT";
    public static final String CATEGORY_PLAYSTORE_EVENTS = "EVENTS";
    public static final String CATEGORY_PLAYSTORE_FAILED = "FAILED";
    public static final String CATEGORY_PLAYSTORE_FINANCE = "FINANCE";
    public static final String CATEGORY_PLAYSTORE_FOOD_AND_DRINK = "FOOD_AND_DRINK";
    public static final String CATEGORY_PLAYSTORE_GAME = "GAME";
    public static final String CATEGORY_PLAYSTORE_HEALTH_AND_FITNESS = "HEALTH_AND_FITNESS";
    public static final String CATEGORY_PLAYSTORE_HOUSE_AND_HOME = "HOUSE_AND_HOME";
    public static final String CATEGORY_PLAYSTORE_LIBRARIES_AND_DEMO = "LIBRARIES_AND_DEMO";
    public static final String CATEGORY_PLAYSTORE_LIFESTYLE = "LIFESTYLE";
    public static final String CATEGORY_PLAYSTORE_MAPS_AND_NAVIGATION = "MAPS_AND_NAVIGATION";
    public static final String CATEGORY_PLAYSTORE_MEDICAL = "MEDICAL";
    public static final String CATEGORY_PLAYSTORE_MUSIC_AND_AUDIO = "MUSIC_AND_AUDIO";
    public static final String CATEGORY_PLAYSTORE_NEWS_AND_MAGAZINES = "NEWS_AND_MAGAZINES";
    public static final String CATEGORY_PLAYSTORE_NONE = "NONE";
    public static final String CATEGORY_PLAYSTORE_PARENTING = "PARENTING";
    public static final String CATEGORY_PLAYSTORE_PERSONALIZATION = "PERSONALIZATION";
    public static final String CATEGORY_PLAYSTORE_PHOTOGRAPHY = "PHOTOGRAPHY";
    public static final String CATEGORY_PLAYSTORE_PRODUCTIVITY = "PRODUCTIVITY";
    public static final String CATEGORY_PLAYSTORE_SHOPPING = "SHOPPING";
    public static final String CATEGORY_PLAYSTORE_SOCIAL = "SOCIAL";
    public static final String CATEGORY_PLAYSTORE_SPORTS = "SPORTS";
    public static final String CATEGORY_PLAYSTORE_SYSTEM = "SYSTEM";
    public static final String CATEGORY_PLAYSTORE_TOOLS = "TOOLS";
    public static final String CATEGORY_PLAYSTORE_TRAVEL_AND_LOCAL = "TRAVEL_AND_LOCAL";
    public static final String CATEGORY_PLAYSTORE_VIDEO_PLAYERS = "VIDEO_PLAYERS";
    public static final String CATEGORY_PLAYSTORE_WEATHER = "WEATHER";
    private static final String CATEGORY_TAG = "<a itemprop=\"genre\"";
    private static final int CATEGORY_TAG_LENGTH = 25;
    private static final String[] CHAT_APPS = {"com.whatsapp", "com.kakao.talk", "com.skype.raider", "com.facebook.orca", "com.viber.voip", "jp.naver.line.android", "com.snapchat.android", "com.tencent.mm", "com.imo.android.imoim"};
    private static final boolean DBG = Debug.semIsProductDev();
    private static final String END_TAG = "class=";
    private static final String GOOGLE_URL = "https://play.google.com/store/apps/details?id=";
    private static final int PLAYSTORE_CATEGORY_TIMEOUT = 4000;
    private static final String TAG = "WifiTransportLayerUtils";

    /* JADX WARNING: Removed duplicated region for block: B:176:0x0361  */
    /* JADX WARNING: Removed duplicated region for block: B:178:0x0366 A[SYNTHETIC, Splitter:B:178:0x0366] */
    /* JADX WARNING: Removed duplicated region for block: B:188:0x03a1  */
    /* JADX WARNING: Removed duplicated region for block: B:190:0x03a6 A[SYNTHETIC, Splitter:B:190:0x03a6] */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x03bb  */
    /* JADX WARNING: Removed duplicated region for block: B:198:0x03c0 A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x03c9  */
    /* JADX WARNING: Removed duplicated region for block: B:204:0x03ce A[SYNTHETIC, Splitter:B:204:0x03ce] */
    /* JADX WARNING: Unknown top exception splitter block from list: {B:173:0x034a=Splitter:B:173:0x034a, B:185:0x038a=Splitter:B:185:0x038a} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String getApplicationCategory(android.content.Context r26, java.lang.String r27) {
        /*
            r1 = r27
            java.lang.String r0 = "EVENTS"
            java.lang.String r2 = "ENTERTAINMENT"
            java.lang.String r3 = "EDUCATION"
            java.lang.String r4 = "DATING"
            java.lang.String r5 = "COMMUNICATION"
            java.lang.String r6 = "COMICS"
            java.lang.String r7 = "BUSINESS"
            java.lang.String r8 = "BOOKS_AND_REFERENCE"
            java.lang.String r9 = "BEAUTY"
            java.lang.String r10 = "AUTO_AND_VEHICLES"
            java.lang.String r11 = "ART_AND_DESIGN"
            java.lang.String r12 = "GAME"
            java.lang.String r13 = "getApplicationCategory - "
            java.lang.String r14 = "WifiTransportLayerUtils"
            if (r26 == 0) goto L_0x03ed
            if (r1 != 0) goto L_0x0026
            r3 = r1
            r4 = r14
            goto L_0x03ef
        L_0x0026:
            java.lang.String r15 = "NONE"
            r16 = 0
            r17 = 0
            r18 = r15
            java.net.URL r15 = new java.net.URL     // Catch:{ IOException -> 0x0384, Exception -> 0x0344, all -> 0x033b }
            r19 = r14
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x0333, Exception -> 0x032b, all -> 0x0321 }
            r14.<init>()     // Catch:{ IOException -> 0x0333, Exception -> 0x032b, all -> 0x0321 }
            r20 = r13
            java.lang.String r13 = "https://play.google.com/store/apps/details?id="
            r14.append(r13)     // Catch:{ IOException -> 0x0317, Exception -> 0x030e, all -> 0x0303 }
            r14.append(r1)     // Catch:{ IOException -> 0x0317, Exception -> 0x030e, all -> 0x0303 }
            java.lang.String r13 = "&hl=en"
            r14.append(r13)     // Catch:{ IOException -> 0x0317, Exception -> 0x030e, all -> 0x0303 }
            java.lang.String r13 = r14.toString()     // Catch:{ IOException -> 0x0317, Exception -> 0x030e, all -> 0x0303 }
            r15.<init>(r13)     // Catch:{ IOException -> 0x0317, Exception -> 0x030e, all -> 0x0303 }
            r13 = r15
            java.net.URLConnection r14 = r13.openConnection()     // Catch:{ IOException -> 0x0317, Exception -> 0x030e, all -> 0x0303 }
            java.net.HttpURLConnection r14 = (java.net.HttpURLConnection) r14     // Catch:{ IOException -> 0x0317, Exception -> 0x030e, all -> 0x0303 }
            r15 = 4000(0xfa0, float:5.605E-42)
            r14.setConnectTimeout(r15)     // Catch:{ IOException -> 0x02f7, Exception -> 0x02eb, all -> 0x02e2 }
            r14.setReadTimeout(r15)     // Catch:{ IOException -> 0x02f7, Exception -> 0x02eb, all -> 0x02e2 }
            java.io.InputStream r15 = r14.getInputStream()     // Catch:{ IOException -> 0x02f7, Exception -> 0x02eb, all -> 0x02e2 }
            int r17 = r14.getResponseCode()     // Catch:{ IOException -> 0x02f7, Exception -> 0x02eb, all -> 0x02e2 }
            r21 = r17
            r22 = r13
            r13 = 200(0xc8, float:2.8E-43)
            r1 = r21
            if (r1 != r13) goto L_0x02aa
            java.io.BufferedReader r13 = new java.io.BufferedReader     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
            r17 = r1
            java.io.InputStreamReader r1 = new java.io.InputStreamReader     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
            r1.<init>(r15)     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
            r13.<init>(r1)     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
            r16 = r13
            r1 = 0
            java.lang.StringBuffer r13 = new java.lang.StringBuffer     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
            r13.<init>()     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
        L_0x0082:
            java.lang.String r21 = r16.readLine()     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
            r1 = r21
            if (r21 == 0) goto L_0x008e
            r13.append(r1)     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
            goto L_0x0082
        L_0x008e:
            java.lang.String r21 = r13.toString()     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
            r23 = r21
            r21 = r1
            java.lang.String r1 = "<a itemprop=\"genre\""
            r24 = r13
            r13 = r23
            int r1 = r13.indexOf(r1)     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
            r23 = r15
            r15 = -1
            if (r1 == r15) goto L_0x02ae
            int r1 = r1 + 25
            java.lang.String r15 = "class="
            int r15 = r13.indexOf(r15, r1)     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
            java.lang.String r25 = r13.substring(r1, r15)     // Catch:{ IOException -> 0x029d, Exception -> 0x0290, all -> 0x0286 }
            r18 = r25
            r25 = r1
            r1 = r18
            boolean r18 = r1.contains(r12)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r18 == 0) goto L_0x00c2
            r0 = r12
            r18 = r0
            goto L_0x02ae
        L_0x00c2:
            boolean r12 = r1.contains(r11)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r12 == 0) goto L_0x00cd
            r0 = r11
            r18 = r0
            goto L_0x02ae
        L_0x00cd:
            boolean r11 = r1.contains(r10)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r11 == 0) goto L_0x00d8
            r0 = r10
            r18 = r0
            goto L_0x02ae
        L_0x00d8:
            boolean r10 = r1.contains(r9)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r10 == 0) goto L_0x00e3
            r0 = r9
            r18 = r0
            goto L_0x02ae
        L_0x00e3:
            boolean r9 = r1.contains(r8)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r9 == 0) goto L_0x00ee
            r0 = r8
            r18 = r0
            goto L_0x02ae
        L_0x00ee:
            boolean r8 = r1.contains(r7)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r8 == 0) goto L_0x00f9
            r0 = r7
            r18 = r0
            goto L_0x02ae
        L_0x00f9:
            boolean r7 = r1.contains(r6)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r7 == 0) goto L_0x0104
            r0 = r6
            r18 = r0
            goto L_0x02ae
        L_0x0104:
            boolean r6 = r1.contains(r5)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r6 == 0) goto L_0x010f
            r0 = r5
            r18 = r0
            goto L_0x02ae
        L_0x010f:
            boolean r5 = r1.contains(r4)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r5 == 0) goto L_0x011a
            r0 = r4
            r18 = r0
            goto L_0x02ae
        L_0x011a:
            boolean r4 = r1.contains(r3)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r4 == 0) goto L_0x0125
            r0 = r3
            r18 = r0
            goto L_0x02ae
        L_0x0125:
            boolean r3 = r1.contains(r2)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r3 == 0) goto L_0x0130
            r0 = r2
            r18 = r0
            goto L_0x02ae
        L_0x0130:
            boolean r2 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r2 == 0) goto L_0x013a
            r18 = r0
            goto L_0x02ae
        L_0x013a:
            java.lang.String r0 = "FINANCE"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x0148
            java.lang.String r0 = "FINANCE"
            r18 = r0
            goto L_0x02ae
        L_0x0148:
            java.lang.String r0 = "FOOD_AND_DRINK"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x0156
            java.lang.String r0 = "FOOD_AND_DRINK"
            r18 = r0
            goto L_0x02ae
        L_0x0156:
            java.lang.String r0 = "HEALTH_AND_FITNESS"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x0164
            java.lang.String r0 = "HEALTH_AND_FITNESS"
            r18 = r0
            goto L_0x02ae
        L_0x0164:
            java.lang.String r0 = "HOUSE_AND_HOME"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x0172
            java.lang.String r0 = "HOUSE_AND_HOME"
            r18 = r0
            goto L_0x02ae
        L_0x0172:
            java.lang.String r0 = "LIBRARIES_AND_DEMO"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x0180
            java.lang.String r0 = "LIBRARIES_AND_DEMO"
            r18 = r0
            goto L_0x02ae
        L_0x0180:
            java.lang.String r0 = "LIFESTYLE"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x018e
            java.lang.String r0 = "LIFESTYLE"
            r18 = r0
            goto L_0x02ae
        L_0x018e:
            java.lang.String r0 = "MAPS_AND_NAVIGATION"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x019c
            java.lang.String r0 = "MAPS_AND_NAVIGATION"
            r18 = r0
            goto L_0x02ae
        L_0x019c:
            java.lang.String r0 = "MEDICAL"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x01aa
            java.lang.String r0 = "MEDICAL"
            r18 = r0
            goto L_0x02ae
        L_0x01aa:
            java.lang.String r0 = "MUSIC_AND_AUDIO"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x01b8
            java.lang.String r0 = "MUSIC_AND_AUDIO"
            r18 = r0
            goto L_0x02ae
        L_0x01b8:
            java.lang.String r0 = "NEWS_AND_MAGAZINES"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x01c6
            java.lang.String r0 = "NEWS_AND_MAGAZINES"
            r18 = r0
            goto L_0x02ae
        L_0x01c6:
            java.lang.String r0 = "PARENTING"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x01d4
            java.lang.String r0 = "PARENTING"
            r18 = r0
            goto L_0x02ae
        L_0x01d4:
            java.lang.String r0 = "PERSONALIZATION"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x01e2
            java.lang.String r0 = "PERSONALIZATION"
            r18 = r0
            goto L_0x02ae
        L_0x01e2:
            java.lang.String r0 = "PHOTOGRAPHY"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x01f0
            java.lang.String r0 = "PHOTOGRAPHY"
            r18 = r0
            goto L_0x02ae
        L_0x01f0:
            java.lang.String r0 = "PRODUCTIVITY"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x01fe
            java.lang.String r0 = "PRODUCTIVITY"
            r18 = r0
            goto L_0x02ae
        L_0x01fe:
            java.lang.String r0 = "SHOPPING"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x020c
            java.lang.String r0 = "SHOPPING"
            r18 = r0
            goto L_0x02ae
        L_0x020c:
            java.lang.String r0 = "SOCIAL"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x021a
            java.lang.String r0 = "SOCIAL"
            r18 = r0
            goto L_0x02ae
        L_0x021a:
            java.lang.String r0 = "SPORTS"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x0228
            java.lang.String r0 = "SPORTS"
            r18 = r0
            goto L_0x02ae
        L_0x0228:
            java.lang.String r0 = "TOOLS"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x0236
            java.lang.String r0 = "TOOLS"
            r18 = r0
            goto L_0x02ae
        L_0x0236:
            java.lang.String r0 = "TRAVEL_AND_LOCAL"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x0244
            java.lang.String r0 = "TRAVEL_AND_LOCAL"
            r18 = r0
            goto L_0x02ae
        L_0x0244:
            java.lang.String r0 = "VIDEO_PLAYERS"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x0252
            java.lang.String r0 = "VIDEO_PLAYERS"
            r18 = r0
            goto L_0x02ae
        L_0x0252:
            java.lang.String r0 = "WEATHER"
            boolean r0 = r1.contains(r0)     // Catch:{ IOException -> 0x027a, Exception -> 0x026e, all -> 0x0262 }
            if (r0 == 0) goto L_0x025f
            java.lang.String r0 = "WEATHER"
            r18 = r0
            goto L_0x02ae
        L_0x025f:
            r18 = r1
            goto L_0x02ae
        L_0x0262:
            r0 = move-exception
            r3 = r27
            r18 = r1
            r4 = r19
            r2 = r20
            r1 = r0
            goto L_0x03c7
        L_0x026e:
            r0 = move-exception
            r3 = r27
            r15 = r1
            r17 = r14
            r4 = r19
            r2 = r20
            goto L_0x034a
        L_0x027a:
            r0 = move-exception
            r3 = r27
            r15 = r1
            r17 = r14
            r4 = r19
            r2 = r20
            goto L_0x038a
        L_0x0286:
            r0 = move-exception
            r3 = r27
            r1 = r0
            r4 = r19
            r2 = r20
            goto L_0x03c7
        L_0x0290:
            r0 = move-exception
            r3 = r27
            r17 = r14
            r15 = r18
            r4 = r19
            r2 = r20
            goto L_0x034a
        L_0x029d:
            r0 = move-exception
            r3 = r27
            r17 = r14
            r15 = r18
            r4 = r19
            r2 = r20
            goto L_0x038a
        L_0x02aa:
            r17 = r1
            r23 = r15
        L_0x02ae:
            r14.disconnect()
            if (r16 == 0) goto L_0x02d9
            r16.close()     // Catch:{ IOException -> 0x02ba }
            r3 = r27
            goto L_0x02db
        L_0x02ba:
            r0 = move-exception
            r1 = r0
            r0 = r1
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            r2 = r20
            r1.append(r2)
            r3 = r27
            r1.append(r3)
            java.lang.String r1 = r1.toString()
            r4 = r19
            android.util.Log.w(r4, r1)
            r0.printStackTrace()
            goto L_0x02dc
        L_0x02d9:
            r3 = r27
        L_0x02db:
        L_0x02dc:
            r17 = r14
            r15 = r18
            goto L_0x03b3
        L_0x02e2:
            r0 = move-exception
            r3 = r1
            r4 = r19
            r2 = r20
            r1 = r0
            goto L_0x03c7
        L_0x02eb:
            r0 = move-exception
            r3 = r1
            r4 = r19
            r2 = r20
            r17 = r14
            r15 = r18
            goto L_0x034a
        L_0x02f7:
            r0 = move-exception
            r3 = r1
            r4 = r19
            r2 = r20
            r17 = r14
            r15 = r18
            goto L_0x038a
        L_0x0303:
            r0 = move-exception
            r3 = r1
            r4 = r19
            r2 = r20
            r1 = r0
            r14 = r17
            goto L_0x03c7
        L_0x030e:
            r0 = move-exception
            r3 = r1
            r4 = r19
            r2 = r20
            r15 = r18
            goto L_0x034a
        L_0x0317:
            r0 = move-exception
            r3 = r1
            r4 = r19
            r2 = r20
            r15 = r18
            goto L_0x038a
        L_0x0321:
            r0 = move-exception
            r3 = r1
            r2 = r13
            r4 = r19
            r1 = r0
            r14 = r17
            goto L_0x03c7
        L_0x032b:
            r0 = move-exception
            r3 = r1
            r2 = r13
            r4 = r19
            r15 = r18
            goto L_0x034a
        L_0x0333:
            r0 = move-exception
            r3 = r1
            r2 = r13
            r4 = r19
            r15 = r18
            goto L_0x038a
        L_0x033b:
            r0 = move-exception
            r3 = r1
            r2 = r13
            r4 = r14
            r1 = r0
            r14 = r17
            goto L_0x03c7
        L_0x0344:
            r0 = move-exception
            r3 = r1
            r2 = r13
            r4 = r14
            r15 = r18
        L_0x034a:
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x03c1 }
            r1.<init>()     // Catch:{ all -> 0x03c1 }
            java.lang.String r5 = "getApplicationCategory - Exception "
            r1.append(r5)     // Catch:{ all -> 0x03c1 }
            r1.append(r3)     // Catch:{ all -> 0x03c1 }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x03c1 }
            android.util.Log.w(r4, r1)     // Catch:{ all -> 0x03c1 }
            if (r17 == 0) goto L_0x0364
            r17.disconnect()
        L_0x0364:
            if (r16 == 0) goto L_0x0383
            r16.close()     // Catch:{ IOException -> 0x036a }
            goto L_0x0383
        L_0x036a:
            r0 = move-exception
            r1 = r0
            r0 = r1
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
        L_0x0372:
            r1.append(r2)
            r1.append(r3)
            java.lang.String r1 = r1.toString()
            android.util.Log.w(r4, r1)
            r0.printStackTrace()
            goto L_0x03b3
        L_0x0383:
            goto L_0x03b3
        L_0x0384:
            r0 = move-exception
            r3 = r1
            r2 = r13
            r4 = r14
            r15 = r18
        L_0x038a:
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x03c1 }
            r1.<init>()     // Catch:{ all -> 0x03c1 }
            java.lang.String r5 = "getApplicationCategory - IOException "
            r1.append(r5)     // Catch:{ all -> 0x03c1 }
            r1.append(r3)     // Catch:{ all -> 0x03c1 }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x03c1 }
            android.util.Log.w(r4, r1)     // Catch:{ all -> 0x03c1 }
            if (r17 == 0) goto L_0x03a4
            r17.disconnect()
        L_0x03a4:
            if (r16 == 0) goto L_0x0383
            r16.close()     // Catch:{ IOException -> 0x03aa }
            goto L_0x0383
        L_0x03aa:
            r0 = move-exception
            r1 = r0
            r0 = r1
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            goto L_0x0372
        L_0x03b3:
            java.lang.String r0 = "NONE"
            boolean r0 = r15.equals(r0)
            if (r0 == 0) goto L_0x03c0
            java.lang.String r0 = getFrameworkApplicationCategory(r26, r27)
            return r0
        L_0x03c0:
            return r15
        L_0x03c1:
            r0 = move-exception
            r1 = r0
            r18 = r15
            r14 = r17
        L_0x03c7:
            if (r14 == 0) goto L_0x03cc
            r14.disconnect()
        L_0x03cc:
            if (r16 == 0) goto L_0x03eb
            r16.close()     // Catch:{ IOException -> 0x03d2 }
            goto L_0x03eb
        L_0x03d2:
            r0 = move-exception
            r5 = r0
            r0 = r5
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r2)
            r5.append(r3)
            java.lang.String r2 = r5.toString()
            android.util.Log.w(r4, r2)
            r0.printStackTrace()
            goto L_0x03ec
        L_0x03eb:
        L_0x03ec:
            throw r1
        L_0x03ed:
            r3 = r1
            r4 = r14
        L_0x03ef:
            java.lang.String r0 = "getApplicationCategory - null params"
            android.util.Log.w(r4, r0)
            r0 = 0
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiTransportLayerUtils.getApplicationCategory(android.content.Context, java.lang.String):java.lang.String");
    }

    private static String getFrameworkApplicationCategory(Context context, String packageName) {
        int category = -1;
        try {
            category = context.getPackageManager().getApplicationInfo(packageName, 128).category;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "getApplicationCategory - NameNotFoundException " + e);
            e.printStackTrace();
        } catch (Exception e2) {
            Log.w(TAG, "getApplicationCategory - Exception " + e2);
            e2.printStackTrace();
        }
        switch (category) {
            case 0:
                return CATEGORY_PLAYSTORE_GAME;
            case 1:
                return CATEGORY_PLAYSTORE_MUSIC_AND_AUDIO;
            case 2:
                return CATEGORY_PLAYSTORE_VIDEO_PLAYERS;
            case 3:
                return CATEGORY_PLAYSTORE_PHOTOGRAPHY;
            case 4:
                return CATEGORY_PLAYSTORE_SOCIAL;
            case 5:
                return CATEGORY_PLAYSTORE_NEWS_AND_MAGAZINES;
            case 6:
                return CATEGORY_PLAYSTORE_MAPS_AND_NAVIGATION;
            case 7:
                return CATEGORY_PLAYSTORE_PRODUCTIVITY;
            default:
                return CATEGORY_PLAYSTORE_NONE;
        }
    }

    public static boolean isSemGamePackage(String packageName) {
        if (!SemGameManager.isAvailable() || !SemGameManager.isGamePackage(packageName)) {
            return false;
        }
        return true;
    }

    public static boolean isAudioCommunicationMode(Context context) {
        try {
            AudioManager am = (AudioManager) context.getSystemService("audio");
            if (am.getMode() == 3) {
                if (!DBG) {
                    return true;
                }
                Log.d(TAG, "isAudioCommunicationMode - true");
                return true;
            } else if (!DBG) {
                return false;
            } else {
                Log.d(TAG, "isAudioCommunicationMode - false - " + am.getMode());
                return false;
            }
        } catch (Exception e) {
            Log.w(TAG, "isAudioCommunicationMode - " + e);
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isLauchablePackage(Context context, String packageName) {
        if (context.getPackageManager().getLaunchIntentForPackage(packageName) != null) {
            return true;
        }
        return false;
    }

    public static ArrayList<String> getBrowserPackageNameList(Context context) {
        ArrayList<String> result = new ArrayList<>();
        try {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setData(Uri.parse("http://www.google.com"));
            for (ResolveInfo info : context.getPackageManager().queryIntentActivities(intent, ISupplicantStaNetwork.KeyMgmtMask.SUITE_B_192)) {
                result.add(info.activityInfo.packageName);
            }
        } catch (Exception e) {
            Log.w(TAG, "getBrowserPackageNameList - Exception " + e);
            e.printStackTrace();
        }
        return result;
    }

    public static boolean isBrowserApp(Context context, String packageName) {
        try {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setData(Uri.parse("http://www.google.com"));
            for (ResolveInfo info : context.getPackageManager().queryIntentActivities(intent, ISupplicantStaNetwork.KeyMgmtMask.SUITE_B_192)) {
                if (info.activityInfo.packageName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.w(TAG, "isBrowserApp - Exception " + e);
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isChatApp(String packageName) {
        for (String chatApp : CHAT_APPS) {
            if (chatApp.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSystemApp(Context context, String packageName) {
        try {
            if (context.getPackageManager().getApplicationInfo(packageName, 128).isSystemApp()) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "isSystemApp - NameNotFoundException " + e);
            e.printStackTrace();
            return false;
        }
    }

    public static boolean hasPermission(Context context, String packageName, String permission) {
        return context.getPackageManager().checkPermission(permission, packageName) == 0;
    }
}
