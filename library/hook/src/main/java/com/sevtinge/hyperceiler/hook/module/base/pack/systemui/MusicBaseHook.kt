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
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.media.session.PlaybackState
import android.os.Bundle
import android.util.Base64
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
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
import org.json.JSONObject
import kotlin.math.min

abstract class MusicBaseHook : BaseHook() {
    val context: Application by lazy { AndroidAppHelper.currentApplication() }
    private val nSize: Float by lazy { mPrefsMap.getInt("system_ui_statusbar_music_size_n", 15).toFloat() }
    private val hideAodShow: Boolean by lazy { mPrefsMap.getBoolean("system_ui_statusbar_music_hide_aod") }
    private val isAodMode: Boolean by lazy { mPrefsMap.getBoolean("system_ui_statusbar_music_show_aod_mode") }
    private val isShowNotific by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_music_show_notific")
    }
    private val receiver = object : ISuperLyric.Stub() {
        override fun onSuperLyric(data: SuperLyricData) {
            runCatching { this@MusicBaseHook.onSuperLyric(data) }
                .onFailure { logE(TAG, lpparam.packageName, it) }
        }

        override fun onStop(data: SuperLyricData) {
            runCatching {
                if (data.playbackState?.state == PlaybackState.STATE_BUFFERING) return
                this@MusicBaseHook.onStop()
            }.onFailure { logE(TAG, lpparam.packageName, it) }
        }
    }

    init {
        loadClass("android.app.Application").methodFinder().filterByName("onCreate").first()
            .createAfterHook {
                runCatching {
                    SuperLyricTool.registerSuperLyric(context, receiver)
                    // if (isDebug()) logD(TAG, lpparam.packageName, "registerLyricListener")
                }.onFailure {
                    logE(TAG, lpparam.packageName, "registerLyricListener not found: ${it.message}")
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
        val (musicAppName, launchIntent) = resolveAppNameAndLaunchIntent(extraData.packageName)

        // 原始图标 (用于通知和 FocusApi 的 picticker 等)
        val baseBitmap = base64ToBitmap(extraData.base64Icon)
        val activityIconBitmap = runCatching { launchIntent?.let { context.packageManager.getActivityIcon(it).toBitmap() } }.getOrNull()
        val primaryBitmap = baseBitmap ?: activityIconBitmap ?: createEmptyBitmapFallback()
        val icon = Icon.createWithBitmap(primaryBitmap).apply { if (baseBitmap != null) setTint(Color.WHITE) }
        val darkIcon = Icon.createWithBitmap(primaryBitmap).apply { if (baseBitmap != null) setTint(Color.BLACK) }

        // 拆分文字
        val (leftText, rightText) = splitSmart(text, SplitConfig(maxLength = 6))

        // Notification builder
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(text)
            .setSmallIcon(IconCompat.createWithBitmap(primaryBitmap))
            .setTicker(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(createPendingIntent(isClickClock, launchIntent))

        // Island template
        val islandTemplate = buildIslandTemplate(modRes, leftText, rightText, musicAppName, text)

        // icons bundle: NOTE - only the "miui.focus.icon" entry should be scaled to 60% and circle-cropped.
        val circularScaledIconBitmap =
            if (baseBitmap == null) circleCropBitmap(primaryBitmap) else primaryBitmap
        val circularIconForBundle = Icon.createWithBitmap(circularScaledIconBitmap)
            .apply { if (baseBitmap != null) setTint(Color.WHITE) }

        val iconsAdd = Bundle().apply {
            putParcelable("miui.focus.icon", circularIconForBundle)
            putParcelable(
                "miui.focus.share_icon",
                Icon.createWithBitmap(activityIconBitmap)
            )// unchanged
        }

        val tf = extraData.translation

        // senddiyFocus first, fallback to sendFocus
        runCatching {
            // RemoteViews
            val remoteDay = buildRemoteViews(modRes, tf, text)
            val remoteAod = buildAodRemoteViews(modRes, tf, text, Color.WHITE, icon)
            val remoteIsland = buildRemoteViewsIsland(modRes, tf, text)

            val api = when {
                !hideAodShow && isAodMode -> FocusApi.sendDiyFocus(
                    addpics = iconsAdd,
                    islandFirstFloat = false,
                    ticker = text,
                    island = islandTemplate,
                    updatable = true,
                    rvAod = remoteAod,
                    rvIsLand = remoteIsland,
                    enableFloat = false,
                    rv = remoteDay,
                    timeout = 999_999,
                    picticker = icon,
                    isShowNotification = isShowNotific,
                    pictickerdark = darkIcon
                )

                !hideAodShow && !isAodMode -> FocusApi.sendDiyFocus(
                    isShowNotification = isShowNotific,
                    addpics = iconsAdd,
                    islandFirstFloat = false,
                    ticker = text,
                    island = islandTemplate,
                    rvIsLand = remoteIsland,
                    updatable = true,
                    aodPic = icon,
                    aodTitle = text,
                    enableFloat = false,
                    rv = remoteDay,
                    timeout = 999_999,
                    picticker = icon,
                    pictickerdark = darkIcon
                )

                else -> FocusApi.sendDiyFocus(
                    isShowNotification = isShowNotific,
                    islandFirstFloat = false,
                    addpics = iconsAdd,
                    ticker = text,
                    rvIsLand = remoteIsland,
                    island = islandTemplate,
                    updatable = true,
                    enableFloat = false,
                    rv = remoteDay,
                    timeout = 999_999,
                    picticker = icon,
                    pictickerdark = darkIcon
                )
            }

            builder.addExtras(api)
            builder.extras.putString("app_package", extraData.packageName)
            val notification = builder.build()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(CHANNEL_ID.hashCode(), notification)
        }.onFailure { e ->
            logE(TAG, lpparam.packageName, "send diy focus failed: ${e.message}")
            runCatching {
                val baseinfo = FocusApi.baseinfo(basetype = 1, title = text)
                val apiFallback = if (!hideAodShow) {
                    FocusApi.sendFocus(
                        addpics = iconsAdd,
                        ticker = text,
                        aodTitle = text,
                        aodPic = icon,
                        island = islandTemplate,
                        baseInfo = baseinfo,
                        updatable = true,
                        islandFirstFloat = false,
                        enableFloat = false,
                        timeout = 999_999,
                        picticker = icon,
                        pictickerdark = darkIcon
                    )
                } else {
                    FocusApi.sendFocus(
                        islandFirstFloat = false,
                        addpics = iconsAdd,
                        ticker = text,
                        island = islandTemplate,
                        baseInfo = baseinfo,
                        updatable = true,
                        enableFloat = false,
                        timeout = 999_999,
                        picticker = icon,
                        pictickerdark = darkIcon
                    )
                }
                builder.addExtras(apiFallback)
                builder.extras.putString("app_package", extraData.packageName)
                val notification = builder.build()
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(CHANNEL_ID.hashCode(), notification)
            }.onFailure {
                logE(TAG, lpparam.packageName, "fallback send focus failed: ${it.message}")
            }
        }
    }

    private fun createPendingIntent(isClickClock: Boolean, launchIntent: Intent?): PendingIntent? {
        val intent = Intent("$CHANNEL_ID.actions.switchClockStatus")
        return if (isClickClock) {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            launchIntent?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_MUTABLE) }
        }
    }

    private fun resolveAppNameAndLaunchIntent(packageName: String?): Pair<String, Intent?> {
        if (packageName == null) return ("unknown" to null)
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val label = context.packageManager.getApplicationLabel(appInfo).toString()
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            label to launchIntent
        } catch (e: Exception) {
            logE(TAG, e)
            packageName to context.packageManager.getLaunchIntentForPackage(packageName)
        }
    }

    private fun buildIslandTemplate(modRes: Resources, leftText: String, rightText: String?, musicAppName: String, originalText: String): JSONObject {
        val shareData = IslandApi.shareData(
            title = modRes.getString(R.string.system_ui_statusbar_music_share),
            content = modRes.getString(R.string.system_ui_statusbar_music_send),
            sharePic = "miui.focus.share_icon",
            pic = "miui.focus.share_icon",
            shareContent = modRes.getString(R.string.system_ui_statusbar_music_send_share_text, musicAppName, originalText)
        )

        val picInfo = if (leftText.length <= 6) IslandApi.picInfo(pic = "miui.focus.icon") else null

        val left = IslandApi.imageTextInfo(
            picInfo = picInfo,
            textInfo = IslandApi.TextInfo(title = leftText)
        )
        val right = IslandApi.imageTextInfo(
            textInfo = IslandApi.TextInfo(title = rightText ?: ""/*"群里有猫娘"*/),
            type = 2
        )
        val bigIsland =
            IslandApi.bigIslandArea(imageTextInfoLeft = left, imageTextInfoRight = right)
        val smallIsland =
            IslandApi.SmallIslandArea(picInfo = IslandApi.picInfo(pic = "miui.focus.icon"))
        return IslandApi.IslandTemplate(
            shareData = shareData,
            bigIslandArea = bigIsland,
            smallIslandArea = smallIsland
        )
    }

    private fun buildRemoteViews(modRes: Resources, tf: String?, text: String): RemoteViews {
        val layoutId = modRes.getIdentifier("focuslyric_layout", "layout", ProjectApi.mAppModulePkg)
        val textId = modRes.getIdentifier("focuslyric", "id", ProjectApi.mAppModulePkg)
        val tfTextId = modRes.getIdentifier("focustflyric", "id", ProjectApi.mAppModulePkg)
        return RemoteViews(ProjectApi.mAppModulePkg, layoutId).apply {
            if (!tf.isNullOrEmpty()) {
                setViewVisibility(tfTextId, View.VISIBLE)
                setTextViewText(tfTextId, tf)
                setTextViewTextSize(tfTextId, TypedValue.COMPLEX_UNIT_SP, nSize)
            } else {
                setViewVisibility(tfTextId, View.GONE)
            }
            setTextViewText(textId, text)
            setTextViewTextSize(textId, TypedValue.COMPLEX_UNIT_SP, nSize)
        }
    }

    private fun buildRemoteViewsIsland(modRes: Resources, tf: String?, text: String): RemoteViews {
        val layoutId = modRes.getIdentifier("focuslyricisland_layout", "layout", ProjectApi.mAppModulePkg)
        val textId = modRes.getIdentifier("focuslyric", "id", ProjectApi.mAppModulePkg)
        val tfTextId = modRes.getIdentifier("focustflyric", "id", ProjectApi.mAppModulePkg)
        return RemoteViews(ProjectApi.mAppModulePkg, layoutId).apply {
            if (!tf.isNullOrEmpty()) {
                setViewVisibility(tfTextId, View.VISIBLE)
                setTextViewText(tfTextId, tf)
                setTextViewTextSize(tfTextId, TypedValue.COMPLEX_UNIT_SP, nSize)
            } else {
                setViewVisibility(tfTextId, View.GONE)
            }
            setTextViewText(textId, text)
            setTextViewTextSize(textId, TypedValue.COMPLEX_UNIT_SP, nSize)
        }
    }

    private fun buildAodRemoteViews(modRes: Resources, tf: String?, text: String, textColor: Int, icon: Icon): RemoteViews {
        val layoutId = modRes.getIdentifier("focusaodlyric_layout", "layout", ProjectApi.mAppModulePkg)
        val textId = modRes.getIdentifier("focuslyric", "id", ProjectApi.mAppModulePkg)
        val iconId = modRes.getIdentifier("focusicon", "id", ProjectApi.mAppModulePkg)
        val tfTextId = modRes.getIdentifier("focustflyric", "id", ProjectApi.mAppModulePkg)
        return RemoteViews(ProjectApi.mAppModulePkg, layoutId).apply {
            if (!tf.isNullOrEmpty()) {
                setViewVisibility(tfTextId, View.VISIBLE)
                setTextViewText(tfTextId, tf)
                setTextColor(tfTextId, textColor)
                setTextViewTextSize(tfTextId, TypedValue.COMPLEX_UNIT_SP, nSize)
            } else {
                setViewVisibility(tfTextId, View.GONE)
            }
            setTextViewText(textId, text)
            setTextColor(textId, textColor)
            setTextViewTextSize(textId, TypedValue.COMPLEX_UNIT_SP, nSize)
            setImageViewBitmap(iconId, icon.loadDrawable(context)?.toBitmap())
        }
    }

    private fun createNotificationChannel() {
        val modRes = OtherTool.getModuleRes(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val name = runCatching { modRes.getString(R.string.system_ui_statusbar_music_notification) }.getOrDefault("Focus Notification")
        val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT).apply { setSound(null, null) }
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("NotificationPermission")
    fun cancelNotification() {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(CHANNEL_ID.hashCode())
    }

    /**
     *
     * @param [base64] 图片的 Base64
     * @return [android.graphics.Bitmap] 返回图片的 Bitmap?，传入 Base64 无法转换则为 null
     */
    private fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun createEmptyBitmapFallback(): Bitmap {
        return createBitmap(1, 1)
    }

    private fun scaleBitmap(src: Bitmap, scale: Float): Bitmap {
        val w = maxOf(1, (src.width * scale).toInt())
        val h = maxOf(1, (src.height * scale).toInt())
        return src.scale(w, h)
    }

    private fun circleCropBitmap(src: Bitmap): Bitmap {
        // center-crop to a square first
        val size = min(src.width, src.height)
        val x = (src.width - size) / 2
        val y = (src.height - size) / 2
        val squared = Bitmap.createBitmap(src, x, y, size, size)

        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        return output
    }

    companion object {
        const val CHANNEL_ID: String = "channel_id_focusNotifLyrics"
    }

    fun splitSmart(input: String, config: SplitConfig): Pair<String, String?> {
        if (input.isEmpty()) return "" to null

        val tokens = tokenize(input, config.pairedSymbols)
        val raw = tokens.joinToString("") { it.text }
        val logicalLen = tokens.size

        if (logicalLen <= config.maxLength) {
            return splitBySpaceOrNone(tokens, raw, raw.length / 2, config)
        }

        val approxCharIndex = tokens.take(config.maxLength).sumOf { it.text.length }
        return splitBySpaceOrNone(tokens, raw, approxCharIndex, config)
    }

    private fun splitBySpaceOrNone(tokens: List<Token>, raw: String, approxCharIndex: Int, config: SplitConfig): Pair<String, String?> {
        val left = raw.lastIndexOf(' ', approxCharIndex)
        val right = raw.indexOf(' ', approxCharIndex)

        val leftValid = left != -1 && (approxCharIndex - left) <= config.lookahead
        val rightValid = right != -1 && (right - approxCharIndex) <= config.lookahead

        val chosenCharIndex = when {
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

        if (!config.keepSpaceInSecond && second != null && second.startsWith(" ")) {
            second = second.trimStart()
        }

        val minLen = (raw.length * config.minFraction).toInt()
        if (first.length < minLen || (second?.length ?: 0) < minLen) {
            val mid = tokens.size / 2
            first = tokens.take(mid).joinToString("") { it.text }
            second = tokens.drop(mid).joinToString("") { it.text }
        }

        return first to second
    }

    private fun splitTokensByCharIndex(tokens: List<Token>, charIndex: Int): Pair<List<Token>, List<Token>> {
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

    private fun tokenize(input: String, pairs: Map<Char, Char>): List<Token> {
        val tokens = ArrayList<Token>(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            val next = if (i + 1 < input.length) input[i + 1] else null
            if (next != null && pairs[c] == next) {
                tokens.add(Token("$c$next"))
                i += 2
            } else {
                tokens.add(Token(c.toString()))
                i++
            }
        }
        return tokens
    }
}

/**
 * 拆字配置
 */
data class SplitConfig(
    val maxLength: Int,
    val lookahead: Int = 2,
    val minFraction: Double = 0.45,
    val keepSpaceInSecond: Boolean = false,
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
