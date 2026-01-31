package fan.transition;

import android.app.Activity;
import android.app.ActivityOptions;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;

import fan.reflect.ReflectionHelper;

public class ActivityOptionsHelper {

    private static final String TAG = "ActivityOptionsHelper";

    public static final int ANIM_LAUNCH_ACTIVITY_FROM_ROUNDED_VIEW = 102;
    public static final int ANIM_LAUNCH_ACTIVITY_WITH_SCALED_THUMB = 103;


    public static boolean isSupportScaleUpDown(int i) {
        if (i == ANIM_LAUNCH_ACTIVITY_FROM_ROUNDED_VIEW &&
                ScaleUpAnimationHolder.SUPPORT_FROM_ROUND_VIEW) {
            return true;
        }
        return i == ANIM_LAUNCH_ACTIVITY_WITH_SCALED_THUMB &&
                ScaleUpAnimationHolder.SUPPORT_SCALED_THUMB;
    }

    public static boolean isSupportScaleUpDown() {
        return ScaleUpAnimationHolder.SUPPORT_FROM_ROUND_VIEW ||
                ScaleUpAnimationHolder.SUPPORT_SCALED_THUMB;
    }

    public static boolean isSupportUpdateScaleUpDownData() {
        return ScaleUpAnimationHolder.SUPPORT_UPDATE_DATA;
    }

    public static ActivityOptions makeScaleUpAnim(View view, Rect rect, int i, int i2, int i3) {
        if (!isSupportScaleUpDown()) {
            return null;
        }
        int i4 = (i3 == ANIM_LAUNCH_ACTIVITY_FROM_ROUNDED_VIEW || i3 == ANIM_LAUNCH_ACTIVITY_WITH_SCALED_THUMB) ? i3 : ANIM_LAUNCH_ACTIVITY_FROM_ROUNDED_VIEW;
        return makeScaleUpAnim(view, rect, i, i2, null, null, null, null, i4);
    }

    @Deprecated
    public static ActivityOptions makeScaleUpAnimationFromRoundedView(View view, Rect rect, int i, int i2) {
        if (!isSupportScaleUpDown()) {
            return null;
        }
        return makeScaleUpAnim(view, rect, i, i2, null, null, null, null, ANIM_LAUNCH_ACTIVITY_FROM_ROUNDED_VIEW);
    }

    private static ActivityOptions makeScaleUpAnim(View view, Rect rect, int i, int i2, Runnable runnable, Runnable runnable2, Runnable runnable3, Runnable runnable4, int i3) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(captureSnapshot(view), rect.left, rect.top, rect.width(), rect.height());
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        return makeScaleUpAnim(view, bitmapCreateBitmap, iArr[0] + rect.left, iArr[1] + rect.top, i, i2, view.getScaleX(), new Handler(), runnable, runnable2, runnable3, runnable4, i3);
    }

    @Deprecated
    public static ActivityOptions makeScaleUpAnimationFromRoundedView(View view, Bitmap bitmap, int i, int i2, int i3, int i4, float f, Handler handler, Runnable runnable, Runnable runnable2, Runnable runnable3, Runnable runnable4) {
        return makeScaleUpAnim(view, bitmap, i, i2, i3, i4, f, handler, runnable, runnable2, runnable3, runnable4, 102);
    }

    public static ActivityOptions makeScaleUpAnim(View view, Bitmap bitmap, int i, int i2, int i3, int i4, float f, Handler handler, Runnable runnable, Runnable runnable2, Runnable runnable3, Runnable runnable4, int i5) {
        if (!isSupportScaleUpDown()) {
            return null;
        }
        if (ScaleUpAnimationHolder.SUPPORT_SCALED_THUMB) {
            return ActivityOptionsCompat.makeScaleUpDown(view, bitmap, i, i2, i3, i4, f, handler, runnable, runnable2, runnable3, runnable4, i5);
        }
        return ActivityOptionsCompat.makeScaleUpAnimationFromRoundedView(view, bitmap, i, i2, i3, i4, f, handler, runnable, runnable2, runnable3, runnable4);
    }

    public static boolean updateScaleUpDownData(Activity activity, Bundle bundle) {
        if (!isSupportUpdateScaleUpDownData()) {
            return false;
        }
        try {
            ReflectionHelper.invoke(Activity.class, activity, "updateScaleUpDownData", new Class[]{Bundle.class}, bundle);
            return true;
        } catch (Exception unused) {
            return false;
        }
    }

    public static Bitmap captureSnapshot(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        long j = width * height * 4;
        long scaled = ViewConfiguration.get(view.getContext()).getScaledMaximumDrawingCacheSize();
        if (width > 0 && height > 0 && j <= scaled) {
            Canvas canvas;
            try {
                Bitmap bitmap = Bitmap.createBitmap(view.getResources().getDisplayMetrics(), width, height, Bitmap.Config.ARGB_8888);
                bitmap.setDensity(view.getResources().getDisplayMetrics().densityDpi);
                canvas = new Canvas(bitmap);
                view.computeScroll();
                int save = canvas.save();
                canvas.translate((float) -view.getScrollX(), -view.getScaleY());
                view.draw(canvas);
                canvas.restoreToCount(save);
                canvas.setBitmap(null);
                return bitmap;
            } catch (OutOfMemoryError unused2) {
                Log.d(TAG, "too large to create a bitmap!");
                return null;
            }
        }
        if (width > 0 && height > 0) Log.d(TAG, "too large to create a bitmap!");
        return null;
    }

    public static boolean isSupportMiuiClipAnimation() {
        return ClipAnimationHolder.SUPPORT_MIUI_CLIP_ANIMATION;
    }

    public static boolean isSupportMiuiRoundAnimation() {
        return RoundAnimationHolder.SUPPORT_MIUI_ROUND_ANIMATION;
    }

    public static ActivityOptions makeMiuiClipAnimation(Rect rect, Rect rect2, float f, float f2, int i, float f3, boolean z) {
        if (isSupportMiuiClipAnimation()) {
            return ActivityOptionsCompat.makeMiuiClipAnimation(rect, rect2, f, f2, i, f3, z);
        }
        return null;
    }

    public static ActivityOptions makeMiuiRoundAnimation(float f, float f2, int i, float f3) {
        if (isSupportMiuiRoundAnimation()) {
            return ActivityOptionsCompat.makeMiuiRoundAnimation(f, f2, i, f3);
        }
        return null;
    }


    private static class ScaleUpAnimationHolder {
        private static final boolean SUPPORT_FROM_ROUND_VIEW = isSupportFromRoundedView();
        private static final boolean SUPPORT_SCALED_THUMB = isSupportScaledThumb();
        private static final boolean SUPPORT_UPDATE_DATA = isSupportUpdateData();


        @Deprecated
        private static boolean isSupportFromRoundedView() {
            try {
                ActivityOptions.class.getMethod(
                        "makeScaleUpAnimationFromRoundedView", View.class, Bitmap.class,
                        Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Float.TYPE,
                        Handler.class, Runnable.class, Runnable.class, Runnable.class, Runnable.class
                ).setAccessible(true);
                return true;
            } catch (NoSuchMethodException e) {
                Log.d(TAG, e.toString());
                return false;
            }
        }

        private static boolean isSupportScaledThumb() {
            try {
                ActivityOptions.class.getMethod(
                        "makeScaleUpDown", View.class, Bitmap.class,
                        Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE,
                        Float.TYPE, Handler.class, Runnable.class, Runnable.class,
                        Runnable.class, Runnable.class, Integer.TYPE
                ).setAccessible(true);
                return true;
            } catch (NoSuchMethodException e) {
                Log.d(TAG, e.toString());
                return false;
            }
        }

        private static boolean isSupportUpdateData() {
            try {
                Activity.class.getDeclaredMethod(
                        "updateScaleUpDownData",
                        Bundle.class
                ).setAccessible(true);
                return true;
            } catch (NoSuchMethodException e) {
                Log.d(TAG, e.toString());
                return false;
            }
        }

    }


    private static class ClipAnimationHolder {
        private static final boolean SUPPORT_MIUI_CLIP_ANIMATION = isSupportMiuiClipAnimation();

        private static boolean isSupportMiuiClipAnimation() throws SecurityException {
            try {
                ActivityOptions.class.getMethod("makeMiuiClipAnimation", Rect.class, Rect.class,
                        Float.TYPE, Float.TYPE, Integer.TYPE, Float.TYPE, Boolean.TYPE);
                return true;
            } catch (NoSuchMethodException e) {
                Log.d(TAG, e.toString());
                return false;
            }
        }
    }

    private static class RoundAnimationHolder {

        private static final boolean SUPPORT_MIUI_ROUND_ANIMATION = isSupportMiuiRoundAnimation();

        private static boolean isSupportMiuiRoundAnimation() throws SecurityException {
            try {
                ActivityOptions.class.getMethod("makeMiuiRoundAnimation",
                        Float.TYPE, Float.TYPE, Integer.TYPE, Float.TYPE);
                return true;
            } catch (NoSuchMethodException e) {
                Log.d(TAG, e.toString());
                return false;
            }
        }
    }

}
