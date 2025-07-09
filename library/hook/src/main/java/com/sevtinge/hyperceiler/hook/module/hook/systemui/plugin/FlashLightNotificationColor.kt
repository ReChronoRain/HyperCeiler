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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin

import android.content.Context
import android.content.Intent

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.extension.getString
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
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
