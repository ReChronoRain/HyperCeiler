package com.sevtinge.provision.activity;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.sevtinge.provision.R;
import com.sevtinge.provision.utils.OobeUtils;
import com.sevtinge.provision.utils.ProvisionAnimHelper;
import com.sevtinge.provision.utils.ProvisionAnimHelper.AnimListener;

import fan.animation.Folme;
import fan.animation.base.AnimConfig;
import fan.appcompat.app.AppCompatActivity;
import fan.os.Build;

public abstract class ProvisionBaseActivity extends AppCompatActivity
        implements AnimListener {

    protected static float HALF_ALPHA = 0.5f;
    protected static float NO_ALPHA = 1.0f;

    protected ImageView mNewBackBtn;
    private TextureView mDisplayView;

    protected ImageView mImageView;
    protected TextView mTitle;
    private View mTitleSpace;
    protected TextView mSubTitle;
    protected View mTitleLayout;
    protected View mRealTitleLayout;

    protected ImageView mPreviewImage;
    protected Button mSkipButton;
    protected Button mConfirmButton;

    private int mResourceId = 0;

    private boolean mHasPreview;
    private boolean isBackBtnEnable = true;

    private MediaPlayer mMediaPlayer;
    private Handler mHandler = new Handler();
    protected ProvisionAnimHelper mProvisionAnimHelper;
    private View.OnClickListener mNextClickListener = v -> {
        if (OobeUtils.needFastAnimation()) {
            updateButtonState(false);
            mHandler.postDelayed(() -> updateButtonState(true), 5000L);
        }
        Log.d("OobeUtil2", "begin start OOBSETTINGS");
        if (mProvisionAnimHelper != null) {
            mProvisionAnimHelper.setAnimY(getTitleLayoutHeight());
            mProvisionAnimHelper.goNextStep(0);
        }
    };
    private View.OnClickListener mSkipClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (OobeUtils.isTabletLand(ProvisionBaseActivity.this)) {
                onSkipAminStart();
                return;
            }
            if (OobeUtils.needFastAnimation()) {
                updateButtonState(false);
                mHandler.postDelayed(() -> updateButtonState(true), 5000L);
            } else if (!isOtherAnimEnd()) {
                Log.w("OobeUtil2", "other anim not end");
                return;
            } else if (!isAnimEnded()) {
                Log.w("OobeUtil2", "video anim not end");
                return;
            }
            Log.d("OobeUtil2", "begin start OOBSETTINGS");
            if (mProvisionAnimHelper != null) {
                mProvisionAnimHelper.setAnimY(getTitleLayoutHeight());
                mProvisionAnimHelper.goNextStep(1);
            }
        }
    };
    private View.OnClickListener mBackListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isBackBtnEnable) {
                Log.i("OobeUtil2", " mBackListener fast click ");
                return;
            }
            if (OobeUtils.isTabletLand(ProvisionBaseActivity.this)) {
                onBackAnimStart();
                return;
            }
            if (OobeUtils.needFastAnimation()) {
                updateButtonState(false);
                mHandler.postDelayed(() -> updateButtonState(true), 5000L);
            } else if (!isOtherAnimEnd()) {
                Log.w("OobeUtil2", "other anim not end");
                return;
            }
            Log.d("OobeUtil2", "begin back OOBSETTINGS");
            if (mProvisionAnimHelper != null) {
                mProvisionAnimHelper.setAnimY(getTitleLayoutHeight());
                mProvisionAnimHelper.goBackStep();
            }
        }
    };
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Log.i("OobeUtil2", " Inner onSurfaceTextureAvailable ");
            playInnerVideo(new Surface(surface));
            mMediaPlayer.setOnCompletionListener(mp -> {
                if (mImageView != null) {
                    mImageView.setVisibility(View.VISIBLE);
                }
                if (mDisplayView != null) {
                    mDisplayView.setVisibility(View.GONE);
                }
                if (mp != null) {
                    mp.release();
                }
            });
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };

    protected void onBackButtonClick() {}
    protected void onNextButtonClick() {}
    protected void onSkipButtonClick() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupView();
    }

    private void setupView() {
        mHasPreview = hasPreview();
        setContentView(R.layout.provision_detail_layout);

        mNewBackBtn = findViewById(R.id.back_icon);
        mConfirmButton = findViewById(R.id.confirm_button);
        mSkipButton = findViewById(R.id.skip_button);
        Folme.useAt(mNewBackBtn).touch().handleTouchOf(mNewBackBtn, new AnimConfig[0]);
        Folme.useAt(mConfirmButton).touch().handleTouchOf(mConfirmButton, new AnimConfig[0]);
        Folme.useAt(mSkipButton).touch().handleTouchOf(mSkipButton, new AnimConfig[0]);
        if ((mHasPreview || OobeUtils.isTabletLand(this)) && superButtonClickListener()) {
            mNewBackBtn.setOnClickListener(mBackListener);
            mConfirmButton.setOnClickListener(mNextClickListener);
            mSkipButton.setOnClickListener(mSkipClickListener);
        }
        Log.i("OobeUtil2", " current density is " + mNewBackBtn.getResources().getDisplayMetrics().density);
        mImageView = findViewById(R.id.provision_preview_img);
        mDisplayView = findViewById(R.id.video_display_surfaceview);
        mSubTitle = findViewById(R.id.provision_sub_title);
        mTitleSpace = findViewById(R.id.provision_title_space);
        mTitleLayout = findViewById(R.id.provision_lyt_title);
        mRealTitleLayout = findViewById(R.id.provision_real_title);
        mPreviewImage = findViewById(R.id.preview_image);
        mMediaPlayer = new MediaPlayer();
        if (hasNewPageAnim() && !OobeUtils.isInternationalBuild()) {
            mDisplayView.setVisibility(View.VISIBLE);
            mDisplayView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        mTitle = findViewById(R.id.provision_title);
        mTitle.setTypeface(Typeface.create("mipro-regular", Typeface.NORMAL));
        if (OobeUtils.isTabletDevice()) {
            mTitle.setGravity(81);
        } else {
            mTitle.setGravity(17);
        }
        if ("goku".equalsIgnoreCase(OobeUtils.BUILD_DEVICE) && OobeUtils.isFoldLarge(this)) {
            Log.i("OobeUtil2", " goku adapt");
            mTitleLayout.setPaddingRelative(mTitleLayout.getPaddingStart(), getResources().getDimensionPixelOffset(R.dimen.provision_title_padding_top_goku), mTitleLayout.getPaddingEnd(), mTitleLayout.getPaddingBottom());
            View findViewById = findViewById(R.id.provision_lyt_btn);
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) findViewById.getLayoutParams();
            marginLayoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.provision_lyt_btn_margin_bottom_goku);
            findViewById.setLayoutParams(marginLayoutParams);
            adaptGokuWidgetSize(mConfirmButton);
            adaptGokuWidgetSize(mSkipButton);
        }
        if (!mHasPreview) {
            mPreviewImage.setVisibility(View.GONE);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mRealTitleLayout.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, 0);
            mRealTitleLayout.setLayoutParams(layoutParams);
            if (hasSubTitle()) {
                mTitleSpace.setVisibility(View.VISIBLE);
                if (mSubTitle != null) {
                    mSubTitle.setVisibility(View.VISIBLE);
                }
                mTitleLayout.setPaddingRelative(mTitleLayout.getPaddingStart(), 0, mTitleLayout.getPaddingEnd(), 0);
            } else {
                mTitleLayout.setPaddingRelative(mTitleLayout.getPaddingStart(), 0, mTitleLayout.getPaddingEnd(), getResources().getDimensionPixelOffset(R.dimen.provision_dynamic_title_padding_bottom));
            }
        }
        if (Build.IS_INTERNATIONAL_BUILD) {
            OobeUtils.adaptFlipUi(getWindow());
            View findViewById2 = findViewById(R.id.provision_lyt_btn);
            ViewGroup.MarginLayoutParams marginLayoutParams2 = (ViewGroup.MarginLayoutParams) findViewById2.getLayoutParams();
            marginLayoutParams2.bottomMargin = getResources().getDimensionPixelSize(R.dimen.provision_lyt_btn_margin_bottom_flip);
            findViewById2.setLayoutParams(marginLayoutParams2);
        }
        findViewById(R.id.provision_lyt_btn).setVisibility(hasNavigationButton() ? View.VISIBLE : View.GONE);
        mTitleLayout.setVisibility(hasTitle() ? View.VISIBLE : View.GONE);
        if (!OobeUtils.isInternationalBuild()) {
            registerAccessibiltyStateChange(getApplicationContext());
        }
        if (OobeUtils.needFastAnimation() || needDelayBottomButton()) {
            if (needSuperButtonInitial()) {
                updateButtonState(false);
                mHandler.postDelayed(() -> updateButtonState(true), 600L);
            } else {
                updateBackButtonState(false);
                mHandler.postDelayed(() -> updateBackButtonState(true), 600L);
            }
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (mTitle != null) {
            mTitle.setText(getTitle());
        }
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        if (mTitle != null) {
            mTitle.setText(getTitle());
        }
    }

    public void setSubTitle(CharSequence title) {
        if (mSubTitle != null) {
            mSubTitle.setText(title);
        }
    }

    public void setSubTitle(int titleId) {
        setSubTitle(getText(titleId));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mHasPreview) {
            mProvisionAnimHelper = new ProvisionAnimHelper(this, mHandler);
            mProvisionAnimHelper.registerAnimService();
            mProvisionAnimHelper.setAnimListener(this);
            mProvisionAnimHelper.setAnimY(getTitleLayoutHeight());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mProvisionAnimHelper != null && mHasPreview) {
            mProvisionAnimHelper.unregisterAnimService();
            mProvisionAnimHelper = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) mImageView.setImageDrawable(null);
        if (mPreviewImage != null) mPreviewImage.setImageDrawable(null);
        if (!OobeUtils.isInternationalBuild()) {
            //unRegisterAccessibiltyStateChange(getApplicationContext());
        }
    }

    protected String getTitleStringText() {
        return "";
    }

    protected void setPreviewView(Drawable drawable) {
        if (mPreviewImage != null) {
            mPreviewImage.setImageDrawable(drawable);
        }
    }

    protected int getTitleLayoutHeight() {
        if (mTitleLayout != null) {
            return mTitleLayout.getHeight();
        }
        int height = getResources().getDimensionPixelSize(R.dimen.provision_actionbar_height) + getResources().getDimensionPixelSize(R.dimen.provision_padding_top);
        int marginTop = getResources().getDimensionPixelSize(R.dimen.provision_container_margin_top);
        return height + marginTop;
    }

    public void updateButtonState(boolean enabled) {
        Log.i("OobeUtil2", " updateButtonState and enabled is " + enabled);
        if (!OobeUtils.isTabletLand(this) && mNewBackBtn != null && mConfirmButton != null && mSkipButton != null) {
            mNewBackBtn.setAlpha(enabled ? NO_ALPHA : HALF_ALPHA);
            mConfirmButton.setAlpha(enabled ? NO_ALPHA : HALF_ALPHA);
            mSkipButton.setAlpha(enabled ? NO_ALPHA : HALF_ALPHA);
            if (OobeUtils.needFastAnimation() || needDelayBottomButton()) {
                isBackBtnEnable = enabled;
                mConfirmButton.setEnabled(enabled);
                mSkipButton.setEnabled(enabled);
            }
        }
    }

    public void updateBackButtonState(boolean enabled) {
        Log.i("OobeUtil2", " updateBackButtonState and enabled is " + enabled);
        if (!OobeUtils.isTabletLand(this) && mNewBackBtn != null) {
            mNewBackBtn.setAlpha(enabled ? NO_ALPHA : HALF_ALPHA);
            if (OobeUtils.needFastAnimation() || needDelayBottomButton()) {
                isBackBtnEnable = enabled;
            }
        }
    }

    private void adaptGokuWidgetSize(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = getResources().getDimensionPixelOffset(R.dimen.provision_navigation_button_width_goku);
        view.setLayoutParams(params);
    }

    @Override
    public void onAminServiceConnected() {
        if (!OobeUtils.needFastAnimation() && !isAnimEnded()) {
            updateButtonState(false);
        }
    }

    @Override
    public void onBackAnimStart() {
        onBackButtonClick();
        onBackPressed();
    }

    @Override
    public void onNextAminStart() {
        onNextButtonClick();
    }

    @Override
    public void onSkipAminStart() {
        onSkipButtonClick();
    }

    @Override
    public void onAminEnd() {
        if (!OobeUtils.needFastAnimation()) {
            updateButtonState(true);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (OobeUtils.needFastAnimation()) return super.dispatchTouchEvent(ev);
        if (isAnimEnded()) return super.dispatchTouchEvent(ev);
        return true;
    }

    protected boolean isAnimEnded() {
        if (mHasPreview && mProvisionAnimHelper != null) {
            return mProvisionAnimHelper.isAnimEnded();
        }
        return true;
    }

    public boolean hasTitle() {
        return true;
    }

    public boolean hasSubTitle() {
        return !OobeUtils.isTabletLand(this);
    }

    protected boolean isOtherAnimEnd() {
        return true;
    }

    public boolean hasPreview() {
        return !OobeUtils.isTabletLand(this);
    }

    public boolean hasNewPageAnim() {
        return false;
    }

    public boolean hasNavigationButton() {
        return true;
    }

    public boolean superButtonClickListener() {
        return true;
    }

    public boolean needSuperButtonInitial() {
        return true;
    }

    private boolean needDelayBottomButton() {
        return !hasPreview();
    }

    private void registerAccessibiltyStateChange(Context context) {

    }

    public void playInnerVideo(Surface surface) {
        if (surface != null && mMediaPlayer != null && mResourceId != 0) {
            try {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(this, Uri.parse("android.resource://" + getPackageName() + "/" + mResourceId));
                mMediaPlayer.setSurface(surface);
                mMediaPlayer.setOnPreparedListener(mp -> {
                    if (mMediaPlayer != null) {
                        mMediaPlayer.start();
                    }
                    if (mImageView != null) {
                        mImageView.setVisibility(View.GONE);
                    }
                });
                mMediaPlayer.prepare();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
