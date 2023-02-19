package com.sevtinge.cemiuiler.view;

import android.animation.ArgbEvaluator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.sevtinge.cemiuiler.utils.LogUtils;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import de.robv.android.xposed.XposedHelpers;

public class BatteryIndicatorView extends ImageView {

    protected int mDisplayWidth;
    protected boolean mIsBeingCharged;
    protected boolean mIsExtremePowerSave;
    protected boolean mIsPowerSave;
    protected final int mLowLevelSystem = getResources().getInteger(getResources().getIdentifier("config_lowBatteryWarningLevel", "integer", "android"));
    protected int mPowerLevel;
    protected int mTestPowerLevel;
    private int mFullColor = Color.GREEN;
    private int mLowColor = Color.RED;
    private int mPowerSaveColor = Color.rgb(245, 166, 35);
    private int mChargingColor = Color.YELLOW;
    private int mLowLevel = mLowLevelSystem;
    private int mHeight = 5;
    private int mGlow = 0;
    private int mTransparency = 0;
    private int mPadding = 0;
    private int mVisibility = View.VISIBLE;
    private ColorMode mColorMode = ColorMode.DISCRETE;
    private boolean mTesting = false;
    private boolean mRounded = false;
    private boolean mCentered = false;
    private boolean mExpanded = false;
    private boolean mOnKeyguard = false;
    private boolean mBottom = false;
    private boolean mLimited = false;
    private int mTintColor = Color.argb(153, 0, 0, 0);
    private Object mStatusBar = null;

    enum ColorMode {
        DISCRETE, GRADUAL, RAINBOW
    }

    public BatteryIndicatorView(Context context) {
        super(context);
        updateDisplaySize();
    }

    public BatteryIndicatorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        updateDisplaySize();
    }

    public void init(Object statusBar) {
        mStatusBar = statusBar;

        try {
            ShapeDrawable shape = new ShapeDrawable();
            Paint paint = shape.getPaint();
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            shape.setIntrinsicWidth(9999);
            setImageDrawable(shape);
        } catch (Throwable t) {
            LogUtils.log(t);
        }

        updateParameters();
        new PrefsUtils.SharedPrefsObserver(getContext(), new Handler(getContext().getMainLooper())) {
            @Override
            public void onChange(Uri uri) {
                try {
                    String key = uri.getPathSegments().get(2);
                    if (!mTesting ) {
                        updateParameters();
                        update();
                    }
                } catch (Throwable t) {
                    LogUtils.log(t);
                }
            }
        };
        getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                removeCallbacks(step);
                startTest();
            }
        }, new IntentFilter("moralnorm.module.BatteryIndicatorTest"));
    }

    Runnable step = new Runnable() {
        @Override
        public void run() {
            mTestPowerLevel--;
            if (mTestPowerLevel >= 0) {
                update();
                postDelayed(step, mTestPowerLevel == mLowLevel - 1 ? 300 : 20);
            } else {
                removeCallbacks(step);
                mTesting = false;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateParameters();
                        update();
                    }
                }, 1000);
            }
        }
    };

    private void startTest() {
        mTesting = true;
        mTestPowerLevel = 100;
        post(step);
    }

    private void postUpdate() {
        post(BatteryIndicatorView.this::update);
    }

    public void onExpandingChanged(boolean expanded) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        update();
    }

    public void onKeyguardStateChanged(boolean showing) {
        if (mOnKeyguard == showing) return;
        mOnKeyguard = showing;
        update();
    }

    public void onDarkModeChanged(float intensity, int tintColor) {
        //if (intensity != 0.0f && intensity != 1.0f) return;
        if (mTintColor == tintColor) return;
        mTintColor = tintColor;
        update();
    }

    public void onBatteryLevelChanged(int powerLevel, boolean isCharging, boolean isCharged) {
        if (this.mPowerLevel == powerLevel && this.mIsBeingCharged == isCharging && !isCharged) return;
        this.mPowerLevel = powerLevel;
        this.mIsBeingCharged = isCharging && !isCharged;
//		if (isCharging != this.mIsCharged) {
//			this.mIsCharged = isCharging;
//			if (!this.mIsCharged)
//				startChargingAnim();
//			else
//				stopChargingAnim();
//		}
        update();
    }

    public void onPowerSaveChanged(boolean isPowerSave) {
        if (this.mIsPowerSave == isPowerSave) return;
        this.mIsPowerSave = isPowerSave;
        update();
    }

    public void onExtremePowerSaveChanged(boolean isExtremePowerSave) {
        if (this.mIsExtremePowerSave == isExtremePowerSave ) return;
        this.mIsExtremePowerSave = isExtremePowerSave;
        update();
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateDisplaySize();
        postUpdate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            updateDisplaySize();
            postUpdate();
        }
    }

    public void update() {
        if (mLimited) this.setVisibility(mExpanded || mOnKeyguard ? mVisibility : View.GONE);
        clearAnimation();
        updateDrawable();
    }

    public void updateDisplaySize() {
        this.mDisplayWidth = getMeasuredWidth();
    }

    protected void updateParameters() {
        mColorMode = ColorMode.values()[Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_color", "0"))];
        mFullColor = PrefsUtils.getSharedIntPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_color_full_power", Color.GREEN);
        mLowColor = PrefsUtils.getSharedIntPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_color_low_power", Color.RED);
        mPowerSaveColor = PrefsUtils.getSharedIntPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_color_power_saving", Color.rgb(245, 166, 35));
        mChargingColor = PrefsUtils.getSharedIntPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_color_power_charging", Color.YELLOW);
        mLowLevel = PrefsUtils.getSharedIntPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_low_level", mLowLevelSystem);
        mHeight = PrefsUtils.getSharedIntPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_height", 5);
        mGlow = PrefsUtils.getSharedIntPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_glow", 0);
        mRounded = PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_rounded", false);
        mBottom = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_align", "0")) == 1;
        mCentered = PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_centered", false);
        mLimited = PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_limitvis", false);
        mTransparency = PrefsUtils.getSharedIntPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_alpha", 0);
        mPadding = PrefsUtils.getSharedIntPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_padding", 0);
        mVisibility = PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_ui_status_bar_battery_indicator_enable", false) ? View.VISIBLE : View.GONE;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)getLayoutParams();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.gravity = mBottom ? Gravity.BOTTOM : Gravity.TOP;
        setLayoutParams(lp);
        try { this.setImageAlpha(255 - Math.round(255 * mTransparency / 100f)); } catch (Throwable ignore) {};
        this.setVisibility(mVisibility);
        this.setScaleType(mCentered ? ScaleType.CENTER : ScaleType.MATRIX);
        Matrix matrix = new Matrix();
        matrix.setTranslate(0, 0);
        matrix.setScale(1, 1);
        this.setImageMatrix(new Matrix());
    }

    protected void updateDrawable() {
        try {
            int level = this.mTesting ? this.mTestPowerLevel : this.mPowerLevel;
            int color = this.mFullColor;
            if (!this.mTesting && this.mIsBeingCharged)
                color = this.mChargingColor;
            else if (!this.mTesting && (this.mIsPowerSave || this.mIsExtremePowerSave))
                color = this.mPowerSaveColor;
            else if (level <= this.mLowLevel)
                color = this.mLowColor;

            ShapeDrawable shape = (ShapeDrawable)getDrawable();
            shape.setShaderFactory(null);
            Paint paint = shape.getPaint();
            paint.setShader(null);

            if (color == Color.TRANSPARENT && mStatusBar != null)
                try {
                    if (mExpanded) {
                        color = Color.WHITE;
                    } else {
                        if (mOnKeyguard) {
                            boolean isLightWallpaperStatusBar = (boolean) XposedHelpers.callMethod(XposedHelpers.getObjectField(mStatusBar, "mUpdateMonitor"), "isLightWallpaperStatusBar");
                            color = (isLightWallpaperStatusBar ? Color.argb(153, 0, 0, 0) : Color.WHITE);
                        } else {
                            color = mTintColor;
                        }
                    }
                } catch (Throwable t) {
                    LogUtils.log(t);
                }

            int mDisplayPadding = Math.round(mPadding / 100f * this.mDisplayWidth);

            if (mColorMode == ColorMode.GRADUAL) {
                color = level <= this.mLowLevel || (!this.mTesting && (this.mIsBeingCharged || this.mIsPowerSave || this.mIsExtremePowerSave)) ? color : (int)new ArgbEvaluator().evaluate(1f - (level - this.mLowLevel) / (100f - this.mLowLevel), color, mLowColor);
            } else if (mColorMode == ColorMode.RAINBOW) {
                int steps = 15;
                float jump = 300f / (float)steps;
                float[] pos = new float[steps];
                int[] rainbow = new int[steps];
                for (int i = 0; i < steps; i++) {
                    pos[i] = i / (float)(steps - 1);
                    float c = (mCentered ? 240 : 0) + jump * i;
                    if (c > 360) c -= 360;
                    rainbow[i] = Color.HSVToColor(255, new float[]{ c, 1.0f, 1.0f});
                }
                shape.setShaderFactory(new ShapeDrawable.ShaderFactory() {
                    @Override
                    public Shader resize(int width, int height) {
                        if (mCentered)
                            return new LinearGradient(width / 2f - (mDisplayWidth - mDisplayPadding * 2) / 2f, height / 2f, (mDisplayWidth - mDisplayPadding * 2), height / 2f, rainbow, pos, Shader.TileMode.CLAMP);
                        else
                            return new LinearGradient(0, height / 2f, (mDisplayWidth - mDisplayPadding * 2), height / 2f, rainbow, pos, Shader.TileMode.CLAMP);
                    }
                });
            }
            paint.setColor(color);
            shape.setShape(mRounded ? new RoundRectShape(new float[] { mHeight, mHeight, mHeight, mHeight, mHeight, mHeight, mHeight, mHeight }, null, null) : new RectShape());

            int mWidth = Math.round((this.mDisplayWidth - mDisplayPadding * 2) * level / 100f);
            float mDensity = getResources().getDisplayMetrics().density;
            int sbHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
            if (mGlow == 0) {
                paint.clearShadowLayer();
                if (mBottom)
                    setPadding(mDisplayPadding, 0, mDisplayPadding, -mHeight);
                else
                    setPadding(mDisplayPadding, -mHeight, mDisplayPadding, 0);
                shape.setIntrinsicHeight(mHeight * 2);
                shape.setIntrinsicWidth(mWidth);
            } else {
                int shadowPadding = sbHeight - mHeight;
                paint.setShadowLayer(
                        (mGlow / 100f) * (sbHeight - 9 * mDensity),
                        (mCentered || mDisplayPadding > 0) ? 0 : shadowPadding / 2f,
                        mBottom ? mHeight - 10 : 10 - mHeight,
                        Color.argb(Math.min(Math.round(mGlow / 100f * 255), Math.round(255 - mTransparency / 100f * 255)), Color.red(color), Color.green(color), Color.blue(color))
                );
                if (mDisplayPadding == 0)
                    setPadding(mCentered ? 0 : -shadowPadding, mBottom ? shadowPadding : -shadowPadding, mCentered ? 0 : Math.min(mDisplayWidth - mWidth, shadowPadding), mBottom ? -shadowPadding : shadowPadding);
                else
                    setPadding(mDisplayPadding, mBottom ? shadowPadding : -shadowPadding, mDisplayPadding, mBottom ? -shadowPadding : shadowPadding);
                shape.setIntrinsicHeight(sbHeight);
                shape.setIntrinsicWidth(mWidth + (mCentered ? 0 : (mDisplayPadding == 0 ? shadowPadding : 0)));
            }

            invalidate();
        } catch (Throwable t) {
            LogUtils.log(t);
        }
    }

}