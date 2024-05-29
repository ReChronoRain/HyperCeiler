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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model

import android.annotation.*
import android.view.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.log.*
import de.robv.android.xposed.*
import de.robv.android.xposed.XC_MethodHook.*

@SuppressLint("StaticFieldLeak")
object MobileTypeTextCustom : BaseHook() {

    private val statusBarMobileClass by lazy {
        loadClass("com.android.systemui.statusbar.StatusBarMobileView")
    }

    private val mobileSignalControllerClass by lazy {
        loadClass("com.android.systemui.statusbar.connectivity.MobileSignalController")
    }

    private val qpt by lazy {
        mPrefsMap.getStringAsInt("system_ui_status_bar_icon_show_mobile_network_type", 0)
    }

    private val hideIndicator by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_mobile_indicator")
    }

    private val isShowPaw by lazy {
        // 显示爪爪
        mPrefsMap.getBoolean("system_ui_status_bar_icon_paw")
    }

    private var mMobileType: View? = null
    private var dataConnected: Boolean = false
    private var wifiAvailable: Boolean = false
    private var cache: Int = 0

    override fun init() {

        hookAllMethods(statusBarMobileClass, "updateState", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if ((qpt != 0) || hideIndicator || isShowPaw) {
                    getMobileType(param) // 隐藏网络类型图标及移动网络指示器
                }
            }
        })

        hookAllMethods(statusBarMobileClass, "applyMobileState", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if ((qpt != 0) || hideIndicator || isShowPaw) {
                    getMobileType(param) // 隐藏网络类型图标及移动网络指示器
                }
            }
        })

        /*findAndHookMethod(mobileSignalControllerClass, "updateTelephony", object : MethodHook() {
            override fun after(param: MethodHookParam?) {
                findAndHookMethod(mobileSignalControllerClass, "getMobileTypeName", Int::class.java, object : MethodHook() {
                    override fun after(param: MethodHookParam?) {
                        if (mPrefsMap.getBoolean("system_ui_status_bar_icon_paw") && mPrefsMap.getString("system_ui_status_bar_mobile_type_custom", "") == null) {
                            param?.result = null
                        } else {
                            mPrefsMap.getString("system_ui_status_bar_mobile_type_custom", "ERR")
                        }
                    }
                })
            }
        })*/

        mobileSignalControllerClass.methodFinder()
            .filterByName("getMobileTypeName")
            .filterByParamTypes {
                it[0] == Int::class.java
            }.single().createHook {
                after {
                    if (isShowPaw && mPrefsMap.getString("system_ui_status_bar_mobile_type_custom", "") == "") {
                        XposedLogUtils.logD("dataConnected $dataConnected")
                        XposedLogUtils.logD("wifiAvailable $wifiAvailable")
                        XposedLogUtils.logD("cache $cache")
                        if ((qpt == 0 && (!dataConnected || wifiAvailable) && cache != 0) || (qpt == 2 && wifiAvailable) || (qpt == 4 && !dataConnected && wifiAvailable)) {
                            it.result = null
                        }
                    } else {
                        mPrefsMap.getString("system_ui_status_bar_mobile_type_custom", "ERR")
                    }
                }
            }

    }

    @SuppressLint("StaticFieldLeak")
    private fun getMobileType(param: MethodHookParam) {
        mMobileType = XposedHelpers.getObjectField(param.thisObject, "mMobileType") as View
        dataConnected = XposedHelpers.getObjectField(param.args[0], "dataConnected") as Boolean
        wifiAvailable = XposedHelpers.getObjectField(param.args[0], "wifiAvailable") as Boolean
        cache += 1
    }

}
