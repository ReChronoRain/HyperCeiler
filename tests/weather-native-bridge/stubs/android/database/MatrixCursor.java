package android.database;
import java.util.*;
public class MatrixCursor implements Cursor {
 public String[] columns; public List<Object[]> rows=new ArrayList<>(); public int pos; public Runnable onRead=()->{},onClose=()->{};
 public MatrixCursor(String[] c){columns=c.clone();} public class RowBuilder {Object[] r; int i;RowBuilder(Object[] r){this.r=r;}public RowBuilder add(Object v){r[i++]=v;return this;}public RowBuilder add(String n,Object v){return add(v);}}
 public RowBuilder newRow(){Object[] r=new Object[columns.length];rows.add(r);return new RowBuilder(r);}
 Object v(int i){onRead.run();return rows.get(pos)[i];}public boolean moveToFirst(){pos=0;return !rows.isEmpty();}public int getCount(){return rows.size();}
 public int getInt(int i){return ((Number)v(i)).intValue();}public long getLong(int i){return ((Number)v(i)).longValue();}public String getString(int i){return (String)v(i);}
 public int getColumnCount(){return columns.length;}public String getColumnName(int i){return columns[i];}public boolean isNull(int i){return v(i)==null;}
 public int getType(int i){Object x=v(i);return x==null?0:x instanceof String?3:x instanceof Float||x instanceof Double?2:x instanceof Number?1:4;}public void close(){onClose.run();}
}
