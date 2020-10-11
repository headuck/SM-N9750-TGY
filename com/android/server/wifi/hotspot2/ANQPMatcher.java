package com.android.server.wifi.hotspot2;

import android.os.SystemProperties;
import com.android.server.wifi.IMSIParameter;
import com.android.server.wifi.hotspot2.anqp.CellularNetwork;
import com.android.server.wifi.hotspot2.anqp.DomainNameElement;
import com.android.server.wifi.hotspot2.anqp.NAIRealmData;
import com.android.server.wifi.hotspot2.anqp.NAIRealmElement;
import com.android.server.wifi.hotspot2.anqp.RoamingConsortiumElement;
import com.android.server.wifi.hotspot2.anqp.ThreeGPPNetworkElement;
import com.android.server.wifi.hotspot2.anqp.eap.AuthParam;
import com.android.server.wifi.hotspot2.anqp.eap.EAPMethod;
import com.samsung.android.net.wifi.OpBrandingLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ANQPMatcher {
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();

    public static boolean matchDomainName(DomainNameElement element, String fqdn, IMSIParameter imsiParam, List<String> simImsiList) {
        String numericOfSim;
        if (element == null) {
            return false;
        }
        String mccMncOfSim = null;
        if (OpBrandingLoader.Vendor.SKT == mOpBranding && (numericOfSim = SystemProperties.get("gsm.sim.operator.numeric")) != null && numericOfSim.length() == 5) {
            mccMncOfSim = numericOfSim.substring(0, 3) + "0" + numericOfSim.substring(3, 5);
        }
        for (String domain : element.getDomains()) {
            if (DomainMatcher.arg2SubdomainOfArg1(fqdn, domain)) {
                return true;
            }
            String mccMncOfDomain = Utils.getMccMnc(Utils.splitDomain(domain));
            if (matchMccMnc(mccMncOfDomain, imsiParam, simImsiList)) {
                return true;
            }
            if (OpBrandingLoader.Vendor.SKT == mOpBranding && mccMncOfDomain != null && mccMncOfSim != null && imsiParam != null && mccMncOfDomain.equals(mccMncOfSim)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchRoamingConsortium(RoamingConsortiumElement element, long[] providerOIs) {
        if (element == null || providerOIs == null) {
            return false;
        }
        List<Long> rcOIs = element.getOIs();
        for (long oi : providerOIs) {
            if (rcOIs.contains(Long.valueOf(oi))) {
                return true;
            }
        }
        return false;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: CodeShrinkVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Don't wrap MOVE or CONST insns: 0x0028: MOVE  (r0v8 'bestMatch' int) = (r3v0 'match' int)
        	at jadx.core.dex.instructions.args.InsnArg.wrapArg(InsnArg.java:164)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.assignInline(CodeShrinkVisitor.java:133)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.checkInline(CodeShrinkVisitor.java:118)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.shrinkBlock(CodeShrinkVisitor.java:65)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.shrinkMethod(CodeShrinkVisitor.java:43)
        	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.visit(CodeShrinkVisitor.java:35)
        */
    public static int matchNAIRealm(com.android.server.wifi.hotspot2.anqp.NAIRealmElement r5, java.lang.String r6, int r7, com.android.server.wifi.hotspot2.anqp.eap.AuthParam r8) {
        /*
            if (r5 == 0) goto L_0x002f
            java.util.List r0 = r5.getRealmDataList()
            boolean r0 = r0.isEmpty()
            if (r0 == 0) goto L_0x000d
            goto L_0x002f
        L_0x000d:
            r0 = -1
            java.util.List r1 = r5.getRealmDataList()
            java.util.Iterator r1 = r1.iterator()
        L_0x0016:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L_0x002e
            java.lang.Object r2 = r1.next()
            com.android.server.wifi.hotspot2.anqp.NAIRealmData r2 = (com.android.server.wifi.hotspot2.anqp.NAIRealmData) r2
            int r3 = matchNAIRealmData(r2, r6, r7, r8)
            if (r3 <= r0) goto L_0x002d
            r0 = r3
            r4 = 7
            if (r0 != r4) goto L_0x002d
            goto L_0x002e
        L_0x002d:
            goto L_0x0016
        L_0x002e:
            return r0
        L_0x002f:
            r0 = 0
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.hotspot2.ANQPMatcher.matchNAIRealm(com.android.server.wifi.hotspot2.anqp.NAIRealmElement, java.lang.String, int, com.android.server.wifi.hotspot2.anqp.eap.AuthParam):int");
    }

    public static int getCarrierEapMethodFromMatchingNAIRealm(String realm, NAIRealmElement element) {
        if (element == null || element.getRealmDataList().isEmpty()) {
            return -1;
        }
        for (NAIRealmData realmData : element.getRealmDataList()) {
            int eapMethodID = getEapMethodForNAIRealmWithCarrier(realm, realmData);
            if (eapMethodID != -1) {
                return eapMethodID;
            }
        }
        return -1;
    }

    public static boolean matchThreeGPPNetwork(ThreeGPPNetworkElement element, IMSIParameter imsiParam, List<String> simImsiList) {
        if (element == null) {
            return false;
        }
        for (CellularNetwork network : element.getNetworks()) {
            if (matchCellularNetwork(network, imsiParam, simImsiList)) {
                return true;
            }
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x0032 A[LOOP:1: B:10:0x0032->B:13:0x0043, LOOP_START, PHI: r1 
      PHI: (r1v5 'eapMethodMatch' int) = (r1v4 'eapMethodMatch' int), (r1v7 'eapMethodMatch' int) binds: [B:9:0x0029, B:13:0x0043] A[DONT_GENERATE, DONT_INLINE]] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int matchNAIRealmData(com.android.server.wifi.hotspot2.anqp.NAIRealmData r5, java.lang.String r6, int r7, com.android.server.wifi.hotspot2.anqp.eap.AuthParam r8) {
        /*
            r0 = -1
            java.util.List r1 = r5.getRealms()
            java.util.Iterator r1 = r1.iterator()
        L_0x0009:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L_0x001e
            java.lang.Object r2 = r1.next()
            java.lang.String r2 = (java.lang.String) r2
            boolean r3 = com.android.server.wifi.hotspot2.DomainMatcher.arg2SubdomainOfArg1(r6, r2)
            if (r3 == 0) goto L_0x001d
            r0 = 4
            goto L_0x001e
        L_0x001d:
            goto L_0x0009
        L_0x001e:
            java.util.List r1 = r5.getEAPMethods()
            boolean r1 = r1.isEmpty()
            if (r1 == 0) goto L_0x0029
            return r0
        L_0x0029:
            r1 = -1
            java.util.List r2 = r5.getEAPMethods()
            java.util.Iterator r2 = r2.iterator()
        L_0x0032:
            boolean r3 = r2.hasNext()
            r4 = -1
            if (r3 == 0) goto L_0x0047
            java.lang.Object r3 = r2.next()
            com.android.server.wifi.hotspot2.anqp.eap.EAPMethod r3 = (com.android.server.wifi.hotspot2.anqp.eap.EAPMethod) r3
            int r1 = matchEAPMethod(r3, r7, r8)
            if (r1 == r4) goto L_0x0046
            goto L_0x0047
        L_0x0046:
            goto L_0x0032
        L_0x0047:
            if (r1 != r4) goto L_0x005d
            com.samsung.android.feature.SemCscFeature r2 = com.samsung.android.feature.SemCscFeature.getInstance()
            java.lang.String r3 = "CscFeature_Wifi_CaptivePortalException"
            java.lang.String r2 = r2.getString(r3)
            java.lang.String r3 = "CCT"
            boolean r2 = r3.equals(r2)
            if (r2 == 0) goto L_0x005c
            return r0
        L_0x005c:
            return r4
        L_0x005d:
            if (r0 != r4) goto L_0x0060
            return r1
        L_0x0060:
            r2 = r0 | r1
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.hotspot2.ANQPMatcher.matchNAIRealmData(com.android.server.wifi.hotspot2.anqp.NAIRealmData, java.lang.String, int, com.android.server.wifi.hotspot2.anqp.eap.AuthParam):int");
    }

    private static int getEapMethodForNAIRealmWithCarrier(String realm, NAIRealmData realmData) {
        int realmMatch = -1;
        Iterator<String> it = realmData.getRealms().iterator();
        while (true) {
            if (it.hasNext()) {
                if (DomainMatcher.arg2SubdomainOfArg1(realm, it.next())) {
                    realmMatch = 4;
                    break;
                }
            } else {
                break;
            }
        }
        if (realmMatch == -1) {
            return -1;
        }
        for (EAPMethod eapMethod : realmData.getEAPMethods()) {
            if (Utils.isCarrierEapMethod(eapMethod.getEAPMethodID())) {
                return eapMethod.getEAPMethodID();
            }
        }
        return -1;
    }

    private static int matchEAPMethod(EAPMethod method, int eapMethodID, AuthParam authParam) {
        if (method.getEAPMethodID() != eapMethodID) {
            return -1;
        }
        if (authParam == null) {
            return 2;
        }
        Map<Integer, Set<AuthParam>> authParams = method.getAuthParams();
        if (authParams.isEmpty()) {
            return 2;
        }
        Set<AuthParam> paramSet = authParams.get(Integer.valueOf(authParam.getAuthTypeID()));
        if (paramSet == null || !paramSet.contains(authParam)) {
            return -1;
        }
        return 3;
    }

    private static boolean matchCellularNetwork(CellularNetwork network, IMSIParameter imsiParam, List<String> simImsiList) {
        for (String plmn : network.getPlmns()) {
            if (matchMccMnc(plmn, imsiParam, simImsiList)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchMccMnc(String mccMnc, IMSIParameter imsiParam, List<String> simImsiList) {
        if (imsiParam == null || simImsiList == null || mccMnc == null || !imsiParam.matchesMccMnc(mccMnc)) {
            return false;
        }
        for (String imsi : simImsiList) {
            if (imsi.startsWith(mccMnc)) {
                return true;
            }
        }
        return false;
    }
}
