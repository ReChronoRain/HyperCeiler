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
package com.sevtinge.hyperceiler.main.page.about.view;

import static com.sevtinge.hyperceiler.common.utils.PersistConfig.isLunarNewYearThemeView;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.main.page.about.controller.BgEffectDataManager;

import java.io.InputStream;
import java.util.Scanner;

import fan.animation.Folme;
import fan.animation.FolmeEase;
import fan.animation.IStateStyle;
import fan.animation.base.AnimConfig;
import fan.appcompat.app.ActionBar;
import fan.internal.utils.ViewUtils;

public class BgEffectPainter {


    AnimConfig mAnimConfig1;
    AnimConfig mAnimConfig2;
    BgEffectDataManager.BgEffectData mBgEffectData;
    BgEffectDataManager mBgEffectDataManager;
    RuntimeShader mBgRuntimeShader;
    Handler mHandler;
    IStateStyle mStateStyle;

    private float cycleCount = 0.0f;
    private float[] endColorValue;
    private float[] startColorValue;
    private float uAnimTime = 0.0f;
    private float[] uBgBound = {0.0f, 0.4489f, 1.0f, 0.5511f};
    private float[] uColors = {0.57f, 0.76f, 0.98f, 1.0f, 0.98f, 0.85f, 0.68f, 1.0f, 0.98f, 0.75f, 0.93f, 1.0f, 0.73f, 0.7f, 0.98f, 1.0f};
    private float prevT = 0.0f;
    private float colorInterpT = 0.0f;
    private float gradientSpeed = 1.0f;


    public BgEffectPainter(Context context) {
        String loadShader = loadShader(context.getResources(), R.raw.bg_frag);
        loadShader.getClass();
        mBgRuntimeShader = new RuntimeShader(loadShader);
        mHandler = new Handler(Looper.getMainLooper());
        mBgEffectDataManager = new BgEffectDataManager();
        mBgEffectData = mBgEffectDataManager.getData(BgEffectDataManager.DeviceType.PHONE, BgEffectDataManager.ThemeMode.LIGHT);
        cycleCount = 0.0f;
        mBgRuntimeShader.setFloatUniform("uTranslateY", mBgEffectData.uTranslateY);
        mBgRuntimeShader.setFloatUniform("uPoints", mBgEffectData.uPoints);
        mBgRuntimeShader.setFloatUniform("uColors", uColors);
        mBgRuntimeShader.setFloatUniform("uNoiseScale", mBgEffectData.uNoiseScale);
        mBgRuntimeShader.setFloatUniform("uPointOffset", mBgEffectData.uPointOffset);
        mBgRuntimeShader.setFloatUniform("uPointRadiusMulti", mBgEffectData.uPointRadiusMulti);
        mBgRuntimeShader.setFloatUniform("uSaturateOffset", mBgEffectData.uSaturateOffset);
        mBgRuntimeShader.setFloatUniform("uShadowColorMulti", mBgEffectData.uShadowColorMulti);
        mBgRuntimeShader.setFloatUniform("uShadowColorOffset", mBgEffectData.uShadowColorOffset);
        mBgRuntimeShader.setFloatUniform("uShadowOffset", mBgEffectData.uShadowOffset);
        mBgRuntimeShader.setFloatUniform("uBound", uBgBound);
        mBgRuntimeShader.setFloatUniform("uAlphaMulti", mBgEffectData.uAlphaMulti);
        mBgRuntimeShader.setFloatUniform("uLightOffset", mBgEffectData.uLightOffset);
        mBgRuntimeShader.setFloatUniform("uAlphaOffset", mBgEffectData.uAlphaOffset);
        mBgRuntimeShader.setFloatUniform("uShadowNoiseScale", mBgEffectData.uShadowNoiseScale);

        mAnimConfig1 = new AnimConfig().setEase(FolmeEase.spring(0.9f, 1.3f));
        mAnimConfig2 = new AnimConfig().setEase(FolmeEase.spring(0.9f, 0.6f));
        mStateStyle = Folme.useValue(this);
        float[] gradientColors = mBgEffectData.gradientColors2;
        startColorValue = gradientColors;
        endColorValue = gradientColors;
    }

    public RenderEffect getRenderEffect() {
        return RenderEffect.createShaderEffect(mBgRuntimeShader);
    }

    public void stop() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mStateStyle != null) {
            mStateStyle.cancel();
            mStateStyle.clean();
            mStateStyle = null;
        }
    }


    public void updateMaterials(float f) {
        uAnimTime += f * gradientSpeed;
        computeGradientColor();
        mBgRuntimeShader.setFloatUniform("uAnimTime", uAnimTime);
        mBgRuntimeShader.setFloatUniform("uColors", uColors);
    }

    public void setResolution(float f, float f2) {
        mBgRuntimeShader.setFloatUniform("uResolution", f, f2);
    }

    private String loadShader(Resources res, int i) {
        try {
            InputStream openRawResource = res.openRawResource(i);
            try {
                Scanner scanner = new Scanner(openRawResource);
                try {
                    StringBuilder sb = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        sb.append(scanner.nextLine());
                        sb.append("\n");
                    }
                    String sb2 = sb.toString();
                    scanner.close();
                    if (openRawResource != null) {
                        openRawResource.close();
                    }
                    return sb2;
                } finally {
                }
            } finally {
            }
        } catch (Exception e) {
            Log.e("Error", e.toString());
            return null;
        }
    }

    private void computeGradientColor() {
        double d = uAnimTime / mBgEffectData.colorInterpPeriod;
        float floor = (float) Math.floor((d - Math.floor(d)) * 2.0d);
        if (Math.abs(prevT - floor) > 0.5d) {
            if (cycleCount % 4.0f == 0.0f) {
                startColorValue = mBgEffectData.gradientColors2;
                endColorValue = mBgEffectData.gradientColors1;
                executeAnim();
            } else if (cycleCount % 4.0f == 1.0f) {
                startColorValue = mBgEffectData.gradientColors1;
                endColorValue = mBgEffectData.gradientColors2;
                executeAnim();
            } else if (cycleCount % 4.0f == 2.0f) {
                startColorValue = mBgEffectData.gradientColors2;
                endColorValue = mBgEffectData.gradientColors3;
                executeAnim();
            } else if (cycleCount % 4.0f == 3.0f) {
                startColorValue = mBgEffectData.gradientColors3;
                endColorValue = mBgEffectData.gradientColors2;
                executeAnim();
            }
            cycleCount += 1.0f;
        }
        prevT = floor;
        linearInterpolate(uColors, startColorValue, endColorValue, colorInterpT);
    }

    private void executeAnim() {
        if (mStateStyle != null) {
            mStateStyle.setTo(new Object[]{"colorInterpT", Float.valueOf(0.0f)});
            mStateStyle.to(new Object[]{"colorInterpT", Float.valueOf(1.0f), mAnimConfig1});
            mStateStyle.to(new Object[]{"gradientSpeed", Float.valueOf(mBgEffectData.gradientSpeedChange), mAnimConfig1});
            mHandler.postDelayed(() ->
                mStateStyle.to(new Object[]{"gradientSpeed",
                    Float.valueOf(mBgEffectData.gradientSpeedRest), mAnimConfig2}),
                300L
            );
        }
    }

    public static void linearInterpolate(float[] fArr, float[] fArr2, float[] fArr3, float f) {
        for (int i = 0; i < fArr2.length; i++) {
            float f2 = fArr2[i];
            fArr[i] = f2 + ((fArr3[i] - f2) * f);
        }
    }

    public void setType(BgEffectDataManager.DeviceType deviceType, BgEffectDataManager.ThemeMode themeMode, float[] fArr) {
        uBgBound = fArr;
        mBgRuntimeShader.setFloatUniform("uBound", fArr);
        BgEffectDataManager.BgEffectData data = mBgEffectDataManager.getData(deviceType, themeMode);
        mBgEffectData = data;
        uAnimTime = 0.0f;
        float[] gradientColors2 = data.gradientColors2;
        startColorValue = gradientColors2;
        endColorValue = gradientColors2;
        mBgRuntimeShader.setFloatUniform("uTranslateY", mBgEffectData.uTranslateY);
        mBgRuntimeShader.setFloatUniform("uPoints", mBgEffectData.uPoints);
        mBgRuntimeShader.setFloatUniform("uNoiseScale", mBgEffectData.uNoiseScale);
        mBgRuntimeShader.setFloatUniform("uPointOffset", mBgEffectData.uPointOffset);
        mBgRuntimeShader.setFloatUniform("uPointRadiusMulti", mBgEffectData.uPointRadiusMulti);
        mBgRuntimeShader.setFloatUniform("uSaturateOffset", mBgEffectData.uSaturateOffset);
        mBgRuntimeShader.setFloatUniform("uShadowColorMulti", mBgEffectData.uShadowColorMulti);
        mBgRuntimeShader.setFloatUniform("uShadowColorOffset", mBgEffectData.uShadowColorOffset);
        mBgRuntimeShader.setFloatUniform("uShadowOffset", mBgEffectData.uShadowOffset);
        mBgRuntimeShader.setFloatUniform("uAlphaMulti", mBgEffectData.uAlphaMulti);
        mBgRuntimeShader.setFloatUniform("uLightOffset", mBgEffectData.uLightOffset);
        mBgRuntimeShader.setFloatUniform("uAlphaOffset", mBgEffectData.uAlphaOffset);
        mBgRuntimeShader.setFloatUniform("uShadowNoiseScale", mBgEffectData.uShadowNoiseScale);
    }
}
