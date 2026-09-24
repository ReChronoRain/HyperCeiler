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
package com.sevtinge.hyperceiler.libhook.rules.home.other;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.util.List;

import io.github.libxposed.api.XposedInterface;

/**
 * 让桌面长按图标菜单里的「卸载」交给指定的卸载器处理。
 * <p>
 * 只挂一个入口：长按菜单的「卸载」项
 * {@code SystemShortcutMenuItem$UninstallShortcutMenuItem#getOnClickListener()}，
 * 点击时自行取包名并转发，不再弹桌面自带的卸载对话框。
 * <p>
 * 桌面真正发起卸载的 {@code UninstallController#uninstallApp} / {@code #uninstallApps}
 * <b>刻意不拦</b>：那里的宿主已经开始播放卸载动画并等待 {@code DeleteObserver} 回调，
 * 拦下来只会得到「动画播了但应用没卸载」的假象，而且 {@code uninstallApp} 是对话框确认后的动作，
 * 从菜单入口接管已经完全覆盖了用户可见的卸载路径。
 * <p>
 * 命中后只做三件事：
 * <ol>
 *     <li>阻断桌面原有的卸载流程（返回自定义 listener，不调用原 listener）；</li>
 *     <li>按设置 {@code home_other_uninstaller} 决定新 Intent 的目标 —— 空值交给系统解析、
 *     {@code __follow_default_installer__} 用系统默认安装器、其它值当作卸载器包名，
 *     并在能解析到时用 {@code setComponent} 锁定该包下的卸载 Activity；</li>
 *     <li>转发这个重新构造的 {@link Intent#ACTION_DELETE} + {@code package:<pkg>}：
 *     用桌面的 Launcher Activity 启动（与桌面自己的卸载界面同任务栈）。</li>
 * </ol>
 */
public class UninstallWithDefaultPackageManager extends BaseHook {

    private static final String SCHEME_PACKAGE = "package";

    /**
     * 探测「系统默认安装器」用的 apk MIME。
     */
    private static final String MIME_APK = "application/vnd.android.package-archive";

    /**
     * 长按菜单里的「卸载」项。
     * <p>
     * 每一项菜单动作对应一个内部类，卸载项与 {@code $ShareAppShortcutMenuItem}、{@code $AppDetailsShortcutMenuItem} 同构。
     */
    private static final String UNINSTALL_MENU_ITEM =
        "com.miui.home.launcher.shortcuts.SystemShortcutMenuItem$UninstallShortcutMenuItem";

    /**
     * 桌面 Application，用来取当前的 Launcher（Activity），作为转发与关闭菜单的入口。
     */
    private static final String LAUNCHER_APPLICATION = "com.miui.home.launcher.Application";

    /**
     * 设置项：卸载器（空 = 系统默认解析；{@link #FOLLOW_DEFAULT_INSTALLER} = 跟随默认安装器；其它 = 卸载器包名）。
     */
    private static final String PREFS_UNINSTALLER = "home_other_uninstaller";

    /**
     * 卸载器设置值：跟随系统默认安装器。
     */
    private static final String FOLLOW_DEFAULT_INSTALLER = "__follow_default_installer__";

    private String mUninstallerSetting = "";

    @Override
    public void init() {
        mUninstallerSetting = PrefsBridge.getString(PREFS_UNINSTALLER, "");
        XposedLog.d(TAG, getPackageName(), "init: uninstaller=" + describeUninstaller(mUninstallerSetting));

        hookUninstallMenuItem();
    }

    /**
     * 用自定义的点击监听替换菜单项原来的监听。
     * <p>
     * 菜单项是「所有图标共用」的模板对象，包名在点击时才能从
     * {@code Launcher#getShortcutMenuLayer()#getBindedItemInfo()} 取到，
     * 所以这里返回的 listener 内部才去解析包名。
     */
    private void hookUninstallMenuItem() {
        findAndChainMethod(UNINSTALL_MENU_ITEM, "getOnClickListener", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Object original = chain.proceed();
                return (View.OnClickListener) view -> {
                    try {
                        Activity launcher = getLauncherActivity();
                        String pkgName = resolveBindedPackageName(launcher);
                        if (TextUtils.isEmpty(pkgName)) {
                            XposedLog.w(TAG, getPackageName(),
                                "[menu] uninstall menu item clicked but package name not found");
                        } else {
                            XposedLog.d(TAG, getPackageName(), "[menu] uninstall menu item clicked: pkg=" + pkgName);
                            if (forwardUninstall(launcher != null ? launcher : view.getContext(), pkgName)) {
                                hideShortcutMenu(launcher);
                                return;
                            }
                            XposedLog.w(TAG, getPackageName(), "[menu] forward failed, let MIUI home handle the menu item");
                        }
                    } catch (Throwable t) {
                        XposedLog.w(TAG, getPackageName(), "[menu] failed to intercept uninstall menu item", t);
                    }

                    // 转发失败时交回桌面原本的行为，避免点了没反应。
                    if (original instanceof View.OnClickListener) {
                        ((View.OnClickListener) original).onClick(view);
                    }
                };
            }
        });
    }

    // ==================== 转发 ====================

    /**
     * 把卸载请求交给卸载器：优先用桌面的 Launcher Activity 启动，取不到时退回宿主 Application。
     *
     * @return 是否成功启动
     */
    private boolean forwardUninstall(Context context, String pkgName) {
        boolean newTask = !(context instanceof Activity);
        Context host = context != null ? context : getHostContext();
        if (host == null) {
            XposedLog.w(TAG, getPackageName(), "[menu] no Context available, cannot forward");
            return false;
        }

        XposedLog.d(TAG, getPackageName(), "[menu] forward uninstall: pkg=" + pkgName
            + ", uninstaller=" + describeUninstaller(mUninstallerSetting));

        if (startUninstallIntent(host, newTask,
            buildUninstallIntent(host, pkgName, mUninstallerSetting))) {
            return true;
        }

        if (!TextUtils.isEmpty(mUninstallerSetting)) {
            XposedLog.w(TAG, getPackageName(), "[menu] uninstaller "
                + describeUninstaller(mUninstallerSetting) + " failed, fallback to system default resolver");
            return startUninstallIntent(host, newTask, buildUninstallIntent(host, pkgName, ""));
        }
        return false;
    }

    private boolean startUninstallIntent(Context context, boolean newTask, Intent intent) {
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            context.startActivity(intent);
            XposedLog.d(TAG, getPackageName(), "[menu] forwarded: " + intent);
            return true;
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "[menu] start uninstall Activity failed: " + intent, t);
            return false;
        }
    }

    /**
     * 构造卸载 Intent。
     * <p>
     * {@code uninstallerSetting} 为空 → 交给系统解析；{@link #FOLLOW_DEFAULT_INSTALLER} → 用系统默认安装器；
     * 其它值 → 当作卸载器包名。指定了目标时，尽量再解析该包下的卸载 Activity 并 {@code setComponent} 锁定，
     * 避免同包内多个入口或解析被别的候选接走。
     */
    private Intent buildUninstallIntent(Context context, String pkgName, String uninstallerSetting) {
        Intent intent = new Intent(Intent.ACTION_DELETE, Uri.fromParts(SCHEME_PACKAGE, pkgName, null));

        String target = uninstallerSetting;
        if (FOLLOW_DEFAULT_INSTALLER.equals(uninstallerSetting)) {
            target = resolveDefaultInstallerPackage(context);
            if (TextUtils.isEmpty(target)) {
                XposedLog.w(TAG, getPackageName(),
                    "[target] default installer not found, fallback to system default resolver");
                return intent;
            }
        }

        if (TextUtils.isEmpty(target)) {
            return intent;
        }

        intent.setPackage(target);
        ComponentName component = resolveUninstallActivity(context, pkgName, target);
        if (component != null) {
            intent.setComponent(component);
        }
        return intent;
    }

    /**
     * 系统默认安装器 = 当前处理 apk 安装（{@link #MIME_APK}）的默认应用。
     */
    private String resolveDefaultInstallerPackage(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent probe = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(Uri.parse("file:///sdcard/__probe__.apk"), MIME_APK)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ResolveInfo info = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null && info.activityInfo != null) {
                return info.activityInfo.packageName;
            }
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "[target] resolve default installer failed", t);
        }
        return null;
    }

    /**
     * 找出 {@code target} 包内处理 {@code package:<pkg>} 卸载的 Activity。
     */
    private ComponentName resolveUninstallActivity(Context context, String pkgName, String target) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent probe = new Intent(Intent.ACTION_DELETE, Uri.fromParts(SCHEME_PACKAGE, pkgName, null))
                .setPackage(target);
            List<ResolveInfo> infos = pm.queryIntentActivities(probe, 0);
            if (infos != null) {
                for (ResolveInfo info : infos) {
                    if (info.activityInfo == null || !target.equals(info.activityInfo.packageName)) {
                        continue;
                    }
                    return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                }
            }
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "[target] resolve uninstall activity failed", t);
        }
        return null;
    }

    /**
     * 拿宿主进程的 Application context（取不到 Launcher Activity 时的兜底）。
     */
    private Context getHostContext() {
        try {
            Class<?> activityThread = findClassIfExists("android.app.ActivityThread");
            if (activityThread == null) {
                return null;
            }
            Object application = callStaticMethod(activityThread, "currentApplication");
            if (application instanceof Context) {
                return (Context) application;
            }
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "getHostContext failed", t);
        }
        return null;
    }

    private static String describeUninstaller(String setting) {
        if (TextUtils.isEmpty(setting)) {
            return "system default";
        }
        if (FOLLOW_DEFAULT_INSTALLER.equals(setting)) {
            return "follow default installer";
        }
        return setting;
    }

    // ==================== 解析被长按的项 ====================

    /**
     * 当前桌面 Launcher Activity。
     * <p>
     * 用它启动卸载界面，能与桌面自己的卸载入口保持同一任务栈；它同时也是 {@code hideShortcutMenuWithoutAnim} 的宿主。
     */
    private Activity getLauncherActivity() {
        try {
            Class<?> application = findClassIfExists(LAUNCHER_APPLICATION);
            if (application == null) {
                return null;
            }
            Object launcher = callStaticMethod(application, "getLauncher");
            if (launcher instanceof Activity) {
                return (Activity) launcher;
            }
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "get launcher failed", t);
        }
        return null;
    }

    /**
     * 长按菜单绑定的那个图标项的包名。
     */
    private String resolveBindedPackageName(Activity launcher) {
        if (launcher == null) {
            return null;
        }
        try {
            Object layer = callMethod(launcher, "getShortcutMenuLayer");
            if (layer == null) {
                return null;
            }
            return resolvePackageName(callMethod(layer, "getBindedItemInfo"));
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "[menu] resolve bound item info failed", t);
            return null;
        }
    }

    /**
     * 从图标项/快捷方式项里取包名，逐级兜底。
     */
    private String resolvePackageName(Object itemInfo) {
        if (itemInfo == null) {
            return null;
        }

        Object pkgName = tryCallMethod(itemInfo, "getPackageName");
        if (pkgName instanceof String && !TextUtils.isEmpty((String) pkgName)) {
            return (String) pkgName;
        }

        Object component = tryCallMethod(itemInfo, "getTargetComponent");
        if (component instanceof ComponentName) {
            return ((ComponentName) component).getPackageName();
        }

        Object intent = tryCallMethod(itemInfo, "getIntent");
        if (intent instanceof Intent) {
            ComponentName intentComponent = ((Intent) intent).getComponent();
            if (intentComponent != null) {
                return intentComponent.getPackageName();
            }
        }
        return null;
    }

    private Object tryCallMethod(Object target, String methodName) {
        try {
            return callMethod(target, methodName);
        } catch (Throwable ignored) {
            // 该类上不存在这个方法，交给下一级兜底。
            return null;
        }
    }

    /**
     * 关闭长按菜单，避免转发后菜单还留在桌面上。
     */
    private void hideShortcutMenu(Activity launcher) {
        if (launcher == null) {
            return;
        }
        try {
            callMethod(launcher, "hideShortcutMenuWithoutAnim");
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "[menu] hide shortcut menu failed", t);
        }
    }
}
