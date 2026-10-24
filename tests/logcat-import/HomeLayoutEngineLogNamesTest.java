/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.log;

import com.sevtinge.hyperceiler.log.db.LogEntry;

public final class HomeLayoutEngineLogNamesTest {
    public static void main(String[] args) {
        // Simulate Room reading a row persisted by an earlier module build.
        LogEntry historical = new LogEntry("Xposed", "W", "HomeTweaks",
            "HomeTweaks 已启动 hometweaks::Config /files/hometweaks.bin", 1234, "logcat", "1000:12:34");
        historical.setId(-42);
        assert historical.getTag().equals("HyperCeiler.HomeLayoutEngine");
        assert historical.getMessage().equals("HomeLayoutEngine 已启动 home_layout_engine::Config /files/home_layout_engine.bin");
        assert historical.getId() == -42 && historical.getTimestamp() == 1234;
        assert historical.getLevel().equals("W") && historical.getProcessIds().equals("1000:12:34");
        assert historical.getSourceGroup().equals("logcat");
        assert HomeLayoutEngineLogNames.text(historical.getMessage()).equals(historical.getMessage());
        LogEntry unrelated = new LogEntry("Xposed", "E", "Renderer", "original stack trace");
        assert unrelated.getTag().equals("Renderer") && unrelated.getMessage().equals("original stack trace");
        System.out.println("Historical log names: PASS (Room row display/export, identity/severity preserved, idempotence, unrelated record unchanged)");
    }
}
