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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.sevtinge.hyperceiler.module.base.BaseHook

import com.sevtinge.hyperceiler.utils.setObjectField
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object FixMediaControlPanel : BaseHook() {
    override fun init() {
        try {
            EzXHelper.initHandleLoadPackage(lpparam)
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaControlPanel",
                lpparam.classLoader,
                "setArtwork",
                XposedHelpers.findClass("com.android.systemui.media.MediaData", lpparam.classLoader),
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.thisObject.setObjectField("mCurrentKey", "")
                    }
                })
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, t)
        }
    }
}
