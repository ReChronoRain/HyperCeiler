package android.os;

public final class Binder {
    private static int uid;
    private static int pid;

    private Binder() { }

    public static int getCallingUid() { return uid; }
    public static int getCallingPid() { return pid; }

    public static void setCallingIdentityForTest(int newUid, int newPid) {
        uid = newUid;
        pid = newPid;
    }
}
