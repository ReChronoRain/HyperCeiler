package com.sevtinge.hyperceiler.libhook.rules.voiceassist;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.base.BaseData;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class RemoveScreenTransWatermark extends BaseHook {

    private Class<?> mClazz;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mClazz = optionalMember("ProcessScreenTransResultClazz", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create()
                        .usingStrings("TextConfig(text=")
                    )).singleOrNull();
                return clazzData;
            }
        });
        return true;
    }

    @Override
    public void init() {
        hookAllConstructors(mClazz, new IMethodHook() {
            @Override
            public void before(HookParam param) throws Throwable {
                param.getArgs()[0] = " ";
            }
        });
    }
}
