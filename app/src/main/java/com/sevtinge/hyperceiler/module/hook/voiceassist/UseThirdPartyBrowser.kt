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
package com.sevtinge.hyperceiler.module.hook.voiceassist

import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.addUsingStringsEquals
import de.robv.android.xposed.*
import java.lang.reflect.*

object UseThirdPartyBrowser : BaseHook() {
    private var browserActivityWithIntent: Method? = null
    override fun init() {
        DexKit.getDexKitBridge().findMethod {
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
        logI(
            TAG,
            this.lpparam.packageName,
            "com.miui.voiceassist browserActivityWithIntent method is $browserActivityWithIntent"
        )
        // Class<?> clazz = XposedHelpers.findClass("e.D.L.pa.Wa", lpparam.classLoader);
        XposedBridge.hookMethod(browserActivityWithIntent, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                // XposedBridge.log("0)Hook到Activity启动，开始判断");
                val intent = param.args[0] as android.content.Intent
                logI(TAG, this@UseThirdPartyBrowser.lpparam.packageName, intent.toString())
                try {
                    if (intent.getPackage() == "com.android.browser") {
                        logI(
                            TAG,
                            this@UseThirdPartyBrowser.lpparam.packageName,
                            "com.miui.voiceassist get URL " + intent.dataString
                        )
                        val uri = android.net.Uri.parse(intent.dataString)
                        val newIntent = android.content.Intent()
                        newIntent.setAction("android.intent.action.VIEW")
                        newIntent.setData(uri)
                        param.args[0] = newIntent
                    }
                } catch (e: Exception) {
                    logE(TAG, this@UseThirdPartyBrowser.lpparam.packageName, e)
                }
            }
        })
    }
}
