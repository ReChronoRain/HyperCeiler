package com.sevtinge.cemiuiler.module.hook.securitycenter.lab

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.hook.securitycenter.lab.LabUtilsClass.labUtilClass
import com.sevtinge.cemiuiler.utils.Helpers


object AiClipboardEnable : BaseHook() {
    private var labUtils: Class<*>? = null
    override fun init() {
        labUtilClass.forEach {
            labUtils = it.getInstance(EzXHelper.classLoader)
            logI("labUtils class is $labUtils")
            findAndHookMethod(
                "com.miui.permcenter.settings.PrivacyLabActivity",
                "onCreateFragment",
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        val fm = Helpers.getStaticObjectFieldSilently(labUtils, "b")
                        if (fm != null) {
                            try {
                                val featMap = fm as MutableMap<String, Int>
                                featMap["mi_lab_ai_clipboard_enable"] = 0
                                // featMap.put("mi_lab_blur_location_enable", 0);
                            } catch (ignore: Throwable) {
                            }
                        }
                    }
                })
        }

       /* try {
            val result: List<DexClassDescriptor> = Objects.requireNonNull<List<DexClassDescriptor>>(
                SecurityCenterDexKit.mSecurityCenterResultClassMap.get("LabUtils")
            )
            for (descriptor in result) {
                labUtils = descriptor.getClassInstance(lpparam.classLoader)
                log("labUtils class is $labUtils")
                findAndHookMethod(
                    "com.miui.permcenter.settings.PrivacyLabActivity",
                    "onCreateFragment",
                    object : MethodHook() {
                        protected override fun before(param: MethodHookParam) {
                            val fm = Helpers.getStaticObjectFieldSilently(labUtils, "b")
                            if (fm != null) {
                                try {
                                    val featMap = fm as MutableMap<String, Int>
                                    featMap["mi_lab_ai_clipboard_enable"] = 0
                                    // featMap.put("mi_lab_blur_location_enable", 0);
                                } catch (ignore: Throwable) {
                                }
                            }
                        }
                    })
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }*/
    }
}
