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
    private static int failureIndex = -1;
    private static String failureMode;
    private static int queries;

    public static void failAt(int index, String mode) {
        failureIndex = index;
        failureMode = mode;
        queries = 0;
    }

    public static int queryCount() { return queries; }

    /** Replace the rows every resolver instance answers with. */
    public static void setRows(java.util.Map<String, Integer> rows) {
        ROWS.clear();
        if (rows != null) ROWS.putAll(rows);
        failAt(-1, null);
    }

    public android.database.Cursor query(android.net.Uri uri, String[] projection, String selection,
        String[] selectionArgs, String sortOrder) {
        final String path = uri.toString();
        final int slash = path.lastIndexOf('/');
        final String key = slash >= 0 ? path.substring(slash + 1) : path;
        final Integer value = ROWS.get(key);
        final boolean fail = queries++ == failureIndex;
        if (fail && "query".equals(failureMode)) throw new IllegalStateException("provider frozen");
        if (fail && "null".equals(failureMode)) return null;
        return new android.database.Cursor() {
            private boolean read;

            @Override
            public boolean moveToFirst() {
                if (fail && "move".equals(failureMode)) throw new IllegalStateException("provider died");
                read = false;
                return value != null;
            }

            @Override
            public int getInt(int columnIndex) {
                if (fail && "read".equals(failureMode)) throw new IllegalStateException("invalid cursor");
                read = true;
                return value;
            }

            @Override
            public void close() {
                if (fail && "close".equals(failureMode)) throw new IllegalStateException("close failed");
            }
        };
    }
}
