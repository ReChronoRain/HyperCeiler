package com.sevtinge.hyperceiler.provision.activity;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.provision.fragment.CongratulationFragment;

public class CongratulationActivity extends ProvisionDetailActivity {

    private boolean mIsDisableBack = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsDisableBack = getIntent().getBooleanExtra("extra_disable_back", false);
    }

    @Override
    protected Fragment getFragment() {
        return new CongratulationFragment();
    }

    @Override
    protected String getFragmentTag() {
        return CongratulationFragment.class.getSimpleName();
    }

    @Override
    public void onBackPressed() {
        if (!mIsDisableBack) {
            setResult(0);
            super.onBackPressed();
        }
    }
}
