package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.sevtinge.hyperceiler.R;

import java.util.regex.Pattern;

import fan.os.SystemProperties;

public class AboutPhoneUtils {

    private static final String TAG = "AboutPhoneUtils";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[a-zA-Z][0-9]{1,3}$");

    public static String getXmsVersion() {
        return SystemProperties.get("persist.sys.xms.version");
    }

    public static String getRoXmsVersion() {
        return SystemProperties.get("ro.mi.xms.version.incremental");
    }

    public static String getOsVersionCode() {
        String str = SystemProperties.get("ro.mi.os.version.incremental", "");
        return (TextUtils.isEmpty(str) || !str.startsWith("OS") || str.length() <= 2) ? str : str.substring(2);
    }

    public static String addVersionSuffix(Context context) {
        String xmsVersion = getXmsVersion();
        String roXmsVersion = getRoXmsVersion();
        String osVersionCode = getOsVersionCode();

        boolean isXmsVersionValid = isValid(xmsVersion);
        boolean isRoXmsVersionValid = isValid(roXmsVersion);
        if (!isXmsVersionValid && !isRoXmsVersionValid) {
            return osVersionCode;
        }
        if (!isXmsVersionValid || isRoXmsVersionValid) {
            xmsVersion = (isXmsVersionValid || !isRoXmsVersionValid) ? compareValidVersion(xmsVersion, roXmsVersion) : roXmsVersion;
        }
        return insertSuffixBeforeBeta(context, osVersionCode, xmsVersion);
    }

    private static boolean isValid(String str) {
        return str != null && VERSION_PATTERN.matcher(str).matches();
    }

    private static String compareValidVersion(String s, String s2) {
        char lowerCase = Character.toLowerCase(s.charAt(0));
        char lowerCase2 = Character.toLowerCase(s2.charAt(0));
        if (lowerCase != lowerCase2) {
            if (lowerCase > lowerCase2) {
                return s;
            }
            return s2;
        }
        try {
            int int1 = Integer.parseInt(s.substring(1));
            try {
                if (int1 >= Integer.parseInt(s2.substring(1))) {
                    return s;
                }
                return s2;
            } catch (Exception e) {
                Log.d(TAG, "ParseInt err: " + e);
                return s;
            }
        } catch (final Exception e) {
            Log.d(TAG, "ParseInt err: " + e);
            return s2;
        }
    }

    private static String insertSuffixBeforeBeta(Context context, String str, String str2) {
        String str3 = null;
        String string = context != null ? context.getString(R.string.developer_build) : null;
        if (string != null && str.endsWith(string)) {
            str3 = string;
        }
        if (str.endsWith("Beta")) {
            str3 = "Beta";
        }
        if (str3 != null) {
            return str.substring(0, str.length() - str3.length()).trim() + "." + str2 + " " + str3;
        }
        return str + "." + str2;
    }
}
