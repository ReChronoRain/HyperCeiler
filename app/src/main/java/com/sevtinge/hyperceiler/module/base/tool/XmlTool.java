/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.base.tool;

import android.content.Context;
import android.content.res.Resources;

import com.sevtinge.hyperceiler.utils.ContextUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedHelpers;

/**
 * Xml 钩子
 *
 * @author 焕晨HChen
 */
public class XmlTool {
    private static final String TAG = "XmlTool";
    private IXposedHookZygoteInit.StartupParam startupParam = null;
    private boolean hooksApplied = false;
    private int index = -1;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> typeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> valueMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> titleMap = new ConcurrentHashMap<>();

    /**
     * 建议直接调用这些
     */
    public static final String TAG_BOOL = "bool";
    public static final String TAG_FLOAT = "float";
    public static final String TAG_INTEGER = "integer";
    public static final String TAG_INTEGER_ARRAY = "integer-array";
    public static final String TAG_ITEM = "item";
    public static final String TAG_STRING = "string";
    public static final String TAG_STRING_ARRAY = "string-array";

    public XmlTool(IXposedHookZygoteInit.StartupParam startupParam) {
        this.startupParam = startupParam;
        applyHook();
    }

    private void applyHook() {
        if (hooksApplied) return;
        hooksApplied = true;
        Class<?> KXmlParser = XposedHelpers.findClassIfExists("com.android.org.kxml2.io.KXmlParser",
                startupParam.getClass().getClassLoader());
        Class<?> XmlBlock = XposedHelpers.findClassIfExists("android.content.res.XmlBlock$Parser",
                startupParam.getClass().getClassLoader());
        if (KXmlParser != null) {
            Method[] xmlMethods = KXmlParser.getDeclaredMethods();
            for (Method method : xmlMethods) {
                String name = method.getName();
                switch (name) {
                    case "nextText" -> xmlHookMethod(method, hookNextText);
                    case "getAttributeValue" -> {
                        if (method.getParameterCount() == 1 &&
                                method.getParameterTypes()[0].equals(int.class)) {
                            xmlHookMethod(method, hookGetAttributeValueInt);
                        } else if (method.getParameterCount() == 2
                                && method.getParameterTypes()[0].equals(String.class)
                                && method.getParameterTypes()[1].equals(String.class)) {
                            xmlHookMethod(method, hookGetAttributeValueString);
                        }
                    }
                }
            }
        }
        if (XmlBlock != null) {
            xmlFindHookMethod(XmlBlock, "getAttributeValue",
                    String.class, String.class, hookGetAttributeValueBlock);
        }
    }

    private void xmlHookMethod(Method method, HookTool.MethodHook methodHook) {
        HookTool.hookMethod(method, methodHook);
    }

    private void xmlFindHookMethod(Class<?> clzz, String name, Object... args) {
        HookTool.findAndHookMethod(clzz, name, args);
    }

    private final HookTool.MethodHook hookGetAttributeValueInt = new HookTool.MethodHook() {
        @Override
        protected void before(MethodHookParam param) {
            index = (int) param.args[0];
        }
    };

    /**
     * 不完善
     */
    private final HookTool.MethodHook hookGetAttributeValueString = new HookTool.MethodHook() {
        @Override
        protected void after(MethodHookParam param) {
            // XposedLogUtils.logE(TAG, "namespace: " + param.args[0] + " name: " + param.args[1] + " result: " + param.getResult());
        }
    };

    /**
     * 读取 res.getXml() 会使用
     */
    private final HookTool.MethodHook hookGetAttributeValueBlock = new HookTool.MethodHook() {
        @Override
        protected void after(MethodHookParam param) {
            String name = (String) param.args[1];
            // 先限制一下
            if (!"title".equals(name) && !"summary".equals(name)) return;
            Context context;
            context = OtherTool.findContext(ContextUtils.FLAG_ALL);
            if (context == null) return;
            String result = (String) param.getResult();
            if (result == null) return;
            int id = getModId(result);
            if (id == -1) return;
            Resources res = context.getResources();
            // String test = getModTitle(res, result);
            String pkgName = null;
            String resType = null;
            String resName = null;
            try {
                pkgName = res.getResourcePackageName(id);
                resType = res.getResourceTypeName(id);
                resName = res.getResourceEntryName(id);
            } catch (Throwable ignore) {
            }
            if (pkgName == null || resType == null || resName == null) return;
            Integer mID = titleMap.get(pkgName + ":" + resType + "/" + resName);
            // String mid = null;
            if (mID != null) {
                param.setResult("@" + mID);
                // mid = getModTitle(res, "@" + mID);
            }
            // XposedLogUtils.logE(TAG, "pk: " + pkgName + " ty: " + resType + " na: " + resName + " sss: " + test + " m: " + mid);
            // XposedLogUtils.logE(TAG, "namespace: " + param.args[0] + " name: " + param.args[1] + " result: " + result);
        }
    };

    private final HookTool.MethodHook hookNextText = new HookTool.MethodHook() {
        @Override
        protected void after(MethodHookParam param) {
            if (index == -1) return;
            String[] attributes = (String[]) XposedHelpers.getObjectField(param.thisObject, "attributes");
            String type = (String) XposedHelpers.callMethod(param.thisObject, "getName");
            if (type == null) return;
            String key = attributes[(index * 4) + 3];
            // String result = (String) param.getResult();
            ConcurrentHashMap<String, String> get = typeMap.get(type);
            if (get == null) return;
            String value = get.get(key);
            if (value == null) return;
            param.setResult(value);
            // XposedLogUtils.logE(TAG, "type: " + type + " key: " + key + " result: " + result + " value: " + value);
        }
    };

    private int getModId(String title) {
        if (title == null) {
            return -1;
        }
        int titleResId = -1;
        try {
            titleResId = Integer.parseInt(title.substring(1));
        } catch (Throwable e) {
            return titleResId;
        }
        if (titleResId <= 0) {
            return -1;
        }
        return titleResId;
    }

    private String getModTitle(Resources res, String title) {
        int id = getModId(title);
        if (id == -1) return null;
        return res.getString(id);
    }

    /**
     * 设置 Xml 值替换
     * type 是指需要替换的参数类型
     * 可能是:
     * <p>
     * bool, <br/>
     * integer,<br/>
     * string,<br/>
     * item,<br/>
     * float,<br/>
     * string-array, // 此类可能需要设置为 item 才能替换<br/>
     * integer-array, // 此类可能需要设置为 item 才能替换<br/>
     * ...<br/>
     * </p>
     *
     * @param type  类型
     * @param key   key
     * @param value 替换值
     */
    public void setValueReplacement(String type, String key, Object value) {
        applyHook();
        valueMap.put(key, String.valueOf(value));
        typeMap.put(type, valueMap);
    }

    /**
     * 支持修改 Xml 中 <br/>
     * android:summary<br/>
     * 和<br/>
     * android:title<br/>
     * 的返回值
     *
     * @param pkg   包名
     * @param type  类型
     * @param name  资源名
     * @param value 返回值
     */
    public void setXmlTitleReplacement(String pkg, String type, String name, int value) {
        applyHook();
        titleMap.put(pkg + ":" + type + "/" + name, value);
    }
}
