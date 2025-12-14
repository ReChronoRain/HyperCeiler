/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.main.page.about.controller;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.main.page.about.view.BgEffectPainter;

import fan.appcompat.app.ActionBar;
import fan.internal.utils.ViewUtils;
import fan.os.Build;

public class BgEffectController implements Runnable {
    private float[] bound;
    BgEffectPainter mBgEffectPainter;
    private float mDeltaTime;
    private long mLastGlobalTime;
    private final View mTarget;
    private float mTime;
    private float mTimeDirection = 1.0f;

    public BgEffectController(View view) {
        this.mTarget = view;
    }

    public void start() {
        if (this.mBgEffectPainter == null) {
            this.mBgEffectPainter = new BgEffectPainter(this.mTarget.getContext());
            this.mLastGlobalTime = System.nanoTime();
            resetTime();
            this.mTarget.post(this);
        }
    }

    @Override
    public void run() {
        if (this.mBgEffectPainter != null) {
            tickPingPong();
            this.mBgEffectPainter.setResolution(this.mTarget.getWidth(), this.mTarget.getHeight());
            this.mBgEffectPainter.updateMaterials(this.mDeltaTime);
            this.mTarget.setRenderEffect(this.mBgEffectPainter.getRenderEffect());
            this.mTarget.postDelayed(this, 16L);
        }
    }

    private void tickPingPong() {
        long nanoTime = System.nanoTime();
        float f = (float) ((nanoTime - this.mLastGlobalTime) * 1.0E-9d);
        this.mDeltaTime = f;
        float f2 = this.mTime;
        float f3 = this.mTimeDirection;
        float f4 = f2 + (f * f3);
        this.mTime = f4;
        if (f3 > 0.0f) {
            if (f4 >= 7200.0f) {
                this.mTimeDirection = -1.0f;
            }
        } else if (f4 <= 0.0f) {
            this.mTimeDirection = 1.0f;
        }
        this.mLastGlobalTime = nanoTime;
    }

    public void resetTime() {
        this.mLastGlobalTime = System.nanoTime();
        this.mTime = 0.0f;
        this.mTimeDirection = 1.0f;
    }

    public void stop() {
        if (this.mBgEffectPainter != null) {
            this.mTarget.removeCallbacks(this);
            this.mBgEffectPainter.stop();
            this.mBgEffectPainter = null;
            this.mTarget.setRenderEffect(null);
        }
    }

    public void setType(Context context, View view, ActionBar actionBar) {
        resetTime();
        calcAnimationBound(context, view, actionBar);
        if (ViewUtils.isNightMode(context)) {
            if (Build.IS_TABLET) {
                this.mBgEffectPainter.setType(BgEffectDataManager.DeviceType.TABLET, BgEffectDataManager.ThemeMode.DARK, this.bound);
            } else {
                this.mBgEffectPainter.setType(BgEffectDataManager.DeviceType.PHONE, BgEffectDataManager.ThemeMode.DARK, this.bound);
            }
            return;
        }
        if (Build.IS_TABLET) {
            this.mBgEffectPainter.setType(BgEffectDataManager.DeviceType.TABLET, BgEffectDataManager.ThemeMode.LIGHT, this.bound);
        } else {
            this.mBgEffectPainter.setType(BgEffectDataManager.DeviceType.PHONE, BgEffectDataManager.ThemeMode.LIGHT, this.bound);
        }
    }

    private void calcAnimationBound(Context context, View view, ActionBar actionBar) {
        float height = (actionBar != null ? actionBar.getHeight() + 0.0f : 0.0f) + context.getResources().getDimensionPixelSize(R.dimen.logo_area_height);
        float height2 = height / ((ViewGroup) view.getParent()).getHeight();
        float width = ((ViewGroup) view.getParent()).getWidth();
        if (width <= height) {
            this.bound = new float[]{0.0f, 1.0f - height2, 1.0f, height2};
        } else {
            this.bound = new float[]{((width - height) / 2.0f) / width, 1.0f - height2, height / width, height2};
        }
    }
}
