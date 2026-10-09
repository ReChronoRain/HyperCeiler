package android.os;

public final class Parcel {
    private final String descriptor;
    private byte[] bytes = new byte[256];
    private int size;
    private int position;

    public Parcel(String descriptor, long... values) {
        this.descriptor = descriptor;
        for (long value : values) writeLong(value);
        position = 0;
    }

    public void enforceInterface(String expected) {
        if (!expected.equals(descriptor)) throw new SecurityException("descriptor mismatch");
    }

    public int dataAvail() { return size - position; }
    public int dataPosition() { return position; }
    public int dataSize() { return size; }
    public void setDataSize(int value) {
        if (value < 0) throw new IllegalArgumentException();
        if (value > bytes.length) bytes = java.util.Arrays.copyOf(bytes, value);
        size = value; if (position > value) position = value;
    }
    public void setDataPosition(int value) {
        if (value < 0 || value > size) throw new IllegalArgumentException(); position = value;
    }
    private void put(long value, int length) {
        int end = position + length;
        if (end > bytes.length) bytes = java.util.Arrays.copyOf(bytes, Math.max(end, bytes.length * 2));
        for (int i = 0; i < length; i++) bytes[position++] = (byte)(value >>> (i * 8));
        size = Math.max(size, end);
    }
    private long get(int length) {
        if (position + length > size) throw new IllegalStateException("underflow");
        long value = 0; for (int i = 0; i < length; i++) value |= (bytes[position++] & 255L) << (i * 8);
        return value;
    }
    public void writeInt(int value) { put(value,4); }
    public void writeLong(long value) { put(value,8); }
    public int readInt() { return (int)get(4); }
    public long readLong() { return get(8); }
    public byte[] marshall() { return java.util.Arrays.copyOf(bytes,size); }
}
