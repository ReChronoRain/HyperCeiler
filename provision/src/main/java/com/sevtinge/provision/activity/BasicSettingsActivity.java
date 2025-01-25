package com.sevtinge.provision.activity;

import androidx.fragment.app.Fragment;

import com.sevtinge.provision.R;
import com.sevtinge.provision.fragment.BasicSettingsFragment;

public class BasicSettingsActivity extends BaseActivity {

    @Override
    protected Fragment getFragment() {
        return new BasicSettingsFragment();
    }

    @Override
    protected String getFragmentTag() {
        return BasicSettingsFragment.class.getSimpleName();
    }

    @Override
    protected CharSequence getListDescCharSequence() {
        return null;
    }

    @Override
    protected int getLogoDrawableId() {
        return 0;
    }

    @Override
    protected int getPreviewDrawable() {
        return R.drawable.provision_basic_settings;
    }

    @Override
    protected int getTitleStringId() {
        return R.string.provision_basic_settings_title;
    }

    @Override
    public void onNextAminStart() {
        super.onNextAminStart();
        setResult(-1);
        finish();
    }
}
