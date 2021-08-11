package com.tencent.mtt.hippy.devsupport.inspector.domain;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.adapter.http.HippyHttpRequest;
import com.tencent.mtt.hippy.devsupport.inspector.Inspector;
import com.tencent.mtt.hippy.devsupport.inspector.model.HttpResponse;
import com.tencent.mtt.hippy.devsupport.inspector.model.InspectEvent;
import com.tencent.mtt.hippy.devsupport.inspector.model.NetworkModel;

import org.json.JSONObject;

public class NetworkDomain extends InspectorDomain {

  private static final String TAG = "NetworkDomain";

  public static final String NETWORK_DOMAIN_NAME = "Network";

  private static final String METHOD_GET_RESPONSE_BODY = "getResponseBody";

  private NetworkModel networkModel;

  public NetworkDomain(Inspector inspector) {
    super(inspector);
    networkModel = new NetworkModel();
  }

  @Override
  protected boolean handleRequest(HippyEngineContext context, String method, int id, JSONObject paramsObj) {
    switch (method) {
      case METHOD_GET_RESPONSE_BODY: {
        handleGetResponseBody(context, id, paramsObj);
        break;
      }
      default: {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getDomainName() {
    return NETWORK_DOMAIN_NAME;
  }

  private void handleGetResponseBody(HippyEngineContext context, int id, JSONObject paramsObj) {
    String requestId = paramsObj.optString("requestId");
    JSONObject result = networkModel.getResponseBody(requestId);
    sendRspToFrontend(id, result);
  }

  public void requestWillBeSent(String requestId, HippyHttpRequest request) {
    if (!isEnable()) {
      // 没打开devtools的情况下不需要拦截
      return;
    }

    InspectEvent event = new InspectEvent("Network.requestWillBeSent",
      networkModel.getRequestData(requestId, request));
    sendEventToFrontend(event);
  }

  public void responseReceived(String requestId, HttpResponse response) {
    if (!isEnable()) {
      // 没打开devtools的情况下不需要拦截
      return;
    }

    InspectEvent event = new InspectEvent("Network.responseReceived",
      networkModel.getResponseReceived(requestId, response));
    sendEventToFrontend(event);
  }

  public void loadingFinished(String requestId, HttpResponse response) {
    if (!isEnable()) {
      // 没打开devtools的情况下不需要拦截
      return;
    }

    InspectEvent event = new InspectEvent("Network.loadingFinished",
      networkModel.getLoadingFinished(requestId, response));
    sendEventToFrontend(event);
  }

}
