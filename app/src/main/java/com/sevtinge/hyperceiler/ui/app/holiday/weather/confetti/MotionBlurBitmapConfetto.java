package com.sevtinge.hyperceiler.ui.app.holiday.weather.confetti;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.sevtinge.hyperceiler.ui.app.holiday.weather.PrecipType;
import com.sevtinge.hyperceiler.ui.app.holiday.weather.confetto.Confetto;
import com.sevtinge.hyperceiler.ui.app.holiday.weather.confetto.ConfettoInfo;

public final class MotionBlurBitmapConfetto extends Confetto {

    private final ConfettoInfo mConfettoInfo;
    public static final float SNOW_RADIUS = 7.5f;
    public static final float RAIN_STRETCH = 20f;

    public MotionBlurBitmapConfetto(ConfettoInfo info) {
        mConfettoInfo = info;
    }

    public ConfettoInfo getConfettoInfo() {
        return mConfettoInfo;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    protected void configurePaint(Paint paint) {
        super.configurePaint(paint);
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
    }

    @Override
    protected void drawInternal(Canvas canvas, Matrix matrix, Paint paint, float x, float y, float rotation, float percentAnimated) {
        if (mConfettoInfo.getPrecipType() == PrecipType.RAIN) {
            float rainStretch = RAIN_STRETCH * (mConfettoInfo.getScaleFactor() + 1.0f) / 2f;
            float dX = currentVelocityX;
            float dY = currentVelocityY;
            float x1 = x - dX * rainStretch;
            float y1 = y - dY * rainStretch;
            float x2 = x + dX * rainStretch;
            float y2 = y + dY * rainStretch;
            paint.setStrokeWidth(mConfettoInfo.getScaleFactor());
            paint.setShader(new LinearGradient(x1,
                    y1,
                    x2,
                    y2,
                    new int[]{Color.TRANSPARENT, Color.WHITE, Color.WHITE, Color.TRANSPARENT},
                    new float[]{0f, 0.45f, 0.55f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawLine(x1, y1, x2, y2, paint);
        } else if (mConfettoInfo.getPrecipType() == PrecipType.SNOW) {
            float sigmoid = (float) (1f / (1f + Math.pow(Math.E, -(mConfettoInfo.getScaleFactor() - 1f))));
            paint.setShader(new RadialGradient(x,
                    y,
                    SNOW_RADIUS * mConfettoInfo.getScaleFactor(),
                    new int[]{Color.WHITE, Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT},
                    new float[]{0f, (0.3f * sigmoid) + 0.15f, 0.95f - (0.35f * sigmoid), 1f},
                    Shader.TileMode.CLAMP
                    ));

            canvas.drawCircle(x, y, SNOW_RADIUS * mConfettoInfo.getScaleFactor(), paint);
        } else if (mConfettoInfo.getPrecipType() == PrecipType.CUSTOM) {
            matrix.preTranslate(x, y);
            matrix.preRotate(rotation,
                    mConfettoInfo.getCustomBitmap().getWidth() / 2f,
                    mConfettoInfo.getCustomBitmap().getHeight() / 2f);
            matrix.preScale(mConfettoInfo.getScaleFactor(), mConfettoInfo.getScaleFactor());
            canvas.drawBitmap(mConfettoInfo.getCustomBitmap(), matrix, paint);
        }
    }
}
