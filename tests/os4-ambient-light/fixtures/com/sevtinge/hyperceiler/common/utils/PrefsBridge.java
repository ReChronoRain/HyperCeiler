package com.sevtinge.hyperceiler.common.utils;
import java.util.*;
public final class PrefsBridge {
 public static final Map<String,Object>values=new HashMap<>();public static final List<String>reads=new ArrayList<>();
 public static boolean getBoolean(String key){reads.add(key);return Boolean.TRUE.equals(values.get(key));}
 public static int getStringAsInt(String k,int d){return getInt(k,d);}
 public static int getInt(String key,int fallback){reads.add(key);Object v=values.get(key);return v instanceof Integer?(Integer)v:fallback;}
}

