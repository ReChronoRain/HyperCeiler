/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.fragment.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.SubPickerActivity;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.ui.fragment.sub.AppPicker;
import com.sevtinge.hyperceiler.utils.KillApp;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import miui.telephony.TelephonyManager;
import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class ControlCenterSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    SwitchPreference mFixMediaPanel;
    Preference mExpandNotification;
    SwitchPreference mNotice;
    SwitchPreference mNoticex;
    SeekBarPreferenceEx mNewCCGrid;
    SeekBarPreferenceEx mNewCCGridColumns;
    SwitchPreference mNewCCGridRect;
    SwitchPreference mNewCCGridLabel;
    DropDownPreference mFiveG;
    DropDownPreference mBluetoothSytle;
    SwitchPreference mRoundedRect;
    SeekBarPreferenceEx mRoundedRectRadius;
    SwitchPreference mThemeBlur;
    SwitchPreference mMusicCtrlPanelMix;

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
        mExpandNotification = findPreference("prefs_key_system_ui_control_center_expand_notification");
        mFixMediaPanel = findPreference("prefs_key_system_ui_control_center_fix_media_control_panel");
        mNewCCGrid = findPreference("prefs_key_system_control_center_cc_rows");
        mNewCCGridColumns = findPreference("prefs_key_system_control_center_cc_columns");
        mNewCCGridRect = findPreference("prefs_key_system_ui_control_center_rounded_rect");
        mNewCCGridLabel = findPreference("prefs_key_system_control_center_qs_tile_label");
        mNotice = findPreference("prefs_key_n_enable");
        mNoticex = findPreference("prefs_key_n_enable_fix");
        mBluetoothSytle = findPreference("prefs_key_system_ui_control_center_cc_bluetooth_tile_style");
        mFiveG = findPreference("prefs_key_system_control_center_5g_new_tile");
        mRoundedRect = findPreference("prefs_key_system_ui_control_center_rounded_rect");
        mRoundedRectRadius = findPreference("prefs_key_system_ui_control_center_rounded_rect_radius");
        mTaplus = findPreference("prefs_key_security_center_taplus");
        mThemeBlur = findPreference("prefs_key_system_ui_control_center_unlock_blur_supported");
        mMusicCtrlPanelMix = findPreference("prefs_key_system_ui_control_center_media_control_panel_background_mix");
        handler = new Handler();

        mExpandNotification.setOnPreferenceClickListener(
            preference -> {
                Intent intent = new Intent(getActivity(), SubPickerActivity.class);
                intent.putExtra("mode", AppPicker.LAUNCHER_MODE);
                intent.putExtra("key", preference.getKey());
                startActivity(intent);
                return true;
            }
        );

        mTaplus.setOnPreferenceChangeListener(
            (preference, o) -> {
                killTaplus();
                return true;
            }
        );

        mFixMediaPanel.setVisible(isAndroidVersion(31) || isAndroidVersion(32));
        mNewCCGrid.setVisible(!isAndroidVersion(30) && !isHyperOSVersion(1f));
        mNewCCGridColumns.setVisible(!isHyperOSVersion(1f));
        mNewCCGridRect.setVisible(!isAndroidVersion(30));
        mNewCCGridLabel.setVisible(!isHyperOSVersion(1f));
        mNotice.setVisible(!isAndroidVersion(30) && !isMoreHyperOSVersion(1f));
        mNoticex.setVisible(isMoreAndroidVersion(33));
        mBluetoothSytle.setVisible(!isAndroidVersion(30) && !isHyperOSVersion(1f));
        mFiveG.setVisible(TelephonyManager.getDefault().isFiveGCapable());
        mThemeBlur.setVisible(isMoreHyperOSVersion(1f));
        mRoundedRectRadius.setVisible(PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_ui_control_center_rounded_rect", false) && isMoreHyperOSVersion(1f));
        mMusicCtrlPanelMix.setVisible(isMoreHyperOSVersion(1f));

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
                    AndroidLogUtils.logD("SeekBarPreferenceEx", "onProgressChanged -> system_control_center_old_qs_grid_columns", t);
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
        ThreadPoolManager.getInstance().submit(() -> handler.post(() ->
            KillApp.killApps("com.miui.contentextension")));
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
