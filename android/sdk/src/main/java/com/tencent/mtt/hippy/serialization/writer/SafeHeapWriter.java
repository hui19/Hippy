package com.tencent.mtt.hippy.serialization.writer;

import java.nio.ByteOrder;

public class SafeHeapWriter implements BinaryWriter {
  static final int INITIAL_CAPACITY = 1024;

  private byte[] value;
  private int count = 0;
  private ByteOrder order = ByteOrder.LITTLE_ENDIAN;

  public SafeHeapWriter() {
    value = new byte[INITIAL_CAPACITY];
  }

  public SafeHeapWriter(int capacity) {
    if (capacity < 0) {
      throw new NegativeArraySizeException();
    }
    value = new byte[capacity];
  }

  private void enlargeBuffer(int min) {
    int twice = (value.length << 1) + 2;
    @SuppressWarnings("ManualMinMaxCalculation") byte[] newData = new byte[min > twice ? min : twice];
    System.arraycopy(value, 0, newData, 0, count);
    value = newData;
  }

  @Override
  public ByteOrder order() {
    return order;
  }

  @Override
  public ByteOrder order(ByteOrder order) {
    return this.order = order;
  }

  @Override
  public void putByte(byte b) {
    if (count == value.length) {
      enlargeBuffer(count + 1);
    }
    value[count++] = b;
  }

  @Override
  public void putBytes(byte[] bytes, int start, int length) {
    // start + length could overflow, start/length maybe MaxInt
    if (start >= 0 && 0 <= length && length <= bytes.length - start) {
      int newSize = count + length;
      if (newSize > value.length) {
        enlargeBuffer(newSize);
      }

      System.arraycopy(bytes, start, value, count, length);
      count = newSize;
    } else {
      throw new ArrayIndexOutOfBoundsException();
    }
  }

  @Override
  public void putDouble(double d) {
    putInt64(Double.doubleToRawLongBits(d));
  }

  @Override
  public void putVarint(long l) {
    if (count + 10 > value.length) {
      enlargeBuffer(count + 10);
    }

    long rest = l;
    byte b;
    do {
      b = (byte) rest;
      b |= 0x80;
      value[count++] = b;
      rest >>>= 7;
    } while (rest != 0);
    value[count - 1] = (byte) (b & 0x7f);
  }

  // region putInt64
  public void putInt64LE(long l) {
    value[count++] = (byte) l;
    value[count++] = (byte) (l >> 8);
    value[count++] = (byte) (l >> 16);
    value[count++] = (byte) (l >> 24);
    value[count++] = (byte) (l >> 32);
    value[count++] = (byte) (l >> 40);
    value[count++] = (byte) (l >> 48);
    value[count++] = (byte) (l >> 56);
  }

  public void putInt64BE(long l) {
    value[count++] = (byte) (l >> 56);
    value[count++] = (byte) (l >> 48);
    value[count++] = (byte) (l >> 40);
    value[count++] = (byte) (l >> 32);
    value[count++] = (byte) (l >> 24);
    value[count++] = (byte) (l >> 16);
    value[count++] = (byte) (l >> 8);
    value[count++] = (byte) l;
  }

  @Override
  public void putInt64(long l) {
    if (count + 8 > value.length) {
      enlargeBuffer(count + 8);
    }

    if (order == ByteOrder.LITTLE_ENDIAN) {
      putInt64LE(l);
    } else {
      putInt64BE(l);
    }
  }
  // endregion

  // region putChar
  public void putCharLE(char c) {
    value[count++] = ((byte) c);
    value[count++] = ((byte)(c >> 8));
  }

  public void putCharBE(char c) {
    value[count++] = ((byte)(c >> 8));
    value[count++] = ((byte) c);
  }

  @Override
  public void putChar(char c) {
    if (count + 2 > value.length) {
      enlargeBuffer(count + 2);
    }

    if (order == ByteOrder.LITTLE_ENDIAN) {
      putCharLE(c);
    } else {
      putCharBE(c);
    }
  }
  // endregion

  @Override
  public int length() {
    return count;
  }

  @Override
  public int length(int length) {
    if (length < 0) {
      length = count + length;
      if (length < 0) {
        throw new IndexOutOfBoundsException();
      }
    }

    if (length > value.length) {
      enlargeBuffer(length);
    }

    return count = length;
  }

  @Override
  public byte[] value() {
    return value;
  }

  @Override
  public void reset() {
    count = 0;
  }
}
