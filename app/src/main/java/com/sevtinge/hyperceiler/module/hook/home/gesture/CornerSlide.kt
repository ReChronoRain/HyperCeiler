package com.sevtinge.hyperceiler.module.hook.home.gesture

import android.app.ActivityManager.RunningTaskInfo
import android.app.AndroidAppHelper
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import com.sevtinge.hyperceiler.module.app.GlobalActions
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.hook.home.LockApp
import com.sevtinge.hyperceiler.utils.callStaticMethod
import com.sevtinge.hyperceiler.utils.getFloatField
import com.sevtinge.hyperceiler.utils.getIntField
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class CornerSlide : BaseHook() {

    var lockApp: LockApp = LockApp()

    var context: Context? = null;

    override fun init() {
        findAndHookMethod(
            "com.android.systemui.shared.recents.system.AssistManager", "isSupportGoogleAssist", Int::class.java,
            XC_MethodReplacement.returnConstant(true),
        )

        val FsGestureAssistHelper = findClassIfExists("com.miui.home.recents.FsGestureAssistHelper")
        findAndHookMethod(FsGestureAssistHelper, "canTriggerAssistantAction", Float::class.java, Float::class.java, Int::class.java, object : MethodHook() {
            override fun before(param: MethodHookParam) {
                val isDisabled = FsGestureAssistHelper.callStaticMethod("isAssistantGestureDisabled", param.args[2]) as Boolean
                if (!isDisabled) {
                    val mAssistantWidth: Int = param.thisObject.getIntField("mAssistantWidth")
                    val f = param.args[0] as Float
                    val f2 = param.args[1] as Float
                    if (f < mAssistantWidth || f > f2 - mAssistantWidth) {
                        param.result = true
                        return
                    }
                }
                param.result = false
            }
        })

        var inDirection = 0
        findAndHookMethod(FsGestureAssistHelper, "handleTouchEvent", MotionEvent::class.java, View::class.java, object : MethodHook() {
            override fun after(param: MethodHookParam) {
                val motionEvent = param.args[0] as MotionEvent
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    val mDownX = param.thisObject.getFloatField("mDownX")
                    val mAssistantWidth = param.thisObject.getIntField("mAssistantWidth")
                    inDirection = if (mDownX < mAssistantWidth) 0 else 1
                }
            }
        })

        findAndHookConstructor("com.miui.home.recents.NavStubView",
            Context::class.java,
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    context = param.args[0] as Context
                }
            })

        findAndHookMethod("com.miui.home.recents.SystemUiProxyWrapper", "startAssistant", Bundle::class.java, object : MethodHook() {
            override fun before(param: MethodHookParam) {
                val bundle = param.args[0] as? Bundle
                if (bundle?.getInt("triggered_by", 0) != 83 || bundle.getInt("invocation_type", 0) != 1) {
                    return
                }
                val direction = if (inDirection == 1) "right" else "left"
                XposedBridge.log(mPrefsMap.getInt("home_navigation_assist_${direction}_slide_action", 0).toString())
                if (mPrefsMap.getInt("home_navigation_assist_${direction}_slide_action", 0) == 10) {
                    XposedBridge.log("114")
                    // val context = findContext(FLAG_ALL)
                    val runningTaskInfo = XposedHelpers.callMethod(
                        XposedHelpers.callStaticMethod(
                            findClassIfExists("com.miui.home.recents.RecentsModel"), "getInstance", context), "getRunningTaskContainHome"
                    ) as RunningTaskInfo
                    lockApp.onLockApp(context, runningTaskInfo)
                    if (LockApp.getLockApp(context) == lockApp.taskId && lockApp.lockId != -1) {
                        findAndHookMethod("com.miui.home.recents.NavStubView",
                            "onTouchEvent", MotionEvent::class.java,
                            object : MethodHook() {
                                override fun before(param: MethodHookParam) {
                                    param.result = false
                                }
                            }
                        )
                    }
                } else {
                    GlobalActions.handleAction(
                        AndroidAppHelper.currentApplication(),
                        "prefs_key_home_navigation_assist_${direction}_slide"
                    )
                }
            }
        })
    }
}
