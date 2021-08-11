package com.tencent.mtt.hippy;

import com.tencent.mtt.hippy.adapter.http.HippyHttpRequest;
import com.tencent.mtt.hippy.adapter.http.HippyHttpResponse;
import com.tencent.mtt.hippy.devsupport.inspector.Inspector;
import com.tencent.mtt.hippy.devsupport.inspector.domain.NetworkDomain;
import com.tencent.mtt.hippy.devsupport.inspector.model.HttpResponse;
import com.tencent.mtt.hippy.utils.LogUtils;

/**
 * http请求request、response钩子
 * 提供给
 */
public class HippyHttpWatcher {

  private static final String TAG = "HippyHttpWatcher";

  private static HippyHttpWatcher sInstance;
  private HippyEngineContext mContext;

  public static synchronized HippyHttpWatcher getInstance(HippyEngineContext context) {
    if (sInstance == null) {
      sInstance = new HippyHttpWatcher(context);
    }
    return sInstance;
  }

  private HippyHttpWatcher(HippyEngineContext context) {
    mContext = context;
  }

  public void requestWillBeSent(String requestId, HippyHttpRequest request) {
    LogUtils.d(TAG, "requestWillBeSent: url=" + request.getUrl() + ", requestId=" + requestId);
    NetworkDomain networkDomain = (NetworkDomain) Inspector.getInstance(mContext).getDomain(NetworkDomain.NETWORK_DOMAIN_NAME);
    networkDomain.requestWillBeSent(requestId, request);
  }

  public void responseReceived(String requestId, HippyHttpResponse response, byte[] originRspData, String errMsg) {
    LogUtils.d(TAG, "responseReceived: url=" + response.getStatusCode() + ", requestId=" + requestId);
    NetworkDomain networkDomain = (NetworkDomain) Inspector.getInstance(mContext).getDomain(NetworkDomain.NETWORK_DOMAIN_NAME);
    HttpResponse httpResponse = new HttpResponse(response, originRspData, errMsg);
    networkDomain.responseReceived(requestId, httpResponse);
    networkDomain.loadingFinished(requestId, httpResponse);
  }

}
