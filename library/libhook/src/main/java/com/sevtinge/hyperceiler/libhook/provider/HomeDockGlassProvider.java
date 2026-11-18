/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

/** Registered application process for the original HWUI material, outside SystemUI. */
public final class HomeDockGlassProvider extends ContentProvider {
    public static final String AUTHORITY = "com.sevtinge.hyperceiler.provider.homeglass";
    private DockGlassHost host;

    public static boolean isProcess(Context context) {
        return (context.getPackageName() + ":home_glass").equals(Application.getProcessName());
    }

    @Override public boolean onCreate() {
        try {
            host = new DockGlassHost(getContext().getPackageManager().getPackageUid("com.miui.home", 0));
            return true;
        } catch (PackageManager.NameNotFoundException missing) {
            throw new IllegalStateException("System launcher identity unavailable", missing);
        }
    }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        if (method == null || !method.startsWith("dock_glass_"))
            throw new IllegalArgumentException("Only Dock glass operations are supported");
        if ("dock_glass_native_surface_create".equals(method)) {
            Bundle created = host.call(getContext(), "dock_glass_create", arg, extras);
            if (created.containsKey("error")) return created;
            boolean transferred = false;
            try {
                SurfaceControlViewHost.SurfacePackage surface = created.getParcelable(
                        "surface", SurfaceControlViewHost.SurfacePackage.class);
                if (surface == null) throw new IllegalStateException("Glass surface unavailable");
                Object control = HiddenApiBypass.invoke(SurfaceControlViewHost.SurfacePackage.class,
                        surface, "getSurfaceControl");
                if (!(control instanceof SurfaceControl)) throw new IllegalStateException("Glass control unavailable");
                Bundle result = new Bundle();
                result.putParcelable("surfaceControl", (SurfaceControl) control);
                result.putBoolean("frameCommitted", true);
                result.putBinder("lifetime", created.getBinder("lifetime"));
                result.putInt("rendererPid", created.getInt("rendererPid"));
                // Host retains the SurfacePackage until the native owner calls
                // release or dies. Bundle serialization uses ordinary flags.
                transferred = true;
                return result;
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Glass control transfer unavailable", error);
            } finally {
                if (!transferred) host.call(getContext(), "dock_glass_release", arg, null);
            }
        }
        if ("dock_glass_native_create_test".equals(method)) {
            // The real create path validates caller, dimensions and live owner.
            // Return only primitive evidence so this probe does not depend on
            // SurfacePackage decoding or attach a layer to the desktop.
            Bundle created = host.call(getContext(), "dock_glass_create", arg, extras);
            try {
                Bundle result = new Bundle();
                result.putBoolean("frameCommitted", created.containsKey("surface"));
                if (created.containsKey("error")) result.putString("error", created.getString("error"));
                return result;
            } finally {
                host.call(getContext(), "dock_glass_release", arg, null);
            }
        }
        return host.call(getContext(), method, arg, extras);
    }

    @Override public void shutdown() {
        host.close();
        super.shutdown();
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("Dock glass uses call operations");
    }
    @Override public String getType(Uri uri) { throw new UnsupportedOperationException("Dock glass uses call operations"); }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("Dock glass uses call operations"); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("Dock glass uses call operations"); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("Dock glass uses call operations"); }
}
