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
package com.sevtinge.hyperceiler.log;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;

public class HighLightUtils {

    private static final int COLOR_HIGHLIGHT = 0x66FFA500;

    public static CharSequence getHighlightedText(String fullText, String keyword) {
        if (fullText == null || fullText.isEmpty()) return "";
        if (keyword == null || keyword.isEmpty()) return fullText;

        SpannableStringBuilder builder = new SpannableStringBuilder(fullText);
        String lowerText = fullText.toLowerCase();
        String lowerKey = keyword.toLowerCase();

        int start = 0;
        while ((start = lowerText.indexOf(lowerKey, start)) != -1) {
            int end = start + lowerKey.length();
            builder.setSpan(new BackgroundColorSpan(COLOR_HIGHLIGHT), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = end;
        }
        return builder;
    }
}
