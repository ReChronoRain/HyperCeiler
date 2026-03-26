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

import android.app.PendingIntent
import android.os.Bundle


class StringToastBundle {
    private var mBundle: Bundle = Bundle()
    private var mPackageName: String? = null
    private var mCategory: String? = null
    private var mIntent: PendingIntent? = null
    private var mDuration: Long = 0
    private var mLevel = 0f
    private var mRate = 0f
    private var mCharge: String? = null
    private var mFlag = 0
    private var mParam: String? = null
    private var mStatus: String? = null

    fun setPackageName(name: String?) {
        mPackageName = name
    }

    fun setStrongToastCategory(category: String?) {
        mCategory = category
    }

    fun setTarget(intent: PendingIntent?) {
        mIntent = intent
    }

    fun setDuration(duration: Long) {
        mDuration = duration
    }

    fun setLevel(level: Float) {
        mLevel = level
    }

    fun setRapidRate(rate: Float) {
        mRate = rate
    }

    fun setCharge(charge: String?) {
        mCharge = charge
    }

    fun setStringToastChargeFlag(flag: Int) {
        mFlag = flag
    }

    fun setParam(param: String?) {
        mParam = param
    }

    fun setStatusBarStrongToast(status: String?) {
        mStatus = status
    }

    fun onCreate() {
        mBundle.putString("package_name", mPackageName)
        mBundle.putString("strong_toast_category", mCategory)
        mBundle.putParcelable("target", mIntent)
        mBundle.putLong("duration", mDuration)
        mBundle.putFloat("level", mLevel)
        mBundle.putFloat("rapid_rate", mRate)
        mBundle.putString("charge", mCharge)
        mBundle.putInt("string_toast_charge_flag", mFlag)
        mBundle.putString("param", mParam)
        mBundle.putString("status_bar_strong_toast", mStatus)
    }

    class Builder {
        private var mBundle: Bundle = Bundle()

        fun setPackageName(name: String?): Builder {
            mBundle.putString("package_name", name)
            return this
        }

        fun setStrongToastCategory(category: String?): Builder {
            mBundle.putString("strong_toast_category", category)
            return this
        }

        fun setTarget(intent: PendingIntent?): Builder {
            mBundle.putParcelable("target", intent)
            return this
        }

        fun setDuration(duration: Long): Builder {
            mBundle.putLong("duration", duration)
            return this
        }

        fun setLevel(level: Float): Builder {
            mBundle.putFloat("level", level)
            return this
        }

        fun setRapidRate(rate: Float): Builder {
            mBundle.putFloat("rapid_rate", rate)
            return this
        }

        fun setCharge(charge: String?): Builder {
            mBundle.putString("charge", charge)
            return this
        }

        fun setStringToastChargeFlag(flag: Int): Builder {
            mBundle.putInt("string_toast_charge_flag", flag)
            return this
        }

        fun setParam(param: String?): Builder {
            mBundle.putString("param", param)
            return this
        }

        fun setStatusBarStrongToast(status: String?): Builder {
            mBundle.putString("status_bar_strong_toast", status)
            return this
        }

        fun onCreate(): Bundle {
            return mBundle
        }
    }
}
