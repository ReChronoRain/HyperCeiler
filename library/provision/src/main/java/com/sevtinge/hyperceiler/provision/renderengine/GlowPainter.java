package com.sevtinge.hyperceiler.provision.renderengine;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.util.Log;

import com.sevtinge.hyperceiler.provision.R;

import java.io.InputStream;
import java.util.Scanner;

public class GlowPainter {


    float uTime;
    float[] uResolution = {1920.0f, 1080.0f};
    long mFrameDuration = 16;
    float uScale2 = 0.82f;
    float uSpeed2 = 0.49f;
    float uColorInMin = 0.3f;
    float uColorInMax = 1.0f;
    float uColorOutMin = 0.3f;
    float uColorOutMax = 0.86f;
    float uColorMidPoint = 0.47f;
    float uUseOklab = 1.0f;
    float[] uColorBlack = {0.961f, 0.157f, 0.157f};
    float[] uColorMid = {0.604f, 0.659f, 0.961f};
    float[] uColorWhite = {0.302f, 0.29f, 0.843f};
    float uScale = 1.3f;
    float uSpeed = 0.4f;
    float uBrightnessInMin = 0.25f;
    float uBrightnessInMax = 1.0f;
    float uBrightnessOutMin = 0.25f;
    float uBrightnessOutMax = 1.0f;
    float uShowCircle = 1.0f;
    float uCircleThickness = 0.4f;
    float uCircleFinalRadius = 1.0f;
    float uCircleYOffset = 0.1f;
    float uCircleSpeed = 0.9f;
    float uCircleColorFreq = 1.0f;
    float uCircleColorSpeed = 0.0f;
    float uCircleEasing = 1.4f;
    float uCircleAnimationOffset = 0.0f;
    float uMaskDelay = 0.3f;
    float uMaskThickness = 0.3f;
    float uCircleScreenBlend = 1.0f;
    float uCircleAddBlend = 0.04f;
    float uCircleColorOffset = 0.25f;
    float uCircleUVDistort = 0.0f;
    float uColorToDistortWidthRatio = 0.6f;
    float uDistortStartTime = 0.2f;
    float uDistortEndTime = 0.3f;
    float uDistortStart = 0.0f;
    float uDistortEnd = 1.0f;
    float uStripeFrequency = 0.0f;
    float uStripeStrengthX = 0.0f;
    float uStripeStrengthY = 0.0f;
    float uStripeUVDistort = 0.0f;

    RuntimeShader mShader;

    public GlowPainter(Context context) {
        String strLoadShader = loadShader(context.getResources(), R.raw.glow);
        strLoadShader.getClass();
        mShader = new RuntimeShader(strLoadShader);
        mShader.setFloatUniform("uScale2", uScale2);
        mShader.setFloatUniform("uSpeed2", uSpeed2);
        mShader.setFloatUniform("uColorInMin", uColorInMin);
        mShader.setFloatUniform("uColorInMax", uColorInMax);
        mShader.setFloatUniform("uColorOutMin", this.uColorOutMin);
        mShader.setFloatUniform("uColorOutMax", this.uColorOutMax);
        mShader.setFloatUniform("uColorMidPoint", this.uColorMidPoint);
        mShader.setFloatUniform("uUseOklab", this.uUseOklab);
        mShader.setFloatUniform("uColorBlack", this.uColorBlack);
        mShader.setFloatUniform("uColorMid", this.uColorMid);
        mShader.setFloatUniform("uColorWhite", this.uColorWhite);
        mShader.setFloatUniform("uScale", this.uScale);
        mShader.setFloatUniform("uSpeed", this.uSpeed);
        mShader.setFloatUniform("uBrightnessInMin", this.uBrightnessInMin);
        mShader.setFloatUniform("uBrightnessInMax", this.uBrightnessInMax);
        mShader.setFloatUniform("uBrightnessOutMin", this.uBrightnessOutMin);
        mShader.setFloatUniform("uBrightnessOutMax", this.uBrightnessOutMax);
        mShader.setFloatUniform("uShowCircle", this.uShowCircle);
        mShader.setFloatUniform("uCircleThickness", this.uCircleThickness);
        mShader.setFloatUniform("uCircleFinalRadius", this.uCircleFinalRadius);
        mShader.setFloatUniform("uCircleYOffset", this.uCircleYOffset);
        mShader.setFloatUniform("uCircleSpeed", this.uCircleSpeed);
        mShader.setFloatUniform("uCircleColorFreq", this.uCircleColorFreq);
        mShader.setFloatUniform("uCircleColorSpeed", this.uCircleColorSpeed);
        mShader.setFloatUniform("uCircleEasing", this.uCircleEasing);
        mShader.setFloatUniform("uCircleAnimationOffset", this.uCircleAnimationOffset);
        mShader.setFloatUniform("uMaskDelay", this.uMaskDelay);
        mShader.setFloatUniform("uMaskThickness", this.uMaskThickness);
        mShader.setFloatUniform("uCircleScreenBlend", this.uCircleScreenBlend);
        mShader.setFloatUniform("uCircleAddBlend", this.uCircleAddBlend);
        mShader.setFloatUniform("uCircleColorOffset", this.uCircleColorOffset);
        mShader.setFloatUniform("uCircleUVDistort", uCircleUVDistort);
        mShader.setFloatUniform("uColorToDistortWidthRatio", uColorToDistortWidthRatio);
        mShader.setFloatUniform("uDistortStartTime", uDistortStartTime);
        mShader.setFloatUniform("uDistortEndTime", uDistortEndTime);
        mShader.setFloatUniform("uDistortStart", uDistortStart);
        mShader.setFloatUniform("uDistortEnd", uDistortEnd);
        mShader.setFloatUniform("uStripeFrequency", uStripeFrequency);
        mShader.setFloatUniform("uStripeStrengthX", uStripeStrengthX);
        mShader.setFloatUniform("uStripeStrengthY", uStripeStrengthY);
        mShader.setFloatUniform("uStripeUVDistort", uStripeUVDistort);
    }

    public RenderEffect getRenderEffect() {
        return RenderEffect.createShaderEffect(mShader);
    }

    public void setAnimTime(float value) {
        mShader.setFloatUniform("uTime", value);
    }

    public void setResolution(float value1, float value2) {
        mShader.setFloatUniform("uResolution", value1, value2);
    }

    public void setCircleYOffset(float value) {
        uCircleYOffset = value;
        mShader.setFloatUniform("uCircleYOffset", value);
    }

    public void needAdmission(boolean need) {
        mShader.setFloatUniform("uShowCircle", need ? 1.0f : 0.0f);
    }

    private String loadShader(Resources resources, int i) {
        try {
            InputStream inputStreamOpenRawResource = resources.openRawResource(i);
            Scanner scanner = new Scanner(inputStreamOpenRawResource);
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
                sb.append("\n");
            }
            String string = sb.toString();
            scanner.close();
            if (inputStreamOpenRawResource != null) {
                inputStreamOpenRawResource.close();
            }
            return string;
        } catch (Exception e) {
            Log.e("Error", e.toString());
            return null;
        }
    }
}
