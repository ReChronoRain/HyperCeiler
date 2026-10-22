package com.sevtinge.hyperceiler.libhook.base
import fixture.*
class LoadPackage { val classLoader: ClassLoader = javaClass.classLoader; val packageName = "com.android.systemui" }
abstract class BaseHook {
    val lpparam = LoadPackage(); val TAG = "test"
    abstract fun init()
    fun findClass(name: String, classLoader: ClassLoader? = null): Class<*> = when (name) {
        "com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow" -> if (Scenario.legacy) LegacyMenu::class.java else Os4Menu::class.java
        "com.android.systemui.statusbar.notification.modal.IModalController",
        "com.android.systemui.statusbar.notification.modal.ModalController" -> ModalController::class.java
        "com.android.systemui.statusbar.CommandQueue" -> CommandQueue::class.java
        "com.miui.systemui.notification.NotificationSettingsHelper" -> SettingsHelper::class.java
        else -> error("Unexpected class lookup: $name")
    }
    fun findClassIfExists(name: String): Class<*>? = findClass(name)
    fun callMethod(receiver: Any, name: String, vararg args: Any?): Any? = receiver.javaClass.methods.single { it.name==name && it.parameterCount==args.size }.invoke(receiver,*args)
}
