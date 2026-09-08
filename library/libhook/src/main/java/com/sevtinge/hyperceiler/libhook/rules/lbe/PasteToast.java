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
/**
 * 优化剪贴板粘贴提示 Toast
 *
 * @author LuoYunXi0407
 */
package com.sevtinge.hyperceiler.libhook.rules.lbe;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.util.Objects;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class PasteToast extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod(
            "com.lbe.security.utility.ToastUtil",
            "initToastView",
            String.class,
            int.class,
            new IMethodHook() {

                @Override
                public void after(HookParam param)  {
                    if(param == null) return;;
                    Object[] args = param.getArgs();
                    int type = (int) args[1];
                    if (type != 1) return;

                    View view = (View) param.getResult();
                    if (view == null) return;
                    view.setAlpha((float) (PrefsBridge.getInt("prefs_key_lbe_paste_toast_custom_toast_opacity", 100)/100.0));


                    Context context = view.getContext();

                    int closeId = context.getResources()
                        .getIdentifier("closeButton", "id", "android");

                    View closeBtn = view.findViewById(closeId);
                    if (!(closeBtn instanceof TextView)) return;

                    TextView btn = (TextView) closeBtn;
                    String btnStr = PrefsBridge.getString("prefs_key_lbe_paste_toast_custom_close_button_text_custom", "");
                    if(!Objects.equals(btnStr, ""))
                    {
                        btn.setText(btnStr);
                    }

                    if(PrefsBridge.getBoolean("prefs_key_lbe_paste_toast_custom_close_button_to_close", false)) {
                        btn.setOnClickListener(v -> {
                            View root = view;
                            root.setVisibility(View.GONE);

                            root.setAlpha(0f);

                            try {
                                Object wm = context.getSystemService(Context.WINDOW_SERVICE);
                                callMethod(wm, "removeView", root);
                            } catch (Throwable ignored) {
                            }

                        });
                    }
                }
            }
        );
    }
}
