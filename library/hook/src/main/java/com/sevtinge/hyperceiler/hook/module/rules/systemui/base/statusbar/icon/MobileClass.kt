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
package com.sevtinge.hyperceiler.hook.module.rules.systemui.base.statusbar.icon

import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass

object MobileClass {
    val statusBarMobileClass by lazy {
        loadClass("com.android.systemui.statusbar.StatusBarMobileView")
    }
    val miuiMobileIconBinder by lazy {
        loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder")
    }
    val mOperatorConfig by lazy {
        loadClass($$"com.miui.interfaces.IOperatorCustomizedPolicy$OperatorConfig")
    }
    val miuiCellularIconVM by lazy {
        loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM")
    }
    val mobileUiAdapter by lazy {
        loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.MobileUiAdapter")
    }
    val networkController by lazy {
        loadClass("com.android.systemui.statusbar.connectivity.NetworkControllerImpl")
    }
    val mobileSignalController by lazy {
        loadClass("com.android.systemui.statusbar.connectivity.MobileSignalController")
    }
    val modernStatusBarMobileView by lazy {
        loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView")
    }
    val statusBarIconControllerImpl by lazy {
        loadClass("com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl")
    }
}
