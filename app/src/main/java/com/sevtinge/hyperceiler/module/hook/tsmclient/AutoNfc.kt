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

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.XModuleResources
import android.nfc.NfcAdapter
import android.widget.Toast
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.FieldFinder.`-Static`.fieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.R
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.BaseXposedInit.mModulePath
import com.sevtinge.hyperceiler.utils.callMethod
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass

object AutoNfc : BaseHook() {
    @SuppressLint("SuspiciousIndentation")
    override fun init() {
        loadClass("com.miui.tsmclient.ui.quick.DoubleClickActivity").methodFinder()
            .filterByName("onCreate")
            .single().createHook {
                after { param ->
                    if (!EzXHelper.isHostPackageNameInited)
                        EzXHelper.initAppContext()
                    NfcAdapter.getDefaultAdapter(EzXHelper.appContext).let { nfcAdapter ->
                        if (nfcAdapter.isEnabled) return@after
                        HiddenApiBypass.invoke(NfcAdapter::class.java, nfcAdapter, "enable")
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
            }

        loadClass("com.miui.tsmclient.ui.quick.DoubleClickActivity").methodFinder()
            .filterByName("onDestroy")
            .single().createHook {
                before {
                    NfcAdapter.getDefaultAdapter(EzXHelper.appContext).let { nfcAdapter ->
                        HiddenApiBypass.invoke(NfcAdapter::class.java, nfcAdapter, "disable")
                    }
                }
            }
    }

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
    }

    private suspend fun waitNFCEnable(context: Context, nfcAdapter: NfcAdapter) {
        repeat(15) {
            if (!nfcAdapter.isEnabled) delay(300) else return@repeat
            if (it == 14)
                Toast.makeText(context, R.string.tsmclient_nfc_turn_on_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
