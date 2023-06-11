package com.sevtinge.cemiuiler.ui.fragment.various;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreference;

public class AlertDialogSettings extends SettingsPreferenceFragment {

    private DropDownPreference mDialogGravity;
    private SeekBarPreference mDialogHorizontalMargin;
    private SeekBarPreference mDialogBottomMargin;

    @Override
    public int getContentResId() {
        return R.xml.various_dialog;
    }

    @Override
    public void initPrefs() {
        mDialogGravity = findPreference("prefs_key_various_dialog_gravity");
        mDialogHorizontalMargin = findPreference("prefs_key_various_dialog_margin_horizontal");
        mDialogBottomMargin = findPreference("prefs_key_various_dialog_margin_bottom");

        int gialogGravity = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getActivity(), "prefs_key_various_dialog_gravity", "0"));

        mDialogHorizontalMargin.setVisible(gialogGravity != 0);
        mDialogBottomMargin.setVisible(gialogGravity == 2);

        mDialogGravity.setOnPreferenceChangeListener((preference, o) -> {
            int i = Integer.parseInt((String) o);
            mDialogHorizontalMargin.setVisible(i != 0);
            mDialogBottomMargin.setVisible(i == 2);
            return true;
        });
    }
}
