package android.content;

import android.content.pm.PackageManager;

/** Minimal host stub: the endpoint only asks the context for its content resolver. */
public class Context {
    public static final int RECEIVER_NOT_EXPORTED = 4;
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

    public android.content.res.Resources getResources(){return android.content.res.Resources.getSystem();}
    public PackageManager getPackageManager() { return new PackageManager(); }
    public ContentResolver getContentResolver() {
        return new ContentResolver();
    }

    @SuppressWarnings("unchecked")
    public <T> T getSystemService(Class<T> serviceClass) {
        return android.os.PowerManager.class.equals(serviceClass)
            ? (T) new android.os.PowerManager() : null;
    }
}
