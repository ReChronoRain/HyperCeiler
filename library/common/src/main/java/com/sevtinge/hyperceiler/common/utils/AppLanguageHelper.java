package com.sevtinge.hyperceiler.common.utils;

import android.app.Activity;
import android.app.LocaleManager;
import android.content.Context;
import android.os.LocaleList;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.common.R;
import com.sevtinge.hyperceiler.common.log.AndroidLog;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 语言切换工具类。
 *
 * <p>实现原则：尽量不造轮子，直接使用 Android 13+ 的 per-app language 平台 API：
 * {@link LocaleManager#setApplicationLocales(LocaleList)} /
 * {@link LocaleManager#getApplicationLocales()}。系统会自动持久化用户选择、
 * 重启 Activity、同步 Configuration 与 Locale.getDefault()，无需我们再做
 * attachBaseContext 包装、Locale.setDefault、resources.updateConfiguration、
 * Activity#recreate 等手工操作。
 *
 * <p>同时把所选索引镜像写入 {@link AppSettingsStore}，仅用于让 Xposed 模块端
 * 通过 PrefsBridge 读取，不参与 UI 进程的语言应用流程。
 */
public class AppLanguageHelper {
    private static final String TAG = "AppLanguageHelper";

    public static final String[] APP_LANGUAGES = {
        "en", "zh_CN", "zh_TW", "zh_HK", "ja_JP", "pl_PL", "ru_RU", "ar_SA", "es_ES",
        "pt_BR", "id_ID", "tr_TR", "vi_VN", "it_IT", "de_DE", "uk_UA", "zh_ME"
    };
    private static final int[] APP_LANGUAGE_LABELS = {
        R.string.settings_app_en,
        R.string.settings_app_zh_cn,
        R.string.settings_app_zh_tw,
        R.string.settings_app_zh_hk,
        R.string.settings_app_ja_jp,
        R.string.settings_app_pl_pl,
        R.string.settings_app_ru_ru,
        R.string.settings_app_ar_sa,
        R.string.settings_app_es_es,
        R.string.settings_app_pt_br,
        R.string.settings_app_in_id,
        R.string.settings_app_tr_tr,
        R.string.settings_app_vi_vn,
        R.string.settings_app_it_it,
        R.string.settings_app_de_de,
        R.string.settings_app_uk_ua,
        R.string.settings_app_zh_me
    };

    private static final Map<String, Locale> localeCache = new HashMap<>();
    private static final Locale[] indexLocaleCache = new Locale[APP_LANGUAGES.length];

    private AppLanguageHelper() {
    }

    // ============================================================
    //  Public API — 调用方原有签名保持不变，便于平滑迁移
    // ============================================================

    /**
     * 兼容入口：旧实现需要在 Application 启动时手工把语言写回 Resources。
     * 平台 LocaleManager 会自动完成这件事，这里只把已选语言镜像写回
     * {@link AppSettingsStore}（避免与系统的 per-app locale 不一致）。
     */
    public static void init(Context context) {
        syncStoredIndexFromSystem(context);
    }

    public static void init(Activity activity) {
        init((Context) activity);
    }

    public static void applyLanguage(Context context, int index) {
        int normalized = normalizeLanguageIndex(index);
        AppSettingsStore.setAppLanguageIndex(context, normalized);
        applySystemLocales(context, localeFromIndex(normalized));
    }

    public static void setLanguage(Context context, String language) {
        setLanguage(context, getCachedLocale(language, ""));
    }

    public static void setLanguage(Context context, String language, String country) {
        setLanguage(context, getCachedLocale(language, country));
    }

    public static void setLanguage(Context context, Locale locale) {
        // 把外部传入的 Locale 落到 APP_LANGUAGES 中最匹配的索引，再走规范化的 Locale 应用，
        // 避免外部 Locale 缺少 region/script 时切换结果偏移。
        int idx = findBestLanguageIndex(locale);
        applyLanguage(context, idx);
    }

    @SuppressWarnings("unused")
    public static void setIndexLanguage(Activity activity, int index, boolean recreate) {
        // recreate 参数已无用：LocaleManager.setApplicationLocales() 自身会重启 Activity
        // （或在 handleConfigChanges=locale 的场景保持当前 Activity）。保留签名仅为兼容旧调用。
        applyLanguage(activity, index);
    }

    public static void clearLanguage(Context context) {
        AppSettingsStore.clearAppLanguageIndex(context);
        // 传入空 LocaleList 让系统回到“跟随系统语言”。
        applySystemLocales(context, null);
    }

    /**
     * OOBE 完成时调用：如果用户从未显式选过语言，把当前生效的语言固化到 App 上，
     * 避免后续系统语言变化把 App 显示语言带偏。
     *
     * <p>实现：读取系统当前生效的 {@link LocaleList}（即 OOBE 期间用户在引导页
     * 选择的那一项；若未选择则为系统语言），落到 APP_LANGUAGES 最佳匹配项后写回。
     */
    public static void freezeCurrentLocaleIfUnset(Context context) {
        if (hasExplicitLanguage(context)) {
            return;
        }
        int idx = findBestLanguageIndex(getCurrentLocale(context));
        applyLanguage(context, idx);
    }

    // ============================================================
    //  Query API — 供 UI / 搜索 / 通知 / Xposed 模块端等使用
    // ============================================================

    public static String getLanguage(Context context) {
        Locale locale = getCurrentLocale(context);
        String country = locale.getCountry();
        return TextUtils.isEmpty(country) ? locale.getLanguage() : locale.getLanguage() + "_" + country;
    }

    public static Locale getCurrentLocale(Context context) {
        // 1) 优先读系统的 per-app locale —— 这才是“当前真正生效”的语言。
        LocaleList applied = getApplicationLocalesSafe(context);
        if (applied != null && !applied.isEmpty()) {
            return applied.get(0);
        }
        // 2) 否则回落到 Configuration —— 跟随系统语言时由系统回填。
        if (context != null) {
            LocaleList configurationLocales = context.getResources().getConfiguration().getLocales();
            if (!configurationLocales.isEmpty()) {
                return configurationLocales.get(0);
            }
        }
        return Locale.getDefault();
    }

    public static int getCurrentLanguageIndex(Context context) {
        LocaleList applied = getApplicationLocalesSafe(context);
        if (applied != null && !applied.isEmpty()) {
            return findBestLanguageIndex(applied.get(0));
        }
        // 跟随系统语言时仍按系统语言匹配最近的列表项作为 picker 默认值。
        return findBestLanguageIndex(getCurrentLocale(context));
    }

    public static CharSequence[] getLanguageEntries(Context context) {
        CharSequence[] entries = new CharSequence[APP_LANGUAGE_LABELS.length];
        for (int i = 0; i < APP_LANGUAGE_LABELS.length; i++) {
            entries[i] = context.getText(APP_LANGUAGE_LABELS[i]);
        }
        return entries;
    }

    public static CharSequence[] getLanguageEntryValues() {
        CharSequence[] values = new CharSequence[APP_LANGUAGES.length];
        for (int i = 0; i < APP_LANGUAGES.length; i++) {
            values[i] = Integer.toString(i);
        }
        return values;
    }

    public static boolean hasExplicitLanguage(Context context) {
        LocaleList applied = getApplicationLocalesSafe(context);
        return applied != null && !applied.isEmpty();
    }

    /**
     * attachBaseContext 兼容入口。
     *
     * <p>Android 13+ per-app language 由系统在 Activity 创建前注入 Configuration，
     * 不再需要我们手工 createConfigurationContext。这里直接返回 base，保留方法仅为
     * 让既有 attachBaseContext 调用点无需修改即可继续工作。
     */
    public static Context wrapContext(Context base) {
        return base;
    }

    // ============================================================
    //  Helpers — Locale 索引映射 / 查找
    // ============================================================

    public static Locale getCachedLocale(String language, String country) {
        String key = TextUtils.isEmpty(country) ? language : language + "_" + country;
        Locale cached = localeCache.get(key);
        if (cached != null) {
            return cached;
        }
        Locale built = buildLocale(language, country);
        localeCache.put(key, built);
        return built;
    }

    public static Locale localeFromAppLanguage(String lang) {
        if (TextUtils.isEmpty(lang)) {
            return Locale.ENGLISH;
        }
        String[] parts = lang.split("_");
        if (parts.length == 1) {
            return buildLocale(parts[0], "");
        } else if (parts.length >= 2) {
            return buildLocale(parts[0], parts[1]);
        } else {
            return Locale.ENGLISH;
        }
    }

    public static Locale localeFromIndex(int index) {
        int normalized = normalizeLanguageIndex(index);
        Locale cached = indexLocaleCache[normalized];
        if (cached != null) {
            return cached;
        }
        Locale built = localeFromAppLanguage(APP_LANGUAGES[normalized]);
        indexLocaleCache[normalized] = built;
        return built;
    }

    public static String newLanguage(String language, String country) {
        return TextUtils.isEmpty(country) ? language : language + "_" + country;
    }

    public static int resultIndex(String[] languages, String value) {
        for (int i = 0; i < languages.length; i++) {
            if (languages[i] != null && languages[i].equals(value)) {
                AndroidLog.i("Language", "Match found: " + languages[i] + " at index " + i);
                return i;
            }
        }
        AndroidLog.e("Language", "No match found for: " + value);
        return 0;
    }

    public static boolean isUpperCase(String string) {
        return string.equals(string.toUpperCase());
    }

    // ============================================================
    //  Internals
    // ============================================================

    private static Locale buildLocale(String language, String country) {
        if (TextUtils.isEmpty(language)) {
            return Locale.ENGLISH;
        }
        try {
            Locale.Builder builder = new Locale.Builder().setLanguage(language);
            if (!TextUtils.isEmpty(country)) {
                builder.setRegion(country);
            }
            // 中文需要显式 script 才能在 LocaleList 中精确匹配 values-zh-rTW / values-zh-rCN，
            // 否则系统按主语言回退会出现“切换繁体后部分文案仍是简体”的混排。
            if ("zh".equalsIgnoreCase(language)) {
                String upperCountry = country == null ? "" : country.toUpperCase(Locale.ROOT);
                if ("TW".equals(upperCountry) || "HK".equals(upperCountry) || "MO".equals(upperCountry)) {
                    builder.setScript("Hant");
                } else {
                    builder.setScript("Hans");
                }
            }
            return builder.build();
        } catch (Throwable t) {
            AndroidLog.w(TAG, "buildLocale fallback for " + language + "_" + country + ": " + t.getMessage());
            return TextUtils.isEmpty(country) ? new Locale(language) : new Locale(language, country);
        }
    }

    private static int findBestLanguageIndex(Locale locale) {
        if (locale == null) {
            return 0;
        }

        // Java 历史遗留：Locale("id").getLanguage() 返回 "in"，按原始 + 别名两个值都匹配一遍，
        // 避免 id_ID/in_ID 之间互相找不回索引。
        String javaLang = locale.getLanguage();
        String country = locale.getCountry();
        String script = locale.getScript();
        String javaForm = newLanguage(javaLang, country);
        int idx = indexOfLanguageValueExact(javaForm);
        if (idx >= 0) {
            return idx;
        }
        String aliasLang = aliasLanguageCode(javaLang);
        if (!TextUtils.equals(aliasLang, javaLang)) {
            String aliasForm = newLanguage(aliasLang, country);
            idx = indexOfLanguageValueExact(aliasForm);
            if (idx >= 0) {
                return idx;
            }
        }

        // 中文优先按 script 匹配 Hans / Hant，避免 zh-Hant-MO 之类被错配到 zh-CN。
        if ("zh".equalsIgnoreCase(javaLang) || "zh".equalsIgnoreCase(aliasLang)) {
            boolean preferHant = "Hant".equalsIgnoreCase(script)
                || "TW".equalsIgnoreCase(country)
                || "HK".equalsIgnoreCase(country)
                || "MO".equalsIgnoreCase(country);
            for (int i = 0; i < APP_LANGUAGES.length; i++) {
                Locale candidate = localeFromIndex(i);
                if (!"zh".equalsIgnoreCase(candidate.getLanguage())) {
                    continue;
                }
                if (preferHant && "Hant".equalsIgnoreCase(candidate.getScript())) {
                    return i;
                }
                if (!preferHant && "Hans".equalsIgnoreCase(candidate.getScript())) {
                    return i;
                }
            }
        }

        // 仅按主语言匹配，确保任意系统 Locale 都能落到一个有效项。
        for (int i = 0; i < APP_LANGUAGES.length; i++) {
            Locale candidate = localeFromIndex(i);
            if (TextUtils.equals(candidate.getLanguage(), javaLang)
                || TextUtils.equals(candidate.getLanguage(), aliasLang)) {
                return i;
            }
        }
        return 0;
    }

    private static int indexOfLanguageValueExact(String value) {
        if (TextUtils.isEmpty(value)) {
            return -1;
        }
        for (int i = 0; i < APP_LANGUAGES.length; i++) {
            if (TextUtils.equals(APP_LANGUAGES[i], value)) {
                return i;
            }
        }
        return -1;
    }

    private static String aliasLanguageCode(String javaCode) {
        if (TextUtils.isEmpty(javaCode)) {
            return javaCode;
        }
        // Locale 内部把 id/he/yi 转成 in/iw/ji，反向归一化以便匹配资源约定。
        return switch (javaCode) {
            case "in" -> "id";
            case "iw" -> "he";
            case "ji" -> "yi";
            default -> javaCode;
        };
    }

    private static int normalizeLanguageIndex(int index) {
        return index >= 0 && index < APP_LANGUAGES.length ? index : 0;
    }

    /**
     * 把目标 Locale 设置为 App 当前生效的语言。
     *
     * @param locale 为 null 表示清除 per-app locale，回到“跟随系统”。
     */
    private static void applySystemLocales(Context context, Locale locale) {
        LocaleManager manager = getLocaleManager(context);
        if (manager == null) {
            return;
        }
        LocaleList target = locale == null
            ? LocaleList.getEmptyLocaleList()
            : new LocaleList(locale);
        try {
            manager.setApplicationLocales(target);
        } catch (Throwable t) {
            AndroidLog.w(TAG, "setApplicationLocales failed: " + t.getMessage());
        }
    }

    /**
     * 启动时把系统已生效的 per-app locale 同步回 {@link AppSettingsStore}，
     * 让 Xposed 模块端通过 PrefsBridge 始终读到最新索引。
     */
    private static void syncStoredIndexFromSystem(Context context) {
        LocaleList applied = getApplicationLocalesSafe(context);
        if (applied == null || applied.isEmpty()) {
            return;
        }
        int idx = findBestLanguageIndex(applied.get(0));
        int stored = AppSettingsStore.getAppLanguageIndex(context, -1);
        if (stored != idx) {
            AppSettingsStore.setAppLanguageIndex(context, idx);
        }
    }

    private static LocaleList getApplicationLocalesSafe(Context context) {
        LocaleManager manager = getLocaleManager(context);
        if (manager == null) {
            return null;
        }
        try {
            return manager.getApplicationLocales();
        } catch (Throwable t) {
            AndroidLog.w(TAG, "getApplicationLocales failed: " + t.getMessage());
            return null;
        }
    }

    private static LocaleManager getLocaleManager(Context context) {
        if (context == null) {
            return null;
        }
        return context.getSystemService(LocaleManager.class);
    }
}
