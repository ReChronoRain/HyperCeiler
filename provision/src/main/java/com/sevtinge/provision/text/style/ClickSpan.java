package com.sevtinge.provision.text.style;

import android.content.Context;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.sevtinge.provision.utils.OobeUtils;

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
