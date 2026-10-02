package android.content;
import java.util.Map; import java.util.Set;
public interface SharedPreferences {
 Map<String,?> getAll(); String getString(String k,String d); Set<String> getStringSet(String k,Set<String>d);
 int getInt(String k,int d); long getLong(String k,long d); float getFloat(String k,float d); boolean getBoolean(String k,boolean d); boolean contains(String k);
 Editor edit(); void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l); void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l);
 interface OnSharedPreferenceChangeListener { void onSharedPreferenceChanged(SharedPreferences s,String k); }
 interface Editor { Editor putString(String k,String v); Editor putStringSet(String k,Set<String>v); Editor putInt(String k,int v); Editor putLong(String k,long v); Editor putFloat(String k,float v); Editor putBoolean(String k,boolean v); Editor remove(String k); Editor clear(); boolean commit(); void apply(); }
}
