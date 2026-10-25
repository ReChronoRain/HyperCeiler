package android.telephony;
public class ServiceState {
 public static final int STATE_IN_SERVICE = 0, STATE_OUT_OF_SERVICE = 1, STATE_EMERGENCY_ONLY = 2, STATE_POWER_OFF = 3;
 public static final int UNKNOWN_ID = -1;
 private int state = STATE_IN_SERVICE, dataRegState = STATE_IN_SERVICE;
 public String alphaLong = "", alphaShort = "", psAlphaLong = "", psAlphaShort = "", csAlphaLong = "", csAlphaShort = "";
 public boolean hiddenApis= true, throwOnDataRegState=false, silent = false;
 public int psCalls, csCalls;
 public ServiceState(){}
 public void setState(int state) { this.state = state; }
 public void setDataRegState(int state) { this.dataRegState = state; }
 public int getState() { return silent ? STATE_OUT_OF_SERVICE : state; }
 private int getDataRegState() { if (throwOnDataRegState) throw new IllegalStateException("hidden api blocked"); return silent ? STATE_OUT_OF_SERVICE : dataRegState; }
 /** 真实 ROM 上是 @SystemApi 的 getNetworkRegistrationInfo(int domain, int transport)。 */
 private NetworkRegistrationInfo getNetworkRegistrationInfo(int domain, int transport) {
  if (domain == 2) psCalls++; else csCalls++;
  if (!hiddenApis || transport != 1) return null;
  if (silent) return new NetworkRegistrationInfo(domain, transport, "");
  String longName = domain == 2 ? psAlphaLong : csAlphaLong;
  String shortName = domain == 2 ? psAlphaShort : csAlphaShort;
  return new NetworkRegistrationInfo(domain, transport, longName, shortName);
 }
 public String getOperatorAlphaLong() { return silent ? "" : alphaLong; }
 public String getOperatorAlphaShort() { return silent ? "" : alphaShort; }
}
