package com.sevtinge.hyperceiler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.log.LogManager;
import com.sevtinge.hyperceiler.common.log.LogStatusManager;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.home.safemode.ExceptionCrashActivity;
import com.sevtinge.hyperceiler.home.task.AppInitializer;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.log.XposedLogLoader;
import com.sevtinge.hyperceiler.logviewer.LogViewerActivity;
import com.sevtinge.hyperceiler.provision.fragment.PermissionSettingsFragment;
import com.sevtinge.hyperceiler.utils.DeviceInfoBuilder;
import com.sevtinge.hyperceiler.utils.ScopeManager;

import java.io.PrintWriter;
import java.io.StringWriter;

import fan.provision.OobeUtils;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class Application extends fan.app.Application
    implements XposedServiceHelper.OnServiceListener {

    private static final String TAG = "Application";
    public static boolean isModuleActivated = false;
    private static final Runnable reloadListener = () -> {};

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        AppInitializer.attach(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 应用启动阶段，预热非 UI 任务（如 Shell、语言包、权限检查）
        AppInitializer.initOnAppCreate(this);
        OobeUtils.syncHookAvailability(this);

        Context appContext = this;
        com.sevtinge.hyperceiler.libhook.utils.log.LogManager.init(
            this.getDataDir().getAbsolutePath(),
            () -> com.sevtinge.hyperceiler.logviewer.XposedLogLoader.getInstance(appContext).syncLogsSync(),
            () -> {
                com.sevtinge.hyperceiler.logviewer.LogManager.init(appContext);
                com.sevtinge.hyperceiler.logviewer.LogManager.setDeviceInfoProvider(DeviceInfoBuilder::build);
                LogViewerActivity.setXposedLogLoader((context, callback) ->
                    com.sevtinge.hyperceiler.logviewer.XposedLogLoader.loadLogs(callback));
            });

        // 初始化日志系统
        LogStatusManager.init(
            getDataDir().getAbsolutePath(),
            // 这里的 Runnable 是为了让 Hook 侧能在 App 启动时同步一次日志
            () -> XposedLogLoader.syncLogsToDatabase(this),
            () -> {
                LogManager.init(this);
                LogManager.setDeviceInfoProvider(DeviceInfoBuilder::build);
                XposedLogLoader.syncLogsToDatabase(this);
            }
        );
        setupCrashHandler();
    }


    @Override
    public void onServiceBind(@NonNull XposedService service) {
        AndroidLog.d(TAG, "LSPosed service connected: " + service.getFrameworkName() + " v" + service.getFrameworkVersion());
        synchronized (this) {
            isModuleActivated = true;
            PermissionSettingsFragment.isModuleActive = true;
            ScopeManager.setService(service);

            SharedPreferences remote = service.getRemotePreferences(PrefsBridge.PREFS_NAME + "_remote");
            PrefsBridge.setRemotePrefs(remote);
            OobeUtils.syncHookAvailability(this);

            reloadListener.run();
        }
    }

    @Override
    public void onServiceDied(@NonNull XposedService service) {
        AndroidLog.e(TAG, "LSPosed service died.");
        synchronized (this) {
            isModuleActivated = false;
            PermissionSettingsFragment.isModuleActive = false;
            PrefsBridge.setRemotePrefs(null);
        }
    }

    private void setupCrashHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            final String crashInfo = sw.toString();

            new Handler(Looper.getMainLooper()).post(() -> {
                Intent intent = new Intent(getApplicationContext(), ExceptionCrashActivity.class);
                intent.putExtra("crashInfo", crashInfo);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                getApplicationContext().startActivity(intent);
            });

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, ex);
            } else {
                android.os.Process.killProcess(Process.myPid());
                System.exit(1);
            }
        });
    }
}
