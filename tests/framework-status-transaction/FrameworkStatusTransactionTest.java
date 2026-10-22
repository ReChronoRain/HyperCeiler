import android.content.*;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.utils.FrameworkStatusManager;
import com.sevtinge.hyperceiler.libhook.base.FrameworkHookGate;
import io.github.libxposed.service.XposedService;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class FrameworkStatusTransactionTest {
 static String key(String k){return "prefs_key_framework_"+k;}
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static class Prefs implements SharedPreferences {
  final Map<String,Object> values=new HashMap<>(); final List<Set<String>> committedKeys=new ArrayList<>(); int commits; boolean fail,throwCommit,readOnly,publishBeforeFailure,throwRead; Runnable after=()->{},before=()->{};
  public synchronized Map<String,?> getAll(){if(throwRead)throw new IllegalStateException("read failure");return new HashMap<>(values);}
  Object v(String k,Object d){return getAll().getOrDefault(k,null)==null?d:getAll().get(k);}
  public String getString(String k,String d){return (String)v(k,d);} public Set<String> getStringSet(String k,Set<String>d){return (Set<String>)v(k,d);}
  public int getInt(String k,int d){return (Integer)v(k,d);}public long getLong(String k,long d){return (Long)v(k,d);}public float getFloat(String k,float d){return (Float)v(k,d);}public boolean getBoolean(String k,boolean d){return (Boolean)v(k,d);}public boolean contains(String k){return getAll().containsKey(k);}
  public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
  public Editor edit(){if(readOnly)throw new UnsupportedOperationException();return new E();}
  class E implements Editor {
   final Map<String,Object> pending=new HashMap<>();boolean clear;
   Editor put(String k,Object v){pending.put(k,v);return this;}
   public Editor putString(String k,String v){return put(k,v);}public Editor putStringSet(String k,Set<String>v){return put(k,v);}public Editor putInt(String k,int v){return put(k,v);}public Editor putLong(String k,long v){return put(k,v);}public Editor putFloat(String k,float v){return put(k,v);}public Editor putBoolean(String k,boolean v){return put(k,v);}public Editor remove(String k){return put(k,null);}public Editor clear(){clear=true;return this;}public void apply(){commit();}
   public boolean commit(){before.run();synchronized(Prefs.this){commits++;committedKeys.add(Set.copyOf(pending.keySet()));if(throwCommit)throw new IllegalStateException("commit failure");if(!fail||publishBeforeFailure){if(clear)values.clear();pending.forEach((k,v)->{if(v==null)values.remove(k);else values.put(k,v);});}}after.run();return !fail;}
  }
 }
 record Service(String name,String version,long code,int api,boolean error) implements XposedService {
  public String getFrameworkName(){return name;}public String getFrameworkVersion(){return version;}public long getFrameworkVersionCode(){return code;}public int getApiVersion(){if(error)throw new IllegalStateException("offline");return api;}
 }
 static final Service OLD=new Service("Old", "v1", 1,100,false), NEW=new Service("New","v2",4294967300L,100,false), OK=new Service("New","v2",4294967300L,102,false);
 static Prefs local,remote;static Context context;
 static void setup(boolean bind) throws Exception { var f=PrefsBridge.class.getDeclaredField("mRemotePrefs");f.setAccessible(true);f.set(null,null);local=new Prefs();remote=new Prefs();context=new Context(local);PrefsBridge.initForApp(context);if(bind)PrefsBridge.setRemotePrefs(remote);}
 static void seed(Service s){FrameworkStatusManager.onServiceBound(s);local.commits=remote.commits=0;local.committedKeys.clear();remote.committedKeys.clear();context.resolver.events.clear();}
 static void status(Map<String,?> m,Service s){check(Objects.equals(m.get(key("api_allow_hook")),s.api>=101&&!s.error),"allow mismatch "+m);check(Objects.equals(m.get(key("check_reason")),s.error?"SERVICE_ERROR":s.api>=101?"COMPATIBLE":"API_TOO_LOW"),"reason mismatch "+m);check(Objects.equals(m.get(key("check_name")),s.name==null?null:s.name.trim()),"name mismatch "+m);check(Objects.equals(m.get(key("check_version")),s.version==null?null:s.version.trim()),"version mismatch "+m);check(Objects.equals(m.get(key("check_version_code")),s.code),"code mismatch "+m);check(Objects.equals(m.get(key("check_api_version")),s.error?-1:s.api),"api mismatch "+m);}
 static boolean gate(Map<String,?> m,Service s){return FrameworkHookGate.isAllowed((Boolean)m.getOrDefault(key("api_allow_hook"),null),(String)m.get(key("check_reason")),()->new FrameworkHookGate.Fingerprint(s.name,s.version,s.code),()->new FrameworkHookGate.Fingerprint((String)m.get(key("check_name")),(String)m.get(key("check_version")),(Long)m.get(key("check_version_code"))));}
 static void await(CountDownLatch l){try{check(l.await(5,TimeUnit.SECONDS),"latch timeout");}catch(InterruptedException e){throw new AssertionError(e);}}
 interface Task {void run()throws Exception;}static int passed,failed;
 static void test(String n,Task t){try{setup(true);t.run();System.out.println("PASS "+n);passed++;}catch(Throwable t1){System.out.println("FAIL "+n+": "+t1);failed++;}}
 public static void main(String[] args)throws Exception {
  test("blocked_revision_atomic_local",()->{seed(OLD);local.after=()->status(local.getAll(),NEW);FrameworkStatusManager.onServiceBound(NEW);check(local.commits==1,"expected 1 local revision");});
  test("blocked_revision_atomic_remote",()->{seed(OLD);remote.after=()->status(remote.getAll(),NEW);FrameworkStatusManager.onServiceBound(NEW);check(remote.commits==1,"expected 1 remote revision");});
  test("compatible_to_blocked_gate_not_mixed",()->{seed(new Service("New","v1",1,102,false));local.after=()->{status(local.getAll(),NEW);check(!gate(local.getAll(),NEW),"partial status allowed blocked current framework");};FrameworkStatusManager.onServiceBound(NEW);});
  test("old_false_with_new_identity_never_temporarily_authorizes",()->{seed(new Service("Old","v1",1,102,false));AtomicBoolean leaked=new AtomicBoolean();local.after=()->{Map<String,?> m=local.getAll();if(Boolean.FALSE.equals(m.get(key("api_allow_hook")))&&gate(m,NEW))leaked.set(true);};FrameworkStatusManager.onServiceBound(NEW);check(!leaked.get(),"new blocked gate combined with old identity authorized current framework");});
  test("blocked_to_compatible_atomic",()->{seed(OLD);local.after=()->status(local.getAll(),OK);FrameworkStatusManager.onServiceBound(OK);check(gate(local.getAll(),OK),"compatible denied");});
  test("observers_only_see_complete_both_stores",()->{seed(OLD);AtomicReference<Throwable> mixed=new AtomicReference<>();context.resolver.observer=()->{try{status(local.getAll(),NEW);status(remote.getAll(),NEW);}catch(Throwable t){mixed.set(t);}};FrameworkStatusManager.onServiceBound(NEW);check(mixed.get()==null,"observer saw mixed revision: "+mixed.get());});
  test("unchanged_allow_notifications_preserved",()->{seed(OLD);FrameworkStatusManager.onServiceBound(NEW);check(context.resolver.events.size()==24,"expected 24 legacy typed URI notifications: "+context.resolver.events.size());check(context.resolver.events.stream().noneMatch(x->x.endsWith("framework_api_allow_hook")),"unchanged allow notified");});
  test("remote_unchanged_allow_not_added_to_editor",()->{seed(OLD);FrameworkStatusManager.onServiceBound(NEW);check(remote.committedKeys.stream().noneMatch(keys->keys.contains(key("api_allow_hook"))),"extra remote listener event for unchanged allow");});
  test("remote_stale_allow_repaired_in_same_revision",()->{seed(OLD);remote.values.put(key("api_allow_hook"),true);remote.after=()->status(remote.getAll(),NEW);FrameworkStatusManager.onServiceBound(NEW);check(remote.commits==1&&remote.committedKeys.getFirst().contains(key("api_allow_hook")),"stale remote gate not repaired");});
  test("remote_read_failure_keeps_local_observers",()->{seed(OLD);remote.throwRead=true;FrameworkStatusManager.onServiceBound(NEW);status(local.getAll(),NEW);check(remote.commits==0&&context.resolver.events.size()==24,"local change hidden by failed remote getAll");});
  test("changed_allow_notifications_preserved",()->{seed(OLD);FrameworkStatusManager.onServiceBound(OK);check(context.resolver.events.size()==26,"expected 26 URI notifications");});
  test("missing_allow_notifications_preserved",()->{FrameworkStatusManager.onServiceBound(OK);check(context.resolver.events.size()==26,"missing allow not notified");status(local.getAll(),OK);});
  test("repeat_status_one_commit_per_store",()->{seed(NEW);FrameworkStatusManager.onServiceBound(NEW);check(local.commits==1&&remote.commits==1,"repeat revision split");check(context.resolver.events.size()==24,"repeat notifications changed");});
  test("long_code_exact",()->{FrameworkStatusManager.onServiceBound(NEW);check(local.getAll().get(key("check_version_code")) instanceof Long,"code type");status(remote.getAll(),NEW);});
  test("null_metadata_removes_previous",()->{seed(OLD);Service s=new Service(null,null,-1,100,false);FrameworkStatusManager.onServiceBound(s);status(local.getAll(),s);check(!local.contains(key("check_name"))&&!remote.contains(key("check_version")),"null not removed");check(context.resolver.events.size()==24,"deletion typed notifications changed");});
  test("service_exception_records_denial",()->{seed(OK);Service s=new Service("X","v3",4,0,true);FrameworkStatusManager.onServiceBound(s);status(local.getAll(),s);check(local.getString(key("check_detail"),"").contains("offline"),"error detail");check(FrameworkStatusManager.hasBlockingIssue(),"status denial lost");});
  test("name_normalization",()->{Service s=new Service(" New "," v2 ",2,102,false);FrameworkStatusManager.onServiceBound(s);status(local.getAll(),s);});
  test("physical_read_failure_skips_publication",()->{seed(OLD);local.throwRead=true;FrameworkStatusManager.onServiceBound(NEW);check(local.commits==0&&remote.commits==0&&context.resolver.events.isEmpty(),"unavailable physical snapshot propagated");check(FrameworkStatusManager.getCurrentStatus().getFrameworkName().equals("New"),"service status not updated after preference read failure");});
  test("physical_commit_false_skips_remote_notifications",()->{seed(OLD);local.fail=true;FrameworkStatusManager.onServiceBound(NEW);check(remote.commits==0&&context.resolver.events.isEmpty(),"failed physical propagated");status(local.getAll(),OLD);});
  test("physical_commit_false_after_memory_publish_is_whole",()->{seed(OLD);local.fail=local.publishBeforeFailure=true;local.after=()->status(local.getAll(),NEW);FrameworkStatusManager.onServiceBound(NEW);status(local.getAll(),NEW);check(remote.commits==0&&context.resolver.events.isEmpty(),"failure propagated");});
  test("physical_commit_throw_skips_remote_notifications",()->{seed(OLD);local.throwCommit=true;FrameworkStatusManager.onServiceBound(NEW);check(remote.commits==0&&context.resolver.events.isEmpty(),"exception propagated");status(local.getAll(),OLD);});
  test("physical_read_only_no_partial_status",()->{seed(OLD);local.readOnly=true;FrameworkStatusManager.onServiceBound(NEW);check(remote.commits==0&&context.resolver.events.isEmpty(),"readonly propagated");status(local.getAll(),OLD);});
  test("remote_failure_keeps_local_observers",()->{seed(OLD);remote.fail=true;FrameworkStatusManager.onServiceBound(NEW);status(local.getAll(),NEW);status(remote.getAll(),OLD);check(context.resolver.events.size()==24,"local change hidden");});
  test("remote_throw_keeps_local_observers",()->{seed(OLD);remote.throwCommit=true;FrameworkStatusManager.onServiceBound(NEW);status(local.getAll(),NEW);check(context.resolver.events.size()==24,"local change hidden");});
  test("remote_read_only_keeps_local_observers",()->{seed(OLD);remote.readOnly=true;FrameworkStatusManager.onServiceBound(NEW);status(local.getAll(),NEW);check(context.resolver.events.size()==24,"local change hidden");});
  test("no_remote_local_revision_and_notifications",()->{setup(false);seed(OLD);FrameworkStatusManager.onServiceBound(NEW);status(local.getAll(),NEW);check(local.commits==1&&context.resolver.events.size()==24,"local only behavior");});
  test("hook_process_read_only",()->{seed(OLD);PrefsBridge.initForHook(remote);FrameworkStatusManager.onServiceBound(NEW);check(local.commits==0&&remote.commits==0&&context.resolver.events.isEmpty(),"hook wrote status");});
  test("bootstrap_gate_default_unchanged",()->{FrameworkStatusManager.init();check(local.getBoolean(key("api_allow_hook"),false),"default absent");check(local.commits==1,"init writes");FrameworkStatusManager.init();check(local.commits==1,"init rewrote present");});
  test("single_pref_write_behavior_unchanged",()->{PrefsBridge.putByApp("ordinary",7);check(local.commits==1&&remote.commits==1&&context.resolver.events.size()==2,"normal write changed");check(local.getInt("prefs_key_ordinary",0)==7,"normal value");});
  test("concurrent_status_callbacks_ordered",()->{seed(OLD);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();AtomicInteger once=new AtomicInteger();remote.before=()->{if(once.getAndIncrement()==0){entered.countDown();await(release);}};Thread a=new Thread(()->{try{FrameworkStatusManager.onServiceBound(NEW);}catch(Throwable t){error.set(t);}});Thread b=new Thread(()->{try{FrameworkStatusManager.onServiceBound(OK);}catch(Throwable t){error.set(t);}});a.start();await(entered);b.start();release.countDown();a.join(5000);b.join(5000);check(!a.isAlive()&&!b.isAlive()&&error.get()==null,"threads failed "+error.get());status(local.getAll(),OK);status(remote.getAll(),OK);check(FrameworkStatusManager.getCurrentStatus().isHookAllowed(),"UI status reordered");});
  test("remote_bind_sync_waits_for_complete_revision",()->{seed(OLD);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);Prefs fresh=new Prefs();AtomicReference<Throwable> error=new AtomicReference<>();remote.before=()->{entered.countDown();await(release);};Thread a=new Thread(()->{try{FrameworkStatusManager.onServiceBound(NEW);}catch(Throwable t){error.set(t);}});Thread b=new Thread(()->{try{PrefsBridge.setRemotePrefs(fresh);}catch(Throwable t){error.set(t);}});a.start();await(entered);b.start();release.countDown();a.join(5000);b.join(5000);check(!a.isAlive()&&!b.isAlive()&&error.get()==null,"bind race "+error.get());status(fresh.getAll(),NEW);check(fresh.commits==1,"new remote split");});
  System.out.println("RESULT "+passed+" passed, "+failed+" failed");if(failed!=0)System.exit(1);
 }
}
