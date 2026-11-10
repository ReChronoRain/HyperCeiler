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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.app.Notification
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.beforeHookMethod

/**
 * 隐藏媒体卡片（通知栏 / 锁屏 / 控制中心 / 媒体岛 / 副屏一并隐藏）
 *
 * 拦截点是媒体数据管线唯一的入口门（HyperOS 3 / MiuiSystemUI 16.03 反编译验证）：
 *
 * ```
 * MediaDataProcessor / LegacyMediaDataManagerImpl.onNotificationAdded(sbn, key):
 *     if (!useQsMediaPlayer || !sbn.getNotification().isMediaNotification()) {
 *         onNotificationRemoved(key); return;   // 媒体数据不生成
 *     }
 * ```
 *
 * 对媒体通知复刻这一拒绝分支后，MediaData 永不生成，下游全部失去数据源：
 * MiuiMediaNotificationControllerImpl.topMediaData（通知栏/锁屏 MiuiMediaHeader、
 * 媒体岛 MiuiIslandMediaController、副屏 topMediaDataChanged）与
 * MediaCarouselInteractor → MediaFilterRepository → MediaHost（控制中心轮播）。
 *
 * 原媒体通知行不会因此重新露出：MediaCoordinator 的 pre-group filter 只按
 * isMediaNotification 过滤通知列表（锁屏/AOD 同一管线），与媒体数据无关；
 * MiuiNotificationListener 在进入管线前已给白名单通知注入 mediaSession extra，
 * 因此按 extras token 判定与 SystemUI 的门口径一致，无绕过路径。
 */
object HideMediaCard : BaseHook() {

    /** 与 MediaDataManager.isMediaNotification() 同口径：extras 携带 MediaSession Token。 */
    private fun isMediaNotification(sbn: StatusBarNotification?): Boolean {
        val notification = sbn?.notification ?: return false
        val extras = runCatching { notification.extras }.getOrNull() ?: return false
        if (!extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return false
        return runCatching {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, Parcelable::class.java) != null
        }.getOrDefault(false)
    }

    /**
     * HyperOS 3 双管线同门，按存在性依次挂载：
     * - MediaDataProcessor：新管线（MediaCarouselInteractor.onNotificationAdded 转发至此）；
     * - LegacyMediaDataManagerImpl：Legacy 管线。
     */
    private val gatedManagers = listOf(
        "com.android.systemui.media.controls.domain.pipeline.MediaDataProcessor",
        "com.android.systemui.media.controls.domain.pipeline.LegacyMediaDataManagerImpl",
    )

    override fun init() {
        var installed = 0
        gatedManagers.forEach { name ->
            val clz = findClassIfExists(name) ?: return@forEach
            if (clz.declaredMethods.none { it.name == "onNotificationAdded" }) return@forEach
            clz.beforeHookMethod("onNotificationAdded") {
                val sbn = it.args.filterIsInstance<StatusBarNotification>().firstOrNull()
                    ?: return@beforeHookMethod
                if (!isMediaNotification(sbn)) return@beforeHookMethod
                val key = it.args.filterIsInstance<String>().firstOrNull()
                // 复刻系统对非媒体通知的分支：清掉可能已存在的条目（onNotificationRemoved 幂等）后返回
                if (key != null) {
                    runCatching { it.thisObject.callMethod("onNotificationRemoved", key) }
                        .onFailure { XposedLog.e(TAG, lpparam.packageName, "hide media card: cleanup failed: ${it.message}") }
                }
                it.result = null
            }
            installed++
            XposedLog.i(TAG, lpparam.packageName, "hide media card: gate installed on $name")
        }

        // HyperOS 2 / AOSP 经典管线兜底：入口为 onNotificationPosted(sbn, token)
        val aospManager = findClassIfExists("com.android.systemui.media.MediaDataManagerImpl")
        if (aospManager?.declaredMethods?.any { it.name == "onNotificationPosted" } == true) {
            aospManager.beforeHookMethod("onNotificationPosted") {
                val sbn = it.args.filterIsInstance<StatusBarNotification>().firstOrNull()
                    ?: return@beforeHookMethod
                if (isMediaNotification(sbn)) it.result = null
            }
            installed++
            XposedLog.i(TAG, lpparam.packageName, "hide media card: gate installed on MediaDataManagerImpl")
        }

        if (installed == 0) {
            XposedLog.e(TAG, lpparam.packageName, "hide media card: no media data gate found, hook skipped")
        }
    }
}
