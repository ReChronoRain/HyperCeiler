package com.sevtinge.hyperceiler;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.NonNull;

import com.fan.common.logviewer.LogManager;
import com.fan.common.logviewer.LogViewerActivity;
import com.fan.common.logviewer.XposedLogLoader;
import com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper;
import com.sevtinge.hyperceiler.common.utils.ScopeManager;
import com.sevtinge.hyperceiler.home.task.AppInitializer;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.oldui.model.data.AppInfoCache;
import com.sevtinge.hyperceiler.oldui.safemode.ExceptionCrashActivity;
import com.sevtinge.hyperceiler.oldui.utils.DeviceInfoBuilder;
import com.sevtinge.hyperceiler.provision.fragment.PermissionSettingsFragment;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.github.libxposed.service.RemotePreferences;
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
        PrefsBridge.init(this);
        //PrefsUtils.init(base);
        XposedServiceHelper.registerListener(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 应用启动阶段，预热非 UI 任务（如 Shell、语言包、权限检查）
        AppInitializer.initOnAppCreate(this);
        // 初始化日志系统
        com.sevtinge.hyperceiler.libhook.utils.log.LogManager.init(this.getDataDir().getAbsolutePath());
        LogManager.setDeviceInfoProvider(DeviceInfoBuilder::build);
        LogManager.init(this);
        LogViewerActivity.setXposedLogLoader((context, callback) -> XposedLogLoader.loadLogs(callback));

        setupCrashHandler();
    }

    @Override
    public void onServiceBind(@NonNull XposedService service) {
        AndroidLog.d(TAG, "LSPosed service connected: " + service.getFrameworkName() + " v" + service.getFrameworkVersion());
        synchronized (this) {
            isModuleActivated = true;
            PermissionSettingsFragment.isModuleActive = true;
            ScopeManager.setService(service);
            PrefsBridge.setRemotePrefs((RemotePreferences) service.getRemotePreferences(PrefsBridge.PREFS_NAME + "_remote"));

            //PrefsUtils.remotePrefs = (RemotePreferences) service.getRemotePreferences(PrefsUtils.mPrefsName + "_remote");
            reloadListener.run();
        }
    }

    @Override
    public void onServiceDied(@NonNull XposedService service) {
        AndroidLog.e(TAG, "LSPosed service died.");
        synchronized (this) {
            isModuleActivated = false;
            PermissionSettingsFragment.isModuleActive = false;
            PrefsUtils.remotePrefs = null;
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
