package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api

import android.graphics.drawable.Drawable
import com.sevtinge.hyperceiler.hook.utils.getBooleanField
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.setBooleanField
import com.sevtinge.hyperceiler.hook.utils.setObjectField

class ShortcutEntity(instance : Any) : BaseReflectObject(instance) {
    val uniqueTag get() = instance.getObjectField("uniqueTag")

    var tag: String?
        get() = instance.getObjectField("tag") as String?
        set(value) = instance.setObjectField("tag", value) as Unit

    var packageName: String?
        get() = instance.getObjectField("packageName") as String?
        set(value) = instance.setObjectField("packageName", value) as Unit

    var targetPackage: String?
        get() = instance.getObjectField("targetPackage") as String?
        set(value) = instance.setObjectField("targetPackage", value) as Unit

    var targetClass: String?
        get() = instance.getObjectField("targetClass") as String?
        set(value) = instance.setObjectField("targetClass", value) as Unit

    var shortcutName: String?
        get() = instance.getObjectField("shortcutName") as String?
        set(value) = instance.setObjectField("shortcutName", value) as Unit

    var action: String?
        get() = instance.getObjectField("action") as String?
        set(value) = instance.setObjectField("action", value) as Unit

    var data: String?
        get() = instance.getObjectField("data") as String?
        set(value) = instance.setObjectField("data", value) as Unit

    var isAvailable: Boolean
        get() = instance.getBooleanField("isAvailable")
        set(value) = instance.setBooleanField("isAvailable", value) as Unit

    var isSystemDefault: Boolean
        get() = instance.getBooleanField("isSystemDefault")
        set(value) = instance.setBooleanField("isSystemDefault", value) as Unit

    var drawable: Drawable?
        get() = instance.getObjectFieldAs("drawable")
        set(value) = instance.setObjectField("drawable", value) as Unit
}
