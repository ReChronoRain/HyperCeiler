package com.sevtinge.hyperceiler.ui.base;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.ShellUtils;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

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
            result = ShellUtils.getResultBoolean("reboot", true);
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
                    ShellUtils.CommandResult commandResult =
                        ShellUtils.execCommand("{ pid=$(pgrep -f '" + packageGet + "' | grep -v $$);" +
                                " [[ $pid != \"\" ]] && { pkill -l 9 -f \"" + packageGet + "\";" +
                                " { [[ $? != 0 ]] && { killall -s 9 \"" + packageGet + "\" &>/dev/null;};}" +
                                " || { { for i in $pid; do kill -s 9 \"$i\" &>/dev/null;done;};}" +
                                " || { echo \"kill error\";};};}" +
                                " || { echo \"kill error\";}",
                            true, true);
                    if (commandResult.result == 0) {
                        if (commandResult.successMsg.equals("kill error")) {
                            pid = false;
                        } else result = true;
                    } else
                        AndroidLogUtils.LogE("doRestart: ", "result: " + commandResult.result +
                            " errorMsg: " + commandResult.errorMsg + " package: " + packageGet, null);
                }
            } else {
                AndroidLogUtils.LogE("doRestart: ", "packageName is null", null);
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
