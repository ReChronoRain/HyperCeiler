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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.app.Others;

import android.content.Context;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.phrase.NewUnPhraseLimit;
import com.sevtinge.hyperceiler.libhook.rules.various.MusicHooks;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.BaiduClipboard;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.ClearClipboard;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.ClipboardLimit;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.LoadInputMethodDex;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.SoGouClipboard;
import com.sevtinge.hyperceiler.libhook.rules.various.clipboard.UnlockIme;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.ArrayList;
import java.util.List;

@HookBase(targetPackage = "VariousThirdApps")
public class VariousThirdApps extends BaseLoad {
    String mPackageName;

    public static List<String> mAppsUsingInputMethod = new ArrayList<>();

    public VariousThirdApps() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
        if (mAppsUsingInputMethod.isEmpty()) {
            mAppsUsingInputMethod = getAppsUsingInputMethod(AppsTool.findContext(AppsTool.FlAG_ONLY_ANDROID));
        }
        mPackageName = getPackageName();
        if (mPrefsMap.getBoolean("various_phrase_clipboardlist")) {
            if (isInputMethod(mPackageName)) {
                initHook(new LoadInputMethodDex());
                initHook(new ClipboardLimit());
            }
        }
        initHook(new UnlockIme(), mPrefsMap.getBoolean("various_unlock_ime") && isInputMethod(mPackageName));
        initHook(new NewUnPhraseLimit(), mPrefsMap.getBoolean("various_phrase_clipboardlist") && isInputMethod(mPackageName));
        initHook(new SoGouClipboard(), mPrefsMap.getBoolean("sogou_xiaomi_clipboard") &&
                ("com.sohu.inputmethod.sogou.xiaomi".equals(mPackageName) || "com.sohu.inputmethod.sogou".equals(mPackageName)));
        initHook(new BaiduClipboard(), mPrefsMap.getBoolean("sogou_xiaomi_clipboard") &&
                ("com.baidu.input".equals(mPackageName) || "com.baidu.input_mi".equals(mPackageName)));
        //initHook(new ClipboardList(), mPrefsMap.getBoolean("various_phrase_clipboardlist") && isInputMethod(mPackageName));
        initHook(new ClearClipboard(), mPrefsMap.getBoolean("add_clipboard_clear") && isInputMethod(mPackageName));

        // 焦点歌词（音乐软件相关）
        initHook(MusicHooks.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_music_switch") && mPrefsMap.getBoolean("system_ui_statusbar_music_show_app"));
    }

    private List<String> getAppsUsingInputMethod(Context context) {
        try {
            if (context == null) {
                XposedLog.e("getAppsUsingInputMethod", "context is null");
                return new ArrayList<>();
            }
            List<String> pkgName = new ArrayList<>();
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> enabledInputMethods = inputMethodManager.getEnabledInputMethodList();
            for (InputMethodInfo inputMethodInfo : enabledInputMethods) {
                pkgName.add(inputMethodInfo.getServiceInfo().packageName);
            }
            return pkgName;
        } catch (Throwable e) {
            XposedLog.e("getAppsUsingInputMethod", "have e: " + e + ", message: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean isInputMethod(String pkgName) {
        if (mAppsUsingInputMethod.isEmpty()) {
            return false;
        }
        for (String inputMethod : mAppsUsingInputMethod) {
            if (inputMethod.equals(pkgName)) {
                return true;
            }
        }
        return false;
    }
}
