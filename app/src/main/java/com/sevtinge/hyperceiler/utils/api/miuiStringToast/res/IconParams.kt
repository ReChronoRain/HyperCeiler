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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils.api.miuiStringToast.res

class IconParams {
    private var category: String? = null
    private var iconFormat: String? = null
    private var iconResName: String? = null
    private var iconType = 0

    fun setCategory(category: String?) {
        this.category = category
    }

    fun setIconFormat(iconFormat: String?) {
        this.iconFormat = iconFormat
    }

    fun setIconType(iconType: Int) {
        this.iconType = iconType
    }

    fun setIconResName(iconResName: String?) {
        this.iconResName = iconResName
    }

    fun getCategory(): String? {
        return category
    }

    fun getIconFormat(): String? {
        return iconFormat
    }

    fun getIconType(): Int {
        return iconType
    }

    fun getIconResName(): String? {
        return iconResName
    }

    override fun toString(): String {
        return "IconParams{category='$category', iconFormat='$iconFormat', iconResName='$iconResName', iconType=$iconType}"
    }
}
