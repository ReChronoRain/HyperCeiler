package com.sevtinge.hyperceiler.search;

import static com.sevtinge.hyperceiler.common.utils.LanguageHelper.APP_LANGUAGES;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.sevtinge.hyperceiler.common.model.data.ModData;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hooker.PersonalAssistantFragment;
import com.sevtinge.hyperceiler.hooker.VariousFragment;
import com.sevtinge.hyperceiler.hooker.framework.CorePatchSettings;
import com.sevtinge.hyperceiler.hooker.framework.DisplaySettings;
import com.sevtinge.hyperceiler.hooker.framework.FreeFormSettings;
import com.sevtinge.hyperceiler.hooker.framework.MiPadSettings;
import com.sevtinge.hyperceiler.hooker.framework.VolumeSettings;
import com.sevtinge.hyperceiler.hooker.securitycenter.OtherSettings;
import com.sevtinge.hyperceiler.hooker.systemui.LockScreenSettings;
import com.sevtinge.hyperceiler.hooker.systemui.StatusBarSettings;
import com.sevtinge.hyperceiler.hooker.various.AOSPSettings;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.search.data.AppDatabase;
import com.sevtinge.hyperceiler.search.data.ModDao;
import com.sevtinge.hyperceiler.search.data.ModEntity;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchHelper {

    private static final String TAG = "SearchHelper";

    public static final int MARK_COLOR_VIBRANT = Color.parseColor("#277af7");
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String APP_NS = "http://schemas.android.com/apk/res-auto";

    // 定义配置注册表
    private static final List<ModConfig> REGISTRY = new ArrayList<>();

    static {
        // 系统框架
        REGISTRY.add(new ModConfig(FreeFormSettings.class, R.xml.framework_freeform, R.string.system_framework));
        REGISTRY.add(new ModConfig(VolumeSettings.class, R.xml.framework_volume, R.string.system_framework));
        REGISTRY.add(new ModConfig(MiPadSettings.class, R.xml.various_mipad, R.string.system_framework));
        REGISTRY.add(new ModConfig(DisplaySettings.class, R.xml.framework_display, R.string.system_framework));
        REGISTRY.add(new ModConfig(OtherSettings.class, R.xml.framework_other, R.string.system_framework));
        REGISTRY.add(new ModConfig(CorePatchSettings.class, R.xml.framework_core_patch, R.string.system_framework));

        // 系统界面
        REGISTRY.add(new ModConfig(LockScreenSettings.class, R.xml.system_ui_lock_screen, R.string.system_ui));
        REGISTRY.add(new ModConfig(StatusBarSettings.class, R.xml.system_ui_status_bar, R.string.system_ui));

        // 杂项 (Dashboard 模式)
        int[] dashXmls = {R.xml.analytics, R.xml.browser, R.xml.fileexplorer, R.xml.mms};
        for (int xml : dashXmls) {
            REGISTRY.add(new ModConfig(DashboardFragment.class, xml, R.string.various));
        }


        // 杂项
        REGISTRY.add(new ModConfig(AOSPSettings.class, R.xml.various_aosp, R.string.various));
        REGISTRY.add(new ModConfig(VariousFragment.class, R.xml.various, R.string.various));
        if (isPad()) {
            REGISTRY.add(new ModConfig(VariousFragment.class, R.xml.various, R.string.various));
        }

        // 实验性
        REGISTRY.add(new ModConfig(DashboardFragment.class, R.xml.theme_manager));
        REGISTRY.add(new ModConfig(PersonalAssistantFragment.class, R.xml.personal_assistant));
    }

    /**
     * 初始化索引（建议在 Application 或 HomePageFragment 中调用）
     */
    public static void initIndex(Context context, boolean force) {
        Log.d(TAG, "initIndex called, force = " + force); // <-- 加这行
        ThreadPoolManager.getInstance().submit(() -> {
            ModDao dao = AppDatabase.getInstance(context).modDao();
            if (force || dao.getCount() == 0) {
                rebuildIndex(context, dao);
            }
        });
    }

    /**
     * 核心解析逻辑：XML -> Room
     */
    private static void rebuildIndex(Context context, ModDao dao) {
        Resources res = getLocaleResources(context);
        List<ModEntity> entities = new ArrayList<>();
        for (ModConfig config : REGISTRY) {
            parsePrefXml(res, entities, config);
        }

        // 写入数据库
        AppDatabase.getInstance(context).runInTransaction(() -> {
            dao.deleteAll();
            dao.insertAll(entities);
            Log.d(TAG, "Database transaction finished."); // 确认这行输出了
        });
    }

    // 3. 搜索接口 (支持拼音匹配)
    /**
     * 执行全文搜索
     * @param context 上下文
     * @param keyword 搜索关键词
     * @return 匹配到的实体列表
     */
    public static List<ModEntity> search(Context context, String keyword) {
        if (TextUtils.isEmpty(keyword)) return new ArrayList<>();

        // 处理 SQLite FTS 的查询特殊字符，并支持前缀匹配（输入 "wifi" 匹配 "wifi设置"）
        // MATCH 语法要求：keyword*
        String query = keyword.trim().replace("'", "''") + "*";

        // 调用 Room DAO 执行搜索
        return AppDatabase.getInstance(context).modDao().search(query);
    }


    // --- 工具方法 ---

    /**
     * 根据用户设置的语言获取对应的 Resources 对象
     */
    private static Resources getLocaleResources(Context context) {
        int selectedLang = Integer.parseInt(PrefsUtils.getSharedStringPrefs(context, "prefs_key_settings_app_language", "0"));
        if (selectedLang < 0 || selectedLang >= APP_LANGUAGES.length) selectedLang = 0;
        //Locale locale = localeFromAppLanguage(appLanguages[selectedLang]);

        Locale locale = Locale.CHINA;

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config).getResources();
    }

    private static int getResIdFromAttr(String attr) {
        if (attr == null || !attr.startsWith("@")) return -1;
        try { return Integer.parseInt(attr.substring(1)); } catch (Exception e) { return -1; }
    }

    private static String getTitleFromAttr(Resources res, String attr) {
        int id = getResIdFromAttr(attr);
        return id > 0 ? res.getString(id) : null;
    }


    private static void parsePrefXml(Resources res, List<ModEntity> entities, ModConfig config) {

        int xmlResId = config.xmlResId;
        int[] internalId = config.internalIds;
        String catPrefsFragment = config.fragmentClass.getName();

        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int order = 0;
            String location = null, locationPad = null;
            int locationId = 0, locationPadId = 0;
            boolean isPadDevice = isPad();
            StringBuilder internalName = null;
            int eventType = xml.getEventType();

            if (internalId.length != 0) {
                internalName = new StringBuilder();
                for (int id : internalId) {
                    if (internalName.length() > 0) {
                        internalName.append("/");
                    }

                    internalName.append(res.getString(id));
                }
            }

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && !"PreferenceCategory".equals(xml.getName())) {
                    try {
                        String titleAttr = xml.getAttributeValue(ANDROID_NS, "title");
                        String keyAttr = xml.getAttributeValue(ANDROID_NS, "key");
                        String isPrefVisibleAttr = xml.getAttributeValue(APP_NS, "isPreferenceVisible");
                        String myLocationPadAttr = xml.getAttributeValue(APP_NS, "myLocationPad");
                        String myLocationAttr = xml.getAttributeValue(APP_NS, "myLocation");

                        String modTitle = getTitleFromAttr(res, titleAttr);
                        boolean isPreferenceVisible = Boolean.parseBoolean(isPrefVisibleAttr);

                        if (locationPad == null && myLocationPadAttr != null) {
                            locationPad = getTitleFromAttr(res, myLocationPadAttr);
                            locationPadId = getResIdFromAttr(myLocationPadAttr);
                        }
                        if (location == null && myLocationAttr != null) {
                            location = getTitleFromAttr(res, myLocationAttr);
                            locationId = getResIdFromAttr(myLocationAttr);
                        }

                        if (!TextUtils.isEmpty(modTitle) && !isPreferenceVisible) {
                            String internalPad = internalName == null ? locationPad : internalName + "/" + locationPad;
                            String internal = internalName == null ? location : internalName + "/" + location;

                            ModEntity modData = new ModEntity();
                            modData.title = modTitle;
                            if (location != null && (!isPadDevice || locationPad == null)) {
                                modData.breadcrumbs = internal;
                                modData.catTitleResId = locationId;
                            } else if (locationPad != null) {
                                modData.breadcrumbs = internalPad;
                                modData.catTitleResId = locationPadId;
                            }
                            modData.xmlResId = xmlResId;
                            modData.key = keyAttr;
                            modData.order = order;
                            modData.fragment = catPrefsFragment;
                            entities.add(modData);
                        }
                        order++;
                    } catch (Throwable t) {
                        AndroidLog.e(TAG, "Failed to get xml keyword object!", t);
                    }
                }
                eventType = xml.next();
            }


        } catch (Throwable t) {
            AndroidLog.e(TAG, "Failed to access XML resource!", t);
        }
    }


    private static class ModConfig {
        Class<?> fragmentClass;
        int xmlResId;
        int[] internalIds; // 对应原版的参数，用于构建面包屑路径

        ModConfig(Class<?> cls, int xml, int... ids) {
            this.fragmentClass = cls;
            this.xmlResId = xml;
            this.internalIds = ids;
        }
    }

}
