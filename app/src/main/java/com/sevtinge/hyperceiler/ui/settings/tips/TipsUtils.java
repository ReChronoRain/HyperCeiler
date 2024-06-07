package com.sevtinge.hyperceiler.ui.settings.tips;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.sevtinge.hyperceiler.R;

public class TipsUtils {

    private static final String[] SETTINGS_NOTIFICATION_COULUMN = {"id", "text", "priority", "style", "action", "extras", "summary", "icon", "url"};

    public static TipsLocalModel query(Context context) {
        Cursor cursor = null;
        Log.d("TipsUtils", "query start ...");
        Cursor cursor2 = null;
        if (context == null) return null;
        TipsLocalModel tipsLocalModel = new TipsLocalModel();
        for (ResolveInfo resolveInfo : context.getPackageManager().queryIntentContentProviders(new Intent("miui.intent.action.SETTINGS_NOTIFICATION_PROVIDER"), 0)) {
            ProviderInfo providerInfo = resolveInfo.providerInfo;
            String str = providerInfo.packageName;
            String str2 = providerInfo.authority;
            if (checkPermission(context, resolveInfo)) {
                try {
                    Log.d("TipsUtils", "query: " + str + "/" + str2);
                    ContentResolver contentResolver = context.getContentResolver();
                    StringBuilder sb = new StringBuilder();
                    sb.append("content://");
                    sb.append(str2);
                    cursor = contentResolver.query(Uri.parse(sb.toString()), SETTINGS_NOTIFICATION_COULUMN, null, null, null);
                } catch (Exception e) {
                    cursor = null;
                } catch (Throwable ignored) {
                }
                try {
                    try {
                        Log.d("TipsUtils", str + ":Cursor = " + cursor);
                        if (cursor != null) {
                            while (cursor.moveToNext()) {
                                int i = cursor.getInt(2);
                                Log.d("TipsUtils", "priority --> " + i);
                                if (i < tipsLocalModel.getPriority()) {
                                    tipsLocalModel.setId(cursor.getString(0));
                                    tipsLocalModel.setAuthority(str2);
                                    tipsLocalModel.setPkg(str);
                                    tipsLocalModel.setPriority(i);
                                    tipsLocalModel.setTitle(cursor.getString(1));

                                    tipsLocalModel.setArrowIcon(getArrowIcon(cursor.getString(3)));
                                    tipsLocalModel.setAction(cursor.getString(4));
                                    tipsLocalModel.setExtras(cursor.getString(5));
                                    tipsLocalModel.setSummary(cursor.getString(6));
                                    tipsLocalModel.setIcon(cursor.getString(7));
                                    tipsLocalModel.setUrl(cursor.getString(8));
                                }
                            }
                        }
                    } catch (Exception e2) {
                        Log.e("TipsUtils", "query error: " + e2.getMessage());
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                    cursor.close();
                } catch (Throwable th2) {
                    cursor2 = cursor;
                    if (cursor2 != null) {
                        cursor2.close();
                    }
                    throw th2;
                }
            } else {
                Log.e("TipsUtils", "query error: no permission, PKG = " + str);
            }
        }
        Log.d("TipsUtils", "query success: " + tipsLocalModel);
        return tipsLocalModel;
    }

    private static boolean checkPermission(Context context, ResolveInfo resolveInfo) {
        if (context == null || resolveInfo == null) {
            return false;
        }
        return (resolveInfo.providerInfo.applicationInfo.flags & 1) > 0;
    }

    public static int getArrowIcon(String str) {
        return R.drawable.miuix_appcompat_arrow_up_down;
    }
}
