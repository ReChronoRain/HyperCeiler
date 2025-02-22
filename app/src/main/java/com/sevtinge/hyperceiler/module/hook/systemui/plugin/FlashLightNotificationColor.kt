package com.sevtinge.hyperceiler.module.hook.systemui.plugin

import android.content.Context
import android.content.Intent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.utils.extension.getString
import com.sevtinge.hyperceiler.utils.getObjectField
import de.robv.android.xposed.*

object FlashLightNotificationColor {
    fun initLoaderHook(classLoader: ClassLoader) {
        loadClass("miui.systemui.flashlight.MiFlashlightManager", classLoader).methodFinder()
        .filterByName("getExtraMiuiFocusParam")
        .first().createAfterHook {
            val mContext = it.thisObject.getObjectField("context") as Context
            val stringTitle = mContext.getString("flashlight_notification_content_title")
            val stringText = mContext.getString("flashlight_notification_content_text")
            val button = mContext.getString("flashlight_notification_button")
            val intent = Intent("miui.systemui.action.ACTION_CLOSE_FLASHLIGHT").apply {
                            setPackage("miui.systemui.plugin")
                        }
            val uri = intent.toUri(Intent.URI_INTENT_SCHEME)
            it.result = "\n            {\n                \"param_v2\": {\n                    \"protocol\": 1,\n                    \"ticker\": \"" + stringText + "\",\n                    \"tickerPic\": \"miui.focus.pic_ticker_pic\",\n                    \"tickerPicDark\": \"miui.focus.pic_ticker_pic\",\n                    \"aodTitle\": \"" + stringText + "\",\n                    \"aodPic\": \"miui.focus.pic_ado_pic\",\n                    \"scene\": \"template_v2\",\n                    \"enableFloat\": true,\n                    \"updatable\": true,\n                    \"reopen\": \"reopen\",\n                    \"baseInfo\": {\n                        \"title\": \"" + stringTitle + "\",\n                        \"colorTitle\": \"\",\n                        \"content\": \"" + stringText + "\",\n                        \"colorContent\": \"\",\n                        \"subContent\": \"\",\n                        \"colorSubContent\": \"\",\n                        \"type\": 2\n                    },\n                    \"bgInfo\": {\n                        \"colorBg\": \"\"\n                    },\n                    \"actions\": [\n                        {\n                            \"actionTitle\":\"" + button + "\",\n                            \"actionIntentType\": 2,\n                            \"actionIntent\": \"" + uri + "\",\n                            \"actionIcon\": \"miui.focus.pic_mark_v2\"\n                        }\n                    ]\n                }\n            }\n            "
        }
    }
}