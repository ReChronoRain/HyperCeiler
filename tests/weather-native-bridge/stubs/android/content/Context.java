package android.content;public class Context {public static final int MODE_PRIVATE=0;
public static final int RECEIVER_NOT_EXPORTED=4;
private static volatile BroadcastReceiver packageReceiver;
public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags) {
    if (flags != RECEIVER_NOT_EXPORTED) throw new AssertionError("unexpected receiver flags");
    packageReceiver = receiver;
    return null;
}
public void unregisterReceiver(BroadcastReceiver receiver) {
    if (packageReceiver == receiver) packageReceiver = null;
}
public static void signalPackage(Intent intent) {
    BroadcastReceiver receiver = packageReceiver;
    if (receiver != null) receiver.onReceive(new Context(), intent);
}
public android.content.pm.PackageManager getPackageManager(){return new android.content.pm.PackageManager();}public ContentResolver getContentResolver(){return ContentResolver.missing?null:new ContentResolver();}public SharedPreferences getSharedPreferences(String n,int m){return ContentResolver.prefs();}public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}public java.io.File getFilesDir(){return new java.io.File("/tmp");}@SuppressWarnings("unchecked")public <T> T getSystemService(Class<T> c){return android.os.PowerManager.class.equals(c)?(T)new android.os.PowerManager():null;}}
