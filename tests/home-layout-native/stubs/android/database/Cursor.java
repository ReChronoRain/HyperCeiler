package android.database;

/** Minimal host stub: a cursor the provider path can iterate without an Android runtime. */
public interface Cursor extends AutoCloseable {
    boolean moveToFirst();

    int getInt(int columnIndex);

    @Override
    void close();
}
