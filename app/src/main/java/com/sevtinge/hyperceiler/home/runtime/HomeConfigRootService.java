/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.home.runtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Binder;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import androidx.annotation.Keep;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import com.sevtinge.hyperceiler.libhook.IHomeConfigAgent;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
import com.sevtinge.hyperceiler.libhook.provider.HomeRecentsHapticPrefs;
import com.sevtinge.hyperceiler.libhook.provider.HomeRecentCardsPrefs;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeLayoutNativeEndpointOS4;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeLayoutWire;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.HomeRecentsHapticOS4;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.HomeRecentsHapticProtocol;
import com.topjohnwu.superuser.ipc.RootService;
import org.lsposed.hiddenapibypass.HiddenApiBypass;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Typed configuration and feedback only, no caller-supplied paths or arbitrary commands. */
@Keep
public final class HomeConfigRootService extends RootService {
    private static final String TAG="HyperCeiler.HomeLocal";
    private static final String CARDS_OWNER="debug.hyperceiler.home_cards_owner";
    private final String ownerSession=Long.toHexString(System.nanoTime());
    private long ownerEpoch;
    private Boolean nativeCardsOwner;
    private final Object connectionLock=new Object();
    private final AtomicLong changes=new AtomicLong();
    private final Set<LocalSocket> channels=ConcurrentHashMap.newKeySet();
    private Handler worker;
    private HandlerThread workerThread;
    private FileObserver observer;
    private File preferences;
    private volatile boolean running=true, ready=false;
    private LocalSocket socket;
    private LocalSocket hapticSocket;
    private record HapticSnapshot(HomeRecentsHapticPrefs.Profile profile,long changes) {}
    private volatile HapticSnapshot hapticSnapshot;
    private volatile boolean frameworkReady;
    private boolean probeOnly;
    private ParcelFileDescriptor frameworkMemory;
    private long sequence;
    private int socketVersion=HomeLocalWire.VERSION;
    private int moduleUid, homeUid;
    private String lastFailure="";
    private final Runnable publish=this::publish;

    private final IHomeConfigAgent.Stub endpoint=new IHomeConfigAgent.Stub() {
        private void authorize() {
            int uid=Binder.getCallingUid();
            if(uid!=moduleUid&&uid!=0)throw new SecurityException("Module caller required");
        }
        @Override public int protocolVersion(){authorize();return HomeLocalWire.CONTROL_VERSION;}
        @Override public boolean initializeFramework(ParcelFileDescriptor shared){authorize();return initializeFrameworkMemory(shared);}
        @Override public boolean initializeFromCallerMap(long nonce){authorize();return initializeCallerMemory(Binder.getCallingPid(),nonce);}
        @Override public boolean isReady(){authorize();return ready;}
        @Override public void refresh(){authorize();worker.post(publish);}
        @Override public void stop(){authorize();requestStop();}
    };

    @Override public void onCreate() {
        super.onCreate();
        if(android.os.Process.myUid()!=0)throw new SecurityException("Root config requires uid 0");
        try {
            moduleUid=getApplicationInfo().uid;
            String backend=android.os.SystemProperties.get(HomeConfigRootClient.BACKEND_PROPERTY,"wms");
            if(!"root-socket".equals(backend)&&!"root-probe".equals(backend)) {
                // A deferred libsu launch may arrive after the experiment ended.
                new Handler(Looper.getMainLooper()).post(this::requestStop);return;
            }
            var home=getPackageManager().getApplicationInfo("com.miui.home",0);
            homeUid=home.uid;
            preferences=new File(getApplicationInfo().deviceProtectedDataDir,"shared_prefs/hyperceiler_prefs.xml");
            workerThread=new HandlerThread("hc-home-root-config");workerThread.start();
            worker=new Handler(workerThread.getLooper());
            probeOnly="root-probe".equals(backend);
            if(probeOnly){worker.postDelayed(this::requestStop,10000);return;}
            publishCardsOwner(false);
            observer=new FileObserver(preferences.getParentFile(),FileObserver.CLOSE_WRITE|FileObserver.MOVED_TO) {
                @Override public void onEvent(int event,String path) {
                    if(!preferences.getName().equals(path))return;
                    changes.incrementAndGet();worker.removeCallbacks(publish);worker.postDelayed(publish,50);
                }
            };
            observer.startWatching();
            new Thread(this::connect,"hc-home-root-channel").start();
        } catch (android.content.pm.PackageManager.NameNotFoundException failure) {
            throw new IllegalStateException("System launcher unavailable",failure);
        }
    }
    @Override public IBinder onBind(Intent intent){return endpoint;}
    @Override public boolean onUnbind(Intent intent) {
        // Daemon mode outlives its clients. Stop after the final unbind only when
        // the backend is disabled, outside libsu's service-table iteration.
        new Handler(Looper.getMainLooper()).post(() -> {
            String backend=android.os.SystemProperties.get(HomeConfigRootClient.BACKEND_PROPERTY,"wms");
            if(!"root-socket".equals(backend)&&!"root-probe".equals(backend))requestStop();
        });
        return false;
    }

    private LocalSocket newChannel() throws IOException {
        var channel=new LocalSocket();channels.add(channel);
        if(!running){channels.remove(channel);channel.close();throw new IOException("Runtime stopped");}
        return channel;
    }

    private void closeChannels() {
        // A slow writer may hold connectionLock. Closing from here also wakes a
        // blocked connect/write/read, so shutdown must not wait for that lock.
        for(var channel:channels)try{channel.close();}catch(IOException ignored){}
    }

    private void requestStop(){running=false;closeChannels();stopSelf();}

    private boolean initializeCallerMemory(int callerPid,long nonce) {
        if(nonce<=0)return false;
        try {
            for(String line:Files.readAllLines(new File("/proc/"+callerPid+"/maps").toPath())) {
                String[] columns=line.trim().split("\\s+",6);
                if(columns.length!=6||!columns[1].equals("r--s")||!columns[5].contains("ApplicationSharedMemory"))continue;
                String[] range=columns[0].split("-",2);
                long start=Long.parseUnsignedLong(range[0],16),end=Long.parseUnsignedLong(range[1],16);
                if(nonce<start||nonce>=end||Long.parseUnsignedLong(columns[2],16)!=0)continue;
                // Root reads only the Binder caller's framework-owned region. No
                // caller-supplied path, writable mapping or guessed memory layout.
                return initializeFrameworkMemory(ParcelFileDescriptor.open(
                    new File("/proc/"+callerPid+"/map_files/"+columns[0]),ParcelFileDescriptor.MODE_READ_ONLY));
            }
        } catch(IOException|RuntimeException failure) {ModuleLog.w(TAG,"Helper caller shared memory unavailable",failure);}
        return false;
    }

    private synchronized boolean initializeFrameworkMemory(ParcelFileDescriptor shared) {
        try {
            var type=Class.forName("com.android.internal.os.ApplicationSharedMemory");
            Object instance=null;
            try {instance=HiddenApiBypass.invoke(type,null,"getInstance");}
            catch(java.lang.reflect.InvocationTargetException absent) {
                if(!(absent.getCause() instanceof IllegalStateException))throw absent;
            }
            if(instance==null) {
                if(shared==null)throw new IllegalArgumentException("System shared memory descriptor required");
                frameworkMemory=ParcelFileDescriptor.dup(shared.getFileDescriptor());
                instance=HiddenApiBypass.invoke(type,null,"fromFileDescriptor",frameworkMemory.getFileDescriptor(),false);
                HiddenApiBypass.invoke(type,null,"setInstance",instance);
            }
            frameworkReady=true;
        } catch(ClassNotFoundException unsupported) {frameworkReady=true;}
        catch(ReflectiveOperationException|IOException|RuntimeException failure) {
            ModuleLog.w(TAG,"Helper system shared memory initialization failed",failure);return false;
        } finally {if(shared!=null)try{shared.close();}catch(IOException ignored){}}
        changes.incrementAndGet();
        worker.post(probeOnly?this::probeCapabilities:publish);
        return true;
    }

    private void probeCapabilities() {
        try {
            boolean interactive=getSystemService(android.os.PowerManager.class).isInteractive();
            boolean locked=getSystemService(android.app.KeyguardManager.class).isKeyguardLocked();
            var vibrator=getSystemService(android.os.Vibrator.class);
            ModuleLog.i(TAG,"Helper capability probe interactive="+interactive+" locked="+locked
                +" vibrator="+(vibrator!=null&&vibrator.hasVibrator())
                +" amplitude="+(vibrator!=null&&vibrator.hasAmplitudeControl()));
            if(android.os.SystemProperties.getBoolean("debug.hyperceiler.home_glass_probe",false)) {
                var result=com.sevtinge.hyperceiler.libhook.provider.HomeDockGlassProbe.run(this);
                ModuleLog.i(TAG,"Helper unparented glass probe frameCommitted="+result.getBoolean("frameCommitted")
                    +" error="+result.getString("error","")
                    +", background sampling and desktop attachment not verified");
            }
        } catch(RuntimeException failure) {ModuleLog.w(TAG,"Helper capability probe failed",failure);}
        finally {stopSelf();}
    }

    private void publish() {
        final boolean waveChannel;
        synchronized(connectionLock){if(socket==null)return;waveChannel=socketVersion>=7;}
        final long version=changes.get();
        Parcel parcel=Parcel.obtain();
        try {
            if(!preferences.isFile())throw new IllegalStateException("Physical preference file unavailable");
            // A fresh framework instance reads one complete physical XML revision,
            // independent of the module UI process and its in-memory preferences.
            SharedPreferences prefs=(SharedPreferences)HiddenApiBypass.newInstance(
                Class.forName("android.app.SharedPreferencesImpl"),preferences,MODE_PRIVATE);
            var values=prefs.getAll();
            var profile=HomeRecentsHapticPrefs.profile(values);
            var cards=HomeRecentCardsPrefs.selection(values);
            var dock=com.sevtinge.hyperceiler.libhook.provider.HomeDockPrefs.profile(values);
            if(profile==null)throw new IllegalStateException("Physical feedback snapshot rejected");
            if(!frameworkReady)profile=new HomeRecentsHapticPrefs.Profile(false,false,profile.type(),profile.strength(),profile.custom(),profile.effectId());
            try(var cursor=HomeLayoutPrefsSnapshot.cursorValues(values)) {
                var snapshot=HomeLayoutNativeEndpointOS4.readLocalSnapshot(this,cursor);
                if(snapshot==null)throw new IllegalStateException("Physical preference snapshot rejected");
                if(changes.get()!=version){worker.post(publish);return;}
                HomeLayoutWire.write(parcel,snapshot.layout(),snapshot.gestures(),android.os.Process.myPid());
            }
            byte[] bytes=parcel.marshall();
            byte[][] waves=null;
            if(waveChannel&&"launcher-published".equals(android.os.SystemProperties.get(
                "debug.hyperceiler.home_haptic_backend","launcher-published"))) {
                int count=profile.enabled()?8:profile.close()?1:0;
                if(count>0)try {
                    var prepared=new byte[count][];
                    for(int i=0;i<count;i++) {
                        prepared[i]=hapticWave(profile,i+1);
                        if(prepared[i]==null){prepared=null;break;}
                    }
                    waves=prepared;
                } catch(RuntimeException error) {
                    ModuleLog.w(TAG,"Snapshot waveforms unavailable",error);
                }
            }
            if(changes.get()!=version){worker.post(publish);return;}
            synchronized(connectionLock) {
                if(socket==null)return;
                hapticSnapshot=new HapticSnapshot(profile,version);
                HomeLocalWire.writeSnapshot(new DataOutputStream(socket.getOutputStream()),bytes,profile,cards,dock,waves,++sequence,socketVersion);
            }
            lastFailure="";
        } catch (ReflectiveOperationException|IOException|RuntimeException|LinkageError failure) {
            if(!running)return;
            String reason=failure.getClass().getSimpleName()+": "+failure.getMessage();
            if(!reason.equals(lastFailure)){ModuleLog.w(TAG,"Root snapshot unavailable: "+reason);lastFailure=reason;}
            if(failure instanceof IOException) {
                synchronized(connectionLock){if(socket!=null)try{socket.close();}catch(IOException ignored){}}
            }
            ready=false;
            hapticSnapshot=null;
            worker.removeCallbacks(publish);worker.postDelayed(publish,10_000);
        } finally {parcel.recycle();}
    }

    private void connect() {
        int attempt=0;
        while(running&&"root-socket".equals(android.os.SystemProperties.get(HomeConfigRootClient.BACKEND_PROPERTY,"wms"))) {
            try {
                int pid=findLauncher();
                if(pid<=0)throw new IOException("Launcher process unavailable");
                LocalSocket connection=null;
                int version=HomeLocalWire.VERSION;
                for(;version>=4;version--) {
                    LocalSocket candidate=newChannel();
                    try {
                        String suffix=version==4?"":Integer.toString(version);
                        candidate.connect(new LocalSocketAddress("hc.home.config"+suffix+"."+homeUid+"."+pid));
                        connection=candidate;break;
                    } catch(IOException unavailable) {
                        channels.remove(candidate);candidate.close();
                        if(version==4)throw unavailable;
                    }
                }
                try(LocalSocket active=connection) {
                    var identity=active.getPeerCredentials();
                    if(identity.getUid()!=homeUid||identity.getPid()!=pid)throw new IOException("Launcher peer identity mismatch");
                    synchronized(connectionLock){socket=active;sequence=0;socketVersion=version;}
                    new Thread(() -> serveHaptics(pid,active),"hc-home-root-haptic").start();
                    worker.post(publish);
                    var input=new DataInputStream(active.getInputStream());
                    HomeLocalWire.ConfigurationAck previous=null;
                    java.util.function.LongSupplier published=()->{synchronized(connectionLock){return sequence;}};
                    while(running) {
                        var acknowledgement=HomeLocalWire.readConfigurationAck(input,published,previous);previous=acknowledgement;
                        if(acknowledgement.accepted()&&!ready)ModuleLog.i(TAG,"Root configuration accepted by launcher pid="+pid+" protocol="+version);
                        ready=acknowledgement.accepted();
                        publishCardsOwner(ready&&acknowledgement.nativeCards());
                        if(ready)attempt=0;
                    }
                } finally {channels.remove(connection);}
            } catch(IOException|RuntimeException failure) {
                if(running&&attempt==0)ModuleLog.i(TAG,"Waiting for native configuration channel: "+failure.getMessage());
            } finally {synchronized(connectionLock){
                socket=null;ready=false;hapticSnapshot=null;
                publishCardsOwner(false);
                if(hapticSocket!=null)try{hapticSocket.close();}catch(IOException ignored){}
                hapticSocket=null;
            }}
            try {Thread.sleep(new long[]{500,2000,10000,30000}[Math.min(attempt++,3)]);}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();break;}
        }
        stopSelf();
    }
    private void publishCardsOwner(boolean nativeBackend) {
        synchronized(connectionLock) {
            if(nativeCardsOwner!=null&&nativeCardsOwner==nativeBackend)return;
            String token=(nativeBackend?"native:":"legacy:")+ownerSession+"-"+Long.toHexString(++ownerEpoch);
            try {
                android.os.SystemProperties.set(CARDS_OWNER,token);nativeCardsOwner=nativeBackend;
                ModuleLog.i(TAG,"Recent-card execution owner="+(nativeBackend?"launcher":"legacy")+", generation="+token);
            } catch(RuntimeException failure){ModuleLog.w(TAG,"Recent-card owner publication failed, previous owner retained",failure);}
        }
    }

    private byte[] hapticWave(HomeRecentsHapticPrefs.Profile profile,int count) {
        var effect=HomeRecentsHapticOS4.waveform(this,profile,count);
        if(effect==null)return null;
        Parcel arguments=Parcel.obtain();
        try {
            // The authenticated Home UID owns the request and appends its token.
            arguments.writeInt(homeUid);arguments.writeInt(getDeviceId());
            arguments.writeString("com.miui.home");
            arguments.writeTypedObject(android.os.CombinedVibration.createParallel(effect),0);
            arguments.writeTypedObject(android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_TOUCH),0);
            arguments.writeString(null);
            var wave=arguments.marshall();
            return wave.length>0&&wave.length<=HomeLocalWire.MAX_HAPTIC_WAVE&&(wave.length&3)==0?wave:null;
        } finally {arguments.recycle();}
    }

    private void serveHaptics(int pid,LocalSocket owner) {
        int attempt=0;
        while(running) {
            synchronized(connectionLock){if(socket!=owner)return;}
            LocalSocket connection;
            try {connection=newChannel();}
            catch(IOException stopped){return;}
            try(connection) {
                connection.connect(new LocalSocketAddress("hc.home.haptic."+homeUid+"."+pid));
                var identity=connection.getPeerCredentials();
                if(identity.getUid()!=homeUid||identity.getPid()!=pid)throw new IOException("Feedback peer identity mismatch");
                synchronized(connectionLock){if(socket!=owner)return;hapticSocket=connection;}
                var input=new DataInputStream(connection.getInputStream());
                var output=new DataOutputStream(connection.getOutputStream());
                int reported=0;
                while(running) {
                    var request=HomeLocalWire.readHaptic(input);
                    var current=hapticSnapshot;
                    boolean performed=false;
                    byte[] wave=null;
                    if(current!=null&&current.changes()==changes.get()
                        &&request.profile()==current.profile().encoded()
                        &&request.operation()>=1&&request.operation()<=2
                        &&HomeRecentsHapticProtocol.validExtendedRequest(request.operation(),request.timestamp(),request.count(),
                            0,0,android.os.SystemClock.uptimeMillis())
                        &&(request.operation()==2?current.profile().close():current.profile().enabled())
                        &&getSystemService(android.os.PowerManager.class).isInteractive()
                        &&!getSystemService(android.app.KeyguardManager.class).isKeyguardLocked()) {
                        if(request.prepare())wave=hapticWave(current.profile(),request.count());
                        else performed=HomeRecentsHapticOS4.perform(this,current.profile(),request.count());
                    }
                    if(request.prepare())HomeLocalWire.writeHapticWave(output,request.timestamp(),wave);
                    else HomeLocalWire.writeHapticReply(output,request.timestamp(),performed);
                    attempt=0;
                    if(reported++<8)ModuleLog.i(TAG,"local feedback operation="+request.operation()+" performed="+performed+" prepared="+(wave!=null)
                        +" profile="+request.profile()+" count="+request.count());
                }
            } catch(IOException|RuntimeException failure) {
                if(running&&attempt==0)ModuleLog.w(TAG,"Local feedback channel closed",failure);
            } finally {
                if(connection!=null)channels.remove(connection);
                synchronized(connectionLock){if(hapticSocket==connection)hapticSocket=null;}
            }
            synchronized(connectionLock){if(socket!=owner)return;}
            try {Thread.sleep(new long[]{500,2000,10000,30000}[Math.min(attempt++,3)]);}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();return;}
        }
    }

    private int findLauncher() throws IOException {
        File[] processes=new File("/proc").listFiles();
        if(processes==null)return -1;
        for(File entry:processes) {
            String name=entry.getName();
            if(name.isEmpty()||!name.chars().allMatch(Character::isDigit))continue;
            try {
                String cmd=new String(Files.readAllBytes(new File(entry,"cmdline").toPath()),StandardCharsets.UTF_8);
                int end=cmd.indexOf('\0');
                if((end<0?cmd:cmd.substring(0,end)).equals("com.miui.home"))return Integer.parseInt(name);
            } catch(IOException ignored) { /* Processes can exit during enumeration. */ }
        }
        return -1;
    }
    @Override public void onDestroy() {
        running=false;
        if(!probeOnly)publishCardsOwner(false);
        closeChannels();
        if(observer!=null)observer.stopWatching();
        if(workerThread!=null)workerThread.quitSafely();
        if(frameworkMemory!=null)try{frameworkMemory.close();}catch(IOException ignored){}
        super.onDestroy();
    }
}
