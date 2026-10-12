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

package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import android.content.Context;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

/**
 * 关闭系统侧的"设备名称违规词校验"。
 *
 * <p>HyperOS 里热点名称被强制改回机型名（蓝牙名、P2P 名同理）的真正入口是
 * {@code com.android.server.wifi.Utils#checkDeviceNameIsIllegalSync}：
 * 设置页开热点时会把 {@code SystemLastCallSoftAp} 置 1，接着 AP 启动流程
 * （{@code SoftApManager} → {@code WifiOptimizationImpl#checkApSsidIsIllegalSync}）调用它，
 * 只要返回 true 就会把 SSID 覆写为 {@code ro.product.marketname}。</p>
 *
 * <p>该方法位于 Wi-Fi 模块的类加载器里（APEX 的 {@code service-wifi.jar} 与 MIUI 的
 * {@code /system_ext/framework/miui-wifi-service.jar} 被 {@code SystemServer} 合并进同一个
 * {@code PathClassLoader}），所以先用 {@code SystemServiceManager.loadClassFromLoader} 拿到
 * 该加载器，再挂上目标方法。Android 15/16 都走这条路径。</p>
 */
public class AntiQues extends BaseHook {
    private static final String TAG = "AntiQues";
    /** Wi-Fi 服务类；它是模块类加载器被创建时的入口类。 */
    private static final String WIFI_SERVICE_CLASS = "com.android.server.wifi.WifiService";
    /** 真正的判定入口，定义在 MIUI 的 miui-wifi-service.jar 中。 */
    private static final String WIFI_UTILS_CLASS = "com.android.server.wifi.Utils";
    private static final String WIFI_UTILS_METHOD = "checkDeviceNameIsIllegalSync";

    private XposedInterface.HookHandle mLoaderHook;

    @Override
    public void init() {
        mLoaderHook = findAndHookMethod("com.android.server.SystemServiceManager",
            "loadClassFromLoader", String.class, ClassLoader.class, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    if (!WIFI_SERVICE_CLASS.equals(param.getArgs()[0])) return;
                    hookWifiUtils((ClassLoader) param.getArgs()[1]);
                }
            });
    }

    private void hookWifiUtils(ClassLoader classLoader) {
        try {
            findAndHookMethod(WIFI_UTILS_CLASS, classLoader, WIFI_UTILS_METHOD,
                Context.class, int.class, String.class, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.setResult(false);
                    }
                });
        } catch (Throwable t) {
            // 目标类尚未加载或加载器不符时保留外层 Hook，等下一次 loadClassFromLoader 再试，
            // 同时把失败原因写进日志（原实现静默吞掉异常，导致失效时无从排查）。
            XposedLog.w(TAG, "Hook " + WIFI_UTILS_CLASS + "#" + WIFI_UTILS_METHOD + " failed, will retry: " + t);
            return;
        }
        XposedLog.d(TAG, "Hooked " + WIFI_UTILS_CLASS + "#" + WIFI_UTILS_METHOD);
        if (mLoaderHook != null) {
            mLoaderHook.unhook();
            mLoaderHook = null;
        }
    }
}
