package com.sevtinge.hyperceiler.provision.activity;

import android.os.Handler;

import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.provision.fragment.PermissionSettingsFragment;

public class PermissionSettingsActivity extends BaseActivity {

    private Handler mBottomHandler = new Handler();

    @Override
    protected int getLogoDrawableId() {
        return 0;
    }

    @Override
    protected int getPreviewDrawable() {
        return R.drawable.provision_service_state;
    }

    @Override
    protected int getTitleStringId() {
        return R.string.provision_permission_settings_title;
    }

    @Override
    protected CharSequence getListDescCharSequence() {
        return null;
    }

    @Override
    protected Fragment getFragment() {
        return new PermissionSettingsFragment();
    }

    @Override
    protected String getFragmentTag() {
        return PermissionSettingsFragment.class.getSimpleName();
    }

    @Override
    public void onNextAminStart() {
        super.onNextAminStart();
        setResult(-1);
        finish();
    }

    public void enableBtnClick() {
        addClickable(false);
        mBottomHandler.postDelayed(() -> addClickable(true), 1000L);
    }

    public void addClickable(boolean clickable) {
        if (mConfirmButton != null) {
            mConfirmButton.setAlpha(clickable ? NO_ALPHA : HALF_ALPHA);
            mConfirmButton.setClickable(clickable);
        }
        if (mNewBackBtn != null) {
            mNewBackBtn.setAlpha(clickable ? NO_ALPHA : HALF_ALPHA);
            mNewBackBtn.setClickable(clickable);
        }
    }

}
