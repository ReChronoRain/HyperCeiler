package com.sevtinge.hyperceiler.module.hook.various

import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.callStaticMethod
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.utils.hookAfterMethod
import de.robv.android.xposed.XposedHelpers
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.tool.ResourcesTool

class ClearClipboard : BaseHook() {
    override fun init() {
        if (!isMoreHyperOSVersion(2f)) return
        MethodFinder.fromClass("android.inputmethodservice.InputMethodModuleManager")
            .filterByName("loadDex")
            .filterByParamTypes(ClassLoader::class.java, String::class.java)
            .first().createAfterHook {
                creatHook(it.args[0] as ClassLoader)
            }
    }

    fun creatHook(classLoader: ClassLoader) {
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