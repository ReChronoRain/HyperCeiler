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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api

import android.content.Intent
import com.sevtinge.hyperceiler.hook.utils.callMethod

@Suppress("unused")
class ActivityStarter(instance: Any) : BaseReflectObject(instance) {
    @JvmOverloads
    fun startActivity(intent: Intent, isCreateStatusBarTransitionAnimator: Boolean = true) {
        instance.callMethod("startActivity", intent, isCreateStatusBarTransitionAnimator)
    }
}
