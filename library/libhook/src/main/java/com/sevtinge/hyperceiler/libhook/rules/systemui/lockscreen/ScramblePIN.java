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
package com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.IdentityHashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class ScramblePIN extends BaseHook {

    Class<?> mKeyguardPINView;

    @Override
    public void init() {
        if (isMoreHyperOSVersion(4f)) {
            hookOS4();
            return;
        }
        if (isMoreAndroidVersion(36)) {
            // thanks xzakota
            mKeyguardPINView = findClassIfExists("com.android.keyguard.widget.MiuiKeyguardPINView");
        } else {
            mKeyguardPINView = findClassIfExists("com.android.keyguard.KeyguardPINView");
        }

        findAndHookMethod(mKeyguardPINView, "onFinishInflate", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                View[][] mViews = (View[][]) getObjectField(param.getThisObject(), "mViews");
                ArrayList<View> mRandomViews = collectRandomViews(mViews);

                Collections.shuffle(mRandomViews);
                View pinview = (View) param.getThisObject();
                ViewGroup[] rows = getRowContainers(pinview);
                redistributeViews(mViews, mRandomViews, rows);

                setObjectField(param.getThisObject(), "mViews", mViews);
            }
        });
    }

    private void hookOS4() {
        try {
            Class<?> pin = findClassIfExists("com.android.keyguard.KeyguardPINView");
            Class<?> key = findClassIfExists("com.android.keyguard.NumPadKey");
            if (pin == null || key == null || !View.class.isAssignableFrom(pin)
                || !View.class.isAssignableFrom(key)) throw new IllegalStateException("Missing OS4 PIN view");
            Field views = instanceField(pin, "mViews", View[][].class);
            Field numbers = instanceField(pin, "mNumViews", View[][].class);
            Field digit = instanceField(key, "mDigit", int.class);
            Method inflate = pin.getDeclaredMethod("onFinishInflate");
            if (inflate.getReturnType() != void.class || Modifier.isStatic(inflate.getModifiers())) {
                throw new IllegalStateException("Unknown OS4 PIN inflate method");
            }
            inflate.setAccessible(true);
            if (hookMethod(inflate, new IMethodHook() {
                @Override public void after(HookParam param) {
                    if (param.getHasThrowable() || param.getThisObject().getClass() != pin) return;
                    try {
                        shuffleOS4((View) param.getThisObject(), key, digit,
                            (View[][]) views.get(param.getThisObject()),
                            (View[][]) numbers.get(param.getThisObject()));
                    } catch (ReflectiveOperationException | RuntimeException error) {
                        debugCallbackError("OS4 PIN layout", error);
                    }
                }
            }) == null) throw new IllegalStateException("OS4 PIN hook unavailable");
        } catch (ReflectiveOperationException | RuntimeException error) {
            throw new IllegalStateException("OS4 PIN view shape or registration unavailable", error);
        }
    }

    private static Field instanceField(Class<?> owner, String name, Class<?> type) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        if (field.getType() != type || Modifier.isStatic(field.getModifiers())) {
            throw new IllegalStateException("Unknown OS4 PIN field: " + name);
        }
        field.setAccessible(true);
        return field;
    }

    private void shuffleOS4(View root, Class<?> key, Field digit, View[][] views, View[][] numbers)
        throws IllegalAccessException {
        // This exact phone layout has two animation matrices sharing the first
        // three digit rows. Tiny/fold subclasses and unknown shapes stay original.
        if (views == null || numbers == null || views.length != 6 || numbers.length != 4
            || views[4] == null || views[4].length != 3
            || numbers[3] == null || numbers[3].length != 3
            || views[4][1] != numbers[3][1]) return;
        ViewGroup[] rows = new ViewGroup[4];
        ArrayList<View> original = new ArrayList<>(10);
        IdentityHashMap<View, Boolean> identities = new IdentityHashMap<>();
        boolean[] digits = new boolean[10];
        ViewGroup.LayoutParams[] positions = new ViewGroup.LayoutParams[10];
        for (int row = 0; row < 4; row++) {
            int id = root.getResources().getIdentifier("row" + (row + 1), "id", "com.android.systemui");
            View candidate = id == 0 ? null : root.findViewById(id);
            if (!(candidate instanceof ViewGroup group) || group.getChildCount() != 3) return;
            for (int earlier = 0; earlier < row; earlier++) if (rows[earlier] == group) return;
            rows[row] = group;
            if (row < 3 && (views[row + 1] == null || views[row + 1].length != 3
                || numbers[row] != views[row + 1])) return;
            for (int column = row == 3 ? 1 : 0; column < (row == 3 ? 2 : 3); column++) {
                View view = group.getChildAt(column);
                if (view == null || view.getClass() != key || view != views[row + 1][column]
                    || view.getParent() != group || identities.put(view, Boolean.TRUE) != null) return;
                int value = digit.getInt(view);
                if (value < 0 || value > 9 || digits[value]) return;
                digits[value] = true;
                positions[original.size()] = view.getLayoutParams();
                if (positions[original.size()] == null) return;
                original.add(view);
            }
        }
        // Preserve the bottom spacer/confirm control. Never move input/delete,
        // emergency, verification listeners, digit values, labels, or view IDs.
        if (key.isInstance(rows[3].getChildAt(0)) || key.isInstance(rows[3].getChildAt(2))) return;
        ArrayList<View> shuffled = new ArrayList<>(original);
        Collections.shuffle(shuffled);
        try {
            moveDigits(rows, original, shuffled, positions);
        } catch (RuntimeException error) {
            try { moveDigits(rows, original, original, positions); }
            catch (RuntimeException rollback) { error.addSuppressed(rollback); }
            throw error;
        }
        int index = 0;
        for (int row = 1; row <= 3; row++) {
            for (int column = 0; column < 3; column++) views[row][column] = shuffled.get(index++);
        }
        views[4][1] = shuffled.get(9);
        numbers[3][1] = shuffled.get(9);
    }

    private static void moveDigits(ViewGroup[] rows, List<View> all, List<View> order,
                                   ViewGroup.LayoutParams[] positions) {
        for (View view : all) {
            if (view.getParent() instanceof ViewGroup parent) parent.removeView(view);
        }
        for (int index = 0; index < 10; index++) {
            rows[index < 9 ? index / 3 : 3].addView(order.get(index), index < 9 ? index % 3 : 1, positions[index]);
        }
    }

    /**
     * 收集需要随机排列的视图
     */
    private ArrayList<View> collectRandomViews(View[][] mViews) {
        ArrayList<View> views = new ArrayList<>(10);
        for (int row = 1; row <= 3; row++) {
            for (int col = 0; col <= 2; col++) {
                if (mViews[row][col] != null) {
                    views.add(mViews[row][col]);
                }
            }
        }
        views.add(mViews[4][1]);
        return views;
    }

    /**
     * 获取行容器
     */
    private ViewGroup[] getRowContainers(View pinview) {
        Resources res = pinview.getResources();
        String pkg = "com.android.systemui";

        ViewGroup[] rows = new ViewGroup[5];
        for (int i = 1; i <= 4; i++) {
            rows[i] = pinview.findViewById(res.getIdentifier("row" + i, "id", pkg));
        }
        return rows;
    }

    /**
     * 清空并重新分配视图
     */
    private void redistributeViews(View[][] mViews, List<View> randomViews, ViewGroup[] rows) {
        if (randomViews.size() < 10) return;
        // 清空第1-3行
        for (int i = 1; i <= 3; i++) {
            rows[i].removeAllViews();
        }
        rows[4].removeViewAt(1);

        // 重新分配
        int idx = 0;
        for (int row = 1; row <= 3; row++) {
            mViews[row] = new View[3];
            for (int col = 0; col < 3; col++) {
                mViews[row][col] = randomViews.get(idx);
                rows[row].addView(randomViews.get(idx++));
            }
        }

        mViews[4][1] = randomViews.get(idx);
        rows[4].addView(randomViews.get(idx), 1);
    }
}
