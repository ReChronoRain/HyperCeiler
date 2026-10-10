package android.content;

import android.content.pm.PackageManager;

/** Minimal host stub: the endpoint only asks the context for its content resolver. */
public class Context {
    public PackageManager getPackageManager() {
        return new PackageManager();
    }
    public ContentResolver getContentResolver() {
        return new ContentResolver();
    }
}
