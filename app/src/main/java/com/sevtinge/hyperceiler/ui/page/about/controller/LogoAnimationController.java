/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.page.about.controller;

import android.content.Context;
import android.view.View;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.common.utils.SettingsFeatures;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils;

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
        actionBarPadding = getDimensionPixelSize(R.dimen.logo_area_height) - getDimensionPixelSize(fan.appcompat.R.dimen.miuix_appcompat_action_bar_default_height);
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
        int basePadding = getDimensionPixelSize(R.dimen.logo_bottom) - btnPadding - getDimensionPixelSize(R.dimen.logo_margin_top);
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

    public void startAnimation(int scrollY, View iconLogoView, View textLogoView, View iconLogoViewShade, View textLogoViewShade, HyperCardView updateTextView, View versionLayout, View bgEffectView, View titleView) {
        float scroll = calculateScrollFactor(scrollY, actionBarPadding);
        float scale = 1.0f - scroll * SCALE_FACTOR;

        resetViewsAlphaAndScale(iconLogoView, textLogoView, iconLogoViewShade, textLogoViewShade);

        applyAnimation(scroll, iconLogoView, textLogoView, iconLogoViewShade, textLogoViewShade, updateTextView, versionLayout, bgEffectView, titleView, scale, scrollY);
    }

    private void resetViewsAlphaAndScale(View... views) {
        for (View view : views) {
            view.setAlpha(1.0f);
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
        }
    }

    private void applyAnimation(float scroll, View iconLogoView, View textLogoView, View iconLogoViewShade, View textLogoViewShade, HyperCardView updateTextView, View versionLayout, View bgEffectView, View titleView, float scale, int scrollY) {
        if (scrollY >= logoPadding) {
            float scroll2 = calculateScrollFactor(scrollY - logoPadding, logoHeight);
            float scale2 = 1.0f - SCALE_FACTOR * scroll2;
            setViewAlphaAndScale(iconLogoView, 1.0f - scroll2, scale2);
            setViewAlphaAndScale(textLogoView, 1.0f - scroll2, scale2);
            setViewAlphaAndScale(iconLogoViewShade, 1.0f - scroll2, scale2);
            setViewAlphaAndScale(textLogoViewShade, 1.0f - scroll2, scale2);
            if (scroll2 == 1.0f) {
                titleView.setAlpha(scroll2);
            } else {
                titleView.setAlpha(0.0f);
            }
        } else {
            resetViewsAlphaAndScale(iconLogoViewShade, textLogoViewShade);
            titleView.setAlpha(0.0f);
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
