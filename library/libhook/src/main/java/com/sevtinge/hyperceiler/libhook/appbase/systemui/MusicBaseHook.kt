/** This file is part of HyperCeiler.

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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.appbase.systemui

import android.annotation.SuppressLint
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
import com.hchen.superlyricapi.ISuperLyric
import com.hchen.superlyricapi.SuperLyricData
import com.hchen.superlyricapi.SuperLyricTool
import com.hyperfocus.api.FocusApi
import com.hyperfocus.api.IslandApi
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import org.json.JSONObject
import kotlin.math.min

/**
 * 音乐歌词通知基础 Hook
 *
 * 负责接收歌词数据并通过 Focus 通知展示
 *
 * @see SuperLyricData 歌词数据结构
 * @see FocusApi Focus 通知 API
 */
abstract class MusicBaseHook : BaseHook() {

    companion object {
        const val CHANNEL_ID: String = "channel_id_focusNotifLyrics"
        private const val NOTIFICATION_TIMEOUT = 999999
        private const val DEFAULT_FONT_SIZE = 15
        private const val PENDING_INTENT_REQUEST_CODE = 0
        private const val SALT_MUSIC_PACKAGE = "com.salt.music"}

    val context: Context by lazy { EzXposed.appContext }

    private val nSize: Float by lazy {
        mPrefsMap.getInt("system_ui_statusbar_music_size_n", DEFAULT_FONT_SIZE).toFloat()
    }
    private val hideAodShow: Boolean by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_music_hide_aod")
    }
    private val isAodMode: Boolean by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_music_show_aod_mode")
    }
    private val isShowApp by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_music_show_app")
    }
    private val isClickClock by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_music_click_clock")
    }
    private val narrowFontMode: Boolean by lazy { mPrefsMap.getBoolean("system_ui_statusbar_music_narrow_font") }

    private val isShowNotific by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_music_show_notific", true)
    }
    private val isFallbackFocusNotification by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_music_fallback_focus_notification", false)
    }

    private val circlePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG)
    }

    // 缓存资源 ID
    private val resourceIds by lazy {
        val modRes = AppsTool.getModuleRes(context)
        ResourceIds(
            focuslyricLayout = modRes.getIdentifier("focuslyric_layout", "layout", ProjectApi.mAppModulePkg),
            focuslyricIslandLayout = modRes.getIdentifier("focuslyricisland_layout", "layout", ProjectApi.mAppModulePkg),
            focusaodlyricLayout = modRes.getIdentifier("focusaodlyric_layout", "layout", ProjectApi.mAppModulePkg),
            focuslyricId = modRes.getIdentifier("focuslyric", "id", ProjectApi.mAppModulePkg),
            focusiconId = modRes.getIdentifier("focusicon", "id", ProjectApi.mAppModulePkg),
            focustflyricId = modRes.getIdentifier("focustflyric", "id", ProjectApi.mAppModulePkg)
        )
    }

    private val modRes: Resources by lazy {
        AppsTool.getModuleRes(context)
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val channelCreated: Boolean by lazy {
        createNotificationChannel()
        true
    }

    private val receiver = object : ISuperLyric.Stub() {
        override fun onSuperLyric(data: SuperLyricData) {
            runCatching { this@MusicBaseHook.onSuperLyric(data) }
                .onFailure { XposedLog.e(TAG, lpparam.packageName, it) }
        }

        override fun onStop(data: SuperLyricData) {
            runCatching {
                if (data.playbackState?.state == PlaybackState.STATE_BUFFERING) return
                this@MusicBaseHook.onStop()
            }.onFailure { XposedLog.e(TAG, lpparam.packageName, it) }
        }
    }

    init {
        EzxHelpUtils.runOnApplicationAttach { context ->
            runCatching {
                SuperLyricTool.registerSuperLyric(context, receiver)
            }.onFailure {
                XposedLog.e(TAG, lpparam.packageName, "registerLyricListener not found: ${it.message}")
            }
        }
    }

    abstract fun onSuperLyric(data: SuperLyricData)
    abstract fun onStop()

    /**
     * 发送歌词通知
     *
     * @param text 歌词文本
     * @param extraData 歌词附加数据
     */
    @SuppressLint("NotificationPermission", "LaunchActivityFromNotification")
    fun sendNotification(text: String, extraData: SuperLyricData) {
        // 确保 Channel 已创建
        channelCreated

        val (musicAppName, launchIntent) = resolveAppNameAndLaunchIntent(extraData.packageName)

        // 准备图标
        val iconBundle = prepareIcons(extraData, launchIntent)

        // 拆分文字
        val (leftText, rightText) = splitSmart(text, SplitConfig(maxLength = 6))

        // Notification builder
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Xiaomi Focus Lyric")
            .setContentText(text)
            .setSmallIcon(IconCompat.createWithBitmap(iconBundle.primaryBitmap))
            .setTicker(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(createPendingIntent(launchIntent))

        // Island template
        val islandTemplate = buildIslandTemplate(leftText, rightText, musicAppName, text)

        val tf = extraData.translation

        // 发送通知
        sendFocusNotification(builder, text, tf, iconBundle, islandTemplate, extraData.packageName)
    }

    /**
     * 创建 PendingIntent
     */
    private fun createPendingIntent(launchIntent: Intent?): PendingIntent? {
        return if (isClickClock) {
            val intent = Intent("$CHANNEL_ID.actions.switchClockStatus")
            PendingIntent.getBroadcast(context, PENDING_INTENT_REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            launchIntent?.let {
                PendingIntent.getActivity(context, PENDING_INTENT_REQUEST_CODE, it, PendingIntent.FLAG_MUTABLE)
            }
        }
    }

    /**
     * 解析应用名称和启动 Intent
     */
    private fun resolveAppNameAndLaunchIntent(packageName: String?): Pair<String, Intent?> {
        packageName ?: return "unknown" to null

        return runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            label to launchIntent
        }.getOrElse { e ->
            XposedLog.e(TAG, e)
            packageName to context.packageManager.getLaunchIntentForPackage(packageName)
        }
    }

    /**
     * 准备图标资源
     */
    private fun prepareIcons(extraData: SuperLyricData, launchIntent: Intent?): IconBundle {
        val baseBitmap = base64ToBitmap(extraData.base64Icon)
        val activityIconBitmap = launchIntent?.let {
            runCatching { context.packageManager.getActivityIcon(it).toBitmap() }.getOrNull()
        }

        val isSaltMusic = extraData.packageName == SALT_MUSIC_PACKAGE
        val primaryBitmap = when {
            isSaltMusic -> activityIconBitmap
            else -> baseBitmap ?: activityIconBitmap
        } ?: createEmptyBitmapFallback()

        val hasTint = baseBitmap != null
        val circularBitmap = if (hasTint) primaryBitmap else circleCropBitmap(primaryBitmap)

        return IconBundle(
            primaryBitmap = primaryBitmap,
            icon = primaryBitmap.toIcon(if (hasTint) Color.WHITE else null),
            darkIcon = primaryBitmap.toIcon(if (hasTint) Color.BLACK else null),
            circularIcon = circularBitmap.toIcon(if (hasTint) Color.WHITE else null),
            activityIcon = activityIconBitmap,
            hasTint = hasTint
        )
    }

    /**
     * Bitmap 转 Icon 扩展函数
     */
    private fun Bitmap.toIcon(tintColor: Int? = null): Icon {
        return Icon.createWithBitmap(this).apply {
            tintColor?.let { setTint(it) }
        }
    }

    /**
     * 创建图标 Bundle
     */
    private fun createIconsBundle(iconBundle: IconBundle): Bundle = Bundle().apply {
        putParcelable("miui.focus.icon", iconBundle.circularIcon)
        putParcelable("miui.focus.share_icon", Icon.createWithBitmap(iconBundle.activityIcon))
        if (!isShowApp) putParcelable("miui.appIcon", iconBundle.primaryBitmap)
    }

    /**
     * 发送 Focus 通知
     */
    private fun sendFocusNotification(
        builder: NotificationCompat.Builder,
        text: String,
        tf: String?,
        iconBundle: IconBundle,
        islandTemplate: JSONObject,
        packageName: String?
    ) {
        val iconsAdd = createIconsBundle(iconBundle)

        if (!isFallbackFocusNotification) {
            runCatching {
                val remoteDay = buildRemoteViews(tf, text, RemoteViewType.DAY)
                val remoteIsland = buildRemoteViews(tf, text, RemoteViewType.ISLAND)

                val focusExtras = buildFocusExtras(
                    text, tf, iconBundle, islandTemplate, iconsAdd, remoteDay, remoteIsland
                )

                postNotification(builder, focusExtras, packageName)
            }.onFailure { e ->
                XposedLog.w(TAG, lpparam.packageName, "send diy focus failed: ${e.message}")
                sendFallbackNotification(
                    builder,
                    text,
                    tf,
                    iconBundle,
                    islandTemplate,
                    packageName,
                    iconsAdd
                )
            }
        } else {
            XposedLog.w(TAG, lpparam.packageName, "send Focus Fallback Notification")
            sendFallbackNotification(
                builder,
                text,
                tf,
                iconBundle,
                islandTemplate,
                packageName,
                iconsAdd
            )

        }
    }

    /**
     * 构建 Focus 通知扩展数据
     */
    private fun buildFocusExtras(
        text: String,
        tf: String?,
        iconBundle: IconBundle,
        islandTemplate: JSONObject,
        iconsAdd: Bundle,
        remoteDay: RemoteViews,
        remoteIsland: RemoteViews
    ): Bundle {
        return when {
            !hideAodShow && isAodMode -> {
                val remoteAod = buildRemoteViews(tf, text, RemoteViewType.AOD, iconBundle.icon)
                FocusApi.sendDiyFocus(
                    isShowNotification = isShowNotific,
                    addpics = iconsAdd,
                    islandFirstFloat = false,
                    ticker = text,
                    island = islandTemplate,
                    updatable = true,
                    rvAod = remoteAod,rvIsLand = remoteIsland,
                    enableFloat = false,
                    rv = remoteDay,
                    timeout = NOTIFICATION_TIMEOUT,
                    picticker = iconBundle.icon,
                    pictickerdark = iconBundle.darkIcon
                )
            }
            !hideAodShow && !isAodMode -> {
                FocusApi.sendDiyFocus(
                    isShowNotification = isShowNotific,
                    addpics = iconsAdd,
                    islandFirstFloat = false,
                    ticker = text,
                    island = islandTemplate,rvIsLand = remoteIsland,
                    updatable = true,
                    aodPic = iconBundle.icon,
                    aodTitle = text,
                    enableFloat = false,
                    rv = remoteDay,
                    timeout = NOTIFICATION_TIMEOUT,
                    picticker = iconBundle.icon,
                    pictickerdark = iconBundle.darkIcon
                )
            }
            else -> {
                FocusApi.sendDiyFocus(
                    addpics = iconsAdd,
                    islandFirstFloat = false,
                    ticker = text,
                    rvIsLand = remoteIsland,
                    island = islandTemplate,
                    updatable = true,
                    enableFloat = false,
                    rv = remoteDay,
                    timeout = NOTIFICATION_TIMEOUT,
                    picticker = iconBundle.icon,
                    pictickerdark = iconBundle.darkIcon
                )
            }
        }
    }

    /**
     * 发送焦点通知
     */
    private fun sendFallbackNotification(
        builder: NotificationCompat.Builder,
        text: String,
        tf: String?,
        iconBundle: IconBundle,
        islandTemplate: JSONObject,
        packageName: String?,
        iconsAdd: Bundle
    ) {
        runCatching {
            val baseinfo = FocusApi.baseinfo(
                basetype = if (tf == null) 1 else 2,
                title = text,
                content = tf
            )
            val apiFallback = if (!hideAodShow) {
                FocusApi.sendFocus(
                    addpics = iconsAdd,
                    ticker = text,
                    aodTitle = text,
                    aodPic = iconBundle.icon,
                    island = islandTemplate,
                    baseInfo = baseinfo,
                    updatable = true,
                    enableFloat = false,
                    timeout = NOTIFICATION_TIMEOUT,
                    picticker = iconBundle.icon,
                    pictickerdark = iconBundle.darkIcon
                )
            } else {
                FocusApi.sendFocus(
                    addpics = iconsAdd,
                    ticker = text,
                    island = islandTemplate,
                    baseInfo = baseinfo,
                    updatable = true,
                    enableFloat = false,
                    timeout = NOTIFICATION_TIMEOUT,
                    picticker = iconBundle.icon,
                    pictickerdark = iconBundle.darkIcon
                )
            }
            postNotification(builder, apiFallback, packageName)
        }.onFailure {
            XposedLog.e(TAG, lpparam.packageName, "fallback send focus failed: ${it.message}")
        }
    }

    /**
     * 发送通知
     */
    private fun postNotification(builder: NotificationCompat.Builder, extras: Bundle, packageName: String?) {
        builder.addExtras(extras)
        builder.extras.putString("app_package", packageName)
        val notification = builder.build()
        notificationManager.notify(CHANNEL_ID.hashCode(), notification)
    }

    /**
     * 构建 Island 模板
     */
    private fun buildIslandTemplate(
        leftText: String,
        rightText: String?,
        musicAppName: String,
        originalText: String
    ): JSONObject {
        val shareData = IslandApi.shareData(
            title = modRes.getString(R.string.system_ui_statusbar_music_share),
            content = modRes.getString(R.string.system_ui_statusbar_music_send),
            sharePic = "miui.focus.share_icon",
            pic = "miui.focus.share_icon",
            shareContent = modRes.getString(
                R.string.system_ui_statusbar_music_send_share_text,
                musicAppName,
                originalText
            )
        )

        val picInfo = if (leftText.length <= 6) IslandApi.picInfo(pic = "miui.focus.icon") else null

        val left = IslandApi.imageTextInfo(
            picInfo = picInfo,
            textInfo = IslandApi.TextInfo(title = leftText, narrowFont = narrowFontMode)
        )
        val right = IslandApi.imageTextInfo(
            textInfo = IslandApi.TextInfo(title = rightText.orEmpty(), narrowFont = narrowFontMode),
            type = 2
        )
        val bigIsland = IslandApi.bigIslandArea(imageTextInfoLeft = left, imageTextInfoRight = right)
        val smallIsland = IslandApi.SmallIslandArea(picInfo = IslandApi.picInfo(pic = "miui.focus.icon"))

        return IslandApi.IslandTemplate(
            shareData = shareData,
            bigIslandArea = bigIsland,
            smallIslandArea = smallIsland
        )
    }

    /**
     * 构建 RemoteViews
     */
    private fun buildRemoteViews(
        tf: String?,
        text: String,
        type: RemoteViewType,
        icon: Icon? = null
    ): RemoteViews {
        val layoutId = when (type) {
            RemoteViewType.DAY -> resourceIds.focuslyricLayout
            RemoteViewType.ISLAND -> resourceIds.focuslyricIslandLayout
            RemoteViewType.AOD -> resourceIds.focusaodlyricLayout}

        val isAod = type == RemoteViewType.AOD

        return RemoteViews(ProjectApi.mAppModulePkg, layoutId).apply {
            // 设置翻译文本
            if (!tf.isNullOrEmpty()) {
                setViewVisibility(resourceIds.focustflyricId, View.VISIBLE)
                setTextViewText(resourceIds.focustflyricId, tf)
                setTextViewTextSize(resourceIds.focustflyricId, TypedValue.COMPLEX_UNIT_SP, nSize)
                if (isAod) {
                    setTextColor(resourceIds.focustflyricId, Color.WHITE)
                }
            } else {
                setViewVisibility(resourceIds.focustflyricId, View.GONE)
            }

            // 设置主文本
            setTextViewText(resourceIds.focuslyricId, text)
            setTextViewTextSize(resourceIds.focuslyricId, TypedValue.COMPLEX_UNIT_SP, nSize)
            if (isAod) {
                setTextColor(resourceIds.focuslyricId, Color.WHITE)
            }

            // AOD 模式设置图标
            if (isAod && icon != null) {
                setImageViewBitmap(resourceIds.focusiconId, icon.loadDrawable(context)?.toBitmap())
            }
        }
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        val name = runCatching {
            modRes.getString(R.string.system_ui_statusbar_music_notification)
        }.getOrDefault("Focus Notification")

        val channel = NotificationChannel(
            CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setSound(null, null)
        }

        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 取消通知
     */
    @SuppressLint("NotificationPermission")
    fun cancelNotification() {
        notificationManager.cancel(CHANNEL_ID.hashCode())
    }

    /**
     * 将 Base64 字符串转换为 Bitmap
     */
    private fun base64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrEmpty()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    /**
     * 创建空白 Bitmap 作为降级方案
     */
    private fun createEmptyBitmapFallback(): Bitmap = createBitmap(1, 1)

    /**
     * 将 Bitmap 裁剪为圆形
     */
    /**
     * 将 Bitmap 裁剪为圆形
     */
    private fun circleCropBitmap(src: Bitmap): Bitmap {
        val size = min(src.width, src.height)
        val x = (src.width - size) / 2
        val y = (src.height - size) / 2
        val squared = Bitmap.createBitmap(src, x, y, size, size)

        val output = createBitmap(size, size)
        try {
            val canvas = Canvas(output)
            circlePaint.shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

            val radius = size / 2f
            canvas.drawCircle(radius, radius, radius, circlePaint)
        } finally {
            circlePaint.shader = null
            if (squared !== src) {
                squared.recycle()
            }
        }
        return output
    }

    /**
     * 智能分割文本
     *
     * @param input 输入文本
     * @param config 分割配置
     * @return 分割后的文本对
     */
    fun splitSmart(input: String, config: SplitConfig): Pair<String, String?> {
        if (input.isEmpty()) return "" to null

        val tokens = tokenize(input, config.pairedSymbols)
        val logicalLen = tokens.size

        if (logicalLen <= config.maxLength) {
            return splitBySpaceOrNone(tokens, input, input.length / 2, config)
        }

        val approxCharIndex = tokens.take(config.maxLength).sumOf { it.text.length }
        return splitBySpaceOrNone(tokens, input, approxCharIndex, config)
    }

    /**
     * 按空格或无分隔符分割
     */
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
            leftValid -> if (left > config.maxLength) approxCharIndex else left
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

    /**
     * 按字符索引分割 Token 列表
     */
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
     * 将输入字符串分词
     */
    private fun tokenize(input: String, pairs: Map<Char, Char>): List<Token> {
        val tokens = ArrayList<Token>(input.length)
        var i = 0

        while (i < input.length) {
            val c = input[i]
            val next = input.getOrNull(i + 1)
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
 * 资源 ID 缓存
 */
private data class ResourceIds(
    val focuslyricLayout: Int,
    val focuslyricIslandLayout: Int,
    val focusaodlyricLayout: Int,
    val focuslyricId: Int,
    val focusiconId: Int,
    val focustflyricId: Int
)

/**
 * 图标包
 */
private data class IconBundle(
    val primaryBitmap: Bitmap,
    val icon: Icon,
    val darkIcon: Icon,
    val circularIcon: Icon,
    val activityIcon: Bitmap?,
    val hasTint: Boolean
)

/**
 * RemoteView 类型
 */
private enum class RemoteViewType {
    DAY, ISLAND, AOD
}

/**
 * 拆字配置
 *
 * @param maxLength 最大长度
 * @param lookahead 前瞻距离
 * @param minFraction 最小分割比例
 * @param keepSpaceInSecond 是否保留第二部分开头的空格
 * @param pairedSymbols 成对符号映射
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
        '"' to '"',
        '\'' to '\'',
        '「' to '」',
        '『' to '』'
    )
)

/**
 * 分词 Token
 */
data class Token(val text: String)
