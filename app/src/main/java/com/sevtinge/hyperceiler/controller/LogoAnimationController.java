package com.sevtinge.hyperceiler.controller;

import android.content.Context;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.SettingsFeatures;
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils;

import fan.cardview.HyperCardView;

public class LogoAnimationController {

    private static final float MAX_SCROLL_FACTOR = 1.0f;
    private static final float MIN_SCROLL_FACTOR = 0.0f;
    private static final float SCALE_FACTOR = 0.1f;
    private static final float ALPHA_FACTOR = 0.1f;

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
        actionBarPadding = getDimensionPixelSize(R.dimen.logo_area_height);
        btnPadding = getDimensionPixelSize(R.dimen.update_btn_margin_bottom);
        startY = getDimensionPixelSize(R.dimen.screen_effect_actionbar_height);
        logoHeight = getDimensionPixelSize(R.dimen.logo_height);
        isNeedUpdate = needUpdate;
        logoPadding = calculateLogoPadding(context, needUpdate);
    }

    private int getDimensionPixelSize(int dimenResId) {
        return mContext.getResources().getDimensionPixelSize(dimenResId);
    }

    private int calculateLogoPadding(Context context, boolean needUpdate) {
        int basePadding = getDimensionPixelSize(R.dimen.logo_bottom) - btnPadding;
        if (needUpdate) {
            return basePadding;
        } else if (SettingsFeatures.isSplitTabletDevice()) {
            return basePadding - DisplayUtils.dp2px(context, 27.0f);
        } else {
            return basePadding - DisplayUtils.dp2px(context, 30.0f);
        }
    }

    public void setActionBarAlpha(View view) {
        view.setAlpha(0.0f);
    }

    public void startAnimation(int scrollY, View iconLogoView, View textLogoView, View iconLogoViewShade, View textLogoViewShade, HyperCardView updateTextView, View versionLayout, View bgEffectView) {
        float scroll = calculateScrollFactor(scrollY, actionBarPadding);
        float scale = 1.0f - scroll * SCALE_FACTOR;

        resetViewsAlphaAndScale(iconLogoView, textLogoView, iconLogoViewShade, textLogoViewShade);

        applyAnimation(scroll, iconLogoView, textLogoView, iconLogoViewShade, textLogoViewShade, updateTextView, versionLayout, bgEffectView, scale, scrollY);
    }

    private void resetViewsAlphaAndScale(View... views) {
        for (View view : views) {
            view.setAlpha(1.0f);
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
        }
    }

    private void applyAnimation(float scroll, View iconLogoView, View textLogoView, View iconLogoViewShade, View textLogoViewShade, HyperCardView updateTextView, View versionLayout, View bgEffectView, float scale, int scrollY) {
        if (scrollY >= logoPadding) {
            float scroll2 = calculateScrollFactor(scrollY - logoPadding, logoHeight);
            float scale2 = 1.0f - SCALE_FACTOR * scroll2;
            setViewAlphaAndScale(iconLogoView, 1.0f - scroll2, scale2);
            setViewAlphaAndScale(textLogoView, 1.0f - scroll2, scale2);
            setViewAlphaAndScale(iconLogoViewShade, 1.0f - scroll2, scale2);
            setViewAlphaAndScale(textLogoViewShade, 1.0f - scroll2, scale2);
        } else {
            resetViewsAlphaAndScale(iconLogoViewShade, textLogoViewShade);
        }

        versionLayout.setAlpha(1.0f - scroll * (actionBarPadding / (float) logoPadding));
        versionLayout.setScaleX(scale);
        versionLayout.setScaleY(scale);
        setPivotXY(versionLayout);

        bgEffectView.setAlpha(1.0f - scroll);
        updateTextView.setAlpha(isNeedUpdate ? 1.0f - scroll * (actionBarPadding / (float) btnPadding) : 0.0f);
        updateTextView.setScaleX(scale);
        updateTextView.setScaleY(scale);
        updateTextView.setClickable(updateTextView.getAlpha() > 0.0f);
    }

    private void setViewAlphaAndScale(View view, float alpha, float scale) {
        view.setAlpha(alpha);
        view.setScaleX(scale);
        view.setScaleY(scale);
        setPivotXY(view);
    }

    private void setPivotXY(View view) {
        view.setPivotX((float) (view.getMeasuredWidth() / 2));
        view.setPivotY((float) (view.getMeasuredHeight() / 2));
    }

    private float calculateScrollFactor(int scrollY, int padding) {
        return Math.min(MAX_SCROLL_FACTOR, Math.max(MIN_SCROLL_FACTOR, Math.abs(scrollY) / (float) padding));
    }

    public void startButtonAnimation(int scrollValue, HyperCardView updateTextView) {
        float scroll = calculateScrollFactor(scrollValue, actionBarPadding);
        float scale = 1.0f - SCALE_FACTOR * scroll;

        if (scrollValue == 0) {
            updateTextView.setAlpha(isNeedUpdate ? 1.0f : 0.0f);
            updateTextView.setScaleX(1.0f);
            updateTextView.setScaleY(1.0f);
            updateTextView.setClickable(isNeedUpdate);
        } else {
            updateTextView.setAlpha(isNeedUpdate ? 1.0f - scroll * (actionBarPadding / (float) btnPadding) : 0.0f);
            updateTextView.setScaleX(scale);
            updateTextView.setScaleY(scale);
            updateTextView.setClickable(updateTextView.getAlpha() > 0.0f);
        }
    }
}
