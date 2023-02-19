package com.sevtinge.cemiuiler.ui.various;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.PickerHomeActivity;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreference;

public class VariousDialogActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Fragment initFragment() {
        return new VariousDialogFragment();
    }

    public static class VariousDialogFragment extends SubFragment {

        private DropDownPreference mDialogGravity;
        private SeekBarPreference mDialogHorizontalMargin;
        private SeekBarPreference mDialogBottomMargin;

        private Preference mDialogBackgroundBlur;

        @Override
        public int getContentResId() {
            return R.xml.prefs_various_dialog;
        }

        @Override
        public void initPrefs() {
            mDialogGravity = findPreference("prefs_key_various_dialog_gravity");
            mDialogHorizontalMargin = findPreference("prefs_key_various_dialog_margin_horizontal");
            mDialogBottomMargin = findPreference("prefs_key_various_dialog_margin_bottom");

            mDialogBackgroundBlur = findPreference("prefs_key_various_dialog_bg_blur");

            int gialogGravity = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getActivity(),"prefs_key_various_dialog_gravity","0"));

            mDialogHorizontalMargin.setVisible(gialogGravity != 0);
            mDialogBottomMargin.setVisible(gialogGravity == 2);

            mDialogGravity.setOnPreferenceChangeListener((preference, o) -> {
                int i = Integer.parseInt((String) o);
                mDialogHorizontalMargin.setVisible(i != 0);
                mDialogBottomMargin.setVisible(i == 2);
                return true;
            });

            mDialogBackgroundBlur.setOnPreferenceClickListener(preference -> {
                openMultiAction(preference, null, PickerHomeActivity.Actions.Blur);
                return true;
            });
        }
    }
}
