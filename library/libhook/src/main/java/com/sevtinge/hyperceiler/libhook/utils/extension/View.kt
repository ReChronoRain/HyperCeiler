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

import android.view.*

fun View.setPadding(padding: Int) = setPadding(padding, padding, padding, padding)

fun View.setPaddingLeft(paddingLeft: Int) = setPaddingSide(paddingLeft, paddingRight)
fun View.setPaddingRight(paddingRight: Int) = setPaddingSide(paddingLeft, paddingRight)

fun View.setPaddingSide(paddingSide: Int) = setPaddingSide(paddingSide, paddingSide)
fun View.setPaddingSide(
    paddingLeft: Int,
    paddingRight: Int
) = setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
