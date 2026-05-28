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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

/**
 * 阻止 HyperOS 3 在两条系统级路径上误清正在进行的 Focus 通知（让超级岛保留）：
 * <ol>
 *     <li><b>划掉最近任务</b> ── MIUI 走 ProcessCleanerBase.tryToForceStopPackage
 *         (reason=&quot;SwipeUpClean&quot;) → forceStopPackage → PACKAGE_RESTARTED 广播 →
 *         NotificationManagerService.cancelAllNotificationsInt(reason=5)。
 *         我们在 tryToForceStopPackage 打 15s TTL 的 pkg 标记，再在
 *         NotificationManagerServiceImpl.skipClearAll(reason=5) 命中标记 + 可更新
 *         Focus 时强制返回 true。</li>
 *     <li><b>划掉自动分组的通知组</b> ── 用户在通知栏整体划掉某 App 的非 Focus
 *         通知组时，NotificationManagerService 走 cancelGroupChildrenByListLocked
 *         → cancelNotificationLocked(reason=12) 循环取消。MIUI 的 skipClearAll
 *         只覆盖批量入口，不覆盖这里。我们直接 hook
 *         NotificationManagerService.cancelNotificationLocked，对可更新 Focus 通知
 *         在 reason=12 时阻止取消。</li>
 * </ol>
 * 不影响：手动强停、通知栏手动单独划掉 Focus 通知本身、外卖 App 主动 cancel
 * 订单通知、普通营销通知。
 */
public class KeepFocusOnSwipe extends BaseHook {

    private static final String SWIPE_UP_CLEAN = "SwipeUpClean";
    /** Matches {@code NotificationListenerService.REASON_PACKAGE_CHANGED}. */
    private static final int REASON_PACKAGE_CHANGED = 5;
    /** Matches {@code NotificationListenerService.REASON_GROUP_SUMMARY_CANCELED}. */
    private static final int REASON_GROUP_SUMMARY_CANCELED = 12;
    private static final long MARK_TTL_MILLIS = 15_000L;

    private static final String EXTRA_FOCUS_PARAM = "miui.focus.param";
    private static final String SCENE_FOOD_DELIVERY = "foodDelivery";
    private static final String SCENE_CAR_HAILING = "carHailing";

    /** pkg -> mark timestamp (ms). Shared between the two hooks. */
    private static final ConcurrentHashMap<String, Long> SWIPE_MARKS = new ConcurrentHashMap<>();

    /**
     * Memoizes the parsed Settings.Secure["updatable_focus_notifs"] JSON so that
     * a single cleanup burst (many notifications for one pkg processed in a row)
     * does not re-read the setting and re-parse the JSON for every record.
     * Volatile because writes happen on the binder thread serving skipClearAll.
     */
    private static volatile String sLastRawFocusNotifs;
    private static volatile Set<String> sLastParsedFocusNotifs;

    @Override
    public void init() {
        hookSwipeMarker();
        hookSkipClearAll();
        hookGroupChildCancel();
    }

    private void hookSwipeMarker() {
        findAndHookMethod(
            "com.android.server.am.ProcessCleanerBase",
            "tryToForceStopPackage",
            String.class, int.class, String.class,
            new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Object reasonObj = param.getArgs()[2];
                    if (!(reasonObj instanceof String)) return;
                    String reason = (String) reasonObj;
                    // getKillReason(policy=10) may append a suffix to the base string.
                    if (!reason.startsWith(SWIPE_UP_CLEAN)) return;

                    Object pkgObj = param.getArgs()[0];
                    if (!(pkgObj instanceof String)) return;
                    long now = System.currentTimeMillis();
                    pruneExpiredMarks(now);
                    SWIPE_MARKS.put((String) pkgObj, now);
                }
            }
        );
    }

    /**
     * Opportunistically drop entries whose TTL has elapsed. Called from
     * {@code tryToForceStopPackage} hook (the only writer), so even if a marked
     * package never triggers a follow-up {@code skipClearAll} check, the map
     * stays bounded by the number of swipe-up-cleans inside one TTL window.
     */
    private static void pruneExpiredMarks(long now) {
        Iterator<Map.Entry<String, Long>> it = SWIPE_MARKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (now - e.getValue() > MARK_TTL_MILLIS) {
                it.remove();
            }
        }
    }

    private void hookSkipClearAll() {
        Class<?> recordCls = findClassIfExists("com.android.server.notification.NotificationRecord");
        if (recordCls == null) return;

        findAndHookMethod(
            "com.android.server.notification.NotificationManagerServiceImpl",
            "skipClearAll",
            recordCls, int.class,
            new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    if (Boolean.TRUE.equals(param.getResult())) return;

                    Object reasonObj = param.getArgs()[1];
                    if (!(reasonObj instanceof Integer)) return;
                    if ((Integer) reasonObj != REASON_PACKAGE_CHANGED) return;

                    Object record = param.getArgs()[0];
                    if (record == null) return;

                    StatusBarNotification sbn;
                    String key;
                    try {
                        sbn = (StatusBarNotification) callMethod(record, "getSbn");
                        key = (String) callMethod(record, "getKey");
                    } catch (Throwable ignored) {
                        return;
                    }
                    if (sbn == null || key == null) return;

                    if (!isMarkedRecently(sbn.getPackageName())) return;
                    if (!isUpdatableFocusNotification(sbn, key)) return;

                    param.setResult(true);
                }
            }
        );
    }

    /**
     * Hook NotificationManagerService.cancelNotificationLocked (8-arg overload, the real
     * implementation; the 6-arg shim delegates to it). When a non-Focus notification group
     * is dismissed, NMS calls cancelGroupChildrenByListLocked which removes each child from
     * the in-memory maps and then invokes this method with reason=12 to broadcast the
     * removal. If any of those children happens to be an updatable Focus (e.g. MIUI auto-
     * grouped the delivery notif with the app's other notifs), the broadcast tears down
     * the super-island. Returning early here suppresses the broadcast — SystemUI never
     * sees the removal so the island stays. The next focus update from the App re-enqueues
     * normally and restores NMS state.
     * <p>
     * Unlike the swipe-clean path, no SwipeMarker check: this is a per-record decision
     * driven by reason + record content, not by who triggered the cancel.
     */
    private void hookGroupChildCancel() {
        Class<?> recordCls = findClassIfExists("com.android.server.notification.NotificationRecord");
        if (recordCls == null) return;

        findAndHookMethod(
            "com.android.server.notification.NotificationManagerService",
            "cancelNotificationLocked",
            recordCls, boolean.class, int.class, int.class, int.class,
            boolean.class, String.class, long.class,
            new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Object reasonObj = param.getArgs()[2];
                    if (!(reasonObj instanceof Integer)) return;
                    if ((Integer) reasonObj != REASON_GROUP_SUMMARY_CANCELED) return;

                    Object record = param.getArgs()[0];
                    if (record == null) return;

                    StatusBarNotification sbn;
                    String key;
                    try {
                        sbn = (StatusBarNotification) callMethod(record, "getSbn");
                        key = (String) callMethod(record, "getKey");
                    } catch (Throwable ignored) {
                        return;
                    }
                    if (sbn == null || key == null) return;

                    if (!isUpdatableFocusNotification(sbn, key)) return;

                    // void method — suppress the cancel by short-circuiting.
                    param.setResult(null);
                }
            }
        );
    }

    private static boolean isMarkedRecently(String pkg) {
        if (pkg == null) return false;
        Long t = SWIPE_MARKS.get(pkg);
        if (t == null) return false;
        if (System.currentTimeMillis() - t > MARK_TTL_MILLIS) {
            SWIPE_MARKS.remove(pkg);
            return false;
        }
        return true;
    }

    /**
     * Mirrors HyperOS's own {@code FocusTemplate} definition:
     * <pre>
     * updatable = (param["updatable"] == true
     *              || scene == foodDelivery
     *              || scene == carHailing)
     *             && hasPermission()
     * </pre>
     * The secure-setting check is the authoritative path because SystemUI's
     * {@code FocusCoordinator} writes the system-accepted set there; the param
     * check is a fallback for the small window where the setting hasn't been
     * refreshed yet.
     */
    private static boolean isUpdatableFocusNotification(StatusBarNotification sbn, String key) {
        if (isKeyInUpdatableFocusNotifs(key)) return true;
        return looksLikeUpdatableFocusParam(sbn);
    }

    private static boolean isKeyInUpdatableFocusNotifs(String key) {
        try {
            Context ctx = getSystemContext();
            if (ctx == null) return false;
            String raw = Settings.Secure.getString(ctx.getContentResolver(), "updatable_focus_notifs");
            if (TextUtils.isEmpty(raw)) return false;
            Set<String> parsed = parsedFocusNotifs(raw);
            return parsed.contains(key);
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Set<String> parsedFocusNotifs(String raw) {
        // Fast path: same raw payload as last time -> reuse parsed Set.
        // Compared by equals() to tolerate identical reissues from Settings.
        Set<String> cached = sLastParsedFocusNotifs;
        if (cached != null && raw.equals(sLastRawFocusNotifs)) {
            return cached;
        }
        Set<String> parsed = parseFocusNotifsArray(raw);
        sLastRawFocusNotifs = raw;
        sLastParsedFocusNotifs = parsed;
        return parsed;
    }

    private static Set<String> parseFocusNotifsArray(String raw) {
        Set<String> set = new LinkedHashSet<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                String k = arr.optString(i, "");
                if (!k.isEmpty()) set.add(k);
            }
        } catch (Throwable ignored) {
            return Collections.emptySet();
        }
        return set;
    }

    private static boolean looksLikeUpdatableFocusParam(StatusBarNotification sbn) {
        try {
            if (sbn.getNotification() == null) return false;
            Bundle extras = sbn.getNotification().extras;
            if (extras == null) return false;
            String paramJson = extras.getString(EXTRA_FOCUS_PARAM);
            if (TextUtils.isEmpty(paramJson)) return false;
            JSONObject obj = new JSONObject(paramJson);
            if (obj.optBoolean("updatable", false)) return true;
            String scene = obj.optString("scene", "");
            return SCENE_FOOD_DELIVERY.equals(scene) || SCENE_CAR_HAILING.equals(scene);
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Context getSystemContext() {
        try {
            Class<?> atCls = Class.forName("android.app.ActivityThread");
            Object at = atCls.getMethod("currentActivityThread").invoke(null);
            if (at == null) return null;
            return (Context) atCls.getMethod("getSystemContext").invoke(at);
        } catch (Throwable t) {
            return null;
        }
    }
}
