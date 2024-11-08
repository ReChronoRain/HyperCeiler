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
package com.sevtinge.hyperceiler.ui.fragment.base;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.SettingsHelper;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.PreferenceFragment;

public class BasePreferenceFragment extends PreferenceFragment {

    protected PreferenceManager mPreferenceManager;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        mPreferenceManager = getPreferenceManager();
        SettingsHelper.initSharedPreferences(mPreferenceManager, PrefsUtils.mPrefsName, Context.MODE_PRIVATE);
    }

    public void setTitle(int titleResId) {
        setTitle(getResources().getString(titleResId));
    }

    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            getActivity().setTitle(title);
        }
    }

    public String getFragmentName(Fragment fragment) {
        return fragment.getClass().getName();
    }

    public String getPreferenceTitle(Preference preference) {
        return preference.getTitle().toString();
    }

    public String getPreferenceKey(Preference preference) {
        return preference.getKey();
    }

    public void finish() {
        getActivity().finish();
    }
}
