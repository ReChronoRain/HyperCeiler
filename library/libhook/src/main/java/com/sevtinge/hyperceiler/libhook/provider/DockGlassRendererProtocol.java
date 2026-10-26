package com.sevtinge.hyperceiler.libhook.provider;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/** Direct system_server -> SystemUI renderer IPC. No HyperCeiler provider/process dependency. */
public final class DockGlassRendererProtocol {
    public static final int REGISTER = 0x0048434E;
    public static final int ACK = 0x48434731;
    public static final int VERSION = 1;
    public static final String DESCRIPTOR = "com.sevtinge.hyperceiler.DockGlassRenderer.v1";
    public static final String WINDOW_DESCRIPTOR = "android.view.IWindowManager";

    private DockGlassRendererProtocol() {}

    public static boolean allowed(String method) {
        if (method == null) return false;
        return switch (method) {
            case "dock_glass_create", "dock_glass_status", "dock_glass_probe",
                "dock_glass_pause_capture", "dock_glass_resume_capture",
                "dock_glass_sync_geometry", "dock_glass_fresh_capture", "dock_glass_refresh",
                "dock_glass_release", "dock_glass_unlock", "dock_glass_diagnostics" -> true;
            default -> false;
        };
    }

    public static Bundle call(IBinder renderer, String method, String id, Bundle args)
            throws RemoteException {
        if (!allowed(method)) throw new IllegalArgumentException("Unknown renderer operation");
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeString(method); data.writeString(id); data.writeBundle(args);
            if (!renderer.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0)) {
                throw new RemoteException("Renderer protocol unavailable");
            }
            reply.readException();
            Bundle result = reply.readBundle(DockGlassRendererProtocol.class.getClassLoader());
            if (result == null) throw new RemoteException("Missing renderer response");
            return result;
        } finally { reply.recycle(); data.recycle(); }
    }

    public static void register(IBinder window, IBinder renderer) throws RemoteException {
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(WINDOW_DESCRIPTOR);
            data.writeInt(VERSION); data.writeStrongBinder(renderer);
            if (!window.transact(REGISTER, data, reply, 0)) {
                throw new RemoteException("Dock broker not installed");
            }
            reply.readException();
            if (reply.readInt() != ACK) throw new RemoteException("Dock broker version mismatch");
        } finally { reply.recycle(); data.recycle(); }
    }
}
