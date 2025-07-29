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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.securitycenter;

import static com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.setProp;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;

public class EnableGameSpeed extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {

        Method method1 = DexKit.findMember("PropVoidData", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("android.os.SystemProperties", "set", "SystemPropertiesUtils", "SystemPropertiesUtils getInt:")
                                .returnType(void.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method1, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                String param0 = (String) param.args[0];
                if (Objects.equals(param0, "debug.game.video.speed")) param.args[1] = "true";
            }
        });

        Method method2 = DexKit.findMember("PropBooleanData", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("android.os.SystemProperties", "getBoolean", "SystemPropertiesUtils", "SystemPropertiesUtils getInt:")
                                .returnType(boolean.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method2, new MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                String param0 = (String) param.args[0];
                if (Objects.equals(param0, "debug.game.video.support")) param.setResult(true);
            }
        });

        Method method3 = DexKit.findMember("IsSupport", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("debug.game.video.support")
                                .returnType(boolean.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method3, new MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        Method method4 = DexKit.findMember("OpenGameBooster", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("pref_open_game_booster")
                                .returnType(boolean.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method4, new MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        Method method5 = DexKit.findMember("Boot", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("debug.game.video.boot")
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method5, new MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                mSetProp();
                param.setResult(null);
            }
        });

        findAndHookMethod("com.miui.gamebooster.service.GameBoosterService",
            "onCreate", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    mSetProp();
                }
            }
        );
    }

    public void mSetProp() {
        setProp("debug.game.video.boot", "true");
        setProp("debug.game.video.speed", "true");
    }
}
