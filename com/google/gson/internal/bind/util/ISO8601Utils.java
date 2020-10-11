package com.google.gson.internal.bind.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class ISO8601Utils {
    private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone(UTC_ID);
    private static final String UTC_ID = "UTC";

    public static String format(Date date) {
        return format(date, false, TIMEZONE_UTC);
    }

    public static String format(Date date, boolean millis) {
        return format(date, millis, TIMEZONE_UTC);
    }

    public static String format(Date date, boolean millis, TimeZone tz) {
        Calendar calendar = new GregorianCalendar(tz, Locale.US);
        calendar.setTime(date);
        StringBuilder formatted = new StringBuilder("yyyy-MM-ddThh:mm:ss".length() + (millis ? ".sss".length() : 0) + (tz.getRawOffset() == 0 ? "Z" : "+hh:mm").length());
        padInt(formatted, calendar.get(1), "yyyy".length());
        char c = '-';
        formatted.append('-');
        padInt(formatted, calendar.get(2) + 1, "MM".length());
        formatted.append('-');
        padInt(formatted, calendar.get(5), "dd".length());
        formatted.append('T');
        padInt(formatted, calendar.get(11), "hh".length());
        formatted.append(':');
        padInt(formatted, calendar.get(12), "mm".length());
        formatted.append(':');
        padInt(formatted, calendar.get(13), "ss".length());
        if (millis) {
            formatted.append('.');
            padInt(formatted, calendar.get(14), "sss".length());
        }
        int offset = tz.getOffset(calendar.getTimeInMillis());
        if (offset != 0) {
            int hours = Math.abs((offset / 60000) / 60);
            int minutes = Math.abs((offset / 60000) % 60);
            if (offset >= 0) {
                c = '+';
            }
            formatted.append(c);
            padInt(formatted, hours, "hh".length());
            formatted.append(':');
            padInt(formatted, minutes, "mm".length());
        } else {
            formatted.append('Z');
        }
        return formatted.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:113:0x0232  */
    /* JADX WARNING: Removed duplicated region for block: B:114:0x0234  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.util.Date parse(java.lang.String r23, java.text.ParsePosition r24) throws java.text.ParseException {
        /*
            r1 = r23
            r2 = r24
            java.lang.String r3 = "'"
            r4 = 0
            int r0 = r24.getIndex()     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            int r5 = r0 + 4
            int r0 = parseInt(r1, r0, r5)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            r6 = 45
            boolean r7 = checkOffset(r1, r5, r6)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            if (r7 == 0) goto L_0x001b
            int r5 = r5 + 1
        L_0x001b:
            int r7 = r5 + 2
            int r5 = parseInt(r1, r5, r7)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            boolean r8 = checkOffset(r1, r7, r6)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            if (r8 == 0) goto L_0x0029
            int r7 = r7 + 1
        L_0x0029:
            int r8 = r7 + 2
            int r7 = parseInt(r1, r7, r8)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            r9 = 0
            r10 = 0
            r11 = 0
            r12 = 0
            r13 = 84
            boolean r13 = checkOffset(r1, r8, r13)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            if (r13 != 0) goto L_0x005f
            int r14 = r23.length()     // Catch:{ IndexOutOfBoundsException -> 0x005a, NumberFormatException -> 0x0055, IllegalArgumentException -> 0x0050 }
            if (r14 > r8) goto L_0x005f
            java.util.GregorianCalendar r6 = new java.util.GregorianCalendar     // Catch:{ IndexOutOfBoundsException -> 0x005a, NumberFormatException -> 0x0055, IllegalArgumentException -> 0x0050 }
            int r14 = r5 + -1
            r6.<init>(r0, r14, r7)     // Catch:{ IndexOutOfBoundsException -> 0x005a, NumberFormatException -> 0x0055, IllegalArgumentException -> 0x0050 }
            r2.setIndex(r8)     // Catch:{ IndexOutOfBoundsException -> 0x005a, NumberFormatException -> 0x0055, IllegalArgumentException -> 0x0050 }
            java.util.Date r3 = r6.getTime()     // Catch:{ IndexOutOfBoundsException -> 0x005a, NumberFormatException -> 0x0055, IllegalArgumentException -> 0x0050 }
            return r3
        L_0x0050:
            r0 = move-exception
            r20 = r4
            goto L_0x0224
        L_0x0055:
            r0 = move-exception
            r20 = r4
            goto L_0x0229
        L_0x005a:
            r0 = move-exception
            r20 = r4
            goto L_0x022e
        L_0x005f:
            r15 = 90
            if (r13 == 0) goto L_0x00f0
            int r8 = r8 + 1
            int r6 = r8 + 2
            int r8 = parseInt(r1, r8, r6)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            r9 = r8
            r8 = 58
            boolean r17 = checkOffset(r1, r6, r8)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            if (r17 == 0) goto L_0x0076
            int r6 = r6 + 1
        L_0x0076:
            int r14 = r6 + 2
            int r6 = parseInt(r1, r6, r14)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            r10 = r6
            boolean r6 = checkOffset(r1, r14, r8)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            if (r6 == 0) goto L_0x0087
            int r14 = r14 + 1
            r8 = r14
            goto L_0x0088
        L_0x0087:
            r8 = r14
        L_0x0088:
            int r6 = r23.length()     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            if (r6 <= r8) goto L_0x00ed
            char r6 = r1.charAt(r8)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            if (r6 == r15) goto L_0x00e8
            r14 = 43
            if (r6 == r14) goto L_0x00e8
            r14 = 45
            if (r6 == r14) goto L_0x00e8
            int r14 = r8 + 2
            int r8 = parseInt(r1, r8, r14)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            r11 = 59
            if (r8 <= r11) goto L_0x00ae
            r11 = 63
            if (r8 >= r11) goto L_0x00ae
            r8 = 59
            r11 = r8
            goto L_0x00af
        L_0x00ae:
            r11 = r8
        L_0x00af:
            r8 = 46
            boolean r8 = checkOffset(r1, r14, r8)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            if (r8 == 0) goto L_0x00e2
            int r14 = r14 + 1
            int r8 = r14 + 1
            int r8 = indexOfNonDigit(r1, r8)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            int r15 = r14 + 3
            int r15 = java.lang.Math.min(r8, r15)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            int r19 = parseInt(r1, r14, r15)     // Catch:{ IndexOutOfBoundsException -> 0x022b, NumberFormatException -> 0x0226, IllegalArgumentException -> 0x0221 }
            r20 = r4
            int r4 = r15 - r14
            r21 = r6
            r6 = 1
            if (r4 == r6) goto L_0x00dd
            r6 = 2
            if (r4 == r6) goto L_0x00d9
            r4 = r19
            r12 = r4
            goto L_0x00e0
        L_0x00d9:
            int r4 = r19 * 10
            r12 = r4
            goto L_0x00e0
        L_0x00dd:
            int r4 = r19 * 100
            r12 = r4
        L_0x00e0:
            r4 = r8
            goto L_0x00f2
        L_0x00e2:
            r20 = r4
            r21 = r6
            r8 = r14
            goto L_0x00f2
        L_0x00e8:
            r20 = r4
            r21 = r6
            goto L_0x00f2
        L_0x00ed:
            r20 = r4
            goto L_0x00f2
        L_0x00f0:
            r20 = r4
        L_0x00f2:
            int r4 = r23.length()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            if (r4 <= r8) goto L_0x0213
            r4 = 0
            char r6 = r1.charAt(r8)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r14 = 5
            r15 = 90
            if (r6 != r15) goto L_0x010b
            java.util.TimeZone r15 = TIMEZONE_UTC     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r4 = r15
            r15 = 1
            int r8 = r8 + r15
            r18 = r6
            goto L_0x01e0
        L_0x010b:
            r15 = 43
            if (r6 == r15) goto L_0x0132
            r15 = 45
            if (r6 != r15) goto L_0x0116
            r16 = r4
            goto L_0x0134
        L_0x0116:
            java.lang.IndexOutOfBoundsException r14 = new java.lang.IndexOutOfBoundsException     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.lang.StringBuilder r15 = new java.lang.StringBuilder     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r15.<init>()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r16 = r4
            java.lang.String r4 = "Invalid time zone indicator '"
            r15.append(r4)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r15.append(r6)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r15.append(r3)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.lang.String r4 = r15.toString()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r14.<init>(r4)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            throw r14     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
        L_0x0132:
            r16 = r4
        L_0x0134:
            java.lang.String r4 = r1.substring(r8)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            int r15 = r4.length()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            if (r15 < r14) goto L_0x0140
            r14 = r4
            goto L_0x0151
        L_0x0140:
            java.lang.StringBuilder r15 = new java.lang.StringBuilder     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r15.<init>()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r15.append(r4)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.lang.String r14 = "00"
            r15.append(r14)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.lang.String r14 = r15.toString()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
        L_0x0151:
            r4 = r14
            int r14 = r4.length()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            int r8 = r8 + r14
            java.lang.String r14 = "+0000"
            boolean r14 = r14.equals(r4)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            if (r14 != 0) goto L_0x01d6
            java.lang.String r14 = "+00:00"
            boolean r14 = r14.equals(r4)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            if (r14 == 0) goto L_0x016f
            r19 = r4
            r18 = r6
            r21 = r8
            goto L_0x01dc
        L_0x016f:
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r14.<init>()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.lang.String r15 = "GMT"
            r14.append(r15)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r14.append(r4)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.lang.String r14 = r14.toString()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.util.TimeZone r15 = java.util.TimeZone.getTimeZone(r14)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.lang.String r16 = r15.getID()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r18 = r16
            r19 = r4
            r4 = r18
            boolean r16 = r4.equals(r14)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            if (r16 != 0) goto L_0x01ce
            r18 = r6
            java.lang.String r6 = ":"
            r21 = r8
            java.lang.String r8 = ""
            java.lang.String r6 = r4.replace(r6, r8)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            boolean r8 = r6.equals(r14)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            if (r8 == 0) goto L_0x01a7
            goto L_0x01d4
        L_0x01a7:
            java.lang.IndexOutOfBoundsException r8 = new java.lang.IndexOutOfBoundsException     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r16 = r4
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r4.<init>()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r22 = r6
            java.lang.String r6 = "Mismatching time zone indicator: "
            r4.append(r6)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r4.append(r14)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.lang.String r6 = " given, resolves to "
            r4.append(r6)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.lang.String r6 = r15.getID()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r4.append(r6)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.lang.String r4 = r4.toString()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r8.<init>(r4)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            throw r8     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
        L_0x01ce:
            r16 = r4
            r18 = r6
            r21 = r8
        L_0x01d4:
            r4 = r15
            goto L_0x01de
        L_0x01d6:
            r19 = r4
            r18 = r6
            r21 = r8
        L_0x01dc:
            java.util.TimeZone r4 = TIMEZONE_UTC     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
        L_0x01de:
            r8 = r21
        L_0x01e0:
            java.util.GregorianCalendar r6 = new java.util.GregorianCalendar     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r6.<init>(r4)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r14 = 0
            r6.setLenient(r14)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r14 = 1
            r6.set(r14, r0)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            int r14 = r5 + -1
            r15 = 2
            r6.set(r15, r14)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r14 = 5
            r6.set(r14, r7)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r14 = 11
            r6.set(r14, r9)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r14 = 12
            r6.set(r14, r10)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r14 = 13
            r6.set(r14, r11)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r14 = 14
            r6.set(r14, r12)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            r2.setIndex(r8)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.util.Date r3 = r6.getTime()     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            return r3
        L_0x0213:
            java.lang.IllegalArgumentException r4 = new java.lang.IllegalArgumentException     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            java.lang.String r6 = "No time zone indicator"
            r4.<init>(r6)     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
            throw r4     // Catch:{ IndexOutOfBoundsException -> 0x021f, NumberFormatException -> 0x021d, IllegalArgumentException -> 0x021b }
        L_0x021b:
            r0 = move-exception
            goto L_0x0224
        L_0x021d:
            r0 = move-exception
            goto L_0x0229
        L_0x021f:
            r0 = move-exception
            goto L_0x022e
        L_0x0221:
            r0 = move-exception
            r20 = r4
        L_0x0224:
            r4 = r0
            goto L_0x0230
        L_0x0226:
            r0 = move-exception
            r20 = r4
        L_0x0229:
            r4 = r0
            goto L_0x022f
        L_0x022b:
            r0 = move-exception
            r20 = r4
        L_0x022e:
            r4 = r0
        L_0x022f:
        L_0x0230:
            if (r1 != 0) goto L_0x0234
            r0 = 0
            goto L_0x0248
        L_0x0234:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r5 = 34
            r0.append(r5)
            r0.append(r1)
            r0.append(r3)
            java.lang.String r0 = r0.toString()
        L_0x0248:
            java.lang.String r3 = r4.getMessage()
            if (r3 == 0) goto L_0x0254
            boolean r5 = r3.isEmpty()
            if (r5 == 0) goto L_0x0272
        L_0x0254:
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "("
            r5.append(r6)
            java.lang.Class r6 = r4.getClass()
            java.lang.String r6 = r6.getName()
            r5.append(r6)
            java.lang.String r6 = ")"
            r5.append(r6)
            java.lang.String r3 = r5.toString()
        L_0x0272:
            java.text.ParseException r5 = new java.text.ParseException
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "Failed to parse date ["
            r6.append(r7)
            r6.append(r0)
            java.lang.String r7 = "]: "
            r6.append(r7)
            r6.append(r3)
            java.lang.String r6 = r6.toString()
            int r7 = r24.getIndex()
            r5.<init>(r6, r7)
            r5.initCause(r4)
            throw r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.gson.internal.bind.util.ISO8601Utils.parse(java.lang.String, java.text.ParsePosition):java.util.Date");
    }

    private static boolean checkOffset(String value, int offset, char expected) {
        return offset < value.length() && value.charAt(offset) == expected;
    }

    private static int parseInt(String value, int beginIndex, int endIndex) throws NumberFormatException {
        if (beginIndex < 0 || endIndex > value.length() || beginIndex > endIndex) {
            throw new NumberFormatException(value);
        }
        int digit = beginIndex;
        int result = 0;
        if (digit < endIndex) {
            int i = digit + 1;
            int digit2 = Character.digit(value.charAt(digit), 10);
            if (digit2 >= 0) {
                result = -digit2;
                digit = i;
            } else {
                throw new NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex));
            }
        }
        while (digit < endIndex) {
            int i2 = digit + 1;
            int digit3 = Character.digit(value.charAt(digit), 10);
            if (digit3 >= 0) {
                result = (result * 10) - digit3;
                digit = i2;
            } else {
                throw new NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex));
            }
        }
        return -result;
    }

    private static void padInt(StringBuilder buffer, int value, int length) {
        String strValue = Integer.toString(value);
        for (int i = length - strValue.length(); i > 0; i--) {
            buffer.append('0');
        }
        buffer.append(strValue);
    }

    private static int indexOfNonDigit(String string, int offset) {
        for (int i = offset; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return i;
            }
        }
        return string.length();
    }
}
