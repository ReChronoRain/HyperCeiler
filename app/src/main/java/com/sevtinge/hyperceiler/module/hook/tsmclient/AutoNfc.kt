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
package com.sevtinge.hyperceiler.module.hook.tsmclient

import android.annotation.*
import android.app.*
import android.content.*
import android.nfc.*
import android.os.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.finders.FieldFinder.`-Static`.fieldFinder
import com.sevtinge.hyperceiler.*
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import kotlinx.coroutines.*
import org.lsposed.hiddenapibypass.*


object AutoNfc : BaseHook() {
    private var isNeed: String = ""

    @SuppressLint("SuspiciousIndentation")
    override fun init() {

        findAndHookMethod(Activity::class.java, "onCreate", Bundle::class.java, object : MethodHook() {
            override fun after(param: MethodHookParam) {
                if (isNeed.endsWith("+onDestroy") || isNeed == "") {
                    createHook(param)
                }
                isNeed += "+onCreate"
            }
        })

        findAndHookMethod(Activity::class.java, "onPause", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                isNeed += "+onPause"
                if (isNeed.endsWith("+onPause+onPause")) {
                    destroyHook(param)
                    isNeed += "+onDestroy"
                }
            }
        })

        findAndHookMethod(Activity::class.java, "onDestroy", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if (isNeed.endsWith("+onPause")) destroyHook(param)
                isNeed += "+onDestroy"
            }
        })

        mResHook.setResReplacement(
            "com.miui.tsmclient",
            "string",
            "nfc_off_hint",
            R.string.tsmclient_nfc_turning_on
        )
        mResHook.setResReplacement(
            "com.miui.tsmclient",
            "string",
            "immediately_open",
            R.string.tsmclient_nfc_turn_on_manually
        )
    }
/*
    fun initResource(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        val moduleRes = XModuleResources.createInstance(mModulePath, resparam.res)
        resparam.res.setReplacement(
            "com.miui.tsmclient",
            "string",
            "nfc_off_hint",
            moduleRes.fwd(R.string.tsmclient_nfc_turning_on)
        )
        resparam.res.setReplacement(
            "com.miui.tsmclient",
            "string",
            "immediately_open",
            moduleRes.fwd(R.string.tsmclient_nfc_turn_on_manually)
        )
    }*/

    private fun createHook (param: MethodHookParam) {
        if (!EzXHelper.isHostPackageNameInited)
            EzXHelper.initAppContext()
        NfcAdapter.getDefaultAdapter(EzXHelper.appContext).let { nfcAdapter ->
            if (nfcAdapter.isEnabled) return
            HiddenApiBypass.invoke(NfcAdapter::class.java, nfcAdapter, "enable")
            val activity = param.thisObject as Activity
            if (activity.javaClass.name != "com.miui.tsmclient.ui.quick.DoubleClickActivity") return
            MainScope().launch {
                waitNFCEnable(EzXHelper.appContext, nfcAdapter)
                param.thisObject.javaClass.fieldFinder().filter {
                    type == Boolean::class.java
                }.last().setBoolean(param.thisObject, false)
                val ctaHelperClazz = findClass("com.miui.tsmclient.entity.CTAHelper")
                param.thisObject.javaClass.fieldFinder().filterByType(ctaHelperClazz)
                    .first()[param.thisObject]!!.callMethod("check")
            }
        }
    }

    private fun destroyHook (param: MethodHookParam) {
        NfcAdapter.getDefaultAdapter(EzXHelper.appContext).let { nfcAdapter ->
            HiddenApiBypass.invoke(NfcAdapter::class.java, nfcAdapter, "disable")
        }
    }

    private suspend fun waitNFCEnable(context: Context, nfcAdapter: NfcAdapter) {
        repeat(15) {
            if (!nfcAdapter.isEnabled) delay(300) else return@repeat
            if (it == 14)
                Toast.makeText(context, R.string.tsmclient_nfc_turn_on_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
