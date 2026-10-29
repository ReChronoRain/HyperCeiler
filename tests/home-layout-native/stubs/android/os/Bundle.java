package android.os;
public class Bundle {
    private final java.util.Map<String,Object> values = new java.util.HashMap<>();
    public void putInt(String key, int value) { values.put(key,value); }
    public int getInt(String key, int fallback) { Object v=values.get(key);return v instanceof Integer i?i:fallback; }
    public void putString(String key, String value) { values.put(key,value); }
    public String getString(String key) { Object v=values.get(key);return v instanceof String s?s:null; }
}
