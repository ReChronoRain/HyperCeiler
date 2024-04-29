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
package com.sevtinge.hyperceiler.module.hook.systemui

import android.content.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*

object UnlockClipboard : BaseHook() {
    override fun init() {
        // hook 点来自 淡い夏
        // 解锁原生剪切板编辑框
        // 新方法来自 WOMMO
        val clazzClipboardListener =
            loadClass("com.android.systemui.clipboardoverlay.ClipboardListener")
        if (clazzClipboardListener.declaredFields.any {
                it.name == "sCtsTestPkgList"
            }) clazzClipboardListener.methodFinder().filterByName("onPrimaryClipChanged")
            .filterNonAbstract().single().createHook {
                before {
                    val mClipboardManager =
                        getObjectOrNullAs<ClipboardManager>(it.thisObject, "mClipboardManager")!!
                    val primaryClipSource =
                        invokeMethodBestMatch(mClipboardManager, "getPrimaryClipSource") as String
                    val oldList =
                        getObjectOrNullAs<List<String>>(it.thisObject, "sCtsTestPkgList")!!
                    val newList = mutableListOf<String>().apply {
                        addAll(oldList)
                        if (!contains(primaryClipSource)) add(primaryClipSource)
                    }
                    setObject(it.thisObject, "sCtsTestPkgList", newList)
                }
            }
        else clazzClipboardListener.methodFinder().filterByName("start").filterNonAbstract()
            .single().createHook {
                before {
                    val mClipboardManager =
                        getObjectOrNullAs<ClipboardManager>(it.thisObject, "mClipboardManager")!!
                    mClipboardManager.addPrimaryClipChangedListener(it.thisObject as ClipboardManager.OnPrimaryClipChangedListener?)
                    it.result = null
                }
            }
    }
}