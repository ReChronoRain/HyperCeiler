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
package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.ArrayMap;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.CallSuper;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.tool.ResourcesTool;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public abstract class TileUtils extends BaseHook {
    private final String mQSFactoryClsName = "com.android.systemui.qs.tileimpl.MiuiQSFactory" ;
    private final boolean[] isListened = {false};
    private final String[] mTileProvider = new String[4];
    private Class<?> mResourceIcon;
    private Class<?> mQSFactory;

    /* 固定语法，必须调用。
     * 调用方法：
     * @Override
     * public void init() {
     *     super.init();
     * }
     * */
    @Override
    @CallSuper
    public void init() {
        mQSFactory = findClassIfExists(mQSFactoryClsName);
        if (mQSFactory == null) {
            logE(TAG, "mQSFactory can't is null");
            return;
        }
        Class<?> myTile = customClass();
        mResourceIcon = findClass("com.android.systemui.qs.tileimpl.QSTileImpl$ResourceIcon");
        SystemUiHook();
        customTileProvider();
        showStateMessage(myTile);
        if (isMoreAndroidVersion(34)) {
            tileAllName14(mQSFactory);
        } else {
            tileAllName(mQSFactory);
        }
        try {
            myTile.getDeclaredMethod("isAvailable");
            findAndHookMethod(myTile, "isAvailable", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                    if (tileName != null) {
                        if (tileName.equals(customName())) {
                            tileCheck(param, tileName);
                        }
                    } else {
                        if (needOverride())
                            tileCheck(param, tileName);
                    }
                }
            });
        } catch (NoSuchMethodException e) {
            logE(TAG, "com.android.systemui", "Don't Have isAvailable: " + e);
        }
        tileName(myTile); // 不需要覆写
        try {
            myTile.getDeclaredMethod("handleSetListening", boolean.class);
            findAndHookMethod(myTile, "handleSetListening", boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                    if (tileName != null) {
                        if (tileName.equals(customName())) {
                            try {
                                tileListening(param, tileName);
                                param.setResult(null);
                            } catch (Throwable e) {
                                logE(TAG, "com.android.systemui", "handleSetListening have Throwable: " + e);
                                param.setResult(null);
                            }
                        }
                    } else {
                        if (needOverride())
                            tileListening(param, tileName);
                    }
                }
            });
        } catch (NoSuchMethodException e) {
            logE(TAG, "com.android.systemui", "Don't Have handleSetListening: " + e);
        }
        try {
            myTile.getDeclaredMethod("getLongClickIntent");
            findAndHookMethod(myTile, "getLongClickIntent", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                    if (tileName != null) {
                        if (tileName.equals(customName())) {
                            tileLongClickIntent(param, tileName);
                        }
                    } else {
                        if (needOverride())
                            tileLongClickIntent(param, tileName);
                    }
                }
            });
        } catch (NoSuchMethodException e) {
            logE(TAG, "com.android.systemui", "Don't Have getLongClickIntent: " + e);
        }

        Class<?> expandableClz = findClassIfExists("com.android.systemui.animation.Expandable");

        MethodHook handleLongClickHook = new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                Intent intent = null;
                if (tileName != null) {
                    if (tileName.equals(customName())) {
                        intent = tileHandleLongClick(param, tileName);
                    }
                } else if (needOverride()) {
                    intent = tileHandleLongClick(param, tileName);
                }
                if (intent != null) {
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    Object o = XposedHelpers.callStaticMethod(findClassIfExists("com.android.systemui.controlcenter.utils.ControlCenterUtils"), "getSettingsSplitIntent", context, intent);
                    XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mActivityStarter"), "postStartActivityDismissingKeyguard", o, 0, null);
                    param.setResult(null);
                }
            }
        };
        try {
            if (expandableClz != null) {
                myTile.getDeclaredMethod("handleLongClick", expandableClz);
                findAndHookMethod(myTile, "handleLongClick", expandableClz, handleLongClickHook);
            } else {
                myTile.getDeclaredMethod("handleLongClick", View.class);
                findAndHookMethod(myTile, "handleLongClick", View.class, handleLongClickHook);
            }
        } catch (NoSuchMethodException e) {
            logE(TAG, "com.android.systemui", "Don't Have handleLongClick: " + e);
        }

        MethodHook handleClickHook = new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (tileName != null) {
                    if (tileName.equals(customName())) {
                        try {
                            tileClick(param, tileName);
                            param.setResult(null);
                        } catch (Throwable e) {
                            logE(TAG, "com.android.systemui", "handleClick have Throwable: " + e);
                            param.setResult(null);
                        }
                    }
                } else {
                    if (needOverride())
                        tileClick(param, tileName);
                }
            }

            @Override
            protected void after(MethodHookParam param) {
                if (needAfter()) {
                    String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                    tileClickAfter(param, tileName);
                }
            }
        };
        try {
            if (expandableClz != null) {
                getDeclaredMethod(myTile, "handleClick", expandableClz);
                findAndHookMethod(myTile, "handleClick", expandableClz, handleClickHook);
            } else {
                getDeclaredMethod(myTile, "handleClick", View.class);
                // myTile.getDeclaredMethod("handleClick", View.class);
                findAndHookMethod(myTile, "handleClick", View.class, handleClickHook);
            }
        } catch (NoSuchMethodException e) {
            logE(TAG, "com.android.systemui", "Don't Have handleClick: " + e);
        }

        hookAllMethods(myTile, "handleUpdateState", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (tileName != null) {
                    if (tileName.equals(customName())) {
                        boolean isEnable = false;
                        ArrayMap<String, Integer> tileResMap;
                        tileResMap = tileUpdateState(param, mResourceIcon, tileName);
                        if (tileResMap != null) {
                            int code = tileResMap.get(customName() + "_Enable");
                            if (code == 1) isEnable = true;
                            Object booleanState = param.args[0];
                            XposedHelpers.setObjectField(booleanState, "value", isEnable);
                            // 测试为开关状态控制，2为开，1为关
                            XposedHelpers.setObjectField(booleanState, "state", isEnable ? 2 : 1);
                            String tileLabel = (String) XposedHelpers.callMethod(param.thisObject, "getTileLabel");
                            XposedHelpers.setObjectField(booleanState, "label", tileLabel);
                            XposedHelpers.setObjectField(booleanState, "contentDescription", tileLabel);
                            XposedHelpers.setObjectField(booleanState, "expandedAccessibilityClassName", Switch.class.getName());
                            Object mIcon = XposedHelpers.callStaticMethod(mResourceIcon, "get", isEnable ? tileResMap.get(customName() + "_ON") : tileResMap.get(customName() + "_OFF"));
                            XposedHelpers.setObjectField(booleanState, "icon", mIcon);
                        }
                        param.setResult(null);
                    }
                } else {
                    if (needOverride()) {
                        tileUpdateState(param, mResourceIcon, tileName);
                    }
                }
            }
        });

    }

    /*用于指定磁贴工厂函数
     * 折旧*/
    // public Class<?> customQSFactory() {
    //     return null;
    // }

    /*需要Hook的磁贴Class*/
    public Class<?> customClass() {
        return null;
    }

    /*需要Hook执行的Class方法*/
    private void customTileProvider() {
        mTileProvider[0] = setTileProvider();
        mTileProvider[1] = "createTileInternal";
        mTileProvider[2] = "interceptCreateTile";
        mTileProvider[3] = "createTile";
    }

    /*目标的字段*/
    public String setTileProvider() {
        return "";
    }

    private String[] getCustomTileProvider() {
        return mTileProvider;
    }

    /*请在这里输入你需要的自定义快捷方式名称。*/
    public String customName() {
        return "";
    }

    /*在这里为你的自定义磁贴打上标题
    需要传入资源Id*/
    public int customRes() {
        return -1;
    }

    /* 是否要覆写原有磁贴方法,
     * 当无自定义名称时默认覆写而不走判断名称的逻辑
     * 可以覆写为指定boolean但不建议*/
    public boolean needOverride() {
        return "".equals(customName());
    }

    /*是否需要在after时进行逻辑修改而不是before*/
    public boolean needAfter() {
        return false;
    }

    /*
     * 在第一次Hook时把新的快捷方式加载进快捷方式列表中。
     * */
    private void SystemUiHook() {
        String custom = customName();
        if (!needOverride()) {
            /*if ("".equals(custom)) {
                logE(TAG, "com.android.systemui", "Error custom:" + custom);
                return;
            }*/
            try {
                findClassIfExists("com.android.systemui.SystemUIApplication").getDeclaredMethod("onCreate");
                findAndHookMethod("com.android.systemui.SystemUIApplication", "onCreate", new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        if (!isListened[0]) {
                            isListened[0] = true;
                            // 获取Context
                            Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                            // 获取miui_quick_settings_tiles_stock字符串的值
                            @SuppressLint("DiscouragedApi") int stockTilesResId = mContext.getResources().getIdentifier("miui_quick_settings_tiles_stock", "string", lpparam.packageName);
                            String stockTiles = mContext.getString(stockTilesResId) + "," + custom; // 追加自定义的磁贴
                            // 将拼接后的字符串分别替换下面原有的字符串。
                            mResHook.setObjectReplacement(lpparam.packageName, "string", "miui_quick_settings_tiles_stock", stockTiles);
                            mResHook.setObjectReplacement("miui.systemui.plugin", "string", "miui_quick_settings_tiles_stock", stockTiles);
                            mResHook.setObjectReplacement("miui.systemui.plugin", "string", "quick_settings_tiles_stock", stockTiles);
                        }
                    }
                });
            } catch (NoSuchMethodException e) {
                logE(TAG, "com.android.systemui", "Don't Have onCreate: " + e);
            }
        }
    }

    /*
     * 判断是否是自定义磁贴，如果是则在自定义磁贴前加上Key，用于定位磁贴。
     */
    private void tileAllName(Class<?> QSFactory) {
        if (!needOverride()) {
            try {
                QSFactory.getDeclaredMethod(getCustomTileProvider()[1], String.class);
                tileAllNameMode(QSFactory, 1);
            } catch (NoSuchMethodException e) {
                try {
                    QSFactory.getDeclaredMethod(getCustomTileProvider()[2], String.class);
                    tileAllNameMode(QSFactory, 2);
                } catch (NoSuchMethodException f) {
                    logE(TAG, "com.android.systemui", "Don't Have " + getCustomTileProvider()[2], f);
                }
                logE(TAG, "com.android.systemui", "Don't Have " + getCustomTileProvider()[1], e);
            }
        }
    }

    /*这是上面方法的模组*/
    private void tileAllNameMode(Class<?> QSFactory, int num) {
        findAndHookMethod(QSFactory, getCustomTileProvider()[num], String.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String tileName = (String) param.args[0];
                if (tileName.equals(customName())) {
                    String myTileProvider = getCustomTileProvider()[0];
                    Object provider = XposedHelpers.getObjectField(param.thisObject, myTileProvider);
                    Object tile = XposedHelpers.callMethod(provider, "get");
                    XposedHelpers.setAdditionalInstanceField(tile, "customName", tileName);
                    param.setResult(tile);
                }
            }
        });
    }

    /*安卓14磁贴逻辑被修改，此是解决方法*/
    private void tileAllName14(Class<?> QSFactory) {
        if (!needOverride()) {
            try {
                QSFactory.getDeclaredMethod(getCustomTileProvider()[3], String.class);
                findAndHookMethod(QSFactory, getCustomTileProvider()[3], String.class,
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            String tileName = (String) param.args[0];
                            if (tileName.equals(customName())) {
                                String myTileProvider = getCustomTileProvider()[0];
                                Object provider;
                                Object tile;
                                try {
                                    QSFactory.getDeclaredField(myTileProvider);
                                    provider = XposedHelpers.getObjectField(param.thisObject, myTileProvider);
                                    tile = XposedHelpers.callMethod(provider, "get");
                                    XposedHelpers.setAdditionalInstanceField(tile, "customName", tileName);
                                    if (tile != null) {
                                        Object mHandler = XposedHelpers.getObjectField(tile, "mHandler");
                                        XposedHelpers.callMethod(mHandler, "sendEmptyMessage", 12);
                                        XposedHelpers.callMethod(mHandler, "sendEmptyMessage", 11);
                                        XposedHelpers.callMethod(tile, "setTileSpec", tileName);

                                        param.setResult(tile);
                                    }
                                } catch (NoSuchFieldException e) {
                                    logE(TAG, "provider is null: " + myTileProvider);
                                }
                            }
                        }
                    }
                );
            } catch (NoSuchMethodException e) {
                logE(TAG, "com.android.systemui", "Don't Have " + getCustomTileProvider()[2], e);
            }
        }
    }

    /* 新版系统界面磁贴开启关闭时会显示的文字 */
    private void showStateMessage(Class<?> myTile) {
        try {
            if (!needOverride()) {
                myTile.getDeclaredMethod("handleShowStateMessage");
                findAndHookMethod(myTile, "handleShowStateMessage", new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            try {
                                XposedHelpers.callMethod(param.thisObject, "showStateMessage",
                                    XposedHelpers.callMethod(param.thisObject, "getStateMessage"));
                            } catch (Throwable t) {
                                // try {
                                //     @SuppressLint("PrivateApi") Class<?> QSTileImpl = Class.forName("com.android.systemui.qs.tileimpl.QSTileImpl");
                                //     Method handleShowStateMessage = QSTileImpl.getDeclaredMethod("handleShowStateMessage");
                                //     handleShowStateMessage.invoke(null);
                                // } catch (ReflectiveOperationException e) {
                                //     logE("showStateMessage", " Find class or call method error: " + e);
                                // }
                                String string;
                                Object o = XposedHelpers.getObjectField(param.thisObject, "mState");
                                int i = (int) XposedHelpers.getObjectField(o, "state");
                                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                                if (i != 1) {
                                    if (i != 2) {
                                        string = null;
                                    } else {
                                        string = context.getResources().getString(
                                            R.string.quick_settings_state_change_message_on_my,
                                            XposedHelpers.callMethod(param.thisObject, "getTileLabel"));
                                    }
                                } else {
                                    string = context.getResources().getString(
                                        R.string.quick_settings_state_change_message_off_my,
                                        XposedHelpers.callMethod(param.thisObject, "getTileLabel"));
                                }
                                XposedHelpers.callMethod(param.thisObject, "showStateMessage", string);
                            }
                            // XposedHelpers.callMethod(param.thisObject, "getTileLabel");
                            param.setResult(null);
                        }
                    }
                );
            }
        } catch (NoSuchMethodException e) {
            logE(TAG, "com.android.systemui", "Don't Have handleShowStateMessage: " + e);
        }
    }

    /*为磁贴打上自定义名称*/
    private void tileName(Class<?> myTile) {
        if (!needOverride()) {
            int customValue = customRes();
            String custom = customName();
            if (customValue == -1 || "".equals(custom)) {
                logE(TAG, "com.android.systemui", "Error customValue:" + customValue);
                return;
            }
            try {
                myTile.getDeclaredMethod("getTileLabel");
                findAndHookMethod(myTile, "getTileLabel", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                        if (tileName != null) {
                            if (tileName.equals(custom)) {
                                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                                Resources modRes = ResourcesTool.loadModuleRes(mContext);
                                param.setResult(modRes.getString(customValue));
                            }
                        }
                    }
                });
            } catch (NoSuchMethodException e) {
                logE(TAG, "com.android.systemui", "Don't Have getTileLabel: ", e);
            }
        }
    }

    /*在这里可以为你的自定义磁贴判断系统是否支持*/
    public void tileCheck(MethodHookParam param, String tileName) {
    }

    /*这个方法用于监听系统设置变化
    用于实时刷新开关状态*/
    public void tileListening(MethodHookParam param, String tileName) {
    }

    /*这个方法用于设置长按磁贴的动作*/
    public void tileLongClickIntent(MethodHookParam param, String tileName) {
    }

    /*这是另一个长按动作代码
     * 可能不是很严谨，仅在上面长按动作失效时使用*/
    public Intent tileHandleLongClick(MethodHookParam param, String tileName) {
        return null;
    }

    /*这个方法用于设置单击磁贴的动作*/
    public void tileClick(MethodHookParam param, String tileName) {
    }

    /*在点按开关原逻辑后执行*/
    public void tileClickAfter(MethodHookParam param, String tileName) {
    }

    /*这个方法用于设置更新磁贴状态*/
    public ArrayMap<String, Integer> tileUpdateState(MethodHookParam param, Class<?> mResourceIcon, String tileName) {
        return null;
    }
}
