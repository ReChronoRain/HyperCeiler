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

import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * 方法 Hook 回调接口
 * <p>
 * 提供 before 和 after 两个回调方法，分别在方法执行前后调用。
 * <p>
 * 使用示例:
 * <pre>{@code
 * EzxHelpUtils.hookMethod(method, new IMethodHook() {
 *     @Override
 *     public void before(BeforeHookParam param) {
 *         // 在方法执行前修改参数或设置返回值
 *         param.getArgs()[0] = "modified";
 *         // 或直接设置返回值跳过原方法
 *         param.setResult(true);
 *     }
 *
 *     @Override
 *     public void after(AfterHookParam param) {
 *         // 在方法执行后获取或修改返回值
 *         Object result = param.getResult();
 *         param.setResult(modifiedResult);
 *     }
 * });
 * }</pre>
 *
 * @author HyperCeiler
 */
public interface IMethodHook {

    /**
     * 在目标方法执行前调用
     *
     * @param param Hook 参数，可用于获取/修改参数、设置返回值等
     */
    default void before(BeforeHookParam param) throws Throwable {
    }

    /**
     * 在目标方法执行后调用
     *
     * @param param Hook 参数，可用于获取/修改返回值等
     */
    default void after(AfterHookParam param) throws Exception {
    }

    /**
     * 在方法的 this 对象中存储额外数据
     */
    default void setObjectExtra(BeforeHookParam param, String key, Object value) {
        EzxHelpUtils.setAdditionalInstanceField(param.getThisObject(), key, value);
    }

    /**
     * 从方法的 this 对象中获取额外数据
     */
    default Object getObjectExtra(BeforeHookParam param, String key) {
        return EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), key);
    }

    /**
     * 在 after 中获取 before 中存储的数据
     */
    default Object getObjectExtra(AfterHookParam param, String key) {
        return EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), key);
    }
}
