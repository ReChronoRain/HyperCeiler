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
package com.sevtinge.hyperceiler.ui.page.settings.development;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.common.model.data.AppData;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.hook.utils.ContextUtils;
import com.sevtinge.hyperceiler.hook.utils.KillApp;
import com.sevtinge.hyperceiler.common.utils.PackagesUtils;
import com.sevtinge.hyperceiler.hook.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.hook.utils.ToastHelper;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellExec;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellInit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import fan.appcompat.app.AlertDialog;

public class DevelopmentKillFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener {
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
        return R.xml.prefs_development_kill;
    }

    @Override
    public void initPrefs() {
        mCheck = findPreference("prefs_key_development_kill_find_process");
        mKillPackage = findPreference("prefs_key_development_kill_package");
        mName = findPreference("prefs_key_development_kill_app_name");
        ToastHelper.makeText(ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP), "加载数据，请稍后");
        ExecutorService executorService = ThreadPoolManager.getInstance();
        handler = new Handler(requireContext().getMainLooper());
        initApp(executorService);
        mCheck.setOnPreferenceClickListener(this);
        mName.setOnPreferenceClickListener(this);
        mKillPackage.setOnPreferenceClickListener(this);
        mShell = ShellInit.getShell();
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (!init) {
            showOutDialog("资源尚未加载完毕，请稍后！");
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
                        showOutDialog(listToString("PID：       Process：\n",
                            pidAndPkg(pkg)));
                        return;
                    }
                    showOutDialog("包名错误或不存在，无法查找！\n" + "\"" + userInput + "\"");
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
                            showOutDialog("包名错误或不存在，请查证后输入！\n" + "\"" + userInput + "\"");
                            return;
                        }
                        if (!pidAndPkg(pkg).isEmpty()) {
                            String result = listToString("成功 Kill：\n", pidAndPkg(pkg));
                            if (killPackage(pkg)) {
                                showOutDialog(result);
                            } else {
                                showOutDialog("Kill: " + pkg + " 失败！");
                            }
                        } else {
                            showOutDialog("未找到当前包名有任何正在运行的进程！\n" + "\"" + userInput + "\"");
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
                                String result = listToString("成功 Kill：\n", pidAndPkg(pkg));
                                if (killPackage(pkg)) {
                                    showOutDialog(result);
                                } else {
                                    showOutDialog("Kill: " + pkg + " 失败！");
                                }
                            } else {
                                showOutDialog("未找到当前包名有任何正在运行的进程！\n" + "\"" + userInput + "\"");
                            }
                        } else
                            showOutDialog("包名错误或不存在，请查证后输入！\n" + "\"" + userInput + "\"");
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
        return KillApp.killApps(kill);
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
                        ToastHelper.makeText(ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP), "加载完毕");
                    }
                });
            }
        });
       /*AsyncTask 已经弃用
         new AsyncTask<Void, Void, List<AppData>>() {
            @Override
            protected List<AppData> doInBackground(Void... voids) {
                // 在后台线程中执行耗时任务
                return PackageManagerUtils.getPackageByFlag(0);
            }

            @Override
            protected void onPostExecute(List<AppData> result) {
                ToastHelper.makeText(ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP), "加载完毕");
                // 在UI线程更新UI
                appData = result;
            }
        }.execute();*/
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
}
