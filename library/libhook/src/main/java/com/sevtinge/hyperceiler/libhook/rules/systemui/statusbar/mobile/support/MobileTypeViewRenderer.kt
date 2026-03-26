/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile.support

import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible

internal class MobileTypeViewRenderer(
    private val showMobileType: Boolean,
    private val hideIndicator: Boolean,
    private val mobileNetworkType: Int,
    private val visibilityResolver: MobileTypeVisibilityResolver,
    private val updateMobileTypeDrawable: (imageView: ImageView, showName: String) -> Unit,
) {
    fun applyLargeMobileType(textView: TextView?, showName: String, largeTypeVisible: Boolean) {
        if (textView == null) return

        val previousText = textView.text?.toString().orEmpty()
        val displayName = showName.ifEmpty { previousText }
        if (showName.isNotEmpty() && previousText != showName) {
            textView.text = showName
        }
        if (showMobileType) {
            textView.isVisible = largeTypeVisible && displayName.isNotEmpty()
        }
    }

    fun applyInOut(indicatorView: ImageView?, inOutVisible: Boolean?) {
        if (indicatorView == null || hideIndicator) return
        if (inOutVisible != null) {
            indicatorView.isVisible = inOutVisible
        }
    }

    fun applySmallMobileType(imageView: ImageView?, smallVisible: Boolean, showName: String) {
        if (showMobileType || imageView == null) return

        imageView.isVisible = smallVisible

        val shouldRefreshDrawable = when {
            mobileNetworkType == 4 && !visibilityResolver.shouldUseDualRowDataSimSync() ->
                showName.isNotEmpty()
            mobileNetworkType == 4 ->
                visibilityResolver.shouldRefreshSmallMobileTypeDrawable(smallVisible, showName)
            else ->
                visibilityResolver.shouldRefreshSmallMobileTypeDrawable(smallVisible, showName)
        }
        if (shouldRefreshDrawable) {
            updateMobileTypeDrawable(imageView, showName)
        }
    }
}
