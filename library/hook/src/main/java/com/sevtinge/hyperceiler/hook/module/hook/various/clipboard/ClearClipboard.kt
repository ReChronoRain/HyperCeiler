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
package com.sevtinge.hyperceiler.hook.module.hook.various.clipboard

import android.graphics.drawable.*
import android.view.*
import android.view.View.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.*
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.ResourcesTool
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callStaticMethod
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.hookAfterMethod
import de.robv.android.xposed.*

class ClearClipboard : BaseHook() {
    override fun init() {
        if (!isMoreHyperOSVersion(2f)) return
        MethodFinder.fromClass("android.inputmethodservice.InputMethodModuleManager")
            .filterByName("loadDex")
            .filterByParamTypes(ClassLoader::class.java, String::class.java)
            .first().createAfterHook {
                createHook(it.args[0] as ClassLoader)
            }
    }

    private fun createHook(classLoader: ClassLoader) {
        MethodFinder.fromClass("com.miui.inputmethod.InputMethodClipboardPhrasePopupView", classLoader)
            .filterByName("initPopupWindow")
            .first()
            .hookAfterMethod {
                val onClickAddButton: OnClickListener
                val addButton: ImageView
                val addButtonIcon: Drawable
                val removeIcon: Drawable
                val mPopWindow: PopupWindow = it.thisObject as PopupWindow
                val mClipboardText: TextView = it.thisObject.getObjectFieldAs("mClipboardText")

                it.thisObject.getObjectFieldAs<ImageView>("addButton").apply {
                    addButton = this
                    addButtonIcon = drawable
                    removeIcon = ResourcesTool.loadModuleRes(context).getDrawable(R.drawable.ic_remove, context.theme)

                    callMethod("setVisibility", 0)
                    onClickAddButton = callMethod("getListenerInfo")!!.getObjectFieldAs("mOnClickListener")
                    setOnClickListener {
                        logI("use self OnClickListener")
                        if (!mClipboardText.isSelected) {
                            onClickAddButton.onClick(it)
                            return@setOnClickListener
                        }

                        logI("clean Clipboard")
                        val mAllClipboardList = mPopWindow.getObjectFieldAs<ArrayList<*>>("mAllClipboardList")
                        mAllClipboardList.forEach { mPopWindow.callMethod("deleteSavedFile", it) }
                        mAllClipboardList.clear()

                        mPopWindow.getObjectField("mInputMethodClipboardAdapter")?.callMethod("clearClipboardListItem")
                        XposedHelpers.findClassIfExists("com.miui.inputmethod.MiuiClipboardManager", classLoader)
                            .callStaticMethod(
                                "setClipboardModelList",
                                mPopWindow.getObjectField("mContext"),
                                mAllClipboardList
                            )
                        mPopWindow.callMethod(
                            "maybeChangeEmptyViewAndClearButtonState",
                            mPopWindow.getObjectField("mCurrentImeClipboardList")
                        )
                    }
                    setImageDrawable(removeIcon)
                }

                MethodFinder.fromClass("com.miui.inputmethod.InputMethodClipboardPhrasePopupView", classLoader)
                    .filterByName("onClick")
                    .filterByParamTypes(View::class.java)
                    .first().createAfterHook {
                        if (mClipboardText.isSelected) {
                            addButton.setImageDrawable(removeIcon)
                        } else {
                            addButton.setImageDrawable(addButtonIcon)
                        }
                        addButton.callMethod("setVisibility", 0)
                    }
            }
    }
}
