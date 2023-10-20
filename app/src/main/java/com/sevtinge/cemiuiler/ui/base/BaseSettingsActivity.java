package com.sevtinge.cemiuiler.ui.base;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.MainActivity;
import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.ShellUtils;

import java.util.ArrayList;
import java.util.List;

import moralnorm.appcompat.app.ActionBar;
import moralnorm.appcompat.app.AlertDialog;
import moralnorm.appcompat.app.AppCompatActivity;

public class BaseSettingsActivity extends AppCompatActivity {

    private String initialFragmentName;
    public BaseSettingsProxy mProxy;

    public ActionBar mActionBar;
    public static List<BaseSettingsActivity> mActivityList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        mProxy = new SettingsProxy(this);
        initialFragmentName = mProxy.getInitialFragmentName(intent);
        if (TextUtils.isEmpty(initialFragmentName)) {
            initialFragmentName = intent.getStringExtra(":android:show_fragment");
        }
        super.onCreate(savedInstanceState);
        createUiFromIntent(savedInstanceState, intent);
    }

    protected void createUiFromIntent(Bundle savedInstanceState, Intent intent) {
        mProxy.setupContentView();
        initActionBar();
        mActivityList.add(this);
        Fragment targetFragment = mProxy.getTargetFragment(this, initialFragmentName, savedInstanceState);
        if (targetFragment != null) {
            targetFragment.setArguments(mProxy.getArguments(intent));
            setFragment(targetFragment);
        }
        if (!(this instanceof MainActivity)) {
            findViewById(R.id.search_view).setVisibility(View.GONE);
        }
        showXposedActiveDialog();
    }

    private void initActionBar() {
        setDisplayHomeAsUpEnabled();
    }

    private void setDisplayHomeAsUpEnabled() {
        if (this instanceof MainActivity) {
            getAppCompatActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    public void setActionBarEndView(View view) {
        getAppCompatActionBar().setEndView(view);
    }

    public void setRestartView(View.OnClickListener l) {
        if (l != null) {
            ImageView mRestartView = new ImageView(this);
            mRestartView.setImageResource(R.drawable.ic_reboot_small);
            mRestartView.setOnClickListener(l);
            setActionBarEndView(mRestartView);
        }
    }

    public void showRestartSystemDialog() {
        showRestartDialog(true, "", "");
    }

    public void showRestartDialog(String appLabel, String packagename) {
        showRestartDialog(false, appLabel, packagename);
    }

    public void showRestartDialog(boolean isRestartSystem, String appLabel, String packagename) {
        String isSystem = getResources().getString(R.string.restart_app_desc, appLabel);
        String isOther = getResources().getString(R.string.restart_app_desc, " " + appLabel + " ");

        new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(getResources().getString(R.string.soft_reboot) + " " + appLabel)
            .setMessage(isRestartSystem ? isSystem : isOther)
            .setHapticFeedbackEnabled(true)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> doRestart(packagename, isRestartSystem))
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    public void showXposedActiveDialog() {
        if (!Helpers.isModuleActive) {
            new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.tip)
                .setMessage(R.string.hook_failed)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        }
    }

    public void setFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.frame_content, fragment)
            .commit();
    }

    public void doRestart(String packageName, boolean isRestartSystem) {
        boolean result = false;
        boolean pid = true;
        if (isRestartSystem) {
            result = ShellUtils.getResultBoolean("reboot", true);
        } else {
            ShellUtils.CommandResult commandResult = ShellUtils.execCommand("{ [[ $(pgrep -f '" + packageName +
                "' | grep -v $$) != \"\" ]] && { pkill -l 9 -f \"" + packageName +
                "\"; }; } || { echo \"kill error\"; }", true, true);
            if (commandResult.result == 0) {
                if (commandResult.successMsg.equals("kill error")) {
                    pid = false;
                } else result = true;
            }
            // result = ShellUtils.getResultBoolean("pkill -l 9 -f " + packageName, true);
        }
        if (!result) {
            new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.tip)
                .setMessage(isRestartSystem ? R.string.reboot_failed : pid ? R.string.kill_failed : R.string.pid_failed)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        }
    }
}
