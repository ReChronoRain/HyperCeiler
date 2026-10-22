package com.sevtinge.hyperceiler.dashboard;
import java.util.*;import android.os.Bundle;import androidx.preference.*;import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
/** AndroidX DataStore initial-value sequence transcribed from pinned javap; not a real View. */
public abstract class DashboardFragment {
 public final Map<String,Preference> widgets=new HashMap<>();public abstract int getPreferenceScreenResId();public void initPrefs(){}
 public void onCreatePreferencesBefore(Bundle b,String key){
  String prefix="prefs_key_market_device_modify_";
  Object value=PrefsBridge.values.get(prefix+"new1");boolean checked=value==null?false:(Boolean)value;
  if(value==null)PrefsBridge.putByApp(prefix+"new1",checked);
  var toggle=new SwitchPreference();toggle.checked=checked;widgets.put(prefix+"new1",toggle);
  for(String name:new String[]{"model","device","manufacturer"}){var p=new EditTextPreference((String)PrefsBridge.values.get(prefix+name));p.enabled=checked;widgets.put(prefix+name,p);}
  widgets.put("prefs_key_market_bypass_risk_check",new SwitchPreference());initPrefs();
 }
 @SuppressWarnings("unchecked") public <T extends Preference>T findPreference(String key){return(T)widgets.get(key);}
 public void setFuncHint(Preference p,int value){p.enabled=false;}
 public LegacyPrefs getSharedPreferences(){return new LegacyPrefs();}
 public static class LegacyPrefs{public String getString(String k,String d){return PrefsBridge.getString(k,d);}}
}
