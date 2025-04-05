package com.sevtinge.hyperceiler.utils.extension

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

@SuppressLint("DiscouragedApi")
fun Context.getIdByName(
    name: String,
    type: String = "id"
): Int = resources.getIdentifier(name, type, packageName)

@StringRes
fun Context.getStringIdByName(name: String): Int = getIdByName(name, "string")
fun Context.getString(name: String): String? = getString(getStringIdByName(name))

@DrawableRes
fun Context.getDrawableIdByName(name: String): Int = getIdByName(name, "drawable")
fun Context.getDrawable(
    name: String
): Drawable? = getDrawable(getDrawableIdByName(name))
