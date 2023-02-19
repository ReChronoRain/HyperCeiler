package com.sevtinge.cemiuiler.ui.base;

public class PreferenceFragment extends SubFragment {

    private int mResId;

    public PreferenceFragment(int resId) {
        mResId = resId;
    }

    @Override
    public int getContentResId() {
        return mResId;
    }
}
