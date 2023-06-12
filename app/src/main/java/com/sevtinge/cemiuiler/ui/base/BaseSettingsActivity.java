package com.sevtinge.cemiuiler.ui.base;

import static com.sevtinge.cemiuiler.utils.KotlinXposedHelperKt.exec;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.GlobalActions;
import com.sevtinge.cemiuiler.ui.MainActivity;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.util.ArrayList;
import java.util.List;

import moralnorm.appcompat.app.ActionBar;
import moralnorm.appcompat.app.AlertDialog;
import moralnorm.appcompat.app.AppCompatActivity;
import moralnorm.internal.utils.ViewUtils;

public class BaseSettingsActivity extends AppCompatActivity {

    private String initialFragmentName;
    public BaseSettingsProxy mProxy;

    public ActionBar mActionBar;
    public static List<BaseSettingsActivity> mActivityList = new ArrayList<>();

    public String PACKAGENAME_SYSTEM_UI = "com.android.systemui";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ViewUtils.isNightMode(this) ? R.style.AppTheme_Dark : R.style.AppTheme);
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
        new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(getResources().getString(R.string.soft_reboot) + " " + appLabel)
            .setMessage(getResources().getString(R.string.restart_app_desc1) + appLabel + getResources().getString(R.string.restart_app_desc2))
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
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .show();
        }
    }

    public void setFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.frame_content, fragment)
            .commit();
    }

    public void doRestart(String packagename, boolean isRestartSystem) {
        if (isRestartSystem) {
            restartSystem();
        } else if (packagename.equals(PACKAGENAME_SYSTEM_UI)) {
            restartSystemUI();
        } else {
            restartApps(packagename);
        }
    }

    public void restartSystem() {
        exec("reboot");
    }

    public void restartSystemUI() {
        sendBroadcast(new Intent(GlobalActions.ACTION_PREFIX + "RestartSystemUI"));
    }

    public void restartApps(String packagename) {
        Intent intent = new Intent(GlobalActions.ACTION_PREFIX + "RestartApps");
        intent.putExtra("packageName", packagename);
        sendBroadcast(intent);
    }
}
