package com.sevtinge.hyperceiler.home;

import android.app.Activity;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class CrashReportManager {
    private static final Map<String, Integer> NAME_MAP = Map.of(
        "com.android.systemui", R.string.system_ui,
        "com.android.settings", R.string.system_settings,
        "com.miui.home", R.string.mihome,
        "com.miui.securitycenter", R.string.security_center_hyperos
    );

    public static List<String> getCrashList() {
        try {
            List<?> raw = CrashScope.getCrashingPackages();
            return raw.stream().map(Object::toString).toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static void handleSafeMode(Activity activity, List<String> crashes) {
        if (crashes.isEmpty()) return;

        StringJoiner sj = new StringJoiner(", ");
        for (String pkg : crashes) {
            Integer res = NAME_MAP.get(pkg);
            if (res != null) sj.add(activity.getString(res) + " (" + pkg + ")");
        }

        String names = sj.toString();
        if (names.isEmpty()) return;

        String msg = clean(activity.getString(R.string.safe_mode_later_desc, names));
        DialogHelper.showSafeModeDialog(activity, msg);
    }

    private static String clean(String s) {
        return s.replaceAll("[\\[\\]]", "").replaceAll("\\s{2,}", " ").trim();
    }
}
