package android.content;
import java.util.*;import java.util.concurrent.atomic.*;import android.database.*;import com.sevtinge.hyperceiler.libhook.provider.*;
public class ContentResolver {
 public static volatile Map<String,Object> values=Map.of();public static volatile String failure="";public static volatile Runnable afterSnapshot=()->{};public static volatile java.util.function.Consumer<MatrixCursor> mutate=c->{};
 public static AtomicInteger queries=new AtomicInteger(),reads=new AtomicInteger(),inflight=new AtomicInteger(),max=new AtomicInteger();public static boolean missing;
 public static SharedPreferences prefs(){return ()->{reads.incrementAndGet();if(failure.equals("getAll"))throw new IllegalStateException();return values;};}
 public Cursor query(android.net.Uri u,String[]p,String s,String[]a,String o){
  if(!u.toString().equals("content://com.sevtinge.hyperceiler.provider.sharedprefs/weather_config"))throw new AssertionError(u);
  queries.incrementAndGet();int n=inflight.incrementAndGet();max.accumulateAndGet(n,Math::max);
  try{if(failure.equals("query"))throw new IllegalStateException();if(failure.equals("null"))return null;
   SharedPrefsProvider provider=new SharedPrefsProvider();provider.onCreate();MatrixCursor c=(MatrixCursor)provider.query(u,p,s,a,o);
   if(c==null)return null;String f=failure;
   switch(f){case "empty"->c.rows.clear();case "extraRow"->c.newRow();case "columns"->c.columns=new String[]{"schema"};case "columnName"->c.columns[1]="wrong";case "schema"->c.rows.get(0)[0]=0;case "nullSchema"->c.rows.get(0)[0]=null;case "schemaText"->c.rows.get(0)[0]="1";case "read"->c.onRead=()->{throw new IllegalStateException();};case "close"->c.onClose=()->{throw new IllegalStateException();};}
   mutate.accept(c);afterSnapshot.run();return c;
  }finally{inflight.decrementAndGet();}
 }
}
