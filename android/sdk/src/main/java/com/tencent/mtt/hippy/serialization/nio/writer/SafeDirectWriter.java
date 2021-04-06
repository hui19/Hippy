package com.tencent.mtt.hippy.serialization.nio.writer;

import java.nio.ByteBuffer;

public final class SafeDirectWriter extends AbstractBinaryWriter {
  private static final int INITIAL_CAPACITY = 1024;
  private static final int MAX_CAPACITY = 1024 * 16; // 16k

  public ByteBuffer value;

  public SafeDirectWriter() {
    this(INITIAL_CAPACITY, MAX_CAPACITY);
  }

  public SafeDirectWriter(int initialCapacity, int maxCapacity) {
    super(initialCapacity, maxCapacity);
    value = ByteBuffer.allocateDirect(initialCapacity);
  }

  private void enlargeBuffer(int min) {
    int twice = (value.position() << 1) + 2;
    @SuppressWarnings("ManualMinMaxCalculation") ByteBuffer newData = ByteBuffer.allocateDirect(min > twice ? min : twice);
    value.flip();
    newData.put(value);
    value = newData;
  }

  @Override
  public void putByte(byte b) {
    if (value.position() == value.capacity()) {
      enlargeBuffer(value.position() + 1);
    }
    value.put(b);
  }

  @Override
  public void putBytes(byte[] bytes, int start, int length) {
    if (value.position() + bytes.length > value.capacity()) {
      enlargeBuffer(value.position() + bytes.length);
    }

    value.put(bytes, start, length);
  }

  @Override
  public void putDouble(double d) {
    value.putDouble(d);
  }

  @Override
  public int putVarint(long l) {
    if (value.position() + 10 > value.capacity()) {
      enlargeBuffer(value.position() + 10);
    }

    long rest = l;
    int bytes = 0;
    byte b;
    do {
      b = (byte) rest;
      b |= 0x80;
      value.put(b);
      rest >>>= 7;
      bytes++;
    } while (rest != 0);
    value.position(value.position() - 1);
    value.put((byte) (b & 0x7f));
    return bytes;
  }

  @Override
  public void putInt64(long l) {
    if (value.position() + 8 > value.capacity()) {
      enlargeBuffer(value.position() + 8);
    }

    value.putLong(l);
  }

  @Override
  public void putChar(char c) {
    if (value.position() + 2 > value.capacity()) {
      enlargeBuffer(value.position() + 2);
    }

    value.putChar(c);
  }

  @Override
  public int length() {
    return value.position();
  }

  @Override
  public int length(int length) {
    if (length < 0) {
      length += value.position();
    }
    value.position(length);
    return length;
  }

  @Override
  public ByteBuffer chunked() {
    value.flip();
    ByteBuffer chunked;
    if (value.limit() < maxCapacity) {
      chunked = value.duplicate();
    } else {
      chunked = value;
      value = ByteBuffer.allocateDirect(initialCapacity);
    }
    return chunked;
  }
}
