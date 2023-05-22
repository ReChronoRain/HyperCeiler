package com.sevtinge.cemiuiler.module.systemui.lockscreen

import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers

object NoticeFace : BaseHook() {
    private val mKeyguardUpdateMonitor =
        findClass("com.android.keyguard.KeyguardUpdateMonitor", lpparam.classLoader)

    private val mNotificationEntry =
        findClass("com.android.systemui.statusbar.notification.collection.NotificationEntry", lpparam.classLoader)

    private val mDependency =
        findClass("com.android.systemui.Dependency", lpparam.classLoader)

    private val mNotificationViewHierarchyManager =
        findClass("com.android.systemui.statusbar.NotificationViewHierarchyManager", lpparam.classLoader)

    private val mMiuiFaceUnlockManager =
        findClass("com.android.keyguard.faceunlock.MiuiFaceUnlockManager", lpparam.classLoader)

    private val mHapticFeedBackImpl =
        findClass("com.miui.systemui.util.HapticFeedBackImpl", lpparam.classLoader)

    private val mKeyguardUpdateMonitorInjector =
        findClass("com.android.keyguard.injector.KeyguardUpdateMonitorInjector", lpparam.classLoader)

    var flag = false

    override fun init() {
            findAndHookMethod(mNotificationEntry, "setSensitive",
                Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        super.beforeHookedMethod(param)
                        if (flag) {
                            param!!.args[0] = false
                        }
                    }
                })

            findAndHookMethod(mKeyguardUpdateMonitorInjector, "isFaceUnlock",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        super.beforeHookedMethod(param)
                        if (flag) {
                            param!!.result = true
                        }
                    }
                })

            findAndHookMethod(mKeyguardUpdateMonitor, "onFaceAuthenticated",
                Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam?): Any? {
                        flag = true

                        XposedHelpers.callStaticMethod(mDependency, "get", mNotificationViewHierarchyManager)
                            .let { XposedHelpers.callMethod(it, "updateNotificationViews") }

                        XposedHelpers.callStaticMethod(mDependency, "get", mHapticFeedBackImpl)
                            .let { XposedHelpers.callMethod(it, "getHapticFeedbackUtil") }
                            .let { XposedHelpers.callMethod(it, "performHapticFeedback", "mesh_light", false) }

                        XposedHelpers.callStaticMethod(mDependency, "get", mMiuiFaceUnlockManager)
                            .let { XposedHelpers.getObjectField(it, "mFaceViewList") as ArrayList<*>}
                            .forEach { mFaceViewList ->
                                XposedHelpers.callMethod(mFaceViewList, "get")
                                    ?.let { XposedHelpers.callMethod(it, "startFaceUnlockSuccessAnimation") }
                            }

                        flag = false
                        return null
                    }
                }
            )
    }
}