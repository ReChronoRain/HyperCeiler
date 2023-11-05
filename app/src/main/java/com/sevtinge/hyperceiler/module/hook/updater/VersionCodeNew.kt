package com.sevtinge.hyperceiler.module.hook.updater

import android.os.Build
import android.text.TextUtils
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import de.robv.android.xposed.XposedHelpers

object VersionCodeNew : BaseHook() {
    private val mBigMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("ro.miui.ui.version.name")
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.first()
    }
    private val mOSMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("ro.mi.os.version.incremental", "version:")
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.first()
    }
    private val mOSCode by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("ro.mi.os.version.name", "OS")
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.first()
    }

    private val mOldVersionCode =
        mPrefsMap.getString("various_updater_big_version", "V816")

    private val mVersionCode =
        mPrefsMap.getString("various_updater_miui_version", "V14.0.22.11.26.DEV")


    override fun init() {
        // 原始修改版本名
        val mApplication = findClassIfExists("com.android.updater.Application")

        findAndHookMethod(mApplication, "onCreate", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if (!TextUtils.isEmpty(mVersionCode)) {
                    XposedHelpers.setStaticObjectField(
                        Build.VERSION::class.java,
                        "INCREMENTAL",
                        "$mOldVersionCode.${mVersionCode.substringAfter(".")}"
                    )
                }
            }
        })

        // 大版本名字修改
        mBigMethod.createHook {
            before {
                if (!TextUtils.isEmpty(mOldVersionCode)) {
                    it.result = mOldVersionCode.substringAfter("V")
                }
            }
        }

        // OS 版本名修改
        mOSMethod.createHook {
            before {
                if (!TextUtils.isEmpty(mVersionCode)) {
                    it.result = mVersionCode
                }
            }
        }

        // OS 版本修改
        mOSCode.createHook {
            before {
                if (!TextUtils.isEmpty(mVersionCode)) {
                    it.result =
                        "${mVersionCode.split(".")[0]}.${mVersionCode.split(".")[1]}.${mVersionCode.split(".")[2]}"
                }
            }
        }
    }
}
