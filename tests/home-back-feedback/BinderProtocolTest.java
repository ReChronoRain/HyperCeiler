/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import android.os.Parcel;
import android.os.IBinder;
import android.os.SystemClock;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
public class BinderProtocolTest {
    static int checks;
    static void check(boolean ok, String label) {
        checks++;
        if (!ok)
            throw new AssertionError(label);
    }
    static byte[] packet(int op, long value) {
        return ByteBuffer.allocate(64)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0x48434248)
            .putInt(op)
            .putLong(value)
            .array();
    }
    public static void main(String[] args) throws Exception {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken("android.view.IWindowManager");
            request.writeInt(6);
            request.writeLong(SystemClock.uptimeMillis());
            request.setDataPosition(0);
            check(!Os4BackFeedback.receiveIfMatches(request, reply, 0), "legacy event remains unhandled");
            check(request.dataPosition() == 0 && reply.dataSize() == 0, "legacy parcel remains intact");
            request.enforceInterface("android.view.IWindowManager");
            check(request.readInt() == 6, "legacy event survives peeking");
        } finally {
            request.recycle();
            reply.recycle();
        }
        request = Parcel.obtain();
        reply = Parcel.obtain();
        try {
            request.writeInterfaceToken("android.view.IWindowManager");
            request.writeInt(13);
            request.setDataPosition(0);
            check(Os4BackFeedback.receiveIfMatches(request, null, IBinder.FLAG_ONEWAY),
                  "oneway with no reply rejected safely");
            request.setDataPosition(0);
            check(Os4BackFeedback.receiveIfMatches(request, reply, IBinder.FLAG_ONEWAY) &&
                      reply.dataSize() == 0,
                  "oneway creates no acknowledgement");
        } finally {
            request.recycle();
            reply.recycle();
        }
        request = Parcel.obtain();
        reply = Parcel.obtain();
        try {
            request.writeInterfaceToken("android.view.IWindowManager");
            request.writeInt(13);
            request.writeLong(SystemClock.uptimeMillis());
            request.writeByteArray(packet(4, 0));
            request.setDataPosition(0);
            check(Os4BackFeedback.receiveIfMatches(request, reply, 0), "new event recognized");
            reply.setDataPosition(0);
            byte[] rejected = reply.createByteArray();
            check(rejected.length == 8 &&
                      ByteBuffer.wrap(rejected).order(ByteOrder.LITTLE_ENDIAN).getInt(4) == 0,
                  "non-system receiver cannot execute actions");
        } finally {
            request.recycle();
            reply.recycle();
        }
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        check(activityThread.getMethod("currentActivityThread") != null &&
                  activityThread.getMethod("getSystemContext") != null,
              "framework context methods exist");
        IBinder window = (IBinder)Class.forName("android.os.ServiceManager")
                             .getMethod("getService", String.class)
                             .invoke(null, "window");
        request = Parcel.obtain();
        reply = Parcel.obtain();
        try {
            request.writeInterfaceToken("android.view.IWindowManager");
            request.writeInt(13);
            request.writeLong(SystemClock.uptimeMillis());
            request.writeByteArray(packet(4, 0));
            check(window.transact(0x00484351, request, reply, 0), "existing WMS endpoint is reachable");
            int marker = reply.readInt();
            if (marker == 0x48435131)
                check(reply.readInt() == 0, "legacy endpoint rejects new event");
            else {
                reply.setDataPosition(0);
                byte[] rejected = reply.createByteArray();
                check(rejected.length == 8 &&
                          ByteBuffer.wrap(rejected).order(ByteOrder.LITTLE_ENDIAN).getInt(0) == 0x48434248 &&
                          ByteBuffer.wrap(rejected).order(ByteOrder.LITTLE_ENDIAN).getInt(4) == 0,
                      "live endpoint rejects non-launcher UID");
            }
        } finally {
            request.recycle();
            reply.recycle();
        }
        System.out.println("Binder feedback protocol: " + checks + " checks passed");
    }
}
