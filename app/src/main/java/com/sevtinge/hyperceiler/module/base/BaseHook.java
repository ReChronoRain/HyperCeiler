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
package com.sevtinge.hyperceiler.module.base;

import static com.sevtinge.hyperceiler.utils.log.LogManager.logLevel;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.module.base.tool.HookTool;
import com.sevtinge.hyperceiler.module.base.tool.ResourcesTool;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseHook extends HookTool {
    public String TAG = getClass().getSimpleName();
    public static final ResourcesTool mResHook = XposedInit.mResHook;
    // public static final XmlTool mXmlTool = BaseXposedInit.mXmlTool;
    public static final String ACTION_PREFIX = "com.sevtinge.hyperceiler.module.action.";

    public abstract void init() throws NoSuchMethodException;

    public void onCreate(LoadPackageParam lpparam) {
        try {
            setLoadPackageParam(lpparam);
            init();
            logHookSuccess(lpparam);
        } catch (Throwable t) {
            logHookFailure(t, lpparam);
        }
    }

    private void logHookSuccess(LoadPackageParam lpparam) {
        if (logLevel >= 3) {
            logI(TAG, lpparam.packageName, "Hook Success.");
        }
    }

    private void logHookFailure(Throwable t, LoadPackageParam lpparam) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        if (logLevel >= 1) {
            logE(TAG, lpparam.packageName, "Hook Failed: " + stringWriter);
        }
    }

    @Override
    public void setLoadPackageParam(LoadPackageParam param) {
        lpparam = param;
    }
}
