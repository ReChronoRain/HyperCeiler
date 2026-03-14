package com.sevtinge.hyperceiler.home.tips;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;

import java.lang.ref.WeakReference;

public class HomePageTipHelper {

    private static String mCurrentTip = "";
    private static WeakReference<TextView> sCurrentTipViewRef = new WeakReference<>(null);

    public static View getTipView(Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.settings_tip_main_layout, null, false);
        TextView tipView = v.findViewById(android.R.id.title);

        if (tipView != null) {
            sCurrentTipViewRef = new WeakReference<>(tipView);
            if (!TextUtils.isEmpty(mCurrentTip)) {
                tipView.setText("Tip: " + mCurrentTip);
            } else {
                tipView.setText("Tip: Loading...");
            }
            updateTipTextWithView(context, tipView);
        }

        v.setOnClickListener(view -> {
            if (tipView != null) {
                updateTipTextWithView(context, tipView);
            }
        });
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

        HomePageTipManager.getRandomTipAsync(context.getApplicationContext(), tip -> {
            mCurrentTip = tip;
            targetView.setText("Tip: " + mCurrentTip);
        });
    }
}
