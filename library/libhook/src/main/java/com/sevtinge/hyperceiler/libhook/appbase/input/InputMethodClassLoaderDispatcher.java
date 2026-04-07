/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.appbase.input;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.ClearClipboard;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.ClipboardUnlock;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.UnlockIme;
import com.sevtinge.hyperceiler.libhook.rules.various.input.MiAospIme;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class InputMethodClassLoaderDispatcher extends BaseHook {
    private final Map<ClassLoader, Boolean> mLoadedClassLoaders =
        Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void init() {
        List<InputMethodDexHelper.Loader> enabledLoaders = buildEnabledLoaders();
        if (enabledLoaders.isEmpty()) {
            return;
        }

        try {
            findAndHookMethod(
                EzxHelpUtils.findClass("android.inputmethodservice.InputMethodModuleManager", null),
                "loadDex",
                ClassLoader.class, String.class,
                new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        Object classLoader = param.getArgs()[0];
                        if (!(classLoader instanceof ClassLoader imeClassLoader)) {
                            return;
                        }

                        if (mLoadedClassLoaders.put(imeClassLoader, Boolean.TRUE) != null) {
                            return;
                        }

                        InputMethodDexHelper.dispatchLoaders(
                            TAG, getPackageName(), imeClassLoader, enabledLoaders);
                    }
                }
            );
        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "Failed to hook InputMethodModuleManager.loadDex", t);
        }
    }

    private List<InputMethodDexHelper.Loader> buildEnabledLoaders() {
        List<InputMethodDexHelper.Loader> enabledLoaders = new ArrayList<>(3);

        if (PrefsBridge.getBoolean("various_phrase_clipboardlist")) {
            enabledLoaders.add(InputMethodDexHelper.loader(
                "ClipboardUnlock",
                classLoader -> new ClipboardUnlock().initLoader(classLoader)
            ));
        }

        if (PrefsBridge.getBoolean("add_clipboard_clear")) {
            enabledLoaders.add(InputMethodDexHelper.loader(
                "ClearClipboard",
                classLoader -> new ClearClipboard().initLoader(classLoader)
            ));
        }

        if (InputMethodConfig.shouldHookMiuiIme(getPackageName())) {
            enabledLoaders.add(InputMethodDexHelper.loader(
                "UnlockIme",
                classLoader -> new UnlockIme().initLoader(classLoader)
            ));
        }

        if (InputMethodConfig.shouldHookAospIme(getPackageName())) {
            enabledLoaders.add(InputMethodDexHelper.loader(
                "MiAospIme",
                classLoader -> new MiAospIme().initLoader(classLoader)
            ));
        }

        return enabledLoaders;
    }
}
