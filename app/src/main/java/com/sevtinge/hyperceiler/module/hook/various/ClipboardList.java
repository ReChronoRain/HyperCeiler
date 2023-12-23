package com.sevtinge.hyperceiler.module.hook.various;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class ClipboardList extends BaseHook {
    public static ArrayList<?> lastArray = new ArrayList<>();

    public static String lastFilePath;
    public ArrayList<?> mClipboardList;

    public static String filePath;

    @Override
    public void init() {
        findAndHookMethod("android.inputmethodservice.InputMethodModuleManager",
            "loadDex", ClassLoader.class, String.class, new MethodHook() {
                @SuppressLint("SdCardPath")
                @Override
                protected void after(MethodHookParam param) {
                    // logE(TAG, "get class: " + param.args[0]);
                    filePath = "/data/user/0/" + lpparam.packageName + "/files/array_list.dat";
                    lastFilePath = "/data/user/0/" + lpparam.packageName + "/files/last_list.dat";
                    getNoExpiredData((ClassLoader) param.args[0]);
                    getView((ClassLoader) param.args[0]);
                    clearArray((ClassLoader) param.args[0]);
                }
            }
        );

    }

    /*折旧*/
    public void getClipboardData(ClassLoader classLoader) {
        findAndHookMethod("com.miui.inputmethod.InputMethodUtil",
            "getClipboardData", Context.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    Context context = (Context) param.args[0];
                    Bundle call = context.getContentResolver().call(Uri.parse("content://com.miui.input.provider"),
                        "getClipboardList", (String) null, new Bundle());
                    String string = call != null ? call.getString("savedClipboard") : "";
                    param.setResult(XposedHelpers.callStaticMethod(findClassIfExists("com.miui.inputmethod.InputMethodUtil", classLoader),
                        "getNoExpiredData", context, string, 0));
                }
            }
        );
    }

    public void getNoExpiredData(ClassLoader classLoader) {
        findAndHookMethod("com.miui.inputmethod.InputMethodUtil", classLoader,
            "getNoExpiredData", Context.class, String.class, long.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    ArrayList mArray = new ArrayList<>();
                    try {
                        checkFile(filePath);
                        checkFile(lastFilePath);
                        /*获取原始list数据内容*/
                        ArrayList<?> jsonToBean = jsonToBean((String) param.args[1], classLoader);
                        if (jsonToBean.size() == 0) {
                            /*防止在数据为空时误删数据库数据*/
                            // resetFile();
                            lastArray = new ArrayList<>();
                            if (!isEmptyFile(filePath)) {
                                mArray = jsonToLIst(filePath, classLoader);
                                if (!mArray.isEmpty()) {
                                    param.setResult(mArray);
                                    return;
                                }
                            }
                            param.setResult(new ArrayList<>());
                            logD(TAG + ": get saved clipboard list size is 0.");
                            return;
                        }
                        if (isEmptyFile(lastFilePath)) {
                            lastArray = jsonToLIst(lastFilePath, classLoader);
                        }
                        /*文件不为空说明有数据*/
                        if (!isEmptyFile(filePath)) {
                            /*数据库数据*/
                            mArray = jsonToLIst(filePath, classLoader);
                            if (!lastArray.isEmpty()) {
                                /*虽说不太可能为空但还是检查一下*/
                                if (mArray.isEmpty()) {
                                    logE(TAG, "mArray is empty it's bad");
                                    param.setResult(new ArrayList<>());
                                    return;
                                }
                                Object oneLast = getContent(lastArray, 0);
                                /*防止在只复制一个元素时重复开关界面引发的未知问题*/
                                if (jsonToBean.size() < 2) {
                                    if (!getContent(jsonToBean, 0).equals(getContent(mArray, 0))) {
                                        mArray = addOrHw(mArray, getContent(jsonToBean, 0), jsonToBean);
                                    }
                                    param.setResult(mArray);
                                    return;
                                }
                                /*读取第一第二个数据判断操作*/
                                Object oneArray = getContent(jsonToBean, 0);
                                Object twoArray = getContent(jsonToBean, 1);
                                if (!oneArray.equals(oneLast) && twoArray.equals(oneLast)) {
                                    /*第一个不同第二个相同说明可能换位或新增*/
                                    mArray = addOrHw(mArray, oneArray, jsonToBean);
                                } else if (!oneArray.equals(oneLast) && !twoArray.equals(oneLast)) {
                                    /*两个不同为新增*/
                                    int have = -1;
                                    for (int i = 0; i < jsonToBean.size(); i++) {
                                        have++;
                                        if (getContent(jsonToBean, i).equals(oneLast)) {
                                            break;
                                        }
                                    }
                                    for (int i = 0; i < have; i++) {
                                        mArray.add(i, jsonToBean.get(i));
                                    }
                                }
                                /*else if (jsonToBean.hashCode() != lastArray.hashCode()) {
                                 *//*很极端的情况，应该不会发生*//*
                                    mArray.addAll(0, jsonToBean);
                                }*/
                            }
                            /*置旧*/
                            lastArray = jsonToBean;
                            if (resetFile(lastFilePath)) {
                                writeFile(lastFilePath, listToJson(lastArray));
                            }
                            /*清空文件写入*/
                            if (resetFile(filePath)) {
                                writeFile(filePath, listToJson(mArray));
                            }
                            param.setResult(mArray);
                        } else {
                            /*置旧*/
                            lastArray = jsonToBean;
                            if (resetFile(lastFilePath)) {
                                writeFile(lastFilePath, listToJson(lastArray));
                            }
                            writeFile(filePath, listToJson(jsonToBean));
                            param.setResult(jsonToBean);
                        }
                    } catch (Throwable throwable) {
                        logE(TAG, "getContent is null: " + throwable);
                        param.setResult(mArray);
                    }
                }
            }
        );
    }

    public void getView(ClassLoader classLoader) {
        findAndHookMethod("com.miui.inputmethod.InputMethodClipboardAdapter",
            classLoader,
            "getView", int.class, View.class, ViewGroup.class,
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    mClipboardList = (ArrayList<?>)
                        XposedHelpers.getObjectField(param.thisObject, "mClipboardList");
                }
            }
        );

        hookAllMethods("com.miui.inputmethod.InputMethodUtil", classLoader,
            "setClipboardModelList", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    ArrayList<?> arrayList = (ArrayList<?>) param.args[1];
                    if (arrayList.isEmpty()) {
                        lastArray = new ArrayList<>();
                        if (resetFile(lastFilePath)) {
                            writeFile(lastFilePath, new JSONArray());
                        }
                    } else {
                        lastArray = arrayList;
                        if (resetFile(lastFilePath)) {
                            writeFile(lastFilePath, listToJson(arrayList));
                        }
                    }
                    if (resetFile(filePath)) {
                        writeFile(filePath, listToJson(arrayList));
                    }
                }
            }
        );

    }

    public void clearArray(ClassLoader classLoader) {
        findAndHookMethod("com.miui.inputmethod.InputMethodClipboardPhrasePopupView$3", classLoader,
            "onConfirm",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    /*点击清除全部按键，清理所有数据*/
                    lastArray = new ArrayList<>();
                }
            }
        );
    }

    /*添加或换位*/
    public ArrayList addOrHw(ArrayList mArray, Object oneArray, ArrayList<?> jsonToBean) throws Throwable {
        if (oneArray == null) {
            logE(TAG, "oneArray is null, mArray: " + mArray + " jsonToBean: " + jsonToBean);
            return mArray;
        }
        boolean needAdd = false;
        boolean needHw = false;
        int run = -1;
        for (int i = 0; i < mArray.size(); i++) {
            run++;
            needAdd = true;
            /*如果数据库存在重复数据*/
            if (oneArray.equals(getContent(mArray, i))) {
                needHw = true;
                needAdd = false;
                break;
            }
        }
        if (needHw) {
            mArray.add(0, mArray.get(run));
            mArray.remove(run + 1);
        }
        if (needAdd)
            mArray.add(0, jsonToBean.get(0));
        return mArray;
    }

    public void checkFile(String path) {
        File file = new File(path);
        File parentDir = file.getParentFile();
        if (parentDir == null) {
            logE(TAG, "parentDir is null: " + path);
        }
        if (parentDir != null && !parentDir.exists()) {
            if (parentDir.mkdirs()) {
                logI(TAG, "mkdirs: " + parentDir);
            } else {
                logE(TAG, "mkdirs: " + parentDir);
            }
        }
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    writeFile(path, new JSONArray());
                    logI(TAG, "createNewFile: " + file);
                } else {
                    logE(TAG, "createNewFile: " + file);
                }
            } catch (IOException e) {
                logE(TAG, "createNewFile: " + e);
            }
        }
    }

    public void writeFile(String path, JSONArray jsonArray) {
        if (jsonArray == null) {
            logE(TAG, "write json is null");
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new
            FileWriter(path, false))) {
            writer.write(jsonArray.toString());
        } catch (IOException e) {
            logE(TAG, "writeFile: " + e);
        }
    }

    public boolean isEmptyFile(String path) {
        JSONArray jsonArray = readFile(path);
        return jsonArray.length() == 0;
    }

    public JSONArray readFile(String path) {
        try (BufferedReader reader = new BufferedReader(new
            FileReader(path))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String jsonString = builder.toString();
            if ("".equals(jsonString)) {
                jsonString = "[]";
            }
            JSONArray jsonArray = new JSONArray(jsonString);
            return jsonArray;
        } catch (IOException | JSONException e) {
            logE(TAG, "readFile: " + e);
        }
        return new JSONArray();
    }

    public boolean resetFile(String path) {
        // 清空文件内容
        writeFile(path, new JSONArray());
        return isEmptyFile(path);
    }

    /*JSON到ArrayList*/
    public ArrayList jsonToLIst(String path, ClassLoader classLoader) {
        try {
            JSONArray mJson;
            ArrayList mArray = new ArrayList<>();
            mJson = readFile(path);
            if (mJson.length() == 0) {
                return new ArrayList<>();
            }
            for (int i = 0; i < mJson.length(); i++) {
                JSONObject jSONObject = mJson.getJSONObject(i);
                if (jSONObject != null) {
                    mArray.add(XposedHelpers.callStaticMethod(
                        XposedHelpers.findClassIfExists(
                            "com.miui.inputmethod.ClipboardContentModel", classLoader),
                        "fromJSONObject", jSONObject));
                }
            }
            return mArray;
        } catch (Throwable e) {
            logE(TAG, "jsonToLIst: " + e);
        }
        return new ArrayList<>();
    }

    /*String到ArrayList，中转JSON*/
    public ArrayList<?> jsonToBean(String str, ClassLoader classLoader) {
        ArrayList arrayList = new ArrayList<>();
        try {
            JSONArray jSONArray = new JSONArray(str);
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                if (jSONObject != null) {
                    arrayList.add(XposedHelpers.callStaticMethod(
                        XposedHelpers.findClassIfExists(
                            "com.miui.inputmethod.ClipboardContentModel", classLoader),
                        "fromJSONObject", jSONObject));
                }
            }
        } catch (Throwable e2) {
            logE(TAG, "jsonToBean,parse JSON error: " + e2);
        }
        return arrayList;
    }

    /*ArrayList到JSON*/
    public JSONArray listToJson(ArrayList<?> arrayList) {
        try {
            JSONArray jSONArray = new JSONArray();
            for (int i = 0; i < arrayList.size(); i++) {
                jSONArray.put(XposedHelpers.callMethod(arrayList.get(i), "toJSONObject"));
            }
            return jSONArray;
        } catch (Throwable throwable) {
            logE(TAG, "listToJson: " + throwable);
        }
        return new JSONArray();
    }

    /*获取剪贴板内容*/
    public String getContent(ArrayList<?> arrayList, int num) throws Throwable {
        try {
            return (String) XposedHelpers.callMethod(arrayList.get(num), "getContent");
        } catch (Throwable throwable) {
            logE(TAG, "getContent array: " + arrayList + " num: " + num + " e: " + throwable);
            throw new Throwable("callMethod getContent error: " + throwable);
        }
    }
}
