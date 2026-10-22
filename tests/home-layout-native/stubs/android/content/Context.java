package android.content;

import android.content.pm.PackageManager;

/** Minimal host stub: the endpoint only asks the context for its content resolver. */
public class Context {
    public android.content.res.Resources getResources(){return android.content.res.Resources.getSystem();}
    public PackageManager getPackageManager() { return new PackageManager(); }
    public ContentResolver getContentResolver() {
        return new ContentResolver();
    }
}
