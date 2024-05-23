package com.sevtinge.hyperceiler.ui.fragment.settings.core;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.XmlRes;

import com.sevtinge.hyperceiler.ui.fragment.settings.core.lifecycle.ObservablePreferenceFragment;

import fan.preference.Preference;
import fan.preference.PreferenceScreen;

public class InstrumentedPreferenceFragment extends ObservablePreferenceFragment {

    private static final String TAG = "InstrumentedPrefFrag";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final int resId = getPreferenceScreenResId();
        if (resId > 0) {
            addPreferencesFromResource(resId);
        }
    }

    @Override
    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        updateActivityTitleWithScreenTitle(getPreferenceScreen());
    }

    @Override
    public <T extends Preference> T findPreference(CharSequence key) {
        if (key == null) {
            return null;
        }
        return super.findPreference(key);
    }

    protected final Context getPrefContext() {
        return getPreferenceManager().getContext();
    }

    /**
     * Get the res id for static preference xml for this fragment.
     */
    protected int getPreferenceScreenResId() {
        return -1;
    }

    private void updateActivityTitleWithScreenTitle(PreferenceScreen screen) {
        if (screen != null) {
            final CharSequence title = screen.getTitle();
            if (!TextUtils.isEmpty(title)) {
                getActivity().setTitle(title);
            } else {
                Log.w(TAG, "Screen title missing for fragment " + this.getClass().getName());
            }
        }
    }
}
