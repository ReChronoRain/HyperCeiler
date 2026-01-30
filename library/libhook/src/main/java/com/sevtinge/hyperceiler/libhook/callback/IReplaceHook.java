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
package com.sevtinge.hyperceiler.libhook.callback;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * 方法替换 Hook 回调接口
 * <p>
 * 用于完全替换目标方法的实现。
 * <p>
 * 使用示例:
 * <pre>{@code
 * EzxHelpUtils.hookMethod(method, new IReplaceHook() {
 *     @Override
 *     public Object replace(BeforeHookParam param) {
 *         // 获取参数
 *         String arg0 = (String) param.getArgs()[0];
 *         Object thisObject = param.getThisObject();
 *
 *         // 返回自定义结果，原方法不会执行
 *         return customResult;
 *     }
 * });
 * }</pre>
 *
 * @author HyperCeiler
 */
public interface IReplaceHook {

    /**
     * 替换目标方法的实现
     *
     * @param param Hook 参数，可用于获取参数、thisObject 等
     * @return 方法的返回值
     * @throws Throwable 可以抛出异常
     */
    Object replace(BeforeHookParam param) throws Throwable;
}
