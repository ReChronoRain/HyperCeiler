package android.content;

/** Minimal host stub: the endpoint only asks the context for its content resolver. */
public class Context {
    public android.content.res.Resources getResources(){return android.content.res.Resources.getSystem();}
    public ContentResolver getContentResolver() {
        return new ContentResolver();
    }
}
