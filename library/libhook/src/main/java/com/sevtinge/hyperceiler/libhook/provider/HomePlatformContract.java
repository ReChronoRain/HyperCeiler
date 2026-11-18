/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import org.lsposed.hiddenapibypass.HiddenApiBypass;
import java.lang.reflect.Method;

/** Read-only platform protocol discovery for the launcher without a Java VM. */
final class HomePlatformContract {
    private static final String WINDOW = "android.view.IWindowManager";
    private static final String LISTENER = "com.android.internal.policy.IKeyguardLockedStateListener";
    private HomePlatformContract() { }

    static Bundle query(Context context) {
        int caller = Binder.getCallingUid();
        try {
            if (caller != 0 && caller != 2000 && caller != context.getApplicationInfo().uid
                && caller != context.getPackageManager().getApplicationInfo("com.miui.home", 0).uid)
                throw new SecurityException("Launcher or module caller required");
        } catch (android.content.pm.PackageManager.NameNotFoundException missing) {
            throw new IllegalStateException("Launcher identity unavailable", missing);
        }
        Bundle result = new Bundle();
        try {
            Class<?> window = Class.forName(WINDOW);
            Class<?> listener = Class.forName(LISTENER);
            // Discover actual methods as well as transaction fields. A renamed
            // field or changed payload must not become a guessed Binder call.
            requireMethod(window, "addKeyguardLockedStateListener", listener);
            requireMethod(window, "removeKeyguardLockedStateListener", listener);
            requireMethod(listener, "onKeyguardLockedStateChanged", boolean.class);
            Class<?> windowStub = Class.forName(WINDOW + "$Stub");
            Class<?> listenerStub = Class.forName(LISTENER + "$Stub");
            int add = transaction(windowStub, "addKeyguardLockedStateListener");
            int remove = transaction(windowStub, "removeKeyguardLockedStateListener");
            int changed = transaction(listenerStub, "onKeyguardLockedStateChanged");
            if (add == remove) throw new IllegalStateException("Distinct platform operations required");
            result.putInt("keyguardAdd", add);
            result.putInt("keyguardRemove", remove);
            result.putInt("keyguardChanged", changed);
            result.putBoolean("available", true);
        } catch (ReflectiveOperationException | RuntimeException unsupported) {
            result.clear();
            result.putBoolean("available", false);
            result.putString("reason", unsupported.getClass().getSimpleName() + ": " + unsupported.getMessage());
        }
        return result;
    }

    private static void requireMethod(Class<?> type, String name, Class<?> parameter)
            throws NoSuchMethodException {
        for (var member : HiddenApiBypass.getDeclaredMethods(type)) {
            if (member instanceof Method method && method.getName().equals(name)
                && method.getReturnType() == void.class && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == parameter) return;
        }
        throw new NoSuchMethodException(type.getName() + "#" + name);
    }

    private static int transaction(Class<?> stub, String method) throws ReflectiveOperationException {
        String name = "TRANSACTION_" + method;
        for (var field : HiddenApiBypass.getStaticFields(stub)) {
            if (!field.getName().equals(name) || field.getType() != int.class) continue;
            field.setAccessible(true);
            int value = field.getInt(null);
            if (value < IBinder.FIRST_CALL_TRANSACTION || value > IBinder.LAST_CALL_TRANSACTION)
                throw new IllegalStateException("Platform transaction outside Binder range");
            return value;
        }
        throw new NoSuchFieldException(stub.getName() + "#" + name);
    }
}
