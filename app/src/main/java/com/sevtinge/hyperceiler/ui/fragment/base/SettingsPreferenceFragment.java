package com.sevtinge.hyperceiler.ui.fragment.base;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

public abstract class SettingsPreferenceFragment extends BasePreferenceFragment {

    public String mTitle;
    public int mContentResId = 0;
    public int mTitleResId = 0;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        super.onCreatePreferences(bundle, s);
        Bundle args = getArguments();
        if (args != null) {
            mTitle = args.getString(":fragment:show_title");
            mTitleResId = args.getInt(":fragment:show_title_resid");
            mContentResId = args.getInt("contentResId");
        }
        if (mTitleResId != 0) setTitle(mTitleResId);
        if (!TextUtils.isEmpty(mTitle)) setTitle(mTitle);
        mContentResId = mContentResId != 0 ? mContentResId : getContentResId();
        if (mContentResId != 0) {
            setPreferencesFromResource(mContentResId, s);
            initPrefs();
        }
        ((BaseSettingsActivity)getActivity()).setRestartView(addRestartListener());
    }

    public View.OnClickListener addRestartListener() {
        return null;
    }

    public SharedPreferences getSharedPreferences() {
        return PrefsUtils.mSharedPreferences;
    }

    public boolean hasKey(String key) {
        return getSharedPreferences().contains(key);
    }

    public void initPrefs() {}
    public abstract int getContentResId();
}
