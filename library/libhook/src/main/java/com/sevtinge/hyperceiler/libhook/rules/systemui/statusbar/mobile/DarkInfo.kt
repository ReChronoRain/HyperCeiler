package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile

import android.graphics.Rect

/**
 * 暗色模式信息
 *
 * 统一封装来自 `tintLightColorFlow`（Triple 模式）
 * 和 `onDarkChanged`（6 参数模式）两种来源的暗色数据。
 *
 * 子类无需关心暗色数据的来源差异，只需读取对应字段。
 *
 * @property isUseTint 是否使用 tint 着色（来自 tintLightColorFlow）
 * @property isLight 是否为浅色模式（来自 tintLightColorFlow）
 * @property color tint 颜色值（来自 tintLightColorFlow，可能为 null）
 * @property darkIntensity 暗色强度 0.0~1.0（来自 onDarkChanged）
 * @property tint 暗色 tint 值（来自 onDarkChanged）
 * @property lightColor 亮色颜色（来自 onDarkChanged 6 参数版）
 * @property darkColor 暗色颜色（来自 onDarkChanged 6 参数版）
 * @property areas 暗色区域列表（来自 onDarkChanged）
 */
data class DarkInfo(
    // tintLightColorFlow 来源
    val isUseTint: Boolean = false,
    val isLight: Boolean = true,
    val color: Int? = null,

    // onDarkChanged 来源
    val darkIntensity: Float = 0f,
    val tint: Int = 0,
    val lightColor: Int = 0,
    val darkColor: Int = 0,
    val areas: ArrayList<Rect>? = null
) {
    companion object {
        /**
         * 从 MiuiMobileIconBinder 的 tintLightColorFlow 构建
         * @param isUseTint 是否使用 tint 着色
         * @param isLight 是否为浅色模式
         * @param color tint 颜色值
         */
        @JvmStatic
        fun fromTintLightColor(isUseTint: Boolean, isLight: Boolean, color: Int?): DarkInfo {
            return DarkInfo(
                isUseTint = isUseTint,
                isLight = isLight,
                color = color,
                darkIntensity = if (isLight) 0f else 1f,
                tint = color ?: 0
            )
        }
    }
}
