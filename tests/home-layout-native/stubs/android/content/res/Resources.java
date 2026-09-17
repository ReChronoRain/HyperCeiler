package android.content.res;

import android.util.DisplayMetrics;

/** Host stub: system_server's default display metrics. */
public class Resources {
    private static final Resources INSTANCE = new Resources();

    public static Resources getSystem() {
        return INSTANCE;
    }

    public DisplayMetrics getDisplayMetrics() {
        return new DisplayMetrics();
    }
}
