package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.drawable
class AmbientLightDrawable : android.graphics.drawable.Drawable() {
 companion object { @JvmField var ready=true; @JvmField val all=ArrayList<AmbientLightDrawable>() }
 init {all.add(this)}
 val isReady get()=ready
 @JvmField var paused=true; @JvmField var color=0; @JvmField var light=false; @JvmField var starts=0; @JvmField var stops=0; @JvmField var resizes=0
 fun setGradientColor(c:Int, skipAnim:Boolean=false){color=c};fun setLightMode(v:Boolean){light=v};fun start(){starts++;paused=false};fun pause(){paused=true};fun resume(){paused=false};fun stop(){stops++;paused=true};fun animateNextResize(){resizes++}
}
