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
package com.sevtinge.hyperceiler.provision.utils;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import fan.animation.Folme;
import fan.animation.base.AnimSpecialConfig;
import fan.animation.controller.AnimState;
import fan.animation.property.ViewProperty;
import fan.animation.utils.EaseManager;

public class AnimHelper {

    public static final String TAG = "AnimHelper";

    public static int dp2px(Context context, float f) {
        return (int) ((f * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public static void startPageTextAnim(View view) {
        view.setTranslationY(dp2px(view.getContext(), 100.0f));
        view.setAlpha(0.0f);

        ObjectAnimator translationY = ObjectAnimator.ofFloat(view, "translationY", dp2px(view.getContext(), 100.0f), 0.0f);
        translationY.setInterpolator(EaseManager.getInterpolator(20, 1700.0f));
        translationY.setDuration(1700L);
        translationY.start();

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.0f, 1.0f);
        alpha.setInterpolator(EaseManager.getInterpolator(20, 1400.0f));
        alpha.setDuration(1400L);
        alpha.setStartDelay(300L);
        alpha.start();
    }

    public static void startPageBtnAnim(View target) {
        startPageBtnAnim(target, null);
    }

    public static void startPageBtnEnabledAnim(View target) {
        startPageBtnAnim(target, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                Log.d(TAG, "onAnimationEnd");
                if (target != null) {
                    target.setEnabled(true);
                }
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {

            }
        });
    }

    public static void startPageBtnAnim(View target, Animator.AnimatorListener listener) {
        if (target != null) {
            target.setScaleX(0.98f);
            target.setScaleY(0.98f);
            target.setAlpha(0.0f);

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(target, "scaleX", 0.98f, 1.0f);
            scaleX.setInterpolator(EaseManager.getInterpolator(20, new float[0]));
            scaleX.setDuration(600L);
            scaleX.setStartDelay(1200L);
            scaleX.start();

            ObjectAnimator scaleY = ObjectAnimator.ofFloat(target, "scaleY", 0.98f, 1.0f);
            scaleY.setInterpolator(EaseManager.getInterpolator(20, new float[0]));
            scaleY.setDuration(600L);
            scaleY.setStartDelay(1200L);
            scaleY.start();

            ObjectAnimator alpha = ObjectAnimator.ofFloat(target, "alpha", 0.0f, 1.0f);
            alpha.setInterpolator(EaseManager.getInterpolator(1, new float[0]));
            alpha.setDuration(500L);
            alpha.setStartDelay(1200L);
            if (listener != null) alpha.addListener(listener);
            alpha.start();
        }
    }

    public static void centerPageAnim(View view) {
        AnimState animState = new AnimState("start");
        ViewProperty viewProperty = ViewProperty.ALPHA;
        AnimState add = animState.add(viewProperty, 0.0d);
        ViewProperty viewProperty2 = ViewProperty.TRANSLATION_Y;
        AnimState add2 = add.add(viewProperty2, dp2px(view.getContext(), 30.0f));
        AnimState add3 = new AnimState("end").add(viewProperty, 1.0d).add(viewProperty2, 0.0d);
        AnimSpecialConfig animSpecialConfig = (AnimSpecialConfig) new AnimSpecialConfig().setEase(-2, 0.95f, 0.5f);
        AnimSpecialConfig animSpecialConfig2 = (AnimSpecialConfig) new AnimSpecialConfig().setEase(20, 500.0f);
        fan.animation.base.AnimConfig animConfig = new fan.animation.base.AnimConfig();
        animConfig.setSpecial(viewProperty, animSpecialConfig2);
        animConfig.setSpecial(viewProperty2, animSpecialConfig);
        Folme.useAt(view).state().fromTo(add2, add3, animConfig);
    }

    public static void endPageAnim(View view) {
        view.setTranslationY(dp2px(view.getContext(), 20.0f));
        view.setAlpha(0.0f);

        ObjectAnimator translationY = ObjectAnimator.ofFloat(view, "translationY", dp2px(view.getContext(), 20.0f), 0.0f);
        translationY.setInterpolator(EaseManager.getInterpolator(20, new float[0]));
        translationY.setDuration(1250L);
        translationY.start();

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.0f, 1.0f);
        alpha.setInterpolator(EaseManager.getInterpolator(20, new float[0]));
        alpha.setDuration(1050L);
        alpha.start();
    }

}
