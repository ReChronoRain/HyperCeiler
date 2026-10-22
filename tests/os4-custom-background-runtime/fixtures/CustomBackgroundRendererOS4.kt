package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3
import android.content.Context
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.MediaViewColorConfig
class CustomBackgroundRendererOS4 private constructor() {
 val mediaDataClass:Class<*> get()=BaseHook.classes["test.MediaData"]!!
 data class Options(val colorAnimation:Boolean=false,val inverse:Boolean=false,val blurPercent:Int=10)
 class Frame(val color:Int,val width:Int,val height:Int,val style:Int,val options:Options){val colors=MediaViewColorConfig(color,color+1,0xff102030.toInt(),0xff405060.toInt());fun newPresentation(resizeAnimation:Boolean)=CustomBackgroundDrawableOS4(color).also{it.resize=resizeAnimation};fun applyTo(p:CustomBackgroundDrawableOS4,animate:Boolean){p.color=color;p.updates++}}
 fun render(c:Context,d:Any,style:Int,options:Options,w:Int,h:Int):Frame? {calls++;selections.add(Triple(style,options,w to h));if(throwing)throw IllegalStateException("renderer");if(failure)return null;return Frame(d.javaClass.getField("color").getInt(d)+(if(style==2)100 else 0),w,h,style,options)}
 companion object{@JvmField var available=true;@JvmField var failure=false;@JvmField var throwing=false;@JvmField var calls=0;@JvmField val needs=ArrayList<Set<Int>>();@JvmField val selections=ArrayList<Triple<Int,Options,Pair<Int,Int>>>();fun create(l:ClassLoader,s:Set<Int>):CustomBackgroundRendererOS4?{needs.add(s);return if(available)CustomBackgroundRendererOS4()else null}}
}
