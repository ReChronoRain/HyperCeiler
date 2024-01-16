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
