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
package com.sevtinge.hyperceiler.module.hook.securitycenter.lab

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.hook.securitycenter.lab.LabUtilsClass.labUtilClass


object GetNumberEnable : BaseHook() {
    private var labUtils: Class<*>? = null
    override fun init() {
        labUtilClass.forEach {
            labUtils = it.getInstance(EzXHelper.classLoader)
            logI(TAG, this.lpparam.packageName, "labUtils class is $labUtils")
            findAndHookMethod(
                "com.miui.permcenter.settings.PrivacyLabActivity",
                "onCreateFragment",
                object : MethodHook() {
                    @Throws(Throwable::class)
                    override fun before(param: MethodHookParam) {
                        val fm = getStaticObjectFieldSilently(labUtils, "b")
                        if (fm != null) {
                            try {
                                val featMap = fm as MutableMap<String, Int>
                                featMap["mi_lab_operator_get_number_enable"] = 0
                                // featMap.put("mi_lab_blur_location_enable", 0);
                            } catch (ignore: Throwable) {
                            }
                        }
                    }
                })
        }

        /*   val result: List<DexClassDescriptor> = Objects.requireNonNull<List<DexClassDescriptor>>(
               SecurityCenterDexKit.mSecurityCenterResultClassMap.get("LabUtils")
           )
           for (descriptor in result) {
               labUtils = descriptor.getClassInstance(lpparam.classLoader)
               log("labUtils class is $labUtils")
               findAndHookMethod(
                   "com.miui.permcenter.settings.PrivacyLabActivity",
                   "onCreateFragment",
                   object : BaseHook.MethodHook() {
                       @Throws(Throwable::class)
                       protected override fun before(param: MethodHookParam) {
                           val fm = Helpers.getStaticObjectFieldSilently(labUtils, "b")
                           if (fm != null) {
                               try {
                                   val featMap = fm as MutableMap<String, Int>
                                   featMap["mi_lab_operator_get_number_enable"] = 0
                                   // featMap.put("mi_lab_blur_location_enable", 0);
                               } catch (ignore: Throwable) {
                               }
                           }
                       }
                   })
           }
       } catch (e: Throwable) {
           e.printStackTrace()
       }*/
    }
}
