package com.tencent.mtt.hippy.devsupport.inspector.domain;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.devsupport.inspector.Inspector;
import com.tencent.mtt.hippy.devsupport.inspector.model.DomModel;
import org.json.JSONObject;

public class DomDomain extends InspectorDomain {

  private static final String TAG = "DomDomain";

  private static final String METHOD_GET_DOCUMENT = "getDocument";
  private static final String METHOD_GET_BOX_MODEL = "getBoxModel";
  private static final String METHOD_GET_NODE_FOR_LOCATION = "getNodeForLocation";
  private static final String METHOD_REMOVE_NODE = "removeNode";
  private static final String METHOD_SET_INSPECT_NODE = "setInspectedNode";

  private DomModel domModel;

  public DomDomain(Inspector inspector) {
    super(inspector);
    domModel = new DomModel();
  }

  @Override public String getDomainName() {
    return "DOM";
  }

  @Override
  public void handleRequest(HippyEngineContext context, String method, int id, JSONObject paramsObj) {
    switch (method) {
      case METHOD_GET_DOCUMENT:
        handleGetDocument(context, id);
        break;
      case METHOD_GET_BOX_MODEL:
        break;
      case METHOD_GET_NODE_FOR_LOCATION:
        break;
      case METHOD_REMOVE_NODE:
        break;
      case METHOD_SET_INSPECT_NODE:
        break;
      default:
        break;
    }
  }

  private void handleGetDocument(HippyEngineContext context, int id) {
    String document = domModel.getDocument(context);
    sendRspToFrontend(id, document);
  }


}