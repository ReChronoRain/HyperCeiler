package android.net;

/** Minimal host stub: the endpoint only parses a content:// URI string. */
public final class Uri {
    private final String value;

    private Uri(String value) {
        this.value = value;
    }

    public static Uri parse(String value) {
        return new Uri(value);
    }
    public static Uri withAppendedPath(Uri base, String path) { return parse(base.value+"/"+path); }
    public String getLastPathSegment() { return value.substring(value.lastIndexOf('/')+1); }

    @Override
    public String toString() {
        return value;
    }
}
