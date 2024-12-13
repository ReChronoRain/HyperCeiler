package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.annotation.*
import android.view.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
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
            .first().createAfterHook {
                startPct(it)
            }

        loadClass("miui.systemui.controlcenter.panel.main.brightness.BrightnessPanelSliderController\$seekBarListener\$1")
            .methodFinder().filterByName("onStartTrackingTouch")
            .first().createAfterHook {
                startPct(it)
            }
    }

    private fun startPct(it: XC_MethodHook.MethodHookParam) {
        val brightnessController =
            it.thisObject.getObjectField("this$0")!!.getObjectField("brightnessController")
        val cl = brightnessController!!.javaClass.classLoader
        val controlCenterControllerImpl =
            getObject("com.android.systemui.controlcenter.policy.ControlCenterControllerImpl", cl)
        val controlCenterContentController =
            getObjectArr(controlCenterControllerImpl, "controlCenter.contentController")
        val mContext = controlCenterContentController!!.callMethod("get")!!
            .getObjectField("content")
        val windowView = mContext!!.callMethod("getContentView")
        if (windowView == null) {
            logE(TAG, lpparam.packageName, "mControlPanelContentView is null")
            return
        }
        OtherTool.initPct(windowView as ViewGroup, 2, windowView.context)
        OtherTool.getTextView().visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    override fun init() {
        hookAllMethods(
            MiuiBrightnessController, "onStop",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    OtherTool.removePct(OtherTool.getTextView())
                }
            })

        hookAllMethods(
            MiuiBrightnessController, "onChanged",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    var pctTag = 0
                    if (OtherTool.getTextView() != null && OtherTool.getTextView().tag != null) {
                        pctTag = OtherTool.getTextView().tag as Int
                    }
                    if (pctTag == 0 || OtherTool.getTextView() == null) return
                    val currentLevel = param.args[3] as Int
                    if (brightnessUtils != null) {
                        val maxLevel =
                            brightnessUtils!!.getStaticObjectField("GAMMA_SPACE_MAX") as Int
                        OtherTool.getTextView().text =
                            ((currentLevel * 100) / maxLevel).toString() + "%"
                    }
                }
            })
    }

    private fun getObject(str: String, cl: ClassLoader?): Any? {
        val dependency = loadClass("com.android.systemui.Dependency", cl)
        val cl2 = loadClass(str, cl)
        val mDependency =
            dependency.getStaticObjectField("sDependency")!!.callMethod("getDependencyInner", cl2)
        return mDependency
    }

    private fun getObjectArr(obj: Any?, str: String): Any? {
        if (obj == null) {
            return null
        }
        val fields = str.split(".")
        var currentObj = obj
        for (field in fields) {
            try {
                currentObj = currentObj!!.getObjectField(field)
            } catch (e: Throwable) {
                currentObj = "ObjectFieldNotExist"
            }
            if (currentObj == "ObjectFieldNotExist") {
                return "ObjectFieldNotExist"
            }
        }
        return currentObj
    }
}