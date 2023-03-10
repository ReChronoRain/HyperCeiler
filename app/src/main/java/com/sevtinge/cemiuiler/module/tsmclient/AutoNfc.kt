package com.sevtinge.cemiuiler.module.tsmclient

import android.content.Context
import android.nfc.NfcAdapter
import android.widget.Toast
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.init.InitFields
import com.github.kyuubiran.ezxhelper.utils.*
import com.sevtinge.cemiuiler.R
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass

object AutoNfc : BaseHook() {
    override fun init() {
        findMethod("com.miui.tsmclient.ui.quick.DoubleClickActivity") {
            name == "onCreate"
        }.hookAfter { param ->
            if (!InitFields.isAppContextInited)
                EzXHelperInit.initAppContext()
            NfcAdapter.getDefaultAdapter(InitFields.appContext).let { nfcAdapter ->
                if (nfcAdapter.isEnabled) return@hookAfter
                HiddenApiBypass.invoke(NfcAdapter::class.java, nfcAdapter, "enable")
                MainScope().launch {
                    waitNFCEnable(InitFields.appContext, nfcAdapter)
                    param.thisObject.javaClass.findAllFields {
                        type == Boolean::class.java
                    }.last().setBoolean(param.thisObject, false)
                    val ctaHelperClazz = findClass("com.miui.tsmclient.entity.CTAHelper")
                    param.thisObject.javaClass.findField {
                        type == ctaHelperClazz
                    }.get(param.thisObject)!!.invokeMethod("check")
                }
            }
        }
        findMethod("com.miui.tsmclient.ui.quick.DoubleClickActivity") {
            name == "onDestroy"
        }.hookBefore {
            if (!InitFields.isAppContextInited) EzXHelperInit.initAppContext()
            NfcAdapter.getDefaultAdapter(InitFields.appContext).let { nfcAdapter ->
                HiddenApiBypass.invoke(NfcAdapter::class.java, nfcAdapter, "disable")
            }
        }
    }

    fun initResource(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        resparam.res.setReplacement("com.miui.tsmclient", "string", "nfc_off_hint", R.string.tsmclient_nfc_turning_on)
        resparam.res.setReplacement("com.miui.tsmclient", "string", "immediately_open", R.string.tsmclient_nfc_turn_on_manually)
    }

    private suspend fun waitNFCEnable(context: Context, nfcAdapter: NfcAdapter) {
        repeat(15) {
            if (!nfcAdapter.isEnabled) delay(300)
            else {
                return@repeat
            }
            if (it == 14)
                Toast.makeText(context, R.string.tsmclient_nfc_turn_on_failed, Toast.LENGTH_SHORT).show()
        }
    }
}