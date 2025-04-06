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

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook


object MultiFreeFormSupported : BaseHook() {
    override fun init() {
        runCatching {
            if (!mPrefsMap.getBoolean("system_framework_freeform_recents_to_small_freeform")) {
                loadClass("android.util.MiuiMultiWindowUtils").methodFinder()
                    .filterByName("multiFreeFormSupported")
                    .single().createHook {
                        before {
                            val ex = Throwable()
                            val stackTrace = ex.stackTrace
                            var mResult = true
                            for (i in stackTrace) {
                                if (i.className == "com.android.server.wm.MiuiFreeFormGestureController\$FreeFormReceiver") {
                                    mResult = false
                                    break
                                }
                            }
                            it.result = mResult
                        }
                    }
                logI(TAG, this.lpparam.packageName, "Hook with recents_to_small_freeform success!")
            } else {
                loadClass("android.util.MiuiMultiWindowUtils").methodFinder()
                    .filterByName("multiFreeFormSupported")
                    .single().createHook {
                        returnConstant(true)
                    }
                logI(TAG, this.lpparam.packageName, "Hook success!")
            }
        }
    }

}
