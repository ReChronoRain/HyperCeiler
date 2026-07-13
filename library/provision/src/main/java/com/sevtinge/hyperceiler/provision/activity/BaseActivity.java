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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sevtinge.hyperceiler.common.utils.AppLanguageHelper;
import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.utils.OobeTransitionHelper;
import com.sevtinge.hyperceiler.provision.utils.PageIntercepHelper;
import com.sevtinge.hyperceiler.provision.utils.ProvisionStateHolder;

import fan.provision.OobeUtils;
import fan.provision.ProvisionBaseActivity;

public abstract class BaseActivity extends ProvisionBaseActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // 引导页面中的所有子页面 Activity 都应跨语言切换后同步语言，
        // 否则仅 DefaultActivity 被包裹会造成子页面仍然显示旧语言。
        super.attachBaseContext(AppLanguageHelper.wrapContext(newBase));
    }

    protected Fragment mFragment;

    private boolean mCheckNewJump = true;
    private boolean mIsDisableBack = false;
    private boolean mNavigationCommitted;
    private boolean mWaitingForNextPage;

    private final View.OnClickListener mBackListener = v -> getOnBackPressedDispatcher().onBackPressed();

    private final OnBackPressedCallback mBackCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (mIsDisableBack || mNavigationCommitted) return;
            try {
                additionalProcess();
                if (!(BaseActivity.this instanceof PermissionSettingsActivity)) {
                    ProvisionStateHolder.getInstance().moveToPreviousActivity();
                }
                mNavigationCommitted = true;
                updateButtonState(false);
                finishAfterTransition();
            } catch (Exception e) {
                mNavigationCommitted = false;
                updateButtonState(true);
                Log.e("BaseActivity", "ex: " + e.getMessage());
            }
        }
    };

    protected abstract Fragment getFragment();
    protected abstract String getFragmentTag();

    protected abstract int getLogoDrawableId();
    protected abstract int getPreviewDrawable();
    protected abstract int getTitleStringId();
    protected abstract CharSequence getListDescCharSequence();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!(this instanceof PermissionSettingsActivity)) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.provision_slide_in_left,
                R.anim.provision_slide_out_right
            );
        }
        getOnBackPressedDispatcher().addCallback(this, mBackCallback);
        if (OobeUtils.isProvisioned(this) && !OobeUtils.isDebugOobeMode(this)) {
            setResult(-1);
            finish();
        } else {
            mIsDisableBack = getIntent().getBooleanExtra("extra_disable_back", false);
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager.isDestroyed()) {
                setResult(-1);
                finish();
            } else {
                mFragment = fragmentManager.findFragmentByTag(getFragmentTag());
                if (mFragment == null) {
                    FragmentTransaction beginTransaction = fragmentManager.beginTransaction();
                    mFragment = getFragment();
                    beginTransaction.replace(R.id.provision_container, mFragment, getFragmentTag());
                    beginTransaction.commit();
                }
                if (getTitleStringId() > 0) {
                    setTitle(getTitleStringId());
                } else {
                    setTitle(getTitleStringText());
                }

                if (getPreviewDrawable() > 0) {
                    setPreviewDrawable(getPreviewDrawable());
                }
            }
        }
    }

    protected String getTitleStringText() {
        return "";
    }

    protected CharSequence getDescriptionContent() {
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWaitingForNextPage) {
            mWaitingForNextPage = false;
            mNavigationCommitted = false;
            updateButtonState(true);
        }
        if (mTitle != null) {
            mTitle.setTextDirection(OobeUtils.isRTL() ? 4 : 3);
            int titleStringId = getTitleStringId();
            if (titleStringId > 0) {
                mTitle.setText(titleStringId);
            }
        }
        if (mNewBackBtn != null && !mIsDisableBack && !hasPreview()) {
            mNewBackBtn.setOnClickListener(mBackListener);
        }
        if (mFragment != null) {
            View description = mFragment.getView().findViewById(R.id.list_description);
            if (description != null && (description instanceof TextView textView)) {
                CharSequence listDescCharSequence = getListDescCharSequence();
                if (listDescCharSequence != null) {
                    description.setTextDirection(OobeUtils.isRTL() ? 4 : 3);
                    textView.setText(listDescCharSequence);
                    description.setVisibility(View.VISIBLE);
                } else {
                    description.setVisibility(View.GONE);
                }
            }
            View view = mFragment.getView().findViewById(R.id.description);
            if (view != null && view instanceof TextView) {
                CharSequence descriptionContent = getDescriptionContent();
                if (!TextUtils.isEmpty(descriptionContent)) {
                    view.setTextDirection(OobeUtils.isRTL() ? 4 : 3);
                    ((TextView) view).setText(descriptionContent);
                    view.setVisibility(View.VISIBLE);
                } else {
                    view.setVisibility(View.GONE);
                }
            }
        }
    }

    protected void additionalProcess() {
        setResult(0);
    }

    public final void navigateForward() {
        if (mNavigationCommitted) return;
        Intent intent = ProvisionStateHolder.getInstance().moveToNextActivity();
        if (intent == null) {
            Log.e("BaseActivity", "next OOBE state is unavailable");
            return;
        }

        mNavigationCommitted = true;
        mWaitingForNextPage = true;
        updateButtonState(false);
        try {
            startActivity(intent, OobeTransitionHelper.createPageOptions(this, true));
        } catch (RuntimeException exception) {
            ProvisionStateHolder.getInstance().moveToPreviousActivity();
            mWaitingForNextPage = false;
            mNavigationCommitted = false;
            updateButtonState(true);
            Log.e("BaseActivity", "failed to open next OOBE page", exception);
        }
    }

    protected void setPreviewDrawable(int id) {
        if (mImageView != null) {
            setPreviewView(ContextCompat.getDrawable(this, id));
        }
    }

    public void setCheck(boolean jump) {
        mCheckNewJump = jump;
    }


    @Override
    public void finish() {
        if (PageIntercepHelper.getInstance().isAdapterNewJump(this) && mCheckNewJump) {
            PageIntercepHelper.getInstance().sendFinish(this);
            setResult(PageIntercepHelper.getInstance().getPlaceHolderCode(this));
        } else {
            super.finish();
        }
    }

}
