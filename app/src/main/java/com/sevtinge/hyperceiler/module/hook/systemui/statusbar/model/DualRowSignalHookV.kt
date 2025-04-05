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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.getIdByName
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.module.base.tool.OtherTool.getModuleRes
import com.sevtinge.hyperceiler.module.hook.systemui.base.api.Dependency
import com.sevtinge.hyperceiler.module.hook.systemui.base.statusbar.icon.MobileClass.miuiMobileIconBinder
import com.sevtinge.hyperceiler.module.hook.systemui.base.statusbar.icon.MobileClass.mobileSignalController
import com.sevtinge.hyperceiler.module.hook.systemui.base.statusbar.icon.MobileClass.modernStatusBarMobileView
import com.sevtinge.hyperceiler.module.hook.systemui.base.statusbar.icon.MobileClass.networkController
import com.sevtinge.hyperceiler.module.hook.systemui.base.statusbar.icon.MobilePrefs.showMobileType
import com.sevtinge.hyperceiler.utils.StateFlowHelper.setStateFlowValue
import com.sevtinge.hyperceiler.utils.api.ProjectApi
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.callMethodAs
import com.sevtinge.hyperceiler.utils.callStaticMethod
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils
import com.sevtinge.hyperceiler.utils.getBooleanField
import com.sevtinge.hyperceiler.utils.getIntField
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.getObjectFieldAs
import de.robv.android.xposed.XposedHelpers
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method
import java.util.function.Consumer


class DualRowSignalHookV : BaseHook() {
    private val rightMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_right_margin", 8) - 8
    }
    private val leftMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_left_margin", 8) - 8
    }
    private val iconScale by lazy {
        mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_size", 10)
    }
    private val verticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_vertical_offset", 8)
    }

    private val selectedIconStyle by lazy {
        mPrefsMap.getString("system_ui_status_mobile_network_icon_style", "")
    }
    private val selectedIconTheme by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_iconmanage_mobile_network_icon_theme", 1)
    }

    private val mobileInfo = MobileInfo
    private val dualSignalResMap = HashMap<String, Int>()

    private val setImageResWithTintLight by lazy {
        DexKit.findMember("SetImageResWithTintLight") { bridge ->
            bridge.findMethod {
                matcher {
                    declaredClass(miuiMobileIconBinder)
                    // modifiers = Modifier.STATIC
                    name("setImageResWithTintLight", StringMatchType.Contains)
                }
            }.singleOrNull()
        } as Method
    }

    override fun init() {
        if (!showMobileType) {
            mResHook.setDensityReplacement(
                "com.android.systemui",
                "dimen",
                "status_bar_mobile_type_half_to_top_distance",
                3f
            )
            mResHook.setDensityReplacement(
                "com.android.systemui",
                "dimen",
                "status_bar_mobile_left_inout_over_strength",
                0f
            )
            mResHook.setDensityReplacement(
                "com.android.systemui",
                "dimen",
                "status_bar_mobile_type_middle_to_strength_start",
                -0.4f
            )
        }

        // 加载 drawable 资源
        loadDualSignalRes()
        // 监听信号
        listenMobileSignal()
        // 设置图标
        setDualSignalIcon()
        // 调整移动网络位置及缩放
        setDualRowStyle()
    }

    private fun loadDualSignalRes() {
        loadClass("com.android.systemui.SystemUIApplication")
            .methodFinder()
            .filterByName("onCreate")
            .single()
            .createHook {
                var isHooked = false

                after { param ->
                    if (!isHooked) {
                        isHooked = true
                        val modRes = getModuleRes(
                            param.thisObject.callMethodAs<Context>("getApplicationContext")
                        )
                        (1..2).forEach { slot ->
                            (0..5).forEach { lvl ->
                                arrayOf("", "dark", "tint").forEach { colorMode ->
                                    val isUseTint = colorMode == "tint"
                                    val isLight = colorMode.isNotEmpty()
                                    val dualIconResName =
                                        getSignalIconResName(slot, lvl, isUseTint, isLight)
                                    dualSignalResMap[dualIconResName] = modRes.getIdentifier(
                                        dualIconResName,
                                        "drawable",
                                        ProjectApi.mAppModulePkg
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun setDualRowStyle() {
        modernStatusBarMobileView.methodFinder()
            .filterByName("constructAndBind")
            .single()
            .createHook {
                after { param ->
                    val rootView = param.result as ViewGroup

                    val subId = rootView.getIntField("subId")
                    val mobileGroup = rootView.findViewByIdName("mobile_group") as LinearLayout
                    mobileGroup.setPadding(
                        DisplayUtils.dp2px(leftMargin * 0.5f), 0,
                        DisplayUtils.dp2px(rightMargin * 0.5f), 0
                    )
                    val mobileSignal = mobileGroup.findViewByIdName("mobile_signal") as ImageView
                    val mobileSignal2 = ImageView(mobileSignal.context).apply {
                        adjustViewBounds = true
                        tag = "mobile_signal2"
                    }
                    val signalGroup = mobileSignal.parent as FrameLayout
                    signalGroup.addView(
                        mobileSignal2,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    val adjustSmallXX = { view: View ->
                        view.layoutParams = view.layoutParams.also {
                            it as FrameLayout.LayoutParams
                            it.topMargin = -18
                        }
                    }
                    val smallHD = mobileGroup.findViewByIdName("mobile_small_hd") as ImageView
                    val smallRoam = mobileGroup.findViewByIdName("mobile_small_roam") as ImageView
                    val satellite = mobileGroup.findViewByIdName("mobile_satellite") as ImageView
                    adjustSmallXX(smallHD)
                    adjustSmallXX(smallRoam)
                    adjustSmallXX(satellite)

                    if (verticalOffset != 8) {
                        signalGroup.translationY = DisplayUtils.dp2px(
                            (verticalOffset - 8) * 0.5f
                        ).toFloat()
                    }
                    val mobileSignalLp = mobileSignal.layoutParams as FrameLayout.LayoutParams
                    mobileSignalLp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    if (iconScale != 10) {
                        mobileSignalLp.height = DisplayUtils.dp2px(iconScale * 2.0f)
                        mobileSignalLp.gravity = Gravity.CENTER
                    } else {
                        mobileSignalLp.height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    mobileSignal.layoutParams = mobileSignalLp
                    mobileSignal2.layoutParams = mobileSignalLp

                    XposedHelpers.setAdditionalInstanceField(mobileSignal, "subId", subId)
                }
            }
    }

    private fun listenMobileSignal() {
        var hasStopUseOneSim = false
        var reuseCache: Map<*, *>? = null

        val miuiIconManagerFactory =
            loadClassOrNull("com.android.systemui.statusbar.phone.MiuiIconManagerFactory")
        mobileSignalController.methodFinder()
            .filterByName("notifyListeners")
            .single()
            .createHook {
                after { param ->
                    val signalController = param.thisObject

                    val currentState = signalController.getObjectFieldAs<Any>("mCurrentState")
                    val dataSim = currentState.getBooleanField("dataSim")
                    val signalStrength = currentState.getObjectField("signalStrength")
                    val level = signalStrength?.callMethodAs<Int?>("getMiuiLevel")
                    var isChanged: Boolean

                    if (dataSim) {
                        val subscriptionInfo =
                            signalController.getObjectFieldAs<Any>("mSubscriptionInfo")
                        val subscriptionId = subscriptionInfo.callMethodAs<Int>("getSubscriptionId")

                        val isSubIdChanged = subscriptionId != mobileInfo.subId

                        isChanged = isSubIdChanged || level != mobileInfo.dataSimLevel
                        mobileInfo.subId = subscriptionId
                        mobileInfo.dataSimLevel = level ?: 0
                    } else {
                        isChanged = level != mobileInfo.noDataSimLevel
                        mobileInfo.noDataSimLevel = level ?: 0
                    }

                    if (hasStopUseOneSim) {
                        isChanged = true
                        mobileInfo.noDataSimLevel = 0
                    }

                    if (!isChanged || mobileInfo.subId == -1) {
                        return@after
                    }

                    if (reuseCache == null) {
                        if (miuiIconManagerFactory != null &&
                            Dependency.dependencies.contains(miuiIconManagerFactory) == true
                        ) {
                            reuseCache = Dependency.miuiLegacyDependency
                                ?.getObjectField("mMiuiIconManagerFactory")
                                ?.callMethod("get")
                                ?.getObjectField("mMobileUiAdapter")
                                ?.getObjectField("mobileIconsViewModel")
                                ?.getObjectField("reuseCache") as Map<*, *>?
                        } else {
                            return@after
                        }
                    }

                    reuseCache?.get(mobileInfo.subId)?.let {
                        setStateFlowValue(
                            it.callMethod("getThird")?.getObjectField("signalIconId"),
                            -1
                        )
                    }
                }
            }

        networkController.methodFinder()
            .filterByName("setCurrentSubscriptionsLocked")
            .single()
            .createHook {
                before { param ->
                    val networkController = param.thisObject
                    val subList = param.args[0] as List<*>

                    val currentSubscriptions =
                        networkController.getObjectFieldAs<List<*>>("mCurrentSubscriptions")

                    hasStopUseOneSim = currentSubscriptions.size > subList.size
                }
            }
    }

    /**
     * 涉及到状态栏组件反色的方法
     *
     * 最初版本(版本未知):
     *   -> access$setImageResWithTintLight
     *   -> access$resetImageWithTintLight
     *   -> access$setTintColor
     *     14系列没有此方法导致移动网络类型单独显示不能正常反色
     *
     * K60U(OS2.0.0.31) / 2024-12-06:
     *   -> setImageResWithTintLight
     *   -> access$resetImageWithTintLight
     *   -> setTintColor
     *
     * 13系列 / 2024-12-06:
     *   -> access$setImageResWithTintLight
     *   access$resetImageWithTintLight 和 access$setTintColor 改成了 lambda 内联
     *
     * 14系列 / 2024-12-07:
     *   同上
     */
    private fun setDualSignalIcon() {
        setImageResWithTintLight.createHook {
            before { param ->
                val icon = param.args[0] as ImageView
                if (icon.id == getIdByName("mobile_signal")) {
                    val pair = param.args[2]

                    setIconImageWithTintLightColor(
                        icon, Triple(
                            pair.callMethodAs("getFirst"),
                            pair.callMethodAs("getSecond"),
                            null
                        )
                    )
                    param.result = null
                }
            }
        }

        val resetImageWithTintLight = miuiMobileIconBinder.methodFinder()
            .filterByName("access\$resetImageWithTintLight")
            .singleOrNull()
        if (resetImageWithTintLight != null) {
            resetImageWithTintLight.createHook {
                before { param ->
                    val icon = param.args[0] as ImageView
                    if (icon.id == getIdByName("mobile_signal")) {
                        setIconImageWithTintLightColor(
                            icon, Triple(
                                param.args[1] as Boolean,
                                param.args[2] as Boolean,
                                null
                            )
                        )
                        param.result = null
                    }
                }
            }
        } else {
            val javaAdapterKt = loadClass("com.android.systemui.util.kotlin.JavaAdapterKt")
            miuiMobileIconBinder.methodFinder()
                .filterByName("bind")
                .single()
                .createHook {
                    after { param ->
                        val viewBinding = param.result

                        val tintLightColorFlow = try {
                            viewBinding.getObjectField("\$tintLightColorFlow")
                        } catch (e: Exception) {
                            logE(TAG, lpparam.packageName, e)
                            null
                        } ?: return@after
                        val container = param.args[0] as View
                        val mobileSignal = container.findViewByIdName("mobile_signal") as ImageView

                        javaAdapterKt.callStaticMethod(
                            "collectFlow",
                            container,
                            tintLightColorFlow,
                            Consumer<Any> {
                                setIconImageWithTintLightColor(
                                    mobileSignal, Triple(
                                        it.callMethodAs("getFirst"),
                                        it.callMethodAs("getSecond"),
                                        it.callMethodAs("getThird")
                                    )
                                )
                            }
                        )
                    }
                }
        }
    }

    private fun setIconImageWithTintLightColor(
        mobileSignal: ImageView,
        tintLightColor: Triple<Boolean, Boolean, Int?>
    ) {
        val subId = XposedHelpers.getAdditionalInstanceField(mobileSignal, "subId")
        if (subId == null || subId != mobileInfo.subId) {
            return
        }

        val signalGroup = mobileSignal.parent as FrameLayout
        val mobileSignal2 = signalGroup.findViewWithTag<ImageView?>("mobile_signal2")
        val isUseTint = tintLightColor.first
        val isLight = tintLightColor.second
        val color = tintLightColor.third

        val (mobileSignalIconId, mobileSignal2IconId) = getDualSignalIconPairResId(
            isUseTint, isLight
        )
        if (mobileSignalIconId != null && mobileSignal2IconId != null) {
            mobileSignal.post {
                mobileSignal.setImageResource(mobileSignalIconId)
                mobileSignal2?.setImageResource(mobileSignal2IconId)
                mobileSignal2?.imageTintList = if (color != null) {
                    // 清除 tag 跳过原始图标设置
                    mobileSignal.tag = null
                    if (isUseTint) {
                        ColorStateList.valueOf(color)
                    } else {
                        null
                    }
                } else {
                    mobileSignal.imageTintList
                }
            }
        }
    }

    private fun getSignalIconResName(
        slot: Int,
        level: Int,
        isUseTint: Boolean,
        isLight: Boolean
    ): String {
        val iconTheme = if (selectedIconTheme == 2) {
            "statusbar_signal_oa_"
        } else {
            "statusbar_signal_classic_"
        }

        val iconStyle = if (selectedIconTheme != 1 && selectedIconStyle.isNotEmpty()) {
            "_$selectedIconStyle"
        } else {
            ""
        }
        val colorMode = if ((!isUseTint || selectedIconStyle == "theme")) {
            if (!isLight) {
                "_dark"
            } else {
                ""
            }
        } else {
            "_tint"
        }

        return "$iconTheme${slot}_$level$colorMode$iconStyle"
    }

    private fun getDualSignalIconPairResId(
        isUseTint: Boolean,
        isLight: Boolean
    ): Pair<Int?, Int?> {
        return Pair(
            dualSignalResMap[getSignalIconResName(1, mobileInfo.dataSimLevel, isUseTint, isLight)],
            dualSignalResMap[getSignalIconResName(2, mobileInfo.noDataSimLevel, isUseTint, isLight)]
        )
    }

    data object MobileInfo {
        private const val ID_SUB_NO_DATA_SIM = -1

        var subId = ID_SUB_NO_DATA_SIM
        var dataSimLevel = 0
        var noDataSimLevel = 0

        fun reset() {
            subId = ID_SUB_NO_DATA_SIM
            dataSimLevel = 0
            noDataSimLevel = 0
        }

        override fun toString(): String {
            return "MobileInfo(subId=$subId, dataSimLevel=$dataSimLevel, noDataSimLevel=$noDataSimLevel)"
        }
    }
}
