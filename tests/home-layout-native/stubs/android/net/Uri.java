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

    @Override
    public String toString() {
        return value;
    }
}
