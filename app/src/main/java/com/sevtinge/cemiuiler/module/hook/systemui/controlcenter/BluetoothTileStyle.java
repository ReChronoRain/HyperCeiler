package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter;

import static com.sevtinge.cemiuiler.module.base.BaseXposedInit.mPrefsMap;
import static com.sevtinge.cemiuiler.utils.devicesdk.AppUtilsKt.dp2px2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.Helpers.MethodHook;
import com.sevtinge.cemiuiler.utils.ResourcesHook;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class BluetoothTileStyle {
    public static void initHideDeviceControlEntry(ClassLoader pluginLoader) {
        final int[] tileResIds = {0};
        Helpers.findAndHookMethod("miui.systemui.dagger.PluginComponentFactory", pluginLoader, "create", Context.class, Context.class, new MethodHook() {
            @SuppressLint("DiscouragedApi")
            protected void after(XC_MethodHook.MethodHookParam param) {
                Context pluginContext = (Context) param.args[1];
                tileResIds[0] = pluginContext.getResources().getIdentifier("big_tile", "layout", "miui.systemui.plugin");
            }
        });
        Helpers.hookAllMethods("miui.systemui.controlcenter.dagger.ControlCenterViewModule", pluginLoader, "createBigTileGroup", new MethodHook() {
            @Override
            protected void after(XC_MethodHook.MethodHookParam param) {
                ViewGroup mView = (ViewGroup) param.getResult();
                LayoutInflater li = (LayoutInflater) XposedHelpers.callMethod(param.args[0], "injectable", param.args[1]);
                View btTileView = li.inflate(tileResIds[0], null);
                mView.addView(btTileView, 2);
                btTileView.setTag("big_tile_bt");
            }
        });
        int styleId = mPrefsMap.getStringAsInt("system_ui_control_center_cc_bluetooth_tile_style", 1);
        MethodHook updateStyleHook = new MethodHook() {
            boolean inited = false;
            @Override
            @SuppressLint("DiscouragedApi")
            protected void after(XC_MethodHook.MethodHookParam param) throws Throwable {
                ViewGroup mView = (ViewGroup) XposedHelpers.callMethod(param.thisObject, "getView");
                View bigTileB = (View) XposedHelpers.getObjectField(param.thisObject, "bigTileB");
                if (!inited) {
                    inited = true;
                    Object factory = XposedHelpers.getObjectField(param.thisObject, "tileViewFactory");
                    View btTileView = mView.findViewWithTag("big_tile_bt");
                    int btTileId = ResourcesHook.getFakeResId("bt_big_tile");
                    btTileView.setId(btTileId);
                    Object btController = XposedHelpers.callMethod(factory, "create", btTileView, "bt");
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "btTileView", btTileView);
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "btController", btController);

                    Class<?> mConstraintSetClass = pluginLoader.loadClass("androidx.constraintlayout.widget.ConstraintSet");
                    Object constraintSet = XposedHelpers.newInstance(mConstraintSetClass);
                    XposedHelpers.callMethod(constraintSet, "clone", mView);
                    View bigTileA = (View) XposedHelpers.getObjectField(param.thisObject, "bigTileA");
                    if (styleId == 2) {
                        XposedHelpers.callMethod(constraintSet, "connect", bigTileB.getId(), 7, btTileId, 6);
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 6, bigTileB.getId(), 7);
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 7, bigTileA.getId(), 7);
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 3, bigTileB.getId(), 3);
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 4, 0, 4);
                        XposedHelpers.callMethod(constraintSet, "setMargin", btTileId, 6, (int) dp2px2(10));
                        int labelResId = mView.getResources().getIdentifier("label_container", "id", "miui.systemui.plugin");
                        bigTileB.findViewById(labelResId).setVisibility(View.GONE);
                        btTileView.findViewById(labelResId).setVisibility(View.GONE);
                        int iconResId = mView.getResources().getIdentifier("status_icon", "id", "miui.systemui.plugin");
                        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) bigTileB.findViewById(iconResId).getLayoutParams();
                        layoutParams.leftMargin = (int) dp2px2(3);
                        layoutParams = (LinearLayout.LayoutParams) btTileView.findViewById(iconResId).getLayoutParams();
                        layoutParams.leftMargin = (int) dp2px2(3);
                    }
                    else {
                        XposedHelpers.callMethod(constraintSet, "connect", bigTileB.getId(), 4, btTileId, 3);
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 6, bigTileA.getId(), 6);
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 7, bigTileA.getId(), 7);
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 3, bigTileB.getId(), 4);
                        XposedHelpers.callMethod(constraintSet, "connect", btTileId, 4, 0, 4);
                    }
                    XposedHelpers.callMethod(constraintSet, "constrainWidth", btTileId, 0);
                    XposedHelpers.callMethod(constraintSet, "constrainHeight", btTileId, 0);
                    XposedHelpers.callMethod(constraintSet, "applyTo", mView);
                }
                if (styleId == 3) {
                    ViewGroup.LayoutParams layoutParams = bigTileB.getLayoutParams();
                    int verticalMargin = (int) dp2px2(4);
                    ((ViewGroup.MarginLayoutParams) layoutParams).topMargin = verticalMargin;
                    ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin = verticalMargin;
                }
            }
        };
        Helpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.BigTileGroupController", pluginLoader, "updateResources", updateStyleHook);
        Helpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.BigTileGroupController", pluginLoader, "setListening", boolean.class, new MethodHook() {
            @Override
            protected void after(XC_MethodHook.MethodHookParam param) {
                Object btController = XposedHelpers.getAdditionalInstanceField(param.thisObject, "btController");
                if (btController != null) {
                    XposedHelpers.callMethod(btController, "setListening", param.args[0]);
                }
            }
        });
        Helpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.BigTileGroupController", pluginLoader, "getRowViews", int.class, new MethodHook() {
            @Override
            protected void after(XC_MethodHook.MethodHookParam param) {
                int row = (int) param.args[0];
                Object btTileView;
                if (row == 1 && (btTileView = XposedHelpers.getAdditionalInstanceField(param.thisObject, "btTileView")) != null) {
                    ((ArrayList<Object>)param.getResult()).add(btTileView);
                }
            }
        });
        Helpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.BigTileGroupController", pluginLoader, "getChildControllers", new MethodHook() {
            @Override
            protected void after(XC_MethodHook.MethodHookParam param) {
                Object btController = XposedHelpers.getAdditionalInstanceField(param.thisObject, "btController");
                if (btController != null) {
                    ((ArrayList<Object>)param.getResult()).add(btController);
                }
            }
        });
    }
}
