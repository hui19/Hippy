package com.tencent.mtt.hippy.serialization.nio.reader;

import java.nio.ByteBuffer;

public final class SafeDirectReader implements BinaryReader {
  public ByteBuffer buffer;

  private int base;
  private int count;

  public SafeDirectReader(ByteBuffer byteBuffer) {
    if (byteBuffer != null) {
      reset(byteBuffer);
    }
  }

  @Override
  public byte getByte() {
    return buffer.get();
  }

  @Override
  public ByteBuffer getBytes(int length) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(length);
    buffer.get(byteBuffer.array());
    return byteBuffer;
  }

  @Override
  public int position() {
    return buffer.position() - base;
  }

  @Override
  public int position(int position) {
    if (position < 0) {
      position += buffer.position();
    }
    if (position < 0 || position > count) {
      throw new IndexOutOfBoundsException();
    }
    buffer.position(position + base);
    return position;
  }

  @Override
  public int length() {
    return count;
  }

  @Override
  public double getDouble() {
    return buffer.getDouble();
  }

  @Override
  public long getVarint() {
    long value = 0;
    int shift = 0;
    byte b;
    do {
      b = buffer.get();
      value |= (b & 0x7fL) << shift;
      shift += 7;
    } while ((b & 0x80) != 0);
    return value;
  }

  @Override
  public long readInt64() {
    return buffer.getLong();
  }

  @Override
  public SafeDirectReader reset(ByteBuffer byteBuffer) {
    buffer = byteBuffer;
    base = byteBuffer.position();
    count = byteBuffer.limit() - byteBuffer.position();
    return this;
  }
}
