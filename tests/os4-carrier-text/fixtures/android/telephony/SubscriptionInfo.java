package android.telephony;

/**
 * 测试替身：订阅信息，携带卡槽索引与运营商名称。
 */
public class SubscriptionInfo {

    public CharSequence carrierName = "";
    public boolean throwOnCarrierName;

    private final int id;
    private final int slot;

    public SubscriptionInfo(int id, int slot) {
        this.id = id;
        this.slot = slot;
    }

    public int getSimSlotIndex() {
        return slot;
    }

    public int getSubscriptionId() {
        return id;
    }

    public CharSequence getCarrierName() {
        if (throwOnCarrierName) {
            throw new SecurityException("carrier name denied");
        }
        return carrierName;
    }
}
