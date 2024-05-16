package com.sevtinge.hyperceiler.module.base.dexkit;

import static com.sevtinge.hyperceiler.module.base.dexkit.DexKitCacheFile.readFile;
import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.getPackageVersionCode;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logD;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;

import com.sevtinge.hyperceiler.module.base.dexkit.DexKitCacheFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DexKitData {

    public static void hookMethodWithDexKit(String tag, XC_LoadPackage.LoadPackageParam loadPackageParam, MethodMatcher methodMatcher, Object... callback) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callingClassName = stackTrace[3].getClassName();
        int lastDotIndex = callingClassName.lastIndexOf('.');
        callingClassName = callingClassName.substring(lastDotIndex + 1);
        try {
            String className;
            String methodName;
            List<String> paramList;
            if (!DexKitCacheFile.isEmptyFile(loadPackageParam, callingClassName, tag)) {
                try {
                    className = getClassName(loadPackageParam, callingClassName, tag);
                    methodName = getMethodName(loadPackageParam, callingClassName, tag);
                    paramList = getParamList(loadPackageParam, callingClassName, tag);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            } else {
                MethodData methodData = DexKit.getDexKitBridge().findMethod(FindMethod.create()
                        .matcher(methodMatcher)
                ).singleOrThrow(() -> new IllegalStateException("No Such Method Found."));
                className = methodData.getClassName();
                methodName = methodData.getMethodName();
                paramList = methodData.getParamNames();
                putDexKitCache(loadPackageParam, callingClassName, tag, className, methodName, paramList);
            }
            if (paramList == null) {
                findAndHookMethod(findClassIfExists(className, loadPackageParam.classLoader), methodName, callback);
            } else {
                findAndHookMethod(findClassIfExists(className, loadPackageParam.classLoader), methodName, paramList.toArray(), callback);
            }
        } catch (Exception e){
logE(callingClassName, loadPackageParam.packageName, "Having trouble finding "+tag+": "+e);
        }
    }

    public static void putDexKitCache(XC_LoadPackage.LoadPackageParam loadPackageParam, String callingClassName, String tag, String className, String methodName, List<String> paramList) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("ClassName", className);
            jsonObject.put("MethodName", methodName);
            jsonObject.put("ParamList", paramList);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(jsonObject);
        DexKitCacheFile.writeFile(loadPackageParam, jsonArray, callingClassName, tag);
    }

    public static String getClassName(XC_LoadPackage.LoadPackageParam loadPackageParam, String callingClassName, String tag) throws JSONException {
        return readFile(loadPackageParam, callingClassName, tag).getJSONObject(0).getString("ClassName");
    }

    public static String getMethodName(XC_LoadPackage.LoadPackageParam loadPackageParam, String callingClassName, String tag) throws JSONException {
        return readFile(loadPackageParam, callingClassName, tag).getJSONObject(0).getString("MethodName");
    }

    public static List<String> getParamList(XC_LoadPackage.LoadPackageParam loadPackageParam, String callingClassName, String tag) throws JSONException {
        JSONObject jsonObject = readFile(loadPackageParam, callingClassName, tag).getJSONObject(0);
        if (jsonObject.has("ParamList")) {
            List<String> list = Arrays.asList(jsonObject.getString("ParamList").split(","));
            if (!list.isEmpty()) {
                return list;
            }
        }
        return null;
    }

    public static class MethodHookWithDexKit extends XC_MethodHook {

        protected void before(MethodHookParam param) throws Throwable {
        }

        protected void after(MethodHookParam param) throws Throwable {
        }

        public MethodHookWithDexKit() {
            super();
        }

        public MethodHookWithDexKit(int priority) {
            super(priority);
        }

        public static MethodHookWithDexKit returnConstant(final Object result) {
            return new MethodHookWithDexKit(PRIORITY_DEFAULT) {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(result);
                }
            };
        }

        public static final MethodHookWithDexKit DO_NOTHING = new MethodHookWithDexKit(PRIORITY_HIGHEST * 2) {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(null);
            }
        };

        @Override
        public void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                this.before(param);
            } catch (Throwable t) {
                // logE("BeforeHook", t);
            }
        }

        @Override
        public void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                this.after(param);
            } catch (Throwable t) {
                // logE("AfterHook", t);
            }
        }
    }

}
