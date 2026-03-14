package com.sevtinge.hyperceiler.home.banner;

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
import static com.sevtinge.hyperceiler.utils.LSPosedScopeHelper.mNotInSelectedScope;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.log.LoggerHealthChecker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import kotlin.text.Charsets;

public class HomePageBannerManager {

    private static final Object CACHE_LOCK = new Object();
    private static final long BANNER_CACHE_TTL_MS = 5 * 60 * 1000L;
    private static volatile List<BannerBean> sCachedBannerList;
    private static volatile long sCachedAtUptimeMs = 0L;

    /**
     * 核心方法：判断并返回所有需要显示的本地 Banner 数据
     */
    public static List<BannerBean> getLocalBannerBeans(Context context) {
        long now = SystemClock.uptimeMillis();
        List<BannerBean> cached = sCachedBannerList;
        if (cached != null && now - sCachedAtUptimeMs < BANNER_CACHE_TTL_MS) {
            return new ArrayList<>(cached);
        }

        synchronized (CACHE_LOCK) {
            cached = sCachedBannerList;
            now = SystemClock.uptimeMillis();
            if (cached != null && now - sCachedAtUptimeMs < BANNER_CACHE_TTL_MS) {
                return new ArrayList<>(cached);
            }

            List<BannerBean> list = new ArrayList<>();

            if (isFuckCoolapkSDay()) {
                list.add(createLocalTipBean(
                    "fuck_coolapk",
                    context.getString(com.sevtinge.hyperceiler.core.R.string.headtip_tip_fuck_coolapk),
                    -1,
                    null));
            }

            if (isBirthday()) {
                list.add(createLocalTipBean(
                    "happy_birthday",
                    context.getString(com.sevtinge.hyperceiler.core.R.string.happy_birthday_hyperceiler),
                    com.sevtinge.hyperceiler.core.R.drawable.ic_hyperceiler_cartoon,
                    null));
            }

            if (isLoggerAlive()) {
                list.add(createLocalNoticeBean(
                    "dead_logger",
                    context.getString(com.sevtinge.hyperceiler.core.R.string.headtip_notice_dead_logger),
                    null));
            }

            boolean isUnofficialRom = isUnofficialRom(context);
            boolean isFullSupport = getSupportStatus() == SUPPORT_FULL;
            boolean isWhileXposed = isWhileXposed();
            boolean isSignPass = SignUtils.isSignCheckPass(context);

            int titleResId = !isSignPass ? R.string.headtip_warn_sign_verification_failed :
                isUnofficialRom ? R.string.headtip_warn_not_offical_rom :
                    !isWhileXposed ? R.string.headtip_warn_unsupport_xposed :
                        !isFullSupport ? R.string.headtip_warn_unsupport_sysver : -1;

            if (titleResId != -1) {
                list.add(createLocalWarningBean(
                    "warning",
                    context.getString(titleResId),
                    null));
            }

            if (isSupportAutoSafeMode()) {
                list.add(createLocalTipBean(
                    "auto_safe_mode",
                    context.getString(com.sevtinge.hyperceiler.core.R.string.headtip_tip_auto_safe_mode),
                    null));
            }

            sCachedBannerList = List.copyOf(list);
            sCachedAtUptimeMs = now;
            return new ArrayList<>(sCachedBannerList);
        }
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
        if (isRelease()) return false;
        if ("NOT_CHECKED".equals(LoggerHealthChecker.diagSummary)) return false;
        return !IS_LOGGER_ALIVE;
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
