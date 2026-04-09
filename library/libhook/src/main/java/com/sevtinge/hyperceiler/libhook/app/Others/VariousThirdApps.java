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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.app.Others;

import android.content.Context;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.appbase.input.InputMethodClassLoaderDispatcher;
import com.sevtinge.hyperceiler.libhook.appbase.input.InputMethodConfig;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.various.MusicHooks;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.BaiduClipboard;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.SoGouClipboard;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.UnlockIme;
import com.sevtinge.hyperceiler.libhook.rules.various.input.MiAospIme;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@HookBase(targetPackage = "VariousThirdApps")
public class VariousThirdApps extends BaseLoad {
    private static final String XIAOMI_SOGOU_PACKAGE = "com.sohu.inputmethod.sogou.xiaomi";
    private static final String SOGOU_PACKAGE = "com.sohu.inputmethod.sogou";
    private static final String XIAOMI_BAIDU_PACKAGE = "com.baidu.input_mi";
    private static final String BAIDU_PACKAGE = "com.baidu.input";

    private static Set<String> sEnabledInputMethodPackages = Collections.emptySet();

    private String mPackageName;


    @Override
    public void onPackageLoaded() {
        mPackageName = getPackageName();
        boolean isInputMethod = isEnabledInputMethodPackage(mPackageName);

        if (isInputMethod) {
            initInputMethodHooks();
            initClipboardHooks();
            return;
        }

        initMusicHooks();
    }

    private void initInputMethodHooks() {
        boolean needMiuiImeUnlock = InputMethodConfig.shouldHookMiuiIme(mPackageName);
        boolean needAospIme = InputMethodConfig.shouldHookAospIme(mPackageName);

        initHook(new InputMethodClassLoaderDispatcher());
        initHook(new UnlockIme(), needMiuiImeUnlock);
        initHook(new MiAospIme(), needAospIme);
    }

    private void initClipboardHooks() {
        boolean enableClipboardHook = PrefsBridge.getBoolean("sogou_xiaomi_clipboard");

        initHook(new SoGouClipboard(), enableClipboardHook && isSogouPackage(mPackageName));
        initHook(new BaiduClipboard(), enableClipboardHook && isBaiduPackage(mPackageName));
    }

    private void initMusicHooks() {
        initHook(MusicHooks.INSTANCE, PrefsBridge.getBoolean("system_ui_statusbar_music_switch") && PrefsBridge.getBoolean("system_ui_statusbar_music_show_app"));
    }
    private Set<String> getEnabledInputMethodPackages(Context context) {
        try {
            if (context == null) {
                XposedLog.e("getEnabledInputMethodPackages", "context is null");
                return Collections.emptySet();
            }

            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager == null) {
                XposedLog.e("getEnabledInputMethodPackages", "inputMethodManager is null");
                return Collections.emptySet();
            }

            List<InputMethodInfo> enabledInputMethods = inputMethodManager.getEnabledInputMethodList();
            LinkedHashSet<String> packages = new LinkedHashSet<>();
            for (InputMethodInfo inputMethodInfo : enabledInputMethods) {
                if (inputMethodInfo.getServiceInfo() != null) {
                    packages.add(inputMethodInfo.getServiceInfo().packageName);
                }
            }
            return packages;
        } catch (Throwable e) {
            XposedLog.e("getEnabledInputMethodPackages", "have e: " + e + ", message: " + e.getMessage());
            return Collections.emptySet();
        }
    }

    private boolean isEnabledInputMethodPackage(String packageName) {
        if (sEnabledInputMethodPackages.isEmpty()) {
            sEnabledInputMethodPackages = getEnabledInputMethodPackages(
                AppsTool.findContext(AppsTool.FlAG_ONLY_ANDROID));
        }
        return sEnabledInputMethodPackages.contains(packageName);
    }

    private boolean isSogouPackage(String packageName) {
        return XIAOMI_SOGOU_PACKAGE.equals(packageName) || SOGOU_PACKAGE.equals(packageName);
    }

    private boolean isBaiduPackage(String packageName) {
        return BAIDU_PACKAGE.equals(packageName) || XIAOMI_BAIDU_PACKAGE.equals(packageName);
    }
}
