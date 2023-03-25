package com.sevtinge.cemiuiler.module.home.dock

import android.content.Context
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookMethod
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.woobox.hookAfterMethod
import com.sevtinge.cemiuiler.utils.woobox.hookBeforeMethod
import de.robv.android.xposed.XC_MethodHook

object FoldDeviceDock : BaseHook() {
    override fun init() {

        //if (!mPrefsMap.getBoolean("home_fold_dock")) return
        var hook1: XC_MethodHook.Unhook? = null
        var hook2: XC_MethodHook.Unhook? = null
        var hook3: XC_MethodHook.Unhook? = null
        findMethod("com.miui.home.launcher.hotseats.HotSeats") {
            name == "initContent"
        }.hookMethod {
            before {
                hook1 = "com.miui.home.launcher.DeviceConfig".hookBeforeMethod(
                    "isFoldDevice"
                ) { hookParam ->
                    hookParam.result = true
                }
            }
            after {
                hook1?.unhook()
            }
        }

        try {
            findMethod("com.miui.home.launcher.hotseats.HotSeats") {
                name == "updateContent"
            }
        } catch (e: Exception) {
            findMethod("com.miui.home.launcher.hotseats.HotSeats") {
                name == "updateContentView"
            }
        }.hookMethod {
            before {
                hook2 = "com.miui.home.launcher.Application".hookBeforeMethod(
                    "isInFoldLargeScreen"
                ) { hookParam ->
                    hookParam.result = true
                }
            }
            after {
                hook2?.unhook()

            }
        }

        findMethod("com.miui.home.launcher.hotseats.HotSeats") {
            name == "isNeedUpdateItemInfo"
        }.hookMethod {
            before {
                hook2 = "com.miui.home.launcher.Application".hookBeforeMethod(
                    "isInFoldLargeScreen"
                ) { hookParam ->
                    hookParam.result = true
                }
            }
            after {
                hook2?.unhook()
            }
        }

        findMethod("com.miui.home.launcher.hotseats.HotSeatsListRecentsAppProvider\$1") {
            name == "handleMessage" && parameterCount == 1
        }.hookMethod {
            before {
                hook3 = "com.miui.home.launcher.Application".hookBeforeMethod("isInFoldLargeScreen") { hookParam -> hookParam.result = true }
            }
            after { hook3?.unhook() }
        }

        "com.miui.home.launcher.DeviceConfig".hookAfterMethod("getHotseatMaxCount") {
            it.result = mPrefsMap.getInt("home_fold_dock_hotseat", 3)
        }

        "com.miui.home.launcher.hotseats.HotSeatsListRecentsAppProvider".hookBeforeMethod("getLimitCount") {
            it.result = mPrefsMap.getInt("home_fold_dock_run", 2)
        }

        "com.miui.home.launcher.allapps.LauncherMode".hookBeforeMethod("isHomeSupportSearchBar", Context::class.java) {
            it.result = false
        }

    }
}