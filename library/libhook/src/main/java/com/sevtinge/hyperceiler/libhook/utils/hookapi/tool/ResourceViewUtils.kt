/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

// -------------------- Context 资源查找 --------------------

@SuppressLint("DiscouragedApi")
fun Resources.getIdByName(
    name: String,
    type: String,
    packageName: String
): Int = getIdentifier(name, type, packageName)

@SuppressLint("DiscouragedApi")
fun Context.getIdByName(
    name: String,
    type: String = "id",
    packageName: String = this.packageName
): Int = resources.getIdByName(name, type, packageName)

@StringRes
fun Context.getStringIdByName(name: String): Int = getIdByName(name, "string")

fun Context.getString(name: String): String = getString(getStringIdByName(name))

@DimenRes
fun Context.getDimenByName(name: String): Int = getIdByName(name, "dimen")

@DrawableRes
fun Context.getDrawableIdByName(name: String): Int = getIdByName(name, "drawable")

fun Context.getDrawable(name: String): Drawable? =
    getDrawable(getDrawableIdByName(name))

fun View.findViewByIdName(name: String): View? {
    val viewId = context.getIdByName(name)
    return if (viewId != 0) findViewById(viewId) else null
}

fun Activity.findViewByIdName(name: String): View? {
    val viewId = getIdByName(name)
    return if (viewId != 0) findViewById(viewId) else null
}

// -------------------- View padding --------------------

fun View.setPadding(padding: Int) =
    setPadding(padding, padding, padding, padding)

fun View.setPaddingLeft(paddingLeft: Int) =
    setPaddingSide(paddingLeft, paddingRight)

fun View.setPaddingRight(paddingRight: Int) =
    setPaddingSide(paddingLeft, paddingRight)

fun View.setPaddingSide(paddingSide: Int) =
    setPaddingSide(paddingSide, paddingSide)

fun View.setPaddingSide(paddingLeft: Int, paddingRight: Int) =
    setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

// -------------------- Resources Hook 数据类 --------------------

data class ResourcesHookData(val type: String, val afterValue: Any)

class ResourcesHookMap<String, ResourcesHookData> : HashMap<String, ResourcesHookData>() {
    fun isKeyExist(key: String): Boolean = getOrDefault(key, null) != null
}