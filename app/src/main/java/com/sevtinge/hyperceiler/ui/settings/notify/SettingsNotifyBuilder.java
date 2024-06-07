package com.sevtinge.hyperceiler.ui.settings.notify;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.sevtinge.hyperceiler.R;

public class SettingsNotifyBuilder {

    private Drawable mIcon;
    private String mTitle;
    private String mSummary;
    private int mTextColor;
    private Drawable mBackground;
    private View.OnClickListener mOnClickListener;
    private static SettingsNotifyBuilder builder;

    public static SettingsNotifyBuilder getInstance() {
        return builder == null ? new SettingsNotifyBuilder() : builder;
    }

    public SettingsNotify build(Context context) {
        return tryBuild(context, 0);
    }

    private SettingsNotify tryBuild(Context context, int id) {
        SettingsNotify notify = new SettingsNotify();
        notify.setNotifyId(id);
        if (SettingsNotifyHelper.isBirthday()) {
            setIcon(context.getDrawable(R.drawable.ic_hyperceiler_cartoon));
            setTitle("");
            setSummary(context.getString(R.string.happy_birthday_hyperceiler));
            setTextColor(Color.parseColor("#fc5b8d"));
            setBackground(context.getDrawable(R.drawable.headtip_hyperceiler_background));
            setOnClickListener(null);
            return notify;
        } else if (SettingsNotifyHelper.isOfficialRom()) {
            setTitle("");
            setSummary(context.getString(R.string.headtip_warn_not_offical_rom));
            setTextColor(Color.RED);
            setBackground(context.getDrawable(R.drawable.headtip_warn_background));
            setOnClickListener(null);
            return notify;
        } else if (SettingsNotifyHelper.isSignPass(context)) {
            setTitle("");
            setSummary(context.getString(R.string.headtip_warn_sign_verification_failed));
            setTextColor(Color.RED);
            setBackground(context.getDrawable(R.drawable.headtip_warn_background));
            setOnClickListener(null);
            return notify;
        }
        return null;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setSummary(String summary) {
        mSummary = summary;
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
    }

    public void setTextColor(int textColor) {
        mTextColor = textColor;
    }

    public void setBackground(Drawable background) {
        mBackground = background;
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mOnClickListener = listener;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSummary() {
        return mSummary;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public int getTextColor() {
        return mTextColor;
    }

    public Drawable getBackground() {
        return mBackground;
    }

    public View.OnClickListener getOnClickListener() {
        return mOnClickListener;
    }
}
