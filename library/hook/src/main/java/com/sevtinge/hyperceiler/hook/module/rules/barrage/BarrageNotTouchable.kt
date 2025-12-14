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
package com.sevtinge.hyperceiler.hook.module.rules.barrage

import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook
import java.lang.reflect.Method

// https://github.com/YifePlayte/WOMMO/blob/main/app/src/main/java/com/yifeplayte/wommo/hook/hooks/singlepackage/barrage/BarrageNotTouchable.kt
object BarrageNotTouchable : BaseHook() {

    override fun init() {
        loadClass($$"com.xiaomi.barrage.utils.BarrageWindowUtils$ComputeInternalInsetsHandler").methodFinder()
            .filterByName("invoke").filterNonAbstract().single().createBeforeHook { param ->
                val method = param.args[1] as Method
                if (!method.name.equals("onComputeInternalInsets")) return@createBeforeHook

                val barrageWindowUtils = param.thisObject.getObjectFieldOrNull("this$0")!!

                val mWindowParams =
                    barrageWindowUtils.getObjectFieldOrNull("mWindowParams") as LayoutParams
                val mWindowManager =
                    barrageWindowUtils.getObjectFieldOrNull("mWindowManager") as WindowManager
                val mView = barrageWindowUtils.getObjectFieldOrNull("mView") as View
                val mWindowTouchable = barrageWindowUtils.getObjectFieldOrNull("mWindowTouchable")

                if (mWindowTouchable == true || mWindowParams.flags and LayoutParams.FLAG_NOT_TOUCHABLE == 0) {
                    barrageWindowUtils.setObjectField("mWindowTouchable", false)
                    mWindowParams.flags = mWindowParams.flags or LayoutParams.FLAG_NOT_TOUCHABLE
                    mWindowManager.updateViewLayout(mView, mWindowParams)
                }

                param.result = null
            }

    }
}
