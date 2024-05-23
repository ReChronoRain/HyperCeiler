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
package com.sevtinge.hyperceiler.ui.fragment.base;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logW;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.settings.SubSettings;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import java.util.ArrayList;

import fan.appcompat.app.AlertDialog;

public abstract class SettingsPreferenceFragment extends BasePreferenceFragment {

    public final String TAG = getClass().getSimpleName();
    public MenuItem mRestartMenu;
    public String mTitle;
    public String mPreferenceKey;
    public int mContentResId = 0;
    public int mTitleResId = 0;
    private boolean mPreferenceHighlighted = false;
    private final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        highlightPreferenceIfNeeded(mPreferenceKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY);
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        super.onCreatePreferences(bundle, s);
        Bundle args = getArguments();
        if (args != null) {
            mTitle = args.getString(":fragment:show_title");
            mTitleResId = args.getInt(":fragment:show_title_resid");
            mPreferenceKey = args.getString(":settings:fragment_args_key");
            mContentResId = args.getInt("contentResId");
        }
        if (mTitleResId != 0) setTitle(mTitleResId);
        if (!TextUtils.isEmpty(mTitle)) setTitle(mTitle);
        mContentResId = mContentResId != 0 ? mContentResId : getContentResId();
        if (mContentResId > 0) {
            setPreferencesFromResource(mContentResId, s);
            initPrefs();
        }
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
    }

    public void highlightPreferenceIfNeeded(String key) {
        if (isAdded() && !mPreferenceHighlighted && !TextUtils.isEmpty(key)) {
            requestHighlight(key);
            mPreferenceHighlighted = true;
        }
    }

    public SubSettings getSubSettings() {
        return (SubSettings) getActivity();
    }

    public View.OnClickListener addRestartListener() {
        return null;
    }

    public SharedPreferences getSharedPreferences() {
        return PrefsUtils.mSharedPreferences;
    }

    public boolean hasKey(String key) {
        return getSharedPreferences().contains(key);
    }

    public void initPrefs() {
    }

    public abstract int getContentResId();

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_sub, menu);
        mRestartMenu = menu.findItem(R.id.restart);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item == mRestartMenu) {

        }
        return super.onOptionsItemSelected(item);
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

        new AlertDialog.Builder(requireContext())
                .setCancelable(false)
                .setTitle(getResources().getString(R.string.soft_reboot) + " " + appLabel)
                .setMessage(isRestartSystem ? isSystem : isOther)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> doRestart(packageName, isRestartSystem))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
                        if (!outPut.isEmpty()) {
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
            new AlertDialog.Builder(requireContext())
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
