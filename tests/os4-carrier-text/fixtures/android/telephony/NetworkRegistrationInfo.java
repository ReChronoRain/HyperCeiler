package android.telephony;

/**
 * 测试替身：注册信息对象，承载 domain/transport 与 CellIdentity。
 */
public class NetworkRegistrationInfo {

    public String alphaLong;
    public String alphaShort;
    public boolean brokenCellIdentity;

    private final int domain;
    private final int transport;

    public NetworkRegistrationInfo(int domain, int transport, String alphaLong) {
        this(domain, transport, alphaLong, "");
    }

    public NetworkRegistrationInfo(int domain, int transport, String alphaLong, String alphaShort) {
        this.domain = domain;
        this.transport = transport;
        this.alphaLong = alphaLong;
        this.alphaShort = alphaShort;
    }

    public int getDomain() {
        return domain;
    }

    public int getTransportType() {
        return transport;
    }

    /** 隐藏 API；CellIdentity 在真实 ROM 上也可能取不到。 */
    public CellIdentity getCellIdentity() {
        if (brokenCellIdentity) {
            throw new IllegalStateException("cell identity unavailable");
        }
        return new CellIdentity(alphaLong, alphaShort);
    }
}
