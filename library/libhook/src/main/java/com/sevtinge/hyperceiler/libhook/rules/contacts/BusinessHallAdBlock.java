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

package com.sevtinge.hyperceiler.libhook.rules.contacts;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

/**
 * 去除营业厅广告与推广模块
 * <p>
 * 多层防御策略：
 * Layer 1: 数据层 — RecommendResponse.Data getter → return null（Kotlin data class getter 不受混淆影响）
 * Layer 1.5: 数据层 — CommonProduct.getActivityData() → return null
 * Layer 2A: 数据层 — ContainerAdapter 数据设置方法 → 跳过执行（阻止充话费/更多服务的标志位被置 true）
 * Layer 2B: UI 层 — getItemCount 过滤 viewType（兜底）
 * Layer 3: 弹窗层 — RecommendDialog/PopupActivity → onCreate 后立即关闭
 * Layer 4: SIM tab — 替换 "0元领副卡" 推广文字
 */
public class BusinessHallAdBlock extends BaseHook {

    private static final String TAG = "BHAdBlock";

    // 目标类名
    private static final String CLS_RECOMMEND_DATA = "com.mobile.businesshall.bean.RecommendResponse$Data";
    private static final String CLS_COMMON_PRODUCT = "com.mobile.businesshall.bean.CommonProduct";
    private static final String CLS_CONTAINER_ADAPTER = "com.mobile.businesshall.ui.main.home.homeDecompose.ContainerAdapter";
    private static final String CLS_RECOMMEND_DIALOG = "com.mobile.businesshall.ui.main.home.RecommendDialogFragment";
    private static final String CLS_RECOMMEND_POPUP = "com.mobile.businesshall.ui.common.RecommendPopupActivity";
    private static final String CLS_SIM_CONTAINER = "com.mobile.businesshall.widget.SimContainerView";

    // viewType 常量（ContainerAdapter 内部映射）
    private static final int VT_PACKAGE = 0;        // 套餐用量（保留）
    private static final int VT_NO_SIM = 1;         // 无SIM提示（保留）
    private static final int VT_SIM_RECOMMEND = 2;  // SIM推荐（广告）
    private static final int VT_CARD_BANNER = 3;    // 有卡推荐Banner（广告）
    private static final int VT_RECHARGE = 4;        // 充话费（屏蔽）
    private static final int VT_TRAFFIC = 5;         // 流量（保留）
    private static final int VT_MORE_SERVICE = 6;   // 更多服务（屏蔽）
    private static final int VT_NO_CARD_REC = 7;    // 无卡推荐（广告）
    private static final int VT_BOTTOM = 8;         // 底部按钮（保留）

    // 需要过滤的 viewType 集合
    private static final int[] BLOCKED_VIEW_TYPES = {
            VT_SIM_RECOMMEND, VT_CARD_BANNER, VT_RECHARGE, VT_MORE_SERVICE, VT_NO_CARD_REC
    };

    // RecommendResponse.Data 的广告字段 getter 方法名
    private static final String[] AD_GETTER_METHODS = {
            "getPopUpWindowRec", "getContactRec", "getContactDualCard",
            "getContactNoCard", "getContactOneCard", "getPayResultRec",
            "getContactIndexTrafficQuery", "getContactIndexVirtualCard",
    };

    // Layer 4 缓存（resolve 一次后复用）
    private boolean layer4Resolved = false;
    private String layer4AdText = null;
    private int layer4TvId = 0;
    private int layer4DotId = 0;

    @Override
    public void init() {
        log("init start");
        hookAdGetters();
        hookProductActivityData();
        hookContainerAdapter();
        hookRecommendPopup();
        hookSimTabText();
        log("init done");
    }

    private void log(String msg) {
        XposedLog.i(TAG, getPackageName(), msg);
    }

    // ===== Layer 1 =====

    private void hookAdGetters() {
        // 先尝试 $ 分隔符（JVM 标准内部类），再尝试 . 分隔符
        Class<?> dataClass = findClassIfExists(CLS_RECOMMEND_DATA);
        if (dataClass == null) {
            dataClass = findClassIfExists(CLS_RECOMMEND_DATA.replace('$', '.'));
        }
        if (dataClass == null) {
            log("RecommendResponse.Data not found, skip Layer1");
            return;
        }

        int hooked = 0;
        for (String getterName : AD_GETTER_METHODS) {
            try {
                Method getter = dataClass.getDeclaredMethod(getterName);
                hookMethod(getter, returnConstant(null));
                hooked++;
            } catch (NoSuchMethodException e) {
                log("Getter not found: " + getterName);
            } catch (Throwable t) {
                log("Hook getter error: " + getterName + " → " + t.getMessage());
            }
        }
        log("Layer1: hooked " + hooked + "/" + AD_GETTER_METHODS.length + " getters");
    }

    // ===== Layer 1.5 =====

    private void hookProductActivityData() {
        Class<?> clazz = findClassIfExists(CLS_COMMON_PRODUCT);
        if (clazz == null) {
            log("CommonProduct not found, skip Layer1.5");
            return;
        }
        try {
            Method getter = clazz.getDeclaredMethod("getActivityData");
            hookMethod(getter, returnConstant(null));
            log("Layer1.5: hooked CommonProduct.getActivityData");
        } catch (NoSuchMethodException e) {
            log("Layer1.5: getActivityData not found");
        } catch (Throwable t) {
            log("Layer1.5 error: " + t.getMessage());
        }
    }

    // ===== Layer 2 =====

    private void hookContainerAdapter() {
        Class<?> adapterClazz = findClassIfExists(CLS_CONTAINER_ADAPTER);
        if (adapterClazz == null) {
            log("ContainerAdapter not found, skip Layer2");
            return;
        }

        Class<?> commonProductClass = findClassIfExists(CLS_COMMON_PRODUCT);

        // Part A: 阻止充话费(viewType=4)和更多服务(viewType=6)的数据填充
        if (commonProductClass != null) {
            int blocked = 0;
            for (Method m : adapterClazz.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (m.getReturnType() == void.class && params.length >= 1
                        && params[0] == commonProductClass) {
                    try {
                        hookMethod(m, new IMethodHook() {
                            @Override
                            public void before(HookParam param) {
                                param.setResult(null);
                            }
                        });
                        blocked++;
                    } catch (Throwable ignored) {
                    }
                }
            }
            log("Layer2A: blocked " + blocked + " data setters");
        }

        // Part B: getItemCount 过滤 viewType（兜底）
        Method itemCountMethod = findMethodByNames(adapterClazz, "getItemCount", "r");
        if (itemCountMethod == null) {
            // 最终兜底：找 public int 无参方法
            itemCountMethod = findFirstPublicIntMethod(adapterClazz);
        }

        if (itemCountMethod != null) {
            try {
                hookMethod(itemCountMethod, new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        ArrayList<Integer> typeList = findTypeList(param.getThisObject());
                        if (typeList == null || typeList.isEmpty()) return;

                        // 先检查是否需要过滤，避免不必要的 ArrayList 分配
                        boolean needsFilter = false;
                        for (Integer type : typeList) {
                            if (isBlockedType(type)) {
                                needsFilter = true;
                                break;
                            }
                        }
                        if (!needsFilter) return;

                        typeList.removeIf(type -> isBlockedType(type));
                        param.setResult(typeList.size());
                    }
                });
                log("Layer2B: hooked " + itemCountMethod.getName());
            } catch (Throwable t) {
                log("Layer2B error: " + t.getMessage());
            }
        }
    }

    private static boolean isBlockedType(int type) {
        for (int blocked : BLOCKED_VIEW_TYPES) {
            if (type == blocked) return true;
        }
        return false;
    }

    /**
     * 查找 ContainerAdapter 内部的 viewType 列表字段。
     * 使用 EzxHelpUtils.findFirstFieldByExactType 缓存 Field 引用，避免每次反射扫描。
     */
    @SuppressWarnings("unchecked")
    private ArrayList<Integer> findTypeList(Object adapter) {
        // 已知混淆名快速路径
        for (String name : new String[]{"r", "f19765r"}) {
            Object field = getObjectField(adapter, name);
            if (field instanceof ArrayList && !((ArrayList<?>) field).isEmpty()
                    && ((ArrayList<?>) field).get(0) instanceof Integer) {
                return (ArrayList<Integer>) field;
            }
        }

        // 通过类型查找（EzxHelpUtils 内部有缓存）
        try {
            Field f = EzxHelpUtils.findFirstFieldByExactTypeIfExists(adapter.getClass(), ArrayList.class);
            if (f != null) {
                f.setAccessible(true);
                Object value = f.get(adapter);
                if (value instanceof ArrayList) {
                    ArrayList<?> list = (ArrayList<?>) value;
                    if (!list.isEmpty() && list.get(0) instanceof Integer) {
                        return (ArrayList<Integer>) list;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // ===== Layer 3 =====

    private void hookRecommendPopup() {
        Class<?> dialogClazz = findClassIfExists(CLS_RECOMMEND_DIALOG);
        if (dialogClazz != null) {
            try {
                findAndHookMethod(dialogClazz, "onCreate", android.os.Bundle.class,
                        new IMethodHook() {
                            @Override
                            public void after(HookParam param) {
                                dismissDialog(param.getThisObject());
                            }
                        });
                log("Layer3: hooked RecommendDialogFragment");
            } catch (Throwable t) {
                log("Layer3 dialog error: " + t.getMessage());
            }
        }

        Class<?> popupClazz = findClassIfExists(CLS_RECOMMEND_POPUP);
        if (popupClazz != null) {
            try {
                findAndHookMethod(popupClazz, "onCreate", android.os.Bundle.class,
                        new IMethodHook() {
                            @Override
                            public void after(HookParam param) {
                                try {
                                    callMethod(param.getThisObject(), "finish");
                                } catch (Throwable ignored) {
                                }
                            }
                        });
                log("Layer3: hooked RecommendPopupActivity");
            } catch (Throwable t) {
                log("Layer3 popup error: " + t.getMessage());
            }
        }
    }

    private void dismissDialog(Object fragment) {
        for (String name : new String[]{"dismiss", "I1", "dismissAllowingStateLoss"}) {
            try {
                callMethod(fragment, name);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    // ===== Layer 4 =====

    private void hookSimTabText() {
        Class<?> clazz = findClassIfExists(CLS_SIM_CONTAINER);
        if (clazz == null) {
            log("SimContainerView not found, skip Layer4");
            return;
        }

        IMethodHook fixHook = new IMethodHook() {
            @Override
            public void after(HookParam param) {
                fixSimTabTexts(param.getThisObject());
            }
        };

        // 只 hook 已知的方法名，避免 hook 过多无关方法（如动画、布局回调）
        int hookedCount = 0;
        for (String name : new String[]{"setItems", "setSimInfoList", "d", "f", "a", "b", "c"}) {
            // 无参版本
            try {
                hookMethod(clazz.getDeclaredMethod(name), fixHook);
                hookedCount++;
            } catch (NoSuchMethodException ignored) {
            }
            // ArrayList 参数版本
            try {
                hookMethod(clazz.getDeclaredMethod(name, ArrayList.class), fixHook);
                hookedCount++;
            } catch (NoSuchMethodException ignored) {
            }
        }
        log("Layer4: hooked " + hookedCount + " methods");
    }

    private void fixSimTabTexts(Object viewObj) {
        if (!(viewObj instanceof View)) return;
        View view = (View) viewObj;

        // 首次调用时 resolve 资源 ID，后续复用
        if (!layer4Resolved) {
            Context ctx = view.getContext();
            String pkg = ctx.getPackageName();
            int adStrId = ctx.getResources().getIdentifier("bh_receive_zero_card", "string", pkg);
            if (adStrId != 0) {
                layer4AdText = ctx.getString(adStrId);
            }
            layer4TvId = ctx.getResources().getIdentifier("textview1", "id", pkg);
            layer4DotId = ctx.getResources().getIdentifier("iv_red_dot", "id", pkg);
            layer4Resolved = true;
        }

        if (layer4AdText == null || layer4AdText.isEmpty() || layer4TvId == 0) return;
        scanAndFixTabs((ViewGroup) view, layer4AdText, layer4TvId, layer4DotId);
    }

    private void scanAndFixTabs(ViewGroup root, String adText, int tvId, int dotId) {
        int count = root.getChildCount();
        int matchCount = 0;
        for (int i = 0; i < count; i++) {
            if (root.getChildAt(i).findViewById(tvId) != null) {
                matchCount++;
            }
        }

        // 所有子 View 都包含目标 id → 这是 tab 容器
        if (count > 1 && matchCount == count) {
            for (int i = 0; i < count; i++) {
                View child = root.getChildAt(i);
                View tv = child.findViewById(tvId);
                if (tv instanceof android.widget.TextView) {
                    CharSequence text = ((android.widget.TextView) tv).getText();
                    if (text != null && text.toString().equals(adText)) {
                        ((android.widget.TextView) tv).setText("SIM " + (i + 1));
                        if (dotId != 0) {
                            View dot = child.findViewById(dotId);
                            if (dot != null) dot.setVisibility(View.GONE);
                        }
                    }
                }
            }
            return;
        }

        for (int i = 0; i < count; i++) {
            View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                scanAndFixTabs((ViewGroup) child, adText, tvId, dotId);
            }
        }
    }

    // ===== 工具方法 =====

    private static Method findMethodByNames(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                return clazz.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Method findFirstPublicIntMethod(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            int mods = m.getModifiers();
            if (m.getParameterCount() == 0 && m.getReturnType() == int.class
                    && java.lang.reflect.Modifier.isPublic(mods)
                    && !java.lang.reflect.Modifier.isStatic(mods)) {
                return m;
            }
        }
        return null;
    }
}
