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
package com.sevtinge.hyperceiler.utils.api.miuiStringToast.res

class TextParams {
    private var text: String? = null
    private var textColor = 0

    fun setText(text: String?) {
        this.text = text
    }

    fun setTextColor(textColor: Int) {
        this.textColor = textColor
    }

    fun getText(): String {
        return text!!
    }

    fun getTextColor(): Int {
        return textColor
    }

    override fun toString(): String {
        return "TextParams{text='$text', textColor=$textColor}"
    }
}
