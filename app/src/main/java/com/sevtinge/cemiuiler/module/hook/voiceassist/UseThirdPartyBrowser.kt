package com.sevtinge.cemiuiler.module.hook.voiceassist

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.log.XposedLogUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Method

object UseThirdPartyBrowser : BaseHook() {
    private var browserActivityWithIntent: Method? = null
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals(
                    "IntentUtils", "permission click No Application can handle your intent"
                )
            }
        }.forEach {
            browserActivityWithIntent = it.getMethodInstance(lpparam.classLoader)
        }

        // XposedBridge.log("Hook到小爱同学进程！");
        /*try {
            val result: List<DexMethodDescriptor> =
                java.util.Objects.requireNonNull<List<DexMethodDescriptor>>(
                    VoiceAssistDexKit.mVoiceAssistResultMethodsMap["BrowserActivityWithIntent"]
                )
            for (descriptor in result) {
                browserActivityWithIntent = descriptor.getMethodInstance(lpparam.classLoader)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }*/
        XposedLogUtils.logI("com.miui.voiceassist browserActivityWithIntent method is $browserActivityWithIntent")
        // Class<?> clazz = XposedHelpers.findClass("e.D.L.pa.Wa", lpparam.classLoader);
        XposedBridge.hookMethod(browserActivityWithIntent, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                // XposedBridge.log("0)Hook到Activity启动，开始判断");
                val intent = param.args[0] as android.content.Intent
                XposedLogUtils.logI(intent.toString())
                try {
                    if (intent.getPackage() == "com.android.browser") {
                        XposedLogUtils.logI("com.miui.voiceassist get URL " + intent.dataString)
                        val uri = android.net.Uri.parse(intent.dataString)
                        val newIntent = android.content.Intent()
                        newIntent.setAction("android.intent.action.VIEW")
                        newIntent.setData(uri)
                        param.args[0] = newIntent
                    }
                } catch (e: Exception) {
                   XposedLogUtils.logE(TAG, e)
                }
            }
        })
    }
}
