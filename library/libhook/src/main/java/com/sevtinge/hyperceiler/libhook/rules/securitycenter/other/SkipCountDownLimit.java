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

package com.sevtinge.hyperceiler.libhook.rules.securitycenter.other;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class SkipCountDownLimit extends BaseHook {
    @Override
    public void init() {
        Method method = DexKit.findMember("SkipCountDownLimitFragment", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create().className("com.miui.permcenter.privacymanager.InterceptBaseFragment"))
                ).findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create().usingNumbers(-1, 0))
                ).singleOrNull();
                return methodData;
            }
        });
        if (mPrefsMap.getBoolean("security_center_skip_count_down_limit_direct")){
            findAndHookMethod("com.miui.permcenter.privacymanager.InterceptBaseFragment", "onInflateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    callMethod(param.getThisObject(), method.getName(), true);
                }
            });
        } else {
            findAndHookMethod("com.miui.permcenter.privacymanager.model.InterceptBaseActivity", "onCreate", Bundle.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    Bundle bundle = (Bundle) param.getArgs()[0];
                    if (bundle == null) {
                        bundle = new Bundle();
                        param.getArgs()[0] = bundle;
                    }
                    bundle.putInt("KET_STEP_COUNT", 0);
                    bundle.putBoolean("KEY_ALLOW_ENABLE", true);
                }
            });
        }
    }
}
