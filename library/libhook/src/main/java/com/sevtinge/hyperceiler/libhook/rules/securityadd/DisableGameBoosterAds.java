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
package com.sevtinge.hyperceiler.libhook.rules.securityadd;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.json.JSONObject;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

public class DisableGameBoosterAds extends BaseHook {
    private Method mIsInternationalMethod;
    private Method mXunyouMethod;

    private static final ThreadLocal<Boolean> sInIsInternational = new ThreadLocal<>();

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mIsInternationalMethod = requiredMember("IsInternational", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("is_international")
                        )).singleOrNull();
                return methodData;
            }
        });
        mXunyouMethod = requiredMember("Xunyou", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("gt_xunyou_net_privacy_alter_not_show")
                        )).singleOrNull();
                return methodData;
            }
        });
        return true;
    }

    @Override
    public void init() {
        findAndChainMethod(JSONObject.class, "put",
            String.class, boolean.class,
            (XposedInterface.Hooker) chain -> {
                if (Boolean.TRUE.equals(sInIsInternational.get())) {
                    chain.getArgs().set(1, true);
                }
                return chain.proceed();
            }
        );

        chain(mIsInternationalMethod, chain -> {
            sInIsInternational.set(true);
            try {
                return chain.proceed();
            } finally {
                sInIsInternational.remove();
            }
        });

        chain(mXunyouMethod, chain -> null);
    }
}
