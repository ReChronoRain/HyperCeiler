package com.sevtinge.hyperceiler.common.view;

import android.view.animation.Interpolator;

public class CubicEaseOutInterpolater implements Interpolator {
    @Override
    public float getInterpolation(float input) {
        float f = input - 1.0f;
        return (f * f * f) + 1.0f;
    }
}
