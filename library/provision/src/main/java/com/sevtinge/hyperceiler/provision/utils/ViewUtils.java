package com.sevtinge.hyperceiler.provision.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.view.Window;


public class ViewUtils {
    private static final String TAG = "ViewUtils";

    public interface RoundedBitmapCallback {
        void onBitmapReady(Bitmap bitmap);
    }

    public static void captureRoundedBitmap(Activity activity, View view, Handler handler, final RoundedBitmapCallback roundedBitmapCallback) {
        try {
            int[] iArr = new int[2];
            view.getLocationInWindow(iArr);
            int width = view.getWidth();
            int height = view.getHeight();
            if (width != 0 && height != 0) {
                final Bitmap bitmapCreateBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Window window = activity.getWindow();
                int i = iArr[0];
                int i2 = iArr[1];
                PixelCopy.request(window, new Rect(i, i2, width + i, height + i2), bitmapCreateBitmap, new PixelCopy.OnPixelCopyFinishedListener() {
                    @Override
                    public final void onPixelCopyFinished(int i3) {
                        ViewUtils.$r8$lambda$NnYv_JfPqrX4fW6_6EFBfXItcPU(bitmapCreateBitmap, roundedBitmapCallback, i3);
                    }
                }, handler);
                return;
            }
            Log.d(TAG, "width  " + width + " height " + height);
            roundedBitmapCallback.onBitmapReady(null);
        } catch (IllegalArgumentException unused) {
            roundedBitmapCallback.onBitmapReady(null);
        }
    }

    public static void $r8$lambda$NnYv_JfPqrX4fW6_6EFBfXItcPU(Bitmap bitmap, RoundedBitmapCallback roundedBitmapCallback, int i) {
        Log.d(TAG, "PixelCopy request " + i);
        if (i == 0) {
            roundedBitmapCallback.onBitmapReady(cropBitmapToCircle(bitmap));
        } else {
            roundedBitmapCallback.onBitmapReady(null);
        }
    }

    private static Bitmap cropBitmapToCircle(Bitmap bitmap) {
        int iMin = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iMin, iMin, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint(1);
        Rect rect = new Rect(0, 0, iMin, iMin);
        canvas.drawARGB(0, 0, 0, 0);
        float f = iMin / 2.0f;
        canvas.drawCircle(f, f, f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, (Rect) null, rect, paint);
        return bitmapCreateBitmap;
    }

    public static Bitmap rotateBitmap180(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.setScale(-1.0f, 1.0f);
        matrix.postTranslate(bitmap.getWidth(), 0.0f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
