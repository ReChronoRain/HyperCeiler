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
 * along with it program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.mediaControlPanel
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaNotificationControllerImpl
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaViewControllerImpl
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook

// https://github.com/HowieHChen/XiaomiHelper/blob/77196977a7d55000973188b2c20faafcb36ad15e/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/media/CustomLayout.kt
object MediaViewLayout : BaseHook() {

    private val mediaButtonClass by lazy {
        loadClassOrNull("com.android.systemui.media.controls.shared.model.MediaButton")
            ?: loadClassOrNull("com.android.systemui.media.controls.models.player.MediaButton")
    }
    private val constraintSetClass by lazy {
        loadClass("androidx.constraintlayout.widget.ConstraintSet")
    }
    private val mediaViewControllerClass by lazy {
        loadClassOrNull("com.android.systemui.media.controls.ui.controller.MediaViewController")
            ?: loadClassOrNull("com.android.systemui.media.controls.ui.MediaViewController")
    }
    private val clear by lazy {
        constraintSetClass.methodFinder()
            .filterByName("clear")
            .filterByParamTypes(Int::class.java, Int::class.java)
            .filterByParamCount(2)
            .first()
    }
    private val setVisibility by lazy {
        constraintSetClass.methodFinder()
            .filterByName("setVisibility")
            .filterByParamTypes(Int::class.java, Int::class.java)
            .filterByParamCount(2)
            .first()
    }
    private val connect by lazy {
        constraintSetClass.methodFinder()
            .filterByName("connect")
            .filterByParamTypes(Int::class.java, Int::class.java, Int::class.java, Int::class.java)
            .filterByParamCount(4)
            .first()
    }
    private val setMargin by lazy {
        constraintSetClass.methodFinder()
            .filterByName("setMargin")
            .filterByParamTypes(Int::class.java, Int::class.java, Int::class.java)
            .filterByParamCount(3)
            .first()
    }
    private val setGoneMargin by lazy {
        constraintSetClass.methodFinder()
            .filterByName("setGoneMargin")
            .filterByParamTypes(Int::class.java, Int::class.java, Int::class.java)
            .filterByParamCount(3)
            .first()
    }

    private val headerTitle by lazy {
        appContext.resources.getIdentifier("header_title", "id", lpparam.packageName)
    }
    private val headerArtist by lazy {
        appContext.resources.getIdentifier("header_artist", "id", lpparam.packageName)
    }
    private val icon by lazy {
        appContext.resources.getIdentifier("icon", "id", lpparam.packageName)
    }
    private val albumArt by lazy {
        appContext.resources.getIdentifier("album_art", "id", lpparam.packageName)
    }
    private val mediaSeamless by lazy {
        appContext.resources.getIdentifier("media_seamless", "id", lpparam.packageName)
    }
    private val actions by lazy {
        appContext.resources.getIdentifier("actions", "id", lpparam.packageName)
    }
    private val action0 by lazy {
        appContext.resources.getIdentifier("action0", "id", lpparam.packageName)
    }
    private val action1 by lazy {
        appContext.resources.getIdentifier("action1", "id", lpparam.packageName)
    }
    private val action2 by lazy {
        appContext.resources.getIdentifier("action2", "id", lpparam.packageName)
    }
    private val action3 by lazy {
        appContext.resources.getIdentifier("action3", "id", lpparam.packageName)
    }
    private val action4 by lazy {
        appContext.resources.getIdentifier("action4", "id", lpparam.packageName)
    }

    private val actionsOrder by lazy {
        mPrefsMap.getStringAsInt("system_ui_control_center_media_control_media_button_mode", 0)
    }
    private val album by lazy {
        mPrefsMap.getStringAsInt("system_ui_control_center_media_control_media_album_mode", 0)
    }
    private val headerMargin by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_title_margin", 210).toFloat() / 10
    }
    private val headerPadding by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_title_padding", 20).toFloat() / 10
    }
    private val hideSeamless by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_media_control_media_button_hide_seamless")
    }
    private val actionsLeftAligned by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_media_control_media_button_actions_left_aligned")
    }
    private val type by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_media_button", 140)
    }
    private val typeCustom by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_media_button_custom", 140)
    }

    private val isAndroidB by lazy {
        isMoreAndroidVersion(36)
    }

    override fun init() {
        // a16
        // 虽然可以但还是太抽象了
        /*if (actionsOrder != 0) {
            miuiMediaViewControllerImpl!!.apply {
                var isHook = false

                methodFinder().filterByName("bindButtonCommon")
                    .first()
                    .createBeforeHook {
                        if (!isHook) it.result = null
                    }

                methodFinder().filterByName("bindMediaData")
                    .first()
                    .createAfterHook {
                        val media =
                            it.args[0].getObjectField("semanticActions") ?: return@createAfterHook
                        val holder =
                            it.thisObject.getObjectField("holder") ?: return@createAfterHook
                        val prevOrCustom = media.getObjectField("prevOrCustom")
                        val playOrPause = media.getObjectField("playOrPause")
                        val nextOrCustom = media.getObjectField("nextOrCustom")
                        val custom0 = media.getObjectField("custom0")
                        val custom1 = media.getObjectField("custom1")
                        val action0 = holder.getObjectField("action0") as ImageView
                        val action1 = holder.getObjectField("action1") as ImageView
                        val action2 = holder.getObjectField("action2") as ImageView
                        val action3 = holder.getObjectField("action3") as ImageView
                        val action4 = holder.getObjectField("action4") as ImageView

                        val order = when (actionsOrder) {
                            1 -> listOf(prevOrCustom, playOrPause, nextOrCustom, custom0, custom1)
                            else -> listOf(playOrPause, prevOrCustom, nextOrCustom, custom0, custom1)
                        }
                        val actions = listOf(action0, action1, action2, action3, action4)

                        actions.zip(order).forEach { (action, data) ->
                            isHook = true
                            it.thisObject.callMethod("bindButtonCommon", action, data)
                            isHook = false
                        }
                    }
            }
        }*/

        if (actionsOrder != 0) {
            if (isAndroidB) {
                mediaButtonClass!!.constructorFinder().first()
                    .createAfterHook {
                        val media = it.thisObject
                        val custom0 = media.getObjectField("custom0")
                        val prevOrCustom = media.getObjectField("prevOrCustom")
                        val playOrPause = media.getObjectField("playOrPause")
                        val nextOrCustom = media.getObjectField("nextOrCustom")
                        val custom1 = media.getObjectField("custom1")

                        when (actionsOrder) {
                            1 -> {
                                media.setObjectField("custom0", prevOrCustom)
                                media.setObjectField("prevOrCustom", playOrPause)
                                media.setObjectField("playOrPause", nextOrCustom)
                                media.setObjectField("nextOrCustom", custom0)
                                media.setObjectField("custom1", custom1)
                            }

                            2 -> {
                                media.setObjectField("custom0", playOrPause)
                                media.setObjectField("prevOrCustom", prevOrCustom)
                                media.setObjectField("playOrPause", nextOrCustom)
                                media.setObjectField("nextOrCustom", custom0)
                                media.setObjectField("custom1", custom1)
                            }
                        }
                    }
            } else {
                mediaButtonClass!!.methodFinder()
                    .filterByName("getActionById")
                    .first()
                    .createBeforeHook {
                        val id = it.args[0] as Int
                        logD(TAG, "getActionById: $id, actionsOrder: $actionsOrder")
                        when (id) {
                            action0 -> {
                                it.result =
                                    if (actionsOrder == 1) it.thisObject.getObjectField("prevOrCustom")
                                    else it.thisObject.getObjectField("playOrPause")
                            }
                            action1 -> {
                                it.result =
                                    if (actionsOrder == 1) it.thisObject.getObjectField("playOrPause")
                                    else it.thisObject.getObjectField("prevOrCustom")
                            }
                            action2 -> it.result = it.thisObject.getObjectField("nextOrCustom")
                            action3 -> it.result = it.thisObject.getObjectField("custom0")
                            action4 -> it.result = it.thisObject.getObjectField("custom1")
                        }
                    }
            }
        }


        if (album == 2 || headerMargin != 21.0f || headerPadding != 2.0f || actionsLeftAligned || hideSeamless) {
            if (isAndroidB) {
                // 暂不生效
                miuiMediaNotificationControllerImpl!!.constructorFinder()
                    .first().createAfterHook {
                        val normalLayout =
                            it.thisObject.getObjectFieldOrNull("normalLayout")
                                ?: return@createAfterHook
                        updateConstraintSet(normalLayout)
                    }
            } else {
                mediaViewControllerClass!!.methodFinder()
                    .filterByName("loadLayoutForType")
                    .first().createAfterHook {
                        val expandedLayout = it.thisObject.getObjectFieldOrNull("expandedLayout")
                            ?: return@createAfterHook
                        updateConstraintSet(expandedLayout)
                    }
            }
        }

        if (type != 140 || typeCustom != 140) {
            val drawableUtils = loadClass("com.miui.utils.DrawableUtils", lpparam.classLoader)
            if (isAndroidB) {
                miuiMediaViewControllerImpl!!
            } else {
                mediaControlPanel!!
            }.methodFinder().filterByName("bindButtonCommon")
                .first().createBeforeHook {
                    val mediaAction = it.args[1]
                    val button = it.args[0] as ImageButton
                    val desc = mediaAction.getObjectFieldOrNullAs<String>("contentDescription")
                        ?: return@createBeforeHook

                    if ((typeCustom != 140) && !desc.contains("Play") && !desc.contains("Pause") && !desc.contains("Previous track") && !desc.contains("Next track")) {
                        val loadDrawable =
                            mediaAction.getObjectFieldOrNullAs<Drawable>("icon") ?: return@createBeforeHook
                        val method =
                            drawableUtils.getDeclaredMethod("drawable2Bitmap", Drawable::class.java)
                        val bitmap = method.invoke(null, loadDrawable) as Bitmap
                        val scaledBitmap =
                            bitmap.scale(typeCustom, typeCustom)

                        mediaAction.setObjectField("icon", scaledBitmap.toDrawable(button.context.resources))
                    } else if (type != 140) {
                        val loadDrawable =
                            mediaAction.getObjectFieldOrNullAs<Drawable>("icon") ?: return@createBeforeHook
                        val method =
                            drawableUtils.getDeclaredMethod("drawable2Bitmap", Drawable::class.java)
                        val bitmap = method.invoke(null, loadDrawable) as Bitmap
                        val scaledBitmap = bitmap.scale(type, type)

                        mediaAction.setObjectField("icon", scaledBitmap.toDrawable(button.context.resources))
                    }

                }
        }

    }

    private fun updateConstraintSet(constraintSet: Any) {
        val standardMargin = 26
        if (album == 2) {
//                            connect?.invoke(expandedLayout,
//                                header_title, ConstraintSet.START,
//                                ConstraintSet.PARENT_ID, ConstraintSet.START
//                            )
//                            connect?.invoke(expandedLayout,
//                                header_artist, ConstraintSet.START,
//                                ConstraintSet.PARENT_ID, ConstraintSet.START
//                            )
//                            connect?.invoke(expandedLayout,
//                                actions, ConstraintSet.TOP,
//                                ConstraintSet.PARENT_ID, ConstraintSet.TOP
//                            )
//                            connect?.invoke(expandedLayout,
//                                action0, ConstraintSet.TOP,
//                                ConstraintSet.PARENT_ID, ConstraintSet.TOP
//                            )
            setVisibility.invoke(constraintSet, icon, View.GONE)
            setGoneMargin.invoke(constraintSet, headerTitle, ConstraintSet.START, dp2px(standardMargin))
            setGoneMargin.invoke(constraintSet, headerArtist, ConstraintSet.START, dp2px(standardMargin))
            setGoneMargin.invoke(constraintSet, actions, ConstraintSet.TOP, dp2px(68.5f))
            setGoneMargin.invoke(constraintSet, action0, ConstraintSet.TOP, dp2px(79.5f))
            setVisibility.invoke(constraintSet, albumArt, View.GONE)
        }
        if (headerMargin != 21.0f) {
            val headerMarginTop = dp2px(headerMargin)
            setMargin.invoke(constraintSet, headerTitle, ConstraintSet.TOP, dp2px(headerMarginTop))
            setGoneMargin.invoke(constraintSet, headerTitle, ConstraintSet.TOP, dp2px(headerMarginTop))
        }
        if (headerPadding != 2.0f) {
            setMargin.invoke(constraintSet, headerArtist, ConstraintSet.TOP, dp2px(headerPadding))
        }
        if (actionsLeftAligned) {
            clear.invoke(constraintSet, action4, ConstraintSet.RIGHT)
        }
        if (hideSeamless) {
            setVisibility.invoke(constraintSet, mediaSeamless, View.GONE)
//                            connect?.invoke(expandedLayout,
//                                header_title, ConstraintSet.END,
//                                ConstraintSet.PARENT_ID, ConstraintSet.END
//                            )
//                            connect?.invoke(expandedLayout,
//                                header_artist, ConstraintSet.END,
//                                ConstraintSet.PARENT_ID, ConstraintSet.END
//                            )
            setGoneMargin.invoke(constraintSet, headerTitle, ConstraintSet.END, dp2px(standardMargin))
            setGoneMargin.invoke(constraintSet, headerArtist, ConstraintSet.END, dp2px(standardMargin))
        }
    }

}
