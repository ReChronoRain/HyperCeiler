package com.sevtinge.hyperceiler.libhook.base
import java.lang.reflect.Method
import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.Hooks
/** Host install seam only, delegates every callback to the actual Hooker/Chain types. */
object BaseHook {
 @JvmStatic fun chain(method:Method,callback:XposedInterface.Hooker){
  Hooks.hooks.getOrPut(method){mutableListOf()}.add(callback)
 }
}
