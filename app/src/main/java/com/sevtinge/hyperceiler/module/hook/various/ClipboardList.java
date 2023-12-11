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

    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("android.inputmethodservice.InputMethodModuleManager",
            "loadDex", ClassLoader.class, String.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
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
                    ArrayList mArray = new ArrayList<>();
                    /*获取原始list数据内容*/
                    ArrayList<?> jsonToBean = jsonToBean((String) param.args[1], classLoader);
                    /*防止null引发的未知后果*/
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
                        mArray = jsonToLIst(classLoader);
                        if (lastArray != null) {
                            /*虽说不太可能为null但还是检查一下*/
                            if (mArray == null) {
                                logE(TAG, "mArray is null it's bad");
                                param.setResult(new ArrayList<>());
                                return;
                            }
                            Object oneLast = getContent(lastArray, 0);
                            /*防止在只复制一个元素时重复开关界面引发的null*/
                            if (jsonToBean.size() < 2) {
                                if (!getContent(jsonToBean, 0).equals(getContent(mArray, 0))) {
                                    addOrHw(mArray, getContent(jsonToBean, 0), jsonToBean);
                                }
                                param.setResult(mArray);
                                return;
                            }
                            /*读取第一第二个数据判断操作*/
                            Object oneArray = getContent(jsonToBean, 0);
                            Object twoArray = getContent(jsonToBean, 1);
                            if (oneArray == null || twoArray == null || oneLast == null) {
                                logE(TAG, "oneArray or twoArray or oneLast is null");
                                param.setResult(new ArrayList<>());
                                return;
                            }
                            if (!oneArray.equals(oneLast) && twoArray.equals(oneLast)) {
                                /*第一个不同第二个相同说明可能换位或新增*/
                                mArray = addOrHw(mArray, oneArray, jsonToBean);
                            } else if (!oneArray.equals(oneLast) && !twoArray.equals(oneLast)) {
                                /*两个不同为新增*/
                                int have = 0;
                                for (int i = 0; i < jsonToBean.size(); i++) {
                                    ++have;
                                    if (getContent(jsonToBean, i).equals(oneLast)) {
                                        break;
                                    }
                                }
                                for (int i = 0; i < have - 1; i++) {
                                    mArray.add(i, jsonToBean.get(i));
                                }
                            }

                        }
                        /*置旧*/
                        lastArray = jsonToBean;
                        /*清空文件写入*/
                        if (resetFile()) {
                            writeFile(listToJson(mArray));
                        }
                        param.setResult(mArray);
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
                    mClipboardList = (ArrayList<?>)
                        XposedHelpers.getObjectField(param.thisObject, "mClipboardList");
                }
            }
        );

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
