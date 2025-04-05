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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook;

import android.app.Application;
import android.content.Context;

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.tool.ParamTool;
import com.sevtinge.hyperceiler.callback.IAttachBaseContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class ApplicationHook extends BaseHC {
    public static IAttachBaseContext[] iAttachBaseContexts;

    public ApplicationHook(IAttachBaseContext... attachBaseContexts) {
        iAttachBaseContexts = attachBaseContexts;
    }

    public static void addAttachBaseContextCallBack(IAttachBaseContext iAttachBaseContext) {
        ArrayList<IAttachBaseContext> list;
        if (iAttachBaseContexts != null)
            list = new ArrayList<>(Arrays.asList(iAttachBaseContexts));
        else
            list = new ArrayList<>();

        list.add(iAttachBaseContext);
        iAttachBaseContexts = list.toArray(new IAttachBaseContext[0]);
    }

    @Override
    public void init() {
        hookMethod(Application.class, "attach", Context.class, new IHook() {
            @Override
            public void before() {
                callBackBefore(this, (Context) getArgs(0));
            }

            @Override
            public void after() {
                callBackAfter(this, (Context) getArgs(0));
            }
        });
    }

    private void callBackBefore(ParamTool param, Context context) {
        if (iAttachBaseContexts != null) {
            Arrays.stream(iAttachBaseContexts).forEach(new Consumer<IAttachBaseContext>() {
                @Override
                public void accept(IAttachBaseContext iAttachBaseContext) {
                    iAttachBaseContext.onAttachBaseContextCreateBefore(param, context);
                }
            });
        }
    }

    private void callBackAfter(ParamTool param, Context context) {
        if (iAttachBaseContexts != null) {
            Arrays.stream(iAttachBaseContexts).forEach(new Consumer<IAttachBaseContext>() {
                @Override
                public void accept(IAttachBaseContext iAttachBaseContext) {
                    iAttachBaseContext.onAttachBaseContextCreateAfter(param, context);
                }
            });
        }
    }
}
