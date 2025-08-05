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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.hook.IHook;
import com.sevtinge.hyperceiler.hook.utils.input.ContentModel;
import com.sevtinge.hyperceiler.hook.utils.input.FileHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 解除常用语剪贴板时间限制，条数限制和字数限制。
 * from <a href="https://github.com/HChenX/ClipboardList">ClipboardList</a>
 *
 * @author 焕晨HChen
 */
public class ClipboardLimit extends HCBase {
    private static final String TAG = "ClipboardList";
    private static ClassLoader classLoader;
    private static Gson mGson;
    private static String mDataPath;
    private static boolean isNewVersion = false;
    private static boolean isRegistered = false;
    private static String mText = null;
    private static Integer mMaxSize = -1;

    @Override
    protected void init() {
    }

    @Override
    protected void initApplicationAfter(@NonNull Context context) {
        if (isRegistered) return;

        context.getContentResolver().registerContentObserver(
            Uri.parse("content://com.miui.phrase.input.provider/across"),
            false,
            new ContentObserver(new Handler(context.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    if (selfChange || !isNewVersion) return;

                    Bundle call = context.getContentResolver().call(
                        Uri.parse("content://com.miui.phrase.input.provider/get/across"),
                        "method_get_the_across_devices_data",
                        null, new Bundle()
                    );
                    if (call == null) return;

                    String content = call.getString("list");
                    if (content == null || content.isEmpty()) return;

                    addClipboardItem(context, ContentModel.cloneContentModel(content));
                }
            }
        );

        isRegistered = true;
    }

    public static void unlock(@NonNull ClassLoader classLoader) {
        mGson = new GsonBuilder().setPrettyPrinting().create();
        mDataPath = loadPackageParam.appInfo.dataDir + "/files/clipboard_data.dat";

        if (!FileHelper.exists(mDataPath)) {
            logE(TAG, "Failed create data file, can't unlock clipboard limit!!");
            return;
        }

        ClipboardLimit.classLoader = classLoader;
        if (isNewVersion()) newVersionHook();
        else oldVersionHook();
    }

    private static boolean isNewVersion() {
        isNewVersion = existsClass("com.miui.inputmethod.MiuiClipboardManager", classLoader);
        return isNewVersion;
    }

    private static void newVersionHook() {
        setStaticField("com.miui.inputmethod.MiuiClipboardManager", classLoader, "MAX_CLIP_DATA_ITEM_SIZE", Integer.MAX_VALUE);

        buildChain("com.miui.inputmethod.MiuiClipboardManager", classLoader)
            .findAllMethod("addClipDataToPhrase")
            .hook(new IHook() {
                @Override
                public void before() {
                    Object clipboardContentModel = getArg(2);
                    ArrayList<ContentModel> cloneContentModel = ContentModel.cloneContentModel(clipboardContentModel);
                    addClipboardItem((Context) getArg(0), cloneContentModel);

                    // returnNull(); // 跨设备会使用这个方法，不可以直接拦截执行
                    logD(TAG, "Add clipboard item: " + cloneContentModel);
                }
            }) // 添加剪贴板条目

            .findMethod("getClipboardData", Context.class)
            .hook(new IHook() {
                @Override
                public void before() {
                    setResult(
                        loadClipboardData((Context) getArg(0))
                    );
                }
            }) // 获取剪贴板数据

            .findAllMethod("setClipboardModelList")
            .hook(new IHook() {
                @Override
                public void before() {
                    List<?> dataList = (List<?>) getArg(1);
                    FileHelper.write(mDataPath, mGson.toJson(dataList));
                    if (!dataList.isEmpty()) returnNull(); // 非空代表是模块储存的数据，为空代表用户可能清除了数据

                    logD(TAG, "Save clipboard data!!");
                }
            }) // 保存剪贴板数据

            .findMethodIfExist("clearClipBoardData", Context.class)
            .hook(new IHook() {
                @Override
                public void after() {
                    FileHelper.write(mDataPath, "[]");

                    logD(TAG, "Clear all clipboard data!!");
                }
            })

            .findAllMethod("commitClipDataAndTrack")
            .hook(new IHook() {
                @Override
                public void before() {
                    Integer type = (Integer) getArg(3);
                    if (type == null) return;
                    if (type == 3 || type == 2) {
                        setArg(3, 10);
                    }
                }
            })  // 修复小米的 BUG

            .findMethod("processSingleItemOfClipData", ClipData.class, String.class)
            .hook(new IHook() {
                @Override
                public void before() {
                    ClipData clipData = (ClipData) getArg(0);
                    ClipData.Item item = clipData.getItemAt(0);
                    if (item.getText() != null && !TextUtils.isEmpty(item.getText().toString())) {
                        mText = item.getText().toString();
                    }
                }
            }) // 解除 5000 字限制

            .findMethod("buildClipDataItemModelBasedTextData", String.class)
            .hook(new IHook() {
                @Override
                public void before() {
                    if (mMaxSize == -1) {
                        mMaxSize = (Integer) getStaticField(
                            "com.miui.inputmethod.MiuiClipboardManager",
                            classLoader,
                            "MAX_CLIP_CONTENT_SIZE"
                        );

                        if (mMaxSize == null)
                            mMaxSize = 5000;
                    }

                    String string = (String) getArg(0);
                    if (string.length() == mMaxSize) {
                        if (mText != null) setArg(0, mText);
                    }
                    mText = null;
                }
            }); // 解除 5000 字限制

        if (existsMethod("com.miui.inputmethod.InputMethodClipboardPhrasePopupView", classLoader, "setRemoteDataToView")) {
            hookMethod("com.miui.inputmethod.InputMethodClipboardPhrasePopupView",
                classLoader,
                "setRemoteDataToView",
                new IHook() {
                    @Override
                    public void before() {
                        returnNull();
                    }
                }
            );
        }
    }

    private static void oldVersionHook() {
        buildChain("com.miui.inputmethod.InputMethodUtil", classLoader)
            .findMethod("getClipboardData", Context.class)
            .hook(new IHook() {
                @Override
                public void before() {
                    setResult(
                        loadClipboardData((Context) getArg(0))
                    );
                }
            }) // 读取剪贴板数据

            .findMethod("addClipboard", String.class, String.class, int.class, Context.class)
            .hook(new IHook() {
                @Override
                public void before() {
                    String content = (String) getArg(1);
                    int type = (int) getArg(2);
                    ArrayList<ContentModel> contentModels = new ArrayList<>();
                    ContentModel contentModel = new ContentModel();
                    contentModel.setContent(content);
                    contentModel.setType(type);
                    contentModel.setTime(System.currentTimeMillis());
                    contentModels.add(contentModel);

                    addClipboardItem((Context) getArg(3), contentModels);
                    returnNull();
                }
            }) // 添加剪贴板条目

            .findMethod("setClipboardModelList", Context.class, ArrayList.class)
            .hook(new IHook() {
                @Override
                public void before() {
                    ArrayList<?> dataList = (ArrayList<?>) getArg(1);
                    FileHelper.write(mDataPath, mGson.toJson(dataList));
                    if (!dataList.isEmpty()) returnNull();
                }
            }); // 保存剪贴板数据
    }

    @NonNull
    private static ArrayList<?> loadClipboardData(@NonNull Context context) {
        ArrayList<ContentModel> modelList = toContentModelList(FileHelper.read(mDataPath));
        if (modelList.isEmpty()) {
            String data = getContentData(context);
            if (data.isEmpty())
                return new ArrayList<>();

            ArrayList<ContentModel> contentModels = toContentModelList(data);
            FileHelper.write(mDataPath, mGson.toJson(contentModels));
            return toClipboardList(contentModels);
        }
        return toClipboardList(modelList);
    }

    private static void addClipboardItem(@NonNull Context context, @NonNull List<ContentModel> contentModels) {
        if (FileHelper.isEmpty(mDataPath)) {
            // 数据库为空时写入数据
            String data = getContentData(context);
            ArrayList<ContentModel> dataList;
            dataList = toContentModelList(data);
            dataList.addAll(0, contentModels);
            FileHelper.write(mDataPath, mGson.toJson(dataList));
            return;
        }

        ArrayList<ContentModel> data = toContentModelList(FileHelper.read(mDataPath));
        if (data.isEmpty()) {
            logW(TAG, "Clipboard data is empty!!");
        } else {
            HashSet<String> contenHashSet = new HashSet<>();
            contentModels.forEach(model -> contenHashSet.add(model.getContent()));
            data.removeIf(model -> contenHashSet.contains(model.getContent()));

            data.addAll(0, contentModels);
            FileHelper.write(mDataPath, mGson.toJson(data));
        }
    }

    @NonNull
    private static String getContentData(@NonNull Context context) {
        Bundle call = context.getContentResolver().call(
            Uri.parse(isNewVersion ? "content://com.miui.phrase.input.provider" : "content://com.miui.input.provider"),
            "getClipboardList",
            null, new Bundle()
        );
        return call != null ? Optional.ofNullable(call.getString("savedClipboard")).orElse("") : "";
    }

    @NonNull
    private static ArrayList<ContentModel> toContentModelList(@NonNull String data) {
        if (data.isEmpty()) return new ArrayList<>();

        ArrayList<ContentModel> contentModels = mGson.fromJson(
            data,
            new TypeToken<ArrayList<ContentModel>>() {
            }.getType()
        );
        if (contentModels == null)
            return new ArrayList<>();
        return contentModels;
    }

    @NonNull
    private static ArrayList<?> toClipboardList(@NonNull ArrayList<ContentModel> dataList) {
        return dataList.stream()
            .filter(Objects::nonNull)
            .map(ClipboardLimit::restoreContentModel)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @NonNull
    private static Object restoreContentModel(@NonNull ContentModel contentModel) {
        return callStaticMethod(
            "com.miui.inputmethod.ClipboardContentModel",
            classLoader,
            "fromJSONObject",
            contentModel.toJSONObject()
        );
    }
}
