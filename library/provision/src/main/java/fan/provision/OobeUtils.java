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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package fan.provision;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.LayoutDirection;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.sevtinge.hyperceiler.provision.R;

import java.security.SecureRandom;
import java.util.Locale;

import fan.internal.utils.LiteUtils;
import fan.os.Build;

public class OobeUtils {

    private static final String TAG = "Provision_Utils";
    public static final String EXTRA_DEBUG_OOBE = "extra_debug_oobe";
    private static final String PREF_OOBE_STATE = "pref_oobe_state";
    private static final String PREF_OPERATOR_STATUS = "operator_status";
    private static final String PREF_HOOK_STATE = "hyperceiler_prefs";
    private static final String KEY_IS_PROVISIONED = "is_provisioned";
    private static final String KEY_ALLOW_HOOK = "prefs_key_allow_hook";

    public static float NO_ALPHA = 1.0f;
    public static float HALF_ALPHA = 0.5f;

    public static boolean isEndBoot = true;

    private static String sCurrentCode = ""; // 缓存当前的验证码

    public static boolean isProvisioned(Context context) {
        return context.getSharedPreferences(PREF_OOBE_STATE, Context.MODE_PRIVATE).getBoolean(KEY_IS_PROVISIONED, false);
    }

    public static boolean isDebugOobeMode(Context context) {
        if (!(context instanceof Activity activity)) {
            return false;
        }
        Intent intent = activity.getIntent();
        return intent != null && intent.getBooleanExtra(EXTRA_DEBUG_OOBE, false);
    }

    public static boolean shouldPersistOobeState(Context context) {
        return !isDebugOobeMode(context);
    }

    public static void resetOobeState(Context context) {
        context.getSharedPreferences(PREF_OOBE_STATE, Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences(PREF_OPERATOR_STATUS, Context.MODE_PRIVATE).edit().clear().apply();
        syncHookAvailability(context, false);
        sCurrentCode = "";
    }

    public static void setProvisioned(Context context, boolean provisioned) {
        context.getSharedPreferences(PREF_OOBE_STATE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_PROVISIONED, provisioned)
            .apply();
        syncHookAvailability(context, provisioned);
    }

    public static void syncHookAvailability(Context context) {
        syncHookAvailability(context, isProvisioned(context));
    }

    public static void syncHookAvailability(Context context, boolean allowHook) {
        Context protectedContext = context.createDeviceProtectedStorageContext();
        protectedContext.getSharedPreferences(PREF_HOOK_STATE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ALLOW_HOOK, allowHook)
            .apply();
    }

    public static boolean isRTL() {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == LayoutDirection.RTL;
    }

    public static boolean shouldNotFinishDefaultActivity() {
        return false;
    }

    public static boolean isGestureLineShow(Context context) {
        return context == null || Settings.Global.getInt(context.getContentResolver(), "hide_gesture_line", 0) == 0;
    }

    public static boolean isMiuiVersionLite() {
        return LiteUtils.isCommonLiteStrategy();
    }

    public static boolean isLiteOrLowDevice() {
        return isLowEndDevice() || isCpuOrGpuLowLevel();
    }

    public static boolean isLowEndDevice() {
        return false;
    }

    public static boolean isCpuOrGpuLowLevel() {
        return false;
    }

    public static boolean isMiuiSdkSupportFolme() {
        return true;
    }

    public static View getNextView(View view) {
        return view.findViewById(R.id.next);
    }

    public static View getNextView(Activity activity) {
        return ((ProvisionBaseActivity) activity).getNextButton();
    }

    public static boolean getOperatorState(Context context, String str) {
        if (isDebugOobeMode(context)) {
            return false;
        }
        return context.getSharedPreferences(PREF_OPERATOR_STATUS, 0).getBoolean(str, false);
    }

    public static void saveOperatorState(Context context, String str, boolean z) {
        if (isDebugOobeMode(context)) {
            return;
        }
        SharedPreferences.Editor edit = context.getSharedPreferences(PREF_OPERATOR_STATUS, 0).edit();
        edit.clear();
        edit.putBoolean(str, z);
        edit.apply();
    }


    public static boolean isInternationalBuild() {
        return Build.IS_INTERNATIONAL_BUILD;
    }

    public static void adaptFlipUi(Window window) {
        window.addFlags(134217728);
    }

    public static boolean isFoldLarge(Context context) {
        if (context == null) return false;
        int screenLayout = context.getResources().getConfiguration().screenLayout & 15;
        return screenLayout == 3 || screenLayout == 4;
    }

    public static boolean isPortOrientation(Context context) {
        return context != null && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public static boolean isLandOrientation(Context context) {
        return context != null && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean isTabletDevice() {
        return Build.IS_TABLET;
    }


    public static boolean isTabletLand(Context context) {
        return isLandOrientation(context) && isTabletDevice();
    }

    public static boolean isTabletPort(Context context) {
        return isPortOrientation(context) && isTabletDevice();
    }


    public static boolean needFastAnimation() {
        return !isInternationalBuild();
    }

    // 只有在需要“刷新”验证码时才调用这个（比如对话框显示前）
    public static String refreshSecureSixDigit() {
        SecureRandom sr = new SecureRandom();
        int num = 100000 + sr.nextInt(900000);
        sCurrentCode = String.valueOf(num);
        return sCurrentCode;
    }

    // 其他地方获取验证码，直接返回缓存的值
    public static String getSecureSixDigit() {
        if (sCurrentCode.isEmpty()) {
            return refreshSecureSixDigit(); // 如果为空则生成一次
        }
        return sCurrentCode;
    }

    public static Intent getLicenseIntent(String url) {
        Intent intent = new Intent("fan.intent.action.WEBVIEW");
        intent.putExtra("web_url", url);
        return intent;
    }

    public static void startActivity(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "ActivityNotFound", Toast.LENGTH_LONG).show();
            Log.e(TAG, "startActivity ActivityNotFound:", e);
        }
    }
}
