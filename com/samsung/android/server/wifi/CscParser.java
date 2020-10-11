package com.samsung.android.server.wifi;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.wifi.hotspot2.SystemInfo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CscParser {
    private static final String CSC_CHAMELEON_FILE = "/carrier/chameleon.xml";
    private static final String CSC_ID_FILE = "/system/SW_Configuration.xml";
    private static String CSC_OTHERS_FILE = "/system/csc/others.xml";
    private static String CSC_XML_FILE = "/system/csc/customer.xml";
    private static final String OMC_ID_FILE = "/system/omc/SW_Configuration.xml";
    private static final String SALES_CODE_PATH = "/efs/imei/mps_code.dat";
    private static final String TAG = "CscParser";
    private Document mDoc;
    private Node mRoot;

    public static class CscNodeList implements NodeList {
        private ArrayList<Node> children = new ArrayList<>();

        /* access modifiers changed from: package-private */
        public void appendChild(Node newChild) {
            this.children.add(newChild);
        }

        public int getLength() {
            return this.children.size();
        }

        public Node item(int index) {
            return this.children.get(index);
        }
    }

    public CscParser(Context context) {
    }

    public CscParser(String fileName) {
        try {
            update(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void update(String fileName) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        if (new File(fileName).exists()) {
            Log.e(TAG, "update(): xml file exist");
            this.mDoc = builder.parse(new File(fileName));
            this.mRoot = this.mDoc.getDocumentElement();
            return;
        }
        Log.e(TAG, "update(): xml file not exist");
    }

    public String get(String path) {
        Node firstChild;
        Node node = search(path);
        if (node == null || (firstChild = node.getFirstChild()) == null) {
            return null;
        }
        return firstChild.getNodeValue();
    }

    public Node search(String path) {
        if (path == null) {
            return null;
        }
        Node node = this.mRoot;
        StringTokenizer tokenizer = new StringTokenizer(path, ".");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (node == null) {
                return null;
            }
            node = search(node, token);
        }
        return node;
    }

    public Node search(Node parent, String name) {
        NodeList children;
        if (!(parent == null || (children = parent.getChildNodes()) == null)) {
            int n = children.getLength();
            for (int i = 0; i < n; i++) {
                Node child = children.item(i);
                if (child.getNodeName().equals(name)) {
                    return child;
                }
            }
        }
        return null;
    }

    public NodeList searchList(Node parent, String name) {
        if (parent == null) {
            return null;
        }
        try {
            CscNodeList list = new CscNodeList();
            NodeList children = parent.getChildNodes();
            if (children != null) {
                int n = children.getLength();
                for (int i = 0; i < n; i++) {
                    Node child = children.item(i);
                    if (child.getNodeName().equals(name)) {
                        try {
                            list.appendChild(child);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return list;
        } catch (Exception e2) {
            return null;
        }
    }

    public String getValue(Node node) {
        if (node == null) {
            return null;
        }
        if (node.getChildNodes().getLength() > 1) {
            String stringValue = new String();
            for (int idx = 0; idx < node.getChildNodes().getLength(); idx++) {
                stringValue = stringValue + node.getChildNodes().item(idx).getNodeValue();
            }
            return stringValue;
        }
        Node firstChild = node.getFirstChild();
        if (firstChild != null) {
            return firstChild.getNodeValue();
        }
        return null;
    }

    public String getAttrbute(String tagPath, int index, int mode) {
        String attribute = null;
        String[] tagSplit = tagPath.split("[.]");
        int tagCount = tagSplit.length;
        int tagCount2 = tagCount - 1;
        if (tagCount < 3) {
            return null;
        }
        int tagCount3 = tagCount2 - 1;
        String tagAttr = tagSplit[tagCount2];
        String tagList = tagSplit[tagCount3];
        String tagNode = null;
        for (int i = 0; i < tagCount3; i++) {
            if (tagNode == null) {
                tagNode = tagSplit[i];
            } else {
                tagNode = tagNode + "." + tagSplit[i];
            }
        }
        NodeList nodeList = searchList(search(tagNode), tagList);
        if (nodeList != null && nodeList.getLength() > index) {
            attribute = ((Element) nodeList.item(index)).getAttribute(tagAttr);
        }
        if (attribute != null && mode == 1) {
            String[] attrSlash = attribute.split("/");
            int cntSlash = attrSlash.length - 1;
            if (attrSlash[cntSlash] != null) {
                String[] attrSplit = attrSlash[cntSlash].split("[.]");
                if (attrSplit[0] != null) {
                    attribute = attrSplit[0];
                }
            }
        }
        Log.d(TAG, tagList + ": " + attribute);
        return attribute;
    }

    /* JADX WARNING: Removed duplicated region for block: B:40:0x00b1 A[ORIG_RETURN, RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:50:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String getSalesCode() {
        /*
            java.lang.String r0 = "/efs/imei/mps_code.dat"
            java.lang.String r1 = "IOException : "
            java.lang.String r2 = "CscParser"
            r3 = 0
            r4 = 0
            r5 = 0
            java.io.File r6 = new java.io.File     // Catch:{ FileNotFoundException -> 0x007e, IOException -> 0x0053 }
            r6.<init>(r0)     // Catch:{ FileNotFoundException -> 0x007e, IOException -> 0x0053 }
            boolean r7 = r6.exists()     // Catch:{ FileNotFoundException -> 0x007e, IOException -> 0x0053 }
            if (r7 == 0) goto L_0x0026
            java.io.FileReader r7 = new java.io.FileReader     // Catch:{ FileNotFoundException -> 0x007e, IOException -> 0x0053 }
            r7.<init>(r0)     // Catch:{ FileNotFoundException -> 0x007e, IOException -> 0x0053 }
            r4 = r7
            java.io.BufferedReader r0 = new java.io.BufferedReader     // Catch:{ FileNotFoundException -> 0x007e, IOException -> 0x0053 }
            r0.<init>(r4)     // Catch:{ FileNotFoundException -> 0x007e, IOException -> 0x0053 }
            r5 = r0
            java.lang.String r0 = r5.readLine()     // Catch:{ FileNotFoundException -> 0x007e, IOException -> 0x0053 }
            r3 = r0
            goto L_0x002b
        L_0x0026:
            java.lang.String r0 = "mps_code.dat does not exist"
            android.util.Log.e(r2, r0)     // Catch:{ FileNotFoundException -> 0x007e, IOException -> 0x0053 }
        L_0x002b:
            if (r4 == 0) goto L_0x0033
            r4.close()     // Catch:{ IOException -> 0x0031 }
            goto L_0x0033
        L_0x0031:
            r0 = move-exception
            goto L_0x0039
        L_0x0033:
            if (r5 == 0) goto L_0x0050
            r5.close()     // Catch:{ IOException -> 0x0031 }
            goto L_0x0050
        L_0x0039:
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
        L_0x003e:
            r6.append(r1)
            java.lang.String r1 = r0.getMessage()
            r6.append(r1)
            java.lang.String r1 = r6.toString()
            android.util.Log.e(r2, r1)
            goto L_0x00ab
        L_0x0050:
            goto L_0x00ab
        L_0x0051:
            r0 = move-exception
            goto L_0x00b4
        L_0x0053:
            r0 = move-exception
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ all -> 0x0051 }
            r6.<init>()     // Catch:{ all -> 0x0051 }
            r6.append(r1)     // Catch:{ all -> 0x0051 }
            java.lang.String r7 = r0.getMessage()     // Catch:{ all -> 0x0051 }
            r6.append(r7)     // Catch:{ all -> 0x0051 }
            java.lang.String r6 = r6.toString()     // Catch:{ all -> 0x0051 }
            android.util.Log.e(r2, r6)     // Catch:{ all -> 0x0051 }
            if (r4 == 0) goto L_0x0072
            r4.close()     // Catch:{ IOException -> 0x0070 }
            goto L_0x0072
        L_0x0070:
            r0 = move-exception
            goto L_0x0078
        L_0x0072:
            if (r5 == 0) goto L_0x0050
            r5.close()     // Catch:{ IOException -> 0x0070 }
            goto L_0x0050
        L_0x0078:
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            goto L_0x003e
        L_0x007e:
            r0 = move-exception
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ all -> 0x0051 }
            r6.<init>()     // Catch:{ all -> 0x0051 }
            java.lang.String r7 = "File not Found exception: "
            r6.append(r7)     // Catch:{ all -> 0x0051 }
            java.lang.String r7 = r0.getMessage()     // Catch:{ all -> 0x0051 }
            r6.append(r7)     // Catch:{ all -> 0x0051 }
            java.lang.String r6 = r6.toString()     // Catch:{ all -> 0x0051 }
            android.util.Log.e(r2, r6)     // Catch:{ all -> 0x0051 }
            if (r4 == 0) goto L_0x009f
            r4.close()     // Catch:{ IOException -> 0x009d }
            goto L_0x009f
        L_0x009d:
            r0 = move-exception
            goto L_0x00a5
        L_0x009f:
            if (r5 == 0) goto L_0x0050
            r5.close()     // Catch:{ IOException -> 0x009d }
            goto L_0x0050
        L_0x00a5:
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            goto L_0x003e
        L_0x00ab:
            boolean r0 = android.text.TextUtils.isEmpty(r3)
            if (r0 == 0) goto L_0x00b3
            java.lang.String r3 = "none"
        L_0x00b3:
            return r3
        L_0x00b4:
            if (r4 == 0) goto L_0x00bc
            r4.close()     // Catch:{ IOException -> 0x00ba }
            goto L_0x00bc
        L_0x00ba:
            r6 = move-exception
            goto L_0x00c2
        L_0x00bc:
            if (r5 == 0) goto L_0x00d9
            r5.close()     // Catch:{ IOException -> 0x00ba }
            goto L_0x00d9
        L_0x00c2:
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            r7.append(r1)
            java.lang.String r1 = r6.getMessage()
            r7.append(r1)
            java.lang.String r1 = r7.toString()
            android.util.Log.e(r2, r1)
            goto L_0x00da
        L_0x00d9:
        L_0x00da:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.CscParser.getSalesCode():java.lang.String");
    }

    public static String getCustomerPath() {
        String omc_path = SystemProperties.get("persist.sys.omc_path", SystemInfo.UNKNOWN_INFO);
        if (!SystemInfo.UNKNOWN_INFO.equals(omc_path)) {
            File file = new File(omc_path + "/customer.xml");
            if (file.exists()) {
                if (file.canRead()) {
                    Log.i(TAG, "getCustomerPath : omc customer file can read");
                    return omc_path + "/customer.xml";
                }
                Log.e(TAG, "getCustomerPath : omc customer file exist but can't read");
                return CSC_XML_FILE;
            }
        }
        Log.e(TAG, "getCustomerPath : /system/csc/customer.xml file exist");
        return CSC_XML_FILE;
    }

    public static String getOthersPath() {
        String omc_path = SystemProperties.get("persist.sys.omc_path", SystemInfo.UNKNOWN_INFO);
        if (!SystemInfo.UNKNOWN_INFO.equals(omc_path)) {
            File file = new File(omc_path + "/others.xml");
            if (file.exists()) {
                if (file.canRead()) {
                    Log.i(TAG, "getOthersPath : omc others file can read");
                    return omc_path + "/others.xml";
                }
                Log.e(TAG, "getOthersPath : omc others file exist but can't read");
                return CSC_OTHERS_FILE;
            }
        }
        Log.e(TAG, "getOthersPath : /system/csc/others.xml file exist");
        return CSC_OTHERS_FILE;
    }

    public static String getSWConfigPath() {
        File file = new File(OMC_ID_FILE);
        if (!file.exists()) {
            Log.e(TAG, "getSWConfigPath : customer SW_Configuration file exist");
            return CSC_ID_FILE;
        } else if (file.canRead()) {
            Log.i(TAG, "getSWConfigPath : omc SW_Configuration file can read");
            return OMC_ID_FILE;
        } else {
            Log.e(TAG, "getSWConfigPath : omc SW_Configuration file exist but can't read");
            return CSC_ID_FILE;
        }
    }

    public static String getOrgCustomerPath() {
        return "/system/csc/customer.xml";
    }

    public static String getOrgOthersPath() {
        return "/system/csc/others.xml";
    }

    public static String getChameleonPath() {
        return CSC_CHAMELEON_FILE;
    }

    public static String getIDPath() {
        return CSC_ID_FILE;
    }

    public static String getOmcIDPath() {
        return OMC_ID_FILE;
    }
}
