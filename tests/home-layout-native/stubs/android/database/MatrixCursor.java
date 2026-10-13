package android.database;
public final class MatrixCursor implements Cursor {
    private final String[] columns;
    private Object[] row;
    public MatrixCursor(String[] columns) { this.columns = columns.clone(); }
    public RowBuilder newRow() { row = new Object[columns.length]; return new RowBuilder(); }
    public final class RowBuilder {
        private int index;
        public RowBuilder add(Object value) { row[index++] = value; return this; }
    }
    public boolean moveToFirst() { return row != null; }
    public int getInt(int index) { return ((Number) row[index]).intValue(); }
    public String getString(int index) { return (String) row[index]; }
    public boolean isNull(int index) { return row[index] == null; }
    public int getColumnCount() { return columns.length; }
    public String getColumnName(int index) { return columns[index]; }
    public void close() {}
}
