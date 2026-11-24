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
package com.sevtinge.hyperceiler.libhook.rules.guardprovider;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IReplaceHook;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Method;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class DisableUploadAppListNew extends BaseHook {
    private static final String MI_ENGINE = "com.miui.guardprovider.engine.mi.MiEngineNG";

    private Method mAntiDefraudAppManagerMethod;
    private Method mAutoUpdateMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        if (isMoreHyperOSVersion(4f)) {
            mAntiDefraudAppManagerMethod = requiredMember("UpdateAppListOS4", bridge ->
                bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .usingStrings("AntiDefraudAppManager", "updateAllDetectApps error")
                    .addCaller(MethodMatcher.create().declaredClass(MI_ENGINE))
                    .paramTypes(Context.class)
                    .returnType(void.class))).singleOrNull());

            mAutoUpdateMethod = requiredMember("AutoUpdateOS4", bridge ->
                bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .declaredClass(MI_ENGINE)
                    .addInvoke(MethodMatcher.create()
                        .declaredClass(mAntiDefraudAppManagerMethod.getDeclaringClass())
                        .name(mAntiDefraudAppManagerMethod.getName())
                        .paramTypes(Context.class)
                        .returnType(void.class))
                    .paramCount(1)
                    .returnType(void.class))).singleOrNull());
            return true;
        }

        mAntiDefraudAppManagerMethod = requiredMember("AntiDefraudAppManager", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("AntiDefraudAppManager", "https://flash.sec.miui.com/detect/app")
            )).singleOrNull());
        return true;
    }

    @Override
    public void init() {
        if (mAutoUpdateMethod != null) {
            hookAutomaticUpload();
            return;
        }

        com.sevtinge.hyperceiler.libhook.base.BaseHook.hookMethod(mAntiDefraudAppManagerMethod, new IReplaceHook() {
            @Override
            public Object replace(HookParam param) {
                return null;
            }
        });
    }

    private void hookAutomaticUpload() {
        ThreadLocal<Boolean> duringAutoUpdate = new ThreadLocal<>();
        // The same app-list method also handles explicit scan requests.
        var uploadHook = chain(mAntiDefraudAppManagerMethod, invocation ->
            Boolean.TRUE.equals(duringAutoUpdate.get()) ? null : invocation.proceed());
        if (uploadHook == null) return;

        chain(mAutoUpdateMethod, invocation -> {
            if (Boolean.TRUE.equals(duringAutoUpdate.get())) return invocation.proceed();
            duringAutoUpdate.set(true);
            try {
                return invocation.proceed();
            } finally {
                duringAutoUpdate.remove();
            }
        });
    }
}
