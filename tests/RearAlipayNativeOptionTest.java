import com.sevtinge.hyperceiler.libhook.rules.systemframework.input.RearAlipayGesturePolicy;
import java.util.LinkedHashMap;
import java.util.List;

public final class RearAlipayNativeOptionTest {
    private static int checks;
    private static void check(boolean value, String name) {
        checks++;
        if (!value) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        LinkedHashMap<String, String> choices = new LinkedHashMap<>();
        choices.put("none", "无");
        choices.put(RearAlipayGesturePolicy.ALIPAY, "支付宝付款码");
        choices.put("screen_shot", "截屏");
        String rear = RearAlipayGesturePolicy.REAR_ALIPAY;
        var result = RearAlipayGesturePolicy.addRearChoices(choices, new String[]{"back_double_tap"}, 2, "支付宝付款码(背屏)", "支付宝乘车码(背屏)");
        check(result != choices && choices.size() == 3, "do not mutate OEM choices");
        check("支付宝付款码(背屏)".equals(result.get(rear)), "exact option label");
        check("支付宝乘车码(背屏)".equals(result.get(RearAlipayGesturePolicy.REAR_ALIPAY_BUS)), "exact transit option label");
        check(!choices.containsKey(RearAlipayGesturePolicy.ALIPAY_BUS)
            && result.containsKey(RearAlipayGesturePolicy.REAR_ALIPAY_BUS), "transit added even when OEM back list lacks ordinary transit");
        check(RearAlipayGesturePolicy.addRearChoices(result, new String[]{"back_triple_tap"}, 2, "payment", "transit")
            .get(RearAlipayGesturePolicy.REAR_ALIPAY_BUS).equals("transit"), "triple tap transit label and refresh");
        check(RearAlipayGesturePolicy.addRearChoices(choices, new String[]{"back_double_tap"}, 2, "payment", null) == choices,
            "missing transit resource leaves native choices intact");
        check(List.copyOf(result.keySet()).equals(List.of("none", RearAlipayGesturePolicy.ALIPAY, rear, RearAlipayGesturePolicy.REAR_ALIPAY_BUS, "screen_shot")), "insert beside native Alipay");
        check(RearAlipayGesturePolicy.addRearChoices(result, new String[]{"back_double_tap"}, 2, "支付宝付款码(背屏)", "支付宝乘车码(背屏)").size() == 5, "idempotent resume");
        check(RearAlipayGesturePolicy.addRearChoices(choices, new String[]{"back_triple_tap"}, 2, "支付宝付款码(背屏)", "支付宝乘车码(背屏)").containsKey(rear), "triple tap option");
        check(RearAlipayGesturePolicy.addRearChoices(choices, new String[]{"fingerprint_double_tap"}, 2, "rear", "transit") == choices, "leave fingerprint list intact");
        check(RearAlipayGesturePolicy.addRearChoices(choices, new String[]{"back_double_tap", "fingerprint_double_tap"}, 2, "rear", "transit") == choices, "no broad list injection");
        check(RearAlipayGesturePolicy.addRearChoices(choices, new String[]{"back_double_tap"}, 1, "rear", "transit") == choices, "exact query type");
        check(RearAlipayGesturePolicy.addRearChoices(choices, null, 2, "rear", "transit") == choices, "null query");
        check(RearAlipayGesturePolicy.addRearChoices(null, new String[]{"back_double_tap"}, 2, "rear", "transit") == null, "preserve failed OEM lookup");
        var empty = new LinkedHashMap<String,String>();
        check(RearAlipayGesturePolicy.addRearChoices(empty, new String[]{"back_double_tap"}, 2, "rear", "transit") == empty, "respect OEM feature filter");
        System.out.println("PASS: " + checks + " native option checks");
    }
}
