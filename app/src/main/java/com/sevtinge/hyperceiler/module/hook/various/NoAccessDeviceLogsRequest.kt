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
package com.sevtinge.hyperceiler.module.hook.various

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.sevtinge.hyperceiler.module.base.BaseHook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers


object NoAccessDeviceLogsRequest : BaseHook() {
    private lateinit var mActivityManagerInternal: Any
    private lateinit var mLogcatManagerService: Any
    override fun init() {
        val logAccessClient = loadClass(
            "com.android.server.logcat.LogcatManagerService\$LogAccessClient",
            lpparam.classLoader
        )

        XposedHelpers.findAndHookMethod(
            "com.android.server.logcat.LogcatManagerService", lpparam.classLoader,
            "onStart",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        mLogcatManagerService = param.thisObject
                        mActivityManagerInternal = XposedHelpers.getObjectField(
                            mLogcatManagerService, "mActivityManagerInternal"
                        )
                    } catch (t: Throwable) {
                        logE(TAG, "NoAccessDeviceLogsRequest -> onStart", t)
                    }
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            "com.android.server.logcat.LogcatManagerService", lpparam.classLoader,
            "processNewLogAccessRequest",
            logAccessClient,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val client = param.args[0]
                        if (client == null || mActivityManagerInternal == null) return
                        val uid = XposedHelpers.getIntField(client, "mUid")
                        val packageName =
                            XposedHelpers.getObjectField(client, "mPackageName") as String
                        XposedHelpers.callMethod(
                            mLogcatManagerService,
                            "onAccessApprovedForClient",
                            client
                        )
                        // debug 用，取消禁用详细日志输出可进行调试
                        logI(
                            TAG,
                            this@NoAccessDeviceLogsRequest.lpparam.packageName,
                            "NoAccessDeviceLogsRequest bypass for package=$packageName uid=$uid"
                        )
                        param.result = null
                    } catch (t: Throwable) {
                        // 输出异常日志
                        logE(
                            TAG,
                            this@NoAccessDeviceLogsRequest.lpparam.packageName,
                            "processNewLogAccessRequest failed",
                            t
                        )
                    }
                }
            })
        // 米客原来的取消方法，未知情况封堵失败
        /*try {
            loadClass("com.android.server.logcat.LogcatManagerService").methodFinder().filter {
                name == "onLogAccessRequested"
            }.toList().createHooks {
                before { param ->
                    XposedHelpers.callMethod(param.thisObject, "declineRequest", param.args[0])
                    param.result = null
                }
            }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, t)
        }*/
    }
}
