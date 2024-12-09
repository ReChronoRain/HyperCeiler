package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model

import android.content.*
import android.content.res.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.getIdByName
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.tool.OtherTool.*
import com.sevtinge.hyperceiler.module.hook.systemui.*
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.miuiMobileIconBinder
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.mobileSignalController
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.modernStatusBarMobileView
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.networkController
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.showMobileType
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.StateFlowHelper.getStateFlowValue
import com.sevtinge.hyperceiler.utils.StateFlowHelper.setStateFlowValue
import com.sevtinge.hyperceiler.utils.api.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import de.robv.android.xposed.*
import org.luckypray.dexkit.query.*
import org.luckypray.dexkit.query.enums.*
import org.luckypray.dexkit.query.matchers.*
import java.lang.reflect.*
import java.util.function.*
import kotlin.reflect.*


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
                    modifiers = Modifier.STATIC
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
                        val miuiIconManagerFactoryClz = findClassIfExists(
                            "com.android.systemui.statusbar.phone.MiuiIconManagerFactory"
                        ) ?: return@after
                        if (Dependency.mDependencies?.contains(miuiIconManagerFactoryClz) == true) {
                            reuseCache = Dependency.mMiuiLegacyDependency
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

    private fun setDualSignalIcon() {
        setImageResWithTintLight.createHook {
            before { param ->
                val icon = param.args[0] as ImageView
                val pair = param.args[2]

                setIconImageWithTintLightColor(
                    icon, Triple(
                        pair.callMethodAs("getFirst"),
                        pair.callMethodAs("getSecond"),
                        null
                    )
                )
            }
        }

        val constructor = loadClass("com.android.systemui.util.kotlin.JavaAdapter")
            .getConstructor(loadClass("kotlinx.coroutines.CoroutineScope"))
        // 待优化查找
        loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder\$bind$1$1")
            .methodFinder()
            .filterByName("invokeSuspend")
            .single()
            .createHook {
                after { param ->
                    val binder = param.thisObject

                    val test = binder.getObjectFieldAs<Any>("L$0")
                    val tintViewList = binder.getObjectFieldAs<List<*>>("\$tintViewList")

                    var valueClz: KClass<*>
                    val tintLightColorFlow = try {
                        valueClz = Triple::class
                        binder.getObjectField("\$tintLightColorFlow")
                    } catch (e: Throwable) {
                        valueClz = Pair::class
                        binder.getObjectField("\$tintLightFlow")
                    } ?: return@after

                    val newInstance = constructor.newInstance(test)
                    if (valueClz == Triple::class) {
                        newInstance.callMethod(
                            "alwaysCollectFlow",
                            tintLightColorFlow,
                            Consumer<Any> {
                                val triple = Triple<Boolean, Boolean, Int?>(
                                    it.callMethodAs("getFirst"),
                                    it.callMethodAs("getSecond"),
                                    it.callMethodAs("getThird")
                                )
                                tintViewList.forEach { view ->
                                    if (view is ImageView) {
                                        setIconImageWithTintLightColor(view, triple)
                                    }
                                }
                            }
                        )
                    } else { // valueClz == Pair::class
                        // 因 $iconTint 在 $tintLightFlow 后发生更改，只能 collect $iconTint
                        val colorFlow = binder.getObjectFieldAs<Any>("\$iconTint")
                        newInstance.callMethod(
                            "alwaysCollectFlow",
                            colorFlow,
                            Consumer<Int> {
                                val tintLight = getStateFlowValue(tintLightColorFlow) as Any
                                val triple = Triple<Boolean, Boolean, Int?>(
                                    tintLight.callMethodAs("getFirst"),
                                    tintLight.callMethodAs("getSecond"),
                                    it
                                )
                                tintViewList.forEach { view ->
                                    if (view is ImageView) {
                                        setIconImageWithTintLightColor(view, triple)
                                    }
                                }
                            }
                        )
                    }
                }
            }
    }

    private fun setIconImageWithTintLightColor(
        icon: ImageView,
        tintLightColor: Triple<Boolean, Boolean, Int?>
    ) {
        if (icon.id == getIdByName("mobile_signal")) {
            val subId = XposedHelpers.getAdditionalInstanceField(icon, "subId")
            if (subId == null || subId != mobileInfo.subId) {
                return
            }

            val signalGroup = icon.parent as FrameLayout
            val mobileSignal2 = signalGroup.findViewWithTag<ImageView?>("mobile_signal2")
            val isUseTint = tintLightColor.first
            val isLight = tintLightColor.second
            val color = tintLightColor.third

            val (mobileSignalIconId, mobileSignal2IconId) = getDualSignalIconPairResId(
                isUseTint, isLight
            )
            if (mobileSignalIconId != null && mobileSignal2IconId != null) {
                icon.post {
                    icon.setImageResource(mobileSignalIconId)
                    // 清除 tag 跳过原始图标设置
                    icon.tag = null
                    mobileSignal2?.setImageResource(mobileSignal2IconId)
                    mobileSignal2?.imageTintList = if (color != null) {
                        if (isUseTint) {
                            ColorStateList.valueOf(color)
                        } else {
                            null
                        }
                    } else {
                        icon.imageTintList
                    }
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