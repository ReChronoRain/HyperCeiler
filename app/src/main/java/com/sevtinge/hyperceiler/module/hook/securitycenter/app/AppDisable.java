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
package com.sevtinge.hyperceiler.module.hook.securitycenter.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.MiuiDialog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

@SuppressLint("DiscouragedApi")
public class AppDisable extends BaseHook {

    public ArrayList<String> mMiuiCoreApps = new ArrayList<>();
    private MenuItem menuItem = null;

    @Override
    public void init() {
        findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity",
                "onCreateOptionsMenu", Menu.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        Activity act = (Activity) param.thisObject;
                        Menu menu = (Menu) param.args[0];
                        menuItem = menu.findItem(6);
                        if (menuItem != null) menuItem.setVisible(false);
                        MenuItem dis = menu.add(0, 666, 1,
                                act.getResources().getIdentifier("app_manager_disable_text", "string", lpparam.packageName));
                        dis.setIcon(act.getResources().getIdentifier("action_button_stop_svg", "drawable", lpparam.packageName));
                        dis.setEnabled(true);
                        dis.setShowAsAction(1);
                        // XposedHelpers.setAdditionalInstanceField(param.thisObject, "mDisableButton", dis);
                        PackageManager pm = act.getPackageManager();
                        Field piField = XposedHelpers.findFirstFieldByExactType(act.getClass(), PackageInfo.class);
                        PackageInfo mPackageInfo = (PackageInfo) piField.get(act);
                        ApplicationInfo appInfo = pm.getApplicationInfo(mPackageInfo.packageName, PackageManager.GET_META_DATA);
                        boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                        boolean isUpdatedSystem = (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
                        dis.setTitle(act.getResources().getIdentifier(appInfo.enabled ? "app_manager_disable_text" : "app_manager_enable_text",
                                "string", lpparam.packageName));
                        getModuleRes(act);
                        mMiuiCoreApps = new ArrayList<>(Arrays.asList(act.getResources().getStringArray(R.array.miui_core_app_package_name)));
                        if (mMiuiCoreApps.contains(mPackageInfo.packageName)) {
                            dis.setEnabled(false);
                        }
                        if (!appInfo.enabled || (isSystem && !isUpdatedSystem)) {
                            MenuItem item = menu.findItem(2);
                            if (item != null) item.setVisible(false);
                        }
                    }
                }
        );

        findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity",
                "onPrepareOptionsMenu", Menu.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        if (menuItem != null) menuItem.setVisible(false);
                    }
                }
        );

        findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity",
                "onResume",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        if (menuItem != null) menuItem.setVisible(false);
                    }
                }
        );

        findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity",
                "onOptionsItemSelected", MenuItem.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        MenuItem item = (MenuItem) param.args[0];
                        if (item != null && item.getItemId() == 666) {
                            Activity act = (Activity) param.thisObject;
                            Resources modRes = getModuleRes(act);
                            Field piField = XposedHelpers.findFirstFieldByExactType(act.getClass(), PackageInfo.class);

                            PackageInfo mPackageInfo = (PackageInfo) piField.get(act);
                            if (mMiuiCoreApps.contains(mPackageInfo.packageName)) {
                                Toast.makeText(act, modRes.getString(R.string.disable_app_settings), Toast.LENGTH_SHORT).show();
                                return;
                            }

                            PackageManager pm = act.getPackageManager();
                            ApplicationInfo appInfo = pm.getApplicationInfo(mPackageInfo.packageName, PackageManager.GET_META_DATA);
                            boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                            int state = pm.getApplicationEnabledSetting(mPackageInfo.packageName);
                            boolean isEnabledOrDefault = (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
                            if (isEnabledOrDefault) {
                                if (isSystem) {
                                    String title = modRes.getString(R.string.disable_app_title);
                                    String text = modRes.getString(R.string.disable_app_text);
                                    DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            setAppState(act, mPackageInfo.packageName, item, false);
                                        }
                                    };
                                    Class<?> miuiDialog = findClassIfExists("miuix.appcompat.app.AlertDialog");
                                    Class<?> builder = findClassIfExists("miuix.appcompat.app.AlertDialog$Builder");
                                    Object newBuilder = XposedHelpers.newInstance(builder, act);
                                    /*XposedHelpers.callMethod(XposedHelpers.callMethod(XposedHelpers.callMethod(XposedHelpers.callMethod(
                                                    XposedHelpers.callMethod(XposedHelpers.callMethod(newBuilder, "setTitle", title),
                                                            "setMessage", text), "setPositiveButton", android.R.string.ok, onClickListener),
                                            "setNegativeButton", android.R.string.cancel, null), "create"), "show");*/
                                    new MiuiDialog.Builder(lpparam.classLoader, act)
                                            .setTitle(title)
                                            .setMessage(text)
                                            .setHapticFeedbackEnabled(true)
                                            .setPositiveButton(android.R.string.ok, onClickListener)
                                            .setNegativeButton(android.R.string.cancel, null)
                                            .show();
                                    // setAppState(act, mPackageInfo.packageName, item, false);
                                } else {
                                    setAppState(act, mPackageInfo.packageName, item, false);
                                }
                            } else {
                                setAppState(act, mPackageInfo.packageName, item, true);
                            }
                            param.setResult(true);
                        }
                    }
                }
        );
    }

    private void setAppState(final Activity act, String pkgName, MenuItem item, boolean enable) {
        try {
            PackageManager pm = act.getPackageManager();
            pm.setApplicationEnabledSetting(pkgName, enable ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
            int state = pm.getApplicationEnabledSetting(pkgName);
            boolean isEnabledOrDefault = (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
            if ((enable && isEnabledOrDefault) || (!enable && !isEnabledOrDefault)) {
                item.setTitle(act.getResources().getIdentifier(enable ? "app_manager_disable_text" : "app_manager_enable_text", "string", "com.miui.securitycenter"));
                Toast.makeText(act, act.getResources().getIdentifier(enable ? "app_manager_enabled" : "app_manager_disabled", "string", "com.miui.securitycenter"), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(act, getModuleRes(act).getString(R.string.disable_app_fail), Toast.LENGTH_LONG).show();
            }
            new Handler().postDelayed(act::invalidateOptionsMenu, 500);
        } catch (Throwable t) {
            logW(TAG, "", t);
        }
    }
}
