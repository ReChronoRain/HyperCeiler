package com.sevtinge.hyperceiler.provision.fragment;

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

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.provision.renderengine.AIRender;
import com.sevtinge.hyperceiler.provision.utils.AnimHelper;
import com.sevtinge.hyperceiler.provision.utils.BlurUtils;
import com.sevtinge.hyperceiler.provision.utils.IOnFocusListener;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;

import fan.animation.Folme;
import fan.animation.base.AnimConfig;
import fan.core.utils.MiuiBlurUtils;
import fan.internal.utils.LiteUtils;
import fan.os.Build;

public class CongratulationFragment extends BaseFragment implements IOnFocusListener {

    private static final String TAG = "Provision:CongratulationFragment";
    private GLSurfaceView mGlSurfaceView;
    private ImageView mBackgroundImage;

    private View mContentView;
    private ImageView mLogoImage;
    private TextView mCongratulationText;
    private TextView mCongratulationLabel;
    private View mLogoContentView;
    private View mNext;
    private View mBtnBg;

    private AIRender mRender;
    private boolean isFinishStep = false;

    @Override
    protected int getLayoutId() {
        return R.layout.provision_congratulation_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGlSurfaceView = view.findViewById(R.id.gl_surface_view);
        mBackgroundImage = view.findViewById(R.id.background_image);

        mContentView = view.findViewById(R.id.content_view);
        mLogoContentView = view.findViewById(R.id.logo_content);
        mLogoImage = view.findViewById(R.id.logo_image);
        mCongratulationText = view.findViewById(R.id.congratulation_text);
        mCongratulationLabel = view.findViewById(R.id.congratulation_label);
        mBtnBg = view.findViewById(R.id.btn_bg);
        mNext = view.findViewById(R.id.next);

        if (mCongratulationText != null && mCongratulationLabel != null) {
            mCongratulationText.setText(R.string.provision_congratulation_title);
            mCongratulationLabel.setText(R.string.provision_congratulation_label);
            mCongratulationLabel.setVisibility(View.GONE);
        }
        if (OobeUtils.IS_SUPPORT_WELCOM_ANIM && mLogoContentView != null && mNext != null && OobeUtils.isEndBoot) {
            Log.i(TAG, "onViewCreated");
            mLogoContentView.setVisibility(View.GONE);
            if (shoudPlayBtnAnim()) {
                mNext.setVisibility(View.GONE);
            }
        }
        if (mNext != null && OobeUtils.isGestureLineShow(getContext())) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mNext.getLayoutParams();
            if (params != null) {
                params.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.provision_congratulation_end_bottom_margin_full_screen);
            }
            mNext.setLayoutParams(params);
        }
        if (OobeUtils.isMiuiSdkSupportFolme() && mNext != null) {
            Folme.useAt(mNext).touch().handleTouchOf(mNext, new AnimConfig[0]);
        }
        if (mNext != null) {
            mNext.setEnabled(false);
            mNext.setOnClickListener(v -> nextStep());
        }
        setupBlurBackground();
    }

    private void setupBlurBackground() {
        if (!OobeUtils.isLiteOrLowDevice()) {
            mBackgroundImage.setBackground(null);
            if (mGlSurfaceView != null) {
                mGlSurfaceView.setVisibility(View.VISIBLE);
                mGlSurfaceView.setEGLContextClientVersion(3);
                mRender = new AIRender(requireContext());
                mGlSurfaceView.setRenderer(mRender);
            }
            if (MiuiBlurUtils.isEnable() && !LiteUtils.isCommonLiteStrategy() && MiuiBlurUtils.isEffectEnable(getContext())) {
                Log.i(TAG, " MiuiBlur EffectEnabled ");
                mLogoImage.setImageResource(R.drawable.provision_logo_image);
                mBtnBg.setBackgroundResource(R.drawable.provision_congratulation_btn_background);
                float density = getResources().getDisplayMetrics().density;
                MiuiBlurUtils.setBackgroundBlur(mContentView, (int) ((density * 50.0f) + 0.5f));
                MiuiBlurUtils.setViewBlurMode(mContentView, 0);
                MiuiBlurUtils.setPassWindowBlurEnabled(mContentView, true);
                BlurUtils.setupLogoBlur(mLogoImage);
                BlurUtils.setupViewBlur(mBtnBg, new int[]{-12763843, -15021056}, new int[]{100, 106});
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (OobeUtils.IS_SUPPORT_WELCOM_ANIM && mLogoContentView != null && mNext != null && OobeUtils.isEndBoot) {
            Log.i(TAG, " onWindowFocusChanged called ");
            mLogoContentView.setVisibility(View.VISIBLE);
            AnimHelper.endPageAnim(mLogoContentView);
            if (shoudPlayBtnAnim()) {
                mNext.setVisibility(View.VISIBLE);
                mNext.postDelayed(() -> {
                    if (mNext != null) {
                        mNext.setEnabled(true);
                    }
                    Log.d(TAG, "anim time out");
                }, 2000L);
                AnimHelper.startPageBtnEnabledAnim(mNext);
            } else {
                mNext.setEnabled(true);
                Log.d(TAG, "shoud play button anim false");
            }
            OobeUtils.isEndBoot = false;
            displayOSLogoDelay();
        } else {
            if (mNext != null) {
                mNext.setEnabled(true);
            }
            Log.d(TAG, "should anim false");
        }
    }

    public void nextStep() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.getWindow().addFlags(128);
        }
        finishSetup();
    }

    public boolean shoudPlayBtnAnim() {
        return !Build.IS_INTERNATIONAL_BUILD;
    }

    private void displayOSLogoDelay() {
        new Handler().postDelayed(() -> {
            if (mLogoContentView != null && mNext != null) {
                mLogoContentView.setVisibility(View.VISIBLE);
                if (shoudPlayBtnAnim()) {
                    mNext.setVisibility(View.VISIBLE);
                }
            }
        }, 5000L);
    }

    public void finishSetup() {
        isFinishStep = true;
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                activity.setResult(-1);
                activity.finish();
            });
        } else {
            Log.d(TAG, "activity is null");
        }
    }
}
