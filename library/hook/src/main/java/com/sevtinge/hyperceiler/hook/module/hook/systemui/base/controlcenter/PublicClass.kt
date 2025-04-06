package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion

object PublicClass {
    val miuiMediaControlPanel by lazy {
        loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaControlPanel")
    }

    val notificationUtil by lazy {
        if (isMoreAndroidVersion(35)) {
            loadClassOrNull("com.miui.systemui.notification.MiuiBaseNotifUtil")
        } else {
            loadClassOrNull("com.android.systemui.statusbar.notification.NotificationUtil")
        }
    }

    val mediaViewHolder by lazy {
        if (isMoreAndroidVersion(35)) {
            loadClassOrNull("com.android.systemui.media.controls.ui.view.MediaViewHolder")
        } else {
            loadClassOrNull("com.android.systemui.media.controls.models.player.MediaViewHolder")
        }
    }

    val seekBarObserver by lazy {
        if (isMoreAndroidVersion(35)) {
            loadClassOrNull("com.android.systemui.media.controls.ui.binder.SeekBarObserver")
        } else {
            loadClassOrNull("com.android.systemui.media.controls.models.player.SeekBarObserver")
        }
    }

    val playerTwoCircleView by lazy {
        if (isMoreAndroidVersion(35)) {
            loadClassOrNull("com.miui.systemui.notification.media.PlayerTwoCircleView")
        } else {
            loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.PlayerTwoCircleView")
        }
    }

    val statusBarStateControllerImpl by lazy {
        loadClassOrNull("com.android.systemui.statusbar.StatusBarStateControllerImpl")
    }
}
