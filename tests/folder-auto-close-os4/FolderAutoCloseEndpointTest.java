package com.sevtinge.hyperceiler.libhook.rules.home.dock;
import android.content.ContentResolver;
import android.os.Binder;
import android.os.Parcel;
import java.util.Map;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
public final class FolderAutoCloseEndpointTest {
 static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 public static void main(String[]args){
  ContentResolver.setSpecs(HomeLayoutPrefsSnapshot.specs());
  for(int value:new int[]{0,1,0,1}){
   ContentResolver.setRows(Map.of("prefs_key_home_folder_auto_close",value));
   check(HomeLayoutNativeEndpointOS4.refreshFromProvider());var read=HomeLayoutNativeEndpointOS4.readPreferences();
   check(read.folderAutoClose()==(value==1));
   Binder.setCallingIdentityForTest(10100,42);var e=new HomeLayoutNativeEndpointOS4((u,p)->true,()->read);
   check(e.receive(new Parcel("android.view.IWindowManager"),0).folderAutoClose()==(value==1));
   check(!new HomeLayoutNativeEndpointOS4((u,p)->false,()->read).receive(new Parcel("android.view.IWindowManager"),0).folderAutoClose());
  }
  ContentResolver.setRows(Map.of());check(HomeLayoutNativeEndpointOS4.refreshFromProvider());check(!HomeLayoutNativeEndpointOS4.readPreferences().folderAutoClose());
  System.out.println("FOLDER_AUTO_CLOSE_JAVA="+checks+" checks; failed=0; physical provider/authorized snapshot/default off");
 }
}
