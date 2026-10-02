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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import io.github.kyuubiran.ezxhelper.xposed.common.HookParam

/**
 * 恢复新版安全服务中被隐藏的游戏“二倍速”启动功能。
 *
 * 把相关系统属性写成真实 "true"，让游戏加速/性能服务读到后开启加速。
 */
object UnlockGameSpeed : BaseHook() {
    private const val TARGET_CLASS = "android.os.SystemProperties"

    /** 需要写成真实 "true" 的属性。 */
    private val writeProps = mapOf(
        "debug.game.video.support" to "true",
        "debug.game.video.speed" to "true"
    )

    override fun init() {
        // 模块加载时写一次
        writeProps()

        // 游戏加速服务初始化后再写一次，防止属性被复位
        try {
            findAndHookMethod(
                "com.miui.gamebooster.service.GameBoosterService",
                "onCreate",
                object : IMethodHook {
                    override fun after(param: HookParam) {
                        XposedLog.d(TAG, lpparam.packageName, "GameBoosterService.onCreate, write props")
                        writeProps()
                    }
                })
        } catch (t: Throwable) {
            XposedLog.w(TAG, lpparam.packageName, "hook GameBoosterService.onCreate failed", t)
        }
    }

    private fun writeProps() {
        val spClass = findClassIfExists(TARGET_CLASS) ?: return
        writeProps.forEach { (key, value) ->
            try {
                callStaticMethod(spClass, "set", key, value)
                XposedLog.d(TAG, lpparam.packageName, "setProp $key = $value")
            } catch (t: Throwable) {
                XposedLog.w(TAG, lpparam.packageName, "setProp $key failed", t)
            }
        }
    }
}
