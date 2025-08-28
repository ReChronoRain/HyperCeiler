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

    BgEffectPainter mBgEffectPainter;

    private float[] bound;
    private float mDeltaTime;
    private long mLastGlobalTime;
    private final View mTarget;
    private float mTime;
    private float mTimeDirection = 1.0f;

    public BgEffectController(View view) {
        mTarget = view;
    }

    public void start() {
        if (mBgEffectPainter == null) {
            mBgEffectPainter = new BgEffectPainter(mTarget.getContext());
            mLastGlobalTime = System.nanoTime();
            resetTime();
            mTarget.post(this);
        }
    }

    @Override
    public void run() {
        if (mBgEffectPainter != null) {
            tickPingPong();
            mBgEffectPainter.setResolution(mTarget.getWidth(), mTarget.getHeight());
            mBgEffectPainter.updateMaterials(mDeltaTime);
            mTarget.setRenderEffect(mBgEffectPainter.getRenderEffect());
            mTarget.postDelayed(this, 16L);
        }
    }

    private void tickPingPong() {
        long nanoTime = System.nanoTime();
        float f = (float) ((nanoTime - mLastGlobalTime) * 1.0E-9d);
        mDeltaTime = f;
        mTime = mTime + (f * mTimeDirection);
        if (mTimeDirection > 0.0f) {
            if (mTime >= 7200.0f) {
                mTimeDirection = -1.0f;
            }
        } else if (mTime <= 0.0f) {
            mTimeDirection = 1.0f;
        }
        mLastGlobalTime = nanoTime;
    }

    public void resetTime() {
        mLastGlobalTime = System.nanoTime();
        mTime = 0.0f;
        mTimeDirection = 1.0f;
    }

    public void stop() {
        if (mBgEffectPainter != null) {
            mTarget.removeCallbacks(this);
            mBgEffectPainter.stop();
            mBgEffectPainter = null;
            mTarget.setRenderEffect(null);
        }
    }

    public void setType(Context context, View view, ActionBar actionBar) {
        resetTime();
        calcAnimationBound(context, view, actionBar);
        if (ViewUtils.isNightMode(context)) {
            if (Build.IS_TABLET) {
                mBgEffectPainter.setType(BgEffectDataManager.DeviceType.TABLET, BgEffectDataManager.ThemeMode.DARK, bound);
            } else {
                mBgEffectPainter.setType(BgEffectDataManager.DeviceType.PHONE, BgEffectDataManager.ThemeMode.DARK, bound);
            }
            return;
        }
        if (Build.IS_TABLET) {
            mBgEffectPainter.setType(BgEffectDataManager.DeviceType.TABLET, BgEffectDataManager.ThemeMode.LIGHT, bound);
        } else {
            mBgEffectPainter.setType(BgEffectDataManager.DeviceType.PHONE, BgEffectDataManager.ThemeMode.LIGHT, bound);
        }
    }

    private void calcAnimationBound(Context context, View view, ActionBar actionBar) {
        float height = (actionBar != null ? actionBar.getHeight() + 0.0f : 0.0f) + context.getResources().getDimensionPixelSize(R.dimen.logo_area_height);
        float height2 = height / ((ViewGroup) view.getParent()).getHeight();
        float width = ((ViewGroup) view.getParent()).getWidth();
        if (width <= height) {
            bound = new float[]{0.0f, 1.0f - height2, 1.0f, height2};
        } else {
            bound = new float[]{((width - height) / 2.0f) / width, 1.0f - height2, height / width, height2};
        }
    }
}
