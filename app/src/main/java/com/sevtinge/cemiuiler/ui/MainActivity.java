package com.sevtinge.cemiuiler.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.GlobalActions;
import com.sevtinge.cemiuiler.ui.main.fragment.MainFragment;
import com.sevtinge.cemiuiler.ui.main.base.BaseMainActivity;
import com.sevtinge.cemiuiler.utils.ALPermissionManager;
import com.sevtinge.cemiuiler.view.CustomMultipleChoiceView;


import java.util.Arrays;
import java.util.List;

import moralnorm.appcompat.app.AlertDialog;

public class MainActivity extends BaseMainActivity {

    private MainFragment mMainFrag = new MainFragment();

    private Intent mIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setImmersionMenuEnabled(true);
        mIntent = getIntent();
        if (mIntent != null) {
            getAppCompatActionBar().setDisplayHomeAsUpEnabled(mIntent.getBooleanExtra("isDisplayHomeAsUpEnabled", false));
        }
        /*获取root
        String apkRoot = "chmod 777 " + getPackageCodePath();
        ALPermissionManager.RootCommand(apkRoot);
        */

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.restart_home:

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
                            switch (i) {
                                case 4:
                                    Intent intent = new Intent(GlobalActions.ACTION_PREFIX + "RestartSystemUI");
                                    intent.setPackage("com.android.systemui");
                                    sendBroadcast(intent);
                                    break;

                                default:
                                    restartApp(mAppPackageName.get(i));
                                    break;
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
                break;

            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            default:
                break;
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
