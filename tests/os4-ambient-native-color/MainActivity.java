package local.hyperceiler.colorprobe;
import android.app.Activity;
import android.os.Bundle;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.*;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.AmbientNativeColorOS4;
public final class MainActivity extends Activity {
 static {System.loadLibrary("oemprobe");System.loadLibrary("HyperCeilerAmbientOS4");}
 private static native void loadOem(String p);private static native int oem(int[] p,int w,int h);private static native void fault(int s);private static native int forceCalls();private static native int initAttempts();private static native long special(int mode,int[] p);
 static Set<String> tasks(){String[] ids=new File("/proc/self/task").list();if(ids==null)throw new AssertionError("task list");return new TreeSet<>(Arrays.asList(ids));}
 static int[] pixels(int color){int[] p=new int[12544];Arrays.fill(p,color);return p;}
 @Override public void onCreate(Bundle b){super.onCreate(b);new Thread(()->{try{run();}catch(Throwable t){android.util.Log.e("ColorProbe","probe failed",t);try{JSONObject j=new JSONObject();j.put("error",t.toString());save(j);}catch(Exception ignored){}}},"actual-production-color-probe").start();}
 void save(JSONObject j)throws Exception{j.put("init_attempts",initAttempts());try(FileOutputStream f=new FileOutputStream(new File(getFilesDir(),"report.json"))){f.write(j.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}}
 void run()throws Exception{
  int scenario=getIntent().getIntExtra("scenario",0);File file=new File(getFilesDir(),"libMiMainColor.so");Files.copy(Paths.get(getApplicationInfo().nativeLibraryDir,"libMiMainColor.so"),file.toPath(),StandardCopyOption.REPLACE_EXISTING);
  fault(scenario);int[] red=pixels(0xffff0000);JSONObject j=new JSONObject();j.put("scenario",scenario);
  Set<String> beforeOem=tasks();loadOem(file.getAbsolutePath());Thread.sleep(80);Set<String> before=tasks();
  int invalid=0;if(AmbientNativeColorOS4.nativeResolve(null,1,1)==0)invalid++;if(AmbientNativeColorOS4.nativeResolve(red,0,1)==0)invalid++;if(AmbientNativeColorOS4.nativeResolve(red,-1,1)==0)invalid++;if(AmbientNativeColorOS4.nativeResolve(red,Integer.MAX_VALUE,Integer.MAX_VALUE)==0)invalid++;if(AmbientNativeColorOS4.nativeResolve(red,red.length+1,1)==0)invalid++;
  j.put("invalid_inputs",invalid);j.put("loads_after_invalid",forceCalls());j.put("attempts_after_invalid",initAttempts());
  if(scenario==11){if(!file.setWritable(true))throw new AssertionError("chmod");try(RandomAccessFile f=new RandomAccessFile(file,"rw")){f.seek(100);int old=f.read();f.seek(100);f.write(old^1);}}else if(scenario==12){file.delete();}
  long value;
  if(scenario==17){CountDownLatch first=new CountDownLatch(1);AtomicInteger good=new AtomicInteger();int expected=oem(red,112,112);Thread[] concurrent=new Thread[8];for(int k=0;k<8;k++){concurrent[k]=new Thread(()->{try{first.await();if(AmbientNativeColorOS4.nativeResolve(red,112,112)==(0x100000000L|Integer.toUnsignedLong(expected)))good.incrementAndGet();}catch(Exception ignored){}});concurrent[k].start();}first.countDown();for(Thread t:concurrent)t.join();j.put("first_concurrent_equal",good.get());value=AmbientNativeColorOS4.nativeResolve(red,112,112);}
  else value=AmbientNativeColorOS4.nativeResolve(red,112,112);Thread.sleep(80);Set<String> after=tasks();Set<String> added=new TreeSet<>(after);added.removeAll(before);JSONObject state=new JSONObject();for(String id:added){try{state.put(id,new String(Files.readAllBytes(Paths.get("/proc/self/task",id,"wchan")),java.nio.charset.StandardCharsets.UTF_8).trim());}catch(Exception e){state.put(id,"unavailable");}}
  j.put("initial_result",value);j.put("original_added_threads",before.size()-beforeOem.size());j.put("private_added_threads",after.size()-before.size());j.put("new_private_thread_waits",state);
  if(scenario>=13&&scenario<=15){long contract=special(scenario,red);j.put("special_contract",contract);if(scenario==13)j.put("after_jni_exception_recovers",AmbientNativeColorOS4.nativeResolve(red,112,112)==value);j.put("force_calls",forceCalls());save(j);return;}
  if(scenario==16){int[] large=new int[25088];Arrays.fill(large,0xff0000ff);j.put("max_allowed_equal",AmbientNativeColorOS4.nativeResolve(large,112,224)==(0x100000000L|Integer.toUnsignedLong(oem(large,112,224))));j.put("above_max_rejected",AmbientNativeColorOS4.nativeResolve(new int[25089],1,25089)==0);j.put("force_calls",forceCalls());save(j);return;}
  if(scenario!=0&&scenario!=10&&scenario!=17){int loads=forceCalls(),attempts=initAttempts();for(int k=0;k<12;k++)if(AmbientNativeColorOS4.nativeResolve(red,112,112)!=0)throw new AssertionError("failure reopened");j.put("all_failed",value==0);j.put("no_retry",forceCalls()==loads&&initAttempts()==attempts);j.put("force_calls",forceCalls());save(j);return;}
  int equal=0;Random random=new Random(0x5123);int[] p=new int[12544];for(int t=0;t<136;t++){for(int k=0;k<p.length;k++)p[k]=t<8?new int[]{0xffff0000,0xff00ff00,0xff0000ff,0xffffff00,0xffff00ff,0xff00ffff,0xff888888,0xff123456}[t]:0xff000000|random.nextInt(1<<24);long actual=AmbientNativeColorOS4.nativeResolve(p,112,112);if(actual==(0x100000000L|Integer.toUnsignedLong(oem(p,112,112))))equal++;}j.put("serial_equal",equal);
  int[] colors={0xffff0000,0xff00ff00,0xff0000ff,0xffff00ff,0xff00ffff};int[][] arrays=new int[5][];int[] expected=new int[5];for(int k=0;k<5;k++){arrays[k]=pixels(colors[k]);expected[k]=oem(arrays[k],112,112);}CountDownLatch start=new CountDownLatch(1);AtomicInteger bad=new AtomicInteger();Thread[] workers=new Thread[5];for(int i=0;i<5;i++){final int n=i;workers[i]=new Thread(()->{try{start.await();for(int k=0;k<200;k++){long result=n==4?(0x100000000L|Integer.toUnsignedLong(oem(arrays[n],112,112))):AmbientNativeColorOS4.nativeResolve(arrays[n],112,112);if(result!=(0x100000000L|Integer.toUnsignedLong(expected[n])))bad.incrementAndGet();}}catch(Exception e){bad.incrementAndGet();}});workers[i].start();}start.countDown();for(Thread t:workers)t.join();Thread.sleep(80);j.put("parallel_calls",1000);j.put("parallel_mismatches",bad.get());j.put("force_calls",forceCalls());Set<String> end=tasks();j.put("private_threads_survive",end.containsAll(added));j.put("new_tasks_after_repeated",end.size()-after.size());save(j);
 }
}
