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
package com.sevtinge.hyperceiler.hook.module.base.pack.systemui

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.session.PlaybackState
import android.os.Bundle
import android.util.Base64
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.hchen.superlyricapi.ISuperLyric
import com.hchen.superlyricapi.SuperLyricData
import com.hchen.superlyricapi.SuperLyricTool
import com.hyperfocus.api.FocusApi
import com.hyperfocus.api.IslandApi
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool
import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

abstract class MusicBaseHook : BaseHook() {
    val context: Application by lazy { AndroidAppHelper.currentApplication() }
    private val nSize by lazy {
        mPrefsMap.getInt("system_ui_statusbar_music_size_n", 15).toFloat()
    }
    private val isAodShow by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_music_hide_aod")
    }
    private val isAodMode by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_music_show_aod_mode")
    }



    private val receiver = object : ISuperLyric.Stub() {
        override fun onSuperLyric(data: SuperLyricData) {
            runCatching {
                this@MusicBaseHook.onSuperLyric(data)
            }.onFailure {
                logE(TAG, lpparam.packageName, it)
            }
        }

        override fun onStop(data: SuperLyricData) {
            runCatching {
                if (data.playbackState?.state == PlaybackState.STATE_BUFFERING) return
                this@MusicBaseHook.onStop()
            }.onFailure {
                logE(TAG, lpparam.packageName, it)
            }
        }
    }

    init {
        loadClass("android.app.Application").methodFinder().filterByName("onCreate").first()
            .createAfterHook {
                runCatching {
                    SuperLyricTool.registerSuperLyric(context, receiver)
                    // if (isDebug()) logD(TAG, lpparam.packageName, "registerLyricListener")
                }.onFailure {
                    logE(TAG, "registerLyricListener is no found")
                }
            }
    }

    abstract fun onSuperLyric(data: SuperLyricData)
    abstract fun onStop()

    @SuppressLint("NotificationPermission", "LaunchActivityFromNotification")
    fun sendNotification(text: String, extraData: SuperLyricData) {
        createNotificationChannel()
        val modRes = OtherTool.getModuleRes(context)
        val isClickClock = mPrefsMap.getBoolean("system_ui_statusbar_music_click_clock")
        val launchIntent = context.packageManager.getLaunchIntentForPackage(extraData.packageName)
        // 图标处理
        val basebitmap = base64ToDrawable(extraData.base64Icon)
        val bitmap = basebitmap ?: context.packageManager.getActivityIcon(launchIntent!!).toBitmap()
        val icon: Icon = Icon.createWithBitmap(bitmap).apply { if (basebitmap != null) setTint(Color.WHITE) }
        val dartIcon : Icon = Icon.createWithBitmap(bitmap).apply { if (basebitmap != null) setTint(Color.BLACK) }
        val (lefttext,righttext) = splitSmart(text,SplitConfig(
            maxLength = 6
        ))
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        val intent = Intent("$CHANNEL_ID.actions.switchClockStatus")
        // 翻译
        val tf = extraData.translation
        // 对唱对齐方式，有性能问题放弃
        // val dule = extraData.extra?.getBoolean(KEY_DUTE,false)?:false

        val picInfo = if (lefttext.length <= 6 ){
            IslandApi.PicInfo(
                pic = "miui.focus.icon",
            )
        } else {
            null
        }
        val textInfoLeft = IslandApi.TextInfo(
            title = lefttext
        )
        val textInfoRight = IslandApi.TextInfo(
            title = righttext?:"群里有猫娘"
        )
        val left = IslandApi.ImageTextInfo(
            picInfo = picInfo,
            textInfo = textInfoLeft
        )

        val right = IslandApi.ImageTextInfo(
            textInfo =  textInfoRight,
            type = 2
        )

        val bigIsland = IslandApi.BigIslandArea(
            imageTextInfoLeft = left,
            imageTextInfoRight = right

        )

        val smallIsland = IslandApi.SmallIslandArea(
            picInfo = picInfo
        )

        val Island = IslandApi.IslandTemplate(
            islandOrder = true,
            bigIslandArea = bigIsland,
            smallIslandArea = smallIsland
        )

        val iconsAdd = Bundle()
        iconsAdd.putParcelable("miui.focus.icon",icon)

        // 需要重启音乐软件生效
        val pendingIntent = if (isClickClock) {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_MUTABLE)
        }
        builder.setContentTitle(text)
            .setSmallIcon(IconCompat.createWithBitmap(bitmap))
            .setTicker(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        fun buildRemoteViews(): RemoteViews {
            val layoutId = modRes.getIdentifier("focuslyric_layout", "layout", ProjectApi.mAppModulePkg)
            val textId = modRes.getIdentifier("focuslyric", "id", ProjectApi.mAppModulePkg)
            val tf_text_id = modRes.getIdentifier("focustflyric", "id", ProjectApi.mAppModulePkg)
            return RemoteViews(ProjectApi.mAppModulePkg, layoutId).apply {
                if (tf != null){
                    setViewVisibility(tf_text_id, View.VISIBLE)
                    setTextViewText(tf_text_id, tf)
                    setTextViewTextSize(tf_text_id, TypedValue.COMPLEX_UNIT_SP, nSize)
                } else {
                    setViewVisibility(tf_text_id, View.GONE)
                }
                setTextViewText(textId, text)
                setTextViewTextSize(textId, TypedValue.COMPLEX_UNIT_SP, nSize)
            }
        }

        fun buildRemoteViewsIsLand(): RemoteViews {
            val layoutId = modRes.getIdentifier("focuslyricisland_layout", "layout", ProjectApi.mAppModulePkg)
            val textId = modRes.getIdentifier("focuslyric", "id", ProjectApi.mAppModulePkg)
            val tf_text_id = modRes.getIdentifier("focustflyric", "id", ProjectApi.mAppModulePkg)
            return RemoteViews(ProjectApi.mAppModulePkg, layoutId).apply {
                if (tf != null){
                    setViewVisibility(tf_text_id, View.VISIBLE)
                    setTextViewText(tf_text_id, tf)
                    setTextViewTextSize(tf_text_id, TypedValue.COMPLEX_UNIT_SP, nSize)
                } else {
                    setViewVisibility(tf_text_id, View.GONE)
                }
                setTextViewText(textId, text)
                setTextViewTextSize(textId, TypedValue.COMPLEX_UNIT_SP, nSize)
            }
        }

        fun buildAodRemoteViews(textColor: Int): RemoteViews {
            val layoutId = modRes.getIdentifier("focusaodlyric_layout", "layout", ProjectApi.mAppModulePkg)
            val textId = modRes.getIdentifier("focuslyric", "id", ProjectApi.mAppModulePkg)
            val iconId = modRes.getIdentifier("focusicon", "id", ProjectApi.mAppModulePkg)
            val tf_text_id = modRes.getIdentifier("focustflyric", "id", ProjectApi.mAppModulePkg)
            return RemoteViews(ProjectApi.mAppModulePkg, layoutId).apply {
                if (tf != null){
                    setViewVisibility(tf_text_id, View.VISIBLE)
                    setTextViewText(tf_text_id, tf)
                    setTextColor(tf_text_id, textColor)
                    setTextViewTextSize(tf_text_id, TypedValue.COMPLEX_UNIT_SP, nSize)
                } else {
                    setViewVisibility(tf_text_id, View.GONE)
                }
                setTextViewText(textId, text)
                setTextColor(textId, textColor)
                setTextViewTextSize(textId, TypedValue.COMPLEX_UNIT_SP, nSize)
                setTextViewTextSize(textId, TypedValue.COMPLEX_UNIT_SP, nSize)
                setImageViewBitmap(iconId, icon.loadDrawable(context)?.toBitmap())
            }
        }

        runCatching {
            val remoteViewsDay = buildRemoteViews()
            val remoteViewsAod = buildAodRemoteViews(Color.WHITE)
            val remoteViewsIsLand = buildRemoteViewsIsLand()

            val api = if (!isAodShow) {
                if (isAodMode) {
                    FocusApi.senddiyFocus(
                        addpics = iconsAdd,
                        ticker = text,
                        island = Island,
                        updatable = true,
                        rvAod = remoteViewsAod,
                        rvIsLand = remoteViewsIsLand,
                        enableFloat = false,
                        rv = remoteViewsDay,
                        timeout = 999999,
                        picticker = icon,
                        pictickerdark = dartIcon
                    )
                } else {
                    FocusApi.senddiyFocus(
                        addpics = iconsAdd,
                        ticker = text,
                        island = Island,
                        rvIsLand = remoteViewsIsLand,
                        updatable = true,
                        aodPic = icon,
                        aodTitle = text,
                        enableFloat = false,
                        rv = remoteViewsDay,
                        timeout = 999999,
                        picticker = icon,
                        pictickerdark = dartIcon
                    )
                }
            } else {
                FocusApi.senddiyFocus(
                    addpics = iconsAdd,
                    ticker = text,
                    rvIsLand = remoteViewsIsLand,
                    island = Island,
                    updatable = true,
                    enableFloat = false,
                    rv = remoteViewsDay,
                    timeout = 999999,
                    picticker = icon,
                    pictickerdark = dartIcon
                )
            }
            builder.addExtras(api)
            builder.extras.putString("app_package", extraData.packageName)
            val notification = builder.build()
            (context.getSystemService("notification") as NotificationManager)
                .notify(CHANNEL_ID.hashCode(), notification)
        }.onFailure {
            logE(TAG, lpparam.packageName, "send focus failed, ${it.message}")
            val baseinfo = FocusApi.baseinfo(
                basetype = 1,
                title = text
            )
            val api = if (!isAodShow) {
                FocusApi.sendFocus(
                    addpics = iconsAdd,
                    ticker = text,
                    aodTitle = text,
                    aodPic = icon,
                    island = Island,
                    baseInfo = baseinfo,
                    updatable = true,
                    enableFloat = false,
                    timeout = 999999,
                    picticker = icon,
                    pictickerdark = dartIcon
                )
            } else {
                FocusApi.sendFocus(
                    addpics = iconsAdd,
                    ticker = text,
                    island = Island,
                    baseInfo = baseinfo,
                    updatable = true,
                    enableFloat = false,
                    timeout = 999999,
                    picticker = icon,
                    pictickerdark = dartIcon
                )
            }
            builder.addExtras(api)
            builder.extras.putString("app_package", extraData.packageName)
            val notification = builder.build()
            (context.getSystemService("notification") as NotificationManager)
                .notify(CHANNEL_ID.hashCode(), notification)
        }
    }

    private fun createNotificationChannel() {
        val modRes = OtherTool.getModuleRes(context)
        val notificationManager = context.getSystemService("notification") as NotificationManager
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            modRes.getString(R.string.system_ui_statusbar_music_notification),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.setSound(null, null)
        notificationManager.createNotificationChannel(notificationChannel)
    }


    @SuppressLint("NotificationPermission")
    fun cancelNotification() {
        (context.getSystemService("notification") as NotificationManager).cancel(CHANNEL_ID.hashCode())
    }

    /**
     *
     * @param [base64] 图片的 Base64
     * @return [android.graphics.Bitmap] 返回图片的 Bitmap?，传入 Base64 无法转换则为 null
     */
    private fun base64ToDrawable(base64: String): Bitmap? {
        return try {
            val bitmapArray: ByteArray = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.size)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val CHANNEL_ID: String = "channel_id_focusNotifLyrics"
    }

    /**
     * 拆分字符串
     * @param input 输入字符串
     * @param config 拆字配置
     * @return 拆分后的两个字符串 */
    fun splitSmart(input: String, config: SplitConfig): Pair<String, String?> {
        if (input.isEmpty()) return "" to null

        val tokens = tokenize(input, config.pairedSymbols)
        val logicalLen = tokens.size
        val raw = tokens.joinToString("") { it.text }

        // 1. 不超过 maxLength → 尝试按空格切，否则不切
        if (logicalLen <= config.maxLength) {
            return splitBySpaceOrNone(tokens, raw, raw.length / 2, config)
        }

        // 2. 超过 maxLength → 在 maxLength 附近切
        val splitIndex = config.maxLength
        val approxCharIndex = tokens.take(splitIndex).sumOf { it.text.length }

        return splitBySpaceOrNone(tokens, raw, approxCharIndex, config)
    }

    /**
     * 根据近似字符索引拆分 Token 列表
     * @param tokens Token 列表
     * @param raw 原始字符串
     * @param approxCharIndex 近似字符索引
     * @param config 拆字配置
     * @return 拆分后的两个字符串 */
    private fun splitBySpaceOrNone(
        tokens: List<Token>,
        raw: String,
        approxCharIndex: Int,
        config: SplitConfig
    ): Pair<String, String?> {
        val left = raw.lastIndexOf(' ', approxCharIndex)
        val right = raw.indexOf(' ', approxCharIndex)

        val leftValid = left != -1 && (approxCharIndex - left) <= config.lookahead
        val rightValid = right != -1 && (right - approxCharIndex) <= config.lookahead

        val chosenCharIndex = when {
            // 优先空格，但如果左边长度超过 maxLength → 放弃空格，用 maxLength
            leftValid -> {
                val leftLen = left
                if (leftLen > config.maxLength) approxCharIndex else left
            }
            rightValid -> right
            else -> approxCharIndex
        }

        val (firstTokens, secondTokens) = splitTokensByCharIndex(tokens, chosenCharIndex)

        var first = firstTokens.joinToString("") { it.text }
        var second = secondTokens.joinToString("") { it.text }.ifEmpty { null }

        // 处理 keepSpaceInSecond
        if (!config.keepSpaceInSecond && second != null && second.startsWith(" ")) {
            second = second.trimStart()
        }

        // 避免过短
        val minLen = (raw.length * config.minFraction).toInt()
        if (first.length < minLen || (second?.length ?: 0) < minLen) {
            val mid = tokens.size / 2
            first = tokens.take(mid).joinToString("") { it.text }
            second = tokens.drop(mid).joinToString("") { it.text }
        }

        return first to second
    }
    /**
     * 根据字符索引拆分 Token 列表
     * @param tokens Token 列表
     * @param charIndex 字符索引
     * @return 拆分后的两个 Token 列表 */
    private fun splitTokensByCharIndex(
        tokens: List<Token>,
        charIndex: Int
    ): Pair<List<Token>, List<Token>> {
        var acc = 0
        for ((i, token) in tokens.withIndex()) {
            val nextAcc = acc + token.text.length
            if (charIndex < nextAcc) {
                return if (charIndex == acc) {
                    tokens.subList(0, i) to tokens.subList(i, tokens.size)
                } else {
                    tokens.subList(0, i + 1) to tokens.subList(i + 1, tokens.size)
                }
            }
            acc = nextAcc
        }
        return tokens to emptyList()
    }

    /**
     * 将输入字符串拆分为 Token 列表
     * @param input 输入字符串
     * @param pairs 括号对 */
    private fun tokenize(input: String, pairs: Map<Char, Char>): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            val next = if (i + 1 < input.length) input[i + 1] else null
            if (next != null && pairs[c] == next) {
                tokens.add(Token("$c$next"))
                i += 2
            } else {
                tokens.add(Token(c.toString()))
                i += 1
            }
        }
        return tokens
    }
}


/**
 * 拆字配置
 * @param maxLength 多少字符开始拆
 * @param lookahead 在拆分点前后多少字符内找空格
 * @param minFraction 最小比例（避免过短）
 * @param keepSpaceInSecond 是否保留空格
 * @param pairedSymbols 括号对
 * */
data class SplitConfig(
    val maxLength: Int,
    val lookahead: Int = 2,
    val minFraction: Double = 0.35,
    val keepSpaceInSecond: Boolean = true,
    val pairedSymbols: Map<Char, Char> = mapOf(
        '(' to ')',
        '[' to ']',
        '{' to '}',
        '《' to '》',
        '“' to '”',
        '‘' to '’',
        '「' to '」',
        '『' to '』'
    )
)

data class Token(val text: String)


