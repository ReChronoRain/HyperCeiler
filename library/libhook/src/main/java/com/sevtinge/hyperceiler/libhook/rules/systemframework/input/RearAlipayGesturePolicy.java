/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.input;

import java.util.function.Consumer;
import java.util.LinkedHashMap;

/** Routing only. The native implementation retains gesture detection and payment authorization. */
public final class RearAlipayGesturePolicy {
    public static final String ALIPAY = "launch_alipay_payment_code";
    public static final String ALIPAY_BUS = "launch_alipay_bus_code";
    public static final String REAR_ALIPAY = "hyperceiler_launch_alipay_payment_code_rear";
    public static final String REAR_ALIPAY_BUS = "hyperceiler_launch_alipay_bus_code_rear";
    public static final String POWER = "double_click_power_key";
    private static final String REQUEST_PREFIX = "hyperceiler_rear_alipay_power_user_";

    private RearAlipayGesturePolicy() {}

    public static boolean hasEnabledFeature(boolean power, boolean backTap) {
        return power || backTap;
    }

    public static boolean isPowerCode(String function) {
        return ALIPAY.equals(function) || ALIPAY_BUS.equals(function);
    }

    public static boolean allowPowerDispatch(boolean enabled, String queuedFunction, String selectedFunction,
        String action, int currentUser) {
        return enabled && isPowerCode(queuedFunction) && queuedFunction.equals(selectedFunction)
            && isRequestForUser(action, currentUser);
    }

    public static boolean shouldAppend(boolean enabled, String function, boolean accepted) {
        return enabled && accepted && "mi_pay".equals(function);
    }

    public static boolean isBackTap(String action) {
        return "back_double_tap".equals(action) || "back_triple_tap".equals(action);
    }

    public static boolean useRearForBackTap(boolean enabled, String function, String action) {
        return enabled && isBackTap(action) && backTapFunction(function) != null;
    }

    public static String backTapFunction(String function) {
        if (REAR_ALIPAY.equals(function)) return ALIPAY;
        if (REAR_ALIPAY_BUS.equals(function)) return ALIPAY_BUS;
        return null;
    }

    public static LinkedHashMap<String, String> addRearChoices(LinkedHashMap<String, String> original,
        String[] actions, int type, String paymentLabel, String transitLabel) {
        if (original == null || actions == null || actions.length != 1 || type != 2
            || !isBackTap(actions[0]) || !original.containsKey(ALIPAY)
            || paymentLabel == null || transitLabel == null) return original;
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        original.forEach((key, value) -> {
            if (!REAR_ALIPAY.equals(key) && !REAR_ALIPAY_BUS.equals(key)) result.put(key, value);
            if (ALIPAY.equals(key)) {
                result.put(REAR_ALIPAY, paymentLabel);
                result.put(REAR_ALIPAY_BUS, transitLabel);
            }
        });
        return result;
    }

    public static String powerRequest(int userId) {
        if (userId < 0) throw new IllegalArgumentException("A resolved user ID is required");
        return REQUEST_PREFIX + userId;
    }

    public static boolean isPowerRequest(String action) {
        return action != null && action.startsWith(REQUEST_PREFIX);
    }

    public static boolean isRequestForUser(String action, int userId) {
        return userId >= 0 && (REQUEST_PREFIX + userId).equals(action);
    }

    @FunctionalInterface
    public interface OriginalCall { Object run() throws Throwable; }

    @FunctionalInterface
    public interface ExtraCall { void run(Object result) throws Throwable; }

    public static Object afterOriginal(OriginalCall original, ExtraCall extra, Consumer<Throwable> onFailure) throws Throwable {
        // Deliberately outside the catch: preserve the native exception and never execute an extra action after it.
        Object result = original.run();
        try {
            extra.run(result);
        } catch (Throwable error) {
            onFailure.accept(error);
        }
        return result;
    }
}
