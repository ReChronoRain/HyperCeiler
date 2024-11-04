package com.sevtinge.hyperceiler.module.hook.home.layout

import android.content.*
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.tool.OtherTool.*
import com.sevtinge.hyperceiler.utils.*
import kotlin.math.*

object LayoutRules : BaseHook() {
    private const val GRID_CONFIG = "com.miui.home.launcher.GridConfig"
    private const val PHONE_RULES = "com.miui.home.launcher.compat.PhoneDeviceRules"
    private const val HOME_SETTINGS = "com.miui.home.settings.MiuiHomeSettings"

    private val isHyperOS2Home by lazy {
        // 最低版本未知
        getPackageVersionCode(lpparam) >= 539309777
    }

    private var cellCountX = 0
    private var cellCountY = 0

    override fun init() {
        if (isHyperOS2Home) {
            cellCountX = mPrefsMap.getInt("home_layout_unlock_grids_cell_x", 0)
            cellCountY = mPrefsMap.getInt("home_layout_unlock_grids_cell_y", 0)

            findAndHookMethod(
                HOME_SETTINGS, "setUpScreenCellsConfig",
                Boolean::class.java, Int::class.java,
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        val settings = param.thisObject

                        val mMiuiHomeConfig = settings.getObjectField("mMiuiHomeConfig")
                        val mScreenCellsConfig = settings.getObjectField("mScreenCellsConfig")

                        mMiuiHomeConfig?.callMethod("removePreference", mScreenCellsConfig)
                        param.result = null
                    }
                }
            )

            findAndHookMethod(
                PHONE_RULES, "calGridSize",
                Context::class.java, Int::class.java, Int::class.java, Boolean::class.java,
                object : MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val rules = param.thisObject
                        if (cellCountX == 0) {
                            cellCountX = param.args[1] as Int
                        }

                        val mMaxGridWidth = rules.getIntField("mMaxGridWidth")
                        val mWorkspaceCellSideDefault = rules.getIntField("mWorkspaceCellSideDefault")
                        val mCellSize = rules.getIntField("mCellSize")
                        val mCellCountY = rules.getIntField("mCellCountY")
                        if (cellCountY == 0) {
                            cellCountY = mCellCountY
                        }

                        val cellWidth = (mMaxGridWidth - mWorkspaceCellSideDefault) / cellCountX
                        val cellHeight = mCellSize * mCellCountY / cellCountY

                        rules.setIntField("mCellSize", max(cellWidth, cellHeight))

                        findAndHookMethod(GRID_CONFIG, "getCountCellX", returnConstant(cellCountX))
                        findAndHookMethod(GRID_CONFIG, "getCountCellY", returnConstant(cellCountY))
                        findAndHookMethod(GRID_CONFIG, "getCellWidth", returnConstant(cellWidth))
                        findAndHookMethod(GRID_CONFIG, "getCellHeight", returnConstant(cellHeight))
                    }
                }
            )
        }
    }
}