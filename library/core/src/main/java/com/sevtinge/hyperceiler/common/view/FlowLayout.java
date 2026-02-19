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
package com.sevtinge.hyperceiler.common.view;

import static com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class FlowLayout extends ViewGroup {

    private int mHSpacing = dp2px(12);
    private int mVSpacing = dp2px(12);

    public FlowLayout(Context context) { super(context); }
    public FlowLayout(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setSpacing(int h, int v) {
        mHSpacing = h;
        mVSpacing = v;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int paddingL = getPaddingLeft(), paddingR = getPaddingRight();
        int paddingT = getPaddingTop(), paddingB = getPaddingBottom();
        int usable = width - paddingL - paddingR;
        int x = 0, y = 0, lineH = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int cw = child.getMeasuredWidth(), ch = child.getMeasuredHeight();
            if (x + cw > usable && x > 0) {
                x = 0;
                y += lineH + mVSpacing;
                lineH = 0;
            }
            x += cw + mHSpacing;
            lineH = Math.max(lineH, ch);
        }

        setMeasuredDimension(width, resolveSize(paddingT + y + lineH + paddingB, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int usable = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, lineH = 0;
        int pl = getPaddingLeft(), pt = getPaddingTop();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int cw = child.getMeasuredWidth(), ch = child.getMeasuredHeight();
            if (x + cw > usable && x > 0) {
                x = 0;
                y += lineH + mVSpacing;
                lineH = 0;
            }
            child.layout(pl + x, pt + y, pl + x + cw, pt + y + ch);
            x += cw + mHSpacing;
            lineH = Math.max(lineH, ch);
        }
    }
}
