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
package com.sevtinge.hyperceiler.hook.utils.api.miuiStringToast.res

class StringToastBean {
    private var left: Left? = null
    private var right: Right? = null

    fun getLeft(): Left {
        return left!!
    }

    fun setLeft(left: Left?) {
        this.left = left
    }

    fun getRight(): Right {
        return right!!
    }

    fun setRight(right: Right?) {
        this.right = right
    }

    fun getStringToastBundle(): StringToastBundle {
        return StringToastBundle()
    }
}
