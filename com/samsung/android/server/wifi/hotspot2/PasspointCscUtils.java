package com.samsung.android.server.wifi.hotspot2;

import android.util.Log;
import com.android.server.wifi.util.XmlUtil;
import com.samsung.android.server.wifi.CscParser;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PasspointCscUtils {
    private static final String TAG = "PasspointCscUtils";
    private static PasspointCscUtils instance;
    private static File mPasspointCredentialFile = new File("/data/misc/wifi/cred.conf");
    private CscParser mParser = null;

    /* renamed from: sb */
    StringBuffer f53sb = new StringBuffer();

    private void PasspointCscUtils() {
    }

    public static PasspointCscUtils getInstance() {
        if (instance == null) {
            instance = new PasspointCscUtils();
        }
        return instance;
    }

    public boolean parsingCsc() {
        Node FRIENDLY_NAME;
        String XML_TAG_FQDN = "FullyQualifiedDomainName";
        String XML_TAG_FRIENDLY_NAME = "FriendlyName";
        String XML_TAG_ROAMING_CONSORTIUM_OIS = "RoamingConsortium";
        String XML_TAG_REALM = XmlUtil.WifiEnterpriseConfigXmlUtil.XML_TAG_REALM;
        String XML_TAG_EAP_TYPE = "Eap";
        String XML_TAG_USERNAME = "UserName";
        String XML_TAG_PASSWORD = XmlUtil.WifiEnterpriseConfigXmlUtil.XML_TAG_PASSWORD;
        String XML_TAG_NON_EAP_INNER_METHOD = "InnerMethod";
        String XML_TAG_IMSI = "Imsi";
        String XML_TAG_CA_CERTIFICATE_KEY = XmlUtil.WifiEnterpriseConfigXmlUtil.XML_TAG_CA_CERT;
        String XML_TAG_CLIENT_PRIVATE_KEY = "ClientKey";
        String XML_TAG_CLIENT_PRIVATE_KEY_PASSWORD = "ClientKeyPassword";
        String XML_TAG_PRIORITY = "Priority";
        StringBuilder sb = new StringBuilder();
        String XML_TAG_CLIENT_CERTIFICATE = XmlUtil.WifiEnterpriseConfigXmlUtil.XML_TAG_CLIENT_CERT;
        sb.append("parsingCsc, getCustomerPath: ");
        sb.append(CscParser.getCustomerPath());
        logd(sb.toString());
        this.mParser = new CscParser(CscParser.getCustomerPath());
        Node WifiHS20ProfileNode = this.mParser.search("Settings.Wifi");
        Object obj = "Settings.Wifi";
        NodeList passpointProfileNodeList = this.mParser.searchList(WifiHS20ProfileNode, "Hs20Profile");
        Object obj2 = "Hs20Profile";
        if (passpointProfileNodeList == null) {
            loge("parsingCsc, passpointProfileNodeList is null.");
            return false;
        }
        logd("parsingCsc, parsing WifiHS20Profile from customer.xml.");
        int passpointVendorApNumber = passpointProfileNodeList.getLength();
        StringBuilder sb2 = new StringBuilder();
        Node node = WifiHS20ProfileNode;
        sb2.append("parsingCsc, passpointVendorApNumber: ");
        sb2.append(passpointVendorApNumber);
        logd(sb2.toString());
        String[] FullyQualifiedDomainName = new String[passpointVendorApNumber];
        String[] FriendlyName = new String[passpointVendorApNumber];
        String[] RoamingConsortiumOis = new String[passpointVendorApNumber];
        String[] Realm = new String[passpointVendorApNumber];
        String[] Eap = new String[passpointVendorApNumber];
        String[] UserName = new String[passpointVendorApNumber];
        String[] Password = new String[passpointVendorApNumber];
        String[] NonEAPInnerMethodes = new String[passpointVendorApNumber];
        String[] Imsi = new String[passpointVendorApNumber];
        String[] Priority = new String[passpointVendorApNumber];
        String[] CaCertificateKey = new String[passpointVendorApNumber];
        String[] ClientCertificate = new String[passpointVendorApNumber];
        String[] ClientPrivateKey = new String[passpointVendorApNumber];
        String[] ClientKeyPassword = new String[passpointVendorApNumber];
        if (passpointVendorApNumber == 0) {
            logd("parsingCsc, passpointVendorApNumber is 0.");
            return false;
        }
        int i = 0;
        int passpointCredendtialProfileCnt = 0;
        while (i < passpointVendorApNumber) {
            int passpointVendorApNumber2 = passpointVendorApNumber;
            Node passpointProfileNodeListChild = passpointProfileNodeList.item(i);
            NodeList passpointProfileNodeList2 = passpointProfileNodeList;
            Node FQDN = this.mParser.search(passpointProfileNodeListChild, XML_TAG_FQDN);
            String XML_TAG_FQDN2 = XML_TAG_FQDN;
            Node FRIENDLY_NAME2 = this.mParser.search(passpointProfileNodeListChild, XML_TAG_FRIENDLY_NAME);
            String XML_TAG_FRIENDLY_NAME2 = XML_TAG_FRIENDLY_NAME;
            Node ROAMING_CONSORTIUM_OIS = this.mParser.search(passpointProfileNodeListChild, XML_TAG_ROAMING_CONSORTIUM_OIS);
            String XML_TAG_ROAMING_CONSORTIUM_OIS2 = XML_TAG_ROAMING_CONSORTIUM_OIS;
            Node REALM = this.mParser.search(passpointProfileNodeListChild, XML_TAG_REALM);
            String XML_TAG_REALM2 = XML_TAG_REALM;
            Node EAP_TYPE = this.mParser.search(passpointProfileNodeListChild, XML_TAG_EAP_TYPE);
            String XML_TAG_EAP_TYPE2 = XML_TAG_EAP_TYPE;
            Node USERNAME = this.mParser.search(passpointProfileNodeListChild, XML_TAG_USERNAME);
            String XML_TAG_USERNAME2 = XML_TAG_USERNAME;
            Node PASSWORD = this.mParser.search(passpointProfileNodeListChild, XML_TAG_PASSWORD);
            String XML_TAG_PASSWORD2 = XML_TAG_PASSWORD;
            Node NON_EAP_INNER_METHOD = this.mParser.search(passpointProfileNodeListChild, XML_TAG_NON_EAP_INNER_METHOD);
            String XML_TAG_NON_EAP_INNER_METHOD2 = XML_TAG_NON_EAP_INNER_METHOD;
            Node IMSI = this.mParser.search(passpointProfileNodeListChild, XML_TAG_IMSI);
            String XML_TAG_IMSI2 = XML_TAG_IMSI;
            Node PRIORITY = this.mParser.search(passpointProfileNodeListChild, XML_TAG_PRIORITY);
            String XML_TAG_PRIORITY2 = XML_TAG_PRIORITY;
            Node CA_CERTIFICATE_KEY = this.mParser.search(passpointProfileNodeListChild, XML_TAG_CA_CERTIFICATE_KEY);
            String XML_TAG_CA_CERTIFICATE_KEY2 = XML_TAG_CA_CERTIFICATE_KEY;
            String[] ClientKeyPassword2 = ClientKeyPassword;
            Node CLIENT_CERTIFICATE = this.mParser.search(passpointProfileNodeListChild, XML_TAG_CLIENT_CERTIFICATE);
            Node CLIENT_PRIVATE_KEY = this.mParser.search(passpointProfileNodeListChild, XML_TAG_CLIENT_PRIVATE_KEY);
            String XML_TAG_CLIENT_PRIVATE_KEY_PASSWORD2 = XML_TAG_CLIENT_PRIVATE_KEY_PASSWORD;
            Node CLIENT_PRIVATE_KEY_PASSWORD = this.mParser.search(passpointProfileNodeListChild, XML_TAG_CLIENT_PRIVATE_KEY_PASSWORD2);
            if (FQDN != null) {
                Node node2 = passpointProfileNodeListChild;
                FullyQualifiedDomainName[i] = this.mParser.getValue(FQDN);
            }
            if (FRIENDLY_NAME2 != null) {
                FriendlyName[i] = this.mParser.getValue(FRIENDLY_NAME2);
            }
            if (ROAMING_CONSORTIUM_OIS != null) {
                RoamingConsortiumOis[i] = this.mParser.getValue(ROAMING_CONSORTIUM_OIS);
            }
            if (REALM != null) {
                Realm[i] = this.mParser.getValue(REALM);
            }
            if (EAP_TYPE != null) {
                Eap[i] = this.mParser.getValue(EAP_TYPE);
            }
            if (USERNAME != null) {
                UserName[i] = this.mParser.getValue(USERNAME);
            }
            if (PASSWORD != null) {
                Password[i] = this.mParser.getValue(PASSWORD);
            }
            if (NON_EAP_INNER_METHOD != null) {
                NonEAPInnerMethodes[i] = this.mParser.getValue(NON_EAP_INNER_METHOD);
            }
            if (IMSI != null) {
                Imsi[i] = this.mParser.getValue(IMSI);
            }
            if (PRIORITY != null) {
                Priority[i] = this.mParser.getValue(PRIORITY);
            }
            if (CA_CERTIFICATE_KEY != null) {
                CaCertificateKey[i] = this.mParser.getValue(CA_CERTIFICATE_KEY);
            }
            if (CLIENT_CERTIFICATE != null) {
                Node node3 = FRIENDLY_NAME2;
                FRIENDLY_NAME = CLIENT_CERTIFICATE;
                ClientCertificate[i] = this.mParser.getValue(FRIENDLY_NAME);
            } else {
                FRIENDLY_NAME = CLIENT_CERTIFICATE;
            }
            if (CLIENT_PRIVATE_KEY != null) {
                Node node4 = FRIENDLY_NAME;
                ClientPrivateKey[i] = this.mParser.getValue(CLIENT_PRIVATE_KEY);
            } else {
                Node CLIENT_CERTIFICATE2 = FRIENDLY_NAME;
                Node CLIENT_CERTIFICATE3 = CLIENT_PRIVATE_KEY;
            }
            if (CLIENT_PRIVATE_KEY_PASSWORD != null) {
                ClientKeyPassword2[i] = this.mParser.getValue(CLIENT_PRIVATE_KEY_PASSWORD);
            }
            passpointCredendtialProfileCnt++;
            i++;
            XML_TAG_CLIENT_PRIVATE_KEY_PASSWORD = XML_TAG_CLIENT_PRIVATE_KEY_PASSWORD2;
            passpointVendorApNumber = passpointVendorApNumber2;
            passpointProfileNodeList = passpointProfileNodeList2;
            XML_TAG_FQDN = XML_TAG_FQDN2;
            XML_TAG_FRIENDLY_NAME = XML_TAG_FRIENDLY_NAME2;
            XML_TAG_ROAMING_CONSORTIUM_OIS = XML_TAG_ROAMING_CONSORTIUM_OIS2;
            XML_TAG_REALM = XML_TAG_REALM2;
            XML_TAG_EAP_TYPE = XML_TAG_EAP_TYPE2;
            XML_TAG_USERNAME = XML_TAG_USERNAME2;
            XML_TAG_PASSWORD = XML_TAG_PASSWORD2;
            XML_TAG_NON_EAP_INNER_METHOD = XML_TAG_NON_EAP_INNER_METHOD2;
            XML_TAG_IMSI = XML_TAG_IMSI2;
            XML_TAG_PRIORITY = XML_TAG_PRIORITY2;
            XML_TAG_CA_CERTIFICATE_KEY = XML_TAG_CA_CERTIFICATE_KEY2;
            ClientKeyPassword = ClientKeyPassword2;
        }
        String str = XML_TAG_FQDN;
        String str2 = XML_TAG_FRIENDLY_NAME;
        String str3 = XML_TAG_ROAMING_CONSORTIUM_OIS;
        String str4 = XML_TAG_REALM;
        String str5 = XML_TAG_EAP_TYPE;
        String str6 = XML_TAG_USERNAME;
        String str7 = XML_TAG_PASSWORD;
        String str8 = XML_TAG_NON_EAP_INNER_METHOD;
        String str9 = XML_TAG_IMSI;
        String str10 = XML_TAG_CA_CERTIFICATE_KEY;
        String[] ClientKeyPassword3 = ClientKeyPassword;
        String str11 = XML_TAG_PRIORITY;
        NodeList nodeList = passpointProfileNodeList;
        String str12 = XML_TAG_CLIENT_PRIVATE_KEY_PASSWORD;
        int passpointCredendtialProfileCnt2 = passpointCredendtialProfileCnt;
        try {
            StringBuilder credsb = new StringBuilder();
            credsb.setLength(0);
            logd("parsingCsc, build string of the passpoint credential(count: " + passpointCredendtialProfileCnt2 + ")");
            if (passpointCredendtialProfileCnt2 == 0) {
                return false;
            }
            for (int j = 0; j < passpointCredendtialProfileCnt2; j++) {
                credsb.append("cred={\n");
                if (FullyQualifiedDomainName[j] != null) {
                    credsb.append("    domain=");
                    credsb.append("\"");
                    credsb.append(FullyQualifiedDomainName[j]);
                    credsb.append("\"");
                    credsb.append("\n");
                }
                if (FriendlyName[j] != null) {
                    credsb.append("    friendlyname=");
                    credsb.append("\"");
                    credsb.append(FriendlyName[j]);
                    credsb.append("\"");
                    credsb.append("\n");
                }
                if (RoamingConsortiumOis[j] != null) {
                    credsb.append("    roaming_consortium=");
                    credsb.append(RoamingConsortiumOis[j]);
                    credsb.append("\n");
                }
                if (Realm[j] != null) {
                    credsb.append("    realm=");
                    credsb.append("\"");
                    credsb.append(Realm[j]);
                    credsb.append("\"");
                    credsb.append("\n");
                }
                if (CaCertificateKey[j] != null) {
                    credsb.append("    ca_cert=");
                    credsb.append("\"");
                    credsb.append(CaCertificateKey[j]);
                    credsb.append("\"");
                    credsb.append("\n");
                }
                if (ClientCertificate[j] != null) {
                    credsb.append("    client_cert=");
                    credsb.append("\"");
                    credsb.append(ClientCertificate[j]);
                    credsb.append("\"");
                    credsb.append("\n");
                }
                if (Eap[j] != null) {
                    credsb.append("    eap=");
                    credsb.append(Eap[j]);
                    credsb.append("\n");
                }
                if (UserName[j] != null) {
                    credsb.append("    username=");
                    credsb.append("\"");
                    credsb.append(UserName[j]);
                    credsb.append("\"");
                    credsb.append("\n");
                }
                if (Password[j] != null) {
                    credsb.append("    password=");
                    credsb.append("\"");
                    credsb.append(Password[j]);
                    credsb.append("\"");
                    credsb.append("\n");
                }
                if (NonEAPInnerMethodes[j] != null) {
                    credsb.append("    phase2=");
                    credsb.append("\"");
                    credsb.append(NonEAPInnerMethodes[j]);
                    credsb.append("\"");
                    credsb.append("\n");
                }
                if (Imsi[j] != null) {
                    credsb.append("    imsi=");
                    credsb.append("\"");
                    credsb.append(Imsi[j]);
                    credsb.append("\"");
                    credsb.append("\n");
                }
                if (Priority[j] != null) {
                    credsb.append("    priority=");
                    credsb.append(Priority[j]);
                    credsb.append("\n");
                }
                if (ClientPrivateKey[j] != null) {
                    credsb.append("    private_key=");
                    credsb.append("\"");
                    credsb.append(ClientPrivateKey[j]);
                    credsb.append("\"");
                    credsb.append("\n");
                }
                if (ClientKeyPassword3[j] != null) {
                    credsb.append("    private_key_password=");
                    credsb.append("\"");
                    credsb.append(ClientKeyPassword3[j]);
                    credsb.append("\"");
                    credsb.append("\n");
                }
                credsb.append("}\n");
            }
            logd("parsingCsc, credsb.toString(): " + credsb.toString());
            if (!createPasspointCredendtial(credsb.toString())) {
                loge("parsingCsc, createPasspointCredendtial is false.");
                return false;
            }
            this.f53sb.append(credsb.toString());
            return true;
        } catch (NullPointerException e) {
            loge("parsingCsc, NullPointerException");
            return false;
        }
    }

    private boolean createPasspointCredendtial(String passpointCredendtialProfile) {
        if (passpointCredendtialProfile == null) {
            loge("createPasspointCredendtial, passpointCredendtialProfile is null.");
            return false;
        } else if (passpointCredendtialProfile.length() == 0) {
            logi(", createPasspointCredendtial, There is no Profile in customer.xml.");
            return false;
        } else {
            FileOutputStream out = null;
            try {
                mPasspointCredentialFile.createNewFile();
                out = new FileOutputStream(mPasspointCredentialFile, true);
                out.write(passpointCredendtialProfile.getBytes());
                try {
                    out.close();
                } catch (IOException e2) {
                    loge(e2.toString());
                }
            } catch (FileNotFoundException e) {
                loge("createPasspointCredendtial, FileNotFoundException");
                if (out != null) {
                    out.close();
                }
            } catch (IOException e3) {
                e3.printStackTrace();
                if (out != null) {
                    out.close();
                }
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e22) {
                        loge(e22.toString());
                    }
                }
                throw th;
            }
            return true;
        }
    }

    /* access modifiers changed from: protected */
    public void loge(String s) {
        Log.e(TAG, s);
        this.f53sb.append(s);
    }

    /* access modifiers changed from: protected */
    public void logd(String s) {
        Log.d(TAG, s);
        this.f53sb.append(s);
    }

    /* access modifiers changed from: protected */
    public void logi(String s) {
        Log.i(TAG, s);
        this.f53sb.append(s);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("==== PasspointCscUtils Customer File Dump ====");
        pw.println(this.f53sb.toString());
    }
}
