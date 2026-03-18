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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter

import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull

object PublicClass {

    // OS3
    val hyperProgressSeekBar by lazy {
        loadClassOrNull("miuix.miuixbasewidget.widget.HyperProgressSeekBar")
    }
    val clzConstraintSetClass by lazy {
        loadClassOrNull("androidx.constraintlayout.widget.ConstraintSet")
    }
    val miuiIslandMediaControllerImpl by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediaisland.MiuiIslandMediaControllerImpl") ?:
        loadClassOrNull("com.android.systemui.statusbar.notification.mediaisland.MiuiIslandMediaController")
    }
    val miuiIslandMediaViewHolder by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediaisland.MiuiIslandMediaViewHolder")
    }
    val miuiIslandMediaViewBinderImpl by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediaisland.MiuiIslandMediaViewBinderImpl") ?:
        loadClassOrNull("com.android.systemui.statusbar.notification.mediaisland.MiuiIslandMediaViewBinder")
    }
    val playerIslandConstraintLayout by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediaisland.PlayerIslandConstraintLayout")
    }
    val mediaData by lazy {
        loadClassOrNull("com.android.systemui.media.controls.shared.model.MediaData")
    }
    val miuiMediaNotificationControllerImpl by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaNotificationControllerImpl") ?:
        loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaNotificationController")
    }
    val mediaViewHolderNew by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaViewHolder")
    }
    val miuiMediaViewControllerImpl by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaViewControllerImpl") ?:
        loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaViewController")
    }

    // Android 16
    val seekBarObserverNew by lazy {
        loadClassOrNull($$"com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaViewControllerImpl$seekBarObserver$1")
    }

    // Android 15
    val miuiMediaControlPanel by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaControlPanel")
    }

    val seekBarObserver by lazy {
        loadClassOrNull("com.android.systemui.media.controls.ui.binder.SeekBarObserver")
    }

    val playerTwoCircleView by lazy {
        loadClassOrNull("com.miui.systemui.notification.media.PlayerTwoCircleView")
    }
}
