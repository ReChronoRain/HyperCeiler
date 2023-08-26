package com.sevtinge.cemiuiler.module.screenshot

import android.os.Build 
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass 
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject 
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook 
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object DeviceShellCustomize : BaseHook() {
    private lateinit var device: String 
     private val deviceS by lazy { 
         mPrefsMap.getString("screenshot_device_customize", "")
     } 
  
     override fun init() { 
         loadClass("com.miui.gallery.editor.photo.screen.shell.res.ShellResourceFetcher").methodFinder() 
             .filterByName("getResId") 
             .first().createHook { 
                 before { 
                     if (!this@DeviceShell::device.isInitialized) { 
                         device = Build.DEVICE 
                     } 
                     setStaticObject(loadClass("android.os.Build"), "DEVICE", deviceS) 
                 } 
  
                 after { 
                     setStaticObject(loadClass("android.os.Build"), "DEVICE", device) 
                 } 
             } 
     }
}