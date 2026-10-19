package android.content;
import android.database.Cursor;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
/** Transport only; row/schema/type construction always uses the actual production snapshot class. */
public class ContentResolver {
 public static volatile java.util.Map<String,Object> values=java.util.Map.of();
 public static volatile Runnable afterSnapshot=()->{};
 public static volatile String failure="";
 public static final java.util.concurrent.atomic.AtomicInteger queries=new java.util.concurrent.atomic.AtomicInteger(),reads=new java.util.concurrent.atomic.AtomicInteger();
 public static Cursor cursor(){return HomeLayoutPrefsSnapshot.cursor(()->{reads.incrementAndGet();if(failure.equals("getAll"))throw new IllegalStateException("prefs unavailable");var result=new java.util.HashMap<>(values);afterSnapshot.run();return result;});}
 public Cursor query(android.net.Uri uri,String[] projection,String selection,String[] args,String order){
  if(!uri.toString().equals("content://com.sevtinge.hyperceiler.provider.sharedprefs/home_layout"))throw new AssertionError("wrong URI");queries.incrementAndGet();
  if(failure.equals("query"))throw new IllegalStateException("provider unavailable");if(failure.equals("null"))return null;
  final Cursor delegate=cursor();if(delegate==null)return null;final String mode=failure;
  return new Cursor(){public boolean moveToFirst(){return !mode.equals("empty")&&delegate.moveToFirst();}public int getColumnCount(){return delegate.getColumnCount()+(mode.equals("columns")?1:0);}
   public String getColumnName(int i){return mode.equals("columnName")&&i==2?"wrong_key":delegate.getColumnName(i);}public boolean isNull(int i){return mode.equals("nullSchema")&&i==0||delegate.isNull(i);}
   public int getInt(int i){if(i==0&&mode.equals("schema1"))return 1;if(mode.equals("read")&&i>0)throw new IllegalStateException("bad cell");return delegate.getInt(i);}
   public long getLong(int i){if(mode.equals("read"))throw new IllegalStateException("bad cell");return delegate.getLong(i);}
   public String getString(int i){if(mode.equals("read"))throw new IllegalStateException("bad cell");return delegate.getString(i);}
   public void close(){delegate.close();if(mode.equals("close"))throw new IllegalStateException("dead cursor");}};
 }
}
