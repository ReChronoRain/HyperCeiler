package com.sevtinge.hyperceiler.utils.shell;

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

/**
 * 本工具默认使用 Root 启动。
 * 本工具只可以在本应用使用！不可在 Hook 代码内使用！
 *
 * @author 焕晨HChen
 */
public class ShellInit {
    private static ShellExec mShell = null;
    private static boolean lastReady = false;

    public static void init() {
        try {
            if (mShell != null && !mShell.isDestroy()) {
                return;
            }
            mShell = new ShellExec(true, true);
            lastReady = mShell.ready();
        } catch (RuntimeException e) {
            AndroidLogUtils.LogE(ITAG.TAG, e);
        }
    }

    public static void destroy() {
        if (mShell != null && !mShell.isDestroy()) {
            mShell.close();
            mShell = null;
        } else if (mShell != null && mShell.isDestroy()) {
            mShell = null;
        }
    }

    public static ShellExec getShell() {
        if (mShell != null) {
            if (mShell.isDestroy()) {
                AndroidLogUtils.LogE(ITAG.TAG, "The current shell has been destroyed, please try creating it again!");
                mShell = new ShellExec(true, true);
            }
            return mShell;
        } else {
            if (lastReady) {
                AndroidLogUtils.LogE(ITAG.TAG, "ShellExec is null!! Attempt to rewrite creation...");
                return new ShellExec(true, true);
            } else {
                throw new RuntimeException("ShellExec is null!! " +
                    "And it seems like it has never been created successfully!");
            }
        }
    }

    public static boolean ready() {
        if (mShell != null) {
            if (mShell.isDestroy()) {
                init();
            }
            return mShell.ready();
        }
        return false;
    }
}
