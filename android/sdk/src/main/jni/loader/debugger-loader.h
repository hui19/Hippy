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

#ifndef CORE_DEBUGGER_LOADER_H_
#define CORE_DEBUGGER_LOADER_H_

#include "adr-loader.h"
#include "scoped-java-ref.h"

class DebuggerLoader: public ADRLoader {
 public:
  DebuggerLoader(){};
  virtual ~DebuggerLoader(){};

  inline void SetBridge(std::shared_ptr<JavaRef> bridge) { bridge_ = bridge; };
  virtual std::string Load(const std::string& uri);
  virtual std::unique_ptr<std::vector<char>> LoadBytes(const std::string& uri);

 private:
  std::shared_ptr<JavaRef> bridge_;
};

#endif  // CORE_DEBUGGER_LOADER_H_