package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import com.sevtinge.hyperceiler.libhook.utils.api.BitmapUtils

internal object BrowserLinks {
    private val xiaomiBrowsers = setOf(
        "com.android.browser", "com.mi.globalbrowser", "com.miui.browser", "com.xiaomi.browser"
    )
    private val urlKeys = listOf("web_url", "url", "browser_fallback_url", "target_url", "link")
    val webProbe = Uri.parse("https://example.com/")
    private val pendingIntentGetter by lazy {
        runCatching { PendingIntent::class.java.getDeclaredMethod("getIntent") }.getOrNull()
    }
    private val bareWebAddress = Regex("""(?:[\p{L}\d-]+\.)+[\p{L}\d-]+(?::\d+)?(?:[/?#]\S*)?""")

    fun isXiaomiBrowser(packageName: String?) = packageName in xiaomiBrowsers

    fun webUri(uri: Uri?): Uri? = uri?.normalizeScheme()?.takeIf {
        (it.scheme == "http" || it.scheme == "https") &&
            !it.host.isNullOrBlank() && it.toString().none(Char::isWhitespace)
    }

    fun webUri(text: String?, acceptBareAddress: Boolean = false): Uri? {
        val value =
            text?.trim()?.takeIf { it.isNotEmpty() && it.none(Char::isWhitespace) } ?: return null
        webUri(Uri.parse(value))?.let { return it }
        if (!acceptBareAddress || !bareWebAddress.matches(value)) return null
        return webUri(Uri.parse("https://$value"))?.takeUnless { isXiaomiBrowser(it.host) }
    }

    fun recoverWebUri(intent: Intent): Uri? {
        val data = intent.data
        webUri(data)?.takeUnless(::isBrowserDownload)?.let { return it }
        val scheme = data?.scheme?.lowercase()
        if (data != null && scheme != "mibrowser" && scheme != "intent" && !isBrowserDownload(data)) return null
        val serialized = data?.takeIf { scheme == "intent" }?.let {
            runCatching { Intent.parseUri(it.toString(), Intent.URI_INTENT_SCHEME) }.getOrNull()
        }
        for (key in urlKeys) {
            val candidates = listOf(
                data?.takeIf(Uri::isHierarchical)?.getQueryParameter(key),
                runCatching { intent.getStringExtra(key) }.getOrNull(),
                serialized?.getStringExtra(key)
            )
            candidates.firstNotNullOfOrNull {
                webUri(
                    it,
                    acceptBareAddress = key != "browser_fallback_url"
                )
            }
                ?.let { return it }
        }
        return webUri(serialized?.data)?.takeUnless(::isBrowserDownload)
    }

    fun isBrowserDownload(uri: Uri?): Boolean {
        if (uri == null || !uri.isHierarchical) return false
        val isStoreDetails = when (uri.scheme) {
            "market", "mimarket" -> uri.host == "details"
            "http", "https" -> uri.host == "app.mi.com" && uri.path == "/details"
            else -> false
        }
        return isStoreDetails && isXiaomiBrowser(uri.getQueryParameter("id"))
    }

    fun targetsXiaomiBrowser(intent: Intent): Boolean =
        isXiaomiBrowser(intent.component?.packageName ?: intent.getPackage()) ||
            intent.data?.scheme == "mibrowser" ||
            isBrowserDownload(intent.data)

    fun defaultPackage(context: Context): String? = runCatching {
        val activity = context.packageManager.resolveActivity(
            viewIntent(webProbe),
            PackageManager.MATCH_DEFAULT_ONLY
        )
            ?.activityInfo ?: return null
        activity.packageName.takeUnless {
            it == "android" || it == "com.android.intentresolver" || activity.name.endsWith(".ResolverActivity")
        }
    }.getOrNull()

    fun viewIntent(uri: Uri, packageName: String? = null): Intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setPackage(packageName)
        }

    fun canOpen(context: Context, intent: Intent): Boolean = runCatching {
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }.getOrDefault(false)

    fun forDefaultBrowser(context: Context, uri: Uri, flags: Int = 0): Intent? {
        val browser = defaultPackage(context)
        val intent = viewIntent(uri, browser).setFlags(flags)
        if (canOpen(context, intent)) return intent
        return if (browser != null) viewIntent(uri).setFlags(flags)
            .takeIf { canOpen(context, it) } else null
    }

    fun appIcon(context: Context, packageName: String): Icon? = runCatching {
        Icon.createWithBitmap(
            BitmapUtils.drawableToBitmap(
                context.packageManager.getApplicationIcon(
                    packageName
                )
            )
        )
    }.getOrNull()

    fun notificationIntent(notification: Notification): Intent? {
        val pendingIntent = notification.contentIntent ?: return null
        return runCatching { pendingIntentGetter?.invoke(pendingIntent) as? Intent }.getOrNull()
    }

    fun updateFocusIcon(notification: Notification, icon: Icon) {
        val extras = notification.extras ?: return
        val pictures = extras.getBundle("miui.focus.pics") ?: Bundle()
        pictures.putParcelable("miui.focus.pic_image", icon)
        pictures.putParcelable("miui.land.pic_image", icon)
        extras.putBundle("miui.focus.pics", pictures)
        extras.putParcelable("miui.appIcon", icon)
    }
}
