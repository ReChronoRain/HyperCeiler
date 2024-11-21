package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model

import android.content.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.github.kyuubiran.ezxhelper.interfaces.*
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.getIdByName
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.tool.OtherTool.*
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.DualRowSignalHookV.MobileInfo.ID_CHANGED_DATA_SIM
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.DualRowSignalHookV.MobileInfo.ID_SUB_NO_DATA_SIM
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.utils.api.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import de.robv.android.xposed.*


class DualRowSignalHookV : BaseHook() {
    private val mobileTypeSingle = mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable")

    private val rightMargin =
        mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_right_margin", 8) - 8
    private val leftMargin =
        mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_left_margin", 8) - 8
    private val iconScale =
        mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_size", 10)
    private val verticalOffset =
        mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_vertical_offset", 8)

    private val selectedIconStyle =
        mPrefsMap.getString("system_ui_status_mobile_network_icon_style", "")
    private val selectedIconTheme =
        mPrefsMap.getStringAsInt("system_ui_statusbar_iconmanage_mobile_network_icon_theme", 1)

    private val mobileInfo = MobileInfo
    private val dualSignalResMap = HashMap<String, Int>()

    private val mobileSignalViewMap = HashMap<Int, MutableList<View>>()

    override fun init() {
        hookAllConstructors(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    val cellularIcon = param.thisObject

                    // 移动网络全隐藏
                    // cellularIcon.setObjectField("isVisible", newReadonlyStateFlow(false))

                    // 显示漫游
                    cellularIcon.setObjectField("smallRoamVisible", newReadonlyStateFlow(true))
                    // 隐藏小 hd
                    cellularIcon.setObjectField("smallHdVisible", newReadonlyStateFlow(false))
                }
            }
        )

        if (!mobileTypeSingle) {
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
                        for (slot in 1..2) {
                            for (lvl in 0..5) {
                                for (colorMode in arrayOf("", "dark", "tint")) {
                                    val isUseTint = colorMode == "tint"
                                    val isLight = colorMode.isNotEmpty()

                                    val dualIconResName = getSignalIconResName(
                                        slot, lvl, isUseTint, isLight
                                    )

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
        loadClass(STATUS_BAR_MOBILE_VIEW).methodFinder()
            .filterByName("constructAndBind")
            .single()
            .createHook {
                after { param ->
                    val rootView = param.result as ViewGroup

                    val mobileGroup = rootView.findViewByIdName("mobile_group") as LinearLayout
                    mobileGroup.setPadding(
                        DisplayUtils.dp2px(leftMargin * 0.5f), 0,
                        DisplayUtils.dp2px(rightMargin * 0.5f), 0
                    )

                    val mobileGroupParent = mobileGroup.parent as ViewGroup
                    val subId = rootView.getIntField("subId")

                    val mobileSignal = mobileGroup.findViewByIdName("mobile_signal") as ImageView
                    val mobileRoam = mobileGroup.findViewByIdName("mobile_small_roam") as ImageView

                    if (verticalOffset != 8) {
                        (mobileSignal.parent as FrameLayout).translationY = DisplayUtils.dp2px(
                            (verticalOffset - 8) * 0.5f
                        ).toFloat()
                    }

                    val lp = mobileSignal.layoutParams as FrameLayout.LayoutParams
                    if (iconScale != 10) {
                        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        lp.height = DisplayUtils.dp2px(iconScale * 2.0f)
                        lp.gravity = Gravity.CENTER
                        mobileSignal.layoutParams = lp
                        mobileRoam.layoutParams = lp
                    } else {
                        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        lp.height = ViewGroup.LayoutParams.MATCH_PARENT
                        mobileSignal.layoutParams = lp
                    }

                    if (mobileSignalViewMap[subId] == null) {
                        mobileSignalViewMap[subId] = mutableListOf()
                    }
                    mobileSignalViewMap[subId]?.add(mobileGroupParent)

                    XposedHelpers.setAdditionalInstanceField(mobileSignal, "subId", subId)
                }
            }
    }

    private fun listenMobileSignal() {
        loadClass("com.android.systemui.statusbar.connectivity.MobileSignalController")
            .methodFinder()
            .filterByName("notifyListeners")
            .single()
            .createHook {
                after { param ->
                    val signalController = param.thisObject

                    val mCurrentState = signalController.getObjectFieldAs<Any>("mCurrentState")
                    val dataSim = mCurrentState.getBooleanField("dataSim")
                    val signalStrength = mCurrentState.getObjectField("signalStrength")
                    val miuiLevel = signalStrength?.callMethodAs<Int?>("getMiuiLevel")
                    val isChanged: Boolean

                    if (dataSim) {
                        val subscriptionInfo =
                            signalController.getObjectFieldAs<Any>("mSubscriptionInfo")
                        val subscriptionId =
                            subscriptionInfo.callMethodAs<Int>("getSubscriptionId")

                        val isSubIdChanged = subscriptionId != mobileInfo.subId
                        if (isSubIdChanged) {
                            mobileSignalViewMap[subscriptionId]?.forEach {
                                it.post {
                                    it.visibility = View.VISIBLE
                                }
                            }
                            mobileSignalViewMap[mobileInfo.subId]?.forEach {
                                it.post {
                                    it.visibility = View.GONE
                                }
                            }
                        }

                        isChanged = isSubIdChanged && miuiLevel != mobileInfo.dataSimLevel
                        mobileInfo.subId = subscriptionId
                        mobileInfo.dataSimLevel = miuiLevel ?: 0
                    } else {
                        isChanged = miuiLevel != mobileInfo.noDataSimLevel
                        mobileInfo.noDataSimLevel = miuiLevel ?: 0
                    }

                    if (!isChanged || mobileInfo.subId == -1) {
                        return@after
                    }

                    val sDependency = findClass("com.android.systemui.Dependency")
                        .getStaticObjectFieldAs<Any>("sDependency")
                    val iconController = sDependency.callMethodAs<Any>(
                        "getDependencyInner",
                        findClass("com.android.systemui.statusbar.phone.ui.StatusBarIconController")
                    )

                    val iconGroups = iconController.getObjectFieldAs<HashMap<*, *>>("mIconGroups")
                    val statusBarIconList =
                        iconController.getObjectFieldAs<Any>("mStatusBarIconList")
                    val viewIndex = statusBarIconList.callMethodAs<Int>(
                        "getViewIndex", mobileInfo.subId, "mobile"
                    )

                    iconGroups.forEach { (k, _) ->
                        val child = k.getObjectFieldAs<ViewGroup>("mGroup").getChildAt(viewIndex)
                        if ("mobile" == child.getObjectFieldAs<String>("slot")) {
                            child.callMethod("setSubId", ID_CHANGED_DATA_SIM)
                        }
                    }
                }
            }
    }

    private fun setDualSignalIcon() {
        val setImageCallback = IMethodHookCallback { param ->
            val icon = param.args[0] as ImageView

            if (icon.id == getIdByName("mobile_signal")) {
                val mobileGroup = icon.parent as FrameLayout

                val isSetMethod = "access\$setImageResWithTintLight" == param.method.name
                val subId = XposedHelpers.getAdditionalInstanceField(icon, "subId")

                if (mobileInfo.subId != ID_SUB_NO_DATA_SIM) {
                    val isUseTint: Boolean
                    val isLight: Boolean

                    if (isSetMethod) {
                        val pair = param.args[2]
                        isUseTint = pair.callMethodAs("getFirst")
                        isLight = pair.callMethodAs("getSecond")
                    } else {
                        isUseTint = (param.args[1] as Boolean)
                        isLight = (param.args[2] as Boolean)
                    }

                    XposedHelpers.setAdditionalInstanceField(icon, "isUseTint", isUseTint)
                    XposedHelpers.setAdditionalInstanceField(icon, "isLight", isLight)

                    if (subId == null || subId != mobileInfo.subId) {
                        if (isSetMethod) {
                            mobileSignalViewMap[subId]?.forEach {
                                it.post {
                                    it.visibility = View.GONE
                                }
                            }
                        }

                        // param.result = null
                        // return@IMethodHookCallback
                    }

                    val (mobileSignalIconId, mobileRoamIconId) = getDualSignalIconPairResId(
                        mobileInfo, isUseTint, isLight
                    )
                    if (mobileSignalIconId != null && mobileRoamIconId != null) {
                        icon.post {
                            val mobileRoam =
                                mobileGroup.findViewByIdName("mobile_small_roam") as ImageView

                            icon.setImageResource(mobileSignalIconId)
                            mobileRoam.setImageResource(mobileRoamIconId)
                        }
                    }

                    param.result = null
                } else if (icon.id == getIdByName("mobile_small_roam")) {
                    param.result = null
                } else {
                    // 指示器等
                }
            }
        }

        loadClass(MOBILE_ICON_BINDER).methodFinder()
            .filterByName("access\$resetImageWithTintLight")
            .single()
            .createHook {
                before(setImageCallback)
            }

        loadClass(MOBILE_ICON_BINDER).methodFinder()
            .filterByName("access\$setImageResWithTintLight")
            .single()
            .createHook {
                before(setImageCallback)
            }

        loadClass(STATUS_BAR_MOBILE_VIEW).methodFinder()
            .filterByName("setSubId")
            .single()
            .createHook {
                before { param ->
                    val subId = param.args[0] as Int
                    if (subId == ID_CHANGED_DATA_SIM) {
                        if (mobileInfo.subId != ID_SUB_NO_DATA_SIM) {
                            val mobileGroup = param.thisObject as FrameLayout

                            val mobileSignal = mobileGroup.findViewByIdName(
                                "mobile_signal"
                            ) as ImageView
                            val mobileRoam = mobileGroup.findViewByIdName(
                                "mobile_small_roam"
                            ) as ImageView

                            val isUseTint = XposedHelpers.getAdditionalInstanceField(
                                mobileSignal, "isUseTint"
                            ) as Boolean
                            val isLight = XposedHelpers.getAdditionalInstanceField(
                                mobileSignal, "isLight"
                            ) as Boolean

                            val (mobileSignalIconId, mobileRoamIconId) = getDualSignalIconPairResId(
                                mobileInfo, isUseTint, isLight
                            )
                            if (mobileSignalIconId != null && mobileRoamIconId != null) {
                                mobileSignal.post {
                                    mobileSignal.setImageResource(mobileSignalIconId)
                                    mobileRoam.setImageResource(mobileRoamIconId)
                                }
                            }
                        }

                        param.result = null
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
        mobileInfo: MobileInfo,
        isUseTint: Boolean,
        isLight: Boolean
    ): Pair<Int?, Int?> {
        return Pair(
            dualSignalResMap[getSignalIconResName(1, mobileInfo.dataSimLevel, isUseTint, isLight)],
            dualSignalResMap[getSignalIconResName(2, mobileInfo.noDataSimLevel, isUseTint, isLight)]
        )
    }

    companion object {
        const val STATUS_BAR_MOBILE_VIEW =
            "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"

        const val MOBILE_ICON_BINDER =
            "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder"
    }

    data object MobileInfo {
        const val ID_SUB_NO_DATA_SIM = -1
        const val ID_CHANGED_DATA_SIM = -2

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