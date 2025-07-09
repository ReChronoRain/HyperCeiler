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
package com.sevtinge.hyperceiler.hook.module.hook.systemui

import android.content.ClipboardManager
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.core.util.ObjectUtil.invokeMethodBestMatch
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook

object UnlockClipboard : BaseHook() {
    override fun init() {
        // hook 点来自 淡い夏
        // 解锁原生剪切板编辑框
        // 新方法来自 WOMMO
        val clazzClipboardListener =
            loadClass("com.android.systemui.clipboardoverlay.ClipboardListener")
        if (clazzClipboardListener.declaredFields.any { it.name == "sCtsTestPkgList" })
            clazzClipboardListener.methodFinder().filterByName("onPrimaryClipChanged")
                .filterNonAbstract().single().createBeforeHook { param ->
                    val mClipboardManager =
                        param.thisObject.getObjectFieldOrNullAs<ClipboardManager>("mClipboardManager")!!
                    val primaryClipSource =
                        invokeMethodBestMatch(mClipboardManager, "getPrimaryClipSource") as String?
                    val oldList =
                        param.thisObject.getObjectFieldOrNullAs<List<String>>("sCtsTestPkgList")!!
                    val newList = mutableListOf<String>().apply {
                        addAll(oldList)
                        if (!contains(primaryClipSource)) primaryClipSource?.let { add(it) }
                    }
                    param.thisObject.setObjectField("sCtsTestPkgList", newList)
                }
        else clazzClipboardListener.methodFinder().filterByName("start").filterNonAbstract()
            .single().createBeforeHook {
                val mClipboardManager =
                    it.thisObject.getObjectFieldOrNullAs<ClipboardManager>("mClipboardManager")!!
                mClipboardManager.addPrimaryClipChangedListener(it.thisObject as ClipboardManager.OnPrimaryClipChangedListener?)
                it.result = null
            }
    }
}
