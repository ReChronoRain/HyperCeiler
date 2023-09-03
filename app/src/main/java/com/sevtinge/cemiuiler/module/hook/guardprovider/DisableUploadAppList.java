package com.sevtinge.cemiuiler.module.hook.guardprovider;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class DisableUploadAppList extends BaseHook {

    @Override
    public void init(/*final XC_LoadPackage.LoadPackageParam lpparam*/) {
        if (lpparam.packageName.equals("com.miui.guardprovider")) {
            log("Start to hook package " + lpparam.packageName);

            // Debug mode flag process
            final Class<?> guardApplication = XposedHelpers.findClass("com.miui.guardprovider.GuardApplication", lpparam.classLoader);
            if (guardApplication != null) {
                Field[] guardApplicationFields = guardApplication.getDeclaredFields();
                for (Field field : guardApplicationFields) {
                    if (field.getName().equals("c")) {
                        XposedHelpers.setStaticBooleanField(guardApplication, "c", true);
                        log("Info: GuardProvider will work as debug mode!");
                    }
                    log("Warning: GuardProvider debug mode flag not found!");
                }
            } else {
                log("Warning: GuardApplication class not found. GuardProvider will not work as debug mode! ");
            }

            // Prevent miui from uploading app list
            final Class<?> antiDefraudAppManager = XposedHelpers.findClassIfExists("com.miui.guardprovider.engine.mi.antidefraud.AntiDefraudAppManager", lpparam.classLoader);
            if (antiDefraudAppManager == null) {
                log("Skip: AntiDefraudAppManager class not found.");
                return;
            } else {
                log("Info: AntiDefraudAppManager class found.");
            }

            final Method[] methods = antiDefraudAppManager.getDeclaredMethods();
            Method getAllUnSystemAppsStatus = null;
            for (Method method : methods) {
                if (method.getName().equals("getAllUnSystemAppsStatus") && method.getParameterTypes().length == 1) {
                    getAllUnSystemAppsStatus = method;
                    break;
                }
            }
            if (getAllUnSystemAppsStatus == null) {
                log("Skip: getAllUnSystemAppsStatus method not found.");
                return;
            } else {
                log("Info: getAllUnSystemAppsStatus method found.");
            }

            XposedBridge.hookMethod(getAllUnSystemAppsStatus, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    List list = (List) methodHookParam.args[0];

                    String MIUI_VERSION = null;
                    Field[] antiDefraudAppManagerFields = antiDefraudAppManager.getDeclaredFields();
                    for (Field field : antiDefraudAppManagerFields) {
                        if (field.getName().equals("MIUI_VERSION")) {
                            MIUI_VERSION = (String) XposedHelpers.getStaticObjectField(antiDefraudAppManager, "MIUI_VERSION");
                        }
                    }
                    if (MIUI_VERSION == null) {
                        log("Warning: Can't get MIUI_VERSION.");
                    }

                    String uuid = null;
                    final Class<?> uuidHelper = XposedHelpers.findClassIfExists("i.b", lpparam.classLoader);
                    if (uuidHelper != null) {
                        final Method[] uuidHelperMethods = uuidHelper.getDeclaredMethods();
                        Method getUUID = null;
                        for (Method method : uuidHelperMethods) {
                            if (method.getName().equals("b") && method.getParameterTypes().length == 0) {
                                getUUID = method;
                                break;
                            }
                        }
                        if (getUUID != null) {
                            getUUID.setAccessible(true);
                            uuid = (String) getUUID.invoke(methodHookParam);
                        } else {
                            log("Warning: getUUID method not found.");
                        }
                    } else {
                        log("Warning: uuidHelper class not found.");
                    }

                    JSONObject jSONObject = new JSONObject();
                    jSONObject.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
                    jSONObject.put("os", MIUI_VERSION);
                    jSONObject.put("biz_id", "virus_scan");
                    jSONObject.put("uuid", uuid);

                    JSONArray jSONArray = new JSONArray();
                    for (int i2 = 0; i2 < list.size(); i2++) {
                        JSONObject jSONObject2 = new JSONObject();

                        String pkgName = null;
                        String version = null;
                        String sign = null;
                        String appName = null;

                        Object antiDefraudAppInfo = (Object) list.get(i2);

                        Field[] fields = antiDefraudAppInfo.getClass().getDeclaredFields();
                        for (Field filed : fields) {
                            filed.setAccessible(true);
                            if (filed.getName().equals("pkgName")) {
                                pkgName = (String) filed.get(antiDefraudAppInfo);
                            } else if (filed.getName().equals("version")) {
                                version = (String) filed.get(antiDefraudAppInfo);
                            } else if (filed.getName().equals("sign")) {
                                sign = (String) filed.get(antiDefraudAppInfo);
                            } else if (filed.getName().equals("appName")) {
                                appName = (String) filed.get(antiDefraudAppInfo);
                            }
                        }

                        jSONObject2.put("pkg", pkgName);
                        jSONObject2.put("version", version);
                        jSONObject2.put("signature", sign);
                        jSONObject2.put("appname", appName);

                        jSONArray.put(jSONObject2);
                    }
                    jSONObject.put("content", jSONArray);

                    log("Info: Intercept=" + jSONObject.toString());

                    methodHookParam.setResult(null);
                }
            });
        }
    }

   /* @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("com.miui.guardprovider")) {
            XposedBridge.log("Cemiuiler: Start to hook package " + lpparam.packageName);

            // Debug mode flag process
            final Class<?> guardApplication = XposedHelpers.findClass("com.miui.guardprovider.GuardApplication", lpparam.classLoader);
            if (guardApplication != null) {
                Field[] guardApplicationFields = guardApplication.getDeclaredFields();
                for (Field field : guardApplicationFields) {
                    if (field.getName().equals("c")) {
                        XposedHelpers.setStaticBooleanField(guardApplication, "c", true);
                        XposedBridge.log("Cemiuiler: Info: GuardProvider will work as debug mode!");
                    }
                    XposedBridge.log("Cemiuiler: Warning: GuardProvider debug mode flag not found!");
                }
            } else {
                XposedBridge.log("Cemiuiler: Warning: GuardApplication class not found. GuardProvider will not work as debug mode! ");
            }

            // Prevent miui from uploading app list
            final Class<?> antiDefraudAppManager = XposedHelpers.findClassIfExists("com.miui.guardprovider.engine.mi.antidefraud.AntiDefraudAppManager", lpparam.classLoader);
            if (antiDefraudAppManager == null) {
                XposedBridge.log("Cemiuiler: Skip: AntiDefraudAppManager class not found.");
                return;
            } else {
                XposedBridge.log("Cemiuiler: Info: AntiDefraudAppManager class found.");
            }

            final Method[] methods = antiDefraudAppManager.getDeclaredMethods();
            Method getAllUnSystemAppsStatus = null;
            for (Method method : methods) {
                if (method.getName().equals("getAllUnSystemAppsStatus") && method.getParameterTypes().length == 1) {
                    getAllUnSystemAppsStatus = method;
                    break;
                }
            }
            if (getAllUnSystemAppsStatus == null) {
                XposedBridge.log("Cemiuiler: Skip: getAllUnSystemAppsStatus method not found.");
                return;
            } else {
                XposedBridge.log("Cemiuiler: Info: getAllUnSystemAppsStatus method found.");
            }

            XposedBridge.hookMethod(getAllUnSystemAppsStatus, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    List<Object> list = (List) methodHookParam.args[0];

                    String MIUI_VERSION = null;
                    Field[] antiDefraudAppManagerFields = antiDefraudAppManager.getDeclaredFields();
                    for (Field field : antiDefraudAppManagerFields) {
                        if (field.getName().equals("MIUI_VERSION")) {
                            MIUI_VERSION = (String) XposedHelpers.getStaticObjectField(antiDefraudAppManager, "MIUI_VERSION");
                        }
                    }
                    if (MIUI_VERSION == null) {
                        XposedBridge.log("Cemiuiler: Warning: Can't get MIUI_VERSION.");
                    }

                    String uuid = null;
                    final Class<?> uuidHelper = XposedHelpers.findClassIfExists("i.b", lpparam.classLoader);
                    if (uuidHelper != null) {
                        final Method[] uuidHelperMethods = uuidHelper.getDeclaredMethods();
                        Method getUUID = null;
                        for (Method method : uuidHelperMethods) {
                            if (method.getName().equals("b") && method.getParameterTypes().length == 0) {
                                getUUID = method;
                                break;
                            }
                        }
                        if (getUUID != null) {
                            getUUID.setAccessible(true);
                            uuid = (String) getUUID.invoke(methodHookParam);
                        } else {
                            XposedBridge.log("Cemiuiler: Warning: getUUID method not found.");
                        }
                    } else {
                        XposedBridge.log("Cemiuiler: Warning: uuidHelper class not found.");
                    }

                    JSONObject jSONObject = new JSONObject();
                    jSONObject.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
                    jSONObject.put("os", MIUI_VERSION);
                    jSONObject.put("biz_id", "virus_scan");
                    jSONObject.put("uuid", uuid);

                    JSONArray jSONArray = new JSONArray();
                    for (int i2 = 0; i2 < list.size(); i2++) {
                        JSONObject jSONObject2 = new JSONObject();

                        String pkgName = null;
                        String version = null;
                        String sign = null;
                        String appName = null;

                        Object antiDefraudAppInfo = (Object) list.get(i2);

                        Field[] fields = antiDefraudAppInfo.getClass().getDeclaredFields();
                        for (Field filed : fields) {
                            filed.setAccessible(true);
                            if (filed.getName().equals("pkgName")) {
                                pkgName = (String) filed.get(antiDefraudAppInfo);
                            } else if (filed.getName().equals("version")) {
                                version = (String) filed.get(antiDefraudAppInfo);
                            } else if (filed.getName().equals("sign")) {
                                sign = (String) filed.get(antiDefraudAppInfo);
                            } else if (filed.getName().equals("appName")) {
                                appName = (String) filed.get(antiDefraudAppInfo);
                            }
                        }

                        jSONObject2.put("pkg", pkgName);
                        jSONObject2.put("version", version);
                        jSONObject2.put("signature", sign);
                        jSONObject2.put("appname", appName);

                        jSONArray.put(jSONObject2);
                    }
                    jSONObject.put("content", jSONArray);

                    XposedBridge.log("Cemiuiler: Info: Intercept=" + jSONObject.toString());

                    methodHookParam.setResult(null);
                }
            });
        }
    }*/


}

