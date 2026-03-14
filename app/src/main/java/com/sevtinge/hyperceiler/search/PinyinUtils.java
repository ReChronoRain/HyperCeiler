package com.sevtinge.hyperceiler.search;

import android.text.TextUtils;

public class PinyinUtils {
    /**
     * 获取汉字串拼音首字母，英文字符不变
     */
    public static String getFirstLetters(String str) {
        if (TextUtils.isEmpty(str)) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FA5) { // 如果是中文字符
                sb.append(getCharFirstLetter(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString().toLowerCase();
    }

    // 这是一个简单的汉字首字母映射表逻辑（简化版）
    // 实际生产中如果没有库，也可以只存原文本，但搜索体验会打折扣
    private static char getCharFirstLetter(char ch) {
        // 这里可以实现一个简单的映射，或者暂时返回原字符
        // 为了代码完整性，这里建议还是先存原文本，等后期有需求再加库
        return ch;
    }
}
