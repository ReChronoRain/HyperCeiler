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
package com.sevtinge.hyperceiler.hook.module.hook.screenshot

import android.os.Build
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.setStaticObject
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object DeviceShellCustomize : BaseHook() {
     private lateinit var device: String
     private val deviceS by lazy {
         mPrefsMap.getString("screenshot_device_customize", "")
     }

     override fun init() {
         loadClass("com.miui.gallery.editor.photo.screen.shell.res.ShellResourceFetcher").methodFinder()
             .filterByName("getResId")
             .first().createHook {
                 before {
                     if (!this@DeviceShellCustomize::device.isInitialized) {
                         device = Build.DEVICE
                     }
                     setStaticObject(loadClass("android.os.Build"), "DEVICE", deviceS)
                 }

                 after {
                     setStaticObject(loadClass("android.os.Build"), "DEVICE", device)
                 }
             }
     }
}
