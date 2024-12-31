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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.various.clipboard;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.hook.IHook;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Field;

/**
 * 解除常用语 20 条限制和字数限制
 * from <a href="https://github.com/HChenX/ClipboardList">ClipboardList</a>
 *
 * @author 焕晨HChen
 */
public class NewUnPhraseLimit extends BaseHC {
    private final DexKitBridge dexKitBridge;

    public NewUnPhraseLimit(DexKitBridge dexKitBridge) {
        this.dexKitBridge = dexKitBridge;
    }

    @Override
    public void init() {
        try {
            // 解除 20 条限制
            Class<?> InputMethodUtil = findClass("com.miui.inputmethod.InputMethodUtil").get();
            setStaticField(InputMethodUtil, "sPhraseListSize", 0);
            hookMethod(InputMethodUtil, "queryPhrase", Context.class, new IHook() {
                @Override
                public void after() {
                    setStaticField(InputMethodUtil, "sPhraseListSize", 0);
                }
            });

            Class<?> AddPhraseActivity = findClass("com.miui.phrase.AddPhraseActivity").get();
            hookMethod("com.miui.phrase.PhraseEditActivity", "onClick", View.class, new IHook() {
                @Override
                public void before() {
                    Activity activity = (Activity) thisObject();
                    View view = (View) getArgs(0);
                    int id = activity.getResources().getIdentifier("fab", "id", "com.miui.phrase");
                    if (view.getId() == id) {
                        Intent intent = new Intent(activity, AddPhraseActivity);
                        intent.setAction("com.miui.intent.action.PHRASE_ADD");
                        activity.startActivityForResult(intent, 0);
                        returnNull();
                    }
                }
            });

            // 解除字数限制
            MethodData methodData1 = dexKitBridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .declaredClass(ClassMatcher.create()
                                    .usingStrings("phrase_list")
                            )
                            .usingStrings("layout_inflater")
                    )
            ).singleOrThrow(() -> new RuntimeException("method is null!!"));
            FieldData fieldData = dexKitBridge.findField(FindField.create()
                    .matcher(FieldMatcher.create()
                            .declaredClass(ClassMatcher.create()
                                    .usingStrings("phrase_list")
                            )
                            .type(EditText.class)
                    )
            ).singleOrThrow(() -> new RuntimeException("field is null!!"));
            Field f = fieldData.getFieldInstance(lpparam.classLoader);
            hook(methodData1.getMethodInstance(lpparam.classLoader), new IHook() {
                @Override
                public void after() {
                    EditText editText = (EditText) getField(thisObject(), f);
                    editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Integer.MAX_VALUE)});
                }
            });
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            logE(TAG, e);
        }
    }
}
