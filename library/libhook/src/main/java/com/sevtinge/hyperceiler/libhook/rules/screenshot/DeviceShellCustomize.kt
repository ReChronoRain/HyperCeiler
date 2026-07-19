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
package com.sevtinge.hyperceiler.libhook.rules.screenshot

import android.os.Build
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.java.Fields
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

object DeviceShellCustomize : BaseHook() {
     private lateinit var device: String
     private val deviceS by lazy {
         PrefsBridge.getString("screenshot_device_customize", "")
     }

     override fun init() {
         findClass("com.miui.gallery.editor.photo.screen.shell.res.ShellResourceFetcher").findMethod { name("getResId") }.createHook {
                 before {
                     if (!this@DeviceShellCustomize::device.isInitialized) {
                         device = Build.DEVICE
                     }
                     Fields.setStaticObjectField(findClass("android.os.Build"), "DEVICE", deviceS)
                 }

                 after {
                     Fields.setStaticObjectField(findClass("android.os.Build"), "DEVICE", device)
                 }
             }
     }
}
