package com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin

import android.content.Context
import android.content.Intent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.extension.getString
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import org.json.JSONArray
import org.json.JSONObject

object FlashLightNotificationColor : BaseHook() {
    override fun init() {
        loadClass("miui.systemui.flashlight.MiFlashlightManager").methodFinder()
            .filterByName("getExtraMiuiFocusParam")
            .single().createHook {
                after {
                    val mContext = it.thisObject.getObjectField("context") as Context
                    val stringTitle = mContext.getString("flashlight_notification_content_title")
                    val stringText = mContext.getString("flashlight_notification_content_text")
                    val button = mContext.getString("flashlight_notification_button")
                    val intent = Intent("miui.systemui.action.ACTION_CLOSE_FLASHLIGHT").apply {
                        setPackage("miui.systemui.plugin")
                    }
                    val uri = intent.toUri(Intent.URI_INTENT_SCHEME)

                    val paramV2Obj = JSONObject().apply {
                        put("protocol", 1)
                        put("ticker", stringText)
                        put("tickerPic", "miui.focus.pic_ticker_pic")
                        put("tickerPicDark", "miui.focus.pic_ticker_pic")
                        put("aodTitle", stringText)
                        put("aodPic", "miui.focus.pic_ado_pic")
                        put("scene", "template_v2")
                        put("enableFloat", true)
                        put("updatable", true)
                        put("reopen", "reopen")
                        put("baseInfo", JSONObject().apply {
                            put("title", stringTitle)
                            put("colorTitle", "")
                            put("content", stringText)
                            put("colorContent", "")
                            put("subContent", "")
                            put("colorSubContent", "")
                            put("type", 2)
                        })
                        put("bgInfo", JSONObject().apply {
                            put("colorBg", "")
                        })
                        put("actions", JSONArray().apply {
                            put(JSONObject().apply {
                                put("actionTitle", button)
                                put("actionIntentType", 2)
                                put("actionIntent", uri)
                                put("actionIcon", "miui.focus.pic_mark_v2")
                            })
                        })
                    }
                    val jSONObject = JSONObject().apply {
                        put("param_v2", paramV2Obj)
                    }

                    it.result = jSONObject.toString()
                }
            }
    }
}
