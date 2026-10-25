package android.telephony;

/**
 * 测试替身：小区标识里携带的运营商名称。
 */
public class CellIdentity {

    public String alphaLong;
    public String alphaShort;

    public CellIdentity(String alphaLong, String alphaShort) {
        this.alphaLong = alphaLong;
        this.alphaShort = alphaShort;
    }

    public String getOperatorAlphaLong() {
        return alphaLong;
    }

    public String getOperatorAlphaShort() {
        return alphaShort;
    }
}
