package com.sevtinge.hyperceiler.libhook.base;
/** Framework identity transport only. Production policy is compiled separately. */
public final class BaseLoad {
    public static String name = "FixtureFramework";
    public static String version = "102-test";
    public static long versionCode = 102L;
    public static boolean unavailable;
    public static boolean fail;
    public static final class Framework {
        private void ready() { if (fail) throw new IllegalStateException("framework unavailable"); }
        public String getFrameworkName() { ready(); return name; }
        public String getFrameworkVersion() { ready(); return version; }
        public long getFrameworkVersionCode() { ready(); return versionCode; }
    }
    public static Framework getXposed() { return unavailable ? null : new Framework(); }
    public static void resetFrameworkForTest() {
        name = "FixtureFramework"; version = "102-test"; versionCode = 102L;
        unavailable = false; fail = false;
    }
}
