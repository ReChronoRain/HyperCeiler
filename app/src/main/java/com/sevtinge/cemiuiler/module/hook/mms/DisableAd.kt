package com.sevtinge.cemiuiler.module.hook.mms

import android.content.Context
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

object DisableAd : BaseHook() {
    override fun init() {
        try {
            dexKitBridge.findClass {
                matcher {
                    addUsingStringsEquals("Unknown type of the message: ")
                }
            }.forEach {
                it.getInstance(EzXHelper.classLoader).methodFinder().first {
                    name == "j"
                }.createHook {
                    returnConstant(false)
                }
            }
            /*val result: List<DexClassDescriptor> = Objects.requireNonNull<List<DexClassDescriptor>>(
                MmsDexKit.mMmsResultClassMap["DisableAd"]
            )
            for (descriptor in result) {
                val enableAds: Class<*> = descriptor.getClassInstance(lpparam.classLoader)
                log("EnableAds class is $enableAds")
                findAndHookMethod(enableAds, "j", object : MethodHook() {
                    @Throws(Throwable::class)
                    override fun before(param: MethodHookParam) {
                        param.setResult(false)
                    }
                })
            }*/
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        findAndHookMethod("com.miui.smsextra.ui.BottomMenu", "allowMenuMode",
            Context::class.java, object : MethodHook() {
                @Throws(Throwable::class)
                override fun before(param: MethodHookParam) {
                    param.setResult(false)
                }
            })
    }
}
