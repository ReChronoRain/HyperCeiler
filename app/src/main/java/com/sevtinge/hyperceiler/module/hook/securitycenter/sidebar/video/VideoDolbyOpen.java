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
package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.video;

import com.github.kyuubiran.ezxhelper.HookFactory;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

public class VideoDolbyOpen extends BaseHook {
    @Override
    public void init() {
        // try {
        //     findClassIfExists("com.miui.gamebooster.service.DockWindowManagerService").getDeclaredMethod("N");
        //     findAndHookMethod("com.miui.gamebooster.service.DockWindowManagerService", "N", new MethodHook() {
        //         @Override
        //         protected void before(MethodHookParam param) {
        //             logI("Hook N");
        //             param.setResult(null);
        //         }
        //     });
        // } catch (NoSuchMethodException e) {
        //     logI("Don't Find DockWindowManagerService$N");
        // }

        // 查找类
        // ClassData data = DexKit.INSTANCE.getDexKitBridge().findClass(FindClass.create()
        //     .searchPackages("com.miui.gamebooster.service")
        //     .matcher(ClassMatcher.create()
        //         .className("com.miui.gamebooster.service.DockWindowManagerService")
        //     )
        // ).singleOrThrow(() -> new IllegalStateException("VideoDolbyOpen: No class found ClassData"));
        // // 类加入列表
        // List<ClassData> list = Collections.singletonList(data);

        // 查找方法
        Method method = DexKit.findMember("Dolby", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("checkMiGamePermission error"))
                                .usingStrings("dolby")
                        )).singleOrNull();
                return methodData;
            }
        });
        HookFactory.createMethodHook(method, hookFactory -> hookFactory.before(
                methodHookParam -> methodHookParam.setResult(null)
        ));
    }
}
