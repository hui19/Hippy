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

#include "debugger-loader.h"

#include "stdint.h"

#include "jni-env.h"
#include "jni-utils.h"

#include "core/base/logging.h"

std::string DebuggerLoader::Load(const std::string& uri) {
  std::string ret;
  auto rst = LoadBytes(uri);
  if (rst) {
    ret = rst->data();
  }

  HIPPY_DLOG(hippy::Debug, "DebuggerLoader::Load ret = %s,", ret.c_str());

  return ret;
}

std::unique_ptr<std::vector<char>> DebuggerLoader::LoadBytes(
    const std::string& uri) {
  JNIEnv* env = JNIEnvironment::AttachCurrentThread();
  jstring j_relative_path = env->NewStringUTF(uri.c_str());
  jbyteArray j_rst = (jbyteArray)env->CallObjectMethod(
      bridge_->GetObj(),
      JNIEnvironment::GetInstance()->wrapper_.get_uri_content_method_id,
      j_relative_path);
  env->DeleteLocalRef(j_relative_path);
  return JniUtils::AppendJavaByteArrayToByteVector(env, j_rst);
}

