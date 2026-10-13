package android.database;

/** Minimal host stub: a cursor the provider path can iterate without an Android runtime. */
public interface Cursor extends AutoCloseable {
    boolean moveToFirst();

    int getInt(int columnIndex);
    default String getString(int columnIndex) { return Integer.toString(getInt(columnIndex)); }
    default int getColumnCount() { return 1; }
    default String getColumnName(int index) { return "data"; }
    default boolean isNull(int index) { return false; }

    @Override
    void close();
}
