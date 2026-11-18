/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;

public final class HomeShakeWireOS4 {
    public static final int TRANSACTION = 0x0048534b;
    static final int ACK = 0x48434b31;

    private HomeShakeWireOS4() {}

    static boolean perform() {
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            IBinder window = (IBinder) Class.forName("android.os.ServiceManager")
                .getDeclaredMethod("getService", String.class).invoke(null, "window");
            if (window == null) return false;
            data.writeInterfaceToken("android.view.IWindowManager");
            data.writeLong(SystemClock.uptimeMillis());
            return window.transact(TRANSACTION, data, reply, 0) && reply.dataAvail() == 8
                && reply.readInt() == ACK && reply.readInt() == 1;
        } catch (ReflectiveOperationException | RemoteException | RuntimeException unavailable) {
            return false;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
