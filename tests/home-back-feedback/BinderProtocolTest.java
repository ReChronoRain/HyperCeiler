/* SPDX-License-Identifier: AGPL-3.0-or-later */
import android.os.Parcel;
import android.os.IBinder;
import android.os.SystemClock;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.Os4BackFeedback;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;
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
        Parcel request = Parcel.obtain(), reply = Parcel.obtain();
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
        Method dispatch = Os4BackFeedback.class.getDeclaredMethod("dispatch", byte[].class);
        dispatch.setAccessible(true);
        byte[] empty = (byte[])dispatch.invoke(null, (Object)packet(4, 0));
        check(empty.length == 32 && ByteBuffer.wrap(empty).order(ByteOrder.LITTLE_ENDIAN).getInt(4) == 0,
              "empty preview wire format");
        Class<?> preview = Class.forName(Os4BackFeedback.class.getName() + "$NativePreview");
        Constructor<?> ctor = preview.getDeclaredConstructor(long.class, boolean.class, byte[].class);
        ctor.setAccessible(true);
        byte[] pixels = new byte[96 * 96 * 4];
        for (int i = 0; i < pixels.length; i++)
            pixels[i] = (byte)i;
        Field field = Os4BackFeedback.class.getDeclaredField("nativePreview");
        field.setAccessible(true);
        field.set(null, ctor.newInstance(77L, true, pixels));
        try {
            byte[] full = (byte[])dispatch.invoke(null, (Object)packet(4, 0));
            ByteBuffer data = ByteBuffer.wrap(full).order(ByteOrder.LITTLE_ENDIAN);
            check(full.length == 32 + pixels.length && data.getInt(0) == 0x48434248 && data.getInt(4) == 3 &&
                      data.getLong(8) == 77,
                  "preview identity and flags");
            check(data.getInt(16) == 96 && data.getInt(20) == 96 && data.getInt(24) == pixels.length,
                  "preview dimensions and length");
            check(Arrays.equals(Arrays.copyOfRange(full, 32, full.length), pixels),
                  "pixel payload preserved");
            byte[] cached = (byte[])dispatch.invoke(null, (Object)packet(4, 77));
            check(cached.length == 32 &&
                      ByteBuffer.wrap(cached).order(ByteOrder.LITTLE_ENDIAN).getInt(24) == 0,
                  "cached preview does not repeat pixels");
        } finally {
            field.set(null, null);
        }
        byte[] invalid = packet(4, 0);
        invalid[0] = 0;
        check(((byte[])dispatch.invoke(null, (Object)invalid)).length == 8, "bad protocol magic rejected");
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
