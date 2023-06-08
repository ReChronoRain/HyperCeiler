package com.sevtinge.cemiuiler.ui.systemui;

import android.provider.Settings;
import android.widget.SeekBar;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.prefs.SeekBarPreferenceEx;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.SwitchPreference;

public class ControlCenterActivity extends BaseSystemUIActivity {

    @Override
    public Fragment initFragment() {
        return new ControlCenterFragment();
    }

    public static class ControlCenterFragment extends SubFragment {

        SwitchPreference mFixMediaPanel;

        @Override
        public int getContentResId() {
            return R.xml.system_ui_control_center;
        }

        @Override
        public void initPrefs() {
            mFixMediaPanel = findPreference("prefs_key_system_ui_control_center_fix_media_control_panel");
            mFixMediaPanel.setVisible(SdkHelper.isAndroidS() || SdkHelper.isAndroidSv2());

            ((SeekBarPreferenceEx) findPreference("prefs_key_system_control_center_old_qs_grid_columns")).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) return;
                    if (progress < 3) progress = 5;
                    try {
                        Settings.Secure.putInt(requireActivity().getContentResolver(), "sysui_qqs_count", progress);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }
    }
}
