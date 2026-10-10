package android.database;
public interface Cursor extends AutoCloseable {
 int FIELD_TYPE_NULL=0, FIELD_TYPE_INTEGER=1, FIELD_TYPE_FLOAT=2, FIELD_TYPE_STRING=3, FIELD_TYPE_BLOB=4;
 boolean moveToFirst(); int getCount(); int getInt(int i); long getLong(int i); String getString(int i);
 int getColumnCount(); String getColumnName(int i); boolean isNull(int i); int getType(int i); void close();
}
