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
package com.sevtinge.hyperceiler.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.core.R;

import java.util.LinkedHashSet;
import java.util.List;

import io.github.libxposed.service.HookedTarget;

import fan.appcompat.app.AlertDialog;

/** API 102 的 scope 应用多选与重载结果 Toast。 */
public final class HotReloadDialogHelper {

    private HotReloadDialogHelper() {
    }

    /**
     * 打开 framework scope 应用的多选框。
     *
     * @param preferredPackage 当前功能页对应的应用；仍在 scope 时排第一并默认选中
     */
    public static void showScopedAppPicker(@NonNull Activity activity,
                                           @Nullable String preferredPackage) {
        if (!HotReloadManager.isHotReloadAvailable()) {
            new AlertDialog.Builder(activity)
                .setTitle(R.string.settings_hot_reload)
                .setMessage(R.string.settings_hot_reload_unsupported)
                .setPositiveButton(android.R.string.ok, null)
                .show();
            return;
        }

        ThreadUtils.postOnBackgroundThread(() -> {
            List<HotReloadManager.ScopeApp> apps = HotReloadManager.getScopedApps(activity, preferredPackage);
            ThreadUtils.postOnMainThread(() -> showPickerOnMain(activity, apps));
        });
    }

    private static void showPickerOnMain(@NonNull Activity activity,
                                         @NonNull List<HotReloadManager.ScopeApp> apps) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (apps.isEmpty()) {
            new AlertDialog.Builder(activity)
                .setTitle(R.string.settings_hot_reload)
                .setMessage(R.string.settings_hot_reload_no_scoped_apps)
                .setPositiveButton(android.R.string.ok, null)
                .show();
            return;
        }

        String[] labels = new String[apps.size()];
        boolean[] selected = new boolean[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            HotReloadManager.ScopeApp app = apps.get(i);
            // 选择器只展示应用名称；包名和进程数属于诊断信息，不应干扰日常选择。
            labels[i] = app.getDisplayName();
            selected[i] = app.isPreferred();
        }

        AlertDialog picker = new AlertDialog.Builder(activity)
            .setTitle(R.string.settings_hot_reload_pick_apps)
            .setMessage(R.string.settings_hot_reload_pick_apps_subtitle)
            .setMultiChoiceItems(labels, selected, (dialog, which, checked) -> selected[which] = checked)
            .setPositiveButton(R.string.settings_hot_reload_action, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        picker.setOnShowListener(ignored -> picker.getButton(DialogInterface.BUTTON_POSITIVE)
            .setOnClickListener(view -> {
                LinkedHashSet<String> packages = new LinkedHashSet<>();
                for (int i = 0; i < apps.size(); i++) {
                    if (selected[i]) {
                        packages.add(apps.get(i).getPackageName());
                    }
                }
                if (packages.isEmpty()) {
                    Toast.makeText(activity, R.string.settings_hot_reload_no_apps_selected,
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                picker.dismiss();
                Toast.makeText(activity,
                    activity.getString(R.string.settings_hot_reload_batch_in_progress, packages.size()),
                    Toast.LENGTH_SHORT).show();
                LinkedHashSet<String> requestedPackages = new LinkedHashSet<>(packages);
                ThreadUtils.postOnBackgroundThread(() ->
                    HotReloadManager.hotReloadPackages(requestedPackages,
                        result -> showResultToast(activity, requestedPackages, result))
                );
            })
        );
        picker.show();
    }

    private static void showResultToast(@NonNull Activity activity,
                                        @NonNull LinkedHashSet<String> requestedPackages,
                                        @NonNull HotReloadManager.BatchResult result) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        int targetCount = result.getRequestedTargetCount();
        String message;
        if (targetCount == 0) {
            message = activity.getString(
                R.string.settings_hot_reload_batch_no_running_targets,
                result.getSelectedAppCount()
            );
        } else {
            message = activity.getString(
                result.isTimedOut()
                    ? R.string.settings_hot_reload_batch_result_timeout
                    : R.string.settings_hot_reload_batch_result,
                result.getSucceededTargetCount(),
                targetCount,
                result.getFailedTargetCount(),
                result.getNoRunningTargetPackages().size()
            );
        }
        HotReloadManager.TargetResult firstFailure = result.getFirstFailedTargetResult();
        if (firstFailure != null) {
            HookedTarget target = firstFailure.getTarget();
            String processName = target.getProcessName();
            if (processName == null || processName.isEmpty()) {
                processName = String.valueOf(target.getPid());
            }
            message += '\n' + activity.getString(
                R.string.settings_hot_reload_batch_failure_detail,
                processName,
                shorten(firstFailure.getMessage())
            );
        }
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        showRestartFallbackIfNeeded(activity, requestedPackages, result);
    }

    /**
     * 只在 framework 明确返回“本次热重载失败”时才回退到原有硬重启。
     *
     * <p>超时或 IN_PROGRESS 不能证明失败，不能贸然 kill；而 FAILED / UNSUPPORTED /
     * SERVICE_ERROR 已经明确没有完成安全切换，此时重启全部相关进程才不会留下旧 generation。</p>
     */
    private static void showRestartFallbackIfNeeded(@NonNull Activity activity,
                                                    @NonNull LinkedHashSet<String> requestedPackages,
                                                    @NonNull HotReloadManager.BatchResult result) {
        LinkedHashSet<String> restartPackages = new LinkedHashSet<>();
        boolean restartSystem = false;
        for (HotReloadManager.TargetResult targetResult : result.getTargetResults()) {
            if (!requiresRestartFallback(targetResult.getCode())) {
                continue;
            }
            String packageName = findRequestedPackage(targetResult.getTarget(), requestedPackages);
            if (packageName == null) {
                continue;
            }
            if ("system".equals(packageName)) {
                restartSystem = true;
            } else {
                restartPackages.add(packageName);
            }
        }

        if (restartSystem) {
            // system_server 不能安全地仅杀单个进程，沿用已有的重启设备确认路径。
            DialogHelper.showRestartSystemDialog(activity);
        } else if (!restartPackages.isEmpty()) {
            // AppsTool 会按包名前缀结束该应用的所有进程。
            DialogHelper.showRestartDialog(activity,
                activity.getString(R.string.settings_hot_reload),
                restartPackages.toArray(new String[0]));
        }
    }

    private static boolean requiresRestartFallback(@NonNull HotReloadManager.ResultCode code) {
        return code == HotReloadManager.ResultCode.FAILED
            || code == HotReloadManager.ResultCode.UNSUPPORTED
            || code == HotReloadManager.ResultCode.SERVICE_ERROR;
    }

    @Nullable
    private static String findRequestedPackage(@NonNull HookedTarget target,
                                               @NonNull LinkedHashSet<String> requestedPackages) {
        String processName = target.getProcessName();
        if (processName == null || processName.isEmpty()) {
            return null;
        }
        for (String packageName : requestedPackages) {
            if ("system".equals(packageName)
                && ("system".equals(processName) || "system_server".equals(processName))) {
                return packageName;
            }
            if (processName.equals(packageName) || processName.startsWith(packageName + ':')) {
                return packageName;
            }
        }
        return null;
    }

    @NonNull
    private static String shorten(@NonNull String value) {
        final int maxLength = 180;
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + '\u2026';
    }
}
