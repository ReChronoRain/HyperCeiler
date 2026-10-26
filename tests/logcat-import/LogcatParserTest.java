/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.log;

import java.util.ArrayList;
import java.util.List;

public final class LogcatParserTest {
    private static List<LogcatParser.Entry> parse(String boot, long clearedAt, String input) {
        List<LogcatParser.Entry> entries = new ArrayList<>();
        LogcatParser parser = new LogcatParser(boot, 10123, clearedAt, entries::add);
        input.lines().forEach(parser::line);
        parser.finish();
        return entries;
    }

    public static void main(String[] args) {
        String input = """
            --------- beginning of main
            [ 1792394363.079037  root:123:456 I/HyperCeiler.HomeLayout ]
            folder icon scale resolved

            [ 1792394363.079038  system:124:457 E/HyperCeiler.Raw.Renderer ]
            unable to render
            java.lang.IllegalStateException: test
                at example.render(Test.java:1)

            [ 1792394363.079039  u0_a123:125:458 D/HyperCeiler.Raw.Provision ]
            app record

            [ 1792394363.079040  10123:125:459 I/HyperCeiler.App.Search ]
            already delivered to database listener

            [ 1792394363.079041  10123:125:459 I/HyperCeiler ]
            legacy app record already delivered to listener

            [ 1792394363.079042  1000:126:460 W/HomeTweaks ]
            symbol lookup declined

            [ 1792394363.079043  1000:126:460 I/OtherApp ]
            unrelated record

            [ 1792394363.079044  1000:126:460 I/HyperCeiler.HomeGesture ]
            final record
            """;
        var records = parse("boot-a", 0, input);
        assert records.size() == 5 : records;
        assert records.get(0).timestamp() == 1792394363079L;
        assert records.get(0).processIds().equals("root:123:456");
        assert records.get(1).tag().equals("Renderer");
        assert records.get(1).level().equals("E");
        assert records.get(1).message().equals("unable to render\njava.lang.IllegalStateException: test\n    at example.render(Test.java:1)") : records.get(1);
        assert records.get(2).app();
        assert records.get(2).tag().equals("Provision");
        assert !records.get(3).app();
        assert records.get(3).tag().equals(HomeLayoutEngineLogNames.TAG);
        assert records.get(4).message().equals("final record");
        assert records.stream().allMatch(record -> record.id() < 0);
        assert records.stream().map(LogcatParser.Entry::id).distinct().count() == records.size();
        assert records.equals(parse("boot-a", 0, input)) : "repeat refresh must generate the same primary keys";
        assert records.get(0).id() != parse("boot-b", 0, input).get(0).id();
        assert parse("boot-a", 1792394363079L, input).isEmpty() : "cleared logs must stay cleared";

        String duplicate = "[ 1792394363.079044  1000:126:460 I/HyperCeiler.HomeGesture ]\nrepeat\n\n";
        var duplicates = parse("boot-a", 0, duplicate + duplicate);
        assert duplicates.size() == 2;
        assert duplicates.get(0).id() != duplicates.get(1).id() : "two identical events must survive";
        assert duplicates.equals(parse("boot-a", 0, duplicate + duplicate));
        assert parse("boot-a", 0, "[ 1792394363.079044  1000:126:460 I/HomeTweaks ]\n\nleading blank\n\n")
            .get(0).message().equals("\nleading blank");

        String renamed = "[ 1792394363.079045  1000:126:460 I/HyperCeiler.HomeLayoutEngine ]\nHomeLayoutEngine ready\n\n";
        var newRecord = parse("boot-a", 0, renamed).get(0);
        assert newRecord.tag().equals(HomeLayoutEngineLogNames.TAG);
        assert newRecord.message().equals("HomeLayoutEngine ready");
        String legacy = "[ 1792394363.079045  1000:126:460 I/HomeTweaks ]\nHomeTweaks 已启动 hometweaks::Config /files/hometweaks.bin\n\n";
        var oldRecord = parse("boot-a", 0, legacy).get(0);
        assert oldRecord.tag().equals(HomeLayoutEngineLogNames.TAG);
        assert oldRecord.message().equals("HomeLayoutEngine 已启动 home_layout_engine::Config /files/home_layout_engine.bin");
        assert oldRecord.equals(parse("boot-a", 0, legacy).get(0));
        assert HomeLayoutEngineLogNames.tag("HomeTweaks").equals(HomeLayoutEngineLogNames.TAG);
        assert HomeLayoutEngineLogNames.text(null) == null;
        assert HomeLayoutEngineLogNames.tag("Renderer").equals("Renderer");

        var intermediate = parse("boot-a", 0,
            "[ 1792394363.079045  1000:126:460 I/HyperCeiler.HomePatch ]\nHomePatch home_patch::Config\n\n").get(0);
        assert intermediate.tag().equals(HomeLayoutEngineLogNames.TAG);
        assert intermediate.message().equals("HomeLayoutEngine home_layout_engine::Config");

        String numericUid = "[ 1792394363.1  10123:125:459 W/HyperCeiler.Raw.Test ]\nnumeric uid";
        assert parse("boot-a", 0, numericUid).get(0).app();
        assert parse("boot-a", 0, numericUid).get(0).timestamp() == 1792394363100L;
        assert parse("boot-a", 1792394363099L, numericUid).size() == 1;
        assert parse("boot-a", 0, duplicate + "x".repeat(100000)).get(0).message().length() <= 32768;
        System.out.println("Logcat import parser: PASS (native/Java, app/module, multiline, filtering, stable IDs, repeats, reboot, clear, bounds)");
    }
}
