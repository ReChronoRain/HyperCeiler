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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.tsmclient

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import io.github.kyuubiran.ezxhelper.core.finder.FieldFinder.`-Static`.fieldFinder
import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass


object AutoNfc : BaseHook() {
    private var isNeed: String = ""

    @SuppressLint("SuspiciousIndentation")
    override fun init() {

        findAndHookMethod(Activity::class.java, "onCreate", Bundle::class.java, object : IMethodHook {
            override fun after(param: AfterHookParam) {
                if (isNeed.endsWith("+onDestroy") || isNeed == "") {
                    createHook(param)
                }
                isNeed += "+onCreate"
            }
        })

        findAndHookMethod(Activity::class.java, "onPause", object : IMethodHook {
            override fun before(param: BeforeHookParam) {
                isNeed += "+onPause"
                if (isNeed.endsWith("+onPause+onPause")) {
                    destroyHook()
                    isNeed += "+onDestroy"
                }
            }
        })

        findAndHookMethod(Activity::class.java, "onDestroy", object : IMethodHook {
            override fun before(param: BeforeHookParam) {
                if (isNeed.endsWith("+onPause")) destroyHook()
                isNeed += "+onDestroy"
            }
        })

        setResReplacement(
            "com.miui.tsmclient",
            "string",
            "nfc_off_hint",
            R.string.tsmclient_nfc_turning_on
        )
        setResReplacement(
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

    private fun createHook (param: AfterHookParam) {
        EzXposed.initAppContext()
        NfcAdapter.getDefaultAdapter(EzXposed.appContext).let { nfcAdapter ->
            if (nfcAdapter.isEnabled) return
            HiddenApiBypass.invoke(NfcAdapter::class.java, nfcAdapter, "enable")
            val activity = param.thisObject as Activity
            if (activity.javaClass.name != "com.miui.tsmclient.ui.quick.DoubleClickActivity") return
            MainScope().launch {
                waitNFCEnable(EzXposed.appContext, nfcAdapter)
                param.thisObject.javaClass.fieldFinder().filter {
                    type == Boolean::class.java
                }.last().setBoolean(param.thisObject, false)
                val ctaHelperClazz = findClass("com.miui.tsmclient.entity.CTAHelper")
                param.thisObject.javaClass.fieldFinder().filterByType(ctaHelperClazz)
                    .first()[param.thisObject]!!.callMethod("check")
            }
        }
    }

    private fun destroyHook() {
        NfcAdapter.getDefaultAdapter(EzXposed.appContext).let { nfcAdapter ->
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
