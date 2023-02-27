package com.sevtinge.cemiuiler.ui.home;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.PickerHomeActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.home.base.BaseHomeActivity;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreference;
import moralnorm.preference.SwitchPreference;

public class HomeFolderActivity extends BaseHomeActivity {

    @Override
    public Fragment initFragment() {
        return new HomeFolderFragment();
    }

    public static class HomeFolderFragment extends SubFragment {

        DropDownPreference mFolderShade;
        SeekBarPreference mFolderShadeLevel;

        SeekBarPreference mFolderColumns;
        SwitchPreference mFolderWidth;
        SwitchPreference mFolderSpace;
        Preference mSmallFolderIconBackgroundCustom;

        Preference mBigFolderIconBackground1x2Custom;
        Preference mBigFolderIconBackground2x1Custom;
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

            mBigFolderIconBackground1x2Custom = findPreference("prefs_key_home_big_folder_icon_bg_1x2_custom");
            mBigFolderIconBackground2x1Custom = findPreference("prefs_key_home_big_folder_icon_bg_2x1_custom");
            mBigFolderIconBackgroundCustom = findPreference("prefs_key_home_big_folder_icon_bg_custom");


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

            mSmallFolderIconBackgroundCustom.setOnPreferenceClickListener(preference -> {
                openMultiAction(preference, null, PickerHomeActivity.Actions.Blur);
                return true;
            });

            mBigFolderIconBackground1x2Custom.setOnPreferenceClickListener(preference -> {
                openMultiAction(preference, null, PickerHomeActivity.Actions.Blur);
                return true;
            });

            mBigFolderIconBackground2x1Custom.setOnPreferenceClickListener(preference -> {
                openMultiAction(preference, null, PickerHomeActivity.Actions.Blur);
                return true;
            });

            mBigFolderIconBackgroundCustom.setOnPreferenceClickListener(preference -> {
                openMultiAction(preference, null, PickerHomeActivity.Actions.Blur);
                return true;
            });
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
}
