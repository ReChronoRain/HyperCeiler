/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class ProtocolTest {
    private static int checks;

    private static void check(boolean value, String description) {
        checks++;
        if (!value) {
            throw new AssertionError(description);
        }
    }

    private static ByteBuffer littleEndian(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    public static void main(String[] args) {
        byte[] request = new byte[64];
        littleEndian(request).putInt(0x48434248).putInt(4);
        check(BackFeedbackProtocol.validPacket(request), "native request accepted");
        check(!BackFeedbackProtocol.validPacket(null), "null packet rejected");
        check(!BackFeedbackProtocol.validPacket(Arrays.copyOf(request, 63)), "short packet rejected");
        check(!BackFeedbackProtocol.validPacket(Arrays.copyOf(request, 65)), "trailing bytes rejected");
        request[0] = 0;
        check(!BackFeedbackProtocol.validPacket(request), "bad protocol magic rejected");

        byte[] rejected = BackFeedbackProtocol.acknowledgement(false);
        check(rejected.length == 8 && littleEndian(rejected).getInt(0) == 0x48434248
            && littleEndian(rejected).getInt(4) == 0, "rejection wire format");
        check(littleEndian(BackFeedbackProtocol.acknowledgement(true)).getInt(4) == 1,
            "acceptance wire format");

        byte[] empty = BackFeedbackProtocol.preview(0, false, null, 0);
        check(empty.length == 32 && littleEndian(empty).getInt(4) == 0, "empty preview wire format");
        check(littleEndian(empty).getLong(8) == 0 && littleEndian(empty).getInt(16) == 0
            && littleEndian(empty).getInt(20) == 0, "absent preview has no identity or dimensions");

        byte[] pixels = new byte[96 * 96 * 4];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (byte) i;
        }
        byte[] full = BackFeedbackProtocol.preview(77, true, pixels, 0);
        ByteBuffer data = littleEndian(full);
        check(full.length == 32 + pixels.length && data.getInt(0) == 0x48434248
            && data.getInt(4) == 3 && data.getLong(8) == 77, "preview identity and flags");
        check(data.getInt(16) == 96 && data.getInt(20) == 96 && data.getInt(24) == pixels.length
            && data.getInt(28) == 0, "preview dimensions and payload length");
        check(Arrays.equals(Arrays.copyOfRange(full, 32, full.length), pixels), "pixel payload preserved");
        check(littleEndian(BackFeedbackProtocol.preview(78, false, pixels, 77)).getInt(4) == 1,
            "right edge preview remains distinct");
        byte[] cached = BackFeedbackProtocol.preview(77, true, pixels, 77);
        check(cached.length == 32 && littleEndian(cached).getInt(24) == 0,
            "cached preview does not repeat pixels");
        check(littleEndian(cached).getLong(8) == 77 && littleEndian(cached).getInt(4) == 3,
            "cached preview keeps its session and direction");
        check(BackFeedbackProtocol.preview(78, true, pixels, 77).length == full.length,
            "new session invalidates cached pixels");
        System.out.println("Back feedback protocol: " + checks + " checks passed");
    }
}
