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

import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px

internal class MobileTypeViewRenderer(
    private val showMobileType: Boolean,
    private val hideIndicator: Boolean,
    private val mobileNetworkType: Int,
    private val visibilityResolver: MobileTypeVisibilityResolver,
    /** 下标 A 相对主字号的缩放比例 */
    private val subscriptSizeRatio: Float,
    /** 下标 A 的下沉量（dp）。在这里而不是构造期换算成像素，
     *  因为构造期取 App Context 不保险，绑定发生时才一定可用。 */
    private val subscriptDropDp: Float,
    /** 「右侧间距」设置换算出的 dp（普通情况用） */
    private val baseRightMarginDp: Float,
    /** 5GA 专用的右间距修正量（dp），叠加在 baseRightMarginDp 之上；0 表示不改变 */
    private val gaRightMarginOffsetDp: Float,
    private val updateMobileTypeDrawable: (imageView: ImageView, showName: String) -> Unit,
) {
    fun applyLargeMobileType(textView: TextView?, showName: String, largeTypeVisible: Boolean) {
        if (textView == null) return

        val previousText = textView.text?.toString().orEmpty()
        val displayName = showName.ifEmpty { previousText }
        if (showName.isNotEmpty()) {
            val decorated = decorateMobileTypeText(showName)
            // 系统自身的绑定逻辑通常已经把文本写成了同样的字符串，所以
            // previousText == showName 会跳过赋值；而且那段文本本身往往就是
            // Spanned（带它自己的样式）。因此不能只判断"是不是 Spanned"，
            // 必须检查**我们自己的**下标 Span 在不在、参数对不对。
            // 否则 SystemUI 重启后第一次绑定不会加下标，要等文本变化一次才生效
            // （表现为：重启后 A 恢复原样，开关一次 WiFi 才变回来）。
            val needsDecorate = decorated is Spanned && !isDecorationUpToDate(textView.text)
            if (previousText != showName || needsDecorate) {
                textView.text = decorated
            }
        }
        applyRightMargin(textView, displayName)
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

        val shouldRefreshDrawable = if (mobileNetworkType == 4 && !visibilityResolver.shouldUseDualRowDataSimSync()) {
            showName.isNotEmpty()
        } else {
            visibilityResolver.shouldRefreshSmallMobileTypeDrawable(smallVisible, showName)
        }
        if (shouldRefreshDrawable) {
            updateMobileTypeDrawable(imageView, showName)
        }
    }

    /**
     * "5GA" 末尾的 A 缩小并下沉，做成右下标的样子；其它文本原样返回。
     *
     * 注意：SpannableString.toString() 仍返回纯文本，
     * 所以调用方基于 previousText 的判重逻辑不受影响。
     */
    /**
     * 右侧间距。
     *
     * 普通文本用「右侧间距」设置；文本以 GA 结尾（5GA）时，在其基础上再叠加
     * 5GA 专用的修正量。这样 5GA 可以单独调间距，又完全不影响单纯 5G 的显示
     * （修正量为 0 时两者完全一致）。
     */
    private fun applyRightMargin(textView: TextView, showName: String) {
        val isGa = showName.endsWith("GA", ignoreCase = true)
        val marginDp = baseRightMarginDp + if (isGa) gaRightMarginOffsetDp else 0f
        val rightPx = dp2px(marginDp.coerceAtLeast(0f))
        if (textView.paddingRight != rightPx) {
            textView.setPadding(
                textView.paddingLeft,
                textView.paddingTop,
                rightPx,
                textView.paddingBottom
            )
        }
    }

    /**
     * 当前文本是否已经带有**参数正确**的下标 Span。
     *
     * 只有 Span 缺失、或尺寸/下沉量已经和当前设置不一致时，才需要重新赋值。
     */
    private fun isDecorationUpToDate(current: CharSequence?): Boolean {
        if (current !is Spanned) return false
        val shift = current.getSpans(0, current.length, SubscriptBaselineSpan::class.java)
            .firstOrNull() ?: return false
        if (shift.shiftPx != dp2px(subscriptDropDp)) return false
        val sizeSpan = current.getSpans(0, current.length, RelativeSizeSpan::class.java)
            .firstOrNull() ?: return false
        // RelativeSizeSpan 的 getter 是 getSizeChange()，Kotlin 属性名是 sizeChange
        return sizeSpan.sizeChange == subscriptSizeRatio
    }

    private fun decorateMobileTypeText(text: String): CharSequence {
        if (text.length < 2 || !text.endsWith("GA", ignoreCase = true)) return text
        val aStart = text.length - 1
        return SpannableString(text).apply {
            setSpan(
                RelativeSizeSpan(subscriptSizeRatio),
                aStart,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                SubscriptBaselineSpan(dp2px(subscriptDropDp)),
                aStart,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
