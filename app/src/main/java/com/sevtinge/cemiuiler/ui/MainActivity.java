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
import com.sevtinge.cemiuiler.view.CustomMultipleChoiceView;

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
            List<String> mAppName = Arrays.asList(getResources().getStringArray(R.array.restart_apps_name));
            List<String> mAppPackageName = Arrays.asList(getResources().getStringArray(R.array.restart_apps_packagename));

            AlertDialog dialog = new AlertDialog(this);
            dialog.setTitle(item.getTitle());

            CustomMultipleChoiceView view = new CustomMultipleChoiceView(this);
            LinearLayout mRoot = new LinearLayout(this);

            mRoot.addView(view);
            view.setData(mAppName, null);
            view.deselectAll();
            view.setOnCheckedListener(sparseBooleanArray -> {
                dialog.dismiss();
                for (int i = 0; i < sparseBooleanArray.size(); i++) {
                    if (sparseBooleanArray.get(i)) {
                        if (i == 4) {
                            Intent intent = new Intent(GlobalActions.ACTION_PREFIX + "RestartSystemUI");
                            intent.setPackage("com.android.systemui");
                            sendBroadcast(intent);
                        } else {
                            restartApp(mAppPackageName.get(i));
                        }
                    }
                }
            });
            dialog.setView(mRoot);
            dialog.show();


            /*sendBroadcast(new Intent(GlobalActions.ACTION_PREFIX + "RestartHome"));*/

                /*String[] mAppName = new String[] {"桌面", "设置", "手机管家", "主题壁纸", "智能助理", "系统界面", "全选"};
                String[] mAppPackageName = new String[] {"com.miui.home",
                        "com.android.settings",
                        "com.miui.securitycenter",
                        "com.android.thememanager",
                        "com.miui.personalassistant",
                        "com.android.systemui",
                        ""};
                boolean[] checkedItems = new boolean[]{false, false, false, false, false, false, false};
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(item.getTitle());
                builder.setMultiChoiceItems(mAppName, checkedItems, (dialog, which, isChecked) -> {

                    if (which == (checkedItems.length - 1)) {
                        if (isChecked) {
                            for (int i = 0; i < checkedItems.length; i++) {
                                checkedItems[i] = true;
                            }
                        } else {
                            for (int i = checkedItems.length - 1; i >= 0; i--) {
                                checkedItems[i] = false;
                            }
                        }
                    }
                });
                builder.setNeutralButton(android.R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                });
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    for (int i = 0; i < checkedItems.length; i++) {
                        if(checkedItems[i] != false) {
                            if (!TextUtils.isEmpty(mAppPackageName[i])) {
                                if (mAppPackageName[i].equals("com.android.systemui")) {
                                    sendBroadcast(new Intent(GlobalActions.ACTION_PREFIX + "RestartSystemUI"));
                                } else {
                                    Intent intent = new Intent(GlobalActions.ACTION_PREFIX + "RestartApps");
                                    intent.putExtra("packageName", mAppPackageName[i]);
                                    sendBroadcast(intent);
                                    Toast.makeText(this, mAppPackageName[i], Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                });
                builder.show();*/
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
