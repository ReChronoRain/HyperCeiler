package android.os;

public final class Parcel {
    private final String descriptor;
    private final long[] values;
    private int position;

    public Parcel(String descriptor, long... values) {
        this.descriptor = descriptor;
        this.values = values;
    }

    public void enforceInterface(String expected) {
        if (!expected.equals(descriptor)) throw new SecurityException("descriptor mismatch");
    }

    public int dataAvail() { return (values.length - position) * Long.BYTES; }
    public long readLong() { return values[position++]; }
}
