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

package com.sevtinge.hyperceiler.libhook.rules.phrase;

import static com.sevtinge.hyperceiler.libhook.utils.api.InvokeUtils.setStaticField;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import org.json.JSONArray;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class NewUnPhraseLimit extends BaseHook {

    @Override
    public void init() {
        try {
            findAndHookMethod("android.inputmethodservice.InputMethodModuleManager", "loadDex", ClassLoader.class, String.class, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        ClassLoader classLoader = (ClassLoader) param.getArgs()[0];
                        setStaticField(findClass("com.miui.inputmethod.MiuiClipboardManager", classLoader), "MAX_CLIP_CONTENT_SIZE", Integer.MAX_VALUE);
                        EzxHelpUtils.findAndHookMethod("com.miui.inputmethod.MiuiClipboardManager", classLoader, "init", new IMethodHook() {
                                @Override
                                public void before(BeforeHookParam param) {
                                    setStaticField(findClass("com.miui.inputmethod.MiuiClipboardManager", classLoader), "MAX_CLIP_CONTENT_SIZE", Integer.MAX_VALUE);
                                }

                                @Override
                                public void after(AfterHookParam param) throws Exception {
                                    setStaticField(findClass("com.miui.inputmethod.MiuiClipboardManager", classLoader), "MAX_CLIP_CONTENT_SIZE", Integer.MAX_VALUE);
                                }
                            }
                        );
                    }
                }
            );
        } catch (Exception ignore) {}
        if (Objects.equals(getPackageName(), "com.miui.phrase")) {
            Method method = DexKit.findMember("BuildClipboardJson", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                            .usingStrings("get savedList size :")
                        )).singleOrNull();
                    return methodData;
                }
            });
            hookMethod(method, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    Object newModel = param.getArgs()[2];
                    String oldJson = (String) param.getArgs()[3];
                    Class<?> mgrCls = null;

                    JSONArray jsonArray = new JSONArray();
                    ArrayList<Object> list = new ArrayList<>();

                    if (newModel != null) {
                        list.add(newModel);
                    }

                    if (!TextUtils.isEmpty(oldJson)) {
                        mgrCls = findClass("com.miui.inputmethod.MiuiClipboardManager", getClassLoader());
                        List<?> oldList = (List<?>) callStaticMethod(mgrCls, "jsonToBeanList", oldJson);
                        list.addAll(oldList);
                    }

                    for (int i = 0; i < list.size(); i++) {
                        Object model = list.get(i);
                        Object jsonObj = callMethod(model, "toJSONObject");
                        jsonArray.put(jsonObj);
                    }

                    param.setResult(jsonArray.toString());
                }
            });

            // 解除 20 条限制
            Class<?> InputMethodUtil = findClass("com.miui.inputmethod.InputMethodUtil");
            setStaticField(InputMethodUtil, "sPhraseListSize", 0);
            findAndHookMethod(InputMethodUtil, "queryPhrase", Context.class, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    setStaticField(InputMethodUtil, "sPhraseListSize", 0);
                }
            });

            Class<?> AddPhraseActivity = findClass("com.miui.phrase.AddPhraseActivity");
            findAndHookMethod("com.miui.phrase.PhraseEditActivity", "onClick", View.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    Activity activity = (Activity) param.getThisObject();
                    Intent intent = new Intent(activity, AddPhraseActivity);
                    intent.setAction("com.miui.intent.action.PHRASE_ADD");
                    activity.startActivityForResult(intent, 0);
                    param.setResult(null);
                }
            });

            // 解除字数限制
            Method method1 = DexKit.findMember("phrase$1", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                            .declaredClass(ClassMatcher.create()
                                .usingStrings("phrase_list")
                            )
                            .usingStrings("layout_inflater")
                        )).singleOrNull();
                    return methodData;
                }
            });

            Field field = DexKit.findMember("phrase$2", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    return bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                            .declaredClass(ClassMatcher.create()
                                .usingStrings("phrase_list")
                            )
                            .type(EditText.class)
                        )
                    ).single();
                }
            });
            hookMethod(method1, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        EditText editText = (EditText) getObjectField(param.getThisObject(), field.getName());
                        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Integer.MAX_VALUE)});
                    }
                }
            );
        }
    }
}
