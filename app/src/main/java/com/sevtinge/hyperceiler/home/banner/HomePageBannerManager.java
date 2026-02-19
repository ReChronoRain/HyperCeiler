package com.sevtinge.hyperceiler.home.banner;

import static com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper.mNotInSelectedScope;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Module.scanModules;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.SUPPORT_FULL;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getBaseOs;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getHost;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getRomAuthor;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSupportStatus;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSystemVersionIncremental;
import static com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isRelease;
import static com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp;
import static com.sevtinge.hyperceiler.libhook.utils.log.LogManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils.checkRootPermission;

import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.TextView;

import androidx.preference.PreferenceCategory;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.prefs.LayoutPreference;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import kotlin.text.Charsets;

public class HomePageBannerManager {

    /**
     * 核心方法：判断并返回所有需要显示的本地 Banner 数据
     */
    public static List<BannerBean> getLocalBannerBeans(Context context) {
        List<BannerBean> list = new ArrayList<>();

        // FuckCoolapkSDay
        if (isFuckCoolapkSDay()) {
            list.add(createLocalTipBean(
                "fuck_coolapk",
                context.getString(R.string.headtip_tip_fuck_coolapk),
                -1,
                null));
        }

        // Birthday
        if (isBirthday()) {
            list.add(createLocalTipBean(
                "happy_birthday",
                context.getString(R.string.happy_birthday_hyperceiler),
                com.sevtinge.hyperceiler.core.R.drawable.ic_hyperceiler_cartoon,
                null));
        }

        // LoggerAlive
        if (isLoggerAlive()) {
            list.add(createLocalNoticeBean(
                "dead_logger",
                context.getString(R.string.headtip_notice_dead_logger),
                null));
        }

        boolean isUnofficialRom = isUnofficialRom(context);
        boolean isFullSupport = getSupportStatus() == SUPPORT_FULL;
        boolean isWhileXposed = isWhileXposed();
        boolean isSignPass = SignUtils.isSignCheckPass(context);

        int titleResId = !isSignPass ? com.sevtinge.hyperceiler.core.R.string.headtip_warn_sign_verification_failed :
            isUnofficialRom ? com.sevtinge.hyperceiler.core.R.string.headtip_warn_not_offical_rom :
                !isWhileXposed ? com.sevtinge.hyperceiler.core.R.string.headtip_warn_unsupport_xposed :
                    !isFullSupport ? com.sevtinge.hyperceiler.core.R.string.headtip_warn_unsupport_sysver : -1;

        list.add(createLocalWarningBean(
            "warning",
            titleResId != -1 ? context.getString(titleResId) : null,
            null));

        if (isSupportAutoSafeMode()) {
            list.add(createLocalTipBean(
                "auto_safe_mode",
                context.getString(R.string.headtip_tip_auto_safe_mode),
                null));
        }

        return list;
    }

    private static BannerBean createLocalTipBean(String id, String summary, int iconRes, String actionOrUrl) {
        BannerBean bean = createLocalBean(id, null, summary, iconRes, actionOrUrl);
        bean.setTitleColor("#fc5b8d");
        bean.setSummaryColor("#fc5b8d");
        bean.setBackgroundColorResId(com.sevtinge.hyperceiler.core.R.drawable.headtip_hyperceiler_background);
        return bean;
    }

    private static BannerBean createLocalNoticeBean(String id, String summary, String actionOrUrl) {
        BannerBean bean = createLocalBean(id, null, summary, -1, actionOrUrl);
        bean.setTitleColor("#EDA306");
        bean.setSummaryColor("#EDA306");
        bean.setBackgroundColorResId(com.sevtinge.hyperceiler.core.R.drawable.headtip_notice_background);
        return bean;
    }

    private static BannerBean createLocalWarningBean(String id, String summary, String actionOrUrl) {
        BannerBean bean = createLocalBean(id, null, summary, -1, actionOrUrl);
        bean.setTitleColor("#FF0000");
        bean.setSummaryColor("#FF0000");
        bean.setBackgroundColorResId(com.sevtinge.hyperceiler.core.R.drawable.headtip_warn_background);
        return bean;
    }

    private static BannerBean createLocalTipBean(String id, String summary, String actionOrUrl) {
        BannerBean bean = createLocalBean(id, null, summary, -1, actionOrUrl);
        bean.setTitleColor("#0D84FF");
        bean.setSummaryColor("#0D84FF");
        bean.setBackgroundColorResId(com.sevtinge.hyperceiler.core.R.drawable.headtip_tip_background);
        return bean;
    }

    private static BannerBean createLocalBean(String id, String title, String summary, int iconRes, String actionOrUrl) {
        BannerBean bean = new BannerBean();
        bean.setId(id);
        bean.setTitle(title);
        bean.setSummary(summary);
        bean.setIconResId(iconRes);
        bean.setPriority(1001);
        // 如果是 URL 自动识别
        if (actionOrUrl != null && actionOrUrl.startsWith("http")) {
            bean.setUrl(actionOrUrl);
        } else {
            bean.setAction(actionOrUrl);
        }
        return bean;
    }

    private static boolean isFuckCoolapkSDay() {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        return currentMonth == Calendar.JULY && currentDay == 14;
    }

    private static boolean isBirthday() {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        return currentMonth == Calendar.MAY && currentDay == 1;
    }

    private static boolean isLoggerAlive() {
        return !IS_LOGGER_ALIVE && !isRelease();
    }

    private static boolean isSupportAutoSafeMode() {
        return mNotInSelectedScope.contains("android");
    }

    public static boolean isUnofficialRom(Context context) {
        String baseOs = getBaseOs();
        String romAuthor = getRomAuthor();
        String systemVersion = getSystemVersionIncremental();
        String host = getHost();

        boolean isNotCustomBaseOs = !baseOs.startsWith("V") &&
            !baseOs.startsWith("Xiaomi") &&
            !baseOs.startsWith("Redmi") &&
            !baseOs.startsWith("POCO") &&
            !"null".equals(baseOs);

        boolean hasRomAuthor = !romAuthor.isEmpty();

        boolean isSystemVersionContains = systemVersion.contains("江南") || systemVersion.contains("月色");

        boolean isNotCustomHost = !host.startsWith("pangu-build-component-system") &&
            !host.startsWith("builder-system") &&
            !host.startsWith("non-pangu-pod") &&
            !host.equals("xiaomi.com");

        boolean hasAdvSettings = isAppInstalled(context, "com.baiyang.settings");

        boolean hasBaiyangLicense = !Objects.equals(getProp("ro.system.baiyang.license", ""), "");

        boolean hasCharacteristics = Objects.equals(getProp("ro.kernel.android.checkjni", ""), "0") &&
            Objects.equals(getProp("ro.kernel.checkjni", ""), "0") &&
            Objects.equals(getProp("vendor.bluetooth.startbtlogger", ""), "false") &&
            Objects.equals(getProp("persist.sys.offlinelog.kernel", ""), "false") &&
            (Objects.equals(getProp("persist.sys.offlinelog.bootlog", ""), "false") || Objects.equals(getProp("persist.sys.offlinelog.bootlog", ""), "=false")) &&
            Objects.equals(getProp("sys.miui.ndcd", ""), "off");

        return hasRomAuthor || isSystemVersionContains || Objects.equals(host, "xiaomi.eu") || (isNotCustomBaseOs && isNotCustomHost) || hasAdvSettings || hasBaiyangLicense || hasCharacteristics;
    }

    private static boolean isWhileXposed() {
        if (checkRootPermission() != 0) return true; // 没 root 就别走校验了
        try {
            List<DeviceHelper.Module.ModuleInfo> module = scanModules("/data/adb/modules", Charsets.UTF_8);
            String moduleName = module.getFirst().extractName();
            if (moduleName.contains("nolog") || moduleName.contains("日志")) {
                return false;
            }
            return moduleName.contains("LSPosed IT") || moduleName.equals("LSPosed - Irena");
        } catch (Throwable e) {
            AndroidLog.e("isWhileXposed", e);
            return true;
        }
    }


    private static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}

