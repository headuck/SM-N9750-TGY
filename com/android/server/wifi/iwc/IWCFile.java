package com.android.server.wifi.iwc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

class IWCFile {
    protected File mFile;
    protected String mFilePath = "temp";

    IWCFile(String filePath) {
        this.mFilePath = filePath;
        this.mFile = new File(this.mFilePath);
        checkDirectoryAndCreateFile();
    }

    public BufferedReader getBufferedReader() throws IOException {
        return Files.newBufferedReader(Paths.get(this.mFilePath, new String[0]), Charset.forName("UTF-8"));
    }

    public String readFile() throws IOException {
        return new String(Files.readAllBytes(Paths.get(this.mFilePath, new String[0])));
    }

    /* access modifiers changed from: package-private */
    public byte[] readData() throws IOException {
        return Files.readAllBytes(Paths.get(this.mFilePath, new String[0]));
    }

    public long getSize() {
        return this.mFile.length();
    }

    public boolean isFileExists() {
        return this.mFile.exists();
    }

    private boolean checkDirectoryAndCreateFile() {
        if (this.mFile.exists()) {
            return false;
        }
        try {
            File pf = this.mFile.getParentFile();
            if (pf != null) {
                pf.mkdirs();
            }
            this.mFile.createNewFile();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e2) {
            e2.printStackTrace();
            return false;
        }
    }

    public void writeDataAppend(String data) {
        checkDirectoryAndCreateFile();
        try {
            byte[] byteArray = data.getBytes(Charset.forName("UTF-8"));
            Files.write(Paths.get(this.mFilePath, new String[0]), byteArray, new OpenOption[]{StandardOpenOption.APPEND});
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e2) {
            e2.printStackTrace();
        } catch (SecurityException e3) {
            e3.printStackTrace();
        }
    }

    public void writeData(String data) {
        checkDirectoryAndCreateFile();
        try {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(this.mFilePath, new String[0]), Charset.forName("UTF-8"), new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING});
            writer.write(data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e2) {
            e2.printStackTrace();
        } catch (SecurityException e3) {
            e3.printStackTrace();
        }
    }

    /* access modifiers changed from: package-private */
    public void writeData(byte[] data) throws IOException {
        checkDirectoryAndCreateFile();
        Files.write(Paths.get(this.mFilePath, new String[0]), data, new OpenOption[0]);
    }
}
