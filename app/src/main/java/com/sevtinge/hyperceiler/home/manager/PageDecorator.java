package com.sevtinge.hyperceiler.home.manager;

import static com.sevtinge.hyperceiler.common.utils.PersistConfig.isNeedGrayView;

import android.app.Activity;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;

import com.sevtinge.hyperceiler.holiday.HolidayHelper;

/**
 * 页面装饰器：负责 Activity 的视觉效果增强（灰度滤镜、节日动效等）
 */
public class PageDecorator {

    /**
     * 为 Activity 装载所有视觉特效
     */
    public static void decorate(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        // 1. 应用灰度滤镜（如遇特殊纪念日）
        applyGrayScale(activity, isNeedGrayView);

        // 2. 注入节日特效视图（如春节落花、雪花等）
        // HolidayHelper 内部会处理 R.layout.layout_holiday 的注入
        HolidayHelper.init(activity);
    }

    /**
     * 底层滤镜实现
     */
    private static void applyGrayScale(Activity activity, boolean enabled) {
        if (enabled) {
            View decorView = activity.getWindow().getDecorView();
            Paint paint = new Paint();
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0f);
            paint.setColorFilter(new ColorMatrixColorFilter(cm));
            // 开启硬件加速层以确保 HolidayHelper 的动画流畅
            decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        }
    }

    /**
     * 响应生命周期：暂停动画（减小功耗）
     */
    public static void onPause() {
        HolidayHelper.pauseAnimation();
    }

    /**
     * 响应生命周期：恢复动画
     */
    public static void onResume() {
        HolidayHelper.resumeAnimation();
    }
}
