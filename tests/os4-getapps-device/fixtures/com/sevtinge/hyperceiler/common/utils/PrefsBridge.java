package com.sevtinge.hyperceiler.common.utils;
import java.util.*;
public class PrefsBridge {
 public static final Map<String,Object> values=new LinkedHashMap<>(); public static int reads; public static final List<String> writes=new ArrayList<>();
 public static Map<String,Object> hookValues;
 public static class ActivePrefs {public Map<String,?> getAll(){reads++;return new LinkedHashMap<>(hookValues==null?values:hookValues);}}
 public static ActivePrefs getSharedPreferences(){return new ActivePrefs();}
 public static Map<String,?> getAll(){reads++;return new LinkedHashMap<>(values);}
 public static void putByApp(String k,Object v){values.put(k,v);writes.add(k);}
 public static boolean getBoolean(String k){return (Boolean)values.getOrDefault(k.startsWith("prefs_key_")?k:"prefs_key_"+k,false);}
 public static String getString(String k,String d){return (String)values.getOrDefault(k.startsWith("prefs_key_")?k:"prefs_key_"+k,d);}
 public static int getStringAsInt(String k,int d){return Integer.parseInt(getString(k,""+d));}
}
