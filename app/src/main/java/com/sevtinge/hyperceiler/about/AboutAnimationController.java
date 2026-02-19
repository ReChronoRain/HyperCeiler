package com.sevtinge.hyperceiler.about;

import android.content.Context;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.SettingsFeatures;

import fan.cardview.HyperCardView;
import fan.core.utils.DisplayUtils;

public class AboutAnimationController {
    private int startY = 0;
    private int logoPadding = 0;
    private int btnPadding = 0;
    private int actionBarPadding = 0;
    private int logoHeight = 0;
    private boolean isNeedUpdate = false;
    private Context context = null;

    public static float getUpdateButtonMaxAlpha() {
        return 0.99f;
    }

    public AboutAnimationController(Context context, boolean z) {
        iniData(context, z);
    }

    public void iniData(Context context, boolean z) {
        this.context = context;
        this.actionBarPadding = context.getResources().getDimensionPixelSize(R.dimen.app_logo_area_height);
        this.btnPadding = context.getResources().getDimensionPixelSize(R.dimen.app_update_btn_margin_bottom);
        this.startY = context.getResources().getDimensionPixelSize(R.dimen.screen_effect_actionbar_height);
        this.logoHeight = context.getResources().getDimensionPixelSize(R.dimen.app_logo_height);
        this.isNeedUpdate = z;
        if (z) {
            logoPadding = context.getResources().getDimensionPixelSize(R.dimen.app_logo_bottom) - btnPadding;
        } else if (SettingsFeatures.isSplitTabletDevice()) {
            logoPadding = (context.getResources().getDimensionPixelSize(R.dimen.app_logo_bottom) - btnPadding) - DisplayUtils.dip2px(context, 27.0f);
        } else {
            logoPadding = (context.getResources().getDimensionPixelSize(R.dimen.app_logo_bottom) - btnPadding) - DisplayUtils.dip2px(context, 30.0f);
        }
    }

    public void setActionBarAlpha(View view) {
        view.setAlpha(0.0f);
    }

    public void startAnimation(int i, View view, HyperCardView hyperCardView, View view2, View view3, View view4) {
        float min = Math.min(1.0f, Math.max(0.0f, (Math.abs(i) * 1.0f) / this.actionBarPadding));
        float f = 1.0f - (min * 0.1f);
        if (i == 0) {
            view.setAlpha(1.0f);
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
            view2.setAlpha(1.0f);
            view2.setScaleX(1.0f);
            view2.setScaleY(1.0f);
            view4.setAlpha(1.0f);
            if (this.isNeedUpdate) {
                hyperCardView.setAlpha(getUpdateButtonMaxAlpha());
                hyperCardView.setScaleX(1.0f);
                hyperCardView.setScaleY(1.0f);
                hyperCardView.setClickable(true);
            } else {
                hyperCardView.setClickable(false);
            }
            view3.setAlpha(0.0f);
            return;
        }
        if (i >= this.logoPadding) {
            float min2 = Math.min(1.0f, Math.max(0.0f, (Math.abs(i - logoPadding) * 1.0f) / this.logoHeight));
            float f2 = 1.0f - (0.1f * min2);
            view.setAlpha(1.0f - min2);
            view.setScaleX(f2);
            view.setScaleY(f2);
            view.setPivotX(view.getMeasuredWidth() / 2);
            view.setPivotY(view.getMeasuredHeight() / 2);
            if (min2 == 1.0f) {
                view3.setAlpha(min2);
            } else {
                view3.setAlpha(0.0f);
            }
        } else {
            view.setAlpha(1.0f);
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
            view3.setAlpha(0.0f);
        }
        view2.setAlpha(1.0f - (((this.actionBarPadding * 1.0f) / this.logoPadding) * min));
        view2.setScaleX(f);
        view2.setScaleY(f);
        view2.setPivotX(view2.getMeasuredWidth() / 2);
        view2.setPivotY(view2.getMeasuredHeight() / 2);
        view4.setAlpha(1.0f - min);
        if (this.isNeedUpdate) {
            float f3 = 1.0f - (min * ((this.actionBarPadding * 1.0f) / this.btnPadding));
            if (f3 > 0.99f) {
                f3 = getUpdateButtonMaxAlpha();
            }
            hyperCardView.setAlpha(f3);
            hyperCardView.setScaleX(f);
            hyperCardView.setScaleY(f);
            if (hyperCardView.getAlpha() > 0.0f) {
                hyperCardView.setClickable(true);
                return;
            } else {
                hyperCardView.setClickable(false);
                return;
            }
        }
        hyperCardView.setClickable(false);
    }

    public void startButtonAnimation(int i, HyperCardView hyperCardView) {
        float min = Math.min(1.0f, Math.max(0.0f, (Math.abs(i) * 1.0f) / this.actionBarPadding));
        float f = 1.0f - (0.1f * min);
        if (i == 0) {
            if (this.isNeedUpdate) {
                hyperCardView.setAlpha(getUpdateButtonMaxAlpha());
                hyperCardView.setScaleX(1.0f);
                hyperCardView.setScaleY(1.0f);
                hyperCardView.setClickable(true);
                return;
            }
            hyperCardView.setClickable(false);
            return;
        }
        if (this.isNeedUpdate) {
            float f2 = 1.0f - (min * ((this.actionBarPadding * 1.0f) / this.btnPadding));
            if (f2 > 0.99f) {
                f2 = getUpdateButtonMaxAlpha();
            }
            hyperCardView.setAlpha(f2);
            hyperCardView.setScaleX(f);
            hyperCardView.setScaleY(f);
            if (hyperCardView.getAlpha() > 0.0f) {
                hyperCardView.setClickable(true);
                return;
            } else {
                hyperCardView.setClickable(false);
                return;
            }
        }
        hyperCardView.setClickable(false);
    }
}

