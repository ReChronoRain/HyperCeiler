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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass

object MobileClass {
    val statusBarMobileClass by lazy {
        loadClass("com.android.systemui.statusbar.StatusBarMobileView")
    }
    val miuiMobileIconBinder by lazy {
        loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder")
    }
    val mOperatorConfig by lazy {
        loadClass("com.miui.interfaces.IOperatorCustomizedPolicy\$OperatorConfig")
    }
    val miuiCellularIconVM by lazy {
        loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM")
    }
    val hdController by lazy {
        loadClass("com.android.systemui.statusbar.policy.HDController")
    }
    val networkController by lazy {
        loadClass("com.android.systemui.statusbar.connectivity.NetworkControllerImpl")
    }
    val mobileSignalController by lazy {
        loadClass("com.android.systemui.statusbar.connectivity.MobileSignalController")
    }
    val shadeHeaderController by lazy {
        loadClass("com.android.systemui.shade.ShadeHeaderController")
    }
    val modernStatusBarMobileView by lazy {
        loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView")
    }
}