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
package com.sevtinge.hyperceiler.provision.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.utils.PrefsUtilsDataStore;

import fan.preference.DropDownPreference;
import fan.preference.PreferenceFragment;

public class BasicSettingsFragment extends PreferenceFragment {

    private boolean mIsScrolledBottom = false;

    SwitchPreference mHideAppIcon;
    DropDownPreference mIconModePreference;
    DropDownPreference mIconModeValue;

    private RecyclerView mRecyclerView;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.provision_basic_settings, rootKey);

        PreferenceDataStore dataStore = new PrefsUtilsDataStore(requireContext());
        getPreferenceManager().setPreferenceDataStore(dataStore);

        mHideAppIcon = findPreference("prefs_key_settings_hide_app_icon");
        mIconModePreference = findPreference("prefs_key_settings_icon");
        mIconModeValue = findPreference("prefs_key_settings_icon_mode");

        mHideAppIcon.setPreferenceDataStore(dataStore);
        mIconModePreference.setPreferenceDataStore(dataStore);
        mIconModeValue.setPreferenceDataStore(dataStore);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        mRecyclerView = getListView();
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!mRecyclerView.canScrollVertically(1)) {
                    mIsScrolledBottom = true;
                    adjustNextView();
                }
            }
        });

        int mIconMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(requireContext(), "prefs_key_settings_icon", "0"));
        mHideAppIcon = findPreference("prefs_key_settings_hide_app_icon");
        mIconModePreference = findPreference("prefs_key_settings_icon");
        mIconModeValue = findPreference("prefs_key_settings_icon_mode");

        mHideAppIcon.setChecked(PrefsUtils.getSharedBoolPrefs(requireContext(), "prefs_key_settings_hide_app_icon", false));
        mIconModePreference.setValueIndex(mIconMode);
        mIconModeValue.setValueIndex(Integer.parseInt(PrefsUtils.getSharedStringPrefs(requireContext(), "prefs_key_settings_icon_mode", "0")));

        setIconMode(mIconMode);
        mIconModePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            setIconMode(Integer.parseInt((String) newValue));
            return true;
        });
    }

    public void adjustNextView() {
        /*if (mNextView != null && mNextView instanceof TextView) {
            if (mIsScrolledBottom) {
                ((TextView) mNextView).setText(R.string.next);
            } else {
                ((TextView) mNextView).setText(R.string.more);
            }
        }*/
    }

    private void setIconMode(int mode) {
        mIconModeValue.setVisible(mode != 0);
    }

}
