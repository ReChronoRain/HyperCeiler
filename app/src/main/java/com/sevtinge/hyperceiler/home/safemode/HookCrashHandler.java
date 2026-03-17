package com.sevtinge.hyperceiler.home.safemode;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.CrashIntentContract;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashScope;
import com.sevtinge.hyperceiler.libhook.safecrash.SafeModeHandler;

public final class HookCrashHandler {

    private static final String TAG = "HookCrashHandler";

    private HookCrashHandler() {}

    @Nullable
    public static CrashRecordStore.CrashRecord persist(@NonNull Context context, @NonNull Intent sourceIntent) {
        return CrashRecordStore.persistHookCrash(context, sourceIntent);
    }

    @NonNull
    public static Intent createCrashActivityIntent(@NonNull Context context, @NonNull Intent sourceIntent,
                                                   @Nullable CrashRecordStore.CrashRecord record) {
        Intent intent = new Intent(context, CrashActivity.class);
        intent.setAction("android.intent.action.Crash");

        if (record != null) {
            CrashRecordStore.fillIntent(intent, record);
        } else {
            copyLegacyExtras(sourceIntent, intent);
        }

        return intent;
    }

    public static void showCrashToast(@NonNull Context context, @Nullable CrashRecordStore.CrashRecord record,
                                      @Nullable String fallbackAlias) {
        String alias = record != null ? record.packageAlias : fallbackAlias;
        String pkgName = record != null && record.packageName != null
            ? record.packageName
            : CrashScope.INSTANCE.getPackageName(alias);
        String label = pkgName != null ? pkgName : alias;
        if (label != null && !label.isEmpty()) {
            Toast.makeText(context, "Crash detected: " + label, Toast.LENGTH_LONG).show();
        }
    }

    public static void ensureSafeModeProp(@Nullable CrashRecordStore.CrashRecord record, @Nullable String fallbackAlias) {
        String alias = record != null ? record.packageAlias : fallbackAlias;
        if (alias != null && !alias.isEmpty()) {
            SafeModeHandler.INSTANCE.updateCrashProp(alias);
        }
    }

    private static void copyLegacyExtras(@NonNull Intent sourceIntent, @NonNull Intent targetIntent) {
        targetIntent.putExtra(CrashIntentContract.KEY_LONG_MSG, sourceIntent.getStringExtra(CrashIntentContract.KEY_LONG_MSG));
        targetIntent.putExtra(CrashIntentContract.KEY_STACK_TRACE, sourceIntent.getStringExtra(CrashIntentContract.KEY_STACK_TRACE));
        targetIntent.putExtra(CrashIntentContract.KEY_THROW_CLASS, sourceIntent.getStringExtra(CrashIntentContract.KEY_THROW_CLASS));
        targetIntent.putExtra(CrashIntentContract.KEY_THROW_FILE, sourceIntent.getStringExtra(CrashIntentContract.KEY_THROW_FILE));
        targetIntent.putExtra(CrashIntentContract.KEY_THROW_LINE, sourceIntent.getIntExtra(CrashIntentContract.KEY_THROW_LINE, -1));
        targetIntent.putExtra(CrashIntentContract.KEY_THROW_METHOD, sourceIntent.getStringExtra(CrashIntentContract.KEY_THROW_METHOD));
        targetIntent.putExtra(CrashIntentContract.KEY_PKG_ALIAS, sourceIntent.getStringExtra(CrashIntentContract.KEY_PKG_ALIAS));
    }

    public static void launchCrashActivity(@NonNull Context context, @NonNull Intent activityIntent) {
        try {
            context.startActivity(activityIntent);
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to start CrashActivity", e);
        }
    }
}
