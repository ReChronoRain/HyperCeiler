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
package com.sevtinge.hyperceiler.module.app;

import android.content.Context;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.HookExpand;
import com.sevtinge.hyperceiler.module.base.tool.OtherTool;
import com.sevtinge.hyperceiler.module.hook.clipboard.BaiduClipboard;
import com.sevtinge.hyperceiler.module.hook.clipboard.SoGouClipboard;
import com.sevtinge.hyperceiler.module.hook.various.ClipboardList;
import com.sevtinge.hyperceiler.module.hook.various.UnlockIme;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import java.util.ArrayList;
import java.util.List;

@HookExpand(pkg = "VariousThirdApps", isPad = false, tarAndroid = 33, skip = true)
public class VariousThirdApps extends BaseModule {
    String mPackageName;

    public static List<String> mAppsUsingInputMethod = new ArrayList<>();

    @Override
    public void handleLoadPackage() {
        if (mAppsUsingInputMethod.isEmpty()) {
            mAppsUsingInputMethod = getAppsUsingInputMethod(OtherTool.findContext(OtherTool.FlAG_ONLY_ANDROID));
        }
        mPackageName = mLoadPackageParam.packageName;
        initHook(new UnlockIme(), mPrefsMap.getBoolean("various_unlock_ime") && isInputMethod(mPackageName));
        initHook(new SoGouClipboard(), mPrefsMap.getBoolean("sogou_xiaomi_clipboard") &&
                ("com.sohu.inputmethod.sogou.xiaomi".equals(mPackageName) || "com.sohu.inputmethod.sogou".equals(mPackageName)));
        initHook(new BaiduClipboard(), mPrefsMap.getBoolean("sogou_xiaomi_clipboard") &&
                ("com.baidu.input".equals(mPackageName) || "com.baidu.input_mi".equals(mPackageName)));
        initHook(new ClipboardList(), mPrefsMap.getBoolean("various_phrase_clipboardlist") && isInputMethod(mPackageName));
    }

    private List<String> getAppsUsingInputMethod(Context context) {
        try {
            if (context == null) {
                XposedLogUtils.logE("getAppsUsingInputMethod", "context is null");
                return new ArrayList<>();
            }
            List<String> pkgName = new ArrayList<>();
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> enabledInputMethods = inputMethodManager.getEnabledInputMethodList();
            for (InputMethodInfo inputMethodInfo : enabledInputMethods) {
                pkgName.add(inputMethodInfo.getServiceInfo().packageName);
            }
            return pkgName;
        } catch (Throwable e) {
            XposedLogUtils.logE("getAppsUsingInputMethod", "have e: " + e);
            return new ArrayList<>();
        }
    }

    private boolean isInputMethod(String pkgName) {
        if (mAppsUsingInputMethod.isEmpty()) {
            return false;
        }
        for (String inputMethod : mAppsUsingInputMethod) {
            if (inputMethod.equals(pkgName)) {
                return true;
            }
        }
        return false;
    }
}
