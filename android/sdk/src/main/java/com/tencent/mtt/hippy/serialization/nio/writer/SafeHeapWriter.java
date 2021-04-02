package com.tencent.mtt.hippy.serialization.nio.writer;

import java.nio.ByteBuffer;

public final class SafeHeapWriter implements BinaryWriter {
  private static final int INITIAL_CAPACITY = 64;

  public byte[] value;
  private int count = 0;

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
  public int putVarint(long l) {
    if (count + 10 > value.length) {
      enlargeBuffer(count + 10);
    }

    long rest = l;
    int bytes = 0;
    byte b;
    do {
      b = (byte) rest;
      b |= 0x80;
      value[count++] = b;
      rest >>>= 7;
      bytes++;
    } while (rest != 0);
    value[count - 1] = (byte) (b & 0x7f);
    return bytes;
  }

  @Override
  public void putInt64(long l) {
    if (count + 8 > value.length) {
      enlargeBuffer(count + 8);
    }

    value[count++] = (byte) l;
    value[count++] = (byte) (l >> 8);
    value[count++] = (byte) (l >> 16);
    value[count++] = (byte) (l >> 24);
    value[count++] = (byte) (l >> 32);
    value[count++] = (byte) (l >> 40);
    value[count++] = (byte) (l >> 48);
    value[count++] = (byte) (l >> 56);
  }

  @Override
  public void putChar(char c) {
    if (count + 2 > value.length) {
      enlargeBuffer(count + 2);
    }

    value[count++] = ((byte) c);
    value[count++] = ((byte)(c >> 8));
  }

  @Override
  public int length() {
    return count;
  }

  @Override
  public int length(int length) {
    if (length < 0) {
      length += count;
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
  public final ByteBuffer complete() {
    return ByteBuffer.wrap(value, 0, count);
  }

  @Override
  public SafeHeapWriter reset() {
    count = 0;
    return this;
  }
}