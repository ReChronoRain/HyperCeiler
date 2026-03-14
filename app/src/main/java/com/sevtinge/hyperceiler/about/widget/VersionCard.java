package com.sevtinge.hyperceiler.about.widget;

import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.about.controller.AboutAnimationController;
import com.sevtinge.hyperceiler.common.view.CubicEaseOutInterpolater;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.utils.EggHelper;
import com.sevtinge.hyperceiler.utils.NotificationHelper;
import com.sevtinge.hyperceiler.utils.PermissionUtils;
import com.sevtinge.hyperceiler.utils.SettingsFeatures;

import java.security.SecureRandom;

import fan.cardview.HyperCardView;
import fan.core.utils.MiuiBlurUtils;
import fan.device.DeviceUtils;
import fan.internal.utils.ViewUtils;


public class VersionCard extends FrameLayout implements View.OnClickListener {
    private View mActionBar;
    private View mCardClickView;
    private AnimatorSet mAnimatorSet;
    private View mBgEffectView;
    private DecelerateInterpolator mDecelerateInterpolator;

    private View mIconView;
    private ImageView mIconImageView;
    private ImageView mTextIconImageView;

    private CubicEaseOutInterpolater mInterpolater;
    private boolean mNeedStartAnim = true;
    private boolean mNeedUpdate = true;
    ViewGroup mRootView;
    private HyperCardView mUpdateText;
    private ViewGroup mVersionLayout;
    private TextView mVersionNameText;
    private int mModeValue = 0;
    public AboutAnimationController mAboutAnimationController;
    private int mScrollValue = 0;
    private String mVersionName;
    private String mUpdateInfo = "";
    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (isAttachedToWindow()) {
                checkUpdate();
                if (mNeedUpdate) {
                    mNeedStartAnim = true;
                    mAboutAnimationController.iniData(getContext(), mNeedUpdate);
                    performLogoAnimation(false);
                    if (mScrollValue != 0) {
                        mAboutAnimationController.startButtonAnimation(mScrollValue, mUpdateText);
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
        mRootView = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.app_version_card, this, true);

        mIconView = findViewById(R.id.app_logo);
        mIconImageView = findViewById(R.id.app_logo_view);
        mTextIconImageView = findViewById(R.id.app_text_logo_view);
        mVersionNameText = findViewById(R.id.app_version_text);
        mVersionLayout = findViewById(R.id.version_layout);

        refreshVersionName();

        mUpdateText = findViewById(R.id.update_hint_text);
        int padding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4.0f, getResources().getDisplayMetrics()));
        mUpdateText.setContentPadding(padding, padding, padding, padding);

        mAnimatorSet = new AnimatorSet();
        mInterpolater = new CubicEaseOutInterpolater();
        mDecelerateInterpolator = new DecelerateInterpolator();

        mUpdateText.setOnClickListener(this);
        mUpdateText.setClickable(false);

        checkUpdate();
        applyUpdateButtonVisibility();

        mAboutAnimationController = new AboutAnimationController(getContext(), mNeedUpdate);

        if (!mNeedUpdate) {
            if (mScrollValue == 0) {
                mIconView.setAlpha(1.0f);
                mVersionLayout.setAlpha(1.0f);
            } else {
                if (mActionBar != null && mBgEffectView != null) {
                    mAboutAnimationController.startAnimation(mScrollValue, mIconView, mUpdateText, mVersionLayout, mActionBar, mBgEffectView);
                }
            }
            mHandler.sendEmptyMessageDelayed(0, 1500L);
        }
        setEgg();
        setLogoBlur();
    }

    private void setEgg() {
        mIconImageView.setOnClickListener(v -> {
            if (Math.random() >= 0.97) {
                String[] messages = getResources().getStringArray(R.array.logo_click_egg_messages);
                int index = new SecureRandom().nextInt(messages.length);
                String msg = messages[index];
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
        mIconImageView.setOnLongClickListener(v -> {
            if (Math.random() >= 0.6) {
                String[] messages = getResources().getStringArray(R.array.logo_click_egg_messages);
                int index = new SecureRandom().nextInt(messages.length);
                String msg = messages[index];

                if (!PermissionUtils.hasPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS)) {
                    Toast.makeText(getContext(), getResources().getString(R.string.logo_egg_Notification_tips), Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    NotificationHelper.createEggNotificationChannel(getContext());
                    // 发送通知
                    NotificationManager notificationManager = getSystemService(getContext(), NotificationManager.class);
                    int notificationId = "logo_channel_id".hashCode();
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), "logo_channel_id")
                        .setSmallIcon(R.drawable.ic_hyperceiler)
                        .setContentTitle(getResources().getString(R.string.logo_egg_NotificationName))
                        .setContentText(msg)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);
                    builder.addExtras(EggHelper.INSTANCE.focusBuild(msg, getContext()));

                    notificationManager.notify(notificationId, builder.build());

                    // 9 秒后自动关闭
                    new Handler(Looper.getMainLooper()).postDelayed(() -> notificationManager.cancel(notificationId), 9000);


                    return true;
                }

            }
            return true;
        });
    }

    public AboutAnimationController getAboutAnimationController() {
        return mAboutAnimationController;
    }

    public void checkUpdate() {
        mNeedUpdate = !TextUtils.isEmpty(getUpdateInfo());
    }

    private void applyUpdateButtonVisibility() {
        if (mNeedUpdate) {
            mUpdateText.setVisibility(View.VISIBLE);
        } else {
            mUpdateText.setVisibility(View.GONE);
            mUpdateText.setClickable(false);
        }
    }

    public void refreshBetaView(String str) {
        TextView textView = findViewById(R.id.version_text);
        if (textView != null) {
            String miuiBetaVersionInCard = "";
            if (!TextUtils.isEmpty(miuiBetaVersionInCard)) {
                textView.setText(miuiBetaVersionInCard);
            }
        }
    }

    public void refreshVersionName() {
        mVersionName = BuildConfig.VERSION_NAME + " | " + BuildConfig.BUILD_TYPE;
        if (!TextUtils.isEmpty(mVersionName) && mVersionNameText != null) {
            mVersionNameText.setText(mVersionName);
        }
    }

    public void refreshUpdateStatus(View actionBar, View bgEffectView) {
        boolean needUpdate = false;
        String updateInfo = getUpdateInfo();
        if ((TextUtils.isEmpty(updateInfo)) == mNeedUpdate) {
            mNeedStartAnim = true;
            mActionBar = actionBar;
            mBgEffectView = bgEffectView;
            if (!TextUtils.isEmpty(updateInfo)) {
                needUpdate = true;
            }
            mNeedUpdate = needUpdate;
            mRootView.removeAllViews();
            initView();
            invalidate();
        }
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(getContext(), "click update", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        performLogoAnimation(true);
    }

    public void performLogoAnimation(boolean z) {
        if (mNeedStartAnim && mNeedUpdate) {
            mUpdateText.setAccessibilityTraversalBefore(R.id.version_card_click_view);
            mActionBar.setAccessibilityTraversalBefore(R.id.update_hint_text);
            mNeedStartAnim = false;
            AnimatorSet animatorSet = new AnimatorSet();
            if (mScrollValue == 0) {
                if (z) {
                    animatorSet.playTogether(
                        ObjectAnimator.ofFloat(mIconView, "alpha", 0.0f, 1.0f),
                        ObjectAnimator.ofFloat(mVersionLayout, "alpha", 0.0f, 1.0f),
                        ObjectAnimator.ofFloat(mUpdateText, "alpha", 0.0f, AboutAnimationController.getUpdateButtonMaxAlpha()));
                } else {
                    animatorSet.playTogether(
                        ObjectAnimator.ofFloat(
                            mUpdateText,
                            "alpha",
                            0.0f,
                            AboutAnimationController.getUpdateButtonMaxAlpha()
                        )
                    );
                }
            }
            animatorSet.setDuration(800L);
            animatorSet.setInterpolator(mDecelerateInterpolator);
            Animator[] animators = new Animator[3];
            animators[0] = ObjectAnimator.ofFloat(mIconView, "translationY", SettingsFeatures.isSplitTabletDevice() ? DisplayUtils.dp2px(getContext(), -27.0f) : DisplayUtils.dp2px(getContext(), -30.0f));
            animators[1] = ObjectAnimator.ofFloat(mVersionLayout, "translationY", SettingsFeatures.isSplitTabletDevice() ? DisplayUtils.dp2px(getContext(), -27.0f) : DisplayUtils.dp2px(getContext(), -30.0f));
            animators[2] = animatorSet;
            mAnimatorSet.playTogether(animators);
            mAnimatorSet.setDuration(1000L);
            mAnimatorSet.setInterpolator(mInterpolater);
            mAnimatorSet.setStartDelay(100L);
            mAnimatorSet.start();
            mUpdateText.setClickable(true);
            mUpdateText.setOnClickListener(this);
        }
    }

    private void setLogoBlur() {
        if (!DeviceUtils.isMiuiLiteRom() && MiuiBlurUtils.isEnable() && MiuiBlurUtils.isEffectEnable(getContext())) {
            MiuiBlurUtils.setBackgroundBlur(mRootView, (int) ((getResources().getDisplayMetrics().density * 50.0f) + 0.5f));
            MiuiBlurUtils.setViewBlurMode(mRootView, 0);
            int[] logoColors = {
                getResources().getColor(R.color.app_about_logo_color1),
                getResources().getColor(R.color.app_about_logo_color2),
                getResources().getColor(R.color.app_about_logo_color3)
            };
            if (ViewUtils.isNightMode(getContext().getApplicationContext())) {
                mModeValue = 18;
            } else {
                mModeValue = 19;
            }
            enableTextBlur(mIconImageView, true, logoColors, new int[]{mModeValue, 100, 106});
            mIconImageView.setBackgroundResource(R.drawable.ic_hyperceiler_logo);

            enableTextBlur(mTextIconImageView, true, logoColors, new int[]{mModeValue, 100, 106});
            mTextIconImageView.setBackgroundResource(R.drawable.ic_text_logo);

            Log.d("MiuiVersionCard", "start logoBlur: ");
        } else {
            mIconImageView.setBackgroundResource(R.drawable.ic_hyperceiler_logo);
            mTextIconImageView.setBackgroundResource(R.drawable.ic_text_logo_lite);
        }
    }

    private void enableTextBlur(View view, boolean z, int[] iArr, int[] iArr2) {
        if (z) {
            MiuiBlurUtils.setViewBlurMode(view, 3);
            for (int i = 0; i < iArr.length; i++) {
                MiuiBlurUtils.addBackgroundBlenderColor(view, iArr[i], iArr2[i]);
            }
            return;
        }
        MiuiBlurUtils.setViewBlurMode(view, 0);
        MiuiBlurUtils.clearBackgroundBlendConfig(view);
    }

    public void setCardClickView(View view, View view2) {
        mActionBar = view2;
        mCardClickView = view;
        view.setOnClickListener(this);
        boolean needUpdate = !TextUtils.isEmpty(getUpdateInfo());
        mNeedUpdate = needUpdate;
        if (needUpdate) {
            mUpdateText.setAccessibilityTraversalBefore(R.id.version_card_click_view);
            view2.setAccessibilityTraversalBefore(R.id.update_hint_text);
            mCardClickView.setContentDescription(mVersionName + " , " + getContext().getString(R.string.app_version_update));
        } else {
            view2.setAccessibilityTraversalBefore(R.id.version_card_click_view);
            mCardClickView.setContentDescription(mVersionName);
        }
    }

    public void stopLogoAnimation() {
        if (mAnimatorSet != null) {
            mAnimatorSet.end();
        }
    }

    public void setScrollValue(int value) {
        mScrollValue = value;
    }

    public void setAnimation(int i, View view, View view2) {
        mAboutAnimationController.startAnimation(i, mIconView, mUpdateText, mVersionLayout, view, view2);
    }

    public void setUpdateInfo(@Nullable String updateInfo) {
        mUpdateInfo = updateInfo == null ? "" : updateInfo.trim();
        checkUpdate();
        applyUpdateButtonVisibility();
        if (mNeedUpdate) {
            mNeedStartAnim = true;
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeMessages(0);
    }

    public String getUpdateInfo() {
        return mUpdateInfo;
    }
}
