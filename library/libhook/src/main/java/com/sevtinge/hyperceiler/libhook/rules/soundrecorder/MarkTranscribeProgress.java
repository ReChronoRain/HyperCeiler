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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.soundrecorder;

import android.app.Notification;
import android.app.NotificationManager;
import android.os.Bundle;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

/**
 * 录音机 AI 转写进度通知 (channel_id_record_recognition / id=1001) 把进度写在
 * 标题里 (例: &quot;AI转写中 12%&quot;)，但没有调用 setProgress() 也没有 setOngoing()，
 * 导致通知监听类应用 (小米运动健康、各类手环同步) 把每次 1% 变化都当成新通知
 * 反复推送/震动。
 * <p>
 * 在录音机进程内 hook NotificationManager.notify，对目标通知补上：
 * <ul>
 *     <li>android.progressMax / android.progress / android.progressIndeterminate
 *         extras — 让接收方 inflate 标准模板时看到 ProgressBar，能被进度过滤器
 *         识别。</li>
 *     <li>FLAG_ONGOING_EVENT — 让 StatusBarNotification.isOngoing() 返回 true，
 *         触发监听方的 ongoing 通知过滤。</li>
 * </ul>
 * 完成通知 (&quot;AI转写完成&quot;) 标题里没有百分比，不命中正则，照常下发。
 */
public class MarkTranscribeProgress extends BaseHook {

    private static final int TARGET_ID = 1001;
    private static final String TARGET_CHANNEL = "channel_id_record_recognition";
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("(\\d{1,3})\\s*%");

    @Override
    public void init() {
        findAndHookMethod(NotificationManager.class, "notify",
            int.class, Notification.class,
            new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    fixProgress((Integer) param.getArgs()[0], (Notification) param.getArgs()[1]);
                }
            });

        findAndHookMethod(NotificationManager.class, "notify",
            String.class, int.class, Notification.class,
            new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    fixProgress((Integer) param.getArgs()[1], (Notification) param.getArgs()[2]);
                }
            });
    }

    private static void fixProgress(int id, Notification n) {
        if (n == null || id != TARGET_ID) return;
        if (!TARGET_CHANNEL.equals(n.getChannelId())) return;

        Bundle extras = n.extras;
        if (extras == null) return;

        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        if (title == null) return;

        Matcher m = PROGRESS_PATTERN.matcher(title);
        if (!m.find()) return;

        int percent;
        try {
            percent = Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return;
        }
        if (percent < 0 || percent > 100) return;

        extras.putInt(Notification.EXTRA_PROGRESS_MAX, 100);
        extras.putInt(Notification.EXTRA_PROGRESS, percent);
        extras.putBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false);

        n.flags |= Notification.FLAG_ONGOING_EVENT;
    }
}
