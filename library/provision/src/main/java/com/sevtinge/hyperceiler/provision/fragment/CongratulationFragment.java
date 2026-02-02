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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.activity.DefaultActivity;
import com.sevtinge.hyperceiler.provision.renderengine.GlowController;
import com.sevtinge.hyperceiler.provision.renderengine.RenderViewLayout;
import com.sevtinge.hyperceiler.provision.utils.ActivityOptionsUtils;
import com.sevtinge.hyperceiler.provision.utils.BlurUtils;
import com.sevtinge.hyperceiler.provision.utils.IOnFocusListener;
import com.sevtinge.hyperceiler.provision.utils.Utils;

import fan.animation.FolmeEase;
import fan.animation.IStateStyle;
import fan.animation.ITouchStyle;
import fan.animation.base.AnimConfig;
import fan.animation.controller.AnimState;
import fan.animation.listener.TransitionListener;
import fan.animation.property.ViewProperty;
import fan.provision.OobeUtils;

import fan.animation.Folme;
import fan.core.utils.MiuiBlurUtils;
import fan.internal.utils.LiteUtils;
import fan.os.Build;

public class CongratulationFragment extends BaseFragment implements IOnFocusListener {

    private static final String TAG = "Provision:CongratulationFragment";

    private final int DELAY_TIME = !OobeUtils.isLiteOrLowDevice() ? 10000 : 15000;

    private boolean IS_SUPPORT_NEW_PROVISION_STRATEGY;

    private long endTime;
    private long initTime;

    private boolean isFirstBoot = true;
    private boolean isComplete;

    private View mGlowEffectView;
    private View mContentView;
    private ImageView mLogoImage;
    private View mLogoImageWrapper;
    private View mNext;
    private View mNextView;
    private TextView mSystemStateText;
    private RenderViewLayout mRenderViewLayout;

    private GlowController mGlowController;

    private final Handler mHandler = new Handler();

    @Override
    protected int getLayoutId() {
        return R.layout.provision_congratulation_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initTime = System.currentTimeMillis();
        initSetupWizardAnimConnection();
        initView(view);
        initBackGround();
        setupBlurBackground();
        displayOSLogoDelay();
    }

    private void initView(View view) {
        mRenderViewLayout = view.findViewById(R.id.render_view_layout);
        mContentView = view.findViewById(R.id.content_view);
        mLogoImageWrapper = view.findViewById(R.id.logo_image_wrapper);
        mLogoImage = view.findViewById(R.id.logo_image);
        mSystemStateText = view.findViewById(R.id.system_state_text);
        mNextView = view.findViewById(R.id.next);
        mNext = view.findViewById(R.id.btn_bg);
        mSystemStateText.setText(IS_SUPPORT_NEW_PROVISION_STRATEGY ?
            R.string.provision_system_preparing :
            R.string.provision_system_complete
        );
        if (Utils.IS_SUPPORT_ANIM) {
            mLogoImageWrapper.setVisibility(View.GONE);
        }
        mNextView.setVisibility(View.GONE);
        mNextView.setEnabled(false);
        Folme.use(mNextView).touch().setScale(1.0f, new ITouchStyle.TouchType[0]).handleTouchOf(this.mNextView, new AnimConfig[0]);
        mNextView.setOnClickListener(v -> {
            startHome();
            startPageAnim();
        });
    }

    private void initBackGround() {
        setShaderBackGround();
        setBackGroundNoAnim();
    }

    private void setShaderBackGround() {
        if (Utils.IS_SUPPORT_ANIM && mContentView != null) {
            mContentView.setBackground(null);
            if (mRenderViewLayout != null) {
                mRenderViewLayout.setVisibility(View.VISIBLE);
                mGlowEffectView = new View(requireContext());
                mRenderViewLayout.attachView(mGlowEffectView, 0.2f, -16777216);
                mGlowController = new GlowController(mGlowEffectView);
                mGlowController.start(false);
            }
        }
    }

    private void setBackGroundNoAnim() {
        if (Utils.IS_SUPPORT_ANIM || mContentView == null) return;
        mContentView.setBackgroundResource(R.drawable.provision_logo_image_bg);
    }

    private void setupBlurBackground() {
        if (MiuiBlurUtils.isEnable() && !LiteUtils.isCommonLiteStrategy() &&
            MiuiBlurUtils.isEffectEnable(requireContext())) {
            MiuiBlurUtils.setBackgroundBlur(mContentView, (int) ((getResources().getDisplayMetrics().density * 50.0f) + 0.5f));
            MiuiBlurUtils.setViewBlurMode(mContentView, 0);
            if (mLogoImage != null) {
                setupViewBlur(mLogoImage, true, new int[]{-867546550, -11579569, -15011328}, new int[]{19, 100, 106});
                mLogoImage.setImageResource(R.drawable.provision_logo_image);
            }
            if (mNext != null) {
                setupViewBlur(mNext, true, new int[]{-12763843, -15021056}, new int[]{100, 106});
                mNext.setBackgroundResource(R.drawable.provision_next_btn_background);
            }
            if (mSystemStateText == null) return;
            setupViewBlur(mSystemStateText, true, new int[]{-869915098, -1724697805}, new int[]{19, 3});
        }
    }

    private void setupViewBlur(View view, boolean z, int[] iArr, int[] iArr2) {
        if (view == null) return;
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

    private void displayOSLogoDelay() {
        mContentView.postDelayed(() -> {
            if (mLogoImageWrapper != null) {
                startLogoAnim(mLogoImageWrapper);
                mLogoImageWrapper.setVisibility(View.VISIBLE);
            }
            if (mSystemStateText != null) {
                mSystemStateText.setText(R.string.provision_system_complete);
            }
            if (mNextView != null) {
                startBtnAnim(mNextView);
                mNextView.setVisibility(View.VISIBLE);
            }
            isComplete = true;
            endTime = System.currentTimeMillis();
            Log.d("ProvisionCongratulationActivity", "displayOSLogoDelay: " + (endTime - initTime) + " endTime " + endTime);
        }, DELAY_TIME);
    }

    private void initSetupWizardAnimConnection() {
        Log.d("ProvisionCongratulationActivity", "onSetupComplete");
        refreshCompletedView();
        IS_SUPPORT_NEW_PROVISION_STRATEGY = false;
    }

    private void refreshCompletedView() {
        requireActivity().runOnUiThread(() -> {
            if (mSystemStateText != null) {
                mSystemStateText.setText(R.string.provision_system_complete);
            }
            if (mNextView != null) {
                startBtnAnim(mNextView);
                mNextView.setVisibility(View.VISIBLE);
            }
            isComplete = true;
        });
    }

    private void startAnim() {
        isFirstBoot = false;
        if (Utils.IS_SUPPORT_ANIM && mLogoImageWrapper != null) {
            startLogoAnim(mLogoImageWrapper);
        }
        if (!IS_SUPPORT_NEW_PROVISION_STRATEGY && mNextView != null) {
            startBtnAnim(mNextView);
            isComplete = true;
        }
    }

    private void startLogoAnim(View view) {
        if (view == null || view.getVisibility() == View.VISIBLE) return;
        view.setVisibility(View.VISIBLE);
        AnimState animState = new AnimState("start");
        AnimState alphaState = animState.add(ViewProperty.ALPHA, 0.0d);
        AnimState translationYState = alphaState.add(ViewProperty.TRANSLATION_Y, 100.0d);

        AnimState animState2 = new AnimState("end");
        AnimState alphaState2 = animState2.add(ViewProperty.ALPHA, 1.0d);
        AnimState translationYState2 = alphaState2.add(ViewProperty.TRANSLATION_Y, 0.0d);

        AnimConfig config = new AnimConfig();
        config.setEase(FolmeEase.quartOut(1500L));

        Folme.use(view).state().setTo(translationYState).to(translationYState2, config);
    }

    private void startBtnAnim(View view) {
        if (view == null) return;

        view.postDelayed(() -> {
            view.setEnabled(true);
            Log.d("ProvisionCongratulationActivity", "startBtnAnim: mNextView setEnabled");
        }, 2000L);
        if (view.getVisibility() == View.VISIBLE) return;
        view.setVisibility(View.VISIBLE);
        AnimConfig delay = new AnimConfig().setEase(FolmeEase.sinOut(450L)).setDelay(1000L);
        delay.addListeners(new TransitionListener() {
            @Override
            public void onComplete(Object toTag) {
                super.onComplete(toTag);
                view.setEnabled(true);
                Log.d("ProvisionCongratulationActivity", "onComplete: mNextView setEnabled");
            }
        });
        IStateStyle state = Folme.use(view).state();
        ViewProperty viewProperty = ViewProperty.ALPHA;
        state.setTo(viewProperty, Float.valueOf(0.0f)).to(viewProperty, Float.valueOf(1.0f), delay);
    }

    private void startPageAnim() {
        AnimState animState = new AnimState("start");
        ViewProperty viewProperty = ViewProperty.ALPHA;
        AnimState add = animState.add(viewProperty, 1.0d);
        ViewProperty viewProperty2 = ViewProperty.SCALE_X;
        AnimState add2 = add.add(viewProperty2, 1.0d);
        ViewProperty viewProperty3 = ViewProperty.SCALE_Y;
        AnimState add3 = add2.add(viewProperty3, 1.0d);
        AnimState add4 = new AnimState("end").add(viewProperty, 0.0d).add(viewProperty2, 0.8d).add(viewProperty3, 0.8d);
        AnimConfig animConfig = new AnimConfig();
        animConfig.setSpecial(viewProperty2, FolmeEase.spring(1.0f, 0.36f), new float[0]);
        animConfig.setSpecial(viewProperty3, FolmeEase.spring(1.0f, 0.36f), new float[0]);
        animConfig.setSpecial(viewProperty, FolmeEase.sinOut(360L), new float[0]);
        if (mLogoImageWrapper != null) {
            Folme.use(mLogoImageWrapper).state().setTo(add3).to(add4, animConfig);
        }
        if (mNextView != null) {
            Folme.use(mNextView).state().setTo(add3).to(add4, animConfig);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGlowController != null) {
            mGlowController.start(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("ProvisionCongratulationActivity", "onResume");
        mHandler.postDelayed(() -> {
            if (isFirstBoot) {
                startAnim();
            }
        }, 300L);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d("ProvisionCongratulationActivity", "onConfigurationChanged: " + newConfig);
        setBackGroundNoAnim();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.d("ProvisionCongratulationActivity", "onWindowFocusChanged: isFirstBoot " + this.isFirstBoot);
        if (isFirstBoot) {
            startAnim();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (mGlowController != null) {
            mGlowController.stop();
        }
        if (isComplete) {
            requireActivity().finish();
        }
    }

    private void startHome() {
        getContext().getSharedPreferences("pref_oobe_state", Context.MODE_PRIVATE).edit()
            .putBoolean("is_provisioned", true).apply();
        try {
            ActivityOptions customTaskAnimation = ActivityOptions.makeCustomAnimation(requireContext(), R.anim.enter_home_anim, R.anim.provision_out_anim);
            startActivity(getHomeIntent(), customTaskAnimation.toBundle());
            Log.d(TAG, "startHome success " + customTaskAnimation);
        } catch (Exception ex) {}
        Log.e(TAG, "getActivityOptions fail");
        finish();
    }


    private Intent getHomeIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setPackage(requireContext().getPackageName());
        intent.setClassName(requireContext(), "com.sevtinge.hyperceiler.ui.SplashActivity");
        // 清除掉引导页所在的整个任务栈
        // 这样跳转后，栈内只有主页，按返回键会直接回到手机桌面
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
}
