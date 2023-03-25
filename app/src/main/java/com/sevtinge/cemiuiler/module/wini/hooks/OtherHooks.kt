package com.sevtinge.cemiuiler.module.wini.hooks

import com.sevtinge.cemiuiler.utils.HookUtils


class OtherHooks(private val classLoader: ClassLoader) {
    fun enableModule() {
        HookUtils.replaceMethodResult(
            "cn.houkyo.wini.utils.JavascriptBridge",
            classLoader,
            "getIsModuleActive",
            true
        )
    }

    fun deviceLevelHook() {
        val DeviceLevelUtilsClassName =
            "com.miui.home.launcher.common.DeviceLevelUtils"
        val CpuLevelUtilsClassName =
            "com.miui.home.launcher.common.CpuLevelUtils"
        val UtilitiesClassName = "com.miui.home.launcher.common.Utilities"
        val BlurUtilsClassName = "com.miui.home.launcher.common.BlurUtils"
        // 高斯模糊类型
        HookUtils.replaceMethodResult(
            BlurUtilsClassName,
            classLoader,
            "getBlurType",
            2
        )
        // 打开文件夹是否开启模糊
        HookUtils.replaceMethodResult(
            BlurUtilsClassName,
            classLoader,
            "isUserBlurWhenOpenFolder",
            true
        )
        // 设备等级
        HookUtils.replaceMethodResult(
            DeviceLevelUtilsClassName,
            classLoader,
            "getDeviceLevel",
            2
        )
        HookUtils.replaceMethodResult(
            DeviceLevelUtilsClassName,
            classLoader,
            "isUseSimpleAnim",
            false
        )
        HookUtils.replaceMethodResult(
            CpuLevelUtilsClassName,
            classLoader,
            "getQualcommCpuLevel",
            2,
            String::class.java
        )
        // 下载特效
        HookUtils.replaceMethodResult(
            CpuLevelUtilsClassName,
            classLoader,
            "needMamlDownload",
            true
        )
        // 平滑动画
        HookUtils.replaceMethodResult(
            UtilitiesClassName,
            classLoader,
            "isUseSmoothAnimationEffect",
            true
        )
    }
}