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

public class SimpleAllocator implements Allocator<ByteBuffer> {
  private final boolean isDirect;
  private final ByteOrder order;

  public SimpleAllocator() {
    this(false, null);
  }

  public SimpleAllocator(boolean isDirect, ByteOrder order) {
    this.isDirect = isDirect;
    this.order = order;
  }

  @Override
  public ByteBuffer allocate(int capacity) {
    ByteBuffer buffer = isDirect ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
    if (order != null) {
      buffer.order(order);
    }
    return buffer;
  }

  @Override
  public ByteBuffer expand(ByteBuffer buffer, int capacityNeeded) {
    if (capacityNeeded > buffer.capacity()) {
      int newCapacity = Math.max(capacityNeeded, 2 * buffer.capacity());
      ByteBuffer newBuffer = allocate(newCapacity);
      buffer.flip();
      newBuffer.put(buffer);
      buffer = newBuffer;
    }
    return buffer;
  }

  @Override
  public ByteBuffer release(ByteBuffer buffer) {
    return buffer;
  }
}
