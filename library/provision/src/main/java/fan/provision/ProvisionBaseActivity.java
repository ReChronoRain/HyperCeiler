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
package fan.provision;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
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

import fan.core.utils.EnvStateManager;
import fan.internal.utils.ViewUtils;
import fan.provision.ProvisionAnimHelper.AnimListener;

import fan.appcompat.app.AppCompatActivity;
import fan.appcompat.app.GroupButtonsConfig;
import fan.os.Build;

public abstract class ProvisionBaseActivity extends AppCompatActivity
        implements AnimListener {

    private static final String TAG = "ProvisionBaseActivity";

    protected static float HALF_ALPHA = 0.5f;
    protected static float NO_ALPHA = 1.0f;

    protected LinearLayout mProvisionLyt;

    protected ImageView mNewBackBtn;
    private TextureView mDisplayView;

    protected ImageView mImageView;

    protected ImageView mPreviewImage;

    private View mTitleSpace;

    protected View mTitleLayout;
    protected View mSubTitleLayout;

    protected TextView mTitle;
    protected TextView mSubTitle;

    protected View mProvisionContainer;

    protected Button mSkipButton;
    protected Button mConfirmButton;

    private final int mResourceId = 0;

    private boolean mHasPreview;
    private boolean isBackBtnEnable = true;

    protected ProvisionAnimHelper mProvisionAnimHelper;

    private MediaPlayer mMediaPlayer;
    private GroupButtonsConfig mConfig;
    private final Handler mHandler = new Handler();
    private final View.OnClickListener mNextClickListener = v -> {
        if (OobeUtils.needFastAnimation()) {
            updateButtonState(false);
            mHandler.postDelayed(() -> updateButtonState(true), 5000L);
        }
        Log.d(TAG, "begin start OOBSETTINGS");
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
                Log.w(TAG, "other anim not end");
                return;
            } else if (!isAnimEnded()) {
                Log.w(TAG, "video anim not end");
                return;
            }
            Log.d(TAG, "begin start OOBSETTINGS");
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
                Log.i(TAG, " mBackListener fast click ");
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
                Log.w(TAG, "other anim not end");
                return;
            }
            Log.d(TAG, "begin back OOBSETTINGS");
            if (mProvisionAnimHelper != null) {
                mProvisionAnimHelper.setAnimY(getTitleLayoutHeight());
                mProvisionAnimHelper.goBackStep();
            }
        }
    };
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Log.i(TAG, " Inner onSurfaceTextureAvailable ");
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
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExtraHorizontalPaddingEnable(true);
        setExtraPaddingApplyToContentEnable(false);
        setupView();
    }

    private void setupView() {
        mHasPreview = hasPreview();
        setContentView(R.layout.provision_detail_layout);

        mProvisionLyt = findViewById(R.id.provision_lyt);
        mProvisionContainer = findViewById(R.id.provision_container);

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
        Log.i(TAG, " current density is " + mNewBackBtn.getResources().getDisplayMetrics().density);
        setDefaultPadding();
        setExtraPaddingBottom(mConfirmButton);

        mTitleSpace = findViewById(R.id.provision_title_space);

        mImageView = findViewById(R.id.provision_preview_img);
        mDisplayView = findViewById(R.id.video_display_surfaceview);

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

        if (OobeUtils.isTabletPort(this)) {
            mTitleSpace.setVisibility(View.VISIBLE);
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
            delayEnableButton();
            setBackButtonContentDescription();
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
    public void onContentInsetChanged(Rect contentInset) {
        super.onContentInsetChanged(contentInset);
        int bottom;
        if (isNeedAdaptImageStyle() && OobeUtils.isPortOrientation(this)) {
            bottom = contentInset.bottom + getResources().getDimensionPixelSize(
                R.dimen.provision_container_margin_bottom_pad_port);
        } else {
            bottom = contentInset.bottom;
        }
        ViewUtils.resetPaddingBottom(mProvisionContainer, bottom);
    }

    @Override
    public void onExtraPaddingChanged(int extraHorizontalPadding) {
        super.onExtraPaddingChanged(extraHorizontalPadding);
        adaptPadContainerExtraPadding(extraHorizontalPadding);
    }

    protected void adaptPadContainerExtraPadding(int i) {
        if (mProvisionContainer == null || !Build.IS_TABLET || isNeedAdaptImageStyle()) {
            return;
        }
        int horizontal;
        if (OobeUtils.isLandOrientation(this)) {
            horizontal = getResources().getDimensionPixelSize(R.dimen.provision_container_padding_horizontal_pad_land);
        } else {
            horizontal = getResources().getDimensionPixelSize(R.dimen.provision_container_padding_horizontal_pad_port);
        }
        mProvisionContainer.setPadding(horizontal, mProvisionContainer.getPaddingTop(), horizontal, mProvisionContainer.getPaddingBottom());
        setContainerMargin(i);
        Log.d("OobeUtil2", "adaptContainerMargin: " + i);
    }

    protected void adaptPadImageStyleLayout(DisplayMetrics metrics) {
        if (mProvisionContainer != null && isNeedAdaptImageStyle()) {
            mProvisionContainer.setPadding(0, 0, 0, 0);
            setContainerMargin((metrics.widthPixels - ((int) (EnvStateManager.getScreenShortEdge(this) * 0.6d))) / 2);
        }
    }

    private void setContainerMargin(int margin) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mProvisionContainer.getLayoutParams();
        params.leftMargin = margin;
        params.rightMargin = margin;
        mProvisionContainer.setLayoutParams(params);
    }

    protected void setDefaultPadding() {
        if (mProvisionContainer == null || isNeedDefaultPadding()) return;
        mProvisionContainer.setPadding(0, 0, 0, 0);
    }

    protected void setExtraPaddingBottom(Button button) throws Resources.NotFoundException {
        int dimensionPixelSize;
        if (button == null) return;
        LinearLayout parent = (LinearLayout) button.getParent();
        int dimensionPixelSize2 = getResources().getDimensionPixelSize(R.dimen.group_buttons_layout_extra_padding_bottom);
        if (Build.IS_TABLET) {
            if (OobeUtils.isLandOrientation(this)) {
                dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.group_buttons_layout_extra_padding_bottom_pad_land);
            } else {
                dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.group_buttons_layout_extra_padding_bottom_pad_port);
            }
            dimensionPixelSize2 = dimensionPixelSize;
        }
        parent.setPadding(
            parent.getPaddingLeft(),
            parent.getPaddingTop(),
            parent.getPaddingRight(),
            parent.getPaddingBottom() + dimensionPixelSize2
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
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


    public void playInnerVideo(Surface surface) {
        if (surface == null || mMediaPlayer == null || mResourceId == 0) return;

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

    protected void delayEnableButton() {
        if (needDelayButton()) {
            if (needSuperButtonInitial()) {
                updateButtonState(false);
                mHandler.postDelayed(() -> updateButtonState(true), 1000L);
            } else {
                updateBackButtonState(false);
                mHandler.postDelayed(() -> updateBackButtonState(true), 1000L);
            }
        }
    }

    protected void setBackButtonContentDescription() {
        if (mNewBackBtn != null) {
            mNewBackBtn.setContentDescription(getString(R.string.provision_back));
        }
    }

    protected GroupButtonsConfig getConfig() {
        return GroupButtonsConfig.createBuilder()
            .setButton(0, getText(R.string.provision_next))
            .setButton(1, getText(R.string.provision_skip_underline), null, null, true, false)
            .build();
    }

    public boolean isNeedAdaptImageStyle() {
        return isImageStyleLayout() && Build.IS_TABLET;
    }


    public boolean hasPreview() {
        return !OobeUtils.isTabletLand(this);
    }

    public boolean hasTitle() {
        return true;
    }

    public boolean hasSubTitle() {
        return !OobeUtils.isTabletLand(this);
    }

    public boolean isImageStyleLayout() {
        return false;
    }

    protected boolean isNeedDefaultPadding() {
        return true;
    }

    protected boolean isOtherAnimEnd() {
        return true;
    }

    protected boolean isShowNavigation() {
        return false;
    }

    public boolean hasNewPageAnim() {
        return false;
    }

    public boolean hasNavigationButton() {
        return true;
    }

    public boolean needDelayButton() {
        return true;
    }

    public boolean needLongClickEvent() {
        return false;
    }

    public boolean needSuperButtonInitial() {
        return true;
    }

    public boolean superButtonClickListener() {
        return true;
    }

    private boolean needDelayBottomButton() {
        return !hasPreview();
    }

    protected void onBackButtonClick() {}
    protected void onNextButtonClick() {}
    protected void onSkipButtonClick() {}

    public View getBackButton() {
        return mNewBackBtn;
    }

    public Button getNextButton() {
        return mConfirmButton;
    }

    public Button getSkipButton() {
        return mSkipButton;
    }
}
