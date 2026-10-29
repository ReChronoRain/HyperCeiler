package android.os;
public class Bundle {
 private final java.util.Map<String,Object> values=new java.util.HashMap<>();
 public void putBoolean(String key,boolean value){values.put(key,value);}
 public void putString(String key,String value){values.put(key,value);}
 public void putInt(String key,int value){values.put(key,value);}
 public int getInt(String key,int fallback){Object value=values.get(key);return value instanceof Integer number?number:fallback;}
}
