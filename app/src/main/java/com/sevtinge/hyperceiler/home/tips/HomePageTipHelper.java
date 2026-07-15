package com.sevtinge.hyperceiler.home.tips;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.AppLanguageHelper;

import java.lang.ref.WeakReference;

public class HomePageTipHelper {

    private static final Object TIP_REQUEST_LOCK = new Object();
    private static String mCurrentTip = "";
    /** 产生 mCurrentTip 时使用的应用语言，用于在切语言后令缓存失效。 */
    private static String mCurrentTipLang = "";
    private static boolean sIsLoadingTip;
    private static WeakReference<TextView> sCurrentTipViewRef = new WeakReference<>(null);

    public static View getTipView(Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.settings_tip_main_layout, null, false);
        TextView tipView = v.findViewById(android.R.id.title);

        if (tipView != null) {
            sCurrentTipViewRef = new WeakReference<>(tipView);
            String currentLang = AppLanguageHelper.getLanguage(context);
            boolean langChanged = !TextUtils.equals(currentLang, mCurrentTipLang);
            if (!TextUtils.isEmpty(mCurrentTip) && !langChanged) {
                tipView.setText("Tip: " + mCurrentTip);
            } else {
                // 语言变了或者还没拉过，重新拉一条。
                tipView.setText("Tip: Loading...");
                updateTipTextWithView(context, tipView);
            }
            tipView.setOnClickListener(view -> updateTipTextWithView(context, tipView));
        }
        return v;
    }

    public static void refreshCurrentTip(Context context) {
        TextView target = sCurrentTipViewRef.get();
        if (target != null) {
            updateTipTextWithView(context, target);
        }
    }

    public static void updateTipTextWithView(Context context, final TextView targetView) {
        if (context == null || targetView == null) return;

        synchronized (TIP_REQUEST_LOCK) {
            if (sIsLoadingTip) {
                return;
            }
            sIsLoadingTip = true;
        }

        final String requestLang = AppLanguageHelper.getLanguage(context);
        HomePageTipManager.getRandomTipAsync(context.getApplicationContext(), tip -> {
            synchronized (TIP_REQUEST_LOCK) {
                sIsLoadingTip = false;
            }
            mCurrentTip = tip;
            mCurrentTipLang = requestLang;
            String tipText = "Tip: " + mCurrentTip;
            targetView.setText(tipText);

            TextView currentTipView = sCurrentTipViewRef.get();
            if (currentTipView != null && currentTipView != targetView) {
                currentTipView.setText(tipText);
            }
        });
    }
}
