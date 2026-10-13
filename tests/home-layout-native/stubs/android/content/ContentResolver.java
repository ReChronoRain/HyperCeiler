package android.content;

/** Legacy per-key and atomic single-row provider fixtures; no Android/device dependency. */
public class ContentResolver {
    private static final java.util.Map<String, Object> ROWS = new java.util.HashMap<>();
    private static String[][] specs = new String[0][];
    private static int failureIndex = -1, queries;
    private static String failureMode;
    private static boolean wrongSchema, emptyBatch;
    private static android.database.ContentObserver observer;
    private static String observedUri;
    private static int registrations, unregistrations;
    public static java.util.function.Consumer<String> afterRead;
    public void registerContentObserver(android.net.Uri uri, boolean descendants,
        android.database.ContentObserver candidate) {
        if (!descendants) throw new AssertionError("pref observer must include key descendants");
        observedUri = uri.toString(); observer = candidate; registrations++;
    }
    public void unregisterContentObserver(android.database.ContentObserver candidate) {
        if (observer == candidate) { observer = null; unregistrations++; }
    }
    public static void signalChange(String uri) {
        if (observer != null && uri.startsWith(observedUri + "/"))
            observer.onChange(false, android.net.Uri.parse(uri));
    }
    public static int registrationCount() { return registrations; }
    public static int unregistrationCount() { return unregistrations; }
    public static void setSpecs(String[][] value) { specs = value; }
    public static void failAt(int index, String mode) { failureIndex = index; failureMode = mode; queries = 0; }
    public static int queryCount() { return queries; }
    public static void putRow(String key, int value) { ROWS.put(key, value); }
    public static void wrongSchema(boolean value) { wrongSchema = value; }
    public static void emptyBatch(boolean value) { emptyBatch = value; }
    public static void setRows(java.util.Map<String, ?> rows) {
        ROWS.clear(); if (rows != null) ROWS.putAll(rows);
        wrongSchema = emptyBatch = false; afterRead = null; failAt(-1, null);
    }
    public android.database.Cursor query(android.net.Uri uri, String[] projection, String selection,
        String[] selectionArgs, String sortOrder) {
        final String path = uri.toString();
        if (path.endsWith("/home_layout")) {
            queries++;
            if (failureIndex == 0 && "query".equals(failureMode)) throw new IllegalStateException("provider frozen");
            if (failureIndex == 0 && "null".equals(failureMode)) return null;
            final var snapshot = new java.util.HashMap<>(ROWS);
            return new android.database.Cursor() {
                public int getColumnCount() { return specs.length + 1; }
                public String getColumnName(int i) { return i == 0 ? "schema" : specs[i-1][1]; }
                public boolean moveToFirst() { return !emptyBatch; }
                public boolean isNull(int i) {
                    if (i == 0) return false;
                    if (i-1 == failureIndex && ("query".equals(failureMode) || "null".equals(failureMode) || "move".equals(failureMode)))
                        throw new IllegalStateException("batch unavailable");
                    String key = "prefs_key_" + specs[i-1][1];
                    if (afterRead != null) afterRead.accept(key);
                    return !snapshot.containsKey(key);
                }
                public int getInt(int i) {
                    if (i == 0) return wrongSchema ? 999 : 1;
                    if (i-1 == failureIndex && "read".equals(failureMode)) throw new IllegalStateException("invalid cell");
                    return ((Number)snapshot.get("prefs_key_" + specs[i-1][1])).intValue();
                }
                public String getString(int i) {
                    if (i-1 == failureIndex && "read".equals(failureMode)) throw new IllegalStateException("invalid cell");
                    return (String)snapshot.get("prefs_key_" + specs[i-1][1]);
                }
                public void close() {
                    if (failureIndex >= 0 && "close".equals(failureMode)) throw new IllegalStateException("close failed");
                }
            };
        }
        final String key = path.substring(path.lastIndexOf('/') + 1);
        final Integer value = ROWS.get(key) instanceof Integer v ? v : null;
        final boolean fail = queries++ == failureIndex;
        if (fail && "query".equals(failureMode)) throw new IllegalStateException("provider frozen");
        if (fail && "null".equals(failureMode)) return null;
        return new android.database.Cursor() {
            public boolean moveToFirst() {
                if (fail && "move".equals(failureMode)) throw new IllegalStateException("provider died");
                if (afterRead != null) afterRead.accept(key);
                return value != null;
            }
            public int getInt(int i) {
                if (fail && "read".equals(failureMode)) throw new IllegalStateException("invalid cursor");
                return value;
            }
            public void close() {
                if (fail && "close".equals(failureMode)) throw new IllegalStateException("close failed");
            }
        };
    }
}
