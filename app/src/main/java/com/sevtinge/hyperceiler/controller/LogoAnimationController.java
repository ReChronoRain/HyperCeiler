package com.sevtinge.hyperceiler.controller;

import android.content.Context;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.SettingsFeatures;
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils;

import fan.cardview.HyperCardView;

public class LogoAnimationController {

    private int startY = 0;
    private int logoHeight = 0;
    private int logoPadding = 0;
    private int btnPadding = 0;
    private int actionBarPadding = 0;
    private boolean isNeedUpdate = false;
    private Context mContext = null;

    public LogoAnimationController(Context context, boolean needUpdate) {
        iniData(context, needUpdate);
    }

    public void iniData(Context context, boolean needUpdate) {
        mContext = context;
        actionBarPadding = context.getResources().getDimensionPixelSize(R.dimen.logo_area_height);
        btnPadding = context.getResources().getDimensionPixelSize(R.dimen.update_btn_margin_bottom);
        startY = context.getResources().getDimensionPixelSize(R.dimen.screen_effect_actionbar_height);
        logoHeight = context.getResources().getDimensionPixelSize(R.dimen.logo_height);
        isNeedUpdate = needUpdate;
        if (needUpdate) {
            logoPadding = context.getResources().getDimensionPixelSize(R.dimen.logo_bottom) - btnPadding;
        } else if (SettingsFeatures.isSplitTabletDevice()) {
            logoPadding = (context.getResources().getDimensionPixelSize(R.dimen.logo_bottom) - btnPadding) - DisplayUtils.dp2px(context, 27.0f);
        } else {
            logoPadding = (context.getResources().getDimensionPixelSize(R.dimen.logo_bottom) - btnPadding) - DisplayUtils.dp2px(context, 30.0f);
        }
    }

    public void setActionBarAlpha(View view) {
        view.setAlpha(0.0f);
    }

    public void startAnimation(int scrollY, View iconLogoView, View textLogoView, View iconLogoViewShade, View textLogoViewShade, HyperCardView updateTextView, View versionLayout, View bgEffectView) {
        float scroll = Math.min(1.0f, Math.max(0.0f, Math.abs(scrollY) * 1.0f / actionBarPadding));
        float scale = 1.0f - scroll * 0.1f;
        if (scrollY == 0) {
            iconLogoView.setAlpha(1.0f);
            iconLogoView.setScaleX(1.0f);
            iconLogoView.setScaleY(1.0f);

            textLogoView.setAlpha(1.0f);
            textLogoView.setScaleX(1.0f);
            textLogoView.setScaleY(1.0f);

            iconLogoViewShade.setAlpha(1.0f);
            iconLogoViewShade.setScaleX(1.0f);
            iconLogoViewShade.setScaleY(1.0f);

            textLogoViewShade.setAlpha(1.0f);
            textLogoViewShade.setScaleX(1.0f);
            textLogoViewShade.setScaleY(1.0f);

            versionLayout.setAlpha(1.0f);
            versionLayout.setScaleX(1.0f);
            versionLayout.setScaleY(1.0f);
            bgEffectView.setAlpha(1.0f);
            if (isNeedUpdate) {
                updateTextView.setAlpha(1.0f);
                updateTextView.setScaleX(1.0f);
                updateTextView.setScaleY(1.0f);
                updateTextView.setClickable(true);
            } else {
                updateTextView.setClickable(false);
            }
        } else {
            if (scrollY >= logoPadding) {
                float scroll2 = Math.min(1.0f, Math.max(0.0f, Math.abs(scrollY - logoPadding) * 1.0f / this.logoHeight));
                float scale2 = 1.0f - 0.1f * scroll2;
                iconLogoView.setAlpha(1.0f - scroll2);
                iconLogoView.setScaleX(scale2);
                iconLogoView.setScaleY(scale2);
                iconLogoView.setPivotX((float) (iconLogoView.getMeasuredWidth() / 2));
                iconLogoView.setPivotY((float) (iconLogoView.getMeasuredHeight() / 2));

                textLogoView.setAlpha(1.0f - scroll2);
                textLogoView.setScaleX(scale2);
                textLogoView.setScaleY(scale2);
                textLogoView.setPivotX((float) (textLogoView.getMeasuredWidth() / 2));
                textLogoView.setPivotY((float) (textLogoView.getMeasuredHeight() / 2));

                iconLogoViewShade.setAlpha(1.0f - scroll2);
                iconLogoViewShade.setScaleX(scale2);
                iconLogoViewShade.setScaleY(scale2);
                iconLogoViewShade.setPivotX((float) (iconLogoViewShade.getMeasuredWidth() / 2));
                iconLogoViewShade.setPivotY((float) (iconLogoViewShade.getMeasuredHeight() / 2));

                textLogoViewShade.setAlpha(1.0f - scroll2);
                textLogoViewShade.setScaleX(scale2);
                textLogoViewShade.setScaleY(scale2);
                textLogoViewShade.setPivotX((float) (textLogoViewShade.getMeasuredWidth() / 2));
                textLogoViewShade.setPivotY((float) (textLogoViewShade.getMeasuredHeight() / 2));
            } else {
                iconLogoViewShade.setAlpha(1.0f);
                iconLogoViewShade.setScaleX(1.0f);
                iconLogoViewShade.setScaleY(1.0f);

                textLogoViewShade.setAlpha(1.0f);
                textLogoViewShade.setScaleX(1.0f);
                textLogoViewShade.setScaleY(1.0f);
            }
            versionLayout.setAlpha(1.0f - actionBarPadding * 1.0f / logoPadding * scroll);
            versionLayout.setScaleX(scale);
            versionLayout.setScaleY(scale);
            versionLayout.setPivotX(((float) (versionLayout.getMeasuredWidth()) / 2));
            versionLayout.setPivotY(((float) (versionLayout.getMeasuredHeight()) / 2));
            bgEffectView.setAlpha(1.0f - scroll);
            if (isNeedUpdate) {
                updateTextView.setAlpha(1.0f - scroll * (actionBarPadding * 1.0f / btnPadding));
                updateTextView.setScaleX(scale);
                updateTextView.setScaleY(scale);
                updateTextView.setClickable(updateTextView.getAlpha() > 0.0f);
            } else {
                updateTextView.setClickable(false);
            }
        }
    }

    public void startButtonAnimation(int scrollValue, HyperCardView updateTextView) {
        float scroll = Math.min(1.0f, Math.max(0.0f, (Math.abs(scrollValue) * 1.0f) / actionBarPadding));
        float scale = 1.0f - (0.1f * scroll);
        if (scrollValue == 0) {
            if (isNeedUpdate) {
                updateTextView.setAlpha(1.0f);
                updateTextView.setScaleX(1.0f);
                updateTextView.setScaleY(1.0f);
                updateTextView.setClickable(true);
            } else {
                updateTextView.setClickable(false);
            }
        } else {
            if (isNeedUpdate) {
                updateTextView.setAlpha(1.0f - (scroll * ((actionBarPadding * 1.0f) / btnPadding)));
                updateTextView.setScaleX(scale);
                updateTextView.setScaleY(scale);
                updateTextView.setClickable(updateTextView.getAlpha() > 0.0f);
            } else {
                updateTextView.setClickable(false);
            }
        }
    }

}
