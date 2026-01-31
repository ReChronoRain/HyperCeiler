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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import static com.sevtinge.hyperceiler.libhook.rules.systemframework.others.AutoEffectSwitchForSystem.mEffectInfoService;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.BINDER_KEY_EFFECT_INFO;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.Set;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

/**
 * Binder 代理
 *
 * @author 焕晨HChen
 */
public class EffectBinderProxy extends BaseHook {

    private static final String TAG = "EffectBinderProxy";

    private static final Set<String> TARGET_PACKAGES = Set.of(
        "com.miui.misound",
        "com.android.systemui"
    );

    @Override
    public void init() {
        hookRegisterReceiver();
    }

    private void hookRegisterReceiver() {
        findAndHookMethod(
            "com.android.server.am.ActivityManagerService",
            "registerReceiverWithFeature",
            "android.app.IApplicationThread",
            String.class,
            String.class,
            String.class,
            "android.content.IIntentReceiver",
            IntentFilter.class,
            String.class,
            int.class,
            int.class,
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    handleRegisterReceiver(param);
                }
            }
        );
    }

    /**
     * 处理广播注册结果
     */
    private void handleRegisterReceiver(AfterHookParam param) {
        try {
            Intent intent = (Intent) param.getResult();
            if (intent == null) return;

            String callerPackage = (String) param.getArgs()[1];
            if (callerPackage == null) return;

            // 检查是否是目标包名
            if (!TARGET_PACKAGES.contains(callerPackage)) return;

            // 检查 EffectInfoService 是否已初始化
            if (mEffectInfoService == null) {
                XposedLog.w(TAG, "EffectInfoService is null, cannot inject binder for: " + callerPackage);
                return;
            }

            // 注入 Binder
            injectBinder(intent, callerPackage);
            param.setResult(intent);} catch (Exception e) {
            XposedLog.e(TAG, "handleRegisterReceiver failed", e);
        }
    }

    /**
     * 将 Binder 注入到 Intent 中
     */
    private void injectBinder(Intent intent, String callerPackage) {
        Bundle bundle = new Bundle();
        bundle.putBinder(BINDER_KEY_EFFECT_INFO, mEffectInfoService.asBinder());
        intent.putExtra(BINDER_KEY_EFFECT_INFO, bundle);

        XposedLog.d(TAG, "Binder injected for package: " + callerPackage);
    }
}
