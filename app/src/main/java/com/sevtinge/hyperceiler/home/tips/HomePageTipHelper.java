package com.sevtinge.hyperceiler.home.tips;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;

import java.lang.ref.WeakReference;

import fan.animation.Folme;
import fan.animation.base.AnimConfig;

public class HomePageTipHelper {

    private static String mCurrentTip = "";

    public static View getTipView(Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.settings_tip_main_layout, null, false);
        TextView tipView = v.findViewById(android.R.id.title);

        if (tipView != null) {
            // 1. 如果已有缓存，先展示缓存
            if (!TextUtils.isEmpty(mCurrentTip)) {
                tipView.setText("Tip: " + mCurrentTip);
            } else {
                tipView.setText("Tip: Loading...");
            }

            // 2. 立即触发一次更新，并把当前的 tipView 传进去
            updateTipTextWithView(context, tipView);
        }

        // 点击事件：点击时也把当前的 tipView 传进去
        v.setOnClickListener(view -> {
            if (tipView != null) {
                updateTipTextWithView(context, tipView);
            }
        });

        //Folme.useAt(v).touch().setScale(0.95f).handleTouchOf(v, new AnimConfig[0]);
        return v;
    }

    /**
     * 核心修复：直接传入目标 TextView，确保异步回调能准确找到它
     */
    public static void updateTipTextWithView(Context context, final TextView targetView) {
        if (context == null || targetView == null) return;

        HomePageTipManager.getRandomTipAsync(context.getApplicationContext(), tip -> {
            mCurrentTip = tip; // 更新全局缓存
            // 这里的 targetView 是通过闭包捕获的，即便 masterContainer 重新创建了，
            // 只要这个 targetView 对象还在内存里，setText 就能生效
            targetView.setText("Tip: " + mCurrentTip);
        });
    }
}
