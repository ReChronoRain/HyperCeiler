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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.rules.notes

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object UnlockAI : BaseHook() {

    private val mode by lazy {
        mPrefsMap.getStringAsInt("notes_unlock_ai_mode", 0)
    }

    override fun init() {
        runCatching {
            loadClass("com.miui.common.tool.RomUtils").methodFinder()
                .filterByName("isSupportAi")
                .first()
                .createHook {
                    // 老的 AI 功能
                    returnConstant(mode == 1)
                }
        }.onFailure {
            logE(TAG, lpparam.packageName, "is not support old ai")
        }

        loadClass("com.miui.notes.ai.utils.RomUtils").methodFinder()
            .filterByName("isSupportAiText")
            .first()
            .createHook {
                // 新的 AI 功能
                returnConstant(mode == 2)
            }
    }
}
