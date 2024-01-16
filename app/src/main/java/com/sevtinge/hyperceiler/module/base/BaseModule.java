package com.sevtinge.hyperceiler.module.base;

import android.content.Context;
import android.os.Handler;

import com.sevtinge.hyperceiler.CrashRecord;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.callback.TAG;
import com.sevtinge.hyperceiler.utils.ContextUtils;
import com.sevtinge.hyperceiler.utils.PrefsMap;
import com.sevtinge.hyperceiler.utils.ShellUtils;
import com.sevtinge.hyperceiler.utils.ToastHelper;
import com.sevtinge.hyperceiler.utils.XposedUtils;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseModule implements IXposedHook {

    public LoadPackageParam mLoadPackageParam = null;
    public final PrefsMap<String, Object> mPrefsMap = XposedInit.mPrefsMap;
    private final String path = "/sdcard/Android/hy_crash/";
    private int count = 0;
    ExecutorService executorService;
    Handler handler;

    public void init(LoadPackageParam lpparam) {
        new CrashRecord(lpparam);
        // XposedUtils.mResHook.addResource("id", moralnorm.appcompat.R.string.color_picker_tab_grid);
        // lpparam.classLoader.getResources("id");
        if (checkCount(lpparam.packageName) >= 3) {
            executorService = Executors.newFixedThreadPool(1);
            handler = new Handler();

            // checkCrash(lpparam);
            return;
        }
        mLoadPackageParam = lpparam;
        initZygote();
        handleLoadPackage();
    }

    private void checkCrash(LoadPackageParam param) {
        AndroidLogUtils.LogI(TAG.TAG, "runkk: " + executorService + " han: " + handler);
        executorService.submit(() -> {
            handler.post(() -> {
                Context context = ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP);
                if (context == null) {
                    while (true) {
                        context = ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP);
                        if (context != null) {
                            break;
                        }
                        try {
                            Thread.sleep(500);
                        } catch (Throwable throwable) {

                        }
                    }
                }
                int id = XposedUtils.mResHook.addResource("app_name", R.string.app_name);
                try {
                    // param.classLoader.getResources("app_name");
                    ToastHelper.makeText(context, context.getResources().getText(id));
                } catch (Throwable e) {

                }
                AndroidLogUtils.LogI(TAG.TAG, "con: " + id);
                ToastHelper.makeText(context, context.getResources().getText(id));
            });
        });
    }


    private int checkCount(String pkg) {
        ShellUtils.CommandResult commandResult = ShellUtils.execCommand("ls " + path + " | grep " + pkg, false, true);
        List<String> success = null;
        if (commandResult.result == 0) {
            success = commandResult.successMsg;
        }
        if (success != null) {
            // AndroidLogUtils.LogI(TAG.TAG, "rss: " + success);
            Pattern pattern = Pattern.compile(".*_(.*)");
            Matcher matcher = pattern.matcher(success.get(0));
            if (matcher.find()) {
                count = Integer.parseInt(matcher.group(1));
            }
        }
        return count;
    }

    @Override
    public void initZygote() {
    }

    public void initHook(BaseHook baseHook) {
        initHook(baseHook, true);
    }

    public void initHook(BaseHook baseHook, boolean isInit) {
        if (isInit) {
            baseHook.onCreate(mLoadPackageParam);
        }
    }
}
