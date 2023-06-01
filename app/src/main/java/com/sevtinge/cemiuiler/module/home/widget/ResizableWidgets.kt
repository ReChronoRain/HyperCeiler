package com.sevtinge.cemiuiler.module.home.widget

import android.appwidget.AppWidgetProviderInfo
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers


object ResizableWidgets : BaseHook() {
    override fun init() {
        Helpers.hookAllMethods(
            "android.appwidget.AppWidgetHostView",
            null,
            "getAppWidgetInfo",
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    val widgetInfo = param.result as AppWidgetProviderInfo ?: return
                    widgetInfo.resizeMode =
                        AppWidgetProviderInfo.RESIZE_VERTICAL or AppWidgetProviderInfo.RESIZE_HORIZONTAL
                    widgetInfo.minHeight = 0
                    widgetInfo.minWidth = 0
                    widgetInfo.minResizeHeight = 0
                    widgetInfo.minResizeWidth = 0
                    param.result = widgetInfo
                }
            })
    }

}