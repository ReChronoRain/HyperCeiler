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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model

import android.graphics.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
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
import java.util.ArrayList

object MobileTypeSingle2Hook : BaseHook() {
    private val DarkIconDispatcherClass: Class<*> by lazy {
        loadClass("com.android.systemui.plugins.DarkIconDispatcher", lpparam.classLoader)
    }
    var method: Method? = null
    var method2: Method? = null
    private var mobileId = -1
    private var get0: Float = 0.0f
    private var get1: Int = 0
    private var get2: Int = 0
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

        try {
            method = DarkIconDispatcherClass.getMethod("isInAreas", MutableCollection::class.java, View::class.java)
            try {
                method2 = DarkIconDispatcherClass.getMethod("getTint", MutableCollection::class.java, View::class.java, Integer.TYPE)
            } catch (unused: Throwable) {
                logE(TAG, lpparam.packageName, "DarkIconDispatcher.isInArea not found")
                if (method != null) {
                    return
                }
                return
            }
        } catch (unused2: Throwable) {
            method = null
        }
        if (method == null || method2 == null) {
            return
        }

        findAndHookMethod("com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView", "onDarkChanged", ArrayList::class.java, Float::class.java, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean::class.java, object : MethodHook() {
            override fun after(it: MethodHookParam) {
                if ("mobile" == it.thisObject.getObjectFieldAs<String>("slot")) {
                    get0 =  it.args[1] as Float
                    get1 = it.args[3] as Int
                    get2 = it.args[4] as Int
                    val num = it.args[2] as Int
                    val getBoolean = it.args[5] as Boolean
                    val getView = it.thisObject as ViewGroup
                    if (mobileId < 1) {
                        mobileId = getView.resources.getIdentifier("mobile_type_single", "id", "com.android.systemui")
                    }
                    val textView: TextView = getView.findViewById(mobileId)
                    if (getBoolean) {
                        method2?.invoke(null, it.args[0], textView, num)?.let { it1 ->
                            textView.setTextColor(it1.hashCode())
                        }
                        return
                    }
                    val getBoolean2 = method?.invoke(null, it.args[0], textView)?.let { it1 ->
                        textView.setTextColor(num)
                    } as Boolean
                    if (getBoolean2) {
                        get0 = 0.0f
                    }
                    if (get0 > 0.0f) {
                        get1 = get2
                    }
                    textView.setTextColor(get1)
                    return
                }
            }
        })

        /*modernStatusBarViewClass.methodFinder()
            .filterByName("onDarkChanged")
            .first().createAfterHook {
                XposedLogUtils.logD(TAG, lpparam.packageName, "hook onDarkChanged after")
                if ("mobile" == it.thisObject.getObjectFieldAs<String>("slot")) {
                    XposedLogUtils.logD(TAG, lpparam.packageName, "hook onDarkChanged y")
                    get0 =  it.args[1] as Float
                    get1 = it.args[3] as Int
                    get2 = it.args[4] as Int
                    val num = it.args[2] as Int
                    val getBoolean = it.args[5] as Boolean
                    val getView = it.thisObject as ViewGroup
                    if (mobileId < 1) {
                        mobileId = getView.resources.getIdentifier("mobile_type_single", "id", "com.android.systemui")
                    }
                    XposedLogUtils.logD(TAG, lpparam.packageName, "mobileId $mobileId")
                    val textView: TextView = getView.findViewById(mobileId)
                    if (getBoolean) {
                        method2?.invoke(null, it.args[0], textView, num)
                            ?.let { it1 -> textView.setTextColor(it1.hashCode()) }
                        return@createAfterHook
                    }
                    val getBoolean2 = method?.invoke(null, it.args[0], textView)
                        ?.let { it1 -> textView.setTextColor(it1.hashCode()) } as Boolean
                    if (getBoolean2) {
                        get0 = 0.0f
                    }
                    if (get0 > 0.0f) {
                        get1 = get2
                    }
                    XposedLogUtils.logD(TAG, lpparam.packageName, "get1 $get1")
                    textView.setTextColor(get1)
                    return@createAfterHook
                }
            }*/
    }
}