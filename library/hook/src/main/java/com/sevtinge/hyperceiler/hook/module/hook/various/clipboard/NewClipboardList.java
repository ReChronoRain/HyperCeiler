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

package com.sevtinge.hyperceiler.hook.module.hook.various.clipboard;

import android.content.ClipData;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.core.ParamTool;
import com.hchen.hooktool.hook.IHook;
import com.sevtinge.hyperceiler.hook.utils.ContentModel;
import com.sevtinge.hyperceiler.hook.utils.FileHelper;
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.stream.Collectors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 解除常用语剪贴板时间限制，条数限制和字数限制。
 * from <a href="https://github.com/HChenX/ClipboardList">ClipboardList</a>
 *
 * @author 焕晨HChen
 */
public class NewClipboardList extends HCBase implements LoadInputMethodDex.OnInputMethodDexLoad {
    private Gson mGson;
    private static String mDataPath;
    private boolean isNewMode = false;
    private String content = null;

    private boolean isHooked;


    @Override
    public void load(ClassLoader classLoader) {
        mGson = new GsonBuilder().setPrettyPrinting().create();
        mDataPath = loadPackageParam.appInfo.dataDir + "/files/clipboard_data.dat";
        XposedLogUtils.logI(TAG, "class loader: " + classLoader);

        FileHelper.TAG = TAG;
        if (!FileHelper.exists(mDataPath)) {
            XposedLogUtils.logE(TAG, "file create failed!");
            return;
        }

        ContentModel.classLoader = classLoader;
        if (isNewMethod(classLoader))
            newMethod(classLoader);
        else
            oldMethod(classLoader);
    }

    @Override
    public void init() {
    }

    private boolean isNewMethod(ClassLoader classLoader) {
        return existsClass("com.miui.inputmethod.MiuiClipboardManager", classLoader);
    }

    private void oldMethod(ClassLoader classLoader) {
        buildChain("com.miui.inputmethod.InputMethodUtil", classLoader)
                .findMethod("getClipboardData", Context.class) // 读取剪贴板数据
                .hook(new IHook() {
                    @Override
                    public void before() {
                        getClipboardData(this);
                    }
                })

                .findMethod("addClipboard", String.class, String.class, int.class, Context.class) // 添加剪贴板条目
                .hook(new IHook() {
                    @Override
                    public void before() {
                        addClipboard((String) getArg(1), (int) getArg(2), (Context) getArg(3));
                        returnNull();
                    }
                })

                .findMethod("setClipboardModelList", Context.class, ArrayList.class) // 保存剪贴板数据
                .hook(new IHook() {
                    @Override
                    public void before() {
                        ArrayList<?> dataList = (ArrayList<?>) getArg(1);
                        FileHelper.write(mDataPath, mGson.toJson(dataList));
                        if (!dataList.isEmpty()) returnNull();
                    }
                });
    }

    private String mText = null;
    private int mMax = -1;

    private void newMethod(ClassLoader classLoader) {
        isNewMode = true;
        setStaticField("com.miui.inputmethod.MiuiClipboardManager", classLoader, "MAX_CLIP_DATA_ITEM_SIZE", Integer.MAX_VALUE);
        buildChain("com.miui.inputmethod.MiuiClipboardManager", classLoader)
                .findAllMethod("addClipDataToPhrase") // 添加剪贴板条目
                .hook(new IHook() {
                    @Override
                    public void before() {
                        Object clipboardContentModel = getArg(2);
                        content = ContentModel.getContent(clipboardContentModel);
                        int type = ContentModel.getType(clipboardContentModel);
                        // long time = ContentModel.getTime(clipboardContentModel);
                        addClipboard(content, type, (Context) getArg(0));
                        returnNull();
                    }
                })

                .findMethod("getClipboardData", Context.class) // 获取剪贴板数据
                .hook(new IHook() {
                    @Override
                    public void before() {
                        getClipboardData(this);
                    }
                })

                .findAllMethod("setClipboardModelList") // 保存剪贴板数据
                .hook(new IHook() {
                    @Override
                    public void before() {
                        ArrayList<?> dataList = (ArrayList<?>) getArg(1);
                        FileHelper.write(mDataPath, mGson.toJson(dataList));
                        if (!dataList.isEmpty()) returnNull();
                    }
                })

                .findAllMethod("commitClipDataAndTrack") // 修复小米的 BUG
                .hook(new IHook() {
                    @Override
                    public void before() {
                        int type = (int) getArg(3);
                        if (type == 3 || type == 2) {
                            setArg(3, 10);
                        }
                    }
                })

                .findMethod("processSingleItemOfClipData", ClipData.class, String.class) // 解除 5000 字限制
                .hook(new IHook() {
                    @Override
                    public void before() {
                        ClipData clipData = (ClipData) getArg(0);
                        ClipData.Item item = clipData.getItemAt(0);
                        if (item.getText() != null && !TextUtils.isEmpty(item.getText().toString())) {
                            mText = item.getText().toString();
                        }
                    }
                })

                .findMethod("buildClipDataItemModelBasedTextData", String.class) // 解除 5000 字限制
                .hook(new IHook() {
                    @Override
                    public void before() {
                        if (mMax == -1)
                            mMax = (int) getStaticField("com.miui.inputmethod.MiuiClipboardManager", classLoader,
                                    "MAX_CLIP_CONTENT_SIZE");
                        if (mMax == -1) mMax = 5000;
                        String string = (String) getArg(0);
                        if (string != null && !string.isEmpty()) {
                            if (string.length() == mMax) {
                                if (mText != null && !mText.isEmpty()) setArg(0, mText);
                            }
                        }
                        mText = null;
                    }
                });
        XposedHelpers.findAndHookMethod("com.miui.inputmethod.MiuiClipboardManager", classLoader, "buildClipboardModelDataType", "com.miui.inputmethod.ClipboardContentModel", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String json = (String) XposedHelpers.callMethod(param.args[0], "getContent");
                if (TextUtils.isEmpty(json) || json == null) {
                    param.setResult(null);
                    XposedLogUtils.logW(TAG, loadPackageParam.packageName, "Got null string, skip run. String = " + json);
                }
                XposedHelpers.findAndHookConstructor(JSONArray.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String json = (String) param.args[0];
                        if (TextUtils.isEmpty(json) || json == null) {
                            if (!TextUtils.isEmpty(content) && content != null) {
                                param.args[0] = content;
                                XposedLogUtils.logW(TAG, loadPackageParam.packageName, "Got null string, overwrite param. String = " + param.args[0]);
                            }
                        }
                    }
                });
            }
        });

    }

    private void getClipboardData(ParamTool param) {
        ArrayList<ContentModel> readData = toContentModelList(FileHelper.read(mDataPath));
        if (readData.isEmpty()) {
            String data = getData((Context) param.getArg(0));
            if (data.isEmpty()) param.setResult(new ArrayList<>());
            ArrayList<ContentModel> contentModels = toContentModelList(data);
            FileHelper.write(mDataPath, mGson.toJson(contentModels));
            param.setResult(toClipboardList(contentModels));
            return;
        }
        ArrayList<?> clipboardList = toClipboardList(readData);
        param.setResult(clipboardList);
    }

    private void addClipboard(String add, int type, Context context) {
        if (FileHelper.empty(mDataPath)) {
            // 数据库为空时写入数据
            String string = getData(context);
            ArrayList<ContentModel> dataList;
            dataList = toContentModelList(string);
            dataList.add(0, new ContentModel(add, type, System.currentTimeMillis()));
            FileHelper.write(mDataPath, mGson.toJson(dataList));
            return;
        }
        ArrayList<ContentModel> readData = toContentModelList(FileHelper.read(mDataPath));
        if (readData.isEmpty()) {
            XposedLogUtils.logW(TAG, "can't read any data!");
        } else {
            if (readData.stream().anyMatch(contentModel -> contentModel.content.equals(add))) {
                readData.removeIf(contentModel -> contentModel.content.equals(add));
            }
            readData.add(0, new ContentModel(add, type, System.currentTimeMillis()));
            FileHelper.write(mDataPath, mGson.toJson(readData));
        }
    }

    private String getData(Context context) {
        Bundle call = context.getContentResolver().call(Uri.parse((!isNewMode) ? "content://com.miui.input.provider"
                        : "content://com.miui.phrase.input.provider"),
                "getClipboardList", null, new Bundle());
        return call != null ? call.getString("savedClipboard") : "";
    }

    private ArrayList<ContentModel> toContentModelList(String str) {
        if (str.isEmpty()) return new ArrayList<>();
        ArrayList<ContentModel> contentModels = mGson.fromJson(str,
                new TypeToken<ArrayList<ContentModel>>() {
                }.getType());
        if (contentModels == null) return new ArrayList<>();
        return contentModels;
    }

    private ArrayList<?> toClipboardList(ArrayList<ContentModel> dataList) {
        return dataList.stream().map(list ->
                ContentModel.createContentModel(list.content, list.type, list.time)
        ).collect(Collectors.toCollection(ArrayList::new));
    }

}
