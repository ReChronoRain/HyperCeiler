package com.sevtinge.hyperceiler.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.R;

import java.io.InputStream;
import java.util.Scanner;

import fan.appcompat.app.ActionBar;
import fan.internal.utils.ViewUtils;

public class BgEffectPainter {
    private float[] bound;
    RuntimeShader mBgRuntimeShader;
    Context mContext;
    Resources mResources;
    private float[] uResolution;
    private float uAnimTime = ((float) System.nanoTime()) / 1.0E9f;
    private float[] uBgBound = {0.0f, 0.4489f, 1.0f, 0.5511f};
    private float uTranslateY = 0.0f;
    private float[] uPoints = {0.67f, 0.42f, 1.0f, 0.69f, 0.75f, 1.0f, 0.14f, 0.71f, 0.95f, 0.14f, 0.27f, 0.8f};
    private float[] uColors = {0.57f, 0.76f, 0.98f, 1.0f, 0.98f, 0.85f, 0.68f, 1.0f, 0.98f, 0.75f, 0.93f, 1.0f, 0.73f, 0.7f, 0.98f, 1.0f};
    private float uAlphaMulti = 1.0f;
    private float uNoiseScale = 1.5f;
    private float uPointOffset = 0.1f;
    private float uPointRadiusMulti = 1.0f;
    private float uSaturateOffset = 0.2f;
    private float uLightOffset = 0.1f;
    private float uAlphaOffset = 0.5f;
    private float uShadowColorMulti = 0.3f;
    private float uShadowColorOffset = 0.3f;
    private float uShadowNoiseScale = 5.0f;
    private float uShadowOffset = 0.01f;

    public BgEffectPainter(Context context) {
        mContext = context;
        mResources = context.getResources();
        String loadShader = loadShader(mResources, R.raw.bg_frag);
        mBgRuntimeShader = new RuntimeShader(loadShader);
        mBgRuntimeShader.setFloatUniform("uTranslateY", uTranslateY);
        mBgRuntimeShader.setFloatUniform("uPoints", uPoints);
        mBgRuntimeShader.setFloatUniform("uColors", uColors);
        mBgRuntimeShader.setFloatUniform("uNoiseScale", uNoiseScale);
        mBgRuntimeShader.setFloatUniform("uPointOffset", uPointOffset);
        mBgRuntimeShader.setFloatUniform("uPointRadiusMulti", uPointRadiusMulti);
        mBgRuntimeShader.setFloatUniform("uSaturateOffset", uSaturateOffset);
        mBgRuntimeShader.setFloatUniform("uShadowColorMulti", uShadowColorMulti);
        mBgRuntimeShader.setFloatUniform("uShadowColorOffset", uShadowColorOffset);
        mBgRuntimeShader.setFloatUniform("uShadowOffset", uShadowOffset);
        mBgRuntimeShader.setFloatUniform("uBound", uBgBound);
        mBgRuntimeShader.setFloatUniform("uAlphaMulti", uAlphaMulti);
        mBgRuntimeShader.setFloatUniform("uLightOffset", uLightOffset);
        mBgRuntimeShader.setFloatUniform("uAlphaOffset", uAlphaOffset);
        mBgRuntimeShader.setFloatUniform("uShadowNoiseScale", uShadowNoiseScale);
    }

    public RenderEffect getRenderEffect() {
        return RenderEffect.createRuntimeShaderEffect(mBgRuntimeShader, "uTex");
    }

    public void updateMaterials() {
        mBgRuntimeShader.setFloatUniform("uAnimTime", uAnimTime);
        mBgRuntimeShader.setFloatUniform("uResolution", uResolution);
    }

    public void setAnimTime(float f) {
        uAnimTime = f;
    }

    public void setColors(float[] fArr) {
        uColors = fArr;
        mBgRuntimeShader.setFloatUniform("uColors", fArr);
    }

    public void setPoints(float[] fArr) {
        uPoints = fArr;
        mBgRuntimeShader.setFloatUniform("uPoints", fArr);
    }

    public void setBound(float[] fArr) {
        this.uBgBound = fArr;
        this.mBgRuntimeShader.setFloatUniform("uBound", fArr);
    }

    public void setLightOffset(float f) {
        this.uLightOffset = f;
        this.mBgRuntimeShader.setFloatUniform("uLightOffset", f);
    }

    public void setSaturateOffset(float f) {
        this.uSaturateOffset = f;
        this.mBgRuntimeShader.setFloatUniform("uSaturateOffset", f);
    }

    public void setPhoneLight(float[] fArr) {
        setLightOffset(0.1f);
        setSaturateOffset(0.2f);
        setPoints(new float[]{0.67f, 0.42f, 1.0f, 0.69f, 0.75f, 1.0f, 0.14f, 0.71f, 0.95f, 0.14f, 0.27f, 0.8f});
        setColors(new float[]{0.57f, 0.76f, 0.98f, 1.0f, 0.98f, 0.85f, 0.68f, 1.0f, 0.98f, 0.75f, 0.93f, 1.0f, 0.73f, 0.7f, 0.98f, 1.0f});
        setBound(fArr);
    }

    public void setPhoneDark(float[] fArr) {
        setLightOffset(-0.1f);
        setSaturateOffset(0.2f);
        setPoints(new float[]{0.63f, 0.5f, 0.88f, 0.69f, 0.75f, 0.8f, 0.17f, 0.66f, 0.81f, 0.14f, 0.24f, 0.72f});
        setColors(new float[]{0.0f, 0.31f, 0.58f, 1.0f, 0.53f, 0.29f, 0.15f, 1.0f, 0.46f, 0.06f, 0.27f, 1.0f, 0.16f, 0.12f, 0.45f, 1.0f});
        setBound(fArr);
    }

    public void setPadLight(float[] fArr) {
        setLightOffset(0.1f);
        setSaturateOffset(0.0f);
        setPoints(new float[]{0.67f, 0.37f, 0.88f, 0.54f, 0.66f, 1.0f, 0.37f, 0.71f, 0.68f, 0.28f, 0.26f, 0.62f});
        setColors(new float[]{0.57f, 0.76f, 0.98f, 1.0f, 0.98f, 0.85f, 0.68f, 1.0f, 0.98f, 0.75f, 0.93f, 0.95f, 0.73f, 0.7f, 0.98f, 0.9f});
        setBound(fArr);
    }

    public void setPadDark(float[] fArr) {
        setLightOffset(-0.1f);
        setSaturateOffset(0.2f);
        setPoints(new float[]{0.55f, 0.42f, 1.0f, 0.56f, 0.75f, 1.0f, 0.4f, 0.59f, 0.71f, 0.43f, 0.09f, 0.75f});
        setColors(new float[]{0.0f, 0.31f, 0.58f, 1.0f, 0.53f, 0.29f, 0.15f, 1.0f, 0.46f, 0.06f, 0.27f, 1.0f, 0.16f, 0.12f, 0.45f, 1.0f});
        setBound(fArr);
    }

    public void setResolution(float[] fArr) {
        this.uResolution = fArr;
    }

    private String loadShader(Resources resources, int i) {
        try {
            InputStream openRawResource = resources.openRawResource(i);
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

    public void showRuntimeShader(Context context, View view, ActionBar actionBar) {
        calcAnimationBound(context, view, actionBar);
        if (ViewUtils.isNightMode(context)) {
            setPhoneDark(this.bound);
        } else {
            setPhoneLight(this.bound);
        }
    }

    private void calcAnimationBound(Context context, View view, ActionBar actionBar) {
        float height = (actionBar != null ? actionBar.getHeight() + 0.0f : 0.0f) + context.getResources().getDimensionPixelSize(R.dimen.about_logo_area_height);
        float height2 = height / ((ViewGroup) view.getParent()).getHeight();
        float width = ((ViewGroup) view.getParent()).getWidth();
        if (width <= height) {
            this.bound = new float[]{0.0f, 1.0f - height2, 1.0f, height2};
        } else {
            this.bound = new float[]{((width - height) / 2.0f) / width, 1.0f - height2, height / width, height2};
        }
    }
}
