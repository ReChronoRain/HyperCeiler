package com.sevtinge.hyperceiler.search;

import static com.sevtinge.hyperceiler.common.utils.LanguageHelper.APP_LANGUAGES;
import static com.sevtinge.hyperceiler.common.utils.LanguageHelper.localeFromAppLanguage;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;
import com.sevtinge.hyperceiler.search.data.AppDatabase;
import com.sevtinge.hyperceiler.search.data.ModDao;
import com.sevtinge.hyperceiler.search.data.ModEntity;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SearchHelper {

    private static final String TAG = "SearchHelper";

    public static final int MARK_COLOR_VIBRANT = Color.parseColor("#277af7");
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String APP_NS = "http://schemas.android.com/apk/res-auto";

    /**
     * 标签跳过列表：这些标签不作为可索引的搜索条目
     */
    private static final Set<String> SKIP_TAGS = Set.of(
        "PreferenceScreen",
        "PreferenceCategory",
        "com.sevtinge.hyperceiler.common.prefs.LayoutPreference"
    );

    /**
     * 已注册的 XML 资源 ID 集合，防止重复扫描
     */
    private static final Set<Integer> REGISTERED = Collections.synchronizedSet(new HashSet<>());

    /**
     * 分组名称 → 包名映射，用于搜索结果显示应用图标
     */
    private static final Map<String, String> GROUP_PACKAGE_MAP = new LinkedHashMap<>();

    /**
     * 分组名称 → drawable 资源 ID 映射，用于应用未安装时显示 prefs_main 中定义的图标
     */
    private static final Map<String, Integer> GROUP_ICON_MAP = new LinkedHashMap<>();

    public static Map<String, String> getGroupPackageMap() {
        return Collections.unmodifiableMap(GROUP_PACKAGE_MAP);
    }

    public static Map<String, Integer> getGroupIconMap() {
        return Collections.unmodifiableMap(GROUP_ICON_MAP);
    }

    /**
     * 初始化索引（建议在 Application 或 HomePageFragment 中调用）
     * 动态扫描 prefs_main.xml 中所有 PreferenceHeader，递归解析子 XML
     */
    public static void initIndex(Context context, boolean force) {
        Log.d(TAG, "initIndex called, force = " + force);
        ThreadPoolManager.getInstance().submit(() -> {
            ModDao dao = AppDatabase.getInstance(context).modDao();
            if (force || dao.getCount() == 0) {
                rebuildIndex(context, dao);
            }
        });
    }

    /**
     * 核心索引构建：动态扫描 prefs_main.xml -> 递归解析所有子 XML -> 写入 Room
     */
    private static void rebuildIndex(Context context, ModDao dao) {
        REGISTERED.clear();
        Resources res = getLocaleResources(context);
        List<ModEntity> entities = new ArrayList<>();

        int mainXmlResId = context.getResources().getIdentifier("prefs_main", "xml", context.getPackageName());
        if (mainXmlResId == 0) {
            AndroidLog.e(TAG, "prefs_main.xml not found!");
            return;
        }

        scanMainPrefs(res, mainXmlResId, entities);

        // 写入数据库
        AppDatabase.getInstance(context).runInTransaction(() -> {
            dao.deleteAll();
            dao.insertAll(entities);
            Log.d(TAG, "Index built: " + entities.size() + " entries");
        });
    }

    /**
     * 扫描 prefs_main.xml，提取所有 PreferenceHeader 并递归解析子 XML
     */
    private static void scanMainPrefs(Resources res, int mainXmlResId, List<ModEntity> entities) {
        GROUP_PACKAGE_MAP.clear();
        GROUP_ICON_MAP.clear();
        try (XmlResourceParser xml = res.getXml(mainXmlResId)) {
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xml.getName().contains("PreferenceHeader")) {
                    String fragment = xml.getAttributeValue(ANDROID_NS, "fragment");
                    String titleAttr = xml.getAttributeValue(ANDROID_NS, "title");
                    String summaryAttr = xml.getAttributeValue(ANDROID_NS, "summary");
                    String inflatedXmlAttr = xml.getAttributeValue(APP_NS, "inflatedXml");
                    String iconAttr = xml.getAttributeValue(ANDROID_NS, "icon");

                    String groupTitle = resolveStringRef(res, titleAttr);
                    String packageName = resolveStringRef(res, summaryAttr);
                    if (packageName == null) packageName = summaryAttr;
                    int inflatedXml = resolveResId(inflatedXmlAttr);
                    int iconResId = resolveResId(iconAttr);

                    if (groupTitle != null && packageName != null) {
                        GROUP_PACKAGE_MAP.put(groupTitle, packageName);
                    }
                    if (groupTitle != null && iconResId > 0) {
                        GROUP_ICON_MAP.put(groupTitle, iconResId);
                    }

                    if (inflatedXml > 0) {
                        // 有 inflatedXml 属性 -> 用 DashboardFragment + inflatedXml
                        String frag = fragment != null ? fragment : DashboardFragment.class.getName();
                        scanEntryXml(res, frag, inflatedXml, groupTitle, groupTitle, entities);
                    } else if (fragment != null) {
                        // 只有 fragment，通过反射获取 XML 资源 ID
                        int xmlResId = getXmlResIdFromFragment(fragment);
                        if (xmlResId > 0) {
                            scanEntryXml(res, fragment, xmlResId, groupTitle, groupTitle, entities);
                        }
                    }
                }
                eventType = xml.next();
            }
        } catch (Throwable t) {
            AndroidLog.e(TAG, "Failed to scan prefs_main", t);
        }
    }

    /**
     * 扫描单个 XML 偏好文件，提取所有可搜索的条目
     * @param topLevelGroup 顶级分组名（始终不变，用于 groupName 和图标查找）
     * @param parentBreadcrumb 父级面包屑路径（递归时逐层拼接）
     */
    private static void scanEntryXml(Resources res, String fragment, int xmlResId,
                                     String topLevelGroup, String parentBreadcrumb,
                                     List<ModEntity> entities) {
        if (!REGISTERED.add(xmlResId)) return; // 防止重复扫描

        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int eventType = xml.getEventType();
            int order = 0;
            String location = null;
            int locationId = 0;
            List<ModEntity> batch = new ArrayList<>();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xml.getName();

                    if ("PreferenceScreen".equals(tag)) {
                        // 提取 myLocation 属性作为面包屑的子路径
                        String myLoc = xml.getAttributeValue(APP_NS, "myLocation");
                        if (myLoc != null && location == null) {
                            location = resolveStringRef(res, myLoc);
                            locationId = resolveResId(myLoc);
                        }
                    } else if (!SKIP_TAGS.contains(tag)) {
                        String titleAttr = xml.getAttributeValue(ANDROID_NS, "title");
                        String keyAttr = xml.getAttributeValue(ANDROID_NS, "key");
                        String isVisibleAttr = xml.getAttributeValue(APP_NS, "isPreferenceVisible");
                        String childFragment = xml.getAttributeValue(ANDROID_NS, "fragment");

                        String modTitle = resolveStringRef(res, titleAttr);
                        boolean isHidden = "false".equals(isVisibleAttr);

                        // 递归扫描子 Fragment 指向的 XML
                        if (childFragment != null) {
                            int childXml = getXmlResIdFromFragment(childFragment);
                            if (childXml > 0) {
                                String breadcrumb = buildBreadcrumb(parentBreadcrumb, location);
                                scanEntryXml(res, childFragment, childXml, topLevelGroup, breadcrumb, entities);
                            }
                        }

                        // 索引当前 Preference 项
                        if (!TextUtils.isEmpty(modTitle) && !TextUtils.isEmpty(keyAttr) && !isHidden) {
                            ModEntity mod = new ModEntity();
                            mod.title = modTitle;
                            mod.key = keyAttr;
                            mod.xmlResId = xmlResId;
                            mod.order = order;
                            mod.fragment = fragment;
                            mod.breadcrumbs = buildBreadcrumb(parentBreadcrumb, location);
                            mod.catTitleResId = locationId;
                            mod.groupName = topLevelGroup != null ? topLevelGroup : "";
                            batch.add(mod);
                        }
                        order++;
                    }
                }
                eventType = xml.next();
            }

            if (!batch.isEmpty()) entities.addAll(batch);
        } catch (Throwable t) {
            AndroidLog.e(TAG, "Failed to parse XML: " + xmlResId, t);
        }
    }

    /**
     * 执行全文搜索
     */
    public static List<ModEntity> search(Context context, String keyword) {
        if (TextUtils.isEmpty(keyword)) return new ArrayList<>();
        String query = keyword.trim().replace("'", "''") + "*";
        return AppDatabase.getInstance(context).modDao().search(query);
    }

    /**
     * 清理索引缓存
     */
    public static void clearIndex() {
        REGISTERED.clear();
        GROUP_PACKAGE_MAP.clear();
    }

    // --- 工具方法 ---

    /**
     * 根据用户设置的语言获取对应的 Resources 对象
     */
    private static Resources getLocaleResources(Context context) {
        int selectedLang = PrefsBridge.getStringAsInt("prefs_key_settings_app_language", 0);
        if (selectedLang < 0 || selectedLang >= APP_LANGUAGES.length) selectedLang = 0;
        Locale locale = localeFromAppLanguage(APP_LANGUAGES[selectedLang]);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config).getResources();
    }

    /**
     * 通过反射获取 Fragment 的 getPreferenceScreenResId()
     */
    private static int getXmlResIdFromFragment(String fragmentName) {
        try {
            Class<?> clazz = Class.forName(fragmentName);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Method method = findMethod(clazz, "getPreferenceScreenResId");
            if (method != null) {
                method.setAccessible(true);
                Object result = method.invoke(instance);
                if (result instanceof Integer) return (int) result;
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private static Method findMethod(Class<?> clazz, String name) {
        while (clazz != null) {
            try { return clazz.getDeclaredMethod(name); }
            catch (NoSuchMethodException e) { clazz = clazz.getSuperclass(); }
        }
        return null;
    }

    private static String buildBreadcrumb(String parent, String location) {
        if (parent == null && location == null) return "";
        if (parent == null) return location;
        if (location == null) return parent;
        if (parent.equals(location)) return parent;
        return parent + "/" + location;
    }

    private static String resolveStringRef(Resources res, String attr) {
        if (attr == null) return null;
        if (attr.startsWith("@")) {
            try {
                int id = Integer.parseInt(attr.substring(1));
                if (id > 0) return res.getString(id);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static int resolveResId(String attr) {
        if (attr == null || !attr.startsWith("@")) return 0;
        try {
            int id = Integer.parseInt(attr.substring(1));
            return Math.max(id, 0);
        } catch (NumberFormatException e) { return 0; }
    }
}
