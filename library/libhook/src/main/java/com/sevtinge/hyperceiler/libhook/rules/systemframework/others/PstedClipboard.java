/*
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class PstedClipboard extends BaseHook {

    /**
     * 真正决定要不要弹「已读取剪贴板」提示的是这个非合成（non-synthetic）方法，
     * 它的返回值就是「提示是否已展示」。
     *
     * 这里刻意不挂编译器合成的 lambda$xxx$n：lambda 的编号会因为方法体增删而漂移，
     * 合成参数的个数也会随捕获变量变化，把它们写进匹配条件就等于绑死在某一个 ROM 版本上。
     * 也不判断 Android / ROM 版本号：只按「方法名 + 返回 boolean」这一条跨版本不变的特征匹配，
     * 命中几个就挂几个，因此同一份产物在 OS3 与 OS4 上走的是同一条路径。
     */
    private static final String CLIPBOARD_SERVICE = "com.android.server.clipboard.ClipboardService";
    private static final String NOTIFY_DECIDER = "showAccessNotificationLocked";

    @Override
    public void init() {
        Class<?> service = findClassIfExists(CLIPBOARD_SERVICE);
        if (service == null) {
            return;
        }

        IMethodHook hook = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                // 返回 false：告诉调用方「没有展示提示」，正是本功能想要的结果
                param.setResult(false);
            }
        };

        List<Method> targets = new ArrayList<>();
        for (Method method : service.getDeclaredMethods()) {
            if (method.getName().equals(NOTIFY_DECIDER)
                    && method.getParameterTypes().length > 0
                    && method.getReturnType() == boolean.class) {
                targets.add(method);
            }
        }

        if (!targets.isEmpty()) {
            for (Method method : targets) {
                hookMethod(method, hook);
            }
            return;
        }

        // 兜底：万一外层方法被改名，再退回按 lambda 前缀挂，仍然不写死编号与参数表
        IMethodHook skipBody = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(null);
            }
        };
        String prefix = "lambda$" + NOTIFY_DECIDER + "$";
        for (Method method : service.getDeclaredMethods()) {
            if (method.getName().startsWith(prefix)) {
                hookMethod(method, skipBody);
            }
        }
    }
}
