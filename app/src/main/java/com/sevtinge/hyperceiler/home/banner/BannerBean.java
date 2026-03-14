package com.sevtinge.hyperceiler.home.banner;

import android.text.TextUtils;

import java.util.Objects;

public class BannerBean {

    private String action;
    private String authority;
    private String extras;
    private String icon;
    private String id;
    private String pkg;
    private String title;
    private String titleColor;
    private String summary;
    private String summaryColor;
    private String url;
    private String backgroundColor;

    private int iconResId = -1;
    private int titleResId = -1;
    private int subTitleResId = -1;

    private int titleColorResId = -1;
    private int subTitleColorResId = -1;
    private int backgroundColorResId = -1;
    private int priority = 1000;
    private int arrowIcon = -1;

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String str) {
        authority = str;
    }

    public String getPkg() {
        return this.pkg;
    }

    public void setPkg(String str) {
        this.pkg = str;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String str) {
        this.id = str;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String str) {
        this.title = str;
    }

    public void setTitleColor(String titleColor) {
        this.titleColor = titleColor;
    }

    public String getTitleColor() {
        return titleColor;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int i) {
        this.priority = i;
    }

    public String getSummary() {
        return this.summary;
    }

    public void setSummary(String str) {
        this.summary = str;
    }

    public String getSummaryColor() {
        return summaryColor;
    }

    public void setSummaryColor(String summaryColor) {
        this.summaryColor = summaryColor;
    }

    public int getArrowIcon() {
        return this.arrowIcon;
    }

    public void setArrowIcon(int i) {
        this.arrowIcon = i;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String str) {
        this.action = str;
    }

    public String getExtras() {
        return this.extras;
    }

    public void setExtras(String str) {
        this.extras = str;
    }

    public String getIcon() {
        return this.icon;
    }

    public void setIcon(String str) {
        this.icon = str;
    }

    public int getIconResId() {
        return this.iconResId;
    }

    public void setIconResId(int str) {
        this.iconResId = str;
    }

    public int getTitleColorResId() {
        return titleColorResId;
    }

    public void setTitleColorResId(int titleColorResId) {
        this.titleColorResId = titleColorResId;
    }

    public int getSubTitleColorResId() {
        return subTitleColorResId;
    }

    public void setSubTitleColorResId(int subTitleColorResId) {
        this.subTitleColorResId = subTitleColorResId;
    }

    public int getBackgroundColorResId() {
        return backgroundColorResId;
    }

    public void setBackgroundColorResId(int backgroundColorResId) {
        this.backgroundColorResId = backgroundColorResId;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String str) {
        this.url = str;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BannerBean)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            BannerBean bannerBean = (BannerBean) obj;
            if (this.priority == bannerBean.priority && this.arrowIcon == bannerBean.arrowIcon && TextUtils.equals(this.authority, bannerBean.authority) && TextUtils.equals(this.pkg, bannerBean.pkg) && TextUtils.equals(this.id, bannerBean.id) && TextUtils.equals(this.title, bannerBean.title) && TextUtils.equals(this.summary, bannerBean.summary) && TextUtils.equals(this.icon, bannerBean.icon) && TextUtils.equals(this.action, bannerBean.action) && TextUtils.equals(this.extras, bannerBean.extras) && TextUtils.equals(this.url, bannerBean.url)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.authority, this.pkg, this.id, this.title, Integer.valueOf(this.priority), this.summary, this.icon, Integer.valueOf(this.arrowIcon), this.action, this.extras, this.url);
    }

    public String toString() {
        return "TipsLocalModel{AUTHORITY='" + this.authority + "', PKG='" + this.pkg + "', ID='" + this.id + "', TEXT='" + this.title + "', PRIORITY=" + this.priority + ", SUMMARY=" + this.summary + ", ARROW_ICON=" + this.arrowIcon + ", ACTION='" + this.action + "', EXTRAS='" + this.extras + "', ICON='" + this.icon + "', URL='" + this.url + "'}";
    }
}
