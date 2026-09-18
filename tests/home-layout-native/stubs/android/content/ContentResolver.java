package android.content;

/**
 * Minimal host stub for the module provider read.
 *
 * The endpoint builds one URI per preference and asks the resolver for cursor over it. The stub keeps
 * a map keyed by the last path segment (the preference key) so the whole preference -> pixel
 * conversion can be exercised on the host, without an Android runtime.
 */
public class ContentResolver {
    private static final java.util.Map<String, Integer> ROWS = new java.util.HashMap<>();

    /** Replace the rows every resolver instance answers with. */
    public static void setRows(java.util.Map<String, Integer> rows) {
        ROWS.clear();
        if (rows != null) ROWS.putAll(rows);
    }

    public android.database.Cursor query(android.net.Uri uri, String[] projection, String selection,
        String[] selectionArgs, String sortOrder) {
        final String path = uri.toString();
        final int slash = path.lastIndexOf('/');
        final String key = slash >= 0 ? path.substring(slash + 1) : path;
        final Integer value = ROWS.get(key);
        if (value == null) return null;
        return new android.database.Cursor() {
            private boolean read;

            @Override
            public boolean moveToFirst() {
                read = false;
                return true;
            }

            @Override
            public int getInt(int columnIndex) {
                read = true;
                return value;
            }

            @Override
            public void close() {
            }
        };
    }
}
