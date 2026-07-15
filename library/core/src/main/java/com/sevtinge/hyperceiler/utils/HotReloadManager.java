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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.AndroidLog;

import io.github.libxposed.service.HookedTarget;
import io.github.libxposed.service.HotReloadResult;
import io.github.libxposed.service.XposedService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 热重载管理器（libxposed API 102 能力封装）。
 *
 * <p>提供：</p>
 * <ul>
 *     <li>{@link #isHotReloadAvailable()}：检测当前 framework 是否支持热重载（service API >= 102）；</li>
 *     <li>{@link #getRunningTargets()}：列出本模块当前已注入的目标进程；</li>
 *     <li>{@link #hotReloadByPackage(String, HotReloadCallback)} / {@link #hotReloadAll(HotReloadCallback)}：
 *         按包名 / 全部目标发起一次显式热重载请求；</li>
 *     <li>所有结果回调在主线程派发。</li>
 * </ul>
 */
public final class HotReloadManager {

    private static final String TAG = "HotReloadManager";
    /** Framework 回调异常丢失时，不能让页面永远停留在“正在重载”。 */
    private static final long HOT_RELOAD_TIMEOUT_MS = 15_000L;

    private static final Handler sMain = new Handler(Looper.getMainLooper());

    private HotReloadManager() {
    }

    /**
     * 当前 framework 是否支持热重载（即 XposedService API >= 102 且 service 已绑定）。
     */
    public static boolean isHotReloadAvailable() {
        XposedService service = ScopeManager.getService();
        if (service == null) return false;
        try {
            return service.getApiVersion() >= XposedService.API_102;
        } catch (Throwable t) {
            AndroidLog.w(TAG, "isHotReloadAvailable failed", t);
            return false;
        }
    }

    /**
     * 获取当前模块已注入的运行中目标进程列表。
     *
     * @return 不可修改的列表；当 framework 不支持或调用失败时返回空列表
     */
    @NonNull
    public static List<HookedTarget> getRunningTargets() {
        XposedService service = ScopeManager.getService();
        if (service == null) return Collections.emptyList();
        try {
            if (service.getApiVersion() < XposedService.API_102) {
                return Collections.emptyList();
            }
            return service.getRunningTargets();
        } catch (UnsupportedOperationException uoe) {
            AndroidLog.w(TAG, "getRunningTargets unsupported: " + uoe.getMessage());
            return Collections.emptyList();
        } catch (Throwable t) {
            AndroidLog.w(TAG, "getRunningTargets failed", t);
            return Collections.emptyList();
        }
    }

    /**
     * 按包名筛选并触发热重载。若同包名下有多个进程都注入了本模块，会全部触发。
     *
     * @param packageName 目标包名（不可为空）
     * @param callback    回调（main thread）
     * @return 是否成功提交至少一个热重载请求
     */
    public static boolean hotReloadByPackage(@NonNull String packageName,
                                             @NonNull HotReloadCallback callback) {
        List<HookedTarget> all = getRunningTargets();
        if (all.isEmpty()) {
            AndroidLog.w(TAG, "hotReloadByPackage(" + packageName + "): no running targets");
            postNotAvailable(callback);
            return false;
        }

        // 调试用：把当前所有 target 的 processName 打印出来，便于在 LSPosed/不同框架下
        // 排查 system_server 别名问题
        StringBuilder sb = new StringBuilder("hotReloadByPackage(").append(packageName)
            .append("): available targets =");
        for (HookedTarget t : all) {
            sb.append(" [").append(t.getProcessName())
              .append(" pid=").append(t.getPid())
              .append(" state=").append(t.getState()).append("]");
        }
        AndroidLog.d(TAG, sb.toString());

        int submitted = 0;
        for (HookedTarget target : all) {
            if (matchesPackage(target, packageName)) {
                hotReloadTarget(target, null, callback);
                submitted++;
            }
        }
        if (submitted == 0) {
            AndroidLog.w(TAG, "hotReloadByPackage(" + packageName + "): no matching target");
            postNoMatch(callback, packageName);
            return false;
        }
        return true;
    }

    /** 对所有运行中的目标进程触发一次热重载。 */
    public static boolean hotReloadAll(@NonNull HotReloadCallback callback) {
        List<HookedTarget> all = getRunningTargets();
        if (all.isEmpty()) {
            postNotAvailable(callback);
            return false;
        }
        for (HookedTarget target : all) {
            hotReloadTarget(target, null, callback);
        }
        return true;
    }

    /**
     * 返回当前 framework scope 内、仍可在设备上管理的应用。
     *
     * <p>这与 {@link #getRunningTargets()} 刻意分开：选择器必须展示 framework 已勾选的应用，
     * 即使其中某个应用暂时没有运行中的已注入进程；实际提交时会在结果中明确报告该情况。</p>
     *
     * @param preferredPackage 当前功能页对应的应用；若它仍在 scope 中，会排在列表最前面
     */
    @NonNull
    public static List<ScopeApp> getScopedApps(@NonNull Context context,
                                                @Nullable String preferredPackage) {
        List<String> scope = ScopeManager.getScopeSync();
        if (scope == null || scope.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedPreferred = ScopeManager.normalizeScopePackageName(preferredPackage);
        LinkedHashSet<String> packages = ScopeManager.normalizeScopePackages(scope);
        List<ScopeApp> apps = new ArrayList<>(packages.size());
        for (String packageName : packages) {
            if (!ScopeManager.isScopePackageInstalled(context, packageName)) {
                continue;
            }
            boolean isSystem = ScopeManager.isSystemScopePackage(packageName);
            String label = isSystem
                ? context.getString(com.sevtinge.hyperceiler.core.R.string.settings_hot_reload_system_scope)
                : PackagesUtils.getAppLabel(context, packageName);
            if (TextUtils.isEmpty(label)) {
                label = packageName;
            }
            apps.add(new ScopeApp(
                packageName,
                label,
                packageName.equals(normalizedPreferred)
            ));
        }
        apps.sort(Comparator
            .comparing(ScopeApp::isPreferred).reversed()
            .thenComparing(app -> app.getDisplayName().toLowerCase(Locale.ROOT))
            .thenComparing(ScopeApp::getPackageName));
        return Collections.unmodifiableList(apps);
    }

    /**
     * 对多个 scope 应用发起一次批量热重载。每个应用的所有当前已注入进程都会被提交，
     * 最终只回调一次汇总结果。
     */
    public static void hotReloadPackages(@NonNull Collection<String> packageNames,
                                         @NonNull HotReloadBatchCallback callback) {
        LinkedHashSet<String> selectedPackages = ScopeManager.normalizeScopePackages(packageNames);
        if (selectedPackages.isEmpty()) {
            postBatchResult(callback, new BatchResult(
                0, Collections.emptyList(), Collections.emptyList(), false
            ));
            return;
        }

        List<HookedTarget> allTargets = getRunningTargets();
        Map<String, HookedTarget> targets = new LinkedHashMap<>();
        List<String> noRunningTargets = new ArrayList<>();
        for (String packageName : selectedPackages) {
            boolean matched = false;
            for (HookedTarget target : allTargets) {
                if (!matchesPackage(target, packageName)) {
                    continue;
                }
                matched = true;
                targets.putIfAbsent(targetKey(target), target);
            }
            if (!matched) {
                noRunningTargets.add(packageName);
            }
        }

        if (targets.isEmpty()) {
            postBatchResult(callback, new BatchResult(
                selectedPackages.size(), Collections.emptyList(), noRunningTargets, false
            ));
            return;
        }

        BatchTracker tracker = new BatchTracker(
            selectedPackages.size(),
            new ArrayList<>(targets.values()),
            noRunningTargets,
            callback
        );
        for (HookedTarget target : targets.values()) {
            hotReloadTarget(target, null, tracker::onTargetResult);
        }
        sMain.postDelayed(tracker::onTimeout, HOT_RELOAD_TIMEOUT_MS);
    }

    /**
     * 直接对指定目标触发热重载。
     *
     * @param target   目标（来自 {@link #getRunningTargets()}）
     * @param extras   附加数据（可空）
     * @param callback 回调（main thread）
     */
    public static void hotReloadTarget(@NonNull HookedTarget target,
                                       @Nullable Bundle extras,
                                       @NonNull HotReloadCallback callback) {
        XposedService service = ScopeManager.getService();
        if (service == null) {
            postResult(callback, target, ResultCode.SERVICE_UNAVAILABLE, "Xposed service not bound");
            return;
        }
        try {
            service.hotReloadModule(target, extras, (t, result) ->
                postResult(callback, t, ResultCode.fromStatus(result.status()), result.message()));
        } catch (UnsupportedOperationException uoe) {
            postResult(callback, target, ResultCode.UNSUPPORTED, uoe.getMessage());
        } catch (SecurityException se) {
            postResult(callback, target, ResultCode.INVALID_TARGET, se.getMessage());
        } catch (Throwable t) {
            postResult(callback, target, ResultCode.SERVICE_ERROR, t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static void postBatchResult(@NonNull HotReloadBatchCallback callback,
                                        @NonNull BatchResult result) {
        sMain.post(() -> callback.onCompleted(result));
    }

    @NonNull
    private static String targetKey(@NonNull HookedTarget target) {
        return target.getUid() + ":" + target.getPid() + ":" + target.getProcessName();
    }

    private static boolean matchesPackage(@NonNull HookedTarget target, @NonNull String packageName) {
        String processName = target.getProcessName();
        if (processName == null) return false;
        if (processName.equals(packageName)) return true;

        // system_server 别名：LSPosed / 不同 framework 实现历史上对 system_server 的进程名
        // 写法不一致，常见的有 "system_server"（标准 Android 进程名）和 "system"
        // （HyperCeiler / 部分 framework 内部约定）。两个互为别名。
        // 注意 "android" 是 framework 自身的 manifest 包名，<b>不是</b> 进程名，不在此别名集合内。
        if (isSystemServerName(packageName) && isSystemServerName(processName)) {
            return true;
        }

        // 多进程 (例如 com.miui.home:wallpaper) 也算
        int colon = processName.indexOf(':');
        if (colon > 0) {
            return processName.substring(0, colon).equals(packageName);
        }
        return false;
    }

    private static boolean isSystemServerName(@NonNull String name) {
        return "system_server".equals(name) || "system".equals(name);
    }

    private static void postNotAvailable(@NonNull HotReloadCallback callback) {
        sMain.post(() -> callback.onResult(null, ResultCode.SERVICE_UNAVAILABLE,
            "Hot reload service unavailable or no running targets"));
    }

    private static void postNoMatch(@NonNull HotReloadCallback callback, @NonNull String pkg) {
        sMain.post(() -> callback.onResult(null, ResultCode.NO_MATCHING_TARGET,
            "No running hooked target matches package: " + pkg));
    }

    private static void postResult(@NonNull HotReloadCallback callback,
                                   @Nullable HookedTarget target,
                                   @NonNull ResultCode code,
                                   @Nullable String message) {
        String detail = normalizeMessage(target, code, message);
        sMain.post(() -> callback.onResult(target, code, detail));
    }

    @NonNull
    private static String normalizeMessage(@Nullable HookedTarget target,
                                           @NonNull ResultCode code,
                                           @Nullable String message) {
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }

        String targetState = target == null ? "" : " Target state: " + target.getState().name() + ".";
        return switch (code) {
            case SUCCEEDED -> "Hot reload completed.";
            case FAILED -> "Framework reported FAILED without details. "
                + "The module rejected the transition or a hook failed while the new generation was initializing."
                + targetState;
            case UNSUPPORTED -> "Hot reload is not supported by the current framework.";
            case IN_PROGRESS -> "The target process is already reloading." + targetState;
            case PROCESS_DIED -> "The target process died during hot reload." + targetState;
            case INVALID_TARGET -> "The target process is invalid or no longer running." + targetState;
            case SERVICE_UNAVAILABLE -> "Xposed service is unavailable.";
            case SERVICE_ERROR -> "Xposed service call failed without details.";
            case NO_MATCHING_TARGET -> "No matching hooked target is running.";
            case TIMED_OUT -> "Timed out waiting for the framework hot reload callback.";
        };
    }

    /** scope 应用在选择器中显示的数据。 */
    public static final class ScopeApp {
        @NonNull
        private final String mPackageName;
        @NonNull
        private final String mDisplayName;
        private final boolean mPreferred;

        ScopeApp(@NonNull String packageName, @NonNull String displayName, boolean preferred) {
            mPackageName = packageName;
            mDisplayName = displayName;
            mPreferred = preferred;
        }

        @NonNull
        public String getPackageName() {
            return mPackageName;
        }

        @NonNull
        public String getDisplayName() {
            return mDisplayName;
        }

        public boolean isPreferred() {
            return mPreferred;
        }
    }

    /** 单个目标进程的最终回执。 */
    public static final class TargetResult {
        @NonNull
        private final HookedTarget mTarget;
        @NonNull
        private final ResultCode mCode;
        @NonNull
        private final String mMessage;

        TargetResult(@NonNull HookedTarget target, @NonNull ResultCode code,
                     @Nullable String message) {
            mTarget = target;
            mCode = code;
            mMessage = normalizeMessage(target, code, message);
        }

        @NonNull
        public HookedTarget getTarget() {
            return mTarget;
        }

        @NonNull
        public ResultCode getCode() {
            return mCode;
        }

        @NonNull
        public String getMessage() {
            return mMessage;
        }
    }

    /** 一次多应用重载的汇总回执。 */
    public static final class BatchResult {
        private final int mSelectedAppCount;
        @NonNull
        private final List<TargetResult> mTargetResults;
        @NonNull
        private final List<String> mNoRunningTargetPackages;
        private final boolean mTimedOut;

        BatchResult(int selectedAppCount, @NonNull List<TargetResult> targetResults,
                    @NonNull List<String> noRunningTargetPackages, boolean timedOut) {
            mSelectedAppCount = selectedAppCount;
            mTargetResults = Collections.unmodifiableList(new ArrayList<>(targetResults));
            mNoRunningTargetPackages = Collections.unmodifiableList(
                new ArrayList<>(noRunningTargetPackages));
            mTimedOut = timedOut;
        }

        public int getSelectedAppCount() {
            return mSelectedAppCount;
        }

        public int getRequestedTargetCount() {
            return mTargetResults.size();
        }

        public int getSucceededTargetCount() {
            int succeeded = 0;
            for (TargetResult result : mTargetResults) {
                if (result.getCode() == ResultCode.SUCCEEDED) {
                    succeeded++;
                }
            }
            return succeeded;
        }

        public int getFailedTargetCount() {
            return getRequestedTargetCount() - getSucceededTargetCount();
        }

        @Nullable
        public TargetResult getFirstFailedTargetResult() {
            for (TargetResult result : mTargetResults) {
                if (result.getCode() != ResultCode.SUCCEEDED) {
                    return result;
                }
            }
            return null;
        }

        @NonNull
        public List<TargetResult> getTargetResults() {
            return mTargetResults;
        }

        @NonNull
        public List<String> getNoRunningTargetPackages() {
            return mNoRunningTargetPackages;
        }

        public boolean isTimedOut() {
            return mTimedOut;
        }
    }

    private static final class BatchTracker {
        private final int mSelectedAppCount;
        @NonNull
        private final List<HookedTarget> mTargets;
        @NonNull
        private final List<String> mNoRunningTargetPackages;
        @NonNull
        private final HotReloadBatchCallback mCallback;
        @NonNull
        private final Map<String, TargetResult> mResults = new LinkedHashMap<>();
        private boolean mFinished;

        BatchTracker(int selectedAppCount, @NonNull List<HookedTarget> targets,
                     @NonNull List<String> noRunningTargetPackages,
                     @NonNull HotReloadBatchCallback callback) {
            mSelectedAppCount = selectedAppCount;
            mTargets = targets;
            mNoRunningTargetPackages = noRunningTargetPackages;
            mCallback = callback;
        }

        @MainThread
        void onTargetResult(@Nullable HookedTarget target, @NonNull ResultCode code,
                            @Nullable String message) {
            if (mFinished || target == null) {
                return;
            }
            String key = targetKey(target);
            if (!containsTarget(key) || mResults.containsKey(key)) {
                return;
            }
            mResults.put(key, new TargetResult(target, code, message));
            if (mResults.size() == mTargets.size()) {
                complete(false);
            }
        }

        @MainThread
        void onTimeout() {
            if (mFinished) {
                return;
            }
            for (HookedTarget target : mTargets) {
                String key = targetKey(target);
                if (!mResults.containsKey(key)) {
                    mResults.put(key, new TargetResult(target, ResultCode.TIMED_OUT, null));
                }
            }
            complete(true);
        }

        private boolean containsTarget(@NonNull String key) {
            for (HookedTarget target : mTargets) {
                if (targetKey(target).equals(key)) {
                    return true;
                }
            }
            return false;
        }

        @MainThread
        private void complete(boolean timedOut) {
            if (mFinished) {
                return;
            }
            mFinished = true;
            List<TargetResult> orderedResults = new ArrayList<>(mTargets.size());
            for (HookedTarget target : mTargets) {
                TargetResult result = mResults.get(targetKey(target));
                if (result != null) {
                    orderedResults.add(result);
                }
            }
            mCallback.onCompleted(new BatchResult(
                mSelectedAppCount, orderedResults, mNoRunningTargetPackages, timedOut
            ));
        }
    }

    public enum ResultCode {
        SUCCEEDED,
        FAILED,
        UNSUPPORTED,
        IN_PROGRESS,
        PROCESS_DIED,
        INVALID_TARGET,
        SERVICE_UNAVAILABLE,
        SERVICE_ERROR,
        NO_MATCHING_TARGET,
        TIMED_OUT;

        static ResultCode fromStatus(HotReloadResult.Status status) {
            return switch (status) {
                case SUCCEEDED -> SUCCEEDED;
                case FAILED -> FAILED;
                case UNSUPPORTED -> UNSUPPORTED;
                case IN_PROGRESS -> IN_PROGRESS;
                case PROCESS_DIED -> PROCESS_DIED;
            };
        }
    }

    @FunctionalInterface
    public interface HotReloadCallback {
        /**
         * @param target  目标（提交失败时为 null）
         * @param code    结果码
         * @param message 详细信息（可空）
         */
        @MainThread
        void onResult(@Nullable HookedTarget target, @NonNull ResultCode code, @Nullable String message);
    }

    @FunctionalInterface
    public interface HotReloadBatchCallback {
        /** 在所有目标进程返回结果，或回调超时后调用；始终运行在主线程。 */
        @MainThread
        void onCompleted(@NonNull BatchResult result);
    }
}
