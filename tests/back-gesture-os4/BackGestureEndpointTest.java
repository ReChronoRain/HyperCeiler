package com.sevtinge.hyperceiler.libhook.rules.home.dock;
import android.content.ContentResolver;
import android.os.Binder;
import android.os.Parcel;
import java.util.*;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
public final class BackGestureEndpointTest {
 static int checks; static void check(boolean b){++checks;if(!b)throw new AssertionError("check "+checks);}
 public static void main(String[] args){
  ContentResolver.setSpecs(HomeLayoutPrefsSnapshot.specs());
  for(int height:new int[]{10,25,60,100,9,101})for(int width:new int[]{100,200,400,99,401}){
   ContentResolver.setRows(Map.of("prefs_key_home_navigation_back_area_height",height,"prefs_key_home_navigation_back_area_width",width));
   check(HomeLayoutNativeEndpointOS4.refreshFromProvider());var s=HomeLayoutNativeEndpointOS4.readPreferences();
   check(Arrays.equals(s.backGesture(),new int[]{height>=10&&height<=100?height:60,width>=100&&width<=400?width:100}));
  }
  ContentResolver.setRows(Map.of());check(HomeLayoutNativeEndpointOS4.refreshFromProvider());check(Arrays.equals(HomeLayoutNativeEndpointOS4.readPreferences().backGesture(),new int[]{60,100}));
  Binder.setCallingIdentityForTest(10100,42);int[] knobs=new int[8];int[] tweaks={3,0,8,5,0,8,5,0,0x66,0,0,0,0,100,0,100,0};
  for(int[] v:new int[][]{null,new int[1],{9,100},{101,100},{60,99},{60,401},{25,200},{100,400}}){
   var e=new HomeLayoutNativeEndpointOS4((uid,pid)->true,()->new HomeLayoutNativeEndpointOS4.Snapshot(0,1,4,6,knobs,knobs,tweaks,new int[]{12,12,-1,0},new String[0][],false,new int[7],v));
   var s=e.receive(new Parcel("android.view.IWindowManager"),0);boolean valid=v!=null&&v.length==2&&v[0]>=10&&v[0]<=100&&v[1]>=100&&v[1]<=400;
   check((s.gridEnabled()==1)==valid);if(valid)check(Arrays.equals(s.backGesture(),v));
  }
  System.out.println("BACK_GESTURE_JAVA="+checks+" checks; failed=0");
 }
}
