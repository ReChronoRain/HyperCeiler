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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.view.MenuItem;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

/**
 * 让安全中心「应用管理」里的卸载操作交给指定的卸载器处理。
 * <p>
 * 接管时机由设置 {@code security_center_uninstall_hook_point} 决定（见 {@link #init()}）：
 * <ol>
 *     <li>{@code menu}：宿主「卸载」菜单项的回调
 *     （实测为 {@code com.miui.appmanager.fragment.ApplicationsDetailsFragment.optionsItemSelected(MenuItem)}）；</li>
 *     <li>{@code core}：底层卸载调用（{@link #CORE_APIS} + {@code AppManageUtils} 里名字含 uninstall 的方法），
 *     用于覆盖不走菜单回调的入口；</li>
 *     <li>{@code both}（默认）：两者都挂。</li>
 * </ol>
 * 命中后只做三件事：
 * <ol>
 *     <li>阻断宿主原有的卸载流程（{@code param.setResult(...)}）；</li>
 *     <li>按设置 {@code security_center_uninstaller} 决定新 Intent 的目标 —— 空值交给系统解析、
 *     {@code __follow_default_installer__} 用系统默认安装器、其它值当作卸载器包名，
 *     并在能解析到时用 {@code setComponent} 锁定该包下的卸载 Activity；</li>
 *     <li>转发这个重新构造的 {@link Intent#ACTION_DELETE} + {@code package:<pkg>}：
 *     有前台 Activity 就用它启动（与菜单入口一致，同任务栈、有转场），
 *     否则用宿主 Application + {@code FLAG_ACTIVITY_NEW_TASK}。</li>
 * </ol>
 * <p>
 * 这里<b>不再</b>对 {@code Activity#startActivity} 做任何改写。历史版本的兜底改写会把自己刚设置好的
 * {@code setPackage} 又清掉，导致指定卸载器失效、请求重新落回 intent-filter 优先级更高的 MIUI 包管理器。
 */
@SuppressLint("DiscouragedApi")
public class UninstallWithDefaultPackageManager extends BaseHook {

    private static final String SCHEME_PACKAGE = "package";

    /**
     * 应用信息页携带包名的 extra（安全中心多处使用该 key）。
     */
    private static final String EXTRA_PACKAGE_NAME = "package_name";

    /**
     * 探测「系统默认安装器」用的 apk MIME。
     */
    private static final String MIME_APK = "application/vnd.android.package-archive";

    /**
     * 宿主「卸载」菜单项的字符串资源名，用于与菜单标题比对。
     */
    private static final String[] UNINSTALL_STRING_NAMES = {
        "app_manager_uninstall_text",
        "app_manager_uninstall",
        "uninstall"
    };

    /**
     * 宿主「卸载」菜单项的 item id。
     * <p>
     * 与 {@code AppDisable} 中 {@code menu.findItem(2)} 的卸载项保持一致，仅作标题比对失败时的兜底。
     */
    private static final int UNINSTALL_ITEM_ID = 2;

    /**
     * 设置项：卸载器（空 = 系统默认解析；{@link #FOLLOW_DEFAULT_INSTALLER} = 跟随默认安装器；其它 = 卸载器包名）。
     */
    private static final String PREFS_UNINSTALLER = "security_center_uninstaller";

    /**
     * 卸载器设置值：跟随系统默认安装器。
     */
    private static final String FOLLOW_DEFAULT_INSTALLER = "__follow_default_installer__";

    /**
     * 设置项：接管时机。
     */
    private static final String PREFS_HOOK_POINT = "security_center_uninstall_hook_point";

    private static final String HOOK_POINT_MENU = "menu";
    private static final String HOOK_POINT_CORE = "core";
    private static final String HOOK_POINT_BOTH = "both";

    /**
     * core 时机要挂的底层卸载 API。
     * <p>
     * 前几个是应用侧封装，后面是 AIDL 代理 —— 系统应用常绕过封装直接调 {@code IPackageManager}。
     */
    private static final String[][] CORE_APIS = {
        {"android.content.pm.PackageInstaller", "uninstall"},
        {"android.app.ApplicationPackageManager", "deletePackage"},
        {"android.app.ApplicationPackageManager", "deletePackageAsUser"},
        {"android.app.ApplicationPackageManager", "deletePackageVersioned"},
        {"android.content.pm.IPackageManager$Stub$Proxy", "deletePackage"},
        {"android.content.pm.IPackageManager$Stub$Proxy", "deletePackageAsUser"},
        {"android.content.pm.IPackageManager$Stub$Proxy", "deletePackageVersioned"},
    };

    /**
     * 从底层卸载调用的参数里识别包名的形状。
     */
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+");

    private Method mMenuClickMethod;

    private String mUninstallerSetting = "";

    private String mHookPoint = HOOK_POINT_BOTH;

    /**
     * 最近一次 resume 的 Activity，core 时机转发时用它启动，行为与菜单入口保持一致。
     */
    private volatile WeakReference<Activity> mLastActivity = new WeakReference<>(null);

    // ==================== 入口一：宿主「卸载」菜单项 ====================

    private final IMethodHook mMenuItemHook = new IMethodHook() {
        @Override
        public void before(HookParam param) {
            try {
                Object[] args = param.getArgs();
                if (args == null || args.length == 0 || !(args[0] instanceof MenuItem)) {
                    return;
                }

                Activity activity = resolveActivity(param.getThisObject());
                if (activity == null) {
                    XposedLog.w(TAG, getPackageName(), "[menu] cannot resolve Activity, skip");
                    return;
                }

                if (!isUninstallMenuItem(activity, (MenuItem) args[0])) {
                    return;
                }

                String pkgName = resolvePackageName(param.getThisObject(), activity);
                if (TextUtils.isEmpty(pkgName)) {
                    XposedLog.w(TAG, getPackageName(),
                        "[menu] uninstall menu item clicked but package name not found");
                    return;
                }

                XposedLog.d(TAG, getPackageName(), "[menu] uninstall menu item clicked: pkg=" + pkgName);
                if (forwardUninstall("menu", activity, pkgName)) {
                    // 阻断 MIUI 的静默卸载，用户已经在卸载器界面上了。
                    param.setResult(true);
                } else {
                    XposedLog.w(TAG, getPackageName(), "[menu] forward failed, let MIUI handle the menu item");
                }
            } catch (Throwable t) {
                XposedLog.w(TAG, getPackageName(), "[menu] failed to intercept uninstall menu item", t);
            }
        }
    };

    // ==================== 入口二：底层卸载调用 ====================

    /**
     * 记录前台 Activity，供 core 时机转发时使用（hook 的是 boot 类，只在安全中心进程内生效）。
     */
    private final IMethodHook mActivityResumeHook = new IMethodHook() {
        @Override
        public void before(HookParam param) {
            Object thisObject = param.getThisObject();
            if (thisObject instanceof Activity) {
                mLastActivity = new WeakReference<>((Activity) thisObject);
            }
        }
    };

    /**
     * 为单个底层 API 生成回调。
     */
    private IMethodHook coreCallback(String apiName) {
        return new IMethodHook() {
            @Override
            public void before(HookParam param) {
                try {
                    String pkgName = extractPackageName(param.getArgs());
                    if (TextUtils.isEmpty(pkgName)) {
                        XposedLog.d(TAG, getPackageName(), "[core] " + apiName + " without package name, skip");
                        return;
                    }

                    XposedLog.d(TAG, getPackageName(), "[core] uninstall call via " + apiName + ": pkg=" + pkgName);
                    if (forwardUninstall("core", getLastActivity(), pkgName)) {
                        // 阻断宿主的静默卸载（调用方可能依赖回调，界面刷新行为会因此变化）。
                        param.setResult(null);
                    } else {
                        XposedLog.w(TAG, getPackageName(), "[core] forward failed, keep original uninstall path");
                    }
                } catch (Throwable t) {
                    XposedLog.w(TAG, getPackageName(), "[core] failed to intercept uninstall call via " + apiName, t);
                }
            }
        };
    }

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        // 与 AppDisable 一致：用菜单里「安装本地 apk」的 MIME 字符串定位宿主菜单点击回调。
        // 用 optionalMember：定位失败时只放弃菜单入口，不影响 core 入口。
        mMenuClickMethod = optionalMember("UninstallMenuClick", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) {
                return bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings(MIME_APK)))
                    .singleOrNull();
            }
        });
        return true;
    }

    @Override
    public void init() {
        mHookPoint = PrefsBridge.getString(PREFS_HOOK_POINT, HOOK_POINT_BOTH);
        mUninstallerSetting = PrefsBridge.getString(PREFS_UNINSTALLER, "");
        XposedLog.d(TAG, getPackageName(), "init: hookPoint=" + mHookPoint
            + ", uninstaller=" + describeUninstaller(mUninstallerSetting));

        boolean hookMenu = HOOK_POINT_MENU.equals(mHookPoint) || HOOK_POINT_BOTH.equals(mHookPoint);
        boolean hookCore = HOOK_POINT_CORE.equals(mHookPoint) || HOOK_POINT_BOTH.equals(mHookPoint);

        if (hookMenu) {
            if (mMenuClickMethod != null) {
                hookMethod(mMenuClickMethod, mMenuItemHook);
                XposedLog.d(TAG, getPackageName(), "[menu] hooked " + mMenuClickMethod);
            } else {
                XposedLog.w(TAG, getPackageName(),
                    "[menu] uninstall menu click not found, this entry is inactive");
            }
        }

        if (hookCore) {
            hookCoreUninstall();
            // core 命中时宿主方法里拿不到 Activity，这里记录最近的前台 Activity，
            // 让 core 入口的转发方式与菜单入口一致。
            hookAllMethods(Activity.class, "onResume", mActivityResumeHook);
        }
    }

    /**
     * 挂底层卸载调用。
     */
    private void hookCoreUninstall() {
        for (String[] api : CORE_APIS) {
            hookAllMethods(api[0], api[1], coreCallback(api[0] + "#" + api[1]));
        }

        Class<?> appManageUtils = findClassIfExists("com.miui.appmanager.AppManageUtils");
        if (appManageUtils == null) {
            return;
        }
        for (Method method : appManageUtils.getDeclaredMethods()) {
            if (method.getName().toLowerCase().contains("uninstall")) {
                hookMethod(method, coreCallback(method.toString()));
            }
        }
    }

    // ==================== 转发 ====================

    /**
     * 把卸载请求交给卸载器：优先用界面 Activity 启动，其次用宿主 Application（无前台界面时）。
     *
     * @param source 日志来源标记（{@code menu} / {@code core}）
     * @return 是否成功启动
     */
    private boolean forwardUninstall(String source, Activity activity, String pkgName) {
        Context context = activity != null ? activity : getHostContext();
        if (context == null) {
            XposedLog.w(TAG, getPackageName(), "[" + source + "] no Context available, cannot forward");
            return false;
        }

        XposedLog.d(TAG, getPackageName(), "[" + source + "] forward uninstall: pkg=" + pkgName
            + ", uninstaller=" + describeUninstaller(mUninstallerSetting));

        if (startUninstallIntent(source, context, activity == null,
            buildUninstallIntent(context, pkgName, mUninstallerSetting))) {
            return true;
        }

        if (!TextUtils.isEmpty(mUninstallerSetting)) {
            XposedLog.w(TAG, getPackageName(), "[" + source + "] uninstaller "
                + describeUninstaller(mUninstallerSetting) + " failed, fallback to system default resolver");
            return startUninstallIntent(source, context, activity == null,
                buildUninstallIntent(context, pkgName, ""));
        }
        return false;
    }

    private boolean startUninstallIntent(String source, Context context, boolean newTask, Intent intent) {
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            context.startActivity(intent);
            XposedLog.d(TAG, getPackageName(), "[" + source + "] forwarded: " + intent);
            return true;
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "[" + source + "] start uninstall Activity failed: " + intent, t);
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
     * 拿宿主进程的 Application context（core 时机且没有前台 Activity 时）。
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
            XposedLog.w(TAG, getPackageName(), "[core] getHostContext failed", t);
        }
        return null;
    }

    private Activity getLastActivity() {
        Activity activity = mLastActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        return activity;
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

    // ==================== 判定 ====================

    /**
     * 从底层调用的参数里取被卸载的包名：字符串参数或 {@code VersionedPackage}。
     */
    private String extractPackageName(Object[] args) {
        if (args == null) {
            return null;
        }

        for (Object arg : args) {
            if (arg instanceof String) {
                String value = (String) arg;
                if (PACKAGE_NAME_PATTERN.matcher(value).matches()) {
                    return value;
                }
            } else if (arg != null && "android.content.pm.VersionedPackage".equals(arg.getClass().getName())) {
                Object name = callMethod(arg, "getPackageName");
                if (name instanceof String && !TextUtils.isEmpty((String) name)) {
                    return (String) name;
                }
            }
        }
        return null;
    }

    /**
     * 判断被点击的菜单项是不是「卸载」。
     * <p>
     * 优先按标题与宿主字符串资源比对，取不到时退回 item id 判断。
     */
    private boolean isUninstallMenuItem(Activity activity, MenuItem item) {
        CharSequence title = item.getTitle();
        if (!TextUtils.isEmpty(title)) {
            Resources hostRes = activity.getResources();
            String hostPkg = activity.getPackageName();
            for (String name : UNINSTALL_STRING_NAMES) {
                int resId = hostRes.getIdentifier(name, "string", hostPkg);
                if (resId == 0) {
                    continue;
                }
                if (title.equals(hostRes.getString(resId))) {
                    return true;
                }
            }
        }
        return item.getItemId() == UNINSTALL_ITEM_ID;
    }

    /**
     * 取出承载界面的 Activity：宿主菜单回调可能挂在 Activity 上，也可能挂在 Fragment 上。
     */
    private Activity resolveActivity(Object thisObject) {
        if (thisObject instanceof Activity) {
            return (Activity) thisObject;
        }

        for (String methodName : new String[]{"getActivity", "requireActivity"}) {
            try {
                Object result = callMethod(thisObject, methodName);
                if (result instanceof Activity) {
                    return (Activity) result;
                }
            } catch (Throwable ignored) {
                // 不是 Fragment 或方法不存在，继续尝试下一种取值方式。
            }
        }
        return null;
    }

    /**
     * 找出被卸载应用的包名：优先反射宿主保存的 {@link PackageInfo} 字段，其次读界面 Intent 的 extra。
     */
    private String resolvePackageName(Object thisObject, Activity activity) {
        PackageInfo packageInfo = findPackageInfo(thisObject);
        if (packageInfo == null) {
            packageInfo = findPackageInfo(activity);
        }
        if (packageInfo != null && !TextUtils.isEmpty(packageInfo.packageName)) {
            return packageInfo.packageName;
        }

        Intent intent = activity.getIntent();
        if (intent != null) {
            String pkgName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
            if (!TextUtils.isEmpty(pkgName)) {
                return pkgName;
            }
        }
        return null;
    }

    private PackageInfo findPackageInfo(Object target) {
        if (target == null) {
            return null;
        }

        for (Class<?> clazz = target.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Field field = findFirstFieldByExactType(clazz, PackageInfo.class);
                if (field == null) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof PackageInfo) {
                    return (PackageInfo) value;
                }
            } catch (Throwable ignored) {
                // 忽略单个类上的反射失败，继续看父类。
            }
        }
        return null;
    }
}
