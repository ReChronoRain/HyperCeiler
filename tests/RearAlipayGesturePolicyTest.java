import com.sevtinge.hyperceiler.libhook.rules.systemframework.input.RearAlipayGesturePolicy;

import java.util.concurrent.atomic.AtomicInteger;

public final class RearAlipayGesturePolicyTest {
    private static int checks;

    private static void check(boolean value, String name) {
        checks++;
        if (!value) throw new AssertionError(name);
    }

    public static void main(String[] args) throws Throwable {
        check(RearAlipayGesturePolicy.ALIPAY_BUS.equals(RearAlipayGesturePolicy.backTapFunction(
            RearAlipayGesturePolicy.REAR_ALIPAY_BUS)), "rear transit maps to native transit, not payment");
        check(RearAlipayGesturePolicy.ALIPAY.equals(RearAlipayGesturePolicy.backTapFunction(
            RearAlipayGesturePolicy.REAR_ALIPAY)), "rear payment mapping retained");
        check(RearAlipayGesturePolicy.backTapFunction(RearAlipayGesturePolicy.ALIPAY_BUS) == null,
            "ordinary transit is not remapped to rear display");
        check(RearAlipayGesturePolicy.backTapFunction(null) == null, "null back action stays unhandled");
        check(RearAlipayGesturePolicy.useRearForBackTap(true, RearAlipayGesturePolicy.REAR_ALIPAY_BUS, "back_double_tap"),
            "double back tap supports rear transit");
        check(RearAlipayGesturePolicy.useRearForBackTap(true, RearAlipayGesturePolicy.REAR_ALIPAY_BUS, "back_triple_tap"),
            "triple back tap supports rear transit");
        check(!RearAlipayGesturePolicy.useRearForBackTap(false, RearAlipayGesturePolicy.REAR_ALIPAY_BUS, "back_double_tap"),
            "rear transit requires back switch");
        check(!RearAlipayGesturePolicy.useRearForBackTap(true, RearAlipayGesturePolicy.REAR_ALIPAY_BUS, "double_click_power_key"),
            "rear transit back action cannot hijack power key");
        check(!RearAlipayGesturePolicy.hasEnabledFeature(false, false), "both switches off installs nothing");
        check(RearAlipayGesturePolicy.hasEnabledFeature(true, false), "power can be enabled independently");
        check(RearAlipayGesturePolicy.hasEnabledFeature(false, true), "back tap can be enabled independently");
        check(RearAlipayGesturePolicy.hasEnabledFeature(true, true), "both switches share the dispatcher");
        check(RearAlipayGesturePolicy.isPowerCode(RearAlipayGesturePolicy.ALIPAY), "payment selection supported");
        check(RearAlipayGesturePolicy.isPowerCode(RearAlipayGesturePolicy.ALIPAY_BUS), "transit selection supported");
        check(!RearAlipayGesturePolicy.isPowerCode(null), "null selection rejected");
        check(!RearAlipayGesturePolicy.isPowerCode("launch_wechat_payment_code"), "unsupported selection rejected");
        check(!RearAlipayGesturePolicy.useRearForBackTap(false, RearAlipayGesturePolicy.REAR_ALIPAY, "back_double_tap"),
            "power enabled must not enable the back-tap feature");
        check(RearAlipayGesturePolicy.useRearForBackTap(true, RearAlipayGesturePolicy.REAR_ALIPAY, "back_double_tap"),
            "back-tap routing requires its switch");
        String pending = RearAlipayGesturePolicy.powerRequest(0);
        check(RearAlipayGesturePolicy.allowPowerDispatch(true, RearAlipayGesturePolicy.ALIPAY_BUS,
            RearAlipayGesturePolicy.ALIPAY_BUS, pending, 0), "transit survives native unlock queue");
        check(RearAlipayGesturePolicy.allowPowerDispatch(true, RearAlipayGesturePolicy.ALIPAY,
            RearAlipayGesturePolicy.ALIPAY, pending, 0), "payment survives native unlock queue");
        check(!RearAlipayGesturePolicy.allowPowerDispatch(false, RearAlipayGesturePolicy.ALIPAY_BUS,
            RearAlipayGesturePolicy.ALIPAY_BUS, pending, 0), "disabled queued request discarded");
        check(!RearAlipayGesturePolicy.allowPowerDispatch(true, RearAlipayGesturePolicy.ALIPAY_BUS,
            RearAlipayGesturePolicy.ALIPAY, pending, 0), "mode change discards stale transit request");
        check(!RearAlipayGesturePolicy.allowPowerDispatch(true, RearAlipayGesturePolicy.ALIPAY,
            RearAlipayGesturePolicy.ALIPAY_BUS, pending, 0), "mode change discards stale payment request");
        check(!RearAlipayGesturePolicy.allowPowerDispatch(true, "launch_camera", "launch_camera", pending, 0),
            "unknown action cannot use rear power marker");
        check(!RearAlipayGesturePolicy.allowPowerDispatch(true, RearAlipayGesturePolicy.ALIPAY_BUS,
            RearAlipayGesturePolicy.ALIPAY_BUS, pending, 10), "transit discarded after user switch");
        check(!RearAlipayGesturePolicy.shouldAppend(false, "mi_pay", true), "disabled by default");
        check(RearAlipayGesturePolicy.shouldAppend(true, "mi_pay", true), "wallet adds rear payment");
        check(!RearAlipayGesturePolicy.shouldAppend(true, "mi_pay", false), "rejected gesture stays rejected");
        check(!RearAlipayGesturePolicy.shouldAppend(true, "launch_camera", true), "camera unchanged");
        check(!RearAlipayGesturePolicy.shouldAppend(true, "launch_alipay_payment_code", true), "native payment not duplicated");
        check(!RearAlipayGesturePolicy.shouldAppend(true, null, true), "unknown function unchanged");
        check(RearAlipayGesturePolicy.useRearForBackTap(true, RearAlipayGesturePolicy.REAR_ALIPAY, "back_double_tap"), "double tap rear option");
        check(RearAlipayGesturePolicy.useRearForBackTap(true, RearAlipayGesturePolicy.REAR_ALIPAY, "back_triple_tap"), "triple tap rear option");
        check(!RearAlipayGesturePolicy.useRearForBackTap(true, "launch_alipay_payment_code", "back_triple_tap"), "normal payment stays on main display");
        check(!RearAlipayGesturePolicy.useRearForBackTap(true, "none", "back_double_tap"), "disabled selection unchanged");
        check(!RearAlipayGesturePolicy.useRearForBackTap(true, "screen_shot", "back_double_tap"), "other back actions unchanged");
        check(!RearAlipayGesturePolicy.useRearForBackTap(true, RearAlipayGesturePolicy.REAR_ALIPAY, "fingerprint_double_tap"), "unrelated gestures unchanged");
        String request = RearAlipayGesturePolicy.powerRequest(10);
        check(RearAlipayGesturePolicy.isPowerRequest(request), "recognize module request");
        check(RearAlipayGesturePolicy.isRequestForUser(request, 10), "requesting user retained");
        check(!RearAlipayGesturePolicy.isRequestForUser(request, 0), "discard after user switch");
        check(!RearAlipayGesturePolicy.isPowerRequest(null), "null action");
        check(!RearAlipayGesturePolicy.isRequestForUser(request + "0", 10), "exact user match");
        AtomicInteger calls = new AtomicInteger();
        Object expected = new Object();
        Object actual = RearAlipayGesturePolicy.afterOriginal(() -> { calls.incrementAndGet(); return expected; },
            value -> { check(value == expected, "extra receives original result"); calls.incrementAndGet(); },
            error -> { throw new AssertionError(error); });
        check(actual == expected && calls.get() == 2, "original and extra run once with unchanged return");
        RuntimeException originalError = new RuntimeException("original");
        try {
            RearAlipayGesturePolicy.afterOriginal(() -> { throw originalError; },
                value -> { throw new AssertionError("extra must not run"); },
                error -> { throw new AssertionError("original error must not be caught"); });
            throw new AssertionError("original error lost");
        } catch (RuntimeException error) {
            check(error == originalError, "original exception identity preserved");
        }
        RuntimeException extraError = new RuntimeException("extra");
        actual = RearAlipayGesturePolicy.afterOriginal(() -> expected, value -> { throw extraError; },
            error -> check(error == extraError, "extra failure reported"));
        check(actual == expected, "extra failure leaves wallet result intact");
        System.out.println("PASS: " + checks + " checks");
    }
}
