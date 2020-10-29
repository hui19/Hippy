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

#ifndef HIPPY_BASE_URL_H
#define HIPPY_BASE_URL_H

#include <string>
#include <vector>

#include "core/base/url_host.h"

namespace hippy {
namespace base {

enum URL_PARSE_STATE {
  kUnknownState = -1,
  kSchemeStart,                                                            \
  kScheme,
  kNoScheme,
  kSpecialRelativeOrAuthority,
  kPathOrAuthority,
  kRelative,
  kRelativeSlash,
  kSpecialAuthoritySlashes,
  kSpecialAuthorityIgnoreSlashes,
  kAuthority,
  kHost,
  kHostname,
  kPort,
  kFile,
  kFileSlash,
  kFileHost,
  kPathStart,
  kPath,
  kCannotBeBase,
  kQuery,
  kFragment
};

enum URL_FLAGS {
  URL_FLAGS_NONE = 0,
  URL_FLAGS_FAILED = 0x01,
  URL_FLAGS_CANNOT_BE_BASE = 0x02,
  URL_FLAGS_INVALID_PARSE_STATE = 0x04,
  URL_FLAGS_TERMINATED = 0x08,
  URL_FLAGS_SPECIAL = 0x10,
  URL_FLAGS_HAS_USERNAME = 0x20,
  URL_FLAGS_HAS_PASSWORD = 0x40,
  URL_FLAGS_HAS_HOST = 0x80,
  URL_FLAGS_HAS_PATH = 0x100,
  URL_FLAGS_HAS_QUERY = 0x200,
  URL_FLAGS_HAS_FRAGMENT = 0x400,
  URL_FLAGS_IS_DEFAULT_SCHEME_PORT = 0x800
};

enum COPY_FLAG {
  kCopyUserName = 0x01,
  kCopyPassword = 0x02,
  kCopyHost = 0x04,
  kCopyQuery = 0x08,
  kCopyPath = 0x10,
  kCopyALL = 0x1f,
  kCopyNotQuery = 0x17,
};

class Url {
 public:
  Url();
  ~Url();

 private:
  void Parse(std::shared_ptr<Url> base);

  void ParseScheme(const std::string::iterator& it);
  void Url::CopyBaseUrl(std::shared_ptr<Url> base, int flag);

  URL_PARSE_STATE state_;
  int flag_;
  const std::string orig_;
  std::string scheme_;
  int port_;
  std::string username_;
  std::string password_;
  std::string host_;
  std::string query_;
  std::string fragment_;
  std::vector<std::string> path_;
};
}  // namespace base
}  // namespace hippy
#endif  // HIPPY_BASE_URL_H
