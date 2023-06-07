package com.sevtinge.cemiuiler.module.home.dock

import android.content.Context
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.hookAfterMethod
import com.sevtinge.cemiuiler.utils.hookBeforeMethod
import de.robv.android.xposed.XC_MethodHook

object FoldDeviceDock : BaseHook() {
    override fun init() {

        // if (!mPrefsMap.getBoolean("home_fold_dock")) return
        var hook1: XC_MethodHook.Unhook? = null
        var hook2: XC_MethodHook.Unhook? = null
        var hook3: XC_MethodHook.Unhook? = null

        val mHotSeatsClass = loadClass("com.miui.home.launcher.hotseats.HotSeats")
        mHotSeatsClass.methodFinder().first {
            name == "initContent"
        }.createHook {
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
            mHotSeatsClass.methodFinder().first {
                name == "updateContent"
            }
        } catch (e: Exception) {
            mHotSeatsClass.methodFinder().first {
                name == "updateContentView"
            }
        }.createHook {
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

        mHotSeatsClass.methodFinder().first {
            name == "isNeedUpdateItemInfo"
        }.createHook {
            before {
                hook3 = "com.miui.home.launcher.Application".hookBeforeMethod(
                    "isInFoldLargeScreen"
                ) { hookParam -> hookParam.result = true }
            }
            after {
                hook3?.unhook()
            }
        }

        "com.miui.home.launcher.DeviceConfig".hookAfterMethod(
            "getHotseatMaxCount"
        ) {
            it.result = mPrefsMap.getInt("home_fold_dock_hotseat", 3)
        }

        "com.miui.home.launcher.hotseats.HotSeatsListRecentsAppProvider".hookBeforeMethod(
            "getLimitCount"
        ) {
            it.result = mPrefsMap.getInt("home_fold_dock_run", 2)
        }

        "com.miui.home.launcher.allapps.LauncherMode".hookBeforeMethod(
            "isHomeSupportSearchBar",
            Context::class.java
        ) {
            it.result = false
        }
    }
}
