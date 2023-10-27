package com.sevtinge.hyperceiler.ui.fragment.various;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.SeekBarPreferenceEx;

public class AlertDialogSettings extends SettingsPreferenceFragment {

    private DropDownPreference mDialogGravity;
    private SeekBarPreferenceEx mDialogHorizontalMargin;
    private SeekBarPreferenceEx mDialogBottomMargin;

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
