package com.sevtinge.cemiuiler.module.wini.model

// 系统UI
data class SystemUIModel(
    var notification: NotificationBlurModel = NotificationBlurModel(),
    val quickSetting: QuickSettingModel = QuickSettingModel()
)