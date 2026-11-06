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
package com.sevtinge.hyperceiler.libhook.rules.thememanager

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object UnlockAIWallPaper : BaseHook() {
    override fun useDexKit() = true

    private var deviceChecks: List<Method> = emptyList()

    override fun initDexKit(): Boolean {
        // 改动日志 (2026.11.06)：
        // 该查询（含 enable_video_depth 的类里，所有调用 Set#contains 且返回 boolean 的方法）在三个版本里
        // 都固定得到 2 个方法，.single() 直接抛 NonUniqueResultException，整条规则被跳过：
        //   OS3.0.319      fti#d2ok()Z、fti#eqxt()Z
        //   OS4.0.0.44     MiuiUtils#t()Z（白名单 ai_pet_device）、MiuiUtils#wvg()Z（白名单 ai_record_device）
        //   OS4.0.5.0      MiuiUtils#t()Z、MiuiUtils#wvg()Z
        // 这两个方法在结构上完全同构（拿机型名去 Set 里查白名单），只是名字漂移（OS3 已被混淆），
        // 它们分别控制 AI 宠物壁纸与 AI 录屏壁纸的设备限定，调用点分别是 ThemeProvider#call 与
        // AIActionProvider / AppContextManager / ThemeAndWallSettingFragmentDelegate，都属于本功能范围。
        // 原意就是“把所有设备限定判定改成允许”，所以改成列表全量 Hook（三版都是 2 个，数量也一致）。
        // 另外这里用 optionalMemberList：查不到时返回空列表并跳过规则，而不是抛异常带偏其他规则。
        deviceChecks = optionalMemberList("wallpaper") { dexKitBridge ->
            dexKitBridge.findClass {
                matcher {
                    addUsingString("enable_video_depth", StringMatchType.Contains)
                }
            }.findMethod {
                matcher {
                    addInvoke("Ljava/util/Set;->contains(Ljava/lang/Object;)Z")
                    returnType = "boolean"
                }
            }
        }
        return deviceChecks.isNotEmpty()
    }

    override fun init() {
        deviceChecks.forEach { method ->
            method.createHook {
                returnConstant(true)
            }
        }
    }
}

