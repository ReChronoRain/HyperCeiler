package com.sevtinge.hyperceiler.provision.fragment;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.activity.DefaultActivity;
import com.sevtinge.hyperceiler.provision.renderengine.GlowController;
import com.sevtinge.hyperceiler.provision.renderengine.RenderViewLayout;
import com.sevtinge.hyperceiler.provision.utils.AnimHelper;
import com.sevtinge.hyperceiler.provision.utils.BlurUtils;
import com.sevtinge.hyperceiler.provision.utils.IOnFocusListener;
import com.sevtinge.hyperceiler.provision.utils.Utils;
import com.sevtinge.hyperceiler.provision.utils.ViewUtils;

import fan.animation.listener.TransitionListener;
import fan.core.utils.MiuiBlurUtils;
import fan.provision.OobeUtils;
import fan.transition.ActivityOptionsHelper;

public class StartupFragment extends BaseFragment implements IOnFocusListener {

    private static final String TAG = "StartupFragment";

    private record PickPageAnimationContext(
        DefaultActivity activity, View nextView, int locationX, int locationY, int width, int foregroundColor
    ) {
            private PickPageAnimationContext(
                @NonNull DefaultActivity activity,
                @NonNull View nextView,
                int locationX,
                int locationY,
                int width,
                int foregroundColor
            ) {
                this.activity = activity;
                this.nextView = nextView;
                this.locationX = locationX;
                this.locationY = locationY;
                this.width = width;
                this.foregroundColor = foregroundColor;
            }
        }

    private final boolean IS_SUPPORT_WELCOME_ANIM = !OobeUtils.isLiteOrLowDevice();
    private static final long PERMISSION_LAUNCH_TIMEOUT_MS = 1200L;

    private long lastClickTime = 0;
    private boolean mLaunchPending;
    private boolean mPermissionLaunchStarted;
    private PickPageAnimationContext mPendingAnimationContext;

    private ImageView mBackgroundImage;
    private View mMiuiEnterLayout;


    private View mGlowEffectView;
    private RenderViewLayout mRenderViewLayout;

    private ImageView mLogoImage;

    private ImageView mTextLogoImage;
    private View mLogoImageWrapper;

    private View mNextLayout;
    private View mNext;
    private ImageView mNextArrow;

    private GlowController mGlowController;

    private Handler mAnimationHandler;

    Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Runnable mDisplayOsAndoRunnable = () -> {
        restoreNextButtonState();
        if (mLogoImage != null) {
            mLogoImage.setVisibility(View.VISIBLE);
        }

        if (mLogoImageWrapper != null) {
            mLogoImageWrapper.setVisibility(View.VISIBLE);
        }
        AndroidLog.d(TAG, "displayOsAndoDelay");
    };
    private final Runnable mRestoreNextButtonRunnable = () -> {
        restoreNextButtonState();
        Utils.IS_START_ANIMA = false;
    };
    private final Runnable mPermissionLaunchFallbackRunnable = () -> {
        PickPageAnimationContext context = mPendingAnimationContext;
        if (!mLaunchPending || mPermissionLaunchStarted || context == null) return;
        AndroidLog.w(TAG, "permission page capture timed out, using fallback animation");
        launchPermissionPickPage(context, null);
    };

    private final View.OnClickListener mNextClickListener = v -> {
        if (mLaunchPending) {
            AndroidLog.d(TAG, "permission page launch is already pending");
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (Math.abs(jCurrentTimeMillis - lastClickTime) < 2000) {
            AndroidLog.d(TAG, "click too fast");
            return;
        }
        lastClickTime = jCurrentTimeMillis;
        AndroidLog.d(TAG, "click next button");
        FragmentActivity activity = getActivity();
        View nextView = mNext;
        if (!(activity instanceof DefaultActivity defaultActivity) || nextView == null) {
            AndroidLog.w(TAG, "click next button: activity or next view is unavailable");
            return;
        }
        mLaunchPending = true;
        setNextButtonInteractionEnabled(false);
        Utils.isFirstBoot = false;
        PickPageAnimationContext pickPageAnimationContext = buildPickPageAnimationContext(defaultActivity, nextView);
        mPendingAnimationContext = pickPageAnimationContext;
        defaultActivity.run(-1);
        mMainHandler.removeCallbacks(mPermissionLaunchFallbackRunnable);
        mMainHandler.postDelayed(
            mPermissionLaunchFallbackRunnable,
            PERMISSION_LAUNCH_TIMEOUT_MS
        );
        enterLanguagePickPage(pickPageAnimationContext);
    };

    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAnimationHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 5) {
                    AndroidLog.i(TAG, " isFirstBoot value set");
                    Utils.isFirstBoot = false;
                }
            }
        };
    }


    @Override
    protected int getLayoutId() {
        return R.layout.provision_startup_layout;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mLogoImage = view.findViewById(R.id.logo_image);

        mTextLogoImage = view.findViewById(R.id.text_logo_image);
        mLogoImageWrapper = view.findViewById(R.id.logo_image_wrapper);
        mNextLayout = view.findViewById(R.id.next_layout);
        mNext = view.findViewById(R.id.next);
        mNextArrow = view.findViewById(R.id.next_arrow);
        mBackgroundImage = view.findViewById(R.id.background_image);
        mMiuiEnterLayout = view.findViewById(R.id.miui_enter_layout);

        if (!IS_SUPPORT_WELCOME_ANIM) {
            AndroidLog.i(TAG, "not support anim");
            mBackgroundImage.setImageResource(R.drawable.provision_logo_image_bg);
            mLogoImage.setImageResource(R.drawable.provision_logo_image_lite);
            mTextLogoImage.setImageResource(R.drawable.provision_text_logo_image_lite);
            setNextBackground();
        } else {
            mRenderViewLayout = view.findViewById(R.id.render_view_layout);
            mGlowEffectView = new View(requireContext());
            mRenderViewLayout.attachView(mGlowEffectView, 0.2f, -16777216);
            mGlowController = new GlowController(mGlowEffectView);
            if (Utils.isBlurEffectEnabled(getContext())) {
                AndroidLog.i(TAG, " MiuiBlur EffectEnabled ");
                MiuiBlurUtils.setBackgroundBlur(mMiuiEnterLayout, (int) ((getResources().getDisplayMetrics().density * 50.0f) + 0.5f));
                MiuiBlurUtils.setViewBlurMode(mMiuiEnterLayout, 0);
                BlurUtils.setupViewBlur(mLogoImage, true, new int[]{-867546550, -11579569, -15011328}, new int[]{19, 100, 106});

                BlurUtils.setupViewBlur(mTextLogoImage, true, new int[]{-867546550, -11579569, -15011328}, new int[]{19, 100, 106});
                mLogoImage.setImageResource(R.drawable.provision_logo_image);

                mTextLogoImage.setImageResource(R.drawable.provision_text_logo_image);
                BlurUtils.setupViewBlur(mNext, true, new int[]{-13750738, -15011328}, new int[]{100, 106});
                mNext.setBackgroundResource(R.drawable.provision_next);
                mNextArrow.setVisibility(View.VISIBLE);
                mNextArrow.setImageResource(R.drawable.provision_icon_arrow);
            } else {
                AndroidLog.i(TAG, " MiuiBlur not EffectEnabled ");
                mLogoImage.setImageResource(R.drawable.provision_logo_image_lite);
                mTextLogoImage.setImageResource(R.drawable.provision_text_logo_image_lite);
                setNextBackground();
            }
        }

        if (Utils.IS_START_ANIMA) {
            syncNextButtonState();
        }

        mNextLayout.setOnClickListener(mNextClickListener);
        mNext.setOnClickListener(mNextClickListener);
        mNextArrow.setOnClickListener(mNextClickListener);


        if (IS_SUPPORT_WELCOME_ANIM && mLogoImageWrapper != null && mLogoImage != null &&
                mNextLayout != null && Utils.isFirstBoot) {
            AndroidLog.i(TAG, "SUPPORT_WELCOME_ANIM");
            mLogoImage.setVisibility(View.INVISIBLE);
            mLogoImageWrapper.setVisibility(View.INVISIBLE);
            hideNextButtonState();
        }
    }

    private void setNextBackground() {
        Bitmap bitmap = Utils.getCacheBitmap(isNeedRotation());
        if (bitmap != null) {
            mNext.setBackground(new BitmapDrawable(getResources(), bitmap));
        } else {
            mNext.setBackgroundResource(R.drawable.provision_next_lite);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        lastClickTime = 0;
        AndroidLog.d(TAG, "onStart");
        if (IS_SUPPORT_WELCOME_ANIM && !Utils.isFirstBoot && mGlowController != null) {
            mGlowController.start(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLaunchPending) return;
        lastClickTime = 0;
        syncNextButtonState();
    }

    public void onReturnToStartup() {
        mMainHandler.removeCallbacks(mPermissionLaunchFallbackRunnable);
        mLaunchPending = false;
        mPermissionLaunchStarted = false;
        mPendingAnimationContext = null;
        lastClickTime = 0;
        Utils.IS_START_ANIMA = false;
        restoreNextButtonState();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (IS_SUPPORT_WELCOME_ANIM) {
            AndroidLog.i(TAG, " onWindowFocusChanged " + hasFocus + " isFirst " + Utils.isFirstBoot);
            if (Utils.isFirstBoot) {
                if (mGlowController != null) {
                    mGlowController.start(true);
                    mGlowController.setCircleYOffsetWithView(mLogoImageWrapper, mRenderViewLayout);
                }

                if (mLogoImage != null) {
                    mLogoImage.setVisibility(View.VISIBLE);
                    AnimHelper.startPageLogoAnim(mLogoImage);
                }

                if (mLogoImageWrapper != null) {
                    mLogoImageWrapper.setVisibility(View.VISIBLE);
                    AnimHelper.startPageLogoAnim(mLogoImageWrapper);
                }
                if (mNextLayout != null) {
                    mNextLayout.setVisibility(View.VISIBLE);
                    AnimHelper.startPageBtnAnim(mNextLayout, new TransitionListener() {
                        @Override
                        public void onComplete(Object obj) {
                            super.onComplete(obj);
                            restoreNextButtonState();
                            AndroidLog.d(TAG, "onComplete: mNextLayout restored");
                        }
                    });
                }
                resetFirstStart();
                displayOsAndoDelay();
            }
        }
    }

    /*@Override
    public void onStop() {
        super.onStop();
        AndroidLog.d(TAG, "onStop");
        if (mGlowController != null) {
            mGlowController.stop();
            AndroidLog.d(TAG, "GlowController: stop");
        }
    }*/

    private void resetFirstStart() {
        if (mAnimationHandler != null) {
            mAnimationHandler.removeMessages(5);
            mAnimationHandler.sendEmptyMessageDelayed(5, Utils.isFoldDevice() ? 3000L : 0L);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AndroidLog.i(TAG, "onDestroy");
        mMainHandler.removeCallbacks(mDisplayOsAndoRunnable);
        mMainHandler.removeCallbacks(mRestoreNextButtonRunnable);
        mMainHandler.removeCallbacks(mPermissionLaunchFallbackRunnable);
        if (mAnimationHandler != null) {
            mAnimationHandler.removeCallbacksAndMessages(null);
        }
    }

    private void displayOsAndoDelay() {
        mMainHandler.removeCallbacks(mDisplayOsAndoRunnable);
        mMainHandler.postDelayed(mDisplayOsAndoRunnable, 2500L);
    }

    private void syncNextButtonState() {
        mMainHandler.removeCallbacks(mRestoreNextButtonRunnable);
        if (IS_SUPPORT_WELCOME_ANIM && Utils.isFirstBoot) {
            return;
        }
        if (!Utils.IS_START_ANIMA) {
            restoreNextButtonState();
            return;
        }
        hideNextButtonState();
        mMainHandler.postDelayed(mRestoreNextButtonRunnable, 505L);
    }

    private void hideNextButtonState() {
        if (mNextLayout != null) {
            mNextLayout.setVisibility(View.INVISIBLE);
        }
        setNextButtonInteractionEnabled(false);
    }

    private void restoreNextButtonState() {
        if (mLaunchPending) return;
        if (mNextLayout != null) {
            mNextLayout.setVisibility(View.VISIBLE);
        }
        setNextButtonInteractionEnabled(true);
    }

    private void setNextButtonInteractionEnabled(boolean enabled) {
        if (mNextLayout != null) {
            mNextLayout.setEnabled(enabled);
            mNextLayout.setClickable(enabled);
        }
        if (mNext != null) {
            mNext.setEnabled(enabled);
            mNext.setClickable(enabled);
        }
        if (mNextArrow != null) {
            mNextArrow.setEnabled(enabled);
            mNextArrow.setClickable(enabled);
        }
    }

    @NonNull
    private PickPageAnimationContext buildPickPageAnimationContext(@NonNull DefaultActivity activity, @NonNull View nextView) {
        int[] location = new int[2];
        nextView.getLocationInWindow(location);
        int width = ((nextView.getWidth() - nextView.getPaddingRight()) - nextView.getPaddingLeft()) / 2;
        return new PickPageAnimationContext(
            activity,
            nextView,
            location[0],
            location[1],
            width,
            getAnimForeGroundColor(activity)
        );
    }

    private void enterLanguagePickPage(@NonNull PickPageAnimationContext animationContext) {
        if (animationContext.activity.isFinishing() || animationContext.activity.isDestroyed()) {
            AndroidLog.w(TAG, "enterLanguagePickPage: activity is unavailable");
            return;
        }
        ViewUtils.captureRoundedBitmap(
            animationContext.activity,
            animationContext.nextView,
            mMainHandler,
            bitmap -> launchPermissionPickPage(animationContext, bitmap)
        );
    }


    private void launchPermissionPickPage(@NonNull PickPageAnimationContext animationContext, @Nullable Bitmap bitmap) {
        if (!mLaunchPending || mPermissionLaunchStarted) {
            recycleBitmap(bitmap);
            return;
        }
        DefaultActivity activity = animationContext.activity;
        if (activity.isFinishing() || activity.isDestroyed()) {
            AndroidLog.w(TAG, "launchPermissionPickPage: activity unavailable after bitmap capture");
            recycleBitmap(bitmap);
            return;
        }

        ActivityOptions activityOptions = bitmap != null
            ? buildPickPageAnimation(animationContext, bitmap)
            : buildFallbackPageAnimation(animationContext);

        if (activityOptions == null) {
            if (bitmap == null) {
                AndroidLog.d(TAG, "roundedBitmap is null");
            } else {
                AndroidLog.w(TAG, "launchPermissionPickPage: scale-up animation is unavailable, launching without ActivityOptions");
            }
            recycleBitmap(bitmap);
            Utils.IS_START_ANIMA = false;
        } else if (bitmap == null) {
            Utils.IS_START_ANIMA = false;
        }

        mPermissionLaunchStarted = true;
        mMainHandler.removeCallbacks(mPermissionLaunchFallbackRunnable);
        try {
            AndroidLog.d(TAG, "launchPermissionPickPage: start permission activity");
            activity.enterCurrentState(
                activityOptions == null ? null : activityOptions.toBundle()
            );
            if (mNextLayout != null) {
                mNextLayout.setVisibility(View.INVISIBLE);
            }
        } catch (RuntimeException exception) {
            AndroidLog.e(TAG, "launchPermissionPickPage failed: " + exception);
            mPermissionLaunchStarted = false;
            activity.run(0);
            onReturnToStartup();
        }
    }

    @Nullable
    private ActivityOptions buildFallbackPageAnimation(
        @NonNull PickPageAnimationContext animationContext
    ) {
        try {
            return ActivityOptions.makeScaleUpAnimation(
                animationContext.nextView,
                0,
                0,
                animationContext.nextView.getWidth(),
                animationContext.nextView.getHeight()
            );
        } catch (RuntimeException exception) {
            AndroidLog.w(TAG, "fallback scale-up animation is unavailable: " + exception);
            return null;
        }
    }

    private void recycleBitmap(@Nullable Bitmap bitmap) {
        if (bitmap != null && bitmap != Utils.CACHE_BITMAP && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    @Nullable
    private ActivityOptions buildPickPageAnimation(@NonNull PickPageAnimationContext animationContext, @NonNull Bitmap bitmap) {
        if (animationContext.width <= 0) {
            AndroidLog.w(TAG, "buildPickPageAnimation: next view width is invalid");
            return null;
        }
        View nextLayout = mNextLayout;
        ActivityOptions activityOptionsMakeScaleUpAnim = ActivityOptionsHelper.makeScaleUpAnim(
            animationContext.nextView,
            bitmap,
            animationContext.locationX,
            animationContext.locationY,
            animationContext.width,
            animationContext.foregroundColor,
            1.0f,
            mMainHandler,
            buildExitStartedCallback(nextLayout),
            buildExitFinishCallback(nextLayout),
            null,
            null,
            102
        );
        if (activityOptionsMakeScaleUpAnim == null) {
            return null;
        }
        Utils.LOCATION_X = animationContext.locationX;
        Utils.LOCATION_Y = animationContext.locationY;
        Utils.CACHE_BITMAP = bitmap;
        Utils.IS_RTL = isRtl();
        Utils.IS_START_ANIMA = true;
        return activityOptionsMakeScaleUpAnim;
    }

    @NonNull
    private Runnable buildExitStartedCallback(@Nullable View nextLayout) {
        return () -> {
            if (nextLayout != null) {
                nextLayout.setVisibility(View.INVISIBLE);
                AndroidLog.d(TAG, "exitStartedCallback: " + nextLayout.getVisibility());
            }
        };
    }

    @NonNull
    private Runnable buildExitFinishCallback(@Nullable View nextLayout) {
        return () -> {
            if (nextLayout != null) {
                nextLayout.setVisibility(mLaunchPending ? View.INVISIBLE : View.VISIBLE);
                AndroidLog.d(TAG, "exitFinishCallback: " + nextLayout.getVisibility());
            }
        };
    }

    private int isRtl() {
        return Utils.isRTL() ? 1 : 2;
    }


    private boolean isNeedRotation() {
        return isRtl() != Utils.IS_RTL;
    }

    private int getAnimForeGroundColor(@NonNull Context context) {
        if (Utils.isBlurEffectEnabled(context)) {
            return context.getColor(R.color.anim_foreground_color_blur_enable);
        }
        return context.getColor(R.color.anim_foreground_color);
    }


}
