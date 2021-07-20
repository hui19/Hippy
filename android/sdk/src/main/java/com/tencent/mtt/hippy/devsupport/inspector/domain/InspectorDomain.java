package com.tencent.mtt.hippy.devsupport.inspector.domain;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.devsupport.inspector.Inspector;
import com.tencent.mtt.hippy.devsupport.inspector.model.InspectEvent;
import com.tencent.mtt.hippy.utils.LogUtils;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

public abstract class InspectorDomain {

  private static final String TAG = "InspectorDomain";

  private static final String METHOD_ENABLE = "enable";
  private static final String METHOD_DISABLE = "disable";

  private boolean isEnable;

  private WeakReference<Inspector> mInspectorRef;

  public InspectorDomain(Inspector inspector) {
    mInspectorRef = new WeakReference<>(inspector);
  }

  public void handleRequestFromBackend(HippyEngineContext context, String method, int id, JSONObject paramsObj) {
    if (mInspectorRef == null) {
      LogUtils.e(TAG, "handleRequestFromBackend, mInspectorRef null");
      return;
    }

    Inspector inspector = mInspectorRef.get();
    if (inspector == null) {
      LogUtils.e(TAG, "handleRequestFromBackend, inspector null");
      return;
    }
    if (METHOD_ENABLE.equals(method)) {
      isEnable = true;
      inspector.rspToFrontend(id, null);
    } else if (METHOD_DISABLE.equals(method)) {
      isEnable = false;
      inspector.rspToFrontend(id, null);
    }

    if (isEnable) {
      handleRequest(context, method, id, paramsObj);
    }
  }

  /**
   * 处理 frontend 的 method 调用
   * @param method 调用方法
   * @param id 调用唯一自增ID
   * @param paramsObj 参数
   */
  protected abstract void handleRequest(HippyEngineContext context, String method, int id, JSONObject paramsObj);

  /**
   * 获取 domain
   * @return domain 名称
   */
  public abstract String getDomainName();

  /**
   * 回包给 frontend
   * @param id 调用过来的自增id
   * @param result 回包数据 json
   */
  protected void sendRspToFrontend(int id, JSONObject result) {
    Inspector inspector = mInspectorRef.get();
    if (inspector != null) {
      inspector.rspToFrontend(id, result);
    }
  }

  /**
   * 主动抛事件给 frontend
   * @param event 事件数据
   */
  protected void sendEventToFrontend(InspectEvent event) {
    Inspector inspector = mInspectorRef.get();
    if (inspector != null) {
      inspector.sendEventToFrontend(event);
    }
  }

}
