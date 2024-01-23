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
package com.sevtinge.hyperceiler.utils.devicesdk

import com.sevtinge.hyperceiler.utils.PropUtils.getProp
import java.util.Locale


fun getFingerPrint(): String = android.os.Build.FINGERPRINT
fun getLocale(): String = getProp("ro.product.locale")
fun getLanguage(): String = Locale.getDefault().toString()
fun getBoard(): String = android.os.Build.BOARD
fun getSoc(): String = getProp("ro.board.platform")
fun getDeviceName(): String = android.os.Build.DEVICE
fun getModelName(): String = android.os.Build.MODEL
fun getBrand(): String = android.os.Build.BRAND
fun getManufacture(): String = android.os.Build.MANUFACTURER
fun getSerial(): String = getProp("ro.serialno").replace("\n", "")
