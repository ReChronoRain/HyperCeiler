/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.downloadsui;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.FieldDataList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class AlwaysShowDownloadLink extends BaseHook {
    private Class<?> mDownloadInfoClass;
    private Method mShowTaskDetailMethod;
    private Field mDownloadUrlField;
    private Field mDownloadDescField;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mDownloadInfoClass = requiredMember("DownloadInfo", bridge -> bridge.findClass(FindClass.create()
            .excludePackages("androidx")
            .matcher(ClassMatcher.create()
                .usingEqStrings("/s", " | ", "/")
            )).singleOrNull());
        mShowTaskDetailMethod = requiredMember("ShowTaskDetailMatcher", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .declaredClass(ClassMatcher.create().usingStrings("onEventMainThread noScrollListView="))
                .usingNumbers(4, 8, 0)
                .paramTypes(mDownloadInfoClass)
                .returnType(void.class)
            )).singleOrNull());
        mDownloadUrlField = requiredMember("DownloadUrl", bridge -> bridge.findField(FindField.create()
            .matcher(FieldMatcher.create()
                .addReadMethod(MethodMatcher.create()
                    .usingStrings("android.intent.action.PICK", "com.android.fileexplorer"))
                .declaredClass(mDownloadInfoClass)
                .type(String.class)
            )).singleOrNull());
        mDownloadDescField = requiredMember("DownloadDesc", bridge -> {
            FieldDataList fieldDataList = bridge.findField(FindField.create()
                .matcher(FieldMatcher.create()
                    .addReadMethod(MethodMatcher.create()
                        .declaredClass(ClassMatcher.create().usingStrings("onEventMainThread noScrollListView="))
                        .usingNumbers(4, 8, 0)
                        .paramTypes(mDownloadInfoClass)
                        .returnType(void.class))
                    .type(String.class)
                ));
            for (FieldData field : fieldDataList) {
                if (field.getName().equals(mDownloadUrlField.getName())) {
                    continue;
                }
                return field;
            }
            return null;
        });
        return true;
    }

    @Override
    public void init() {
        hookMethod(mShowTaskDetailMethod, new IMethodHook() {
            public void before(HookParam param) {
                String url = (String) getObjectField(param.getArgs()[0], mDownloadUrlField.getName());
                // @TODO 显示来源应用和路径
                XposedLog.d(TAG, getPackageName(), "url:" + url);
                setObjectField(param.getArgs()[0], mDownloadDescField.getName(), "");
            }
        });

        // findAndHookMethod("h1.h", "R", "i1.a",new IMethodHook() {
        //     @Override
        //     public void before(HookParam param) {
        //         XposedLog.d(TAG, getPackageName(), "source: " + getObjectField(param.getArgs[0], "r") + "  path: " + getObjectField(param.getArgs[0], "i"));
        //         setObjectField(param.getArgs[0], "y", "");
        //     }
        // });
    }
}
