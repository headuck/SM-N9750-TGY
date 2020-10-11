package com.android.server.wifi.p2p.common;

public class DefaultImageRequest {
    public static DefaultImageRequest EMPTY_CIRCULAR_DEFAULT_IMAGE_REQUEST = new DefaultImageRequest((String) null, (String) null, true);
    public static DefaultImageRequest EMPTY_DEFAULT_IMAGE_REQUEST = new DefaultImageRequest();
    public static final float OFFSET_DEFAULT = 0.0f;
    public static final float SCALE_DEFAULT = 1.0f;
    public String displayName;
    public String identifier;
    public boolean isAvailableNumber;
    public boolean isCircular;
    public int letterSize;
    public float offset;
    public float scale;
    public int spamLevel;

    public DefaultImageRequest() {
        this.scale = 1.0f;
        this.offset = OFFSET_DEFAULT;
        this.isCircular = false;
        this.letterSize = 0;
        this.isAvailableNumber = false;
        this.spamLevel = 0;
    }

    public DefaultImageRequest(String displayName2, String identifier2, boolean isCircular2) {
        this(displayName2, identifier2, 1.0f, OFFSET_DEFAULT, isCircular2);
    }

    public DefaultImageRequest(String displayName2, String identifier2, float scale2, float offset2, boolean isCircular2) {
        this.scale = 1.0f;
        this.offset = OFFSET_DEFAULT;
        this.isCircular = false;
        this.letterSize = 0;
        this.isAvailableNumber = false;
        this.spamLevel = 0;
        this.displayName = displayName2;
        this.identifier = identifier2;
        this.scale = scale2;
        this.offset = offset2;
        this.isCircular = isCircular2;
    }
}
