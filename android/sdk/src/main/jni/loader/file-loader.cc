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

#include "file-loader.h"

#include "core/base/file.h"

const static std::string kFileProtocol = "file:";
const static auto kFileProtocolLen = kFileProtocol.length();

bool FileLoader::CheckValid(const std::string& path){
  auto pos = path.find_first_of(base_path_, 0);
  if (pos == 0) {
    return true;
  }

  return false;
}

FileLoader::FileLoader(const std::string& base_path) : base_path_(base_path) {

}

std::string FileLoader::Load(const std::string& uri) {
  std::string rst;
  auto ret = LoadBytes(uri);
  if (ret) {
    rst = ret->data();
  }
  return rst;
}

std::unique_ptr<std::vector<char>> FileLoader::LoadBytes(const std::string& uri) {
  const auto pos = uri.find_first_of(kFileProtocol);
  if (pos != 0) {
    return nullptr;
  }
  std::string path = uri.substr(kFileProtocolLen);
  std::string rst;
  if (CheckValid(path)) {
    return hippy::base::HippyFile::ReadFile(path.c_str(), false);
  }
  return nullptr;
}
