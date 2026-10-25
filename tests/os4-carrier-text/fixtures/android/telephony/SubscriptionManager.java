package android.telephony;
import java.util.ArrayList;
import java.util.List;
public class SubscriptionManager {
 /** activeSubscriptionInfoList 返回 null 的场景由非法值触发。 */
 public static final int NO_CONTEXT = 0;
 public final List<SubscriptionInfo> active = new ArrayList<>();
 public boolean fail;
 public SubscriptionManager() {}
 public List<SubscriptionInfo> getActiveSubscriptionInfoList() {
  if (fail) throw new SecurityException("READ_PHONE_STATE missing");
  return active;
 }
}
