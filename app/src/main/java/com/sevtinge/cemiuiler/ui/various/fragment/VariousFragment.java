package com.sevtinge.cemiuiler.ui.various.fragment;

import android.os.Bundle;
import android.util.Log;

import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.PreferenceFragmentCompat;
import moralnorm.preference.SeekBarPreference;
import moralnorm.preference.SwitchPreference;

public class VariousFragment extends PreferenceFragmentCompat {

    public String TAG = "VariousFragment";

    private SeekBarPreference mDialogHorizontalMargin;
    private SeekBarPreference mDialogBottomMargin;

    private PreferenceCategory mBlurEnabledCat;
    private PreferenceCategory mBlurCustomCat;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        assert args != null;
        String sub = args.getString("sub");
        if (sub == null) return;

        Log.d(TAG, "MoralNorm: " + getArguments());

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        DropDownPreference mDialogGravity = findPreference("prefs_key_various_dialog_gravity");
        mDialogHorizontalMargin = findPreference("prefs_key_various_dialog_horizontal_margin");
        mDialogBottomMargin = findPreference("prefs_key_various_dialog_bottom_margin");

        SwitchPreference mBlurEnabled = findPreference("prefs_key_various_blur_enabled");
        mBlurEnabledCat = findPreference("prefs_key_various_blur_enabled_cat");
        mBlurCustomCat = findPreference("prefs_key_various_blur_custom");

        int gialogGravity = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getActivity(), "prefs_key_various_dialog_gravity", "0"));

        mDialogHorizontalMargin.setVisible(gialogGravity != 0);
        mDialogBottomMargin.setVisible(gialogGravity == 2);

        mDialogGravity.setOnPreferenceChangeListener((preference, o) -> {
            int i = Integer.parseInt((String) o);
            mDialogHorizontalMargin.setVisible(i != 0);
            mDialogBottomMargin.setVisible(i == 2);
            return true;
        });


        boolean bluEnabled = PrefsUtils.getSharedBoolPrefs(getActivity(), "prefs_key_various_blur_enabled", false);
        mBlurEnabledCat.setVisible(bluEnabled);
        mBlurCustomCat.setVisible(bluEnabled);

        mBlurEnabled.setOnPreferenceChangeListener((preference, o) -> {
            mBlurEnabledCat.setVisible((Boolean) o);
            mBlurCustomCat.setVisible((Boolean) o);
            return true;
        });
    }
}
