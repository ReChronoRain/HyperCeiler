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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.home.layout

import android.content.*
import com.sevtinge.hyperceiler.module.hook.home.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import kotlin.math.*

object LayoutRules : HomeBaseHook() {
    private const val PHONE_RULES = "com.miui.home.launcher.compat.PhoneDeviceRules"
    private const val HOME_SETTINGS = "com.miui.home.settings.MiuiHomeSettings"

    private val isUnlockGridsHook by lazy {
        mPrefsMap.getBoolean("home_layout_unlock_grids_new")
    }
    private val isSetWSPaddingTopHook by lazy {
        mPrefsMap.getBoolean("home_layout_workspace_padding_top_enable")
    }
    private val isSetWSPaddingBottomHook by lazy {
        mPrefsMap.getBoolean("home_layout_workspace_padding_bottom_enable")
    }
    private val isSetWSPaddingSideHook by lazy {
        mPrefsMap.getBoolean("home_layout_workspace_padding_horizontal_enable")
    }

    private var sCellCountX = 0
    private var sCellCountY = 0

    private var currentCellCountX = 0
    private var currentCellCountY = 0
    private var currentCellWidth = 0
    private var currentCellHeight = 0

    override fun initForNewHome() {
        if (isUnlockGridsHook) {
            sCellCountX = mPrefsMap.getInt("home_layout_unlock_grids_cell_x", 0)
            sCellCountY = mPrefsMap.getInt("home_layout_unlock_grids_cell_y", 0)
            logI(TAG, lpparam.packageName, "Setup layout rules: ${sCellCountX}x${sCellCountY}")

            findAndHookMethod(
                HOME_SETTINGS, "setUpScreenCellsConfig",
                Boolean::class.java, Int::class.java,
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        val settings = param.thisObject

                        val mMiuiHomeConfig = settings.getObjectField("mMiuiHomeConfig")
                        val mScreenCellsConfig = settings.getObjectField("mScreenCellsConfig")

                        mMiuiHomeConfig?.callMethod("removePreference", mScreenCellsConfig)
                        logI(
                            TAG, lpparam.packageName,
                            "Remove preference($mScreenCellsConfig) form MIUIHomeSettings"
                        )
                        param.result = null
                    }
                }
            )
        }

        findAndHookMethod(
            PHONE_RULES, "calGridSize",
            Context::class.java, Int::class.java, Int::class.java, Boolean::class.java,
            PhoneCalGridSizeHook
        )

        findAndHookMethod(GRID_CONFIG, "getCellWidth", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if (currentCellWidth != 0) {
                    param.result = currentCellWidth
                }
            }
        })

        findAndHookMethod(GRID_CONFIG, "getCellHeight", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if (currentCellHeight != 0) {
                    param.result = currentCellHeight
                }
            }
        })

        findAndHookMethod(GRID_CONFIG, "getCountCellX", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if (isUnlockGridsHook && currentCellCountX != 0) {
                    param.result = currentCellCountX
                }
            }
        })

        findAndHookMethod(GRID_CONFIG, "getCountCellY", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if (isUnlockGridsHook && currentCellCountY != 0) {
                    param.result = currentCellCountY
                }
            }
        })
    }

    object PhoneCalGridSizeHook : MethodHook() {
        override fun after(param: MethodHookParam) {
            val rules = param.thisObject

            val mMaxGridWidth = rules.getIntField("mMaxGridWidth")
            val mWorkspaceCellSideDefault = rules.getIntField("mWorkspaceCellSideDefault")
            val mCellSize = rules.getIntField("mCellSize")
            val mCellCountY = rules.getIntField("mCellCountY")
            val mWorkspaceTopPadding = rules.callMethodAs<Int>("getWorkspacePaddingTop")
            val mWorkspaceCellPaddingBottom =
                rules.getObjectFieldAs<Any>("mWorkspaceCellPaddingBottom")
                    .callMethodAs<Int>("getValue")

            val sWorkspacePaddingTop = if (isSetWSPaddingTopHook) {
                DisplayUtils.dp2px(
                    mPrefsMap.getInt(
                        "home_layout_workspace_padding_top",
                        0
                    ).toFloat()
                )
            } else {
                -1
            }

            val sWorkspacePaddingBottom = if (isSetWSPaddingBottomHook) {
                DisplayUtils.dp2px(
                    mPrefsMap.getInt(
                        "home_layout_workspace_padding_bottom",
                        0
                    ).toFloat()
                )
            } else {
                -1
            }

            val sWorkspaceCellSide = if (isSetWSPaddingSideHook) {
                DisplayUtils.dp2px(
                    mPrefsMap.getInt(
                        "home_layout_workspace_padding_horizontal",
                        0
                    ).toFloat()
                )
            } else {
                -1
            }

            currentCellCountX = if (sCellCountX == 0) {
                param.args[1] as Int
            } else {
                sCellCountX
            }
            currentCellCountY = if (sCellCountY == 0) {
                mCellCountY
            } else {
                sCellCountY
            }

            currentCellWidth = mCellSize
            currentCellHeight = mCellSize

            val cellWorkspaceHeight = mCellSize * mCellCountY

            if (isUnlockGridsHook || isSetWSPaddingSideHook) {
                currentCellWidth = (mMaxGridWidth - if (isSetWSPaddingSideHook) {
                    sWorkspaceCellSide
                } else {
                    mWorkspaceCellSideDefault
                }) / currentCellCountX
            }

            if (isUnlockGridsHook || isSetWSPaddingTopHook || isSetWSPaddingBottomHook) {
                currentCellHeight = (cellWorkspaceHeight + if (isSetWSPaddingTopHook) {
                    mWorkspaceTopPadding - sWorkspacePaddingTop
                } else {
                    0
                } + if (isSetWSPaddingBottomHook) {
                    mWorkspaceCellPaddingBottom - sWorkspacePaddingBottom
                } else {
                    0
                }) / currentCellCountY
            }

            rules.setIntField("mCellSize", max(currentCellWidth, currentCellHeight))

            if (isSetWSPaddingTopHook) {
                rules.getObjectFieldAs<Any>("mWorkspaceTopPadding")
                    .callMethod("setValue", sWorkspacePaddingTop)
            }

            if (isSetWSPaddingBottomHook) {
                rules.getObjectFieldAs<Any>("mWorkspaceCellPaddingBottom")
                    .callMethod("setValue", sWorkspacePaddingBottom)
            }

            if (isSetWSPaddingSideHook) {
                rules.setIntField(
                    "mWorkspaceCellSide",
                    (mMaxGridWidth - currentCellWidth * currentCellCountX) / 2
                )
            }

            logI(
                TAG, lpparam.packageName,
                """ |
                    |Applied layout rules:
                    |  cellCountX    => $currentCellCountX
                    |  cellCountY    => $currentCellCountY
                    |  paddingTop    => $sWorkspacePaddingTop
                    |  paddingBottom => $sWorkspacePaddingBottom
                    |  cellSide      => $sWorkspaceCellSide
                    |  cellSizeO     => $mCellSize
                    |  cellWidth     => $currentCellWidth
                    |  cellHeight    => $currentCellHeight
                """.trimMargin()
            )
        }
    }
}
