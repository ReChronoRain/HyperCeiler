package com.sevtinge.hyperceiler;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.sevtinge.hyperceiler.common.utils.LanguageHelper;
import com.sevtinge.hyperceiler.holiday.HolidayHelper;
import com.sevtinge.hyperceiler.home.CrashReportManager;
import com.sevtinge.hyperceiler.libhook.callback.IResult;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import com.sevtinge.hyperceiler.libhook.utils.pkg.CheckModifyUtils;
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellInit;
import com.sevtinge.hyperceiler.oldui.utils.LogServiceUtils;
import com.sevtinge.hyperceiler.oldui.utils.NoticeProcessor;
import com.sevtinge.hyperceiler.oldui.utils.PermissionUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppInitializer {
    private static final List<String> CHECK_LIST = List.of(
        "com.miui.securitycenter",
        "com.android.camera",
        "com.miui.home"
    );

    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static ExecutorService sExecutor = Executors.newCachedThreadPool();

    public interface OnDataReadyListener {
        void onReady(List<String> crashes, NoticeProcessor.NoticeResult notice);
    }

    public static void start(Activity activity, OnDataReadyListener listener) {
        // 1. 立即执行的轻量任务
        LanguageHelper.init(activity);
        HolidayHelper.init(activity);

        // 2. 异步执行重型任务
        sExecutor.execute(() -> {
            // 组件初始化
            ShellInit.init((IResult) activity);
            PermissionUtils.init(activity);
            LogServiceUtils.init(activity);

            // 业务逻辑预加载
            checkSignatures(activity);
            List<String> crashes = CrashReportManager.getCrashList();
            NoticeProcessor.NoticeResult notice = NoticeProcessor.process(activity);

            // 回调 UI 线程
            sHandler.post(() -> {
                if (listener != null) listener.onReady(crashes, notice);
            });
        });
    }

    private static void checkSignatures(Context context) {
        CHECK_LIST.forEach(pkg -> {
            boolean check = CheckModifyUtils.INSTANCE.isApkModified(context, pkg, CheckModifyUtils.XIAOMI_SIGNATURE);
            CheckModifyUtils.INSTANCE.setCheckResult(pkg, check);
        });
    }

    public static void release() {
        ShellInit.destroy();
        ThreadPoolManager.shutdown();
    }
}
