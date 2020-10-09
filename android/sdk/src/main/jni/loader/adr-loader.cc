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

#include "adr-loader.h"

#include "jni-env.h"
#include "jni-utils.h"

std::string ADRLoader::Normalize(const std::string& uri) {
  JNIEnv* env = JNIEnvironment::AttachCurrentThread();
  jstring j_str_uri = env->NewStringUTF(uri.c_str());
  jclass j_clazz = env->FindClass("android/net/Uri");
  jmethodID j_parse_method_id = env->GetStaticMethodID(
      j_clazz, "parse", "(Ljava/lang/String;)Landroid/net/Uri;");
  jobject j_obj_uri =
      env->CallStaticObjectMethod(j_clazz, j_parse_method_id, j_str_uri);
  jmethodID j_to_tring_method_id =
      env->GetMethodID(j_clazz, "toString", "()Ljava/lang/String;");
  jstring j_parsed_uri =
      (jstring)env->CallObjectMethod(j_obj_uri, j_to_tring_method_id);
  env->DeleteLocalRef(j_str_uri);
  env->DeleteLocalRef(j_clazz);
  return JniUtils::CovertJavaStringToString(env, j_parsed_uri);
}
