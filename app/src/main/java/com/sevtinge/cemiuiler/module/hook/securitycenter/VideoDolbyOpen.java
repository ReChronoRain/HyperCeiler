package com.sevtinge.cemiuiler.module.hook.securitycenter;

import androidx.annotation.NonNull;

import com.github.kyuubiran.ezxhelper.HookFactory;
import com.github.kyuubiran.ezxhelper.interfaces.IMethodHookCallback;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.util.function.Consumer;

import de.robv.android.xposed.XC_MethodHook;

public class VideoDolbyOpen extends BaseHook {
    @Override
    public void init() {
        // try {
        //     findClassIfExists("com.miui.gamebooster.service.DockWindowManagerService").getDeclaredMethod("N");
        //     findAndHookMethod("com.miui.gamebooster.service.DockWindowManagerService", "N", new MethodHook() {
        //         @Override
        //         protected void before(MethodHookParam param) {
        //             logI("Hook N");
        //             param.setResult(null);
        //         }
        //     });
        // } catch (NoSuchMethodException e) {
        //     logI("Don't Find DockWindowManagerService$N");
        // }

        // 查找类
        // ClassData data = DexKit.INSTANCE.getDexKitBridge().findClass(FindClass.create()
        //     .searchPackages("com.miui.gamebooster.service")
        //     .matcher(ClassMatcher.create()
        //         .className("com.miui.gamebooster.service.DockWindowManagerService")
        //     )
        // ).firstOrThrow(() -> new IllegalStateException("VideoDolbyOpen: No class found ClassData"));
        // // 类加入列表
        // List<ClassData> list = Collections.singletonList(data);

        // 查找方法
        MethodData methodData = DexKit.INSTANCE.getDexKitBridge().findMethod(
            FindMethod.create()
                .matcher(MethodMatcher.create()
                    .declaredClass(ClassMatcher.create()
                        .usingStrings("checkMiGamePermission error"))
                    .usingStrings("dolby")
                )
        ).firstOrThrow(() -> new IllegalStateException("VideoDolbyOpen: No class found MethodData"));

        // 执行Hook
        try {
            HookFactory.createMethodHook(methodData.getMethodInstance(lpparam.classLoader), new Consumer<>() {
                @Override
                public void accept(HookFactory hookFactory) {
                    hookFactory.before(
                        new IMethodHookCallback() {
                            @Override
                            public void onMethodHooked(@NonNull XC_MethodHook.MethodHookParam methodHookParam) {
                                methodHookParam.setResult(null);
                            }
                        }
                    );
                }
            });
        } catch (NoSuchMethodException e) {
            logE("NoSuchMethodException: " + e);
        }
    }
}
