package android.content;

import android.net.Uri;

public record Intent(String getAction, Uri getData) {
    public static final String ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    public static final String ACTION_PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";
    public static final String ACTION_PACKAGE_REMOVED = "android.intent.action.PACKAGE_REMOVED";
    public static final String ACTION_PACKAGE_DATA_CLEARED = "android.intent.action.PACKAGE_DATA_CLEARED";
    public static final String EXTRA_REPLACING = "android.intent.extra.REPLACING";

    public boolean getBooleanExtra(String key, boolean defaultValue) { return defaultValue; }
}
