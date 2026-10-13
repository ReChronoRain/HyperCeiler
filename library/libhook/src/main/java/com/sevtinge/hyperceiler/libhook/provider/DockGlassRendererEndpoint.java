package com.sevtinge.hyperceiler.libhook.provider;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;

/** HWUI executes only in SystemUI; system_server receives SurfacePackages, never renders them. */
public final class DockGlassRendererEndpoint extends Binder {
    private final Context context;
    private final DockGlassHost host;
    private volatile boolean closed;

    public DockGlassRendererEndpoint(Context context) {
        // Do NOT gate on Process.myUid() == Process.SYSTEM_UID. That value is a fixed 1000 in
        // AOSP, but OEM builds may host SystemUI under a dedicated app UID (observed: uid 10229
        // / platform_app on HyperOS 4.0.0.28 for nezha, while dada still runs it as 1000).
        // Identity is established by the package name plus the Broker's calling-side checks
        // (/proc/<pid>/cmdline == com.android.systemui AND the uid is system or owned by it).
        if (!"com.android.systemui".equals(context.getPackageName())) {
            throw new SecurityException("Renderer must run in SystemUI");
        }
        this.context = context;
        host = new DockGlassHost();
        attachInterface(null, DockGlassRendererProtocol.DESCRIPTOR);
    }

    @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (code != FIRST_CALL_TRANSACTION) return super.onTransact(code, data, reply, flags);
        if ((flags & FLAG_ONEWAY) != 0 || reply == null) return false;
        if (Binder.getCallingUid() != Process.SYSTEM_UID) {
            throw new SecurityException("Only the system Dock client may render glass");
        }
        data.enforceInterface(DockGlassRendererProtocol.DESCRIPTOR);
        String method = data.readString(), id = data.readString();
        Bundle args = data.readBundle(DockGlassRendererEndpoint.class.getClassLoader());
        data.enforceNoDataAvail();
        if (closed || !DockGlassRendererProtocol.allowed(method)) {
            throw new IllegalStateException("Renderer unavailable or invalid operation");
        }
        Bundle result = host.call(context, method, id, args);
        reply.writeNoException(); reply.writeBundle(result);
        return true;
    }

    public void close() { closed = true; host.close(); }
}
