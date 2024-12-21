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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.various.clipboard;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logD;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logI;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logW;

import android.content.ClipData;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.tool.ParamTool;
import com.sevtinge.hyperceiler.utils.ContentModel;
import com.sevtinge.hyperceiler.utils.FileHelper;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 解除常用语剪贴板时间限制，条数限制和字数限制。
 * from <a href="https://github.com/HChenX/ClipboardList">ClipboardList</a>
 *
 * @author 焕晨HChen
 */
public class NewClipboardList extends BaseHC implements LoadInputMethodDex.OnInputMethodDexLoad {
    private Gson mGson;
    private static String mDataPath;
    private boolean isNewMode = false;

    private boolean isHooked;


    @Override
    public void load(ClassLoader classLoader) {
        mGson = new GsonBuilder().setPrettyPrinting().create();
        mDataPath = lpparam.appInfo.dataDir + "/files/clipboard_data.dat";
        logI(TAG, "class loader: " + classLoader);

        FileHelper.TAG = TAG;
        if (!FileHelper.exists(mDataPath)) {
            logE(TAG, "file create failed!");
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
        chain("com.miui.inputmethod.InputMethodUtil", classLoader,
                method("getClipboardData", Context.class) // 读取剪贴板数据
                        .hook(new IHook() {
                            @Override
                            public void before() {
                                getClipboardData(this);
                            }
                        })

                        .method("addClipboard", String.class, String.class, int.class, Context.class) // 添加剪贴板条目
                        .hook(new IHook() {
                            @Override
                            public void before() {
                                addClipboard((String) getArgs(1), (int) getArgs(2), (Context) getArgs(3));
                                returnNull();
                            }
                        })

                        .method("setClipboardModelList", Context.class, ArrayList.class) // 保存剪贴板数据
                        .hook(new IHook() {
                            @Override
                            public void before() {
                                ArrayList<?> dataList = (ArrayList<?>) getArgs(1);
                                FileHelper.write(mDataPath, mGson.toJson(dataList));
                                if (!dataList.isEmpty()) returnNull();
                            }
                        })
        );
    }

    private String mText = null;
    private int mMax = -1;

    private void newMethod(ClassLoader classLoader) {
        isNewMode = true;
        setStaticField("com.miui.inputmethod.MiuiClipboardManager", classLoader, "MAX_CLIP_DATA_ITEM_SIZE", Integer.MAX_VALUE);
        chain("com.miui.inputmethod.MiuiClipboardManager", classLoader,
                anyMethod("addClipDataToPhrase") // 添加剪贴板条目
                        .hook(new IHook() {
                            @Override
                            public void before() {
                                Object clipboardContentModel = getArgs(2);
                                String content = ContentModel.getContent(clipboardContentModel);
                                int type = ContentModel.getType(clipboardContentModel);
                                // long time = ContentModel.getTime(clipboardContentModel);
                                addClipboard(content, type, (Context) getArgs(0));
                                returnNull();
                            }
                        })

                        .method("getClipboardData", Context.class) // 获取剪贴板数据
                        .hook(new IHook() {
                            @Override
                            public void before() {
                                getClipboardData(this);
                            }
                        })

                        .anyMethod("setClipboardModelList") // 保存剪贴板数据
                        .hook(new IHook() {
                            @Override
                            public void before() {
                                ArrayList<?> dataList = (ArrayList<?>) getArgs(1);
                                FileHelper.write(mDataPath, mGson.toJson(dataList));
                                if (!dataList.isEmpty()) returnNull();
                            }
                        })

                        .anyMethod("commitClipDataAndTrack") // 修复小米的 BUG
                        .hook(new IHook() {
                            @Override
                            public void before() {
                                int type = (int) getArgs(3);
                                if (type == 3 || type == 2) {
                                    setArgs(3, 10);
                                }
                            }
                        })

                        .method("processSingleItemOfClipData", ClipData.class, String.class) // 解除 5000 字限制
                        .hook(new IHook() {
                            @Override
                            public void before() {
                                ClipData clipData = (ClipData) getArgs(0);
                                ClipData.Item item = clipData.getItemAt(0);
                                if (item.getText() != null && !TextUtils.isEmpty(item.getText().toString())) {
                                    mText = item.getText().toString();
                                }
                            }
                        })

                        .method("buildClipDataItemModelBasedTextData", String.class) // 解除 5000 字限制
                        .hook(new IHook() {
                            @Override
                            public void before() {
                                if (mMax == -1)
                                    mMax = (int) getStaticField("com.miui.inputmethod.MiuiClipboardManager", classLoader,
                                            "MAX_CLIP_CONTENT_SIZE");
                                if (mMax == -1) mMax = 5000;
                                String string = (String) getArgs(0);
                                if (string != null && !string.isEmpty()) {
                                    if (string.length() == mMax) {
                                        if (mText != null && !mText.isEmpty()) setArgs(0, mText);
                                    }
                                }
                                mText = null;
                            }
                        })
        );

    }

    private void getClipboardData(ParamTool param) {
        ArrayList<ContentModel> readData = toContentModelList(FileHelper.read(mDataPath));
        if (readData.isEmpty()) {
            String data = getData((Context) param.getArgs(0));
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
            logW(TAG, "can't read any data!");
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
