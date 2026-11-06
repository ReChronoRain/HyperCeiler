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
package com.sevtinge.hyperceiler.libhook.rules.misettings

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object CustomRefreshRate : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        resultMethod
        return true
    }
    private val resultMethod by lazy<Method> {
        requiredMember("CustomRefreshRate") {
            it.findMethod {
                matcher {
                    addEqString("btn_preferce_category")
                    // 改动日志 (2026.11.06)：
                    // btn_preferce_category 在 NewRefreshRateFragment 里被两个方法同时引用：
                    //   NewRefreshRateFragment#A(Z)V（OS3）/ #C(Z)V（OS4.0.0.44、OS4.0.5.0）——真正添加自定义刷新率选项的方法
                    //   NewRefreshRateFragment#onCreatePreferences(Bundle, String)V
                    // 只按字符串匹配会得到 2 个命中，.single() 抛 NonUniqueResultException，
                    // initDexKit 失败后整条规则被跳过（设备日志里就是 unique result: 2）。
                    // Hook 体是 args[0] = true，只有 boolean 入参、void 返回的那个才是类型正确的目标，
                    // 所以按入参个数/类型与返回类型收紧。已在 OS3.0.319、OS4.0.0.44、OS4.0.5.0 三版
                    // 反汇编上离线复算：三版均收敛为唯一命中（分别是 A(Z)V、C(Z)V、C(Z)V）。
                    paramCount = 1
                    paramTypes("boolean")
                    returnType = "void"
                }
            }.single()
        }
    }
    override fun init() {
        val resultClass = findClass("com.xiaomi.misettings.display.RefreshRate.RefreshRateActivity")

        resultMethod.createHook {
            before {
                it.args[0] = true
            }
        }

        resultClass.declaredFields.first { field ->
            Modifier.isFinal(field.modifiers) && Modifier.isStatic(field.modifiers)
        }.apply { isAccessible = true }.set(null, true)
    }
}

