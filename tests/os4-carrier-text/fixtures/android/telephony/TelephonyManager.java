package android.telephony;

/**
 * 测试替身：每个订阅 id 一个管理器；未注册的 id 返回 null，用于模拟"卡槽与订阅不匹配"。
 */
public class TelephonyManager {

    public final int subscriptionId;

    public ServiceState serviceState = new ServiceState();
    public String networkOperatorName = "";
    public String simOperatorName = "";
    public String networkOperator = "";
    public String simOperator = "";
    public boolean throwOnServiceState;
    public boolean throwOnNames;

    public TelephonyManager(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public TelephonyManager createForSubscriptionId(int id) {
        return Registry.managers.get(id);
    }

    public ServiceState getServiceState() {
        if (throwOnServiceState) {
            throw new SecurityException("getServiceState denied");
        }
        return serviceState;
    }

    public String getNetworkOperatorName() {
        if (throwOnNames) {
            throw new SecurityException("network name denied");
        }
        return networkOperatorName;
    }

    public String getSimOperatorName() {
        if (throwOnNames) {
            throw new SecurityException("sim name denied");
        }
        return simOperatorName;
    }

    public String getNetworkOperator() {
        return networkOperator;
    }

    public String getSimOperator() {
        return simOperator;
    }
}
