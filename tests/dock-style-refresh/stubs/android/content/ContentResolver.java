package android.content;
import android.net.Uri; import java.util.*;
public class ContentResolver { public final List<String> events=new ArrayList<>(); public void notifyChange(Uri u,Object o){events.add(u.toString());} }
