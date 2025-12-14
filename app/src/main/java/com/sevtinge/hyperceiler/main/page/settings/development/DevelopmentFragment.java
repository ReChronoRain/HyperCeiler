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

import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.fixLSPosedLogService;
import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.rootExecCmd;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;

import fan.appcompat.app.AlertDialog;

public class DevelopmentFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener {

    Preference mCmdR;
    Preference mDeleteAllDexKitCache;
    Preference mFixLsposedLog;
    Preference mClearAppProperties;

    @Override
    public int getPreferenceScreenResId() {
        return com.sevtinge.hyperceiler.R.xml.prefs_development;
    }

    @Override
    public void initPrefs() {
        mCmdR = findPreference("prefs_key_development_cmd_r");
        mDeleteAllDexKitCache = findPreference("prefs_key_development_delete_all_dexkit_cache");
        mFixLsposedLog = findPreference("prefs_key_development_fix_lsposed_log");
        mClearAppProperties = findPreference("prefs_key_development_clear_app_properties");

        mCmdR.setOnPreferenceClickListener(this);
        mDeleteAllDexKitCache.setOnPreferenceClickListener(this);
        mFixLsposedLog.setOnPreferenceClickListener(this);
        mClearAppProperties.setOnPreferenceClickListener(this);
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
                String fixReturn = fixLSPosedLogService();
                if (fixReturn.equals("SUCCESS")) {
                    Toast.makeText(getActivity(), R.string.success, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.failed) + ": " + fixReturn, Toast.LENGTH_LONG).show();
                }
            }
            case "prefs_key_development_clear_app_properties" -> {
                DialogHelper.showDialog(getActivity(), R.string.warn, R.string.clear_app_properties_desc, (dialog, which) -> {
                    rootExecCmd("resetprop -p --delete persist.hyperceiler.log.level");
                    rootExecCmd("resetprop -p --delete persist.service.hyperceiler.crash.report");
                    rootExecCmd("resetprop -p --delete persist.hyperceiler.crash.report");
                    Toast.makeText(getActivity(), R.string.clear_app_properties_success, Toast.LENGTH_LONG).show();
                });
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

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String userInput = input.getText().toString().trim();
            if (userInput.isEmpty()) {
                dialog.dismiss();
                showInDialog(callback);
            } else {
                callback.onInputReceived(userInput);
                dialog.dismiss();
            }
        }));

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
