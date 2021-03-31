/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.mtt.hippy.serialization;


import com.tencent.mtt.hippy.serialization.utils.IntegerPolyfill;
import com.tencent.mtt.hippy.serialization.writer.BinaryWriter;
import com.tencent.mtt.hippy.serialization.writer.SafeHeapWriter;

import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Implementation of {@code v8::(internal::)ValueSerializer}.
 */
public abstract class PrimitiveValueSerializer extends SharedSerialization {
  /** Writer used for write Data. */
  protected final BinaryWriter writer;
  /** ID of the next serialized object. **/
  private int nextId;
  /** Maps a serialized object to its ID. */
  private final Map<Object, Integer> objectMap = new IdentityHashMap<>();
  /** Unsigned int max value. */
  private static final long MAX_UINT32_VALUE = 4294967295L;

  protected PrimitiveValueSerializer(BinaryWriter writer) {
    super();

    if (writer == null) {
      writer = new SafeHeapWriter();
    }
    this.writer = writer;
  }

  public BinaryWriter getWriter() {
    return writer;
  }

  public void Reset() {
    writer.reset();
    objectMap.clear();
  }

  /**
   * Writes out a header, which includes the format version.
   */
  public void writeHeader() {
    writeTag(SerializationTag.VERSION);
    writer.putVarint(LATEST_VERSION);
  }

  protected void writeTag(SerializationTag tag) {
    writer.putByte(tag.getTag());
  }

  protected void writeTag(ArrayBufferViewTag tag) {
    writer.putByte(tag.getTag());
  }

  protected void writeTag(ErrorTag tag) {
    writer.putByte(tag.getTag());
  }

  /**
   * Serializes a JavaScript delegate object into the buffer.
   * @param value JavaScript delegate object
   */
  public boolean writeValue(Object value) {
    if (value instanceof String) {
      writeString((String) value);
    } else if (value instanceof Number) {
      if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
        writeTag(SerializationTag.INT32);
        writeInt32((int) value);
      } else if (value instanceof Long) {
        long longValue = (long) value;
        if (longValue <= MAX_UINT32_VALUE) {
          writeTag(SerializationTag.UINT32);
          writer.putVarint(longValue);
        } else {
          writeTag(SerializationTag.DOUBLE);
          writer.putDouble((double) value);
        }
      } else if (value instanceof BigInteger) {
        writeTag(SerializationTag.BIG_INT);
        writeBigIntContents((BigInteger) value);
      } else {
        double doubleValue = ((Number) value).doubleValue();
        writeTag(SerializationTag.DOUBLE);
        writer.putDouble(doubleValue);
      }
    } else if (value == Boolean.TRUE) {
      writeTag(SerializationTag.TRUE);
    } else if (value == Boolean.FALSE) {
      writeTag(SerializationTag.FALSE);
    } else if (value == Hole) {
      writeTag(SerializationTag.THE_HOLE);
    } else if (value == Undefined) {
      writeTag(SerializationTag.UNDEFINED);
    } else if (value == Null) {
      writeTag(SerializationTag.NULL);
    } else {
      Integer id = objectMap.get(value);
      if (id != null) {
        writeTag(SerializationTag.OBJECT_REFERENCE);
        writer.putVarint(id);
      } else {
        return false;
      }
    }
    return true;
  }

  protected void writeInt32(int value) {
    int zigzag = (value << 1) ^ (value >> 31);
    writer.putVarint(IntegerPolyfill.toUnsignedLong(zigzag));
  }

  /**
   * Write {@code UInt32} data to the buffer.
   *
   * @param value data
   */
  public void writeUInt32(long value) {
    writer.putVarint(value);
  }

  /**
   * Write {@code UInt64} data to the buffer.
   *
   * @param value data
   */
  public void writeUInt64(long value) {
    writer.putVarint(value);
  }

  /**
   * Write raw {@code byte[]} to the buffer.
   *
   * @param bytes source
   * @param start start position in source
   * @param length length in source
   */
  public void writeBytes(byte[] bytes, int start, int length) {
    writer.putBytes(bytes, start, length);
  }

  /**
   * Write {@code double} data to the buffer.
   *
   * @param value data
   */
  public void writeDouble(double value) {
    writer.putDouble(value);
  }


  protected void writeString(String string) {
    int length = string.length();
    // region one byte string, commonly path
    writeTag(SerializationTag.ONE_BYTE_STRING);
    writer.putVarint(length);
    int i = 0;
    for (; i < length; i++) {
      char c = string.charAt(i);
      if (c < 256) {
        writer.putByte((byte) c);
      } else {
        writer.length(-11 - i); // revert buffer changes
        break;
      }
    }
    if (i == length) {
      return;
    }
    // endregion

    // region two byte string, universal path
    writeTag(SerializationTag.TWO_BYTE_STRING);
    writer.putVarint(length * 2);
    for (i = 0; i < length; i++) {
      char c = string.charAt(i);
      writer.putChar(c);
    }
    // endregion
  }

  protected void writeBigIntContents(BigInteger bigInteger) {
    boolean negative = bigInteger.signum() == -1;
    if (negative) {
      bigInteger = bigInteger.negate();
    }
    int bitLength = bigInteger.bitLength();
    int digits = (bitLength + 63) / 64;
    int bytes = digits * 8;
    int bitfield = bytes;
    bitfield <<= 1;
    if (negative) {
      bitfield++;
    }
    writer.putVarint(bitfield);
    for (int i = 0; i < bytes; i++) {
      byte b = 0;
      for (int bit = 8 * (i + 1) - 1; bit >= 8 * i; bit--) {
        b <<= 1;
        if (bigInteger.testBit(bit)) {
          b++;
        }
      }
      writer.putByte(b);
    }
  }

  protected void assignId(Object object) {
    objectMap.put(object, nextId++);
  }
}
