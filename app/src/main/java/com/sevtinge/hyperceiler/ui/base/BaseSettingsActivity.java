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
package com.sevtinge.hyperceiler.ui.base;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import java.util.ArrayList;
import java.util.List;

import moralnorm.appcompat.app.AlertDialog;

public abstract class BaseSettingsActivity extends BaseActivity {

    private String initialFragmentName;
    public Fragment mFragment;
    public static List<BaseSettingsActivity> mActivityList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        initialFragmentName = mProxy.getInitialFragmentName(intent);
        if (TextUtils.isEmpty(initialFragmentName)) {
            initialFragmentName = intent.getStringExtra(":android:show_fragment");
        }
        createUiFromIntent(savedInstanceState, intent);
    }

    protected void createUiFromIntent(Bundle savedInstanceState, Intent intent) {
        mProxy.setupContentView();
        mActivityList.add(this);
        Fragment targetFragment = mProxy.getTargetFragment(this, initialFragmentName, savedInstanceState);
        if (targetFragment != null) {
            targetFragment.setArguments(mProxy.getArguments(intent));
            setFragment(targetFragment);
        }
    }

    public void showRestartSystemDialog() {
        showRestartDialog(true, "", new String[]{""});
    }

    public void showRestartDialog(String appLabel, String packageName) {
        showRestartDialog(false, appLabel, new String[]{packageName});
    }

    public void showRestartDialog(String appLabel, String[] packageName) {
        showRestartDialog(false, appLabel, packageName);
    }

    public void showRestartDialog(boolean isRestartSystem, String appLabel, String[] packageName) {
        String isSystem = getResources().getString(R.string.restart_app_desc, appLabel);
        String isOther = getResources().getString(R.string.restart_app_desc, " " + appLabel + " ");

        new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(getResources().getString(R.string.soft_reboot) + " " + appLabel)
            .setMessage(isRestartSystem ? isSystem : isOther)
            .setHapticFeedbackEnabled(true)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> doRestart(packageName, isRestartSystem))
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    public void setFragment(Fragment fragment) {
        mFragment = fragment;
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.frame_content, fragment)
            .commit();
    }

    public Fragment getFragment() {
        return mFragment;
    }

    public void doRestart(String[] packageName, boolean isRestartSystem) {
        boolean result = false;
        boolean pid = true;
        if (isRestartSystem) {
            result = ShellInit.getShell().run("reboot").sync().isResult();
        } else {
            if (packageName != null) {
                for (String packageGet : packageName) {
                    if (packageGet == null) {
                        continue;
                    }
                    // String test = "XX";
                    // ShellUtils.CommandResult commandResult = ShellUtils.execCommand("{ [[ $(pgrep -f '" + packageGet +
                    //     "' | grep -v $$) != \"\" ]] && { pkill -l 9 -f \"" + packageGet +
                    //     "\"; }; } || { echo \"kill error\"; }", true, true);

                    boolean getResult =
                        ShellInit.getShell().add("pid=$(pgrep -f \"" + packageGet + "\" | grep -v $$)")
                            .add("if [[ $pid == \"\" ]]; then")
                            .add(" pids=\"\"")
                            .add(" pid=$(ps -A -o PID,ARGS=CMD | grep \"" + packageGet + "\" | grep -v \"grep\")")
                            .add("  for i in $pid; do")
                            .add("   if [[ $(echo $i | grep '[0-9]' 2>/dev/null) != \"\" ]]; then")
                            .add("    if [[ $pids == \"\" ]]; then")
                            .add("      pids=$i")
                            .add("    else")
                            .add("      pids=\"$pids $i\"")
                            .add("    fi")
                            .add("   fi")
                            .add("  done")
                            .add("fi")
                            .add("if [[ $pids != \"\" ]]; then")
                            .add(" pid=$pids")
                            .add("fi")
                            .add("if [[ $pid != \"\" ]]; then")
                            .add(" for i in $pid; do")
                            .add("  kill -s 15 $i &>/dev/null")
                            .add(" done")
                            .add("else")
                            .add(" echo \"No Find Pid!\"")
                            .add("fi").over().sync().isResult();
                    ArrayList<String> outPut = ShellInit.getShell().getOutPut();
                    ArrayList<String> error = ShellInit.getShell().getError();

                    if (getResult) {
                        if (outPut.size() != 0) {
                            if (outPut.get(0).equals("No Find Pid!")) {
                                pid = false;
                            } else {
                                result = true;
                            }
                        } else result = true;
                    } else
                        AndroidLogUtils.logE("doRestart: ", "result: " + ShellInit.getShell().getResult() +
                            " errorMsg: " + error + " package: " + packageGet);

                }
            } else {
                AndroidLogUtils.logE("doRestart: ", "packageName is null");
            }
            // result = ShellUtils.getResultBoolean("pkill -l 9 -f " + packageName, true);
        }
        if (!result) {
            new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.tip)
                .setMessage(isRestartSystem ? R.string.reboot_failed :
                    pid ? R.string.kill_failed : R.string.pid_failed)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        }
    }
}
