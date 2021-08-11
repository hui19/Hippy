package com.tencent.mtt.hippy.devsupport.inspector.model;

import com.tencent.mtt.hippy.adapter.http.HippyHttpResponse;

public class HttpResponse {

  public HippyHttpResponse response;

  public byte[] originalData;
  public String errMsg;

  public HttpResponse(HippyHttpResponse response, byte[] originalData, String errMsg) {
    this.response = response;
    this.originalData = originalData;
    this.errMsg = errMsg;
  }
}
