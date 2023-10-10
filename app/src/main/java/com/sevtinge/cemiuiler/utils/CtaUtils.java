package com.sevtinge.cemiuiler.utils;

import android.app.Activity;
import android.content.Intent;

import com.sevtinge.cemiuiler.R;

public class CtaUtils {
    private static final String APP_PERMISSION_MANAGE_PKG = "com.miui.securitycenter";
    public static final String ACTION_START_CTA_V2 = "miui.intent.action.SYSTEM_PERMISSION_DECLARE";
    public static final String ACTION_START_CTA_V2_NEW = "miui.intent.action.SYSTEM_PERMISSION_DECLARE_NEW";

    private static final String KEY_MANDATORY_PERMISSION = "mandatory_permission";
    private static final String KEY_MAIN_PURPOSE = "main_purpose";
    private static final String KEY_USE_NETWORK = "use_network";
    private static final String KEY_OPTIONAL_PERM = "optional_perm";
    private static final String KEY_OPTIONAL_PERM_DESC = "optional_perm_desc";
    private static final String KEY_OPTIONAL_PERM_SHOW = "optional_perm_show";
    private static final String KEY_SHOW_LOCK = "show_lock";
    private static final String KEY_AGREE_DESC = "agree_desc";

    public static boolean showCtaDialog(Activity activity, int requestCode) {
        String[] mRuntimePermissions = new String[]{"android.permission-group.STORAGE"};
        String[] mRuntimePermissionsDesc = new String[]{activity.getString(R.string.new_permission_storage_desc)};
        Intent intent = new Intent();
        int mActivities = activity.getPackageManager().queryIntentActivities(intent, 0).size();
        intent.setAction(mActivities > 0 ? ACTION_START_CTA_V2_NEW : ACTION_START_CTA_V2);
        intent.setPackage(APP_PERMISSION_MANAGE_PKG);
        intent.putExtra(KEY_MAIN_PURPOSE, activity.getString(R.string.new_cta_app_main_purpose));
        intent.putExtra("runtime_perm", mRuntimePermissions);
        intent.putExtra("runtime_perm_desc", mRuntimePermissionsDesc);
        intent.putExtra(KEY_MANDATORY_PERMISSION, false);
        intent.putExtra(KEY_AGREE_DESC, activity.getResources().getString(R.string.new_cta_agree_desc));
        intent.putExtra("user_agreement", "https://cemiuiler.sevtinge.cc/Protocol");
        intent.putExtra("privacy_policy", "https://cemiuiler.sevtinge.cc/Privacy");
        intent.putExtra(KEY_USE_NETWORK, false);
        intent.putExtra(KEY_SHOW_LOCK, false);
        try {
            /*if (!supportNewPermissionStyle() || activity.getPackageManager().queryIntentActivities(intent, 0).size() <= 0) {
                return false;
            }*/
            activity.startActivityForResult(intent, requestCode);
            return true;
        } catch (Exception unused) {
            return false;
        }
    }
}
