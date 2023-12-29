package com.sevtinge.hyperceiler.module.hook.aiasst;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class AiCaptions extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Class<?> mSupportAiSubtitlesUtils = findClassIfExists("com.xiaomi.aiasst.vision.utils.SupportAiSubtitlesUtils");
        Class<?> mSystemUtils = findClassIfExists("com.xiaomi.aiasst.vision.utils.SystemUtils");
        Class<?> mWhitelistChecker = findClassIfExists("com.xiaomi.aiasst.vision.picksound.whitelist.WhitelistChecker");

        try {
            XposedHelpers.setStaticBooleanField(mWhitelistChecker, "mVerified", true);
        } catch (Exception ignored) {
        }

        try {
            findAndHookMethod(mSupportAiSubtitlesUtils, "isSupportAiSubtitles", Context.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Exception ignored) {
        }
        try {
            findAndHookMethod(mSupportAiSubtitlesUtils, "isSupportOfflineAiSubtitles", Context.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Exception ignored) {
        }
        try {
            findAndHookMethod(mSupportAiSubtitlesUtils, "deviceWhetherSupportOfflineSubtitles", Context.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Exception ignored) {
        }
        try {
            findAndHookMethod(mSupportAiSubtitlesUtils, "isSupportJapanKorea", Context.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Exception ignored) {
        }
        try {
            findAndHookMethod(mSystemUtils, "isSupportAiPickSoundDevice", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Exception ignored) {
        }
    }
}
