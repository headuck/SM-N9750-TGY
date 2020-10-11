package com.android.server.wifi.p2p.common;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

public class GUIUtil {
    static final String TAG = "GUIUtil";
    private static volatile GUIUtil mInstance = null;
    private Context mContext = null;

    public GUIUtil(Context context) {
        this.mContext = context;
    }

    public static GUIUtil getInstance(Context context) {
        if (context != null && mInstance == null) {
            synchronized (GUIUtil.class) {
                if (mInstance == null) {
                    mInstance = new GUIUtil(context);
                }
            }
        }
        return mInstance;
    }

    public static final Bitmap getIconBackground(int iconSize, int circleSize, int color) {
        Bitmap colorIcon = Bitmap.createBitmap(iconSize * 2, iconSize * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(colorIcon);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawCircle((float) iconSize, (float) iconSize, (float) circleSize, paint);
        return colorIcon;
    }

    /* JADX WARNING: Removed duplicated region for block: B:117:0x01dd  */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x01e2  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x01e7 A[SYNTHETIC, Splitter:B:121:0x01e7] */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x01f4  */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x01f9  */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x01fe A[SYNTHETIC, Splitter:B:130:0x01fe] */
    /* JADX WARNING: Removed duplicated region for block: B:139:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:142:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x009a A[SYNTHETIC, Splitter:B:25:0x009a] */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0154 A[SYNTHETIC, Splitter:B:75:0x0154] */
    /* JADX WARNING: Removed duplicated region for block: B:92:0x0198  */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x019d  */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01a2 A[SYNTHETIC, Splitter:B:96:0x01a2] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static android.graphics.Bitmap getContactImage(android.content.Context r24, java.lang.String r25) {
        /*
            r1 = r24
            java.lang.String r0 = "photo_uri"
            r2 = 0
            r3 = 0
            r4 = 0
            java.lang.String r5 = "_id"
            java.lang.String r6 = "display_name"
            java.lang.String r7 = "lookup"
            java.lang.String[] r10 = new java.lang.String[]{r5, r6, r7}
            r5 = 0
            r6 = 1
            r7 = 2
            android.content.ContentResolver r8 = r24.getContentResolver()     // Catch:{ Exception -> 0x01d3, all -> 0x01c9 }
            r21 = r8
            if (r21 == 0) goto L_0x01ae
            android.net.Uri r8 = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI     // Catch:{ Exception -> 0x01d3, all -> 0x01c9 }
            java.lang.String r9 = android.net.Uri.encode(r25)     // Catch:{ Exception -> 0x01d3, all -> 0x01c9 }
            android.net.Uri r16 = android.net.Uri.withAppendedPath(r8, r9)     // Catch:{ Exception -> 0x01d3, all -> 0x01c9 }
            java.lang.String[] r17 = new java.lang.String[]{r0}     // Catch:{ Exception -> 0x01d3, all -> 0x01c9 }
            r18 = 0
            r19 = 0
            r20 = 0
            r15 = r21
            android.database.Cursor r8 = r15.query(r16, r17, r18, r19, r20)     // Catch:{ Exception -> 0x01d3, all -> 0x01c9 }
            r2 = r8
            android.net.Uri r8 = android.provider.ContactsContract.Contacts.CONTENT_FILTER_URI     // Catch:{ Exception -> 0x01d3, all -> 0x01c9 }
            java.lang.String r9 = android.net.Uri.encode(r25)     // Catch:{ Exception -> 0x01d3, all -> 0x01c9 }
            android.net.Uri r9 = android.net.Uri.withAppendedPath(r8, r9)     // Catch:{ Exception -> 0x01d3, all -> 0x01c9 }
            r11 = 0
            r12 = 0
            r13 = 0
            r8 = r21
            android.database.Cursor r8 = r8.query(r9, r10, r11, r12, r13)     // Catch:{ Exception -> 0x01d3, all -> 0x01c9 }
            r3 = r8
            r8 = 0
            r11 = 0
            r12 = -1
            r15 = 0
            r14 = 0
            if (r3 == 0) goto L_0x0096
            boolean r18 = r3.moveToFirst()     // Catch:{ Exception -> 0x008f, all -> 0x0084 }
            if (r18 == 0) goto L_0x0096
            long r18 = r3.getLong(r14)     // Catch:{ Exception -> 0x008f, all -> 0x0084 }
            r12 = r18
            r14 = 1
            java.lang.String r19 = r3.getString(r14)     // Catch:{ Exception -> 0x008f, all -> 0x0084 }
            r8 = r19
            r14 = 2
            java.lang.String r14 = r3.getString(r14)     // Catch:{ Exception -> 0x008f, all -> 0x0084 }
            r11 = r14
            com.android.server.wifi.p2p.common.DefaultImageRequest r14 = new com.android.server.wifi.p2p.common.DefaultImageRequest     // Catch:{ Exception -> 0x008f, all -> 0x0084 }
            r20 = r5
            r5 = 1
            r14.<init>(r8, r11, r5)     // Catch:{ Exception -> 0x007f, all -> 0x0076 }
            r15 = r14
            goto L_0x0098
        L_0x0076:
            r0 = move-exception
            r5 = r4
            r22 = r6
            r4 = r3
            r3 = r2
            r2 = r0
            goto L_0x01f2
        L_0x007f:
            r0 = move-exception
            r22 = r6
            goto L_0x01d8
        L_0x0084:
            r0 = move-exception
            r20 = r5
            r5 = r4
            r22 = r6
            r4 = r3
            r3 = r2
            r2 = r0
            goto L_0x01f2
        L_0x008f:
            r0 = move-exception
            r20 = r5
            r22 = r6
            goto L_0x01d8
        L_0x0096:
            r20 = r5
        L_0x0098:
            if (r2 == 0) goto L_0x0150
            boolean r14 = r2.moveToNext()     // Catch:{ Exception -> 0x014b, all -> 0x0142 }
            if (r14 == 0) goto L_0x0150
            int r0 = r2.getColumnIndex(r0)     // Catch:{ Exception -> 0x014b, all -> 0x0142 }
            int r0 = r2.getInt(r0)     // Catch:{ Exception -> 0x014b, all -> 0x0142 }
            java.lang.String r0 = r2.getString(r0)     // Catch:{ Exception -> 0x014b, all -> 0x0142 }
            r14 = r0
            if (r14 == 0) goto L_0x00da
            android.net.Uri r0 = android.net.Uri.parse(r14)     // Catch:{ Exception -> 0x007f, all -> 0x0076 }
            r5 = r0
            android.content.ContentResolver r0 = r24.getContentResolver()     // Catch:{ Exception -> 0x007f, all -> 0x0076 }
            java.io.InputStream r0 = r0.openInputStream(r5)     // Catch:{ Exception -> 0x007f, all -> 0x0076 }
            r4 = r0
            android.graphics.Bitmap r17 = android.graphics.BitmapFactory.decodeStream(r4)     // Catch:{ Exception -> 0x007f, all -> 0x0076 }
            r2.close()
            if (r3 == 0) goto L_0x00cb
            r3.close()
        L_0x00cb:
            if (r4 == 0) goto L_0x00d9
            r4.close()     // Catch:{ IOException -> 0x00d1 }
            goto L_0x00d9
        L_0x00d1:
            r0 = move-exception
            r18 = r0
            r0 = r18
            r0.printStackTrace()
        L_0x00d9:
            return r17
        L_0x00da:
            if (r15 == 0) goto L_0x0128
            android.content.res.Resources r0 = r24.getResources()     // Catch:{ Exception -> 0x014b, all -> 0x0142 }
            android.graphics.drawable.Drawable r0 = getDefaultImageForContact(r0, r15, r12)     // Catch:{ Exception -> 0x014b, all -> 0x0142 }
            r19 = r0
            r5 = r19
            if (r5 == 0) goto L_0x0125
            android.content.res.Resources r0 = r24.getResources()     // Catch:{ Exception -> 0x014b, all -> 0x0142 }
            r22 = r6
            r6 = 17106259(0x1050553, float:2.4432062E-38)
            float r0 = r0.getDimension(r6)     // Catch:{ Exception -> 0x0193 }
            int r6 = (int) r0     // Catch:{ Exception -> 0x0193 }
            int r0 = com.android.server.wifi.p2p.common.Util.dpTopx(r1, r6)     // Catch:{ Exception -> 0x0193 }
            r19 = r0
            r23 = r6
            r6 = r19
            r0 = 0
            r5.setBounds(r0, r0, r6, r6)     // Catch:{ Exception -> 0x0193 }
            android.graphics.Bitmap r0 = com.android.server.wifi.p2p.common.Util.drawableToBitmap(r5)     // Catch:{ Exception -> 0x0193 }
            r17 = r0
            r2.close()
            if (r3 == 0) goto L_0x0116
            r3.close()
        L_0x0116:
            if (r4 == 0) goto L_0x0124
            r4.close()     // Catch:{ IOException -> 0x011c }
            goto L_0x0124
        L_0x011c:
            r0 = move-exception
            r18 = r0
            r0 = r18
            r0.printStackTrace()
        L_0x0124:
            return r17
        L_0x0125:
            r22 = r6
            goto L_0x012a
        L_0x0128:
            r22 = r6
        L_0x012a:
            r2.close()
            if (r3 == 0) goto L_0x0134
            r3.close()
        L_0x0134:
            if (r4 == 0) goto L_0x0140
            r4.close()     // Catch:{ IOException -> 0x013a }
            goto L_0x0140
        L_0x013a:
            r0 = move-exception
            r5 = r0
            r0 = r5
            r0.printStackTrace()
        L_0x0140:
            r5 = 0
            return r5
        L_0x0142:
            r0 = move-exception
            r22 = r6
            r5 = r4
            r4 = r3
            r3 = r2
            r2 = r0
            goto L_0x01f2
        L_0x014b:
            r0 = move-exception
            r22 = r6
            goto L_0x01d8
        L_0x0150:
            r22 = r6
            if (r15 == 0) goto L_0x0195
            android.content.res.Resources r5 = r24.getResources()     // Catch:{ Exception -> 0x0193 }
            android.graphics.drawable.Drawable r5 = getDefaultImageForContact(r5, r15, r12)     // Catch:{ Exception -> 0x0193 }
            if (r5 == 0) goto L_0x0195
            android.content.res.Resources r6 = r24.getResources()     // Catch:{ Exception -> 0x0193 }
            r0 = 17106259(0x1050553, float:2.4432062E-38)
            float r0 = r6.getDimension(r0)     // Catch:{ Exception -> 0x0193 }
            int r6 = (int) r0     // Catch:{ Exception -> 0x0193 }
            int r0 = com.android.server.wifi.p2p.common.Util.dpTopx(r1, r6)     // Catch:{ Exception -> 0x0193 }
            r14 = r0
            r0 = 0
            r5.setBounds(r0, r0, r14, r14)     // Catch:{ Exception -> 0x0193 }
            android.graphics.Bitmap r0 = com.android.server.wifi.p2p.common.Util.drawableToBitmap(r5)     // Catch:{ Exception -> 0x0193 }
            r17 = r0
            if (r2 == 0) goto L_0x017f
            r2.close()
        L_0x017f:
            if (r3 == 0) goto L_0x0184
            r3.close()
        L_0x0184:
            if (r4 == 0) goto L_0x0192
            r4.close()     // Catch:{ IOException -> 0x018a }
            goto L_0x0192
        L_0x018a:
            r0 = move-exception
            r18 = r0
            r0 = r18
            r0.printStackTrace()
        L_0x0192:
            return r17
        L_0x0193:
            r0 = move-exception
            goto L_0x01d8
        L_0x0195:
            if (r2 == 0) goto L_0x019b
            r2.close()
        L_0x019b:
            if (r3 == 0) goto L_0x01a0
            r3.close()
        L_0x01a0:
            if (r4 == 0) goto L_0x01ac
            r4.close()     // Catch:{ IOException -> 0x01a6 }
            goto L_0x01ac
        L_0x01a6:
            r0 = move-exception
            r5 = r0
            r0 = r5
            r0.printStackTrace()
        L_0x01ac:
            r5 = 0
            return r5
        L_0x01ae:
            r20 = r5
            r22 = r6
            if (r2 == 0) goto L_0x01b7
            r2.close()
        L_0x01b7:
            if (r3 == 0) goto L_0x01bc
            r3.close()
        L_0x01bc:
            if (r4 == 0) goto L_0x01eb
            r4.close()     // Catch:{ IOException -> 0x01c2 }
        L_0x01c1:
            goto L_0x01eb
        L_0x01c2:
            r0 = move-exception
            r5 = r0
            r0 = r5
            r0.printStackTrace()
            goto L_0x01c1
        L_0x01c9:
            r0 = move-exception
            r20 = r5
            r22 = r6
            r5 = r4
            r4 = r3
            r3 = r2
            r2 = r0
            goto L_0x01f2
        L_0x01d3:
            r0 = move-exception
            r20 = r5
            r22 = r6
        L_0x01d8:
            r0.printStackTrace()     // Catch:{ all -> 0x01ed }
            if (r2 == 0) goto L_0x01e0
            r2.close()
        L_0x01e0:
            if (r3 == 0) goto L_0x01e5
            r3.close()
        L_0x01e5:
            if (r4 == 0) goto L_0x01eb
            r4.close()     // Catch:{ IOException -> 0x01c2 }
            goto L_0x01c1
        L_0x01eb:
            r5 = 0
            return r5
        L_0x01ed:
            r0 = move-exception
            r5 = r4
            r4 = r3
            r3 = r2
            r2 = r0
        L_0x01f2:
            if (r3 == 0) goto L_0x01f7
            r3.close()
        L_0x01f7:
            if (r4 == 0) goto L_0x01fc
            r4.close()
        L_0x01fc:
            if (r5 == 0) goto L_0x0208
            r5.close()     // Catch:{ IOException -> 0x0202 }
            goto L_0x0208
        L_0x0202:
            r0 = move-exception
            r6 = r0
            r0 = r6
            r0.printStackTrace()
        L_0x0208:
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.common.GUIUtil.getContactImage(android.content.Context, java.lang.String):android.graphics.Bitmap");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0032, code lost:
        if (r1 != null) goto L_0x0034;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0034, code lost:
        r1.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x003e, code lost:
        if (r1 == null) goto L_0x0041;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0041, code lost:
        return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String getContactName(android.content.Context r10, java.lang.String r11) {
        /*
            java.lang.String r0 = "display_name"
            r1 = 0
            r2 = 0
            android.content.ContentResolver r3 = r10.getContentResolver()     // Catch:{ Exception -> 0x003a }
            if (r3 == 0) goto L_0x0032
            android.net.Uri r4 = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI     // Catch:{ Exception -> 0x003a }
            java.lang.String r5 = android.net.Uri.encode(r11)     // Catch:{ Exception -> 0x003a }
            android.net.Uri r5 = android.net.Uri.withAppendedPath(r4, r5)     // Catch:{ Exception -> 0x003a }
            java.lang.String[] r6 = new java.lang.String[]{r0}     // Catch:{ Exception -> 0x003a }
            r7 = 0
            r8 = 0
            r9 = 0
            r4 = r3
            android.database.Cursor r4 = r4.query(r5, r6, r7, r8, r9)     // Catch:{ Exception -> 0x003a }
            r1 = r4
            if (r1 == 0) goto L_0x0032
            boolean r4 = r1.moveToNext()     // Catch:{ Exception -> 0x003a }
            if (r4 == 0) goto L_0x0032
            int r0 = r1.getColumnIndex(r0)     // Catch:{ Exception -> 0x003a }
            java.lang.String r0 = r1.getString(r0)     // Catch:{ Exception -> 0x003a }
            r2 = r0
        L_0x0032:
            if (r1 == 0) goto L_0x0041
        L_0x0034:
            r1.close()
            goto L_0x0041
        L_0x0038:
            r0 = move-exception
            goto L_0x0042
        L_0x003a:
            r0 = move-exception
            r0.printStackTrace()     // Catch:{ all -> 0x0038 }
            if (r1 == 0) goto L_0x0041
            goto L_0x0034
        L_0x0041:
            return r2
        L_0x0042:
            if (r1 == 0) goto L_0x0047
            r1.close()
        L_0x0047:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.common.GUIUtil.getContactName(android.content.Context, java.lang.String):java.lang.String");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:16:0x007f, code lost:
        if (r2 != null) goto L_0x0070;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0082, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x006e, code lost:
        if (r2 != null) goto L_0x0070;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0070, code lost:
        r2.recycle();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static android.graphics.Bitmap cropIcon(android.content.Context r12, int r13, android.graphics.Bitmap r14) {
        /*
            r0 = 0
            r1 = 0
            r2 = 0
            android.content.res.Resources r3 = r12.getResources()
            float r3 = r3.getDimension(r13)
            int r3 = (int) r3
            android.graphics.Bitmap$Config r4 = android.graphics.Bitmap.Config.ARGB_8888     // Catch:{ Exception -> 0x0076 }
            android.graphics.Bitmap r4 = android.graphics.Bitmap.createBitmap(r3, r3, r4)     // Catch:{ Exception -> 0x0076 }
            r1 = r4
            android.graphics.Canvas r4 = new android.graphics.Canvas     // Catch:{ Exception -> 0x0076 }
            r4.<init>(r1)     // Catch:{ Exception -> 0x0076 }
            android.graphics.Paint r5 = new android.graphics.Paint     // Catch:{ Exception -> 0x0076 }
            r5.<init>()     // Catch:{ Exception -> 0x0076 }
            r6 = 1
            r5.setAntiAlias(r6)     // Catch:{ Exception -> 0x0076 }
            r7 = -328966(0xfffffffffffafafa, float:NaN)
            r5.setColor(r7)     // Catch:{ Exception -> 0x0076 }
            int r7 = r3 / 2
            float r7 = (float) r7     // Catch:{ Exception -> 0x0076 }
            int r8 = r3 / 2
            float r8 = (float) r8     // Catch:{ Exception -> 0x0076 }
            int r9 = r3 / 2
            float r9 = (float) r9     // Catch:{ Exception -> 0x0076 }
            r4.drawCircle(r7, r8, r9, r5)     // Catch:{ Exception -> 0x0076 }
            if (r1 == 0) goto L_0x0069
            if (r14 == 0) goto L_0x0069
            android.graphics.Bitmap$Config r7 = android.graphics.Bitmap.Config.ARGB_8888     // Catch:{ Exception -> 0x0076 }
            android.graphics.Bitmap r7 = r14.copy(r7, r6)     // Catch:{ Exception -> 0x0076 }
            r2 = r7
            android.graphics.Bitmap r6 = android.graphics.Bitmap.createScaledBitmap(r2, r3, r3, r6)     // Catch:{ Exception -> 0x0076 }
            r2 = r6
            android.graphics.Bitmap$Config r6 = android.graphics.Bitmap.Config.ARGB_8888     // Catch:{ Exception -> 0x0076 }
            android.graphics.Bitmap r6 = android.graphics.Bitmap.createBitmap(r3, r3, r6)     // Catch:{ Exception -> 0x0076 }
            r0 = r6
            android.graphics.Canvas r6 = new android.graphics.Canvas     // Catch:{ Exception -> 0x0076 }
            r6.<init>(r0)     // Catch:{ Exception -> 0x0076 }
            r7 = 0
            r8 = 0
            r6.drawBitmap(r2, r8, r8, r7)     // Catch:{ Exception -> 0x0076 }
            android.graphics.Paint r9 = new android.graphics.Paint     // Catch:{ Exception -> 0x0076 }
            r9.<init>()     // Catch:{ Exception -> 0x0076 }
            android.graphics.PorterDuffXfermode r10 = new android.graphics.PorterDuffXfermode     // Catch:{ Exception -> 0x0076 }
            android.graphics.PorterDuff$Mode r11 = android.graphics.PorterDuff.Mode.DST_IN     // Catch:{ Exception -> 0x0076 }
            r10.<init>(r11)     // Catch:{ Exception -> 0x0076 }
            r9.setXfermode(r10)     // Catch:{ Exception -> 0x0076 }
            r6.drawBitmap(r1, r8, r8, r9)     // Catch:{ Exception -> 0x0076 }
            r9.setXfermode(r7)     // Catch:{ Exception -> 0x0076 }
        L_0x0069:
            if (r1 == 0) goto L_0x006e
            r1.recycle()
        L_0x006e:
            if (r2 == 0) goto L_0x0082
        L_0x0070:
            r2.recycle()
            goto L_0x0082
        L_0x0074:
            r4 = move-exception
            goto L_0x0083
        L_0x0076:
            r4 = move-exception
            r4.printStackTrace()     // Catch:{ all -> 0x0074 }
            if (r1 == 0) goto L_0x007f
            r1.recycle()
        L_0x007f:
            if (r2 == 0) goto L_0x0082
            goto L_0x0070
        L_0x0082:
            return r0
        L_0x0083:
            if (r1 == 0) goto L_0x0088
            r1.recycle()
        L_0x0088:
            if (r2 == 0) goto L_0x008d
            r2.recycle()
        L_0x008d:
            throw r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.common.GUIUtil.cropIcon(android.content.Context, int, android.graphics.Bitmap):android.graphics.Bitmap");
    }

    public static boolean isHorizentalDisplay(Context context) {
        if (context.getResources().getConfiguration().orientation == 2) {
            return true;
        }
        return false;
    }

    public static int getValueFromAttr(Context context, int[] attrs) {
        int valueFromAttr = 0;
        try {
            TypedArray a = context.obtainStyledAttributes(new TypedValue().data, attrs);
            valueFromAttr = a.getDimensionPixelSize(0, -1);
            a.recycle();
            return valueFromAttr;
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            return valueFromAttr;
        }
    }

    public static Drawable getDefaultImageForContact(Resources resources, DefaultImageRequest defaultImageRequest, long contactId) {
        return null;
    }

    public static Drawable getDefaultAvatar(Resources res, boolean hires, boolean darkTheme, boolean isCircular, long contactId) {
        int resId;
        int i = (int) (contactId % 5);
        if (!hires || !darkTheme) {
            resId = 17302193;
        } else {
            resId = 17302192;
        }
        Bitmap image = null;
        if (0 == 0) {
            image = BitmapFactory.decodeResource(res, resId);
        }
        int color = res.getColor(Util.getDefaultPhotoBackgroundColor(contactId));
        StrokeRoundedBitmapDrawable drawable = new StrokeRoundedBitmapDrawable(res, image);
        drawable.setAntiAlias(true);
        drawable.setColorFilter(color, PorterDuff.Mode.DST_OVER);
        if (isCircular) {
            drawable.setCornerRadius(Util.getCornerRadius(res, image.getHeight()));
        }
        return drawable;
    }
}
