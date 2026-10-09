package com.sevtinge.hyperceiler.common.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/** Typed GetApps configuration shared by the preference page and process startup. */
public final class GetAppsDeviceConfig {
    public static final String ENABLED = "prefs_key_market_device_modify_new1";
    public static final String LEGACY = "prefs_key_market_device_modify_new";
    public static final String DEVICE = "prefs_key_market_device_modify_device";
    public static final String MODEL = "prefs_key_market_device_modify_model";
    public static final String MANUFACTURER = "prefs_key_market_device_modify_manufacturer";

    public final boolean enabled;
    public final String device;
    public final String model;
    public final String manufacturer;

    private GetAppsDeviceConfig(Map<String, ?> values) {
        enabled = Boolean.TRUE.equals(values.get(ENABLED));
        device = nonBlank(values.get(DEVICE));
        model = nonBlank(values.get(MODEL));
        manufacturer = nonBlank(values.get(MANUFACTURER));
    }

    public static GetAppsDeviceConfig from(Map<String, ?> values) {
        return new GetAppsDeviceConfig(values);
    }

    public boolean hasOverrides() {
        return device != null || model != null || manufacturer != null;
    }

    private static String nonBlank(Object value) {
        if (!(value instanceof String text)) return null;
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Run before preference inflation. The supplied APK's DataStore-backed switch
     * persists false even on its first display, so false cannot identify a legacy
     * user's intent. Never infer enablement from the old enumerated preference.
     * Preserve every existing String, including an intentionally empty field.
     */
    public static Map<String, Object> prepareUi(Map<String, ?> values) {
        Map<String, Object> changes = new LinkedHashMap<>();
        if (!(values.get(ENABLED) instanceof Boolean)) changes.put(ENABLED, false);
        String[] preset = legacyPreset(values.get(LEGACY));
        String[] keys = {DEVICE, MODEL, MANUFACTURER};
        for (int i = 0; i < keys.length; i++) {
            if (!values.containsKey(keys[i])) {
                if (preset != null) changes.put(keys[i], preset[i]);
            } else if (!(values.get(keys[i]) instanceof String)) {
                // An imported non-string cannot be consumed by EditTextPreference.
                changes.put(keys[i], "");
            }
        }
        return changes;
    }

    private static String[] legacyPreset(Object value) {
        if (!(value instanceof String key)) return null;
        return switch (key) {
            case "108" -> new String[]{"dijun", "25042PN24C", "Xiaomi"};
            case "112" -> new String[]{"nezha", "2512BPNDAC", "Xiaomi"};
            case "111" -> new String[]{"popsicle", "25098PN5AC", "Xiaomi"};
            case "110" -> new String[]{"pandora", "2509FPN0BC", "Xiaomi"};
            case "109" -> new String[]{"pudding", "25113PN0EC", "Xiaomi"};
            case "107" -> new String[]{"xuanyuan", "25010PN30C", "Xiaomi"};
            case "106" -> new String[]{"haotian", "2410DPN6CC", "Xiaomi"};
            case "105" -> new String[]{"dada", "24129PN74C", "Xiaomi"};
            case "156" -> new String[]{"luming", "25067PYE3C", "Xiaomi"};
            case "202" -> new String[]{"bixi", "2505APX7BC", "Xiaomi"};
            case "224" -> new String[]{"goku", "24072PX77C", "Xiaomi"};
            case "190" -> new String[]{"avenger", "MIX Alpha", "Xiaomi"};
            case "214" -> new String[]{"yupei", "25097RP43C", "Xiaomi"};
            case "215" -> new String[]{"piano", "25091RP04C", "Xiaomi"};
            case "212" -> new String[]{"jinghu", "25032RP42C", "Xiaomi"};
            case "213" -> new String[]{"violin", "25053RP5CC", "Xiaomi"};
            case "316" -> new String[]{"myron", "25102RKBEC", "Redmi"};
            case "315" -> new String[]{"annibale", "2510DRK44C", "Redmi"};
            case "314" -> new String[]{"dali", "25060RK16C", "Redmi"};
            case "313" -> new String[]{"miro", "24127RK2CC", "Redmi"};
            case "380" -> new String[]{"turner", "25079RPDCC", "Redmi"};
            case "335" -> new String[]{"onyx", "25053RT47C", "Redmi"};
            case "355" -> new String[]{"flourite", "2510ERA8BC", "Redmi"};
            case "403" -> new String[]{"lake", "2409BRN2CC", "Redmi"};
            default -> null;
        };
    }
}
