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

package com.sevtinge.hyperceiler.libhook.rules.guardprovider;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 禁用环境检查（系统安全组件 / com.miui.guardprovider）。
 * <p>
 * 3.1.4 起两处检测的写法都变了：
 * <ul>
 *   <li>su 路径与 {@code ro.build.tags == test-keys} 的检测被整体搬进了一个类的静态
 *       初始化块，检测结果只写进该类的静态 boolean 字段，没有留下任何独立方法，
 *       所以旧锚点 {@code "/system/bin/" + boolean 返回值} 一个都匹配不到；</li>
 *   <li>“已安装 root 管理器”的检测仍是返回 boolean 的静态方法，特征字符串为
 *       {@code "root manager found: "}。</li>
 * </ul>
 * 这里分别处理：静态标志位在初始化后直接改写为 false，root 管理器检测直接返回 false。
 */
public class DisableRootedCheck extends BaseHook {

    /** 存放环境检查结果静态标志位的类（类里只有一个静态 boolean）。 */
    private Class<?> mRootFlagClass;

    /** 检测 Magisk 等 root 管理器是否已安装的方法。 */
    private Method mRootManagerCheckMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        // 检测命中时唯一会打的一句日志，整包只此一处
        mRootFlagClass = optionalMember("RootFlagClass", bridge -> bridge.findClass(FindClass.create()
            .matcher(ClassMatcher.create()
                .usingStrings("Current device is rooted")
            )).singleOrNull());

        mRootManagerCheckMethod = optionalMember("RootManagerCheck", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("root manager found: ")
                .returnType(boolean.class)
            )).singleOrNull());

        if (mRootFlagClass == null && mRootManagerCheckMethod == null) {
            XposedLog.w(TAG, getPackageName(), "Both root check anchors are missing, skip");
            return false;
        }
        return true;
    }

    @Override
    public void init() {
        if (mRootFlagClass != null) {
            clearRootFlags(mRootFlagClass);
        }
        if (mRootManagerCheckMethod != null) {
            XposedLog.d(TAG, getPackageName(), "Hooking root manager check: " + mRootManagerCheckMethod);
            hookMethod(mRootManagerCheckMethod, returnConstant(false));
        }
    }

    /**
     * 把类里所有静态 boolean 标志位写成 false。
     * <p>
     * 写静态字段会先触发该类的 {@code <clinit>}（检测逻辑本身就在里面），
     * 检测结果落下后紧接着被这里的 false 覆盖，之后任何读取都只会拿到 false。
     */
    private void clearRootFlags(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != boolean.class) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.setBoolean(null, false);
                XposedLog.d(TAG, getPackageName(), "Root flag neutralized: " + field.getName());
            } catch (Throwable t) {
                XposedLog.w(TAG, getPackageName(), "Failed to neutralize root flag " + field.getName(), t);
            }
        }
    }
}
