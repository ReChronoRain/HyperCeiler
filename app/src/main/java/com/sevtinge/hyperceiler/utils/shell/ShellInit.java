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

    public static void init() {
        mShell = new ShellExec(true, true);
    }

    public static void destroy() {
        if (mShell != null) mShell.close();
    }

    public static ShellExec getShell() {
        if (mShell != null) {
            return mShell;
        } else {
            AndroidLogUtils.LogE(ITAG.TAG, "ShellExec is null!!", null);
            return null;
        }
    }

    public static boolean ready() {
        if (mShell != null) {
            return mShell.ready();
        }
        return false;
    }
}
