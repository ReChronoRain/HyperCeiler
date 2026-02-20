package com.sevtinge.hyperceiler.home.task;

import static com.sevtinge.hyperceiler.common.utils.DialogHelper.showUserAgreeDialog;
import static com.sevtinge.hyperceiler.common.utils.PersistConfig.isNeedGrayView;
import static com.sevtinge.hyperceiler.oldui.Application.isModuleActivated;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.CtaUtils;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper;
import com.sevtinge.hyperceiler.common.utils.LanguageHelper;
import com.sevtinge.hyperceiler.holiday.HolidayHelper;
import com.sevtinge.hyperceiler.home.manager.PageDecorator;
import com.sevtinge.hyperceiler.home.utils.XposedActivateHelper;
import com.sevtinge.hyperceiler.libhook.callback.IResult;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashScope;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.pkg.CheckModifyUtils;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellInit;
import com.sevtinge.hyperceiler.oldui.model.data.AppInfoCache;
import com.sevtinge.hyperceiler.oldui.utils.LogServiceUtils;
import com.sevtinge.hyperceiler.search.SearchHelper;
import com.sevtinge.hyperceiler.ui.HomePageActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import fan.provision.OobeUtils;

/**
 * 业务管理类：定义任务图及原本属于 Activity 的计算逻辑
 */
public class AppTaskManager {

    private static final String TAG = "AppTaskManager";

    private static final List<String> CHECK_LIST = List.of(
        "com.miui.securitycenter",
        "com.android.camera",
        "com.miui.home"
    );

    private static final Map<String, Integer> APP_NAME_RES_MAP = Map.of(
        "com.android.systemui", R.string.system_ui,
        "com.android.settings", R.string.system_settings,
        "com.miui.home", R.string.mihome,
        "com.hchen.demo", R.string.demo,
        "com.miui.securitycenter", R.string.security_center_hyperos
    );

    /**
     * 第一阶段任务定义：Application 级别
     */
    public static void setupAppTasks(Context context) {
        TaskRunner runner = TaskRunner.getInstance();

        runner.addTask(new Task("LanguageInit", false) { // 主线程同步执行
            @Override
            public void execute() {
                // 提前初始化语言，保证首屏渲染就是正确的语言
                LanguageHelper.init(context, PrefsUtils.mSharedPreferences);
            }
        });

        runner.addTask(new Task("BaseEnv", false) {
            @Override
            public void execute() {
                LSPosedScopeHelper.init();
                AppInfoCache.getInstance(context).init();
                // 原 onCreate 里的各种 Helper 初始化
                //PermissionUtils.init(context);
            }
        });

        runner.addTask(new Task("CoreService", true, "BaseEnv") {
            @Override
            public void execute() {
                LogServiceUtils.init(context.getApplicationContext());
            }
        });
    }

    /**
     * 第二阶段任务定义：Activity 级别
     */
    public static void setupActivityTasks(HomePageActivity activity) {
        TaskRunner runner = TaskRunner.getInstance();

        // UI 装饰任务：必须在主线程执行 (isAsync = false)
        runner.addTask(new Task("UI_Theme", false) {
            @Override
            public void execute() {
                // 调用装饰器：处理滤镜和节日特效
                PageDecorator.decorate(activity);
                XposedActivateHelper.init(activity.getApplicationContext());
            }
        });

        // 异步业务逻辑：计算崩溃、签名校验
        runner.addTask(new Task("BusinessLogic", true, "CoreService") {
            @Override
            public void execute() {
                ShellInit.init(activity);
                // 原 Activity 的 checkAppMod 逻辑迁移到此
                CHECK_LIST.parallelStream().forEach(pkg -> {
                    checkAppMod(activity, pkg);
                });

                // 原 Activity 的 computeCrashList 逻辑迁移到此
                List<String> crashes = computeCrashList();

                // 逻辑执行完，回调 UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    showSafeModeDialogIfNeeded(activity, crashes);
                });
            }
        });

        runner.addTask(new Task("UI_Effect", false) {
            @Override
            public void execute() {
                requestCta(activity);
                XposedActivateHelper.init(activity);
                registerObserver(activity.getApplicationContext());
                SearchHelper.initIndex(activity, true);
            }
        });
    }

    private static void registerObserver(Context context) {
        PrefsUtils.registerOnSharedPreferenceChangeListener(context);
        AppsTool.fixPermissionsAsync(context);
        AppsTool.registerFileObserver(context);
    }

    public static void requestCta(HomePageActivity activity) {
        if (OobeUtils.getOperatorState(activity, "cm_pick_status")) return;
        if (CtaUtils.isCtaNeedShow(activity)) {
            if (CtaUtils.isCtaBypass()) {
                CtaUtils.showCtaDialog(activity.mCtaLauncher, activity);
            } else {
                showUserAgreeDialog(activity);
            }
        }
    }

    private static void checkAppMod(Context context, String pkg) {
        boolean check = CheckModifyUtils.INSTANCE.isApkModified(context, pkg, CheckModifyUtils.XIAOMI_SIGNATURE);
        CheckModifyUtils.INSTANCE.setCheckResult(pkg, check);
    }

    /**
     * 原 Activity 的纯计算逻辑剥离到此
     */
    private static List<String> computeCrashList() {
        try {
            List<?> raw = CrashScope.getCrashingPackages();
            if (raw.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> result = new ArrayList<>(raw.size());
            for (Object o : raw) {
                if (o instanceof String s) {
                    result.add(s);
                } else if (o != null) {
                    result.add(o.toString());
                }
            }
            return result;
        } catch (Throwable t) {
            AndroidLog.e(TAG, "CrashData: " + t);
            return Collections.emptyList();
        }
    }

    @SuppressLint("StringFormatInvalid")
    private static void showSafeModeDialogIfNeeded(Context context, List<String> appCrash) {
        if (appCrash.isEmpty()) return;

        String appName = buildCrashAppNames(context, appCrash);
        if (appName.isEmpty()) return;

        String msg = cleanUpMessage(context.getString(R.string.safe_mode_later_desc, appName));
        DialogHelper.showSafeModeDialog(context, msg);
    }

    private static String buildCrashAppNames(Context context, List<String> appCrash) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String pkg : appCrash) {
            Integer resId = APP_NAME_RES_MAP.get(pkg);
            if (resId != null) {
                joiner.add(context.getString(resId) + " (" + pkg + ")");
            }
        }
        return joiner.toString();
    }

    private static String cleanUpMessage(String msg) {
        if (msg == null || msg.isEmpty()) return "";

        StringBuilder sb = new StringBuilder(msg.length());
        char prev = 0;
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (c == '[' || c == ']') continue;
            if (c == ' ' && prev == ' ') continue;
            if (c == ' ' && (prev == '，' || prev == '、')) continue;
            sb.append(c);
            prev = c;
        }
        return sb.toString().trim();
    }
}
