package com.sevtinge.hyperceiler.ui.settings.tips;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.sevtinge.hyperceiler.R;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class TipsLocalModel {

    private String action;
    private String authority;
    private String extras;
    private String icon;
    private String id;
    private String pkg;
    private String summary;
    private String title;
    private String url;
    private int priority = 1000;
    private int arrowIcon = -1;
    private int textColor = 0;
    private Drawable background = null;

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getSummary() {
        return this.summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getArrowIcon() {
        return this.arrowIcon;
    }

    public void setArrowIcon(int arrowIcon) {
        this.arrowIcon = arrowIcon;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setBackground(Drawable background) {
        this.background = background;
    }

    public Drawable getBackground() {
        return background;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setExtras(String extras) {
        this.extras = extras;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(id) ||
                TextUtils.isEmpty(title) ||
                TextUtils.isEmpty(action) ||
                TextUtils.isEmpty(authority) ||
                priority == 1000 ||
                TextUtils.isEmpty(pkg) ||
                arrowIcon == -1;
    }

    public void update(Context context, String str) {
        if (context != null) {
            Bundle bundle = new Bundle();
            bundle.putString("id", id);
            bundle.putString("action", str);
            try {
                ContentResolver contentResolver = context.getContentResolver();
                contentResolver.call(Uri.parse("content://" + authority), str, null, bundle);
            } catch (Exception e) {
                Log.e("TipsUtils", "update: ", e);
            }
        }
    }

    public Drawable getIcon(Context context) {
        int iconResId;
        if (context == null) return null;
        if (isEmpty()) return context.getDrawable(R.drawable.ic_android);
        try {
            iconResId = Integer.parseInt(icon);
        } catch (NumberFormatException unused) {
            iconResId = -1;
        }
        if (iconResId > 0) {
            return getIconDrawableById(context.getApplicationContext(), iconResId, pkg);
        }
        return null;
    }

    public Intent getIntent() {
        return putParamsIntoIntent(new Intent(action));
    }

    private Intent putParamsIntoIntent(Intent intent) {
        String args;
        if (!TextUtils.isEmpty(extras)) {
            args = URLDecoder.decode(extras, StandardCharsets.UTF_8);
            if (TextUtils.isEmpty(args)) return intent;
            for (String str : args.split("&")) {
                String[] split = str.split("=");
                if (split.length == 2) {
                    if (TextUtils.equals(split[0], "package")) {
                        intent.setPackage(split[1]);
                    } else {
                        intent.putExtra(split[0], split[1]);
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(url)) {
            try {
                intent.setData(Uri.parse(url));
            } catch (Exception unused) {
            }
        }
        return intent;
    }

    public static Drawable getIconDrawableById(Context context, int id, String packageName) {
        Drawable icon = null;
        if (id > 0) {
            try {
                icon = context.createPackageContext(packageName, 0).getResources().getDrawable(id);
            } catch (Exception e) {
                Log.w("MiuiUtils", "Could not get getIconDrawable for " + packageName + ": " + e);
            }
            if (icon == null) {
                try {
                    return context.getResources().getDrawable(id);
                } catch (Exception e2) {
                    Log.w("MiuiUtils", "Could not get getIconDrawable for com.android.settings : " + e2);
                    return icon;
                }
            }
            return icon;
        }
        return null;
    }

    public static boolean isPkgExist(PackageManager pm, String packageName) {
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }

    public String toString() {
        return "TipsLocalModel{AUTHORITY='" +
                authority + "', PKG='" +
                pkg + "', ID='" +
                id + "', TEXT='" +
                title + "', PRIORITY=" +
                priority + ", SUMMARY=" +
                summary + ", ARROW_ICON=" +
                arrowIcon + ", ACTION='" +
                action + "', EXTRAS='" +
                extras + "', ICON='" +
                icon + "', URL='" +
                url + "'}";
    }
}