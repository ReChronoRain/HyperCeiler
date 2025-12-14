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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.main.page.about.controller.BgEffectDataManager;

import java.io.InputStream;
import java.util.Scanner;

import fan.animation.Folme;
import fan.animation.FolmeEase;
import fan.animation.IStateStyle;
import fan.animation.base.AnimConfig;


public class BgEffectPainter {
    AnimConfig animConfig1;
    AnimConfig animConfig2;
    private float cycleCount;
    private float[] endColorValue;
    BgEffectDataManager.BgEffectData mBgEffectData;
    BgEffectDataManager mBgEffectDataManager;
    RuntimeShader mBgRuntimeShader;
    Handler mHandler;
    private float[] startColorValue;
    IStateStyle stateStyle;
    private float uAnimTime = 0.0f;
    private float[] uBgBound = {0.0f, 0.4489f, 1.0f, 0.5511f};
    private final float[] uColors = {0.57f, 0.76f, 0.98f, 1.0f, 0.98f, 0.85f, 0.68f, 1.0f, 0.98f, 0.75f, 0.93f, 1.0f, 0.73f, 0.7f, 0.98f, 1.0f};
    private float prevT = 0.0f;
    private final float colorInterpT = 0.0f;
    private final float gradientSpeed = 1.0f;

    public BgEffectPainter(Context context) {
        this.cycleCount = 0.0f;
        String loadShader = loadShader(context.getResources(), R.raw.bg_frag);
        loadShader.getClass();
        this.mBgRuntimeShader = new RuntimeShader(loadShader);
        this.mHandler = new Handler(Looper.getMainLooper());
        BgEffectDataManager bgEffectDataManager = new BgEffectDataManager();
        this.mBgEffectDataManager = bgEffectDataManager;
        BgEffectDataManager.BgEffectData data = bgEffectDataManager.getData(BgEffectDataManager.DeviceType.PHONE, BgEffectDataManager.ThemeMode.LIGHT);
        this.mBgEffectData = data;
        this.cycleCount = 0.0f;
        this.mBgRuntimeShader.setFloatUniform("uTranslateY", data.uTranslateY);
        this.mBgRuntimeShader.setFloatUniform("uPoints", this.mBgEffectData.uPoints);
        this.mBgRuntimeShader.setFloatUniform("uColors", this.uColors);
        this.mBgRuntimeShader.setFloatUniform("uNoiseScale", this.mBgEffectData.uNoiseScale);
        this.mBgRuntimeShader.setFloatUniform("uPointOffset", this.mBgEffectData.uPointOffset);
        this.mBgRuntimeShader.setFloatUniform("uPointRadiusMulti", this.mBgEffectData.uPointRadiusMulti);
        this.mBgRuntimeShader.setFloatUniform("uSaturateOffset", this.mBgEffectData.uSaturateOffset);
        this.mBgRuntimeShader.setFloatUniform("uShadowColorMulti", this.mBgEffectData.uShadowColorMulti);
        this.mBgRuntimeShader.setFloatUniform("uShadowColorOffset", this.mBgEffectData.uShadowColorOffset);
        this.mBgRuntimeShader.setFloatUniform("uShadowOffset", this.mBgEffectData.uShadowOffset);
        this.mBgRuntimeShader.setFloatUniform("uBound", this.uBgBound);
        this.mBgRuntimeShader.setFloatUniform("uAlphaMulti", this.mBgEffectData.uAlphaMulti);
        this.mBgRuntimeShader.setFloatUniform("uLightOffset", this.mBgEffectData.uLightOffset);
        this.mBgRuntimeShader.setFloatUniform("uAlphaOffset", this.mBgEffectData.uAlphaOffset);
        this.mBgRuntimeShader.setFloatUniform("uShadowNoiseScale", this.mBgEffectData.uShadowNoiseScale);
        this.animConfig1 = new AnimConfig().setEase(FolmeEase.spring(0.9f, 1.3f));
        this.animConfig2 = new AnimConfig().setEase(FolmeEase.spring(0.9f, 0.6f));
        this.stateStyle = Folme.useValue(this);
        float[] fArr = this.mBgEffectData.gradientColors2;
        this.startColorValue = fArr;
        this.endColorValue = fArr;
    }

    public RenderEffect getRenderEffect() {
        return RenderEffect.createShaderEffect(this.mBgRuntimeShader);
    }

    public void stop() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        IStateStyle iStateStyle = this.stateStyle;
        if (iStateStyle != null) {
            iStateStyle.cancel();
            this.stateStyle.clean();
            this.stateStyle = null;
        }
    }

    public void updateMaterials(float f) {
        this.uAnimTime += f * this.gradientSpeed;
        computeGradientColor();
        this.mBgRuntimeShader.setFloatUniform("uAnimTime", this.uAnimTime);
        this.mBgRuntimeShader.setFloatUniform("uColors", this.uColors);
    }

    public void setResolution(float f, float f2) {
        this.mBgRuntimeShader.setFloatUniform("uResolution", f, f2);
    }

    private String loadShader(Resources resources, int i) {
        try {
            InputStream openRawResource = resources.openRawResource(i);
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
        } catch (Exception e) {
            Log.e("Error", e.toString());
            return null;
        }
    }

    private void computeGradientColor() {
        double d = this.uAnimTime / this.mBgEffectData.colorInterpPeriod;
        float floor = (float) Math.floor((d - Math.floor(d)) * 2.0d);
        if (Math.abs(this.prevT - floor) > 0.5d) {
            float f = this.cycleCount;
            if (f % 4.0f == 0.0f) {
                BgEffectDataManager.BgEffectData bgEffectData = this.mBgEffectData;
                this.startColorValue = bgEffectData.gradientColors2;
                this.endColorValue = bgEffectData.gradientColors1;
                executeAnim();
            } else if (f % 4.0f == 1.0f) {
                BgEffectDataManager.BgEffectData bgEffectData2 = this.mBgEffectData;
                this.startColorValue = bgEffectData2.gradientColors1;
                this.endColorValue = bgEffectData2.gradientColors2;
                executeAnim();
            } else if (f % 4.0f == 2.0f) {
                BgEffectDataManager.BgEffectData bgEffectData3 = this.mBgEffectData;
                this.startColorValue = bgEffectData3.gradientColors2;
                this.endColorValue = bgEffectData3.gradientColors3;
                executeAnim();
            } else if (f % 4.0f == 3.0f) {
                BgEffectDataManager.BgEffectData bgEffectData4 = this.mBgEffectData;
                this.startColorValue = bgEffectData4.gradientColors3;
                this.endColorValue = bgEffectData4.gradientColors2;
                executeAnim();
            }
            this.cycleCount += 1.0f;
        }
        this.prevT = floor;
        linearInterpolate(this.uColors, this.startColorValue, this.endColorValue, this.colorInterpT);
    }

    private void executeAnim() {
        IStateStyle iStateStyle = this.stateStyle;
        if (iStateStyle != null) {
            iStateStyle.setTo("colorInterpT", 0.0f);
            this.stateStyle.to("colorInterpT", 1.0f, this.animConfig1);
            this.stateStyle.to("gradientSpeed", this.mBgEffectData.gradientSpeedChange, this.animConfig1);
            this.mHandler.postDelayed(new GradientSpeedResetRunnable(), 300L);
        }
    }

    private class GradientSpeedResetRunnable implements Runnable {
        @Override
        public void run() {
            stateStyle.to("gradientSpeed", mBgEffectData.gradientSpeedRest, animConfig2);
        }
    }

    public static void linearInterpolate(float[] fArr, float[] fArr2, float[] fArr3, float f) {
        for (int i = 0; i < fArr2.length; i++) {
            float f2 = fArr2[i];
            fArr[i] = f2 + ((fArr3[i] - f2) * f);
        }
    }

    public void setType(BgEffectDataManager.DeviceType deviceType, BgEffectDataManager.ThemeMode themeMode, float[] fArr) {
        this.uBgBound = fArr;
        this.mBgRuntimeShader.setFloatUniform("uBound", fArr);
        BgEffectDataManager.BgEffectData data = this.mBgEffectDataManager.getData(deviceType, themeMode);
        this.mBgEffectData = data;
        this.uAnimTime = 0.0f;
        float[] fArr2 = data.gradientColors2;
        this.startColorValue = fArr2;
        this.endColorValue = fArr2;
        this.mBgRuntimeShader.setFloatUniform("uTranslateY", data.uTranslateY);
        this.mBgRuntimeShader.setFloatUniform("uPoints", this.mBgEffectData.uPoints);
        this.mBgRuntimeShader.setFloatUniform("uNoiseScale", this.mBgEffectData.uNoiseScale);
        this.mBgRuntimeShader.setFloatUniform("uPointOffset", this.mBgEffectData.uPointOffset);
        this.mBgRuntimeShader.setFloatUniform("uPointRadiusMulti", this.mBgEffectData.uPointRadiusMulti);
        this.mBgRuntimeShader.setFloatUniform("uSaturateOffset", this.mBgEffectData.uSaturateOffset);
        this.mBgRuntimeShader.setFloatUniform("uShadowColorMulti", this.mBgEffectData.uShadowColorMulti);
        this.mBgRuntimeShader.setFloatUniform("uShadowColorOffset", this.mBgEffectData.uShadowColorOffset);
        this.mBgRuntimeShader.setFloatUniform("uShadowOffset", this.mBgEffectData.uShadowOffset);
        this.mBgRuntimeShader.setFloatUniform("uAlphaMulti", this.mBgEffectData.uAlphaMulti);
        this.mBgRuntimeShader.setFloatUniform("uLightOffset", this.mBgEffectData.uLightOffset);
        this.mBgRuntimeShader.setFloatUniform("uAlphaOffset", this.mBgEffectData.uAlphaOffset);
        this.mBgRuntimeShader.setFloatUniform("uShadowNoiseScale", this.mBgEffectData.uShadowNoiseScale);
    }
}
