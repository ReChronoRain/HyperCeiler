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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.securitycenter.other;

import static de.robv.android.xposed.XposedHelpers.callMethod;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

public class SkipCountDownLimit extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method = (Method) DexKit.findMember("SkipCountDownLimitFragment", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass("com.miui.permcenter.privacymanager.InterceptBaseFragment")
                                .usingNumbers(-1, 0)
                        )).singleOrNull();
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });
        if (mPrefsMap.getBoolean("security_center_skip_count_down_limit_direct")){
            findAndHookMethod("com.miui.permcenter.privacymanager.InterceptBaseFragment", "onInflateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    callMethod(param.thisObject, method.getName(), true);
                }
            });
        } else {
            findAndHookMethod("com.miui.permcenter.privacymanager.model.InterceptBaseActivity", "onCreate", Bundle.class, new MethodHook(){
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    Bundle bundle = (Bundle) param.args[0];
                    if (bundle == null) {
                        bundle = new Bundle();
                        param.args[0] = bundle;
                    }
                    bundle.putInt("KET_STEP_COUNT", 0);
                    bundle.putBoolean("KEY_ALLOW_ENABLE", true);
                }
            });
        }
    }
}
