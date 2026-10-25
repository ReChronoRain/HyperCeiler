package android.telephony;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试替身：订阅列表；{@code fail} 置位时模拟缺少 READ_PHONE_STATE。
 */
public class SubscriptionManager {

    public static final int NO_CONTEXT = 0;

    public final List<SubscriptionInfo> active = new ArrayList<>();
    public boolean fail;

    public List<SubscriptionInfo> getActiveSubscriptionInfoList() {
        if (fail) {
            throw new SecurityException("READ_PHONE_STATE missing");
        }
        return active;
    }
}
