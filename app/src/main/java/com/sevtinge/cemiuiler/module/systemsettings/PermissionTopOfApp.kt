package com.sevtinge.cemiuiler.module.systemsettings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object PermissionTopOfApp : BaseHook() {

    override fun init() {
        if (lpparam.packageName == "com.android.settings") {
            XposedHelpers.findAndHookMethod("com.android.settings.SettingsActivity", lpparam.classLoader, "onCreate", Bundle::class.java,
                object : MethodHook() {
                    @Throws(Throwable::class)
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        //调试是否Hook成功
                        super.beforeHookedMethod(param)
                        XposedBridge.log("Cemiuiler: PermissionTopOfApp onCreate has been done.")
                    }

                    @SuppressLint("PrivateApi")
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        val thisObject = param.thisObject
                        val context = thisObject as Context
                        val getIntentMethod = thisObject.javaClass.getMethod("getIntent")
                        val intent = (getIntentMethod.invoke(thisObject) as Intent)
                        XposedBridge.log("settingsIntent:$intent")
                        if (intent.action == "android.settings.action.MANAGE_OVERLAY_PERMISSION") {
                            //intent中的data Uri 示例： package:com.xxx.xxxxxxx ，故去掉前面的package就是应用包名
                            val packageName = intent.data.toString().substring(8)
                            val intentOpenSub = Intent(context, lpparam.classLoader.loadClass("com.android.settings.SubSettings"))
                            intentOpenSub.action = "android.intent.action.MAIN"
                            intentOpenSub.putExtra(":settings:source_metrics", 221)
                            intentOpenSub.putExtra(
                                ":settings:show_fragment",
                                "com.android.settings.applications.appinfo.DrawOverlayDetails"
                            )
                            val bundleApp = Bundle()
                            bundleApp.putString("package", packageName)
                            intentOpenSub.putExtra(":settings:show_fragment_args", bundleApp)
                            context.startActivity(intentOpenSub)
                        }
                    }
                }
            )
        }
    }
}

