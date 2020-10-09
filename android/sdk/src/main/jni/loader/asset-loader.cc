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

#include "asset-loader.h"

#include "core/base/file.h"
#include "core/base/logging.h"

const static std::string kAssetProtocol = "assets://";
const static auto kAssetProtocalLen = kAssetProtocol.length();

std::unique_ptr<std::vector<char>> AssetLoader::ReadAssetFile(
    AAssetManager* asset_manager,
    const char* file_path,
    bool is_auto_fill) {
  HIPPY_LOG(hippy::Debug, "ReadAssetFile file_path = %s", file_path);

  std::string str_file_path(file_path);
  HIPPY_LOG(hippy::Debug, "ReadAssetFile file_path = %s", str_file_path.c_str());
  const auto pos = str_file_path.find_first_of(kAssetProtocol);
  if (pos != 0) {
    return nullptr;
  }
  HIPPY_LOG(hippy::Debug, "pos = %d, kAssetProtocalLen = %d, str_file_path.substr(kAssetProtocalLen) = %s", pos, kAssetProtocalLen, str_file_path.substr(kAssetProtocalLen).c_str());
  const char* asset_file_path =
      str_file_path.substr(kAssetProtocalLen).c_str();
  HIPPY_LOG(hippy::Debug, "ReadAssetFile asset_file_path = %s", asset_file_path); // todo
  auto asset = AAssetManager_open(
      asset_manager, str_file_path.substr(kAssetProtocalLen).c_str(),
      AASSET_MODE_STREAMING);
  std::vector<char> file_data;
  if (asset) {
    int size = AAsset_getLength(asset);
    if (is_auto_fill) {
      size += 1;
    }
    file_data.resize(size);
    int offset = 0;
    int readbytes;
    while ((readbytes = AAsset_read(asset, file_data.data() + offset,
                                    file_data.size() - offset)) > 0) {
      offset += readbytes;
    }
    if (is_auto_fill) {
      file_data.back();
    }
    AAsset_close(asset);
  }
  return std::make_unique<std::vector<char>>(std::move(file_data));
}

bool AssetLoader::CheckValid(const std::string& path) {
  auto pos = path.find_first_of(base_path_, 0);
  if (pos == 0) {
    return true;
  }

  return false;
}

AssetLoader::AssetLoader(AAssetManager* asset_manager,
                         const std::string& base_path)
    : base_path_(base_path), asset_manager_(asset_manager) {}

std::string AssetLoader::Load(const std::string& uri) {
  std::unique_ptr<std::vector<char>> ret = LoadBytes(uri);
  return ret->data();
}

std::unique_ptr<std::vector<char>> AssetLoader::LoadBytes(
    const std::string& uri) {
  return ReadAssetFile(asset_manager_, uri.c_str(), false);
}
