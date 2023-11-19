package com.sevtinge.hyperceiler.module.hook.camera;

import android.util.SparseArray;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.MethodDataList;
import org.luckypray.dexkit.query.matchers.AnnotationMatcher;
import org.luckypray.dexkit.query.matchers.AnnotationsMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class CustomWatermark extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        logD("0");
        MethodDataList methodDataList = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .returnType(SparseArray.class)
                .modifiers(Modifier.PUBLIC)
                .paramCount(0)
                .name("c")
                .annotations(AnnotationsMatcher.create()
                    .add(AnnotationMatcher.create()
                        .usingStrings("Ljava/lang/String;")
                    )
                )
            )
        );
        for (MethodData methodData : methodDataList) {
            Method method = methodData.getMethodInstance(lpparam.classLoader);
            logD(TAG, lpparam.packageName, "Current hooking method is " + method);
            hookMethod(method, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    SparseArray<String[]> sparseArray = new SparseArray<>(1);
                    sparseArray.put(0, new String[]{mPrefsMap.getString("camera_custom_watermark_manufacturer", "XIAOMI"), mPrefsMap.getString("camera_custom_watermark_device", "MI PHONE")});
                    param.setResult(sparseArray);
                }
            });
        }
    }
}
