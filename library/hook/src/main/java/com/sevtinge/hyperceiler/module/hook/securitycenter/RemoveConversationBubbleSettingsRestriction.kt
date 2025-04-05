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
package com.sevtinge.hyperceiler.module.hook.securitycenter

import android.annotation.SuppressLint
import android.content.Context
import android.util.ArrayMap
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.getObjectField
import org.lsposed.hiddenapibypass.HiddenApiBypass

class RemoveConversationBubbleSettingsRestriction : BaseHook() {
    @SuppressLint("PrivateApi")
    override fun init() {
        loadClass("com.miui.bubbles.settings.BubblesSettings").methodFinder()
            .filterByName("getDefaultBubbles")
            .single().createHook {
                before { param ->
                    val classBubbleApp = loadClass("com.miui.bubbles.settings.BubbleApp")
                    val arrayMap = ArrayMap<String, Any>()
                    val mContext =
                        param.thisObject.getObjectField("mContext") as Context
                    val mCurrentUserId =
                        param.thisObject.getObjectField("mCurrentUserId") as Int
                    val freeformSuggestionList = HiddenApiBypass.invoke(
                        Class.forName("android.util.MiuiMultiWindowUtils"),
                        null,
                        "getFreeformSuggestionList",
                        mContext
                    ) as List<*>
                    if (freeformSuggestionList.isNotEmpty()) {
                        for (str in freeformSuggestionList) {
                            val bubbleApp = classBubbleApp.getConstructor(
                                String::class.java, Int::class.java
                            ).newInstance(str, mCurrentUserId)
                            classBubbleApp.getMethod("setChecked", Boolean::class.java)
                                .invoke(bubbleApp, true)
                            arrayMap[str as String] = bubbleApp
                        }
                    }
                    param.result = arrayMap
                }
            }
    }
}
