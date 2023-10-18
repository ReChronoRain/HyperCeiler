package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.R
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import com.sevtinge.cemiuiler.utils.devicesdk.isAndroidT
import com.sevtinge.cemiuiler.utils.devicesdk.isAndroidU
import de.robv.android.xposed.XposedHelpers

object CCGrid : BaseHook() {
    private val cols by lazy {
        mPrefsMap.getInt("system_control_center_cc_columns", 4)
    }
    private val rows by lazy {
        mPrefsMap.getInt("system_control_center_cc_rows", 4)
    }
    private val label by lazy {
        mPrefsMap.getBoolean("system_control_center_qs_tile_label")
    }
    private var scaledTileWidthDim = -1f

    @SuppressLint("DiscouragedApi")
    override fun init() {
        if (cols > 4) {
            mResHook.setObjectReplacement(
                lpparam.packageName,
                "dimen",
                "qs_control_tiles_columns",
                cols
            )
        }
        Helpers.findAndHookMethod(
            "com.android.systemui.SystemUIApplication",
            lpparam.classLoader,
            "onCreate",
            object : MethodHook() {
                private var isHooked = false
                override fun after(param: MethodHookParam) {
                    if (!isHooked) {
                        isHooked = true
                        val mContext = XposedHelpers.callMethod(
                            param.thisObject,
                            "getApplicationContext"
                        ) as Context
                        val res = mContext.resources
                        val density = res.displayMetrics.density
                        val tileWidthResId = res.getIdentifier(
                            "qs_control_center_tile_width",
                            "dimen",
                            "com.android.systemui"
                        )
                        var tileWidthDim = res.getDimension(tileWidthResId)
                        if (cols > 4) {
                            tileWidthDim /= density
                            scaledTileWidthDim = tileWidthDim * 4 / cols
                            mResHook.setDensityReplacement(
                                lpparam.packageName,
                                "dimen",
                                "qs_control_center_tile_width",
                                scaledTileWidthDim
                            )
                            mResHook.setDensityReplacement(
                                "miui.systemui.plugin",
                                "dimen",
                                "qs_control_center_tile_width",
                                scaledTileWidthDim
                            )
                            mResHook.setDensityReplacement(
                                lpparam.packageName,
                                "dimen",
                                "qs_control_tile_icon_bg_size",
                                scaledTileWidthDim
                            )
                            mResHook.setDensityReplacement(
                                "miui.systemui.plugin",
                                "dimen",
                                "qs_control_tile_icon_bg_size",
                                scaledTileWidthDim
                            )
                            mResHook.setDensityReplacement(
                                "miui.systemui.plugin",
                                "dimen",
                                "qs_cell_height",
                                85f
                            )
                        }
                    }
                }
            })

        val pluginLoaderClass =
            if (isAndroidU()) "com.android.systemui.shared.plugins.PluginInstance\$Factory\$\$ExternalSyntheticLambda0"
            else if (isAndroidT()) "com.android.systemui.shared.plugins.PluginInstance\$Factory"
            else "com.android.systemui.shared.plugins.PluginManagerImpl"

        var appInfo: ApplicationInfo?

        if (isAndroidU()) {
            hookAllMethods("com.android.systemui.shared.plugins.PluginInstance\$Factory",
                "create", object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        appInfo = param.args[1] as ApplicationInfo
                        ClassUtils.loadClass(pluginLoaderClass, lpparam.classLoader).methodFinder().first {
                            name == "get"
                        }.createHook {
                            after { getClassLoader ->
                                if (appInfo!!.packageName == "miui.systemui.plugin") {
                                    val classLoader = getClassLoader.result as ClassLoader
                                    loadCCGrid(classLoader)
                                }
                            }
                        }
                    }
                }
            )
        } else {
            ClassUtils.loadClass(pluginLoaderClass, lpparam.classLoader).methodFinder().first {
                name == "getClassLoader"
            }.createHook {
                after { getClassLoader ->
                    appInfo = getClassLoader.args[0] as ApplicationInfo
                    if (appInfo!!.packageName == "miui.systemui.plugin") {
                        val classLoader = getClassLoader.result as ClassLoader
                        loadCCGrid(classLoader)
                    }
                }
            }
        }
    }

    private fun loadCCGrid(pluginLoader: ClassLoader) {
        if (cols > 4) {
            Helpers.findAndHookConstructor("miui.systemui.controlcenter.qs.QSPager", pluginLoader,
                Context::class.java,
                AttributeSet::class.java,
                object : MethodHook() {
                    override fun after(param: MethodHookParam) {
                        XposedHelpers.setObjectField(
                            param.thisObject,
                            "columns",
                            cols
                        )
                    }
                })
            if (!label) {
                Helpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.StandardTileView", pluginLoader,
                    "createLabel",
                    Boolean::class.javaPrimitiveType,
                    object : MethodHook() {
                        override fun after(param: MethodHookParam) {
                            val label = XposedHelpers.getObjectField(
                                param.thisObject,
                                "label"
                            )
                            if (label != null) {
                                val lb = label as TextView
                                lb.maxLines = 1
                                lb.isSingleLine = true
                                lb.ellipsize = TextUtils.TruncateAt.MARQUEE
                                lb.marqueeRepeatLimit = 0
                                val labelContainer = XposedHelpers.getObjectField(
                                    param.thisObject,
                                    "labelContainer"
                                ) as View
                                labelContainer.setPadding(4, 0, 4, 0)
                            }
                        }
                    })
            }
        }
        if (rows != 4) {
            Helpers.findAndHookMethod(
                "miui.systemui.controlcenter.qs.QSPager",
                pluginLoader,
                "distributeTiles",
                object : MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val collapse = XposedHelpers.getObjectField(
                            param.thisObject,
                            "collapse"
                        ) as Boolean
                        if (collapse) {
                            val pages = XposedHelpers.getObjectField(
                                param.thisObject,
                                "pages"
                            ) as ArrayList<*>
                            for (tileLayoutImpl in pages) {
                                XposedHelpers.callMethod(
                                    tileLayoutImpl,
                                    "removeTiles"
                                )
                            }
                            val pageTiles = ArrayList<Any>()
                            var currentRow = 2
                            val records = XposedHelpers.getObjectField(
                                param.thisObject,
                                "records"
                            ) as ArrayList<*>
                            val it2: Iterator<*> = records.iterator()
                            var i3 = 0
                            var pageNow = 0
                            val bigHeader = XposedHelpers.getObjectField(
                                param.thisObject,
                                "header"
                            )
                            while (it2.hasNext()) {
                                val tileRecord = it2.next()!!
                                pageTiles.add(tileRecord)
                                i3++
                                if (i3 >= cols) {
                                    currentRow++
                                    i3 = 0
                                }
                                if (currentRow >= rows || !it2.hasNext()) {
                                    XposedHelpers.callMethod(
                                        pages[pageNow],
                                        "setTiles",
                                        pageTiles,
                                        if (pageNow == 0) bigHeader else null
                                    )
                                    pageTiles.clear()
                                    val totalRows = XposedHelpers.getObjectField(
                                        param.thisObject,
                                        "rows"
                                    ) as Int
                                    if (currentRow > totalRows) {
                                        XposedHelpers.setObjectField(
                                            param.thisObject,
                                            "rows",
                                            currentRow
                                        )
                                    }
                                    if (it2.hasNext()) {
                                        pageNow++
                                        currentRow = 0
                                    }
                                }
                            }
                            val it3 = pages.iterator()
                            while (it3.hasNext()) {
                                val next2 = it3.next()
                                val isEmpty = XposedHelpers.callMethod(
                                    next2,
                                    "isEmpty"
                                ) as Boolean
                                if (isEmpty) {
                                    it3.remove()
                                }
                            }
                            val pageIndicator = XposedHelpers.getObjectField(
                                param.thisObject,
                                "pageIndicator"
                            )
                            if (pageIndicator != null) {
                                XposedHelpers.callMethod(
                                    pageIndicator,
                                    "setNumPages",
                                    pages.size
                                )
                            }
                            val adapter = XposedHelpers.getObjectField(
                                param.thisObject,
                                "adapter"
                            )
                            XposedHelpers.callMethod(
                                param.thisObject,
                                "setAdapter",
                                adapter
                            )
                            // XposedHelpers.callMethod(param.thisObject, "notifyDataSetChanged");
                        }
                    }
                })
        }
        // 移除磁贴标题相关
        if (mPrefsMap.getBoolean("system_control_center_qs_tile_label")) {
            mHideCCLabels(pluginLoader)
        }

        // 新控制中心矩形圆角
        if (mPrefsMap.getBoolean("system_ui_control_center_rounded_rect")) {
            mResHook.setResReplacement(
                "miui.systemui.plugin",
                "drawable",
                "qs_background_unavailable",
                R.drawable.ic_qs_tile_bg_disabled
            )
            mResHook.setResReplacement(
                "miui.systemui.plugin",
                "drawable",
                "qs_background_disabled",
                R.drawable.ic_qs_tile_bg_disabled
            )
            mResHook.setResReplacement(
                "miui.systemui.plugin",
                "drawable",
                "qs_background_warning",
                R.drawable.ic_qs_tile_bg_warning
            )
            mCCTileCornerHook(pluginLoader)
        }
    }

    private fun mHideCCLabels(pluginLoader: ClassLoader?) {
        mResHook.setDensityReplacement(
            "miui.systemui.plugin",
            "dimen",
            "qs_cell_height",
            85f
        )
        val mQSController = XposedHelpers.findClassIfExists(
            "miui.systemui.controlcenter.qs.tileview.StandardTileView",
            pluginLoader
        )
        Helpers.hookAllMethods(mQSController, "init", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if (param.args.size != 1) return
                val mLabelContainer = XposedHelpers.getObjectField(
                    param.thisObject,
                    "labelContainer"
                ) as View
                if (mLabelContainer != null) {
                    mLabelContainer.visibility = View.GONE
                }
            }
        })
    }

    private fun mCCTileCornerHook(pluginLoader: ClassLoader?) {
        Helpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.ExpandableIconView",
            pluginLoader,
            "setCornerRadius",
            Float::class.javaPrimitiveType,
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    val mContext = XposedHelpers.callMethod(
                        param.thisObject,
                        "getPluginContext"
                    ) as Context
                    var radius = 18f
                    if (scaledTileWidthDim > 0) {
                        radius *= scaledTileWidthDim / 65
                    }
                    param.args[0] = mContext.resources.displayMetrics.density * radius
                }
            })
        Helpers.findAndHookMethod("miui.systemui.dagger.PluginComponentFactory",
            pluginLoader,
            "create",
            Context::class.java,
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    val mContext = param.args[0] as Context
                    val res = mContext.resources
                    val enabledTileBackgroundResId = res.getIdentifier(
                        "qs_background_enabled",
                        "drawable",
                        "miui.systemui.plugin"
                    )
                    val enabledTileColorResId = res.getIdentifier(
                        "qs_enabled_color",
                        "color",
                        "miui.systemui.plugin"
                    )
                    val tintColor = res.getColor(enabledTileColorResId, null)
                    val imgHook: MethodHook = object : MethodHook() {
                        override fun before(param: MethodHookParam) {
                            val resId = param.args[0] as Int
                            if (resId == enabledTileBackgroundResId && resId != 0) {
                                val enableTile = Helpers.getModuleRes(mContext)
                                    .getDrawable(R.drawable.ic_qs_tile_bg_enabled, null)
                                enableTile.setTint(tintColor)
                                param.result = enableTile
                            }
                        }
                    }
                    Helpers.findAndHookMethod(
                        "android.content.res.Resources", pluginLoader, "getDrawable",
                        Int::class.javaPrimitiveType, imgHook
                    )
                    Helpers.findAndHookMethod(
                        "android.content.res.Resources.Theme",
                        pluginLoader,
                        "getDrawable",
                        Int::class.javaPrimitiveType,
                        imgHook
                    )
                }
            })
    }
}
