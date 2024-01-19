package com.sevtinge.hyperceiler.ui.fragment.base;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.ui.base.BaseActivity;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

public abstract class SettingsPreferenceFragment extends BasePreferenceFragment {

    public String mTitle;
    public String mPreferenceKey;
    public int mContentResId = 0;
    public int mTitleResId = 0;
    private boolean mPreferenceHighlighted = false;
    private final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        highlightPreferenceIfNeeded(mPreferenceKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY);
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        super.onCreatePreferences(bundle, s);
        Bundle args = getArguments();
        if (args != null) {
            mTitle = args.getString(":fragment:show_title");
            mTitleResId = args.getInt(":fragment:show_title_resid");
            mPreferenceKey = args.getString(":settings:fragment_args_key");
            mContentResId = args.getInt("contentResId");
        }
        if (mTitleResId != 0) setTitle(mTitleResId);
        if (!TextUtils.isEmpty(mTitle)) setTitle(mTitle);
        mContentResId = mContentResId != 0 ? mContentResId : getContentResId();
        if (mContentResId != 0) {
            setPreferencesFromResource(mContentResId, s);
            initPrefs();
        }
        ((BaseActivity) getActivity()).setRestartView(addRestartListener());
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
    }

    public void highlightPreferenceIfNeeded(String key) {
        if (isAdded() && !mPreferenceHighlighted && !TextUtils.isEmpty(key)) {
            requestHighlight(key);
            mPreferenceHighlighted = true;
        }
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

    public void initPrefs() {
    }

    public abstract int getContentResId();

}
