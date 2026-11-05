/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** Small, bounded, process-private marker store. All filesystem work is on a daemon worker. */
final class BootFuseFileStore implements BootFuseController.Store, AutoCloseable {
    private static final long IO_TIMEOUT_MS = 100L;
    private static final int MAX_VALUE = 512;

    private final Path file;
    private final ExecutorService io;

    BootFuseFileStore(Path file) {
        this.file = file;
        AtomicInteger sequence = new AtomicInteger();
        this.io = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "hc-statusbar-boot-fuse-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override public String read() {
        String value = call(() -> {
            if (!Files.exists(file)) return "";
            return Files.readString(file, StandardCharsets.UTF_8);
        }, null);
        return value == null || value.length() <= MAX_VALUE ? value : null;
    }

    @Override public boolean write(String value) {
        if (value == null || value.length() > MAX_VALUE) return false;
        return call(() -> {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, value, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        }, false);
    }

    @Override public boolean clear() {
        return call(() -> {
            Files.deleteIfExists(file);
            return true;
        }, false);
    }

    @Override public void close() {
        io.shutdownNow();
    }

    private <T> T call(Callable<T> task, T failure) {
        Future<T> future = io.submit(task);
        try {
            return future.get(IO_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return failure;
        } catch (ExecutionException | TimeoutException ignored) {
            future.cancel(true);
            return failure;
        }
    }
}
