package com.sevtinge.hyperceiler.utils.banner;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getBaseOs;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getRomAuthor;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isFullSupport;
import static com.sevtinge.hyperceiler.common.prefs.PreferenceHeader.notInSelectedScope;
import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.IS_LOGGER_ALIVE;

import android.content.Context;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt;
import com.sevtinge.hyperceiler.hook.utils.log.LogManager;
import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.common.prefs.LayoutPreference;

import java.util.Calendar;
import java.util.Objects;

public class HomePageBannerHelper {

    public static void init(Context context, PreferenceCategory preference) {
        new HomePageBannerHelper(context, preference);
    }

    public HomePageBannerHelper(Context context, PreferenceCategory preference) {
        // 优先级由上往下递减，优先级低的会被覆盖执行
        // HyperCeiler
        isFuckCoolapkSDay(context, preference);
        // Birthday
        isBirthday(context, preference);
        // Notice
        isLoggerAlive(context, preference);
        // Warn
        checkWarnings(context, preference);
        // Tip
        isSupportAutoSafeMode(context, preference);
    }

    private void isFuckCoolapkSDay(Context context, PreferenceCategory preference) {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        if (currentMonth == Calendar.JULY && currentDay == 14) {
            preference.addPreference(createBannerPreference(
                context,
                R.layout.headtip_hyperceiler
            ));
        }
    }

    private void isBirthday(Context context, PreferenceCategory preference) {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        if (currentMonth == Calendar.MAY && currentDay == 1) {
            preference.addPreference(createBannerPreference(
                context,
                R.layout.headtip_hyperceiler_birthday
            ));
        }
    }

    private void isLoggerAlive(Context context, PreferenceCategory preference) {
        if (!IS_LOGGER_ALIVE && !BuildConfig.BUILD_TYPE.equals("release")) {
            preference.addPreference(createBannerPreference(
                context,
                R.layout.headtip_notice
            ));
        }
    }

    private void checkWarnings(Context context, PreferenceCategory preference) {
        boolean isOfficialRom = getIsOfficialRom();
        boolean isFullSupport = isFullSupport();
        boolean isSignPass = SignUtils.isSignCheckPass(context);

        if (!isSignPass || !isFullSupport || isOfficialRom) {
            LayoutPreference layoutPreference = (LayoutPreference) createBannerPreference(
                context,
                R.layout.headtip_warn
            );
            TextView titleView = layoutPreference.findViewById(android.R.id.title);
            if (!isSignPass) {
                titleView.setText(R.string.headtip_warn_sign_verification_failed);
            } else if (isOfficialRom) {
                titleView.setText(R.string.headtip_warn_not_offical_rom);
            } else if (!isFullSupport) {
                titleView.setText(R.string.headtip_warn_unsupport_sysver);
            }
            preference.addPreference(layoutPreference);
        }
    }

    private boolean getIsOfficialRom() {
        String baseOs = getBaseOs();
        String romAuthor = getRomAuthor();
        String host = SystemSDKKt.getHost();

        boolean isNotCustomBaseOs = !baseOs.startsWith("V") &&
            !baseOs.startsWith("Xiaomi") &&
            !baseOs.startsWith("Redmi") &&
            !baseOs.startsWith("POCO") &&
            !"null".equals(baseOs);

        boolean hasRomAuthor = !romAuthor.isEmpty();

        boolean isNotCustomHost = !host.startsWith("pangu-build-component-system") &&
            !host.startsWith("builder-system") &&
            !host.startsWith("non-pangu-pod") &&
            !host.equals("xiaomi.com");

        return hasRomAuthor || Objects.equals(host, "xiaomi.eu") || (isNotCustomBaseOs && isNotCustomHost);
    }



    private void isSupportAutoSafeMode(Context context, PreferenceCategory preference) {
        if (notInSelectedScope.contains("android")) {
            preference.addPreference(createBannerPreference(
                context,
                R.layout.headtip_tip
            ));
        }
    }

    private Preference createBannerPreference(Context context, int layoutResId) {
        return new LayoutPreference(context, layoutResId);
    }
}
