package com.android.server.wifi.hotspot2.anqp;

import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class I18Name {
    @VisibleForTesting
    public static final int LANGUAGE_CODE_LENGTH = 3;
    @VisibleForTesting
    public static final int MINIMUM_LENGTH = 3;
    private final String mLanguage;
    private final Locale mLocale;
    private final String mText;

    @VisibleForTesting
    public I18Name(String language, Locale locale, String text) {
        this.mLanguage = language;
        this.mLocale = locale;
        this.mText = text;
    }

    public static I18Name parse(ByteBuffer payload) throws ProtocolException {
        int length = payload.get() & 255;
        if (length >= 3) {
            String language = ByteBufferReader.readString(payload, 3, StandardCharsets.US_ASCII).trim();
            try {
                return new I18Name(language, new Locale.Builder().setLanguage(language).build(), ByteBufferReader.readString(payload, length - 3, StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new ProtocolException("Invalid language: " + language);
            }
        } else {
            throw new ProtocolException("Invalid length: " + length);
        }
    }

    public String getLanguage() {
        return this.mLanguage;
    }

    public Locale getLocale() {
        return this.mLocale;
    }

    public String getText() {
        return this.mText;
    }

    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof I18Name)) {
            return false;
        }
        I18Name that = (I18Name) thatObject;
        if (!TextUtils.equals(this.mLanguage, that.mLanguage) || !TextUtils.equals(this.mText, that.mText)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return (this.mLanguage.hashCode() * 31) + this.mText.hashCode();
    }

    public String toString() {
        return this.mText + ':' + this.mLocale.getLanguage();
    }
}
