package android.database;

/** Minimal host stub: a cursor the provider path can iterate without an Android runtime. */
public interface Cursor extends AutoCloseable {
    default android.os.Bundle getExtras() { return new android.os.Bundle(); }
    boolean moveToFirst();

    int getInt(int columnIndex);
    default long getLong(int columnIndex) { return getInt(columnIndex); }
    default String getString(int columnIndex) { return Integer.toString(getInt(columnIndex)); }
    default int getColumnCount() { return 1; }
    default String getColumnName(int index) { return "data"; }
    default int getColumnIndex(String name) { for(int i=0;i<getColumnCount();++i)if(name.equals(getColumnName(i)))return i;return -1; }
    default boolean isNull(int index) { return false; }

    @Override
    void close();
}
