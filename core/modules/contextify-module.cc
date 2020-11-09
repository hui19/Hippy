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

#include "core/modules/contextify-module.h"

#include <string.h>

#include <memory>
#include <string>

#include "core/base/logging.h"
#include "core/base/uri-loader.h"
#include "core/modules/module-register.h"
#include "core/napi/callback-info.h"
#include "core/napi/js-native-api-types.h"
#include "core/napi/js-native-api.h"
#include "core/napi/native-source-code.h"
#include "core/task/common-task.h"
#include "core/task/javascript-task.h"

REGISTER_MODULE(ContextifyModule, RunInThisContext)
REGISTER_MODULE(ContextifyModule, LoadUriContent)

using Ctx = hippy::napi::Ctx;
using CtxValue = hippy::napi::CtxValue;
using CallbackInfo = hippy::napi::CallbackInfo;
using UriLoader = hippy::base::UriLoader;

void ContextifyModule::RunInThisContext(const hippy::napi::CallbackInfo& info) {
  std::shared_ptr<Scope> scope = info.GetScope();
  std::shared_ptr<Ctx> context = scope->GetContext();
  HIPPY_CHECK(context);

  std::string key;
  if (!context->GetValueString(info[0], &key)) {
    info.GetExceptionValue()->Set(
        context, "The first argument must be non-empty string.");
    return;
  }

  HIPPY_DLOG(hippy::Debug, "RunInThisContext key = %s", key.c_str());
  auto source_code = hippy::GetNativeSourceCode(key.c_str());
  std::shared_ptr<CtxValue> ret = context->EvaluateJavascript(
      source_code.data_, source_code.length_, key.c_str());
  info.GetReturnValue()->Set(ret);
}

void ContextifyModule::LoadUriContent(const CallbackInfo& info) {
  std::shared_ptr<Scope> scope = info.GetScope();
  std::shared_ptr<hippy::napi::Ctx> context = scope->GetContext();
  HIPPY_CHECK(context);

  std::string key;
  if (!context->GetValueString(info[0], &key)) {
    info.GetExceptionValue()->Set(
        context, "The first argument must be non-empty string.");
    return;
  }

  std::shared_ptr<hippy::napi::CtxValue> function = info[1];
  if (!context->IsFunction(function)) {
    function = nullptr;
  }

  HIPPY_DLOG(hippy::Debug, "Require key = %s", key.c_str());
  auto runner = scope->GetWorkerTaskRunner();
  std::unique_ptr<CommonTask> task = std::make_unique<CommonTask>();

  std::weak_ptr<Scope> weak_scope = scope;
  std::weak_ptr<hippy::napi::CtxValue> weak_function = function;

  task->func_ = [weak_scope, weak_function, key]() {
    std::shared_ptr<Scope> scope = weak_scope.lock();
    if (!scope) {
      return;
    }

    std::shared_ptr<UriLoader> loader = scope->GetUriLoader();
    std::string uri = loader->Normalize(key);
    std::string cur_dir;
    std::string file_name;
    auto pos = uri.find_last_of('/');
    if (pos != -1) {
      cur_dir = uri.substr(0, pos + 1);
      file_name = uri.substr(pos + 1);
    }

    const std::string code = loader->Load(uri);
    if (code.empty()) {
      HIPPY_LOG(hippy::Warning, "Load key = %s, uri = %s, code empty",
                key.c_str(), uri.c_str());
    } else {
      HIPPY_DLOG(hippy::Debug, "Load key = %s, uri = %s, code = %s",
                 key.c_str(), uri.c_str(), code.c_str());
    }
    std::shared_ptr<JavaScriptTask> js_task =
        std::make_shared<JavaScriptTask>();
    js_task->callback = [weak_scope, weak_function, code, cur_dir,
                         file_name]() {
      std::shared_ptr<Scope> scope = weak_scope.lock();
      if (!scope) {
        return;
      }

      std::shared_ptr<Ctx> ctx = scope->GetContext();
      std::shared_ptr<CtxValue> status;
      if (!code.empty()) {
        auto last_dir_str_obj = ctx->GetGlobalStrVar("__HIPPYCURDIR__");
        ctx->SetGlobalStrVar("__HIPPYCURDIR__", cur_dir.c_str());
        scope->RunJS(code, file_name);
        ctx->SetGlobalObjVar("__HIPPYCURDIR__", last_dir_str_obj);
        std::shared_ptr<CtxValue> status = ctx->CreateBoolean(true);
      } else {
        std::shared_ptr<CtxValue> status = ctx->CreateBoolean(false);
      }

      std::shared_ptr<CtxValue> function = weak_function.lock();
      if (function) {
        std::shared_ptr<CtxValue> argv[] = {status};
        ctx->CallFunction(function, 1, argv);
      }
    };
    scope->GetTaskRunner()->PostTask(js_task);
  };
  runner->PostTask(std::move(task));

  info.GetReturnValue()->SetUndefined();
}
