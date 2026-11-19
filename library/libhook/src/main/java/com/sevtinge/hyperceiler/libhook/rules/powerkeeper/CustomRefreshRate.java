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
package com.sevtinge.hyperceiler.libhook.rules.powerkeeper;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class CustomRefreshRate extends BaseHook {
    private Method mFucSwitchMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        /*
         * 改动日志 (2026.11.06)：
         * 原查询（usingStrings("custom_mode_switch", "fucSwitch") + returnType(boolean)）在三个版本里全部 0 命中：
         * PowerKeeper 里不存在“返回 boolean 且同时引用这两个串”的方法，两个串都是 JSON 键名/日志文本，
         * 因此 requiredMember 报错，整条规则在 OS3 与 OS4 上都是死的（设备日志里表现为 DexKit 缓存 NPE）。
         * 根据 Hook 体（把 mIsCustomFpsSwitch 写成 "true"）反推真实目标：
         *   字段 Lcom/miui/powerkeeper/statemachine/DisplayFrameSetting;.mIsCustomFpsSwitch 是 static String，
         *   三版反汇编里全 APK 只有一个写手（sput-object）：DisplayFrameSetting#parseCustomModeSwitchFromDb(String)V，
         *   该方法解析云端配置后把结果 sput 到这个字段，再 Settings$System.putString("custom_mode_switch", 字段)。
         * 所以目标就是这个方法，而不是某个“返回 boolean 的 fucSwitch”。它在三个版本里都没被混淆，
         * 故主查询用“类名 + 语义方法名 + String 单参 + void”（三版均唯一）；
         * 万一以后改名，兜底用“单串 custom_mode_switch + String 单参 + void”（单串时 AND/OR 语义等价，
         * 三版也各自唯一）。两者都失败则返回 false，只跳过本规则，不把异常泄到其他规则。
         */
        mFucSwitchMethod = optionalMember("FucSwitchByName", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass("com.miui.powerkeeper.statemachine.DisplayFrameSetting")
                                .name("parseCustomModeSwitchFromDb")
                                .paramTypes("java.lang.String")
                                .returnType(void.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        if (mFucSwitchMethod == null) {
            mFucSwitchMethod = optionalMember("FucSwitchByString", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    MethodData methodData = bridge.findMethod(FindMethod.create()
                            .matcher(MethodMatcher.create()
                                    .usingStrings("custom_mode_switch")
                                    .paramTypes("java.lang.String")
                                    .returnType(void.class)
                            )).singleOrNull();
                    return methodData;
                }
            });
        }
        return mFucSwitchMethod != null;
    }

    @Override
    public void init() {
        hookMethod(mFucSwitchMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                // mIsCustomFpsSwitch 是 static String（不是 boolean），字段存的是 "true"/"false" 文本；
                // setObjectField 对静态字段同样有效（Field#set 会忽略传入的实例）。
                // 只在 before 里预置：若云端/本地配置为空，PowerKeeper 不会覆写，预置值保留；
                // 若配置非空，小米自己的值仍会赢，不改变已有行为。
                com.sevtinge.hyperceiler.libhook.base.BaseHook.setObjectField(param.getThisObject(), "mIsCustomFpsSwitch", "true");
            }
        });
    }
}
