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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.callback;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.DeviceEffectMemory.EffectState;

/**
 * 音效状态控制接口
 *
 * @author 焕晨HChen
 */
public interface IControlForSystem {

    /**
     * 初始化控制器
     */
    void init();

    /**
     * 设置 Context（可选实现）
     */
    default void setContext(Context context) {
        // 默认空实现
    }

    /**
     * 更新上一次的音效状态，用于后续恢复
     */
    void updateLastEffectState();

    /**
     * 将所有音效设置为关闭状态
     *
     * @param context 上下文
     */
    void setEffectToNone(Context context);

    /**
     * 恢复之前保存的音效状态
     */
    void resetAudioEffect();

    /**
     * 应用指定的音效状态
     *
     * @param state 音效状态
     */
    default void applyEffectState(EffectState state) {
        // 默认空实现，子类可重写
    }

    /**
     * 获取当前音效状态
     *
     * @return 当前音效状态
     */
    default EffectState getCurrentEffectState() {
        return null;
    }

    /**
     * 输出当前音效状态（调试用）
     */
    default void dumpAudioEffectState() {
        // 默认空实现
    }
}
