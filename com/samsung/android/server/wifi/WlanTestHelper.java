package com.samsung.android.server.wifi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class WlanTestHelper {
    public static final int TEST_MODE_WIFI_WARNING_DIALOG = 1;

    public static String getConfigFileString() {
        File file = new File("/data/misc/wifi/WifiConfigStore.xml");
        if (!file.exists()) {
            return null;
        }
        BufferedReader br = null;
        try {
            BufferedReader br2 = new BufferedReader(new FileReader(file));
            StringBuffer sb = new StringBuffer();
            while (true) {
                String readLine = br2.readLine();
                String line = readLine;
                if (readLine == null) {
                    break;
                }
                sb.append(line);
                sb.append("\n");
            }
            String stringBuffer = sb.toString();
            try {
                br2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return stringBuffer;
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
            if (br != null) {
                br.close();
            }
            return null;
        } catch (IOException e3) {
            e3.printStackTrace();
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }
            return null;
        } catch (Throwable th) {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e5) {
                    e5.printStackTrace();
                }
            }
            throw th;
        }
    }
}
