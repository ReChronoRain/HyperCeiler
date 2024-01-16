package com.sevtinge.hyperceiler.module.base;

import com.sevtinge.hyperceiler.CrashRecord;
import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.utils.PrefsMap;
import com.sevtinge.hyperceiler.utils.ShellUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseModule implements IXposedHook {

    public LoadPackageParam mLoadPackageParam = null;
    public final PrefsMap<String, Object> mPrefsMap = XposedInit.mPrefsMap;
    private final String path = "/sdcard/Android/hy_crash/";
    private int count = 0;

    public void init(LoadPackageParam lpparam) {
        new CrashRecord(lpparam);
        if (checkCount(lpparam.packageName) >= 3) {
            return;
        }
        mLoadPackageParam = lpparam;
        initZygote();
        handleLoadPackage();
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
