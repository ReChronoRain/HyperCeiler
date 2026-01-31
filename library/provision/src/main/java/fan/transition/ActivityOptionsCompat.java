package fan.transition;

import android.app.ActivityOptions;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ActivityOptionsCompat {

    public static ActivityOptions makeScaleUpAnimationFromRoundedView(
            View view, Bitmap bitmap, int i, int i2, int i3, int i4, float f,
            Handler handler, Runnable runnable, Runnable runnable2, Runnable runnable3, Runnable runnable4) {
        try {
            Method makeScaleUpAnimationFromRoundedView = ActivityOptions.class.getDeclaredMethod(
                    "makeScaleUpAnimationFromRoundedView",
                    View.class,
                    Bitmap.class,
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    float.class,
                    Handler.class,
                    Runnable.class,
                    Runnable.class,
                    Runnable.class,
                    Runnable.class
            );
            makeScaleUpAnimationFromRoundedView.setAccessible(true);
            return (ActivityOptions) makeScaleUpAnimationFromRoundedView.invoke(ActivityOptions.class, view, bitmap, i, i2, i3, i4, f, handler, runnable, runnable2, runnable3, runnable4);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static ActivityOptions makeScaleUpDown(
            View view, Bitmap bitmap, int i, int i2, int i3, int i4, float f,
            Handler handler, Runnable runnable, Runnable runnable2, Runnable runnable3, Runnable runnable4, int i5) {
        try {
            Method makeScaleUpDown = ActivityOptions.class.getDeclaredMethod(
                    "makeScaleUpDown",
                    View.class,
                    Bitmap.class,
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    float.class,
                    Handler.class,
                    Runnable.class,
                    Runnable.class,
                    Runnable.class,
                    Runnable.class,
                    int.class
            );
            makeScaleUpDown.setAccessible(true);
            return (ActivityOptions) makeScaleUpDown.invoke(ActivityOptions.class, view, bitmap, i, i2, i3, i4, f, handler, runnable, runnable2, runnable3, runnable4, i5);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static ActivityOptions makeMiuiClipAnimation(Rect rect, Rect rect2, float f, float f2, int i, float f3, boolean z) {
        try {
            Method makeMiuiClipAnimation = ActivityOptions.class.getDeclaredMethod(
                    "makeMiuiClipAnimation",
                    Rect.class,
                    Rect.class,
                    float.class,
                    float.class,
                    int.class,
                    float.class,
                    boolean.class
            );
            makeMiuiClipAnimation.setAccessible(true);
            return (ActivityOptions) makeMiuiClipAnimation.invoke(ActivityOptions.class, null, rect, rect2, f, f2, i, f3, z);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static ActivityOptions makeMiuiRoundAnimation(float f, float f2, int i, float f3) {
        try {
            Method makeMiuiRoundAnimation = ActivityOptions.class.getDeclaredMethod(
                    "makeMiuiRoundAnimation",
                    float.class,
                    float.class,
                    int.class,
                    float.class
            );
            makeMiuiRoundAnimation.setAccessible(true);
            return (ActivityOptions) makeMiuiRoundAnimation.invoke(ActivityOptions.class, null, f, f2, i, f3);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}