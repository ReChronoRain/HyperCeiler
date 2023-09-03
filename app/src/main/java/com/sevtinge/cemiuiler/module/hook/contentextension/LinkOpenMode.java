package com.sevtinge.cemiuiler.module.hook.contentextension;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class LinkOpenMode extends BaseHook {

    Class<?> mAppsUtils;

    @Override
    public void init() {
        mAppsUtils = findClassIfExists("com.miui.contentextension.utils.AppsUtils");
        int mode = mPrefsMap.getStringAsInt("content_extension_link_open_mode", 0);

        hookAllMethods(mAppsUtils, "generateOpenIntent", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                Context mContext = (Context) param.args[0];
                Object o = param.args[1];
                String detailUrl = (String) XposedHelpers.callMethod(o, "getIntent");
                if (TextUtils.isEmpty(detailUrl)) {
                    detailUrl = (String) XposedHelpers.callMethod(o, "getDetailUrl");
                }
                Uri uri = Uri.parse(detailUrl);
                setOpenIntent(mContext, uri, mode);
                param.setResult(null);
            }
        });
    }


    private void setOpenIntent(Context context, Uri uri, int mode) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        switch (mode) {
            case 0 -> setFreeFormIntent(context, getDefaultBrowserApp(context));

            case 2 -> {
                intent.setPackage("com.android.browser");
                setFreeFormIntent(context, "com.android.browser");
            }
        }
        context.startActivity(intent);
    }


    private void setFreeFormIntent(Context context, String packageName) {
        if (mPrefsMap.getBoolean("system_framework_freeform_jump") && mPrefsMap.getBoolean("system_framework_freeform_content_extension")) {
            Intent mFreeFormIntent = new Intent(ACTION_PREFIX + "SetFreeFormPackage");
            mFreeFormIntent.putExtra("package", packageName);
            context.sendBroadcast(mFreeFormIntent);
        }
    }

    private String getDefaultBrowserApp(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        Uri uri = Uri.parse("http://");
        intent.setDataAndType(uri, null);
        List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.GET_INTENT_FILTERS);
        if (resolveInfoList.size() > 0) {
            ActivityInfo activityInfo = resolveInfoList.get(0).activityInfo;
            return activityInfo.packageName;
        } else {
            return null;
        }
    }
}
