package android.os;
import java.util.*;public class Bundle {private final Map<String,CharSequence>v=new HashMap<>();public CharSequence getCharSequence(String k){return v.get(k);}public void putCharSequence(String k,CharSequence s){v.put(k,s);}}
