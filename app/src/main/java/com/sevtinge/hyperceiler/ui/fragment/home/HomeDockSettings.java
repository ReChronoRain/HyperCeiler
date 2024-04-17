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
package com.sevtinge.hyperceiler.ui.fragment.home;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import moralnorm.preference.ColorPickerPreference;
import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class HomeDockSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    SwitchPreference mDisableRecentIcon;
    Preference mDockBackgroundBlur;
    DropDownPreference mDockBackgroundBlurEnable;
    ColorPickerPreference mDockBackgroundColor;

    @Override
    public int getContentResId() {
        return R.xml.home_dock;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.mihome),
            "com.miui.home"
        );
    }

    @Override
    public void initPrefs() {
        mDisableRecentIcon = findPreference("prefs_key_home_dock_disable_recents_icon");
        mDisableRecentIcon.setVisible(isPad());
        mDockBackgroundBlur = findPreference("prefs_key_home_dock_bg_custom");
        mDockBackgroundColor = findPreference("prefs_key_home_dock_bg_color");
        int mBlurMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_home_dock_add_blur", "0"));
        mDockBackgroundBlurEnable = findPreference("prefs_key_home_dock_add_blur");
        setCanBeVisible(mBlurMode);
        mDockBackgroundBlurEnable.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mDockBackgroundBlurEnable) {
            setCanBeVisible(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setCanBeVisible(int mode) {
        mDockBackgroundBlur.setVisible(mode == 2);
        mDockBackgroundColor.setVisible(mode != 2);
    }
}
