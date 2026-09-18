package com.sevtinge.hyperceiler.tests.dock;

import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockWallpaperEndpoint;

public final class DockWallpaperEndpointTest {
    static final class Window {}
    static final class Token {}
    static final class Data {}
    static final class Empty {}
    static final class Modern {
        void sendWindowWallpaperCommand(Window w, String action, int x, int y, int z, Data extras) { /* Signature fixture only. */ }
    }
    static final class Legacy {
        Data sendWindowWallpaperCommandUnchecked(Window w, String action, int x, int y, int z, Data extras, boolean sync) { return null; }
    }
    static final class ModernSession {
        void sendWallpaperCommand(Token token, String action, int x, int y, int z, Data extras) { /* Signature fixture only. */ }
    }
    static final class LegacySession {
        Data sendWallpaperCommand(Token token, String action, int x, int y, int z, Data extras, boolean sync) { return null; }
    }
    static final class GlobalOnly {
        void sendWallpaperCommand(String action, int x, int y, int z, Data extras) { /* Rejected unscoped fixture. */ }
        void sendWindowWallpaperCommand(String action, int x, int y, int z, Data extras) { /* Rejected unscoped fixture. */ }
    }
    static final class WrongReturn {
        String sendWindowWallpaperCommand(Window w, String action, int x, int y, int z, Data extras) { return null; }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static DockWallpaperEndpoint.Endpoint resolve(Class<?> controller, Class<?> session) throws Exception {
        return DockWallpaperEndpoint.resolve(controller, Window.class, session, Token.class, Data.class);
    }

    public static void main(String[] args) throws Exception {
        var modern = resolve(Modern.class, ModernSession.class);
        check(!modern.sessionScoped() && modern.method().getParameterCount() == 6, "OS4 async controller endpoint");
        var legacy = resolve(Legacy.class, ModernSession.class);
        check(!legacy.sessionScoped() && legacy.method().getParameterCount() == 7, "legacy controller endpoint");
        var session = resolve(Empty.class, ModernSession.class);
        check(session.sessionScoped() && session.method().getParameterCount() == 6, "device IWindowSession signature");
        check(resolve(Empty.class, LegacySession.class).method().getParameterCount() == 7, "legacy Session signature");
        check(resolve(WrongReturn.class, ModernSession.class).sessionScoped(), "reject incompatible result type");
        try {
            resolve(GlobalOnly.class, GlobalOnly.class);
            throw new AssertionError("Must not fall back to an unscoped command");
        } catch (NoSuchMethodException expected) {
            // An unsupported vendor endpoint must leave the static background intact.
        }
        Object owner = new Object();
        Object token = new Object();
        check(DockWallpaperEndpoint.ownsWindow(owner, token, owner, token), "both identities match");
        check(!DockWallpaperEndpoint.ownsWindow(owner, token, new Object(), token), "reject foreign session");
        check(!DockWallpaperEndpoint.ownsWindow(owner, token, owner, new Object()), "reject another window in same session");
        check(!DockWallpaperEndpoint.ownsWindow(null, token, null, token), "missing session is not a match");
        check(!DockWallpaperEndpoint.ownsWindow(owner, null, owner, null), "missing token is not a match");
        System.out.println("DockWallpaperEndpoint tests passed");
    }
}
