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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.app;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getModuleRes;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getPackageVersionCode;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.MiuiDialog;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

@SuppressLint("DiscouragedApi")
public class AppDisable extends BaseHook {

    private ArrayList<String> mMiuiCoreApps = new ArrayList<>();
    private MenuItem menuItem = null;
    private boolean isNewSecurityCenter;
    private String clazzName;

    @Override
    public void init() {
        initVersionCheck();
        hookCreateOptionsMenu();
        hookPrepareOptionsMenu();
        hookOnResume();
        hookOptionsItemSelected();
    }

    /**
     * 检查安全中心版本
     */
    private void initVersionCheck() {
        int versionCode = getPackageVersionCode(getLpparam());
        boolean isPad = isPad();
        isNewSecurityCenter = (versionCode >= 40001000 && !isPad) || (versionCode >= 40011000 && isPad);
        clazzName = isNewSecurityCenter
            ? "com.miui.appmanager.fragment.ApplicationsDetailsFragment"
            : "com.miui.appmanager.ApplicationsDetailsActivity";
    }

    /**
     * Hook 菜单创建
     */
    private void hookCreateOptionsMenu() {
        findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity", "onCreateOptionsMenu",
            Menu.class, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) throws Exception {
                    Activity act = (Activity) param.getThisObject();
                    Menu menu = (Menu) param.getArgs()[0];

                    menuItem = menu.findItem(6);
                    if (menuItem != null) {
                        menuItem.setVisible(false);
                    }

                    addCustomMenuItem(act, menu);
                    PackageInfo packageInfo = getPackageInfo(act, param.getThisObject());
                    if (packageInfo != null) {
                        configureMenuItems(act, menu, packageInfo);
                    }
                }
            });
    }

    /**
     * 添加自定义菜单项
     */
    private void addCustomMenuItem(Activity act, Menu menu) {
        MenuItem disableItem = menu.add(0, 666, 1,
            act.getResources().getIdentifier("app_manager_disable_text", "string", getPackageName()));
        disableItem.setIcon(act.getResources().getIdentifier("action_button_stop_svg", "drawable", getPackageName()));
        disableItem.setEnabled(true);
        disableItem.setShowAsAction(1);
    }

    /**
     * 配置菜单项状态
     */
    private void configureMenuItems(Activity act, Menu menu, PackageInfo packageInfo) throws Exception {
        PackageManager pm = act.getPackageManager();
        ApplicationInfo appInfo = pm.getApplicationInfo(packageInfo.packageName, PackageManager.GET_META_DATA);

        boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        boolean isUpdatedSystem = (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;

        MenuItem disableItem = menu.findItem(666);
        if (disableItem != null) {
            disableItem.setTitle(act.getResources().getIdentifier(
                appInfo.enabled ? "app_manager_disable_text" : "app_manager_enable_text",
                "string", getPackageName()));
        }

        getModuleRes(act);
        mMiuiCoreApps = new ArrayList<>(Arrays.asList(
            act.getResources().getStringArray(R.array.miui_core_app_package_name)));

        if (mMiuiCoreApps.contains(packageInfo.packageName)) {
            if (disableItem != null) {
                disableItem.setEnabled(false);
            }
        }

        if (!appInfo.enabled || (isSystem && !isUpdatedSystem)) {
            MenuItem uninstallItem = menu.findItem(2);
            if (uninstallItem != null) {
                uninstallItem.setVisible(false);
            }
        }
    }

    /**
     * Hook 菜单准备
     */
    private void hookPrepareOptionsMenu() {
        findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity", "onPrepareOptionsMenu",
            Menu.class, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    if (menuItem != null) {
                        menuItem.setVisible(false);
                    }
                }
            });
    }

    /**
     * Hook onResume 生命周期
     */
    private void hookOnResume() {
        findAndHookMethod(clazzName, "onResume", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                if (menuItem != null) {
                    menuItem.setVisible(false);
                }
            }
        });
    }

    /**
     * Hook 菜单项选择事件
     */
    private void hookOptionsItemSelected() {
        Method method = DexKit.findMember("MethodOnOptionsItemSelected", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                return bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("application/vnd.android.package-archive")
                    )).singleOrNull();
            }
        });

        hookMethod(method, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) throws Exception {
                MenuItem item = (MenuItem) param.getArgs()[0];
                if (item != null && item.getItemId() == 666) {
                    handleDisableMenuClick(param, item);
                }
            }
        });
    }

    /**
     * 处理禁用菜单点击
     */
    private void handleDisableMenuClick(AfterHookParam param, MenuItem item) throws Exception {
        Activity act = getActivity(param);
        PackageInfo packageInfo = getPackageInfo(act, param.getThisObject());
        Resources modRes = getModuleRes(act);

        if (packageInfo == null) {
            return;
        }

        // 检查是否为核心应用
        if (mMiuiCoreApps.contains(packageInfo.packageName)) {
            Toast.makeText(act, modRes.getString(R.string.disable_app_settings), Toast.LENGTH_SHORT).show();
            return;
        }

        PackageManager pm = act.getPackageManager();
        ApplicationInfo appInfo = pm.getApplicationInfo(packageInfo.packageName, PackageManager.GET_META_DATA);
        boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        boolean isEnabled = isAppEnabled(pm, packageInfo.packageName);

        if (isEnabled) {
            if (isSystem) {
                showDisableConfirmDialog(act, modRes, packageInfo.packageName, item);
            } else {
                setAppState(act, packageInfo.packageName, item, false);
            }
        } else {
            setAppState(act, packageInfo.packageName, item, true);
        }

        param.setResult(true);
    }

    /**
     * 显示禁用确认对话框
     */
    private void showDisableConfirmDialog(Activity act, Resources modRes, String pkgName, MenuItem item) {
        String title = modRes.getString(R.string.disable_app_title);
        String text = modRes.getString(R.string.disable_app_text);

        new MiuiDialog.Builder(getClassLoader(), act)
            .setTitle(title)
            .setMessage(text)
            .setHapticFeedbackEnabled(true)
            .setPositiveButton(android.R.string.ok, (dialog, which) ->
                setAppState(act, pkgName, item, false))
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    /**
     * 获取 Activity 对象
     */
    private Activity getActivity(AfterHookParam param) {
        if (isNewSecurityCenter) {
            return (Activity) EzxHelpUtils.callMethod(param.getThisObject(), "getActivity");
        } else {
            return (Activity) param.getThisObject();
        }
    }

    /**
     * 获取 PackageInfo
     */
    private PackageInfo getPackageInfo(Activity act, Object obj) throws Exception {
        Field piField;
        if (isNewSecurityCenter) {
            Class<?> fragmentClass = findClassIfExists(clazzName);
            Field fragmentField = EzxHelpUtils.findFirstFieldByExactType(act.getClass(), fragmentClass);
            Object fragment = EzxHelpUtils.getObjectField(act, fragmentField.getName());
            if (fragment == null) {
                return null;
            }
            piField = EzxHelpUtils.findFirstFieldByExactType(fragment.getClass(), PackageInfo.class);
            return (PackageInfo) piField.get(fragment);
        } else {
            piField = EzxHelpUtils.findFirstFieldByExactType(obj.getClass(), PackageInfo.class);
            return (PackageInfo) piField.get(obj);
        }
    }

    /**
     * 检查应用是否启用
     */
    private boolean isAppEnabled(PackageManager pm, String pkgName) {
        int state = pm.getApplicationEnabledSetting(pkgName);
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
    }

    /**
     * 设置应用启用/禁用状态
     */
    private void setAppState(final Activity act, String pkgName, MenuItem item, boolean enable) {
        try {
            PackageManager pm = act.getPackageManager();
            pm.setApplicationEnabledSetting(pkgName,
                enable ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);

            boolean isEnabled = isAppEnabled(pm, pkgName);

            if ((enable && isEnabled) || (!enable && !isEnabled)) {
                item.setTitle(act.getResources().getIdentifier(
                    enable ? "app_manager_disable_text" : "app_manager_enable_text",
                    "string", "com.miui.securitycenter"));
                Toast.makeText(act, act.getResources().getIdentifier(
                    enable ? "app_manager_enabled" : "app_manager_disabled",
                    "string", "com.miui.securitycenter"), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(act, getModuleRes(act).getString(R.string.disable_app_fail),
                    Toast.LENGTH_LONG).show();
            }new Handler(act.getMainLooper()).postDelayed(act::invalidateOptionsMenu, 500);
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "", t);
        }
    }
}
