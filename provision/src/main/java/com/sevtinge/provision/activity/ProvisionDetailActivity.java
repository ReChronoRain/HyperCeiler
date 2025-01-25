package com.sevtinge.provision.activity;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.sevtinge.provision.utils.IOnFocusListener;

import fan.appcompat.app.AppCompatActivity;

public abstract class ProvisionDetailActivity extends AppCompatActivity {

    protected Fragment mFragment;
    protected FragmentManager mFragmentManager;

    protected abstract Fragment getFragment();
    protected abstract String getFragmentTag();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentManager = getSupportFragmentManager();
        mFragment = mFragmentManager.findFragmentByTag(getFragmentTag());
        if (mFragment == null) {
            mFragment = getFragment();
            setFragment(android.R.id.content, mFragment, getFragmentTag());
        }
        setupView();
    }

    protected void setupView() {}

    protected void setFragment(@IdRes int containerViewId, @NonNull Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(containerViewId, fragment, tag)
                .commit();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mFragment instanceof IOnFocusListener) {
            ((IOnFocusListener) mFragment).onWindowFocusChanged(hasFocus);
        }
    }
}
