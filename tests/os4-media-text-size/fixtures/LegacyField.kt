package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media
@Suppress("UNCHECKED_CAST")
fun <T> Any.getMediaViewHolderFieldAs(name: String, island: Boolean): T? = javaClass.getField(name).get(this) as? T
