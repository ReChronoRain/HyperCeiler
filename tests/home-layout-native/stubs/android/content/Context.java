package android.content;

/** Minimal host stub: the endpoint only asks the context for its content resolver. */
public class Context {
    public android.content.pm.PackageManager getPackageManager() {
        return new android.content.pm.PackageManager();
    }
    public ContentResolver getContentResolver() {
        return new ContentResolver();
    }
}
