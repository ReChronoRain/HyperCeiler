package android.content;

/** Minimal host stub: the endpoint only asks the context for its content resolver. */
public class Context {
    public ContentResolver getContentResolver() {
        return new ContentResolver();
    }
}
