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

package com.sevtinge.hyperceiler.libhook.rules.various.clipboard;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

/**
 * 统一管理 InputMethodModuleManager.loadDex 的 hook。
 * <p>
 * 只 hook 一次 loadDex，获取到输入法的 ClassLoader 后，
 * 通过回调分发给所有注册的监听者。如果注册时 ClassLoader 已经就绪，
 * 则立即回调。
 * <p>
 * 使用方法：
 * <pre>{@code
 * // 在 VariousThirdApps 等入口处初始化
 * InputMethodDexHelper.init();
 *
 * // 在各个 Hook 中注册监听
 * InputMethodDexHelper.addListener(classLoader -> {
 *     // 使用 classLoader 做具体的 hook
 * });
 * }</pre>
 */
public final class InputMethodDexHelper {
    private static final String TAG = "InputMethodDexHelper";

    /**
     * 监听回调接口
     */
    public interface OnClassLoaderReadyListener {
        void onClassLoaderReady(@NonNull ClassLoader classLoader);
    }

    private static volatile boolean sInitialized = false;
    private static volatile boolean sLoaded = false;
    private static volatile ClassLoader sClassLoader = null;

    private static final List<OnClassLoaderReadyListener> sListeners = new CopyOnWriteArrayList<>();

    private InputMethodDexHelper() {}

    /**
     * 初始化 loadDex hook。只会执行一次。
     * 应在所有 Hook 注册之前调用。
     */
    public static synchronized void init() {
        if (sInitialized) return;
        sInitialized = true;

        try {
            EzxHelpUtils.findAndHookMethod(
                EzxHelpUtils.findClass("android.inputmethodservice.InputMethodModuleManager", null),
                "loadDex",
                ClassLoader.class, String.class,
                new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        if (sLoaded) return;
                        sLoaded = true;

                        ClassLoader classLoader = (ClassLoader) param.getArgs()[0];
                        sClassLoader = classLoader;

                        XposedLog.d(TAG, "Input method classloader ready: " + classLoader);

                        for (OnClassLoaderReadyListener listener : sListeners) {
                            try {
                                listener.onClassLoaderReady(classLoader);
                            } catch (Throwable t) {
                                XposedLog.e(TAG, "Listener callback error: " + t.getMessage());
                            }
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedLog.e(TAG, "Failed to hook loadDex: " + t.getMessage());
        }
    }

    /**
     * 注册 ClassLoader 就绪回调。
     * <p>
     * 如果 ClassLoader 已经就绪，则立即回调；
     * 否则在 loadDex 被调用后回调。
     *
     * @param listener 回调监听器
     */
    public static void addListener(@NonNull OnClassLoaderReadyListener listener) {
        sListeners.add(listener);

        // 如果已经加载过，立即回调
        if (sLoaded && sClassLoader != null) {
            try {
                listener.onClassLoaderReady(sClassLoader);
            } catch (Throwable t) {
                XposedLog.e(TAG, "Immediate listener callback error: " + t.getMessage());
            }
        }
    }

    /**
     * 获取已加载的 ClassLoader，如果尚未加载则返回 null
     */
    public static ClassLoader getClassLoader() {
        return sClassLoader;
    }

    /**
     * ClassLoader 是否已就绪
     */
    public static boolean isLoaded() {
        return sLoaded;
    }
}
