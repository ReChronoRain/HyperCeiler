package com.sevtinge.cemiuiler.module.mms;

import static com.sevtinge.cemiuiler.module.mms.MmsDexKit.mMmsResultClassMap;

import android.content.Context;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.List;
import java.util.Objects;

import io.luckypray.dexkit.descriptor.member.DexClassDescriptor;

public class DisableAd extends BaseHook {
    @Override
    public void init() {
        try {
            List<DexClassDescriptor> result = Objects.requireNonNull(mMmsResultClassMap.get("DisableAd"));
            for (DexClassDescriptor descriptor : result) {
                Class<?> enableAds = descriptor.getClassInstance(lpparam.classLoader);
                log("EnableAds class is " + enableAds);
                findAndHookMethod(enableAds, "j", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        findAndHookMethod("com.miui.smsextra.ui.BottomMenu", "allowMenuMode", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
