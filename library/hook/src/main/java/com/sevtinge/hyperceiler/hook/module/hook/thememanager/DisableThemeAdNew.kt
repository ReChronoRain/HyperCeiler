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
package com.sevtinge.hyperceiler.hook.module.hook.thememanager

import android.view.View
import android.widget.FrameLayout
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import miui.drm.DrmManager

class DisableThemeAdNew : BaseHook() {
    override fun init() {
        try {
            DrmManager::class.java.methodFinder().filterByName("isSupportAd").toList().createHooks {
                returnConstant(false)
            }
        } catch (t: Throwable) {
            Log.ex(t)
        }
        try {
            DrmManager::class.java.methodFinder().filterByName("setSupportAd").toList().createHooks {
                returnConstant(false)
            }
        } catch (t: Throwable) {
            Log.ex(t)
        }
        try {
            loadClass("com.android.thememanager.basemodule.ad.model.AdInfoResponse").methodFinder().filterByName("isAdValid").filterByParamCount(1).first()
                .createHook {
                    returnConstant(false)
                }
        } catch (t: Throwable) {
            Log.ex(t)
        }

        removeAds(loadClass("com.android.thememanager.recommend.view.listview.viewholder.SelfFontItemAdViewHolder"))
        removeAds(loadClass("com.android.thememanager.recommend.view.listview.viewholder.SelfRingtoneItemAdViewHolder"))
    }

    private fun removeAds(clazz: Class<*>) {
        try {
            clazz.constructorFinder().filterByParamCount(2).first().createHook {
                after {
                    if (it.args[0] != null) {
                        val view = it.args[0] as View
                        val params = FrameLayout.LayoutParams(0, 0)
                        view.layoutParams = params
                        view.visibility = View.GONE
                    }
                }
            }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, t)
        }
    }
}
