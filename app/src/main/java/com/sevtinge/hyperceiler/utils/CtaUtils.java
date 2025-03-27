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
package com.sevtinge.hyperceiler.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.sevtinge.hyperceiler.R;

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

    public static void setCtaEnabled(Context context) {
        SharedPreferences.Editor edit = context.getSharedPreferences("HyperCeiler_Permission", 0).edit();
        edit.putBoolean("key_new_cta_open", true);
        edit.apply();
    }


    public static boolean isCtaEnabled(Context context) {
        return context.getSharedPreferences("HyperCeiler_Permission", 0).getBoolean("key_new_cta_open", false);
    }

    public static boolean showCtaDialog(Activity activity, int requestCode) {
        Intent intent = new Intent();
        int mActivities = activity.getPackageManager().queryIntentActivities(intent, 0).size();
        intent.setAction(mActivities > 0 ? ACTION_START_CTA_V2_NEW : ACTION_START_CTA_V2);
        intent.setPackage(APP_PERMISSION_MANAGE_PKG);
        intent.putExtra(KEY_MANDATORY_PERMISSION, true);
        intent.putExtra("all_purpose", activity.getString(R.string.new_cta_app_all_purpose_title));
        intent.putExtra("runtime_perm", getRuntimePermission());
        intent.putExtra("runtime_perm_desc", getRuntimePermissionDesc(activity));
        intent.putExtra(KEY_OPTIONAL_PERM, getOptionalPermission());
        intent.putExtra(KEY_OPTIONAL_PERM_DESC, getOptionalPermissionDesc(activity));
        intent.putExtra(KEY_OPTIONAL_PERM_SHOW, false);
        intent.putExtra(KEY_AGREE_DESC, activity.getResources().getString(R.string.new_cta_agree_desc));
        intent.putExtra("user_agreement", "https://hyperceiler.sevtinge.com/Protocol");
        intent.putExtra("privacy_policy", "https://hyperceiler.sevtinge.com/Privacy");
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

    private static String[] getRuntimePermission() {
        return new String[0];
    }

    private static String[] getRuntimePermissionDesc(Activity activity) {
        return new String[0];
    }

    private static String[] getOptionalPermission() {
        return new String[0];
    }

    private static String[] getOptionalPermissionDesc(Activity activity) {
        return new String[0];
    }
}
