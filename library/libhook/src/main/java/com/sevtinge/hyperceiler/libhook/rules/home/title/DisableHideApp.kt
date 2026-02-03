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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.home.title

import android.content.ComponentName
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isInternational
import com.sevtinge.hyperceiler.libhook.utils.api.IS_INTERNATIONAL_BUILD
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectField
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

@Suppress("unchecked_cast")
object DisableHideApp : HomeBaseHookNew() {

    private val isShowFileManager by lazy {
        mPrefsMap.getBoolean("home_title_disable_hide_file")
    }

    private val isDisableHideGoogle by lazy {
        mPrefsMap.getBoolean("home_title_disable_hide_google")
    }

    @Version(isPad = false, min = 600000000)
    private fun initOS3Hook() {
        if (isInternational()) return

        loadClass("com.miui.home.model.core.AppFilter").constructorFinder()
            .first().createHook {
                after { param ->
                    val skippedItem =
                        param.thisObject.getObjectField("mSkippedItems") as HashSet<ComponentName>

                    if (isShowFileManager) {
                        skippedItem.removeIf {
                            it.packageName == "com.google.android.documentsui"
                        }
                    }
                }
            }
    }


    override fun initBase() {
        if (IS_INTERNATIONAL_BUILD) return

        loadClass("com.miui.home.launcher.AppFilter").constructorFinder()
            .first().createHook {
                after { param ->
                    val skippedItem =
                        param.thisObject.getObjectField("mSkippedItems") as HashSet<ComponentName>

                    if (isShowFileManager) {
                        skippedItem.removeIf {
                            it.packageName == "com.google.android.documentsui"
                        }
                    }

                    if (isDisableHideGoogle) {
                        skippedItem.removeIf {
                            it.packageName == "com.google.android.googlequicksearchbox"
                                || it.packageName == "com.google.android.gms"
                        }
                    }
                }
            }
    }
}
