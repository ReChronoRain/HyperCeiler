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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.dashboard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.dashboard.base.fragment.BasePreferenceFragment;
import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

public abstract class SettingsPreferenceFragment extends BasePreferenceFragment {

    public String mTitle;
    public int mTitleResId = 0;

    public int mPreferenceResId = 0;

    private String mPreferenceKey;
    private boolean mPreferenceHighlighted = false;
    private final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        highlightPreferenceIfNeeded(mPreferenceKey);
    }

    @Override
    public int getThemeRes() {
        return R.style.AppTheme;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getThemeRes() != 0) setThemeRes(R.style.AppTheme);
        if (savedInstanceState != null) {
            mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY);
        }
    }

    public abstract int getPreferenceScreenResId();

    @Override
    public void onCreatePreferencesBefore(Bundle bundle, String s) {
        Bundle args = getArguments();
        if (args != null) {
            mTitle = args.getString(":fragment:show_title");
            mTitleResId = args.getInt(":fragment:show_title_resid");
            mPreferenceKey = args.getString(":settings:fragment_args_key");
            mPreferenceResId = args.getInt(":settings:fragment_resId");
        }
        if (mTitleResId != 0) setTitle(mTitleResId);
        if (!TextUtils.isEmpty(mTitle)) setTitle(mTitle);
        super.onCreatePreferencesBefore(bundle, s);
    }

    @Override
    public void onCreatePreferencesAfter(Bundle bundle, String s) {
        if (getPreferenceScreenResId() != 0) {
            setPreferencesFromResource(getPreferenceScreenResId(), s);
            initPrefs();
        }
    }

    public void initPrefs() {}

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

    public SubSettings getSubSettings() {
        return (SubSettings) getActivity();
    }

    public SharedPreferences getSharedPreferences() {
        return PrefsUtils.mSharedPreferences;
    }

    public boolean hasKey(String key) {
        if (getSharedPreferences() != null) {
            return getSharedPreferences().contains(key);
        }
        return false;
    }
}
