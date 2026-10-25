package android.telephony;

/**
 * 测试替身：真实 ROM 的 ServiceState 只暴露少量公开方法，注册层名称要走隐藏 API。
 */
public class ServiceState {

    public static final int STATE_IN_SERVICE = 0;
    public static final int STATE_OUT_OF_SERVICE = 1;
    public static final int STATE_EMERGENCY_ONLY = 2;
    public static final int STATE_POWER_OFF = 3;
    public static final int UNKNOWN_ID = -1;

    public String alphaLong = "";
    public String alphaShort = "";
    public String psAlphaLong = "";
    public String psAlphaShort = "";
    public String csAlphaLong = "";
    public String csAlphaShort = "";

    public boolean hiddenApis = true;
    public boolean throwOnDataRegState = false;
    public boolean silent = false;
    public int psCalls;
    public int csCalls;

    private int state = STATE_IN_SERVICE;
    private int dataRegState = STATE_IN_SERVICE;

    public void setState(int newState) {
        this.state = newState;
    }

    public void setDataRegState(int newState) {
        this.dataRegState = newState;
    }

    public int getState() {
        return silent ? STATE_OUT_OF_SERVICE : state;
    }

    /** 隐藏 API；保持可访问性一致，便于按名称反射。 */
    public int getDataRegState() {
        if (throwOnDataRegState) {
            throw new IllegalStateException("hidden api blocked");
        }
        return silent ? STATE_OUT_OF_SERVICE : dataRegState;
    }

    /** 真实 ROM 上是 @SystemApi 的 getNetworkRegistrationInfo(int domain, int transport)。 */
    public NetworkRegistrationInfo getNetworkRegistrationInfo(int domain, int transport) {
        if (domain == 2) {
            psCalls++;
        } else {
            csCalls++;
        }
        if (!hiddenApis || transport != 1) {
            return null;
        }
        if (silent) {
            return new NetworkRegistrationInfo(domain, transport, "");
        }
        String longName = domain == 2 ? psAlphaLong : csAlphaLong;
        String shortName = domain == 2 ? psAlphaShort : csAlphaShort;
        return new NetworkRegistrationInfo(domain, transport, longName, shortName);
    }

    public String getOperatorAlphaLong() {
        return silent ? "" : alphaLong;
    }

    public String getOperatorAlphaShort() {
        return silent ? "" : alphaShort;
    }
}
