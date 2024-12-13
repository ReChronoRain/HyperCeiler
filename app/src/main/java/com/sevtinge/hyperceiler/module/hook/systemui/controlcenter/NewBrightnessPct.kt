package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.annotation.*
import android.view.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.tool.*
import com.sevtinge.hyperceiler.utils.*
import de.robv.android.xposed.*

object NewBrightnessPct : BaseHook() {
    private val MiuiBrightnessController by lazy {
        loadClass("com.android.systemui.controlcenter.policy.MiuiBrightnessController")
    }
    private val brightnessUtils by lazy {
        loadClassOrNull("com.android.systemui.controlcenter.policy.BrightnessUtils")
    }

    fun initLoaderHook(classLoader: ClassLoader) {
        loadClass("miui.systemui.controlcenter.panel.main.brightness.BrightnessSliderController\$seekBarListener\$1", classLoader)
            .methodFinder().filterByName("onStartTrackingTouch")
            .first().createBeforeHook {
                startPct(it)
            }

        loadClass("miui.systemui.controlcenter.panel.main.brightness.BrightnessPanelSliderController\$seekBarListener\$1")
            .methodFinder().filterByName("onStartTrackingTouch")
            .first().createBeforeHook {
                startPct(it)
            }
    }

    private fun startPct(it: XC_MethodHook.MethodHookParam) {
        val windowView = getView("miui.systemui.dagger.PluginComponentFactory", it.thisObject.javaClass.classLoader)
        if (windowView == null) {
            logE(TAG, lpparam.packageName, "ControlCenterWindowViewImpl is null")
            return
        }
        OtherTool.initPct(windowView as ViewGroup, 2)
        OtherTool.mPct.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    override fun init() {
        hookAllMethods(
            MiuiBrightnessController, "onStop",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    OtherTool.removePct(OtherTool.mPct)
                }
            })

        hookAllMethods(
            MiuiBrightnessController, "onChanged",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    var pctTag = 0
                    if (OtherTool.mPct != null && OtherTool.mPct.tag != null) {
                        pctTag = OtherTool.mPct.tag as Int
                    }
                    if (pctTag == 0 || OtherTool.mPct == null) return
                    val currentLevel = param.args[3] as Int
                    if (brightnessUtils != null) {
                        val maxLevel =
                            brightnessUtils!!.getStaticObjectField("GAMMA_SPACE_MAX") as Int
                        OtherTool.mPct.text =
                            ((currentLevel * 100) / maxLevel).toString() + "%"
                    }
                }
            })
    }

    private fun getView(str: String, cl: ClassLoader?): Any? {
        val cl2 = loadClass(str, cl)
        val controlCenterWindowView = cl2.callStaticMethod("getInstance")!!
            .callMethod("getPluginComponent")!!
            .getObjectField("controlCenterWindowViewCreatorProvider")!!
            .callMethod("get")!!
            .getObjectField("windowView")
        return controlCenterWindowView
    }
}