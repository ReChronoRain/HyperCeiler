package com.sevtinge.hyperceiler.ui.fragment.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.PrefsUtils;
import com.sevtinge.hyperceiler.utils.ShellUtils;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import miui.telephony.TelephonyManager;
import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class ControlCenterSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    SwitchPreference mFixMediaPanel;
    SwitchPreference mNotice;
    SwitchPreference mNoticex;
    SeekBarPreferenceEx mNewCCGrid;
    SwitchPreference mNewCCGridRect;
    SwitchPreference mNewCCGridLabel;
    DropDownPreference mFiveG;
    DropDownPreference mBluetoothSytle;
    SwitchPreference mRoundedRect;
    SeekBarPreferenceEx mRoundedRectRadius;

    SwitchPreference mTaplus;
    Handler handler;

    // 临时的，旧控制中心
    SwitchPreference mOldCCGrid;
    SwitchPreference mOldCCGrid1;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_control_center;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }

    @Override
    public void initPrefs() {
        mFixMediaPanel = findPreference("prefs_key_system_ui_control_center_fix_media_control_panel");
        mNewCCGrid = findPreference("prefs_key_system_control_center_cc_rows");
        mNewCCGridRect = findPreference("prefs_key_system_ui_control_center_rounded_rect");
        mNewCCGridLabel = findPreference("prefs_key_system_control_center_qs_tile_label");
        mNotice = findPreference("prefs_key_n_enable");
        mNoticex = findPreference("prefs_key_n_enable_fix");
        mBluetoothSytle = findPreference("prefs_key_system_ui_control_center_cc_bluetooth_tile_style");
        mFiveG = findPreference("prefs_key_system_control_center_5g_new_tile");
        mRoundedRect = findPreference("prefs_key_system_ui_control_center_rounded_rect");
        mRoundedRectRadius = findPreference("prefs_key_system_ui_control_center_rounded_rect_radius");
        mTaplus = findPreference("prefs_key_security_center_taplus");
        handler = new Handler();

        mTaplus.setOnPreferenceChangeListener(
            (preference, o) -> {
                killTaplus();
                return true;
            }
        );

        mFixMediaPanel.setVisible(isAndroidVersion(31) || isAndroidVersion(32));
        mNewCCGrid.setVisible(!isAndroidVersion(30) && !isHyperOSVersion(1f));
        mNewCCGridRect.setVisible(!isAndroidVersion(30));
        mNewCCGridLabel.setVisible(!isHyperOSVersion(1f));
        mNotice.setVisible(!isAndroidVersion(30));
        mNoticex.setVisible(isMoreAndroidVersion(33));
        mBluetoothSytle.setVisible(!isAndroidVersion(30) && !isHyperOSVersion(1f));
        mFiveG.setVisible(TelephonyManager.getDefault().isFiveGCapable());
        mRoundedRectRadius.setVisible(PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_ui_control_center_rounded_rect", false) && isMoreHyperOSVersion(1f));

        mOldCCGrid = findPreference("prefs_key_system_control_center_old_enable");
        mOldCCGrid1 = findPreference("prefs_key_system_control_center_old_enable_1");

        mOldCCGrid.setVisible(isMoreAndroidVersion(33));
        mOldCCGrid1.setVisible(!isMoreAndroidVersion(33));

        mRoundedRect.setOnPreferenceChangeListener(this);

        ((SeekBarPreferenceEx) findPreference("prefs_key_system_control_center_old_qs_grid_columns")).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (progress < 3) progress = 5;
                try {
                    Settings.Secure.putInt(requireActivity().getContentResolver(), "sysui_qqs_count", progress);
                } catch (Throwable t) {
                    AndroidLogUtils.LogD("SeekBarPreferenceEx", "onProgressChanged -> system_control_center_old_qs_grid_columns", t);
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

    public void killTaplus() {
        new Thread(() -> handler.post(() ->
            ShellUtils.execCommand("killall -s 9 com.miui.contentextension",
                true, false))).start();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mRoundedRect) {
            setCanBeVisible((Boolean) o);
        }
        return true;
    }

    private void setCanBeVisible(boolean mode) {
        mRoundedRectRadius.setVisible(mode && isMoreHyperOSVersion(1f));
    }
}
