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
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.IS_INTERNATIONAL_BUILD
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isInternational
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField

@Suppress("unchecked_cast")
object DisableHideApp : HomeBaseHookNew() {

    private val isShowFileManager by lazy {
        PrefsBridge.getBoolean("home_title_disable_hide_file")
    }

    private val isDisableHideGoogle by lazy {
        PrefsBridge.getBoolean("home_title_disable_hide_google")
    }

    @Version(isPad = false, min = 600000000)
    private fun initOS3Hook() {
        if (IS_INTERNATIONAL_BUILD) return

        Constructors.find(loadClass("com.miui.home.model.core.AppFilter"))
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

        Constructors.find(loadClass("com.miui.home.launcher.AppFilter"))
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
