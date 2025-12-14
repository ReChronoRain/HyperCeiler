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
package com.sevtinge.hyperceiler.hook.module.rules.systemui.base.controlcenter

import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull

object PublicClass {

    // Android 16
    val mediaViewHolderNew by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaViewHolder")
    }
    val miuiMediaViewControllerImpl by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaViewControllerImpl")
    }
    val seekBarObserverNew by lazy {
        loadClassOrNull($$"com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaViewControllerImpl$seekBarObserver$1")
    }
    val miuiMediaNotificationControllerImpl by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaNotificationControllerImpl")
    }

    // Android 15-
    val miuiMediaControlPanel by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaControlPanel")
    }

    val mediaControlPanel by lazy {
        loadClassOrNull("com.android.systemui.media.controls.ui.controller.MediaControlPanel")
            ?: loadClassOrNull("com.android.systemui.media.controls.ui.MediaControlPanel")
    }

    val notificationUtil by lazy {
        loadClassOrNull("com.miui.systemui.notification.MiuiBaseNotifUtil")
    }

    val mediaViewHolder by lazy {
        loadClassOrNull("com.android.systemui.media.controls.ui.view.MediaViewHolder")
    }

    val seekBarObserver by lazy {
        loadClassOrNull("com.android.systemui.media.controls.ui.binder.SeekBarObserver")
            ?: loadClassOrNull($$"com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaViewControllerImpl$seekBarObserver$1")
            ?: loadClassOrNull("com.android.systemui.media.controls.models.player.SeekBarObserver")
    }

    val playerTwoCircleView by lazy {
        loadClassOrNull("com.miui.systemui.notification.media.PlayerTwoCircleView")
    }

    val statusBarStateControllerImpl by lazy {
        loadClassOrNull("com.android.systemui.statusbar.StatusBarStateControllerImpl")
    }
}
