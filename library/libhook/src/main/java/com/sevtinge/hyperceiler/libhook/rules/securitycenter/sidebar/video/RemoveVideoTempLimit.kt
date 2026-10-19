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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.video

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

/**
 * 移除视频工具箱画质提升温度限制
 *
 * 安全服务中的 VideoToolboxTempMonitor 每 5 秒读取一次主板温度，
 * 超过 41°C 时自动禁用「极清播放」「画质提升」等功能，
 * 低于 39°C 且持续 10 分钟后才恢复。
 * 这里让温度读取方法恒返回 0°C，等效于移除温度墙，
 * 监控逻辑永远处于正常分支，不会再触发过热禁用。
 */
object RemoveVideoTempLimit : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        readBoardTemp
        return true
    }

    // VideoToolboxTempMonitor 温度监控类中读取主板温度的方法（混淆名如 float q()）。
    // 锚点选择说明：
    // - 不能用日志 TAG "VideoToolboxTempMonitor" 找类：外层类和内部 Runnable 类
    //   都引用该字符串，findClass 结果不唯一（NonUniqueResultException: 2）
    // - 不能用 thermal 节点路径：通话工具箱（ConversationManager）的读温方法
    //   引用相同路径，同样不唯一
    // - 错误日志 "read board temp fail, node=" 全包只有该读温方法引用，唯一锚定
    private val readBoardTemp by lazy<Method> {
        requiredMember("RemoveVideoTempLimitMethod") {
            it.findMethod {
                matcher {
                    returnType = "float"
                    addUsingString("read board temp fail, node=", StringMatchType.Equals)
                }
            }.single()
        }
    }

    override fun init() {
        XposedLog.d(TAG, lpparam.packageName, "find VideoToolboxTempMonitor readBoardTemp Method is $readBoardTemp")
        readBoardTemp.createHook {
            returnConstant(0.0f)
        }
    }
}
