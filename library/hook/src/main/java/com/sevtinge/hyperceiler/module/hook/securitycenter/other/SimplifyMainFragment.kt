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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/

package com.sevtinge.hyperceiler.module.hook.securitycenter.other

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object SimplifyMainFragment : BaseHook() {
    override fun init() {
        loadClass("com.miui.common.card.CardViewRvAdapter").methodFinder()
            .filterByName("addAll")
            .filterByParamTypes(List::class.java)
            .single()
            .createHook {
                before { param ->
                    val oldModelList = param.args[0] as List<*>
                    val removedModel = listOf(
                        // 功能推荐
                        "com.miui.common.card.models.FuncListBannerCardModel",
                        // 常用功能
                        // "com.miui.common.card.models.CommonlyUsedFunctionCardModel",
                        // 大家都在用
                        "com.miui.common.card.models.PopularActionCardModel"
                    )

                    param.args[0] = oldModelList.filterNot { model ->
                        removedModel.contains(model!!.javaClass.name)
                    }
                }
            }
    }
}
