package com.sevtinge.hyperceiler.module.hook.various;

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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class ClipboardList extends BaseHook {
    public ArrayList<?> lastArray = null;
    public ArrayList<?> mClipboardList;

    // public int num;

    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("android.inputmethodservice.InputMethodModuleManager",
            "loadDex", ClassLoader.class, String.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    // haveClassLoader((ClassLoader) param.args[0]);
                    // getClipboardData((ClassLoader) param.args[0]);
                    // getClipboardData((ClassLoader) param.args[0]);
                    getNoExpiredData((ClassLoader) param.args[0]);
                    /*看不懂，直接禁用*/
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
                protected void before(MethodHookParam param) throws Throwable {
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
                protected void before(MethodHookParam param) throws Throwable {
                    // JSONArray mJson;
                    ArrayList mArray = new ArrayList<>();
                    // logE(TAG, "g1: " + param.args[1]);
                    /*获取原始list数据内容*/
                    ArrayList<?> jsonToBean = jsonToBean((String) param.args[1], classLoader);
                    // logE(TAG, "jsonToBean: " + jsonToBean);
                    // JSONArray jsonArray = new JSONArray((String) param.args[1]);
                    /*数据为空说明被清理，清空文件数据并置空历史数据*/
                    if (jsonToBean == null) {
                        logE(TAG, "jsonToBean is null");
                        param.setResult(new ArrayList<>());
                        return;
                    }
                    if (jsonToBean.size() == 0) {
                        /*防止在数据为空时误删数据库数据*/
                        // resetFile();
                        lastArray = null;
                        if (!checkFile()) {
                            mArray = jsonToLIst(classLoader);
                            if (!mArray.isEmpty()) {
                                param.setResult(mArray);
                                return;
                            }
                        }
                        param.setResult(new ArrayList<>());
                        logD(TAG + ": get saved clipboard list size is 0.");
                        return;
                    }
                    /*文件不为空说明有数据*/
                    if (!checkFile()) {
                        // logE(TAG, "re: " + readFile());
                        mArray = jsonToLIst(classLoader);
                        if (lastArray != null) {
                            if (mArray == null) {
                                logE(TAG, "mArray is null it's bad");
                                param.setResult(new ArrayList<>());
                                return;
                            }
                            Object oneLast = getContent(lastArray, 0);
                            /*防止在只复制一个元素时重复开关界面引发的null*/
                            if (jsonToBean.size() < 2) {
                                if (!getContent(jsonToBean, 0).equals(getContent(mArray, 0))) {
                                    // mArray.add(0, jsonToBean.get(0));
                                    addOrHw(mArray, getContent(jsonToBean, 0), jsonToBean);
                                }
                                // logE(TAG, "ppp");
                                param.setResult(mArray);
                                return;
                            }
                            Object oneArray = getContent(jsonToBean, 0);
                            Object twoArray = getContent(jsonToBean, 1);
                            if (oneArray == null || twoArray == null || oneLast == null) {
                                logE(TAG, "oneArray or twoArray or oneLast is null");
                                param.setResult(new ArrayList<>());
                                return;
                            }
                            // Object twoLast = getContent(lastArray, 1);
                            if (!oneArray.equals(oneLast) && twoArray.equals(oneLast)) {
                                mArray = addOrHw(mArray, oneArray, jsonToBean);
                                // logE(TAG, "tttt");
                            } else if (!oneArray.equals(oneLast) && !twoArray.equals(oneLast)) {
                                // logE(TAG, "size: " + jsonToBean.size());
                                /*if (jsonToBean.size() >= 20) {
                                    for (int i = 0; i < jsonToBean.size(); i++) {
                                        oneArray = getContent(jsonToBean, i);
                                        if (oneArray != null && !oneArray.equals(oneLast)) {
                                            mArray.add(i, jsonToBean.get(i));
                                        }
                                    }
                                } else {

                                }*/
                                int have = 0;
                                for (int i = 0; i < jsonToBean.size(); i++) {
                                    ++have;
                                    // logE(TAG, "rr: " + getContent(jsonToBean, i) + " have: " + have);
                                    if (getContent(jsonToBean, i).equals(oneLast)) {
                                        break;
                                    }
                                }
                                for (int i = 0; i < have - 1; i++) {
                                    mArray.add(i, jsonToBean.get(i));
                                }
                                // for (int i = 0; i < mArray.size(); i++) {
                                //     logE(TAG, "mm: " + getContent(mArray, i));
                                // }
                                // logE(TAG, "ikk");
                            }

                        }
                        /*置旧*/
                        lastArray = jsonToBean;
                        // logE(TAG, "44: " + lastArray + " 66: " + jsonToBean);
                        /*清空文件写入*/
                        if (resetFile()) {
                            writeFile(listToJson(mArray));
                        }
                        // logE(TAG, "im run");
                        param.setResult(mArray);
                        /*lastArray不为空说明有历史数据*/
                        /*if (lastArray != null) {
                            oneLast = getContent(lastArray, 0);
                            Object oneArray = getContent(jsonToBean, 0);
                            if (oneArray != null && !oneArray.equals(oneLast)) {
                                boolean check = false;
                                for (int i = 0; i < jsonToBean.size(); i++) {
                                    oneArray = getContent(jsonToBean, i);
                                    根据第一个元素判断是否新增数据
                                    if (oneArray != null && !oneArray.equals(oneLast)) {
                                        boolean add = true;
                                        int run = 0;
                                        if (!check)
                                            for (int j = 0; j < mArray.size(); j++) {
                                                run++;
                                                if (getContent(jsonToBean, 0).equals(getContent(mArray, j))) {
                                                    add = false;
                                                    break;
                                                }
                                            }
                                        if (add) {
                                            mArray.add(i, jsonToBean.get(i));
                                        } else {
                                            Object remove = mArray.get(run);
                                            mArray.remove(run);
                                            mArray.add(0, remove);
                                            check = true;
                                            logE(TAG, "re: " + remove + " nn: " + num);
                                        }
                                    } else {
                                        break;
                                    }
                                    logE(TAG, "22: " + oneArray + " 33: " + oneLast);
                                }
                            }
                        }*/
                    } else {
                        /*置旧*/
                        lastArray = jsonToBean;
                        writeFile(listToJson(jsonToBean));
                        param.setResult(jsonToBean);
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
                protected void after(MethodHookParam param) throws Throwable {
                    // num = (int) param.args[0];
                    mClipboardList = (ArrayList<?>)
                        XposedHelpers.getObjectField(param.thisObject, "mClipboardList");
                }
            }
        );

        /*findAndHookMethod("com.miui.inputmethod.InputMethodClipboardAdapter", classLoader,
            "getCount",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    XposedHelpers.setObjectField(param.thisObject, "mClipboardList", jsonToLIst(classLoader));
                    mClipboardList = (ArrayList<?>)
                        XposedHelpers.getObjectField(param.thisObject, "mClipboardList");
                    ArrayList arrayList = jsonToLIst(classLoader);
                    int size;
                    if (arrayList == null) {
                        size = 0;
                    } else
                        size = arrayList.size();
                    XposedHelpers.callMethod(XposedHelpers.getObjectField(
                            param.thisObject, "mClipClearBtn"),
                        "setEnabled", size != 0);
                    param.setResult(size);
                    // logE(TAG, "runnnn : " + size);
                }
            }
        );*/

        /*findAndHookMethod("com.miui.inputmethod.InputMethodClipboardAdapter$3$1", classLoader,
            "onClickDelete",
            new MethodHook() {
                *//*@Override
                protected void after(MethodHookParam param) throws Throwable {
                    logE(TAG, "rnn: " + num + " nn :" + getContent(mClipboardList, num));
                    ArrayList mArray = jsonToLIst(classLoader);
                    mArray.remove(mArray.size() - 1 - num);
                    if (resetFile()) {
                        writeFile(listToJson(mArray));
                    }
                }*//*

                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    for (int i = 0; i < mClipboardList.size(); i++) {
                        logE(TAG, "gg: " + getContent(mClipboardList, i));
                    }
                    // param.setResult(null);
                }
            }
        );*/

        hookAllMethods("com.miui.inputmethod.InputMethodUtil", classLoader,
            "setClipboardModelList", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    ArrayList<?> arrayList = (ArrayList<?>) param.args[1];
                    if (arrayList.isEmpty()) {
                        lastArray = null;
                    } else lastArray = arrayList;
                    if (resetFile()) {
                        writeFile(listToJson(arrayList));
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
                    lastArray = null;
                    // resetFile();
                }
            }
        );
    }

    public ArrayList addOrHw(ArrayList mArray, Object oneArray, ArrayList<?> jsonToBean) {
        boolean needAdd = false;
        boolean needHw = false;
        int run = 0;
        for (int i = 0; i < mArray.size(); i++) {
            run++;
            needAdd = true;
            if (oneArray.equals(getContent(mArray, i))) {
                needHw = true;
                needAdd = false;
                break;
            }
        }
        if (needHw) {
            mArray.add(0, mArray.get(run - 1));
            mArray.remove(run);
        }
        if (needAdd)
            mArray.add(0, jsonToBean.get(0));
        return mArray;
    }

    public void writeFile(JSONArray jsonArray) {
        /*File folder = new File("/data/user/0/" + lpparam.packageName + "/files");
        // 检查文件夹是否存在
        if (!folder.exists()) {
            // 文件夹不存在，尝试创建
            folder.mkdirs(); // 创建文件夹及其父文件夹
        }
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(
                new FileOutputStream(new File(
                    "/data/user/0/" + lpparam.packageName + "/files", "array_list.dat")));
            outputStream.writeObject(arrayList);
            outputStream.close();
        } catch (IOException e) {
            logE(TAG, "writeFile: " + e);
        }*/
        if (jsonArray == null) {
            logE(TAG, "write json is null");
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new
            FileWriter("/data/user/0/" +
            lpparam.packageName + "/files/array_list.dat"))) {
            writer.write(jsonArray.toString());
        } catch (IOException e) {
            logE(TAG, "writeFile: " + e);
        }
    }

    public boolean checkFile() {
        try (BufferedReader reader = new BufferedReader(new
            FileReader("/data/user/0/" +
            lpparam.packageName + "/files/array_list.dat"))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String jsonString = builder.toString();
            if (jsonString.equals("[]") || jsonString.isEmpty()) {
                resetFile();
                return true;
            }
            return false;
        } catch (IOException e) {
            logE(TAG, "readFile: " + e);
            return true;
        }
    }

    public JSONArray readFile() {
        /*ArrayList<?> loadedArrayList = null;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(
                new FileInputStream(
                    new File("/data/user/0/" + lpparam.packageName + "/files", "array_list.dat")));
            loadedArrayList = (ArrayList<?>) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            logE(TAG, "readFile: " + e);
        }
        return loadedArrayList;*/
        try (BufferedReader reader = new BufferedReader(new
            FileReader("/data/user/0/" +
            lpparam.packageName + "/files/array_list.dat"))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String jsonString = builder.toString();
            JSONArray jsonArray = new JSONArray(jsonString);
            return jsonArray;
        } catch (IOException | JSONException e) {
            logE(TAG, "readFile: " + e);
        }
        return null;
    }

    public boolean resetFile() {
        // 清空文件内容
        try (BufferedWriter writer = new BufferedWriter(new
            FileWriter("/data/user/0/" +
            lpparam.packageName + "/files/array_list.dat", false))) {
            writer.write("");
            return true;
        } catch (IOException e) {
            logE(TAG, "writeFile: " + e);
            return false;
        }
    }

    public ArrayList jsonToLIst(ClassLoader classLoader) {
        try {
            JSONArray mJson;
            ArrayList mArray = new ArrayList<>();
            mJson = readFile();
            if (mJson == null) {
                logE(TAG, "jsonToLIst readFile is null");
                return null;
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
        } catch (JSONException e) {
            logE(TAG, "jsonToLIst: " + e);
        }
        return null;
    }

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
        } catch (JSONException e2) {
            logE(TAG, "jsonToBean,parse JSON error: " + e2);
        }
        return arrayList;
    }

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
        return null;
    }

    public String getContent(ArrayList<?> arrayList, int num) {
        try {
            return (String) XposedHelpers.callMethod(arrayList.get(num), "getContent");
        } catch (Throwable throwable) {
            logE(TAG, "getContent: " + throwable);
        }
        return null;
    }
}
