package com.sevtinge.hyperceiler.home.utils;

import android.content.Context;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.home.Header;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HeaderManager {

    private static final String PREF_REMOVE_LIST = "header_remove_list";

    /**
     * 获取经过“安装状态”和“用户偏好”双重过滤后的列表（用于首页显示）
     */
    public static List<Header> getDisplayHeaders(Context context, List<Header> allHeaders) {
        Set<String> removeList = PrefsUtils.getSharedStringSetPrefs(context, PREF_REMOVE_LIST);
        List<Header> filtered = new ArrayList<>();

        for (Header h : allHeaders) {
            String pkg = getPackageName(h);
            // 只有已安装 且 不在移除名单中 才会显示
            if (isAppInstalled(context, pkg) && !removeList.contains(pkg)) {
                h.displayStatus = true;
                filtered.add(h);
            } else {
                h.displayStatus = false;
            }
        }
        return filtered;
    }

    /**
     * 获取用于弹窗选择的列表（只过滤未安装，显示所有已安装供勾选）
     */
    public static List<Header> getCustomOrderHeaders(Context context, List<Header> allHeaders) {
        Set<String> removeList = PrefsUtils.getSharedStringSetPrefs(context, PREF_REMOVE_LIST);
        List<Header> customList = new ArrayList<>();

        for (Header h : allHeaders) {
            String pkg = getPackageName(h);
            // 弹窗里只展示已安装的应用，不管勾没勾选
            if (isAppInstalled(context, pkg)) {
                // 深拷贝一份，防止弹窗里的操作直接影响到内存中的原始数据
                Header copy = copyHeader(h);
                copy.displayStatus = !removeList.contains(pkg);
                customList.add(copy);
            }
        }
        return customList;
    }

    /**
     * 保存当前的勾选状态到本地
     */
    public static void saveHeaderPreferences(Context context, List<Header> editedHeaders) {
        Set<String> removeList = new HashSet<>();
        for (Header h : editedHeaders) {
            if (!h.displayStatus) {
                removeList.add(getPackageName(h));
            }
        }
        PrefsUtils.putStringSet(PREF_REMOVE_LIST, removeList);
    }

    /**
     * 检查应用是否安装
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName) || !packageName.contains(".")) return true;
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String getPackageName(Header h) {
        return h.summary != null ? h.summary.toString() : "";
    }

    private static Header copyHeader(Header h) {
        Header copy = new Header();
        copy.title = h.title;
        copy.titleRes = h.titleRes;
        copy.summary = h.summary;
        copy.fragment = h.fragment;
        copy.fragmentArguments = h.fragmentArguments;
        copy.intent = h.intent;

        return copy;
    }
}
