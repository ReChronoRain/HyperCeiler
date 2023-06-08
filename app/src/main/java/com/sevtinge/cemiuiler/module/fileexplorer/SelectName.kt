package com.sevtinge.cemiuiler.module.fileexplorer

import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.getObjectField

object SelectName : BaseHook() {
    override fun init() {
        loadClass("com.android.fileexplorer.view.FileListItem").methodFinder().first {
            name == "onFinishInflate"
        }.createHook {
            after {
                (it.thisObject.getObjectField("mFileNameTextView") as TextView).apply {
                    setTextIsSelectable(mPrefsMap.getBoolean("file_explorer_can_selectable"))
                    isSingleLine = mPrefsMap.getBoolean("file_explorer_is_single_line")
                }
            }
        }
    }
}
