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
package com.sevtinge.hyperceiler.hook.module.hook.downloads

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks
import java.io.IOException

// https://github.com/YifePlayte/WOMMO/blob/6069e7d/app/src/main/java/com/yifeplayte/wommo/hook/hooks/singlepackage/downloadprovider/RemoveXlDownload.kt
object RemoveXlDownload : BaseHook() {
    override fun init() {
        loadClass("com.android.providers.downloads.config.XLConfig").methodFinder()
            .filter { name in setOf("setDebug", "setSoDebug") }
            .filterNonAbstract().toList()
            .createHooks {
                returnConstant(null)
            }

        loadClass("com.android.providers.downloads.util.FileUtil").methodFinder()
            .filterByName("createFile").single().createHook {
                before {
                    if ((it.args[0] as String).contains(".xlDownload")) {
                        it.throwable = IOException(".xlDownload is blocked")
                    }
                }
            }
    }
}
