package com.sevtinge.cemiuiler.ui.fragment.home;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreference;
import moralnorm.preference.SwitchPreference;

public class HomeFolderSettings extends SettingsPreferenceFragment {

    DropDownPreference mFolderShade;
    SeekBarPreference mFolderShadeLevel;

    SeekBarPreference mFolderColumns;
    SwitchPreference mFolderWidth;
    SwitchPreference mFolderSpace;
    Preference mSmallFolderIconBackgroundCustom;
    Preference mBigFolderIconBackgroundCustom;

    @Override
    public int getContentResId() {
        return R.xml.home_folder;
    }

    @Override
    public void initPrefs() {
        mFolderShade = findPreference("prefs_key_home_folder_shade");
        mFolderShadeLevel = findPreference("prefs_key_home_folder_shade_level");

        mFolderColumns = findPreference("prefs_key_home_folder_columns");
        mFolderWidth = findPreference("prefs_key_home_folder_width");
        mFolderSpace = findPreference("prefs_key_home_folder_space");
        mSmallFolderIconBackgroundCustom = findPreference("prefs_key_home_small_folder_icon_bg_custom");

        setFolderShadeLevelEnable(Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_home_folder_shade", "0")));
        setFolderWidthEnable(PrefsUtils.mSharedPreferences.getInt(mFolderColumns.getKey(), 3));
        setFolderSpaceEnable(PrefsUtils.mSharedPreferences.getInt(mFolderColumns.getKey(), 3));

        mFolderShade.setOnPreferenceChangeListener((preference, o) -> {
            setFolderShadeLevelEnable(Integer.parseInt((String) o));
            return true;
        });

        mFolderColumns.setOnPreferenceChangeListener(((preference, o) -> {
            setFolderWidthEnable((Integer) o);
            setFolderSpaceEnable((Integer) o);
            return true;
        }));
    }

    private void setFolderShadeLevelEnable(int i) {
        boolean isEnable = i != 0;
        mFolderShadeLevel.setVisible(isEnable);
        mFolderShadeLevel.setEnabled(isEnable);
    }

    private void setFolderWidthEnable(int columns) {
        boolean isEnable = columns > 1;
        mFolderWidth.setVisible(isEnable);
        mFolderWidth.setEnabled(isEnable);
    }

    private void setFolderSpaceEnable(int columns) {
        boolean isEnable = columns > 3;
        mFolderSpace.setVisible(isEnable);
        mFolderSpace.setEnabled(isEnable);
    }
}
