package android.content;

/**
 * 测试替身：只保留运营商名称解析实际用到的系统服务查询。
 */
public class Context {

    public static final String TELEPHONY_SUBSCRIPTION_SERVICE = "telephony_subscription_service";
    public static final String TELEPHONY_SERVICE = "phone";

    public android.telephony.SubscriptionManager subscriptionManager;
    public boolean failSubscriptionService;

    @SuppressWarnings("unchecked")
    public final <T> T getSystemService(Class<T> type) {
        if (type == android.telephony.SubscriptionManager.class) {
            if (failSubscriptionService) {
                throw new IllegalStateException("subscription service unavailable");
            }
            return (T) subscriptionManager;
        }
        if (type == android.telephony.TelephonyManager.class) {
            return (T) new android.telephony.TelephonyManager(-1);
        }
        return null;
    }

    public Object getSystemService(String name) {
        return null;
    }

    public String getSystemServiceName(Class<?> type) {
        return null;
    }
}
