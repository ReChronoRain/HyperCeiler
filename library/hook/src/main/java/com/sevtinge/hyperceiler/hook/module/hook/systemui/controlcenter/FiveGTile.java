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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.github.kyuubiran.ezxhelper.EzXHelper;
import com.sevtinge.hyperceiler.hook.R;
import com.sevtinge.hyperceiler.hook.module.base.tool.ResourcesTool;
import com.sevtinge.hyperceiler.hook.utils.TileUtils;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.TelephonyManager;

import java.lang.reflect.Array;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public class FiveGTile extends TileUtils {
    String mNfcTileClsName = "com.android.systemui.qs.tiles.MiuiNfcTile";

    boolean Style = mPrefsMap.getStringAsInt("system_control_center_5g_new_tile", 0) == 1;

    @SuppressLint("MissingSuperCall")
    @Override
    public void init() {
        if (mPrefsMap.getStringAsInt("system_control_center_5g_new_tile", 0) == 3) {
            final boolean[] isInitFinished = {false};
            findAndHookMethod(
                    "com.android.systemui.statusbar.policy.MiuiFiveGServiceClient", "update5GIcon",
                    new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) {
                            if (TelephonyManager.getDefault().isFiveGCapable() && !isInitFinished[0]) {
                                initStyle3();
                                isInitFinished[0] = true;
                            }
                        }
                    }
            );

            return;
        }

        super.init();
    }

    private void initStyle3() {
        Resources modRes = ResourcesTool.loadModuleRes(EzXHelper.getAppContext());
        String fiveG = modRes.getString(customRes());

        String detailContentClzName = "com.android.systemui.qs.QSDetailContent";
        String itemClzName = detailContentClzName + "$Item";
        findAndHookMethod(
                "com.android.systemui.qs.tiles.MiuiCellularTile$CellularDetailAdapter",
                "createDetailView",
                Context.class, View.class, ViewGroup.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        if (EzXHelper.getAppContext().getResources().getConfiguration()
                                .orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            return;
                        }

                        View content = (View) param.getResult();

                        content.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                            @Override
                            public void onViewAttachedToWindow(@NonNull View v) {
                                View view = content;
                                while ((view = (View) view.getParent()) != null) {
                                    int id = view.getId();
                                    if (id == View.NO_ID) {
                                        break;
                                    }

                                    String idName = view.getResources().getResourceName(id);
                                    if (idName.endsWith("detail_container")) {
                                        int maxHeight = (int) XposedHelpers.callMethod(view, "getMaxHeight");
                                        XposedHelpers.callMethod(view, "setMaxHeight", maxHeight +
                                                DisplayUtils.dp2px(45f)
                                        );
                                        break;
                                    }
                                }

                                v.removeOnAttachStateChangeListener(this);
                            }

                            @Override
                            public void onViewDetachedFromWindow(@NonNull View v) {
                            }
                        });
                    }
                }
        );

        findAndHookMethod(
                "com.android.systemui.qs.tiles.MiuiCellularTile$CellularDetailAdapter",
                "onDetailItemClick", findClass(itemClzName),
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        Object item = param.args[0];
                        int type = (int) XposedHelpers.callMethod(item, "getType");

                        if (type == 2333) {
                            Object title = XposedHelpers.getObjectField(item, "title");
                            // boolean isChecked = XposedHelpers.getBooleanField(item, "isChecked");
                            if (fiveG.equals(title)) {
                                TelephonyManager manager = TelephonyManager.getDefault();
                                boolean userFiveGEnabled = manager.isUserFiveGEnabled();
                                manager.setUserFiveGEnabled(!userFiveGEnabled);
                                if (userFiveGEnabled) {
                                    logD(TAG, lpparam.packageName, "from 5G to none 5G");
                                } else {
                                    logD(TAG, lpparam.packageName, "from none 5G to 5G");
                                }
                                param.setResult(null);
                            }
                        }
                    }
                }
        );

        findAndHookMethod(
                detailContentClzName, "setItems",
                findClass(itemClzName + "[]"),
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        ViewGroup content = (ViewGroup) param.thisObject;

                        Object suffix = XposedHelpers.getObjectField(content, "suffix");
                        if (!"Cellular".equals(suffix)) {
                            return;
                        }

                        Object[] rawItems = (Object[]) param.args[0];
                        if (rawItems.length == 0) {
                            return;
                        }

                        Object[] finalItems = (Object[]) Array.newInstance(findClass(itemClzName), rawItems.length + 2);

                        for (int i = rawItems.length - 1; i >= 0; i--) {
                            Object item = rawItems[i];
                            if (item.getClass().getName().endsWith("SelectableItem")) {
                                System.arraycopy(rawItems, 0, finalItems, 0, i + 1);

                                finalItems[i + 1] = findClass(detailContentClzName + "$TextDividerItem")
                                        .getConstructor(CharSequence.class)
                                        .newInstance(modRes.getString(R.string.system_framework_phone_network_title));
                                try {
                                    finalItems[i + 2] = findClass(detailContentClzName + "$ToggleItem")
                                            .getConstructor(CharSequence.class, CharSequence.class, boolean.class)
                                            .newInstance(
                                                    fiveG,
                                                    null,
                                                    TelephonyManager.getDefault().isUserFiveGEnabled()
                                            );
                                } catch (Exception e) {
                                    finalItems[i + 2] = findClass(detailContentClzName + "$ToggleItem")
                                            .getConstructor(CharSequence.class, CharSequence.class, boolean.class, Object.class)
                                            .newInstance(
                                                    fiveG,
                                                    null,
                                                    TelephonyManager.getDefault().isUserFiveGEnabled(),
                                                    null
                                            );
                                }

                                if (i + 3 != finalItems.length) {
                                    System.arraycopy(rawItems, i + 1, finalItems, i + 3, rawItems.length - i - 1);
                                }
                                break;
                            }
                        }
                        param.args[0] = finalItems;
                    }
                }
        );
    }

    @Override
    public Class<?> customClass() {
        return findClassIfExists(mNfcTileClsName);
    }

    @Override
    public String setTileProvider() {
        return isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "nfcTileProvider" : "mNfcTileProvider";
    }

    @Override
    public String customName() {
        return "custom_5G";
    }

    @Override
    public int customRes() {
        if (mPrefsMap.getStringAsInt("system_control_center_5g_new_tile", 0) == 3) {
            return R.string.dashboard_5g;
        } else {
            return R.string.tiles_5g;
        }
    }

    @Override
    public void tileCheck(MethodHookParam param, String tileName) {
        // 获取设置是否支持5G
        param.setResult(TelephonyManager.getDefault().isFiveGCapable());
    }

    @Override
    public void tileLongClickIntent(MethodHookParam param, String tileName) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.settings.MiuiFiveGNetworkSetting"));
        // 原活动是 com.android.phone.settings.PreferredNetworkTypeListPreference
        param.setResult(intent);
    }

    @Override
    public void tileClick(MethodHookParam param, String tileName) {
        TelephonyManager manager = TelephonyManager.getDefault();
        // 切换5G状态
        manager.setUserFiveGEnabled(!manager.isUserFiveGEnabled());

        logD(TAG, lpparam.packageName, "5G" + manager.isUserFiveGEnabled());
        // 更新磁贴状态
        XposedHelpers.callMethod(param.thisObject, "refreshState");
    }

    @Override
    public void tileListening(MethodHookParam param, String tileName) {
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        boolean mListening = (boolean) param.args[0];
        if (mListening) {
            ContentObserver contentObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
                @Override
                public void onChange(boolean z) {
                    XposedHelpers.callMethod(param.thisObject, "refreshState");
                }
            };
            mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("fiveg_user_enable"), false, contentObserver);
            mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("dual_nr_enabled"), false, contentObserver);
            XposedHelpers.setAdditionalInstanceField(param.thisObject, "tileListener", contentObserver);
        } else {
            ContentObserver contentObserver = (ContentObserver) XposedHelpers.getAdditionalInstanceField(param.thisObject, "tileListener");
            mContext.getContentResolver().unregisterContentObserver(contentObserver);
        }
    }

    @Override
    public ArrayMap<String, Integer> tileUpdateState(MethodHookParam param, Class<?> mResourceIcon, String tileName) {
        boolean isEnable;
        TelephonyManager manager = TelephonyManager.getDefault();
        isEnable = manager.isUserFiveGEnabled();
        ArrayMap<String, Integer> tileResMap = new ArrayMap<>();
        tileResMap.put("custom_5G_Enable", isEnable ? 1 : 0);
        tileResMap.put("custom_5G_ON",
                Style ?
                        R.drawable.ic_control_center_5g_toggle_on : R.drawable.ic_control_center_5g_toggle_v2_on);
        tileResMap.put("custom_5G_OFF",
                Style ?
                        R.drawable.ic_control_center_5g_toggle_off : R.drawable.ic_control_center_5g_toggle_v2_off);
        return tileResMap;
    }
}
