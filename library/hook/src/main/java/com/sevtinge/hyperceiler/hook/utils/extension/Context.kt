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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.utils.extension

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
fun Context.getString(name: String): String = getString(getStringIdByName(name))

@DrawableRes
fun Context.getDrawableIdByName(name: String): Int = getIdByName(name, "drawable")
fun Context.getDrawable(
    name: String
): Drawable? = getDrawable(getDrawableIdByName(name))
