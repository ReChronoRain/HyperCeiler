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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.provision.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.activity.DefaultActivity;
import com.sevtinge.hyperceiler.provision.activity.PermissionSettingsActivity;
import com.sevtinge.hyperceiler.provision.renderengine.GlowController;
import com.sevtinge.hyperceiler.provision.renderengine.RenderViewLayout;
import com.sevtinge.hyperceiler.provision.utils.AnimHelper;
import com.sevtinge.hyperceiler.provision.utils.BlurUtils;
import com.sevtinge.hyperceiler.provision.utils.IOnFocusListener;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;
import com.sevtinge.hyperceiler.provision.utils.ViewUtils;

import fan.animation.Folme;
import fan.animation.listener.TransitionListener;
import fan.core.utils.MiuiBlurUtils;
import fan.internal.utils.LiteUtils;
import fan.navigation.utils.Utils;
import fan.transition.ActivityOptionsHelper;

public class StartupFragment extends BaseFragment implements IOnFocusListener {

    private static final String TAG = "StartupFragment";

    private static final int MSG_STOP_LOGO = 2;
    private static final int MSG_RELEASE_LOGO = 3;

    public boolean IS_START_ANIMA = false;
    private final boolean IS_SUPPORT_WELCOME_ANIM = !OobeUtils.isMiuiVersionLite();

    private long lastClickTime = 0;

    private ImageView mBackgroundImage;

    private View mLogoImageWrapper;
    private ImageView mLogoImage;


    private View mNext;
    private View mNextLayout;
    private ImageView mNextArrow;

    private Handler mAnimationHandler;

    private View mGlowEffectView;
    private View mMiuiEnterLayout;
    private GlowController mGlowController;
    private RenderViewLayout mRenderViewLayout;

    Handler mMainHandler = new Handler(Looper.getMainLooper());
    Runnable mExitStartedCallback = () -> {
        if (mNextLayout != null) {
            mNextLayout.setVisibility(View.INVISIBLE);
            Log.d(TAG, "exitStartedCallback: " + mNextLayout.getVisibility());
        }
    };
    Runnable mExitFinishCallback = () -> {
        if (mNextLayout != null) {
            mNextLayout.setVisibility(View.VISIBLE);
            Log.d(TAG, "exitFinishCallback: " + mNextLayout.getVisibility());
        }
        FragmentActivity activity = getActivity();
        if (activity != null) {
            String topActivityClassName = OobeUtils.getTopActivityClassName(activity);
            if (topActivityClassName == null || !topActivityClassName.contains(DefaultActivity.class.getSimpleName())) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                Fragment fragment = fragmentManager.findFragmentByTag(StartupFragment.class.getSimpleName());
                if (fragment != null) {
                    FragmentTransaction beginTransaction = fragmentManager.beginTransaction();
                    beginTransaction.remove(fragment);
                    beginTransaction.commitAllowingStateLoss();
                    Log.d(StartupFragment.TAG, "remove startup fragment finished");
                }
            } else {
                Log.d(TAG, "topActivity is DefaultActivity");
            }
        } else {
            Log.w(TAG, "exitFinishCallback: getActivity() is null, fragment may be detached");
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAnimationHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 5) {
                    Log.i(TAG, " isFirstBoot value set");
                    OobeUtils.isFirstBoot = false;
                }
            }
        };
    }

    @Override
    protected int getLayoutId() {
        return R.layout.provision_startup_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLogoImage = view.findViewById(R.id.logo_image);
        mLogoImageWrapper = view.findViewById(R.id.logo_image_wrapper);

        mNextLayout = view.findViewById(R.id.next_layout);
        mNext = view.findViewById(R.id.next);
        mNextArrow = view.findViewById(R.id.next_arrow);
        mBackgroundImage = view.findViewById(R.id.background_image);
        mMiuiEnterLayout = view.findViewById(R.id.miui_enter_layout);

        /*if (mNextArrow != null) {
            mNextArrow.setImportantForAccessibility(MSG_STOP_LOGO);
        }*/

        if (IS_SUPPORT_WELCOME_ANIM) {
            mRenderViewLayout = view.findViewById(R.id.render_view_layout);
            mGlowEffectView = new View(getActivity());
            mRenderViewLayout.attachView(mGlowEffectView, 0.2f, -16777216);
            mGlowController = new GlowController(mGlowEffectView);
            if (OobeUtils.isBlurEffectEnabled(getContext())) {
                Log.i(TAG, " MiuiBlur EffectEnabled ");
                MiuiBlurUtils.setBackgroundBlur(mMiuiEnterLayout, (int) ((getResources().getDisplayMetrics().density * 50.0f) + 0.5f));
                MiuiBlurUtils.setViewBlurMode(mMiuiEnterLayout, 0);
                BlurUtils.setupViewBlur(mLogoImage, true, new int[]{-867546550, -11579569, -15011328}, new int[]{19, 100, 106});
                mLogoImage.setImageResource(R.drawable.provision_logo_image);
                BlurUtils.setupViewBlur(mNext, true, new int[]{-13750738, -15011328}, new int[]{100, 106});
                mNext.setBackgroundResource(R.drawable.provision_next);
                mNextArrow.setVisibility(View.VISIBLE);
                mNextArrow.setImageResource(R.drawable.provision_icon_arrow);
            } else {
                Log.i(TAG, " MiuiBlur not EffectEnabled ");
                mLogoImage.setImageResource(R.drawable.provision_logo_image_lite);
                setNextBackground();
            }
        } else {
            Log.i(TAG, "not support anim");
            mBackgroundImage.setImageResource(R.drawable.provision_logo_image_bg);
            mLogoImage.setImageResource(R.drawable.provision_logo_image_lite);
            setNextBackground();
        }

        if (IS_START_ANIMA) {
            mNextLayout.setVisibility(View.INVISIBLE);
            mMainHandler.postDelayed(() -> {
                if (mNextLayout.getVisibility() == View.INVISIBLE) {
                    mNextLayout.setVisibility(View.VISIBLE);
                }
                IS_START_ANIMA = false;
            }, 505L);
        }

        mNextLayout.setOnClickListener(v -> {
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (Math.abs(jCurrentTimeMillis - lastClickTime) < 2000) {
                Log.d(TAG, "click too fast");
                return;
            }
            lastClickTime = jCurrentTimeMillis;
            Log.d(TAG, "click next button");
            //recordPageStayTime();
            DefaultActivity defaultActivity = (DefaultActivity) getActivity();
            OobeUtils.isFirstBoot = false;
            defaultActivity.run(-1);
            //OTHelper.rdCountEvent(Constants.KEY_CLICK_FIRST_PAGE_START);
            //BoostHelper.getInstance().boostDefault(this.mNext);
            enterLanguagePickPage();
        });

        if (IS_SUPPORT_WELCOME_ANIM && mLogoImageWrapper != null &&
            mNextLayout != null && OobeUtils.isFirstBoot) {
            Log.i(TAG, "SUPPORT_WELCOME_ANIM");
            mLogoImageWrapper.setVisibility(View.INVISIBLE);
            mNextLayout.setVisibility(View.INVISIBLE);
            mNextLayout.setEnabled(false);
        }
    }

    private boolean isNeedRotation() {
        return isRtl() != IS_RTL;
    }

    public static Bitmap CACHE_BITMAP = null;

    private void setNextBackground() {
        Bitmap bitmapRotateBitmap180 = CACHE_BITMAP;
        if (bitmapRotateBitmap180 != null) {
            if (isNeedRotation()) {
                bitmapRotateBitmap180 = ViewUtils.rotateBitmap180(CACHE_BITMAP);
            }
            mNext.setBackground(new BitmapDrawable(getResources(), bitmapRotateBitmap180));
            return;
        }
        mNext.setBackgroundResource(R.drawable.provision_next_lite);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (IS_SUPPORT_WELCOME_ANIM && !OobeUtils.isFirstBoot && mGlowController != null) {
            mGlowController.start(false);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (IS_SUPPORT_WELCOME_ANIM) {
            Log.i(TAG, " onWindowFocusChanged " + hasFocus + " isFirst " + OobeUtils.isFirstBoot);
            if (OobeUtils.isFirstBoot) {
                if (mGlowController != null) {
                    mGlowController.start(true);
                    mGlowController.setCircleYOffsetWithView(mLogoImageWrapper, mRenderViewLayout);
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
                            mNextLayout.setEnabled(true);
                            Log.d(TAG, "onComplete: mNextLayout setEnabled");
                        }
                    });
                }
                resetFirstStart();
                displayOsLogoDelay();
            }
        }
    }

    private void resetFirstStart() {
        if (mAnimationHandler != null) {
            mAnimationHandler.removeMessages(5);
            mAnimationHandler.sendEmptyMessageDelayed(5, 3000L);
        }
    }

    private void displayOsLogoDelay() {
        new Handler().postDelayed(() -> {
            if (mNextLayout != null) {
                mNextLayout.setVisibility(View.VISIBLE);
                mNextLayout.setEnabled(true);
            }
            if (mLogoImageWrapper != null) {
                mLogoImageWrapper.setVisibility(View.VISIBLE);
            }
            Log.d(TAG, "displayOsLogoDelay");

        }, 2500L);
    }

    public void onKeyDownChild(int keyCode, KeyEvent event) {

    }

    private void enterLanguagePickPage() {
        ViewUtils.captureRoundedBitmap(getActivity(), mNext, mMainHandler, new ViewUtils.RoundedBitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                if (bitmap != null) {
                    startPickPage(bitmap);
                    return;
                }
                Log.d(TAG, "roundedBitmap is null ");
                Intent intent = new Intent();
                intent.setClass(getActivity(), PermissionSettingsActivity.class);
                intent.putExtra("isShowDelayAnim", true);
                getActivity().startActivityForResult(intent, 0);
            }
        });
    }

    public static int LOCATION_X = -1;
    public static int LOCATION_Y = -1;
    public static int IS_RTL = 0;

    private void startPickPage(Bitmap bitmap) {
        int animForeGroundColor = getAnimForeGroundColor();
        int width = ((mNext.getWidth() - mNext.getPaddingRight()) - mNext.getPaddingLeft()) / 2;
        int[] iArr = new int[2];
        mNext.getLocationInWindow(iArr);
        ActivityOptions activityOptionsMakeScaleUpAnim = ActivityOptionsHelper.makeScaleUpAnim(this.mNext, bitmap, iArr[0], iArr[1], width, animForeGroundColor, 1.0f, mMainHandler, mExitStartedCallback, mExitFinishCallback, null, null, 102);
        LOCATION_X = iArr[0];
        LOCATION_Y = iArr[1];
        CACHE_BITMAP = bitmap;
        IS_RTL = isRtl();
        IS_START_ANIMA = true;
        Intent intent = new Intent();
        intent.setClass(getActivity(), PermissionSettingsActivity.class);
        intent.putExtra("isShowDelayAnim", true);
        requireActivity().startActivityForResult(intent, 0, activityOptionsMakeScaleUpAnim.toBundle());
    }

    private int isRtl() {
        return OobeUtils.isRTL() ? 1 : 2;
    }

    private int getAnimForeGroundColor() {
        if (OobeUtils.isBlurEffectEnabled(getContext())) {
            return getResources().getColor(R.color.anim_foreground_color_blur_enable);
        }
        return getResources().getColor(R.color.anim_foreground_color);
    }
}
