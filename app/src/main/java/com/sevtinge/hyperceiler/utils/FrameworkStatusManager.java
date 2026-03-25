package com.sevtinge.hyperceiler.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;

import io.github.libxposed.service.XposedService;

public final class FrameworkStatusManager {

    private static final String TAG = "FrameworkStatusManager";
    private static final String HELP_SECTION_GAP = "\n\n\n";
    public static final String PREF_KEY_ALLOW_HOOK = "framework_api_allow_hook";
    public static final String PREF_KEY_REASON = "framework_check_reason";
    public static final String PREF_KEY_NAME = "framework_check_name";
    public static final String PREF_KEY_VERSION = "framework_check_version";
    public static final String PREF_KEY_VERSION_CODE = "framework_check_version_code";
    public static final String PREF_KEY_API_VERSION = "framework_check_api_version";
    public static final String PREF_KEY_DETAIL = "framework_check_detail";
    public static final int MIN_REQUIRED_API_VERSION = 101;
    private static final int UNKNOWN_API_VERSION = -1;
    private static final long UNKNOWN_VERSION_CODE = -1L;
    private static volatile Status sCurrentStatus = Status.pending(true);

    private FrameworkStatusManager() {}

    public static void init() {
        ensureHookGateInitialized();
        sCurrentStatus = restoreStatus();
    }

    public static void onServiceBound(@NonNull XposedService service) {
        Status status = evaluateService(service);
        updateHookGate(status.isHookAllowed());
        persistStatus(status);
        sCurrentStatus = status;
    }

    public static void onServiceDied() {
        sCurrentStatus = restoreStatus();
    }

    @NonNull
    public static Status getCurrentStatus() {
        return sCurrentStatus;
    }

    public static boolean hasBlockingIssue() {
        return sCurrentStatus.hasBlockingIssue();
    }

    @SuppressLint("StringFormatInvalid")
    @Nullable
    public static String getBannerSummary(@NonNull Context context) {
        Status status = sCurrentStatus;
        return switch (status.getReason()) {
            case API_TOO_LOW -> context.getString(
                R.string.headtip_warn_unsupport_xposed,
                getDisplayFrameworkName(context, status),
                status.getFrameworkApiVersion(),
                MIN_REQUIRED_API_VERSION
            );
            case SERVICE_ERROR -> context.getString(R.string.headtip_warn_framework_service_error);
            default -> null;
        };
    }

    @NonNull
    public static String buildHelpSummary(@NonNull Context context) {
        Status status = sCurrentStatus;
        StringBuilder sb = new StringBuilder();
        sb.append(context.getString(R.string.help_framework_warning_section_framework_version));
        sb.append("\n\n");
        sb.append(buildFrameworkVersionText(context, status));
        sb.append(HELP_SECTION_GAP);
        sb.append(context.getString(R.string.help_framework_warning_section_banner_reason));
        sb.append("\n\n");
        sb.append(context.getString(R.string.help_framework_warning_banner_reason));
        return sb.toString();
    }

    private static void ensureHookGateInitialized() {
        SharedPreferences prefs = PrefsBridge.getSharedPreferences();
        if (prefs == null || prefs.contains("prefs_key_" + PREF_KEY_ALLOW_HOOK)) {
            return;
        }
        PrefsBridge.putByApp(PREF_KEY_ALLOW_HOOK, true);
    }

    @NonNull
    private static Status restoreStatus() {
        boolean hookAllowed = PrefsBridge.getBoolean(PREF_KEY_ALLOW_HOOK, true);
        String storedReason = PrefsBridge.getString(PREF_KEY_REASON, null);
        if (TextUtils.isEmpty(storedReason)) {
            return Status.pending(hookAllowed);
        }

        Reason reason;
        try {
            reason = Reason.valueOf(storedReason);
        } catch (IllegalArgumentException e) {
            AndroidLog.w(TAG, "Unknown stored framework check reason: " + storedReason, e);
            return Status.pending(hookAllowed);
        }

        String frameworkName = normalizeFrameworkText(PrefsBridge.getString(PREF_KEY_NAME, null));
        String frameworkVersion = normalizeFrameworkText(PrefsBridge.getString(PREF_KEY_VERSION, null));
        long frameworkVersionCode = PrefsBridge.getLong(PREF_KEY_VERSION_CODE, UNKNOWN_VERSION_CODE);
        int frameworkApiVersion = PrefsBridge.getInt(PREF_KEY_API_VERSION, UNKNOWN_API_VERSION);
        String detail = normalizeFrameworkText(PrefsBridge.getString(PREF_KEY_DETAIL, null));

        Status status = switch (reason) {
            case COMPATIBLE -> Status.compatible(
                frameworkName,
                frameworkVersion,
                frameworkVersionCode,
                frameworkApiVersion
            );
            case API_TOO_LOW -> Status.apiTooLow(
                frameworkName,
                frameworkVersion,
                frameworkVersionCode,
                frameworkApiVersion
            );
            case SERVICE_ERROR -> Status.serviceError(
                frameworkName,
                frameworkVersion,
                frameworkVersionCode,
                detail
            );
            case PENDING -> Status.pending(hookAllowed);
        };
        return status.withHookAllowed(hookAllowed);
    }

    @NonNull
    private static Status evaluateService(@NonNull XposedService service) {
        String frameworkName = readFrameworkName(service);
        String frameworkVersion = readFrameworkVersion(service);
        long frameworkVersionCode = readFrameworkVersionCode(service);
        ApiReadResult apiReadResult = readFrameworkApiVersion(service);
        int frameworkApiVersion = apiReadResult.apiVersion;

        if (frameworkApiVersion == UNKNOWN_API_VERSION) {
            return Status.serviceError(
                frameworkName,
                frameworkVersion,
                frameworkVersionCode,
                apiReadResult.detail
            );
        }

        if (frameworkApiVersion < MIN_REQUIRED_API_VERSION) {
            return Status.apiTooLow(
                frameworkName,
                frameworkVersion,
                frameworkVersionCode,
                frameworkApiVersion
            );
        }

        return Status.compatible(
            frameworkName,
            frameworkVersion,
            frameworkVersionCode,
            frameworkApiVersion
        );
    }

    @Nullable
    private static String readFrameworkName(@NonNull XposedService service) {
        try {
            return normalizeFrameworkText(service.getFrameworkName());
        } catch (Throwable t) {
            AndroidLog.w(TAG, "Failed to read framework name.", t);
            return null;
        }
    }

    @Nullable
    private static String readFrameworkVersion(@NonNull XposedService service) {
        try {
            return normalizeFrameworkText(service.getFrameworkVersion());
        } catch (Throwable t) {
            AndroidLog.w(TAG, "Failed to read framework version.", t);
            return null;
        }
    }

    private static long readFrameworkVersionCode(@NonNull XposedService service) {
        try {
            return service.getFrameworkVersionCode();
        } catch (Throwable t) {
            AndroidLog.w(TAG, "Failed to read framework version code.", t);
            return UNKNOWN_VERSION_CODE;
        }
    }

    @NonNull
    private static ApiReadResult readFrameworkApiVersion(@NonNull XposedService service) {
        try {
            return new ApiReadResult(service.getApiVersion(), null);
        } catch (Throwable t) {
            AndroidLog.e(TAG, "Failed to read framework API version.", t);
            return new ApiReadResult(UNKNOWN_API_VERSION, contextlessDetail(t));
        }
    }

    @Nullable
    private static String contextlessDetail(@Nullable Throwable t) {
        if (t == null) {
            return null;
        }
        String message = t.getMessage();
        if (TextUtils.isEmpty(message)) {
            return t.getClass().getSimpleName();
        }
        return t.getClass().getSimpleName() + ": " + message;
    }

    private static void updateHookGate(boolean allowHook) {
        if (PrefsBridge.getBoolean(PREF_KEY_ALLOW_HOOK, true) != allowHook) {
            PrefsBridge.putByApp(PREF_KEY_ALLOW_HOOK, allowHook);
            return;
        }
        SharedPreferences prefs = PrefsBridge.getSharedPreferences();
        if (prefs != null && !prefs.contains("prefs_key_" + PREF_KEY_ALLOW_HOOK)) {
            PrefsBridge.putByApp(PREF_KEY_ALLOW_HOOK, allowHook);
        }
    }

    private static void persistStatus(@NonNull Status status) {
        PrefsBridge.putByApp(PREF_KEY_REASON, status.getReason().name());
        PrefsBridge.putByApp(PREF_KEY_NAME, status.getFrameworkName());
        PrefsBridge.putByApp(PREF_KEY_VERSION, status.getFrameworkVersion());
        PrefsBridge.putByApp(PREF_KEY_VERSION_CODE, status.getFrameworkVersionCode());
        PrefsBridge.putByApp(PREF_KEY_API_VERSION, status.getFrameworkApiVersion());
        PrefsBridge.putByApp(PREF_KEY_DETAIL, status.getDetail());
    }

    @NonNull
    private static String getDisplayFrameworkName(@NonNull Context context, @NonNull Status status) {
        return getDisplayText(
            status.getFrameworkName(),
            context.getString(R.string.help_framework_warning_unknown_framework)
        );
    }

    @NonNull
    private static String buildFrameworkVersionText(@NonNull Context context, @NonNull Status status) {
        if (status.getFrameworkName() == null
            && status.getFrameworkVersion() == null
            && status.getFrameworkVersionCode() < 0
            && status.getFrameworkApiVersion() < 0) {
            return context.getString(R.string.help_framework_warning_version_unavailable);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getDisplayFrameworkName(context, status));

        if (!TextUtils.isEmpty(status.getFrameworkVersion())) {
            sb.append(" ");
            sb.append(status.getFrameworkVersion());
        }

        if (status.getFrameworkVersionCode() >= 0) {
            sb.append(" (");
            sb.append(status.getFrameworkVersionCode());
            sb.append(")");
        }

        if (status.getFrameworkApiVersion() >= 0) {
            sb.append(", API ");
            sb.append(status.getFrameworkApiVersion());
        }

        return sb.toString();
    }

    @NonNull
    private static String getDisplayText(@Nullable String value, @NonNull String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    @Nullable
    private static String normalizeFrameworkText(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String trimValue = value.trim();
        return trimValue.isEmpty() ? null : trimValue;
    }

    public enum Reason {
        PENDING,
        COMPATIBLE,
        API_TOO_LOW,
        SERVICE_ERROR
    }

    public static final class Status {
        private final Reason reason;
        private final String frameworkName;
        private final String frameworkVersion;
        private final long frameworkVersionCode;
        private final int frameworkApiVersion;
        private final String detail;
        private final boolean hookAllowed;

        private Status(
            @NonNull Reason reason,
            @Nullable String frameworkName,
            @Nullable String frameworkVersion,
            long frameworkVersionCode,
            int frameworkApiVersion,
            @Nullable String detail,
            boolean hookAllowed
        ) {
            this.reason = reason;
            this.frameworkName = frameworkName;
            this.frameworkVersion = frameworkVersion;
            this.frameworkVersionCode = frameworkVersionCode;
            this.frameworkApiVersion = frameworkApiVersion;
            this.detail = detail;
            this.hookAllowed = hookAllowed;
        }

        @NonNull
        public static Status pending(boolean hookAllowed) {
            return new Status(
                Reason.PENDING,
                null,
                null,
                UNKNOWN_VERSION_CODE,
                UNKNOWN_API_VERSION,
                null,
                hookAllowed
            );
        }

        @NonNull
        public static Status compatible(
            @Nullable String frameworkName,
            @Nullable String frameworkVersion,
            long frameworkVersionCode,
            int frameworkApiVersion
        ) {
            return new Status(
                Reason.COMPATIBLE,
                frameworkName,
                frameworkVersion,
                frameworkVersionCode,
                frameworkApiVersion,
                null,
                true
            );
        }

        @NonNull
        public static Status apiTooLow(
            @Nullable String frameworkName,
            @Nullable String frameworkVersion,
            long frameworkVersionCode,
            int frameworkApiVersion
        ) {
            return new Status(
                Reason.API_TOO_LOW,
                frameworkName,
                frameworkVersion,
                frameworkVersionCode,
                frameworkApiVersion,
                null,
                false
            );
        }

        @NonNull
        public static Status serviceError(
            @Nullable String frameworkName,
            @Nullable String frameworkVersion,
            long frameworkVersionCode,
            @Nullable String detail
        ) {
            return new Status(
                Reason.SERVICE_ERROR,
                frameworkName,
                frameworkVersion,
                frameworkVersionCode,
                UNKNOWN_API_VERSION,
                detail,
                false
            );
        }

        @NonNull
        public Status withHookAllowed(boolean hookAllowed) {
            return new Status(
                reason,
                frameworkName,
                frameworkVersion,
                frameworkVersionCode,
                frameworkApiVersion,
                detail,
                hookAllowed
            );
        }

        @NonNull
        public Reason getReason() {
            return reason;
        }

        @Nullable
        public String getFrameworkName() {
            return frameworkName;
        }

        @Nullable
        public String getFrameworkVersion() {
            return frameworkVersion;
        }

        public long getFrameworkVersionCode() {
            return frameworkVersionCode;
        }

        public int getFrameworkApiVersion() {
            return frameworkApiVersion;
        }

        @Nullable
        public String getDetail() {
            return detail;
        }

        public boolean isHookAllowed() {
            return hookAllowed;
        }

        public boolean hasBlockingIssue() {
            return reason == Reason.API_TOO_LOW || reason == Reason.SERVICE_ERROR;
        }

    }

    private record ApiReadResult(int apiVersion, String detail) {
        private ApiReadResult(int apiVersion, @Nullable String detail) {
            this.apiVersion = apiVersion;
            this.detail = detail;
        }
    }
}
