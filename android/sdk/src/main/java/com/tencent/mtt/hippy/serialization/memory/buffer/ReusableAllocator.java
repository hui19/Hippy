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
package com.tencent.mtt.hippy.serialization.memory.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ReusableAllocator extends SimpleAllocator {
  private ByteBuffer reusedBuffer;
  private int maxCacheSize = 16 * 1024; // 16k

  public ReusableAllocator() {
    super();
  }

  public ReusableAllocator(int maxCacheSize, boolean isDirect, ByteOrder order) {
    super(isDirect, order);
    this.maxCacheSize = maxCacheSize;
  }

  @Override
  public ByteBuffer allocate(int capacity) {
    if (reusedBuffer != null) {
      if (reusedBuffer.capacity() >= capacity) {
        ByteBuffer buffer = reusedBuffer;
        reusedBuffer = null;
        buffer.clear();
        return buffer;
      }
    }

    return super.allocate(capacity);
  }

  @Override
  public ByteBuffer release(ByteBuffer buffer) {
    int capacity = buffer.capacity();
    if (capacity <= maxCacheSize && (reusedBuffer == null || reusedBuffer.capacity() < capacity)) {
      reusedBuffer = buffer;
    }
    return super.release(buffer);
  }
}