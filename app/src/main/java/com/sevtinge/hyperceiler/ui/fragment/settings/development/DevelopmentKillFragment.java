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
package com.sevtinge.hyperceiler.ui.fragment.settings.development;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.AppData;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.ContextUtils;
import com.sevtinge.hyperceiler.utils.PackageManagerUtils;
import com.sevtinge.hyperceiler.utils.ShellUtils;
import com.sevtinge.hyperceiler.utils.ToastHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import moralnorm.appcompat.app.AlertDialog;
import moralnorm.preference.Preference;

public class DevelopmentKillFragment extends SettingsPreferenceFragment {
    public List<AppData> appData = new ArrayList<>();
    Preference mKillPackage;

    Preference mName;
    Preference mCheck;
    ExecutorService executorService;
    Handler handler;

    public interface EditDialogCallback {
        void onInputReceived(String userInput);
    }

    public interface KillCallback {
        void onKillCallback(String kill, String rest);
    }

    public interface GetCallback {
        void onCallback(Boolean boo, String rest);
    }

    @Override
    public int getContentResId() {
        return R.xml.prefs_development_kill;
    }

    @Override
    public void initPrefs() {
        mKillPackage = findPreference("prefs_key_development_kill_package");
        mName = findPreference("prefs_key_development_kill_name");
        mCheck = findPreference("prefs_key_development_kill_check");
        ToastHelper.makeText(ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP), "加载数据，请稍后");
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        handler = new Handler();
        initApp(executorService);
        mCheck.setOnPreferenceClickListener(
            preference -> {
                showInDialog(
                    userInput -> {
                        String pkg = "";
                        for (AppData appData1 : appData) {
                            if (appData1.label.equals(userInput)) {
                                pkg = appData1.packageName;
                            }
                        }
                        if (!(pkg == null || pkg.equals(""))) {
                            showOutDialog(getPackage(pkg, true, null));
                            return;
                        }
                        showOutDialog(getPackage(userInput, true, null));
                    }
                );
                return true;
            }
        );
        mName.setOnPreferenceClickListener(
            preference -> {
                showInDialog(
                    userInput -> {
                        if (!userInput.equals("")) {
                            String pkg = "";
                            for (AppData appData1 : appData) {
                                if (appData1.label.equals(userInput)) {
                                    pkg = appData1.packageName;
                                }
                            }
                            if (!(pkg == null || pkg.equals(""))) {
                                String finalPkg = pkg;
                                getAndKill(pkg, (boo, rest) -> {
                                    if (boo) {
                                        showOutDialog("kill success: " + rest);
                                    } else {
                                        showOutDialog("kill error: " + userInput + "\npkg: " + finalPkg);
                                    }
                                });
                            } else showOutDialog("kill error maybe not present: " + userInput);
                        }
                    }
                );
                return true;
            }
        );
        mKillPackage.setOnPreferenceClickListener(
            preference -> {
                showInDialog(
                    userInput -> {
                        if (!userInput.equals("")) {
                            getAndKill(userInput, (boo, rest) -> {
                                if (boo) {
                                    showOutDialog("kill success: " + rest);
                                } else showOutDialog("kill error: " + userInput);
                            });
                        }
                    }
                );
                return true;
            }
        );
    }

    private void getAndKill(String pkg, GetCallback getCallback) {
        getPackage(pkg, false, (kill, rest) -> {
                getCallback.onCallback(killPackage(kill), rest);
            }
        );
    }

    private boolean killPackage(String kill) {
        ShellUtils.CommandResult commandResult =
            ShellUtils.execCommand(
                "{ pid=$(pgrep -f '" + kill + "' | grep -v $$);" +
                    " [[ $pid != \"\" ]] && { pkill -l 9 -f \"" + kill + "\";" +
                    " { [[ $? != 0 ]] && { killall -s 9 \"" + kill + "\" &>/dev/null;};}" +
                    " || { { for i in $pid; do kill -s 9 \"$i\" &>/dev/null;done;};}" +
                    " || { echo \"kill error\";};};}" +
                    " || { echo \"kill error\";}",
                true, true);
        if (commandResult.result == 0) {
            return !commandResult.successMsg.equals("kill error");
        } else
            return false;
    }

    private String getPackage(String pkg, boolean ned, KillCallback killCallback) {
        ShellUtils.CommandResult commandResult = ShellUtils.execCommand(
            "pid=$(ps -A -o PID,ARGS=CMD | grep \"" + pkg + "\" | grep -v \"grep\");" +
                " get=\"\"; for i in $pid; do if [[ $(echo $i | grep '[0-9]' 2>/dev/null) == \"\" ]];" +
                " then if [[ $get == \"\" ]]; then get=$i; else get=\"$get฿$i\";" +
                " fi; fi; done; echo $get\n", true, true);
        if (ned) return commandResult.successMsg.replace("฿", "\n");
        if (commandResult.result == 0) {
            killCallback.onKillCallback(pkg, commandResult.successMsg.replace("฿", "\n"));
        }
        return null;
    }

    private void initApp(ExecutorService executorService) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                appData = PackageManagerUtils.getPackageByFlag(0);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ToastHelper.makeText(ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP), "加载完毕");
                    }
                });
            }
        });
       /* AsyncTask 已经弃用
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
                if (userInput.equals("")) {
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
