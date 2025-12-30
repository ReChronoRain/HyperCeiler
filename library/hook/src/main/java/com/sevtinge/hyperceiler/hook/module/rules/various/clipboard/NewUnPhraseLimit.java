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

package com.sevtinge.hyperceiler.hook.module.rules.various.clipboard;

import static com.sevtinge.hyperceiler.hook.utils.InvokeUtils.setStaticField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;

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

/**
 * 解除常用语 20 条限制和字数限制
 * from <a href="https://github.com/HChenX/ClipboardList">ClipboardList</a>
 *
 * @author 焕晨HChen
 */
public class NewUnPhraseLimit extends BaseHook {

    @Override
    public void init() {
        // 解除 20 条限制
        Class<?> InputMethodUtil = findClass("com.miui.inputmethod.InputMethodUtil");
        setStaticField(InputMethodUtil, "sPhraseListSize", 0);
        findAndHookMethod(InputMethodUtil, "queryPhrase", Context.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                setStaticField(InputMethodUtil, "sPhraseListSize", 0);
            }
        });

        Class<?> AddPhraseActivity = findClass("com.miui.phrase.AddPhraseActivity");
        findAndHookMethod("com.miui.phrase.PhraseEditActivity", "onClick", View.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Intent intent = new Intent(activity, AddPhraseActivity);
                intent.setAction("com.miui.intent.action.PHRASE_ADD");
                activity.startActivityForResult(intent, 0);
                param.setResult(null);
            }
        });

        // 解除字数限制
        Method method = DexKit.findMember("phrase$1", new IDexKit() {
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
        hookMethod(method, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    EditText editText = (EditText) getObjectField(param.thisObject, field.getName());
                    editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Integer.MAX_VALUE)});
                }
            }
        );
    }
}
