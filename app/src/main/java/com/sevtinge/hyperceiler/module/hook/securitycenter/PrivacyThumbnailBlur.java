package com.sevtinge.hyperceiler.module.hook.securitycenter;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseTool;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;

public class PrivacyThumbnailBlur extends BaseTool {
    @Override
    public void doHook() {
        Method method = (Method) DexKit.getDexKitBridge("ptb", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                return bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("miui_recents_privacy_thumbnail_blur")
                                )
                                .paramTypes(Context.class, String.class, boolean.class)
                        )
                ).singleOrNull().getMethodInstance(lpparam.classLoader);
            }
        });

        hookMethod(method, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
                if (Arrays.stream(stackTraceElements).noneMatch(stackTraceElement ->
                        stackTraceElement.getClassName().equals("PrivacyThumbnailBlurSettings")
                )) {
                    param.setResult(null);
                }
            }
        });
    }
}
