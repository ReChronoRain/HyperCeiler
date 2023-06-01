package com.sevtinge.cemiuiler.module.screenshot

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Environment
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import java.io.ByteArrayOutputStream
import java.io.File


object ScreenshotConfig: BaseHook() {
    override fun init() {
        Helpers.hookAllMethods(
            "android.content.ContentResolver", lpparam.classLoader,
            "update",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    if (param.args.size != 4) return
                    val contentValues = param.args[1] as ContentValues
                    var displayName = contentValues.getAsString("_display_name")
                    if (displayName != null && displayName.contains("Screenshot")) {
                        val context: Context = Helpers.findContext()
                        val format: Int =
                            Helpers.getSharedStringPref(context, "pref_key_system_screenshot_format", "2").toInt()
                        val ext = if (format <= 2) ".jpg" else if (format == 3) ".png" else ".webp"
                        displayName =
                            displayName.replace(".png", "")
                                .replace(".jpg", "")
                            .replace(".webp", "") + ext
                        contentValues.put("_display_name", displayName)
                    }
                }
            })

        Helpers.findAndHookMethod("android.content.ContentResolver", lpparam.classLoader, "insert",
            Uri::class.java,
            ContentValues::class.java, object : MethodHook() {
                @Throws(Throwable::class)
                override fun before(param: MethodHookParam) {
                    param.args[0] as Uri
                    val contentValues = param.args[1] as ContentValues
                    var displayName = contentValues.getAsString("_display_name")
                    if (displayName!!.contains("Screenshot")) {
                        val context: Context = Helpers.findContext()
                        val format: Int =
                            Helpers.getSharedStringPref(context, "system_screenshot_format", "2").toInt()
                        val ext = if (format <= 2) ".jpg" else if (format == 3) ".png" else ".webp"
                        val mScreenshotDir: File
                        displayName = displayName!!.replace(".png", "").replace(".jpg", "")
                            .replace(".webp", "") + ext

                        if (mPrefsMap.getBoolean("screenshot_save_to_pictures")) {
                            mScreenshotDir =
                                 File(
                                    Environment.getExternalStoragePublicDirectory(
                                        if (mPrefsMap.getBoolean("screenshot_save_to_pictures"))
                                            Environment.DIRECTORY_PICTURES
                                        else
                                            Environment.DIRECTORY_DCIM),
                                    "Screenshots"
                                )
                            if (!mScreenshotDir.exists()) mScreenshotDir.mkdirs()
                            val relativePath: String =
                                mScreenshotDir.path.replace(Environment.getExternalStorageDirectory().path + File.separator, "")
                            contentValues.put("relative_path", relativePath)
                            if (contentValues.getAsString("_data") != null) {
                                contentValues.put("_data", mScreenshotDir.path + "/" + displayName)
                            }
                        }
                        contentValues.put("_display_name", displayName)
                    }
                }
            })

        val format: Int = mPrefsMap.getStringAsInt("system_screenshot_format", 2)
        if (format > 2) {
            Helpers.findAndHookMethod(
                "com.miui.screenshot.MiuiScreenshotApplication", lpparam.classLoader,
                "attachBaseContext", Context::class.java,
                object : MethodHook() {
                    @Throws(Throwable::class)
                    override fun after(param: MethodHookParam) {
                        val versionCode = Helpers.getPackageVersionCode(lpparam)
                        val changeFormatHook: MethodHook = object : MethodHook() {
                            @Throws(Throwable::class)
                            override fun before(param: MethodHookParam) {
                                if (param.args.size != 7) return
                                val compress =
                                    if (format <= 2) CompressFormat.JPEG else if (format == 3) CompressFormat.PNG else CompressFormat.WEBP_LOSSLESS
                                param.args[4] = compress
                            }
                        }

                        when {
                            versionCode >= 10400056 -> {
                                Helpers.hookAllMethods(
                                    "com.miui.screenshot.u0.f\$a", lpparam.classLoader,
                                    "a", changeFormatHook
                                )
                            }
                            versionCode >= 10400034 -> {
                                Helpers.hookAllMethods(
                                    "com.miui.screenshot.x0.e\$a", lpparam.classLoader,
                                    "a", changeFormatHook
                                )
                            }
                        }
                    }
                })
        }

        Helpers.hookAllMethods(
            "android.graphics.Bitmap", lpparam.classLoader,
            "compress",
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun before(param: MethodHookParam) {
                    val context: Context = Helpers.findContext()
                    var quality = param.args[1] as Int
                    if (quality != 100 || param.args[2] is ByteArrayOutputStream) return
                    val format: Int =
                        Helpers.getSharedStringPref(context, "screenshot_format", "2").toInt()
                    quality =
                        Helpers.getSharedIntPref(context, "screenshot_quality", 100)
                    if (format == 3) {
                        quality = 100
                    }
                    val compress =
                        if (format <= 2) CompressFormat.JPEG else if (format == 3) CompressFormat.PNG else CompressFormat.WEBP_LOSSLESS
                    param.args[0] = compress
                    param.args[1] = quality
                }
            })
    }
}