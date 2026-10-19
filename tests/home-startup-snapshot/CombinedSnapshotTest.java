package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Parcel;
import android.os.SystemClock;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.nio.*;
import java.nio.file.*;

/** Complete production endpoint/provider/gate, deterministic clock/Android transport adapters. */
public final class CombinedSnapshotTest {
 static final String DESC="android.view.IWindowManager";
 static final AtomicLong now=new AtomicLong();
 static int passed,failed;
 static void check(boolean b){if(!b)throw new AssertionError();}
 static void eq(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 static void set(String n,Object v){try{var f=HomeLayoutNativeEndpointOS4.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}catch(Exception e){throw new AssertionError(e);}}
 static Object get(String n){try{var f=HomeLayoutNativeEndpointOS4.class.getDeclaredField(n);f.setAccessible(true);return f.get(null);}catch(Exception e){throw new AssertionError(e);}}
 static void reset(){
  for(String s:new String[]{"cachedValues","cachedFrameworkReason","cachedFrameworkFingerprint","combinedPublished"})set(s,null);
  for(String s:new String[]{"refreshEpoch","refreshTicket","combinedEpoch","combinedRevision","hyosSnapshotAtNanos"})set(s,0L);
  set("paused",false);set("hyosSnapshotLive",false);set("refresher",new Thread());
  BaseHook.resetTestState();BaseLoad.resetFrameworkForTest();Binder.setCallingIdentityForTest(10100,42);
  PackageManager.packages=new String[]{"com.miui.home"};PackageManager.fail=false;
  ContentResolver.values=Map.of();ContentResolver.failure="";ContentResolver.afterSnapshot=()->{};
  ContentResolver.queries.set(0);ContentResolver.reads.set(0);now.set(10_000_000_000L);SystemClock.source=now::get;
 }
 static void run(String n,Runnable r){reset();try{r.run();passed++;System.out.println("PASS "+n);}catch(Throwable t){failed++;System.out.println("FAIL "+n+" "+t);t.printStackTrace(System.out);}}
 static HomeLayoutNativeEndpointOS4 endpoint(){return new HomeLayoutNativeEndpointOS4((u,p)->u==10100&&p==42,()->{throw new AssertionError("legacy separate layout reader forbidden");});}
 static HomeLayoutNativeEndpointOS4.CombinedSnapshot read(){return endpoint().receiveCombinedSnapshot(new Parcel(DESC),0);}
 static Map<String,Object> prefs(Object...p){var m=new HashMap<String,Object>();for(int i=0;i<p.length;i+=2)m.put("prefs_key_"+p[i],p[i+1]);return m;}
 static void publish(Map<String,Object> p){ContentResolver.values=p;check(HomeLayoutNativeEndpointOS4.refreshFromProvider());}
 static byte[] wire(HomeLayoutNativeEndpointOS4.CombinedSnapshot s){Parcel p=new Parcel(DESC);for(int i=0;i<100;i++)p.writeInt(0x77777777);HomeLayoutNativeEndpointOS4.writeCombinedReply(p,s);eq(p.dataSize(),188);eq(p.dataPosition(),188);return p.marshall();}
 static void inactive(HomeLayoutNativeEndpointOS4.CombinedSnapshot s,int available){
  eq(s.available(),available);eq(s.flags(),0);eq(s.deadline(),0L);
  ByteBuffer b=ByteBuffer.wrap(wire(s)).order(ByteOrder.LITTLE_ENDIAN);
  eq(b.getInt(0),0x48435331);eq(b.getInt(4),available);eq(b.getInt(8),0);eq(b.getLong(28),0L);
  eq(b.getInt(48),8);eq(b.getInt(116),17);
  for(int off=36;off<188;off+=4)if(off!=48&&off!=116)eq(b.getInt(off),0);
 }
 static final class Gate {final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);void block(){entered.countDown();await(release);}void entered(){await(entered);}}
 static void await(CountDownLatch l){try{check(l.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new AssertionError(e);}}
 static void join(Thread t){try{t.join(5000);check(!t.isAlive());}catch(InterruptedException e){throw new AssertionError(e);}}

 public static void main(String[] args){
  run("exact independent transaction and ACK constants",()->{eq(HomeLayoutNativeEndpointOS4.COMBINED_TRANSACTION_CODE,0x00484353);eq(HomeLayoutNativeEndpointOS4.COMBINED_ACK,0x48435331);eq(HomeLayoutNativeEndpointOS4.COMBINED_REPLY_BYTES,188);eq(HomeLayoutNativeEndpointOS4.HYOS_TRANSACTION_CODE,0x00484359);eq(HomeLayoutNativeEndpointOS4.TRANSACTION_CODE,0x0048434c);});
  run("cold missing combined snapshot unavailable not guessed layout",()->{inactive(read(),0);eq(read().epoch(),0L);eq(read().revision(),0L);});
  run("old reload numeric cache cannot activate combined snapshot",()->{BaseHook.putHotReloadRuntimeState("OS4.HomeLayout.ProviderSnapshot.v1",Map.of("allow_hook",1,"home_layout_unlock_grids_new",1,"home_layout_unlock_grids_cell_x",9));inactive(read(),0);check(get("cachedValues")!=null);});
  run("null writer result canonical188 unavailable no trailing previous reply",()->inactive(nullResult(),0));
  run("empty known snapshot is denied with positive publication identity",()->{publish(Map.of());var s=read();inactive(s,1);check(s.epoch()>0&&s.revision()>0);});
  run("enabled snapshot preserves UI defaults as disabled controls",()->{publish(prefs("allow_hook",true));var s=read();eq(s.available(),1);eq(s.flags(),1);eq(s.layout().gridEnabled(),0);eq(s.layout().cellX(),4);eq(s.layout().cellY(),6);eq(s.layout().tweaks()[0],3);eq(s.layout().tweaks()[1],0);eq(s.layout().tweaks()[8],0x66);for(int v:s.layout().knobDeltaDp())eq(v,0);eq(s.deadline(),15_000_000_000L);});
  run("single provider query combines flags and layout of same physical getAll",()->{publish(prefs("allow_hook",true,"home_title_hide_report_text",true,"home_layout_unlock_grids_new",true,"home_layout_unlock_grids_cell_x",7,"home_layout_unlock_grids_cell_y",11,"home_layout_workspace_padding_top_enable",true,"home_layout_workspace_padding_top",53));var s=read();eq(s.flags(),33);eq(s.layout().cellX(),7);eq(s.layout().cellY(),11);eq(s.layout().knobDeltaDp()[2],23);eq(ContentResolver.reads.get(),1);eq(ContentResolver.queries.get(),1);read();eq(ContentResolver.queries.get(),1);});
  run("wire offsets exact int32 int64 counters and 8+17 arrays",()->{set("combinedEpoch",0x1122334455667788L);set("combinedRevision",0x2233445566778800L);publish(prefs("allow_hook",true,"home_layout_unlock_grids_new",true,"home_layout_unlock_grids_cell_x",8,"home_layout_unlock_grids_cell_y",12,"home_layout_hotseats_margin_bottom_enable",true,"home_layout_hotseats_margin_bottom",101,"home_dock_unlock_hotseat",true));var s=read();var b=ByteBuffer.wrap(wire(s)).order(ByteOrder.LITTLE_ENDIAN);eq(b.getInt(4),1);eq(b.getInt(8),1);eq(b.getLong(12),0x1122334455667788L);eq(b.getLong(20),0x2233445566778801L);eq(b.getLong(28),15_000_000_000L);eq(b.getInt(36),1);eq(b.getInt(40),8);eq(b.getInt(44),12);eq(b.getInt(48),8);eq(b.getInt(52),1);eq(b.getInt(56),31);eq(b.getInt(116),17);eq(b.getInt(184),1);});
  run("disabled knob always zero delta despite stored nondefault slider",()->{publish(prefs("allow_hook",true,"home_layout_hotseats_margin_bottom_enable",false,"home_layout_hotseats_margin_bottom",100));var s=read();eq(s.layout().knobEnabled()[0],0);eq(s.layout().knobDeltaDp()[0],0);});
  run("returned arrays cannot corrupt cached publication",()->{publish(prefs("allow_hook",true,"home_layout_workspace_padding_top_enable",true,"home_layout_workspace_padding_top",55));var s=read();s.layout().knobEnabled()[2]=0;s.layout().knobDeltaDp()[2]=999;s.layout().tweaks()[0]=15;eq(s.layout().knobDeltaDp()[2],25);eq(read().layout().knobDeltaDp()[2],25);eq(read().layout().tweaks()[0],3);});
  run("all supported flags remain exact63",()->{publish(prefs("allow_hook",true,"home_other_high_models",true,"home_other_disable_prestart",true,"home_dock_bg_custom_enable",true,"home_dock_add_blur","1","home_title_hide_report_text",true));eq(read().flags(),63);});
  run("soft glass still requires dock",()->{publish(prefs("allow_hook",true,"home_dock_add_blur","1"));eq(read().flags(),1);});
  run("global off canonical zero layout despite all enabled controls",()->{publish(prefs("allow_hook",false,"home_title_hide_report_text",true,"home_layout_unlock_grids_new",true,"home_layout_unlock_grids_cell_x",9));inactive(read(),1);});
  run("framework stored false unchanged fingerprint denied",()->{publish(prefs("allow_hook",true,"framework_api_allow_hook",false,"framework_check_reason","SERVICE_ERROR","framework_check_name","FixtureFramework","framework_check_version","102-test","framework_check_version_code",102L));inactive(read(),1);});
  run("changed framework override same gate as Java main",()->{publish(prefs("allow_hook",true,"framework_api_allow_hook",false,"framework_check_reason","SERVICE_ERROR","framework_check_name","previous"));eq(read().flags(),1);});
  run("framework service failing cannot authorize override",()->{publish(prefs("allow_hook",true,"framework_api_allow_hook",false,"framework_check_reason","SERVICE_ERROR","framework_check_name","previous"));BaseLoad.fail=true;inactive(read(),1);});
  run("provider delayed two seconds gets only remaining three seconds",()->{ContentResolver.afterSnapshot=()->now.addAndGet(2_000_000_000L);publish(prefs("allow_hook",true));eq(read().deadline(),15_000_000_000L);});
  run("provider query at5 seconds cannot publish or renew",()->{publish(prefs("allow_hook",true));long rev=read().revision();ContentResolver.afterSnapshot=()->now.addAndGet(5_000_000_000L);check(!HomeLayoutNativeEndpointOS4.refreshFromProvider());var s=read();inactive(s,0);check(s.revision()>rev);});
  run("server deadline expires exactly at boundary",()->{publish(prefs("allow_hook",true));now.set(14_999_999_999L);eq(read().available(),1);now.incrementAndGet();inactive(read(),0);});
  run("suspend-inclusive clock expiry does not depend on nanoTime",()->{publish(prefs("allow_hook",true));now.addAndGet(86_400_000_000_000L);inactive(read(),0);});
  run("repeated reads never renew deadline or revision",()->{publish(prefs("allow_hook",true));var s=read();now.addAndGet(2_000_000_000L);for(int i=0;i<50;i++){var n=read();eq(n.deadline(),s.deadline());eq(n.revision(),s.revision());}eq(ContentResolver.queries.get(),1);});
  run("zero clock cannot authorize or refresh",()->{now.set(0);check(!HomeLayoutNativeEndpointOS4.refreshFromProvider());inactive(read(),0);});
  run("clock deadline overflow rejected",()->{now.set(Long.MAX_VALUE-2);check(!HomeLayoutNativeEndpointOS4.refreshFromProvider());inactive(read(),0);});
  run("clock moves backwards during query rejects snapshot",()->{ContentResolver.afterSnapshot=()->now.decrementAndGet();check(!HomeLayoutNativeEndpointOS4.refreshFromProvider());inactive(read(),0);});
  run("clock moves before published query start cannot extend remaining lease",()->{publish(prefs("allow_hook",true));now.decrementAndGet();inactive(read(),0);});
  run("query failure revokes entire envelope and preserves only legacy numeric cache",()->{publish(prefs("allow_hook",true,"home_layout_unlock_grids_new",true));var old=read();Object numeric=get("cachedValues");ContentResolver.failure="query";check(!HomeLayoutNativeEndpointOS4.refreshFromProvider());var s=read();inactive(s,0);eq(s.epoch(),old.epoch());check(s.revision()>old.revision());check(get("cachedValues")==numeric);});
  for(String mode:new String[]{"getAll","null","empty","columns","columnName","nullSchema","schema1","read","close"})run("bad whole cursor invalidates combined "+mode,()->{publish(prefs("allow_hook",true));ContentResolver.failure=mode;check(!HomeLayoutNativeEndpointOS4.refreshFromProvider());inactive(read(),0);});
  for(int[] dims:new int[][]{{2,6},{10,6},{4,3},{4,14}})run("out of range grid cannot activate partial envelope "+Arrays.toString(dims),()->{publish(prefs("allow_hook",true,"home_layout_unlock_grids_cell_x",dims[0],"home_layout_unlock_grids_cell_y",dims[1]));inactive(read(),0);});
  run("known disabled remains valid even if unused layout values invalid",()->{publish(prefs("allow_hook",false,"home_layout_unlock_grids_cell_x",2));inactive(read(),1);});
  run("publication during gate read cannot return old authorization or mixed data",()->{publish(prefs("allow_hook",true,"home_layout_unlock_grids_cell_x",7));AtomicInteger calls=new AtomicInteger();SystemClock.source=()->{if(calls.incrementAndGet()==2){ContentResolver.values=prefs("allow_hook",false,"home_layout_unlock_grids_cell_x",9);check(HomeLayoutNativeEndpointOS4.refreshFromProvider());}return now.get();};inactive(read(),1);});
  run("two publications racing bounded two read attempts yield unavailable",()->{publish(prefs("allow_hook",true));AtomicInteger calls=new AtomicInteger();SystemClock.source=()->{int c=calls.incrementAndGet();if(c==2||c==6){ContentResolver.values=prefs("allow_hook",true);check(HomeLayoutNativeEndpointOS4.refreshFromProvider());}return now.get();};inactive(read(),0);});
  run("late older provider query cannot overwrite newer revision",()->{Gate g=new Gate();ContentResolver.values=prefs("allow_hook",true,"home_layout_unlock_grids_cell_x",7);ContentResolver.afterSnapshot=g::block;AtomicBoolean accepted=new AtomicBoolean(true);Thread a=new Thread(()->accepted.set(HomeLayoutNativeEndpointOS4.refreshFromProvider()));a.start();g.entered();ContentResolver.afterSnapshot=()->{};publish(prefs("allow_hook",true,"home_layout_unlock_grids_cell_x",9));var newer=read();g.release.countDown();join(a);check(!accepted.get());eq(read().revision(),newer.revision());eq(read().layout().cellX(),9);});
  run("blocked old failure cannot revoke newer successful publication",()->{Gate g=new Gate();ContentResolver.values=prefs("allow_hook",true);ContentResolver.afterSnapshot=()->{g.block();throw new IllegalStateException("late failure");};AtomicBoolean ok=new AtomicBoolean(true);Thread a=new Thread(()->ok.set(HomeLayoutNativeEndpointOS4.refreshFromProvider()));a.start();g.entered();ContentResolver.afterSnapshot=()->{};publish(prefs("allow_hook",true,"home_title_hide_report_text",true));var newer=read();g.release.countDown();join(a);check(!ok.get());eq(read().revision(),newer.revision());eq(read().flags(),33);});
  run("pause retires combined and rejected reload requires live new query",()->{publish(prefs("allow_hook",true));long rev=read().revision();check(HomeLayoutNativeEndpointOS4.pauseForHotReload());inactive(read(),0);HomeLayoutNativeEndpointOS4.resumeAfterRejectedHotReload();set("refresher",new Thread());inactive(read(),0);publish(prefs("allow_hook",true));check(read().revision()>rev);eq(read().flags(),1);});
  run("inflight provider cannot publish after reload retirement",()->{Gate g=new Gate();ContentResolver.values=prefs("allow_hook",true);ContentResolver.afterSnapshot=g::block;AtomicBoolean ok=new AtomicBoolean(true);Thread a=new Thread(()->ok.set(HomeLayoutNativeEndpointOS4.refreshFromProvider()));a.start();g.entered();check(HomeLayoutNativeEndpointOS4.pauseForHotReload());g.release.countDown();join(a);check(!ok.get());inactive(read(),0);});
  run("wrong descriptor rejects before reading snapshot",()->{boolean threw=false;try{endpoint().receiveCombinedSnapshot(new Parcel("wrong"),0);}catch(SecurityException e){threw=true;}check(threw);eq(ContentResolver.queries.get(),0);});
  run("oneway trailing payload never gets authorization",()->{publish(prefs("allow_hook",true));inactive(endpoint().receiveCombinedSnapshot(new Parcel(DESC),1),0);inactive(endpoint().receiveCombinedSnapshot(new Parcel(DESC,7),0),0);});
  for(int[] id:new int[][]{{9999,42},{10100,0},{10100,-1},{10101,42},{10100,43}})run("caller auth required "+Arrays.toString(id),()->{publish(prefs("allow_hook",true));Binder.setCallingIdentityForTest(id[0],id[1]);inactive(read(),0);});
  run("default production verifier rejects non-Home process",()->{publish(prefs("allow_hook",true));Binder.setCallingIdentityForTest(10100,(int)ProcessHandle.current().pid());inactive(new HomeLayoutNativeEndpointOS4().receiveCombinedSnapshot(new Parcel(DESC),0),0);});
  System.out.println("RESULT passes="+passed+" failures="+failed+"; production Java endpoint/provider/gate and wire writer, simulated Android/clock; device NOT_TESTED");
  if(failed>0)System.exit(1);
  if(System.getProperty("hcs.corpus")!=null)writeCorpus(Path.of(System.getProperty("hcs.corpus")));
 }
 static HomeLayoutNativeEndpointOS4.CombinedSnapshot nullResult(){byte[] b=wire(null);ByteBuffer p=ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);eq(p.getInt(4),0);return new HomeLayoutNativeEndpointOS4.CombinedSnapshot(0,0,0,0,0,null);}
 static void corpus(Path dir,String name,HomeLayoutNativeEndpointOS4.CombinedSnapshot value,long observedAt){
  try {
   byte[] bytes=wire(value);Files.write(dir.resolve(name+".bin"),bytes);
   Files.writeString(dir.resolve(name+".hex"),HexFormat.of().formatHex(bytes)+"\n");
   var s=value.layout();
   String meta="{\"available\":"+value.available()+",\"flags\":"+value.flags()+",\"epoch\":"+value.epoch()+",\"revision\":"+value.revision()+",\"deadline\":"+value.deadline()+",\"observed_at_boottime_nanos\":"+observedAt+",\"grid_enabled\":"+s.gridEnabled()+",\"cell_x\":"+s.cellX()+",\"cell_y\":"+s.cellY()+",\"knob_enabled\":"+Arrays.toString(s.knobEnabled())+",\"knob_delta_dp\":"+Arrays.toString(s.knobDeltaDp())+",\"tweaks\":"+Arrays.toString(s.tweaks())+",\"size_bytes\":"+bytes.length+",\"evidence\":\"production writer; host Parcel adapter; not actual Android Binder\"}\n";
   Files.writeString(dir.resolve(name+".json"),meta);
  }catch(java.io.IOException e){throw new AssertionError(e);}
 }
 static void writeCorpus(Path dir){
  try{Files.createDirectories(dir);}catch(java.io.IOException e){throw new AssertionError(e);}
  reset();corpus(dir,"unavailable_cold",read(),now.get());
  set("combinedEpoch",0x1122334455667788L);set("combinedRevision",0x2233445566778800L);
  publish(prefs("allow_hook",true,"home_other_high_models",true,"home_other_disable_prestart",true,"home_dock_bg_custom_enable",true,"home_dock_add_blur","1","home_title_hide_report_text",true,
   "home_layout_unlock_grids_new",true,"home_layout_unlock_grids_cell_x",7,"home_layout_unlock_grids_cell_y",10,
   "home_layout_hotseats_margin_bottom_enable",true,"home_layout_hotseats_margin_bottom",100,
   "home_folder_vertical_spacing_enable",true,"home_folder_vertical_spacing",25,
   "home_layout_workspace_padding_top_enable",true,"home_layout_workspace_padding_top",45,
   "home_layout_workspace_padding_bottom_enable",true,"home_layout_workspace_padding_bottom",160,
   "home_layout_workspace_padding_horizontal_enable",true,"home_layout_workspace_padding_horizontal",30,
   "home_layout_indicator_margin_bottom_enable",true,"home_layout_indicator_margin_bottom",10,"home_other_seek_points","2",
   "home_folder_columns",4,"home_layout_pad_grid_enable",true,"home_layout_pad_major",11,"home_layout_pad_minor",7,
   "home_layout_fold_grid_enable",true,"home_layout_fold_major",9,"home_layout_fold_minor",6,
   "home_layout_icon_scale_enable",true,"home_layout_icon_scale",120,"home_layout_recents_hide_clear",true,"home_layout_recents_no_clear",true,
   "home_animation_open_rate_enable",true,"home_animation_open_rate",150,"home_animation_recents_enable",true,"home_animation_recents_rate",180,"home_dock_unlock_hotseat",true));
  var active=read();corpus(dir,"active_nondefault",active,now.get());
  now.set(active.deadline());corpus(dir,"active_response_delivered_expired",active,now.get());corpus(dir,"unavailable_expired",read(),now.get());
  publish(prefs("allow_hook",false,"home_layout_unlock_grids_new",true,"home_layout_unlock_grids_cell_x",9));corpus(dir,"disabled",read(),now.get());
  ContentResolver.failure="query";check(!HomeLayoutNativeEndpointOS4.refreshFromProvider());corpus(dir,"unavailable_provider_failure",read(),now.get());
 }
}
