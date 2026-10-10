package android.app.admin;
import android.content.ComponentName;
public class DevicePolicyManager {
 public long limit; public int failures;
 public DevicePolicyManager(long limit) { this.limit=limit; }
 public long getMaximumTimeToLock(ComponentName component) {
  if (failures-- > 0) throw new SecurityException("test policy read failure");
  return limit;
 }
}
