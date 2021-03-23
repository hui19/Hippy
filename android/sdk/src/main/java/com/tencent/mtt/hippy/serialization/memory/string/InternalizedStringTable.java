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
package com.tencent.mtt.hippy.serialization.memory.string;

import android.util.LruCache;

import com.tencent.mtt.hippy.exception.UnreachableCodeException;
import com.tencent.mtt.hippy.serialization.StringLocation;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class InternalizedStringTable extends DirectStringTable {
  // region key
  private static final int MAX_KEY_CALC_LENGTH	= 32;
  private static final int KEY_TABLE_SIZE = 2 * 1024;
  private final String[] keyTable = new String[KEY_TABLE_SIZE];
  private final char[] keyCompareTempBuffer = new char[MAX_KEY_CALC_LENGTH];
  // endregion

  // region value - local
  private final int VALUE_CACHE_SIZE = 32;
  private final LruCache<Integer, String> valueCache = new LruCache<>(VALUE_CACHE_SIZE);
  // endregion

  /** byte of "data:image/" URI string */
  private final static char[] DATA_IMAGE_URI = new char[] { 'd', 'a', 't', 'a', ':', 'i', 'm', 'a', 'g', 'e', '/' };
  private final HashMap<String, char[]> cacheablesProperty = new HashMap<String, char[]>() {{
    put("uri", DATA_IMAGE_URI);
    put("src", DATA_IMAGE_URI);
    put("source", DATA_IMAGE_URI);
  }};

  public HashMap<String, char[]> getCacheablesProperty() {
    return cacheablesProperty;
  }

  // region algorithm
  /**
   * This algorithm implements the DJB hash function
   * developed by <i>Daniel J. Bernstein</i>.
   */
  public static int DJB_HASH(byte[] value) {
    long hash = 5381;

    for (byte b : value) {
      hash = ((hash << 5) + hash) + b;
    }

    return (int) hash;
  }

  /**
   * The algorithm forked from the {@link String#hashCode()}.
   */
  private static int STRING_HASH(byte[] value) {
    int hash = 0;

    for (byte b : value) {
      hash = hash * 31 + (b & 0xff);
    }

    return hash;
  }

  /**
   * Fast compares {@link String} and {@link Byte[]} is equal
   * Basic performance considerations, <strong>treated {@link String} as IOS-8859-1 encoding</strong>
   *
   * @param sequence byte sequence
   * @param string an string
   * @return {@code true} if it's equal, {@code false} otherwise
   */
  private boolean equals(byte[] sequence, String string) {
    final int expected = sequence.length;
    final int length = string.length();
    if (length != expected) {
      return false;
    }

    string.getChars(0, length, keyCompareTempBuffer, 0);

    for (int i = 0; i < length; i++) {
      if ((sequence[i] & 0xff) != keyCompareTempBuffer[i]) {
        return false;
      }
    }
    return true;
  }
  // endregion

  // region lookup
  private String lookupKey(byte[] sequence, String encoding) throws UnsupportedEncodingException {
    final int length = sequence.length;
    if (length >= MAX_KEY_CALC_LENGTH) {
      return new String(sequence, encoding);
    }

    final int hashCode = DJB_HASH(sequence);
    final int hashIndex = (keyTable.length - 1) & hashCode;
    String internalized = keyTable[hashIndex];
    if (internalized != null && equals(sequence, internalized)) {
      return internalized;
    }
    internalized = new String(sequence, encoding);
    keyTable[hashIndex] = internalized;
    return internalized;
  }

  private String lookupValue(byte[] sequence, String encoding, Object relatedKey) throws UnsupportedEncodingException {
    if (relatedKey instanceof String) {
      char[] valuePrefix = cacheablesProperty.get(relatedKey);
      if (valuePrefix != null) {
        boolean cacheables = true;

        for (int i = 0; i < valuePrefix.length; i++) {
          if (((byte) valuePrefix[i]) != sequence[i]) {
            cacheables = false;
            break;
          }
        }

        String value = null;
        int hashCode = -1;
        if (cacheables) {
          hashCode = STRING_HASH(sequence);
          value = valueCache.get(hashCode);
        }
        if (value == null) {
          value = new String(sequence, encoding);
          if (cacheables) {
            valueCache.put(hashCode, value);
          }
        }
        return value;
      }
    }

    return new String(sequence, encoding);
  }

  @Override
  public String lookup(byte[] sequence, String encoding, StringLocation location, Object relatedKey) throws UnsupportedEncodingException {
    switch (location) {
      case OBJECT_KEY: // [[fallthrough]]
      case DENSE_ARRAY_KEY: // [[fallthrough]]
      case SPARSE_ARRAY_KEY: // [[fallthrough]]
      case MAP_KEY: {
        return lookupKey(sequence, encoding);
      }
      case OBJECT_VALUE: // [[fallthrough]]
      case DENSE_ARRAY_ITEM: // [[fallthrough]]
      case SPARSE_ARRAY_ITEM: // [[fallthrough]]
      case MAP_VALUE: {
        return lookupValue(sequence, encoding, relatedKey);
      }
      case ERROR_MESSAGE: // [[fallthrough]]
      case ERROR_STACK: // [[fallthrough]]
      case REGEXP: // [[fallthrough]]
      case SET_ITEM: // [[fallthrough]]
      case TOP_LEVEL: {
        return super.lookup(sequence, encoding, location, relatedKey);
      }
      case VOID: {
        return "";
      }
      default: {
        throw new UnreachableCodeException();
      }
    }
  }
  // endregion

  @Override
  public void release() {
    valueCache.evictAll();
    super.release();
  }
}
