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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils.shell;

import com.sevtinge.hyperceiler.callback.IResult;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

/**
 * 本工具默认使用 Root 启动。
 * 本工具只可以在本应用使用！不可在 Hook 代码内使用！
 *
 * @author 焕晨HChen
 */
public class ShellInit {
    private final static String TAG = "ShellInit";
    private static ShellExec mShell = null;
    private static boolean lastReady = false;
    private static IResult mResult = null;

    public static void init() {
        init(null);
    }

    public static void init(IResult result) {
        try {
            if (mShell != null && !mShell.isDestroy()) {
                return;
            }
            mResult = result;
            mShell = new ShellExec(true, true, result);
            lastReady = mShell.ready();
        } catch (RuntimeException e) {
            AndroidLogUtils.logE(TAG, e);
        }
    }

    public static void destroy() {
        if (mShell != null && !mShell.isDestroy()) {
            mShell.close();
            mShell = null;
            mResult = null;
        } else if (mShell != null && mShell.isDestroy()) {
            mShell = null;
            mResult = null;
        }
    }

    public static ShellExec getShell() {
        if (mShell != null) {
            if (!mShell.isRoot()) {
                return mShell;
            }
            if (mShell.isDestroy()) {
                AndroidLogUtils.logW(TAG, "The current shell has been destroyed, please try creating it again!");
                mShell = new ShellExec(true, true, mResult);
            }
            return mShell;
        } else {
            if (lastReady) {
                AndroidLogUtils.logW(TAG, "ShellExec is null!! Attempt to rewrite creation...");
                return new ShellExec(true, true, mResult);
            } else {
                throw new RuntimeException("ShellExec is null!! " +
                    "And it seems like it has never been created successfully!");
            }
        }
    }

    public static boolean ready() {
        if (mShell != null) {
            if (!mShell.isRoot()) {
                return false;
            }
            if (mShell.isDestroy()) {
                init(mResult);
            }
            return mShell.ready();
        }
        if (lastReady) {
            init(mResult);
            return mShell.ready();
        }
        return false;
    }
}
