/*
 *
 * Tencent is pleased to support the open source community by making
 * Hippy available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#pragma once

#include <string>

#include "v8/v8.h"

class Serializer : public v8::ValueSerializer::Delegate {
 public:
  Serializer(v8::Isolate* isolate,
             v8::Local<v8::Context> context,
             std::string& reused_buffer);
  ~Serializer();

  Serializer(const Serializer&) = delete;
  Serializer& operator=(const Serializer&) = delete;

  v8::Maybe<bool> WriteValue(v8::Local<v8::Value> value);
  void WriteHeader();
  std::pair<uint8_t*, size_t> Release();

 protected:
  void ThrowDataCloneError(v8::Local<v8::String> message) override;

#define UNIMPLEMENT_CLONE_TYPE(func_name, received_type)                      \
  v8::Maybe<uint32_t> func_name(v8::Isolate* isolate,                         \
                                v8::Local<received_type> received) override { \
    v8::HandleScope handle_scope(isolate_);                                   \
    isolate->ThrowException(GetDataCloneError(received));                     \
    return v8::Nothing<uint32_t>();                                           \
  }

#if (V8_MAJOR_VERSION > 7 ||                            \
     (V8_MAJOR_VERSION == 7 && V8_MINOR_VERSION > 3) || \
     (V8_MAJOR_VERSION == 7 && V8_MINOR_VERSION == 3 && \
      V8_BUILD_NUMBER >= 42))
  UNIMPLEMENT_CLONE_TYPE(GetWasmModuleTransferId, v8::WasmModuleObject)
#else
  UNIMPLEMENT_CLONE_TYPE(GetWasmModuleTransferId, v8::WasmCompiledModule)
#endif

  void* ReallocateBufferMemory(void* old_buffer,
                               size_t size,
                               size_t* actual_size) override;
  void FreeBufferMemory(void* buffer) override;

 private:
  v8::Local<v8::Value> GetDataCloneError(v8::Local<v8::Value> obj);

  v8::Isolate* isolate_;
  v8::Global<v8::Context> context_global_;
  v8::ValueSerializer serializer_;
  std::string& reused_buffer_;
};
