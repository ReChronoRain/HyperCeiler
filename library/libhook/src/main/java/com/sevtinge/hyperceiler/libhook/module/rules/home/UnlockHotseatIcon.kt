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
package com.sevtinge.hyperceiler.hook.module.rules.home

import com.sevtinge.hyperceiler.hook.module.base.pack.home.HomeBaseHookNew
import com.sevtinge.hyperceiler.hook.utils.findClass
import com.sevtinge.hyperceiler.hook.utils.replaceMethod
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder

class UnlockHotseatIcon : HomeBaseHookNew() {

    @Version(isPad = false, min = 600000000)
    private fun initOS3Hook() {
        DEVICE_CONFIG_NEW.findClass().methodFinder().filterByName("getHotseatMaxCount").first().replaceMethod {
            99
        }
    }

    override fun initBase() {
        DEVICE_CONFIG_OLD.findClass().methodFinder().filterByName("getHotseatMaxCount").first().replaceMethod {
            99
        }
    }
}
