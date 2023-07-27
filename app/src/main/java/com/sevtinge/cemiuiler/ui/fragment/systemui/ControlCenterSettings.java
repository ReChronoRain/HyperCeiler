package com.sevtinge.cemiuiler.ui.fragment.systemui;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidS;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidSv2;

import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;

import com.sevtinge.cemiuiler.R;
import moralnorm.preference.SeekBarPreferenceEx;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class ControlCenterSettings extends SettingsPreferenceFragment {

    SwitchPreference mFixMediaPanel;
    SwitchPreference mNotice;
    SeekBarPreferenceEx mNewCCGrid;
    SwitchPreference mNewCCGridRect;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_control_center;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }

    @Override
    public void initPrefs() {
        mFixMediaPanel = findPreference("prefs_key_system_ui_control_center_fix_media_control_panel");
        mNewCCGrid = findPreference("prefs_key_system_control_center_cc_rows");
        mNewCCGridRect = findPreference("prefs_key_system_ui_control_center_rounded_rect");
        mNotice = findPreference("prefs_key_n_enable");

        mFixMediaPanel.setVisible(isAndroidS() || isAndroidSv2());
        mNewCCGrid.setVisible(!isAndroidR());
        mNewCCGridRect.setVisible(!isAndroidR());
        mNotice.setVisible(!isAndroidR());

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
