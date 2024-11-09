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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.ui.fragment.app;

import android.view.View;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.activity.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

public class UpdaterFragment extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    DropDownPreference mUpdateMode;
    EditTextPreference mBigVersion;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.updater;
    }

    @Override
    public void initPrefs() {
        int mMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_updater_version_mode", "1"));
        mUpdateMode = findPreference("prefs_key_updater_version_mode");
        mBigVersion = findPreference("prefs_key_various_updater_big_version");

        setMode(mMode);
        mUpdateMode.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mUpdateMode) {
            setMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setMode(int mode) {
        mBigVersion.setVisible(mode == 2);
    }
}
