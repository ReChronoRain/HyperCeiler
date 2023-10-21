package com.sevtinge.cemiuiler.ui.fragment.systemui;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidS;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidSv2;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.log.AndroidLogUtils;

import miui.telephony.TelephonyManager;
import moralnorm.preference.DropDownPreference;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class ControlCenterSettings extends SettingsPreferenceFragment {

    SwitchPreference mFixMediaPanel;
    SwitchPreference mNotice;
    SwitchPreference mNoticex;
    SeekBarPreferenceEx mNewCCGrid;
    SwitchPreference mNewCCGridRect;
    SwitchPreference mFiveG;
    DropDownPreference mBluetoothSytle;

    // 临时的，旧控制中心
    SwitchPreference mOldCCGrid;
    SwitchPreference mOldCCGrid1;

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
        mNoticex = findPreference("prefs_key_n_enable_fix");
        mBluetoothSytle = findPreference("prefs_key_system_ui_control_center_cc_bluetooth_tile_style");
        mFiveG = findPreference("prefs_key_system_control_center_5g_tile");

        mFixMediaPanel.setVisible(isAndroidS() || isAndroidSv2());
        mNewCCGrid.setVisible(!isAndroidR());
        mNewCCGridRect.setVisible(!isAndroidR());
        mNotice.setVisible(!isAndroidR());
        mNoticex.setVisible(isMoreAndroidVersion(33));
        mBluetoothSytle.setVisible(!isAndroidR());
        mFiveG.setVisible(TelephonyManager.getDefault().isFiveGCapable());

        mOldCCGrid = findPreference("prefs_key_system_control_center_old_enable");
        mOldCCGrid1 = findPreference("prefs_key_system_control_center_old_enable_1");

        mOldCCGrid.setVisible(isMoreAndroidVersion(33));
        mOldCCGrid1.setVisible(!isMoreAndroidVersion(33));

        ((SeekBarPreferenceEx) findPreference("prefs_key_system_control_center_old_qs_grid_columns")).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (progress < 3) progress = 5;
                try {
                    Settings.Secure.putInt(requireActivity().getContentResolver(), "sysui_qqs_count", progress);
                } catch (Throwable t) {
                    AndroidLogUtils.LogD("SeekBarPreferenceEx",  "onProgressChanged -> system_control_center_old_qs_grid_columns", t);
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
