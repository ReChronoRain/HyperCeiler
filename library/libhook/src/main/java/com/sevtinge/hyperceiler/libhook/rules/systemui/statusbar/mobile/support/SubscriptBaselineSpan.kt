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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
*/

package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile.support

import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/**
 * 只把文字的基线下沉，用来把 "5GA" 末尾的 A 做成右下角小标。
 *
 * 系统自带的 [android.text.style.SubscriptSpan] 会把字号固定缩到 0.5 倍、
 * 且无法单独控制，所以这里把它拆开：字号交给 RelativeSizeSpan，
 * 本 Span 只负责把基线往下压。
 *
 * 符号约定（对照框架的 SuperscriptSpan / SubscriptSpan）：
 * TextPaint.baselineShift **为正是向下、为负是向上**。
 *   SuperscriptSpan: baselineShift += ascent()/2  -> 变负 -> 上标
 *   SubscriptSpan  : baselineShift -= ascent()/2  -> 变正 -> 下标
 * 所以要下沉必须 **加**。
 *
 * @param shiftPx 下沉的像素数，正值向下
 */
internal class SubscriptBaselineSpan(val shiftPx: Int) : MetricAffectingSpan() {

    override fun updateDrawState(tp: TextPaint) {
        tp.baselineShift += shiftPx
    }

    override fun updateMeasureState(tp: TextPaint) {
        tp.baselineShift += shiftPx
    }
}
