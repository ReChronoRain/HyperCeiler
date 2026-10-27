package com.sevtinge.hyperceiler.libhook.rules.home.dock
import android.os.*
import com.android.server.wm.*
private var checks=0
private fun c(b:Boolean){checks++;check(b){"check $checks"}}
fun main(){
 val wm=WindowManagerService();val dc=DisplayContent();val home=WindowState(dc,Session(10123,20));val app=WindowState(dc,Session(10124,30));dc.mCurrentFocus=home
 val logs=mutableListOf<String>();val p=HomeRecentsStatusBarOS4{logs.add(it)}
 p.install(WindowState::class.java);p.bind(home,wm);wm.mH.drain()
 c(logs.any{it.contains("installed")});c(home.getRequestedVisibleTypes()==15)
 Binder.setCallingIdentityForTest(10123,20)
 var seq=0L;var rev=0L
 fun send(mask:Long,revision:Long=++rev,flags:Int=0,descriptor:String="android.view.IWindowManager",trailing:Boolean=false):Int {
  val values=mutableListOf(++seq,System.nanoTime(),mask,revision,revision);if(trailing)values.add(123)
  val input=Parcel(descriptor,*values.toLongArray());val out=Parcel("")
  p.receive(input,out,flags);wm.mH.drain();out.setDataPosition(0);c(out.readInt()==HomeRecentsStatusBarOS4.ACK);val available=out.readInt();val enabled=out.readInt();c(enabled==0||available==1);c(out.dataAvail()==0);return enabled
 }
 c(send(9)==1);c(home.getRequestedVisibleTypes()==14);c(!home.isRequestedVisible(1));c(home.isRequestedVisible(2));c(home.types==15);c(dc.policy.appliedTypes==14)
 c(send(8)==1);c(home.getRequestedVisibleTypes()==15);c(dc.policy.appliedTypes==15)
 c(send(10)==1);c(home.getRequestedVisibleTypes()==14)
 dc.mCurrentFocus=app;c(home.getRequestedVisibleTypes()==15);c(app.getRequestedVisibleTypes()==15)
 dc.mCurrentFocus=home;wm.locked=true;c(home.getRequestedVisibleTypes()==15);wm.locked=false;c(home.getRequestedVisibleTypes()==14)
 home.shown=false;c(home.getRequestedVisibleTypes()==15);home.shown=true
 val current=rev;c(send(8,current)==0);c(home.getRequestedVisibleTypes()==14) // same-revision mismatch rejected
 c(send(8,flags=IBinder.FLAG_ONEWAY)==0);c(home.getRequestedVisibleTypes()==14)
 c(send(8,descriptor="wrong")==0);c(home.getRequestedVisibleTypes()==14)
 c(send(8,trailing=true)==0);c(home.getRequestedVisibleTypes()==14)
 Binder.setCallingIdentityForTest(10124,20);c(send(8)==0);c(home.getRequestedVisibleTypes()==14);Binder.setCallingIdentityForTest(10123,20)
 HomeLayoutNativeEndpointOS4.pref=HomeLayoutNativeEndpointOS4.RecentsBarPreference(true,false,Long.MAX_VALUE)
 c(send(9)==0);c(home.getRequestedVisibleTypes()==15);c(dc.policy.appliedTypes==15)
 HomeLayoutNativeEndpointOS4.pref=HomeLayoutNativeEndpointOS4.RecentsBarPreference(true,true,SystemClock.elapsedRealtimeNanos()+100_000_000)
 c(send(12)==1);c(home.getRequestedVisibleTypes()==14)
 val prefDeadline=HomeLayoutNativeEndpointOS4.pref.deadline();SystemClock.source=java.util.function.LongSupplier{prefDeadline}
 wm.mH.expire();c(home.getRequestedVisibleTypes()==15);c(dc.policy.appliedTypes==15);SystemClock.source=java.util.function.LongSupplier{System.nanoTime()}
 HomeLayoutNativeEndpointOS4.pref=HomeLayoutNativeEndpointOS4.RecentsBarPreference(true,true,Long.MAX_VALUE)
 c(send(9)==1);c(home.getRequestedVisibleTypes()==14)
 val replacement=WindowState(dc,Session(10123,21));dc.mCurrentFocus=replacement;p.bind(replacement,wm);wm.mH.drain()
 c(home.getRequestedVisibleTypes()==15);c(replacement.getRequestedVisibleTypes()==15)
 c(send(9)==0);c(replacement.getRequestedVisibleTypes()==15)
 Binder.setCallingIdentityForTest(10123,21);seq=0;rev=0;c(send(9)==1);c(replacement.getRequestedVisibleTypes()==14)
 p.close();wm.mH.drain();c(replacement.getRequestedVisibleTypes()==15);c(dc.policy.appliedTypes==15)
 c(send(9)==0)
 println("recents bar WMS lifecycle $checks checks passed, simulated WMS and hook dispatch")
}
