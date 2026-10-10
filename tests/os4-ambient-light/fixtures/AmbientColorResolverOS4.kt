package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3
import com.sevtinge.hyperceiler.libhook.base.BaseHook
internal class AmbientColorResolverOS4(val mediaDataClass:Class<*>) {
 data class Colors(val light:Int,val dark:Int)
 companion object { @JvmField var available=true; @JvmField var failure=false; @JvmField var throwing=false; @JvmField var calls=0; @JvmField val needs=ArrayList<Pair<Boolean,Boolean>>()
 fun create(loader:ClassLoader,standard:Boolean,optimized:Boolean):AmbientColorResolverOS4? {needs.add(standard to optimized);return if(available) AmbientColorResolverOS4(BaseHook.classes["test.MediaData"]!!)else null} }
 fun resolve(context:android.content.Context,data:Any,optimized:Boolean):Colors? {check(android.os.Looper.myLooper()!==android.os.Looper.getMainLooper());calls++;if(throwing)throw IllegalArgumentException("resolver fixture");if(failure)return null;val n=data.javaClass.getField("color").getInt(data);return Colors(n,n+100)}
}
