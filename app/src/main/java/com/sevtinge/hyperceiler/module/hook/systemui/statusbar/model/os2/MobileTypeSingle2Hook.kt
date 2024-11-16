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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/

package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.os2

import android.graphics.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.mOperatorConfig
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.miuiMobileIconBinder
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.bold
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.fontSize
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.getLocation
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.leftMargin
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.rightMargin
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.verticalOffset
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import java.lang.reflect.*

object MobileTypeSingle2Hook : BaseHook() {
    var method: Method? = null
    var method2: Method? = null
    override fun init() {
        // by customiuizer
        mOperatorConfig.constructors[0].createHook {
            after {
                // 启用系统的网络类型单独显示
                // 系统的单独显示只有一个大 5G
                it.thisObject.setObjectField("showMobileDataTypeSingle", true)
            }
        }

        miuiMobileIconBinder.methodFinder().filterByName("bind").single()
            .createHook {
                after {
                    // 获取布局
                    val getView = it.args[0] as ViewGroup
                    if ("mobile" == getView.getObjectFieldAs<String>("slot")) {
                        // 大 5G 的 View
                        val textView: TextView =
                            getView.findViewById(
                                getView.resources.getIdentifier("mobile_type_single", "id", "com.android.systemui")
                            )
                        /*val leftInOut: View =
                            getView.findViewById(
                                getView.resources.getIdentifier("mobile_left_mobile_inout", "id", "com.android.systemui")
                            )
                        val rightInOut: View =
                            getView.findViewById(
                                getView.resources.getIdentifier("mobile_right_mobile_inout", "id", "com.android.systemui")
                            )*/
                        val layout = textView.parent as LinearLayout
                        val getView2: ViewGroup =
                            getView.findViewById(
                                getView.resources.getIdentifier("mobile_container_left", "id", "com.android.systemui")
                            )

                        if (!getLocation) {
                            layout.removeView(textView)
                            layout.addView(textView)
                        }
                        if (fontSize != 27) {
                            textView.textSize = fontSize * 0.5f
                        }
                        if (bold) {
                            textView.typeface = Typeface.DEFAULT_BOLD
                        }
                        val marginLeft =
                            dp2px(leftMargin * 0.5f)
                        val marginRight =
                            dp2px(rightMargin * 0.5f)
                        var topMargin = 0
                        if (verticalOffset != 8) {
                            val marginTop =
                                dp2px((verticalOffset - 8) * 0.5f)
                            topMargin = marginTop
                        }
                        textView.setPadding(marginLeft, topMargin, marginRight, 0)

                        // 整理布局，删除多余元素
                        layout.removeView(getView2)
                        val layout2 = FrameLayout(getView.context)
                        layout.addView(layout2)
                        layout2.visibility = View.GONE
                        layout2.addView(getView2)
                    }
                }
            }
    }
}