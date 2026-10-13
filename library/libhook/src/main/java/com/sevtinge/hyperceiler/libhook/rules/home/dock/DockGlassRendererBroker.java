package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.util.Log;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.provider.DockGlassRendererProtocol;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;

/**
 * Boot-class Binder cache survives module hot reload; no application process is kept alive.
 *
 * <p>Runs in system_server. The renderer is registered by the SystemUI process, whose UID is
 * <em>not</em> necessarily {@link Process#SYSTEM_UID}: AOSP hosts SystemUI as uid 1000, but OEM
 * builds may give it a dedicated app UID (observed uid 10229 / {@code platform_app} on HyperOS
 * 4.0.0.28 while another unit still reports 1000). The caller's identity is therefore verified
 * by requiring that {@code /proc/<pid>/cmdline} resolves to {@code com.android.systemui} and
 * that the calling UID is either the system UID or owned solely by that package.</p>
 */
public final class DockGlassRendererBroker {
    private static final String CACHE = "OS4.DockGlass.SystemUIRenderer.v1";
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private volatile IBinder renderer;
    interface ProcessLookup { String name(int pid) throws Exception; }
    private final ProcessLookup processes;

    public DockGlassRendererBroker() {
        this(DockGlassRendererBroker::processName);
    }

    DockGlassRendererBroker(ProcessLookup processes) {
        this.processes = processes;
        renderer = BaseHook.getHotReloadRuntimeState(CACHE, IBinder.class);
    }

    public IBinder renderer() {
        IBinder current = renderer;
        return current != null && current.isBinderAlive() ? current : null;
    }

    public void receive(Parcel data, Parcel reply, int flags) throws Exception {
        if ((flags & IBinder.FLAG_ONEWAY) != 0 || reply == null) {
            throw new IllegalArgumentException("Registration must have a reply");
        }
        int callerUid = Binder.getCallingUid();
        // Hard gate first: the caller must actually be the SystemUI process, by cmdline.
        if (!SYSTEM_UI_PACKAGE.equals(processes.name(Binder.getCallingPid()))
            || !isSystemUiOrSystemUid(callerUid)) {
            throw new SecurityException("Only SystemUI may register a renderer");
        }
        data.enforceInterface(DockGlassRendererProtocol.WINDOW_DESCRIPTOR);
        if (data.readInt() != DockGlassRendererProtocol.VERSION) {
            throw new IllegalArgumentException("Renderer protocol version mismatch");
        }
        IBinder candidate = data.readStrongBinder();
        data.enforceNoDataAvail();
        if (candidate == null || !candidate.isBinderAlive()
            || !DockGlassRendererProtocol.DESCRIPTOR.equals(candidate.getInterfaceDescriptor())) {
            throw new IllegalArgumentException("Invalid renderer endpoint");
        }
        renderer = candidate;
        BaseHook.putHotReloadRuntimeState(CACHE, candidate);
        reply.writeNoException(); reply.writeInt(DockGlassRendererProtocol.ACK);
    }

    /**
     * True when the UID is the classic system UID, or is owned solely by the SystemUI package.
     * The package lookup can transiently fail during an in-place upgrade; that is treated as a
     * rejection (the endpoint retries with backoff), never as an approval.
     */
    private static boolean isSystemUiOrSystemUid(int uid) {
        if (uid == Process.SYSTEM_UID) return true;
        try {
            Context context = EzXposed.getAppContextOrNull();
            if (context == null) return false;
            String[] owners = context.getPackageManager().getPackagesForUid(uid);
            return owners != null && owners.length == 1 && SYSTEM_UI_PACKAGE.equals(owners[0]);
        } catch (Throwable t) {
            Log.w("HyperCeiler.DockGlass", "Renderer UID ownership lookup failed for uid=" + uid, t);
            return false;
        }
    }

    private static String processName(int pid) throws Exception {
        if (pid <= 0) return "";
        try (FileInputStream input = new FileInputStream("/proc/" + pid + "/cmdline")) {
            byte[] bytes = new byte[128]; int size = input.read(bytes);
            if (size <= 0) return "";
            int end = 0; while (end < size && bytes[end] != 0) ++end;
            return new String(bytes, 0, end, StandardCharsets.UTF_8);
        }
    }
}
