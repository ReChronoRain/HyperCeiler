package com.sevtinge.cemiuiler.ui.fragment.home;

import static com.sevtinge.cemiuiler.utils.api.VoyagerApisKt.isPad;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class HomeFolderSettings extends SettingsPreferenceFragment {

    DropDownPreference mFolderShade;
    SeekBarPreferenceEx mFolderShadeLevel;

    SeekBarPreferenceEx mFolderColumns;
    SwitchPreference mFolderWidth;
    SwitchPreference mFolderSpace;
    Preference mSmallFolderIconBackgroundCustom;
    Preference mSmallFolderIconBackgroundCustom1;
    Preference mSmallFolderIconBackgroundCustom2;
    Preference mSmallFolderIconBackgroundCustom3;

    @Override
    public int getContentResId() {
        return R.xml.home_folder;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.home),
            "com.miui.home"
        );
    }

    @Override
    public void initPrefs() {
        mFolderShade = findPreference("prefs_key_home_folder_shade");
        mFolderShadeLevel = findPreference("prefs_key_home_folder_shade_level");

        mFolderColumns = findPreference("prefs_key_home_folder_columns");
        mFolderWidth = findPreference("prefs_key_home_folder_width");
        mFolderSpace = findPreference("prefs_key_home_folder_space");
        mSmallFolderIconBackgroundCustom = findPreference("prefs_key_home_small_folder_icon_bg_custom");

        mSmallFolderIconBackgroundCustom1 = findPreference("prefs_key_home_big_folder_icon_bg_2x1");
        mSmallFolderIconBackgroundCustom2 = findPreference("prefs_key_home_big_folder_icon_bg_1x2");
        mSmallFolderIconBackgroundCustom3 = findPreference("prefs_key_home_big_folder_icon_bg");

        setBigFolderTextForPad();
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

    private void setBigFolderTextForPad(){
        if (isPad()){
            mSmallFolderIconBackgroundCustom1.setTitle(R.string.home_big_folder_icon_bg_2x1_n);
            mSmallFolderIconBackgroundCustom2.setTitle(R.string.home_big_folder_icon_bg_1x2_n);
            mSmallFolderIconBackgroundCustom3.setTitle(R.string.home_big_folder_icon_bg_n);
        }
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
