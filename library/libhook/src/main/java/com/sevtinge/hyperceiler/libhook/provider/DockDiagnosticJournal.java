/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Small private module-only history. Authorization is enforced by DockGlassHost. */
final class DockDiagnosticJournal {
    private static final int MAX_EVENTS = 96;
    private static final int MAX_LENGTH = 512;
    // Per-frame positions arrive at refresh rate and used to fill the whole ring within
    // seconds, evicting the one-shot lifecycle events that actually explain a failure.
    // Keep a short recent tail of them and reserve the rest of the ring for everything else.
    private static final int MAX_FRAME_EVENTS = 16;
    private static final String FRAME_PREFIX = "motion position";

    private DockDiagnosticJournal() {}

    static synchronized Bundle access(Context context, Bundle input) {
        SharedPreferences prefs = context.createDeviceProtectedStorageContext()
                .getSharedPreferences("dock_diagnostic_history", Context.MODE_PRIVATE);
        String previous = prefs.getString("events", "");
        if (input != null) {
            String[] events = input.getStringArray("events");
            if (events == null || events.length > MAX_EVENTS) {
                throw new IllegalArgumentException("Invalid Dock diagnostic batch");
            }
            StringBuilder history = new StringBuilder(previous);
            for (String event : events) {
                if (event == null || event.length() > MAX_LENGTH) {
                    throw new IllegalArgumentException("Invalid Dock diagnostic event");
                }
                history.append(event.replace('\n', ' ').replace('\r', ' ')).append('\n');
            }
            String[] lines = history.toString().split("\n");
            List<String> kept = new ArrayList<>(Math.min(lines.length, MAX_EVENTS));
            int frames = MAX_FRAME_EVENTS;
            for (int i = lines.length - 1; i >= 0 && kept.size() < MAX_EVENTS; i--) {
                String line = lines[i];
                if (line.isEmpty()) continue;
                if (line.startsWith(FRAME_PREFIX)) {
                    if (frames == 0) continue;
                    frames--;
                }
                kept.add(line);
            }
            Collections.reverse(kept);
            StringBuilder bounded = new StringBuilder();
            for (String line : kept) {
                bounded.append(line).append('\n');
            }
            previous = bounded.toString();
            prefs.edit().putString("events", previous).apply();
            // Acknowledge a write without copying the full history over Binder again.
            return Bundle.EMPTY;
        }
        Bundle result = new Bundle();
        result.putString("events", previous);
        result.putInt("diagnosticVersion", 1);
        return result;
    }
}
