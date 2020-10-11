package com.android.server.wifi.hotspot2;

import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.RawByteElement;
import com.android.server.wifi.util.InformationElementUtil;
import com.samsung.android.net.wifi.OpBrandingLoader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NetworkDetail {
    private static final String CHARSET_CN = "gbk";
    private static final String CHARSET_KOR = "ksc5601";
    private static final String CONFIG_CHARSET = OpBrandingLoader.getInstance().getSupportCharacterSet();
    private static final boolean DBG = false;
    private static final String TAG = "NetworkDetail:";
    private static final Map<String, NonUTF8Ssid> mNonUTF8SsidLists = new HashMap();
    private final Map<Constants.ANQPElementType, ANQPElement> mANQPElements;
    private final int mAnqpDomainID;
    private final int mAnqpOICount;
    private final Ant mAnt;
    private final long mBSSID;
    private final int mCapacity;
    private final int mCenterfreq0;
    private final int mCenterfreq1;
    private final int mChannelUtilization;
    private final int mChannelWidth;
    private final Set<Integer> mChipsetOuis;
    private int mDtimInterval = -1;
    private final int[] mEstimatedAirTimeFractions;
    private final InformationElementUtil.ExtendedCapabilities mExtendedCapabilities;
    private final long mHESSID;
    private final HSRelease mHSRelease;
    private final boolean mInternet;
    private final boolean mIsHiddenSsid;
    private final int mMaxRate;
    private final int mMboAssociationDisallowedReasonCode;
    private final int mPrimaryFreq;
    private final long[] mRoamingConsortiums;
    private final String mSSID;
    private final int mStationCount;
    private final int mWifiMode;
    private final byte[] semKtVsData;
    private final byte semKtVsOuiType;
    private final byte[] semVsData;
    private final byte semVsOuiType;

    public enum Ant {
        Private,
        PrivateWithGuest,
        ChargeablePublic,
        FreePublic,
        Personal,
        EmergencyOnly,
        Resvd6,
        Resvd7,
        Resvd8,
        Resvd9,
        Resvd10,
        Resvd11,
        Resvd12,
        Resvd13,
        TestOrExperimental,
        Wildcard
    }

    public enum HSRelease {
        R1,
        R2,
        Unknown
    }

    /* JADX WARNING: Removed duplicated region for block: B:112:0x02b8  */
    /* JADX WARNING: Removed duplicated region for block: B:116:0x02c1  */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x031c  */
    /* JADX WARNING: Removed duplicated region for block: B:120:0x0331  */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x034a  */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x0356  */
    /* JADX WARNING: Removed duplicated region for block: B:127:0x036f  */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x0377  */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x03d6  */
    /* JADX WARNING: Removed duplicated region for block: B:142:0x03e7  */
    /* JADX WARNING: Removed duplicated region for block: B:150:0x040c  */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x0416  */
    /* JADX WARNING: Removed duplicated region for block: B:171:0x02cb A[EDGE_INSN: B:171:0x02cb->B:117:0x02cb ?: BREAK  , SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x01f7  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01fd  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public NetworkDetail(java.lang.String r36, android.net.wifi.ScanResult.InformationElement[] r37, java.util.List<java.lang.String> r38, int r39) {
        /*
            r35 = this;
            r1 = r35
            r2 = r37
            java.lang.String r3 = "NetworkDetail:"
            r35.<init>()
            r4 = -1
            r1.mDtimInterval = r4
            if (r2 == 0) goto L_0x0420
            long r5 = com.android.server.wifi.hotspot2.Utils.parseMac(r36)
            r1.mBSSID = r5
            r5 = 0
            r6 = 0
            r7 = 0
            com.android.server.wifi.util.InformationElementUtil$BssLoad r0 = new com.android.server.wifi.util.InformationElementUtil$BssLoad
            r0.<init>()
            r8 = r0
            com.android.server.wifi.util.InformationElementUtil$Interworking r0 = new com.android.server.wifi.util.InformationElementUtil$Interworking
            r0.<init>()
            r9 = r0
            com.android.server.wifi.util.InformationElementUtil$RoamingConsortium r0 = new com.android.server.wifi.util.InformationElementUtil$RoamingConsortium
            r0.<init>()
            r10 = r0
            com.android.server.wifi.util.InformationElementUtil$Vsa r0 = new com.android.server.wifi.util.InformationElementUtil$Vsa
            r0.<init>()
            r11 = r0
            com.android.server.wifi.util.InformationElementUtil$HtOperation r0 = new com.android.server.wifi.util.InformationElementUtil$HtOperation
            r0.<init>()
            r12 = r0
            com.android.server.wifi.util.InformationElementUtil$VhtOperation r0 = new com.android.server.wifi.util.InformationElementUtil$VhtOperation
            r0.<init>()
            r13 = r0
            com.android.server.wifi.util.InformationElementUtil$ExtendedCapabilities r0 = new com.android.server.wifi.util.InformationElementUtil$ExtendedCapabilities
            r0.<init>()
            r14 = r0
            com.android.server.wifi.util.InformationElementUtil$TrafficIndicationMap r0 = new com.android.server.wifi.util.InformationElementUtil$TrafficIndicationMap
            r0.<init>()
            r15 = r0
            com.android.server.wifi.util.InformationElementUtil$SupportedRates r0 = new com.android.server.wifi.util.InformationElementUtil$SupportedRates
            r0.<init>()
            r16 = r0
            com.android.server.wifi.util.InformationElementUtil$SupportedRates r0 = new com.android.server.wifi.util.InformationElementUtil$SupportedRates
            r0.<init>()
            r17 = r0
            com.android.server.wifi.util.InformationElementUtil$Extension r0 = new com.android.server.wifi.util.InformationElementUtil$Extension
            r0.<init>()
            r18 = r0
            r19 = 0
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r20 = r0
            int r0 = r2.length     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x01c6 }
            r23 = r7
            r7 = 0
        L_0x0069:
            if (r7 >= r0) goto L_0x01b3
            r24 = r2[r7]     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x01a3 }
            r25 = r24
            r4 = r25
            r25 = r0
            int r0 = r4.id     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x01a3 }
            java.lang.Integer r0 = java.lang.Integer.valueOf(r0)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x01a3 }
            r2 = r20
            r2.add(r0)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0195 }
            int r0 = r4.id     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0195 }
            if (r0 == 0) goto L_0x016d
            r20 = r5
            r5 = 1
            if (r0 == r5) goto L_0x015f
            r5 = 5
            if (r0 == r5) goto L_0x014a
            r5 = 11
            if (r0 == r5) goto L_0x013c
            r5 = 50
            if (r0 == r5) goto L_0x012e
            r5 = 61
            if (r0 == r5) goto L_0x0114
            r5 = 107(0x6b, float:1.5E-43)
            if (r0 == r5) goto L_0x0105
            r5 = 111(0x6f, float:1.56E-43)
            if (r0 == r5) goto L_0x00f6
            r5 = 127(0x7f, float:1.78E-43)
            if (r0 == r5) goto L_0x00e7
            r5 = 192(0xc0, float:2.69E-43)
            if (r0 == r5) goto L_0x00d8
            r5 = 221(0xdd, float:3.1E-43)
            if (r0 == r5) goto L_0x00c9
            r5 = 255(0xff, float:3.57E-43)
            if (r0 == r5) goto L_0x00ba
            r5 = r18
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            goto L_0x017d
        L_0x00ba:
            r5 = r18
            r5.from(r4)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0123 }
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            goto L_0x017d
        L_0x00c9:
            r5 = r18
            r11.from(r4)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0123 }
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            goto L_0x017d
        L_0x00d8:
            r5 = r18
            r13.from(r4)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0123 }
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            goto L_0x017d
        L_0x00e7:
            r5 = r18
            r14.from(r4)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0123 }
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            goto L_0x017d
        L_0x00f6:
            r5 = r18
            r10.from(r4)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0123 }
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            goto L_0x017d
        L_0x0105:
            r5 = r18
            r9.from(r4)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0123 }
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            goto L_0x017d
        L_0x0114:
            r5 = r18
            r12.from(r4)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0123 }
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            goto L_0x017d
        L_0x0123:
            r0 = move-exception
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            goto L_0x01d7
        L_0x012e:
            r5 = r18
            r18 = r6
            r6 = r17
            r6.from(r4)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0158 }
            r17 = r2
            r2 = r16
            goto L_0x017d
        L_0x013c:
            r5 = r18
            r18 = r6
            r6 = r17
            r8.from(r4)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0158 }
            r17 = r2
            r2 = r16
            goto L_0x017d
        L_0x014a:
            r5 = r18
            r18 = r6
            r6 = r17
            r15.from(r4)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0158 }
            r17 = r2
            r2 = r16
            goto L_0x017d
        L_0x0158:
            r0 = move-exception
            r17 = r2
            r2 = r16
            goto L_0x01d7
        L_0x015f:
            r5 = r18
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            r2.from(r4)     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0193 }
            goto L_0x017d
        L_0x016d:
            r20 = r5
            r5 = r18
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            byte[] r0 = r4.bytes     // Catch:{ ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException -> 0x0193 }
            r23 = r0
        L_0x017d:
            int r7 = r7 + 1
            r16 = r2
            r0 = r25
            r2 = r37
            r34 = r18
            r18 = r5
            r5 = r20
            r20 = r17
            r17 = r6
            r6 = r34
            goto L_0x0069
        L_0x0193:
            r0 = move-exception
            goto L_0x01d7
        L_0x0195:
            r0 = move-exception
            r20 = r5
            r5 = r18
            r18 = r6
            r6 = r17
            r17 = r2
            r2 = r16
            goto L_0x01d7
        L_0x01a3:
            r0 = move-exception
            r2 = r16
            r34 = r20
            r20 = r5
            r5 = r18
            r18 = r6
            r6 = r17
            r17 = r34
            goto L_0x01d7
        L_0x01b3:
            r2 = r16
            r34 = r20
            r20 = r5
            r5 = r18
            r18 = r6
            r6 = r17
            r17 = r34
            r16 = r5
            r4 = r23
            goto L_0x01fb
        L_0x01c6:
            r0 = move-exception
            r2 = r16
            r34 = r20
            r20 = r5
            r5 = r18
            r18 = r6
            r6 = r17
            r17 = r34
            r23 = r7
        L_0x01d7:
            java.lang.Class r4 = r35.getClass()
            java.lang.String r4 = com.android.server.wifi.hotspot2.Utils.hs2LogTag(r4)
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            r16 = r5
            java.lang.String r5 = "Caught "
            r7.append(r5)
            r7.append(r0)
            java.lang.String r5 = r7.toString()
            android.util.Log.d(r4, r5)
            if (r23 == 0) goto L_0x0416
            r19 = r0
            r4 = r23
        L_0x01fb:
            if (r4 == 0) goto L_0x02c1
            java.nio.charset.Charset r0 = java.nio.charset.StandardCharsets.UTF_8
            java.nio.charset.CharsetDecoder r5 = r0.newDecoder()
            java.nio.ByteBuffer r0 = java.nio.ByteBuffer.wrap(r4)     // Catch:{ CharacterCodingException -> 0x0211 }
            java.nio.CharBuffer r0 = r5.decode(r0)     // Catch:{ CharacterCodingException -> 0x0211 }
            java.lang.String r7 = r0.toString()     // Catch:{ CharacterCodingException -> 0x0211 }
            r0 = r7
            goto L_0x0214
        L_0x0211:
            r0 = move-exception
            r7 = 0
            r0 = r7
        L_0x0214:
            if (r0 != 0) goto L_0x0234
            boolean r7 = r14.isStrictUtf8()
            if (r7 == 0) goto L_0x0227
            if (r19 != 0) goto L_0x021f
            goto L_0x0227
        L_0x021f:
            java.lang.IllegalArgumentException r3 = new java.lang.IllegalArgumentException
            java.lang.String r7 = "Failed to decode SSID in dubious IE string"
            r3.<init>(r7)
            throw r3
        L_0x0227:
            java.lang.String r7 = new java.lang.String
            r20 = r0
            java.nio.charset.Charset r0 = java.nio.charset.StandardCharsets.ISO_8859_1
            r7.<init>(r4, r0)
            r0 = r7
            r20 = r0
            goto L_0x0236
        L_0x0234:
            r20 = r0
        L_0x0236:
            java.lang.String r0 = CONFIG_CHARSET
            java.lang.String r7 = "gbk"
            boolean r0 = r7.equals(r0)
            r23 = r5
            java.lang.String r5 = "ksc5601"
            if (r0 != 0) goto L_0x0254
            java.lang.String r0 = CONFIG_CHARSET
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x024d
            goto L_0x0254
        L_0x024d:
            r25 = r2
            r26 = r12
            r27 = r13
            goto L_0x02b1
        L_0x0254:
            r25 = r2
            int r2 = r4.length
            r26 = r12
            r27 = r13
            long r12 = (long) r2
            boolean r0 = com.android.server.wifi.util.NativeUtil.isUTF8String(r4, r12)
            if (r0 != 0) goto L_0x02b1
            boolean r0 = com.android.server.wifi.util.NativeUtil.isUCNVString(r4, r2)
            if (r0 == 0) goto L_0x02b1
            java.lang.String r12 = ""
            java.lang.String r0 = CONFIG_CHARSET     // Catch:{ Exception -> 0x0298 }
            boolean r0 = r7.equals(r0)     // Catch:{ Exception -> 0x0298 }
            if (r0 == 0) goto L_0x0279
            java.lang.String r0 = new java.lang.String     // Catch:{ Exception -> 0x0298 }
            r0.<init>(r4, r7)     // Catch:{ Exception -> 0x0298 }
            r12 = r0
            goto L_0x027f
        L_0x0279:
            java.lang.String r0 = new java.lang.String     // Catch:{ Exception -> 0x0298 }
            r0.<init>(r4, r5)     // Catch:{ Exception -> 0x0298 }
            r12 = r0
        L_0x027f:
            if (r36 == 0) goto L_0x0290
            java.util.Map<java.lang.String, com.android.server.wifi.hotspot2.NetworkDetail$NonUTF8Ssid> r0 = mNonUTF8SsidLists     // Catch:{ Exception -> 0x0298 }
            java.lang.String r5 = r36.toUpperCase()     // Catch:{ Exception -> 0x0298 }
            com.android.server.wifi.hotspot2.NetworkDetail$NonUTF8Ssid r7 = new com.android.server.wifi.hotspot2.NetworkDetail$NonUTF8Ssid     // Catch:{ Exception -> 0x0298 }
            r7.<init>(r12, r4)     // Catch:{ Exception -> 0x0298 }
            r0.put(r5, r7)     // Catch:{ Exception -> 0x0298 }
            goto L_0x0295
        L_0x0290:
            java.lang.String r0 = "none UTF-8 ssid detected but, bssid is null"
            android.util.Log.e(r3, r0)     // Catch:{ Exception -> 0x0298 }
        L_0x0295:
            r0 = r12
            r5 = r0
            goto L_0x02b3
        L_0x0298:
            r0 = move-exception
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r7 = " Failed to decode UCNV e = "
            r5.append(r7)
            java.lang.String r7 = r0.toString()
            r5.append(r7)
            java.lang.String r5 = r5.toString()
            android.util.Log.e(r3, r5)
        L_0x02b1:
            r5 = r20
        L_0x02b3:
            r0 = 1
            int r2 = r4.length
            r3 = 0
        L_0x02b6:
            if (r3 >= r2) goto L_0x02cb
            byte r7 = r4[r3]
            if (r7 == 0) goto L_0x02be
            r0 = 0
            goto L_0x02cb
        L_0x02be:
            int r3 = r3 + 1
            goto L_0x02b6
        L_0x02c1:
            r25 = r2
            r26 = r12
            r27 = r13
            r0 = r18
            r5 = r20
        L_0x02cb:
            r1.mSSID = r5
            long r2 = r9.hessid
            r1.mHESSID = r2
            r1.mIsHiddenSsid = r0
            int r2 = r8.stationCount
            r1.mStationCount = r2
            int r2 = r8.channelUtilization
            r1.mChannelUtilization = r2
            int r2 = r8.capacity
            r1.mCapacity = r2
            com.android.server.wifi.hotspot2.NetworkDetail$Ant r2 = r9.ant
            r1.mAnt = r2
            boolean r2 = r9.internet
            r1.mInternet = r2
            com.android.server.wifi.hotspot2.NetworkDetail$HSRelease r2 = r11.hsRelease
            r1.mHSRelease = r2
            int r2 = r11.anqpDomainID
            r1.mAnqpDomainID = r2
            byte r2 = r11.semVsOuiType
            r1.semVsOuiType = r2
            byte[] r2 = r11.semVsData
            r1.semVsData = r2
            byte r2 = r11.semKtVsOuiType
            r1.semKtVsOuiType = r2
            byte[] r2 = r11.semKtVsData
            r1.semKtVsData = r2
            java.util.Set<java.lang.Integer> r2 = r11.chipsetOuis
            r1.mChipsetOuis = r2
            int r2 = r10.anqpOICount
            r1.mAnqpOICount = r2
            long[] r2 = r10.getRoamingConsortiums()
            r1.mRoamingConsortiums = r2
            r1.mExtendedCapabilities = r14
            r2 = 0
            r1.mANQPElements = r2
            r3 = r39
            r1.mPrimaryFreq = r3
            boolean r7 = r27.isValid()
            if (r7 == 0) goto L_0x0331
            int r7 = r27.getChannelWidth()
            r1.mChannelWidth = r7
            int r7 = r27.getCenterFreq0()
            r1.mCenterfreq0 = r7
            int r7 = r27.getCenterFreq1()
            r1.mCenterfreq1 = r7
            r12 = r26
            goto L_0x0344
        L_0x0331:
            int r7 = r26.getChannelWidth()
            r1.mChannelWidth = r7
            int r7 = r1.mPrimaryFreq
            r12 = r26
            int r7 = r12.getCenterFreq0(r7)
            r1.mCenterfreq0 = r7
            r7 = 0
            r1.mCenterfreq1 = r7
        L_0x0344:
            boolean r7 = r15.isValid()
            if (r7 == 0) goto L_0x034e
            int r7 = r15.mDtimPeriod
            r1.mDtimInterval = r7
        L_0x034e:
            r7 = 0
            r13 = 0
            boolean r18 = r6.isValid()
            if (r18 == 0) goto L_0x036f
            java.util.ArrayList<java.lang.Integer> r2 = r6.mRates
            r20 = r0
            java.util.ArrayList<java.lang.Integer> r0 = r6.mRates
            int r0 = r0.size()
            r22 = 1
            int r0 = r0 + -1
            java.lang.Object r0 = r2.get(r0)
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r13 = r0.intValue()
            goto L_0x0371
        L_0x036f:
            r20 = r0
        L_0x0371:
            boolean r0 = r25.isValid()
            if (r0 == 0) goto L_0x03d6
            r2 = r25
            java.util.ArrayList<java.lang.Integer> r0 = r2.mRates
            java.util.ArrayList<java.lang.Integer> r3 = r2.mRates
            int r3 = r3.size()
            r22 = 1
            int r3 = r3 + -1
            java.lang.Object r0 = r0.get(r3)
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r7 = r0.intValue()
            if (r7 <= r13) goto L_0x0393
            r0 = r7
            goto L_0x0394
        L_0x0393:
            r0 = r13
        L_0x0394:
            r1.mMaxRate = r0
            int r0 = r1.mPrimaryFreq
            int r3 = r1.mMaxRate
            r25 = r2
            r2 = r16
            r16 = r4
            com.android.server.wifi.util.InformationElementUtil$Extension$HeOperation r4 = r2.f34ho
            if (r4 == 0) goto L_0x03ad
            com.android.server.wifi.util.InformationElementUtil$Extension$HeOperation r4 = r2.f34ho
            boolean r4 = r4.isValid()
            r30 = r4
            goto L_0x03af
        L_0x03ad:
            r30 = 0
        L_0x03af:
            boolean r31 = r27.isValid()
            r4 = 61
            java.lang.Integer r4 = java.lang.Integer.valueOf(r4)
            r21 = r5
            r5 = r17
            boolean r32 = r5.contains(r4)
            r4 = 42
            java.lang.Integer r4 = java.lang.Integer.valueOf(r4)
            boolean r33 = r5.contains(r4)
            r28 = r0
            r29 = r3
            int r0 = com.android.server.wifi.util.InformationElementUtil.WifiMode.determineMode(r28, r29, r30, r31, r32, r33)
            r1.mWifiMode = r0
            goto L_0x03e3
        L_0x03d6:
            r21 = r5
            r2 = r16
            r5 = r17
            r16 = r4
            r3 = 0
            r1.mWifiMode = r3
            r1.mMaxRate = r3
        L_0x03e3:
            com.android.server.wifi.util.InformationElementUtil$Extension$EstimatedServiceParameters r0 = r2.esp
            if (r0 == 0) goto L_0x040c
            r0 = 4
            int[] r3 = new int[r0]
            r1.mEstimatedAirTimeFractions = r3
            r3 = 0
        L_0x03ed:
            if (r3 >= r0) goto L_0x0409
            com.android.server.wifi.util.InformationElementUtil$Extension$EstimatedServiceParameters r4 = r2.esp
            com.android.server.wifi.util.InformationElementUtil$Extension$EstimatedServiceParameters$EspInformation[] r4 = r4.espInformations
            r4 = r4[r3]
            int[] r0 = r1.mEstimatedAirTimeFractions
            if (r4 != 0) goto L_0x03fd
            r22 = r2
            r2 = -1
            goto L_0x0401
        L_0x03fd:
            r22 = r2
            int r2 = r4.estimatedAirTimeFraction
        L_0x0401:
            r0[r3] = r2
            int r3 = r3 + 1
            r2 = r22
            r0 = 4
            goto L_0x03ed
        L_0x0409:
            r22 = r2
            goto L_0x0411
        L_0x040c:
            r22 = r2
            r0 = 0
            r1.mEstimatedAirTimeFractions = r0
        L_0x0411:
            int r0 = r11.mboAssociationDisallowedReasonCode
            r1.mMboAssociationDisallowedReasonCode = r0
            return
        L_0x0416:
            r25 = r2
            java.lang.IllegalArgumentException r2 = new java.lang.IllegalArgumentException
            java.lang.String r3 = "Malformed IE string (no SSID)"
            r2.<init>(r3, r0)
            throw r2
        L_0x0420:
            java.lang.IllegalArgumentException r0 = new java.lang.IllegalArgumentException
            java.lang.String r2 = "Null information elements"
            r0.<init>(r2)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.hotspot2.NetworkDetail.<init>(java.lang.String, android.net.wifi.ScanResult$InformationElement[], java.util.List, int):void");
    }

    private static ByteBuffer getAndAdvancePayload(ByteBuffer data, int plLength) {
        ByteBuffer payload = data.duplicate().order(data.order());
        payload.limit(payload.position() + plLength);
        data.position(data.position() + plLength);
        return payload;
    }

    private NetworkDetail(NetworkDetail base, Map<Constants.ANQPElementType, ANQPElement> anqpElements) {
        this.mSSID = base.mSSID;
        this.mIsHiddenSsid = base.mIsHiddenSsid;
        this.mBSSID = base.mBSSID;
        this.mHESSID = base.mHESSID;
        this.mStationCount = base.mStationCount;
        this.mChannelUtilization = base.mChannelUtilization;
        this.mCapacity = base.mCapacity;
        this.mAnt = base.mAnt;
        this.mInternet = base.mInternet;
        this.mHSRelease = base.mHSRelease;
        this.mAnqpDomainID = base.mAnqpDomainID;
        this.semVsOuiType = base.semVsOuiType;
        this.semVsData = base.semVsData;
        this.semKtVsOuiType = base.semKtVsOuiType;
        this.semKtVsData = base.semKtVsData;
        this.mChipsetOuis = base.mChipsetOuis;
        this.mAnqpOICount = base.mAnqpOICount;
        this.mRoamingConsortiums = base.mRoamingConsortiums;
        this.mExtendedCapabilities = new InformationElementUtil.ExtendedCapabilities(base.mExtendedCapabilities);
        this.mANQPElements = anqpElements;
        this.mChannelWidth = base.mChannelWidth;
        this.mPrimaryFreq = base.mPrimaryFreq;
        this.mCenterfreq0 = base.mCenterfreq0;
        this.mCenterfreq1 = base.mCenterfreq1;
        this.mDtimInterval = base.mDtimInterval;
        this.mWifiMode = base.mWifiMode;
        this.mMaxRate = base.mMaxRate;
        this.mEstimatedAirTimeFractions = base.mEstimatedAirTimeFractions;
        this.mMboAssociationDisallowedReasonCode = base.mMboAssociationDisallowedReasonCode;
    }

    public NetworkDetail complete(Map<Constants.ANQPElementType, ANQPElement> anqpElements) {
        return new NetworkDetail(this, anqpElements);
    }

    public boolean queriable(List<Constants.ANQPElementType> queryElements) {
        return this.mAnt != null && (Constants.hasBaseANQPElements(queryElements) || (Constants.hasR2Elements(queryElements) && this.mHSRelease == HSRelease.R2));
    }

    public boolean has80211uInfo() {
        return (this.mAnt == null && this.mRoamingConsortiums == null && this.mHSRelease == null) ? false : true;
    }

    public boolean hasInterworking() {
        return this.mAnt != null;
    }

    public String getSSID() {
        return this.mSSID;
    }

    public String getTrimmedSSID() {
        if (this.mSSID == null) {
            return "";
        }
        for (int n = 0; n < this.mSSID.length(); n++) {
            if (this.mSSID.charAt(n) != 0) {
                return this.mSSID;
            }
        }
        return "";
    }

    public long getHESSID() {
        return this.mHESSID;
    }

    public long getBSSID() {
        return this.mBSSID;
    }

    public int getStationCount() {
        return this.mStationCount;
    }

    public int getChannelUtilization() {
        return this.mChannelUtilization;
    }

    public int getCapacity() {
        return this.mCapacity;
    }

    public boolean isInterworking() {
        return this.mAnt != null;
    }

    public Ant getAnt() {
        return this.mAnt;
    }

    public boolean isInternet() {
        return this.mInternet;
    }

    public HSRelease getHSRelease() {
        return this.mHSRelease;
    }

    public int getAnqpDomainID() {
        return this.mAnqpDomainID;
    }

    public byte semGetVsOuiType() {
        return this.semVsOuiType;
    }

    public byte[] semGetVsData() {
        return this.semVsData;
    }

    public byte semGetKtVsOuiType() {
        return this.semKtVsOuiType;
    }

    public byte[] semGetKtVsData() {
        return this.semKtVsData;
    }

    public Set<Integer> getChipsetOuis() {
        return this.mChipsetOuis;
    }

    public byte[] getOsuProviders() {
        ANQPElement osuProviders;
        Map<Constants.ANQPElementType, ANQPElement> map = this.mANQPElements;
        if (map == null || (osuProviders = map.get(Constants.ANQPElementType.HSOSUProviders)) == null) {
            return null;
        }
        return ((RawByteElement) osuProviders).getPayload();
    }

    public int getAnqpOICount() {
        return this.mAnqpOICount;
    }

    public long[] getRoamingConsortiums() {
        return this.mRoamingConsortiums;
    }

    public Map<Constants.ANQPElementType, ANQPElement> getANQPElements() {
        return this.mANQPElements;
    }

    public int getChannelWidth() {
        return this.mChannelWidth;
    }

    public int getCenterfreq0() {
        return this.mCenterfreq0;
    }

    public int getCenterfreq1() {
        return this.mCenterfreq1;
    }

    public int getWifiMode() {
        return this.mWifiMode;
    }

    public int getDtimInterval() {
        return this.mDtimInterval;
    }

    public boolean is80211McResponderSupport() {
        return this.mExtendedCapabilities.is80211McRTTResponder();
    }

    public boolean isSSID_UTF8() {
        return this.mExtendedCapabilities.isStrictUtf8();
    }

    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        NetworkDetail that = (NetworkDetail) thatObject;
        if (!getSSID().equals(that.getSSID()) || getBSSID() != that.getBSSID()) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        long j = this.mBSSID;
        return (((this.mSSID.hashCode() * 31) + ((int) (j >>> 32))) * 31) + ((int) j);
    }

    public String toString() {
        return String.format("NetworkInfo{SSID='%s', HESSID=%x, BSSID=%x, StationCount=%d, ChannelUtilization=%d, Capacity=%d, Ant=%s, Internet=%s, HSRelease=%s, AnqpDomainID=%d, AnqpOICount=%d, RoamingConsortiums=%s}", new Object[]{this.mSSID, Long.valueOf(this.mHESSID), Long.valueOf(this.mBSSID), Integer.valueOf(this.mStationCount), Integer.valueOf(this.mChannelUtilization), Integer.valueOf(this.mCapacity), this.mAnt, Boolean.valueOf(this.mInternet), this.mHSRelease, Integer.valueOf(this.mAnqpDomainID), Integer.valueOf(this.mAnqpOICount), Utils.roamingConsortiumsToString(this.mRoamingConsortiums)});
    }

    public String toKeyString() {
        if (this.mHESSID != 0) {
            return String.format("'%s':%012x (%012x)", new Object[]{this.mSSID, Long.valueOf(this.mBSSID), Long.valueOf(this.mHESSID)});
        }
        return String.format("'%s':%012x", new Object[]{this.mSSID, Long.valueOf(this.mBSSID)});
    }

    public String getBSSIDString() {
        return toMACString(this.mBSSID);
    }

    public boolean isBeaconFrame() {
        return this.mDtimInterval > 0;
    }

    public boolean isHiddenBeaconFrame() {
        return isBeaconFrame() && this.mIsHiddenSsid;
    }

    public static String toMACString(long mac) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int n = 5; n >= 0; n--) {
            if (first) {
                first = false;
            } else {
                sb.append(':');
            }
            sb.append(String.format("%02x", new Object[]{Long.valueOf((mac >>> (n * 8)) & 255)}));
        }
        return sb.toString();
    }

    public static Map<String, NonUTF8Ssid> getNonUTF8SsidLists() {
        return mNonUTF8SsidLists;
    }

    public static void clearNonUTF8SsidLists() {
        Map<String, NonUTF8Ssid> map = mNonUTF8SsidLists;
        if (map != null) {
            map.clear();
        }
    }

    public static String toOUIString(int oui) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int n = 0; n < 3; n++) {
            if (first) {
                first = false;
            } else {
                sb.append(':');
            }
            sb.append(String.format("%02x", new Object[]{Integer.valueOf((oui >>> (n * 8)) & 255)}));
        }
        return sb.toString();
    }

    public int getEstimatedAirTimeFraction(int accessCategory) {
        int[] iArr = this.mEstimatedAirTimeFractions;
        if (iArr == null) {
            return -1;
        }
        return iArr[accessCategory];
    }

    public int getMboAssociationDisallowedReasonCode() {
        return this.mMboAssociationDisallowedReasonCode;
    }

    public static class NonUTF8Ssid {
        public final String ssid;
        public final byte[] ssidOctets;

        public NonUTF8Ssid(String ssid2, byte[] ssidOctets2) {
            this.ssid = ssid2;
            this.ssidOctets = ssidOctets2;
        }
    }
}
