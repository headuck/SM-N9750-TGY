package com.android.server.wifi.p2p.common;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

public class LetterTileDrawable extends Drawable {
    private static Bitmap DEFAULT_AVATAR = null;
    private static Bitmap DEFAULT_AVATAR_1 = null;
    private static Bitmap DEFAULT_AVATAR_2 = null;
    private static Bitmap DEFAULT_AVATAR_3 = null;
    private static Bitmap SPAM_LEVEL_ICON_DANGER = null;
    private static Bitmap SPAM_LEVEL_ICON_SAFETY = null;
    private static Bitmap SPAM_LEVEL_ICON_WARNING = null;
    public static final int TYPE_DEFAULT = 1;
    public static final int TYPE_PERSON = 1;
    private static int sColors = 0;
    private static int sDangerColors = 0;
    private static int sDefaultColor;
    private static final char[] sFirstChar = new char[2];
    private static float sLetterToTileRatio;
    protected static final Paint sPaint = new Paint();
    protected static final Rect sRect = new Rect();
    protected static final RectF sRectF = new RectF();
    private static int sSafetyColors = 0;
    protected static int sStrokeColor;
    protected static final Paint sStrokePaint = new Paint();
    protected static int sStrokeWidth;
    private static int sTileFontColor;
    private static int sTileFontPadding = 0;
    private static int sWarningColors = 0;
    private int intrinsicHeight = -1;
    private int intrinsicWidth = -1;
    private boolean mAvailableNumber = false;
    private long mContactId = -1;
    private String mDisplayName;
    protected boolean mIsCircle = false;
    protected float mOffset = DefaultImageRequest.OFFSET_DEFAULT;
    protected final Paint mPaint;
    protected Resources mResources;
    protected float mScale = 1.0f;
    private int mSpamLevel;
    private int sTileFontSize = 0;

    public LetterTileDrawable(Resources res) {
        this.mResources = res;
        this.mPaint = new Paint();
        this.mPaint.setFilterBitmap(true);
        this.mPaint.setDither(true);
        if (sColors == 0) {
            sColors = res.getColor(17170759, (Resources.Theme) null);
            sDefaultColor = res.getColor(17170859, (Resources.Theme) null);
            sTileFontColor = res.getColor(17170443, (Resources.Theme) null);
            sLetterToTileRatio = res.getFraction(18022414, 1, 1);
            sTileFontPadding = res.getDimensionPixelSize(17105329);
            preloadDefaultPhotos(res);
            sPaint.setTypeface(getTypeface(res.getString(17040701), 0));
            sPaint.setTextAlign(Paint.Align.CENTER);
            sPaint.setAntiAlias(true);
            sSafetyColors = res.getColor(17171249, (Resources.Theme) null);
            sWarningColors = res.getColor(17171250, (Resources.Theme) null);
            sDangerColors = res.getColor(17171248, (Resources.Theme) null);
            SPAM_LEVEL_ICON_SAFETY = BitmapFactory.decodeResource(res, 17303185);
            SPAM_LEVEL_ICON_WARNING = BitmapFactory.decodeResource(res, 17303186);
            SPAM_LEVEL_ICON_DANGER = BitmapFactory.decodeResource(res, 17303187);
        }
        this.mPaint.setColor(sColors);
        sStrokeColor = res.getColor(17171463);
        sStrokePaint.setColor(sStrokeColor);
        sStrokePaint.setStyle(Paint.Style.STROKE);
        sStrokeWidth = 0;
        sStrokePaint.setStrokeWidth((float) sStrokeWidth);
        sStrokePaint.setAntiAlias(true);
    }

    private Typeface getTypeface(String fontName, int fontStyle) {
        return Typeface.create(fontName, fontStyle);
    }

    public void draw(Canvas canvas) {
        if (!getBounds().isEmpty()) {
            drawLetterTile(canvas);
        }
    }

    /* access modifiers changed from: protected */
    public void drawBitmap(Bitmap bitmap, int width, int height, Canvas canvas) {
        Rect destRect = copyBounds();
        int halfLength = (int) ((this.mScale * ((float) Math.min(destRect.width(), destRect.height()))) / 2.0f);
        destRect.set(destRect.centerX() - halfLength, (int) (((float) (destRect.centerY() - halfLength)) + (this.mOffset * ((float) destRect.height()))), destRect.centerX() + halfLength, (int) (((float) (destRect.centerY() + halfLength)) + (this.mOffset * ((float) destRect.height()))));
        sRect.set(0, 0, width, height);
        canvas.drawBitmap(bitmap, sRect, destRect, this.mPaint);
    }

    /* access modifiers changed from: protected */
    public void drawLetterTile(Canvas canvas) {
        Bitmap bitmap;
        int numberOfLetters;
        int i = this.mSpamLevel;
        if (i > 0) {
            sPaint.setColor(getSpamLevelColor(i));
        } else {
            sPaint.setColor(getColor());
        }
        sPaint.setAlpha(this.mPaint.getAlpha());
        Rect bounds = getBounds();
        RectF strokeRect = new RectF((float) (bounds.left + sStrokeWidth), (float) (bounds.top + sStrokeWidth), (float) (bounds.right - sStrokeWidth), (float) (bounds.bottom - sStrokeWidth));
        int minDimension = Math.min(bounds.width(), bounds.height());
        if (this.mIsCircle) {
            float cornerRadius = Util.getCornerRadius(this.mResources, bounds.height());
            sRectF.set(bounds);
            canvas.drawRoundRect(strokeRect, cornerRadius, cornerRadius, sPaint);
            canvas.drawRoundRect(strokeRect, cornerRadius, cornerRadius, sStrokePaint);
        } else {
            canvas.drawRect(bounds, sPaint);
            canvas.drawRect(bounds, sStrokePaint);
        }
        String str = this.mDisplayName;
        if (str == null || TextUtils.isEmpty(str) || (!isLetters(this.mDisplayName.charAt(0)) && !this.mAvailableNumber)) {
            int i2 = this.mSpamLevel;
            if (i2 > 0) {
                bitmap = getSpamLevelIcon(i2);
            } else {
                bitmap = getBitmapForDefaultAvatar();
            }
            drawBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), canvas);
            return;
        }
        sFirstChar[0] = Character.toUpperCase(this.mDisplayName.charAt(0));
        if (!this.mAvailableNumber || this.mDisplayName.length() <= 1) {
            numberOfLetters = 1;
        } else {
            sFirstChar[1] = Character.toUpperCase(this.mDisplayName.charAt(1));
            numberOfLetters = 2;
        }
        int numberOfLetters2 = this.sTileFontSize;
        if (numberOfLetters2 != 0) {
            sPaint.setTextSize((float) numberOfLetters2);
        } else {
            sPaint.setTextSize(this.mScale * sLetterToTileRatio * ((float) minDimension));
        }
        sPaint.getTextBounds(sFirstChar, 0, numberOfLetters, sRect);
        sPaint.setColor(sTileFontColor);
        canvas.drawText(sFirstChar, 0, numberOfLetters, (float) bounds.centerX(), ((((float) bounds.height()) / 2.0f) - ((sPaint.descent() + sPaint.ascent()) / 2.0f)) + ((float) sTileFontPadding), sPaint);
    }

    /* access modifiers changed from: protected */
    public Bitmap getBitmapForDefaultAvatar() {
        long j = this.mContactId;
        if (j < 1) {
            return DEFAULT_AVATAR;
        }
        int i = (int) (j % 4);
        if (i == 1) {
            return DEFAULT_AVATAR_1;
        }
        if (i == 2) {
            return DEFAULT_AVATAR_2;
        }
        if (i != 3) {
            return DEFAULT_AVATAR;
        }
        return DEFAULT_AVATAR_3;
    }

    public int getColor() {
        Resources resources;
        long j = this.mContactId;
        if (j == -1 || (resources = this.mResources) == null) {
            return sDefaultColor;
        }
        return resources.getColor(Util.getDefaultPhotoBackgroundColor(j), (Resources.Theme) null);
    }

    public void setAlpha(int alpha) {
        this.mPaint.setAlpha(alpha);
    }

    public void setColorFilter(ColorFilter cf) {
        this.mPaint.setColorFilter(cf);
    }

    public int getOpacity() {
        return -1;
    }

    public void setScale(float scale) {
        this.mScale = scale;
    }

    public void setOffset(float offset) {
        this.mOffset = offset;
    }

    public void setContactDetails(String displayName, String identifier) {
        this.mDisplayName = displayName;
    }

    public void setIsCircular(boolean isCircle) {
        this.mIsCircle = isCircle;
    }

    public void setContactId(long contactId) {
        this.mContactId = contactId;
    }

    public static boolean isLetters(char c) {
        return Character.isLetter(c);
    }

    public void setLetterSize(int size) {
        this.sTileFontSize = size;
    }

    public void setAvailableNumber(boolean availableNumber) {
        this.mAvailableNumber = availableNumber;
    }

    public void setSpamLevel(int level) {
        this.mSpamLevel = level;
    }

    private int getSpamLevelColor(int level) {
        if (level == 1) {
            return sSafetyColors;
        }
        if (level == 2) {
            return sWarningColors;
        }
        if (level != 3) {
            return getColor();
        }
        return sDangerColors;
    }

    private Bitmap getSpamLevelIcon(int level) {
        if (level == 1) {
            return SPAM_LEVEL_ICON_SAFETY;
        }
        if (level == 2) {
            return SPAM_LEVEL_ICON_WARNING;
        }
        if (level != 3) {
            return getBitmapForDefaultAvatar();
        }
        return SPAM_LEVEL_ICON_DANGER;
    }

    private void preloadDefaultPhotos(Resources res) {
        DEFAULT_AVATAR = BitmapFactory.decodeResource(res, 17302193);
        DEFAULT_AVATAR_1 = BitmapFactory.decodeResource(res, 17302193);
        DEFAULT_AVATAR_2 = BitmapFactory.decodeResource(res, 17302193);
        DEFAULT_AVATAR_3 = BitmapFactory.decodeResource(res, 17302193);
    }

    /* access modifiers changed from: protected */
    public void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        this.intrinsicWidth = bounds.width();
        this.intrinsicHeight = bounds.height();
    }

    public int getIntrinsicWidth() {
        int i = this.intrinsicWidth;
        return i > 0 ? i : super.getIntrinsicWidth();
    }

    public int getIntrinsicHeight() {
        int i = this.intrinsicHeight;
        return i > 0 ? i : super.getIntrinsicHeight();
    }
}
