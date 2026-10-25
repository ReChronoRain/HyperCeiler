package android.telephony;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试替身：订阅 id -> TelephonyManager 的注册表。
 */
public final class Registry {

    public static final Map<Integer, TelephonyManager> managers = new HashMap<>();

    private Registry() {
    }
}
