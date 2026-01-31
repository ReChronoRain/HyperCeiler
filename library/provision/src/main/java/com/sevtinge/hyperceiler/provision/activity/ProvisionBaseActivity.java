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
package com.sevtinge.hyperceiler.provision.activity;

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

import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;
import com.sevtinge.hyperceiler.provision.utils.ProvisionAnimHelper;
import com.sevtinge.hyperceiler.provision.utils.ProvisionAnimHelper.AnimListener;

import fan.animation.Folme;
import fan.appcompat.app.AppCompatActivity;
import fan.appcompat.app.GroupButtonsConfig;
import fan.os.Build;

public abstract class ProvisionBaseActivity extends AppCompatActivity
        implements AnimListener {

    protected static float HALF_ALPHA = 0.5f;
    protected static float NO_ALPHA = 1.0f;

    protected ImageView mNewBackBtn;
    private TextureView mDisplayView;

    protected ImageView mImageView;
    private View mTitleSpace;
    protected TextView mTitle;
    protected View mTitleLayout;
    protected TextView mSubTitle;
    protected View mSubTitleLayout;

    protected ImageView mPreviewImage;
    protected Button mSkipButton;
    protected Button mConfirmButton;

    private final int mResourceId = 0;

    private boolean mHasPreview;
    private boolean isBackBtnEnable = true;

    private MediaPlayer mMediaPlayer;
    private GroupButtonsConfig mConfig;
    private final Handler mHandler = new Handler();
    protected ProvisionAnimHelper mProvisionAnimHelper;
    private final View.OnClickListener mNextClickListener = v -> {
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
    private final View.OnClickListener mSkipClickListener = new View.OnClickListener() {
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
    private final View.OnClickListener mBackListener = new View.OnClickListener() {
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
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
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

    public boolean needLongClickEvent() {
        return false;
    }

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

        mConfig = getConfig();
        addGroupButtons(mConfig);
        mConfirmButton = mConfig.getPrimaryButton();
        mSkipButton = mConfig.getSecondaryButton();

        mConfirmButton.setVisibility(hasNavigationButton() ? View.VISIBLE : View.GONE);
        mConfirmButton.setLongClickable(needLongClickEvent());
        mSkipButton.setLongClickable(needLongClickEvent());
        if (superButtonClickListener()) {
            mNewBackBtn.setOnClickListener(mBackListener);
            mConfirmButton.setOnClickListener(mNextClickListener);
            mSkipButton.setOnClickListener(mSkipClickListener);
        }
        Log.i("OobeUtil2", " current density is " + mNewBackBtn.getResources().getDisplayMetrics().density);
        mImageView = findViewById(R.id.provision_preview_img);
        mDisplayView = findViewById(R.id.video_display_surfaceview);

        mTitleSpace = findViewById(R.id.provision_title_space);
        mPreviewImage = findViewById(R.id.preview_image);

        mTitleLayout = findViewById(R.id.provision_lyt_title);
        mTitle = findViewById(R.id.provision_title);

        mSubTitleLayout = findViewById(R.id.provision_lyt_subtitle);
        mSubTitle = findViewById(R.id.provision_sub_title);

        mImageView = findViewById(R.id.provision_preview_img);
        mDisplayView = findViewById(R.id.video_display_surfaceview);

        mMediaPlayer = new MediaPlayer();
        if (hasNewPageAnim() && !OobeUtils.isInternationalBuild()) {
            mDisplayView.setVisibility(View.VISIBLE);
            mDisplayView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        if (OobeUtils.isTabletDevice()) {
            mTitle.setGravity(81);
        } else {
            mTitle.setGravity(17);
        }
        if (!mHasPreview) {
            mPreviewImage.setVisibility(View.GONE);
            if (mTitleSpace != null) {
                ViewGroup.LayoutParams params = mTitleSpace.getLayoutParams();
                params.height = getResources().getDimensionPixelOffset(R.dimen.provision_space_between_actionbar_title_pad_port_no_lottie);
                mTitleSpace.setLayoutParams(params);
            }

            mSubTitleLayout.setVisibility(View.VISIBLE);
            mSubTitle.setVisibility(hasSubTitle() ? View.VISIBLE : View.GONE);
            if (OobeUtils.isTabletLand(this)) {
                mSubTitleLayout.setMinimumHeight(getResources().getDimensionPixelOffset(R.dimen.provision_subtitle_lyt_min_height_pad_land));
            } else if (OobeUtils.isTabletPort(this)) {
                mSubTitleLayout.setMinimumHeight(getResources().getDimensionPixelOffset(R.dimen.provision_subtitle_lyt_min_height_pad_port));
            }
            mTitleLayout.setPaddingRelative(
                mTitleLayout.getPaddingStart(),
                Build.IS_TABLET ? 0 : getResources().getDimensionPixelOffset(R.dimen.provision_title_padding_top_no_lottie),
                mTitleLayout.getPaddingEnd(),
                0
            );
        }

        if (!OobeUtils.isInternationalBuild()) {
            registerAccessibiltyStateChange(getApplicationContext());
        }
        /*if (OobeUtils.needFastAnimation() || needDelayBottomButton()) {
            if (needSuperButtonInitial()) {
                updateButtonState(false);
                mHandler.postDelayed(() -> updateButtonState(true), 600L);
            } else {
                updateBackButtonState(false);
                mHandler.postDelayed(() -> updateBackButtonState(true), 600L);
            }
        }*/
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
        return mPreviewImage == null ? 0 : mPreviewImage.getHeight();
    }

    public void updateBackButtonState(boolean enabled) {
        if (mNewBackBtn != null) {
            isBackBtnEnable = enabled;
        }
    }

    public void updateButtonState(boolean enabled) {
        if (mNewBackBtn != null) {
            isBackBtnEnable = enabled;
        }
        if (mConfirmButton != null) {
            mConfirmButton.setEnabled(enabled);
        }
        if (mSkipButton != null) {
            mSkipButton.setEnabled(enabled);
        }
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



    public View getBackButton() {
        return mNewBackBtn;
    }

    public Button getNextButton() {
        return mConfirmButton;
    }

    public Button getSkipButton() {
        return mSkipButton;
    }

    protected GroupButtonsConfig getConfig() {
        return GroupButtonsConfig.createBuilder()
            .setButton(0, getText(R.string.provision_next))
            .setButton(1, getText(R.string.provision_skip_underline), null, null, true, false)
            .build();
    }
}
