package com.sevtinge.hyperceiler.search;

import static com.sevtinge.hyperceiler.utils.LanguageHelper.APP_LANGUAGES;
import static com.sevtinge.hyperceiler.utils.LanguageHelper.localeFromAppLanguage;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.text.TextUtils;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.home.HomePageFragment;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import com.sevtinge.hyperceiler.prefs.LayoutPreference;
import com.sevtinge.hyperceiler.prefs.PreferenceHeader;
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

    private static final Set<Integer> REGISTERED = Collections.synchronizedSet(new HashSet<>());

    private static final Map<String, String> GROUP_PACKAGE_MAP = new LinkedHashMap<>();

    private static final Map<String, Integer> GROUP_ICON_MAP = new LinkedHashMap<>();

    public static Map<String, String> getGroupPackageMap() {
        return Collections.unmodifiableMap(GROUP_PACKAGE_MAP);
    }

    public static Map<String, Integer> getGroupIconMap() {
        return Collections.unmodifiableMap(GROUP_ICON_MAP);
    }

    public static void initIndex(Context context, boolean force) {
        AndroidLog.d(TAG, "initIndex: force = " + force);
        ThreadPoolManager.getInstance().submit(() -> {
            ModDao dao = AppDatabase.getInstance(context).modDao();
            if (force || dao.getCount() == 0) {
                rebuildIndex(context, dao);
            }
        });
    }

    private static void rebuildIndex(Context context, ModDao dao) {
        REGISTERED.clear();
        Resources res = getLocaleResources(context);
        List<ModEntity> entities = new ArrayList<>();

        int headersXmlResId = HomePageFragment.getHomeHeadersResourceId();
        if (headersXmlResId == 0) {
            AndroidLog.e(TAG, "Home headers xml not found!");
            return;
        }

        scanHeaders(res, headersXmlResId, entities);

        AppDatabase.getInstance(context).runInTransaction(() -> {
            dao.deleteAll();
            dao.insertAll(entities);
            AndroidLog.d(TAG, "rebuildIndex: inserted " + entities.size() + " entries");
        });
    }

    private static void scanHeaders(Resources res, int headersXmlResId, List<ModEntity> entities) {
        GROUP_PACKAGE_MAP.clear();
        GROUP_ICON_MAP.clear();
        try (XmlResourceParser xml = res.getXml(headersXmlResId)) {
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && isPreferenceHeaderTag(xml.getName())) {
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
                        String frag = fragment != null ? fragment : DashboardFragment.class.getName();
                        scanEntryXml(res, frag, inflatedXml, groupTitle, groupTitle, entities);
                    } else if (fragment != null) {
                        int xmlResId = getXmlResIdFromFragment(fragment);
                        if (xmlResId > 0) {
                            scanEntryXml(res, fragment, xmlResId, groupTitle, groupTitle, entities);
                        }
                    }
                }
                eventType = xml.next();
            }
        } catch (Throwable t) {
            AndroidLog.e(TAG, "Failed to scan home headers", t);
        }
    }

    private static void scanEntryXml(Resources res, String fragment, int xmlResId,
                                     String topLevelGroup, String parentBreadcrumb,
                                     List<ModEntity> entities) {
        if (!REGISTERED.add(xmlResId)) return;

        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int eventType = xml.getEventType();
            int order = 0;
            String location = null;
            int locationId = 0;
            List<ModEntity> batch = new ArrayList<>();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xml.getName();

                    if (isPreferenceScreenTag(tag)) {
                        String myLoc = xml.getAttributeValue(APP_NS, "myLocation");
                        if (myLoc != null && location == null) {
                            location = resolveStringRef(res, myLoc);
                            locationId = resolveResId(myLoc);
                        }
                    } else if (!shouldSkipTag(tag)) {
                        String titleAttr = xml.getAttributeValue(ANDROID_NS, "title");
                        String keyAttr = xml.getAttributeValue(ANDROID_NS, "key");
                        String isVisibleAttr = xml.getAttributeValue(APP_NS, "isPreferenceVisible");
                        String childFragment = xml.getAttributeValue(ANDROID_NS, "fragment");

                        String modTitle = resolveStringRef(res, titleAttr);
                        boolean isHidden = "false".equals(isVisibleAttr);

                        if (childFragment != null) {
                            int childXml = getXmlResIdFromFragment(childFragment);
                            if (childXml > 0) {
                                String breadcrumb = buildBreadcrumb(parentBreadcrumb, location);
                                scanEntryXml(res, childFragment, childXml, topLevelGroup, breadcrumb, entities);
                            }
                        }

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

    public static List<ModEntity> search(Context context, String keyword) {
        if (TextUtils.isEmpty(keyword)) return new ArrayList<>();
        String trimmed = keyword.trim();
        ModDao dao = AppDatabase.getInstance(context).modDao();

        // 中文（简体/繁体）按字模糊匹配，其它语言按词汇匹配
        if (containsChinese(trimmed)) {
            return dao.testSearch(trimmed);
        } else {
            String query = trimmed.replace("'", "''") + "*";
            return dao.search(query);
        }
    }

    private static boolean containsChinese(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF  // CJK Unified Ideographs
                || c >= 0x3400 && c <= 0x4DBF  // CJK Extension A
                || c >= 0xF900 && c <= 0xFAFF) { // CJK Compatibility Ideographs
                return true;
            }
        }
        return false;
    }

    public static void clearIndex() {
        REGISTERED.clear();
        GROUP_PACKAGE_MAP.clear();
        GROUP_ICON_MAP.clear();
    }
    private static Resources getLocaleResources(Context context) {
        int selectedLang = PrefsBridge.getStringAsInt("prefs_key_settings_app_language", 0);
        if (selectedLang < 0 || selectedLang >= APP_LANGUAGES.length) selectedLang = 0;
        Locale locale = localeFromAppLanguage(APP_LANGUAGES[selectedLang]);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config).getResources();
    }

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

    private static boolean shouldSkipTag(String tag) {
        return isPreferenceCategoryTag(tag)
            || isLayoutPreferenceTag(tag)
            || isContainerTag(tag);
    }

    private static boolean isPreferenceHeaderTag(String tag) {
        return "header".equals(tag) || isTagOrSubclass(tag, PreferenceHeader.class);
    }

    private static boolean isPreferenceScreenTag(String tag) {
        return isTagOrSubclass(tag, PreferenceScreen.class);
    }

    private static boolean isPreferenceCategoryTag(String tag) {
        return isTagOrSubclass(tag, PreferenceCategory.class);
    }

    private static boolean isLayoutPreferenceTag(String tag) {
        return isTagOrSubclass(tag, LayoutPreference.class);
    }

    private static boolean isContainerTag(String tag) {
        Class<?> tagClass = loadTagClass(tag);
        return tagClass != null && PreferenceGroup.class.isAssignableFrom(tagClass);
    }

    private static boolean isTagOrSubclass(String tag, Class<?> targetClass) {
        if (TextUtils.isEmpty(tag)) {
            return false;
        }
        if (targetClass.getSimpleName().equals(tag) || targetClass.getName().equals(tag)) {
            return true;
        }
        Class<?> tagClass = loadTagClass(tag);
        return tagClass != null && targetClass.isAssignableFrom(tagClass);
    }

    private static Class<?> loadTagClass(String tag) {
        if (TextUtils.isEmpty(tag) || !tag.contains(".")) {
            return null;
        }
        try {
            return Class.forName(tag);
        } catch (Throwable ignored) {
            return null;
        }
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
