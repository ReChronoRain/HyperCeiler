package android.content.pm;
public class PackageManager {
    public static String[] packages = {"com.miui.home"};
    public static boolean fail;
    public String[] getPackagesForUid(int uid) {
        if (fail) throw new IllegalStateException("package service unavailable");
        return uid < 10000 ? null : packages;
    }
}
