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

package com.sevtinge.hyperceiler.libhook.rules.various.clipboard;

import android.content.ClipData;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

/**
 * 解除输入法剪贴板限制（运行在输入法进程中）。
 * <p>
 * 功能：
 * <ul>
 *   <li>提升单条剪贴板文本长度上限（默认 30000 → 500000）</li>
 *   <li>绕过 processSingleItemOfClipData 中的 substring 截断</li>
 * </ul>
 * <p>
 * 保留 72 小时过期清理机制，避免剪贴板条目无限积累导致卡顿。
 * <p>
 * 注意：不再将 MAX_CLIP_CONTENT_SIZE 设为 Integer.MAX_VALUE，
 * 以避免剪贴板 JSON 无限膨胀导致输入法卡顿。
 */
public class ClipboardUnlock extends BaseHook {
    private static final String TAG = "ClipboardUnlock";

    /**
     * 提升后的单条文本长度上限。
     * 原始值为 30000（Android 9+）或 5000。
     * 设为 500000（500KB），足以容纳绝大多数文本，同时避免无限膨胀。
     */
    private static final int UNLOCKED_CLIP_CONTENT_SIZE = 500_000;

    @Override
    public void init() {
        InputMethodDexHelper.addListener(this::applyHooks);
    }

    private void applyHooks(ClassLoader classLoader) {
        hookMaxClipContentSize(classLoader);
        hookProcessSingleItem(classLoader);
    }

    /**
     * 提升 MAX_CLIP_CONTENT_SIZE 到合理上限。
     */
    private void hookMaxClipContentSize(ClassLoader classLoader) {
        try {
            Class<?> mgrCls = EzxHelpUtils.findClass(
                "com.miui.inputmethod.MiuiClipboardManager", classLoader);
            EzxHelpUtils.setStaticIntField(mgrCls, "MAX_CLIP_CONTENT_SIZE",
                UNLOCKED_CLIP_CONTENT_SIZE);
        } catch (Throwable t) {
            XposedLog.e(TAG, "Failed to set MAX_CLIP_CONTENT_SIZE: " + t.getMessage());
        }
    }

    /**
     * 绕过 processSingleItemOfClipData 中对超长文本的截断。
     * <p>
     * 原始逻辑：当 string.length() > MAX_CLIP_CONTENT_SIZE 时，
     * 调用 string.substring(0, MAX_CLIP_CONTENT_SIZE)。
     * <p>
     * 这里直接 hook processSingleItemOfClipData，在进入前临时将
     * MAX_CLIP_CONTENT_SIZE 设为 Integer.MAX_VALUE，退出后恢复。
     * 比 hook String.substring 更精准、更安全。
     */
    private void hookProcessSingleItem(ClassLoader classLoader) {
        try {
            EzxHelpUtils.findAndHookMethod(
                "com.miui.inputmethod.MiuiClipboardManager", classLoader,
                "processSingleItemOfClipData",
                ClipData.class, String.class,
                new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        try {
                            Class<?> mgrCls = param.getThisObject().getClass();
                            EzxHelpUtils.setStaticIntField(mgrCls,
                                "MAX_CLIP_CONTENT_SIZE", Integer.MAX_VALUE);
                        } catch (Throwable t) {
                            XposedLog.e(TAG, "processSingleItem before: " + t.getMessage());
                        }
                    }

                    @Override
                    public void after(HookParam param) {
                        try {
                            Class<?> mgrCls = param.getThisObject().getClass();
                            EzxHelpUtils.setStaticIntField(mgrCls,
                                "MAX_CLIP_CONTENT_SIZE", UNLOCKED_CLIP_CONTENT_SIZE);
                        } catch (Throwable t) {
                            XposedLog.e(TAG, "processSingleItem after: " + t.getMessage());
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedLog.e(TAG, "Failed to hook processSingleItemOfClipData: " + t.getMessage());
        }
    }
}
