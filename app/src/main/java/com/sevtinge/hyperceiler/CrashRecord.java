package com.sevtinge.hyperceiler;

import androidx.annotation.NonNull;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 非常不靠谱，我真搞不懂没有 Context 怎么存储
 * 下下策存文件夹？但是可能失效，所以仅作辅助。
 */
public class CrashRecord implements Thread.UncaughtExceptionHandler {
    private final String path = "/sdcard/Android/hy_crash/";
    private final String pkg;
    private int count = 0;
    private Thread.UncaughtExceptionHandler mDef;


    public CrashRecord(XC_LoadPackage.LoadPackageParam param) {
        pkg = param.packageName;
        if (pkg.equals("com.miui.contentcatcher")) return;
        mDef = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        /*AndroidLogUtils.LogE(ITAG.TAG, e);
        ShellUtils.CommandResult commandResult = ShellUtils.execCommand("ls " + path + " | grep " + pkg, false, true);
        List<String> success = null;
        if (commandResult.result == 0) {
            success = commandResult.successMsg;
        }
        if (success != null) {
            AndroidLogUtils.LogI(ITAG.TAG, "rss: " + success);
            Pattern pattern = Pattern.compile(".*_(.*)");
            Matcher matcher = pattern.matcher(success.get(0));
            if (matcher.find()) {
                count = Integer.parseInt(matcher.group(1));
            }
            count = count + 1;
            ShellUtils.execCommand("rm -rf " + path + success.get(0) + " && mkdir -p " + path + pkg + "_" + count, false, false);
        } else {
            ShellUtils.execCommand("mkdir -p " + path + pkg + "_0", false, false);
        }
        if (mDef != null) {
            mDef.uncaughtException(t, e);
        }*/
    }

}
