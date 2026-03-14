package com.sevtinge.hyperceiler.about.controller;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RawRes;

import com.sevtinge.hyperceiler.R;

import java.io.InputStream;
import java.util.Scanner;

import fan.animation.Folme;
import fan.animation.FolmeEase;
import fan.animation.IStateStyle;
import fan.animation.base.AnimConfig;

public class BgEffectPainter {

    AnimConfig animConfig1;
    AnimConfig animConfig2;
    BgEffectDataManager.BgEffectData mBgEffectData;
    BgEffectDataManager mBgEffectDataManager;
    RuntimeShader mBgRuntimeShader;
    Handler mHandler;
    IStateStyle stateStyle;


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
        animConfig1 = new AnimConfig().setEase(FolmeEase.spring(0.9f, 1.3f));
        animConfig2 = new AnimConfig().setEase(FolmeEase.spring(0.9f, 0.6f));
        stateStyle = Folme.useValue(this);
        startColorValue = mBgEffectData.gradientColors2;
        endColorValue = mBgEffectData.gradientColors2;
    }

    public RenderEffect getRenderEffect() {
        return RenderEffect.createShaderEffect(mBgRuntimeShader);
    }

    public void stop() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (stateStyle != null) {
            stateStyle.cancel();
            stateStyle.clean();
            stateStyle = null;
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

    private String loadShader(Resources resources, @RawRes int id) {
        try {
            InputStream stream = resources.openRawResource(id);
            Scanner scanner = new Scanner(stream);
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
                sb.append("\n");
            }
            String s = sb.toString();
            scanner.close();
            if (stream != null) {
                stream.close();
            }
            return s;
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
        if (stateStyle != null) {
            stateStyle.setTo("colorInterpT", Float.valueOf(0.0f));
            stateStyle.to("colorInterpT", Float.valueOf(1.0f), animConfig1);
            stateStyle.to("gradientSpeed", Float.valueOf(mBgEffectData.gradientSpeedChange), animConfig1);
            mHandler.postDelayed(() ->
                stateStyle.to(new Object[]{"gradientSpeed", Float.valueOf(mBgEffectData.gradientSpeedRest), animConfig2}), 300L);
        }
    }

    public static void linearInterpolate(float[] colors, float[] startColor, float[] endColor, float color) {
        for (int i = 0; i < startColor.length; i++) {
            float f2 = startColor[i];
            colors[i] = f2 + ((endColor[i] - f2) * color);
        }
    }

    public void setType(BgEffectDataManager.DeviceType deviceType, BgEffectDataManager.ThemeMode themeMode, float[] uBound) {
        uBgBound = uBound;
        mBgRuntimeShader.setFloatUniform("uBound", uBound);
        mBgEffectData = mBgEffectDataManager.getData(deviceType, themeMode);
        uAnimTime = 0.0f;
        startColorValue = mBgEffectData.gradientColors2;
        endColorValue = mBgEffectData.gradientColors2;
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
