package android.telephony;
public class NetworkRegistrationInfo {
 private final int domain, transport;
 public String alphaLong, alphaShort;
 public boolean brokenCellIdentity;
 public NetworkRegistrationInfo(int domain, int transport, String alphaLong) { this(domain, transport, alphaLong, ""); }
 public NetworkRegistrationInfo(int domain, int transport, String alphaLong, String alphaShort) {
  this.domain = domain; this.transport = transport; this.alphaLong = alphaLong; this.alphaShort = alphaShort;
 }
 public int getDomain() { return domain; }
 public int getTransportType() { return transport; }
 private CellIdentity getCellIdentity() {
  if (brokenCellIdentity) throw new IllegalStateException("cell identity unavailable");
  return new CellIdentity(alphaLong, alphaShort);
 }
}
