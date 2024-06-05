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
package com.sevtinge.hyperceiler.module.hook.demo;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class ToastTest extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Class<?> c = findClassIfExists("com.hchen.demo.MainActivity");
        AnnotatedElement dd = findClassIfExists("com.hchen.demo.TestOne");
        Class<?> d = (Class<?>) dd;
        logE(TAG, "ccc: " + d);
        AnnotatedElement m = c.getDeclaredMethod("makeToast", String.class, d);
        Method method = (Method) m;
        String name = method.getName();
        String clName = method.getDeclaringClass().getName();
        int count = method.getParameterCount();
        Class<?>[] param = method.getParameterTypes();
        ArrayList<String> paramList = new ArrayList<>();
        for (Class<?> p : param) {
            paramList.add(p.getName());
        }
        logE(TAG, "name: " + name + " clz: " + clName + " co: " + count + " pa: " + paramList);
        try {
            AnnotatedElement aaaa = c.getDeclaredField("testOne");
            Field field = (Field) aaaa;
            String fieldName = field.getName();
            String cl = field.getDeclaringClass().getName();
            logE(TAG, "field: " + fieldName + " c: " + cl);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        AnnotatedElement aa = c.getConstructor(String.class);
        Constructor<?> constructor = (Constructor<?>) aa;
        String names = constructor.getName();
        String params = Arrays.toString(constructor.getParameterTypes());
        logE(TAG, "nnn: " + names + " pps: " + params);
        findAndHookMethod("com.hchen.demo.MainActivity", "makeToast",
                String.class, "com.hchen.demo.TestOne",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        logE(TAG, "run");
                    }
                }
        );
    }
}
