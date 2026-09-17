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
package com.sevtinge.hyperceiler.libhook.rules.cloudservice;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

/**
 * 國際版 ROM 上，CloudService 會無條件執行相簿雲同步終止邏輯
 * （ServiceTerminatedHelper：setIsSyncable(gallery, 0) + setSyncAutomatically(gallery, false)），
 * 導致伺服器仍支援相簿同步的帳號（如中國區）也無法同步（介面卡在「正在請求」）。
 *
 * 當使用者在相簿選擇小米雲作為備份伺服器（gallery_backup_server == 1）、
 * 且小米帳號註冊區域為 CN 時自動生效：
 * 攔截上述兩個寫入，並在 CloudService 啟動時主動修復同步狀態。
 */
public class RestoreGallerySync extends BaseHook {
    private static final String TAG = "RestoreGallerySync";
    private static final String AUTHORITY = "com.miui.gallery.cloud.provider";
    private static final String ACCOUNT_TYPE = "com.xiaomi";

    // 1: 尚未檢查, 2: CN 帳號, 3: 非 CN 帳號
    private static volatile int sIsCnAccount = 1;

    @Override
    public void init() {
        findAndHookMethod(ContentResolver.class, "setIsSyncable",
            Account.class, String.class, int.class, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Object[] args = param.getArgs();
                    if (args != null && args.length == 3
                        && AUTHORITY.equals(args[1]) && ((int) args[2]) <= 0 && isCnAccount()) {
                        param.setResult(null);
                    }
                }
            });
        findAndHookMethod(ContentResolver.class, "setSyncAutomatically",
            Account.class, String.class, boolean.class, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Object[] args = param.getArgs();
                    if (args != null && args.length == 3
                        && AUTHORITY.equals(args[1]) && !((boolean) args[2]) && isCnAccount()) {
                        param.setResult(null);
                    }
                }
            });
        repairAsync();
    }

    private static boolean isCnAccount() {
        if (sIsCnAccount == 1) {
            Context context = ContextUtils.getContext(ContextUtils.FLAG_ALL);
            if (context == null) {
                return false;
            }
            try {
                AccountManager accountManager = AccountManager.get(context);
                Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
                if (accounts.length == 0) {
                    return false;
                }
                String region = accountManager.getUserData(accounts[0], "acc_user_region");
                sIsCnAccount = "CN".equalsIgnoreCase(region) ? 2 : 3;
                XposedLog.i(TAG, "account region: " + region);
            } catch (Throwable t) {
                XposedLog.w(TAG, "check account region failed", t);
                return false;
            }
        }
        return sIsCnAccount == 2;
    }

    private void repairAsync() {
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 60; i++) {
                try {
                    Context context = ContextUtils.getContext(ContextUtils.FLAG_ALL);
                    if (context == null || !isCnAccount()) {
                        Thread.sleep(2000);
                        continue;
                    }
                    Account[] accounts = AccountManager.get(context).getAccountsByType(ACCOUNT_TYPE);
                    if (accounts.length == 0) {
                        XposedLog.i(TAG, "no xiaomi account, skip repair");
                        return;
                    }
                    Account account = accounts[0];
                    ContentResolver.setIsSyncable(account, AUTHORITY, 1);
                    ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
                    Bundle extras = new Bundle();
                    extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                    extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                    ContentResolver.requestSync(account, AUTHORITY, extras);
                    XposedLog.i(TAG, "gallery sync state repaired");
                    return;
                } catch (Throwable t) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                        return;
                    }
                }
            }
            XposedLog.w(TAG, "repair timeout, give up");
        });
        thread.setDaemon(true);
        thread.start();
    }
}
