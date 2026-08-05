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

import static com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass;
import static com.sevtinge.hyperceiler.libhook.base.BaseHook.setStaticObjectField;

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

        if ("com.google.android.googlequicksearchbox".equals(mPackageName)) {
            try {
                Class<?> mBuild = findClass("android.os.Build", getClassLoader());

                setStaticObjectField(mBuild, "MANUFACTURER", "Google");
                setStaticObjectField(mBuild, "BRAND", "google");
                setStaticObjectField(mBuild, "MODEL", "Pixel 9 Pro");
                setStaticObjectField(mBuild, "DEVICE", "caiman");
                XposedLog.d("GoogleQuickSearchBox", "Spoofed device info to Pixel 9 Pro success");
            } catch (Throwable e) {
                // android.os.Build's fields are public static final. ART used to allow a
                // reflective write, but Android 17 rejects it, and clearing the FINAL bit
                // in Field.accessFlags does not help either because finality is enforced
                // below that mirror. There is no pure-Java way to spoof these fields on
                // this release, so report it as a warning rather than an error.
                if (isFinalFieldRejection(e)) {
                    XposedLog.w("GoogleQuickSearchBox",
                        "Build field spoofing is not supported on this Android version: " + e.getMessage());
                } else {
                    XposedLog.e("GoogleQuickSearchBox", "Failed to spoof device info: " + e.getMessage());
                }
            }
            return;
        }

        initMusicHooks();
    }

    /** How many causes to walk before giving up, so a self-referential chain cannot loop. */
    private static final int MAX_CAUSE_DEPTH = 16;

    /** True when a throwable (or one of its causes) is the runtime refusing a final write. */
    private static boolean isFinalFieldRejection(Throwable t) {
        Throwable current = t;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof IllegalAccessException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("static final")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
