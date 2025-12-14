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
package com.sevtinge.hyperceiler.hook.module.rules.various.clipboard

import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callStaticMethod
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreSmallVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.hookAfterMethod
import de.robv.android.xposed.XposedHelpers
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

class ClearClipboard : BaseHook() {
    override fun init() {
        if (isMoreSmallVersion(200, 2f)) return
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
            .hookAfterMethod { it ->
                val onClickAddButton: OnClickListener
                val addButton: ImageView
                val addButtonIcon: Drawable
                val removeIcon: Drawable
                val mPopWindow: PopupWindow = it.thisObject as PopupWindow
                val mClipboardText: TextView = it.thisObject.getObjectFieldAs("mClipboardText")

                it.thisObject.getObjectFieldAs<ImageView>("addButton").apply {
                    addButton = this
                    addButtonIcon = drawable
                    removeIcon = OtherTool.getModuleRes(context)
                        .getDrawable(R.drawable.ic_remove, context.theme)

                    callMethod("setVisibility", 0)
                    onClickAddButton = callMethod("getListenerInfo")!!.getObjectFieldAs("mOnClickListener")
                    setOnClickListener { self ->
                        logI("use self OnClickListener")
                        if (!mClipboardText.isSelected) {
                            onClickAddButton.onClick(self)
                            return@setOnClickListener
                        }

                        logI("clean Clipboard")
                        val mAllClipboardList = mPopWindow.getObjectFieldAs<ArrayList<*>>("mAllClipboardList")
                        mAllClipboardList.forEach { arg -> mPopWindow.callMethod("deleteSavedFile", arg) }
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
