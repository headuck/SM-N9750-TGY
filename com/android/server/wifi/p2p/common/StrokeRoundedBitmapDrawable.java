package com.android.server.wifi.p2p.common;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import com.samsung.android.server.wifi.SemWifiConstants;

public class StrokeRoundedBitmapDrawable extends Drawable {
    private static final int DEFAULT_PAINT_FLAGS = 6;
    protected static int sStrokeWidth;
    private boolean mApplyGravity = true;
    protected boolean mApplyStroke = true;
    Bitmap mBitmap;
    private int mBitmapHeight;
    private BitmapShader mBitmapShader;
    private int mBitmapWidth;
    protected float mCornerRadius;
    final Rect mDstRect = new Rect();
    final RectF mDstRectF = new RectF();
    private int mGravity = 119;
    protected Paint mPaint = new Paint(6);
    private int mStrokeColor;
    protected Paint mStrokePaint;
    private int mTargetDensity = SemWifiConstants.ROUTER_OUI_TYPE;

    public final Paint getPaint() {
        return this.mPaint;
    }

    public final Bitmap getBitmap() {
        return this.mBitmap;
    }

    private void computeBitmapSize() {
        this.mBitmapWidth = this.mBitmap.getScaledWidth(this.mTargetDensity);
        this.mBitmapHeight = this.mBitmap.getScaledHeight(this.mTargetDensity);
    }

    public void setTargetDensity(Canvas canvas) {
        setTargetDensity(canvas.getDensity());
    }

    public void setTargetDensity(DisplayMetrics metrics) {
        setTargetDensity(metrics.densityDpi);
    }

    public void setTargetDensity(int density) {
        if (this.mTargetDensity != density) {
            this.mTargetDensity = density == 0 ? SemWifiConstants.ROUTER_OUI_TYPE : density;
            if (this.mBitmap != null) {
                computeBitmapSize();
            }
            invalidateSelf();
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    public void setGravity(int gravity) {
        if (this.mGravity != gravity) {
            this.mGravity = gravity;
            this.mApplyGravity = true;
            invalidateSelf();
        }
    }

    public void setMipMap(boolean mipMap) {
        throw new UnsupportedOperationException();
    }

    public boolean hasMipMap() {
        throw new UnsupportedOperationException();
    }

    public void setAntiAlias(boolean aa) {
        this.mPaint.setAntiAlias(aa);
        invalidateSelf();
    }

    public boolean hasAntiAlias() {
        return this.mPaint.isAntiAlias();
    }

    public void setFilterBitmap(boolean filter) {
        this.mPaint.setFilterBitmap(filter);
        invalidateSelf();
    }

    public void setDither(boolean dither) {
        this.mPaint.setDither(dither);
        invalidateSelf();
    }

    /* access modifiers changed from: package-private */
    public void gravityCompatApply(int gravity, int bitmapWidth, int bitmapHeight, Rect bounds, Rect outRect) {
        Gravity.apply(gravity, bitmapWidth, bitmapHeight, bounds, outRect, 0);
    }

    /* access modifiers changed from: package-private */
    public void updateDstRect() {
        if (this.mApplyGravity) {
            gravityCompatApply(this.mGravity, this.mBitmapWidth, this.mBitmapHeight, getBounds(), this.mDstRect);
            this.mDstRectF.set(this.mDstRect);
            this.mApplyGravity = false;
        }
    }

    public void draw(Canvas canvas) {
        Bitmap bitmap = this.mBitmap;
        if (bitmap != null) {
            updateDstRect();
            Paint paint = this.mPaint;
            if (paint.getShader() == null) {
                canvas.drawBitmap(bitmap, (Rect) null, this.mDstRect, paint);
                if (this.mApplyStroke) {
                    canvas.drawRect(this.mDstRect, this.mStrokePaint);
                    return;
                }
                return;
            }
            RectF strokeRect = new RectF(this.mDstRectF.left + ((float) sStrokeWidth), this.mDstRectF.top + ((float) sStrokeWidth), this.mDstRectF.right - ((float) sStrokeWidth), this.mDstRectF.bottom - ((float) sStrokeWidth));
            float f = this.mCornerRadius;
            canvas.drawRoundRect(strokeRect, f, f, paint);
            if (this.mApplyStroke) {
                float f2 = this.mCornerRadius;
                canvas.drawRoundRect(strokeRect, f2, f2, this.mStrokePaint);
            }
        }
    }

    public void setAlpha(int alpha) {
        if (alpha != this.mPaint.getAlpha()) {
            this.mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    public int getAlpha() {
        return this.mPaint.getAlpha();
    }

    public void setColorFilter(ColorFilter cf) {
        this.mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    public ColorFilter getColorFilter() {
        return this.mPaint.getColorFilter();
    }

    public void setCornerRadius(float cornerRadius) {
        if (isGreaterThanZero(cornerRadius)) {
            this.mPaint.setShader(this.mBitmapShader);
        } else {
            this.mPaint.setShader((Shader) null);
        }
        this.mCornerRadius = cornerRadius;
    }

    public float getCornerRadius() {
        return this.mCornerRadius;
    }

    public int getIntrinsicWidth() {
        return this.mBitmapWidth;
    }

    public int getIntrinsicHeight() {
        return this.mBitmapHeight;
    }

    public int getOpacity() {
        Bitmap bm;
        if (this.mGravity == 119 && (bm = this.mBitmap) != null && !bm.hasAlpha() && this.mPaint.getAlpha() >= 255 && !isGreaterThanZero(this.mCornerRadius)) {
            return -1;
        }
        return -3;
    }

    public StrokeRoundedBitmapDrawable(Resources res, Bitmap bitmap) {
        if (res != null) {
            this.mTargetDensity = res.getDisplayMetrics().densityDpi;
            this.mStrokeColor = res.getColor(17171463);
        }
        this.mBitmap = bitmap;
        if (this.mBitmap != null) {
            computeBitmapSize();
            this.mBitmapShader = new BitmapShader(this.mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        } else {
            this.mBitmapHeight = -1;
            this.mBitmapWidth = -1;
        }
        this.mStrokePaint = new Paint();
        this.mStrokePaint.setColor(this.mStrokeColor);
        this.mStrokePaint.setStyle(Paint.Style.STROKE);
        if (res != null) {
            sStrokeWidth = 0;
        }
        this.mStrokePaint.setStrokeWidth((float) sStrokeWidth);
        this.mStrokePaint.setAntiAlias(true);
    }

    private static boolean isGreaterThanZero(float toCompare) {
        return Float.compare(toCompare, DefaultImageRequest.OFFSET_DEFAULT) > 0;
    }

    public void setStroke(boolean enable) {
        this.mApplyStroke = enable;
    }
}
