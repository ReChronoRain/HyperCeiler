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
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.provision.utils.OobeUtils;

public class TermsTitleSpan extends ClickableSpan {

    private Context mContext;
    private int mHiperlinkType;

    public TermsTitleSpan(Context context, int type) {
        mContext = context;
        mHiperlinkType = type;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        ds.setUnderlineText(false);
    }

    @Override
    public void onClick(@NonNull View widget) {
        Log.i("TermsAndStatementFragment", " here is TermsTitleSpan onClick ");
        /*Intent licenseIntent = Utils.getLicenseIntent("");
        if (mHiperlinkType == 2) {
            Log.i("TermsAndStatementFragment", " here is User Agreement click ");
            licenseIntent.putExtra("android.intent.extra.LICENSE_TYPE", 2);
            //OTHelper.rdCountEvent("key_click_terms_license");
        } else if (mHiperlinkType == 1) {
            Log.i("TermsAndStatementFragment", " here is Privacy Policy click ");
            licenseIntent.putExtra("android.intent.extra.LICENSE_TYPE", 1);
            //OTHelper.rdCountEvent("key_click_terms_privacy");
        }*/
        OobeUtils.startActivity(mContext, OobeUtils.getLicenseIntent("https://raw.githubusercontent.com/ReChronoRain/website/main/Privacy.md"));
    }
}
