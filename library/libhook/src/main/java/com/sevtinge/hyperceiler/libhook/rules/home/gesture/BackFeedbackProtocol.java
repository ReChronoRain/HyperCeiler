/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class BackFeedbackProtocol {
    static final int MAGIC = 0x48434248;
    static final int PACKET_BYTES = 64;

    private BackFeedbackProtocol() {}

    static boolean validPacket(byte[] packet) {
        return packet != null && packet.length == PACKET_BYTES
            && ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt() == MAGIC;
    }

    static byte[] acknowledgement(boolean accepted) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(MAGIC).putInt(accepted ? 1 : 0).array();
    }

    static byte[] preview(long session, boolean left, byte[] pixels, long known) {
        boolean present = pixels != null;
        byte[] payload = present && known != session ? pixels : new byte[0];
        int flags = present ? 1 | (left ? 2 : 0) : 0;
        return ByteBuffer.allocate(32 + payload.length).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(MAGIC).putInt(flags).putLong(present ? session : 0)
            .putInt(present ? 96 : 0).putInt(present ? 96 : 0)
            .putInt(payload.length).putInt(0).put(payload).array();
    }
}
