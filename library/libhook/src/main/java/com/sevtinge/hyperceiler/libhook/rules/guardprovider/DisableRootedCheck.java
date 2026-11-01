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

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Method;

/**
 * 禁用环境检查（系统安全组件 / com.miui.guardprovider）。
 * <p>
 * 3.1.4 起旧锚点 {@code "/system/bin/" + boolean 返回值} 命中数为 0：包内仅剩的
 * su / test-keys 检测属于 miuix AutoDensity，与 guardprovider 自身的环境检查无关；
 * guardprovider 真正在用的只剩「是否安装了 root 管理器」这一处，它是返回 boolean
 * 的静态方法，特征字符串 {@code "root manager found: "}（整包唯一）。
 * <p>
 * 需要注意 AutoDensity 那处的检测结果保存在 {@code static final boolean}，
 * 反射写入会抛 {@code IllegalAccessException}，{@code <clinit>} 又不被反射暴露，
 * 因此这里不处理它。
 */
public class DisableRootedCheck extends BaseHook {

    /** 检测 Magisk 等 root 管理器是否已安装的方法。 */
    private Method mRootManagerCheckMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mRootManagerCheckMethod = optionalMember("RootManagerCheck", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("root manager found: ")
                .returnType(boolean.class)
            )).singleOrNull());

        if (mRootManagerCheckMethod == null) {
            XposedLog.w(TAG, getPackageName(), "root manager check anchor is missing, skip");
            return false;
        }
        return true;
    }

    @Override
    public void init() {
        XposedLog.d(TAG, getPackageName(), "Hooking root manager check: " + mRootManagerCheckMethod);
        hookMethod(mRootManagerCheckMethod, returnConstant(false));
    }
}
