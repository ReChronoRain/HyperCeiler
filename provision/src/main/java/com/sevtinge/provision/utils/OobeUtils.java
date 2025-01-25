package com.sevtinge.provision.utils;

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

import com.sevtinge.provision.R;

import java.util.Locale;

import fan.core.os.BuildCompat;
import fan.internal.utils.LiteUtils;
import fan.os.Build;

public class OobeUtils {

    public static float NO_ALPHA = 1.0f;
    public static float HALF_ALPHA = 0.5f;
    public static final String BUILD_DEVICE = android.os.Build.DEVICE;
    public static boolean IS_SUPPORT_WELCOM_ANIM = !isMiuiVersionLite();

    public static boolean isFirstBoot = true;
    public static boolean isEndBoot = true;

    public static boolean isProvisioned(Context context) {
        return false;
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
        return activity.findViewById(R.id.confirm_button);
    }

    public static boolean getOperatorState(Context context, String str) {
        return context.getSharedPreferences("operator_status", 0).getBoolean(str, false);
    }

    public static void saveOperatorState(Context context, String str, boolean z) {
        SharedPreferences.Editor edit = context.getSharedPreferences("operator_status", 0).edit();
        edit.clear();
        edit.putBoolean(str, z);
        edit.apply();
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
            Log.e("Provision_Utils", "startActivity ActivityNotFound:", e);
        }
    }

    public static boolean isInternationalBuild() {
        return Build.IS_INTERNATIONAL_BUILD;
    }

    public static boolean isFoldLarge(Context context) {
        if (context == null) {
            return false;
        }
        int screenLayout = context.getResources().getConfiguration().screenLayout & 15;
        return screenLayout == 3 || screenLayout == 4;
    }

    public static void adaptFlipUi(Window window) {
        window.addFlags(134217728);
    }

    public static boolean isTabletLand(Context context) {
        return isLandOrientation(context) && isTabletDevice();
    }

    public static boolean isLandOrientation(Context context) {
        return context != null && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }
    public static boolean isTabletDevice() {
        return Build.IS_TABLET;
    }

    public static boolean needFastAnimation() {
        return !isInternationalBuild();
    }

}
