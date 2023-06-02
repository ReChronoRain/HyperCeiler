package com.sevtinge.cemiuiler.module.packageinstaller

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.FieldFinder.`-Static`.fieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

class DisableSafeModelTip : BaseHook() {
    override fun init() {
        try {
            loadClassOrNull("com.miui.packageInstaller.model.ApkInfo")?.methodFinder()?.first {
                name == "getSystemApp"
            }?.createHook {
                returnConstant(true)
            }
            val mInstallProgressActivityClass =
                loadClassOrNull("com.miui.packageInstaller.InstallProgressActivity")
            mInstallProgressActivityClass?.methodFinder()
                ?.first {
                    name == "g0"
                }?.createHook {
                    returnConstant(false)
                }
            mInstallProgressActivityClass?.methodFinder()
                ?.first {
                    name == "Q1"
                }?.createHook {
                    before {
                        it.result = null
                    }
                }
            mInstallProgressActivityClass?.methodFinder()
                ?.filter {
                    true
                }?.toList()?.createHooks {
                    after { param ->
                        param.thisObject.javaClass.fieldFinder().first {
                            type == Boolean::class.java
                        }.setBoolean(param.thisObject, false)
                    }
                }
        } catch (_: Throwable) {
        }
    }
}