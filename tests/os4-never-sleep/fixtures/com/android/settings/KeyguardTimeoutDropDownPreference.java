package com.android.settings;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import java.util.*;
/** A host model of the target ROM methods, transcribed from reviewed DEX smali.
 * It deliberately omits Android views, adapters and actual SettingsProvider IPC.
 */
public class KeyguardTimeoutDropDownPreference {
 public Context mContext; public boolean oled=true,disabledByAdmin,enabled=true;
 public int originalCalls,restrictionUpdates,providerWrites; public long storedTimeout=30000;
 public String value="30000";
 public CharSequence[] entries={"15 秒","30 秒","1 分钟","2 分钟","5 分钟","10 分钟","永不"};
 public CharSequence[] values={"15000","30000","60000","120000","300000","600000","2147483647"};
 public KeyguardTimeoutDropDownPreference(Context context){mContext=context;}
 public CharSequence[] getEntries(){return entries;}
 public CharSequence[] getEntryValues(){return values;}
 public void updateDisableState(boolean noTimeoutLimit){restrictionUpdates++;disabledByAdmin=noTimeoutLimit&&mContext.restricted;}
 public void setEntries(CharSequence[] e){entries=e;}
 public void setEntryValues(CharSequence[] e){values=e;}
 public boolean isDisabledByAdmin(){return disabledByAdmin;}
 public void disableUnusableTimeouts(){
  originalCalls++;
  DevicePolicyManager policy=(DevicePolicyManager)mContext.getSystemService("device_policy");
  long max=policy==null?0:policy.getMaximumTimeToLock(null);
  updateDisableState(max==0);
  if(oled&&!isDisabledByAdmin())max=max==0?2147483646L:Math.min(max,2147483646L);
  if(max==0)return;
  CharSequence[] originalEntries=getEntries(), originalValues=getEntryValues();
  var newEntries=new ArrayList<CharSequence>();var newValues=new ArrayList<CharSequence>();
  for(int i=0;i<originalValues.length;i++)if(Long.parseLong(originalValues[i].toString())<=max){
   newEntries.add(originalEntries[i]);newValues.add(originalValues[i]);
  }
  if(newValues.isEmpty()){enabled=false;return;}
  enabled=true;
  if(newEntries.size()!=originalEntries.length||newValues.size()!=originalValues.length){
   int oldValue=Integer.parseInt(value);
   setEntries(newEntries.toArray(new CharSequence[0]));setEntryValues(newValues.toArray(new CharSequence[0]));
   if(oldValue<=max)value=String.valueOf(oldValue);
   else if(Long.parseLong(newValues.get(newValues.size()-1).toString())==max)value=String.valueOf(max);
  }
 }
 public void onPreferenceChange(String selected){storedTimeout=Integer.parseInt(selected);providerWrites++;value=selected;}
}
