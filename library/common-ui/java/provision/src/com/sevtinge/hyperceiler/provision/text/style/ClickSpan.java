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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.provision.text.style;

import android.content.Context;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.provision.utils.OobeUtils;

import java.util.HashMap;

public class ClickSpan extends ClickableSpan {

    private Context mContext;
    private HashMap<String, Integer> mPrivacyTypeMap;

    public ClickSpan(Context context, HashMap<String, Integer> typeMap) {
        this(typeMap);
        mContext = context;
    }

    private ClickSpan(HashMap<String, Integer> typeMap) {
        mPrivacyTypeMap = typeMap;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        ds.setUnderlineText(false);
        super.updateDrawState(ds);
    }

    @Override
    public void onClick(@NonNull View widget) {
        Spanned spanned = (Spanned) ((TextView) widget).getText();
        int spanStart = spanned.getSpanStart(this);
        int spanEnd = spanned.getSpanEnd(this);
        OobeUtils.startActivity(mContext, OobeUtils.getLicenseIntent("https://limestart.cn/"));
    }
}
