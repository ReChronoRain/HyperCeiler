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
package com.sevtinge.hyperceiler.module.hook.home.title

import android.annotation.*
import android.content.res.*
import com.sevtinge.hyperceiler.module.base.*

object EnableIconMonetColor : BaseHook() {

    @SuppressLint("DiscouragedApi")
    override fun init() {
        val monet = "system_accent1_100"
        val monoColorId = Resources.getSystem().getIdentifier(monet, "color", "android")
        var monoColor = Resources.getSystem().getColor(monoColorId, null)
        if (mPrefsMap.getBoolean("home_other_use_edit_color")) {
            monoColor = mPrefsMap.getInt("home_other_your_color_qwq", -1)
        }
        mResHook.setObjectReplacement(
            "com.miui.home",
            "color",
            "monochrome_default",
            monoColor
        )
    }
/*
    @SuppressLint("DiscouragedApi")
    fun initResource(resParam: XC_InitPackageResources.InitPackageResourcesParam) {
        val monet = "system_accent1_100"
        val monoColorId = Resources.getSystem().getIdentifier(monet, "color", "android")
        var monoColor = Resources.getSystem().getColor(monoColorId, null)
        if (BaseXposedInit.mPrefsMap.getBoolean("home_other_use_edit_color")) {
            monoColor = mPrefsMap.getInt("home_other_your_color_qwq", -1)
        }
        resParam.res.setReplacement(
            "com.miui.home",
            "color",
            "monochrome_default",
            monoColor
        )
//        val ColorEntriesId = Resources.getSystem().getStringArray()
//        val ColorEntries = Resources.getSystem().getStringArray(ColorEntriesId)
//        getInitPackageResourcesParam().res.setReplacement(
//            "com.miui.home",
//            "string",
//            ColorEntries.toString(),
//            "Monet"
//        )
    }*/
}
