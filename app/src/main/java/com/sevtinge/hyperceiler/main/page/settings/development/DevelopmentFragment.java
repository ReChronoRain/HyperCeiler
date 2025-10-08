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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.main.page.settings.development;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.fixLsposedLogService;
import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.rootExecCmd;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.utils.pkg.DebugModeUtils;
import com.sevtinge.hyperceiler.ui.R;

import fan.appcompat.app.AlertDialog;
import fan.preference.DropDownPreference;

public class DevelopmentFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener {

    Preference mCmdR;
    Preference mDeleteAllDexKitCache;
    Preference mFixLsposedLog;
    SwitchPreference mDebugMode;
    DropDownPreference mHomeVersion;
    EditTextPreference mEditHomeVersion;

    public interface EditDialogCallback {
        void onInputReceived(String command);
    }

    @Override
    public int getPreferenceScreenResId() {
        return com.sevtinge.hyperceiler.R.xml.prefs_development;
    }

    @Override
    public void initPrefs() {
        mCmdR = findPreference("prefs_key_development_cmd_r");
        mDeleteAllDexKitCache = findPreference("prefs_key_development_delete_all_dexkit_cache");
        mFixLsposedLog = findPreference("prefs_key_development_fix_lsposed_log");
        mDebugMode = findPreference("prefs_key_development_debug_mode");

        mHomeVersion = findPreference("prefs_key_debug_mode_home");
        mEditHomeVersion = findPreference("prefs_key_debug_mode_home_edit");

        mCmdR.setOnPreferenceClickListener(this);
        mDeleteAllDexKitCache.setOnPreferenceClickListener(this);
        mFixLsposedLog.setOnPreferenceClickListener(this);

        mDebugMode.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isDebug = (boolean) newValue;
            if (isDebug) {
                DialogHelper.showDialog(getActivity(), R.string.tip, R.string.open_debug_mode_tips, (dialog, which) -> {
                    /*Toast.makeText(getActivity(), R.string.feature_doing_func, Toast.LENGTH_LONG).show();
                    mDebugMode.setChecked(false);*/
                    dialog.dismiss();
                }, (dialog, which) -> {
                    mDebugMode.setChecked(false);
                    dialog.dismiss();
                });
            }
            return true;
        });

        int getValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_debug_mode_home", "0"));

        if (isPad()) {
            mHomeVersion.setEntries(R.array.debug_mode_home_pad);
            mHomeVersion.setEntryValues(R.array.debug_mode_home_pad_value);
        }

        mEditHomeVersion.setVisible(getValue == 1);

        mHomeVersion.setOnPreferenceChangeListener((preference, newValue) -> {
            int isNewValue = Integer.parseInt((String) newValue);
            mEditHomeVersion.setVisible(isNewValue == 1);
            if (isNewValue != 1) {
                DebugModeUtils.INSTANCE.setChooseResult("com.miui.home", isNewValue);
            }
            return true;
        });

        mEditHomeVersion.setOnPreferenceChangeListener((preference, newValue) -> {
            int isNewValue = Integer.parseInt((String) newValue);
            DebugModeUtils.INSTANCE.setChooseResult("com.miui.home", isNewValue);
            return true;
        });
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case "prefs_key_development_cmd_r" ->
                    showInDialog(command -> showOutDialog(rootExecCmd(command)));
            case "prefs_key_development_delete_all_dexkit_cache" ->
                    DialogHelper.showDialog(getActivity(), R.string.warn, R.string.delete_all_dexkit_cache_desc, (dialog, which) -> {
                        DexKit.deleteAllCache(requireActivity());
                        Toast.makeText(getActivity(), R.string.delete_all_dexkit_cache_success, Toast.LENGTH_LONG).show();
                    });
            case "prefs_key_development_fix_lsposed_log" -> {
                String fixReturn = fixLsposedLogService();
                if (fixReturn.equals("SUCCESS")) {
                    Toast.makeText(getActivity(), R.string.success, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.failed) + ": " + fixReturn, Toast.LENGTH_LONG).show();
                }
            }
        }
        return true;
    }

    private void showInDialog(DevelopmentKillFragment.EditDialogCallback callback) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.edit_dialog, null);
        EditText input = view.findViewById(R.id.title);

        AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                .setTitle("# root@HyperCeiler > Input")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String userInput = input.getText().toString().trim();
                if (userInput.isEmpty()) {
                    dialog.dismiss();
                    showInDialog(callback);
                } else {
                    callback.onInputReceived(userInput);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void showOutDialog(String show) {
        new AlertDialog.Builder(requireActivity())
                .setCancelable(false)
                .setTitle("# root@HyperCeiler > Output")
                .setMessage(show)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
