package com.sevtinge.cemiuiler.module.wini.model

// 基础模糊配置
data class BaseBlurBackgroundModel(
    var enable: Boolean = true,
    var blurRadius: Int = 80,
    var backgroundColor: String = "#FF000000" // 统一使用ARGB Hex
)