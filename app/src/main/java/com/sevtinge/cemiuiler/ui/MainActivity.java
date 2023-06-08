package com.sevtinge.cemiuiler.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.GlobalActions;
import com.sevtinge.cemiuiler.ui.main.base.BaseMainActivity;
import com.sevtinge.cemiuiler.ui.main.fragment.MainFragment;
import com.sevtinge.cemiuiler.utils.ALPermissionManager;
import com.sevtinge.cemiuiler.view.RestartAlertDialog;

import java.util.Arrays;
import java.util.List;

import moralnorm.appcompat.app.AlertDialog;

public class MainActivity extends BaseMainActivity {

    private final MainFragment mMainFrag = new MainFragment();

    private Intent mIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setImmersionMenuEnabled(true);
        mIntent = getIntent();
        if (mIntent != null) {
            getAppCompatActionBar().setDisplayHomeAsUpEnabled(mIntent.getBooleanExtra("isDisplayHomeAsUpEnabled", false));
        }

        ALPermissionManager.RootCommand(getPackageCodePath());

        // XposedBridge.log("Cemiuiler: Detail log is " + mPrefsMap.getBoolean("settings_disable_detailed_log") + ".");


    }

    @Override
    public Fragment initFragment() {
        return mMainFrag;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.restart_home) {
            RestartAlertDialog dialog = new RestartAlertDialog(this);
            dialog.setTitle(item.getTitle());
            dialog.show();
        } else if (item.getItemId() == R.id.settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void restartApp(String packageName) {
        Intent intent = new Intent(GlobalActions.ACTION_PREFIX + "RestartApps");
        intent.putExtra("packageName", packageName);
        sendBroadcast(intent);
        Toast.makeText(this, packageName, Toast.LENGTH_SHORT).show();
    }
}
