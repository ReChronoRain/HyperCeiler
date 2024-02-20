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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
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
