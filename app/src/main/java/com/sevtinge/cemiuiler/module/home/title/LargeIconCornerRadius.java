package com.sevtinge.cemiuiler.module.home.title;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import moralnorm.internal.utils.DisplayUtils;

public class LargeIconCornerRadius extends BaseHook {

    Class<?> mBigIconUtil;

    @Override
    public void init() {

        mBigIconUtil = findClassIfExists("com.miui.home.launcher.bigicon.BigIconUtil");

        hookAllMethods(mBigIconUtil, "getCroppedFromCorner", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[3];
                Bitmap bitmap = (Bitmap) param.args[2];
                Drawable drawable = new BitmapDrawable(context.getResources(),
                    croppedCorners(bitmap,
                        DisplayUtils.dp2px(context,
                            mPrefsMap.getInt("home_large_icon_corner_radius", 32))));
                param.setResult(drawable);
            }
        });
    }


    public final Bitmap croppedCorners(Bitmap bitmap, float radius) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Paint mCornerPaint = new Paint();
            mCornerPaint.setAntiAlias(true);
            mCornerPaint.setColor(-16777216);
            Bitmap createBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(createBitmap);
            canvas.drawARGB(0, 0, 0, 0);
            mCornerPaint.setXfermode(null);
            RectF rectF = new RectF(0.0f, 0.0f, width, height);
            canvas.drawRoundRect(rectF, radius, radius, mCornerPaint);
            mCornerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            Rect rect = new Rect(0, 0, width, height);
            canvas.drawBitmap(bitmap, rect, rect, mCornerPaint);
            bitmap.recycle();
            return createBitmap;
        } catch (Exception unused) {
            return bitmap;
        }
    }
}
