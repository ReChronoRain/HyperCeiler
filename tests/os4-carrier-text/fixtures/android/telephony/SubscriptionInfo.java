package android.telephony;
public class SubscriptionInfo {
 private final int id, slot;
 public CharSequence carrierName = "";
 public boolean throwOnCarrierName;
 public SubscriptionInfo(int id, int slot) { this.id = id; this.slot = slot; }
 public int getSimSlotIndex() { return slot; }
 public int getSubscriptionId() { return id; }
 public CharSequence getCarrierName() { if (throwOnCarrierName) throw new SecurityException("carrier name denied"); return carrierName; }
}
