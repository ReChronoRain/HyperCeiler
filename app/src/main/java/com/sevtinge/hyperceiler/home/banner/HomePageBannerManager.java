package com.sevtinge.hyperceiler.home.banner;

import static com.sevtinge.hyperceiler.common.log.LogStatusManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.common.utils.ShellUtils.checkRootPermission;
import static com.sevtinge.hyperceiler.common.utils.api.ProjectApi.isRelease;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Module.scanModules;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.SUPPORT_FULL;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getBaseOs;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getHost;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getRomAuthor;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSupportStatus;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSystemVersionIncremental;
import static com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp;
import static com.sevtinge.hyperceiler.utils.LSPosedScopeHelper.mNotInSelectedScope;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.log.LoggerHealthChecker;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashScope;
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import kotlin.text.Charsets;

public class HomePageBannerManager {

    private static final Object CACHE_LOCK = new Object();
    private static final long BANNER_CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final int DEFAULT_BANNER_PRIORITY = 1001;
    private static final int SAFE_MODE_BANNER_PRIORITY = 1100;
    private static final String SAFE_MODE_BANNER_ID = "safe_mode_active";
    private static final String AUTO_SAFE_MODE_BANNER_ID = "auto_safe_mode";
    private static final Map<String, Integer> SAFE_MODE_APP_NAME_RES_MAP = Map.of(
        "com.android.systemui", com.sevtinge.hyperceiler.core.R.string.system_ui,
        "com.android.settings", com.sevtinge.hyperceiler.core.R.string.system_settings,
        "com.miui.home", com.sevtinge.hyperceiler.core.R.string.mihome,
        "com.hchen.demo", R.string.demo,
        "com.miui.securitycenter", com.sevtinge.hyperceiler.core.R.string.security_center_hyperos
    );
    private static volatile List<BannerBean> sCachedBannerList;
    private static volatile long sCachedAtUptimeMs = 0L;
    private static volatile String sCachedSafeModeState = "";
    private static volatile Runnable sRefreshCallback;

    public static void setRefreshCallback(Runnable callback) {
        sRefreshCallback = callback;
    }

    public static void requestRefresh() {
        Runnable callback = sRefreshCallback;
        if (callback != null) {
            callback.run();
        }
    }

    public static boolean updateSafeModeCache(Context context) {
        String currentSafeModeState = getCurrentSafeModeState();
        BannerBean safeModeBanner = createSafeModeBanner(context);
        synchronized (CACHE_LOCK) {
            if (sCachedBannerList == null) {
                return false;
            }

            List<BannerBean> updatedBanners = new ArrayList<>(sCachedBannerList.size() + 1);
            for (BannerBean banner : sCachedBannerList) {
                if (!SAFE_MODE_BANNER_ID.equals(banner.getId())) {
                    updatedBanners.add(banner);
                }
            }

            insertSafeModeBanner(updatedBanners, safeModeBanner);
            updateCache(updatedBanners, SystemClock.uptimeMillis(), currentSafeModeState);
            return true;
        }
    }

    public static void invalidateCache() {
        synchronized (CACHE_LOCK) {
            sCachedBannerList = null;
            sCachedAtUptimeMs = 0L;
            sCachedSafeModeState = "";
        }
    }

    public static boolean needsRefresh() {
        long now = SystemClock.uptimeMillis();
        return !isCacheValid(now, getCurrentSafeModeState());
    }

    /**
     * 核心方法：判断并返回所有需要显示的本地 Banner 数据
     */
    public static List<BannerBean> getLocalBannerBeans(Context context) {
        long now = SystemClock.uptimeMillis();
        String currentSafeModeState = getCurrentSafeModeState();
        if (isCacheValid(now, currentSafeModeState)) {
            return getCachedBannerCopy();
        }

        synchronized (CACHE_LOCK) {
            now = SystemClock.uptimeMillis();
            currentSafeModeState = getCurrentSafeModeState();
            if (isCacheValid(now, currentSafeModeState)) {
                return getCachedBannerCopy();
            }

            updateCache(buildBannerList(context), now, currentSafeModeState);
            return getCachedBannerCopy();
        }
    }

    private static boolean isCacheValid(long now, String safeModeState) {
        return sCachedBannerList != null
            && now - sCachedAtUptimeMs < BANNER_CACHE_TTL_MS
            && Objects.equals(sCachedSafeModeState, safeModeState);
    }

    private static List<BannerBean> getCachedBannerCopy() {
        return new ArrayList<>(sCachedBannerList);
    }

    private static void updateCache(List<BannerBean> banners, long now, String safeModeState) {
        sCachedBannerList = List.copyOf(banners);
        sCachedAtUptimeMs = now;
        sCachedSafeModeState = safeModeState;
    }

    private static List<BannerBean> buildBannerList(Context context) {
        List<BannerBean> banners = new ArrayList<>();
        addFestivalBanners(context, banners);
        addStatusBanners(context, banners);
        addBannerIfPresent(banners, createSafeModeBanner(context));
        if (isSupportAutoSafeMode()) {
            banners.add(createInfoTipBanner(
                "auto_safe_mode",
                context.getString(com.sevtinge.hyperceiler.core.R.string.headtip_tip_auto_safe_mode),
                null));
        }
        return banners;
    }

    private static void addFestivalBanners(Context context, List<BannerBean> banners) {
        if (isFuckCoolapkSDay()) {
            banners.add(createHyperCeilerTipBanner(
                "fuck_coolapk",
                context.getString(com.sevtinge.hyperceiler.core.R.string.headtip_tip_fuck_coolapk),
                -1,
                null));
        }

        if (isBirthday()) {
            banners.add(createHyperCeilerTipBanner(
                "happy_birthday",
                context.getString(com.sevtinge.hyperceiler.core.R.string.happy_birthday_hyperceiler),
                com.sevtinge.hyperceiler.core.R.drawable.ic_hyperceiler_cartoon,
                null));
        }
    }

    private static void addStatusBanners(Context context, List<BannerBean> banners) {
        if (isLoggerAlive()) {
            banners.add(createNoticeBanner(
                "dead_logger",
                context.getString(com.sevtinge.hyperceiler.core.R.string.headtip_notice_dead_logger),
                null));
        }

        int titleResId = resolveWarningTitleResId(context);
        if (titleResId != -1) {
            banners.add(createWarningBanner(
                "warning",
                context.getString(titleResId),
                null));
        }
    }

    private static int resolveWarningTitleResId(Context context) {
        boolean isUnofficialRom = isUnofficialRom(context);
        boolean isFullSupport = getSupportStatus() == SUPPORT_FULL;
        boolean isWhileXposed = isWhileXposed();
        boolean isSignPass = SignUtils.isSignCheckPass(context);

        return !isSignPass ? R.string.headtip_warn_sign_verification_failed :
            isUnofficialRom ? R.string.headtip_warn_not_offical_rom :
                !isWhileXposed ? R.string.headtip_warn_unsupport_xposed :
                    !isFullSupport ? R.string.headtip_warn_unsupport_sysver : -1;
    }

    private static void addBannerIfPresent(List<BannerBean> banners, BannerBean banner) {
        if (banner != null) {
            banners.add(banner);
        }
    }

    private static void insertSafeModeBanner(List<BannerBean> banners, BannerBean safeModeBanner) {
        if (safeModeBanner == null) {
            return;
        }

        int insertIndex = findBannerIndex(banners, AUTO_SAFE_MODE_BANNER_ID);
        if (insertIndex == -1) {
            banners.add(safeModeBanner);
        } else {
            banners.add(insertIndex, safeModeBanner);
        }
    }

    private static int findBannerIndex(List<BannerBean> banners, String bannerId) {
        for (int i = 0; i < banners.size(); i++) {
            if (Objects.equals(bannerId, banners.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private static BannerBean createHyperCeilerTipBanner(String id, String summary, int iconRes, String actionOrUrl) {
        return applyStyle(
            createBanner(id, null, summary, iconRes, actionOrUrl),
            com.sevtinge.hyperceiler.core.R.color.headtip_hyperceiler_text_color,
            com.sevtinge.hyperceiler.core.R.drawable.headtip_hyperceiler_background
        );
    }

    private static BannerBean createNoticeBanner(String id, String summary, String actionOrUrl) {
        return applyStyle(
            createBanner(id, null, summary, -1, actionOrUrl),
            com.sevtinge.hyperceiler.core.R.color.headtip_notice_text_color,
            com.sevtinge.hyperceiler.core.R.drawable.headtip_notice_background
        );
    }

    private static BannerBean createWarningBanner(String id, String summary, String actionOrUrl) {
        return applyStyle(
            createBanner(id, null, summary, -1, actionOrUrl),
            com.sevtinge.hyperceiler.core.R.color.headtip_warn_text_color,
            com.sevtinge.hyperceiler.core.R.drawable.headtip_warn_background
        );
    }

    private static BannerBean createSafeModeBanner(Context context) {
        List<String> crashingPackages;
        try {
            crashingPackages = CrashScope.getCrashingPackages();
        } catch (Throwable e) {
            AndroidLog.e("HomePageBannerManager", "Failed to read safe mode state", e);
            return null;
        }
        if (crashingPackages.isEmpty()) {
            return null;
        }

        String appNames = buildSafeModeAppNames(context, crashingPackages);
        if (appNames.isEmpty()) {
            return null;
        }

        BannerBean bean = createWarningBanner(
            SAFE_MODE_BANNER_ID,
            context.getString(R.string.safe_mode_banner_desc, appNames),
            BannerCallback.ACTION_OPEN_SAFE_MODE_SETTINGS
        );
        bean.setTitle(context.getString(R.string.safe_mode_banner_title));
        bean.setPriority(SAFE_MODE_BANNER_PRIORITY);
        return bean;
    }

    private static BannerBean createInfoTipBanner(String id, String summary, String actionOrUrl) {
        return applyStyle(
            createBanner(id, null, summary, -1, actionOrUrl),
            com.sevtinge.hyperceiler.core.R.color.headtip_tip_text_color,
            com.sevtinge.hyperceiler.core.R.drawable.headtip_tip_background
        );
    }

    private static BannerBean applyStyle(BannerBean bean, int textColorResId, int backgroundResId) {
        bean.setTitleColorResId(textColorResId);
        bean.setSubTitleColorResId(textColorResId);
        bean.setBackgroundColorResId(backgroundResId);
        return bean;
    }

    private static BannerBean createBanner(String id, String title, String summary, int iconRes, String actionOrUrl) {
        BannerBean bean = new BannerBean();
        bean.setId(id);
        bean.setTitle(title);
        bean.setSummary(summary);
        bean.setIconResId(iconRes);
        bean.setPriority(DEFAULT_BANNER_PRIORITY);
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

    private static String buildSafeModeAppNames(Context context, List<String> crashingPackages) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String pkg : crashingPackages) {
            Integer resId = SAFE_MODE_APP_NAME_RES_MAP.get(pkg);
            if (resId != null) {
                joiner.add(context.getString(resId) + " (" + pkg + ")");
            } else {
                joiner.add(pkg);
            }
        }
        return joiner.toString();
    }

    private static String getCurrentSafeModeState() {
        return getProp(CrashScope.PROP_SAFE_MODE, "");
    }

}
