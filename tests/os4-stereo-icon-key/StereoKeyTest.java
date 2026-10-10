package fixture;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.StatusBarIcon;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.android.systemui.statusbar.phone.MiuiIconManagerUtils;

public final class StereoKeyTest {
    private static final String KEY = "prefs_key_system_ui_status_bar_icon_soundbox_group_stereo";
    private static final String OLD = "prefs_key_system_ui_status_bar_icon_soundbox_stereo";
    private static int scenarios;
    private static void check(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }
    private static List<String> seed(boolean stereo) {
        ArrayList<String> value = new ArrayList<>(List.of("sound_box_group", "bluetooth", "foreign_icon"));
        if (stereo) value.add(1, "stereo");
        return value;
    }
    private static boolean expected(int value, boolean original, boolean control) {
        return switch(value) {
            case 1 -> false;
            case 2 -> true;
            case 11 -> !control;
            case 12 -> control;
            default -> original;
        };
    }
    public static void main(String[] args) {
        // Use the XML key supplied by the runner, independently of production.
        check(args.length == 1 && args[0].equals(KEY), "Packaged setting key changed");
        ArrayList<String> right = MiuiIconManagerUtils.RIGHT_BLOCK_LIST;
        ArrayList<String> control = MiuiIconManagerUtils.CONTROL_CENTER_BLOCK_LIST;
        ArrayList<String> mini = MiuiIconManagerUtils.MINI_RIGHT_BLOCK_LIST;
        for (boolean hasStaleKey : new boolean[]{false, true}) {
        for (int mode : new int[]{0, 1, 11, 12, 2, -1, 99}) {
            for (int initial = 0; initial < 4; initial++) {
                boolean wasRight = (initial & 1) != 0;
                boolean wasControl = (initial & 2) != 0;
                right.clear(); right.addAll(seed(wasRight));
                control.clear(); control.addAll(seed(wasControl));
                mini.clear(); mini.addAll(List.of("stereo", "untouched_mini"));
                PrefsBridge.values.clear();
                PrefsBridge.values.put(KEY, Integer.toString(mode));
                // A stale misspelled key must not override the actual UI value.
                if (hasStaleKey) PrefsBridge.values.put(OLD, mode == 2 ? "1" : "2");
                new StatusBarIcon().init();
                check(right.contains("stereo") == expected(mode, wasRight, false), "right mode=" + mode);
                check(control.contains("stereo") == expected(mode, wasControl, true), "control mode=" + mode);
                check(right == MiuiIconManagerUtils.RIGHT_BLOCK_LIST && control == MiuiIconManagerUtils.CONTROL_CENTER_BLOCK_LIST, "final list identity");
                check(mini.equals(List.of("stereo", "untouched_mini")), "mini list changed");
                for (List<String> list : List.of(right, control)) {
                    ArrayList<String> unrelated = new ArrayList<>(list);
                    unrelated.removeIf("stereo"::equals);
                    check(unrelated.equals(seed(false)), "unrelated icon or ordering changed");
                    check(Collections.frequency(list, "stereo") <= 1, "duplicate stereo");
                }
                List<String> savedRight = List.copyOf(right);
                List<String> savedControl = List.copyOf(control);
                new StatusBarIcon().init();
                check(right.equals(savedRight) && control.equals(savedControl), "repeat init not idempotent");
                scenarios++;
            }
        }
        }
        for (String raw : new String[]{null, "broken", ""}) {
            right.clear(); right.addAll(seed(true)); control.clear(); control.addAll(seed(false));
            PrefsBridge.values.clear();
            if (raw != null) PrefsBridge.values.put(KEY, raw);
            new StatusBarIcon().init();
            check(right.equals(seed(true)) && control.equals(seed(false)), "default fallback changed");
            scenarios++;
        }
        System.out.println("PASS " + scenarios + " actual-production key/mode scenarios; Android runtime NOT_TESTED");
    }
}
