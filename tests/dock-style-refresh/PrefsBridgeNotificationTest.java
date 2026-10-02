import android.content.*;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import java.util.*;
public final class PrefsBridgeNotificationTest {
 static final String K="prefs_key_home_dock_unlock_style";
 static int pass,fail;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void test(String n,Runnable r){try{r.run();pass++;System.out.println("PASS "+n);}catch(AssertionError e){fail++;System.out.println("FAIL "+n+": "+e.getMessage());}}
 static final class Prefs implements SharedPreferences {
  final Map<String,Object> values=new HashMap<>(); boolean succeed=true,readOnly=false;
  public Map<String,?> getAll(){return new HashMap<>(values);} public String getString(String k,String d){return (String)values.getOrDefault(k,d);}
  @SuppressWarnings("unchecked") public Set<String> getStringSet(String k,Set<String>d){return (Set<String>)values.getOrDefault(k,d);}
  public int getInt(String k,int d){return (int)values.getOrDefault(k,d);} public long getLong(String k,long d){return (long)values.getOrDefault(k,d);}
  public float getFloat(String k,float d){return (float)values.getOrDefault(k,d);} public boolean getBoolean(String k,boolean d){return (boolean)values.getOrDefault(k,d);}
  public boolean contains(String k){return values.containsKey(k);} public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){} public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
  public Editor edit(){if(readOnly)throw new UnsupportedOperationException();return new Edit();}
  final class Edit implements Editor {
   final Map<String,Object> puts=new HashMap<>(); final Set<String> removes=new HashSet<>(); boolean clear;
   public Editor putString(String k,String v){puts.put(k,v);return this;} public Editor putStringSet(String k,Set<String>v){puts.put(k,v);return this;} public Editor putInt(String k,int v){puts.put(k,v);return this;}
   public Editor putLong(String k,long v){puts.put(k,v);return this;} public Editor putFloat(String k,float v){puts.put(k,v);return this;} public Editor putBoolean(String k,boolean v){puts.put(k,v);return this;}
   public Editor remove(String k){removes.add(k);return this;} public Editor clear(){clear=true;return this;}
   public boolean commit(){if(!succeed)return false;if(clear)values.clear();removes.forEach(values::remove);values.putAll(puts);return true;} public void apply(){commit();}
  }
 }
 static void exercise(String label,boolean remove,int remoteMode){
  Prefs local=new Prefs(),remote=new Prefs(); local.values.put(K,"daybreak"); Context ctx=new Context(local);
  PrefsBridge.initForApp(ctx); PrefsBridge.setRemotePrefs(remoteMode==0?null:remote);ctx.resolver.events.clear();
  remote.succeed=remoteMode!=2; remote.readOnly=remoteMode==3;
  if(remove)PrefsBridge.removeByApp("home_dock_unlock_style");else PrefsBridge.putString("home_dock_unlock_style","gale");
  check(remove?!local.values.containsKey(K):"gale".equals(local.values.get(K)),"physical value committed");
  check(ctx.resolver.events.size()==2,"expected 2 provider notifications, got "+ctx.resolver.events.size());
  check(ctx.resolver.events.get(0).endsWith("/string/"+K),"full pref key string URI");
  check(ctx.resolver.events.get(1).endsWith("/pref/string/"+K),"all-prefs URI");
  if(remoteMode==1)check(remove?!remote.values.containsKey(K):"gale".equals(remote.values.get(K)),"remote success preserved");
 }
 public static void main(String[] a){
  String[] modes={"remote absent","remote writable","remote commit false","remote read-only"};
  for(int i=0;i<4;i++){final int m=i;test("put / "+modes[i],()->exercise("put",false,m));test("remove / "+modes[i],()->exercise("remove",true,m));}
  test("failed physical commit emits no notification",()->{Prefs p=new Prefs();Context c=new Context(p);PrefsBridge.initForApp(c);PrefsBridge.setRemotePrefs(null);p.succeed=false;PrefsBridge.putString(K,"gale");PrefsBridge.removeByApp(K);check(c.resolver.events.isEmpty(),"failed local writes silent");check(!p.values.containsKey(K),"failed local value unchanged");});
  test("hook writes remain blocked",()->{Prefs p=new Prefs();Context c=new Context(p);PrefsBridge.initForApp(c);PrefsBridge.initForHook(p);PrefsBridge.putString(K,"gale");PrefsBridge.removeByApp(K);check(p.values.isEmpty()&&c.resolver.events.isEmpty(),"hook wrote or notified");});
  System.out.println("PrefsBridge notifications: "+pass+" passed, "+fail+" failed");if(fail>0)System.exit(1);
 }
}
