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
