package com.tencent.mtt.hippy.serialization.writer;

import java.nio.ByteOrder;

public interface BinaryWriter {
  public ByteOrder order();
  public ByteOrder order(ByteOrder order);
  public void putByte(byte b);
  public void putBytes(byte[] bytes, int start, int length);
  public void putDouble(double d);

  /**
   * Writes an unsigned integer as a base-128 varint.
   * The number is written, 7 bits at a time, from the least significant to the
   * most significant 7 bits. Each byte, except the last, has the MSB set.
   * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding">protocol buffers encoding</a>
   *
   * @param l data
   */
  public void putVarint(long l);
  public void putInt64(long l);
  public void putChar(char c);
  public int length();
  public int length(int length);
  public byte[] value();
  public void reset();
}
