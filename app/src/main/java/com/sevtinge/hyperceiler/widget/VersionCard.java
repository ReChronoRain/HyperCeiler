package com.sevtinge.hyperceiler.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.controller.LogoAnimationController;
import com.sevtinge.hyperceiler.utils.SettingsFeatures;
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils;
import com.sevtinge.hyperceiler.view.CubicEaseOutInterpolater;

import fan.cardview.HyperCardView;
import fan.core.utils.MiuiBlurUtils;
import fan.internal.utils.ViewUtils;

public class VersionCard extends FrameLayout implements View.OnClickListener {

    private String versionName;

    private int scrollValue;
    private int modeValue = 0;
    private boolean mNeedStartAnim = true;
    private boolean mNeedUpdate = true;

    ViewGroup mRootView;
    private LinearLayout mLogoView;
    private ImageView mIconLogoView;
    private ImageView mTextLogoView;
    private LinearLayout mLogoViewShade;
    private ImageView mIconLogoViewShade;
    private ImageView mTextLogoViewShade;
    private ViewGroup mVersionLayout;
    private HyperCardView mUpdateText;

    private AnimatorSet mAnimatorSet;
    private CubicEaseOutInterpolater mInterpolater;
    private DecelerateInterpolator mDecelerateInterpolator;

    public LogoAnimationController mAnimationController;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (isAttachedToWindow()) {
                checkUpdate();
                if (mNeedUpdate) {
                    mAnimationController.iniData(getContext(), mNeedUpdate);
                    performLogoAnimation(false);
                    if (scrollValue != 0) {
                        mAnimationController.startButtonAnimation(scrollValue, mUpdateText);
                    }
                } else {
                    mHandler.sendEmptyMessageDelayed(0, 1500L);
                }
            }
        }
    };

    public VersionCard(@NonNull Context context) {
        super(context);
        initView();
    }

    public VersionCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        mRootView = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.layout_version_card, this, true);
        mLogoView = findViewById(R.id.logo_view_linear_layout);
        mIconLogoView = findViewById(R.id.app_icon_logo_view);
        mTextLogoView = findViewById(R.id.app_text_logo_view);
        mLogoViewShade = findViewById(R.id.logo_view_linear_layout_shade);
        mIconLogoViewShade = findViewById(R.id.app_icon_logo_view_shade);
        mTextLogoViewShade = findViewById(R.id.app_text_logo_view_shade);
        TextView versionTextView = findViewById(R.id.version_text);
        versionName = BuildConfig.VERSION_NAME + " | " + BuildConfig.BUILD_TYPE;
        if (!TextUtils.isEmpty(versionName)) {
            versionTextView.setText(versionName);
        }
        mVersionLayout = findViewById(R.id.version_layout);
        mUpdateText = findViewById(R.id.update_hint_text);
        mAnimatorSet = new AnimatorSet();
        mInterpolater = new CubicEaseOutInterpolater();
        mDecelerateInterpolator = new DecelerateInterpolator();
        mUpdateText.setOnClickListener(this);
        mUpdateText.setClickable(false);
        checkUpdate();
        mAnimationController = new LogoAnimationController(getContext(), mNeedUpdate);
        if (!mNeedUpdate) {
            mIconLogoView.setAlpha(1.0f);
            mTextLogoView.setAlpha(1.0f);
            mIconLogoViewShade.setAlpha(1.0f);
            mTextLogoViewShade.setAlpha(1.0f);
            mVersionLayout.setAlpha(1.0f);
            mHandler.sendEmptyMessageDelayed(0, 1500L);
        }
        setLogoBlur();
    }

    public void checkUpdate() {
        mNeedUpdate = !TextUtils.isEmpty(getUpdateInfo());
    }

    public void refreshUpdateStatus() {
        if ((TextUtils.isEmpty(getUpdateInfo())) == mNeedUpdate) {
            mNeedStartAnim = true;
            mNeedUpdate = !TextUtils.isEmpty(getUpdateInfo());
            mRootView.removeAllViews();
            initView();
            invalidate();
        }
    }

    public static String getUpdateInfo() {
        return "";
    }


    @Override
    public void onClick(View v) {

    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        performLogoAnimation(true);
    }

    public void performLogoAnimation(boolean z) {
        if (mNeedStartAnim && mNeedUpdate) {
            mNeedStartAnim = false;
            mIconLogoView.setPivotX(0.0f);
            mTextLogoView.setPivotX(0.0f);
            mIconLogoViewShade.setPivotX(0.0f);
            mTextLogoViewShade.setPivotX(0.0f);
            AnimatorSet animatorSet = new AnimatorSet();
            if (scrollValue == 0) {
                if (z) {
                    animatorSet.playTogether(
                            ObjectAnimator.ofFloat(mIconLogoView, "alpha", 0.0f, 1.0f),
                            ObjectAnimator.ofFloat(mTextLogoView, "alpha", 0.0f, 1.0f),
                            ObjectAnimator.ofFloat(mIconLogoViewShade, "alpha", 0.0f, 1.0f),
                            ObjectAnimator.ofFloat(mTextLogoViewShade, "alpha", 0.0f, 1.0f),
                            ObjectAnimator.ofFloat(mVersionLayout, "alpha", 0.0f, 1.0f),
                            ObjectAnimator.ofFloat(mUpdateText, "alpha", 0.0f, 1.0f)
                    );
                } else {
                    animatorSet.playTogether(ObjectAnimator.ofFloat(mUpdateText, "alpha", 0.0f, 1.0f));
                }
            }
            animatorSet.setDuration(800L);
            animatorSet.setInterpolator(mDecelerateInterpolator);
            Animator[] animatorItems = new Animator[6];
            animatorItems[0] = ObjectAnimator.ofFloat(
                    mIconLogoView, "translationY",
                    SettingsFeatures.isSplitTabletDevice() ? DisplayUtils.dp2px(getContext(), -27.0f) :
                            DisplayUtils.dp2px(getContext(), -30.0f)
            );
            animatorItems[1] = ObjectAnimator.ofFloat(
                    mTextLogoView, "translationY",
                    SettingsFeatures.isSplitTabletDevice() ? DisplayUtils.dp2px(getContext(), -27.0f) :
                            DisplayUtils.dp2px(getContext(), -30.0f)
            );
            animatorItems[2] = ObjectAnimator.ofFloat(
                    mIconLogoViewShade, "translationY",
                    SettingsFeatures.isSplitTabletDevice() ? DisplayUtils.dp2px(getContext(), -27.0f) :
                            DisplayUtils.dp2px(getContext(), -30.0f)
            );
            animatorItems[3] = ObjectAnimator.ofFloat(
                    mTextLogoViewShade, "translationY",
                    SettingsFeatures.isSplitTabletDevice() ? DisplayUtils.dp2px(getContext(), -27.0f) :
                            DisplayUtils.dp2px(getContext(), -30.0f)
            );
            animatorItems[4] = ObjectAnimator.ofFloat(
                    mVersionLayout, "translationY",
                    SettingsFeatures.isSplitTabletDevice() ? DisplayUtils.dp2px(getContext(), -27.0f) :
                            DisplayUtils.dp2px(getContext(), -30.0f)
            );
            animatorItems[5] = animatorSet;
            mAnimatorSet.playTogether(animatorItems);
            mAnimatorSet.setDuration(1000L);
            mAnimatorSet.setInterpolator(mInterpolater);
            mAnimatorSet.setStartDelay(100L);
            mAnimatorSet.start();
            mUpdateText.setClickable(true);
            mUpdateText.setOnClickListener(this);
        }
    }

    private void setLogoBlur() {
        if (MiuiBlurUtils.isEnable() && MiuiBlurUtils.isEffectEnable(getContext())) {
            MiuiBlurUtils.setBackgroundBlur(mRootView, (int) ((getResources().getDisplayMetrics().density * 50.0f) + 0.5f));
            MiuiBlurUtils.setViewBlurMode(mRootView, 0);
            int[] iArr = {getResources().getColor(R.color.logo_color1, null),
                    getResources().getColor(R.color.logo_color2, null),
                    getResources().getColor(R.color.logo_color3, null)
            };
            modeValue = ViewUtils.isNightMode(getContext()) ? 18 : 19;
            enableTextBlur(mIconLogoView, true, iArr, new int[]{modeValue, 100, 106});
            enableTextBlur(mTextLogoView, true, iArr, new int[]{modeValue, 100, 106});
            mIconLogoView.setBackgroundResource(R.drawable.ic_hyperceiler_logo);
            mTextLogoView.setBackgroundResource(R.drawable.ic_text_logo);
            Log.d("VersionCard", "start logoBlur: ");
        } else {
            mIconLogoView.setImageResource(R.drawable.ic_hyperceiler_logo);
            mTextLogoView.setImageResource(R.drawable.ic_text_logo);
            mIconLogoView.setColorFilter(getResources().getColor(R.color.logo_overlay_color, null));
            mTextLogoView.setColorFilter(getResources().getColor(R.color.logo_overlay_color, null));
            mLogoViewShade.setVisibility(View.VISIBLE);
            Paint overlayPaint = new Paint();
            overlayPaint.setBlendMode(BlendMode.OVERLAY);
            overlayPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
            mLogoView.setLayerType(LAYER_TYPE_HARDWARE, overlayPaint);
        }
    }

    private void enableTextBlur(View view, boolean z, int[] iArr, int[] iArr2) {
        if (z) {
            MiuiBlurUtils.setViewBlurMode(view, 3);
            for (int i = 0; i < iArr.length; i++) {
                MiuiBlurUtils.addBackgroundBlenderColor(view, iArr[i], iArr2[i]);
            }
        } else {
            MiuiBlurUtils.setViewBlurMode(view, 0);
            MiuiBlurUtils.clearBackgroundBlenderColor(view);
        }
    }
    public void stopLogoAnimation() {
        if (mAnimatorSet != null) {
            mAnimatorSet.end();
        }
    }

    public void setScrollValue(int value) {
        scrollValue = value;
    }

    public void setAnimation(int scrollY, View bgEffectView) {
        mAnimationController.startAnimation(scrollY, mIconLogoView, mTextLogoView, mIconLogoViewShade, mTextLogoViewShade, mUpdateText, mVersionLayout, bgEffectView);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeMessages(0);
    }
}
