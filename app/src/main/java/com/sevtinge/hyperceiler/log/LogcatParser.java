/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Streaming parser for logcat's long,epoch,usec,uid format; never retains other apps' records. */
final class LogcatParser {
    private static final Pattern HEADER = Pattern.compile(
        "^\\[\\s*(\\d+)\\.(\\d{1,9})\\s+(?:(\\S+):)?\\s*(\\d+):\\s*(\\d+)\\s+([VDIWEFAS])/([^\\]]*?)\\s*\\]$");
    private static final int MAX_MESSAGE = 32 * 1024;

    record Entry(long id, long timestamp, String level, String tag, String message, String processIds, boolean app) {}

    private final Consumer<Entry> consumer;
    private final String bootId, appUid;
    private final long clearedAt;
    private final int appUidNumber;
    private final Map<String, Integer> occurrences = new HashMap<>();
    private Matcher current;
    private boolean hasMessageLine;
    private final StringBuilder message = new StringBuilder();

    LogcatParser(String bootId, int appUidNumber, long clearedAt, Consumer<Entry> consumer) {
        this.bootId = bootId;
        this.appUidNumber = appUidNumber;
        this.appUid = "u" + (appUidNumber / 100000) + "_a" + (appUidNumber % 100000 - 10000);
        this.clearedAt = clearedAt;
        this.consumer = consumer;
    }

    void line(String line) {
        Matcher next = HEADER.matcher(line);
        if (next.matches()) {
            finish();
            String tag = next.group(7).trim();
            if ((tag.startsWith("HyperCeiler") || HomeLayoutEngineLogNames.isLegacyTag(tag))
                && !tag.startsWith("HyperCeiler.App.")) current = next;
        } else if (current != null && message.length() < MAX_MESSAGE && !line.startsWith("--------- ")) {
            if (hasMessageLine) message.append('\n');
            hasMessageLine = true;
            int remaining = MAX_MESSAGE - message.length();
            if (remaining > 0) message.append(line, 0, Math.min(remaining, line.length()));
        }
    }

    void finish() {
        if (current == null) return;
        Matcher header = current;
        current = null;
        // long format adds an empty separator line after each record.
        if (!message.isEmpty() && message.charAt(message.length() - 1) == '\n') message.setLength(message.length() - 1);
        String text = message.toString();
        message.setLength(0);
        hasMessageLine = false;
        try {
            String fraction = (header.group(2) + "000").substring(0, 3);
            long timestamp = Math.addExact(Math.multiplyExact(Long.parseLong(header.group(1)), 1000), Long.parseLong(fraction));
            if (timestamp <= clearedAt) return;
            String tag = header.group(7).trim();
            String uid = header.group(3) == null ? "" : header.group(3);
            boolean app = uid.equals(appUid) || uid.equals(Integer.toString(appUidNumber));
            // Legacy AndroidLog output in the app is already persisted by its listener.
            if (app && tag.equals("HyperCeiler")) return;
            String identity = bootId + '\n' + header.group() + '\n' + text;
            String digest = java.util.Base64.getEncoder().encodeToString(hash(identity));
            int occurrence = occurrences.merge(digest, 1, Integer::sum);
            byte[] bytes = hash(digest + ':' + occurrence);
            long id = java.nio.ByteBuffer.wrap(bytes).getLong() | Long.MIN_VALUE;
            if (tag.startsWith("HyperCeiler.Raw.")) tag = tag.substring("HyperCeiler.Raw.".length());
            // Keep IDs based on the original event so refresh remains idempotent.
            consumer.accept(new Entry(id, timestamp, header.group(6), HomeLayoutEngineLogNames.tag(tag), HomeLayoutEngineLogNames.text(text),
                uid + ':' + header.group(4) + ':' + header.group(5), app));
        } catch (NumberFormatException | ArithmeticException ignored) {
            // An invalid header cannot prevent later records from being imported.
        }
    }

    private static byte[] hash(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
