package com.sevtinge.hyperceiler.provision.fragment;

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

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.provision.activity.ProvisionActivity;
import com.sevtinge.hyperceiler.provision.renderengine.AIRender;
import com.sevtinge.hyperceiler.provision.utils.AnimHelper;
import com.sevtinge.hyperceiler.provision.utils.BlurUtils;
import com.sevtinge.hyperceiler.provision.utils.IOnFocusListener;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;

import fan.animation.Folme;
import fan.animation.base.AnimConfig;
import fan.core.utils.MiuiBlurUtils;
import fan.internal.utils.LiteUtils;

public class StartupFragment extends BaseFragment implements IOnFocusListener {

    private static final String TAG = "StartupFragment";

    private static final int MSG_STOP_LOGO = 2;
    private static final int MSG_RELEASE_LOGO = 3;

    private GLSurfaceView mGlSurfaceView;
    private ImageView mBackgroundImage;

    private View mContentView;
    private View mLogoImageWrapper;
    private ImageView mLogoImage;

    private View mNext;
    private ImageView mNextArrow;
    private Handler mAnimationHandler;

    private AIRender mRender;

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
        mGlSurfaceView = view.findViewById(R.id.background_glsurfaceview);
        mLogoImage = view.findViewById(R.id.logo_image);
        mLogoImageWrapper = view.findViewById(R.id.logo_image_wrapper);
        mNext = view.findViewById(R.id.next);
        mNextArrow = view.findViewById(R.id.next_arrow);
        mContentView = view.findViewById(R.id.content_view);
        mBackgroundImage = view.findViewById(R.id.background_image);

        setupBlurBackground();

        if (mNextArrow != null) {
            mNextArrow.setImportantForAccessibility(MSG_STOP_LOGO);
        }
        mContentView.setClickable(true);
        //mContentView.setOnTouchListener(new StartupFragment$4(this));
        mContentView.setImportantForAccessibility(MSG_STOP_LOGO);
        /*if (Utils.applicationInstalled(getActivity(), QR_APPLICATION_ID)) {
            view.findViewById(R.id.show_qr_view).setOnClickListener(new StartupFragment$5(this));
        }*/
        if (OobeUtils.isMiuiSdkSupportFolme()) {
            Folme.useAt(mNext).touch().handleTouchOf(mNext, new AnimConfig[0]);
        }
        mNext.setOnClickListener(v -> {
            ProvisionActivity activity = (ProvisionActivity) getActivity();
            OobeUtils.isFirstBoot = false;
            activity.run(-1);
        });
        //Utils.setGmsAppEnabledStateForCn(getActivity(), MSG_STOP_LOGO);
        if (OobeUtils.IS_SUPPORT_WELCOM_ANIM && mLogoImageWrapper != null && mNext != null && OobeUtils.isFirstBoot) {
            Log.i(TAG, " onViewCreated");
            mLogoImageWrapper.setVisibility(View.INVISIBLE);
            mNext.setVisibility(View.INVISIBLE);
            mNextArrow.setVisibility(View.INVISIBLE);
        }
    }

    private void setupBlurBackground() {
        if (!OobeUtils.isLiteOrLowDevice()) {
            mBackgroundImage.setBackground(null);
            if (mGlSurfaceView != null) {
                mGlSurfaceView.setVisibility(View.VISIBLE);
                mGlSurfaceView.setEGLContextClientVersion(3);
                mRender = new AIRender(getContext().getApplicationContext());
                mGlSurfaceView.setRenderer(mRender);
            }
            if (MiuiBlurUtils.isEnable() && !LiteUtils.isCommonLiteStrategy() && MiuiBlurUtils.isEffectEnable(getContext())) {
                Log.i(TAG, "MiuiBlur EffectEnabled");
                mLogoImage.setImageResource(R.drawable.provision_logo_image);
                mNext.setBackgroundResource(R.drawable.provision_next);
                mNextArrow.setVisibility(View.VISIBLE);
                mNextArrow.setImageResource(R.drawable.provision_icon_arrow);
                float density = getResources().getDisplayMetrics().density;
                MiuiBlurUtils.setBackgroundBlur(mContentView, (int) ((density * 50.0f) + 0.5f));
                MiuiBlurUtils.setViewBlurMode(mContentView, 0);
                MiuiBlurUtils.setPassWindowBlurEnabled(mContentView, true);
                BlurUtils.setupLogoBlur(mLogoImage);
                BlurUtils.setupViewBlur(mNext, new int[]{-13750738, -15011328}, new int[]{100, 106});
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.i(TAG, " onWindowFocusChanged");
        if (OobeUtils.IS_SUPPORT_WELCOM_ANIM && mLogoImageWrapper != null && mNext != null && OobeUtils.isFirstBoot) {
            mLogoImageWrapper.setVisibility(View.VISIBLE);
            //accelerateView(this.mLogoImageWrapper);
            mNext.setVisibility(View.VISIBLE);
            mNextArrow.setVisibility(View.VISIBLE);
            AnimHelper.startPageTextAnim(mLogoImageWrapper);
            AnimHelper.startPageBtnAnim(mNext);
            AnimHelper.startPageBtnAnim(mNextArrow);
            resetFirstStart();
            displayOsLogoDelay();
            //requestStartAccessibilityFocus(2000);
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
            if (mLogoImageWrapper != null && mNext != null && mNextArrow != null) {
                mLogoImageWrapper.setVisibility(View.VISIBLE);
                mNext.setVisibility(View.VISIBLE);
                mNextArrow.setVisibility(View.VISIBLE);
            }
        }, 7000L);
    }

    public void onKeyDownChild(int keyCode, KeyEvent event) {

    }
}
