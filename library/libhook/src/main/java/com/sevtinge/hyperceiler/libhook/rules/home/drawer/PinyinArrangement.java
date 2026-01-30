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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.home.drawer;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.LocaleList;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.Locale;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class PinyinArrangement extends BaseHook {
    LocaleList locale;
    Activity activity;

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.compat.AlphabeticIndexCompat",
                "computeSectionName", CharSequence.class, new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        CharSequence charSequence = (CharSequence) param.getArgs()[0];
                        String bucketLabel;
                        Class<?> Pinyin = findClassIfExists("com.github.promeg.pinyinhelper.Pinyin");
                        if (Pinyin == null) {
                            XposedLog.w(TAG, getPackageName(), "Pinyin is null!");
                            return;
                        }
                        String trim = (String) callStaticMethod(findClass("com.miui.home.launcher.common.Utilities"), "trim", charSequence);
                        if (!charSequence.isEmpty() && (boolean) callStaticMethod(Pinyin, "isChinese", charSequence.charAt(0))) {
                            bucketLabel = String.valueOf(
                                    ((String) callStaticMethod(Pinyin, "toPinyin", charSequence.toString(), "")).charAt(0));
                        } else {
                            Object o = getObjectField(param.getThisObject(), "mBaseIndex");
                            bucketLabel = (String) callMethod(o, "getBucketLabel", callMethod(o, "getBucketIndex", trim));
                        }
                        // logE("bucketLabel: " + bucketLabel + " trim1: " + trim);
                        String trim2 = (String) callStaticMethod(findClass("com.miui.home.launcher.common.Utilities"), "trim", bucketLabel);
                        // logE("trim2: " + trim2);
                        if (!trim2.isEmpty() || trim.length() <= 0) {
                            param.setResult(bucketLabel);
                            return;
                        }
                        int codePointAt = trim.codePointAt(0);
                        param.setResult(Character.isDigit(codePointAt) ? "…" : Character.isLetter(codePointAt) ? (String) getObjectField(param.getThisObject(), "mDefaultMiscLabel") : "∙");
                    }
                }
        );

        hookAllMethods("com.miui.home.launcher.allapps.BaseAlphabeticalAppsList",
                "onAppsUpdated", new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        activity = (Activity) getObjectField(param.getThisObject(), "mLauncher");
                        locale = activity.getResources().getConfiguration().getLocales();
                        activity.getResources().getConfiguration().setLocale(Locale.SIMPLIFIED_CHINESE);
                    }

                    @Override
                    public void after(AfterHookParam param) {
                        Configuration configuration = activity.getResources().getConfiguration();
                        configuration.setLocales(locale);
                    }
                }
        );

    }
}
