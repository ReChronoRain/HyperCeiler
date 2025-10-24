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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.systemframework

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi.isDebug
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.getIntField
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook

object NoAccessDeviceLogsRequest : BaseHook() {
    private lateinit var mActivityManagerInternal: Any
    private lateinit var mLogcatManagerService: Any
    override fun init() {

        loadClass("com.android.server.logcat.LogcatManagerService", lpparam.classLoader).methodFinder().apply {
            filterByName("onStart")
            .first()
            .createAfterHook {
                try {
                    mLogcatManagerService = it.thisObject
                    mActivityManagerInternal =
                        mLogcatManagerService.getObjectField("mActivityManagerInternal")!!
                } catch (t: Throwable) {
                    logE(TAG, "NoAccessDeviceLogsRequest -> onStart", t)
                }
            }

            filterByName("processNewLogAccessRequest")
            .first()
            .createBeforeHook {
                try {
                    val client = it.args[0]
                    if (client == null || mActivityManagerInternal == null) return@createBeforeHook
                    val uid = client.getIntField("mUid")
                    val packageName =
                        client.getObjectField("mPackageName") as String
                    mLogcatManagerService.callMethod("onAccessApprovedForClient", client)

                    // debug 用，取消禁用详细日志输出可进行调试
                    if (isDebug()) {
                        logD(
                            TAG,
                            this@NoAccessDeviceLogsRequest.lpparam.packageName,
                            "NoAccessDeviceLogsRequest bypass for package=$packageName uid=$uid"
                        )
                    }
                    it.result = null
                } catch (t: Throwable) {
                    // 输出异常日志
                    logE(TAG, this@NoAccessDeviceLogsRequest.lpparam.packageName, "processNewLogAccessRequest failed", t)
                }
            }
        }

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
