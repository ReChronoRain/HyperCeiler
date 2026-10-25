package android.telephony;
public class CellIdentity {
 public String alphaLong, alphaShort;
 public CellIdentity(String alphaLong, String alphaShort) { this.alphaLong = alphaLong; this.alphaShort = alphaShort; }
 public String getOperatorAlphaLong() { return alphaLong; }
 public String getOperatorAlphaShort() { return alphaShort; }
}
