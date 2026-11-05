/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.content.Context;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.BootFuseController;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.StatusBarBootFuse;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.StatusBarFollowGate;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;

/** SystemUI entry for the copied status-bar-follow contracts. Default-off and fail-closed. */
public final class StatusBarFollowSystemUi extends SystemUiBarFollowRuntime {
    private final Map<Class<?>, String> loadedDexContainers = new ConcurrentHashMap<>();

    @Override public void init() {
        StatusBarBootFuse.Session fuse = null;
        try {
            if (!StatusBarFollowGate.enabled()) return;
            Context context = EzXposed.getAppContextOrNull();
            if (context == null) return;
            fuse = StatusBarBootFuse.open(context, "systemui");
            if (fuse == null) return;
            BootFuseController.Decision decision = fuse.controller().beginInstall(
                PrefsBridge.getString("system_ui_status_bar_boot_fuse_reset_request", ""));
            if (decision != BootFuseController.Decision.ALLOW) {
                moduleLog(LOG_WARN, TAG, "Status-bar follow boot fuse denied SystemUI install state=" + decision);
                return;
            }
            ClassLoader loader = EzXposed.getSafeClassLoader();
            final StatusBarBootFuse.Session session = fuse;
            if (!session.startBootCompletionObserver(context, () -> {
                try {
                    hookRecentsBarFollow(loader, Collections.emptySet());
                    session.markInstalled();
                    registerHotReloadCleanup(this::closeBarFollowRuntime);
                } catch (Throwable failure) {
                    rollbackStatusbarHooks();
                    moduleLog(LOG_WARN, TAG, "Deferred SystemUI status-bar installation failed", failure);
                    session.close();
                }
            })) return;
            registerHotReloadCleanup(session::close);
        } catch (Throwable ignored) {
            // Contract failure preserves the native status bar path.
        } finally {
            if (fuse != null && !fuse.hasStarted() && !fuse.hasInstalled()) fuse.close();
        }
    }

    @Override protected String resolveBarFollowDexContainer(Class<?> targetClass) throws Throwable {
        String cached = loadedDexContainers.get(targetClass);
        if (cached != null && !cached.isEmpty()) return cached;
        String targetName = targetClass.getName();
        List<String> matches = new ArrayList<>();
        ClassLoader loader = targetClass.getClassLoader();
        while (loader != null) {
            Object pathList;
            Object dexElements;
            try {
                pathList = readField(loader, "pathList");
                dexElements = readField(pathList, "dexElements");
            } catch (Throwable ignored) {
                loader = loader.getParent();
                continue;
            }
            int count = Array.getLength(dexElements);
            for (int i = 0; i < count; ++i) {
                Object element = Array.get(dexElements, i);
                try {
                    Object dexFile = readField(element, "dexFile");
                    if (dexFile == null || !dexFileDeclaresClass(dexFile, targetName)) continue;
                    Method getName = dexFile.getClass().getMethod("getName");
                    String path = String.valueOf(getName.invoke(dexFile));
                    if (!path.isEmpty() && !matches.contains(path)) matches.add(path);
                } catch (Throwable ignored) { }
            }
            loader = loader.getParent();
        }
        if (matches.size() != 1)
            throw new IllegalStateException("Expected one loaded dex container for " + targetName + ", found=" + matches);
        loadedDexContainers.put(targetClass, matches.get(0));
        return matches.get(0);
    }

    private static boolean dexFileDeclaresClass(Object dexFile, String targetName) throws ReflectiveOperationException {
        Object entriesObject = dexFile.getClass().getMethod("entries").invoke(dexFile);
        if (!(entriesObject instanceof Enumeration<?> entries)) return false;
        while (entries.hasMoreElements()) if (targetName.equals(entries.nextElement())) return true;
        return false;
    }

    @Override protected void sendAuthenticatedMiuiHomeOpenBreakCommand(Object controller, boolean toHome) {
        throw new UnsupportedOperationException("status-bar follow does not own input handoff");
    }

    @Override protected void publishSystemUiReturnHomeFinish(Object controller, Object transaction) {
        throw new UnsupportedOperationException("status-bar follow does not own input handoff");
    }
}
