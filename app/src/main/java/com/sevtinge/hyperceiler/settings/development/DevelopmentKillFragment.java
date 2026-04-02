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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.settings.development;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.utils.PermissionUtils;
import com.sevtinge.hyperceiler.common.utils.shell.ShellExec;
import com.sevtinge.hyperceiler.common.utils.shell.ShellInit;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import com.sevtinge.hyperceiler.libhook.utils.api.ToastHelper;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import com.sevtinge.hyperceiler.model.data.AppData;
import com.sevtinge.hyperceiler.utils.PackagesUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import fan.appcompat.app.AlertDialog;

public class DevelopmentKillFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener {
    private static final int REQUEST_GET_INSTALLED_APPS = 1203;

    private List<AppData> appData = new ArrayList<>();
    private boolean init = false;
    Handler handler;
    ShellExec mShell;
    Preference mKillPackage;

    Preference mName;
    Preference mCheck;

    public interface EditDialogCallback {
        void onInputReceived(String userInput);
    }

    @Override
    public int getPreferenceScreenResId() {
        return com.sevtinge.hyperceiler.R.xml.prefs_development_kill;
    }

    @Override
    public void initPrefs() {
        mCheck = findPreference("prefs_key_development_kill_find_process");
        mKillPackage = findPreference("prefs_key_development_kill_package");
        mName = findPreference("prefs_key_development_kill_app_name");
        ExecutorService executorService = ThreadPoolManager.getInstance();
        handler = new Handler(requireContext().getMainLooper());
        if (ensureInstalledAppsPermission()) {
            ToastHelper.makeText(ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP), getString(R.string.development_kill_loading_data));
            initApp(executorService);
        }
        mCheck.setOnPreferenceClickListener(this);
        mName.setOnPreferenceClickListener(this);
        mKillPackage.setOnPreferenceClickListener(this);
        mShell = ShellInit.getShell();
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (!init) {
            showOutDialog(getString(R.string.development_kill_resource_not_ready));
            return true;
        }
        switch (preference.getKey()) {
            case "prefs_key_development_kill_find_process" -> showInDialog(new EditDialogCallback() {
                @Override
                public void onInputReceived(String userInput) {
                    String pkg = "";
                    for (AppData appData1 : appData) {
                        if (appData1.label.equalsIgnoreCase(userInput)) {
                            pkg = appData1.packageName;
                        }
                    }
                    if (!(pkg == null || pkg.isEmpty())) {
                        showOutDialog(listToString(getString(R.string.development_kill_process_header),
                            pidAndPkg(pkg)));
                        return;
                    }
                    showOutDialog(getString(R.string.development_kill_package_invalid_for_find, userInput));
                }
            });
            case "prefs_key_development_kill_package" -> showInDialog(new EditDialogCallback() {
                @Override
                public void onInputReceived(String userInput) {
                    if (!userInput.isEmpty()) {
                        String pkg = "";
                        for (AppData appData1 : appData) {
                            if (appData1.packageName.equalsIgnoreCase(userInput)) {
                                pkg = appData1.packageName;
                            }
                        }
                        if (pkg.isEmpty()) {
                            showOutDialog(getString(R.string.development_kill_package_invalid, userInput));
                            return;
                        }
                        if (!pidAndPkg(pkg).isEmpty()) {
                            String result = listToString(getString(R.string.development_kill_success_prefix), pidAndPkg(pkg));
                            if (killPackage(pkg)) {
                                showOutDialog(result);
                            } else {
                                showOutDialog(getString(R.string.development_kill_failed_with_pkg, pkg));
                            }
                        } else {
                            showOutDialog(getString(R.string.development_kill_process_not_found, userInput));
                        }
                    }
                }
            });
            case "prefs_key_development_kill_app_name" -> showInDialog(new EditDialogCallback() {
                @Override
                public void onInputReceived(String userInput) {
                    if (!userInput.isEmpty()) {
                        String pkg = "";
                        for (AppData appData1 : appData) {
                            if (appData1.label.equalsIgnoreCase(userInput)) {
                                pkg = appData1.packageName;
                            }
                        }
                        if (!(pkg == null || pkg.isEmpty())) {
                            if (!pidAndPkg(pkg).isEmpty()) {
                                String result = listToString(getString(R.string.development_kill_success_prefix), pidAndPkg(pkg));
                                if (killPackage(pkg)) {
                                    showOutDialog(result);
                                } else {
                                    showOutDialog(getString(R.string.development_kill_failed_with_pkg, pkg));
                                }
                            } else {
                                showOutDialog(getString(R.string.development_kill_process_not_found, userInput));
                            }
                        } else
                            showOutDialog(getString(R.string.development_kill_package_invalid, userInput));
                    }
                }
            });
        }
        return true;
    }

    private ArrayList<String> pidAndPkg(String pkg) {
        mShell.add("pid=$(ps -A -o PID,ARGS=CMD | grep \"" + pkg + "\" | grep -v \"grep\")")
            .add("if [[ $pid == \"\" ]]; then")
            .add(" echo \"No Find Pid!\"")
            .add("else")
            .add(" ps -A -o PID,ARGS=CMD | grep \"" + pkg + "\" | grep -v \"grep\"")
            .add("fi").over().sync();
        ArrayList<String> pid = mShell.getOutPut();
        if (pid.isEmpty()) {
            return new ArrayList<>();
        }
        if (pid.get(0).equals("No Find Pid!")) {
            return new ArrayList<>();
        }
        return pid;
    }

    private String listToString(String title, ArrayList<String> arrayList) {
        StringBuilder s = new StringBuilder(title);
        for (int i = 0; i < arrayList.size(); i++) {
            s.append(arrayList.get(i)).append("\n");
        }
        return s.toString();
    }

    private boolean killPackage(String kill) {
        return AppsTool.killApps(kill);
    }

    private void initApp(ExecutorService executorService) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                appData = PackagesUtils.getInstalledPackagesByFlag(0);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        init = true;
                        ToastHelper.makeText(ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP), getString(R.string.development_kill_loading_done));
                    }
                });
            }
        });
    }

    private void showInDialog(EditDialogCallback callback) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.edit_dialog, null);
        EditText input = view.findViewById(R.id.title);
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.kill_1)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                String userInput = input.getText().toString();
                if (userInput.isEmpty()) {
                    dialog.dismiss();
                    showInDialog(callback);
                    return;
                }
                callback.onInputReceived(userInput);
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
            .show();
    }

    private void showOutDialog(String show) {
        new AlertDialog.Builder(getActivity())
            .setCancelable(false)
            .setTitle(R.string.edit_out)
            .setMessage(show)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private boolean ensureInstalledAppsPermission() {
        if (PermissionUtils.canReadInstalledApps(requireContext())) {
            return true;
        }
        requestPermissions(
            new String[]{PermissionUtils.PERMISSION_GET_INSTALLED_APPS},
            REQUEST_GET_INSTALLED_APPS
        );
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_GET_INSTALLED_APPS) {
            return;
        }

        if (PermissionUtils.canReadInstalledApps(requireContext())
            || PermissionUtils.isInstalledAppsPermissionGranted(permissions, grantResults)) {
            ToastHelper.makeText(ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP), getString(R.string.development_kill_loading_data));
            initApp(ThreadPoolManager.getInstance());
            return;
        }

        showOutDialog(getString(R.string.development_kill_permission_not_granted));
    }
}
