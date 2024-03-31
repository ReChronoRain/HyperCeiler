package com.sevtinge.hyperceiler.module.hook.calendar;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

public class UnlockSubscription extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData1 = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                        .declaredClass(ClassMatcher.create()
                                .usingStrings("Cal:D:CalendarApplicationDelegate"))
                        .usingStrings("key_subscription_display", "key_import_todo", "key_chinese_almanac_pref", "key_weather_display", "key_ai_time_parse")
                        .paramCount(0)
                )
        ).singleOrThrow(() -> new IllegalStateException("UnlockSubscription: Cannot found MethodData"));
        Method method1 = methodData1.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "Method is " + method1);
        hookMethod(method1, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                try {
                    findAndHookMethod(findClass("android.app.SharedPreferencesImpl$EditorImpl"), "putBoolean", String.class, boolean.class, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            if (param.args[0] == "key_subscription_display" ||
                                    param.args[0] == "key_import_todo" ||
                                    param.args[0] == "key_chinese_almanac_pref" ||
                                    param.args[0] == "key_weather_display" ||
                                    param.args[0] == "key_ai_time_parse") param.args[1] = true;
                        }
                    });
                } catch (Exception e) {
                    logE(TAG, lpparam.packageName, "Cannot hook android.app.SharedPreferencesImpl$EditorImpl.putBoolean(String, boolean)", e);
                }
            }
        });
    }
}
