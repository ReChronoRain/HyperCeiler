package com.sevtinge.hyperceiler.libhook.rules.home.dock;
import android.content.ContentResolver;
import android.os.Binder;
import android.os.Parcel;
import java.util.*;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
public final class FolderLayoutEndpointTest {
 static int checks;
 static void check(boolean value) { checks++; if(!value) throw new AssertionError("check "+checks); }
 public static void main(String[] args) {
  ContentResolver.setSpecs(HomeLayoutPrefsSnapshot.specs());
  Map<String,Integer> rows=new HashMap<>();
  rows.put("prefs_key_home_folder_title_pos",1);
  rows.put("prefs_key_home_folder_width",1);
  rows.put("prefs_key_home_folder_horizontal_padding_enable",1);
  rows.put("prefs_key_home_folder_horizontal_padding",23);
  rows.put("prefs_key_home_folder_horizontal_padding_pad_h",100);
  rows.put("prefs_key_home_folder_horizontal_padding_pad_v",80);
  rows.put("prefs_key_home_folder_columns",6);
  rows.put("prefs_key_home_folder_vertical_spacing_enable",1);
  rows.put("prefs_key_home_folder_vertical_spacing",48);
  ContentResolver.setRows(rows);
  check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
  var snapshot=HomeLayoutNativeEndpointOS4.readPreferences();
  check(Arrays.equals(snapshot.folder(),new int[]{1,1,1,23,100,80,0}));
  check(snapshot.tweaks().length==17&&snapshot.tweaks()[0]==6&&snapshot.tweaks()[1]==1);
  check(snapshot.knobEnabled()[1]==1&&snapshot.knobDeltaDp()[1]==48);
  for (String key:List.of("home_folder_title_pos","home_folder_width","home_folder_horizontal_padding_enable","home_folder_horizontal_padding","home_folder_horizontal_padding_pad_h","home_folder_horizontal_padding_pad_v"))
    check(Arrays.stream(HomeLayoutPrefsSnapshot.specs()).anyMatch(row->row[1].equals(key)));
  for (int i=0;i<6;i++) {
   String[] keys={"home_folder_title_pos","home_folder_width","home_folder_horizontal_padding_enable","home_folder_horizontal_padding","home_folder_horizontal_padding_pad_h","home_folder_horizontal_padding_pad_v"};
   // Integer slider/dropdown corruption falls back to that field's neutral default.
   if(i==1||i==2)continue;
   var bad=new HashMap<>(rows);bad.put("prefs_key_"+keys[i],9999);ContentResolver.setRows(bad);
   check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
   check(HomeLayoutNativeEndpointOS4.readPreferences().folder()[i]==0);
  }
  ContentResolver.setRows(Map.of());check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
  check(Arrays.equals(HomeLayoutNativeEndpointOS4.readPreferences().folder(),new int[7]));
  check(HomeLayoutNativeEndpointOS4.readPreferences().tweaks()[0]==3);
  Binder.setCallingIdentityForTest(10100,42);
  int[] knobs=new int[8];int[] tweaks={3,0,8,5,0,8,5,0,0x66,0,0,0,0,100,0,100,0};
  int[][] malformed={new int[6],new int[]{2,0,0,0,0,0,0},new int[]{0,1,1,51,0,0,0},new int[]{0,1,1,0,451,0,0},new int[]{0,1,1,0,0,201,0},new int[]{0,0,0,0,0,0,-1}};
  for(int[] bad:malformed){
    var endpoint=new HomeLayoutNativeEndpointOS4((uid,pid)->true,()->new HomeLayoutNativeEndpointOS4.Snapshot(0,1,4,6,knobs,knobs,tweaks,new int[]{12,12,-1,0},new String[0][],false,bad));
    check(endpoint.receive(new Parcel("android.view.IWindowManager"),0).gridEnabled()==0);
  }
  System.out.println("FOLDER_JAVA="+checks+" checks; failed=0");
 }
}
