package com.sevtinge.hyperceiler.module.hook.home.widget

import android.appwidget.AppWidgetProviderInfo
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.Helpers


object ResizableWidgets : BaseHook() {
    override fun init() {
        Helpers.hookAllMethods(
            "android.appwidget.AppWidgetHostView",
            null,
            "getAppWidgetInfo",
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    val widgetInfo = param.result as AppWidgetProviderInfo
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
