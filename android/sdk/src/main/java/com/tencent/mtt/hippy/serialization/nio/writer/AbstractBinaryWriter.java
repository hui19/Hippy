package com.tencent.mtt.hippy.serialization.nio.writer;

import java.nio.ByteBuffer;

public abstract class AbstractBinaryWriter implements BinaryWriter {
  protected final int initialCapacity;
  protected final int maxCapacity;

  /**
   * Non-reusable binary writer
   *
   * @param initialCapacity The writer initial capacity
   */
  AbstractBinaryWriter(int initialCapacity) {
    this(initialCapacity, 0);
  }

  /**
   * Reusable binary writer
   *
   * @param initialCapacity The writer initial capacity
   * @param maxCapacity The max cache capacity
   */
  AbstractBinaryWriter(int initialCapacity, int maxCapacity) {
    if (initialCapacity < 0 || maxCapacity < 0) {
      throw new NegativeArraySizeException();
    }
    this.initialCapacity = initialCapacity;
    this.maxCapacity = maxCapacity;
  }

  @Override
  abstract public void putByte(byte b);

  @Override
  abstract public void putBytes(byte[] bytes, int start, int length);

  @Override
  abstract public void putDouble(double d);

  @Override
  abstract public int putVarint(long l);

  @Override
  abstract public void putInt64(long l);

  @Override
  abstract public void putChar(char c);

  @Override
  abstract public int length();

  @Override
  abstract public int length(int length);

  @Override
  abstract public ByteBuffer chunked();
}
