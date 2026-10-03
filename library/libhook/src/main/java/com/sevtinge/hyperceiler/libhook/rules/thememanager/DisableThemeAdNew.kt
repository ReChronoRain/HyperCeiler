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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.thememanager

import android.view.View
import android.widget.FrameLayout
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import miui.drm.DrmManager

class DisableThemeAdNew : BaseHook() {
    override fun init() {
        runCatching {
            DrmManager::class.java.findAllMethods { name("isSupportAd") }.createHooks {
                returnConstant(false)
            }
        }.onFailure {
            XposedLog.e(TAG, it)
        }
        runCatching {
            DrmManager::class.java.findAllMethods { name("setSupportAd") }.createHooks {
                returnConstant(false)
            }
        }.onFailure {
            XposedLog.e(TAG, it)
        }
        // AdInfoResponse no longer exists in the global theme store on HyperOS 3.3 (the
        // built-in ad model was replaced wholesale by third-party SDKs). A missing class
        // is expected here and should not be logged as an error with a full stack trace.
        val adInfoResponse = findClassIfExists("com.android.thememanager.basemodule.ad.model.AdInfoResponse")
        if (adInfoResponse == null) {
            XposedLog.d(TAG, packageName, "AdInfoResponse not present, skip")
        } else {
            runCatching {
                adInfoResponse.findMethod { name("isAdValid"); paramCount(1) }
                    .createHook {
                        returnConstant(false)
                    }
            }.onFailure {
                XposedLog.e(TAG, it)
            }
        }

        // These two built-in ad ViewHolders (like AdInfoResponse above) are gone on some
        // versions -- the global theme store on HyperOS 3.3 uses third-party ad SDKs and
        // no longer ships com.android.thememanager.basemodule.ad or
        // Self*ItemAdViewHolder.
        // The loadClass calls here had no fallback, so they threw ClassNotFoundError and
        // aborted init(), discarding everything after the DrmManager hooks that had
        // already succeeded.
        removeAdsIfPresent("com.android.thememanager.recommend.view.listview.viewholder.SelfFontItemAdViewHolder")
        removeAdsIfPresent("com.android.thememanager.recommend.view.listview.viewholder.SelfRingtoneItemAdViewHolder")
    }

    private fun removeAdsIfPresent(className: String) {
        val clazz = findClassIfExists(className)
        if (clazz == null) {
            XposedLog.d(TAG, packageName, "$className not present, skip")
            return
        }
        removeAds(clazz)
    }

    private fun removeAds(clazz: Class<*>) {
        try {
            Constructors.find(clazz).filterByParamCount(2).first().createHook {
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
            XposedLog.e(TAG, this.lpparam.packageName, t)
        }
    }
}
