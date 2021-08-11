package com.tencent.mtt.hippy.devsupport.inspector.model;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.tencent.mtt.hippy.adapter.http.HippyHttpRequest;
import com.tencent.mtt.hippy.adapter.http.HippyHttpResponse;
import com.tencent.mtt.hippy.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkModel {

  private static final String TAG = "NetworkModel";

  private Map<String, HippyHttpRequest> requestMap = new HashMap<>();
  private Map<String, HttpResponse> responseMap = new HashMap<>();

  public JSONObject getResponseBody(String requestId) {
    JSONObject responseBody = new JSONObject();
    try {
      HttpResponse response = responseMap.get(requestId);
      JSONObject responseHeaderJson = getResponseHeaderJson(response.response);
      String mimeType = getResponseMimeType(responseHeaderJson);
      String data = readAsString(response.originalData, mimeType);
      responseBody.put("body", data);
      responseBody.put("base64Encoded", false);

      requestMap.remove(requestId);
      responseMap.remove(requestId);
    } catch (Exception e) {
      LogUtils.e(TAG, "getResponseBody, exception=", e);
    }
    return responseBody;
  }

  public JSONObject getLoadingFinished(String requestId, HttpResponse response) {
    JSONObject loadingFinished = new JSONObject();
    try {
      loadingFinished.put("requestId", requestId);
      loadingFinished.put("timestamp", System.currentTimeMillis() / 1000);
      JSONObject responseHeaderJson = getResponseHeaderJson(response.response);
      String mimeType = getResponseMimeType(responseHeaderJson);
      loadingFinished.put("encodedDataLength", getEncodedDataLength(response, mimeType));
      loadingFinished.put("shouldReportCorbBlocking", false);
    } catch (Exception e) {
      LogUtils.e(TAG, "getLoadingFinished, exception=", e);
    }
    return loadingFinished;
  }

  public JSONObject getResponseReceived(String requestId, HttpResponse response) {
    JSONObject responseReceived = new JSONObject();
    try {
      responseReceived.put("requestId", requestId);
      responseReceived.put("loaderId", requestId);
      responseReceived.put("timestamp", System.currentTimeMillis() / 1000);
      responseReceived.put("type", ResourceType.FETCH);
      JSONObject responseObj = new JSONObject();
      HippyHttpRequest request = requestMap.get(requestId);
      responseObj.put("url", request.getUrl());
      responseObj.put("status", response.response.getStatusCode());
      responseObj.put("statusText", response.response.getResponseMessage());
      JSONObject responseHeaderJson = getResponseHeaderJson(response.response);
      String mimeType = getResponseMimeType(responseHeaderJson);
      responseObj.put("headers", responseHeaderJson);
      responseObj.put("mimeType", mimeType);
      responseObj.put("requestHeaders", getRequestHeaderJson(request));
      responseObj.put("connectionReused", false);
      responseObj.put("connectionId", Long.parseLong(requestId));
      responseObj.put("fromDiskCache", false);
      responseObj.put("fromPrefetchCache", false);
      responseObj.put("fromServiceWorker", false);
      responseObj.put("encodedDataLength", getEncodedDataLength(response, mimeType));
      responseObj.put("securityState", SecurityState.SECURE);
      responseReceived.put("response", responseObj);
      responseMap.put(requestId, response);
    } catch (Exception e) {
      LogUtils.e(TAG, "getResponseReceived, exception=", e);
    }
    return responseReceived;
  }

  public JSONObject getRequestData(String requestId, HippyHttpRequest request) {
    JSONObject requestData = new JSONObject();
    try {
      requestData.put("requestId", requestId);
      requestData.put("loaderId", requestId);
      long currentTime = System.currentTimeMillis() / 1000;
      requestData.put("timestamp", currentTime);
      requestData.put("wallTime", currentTime);
      Initiator initiator = new Initiator();
      initiator.type = InitiatorType.OTHER;
      requestData.put("initiator", initiator.toJson());
      JSONObject requestObj = new JSONObject();
      requestObj.put("url", request.getUrl());
      requestObj.put("method", request.getMethod().toUpperCase());
      requestObj.put("header", getRequestHeaderJson(request));
      String body = request.getBody();
      requestObj.put("postData", body);
      requestObj.put("hasPostData", !TextUtils.isEmpty(body));
      JSONArray postDataEntry = new JSONArray();
      JSONObject bodyObj = new JSONObject();
      if (!TextUtils.isEmpty(body)) {
        String bodyStr = Base64.encodeToString(body.getBytes(), Base64.DEFAULT);
        bodyObj.put("bytes", bodyStr);
      } else {
        bodyObj.put("bytes", "");
      }
      postDataEntry.put(bodyObj);
      requestObj.put("postDataEntries", postDataEntry);
      requestObj.put("mixedContentType", SecurityMixedContentType.NONE);
      requestObj.put("initialPriority", ResourcePriority.VERYHIGH);
      requestObj.put("referrerPolicy", ReferrerPolicy.STRICTORIGINWHENCROSSORIGIN);
      requestData.put("request", requestObj);
      requestData.put("type", ResourceType.FETCH);

      requestMap.put(requestId, request);
    } catch (Exception e) {
      LogUtils.e(TAG, "handleRequestFromBackend, exception=", e);
    }
    return requestData;
  }

  private int getEncodedDataLength(HttpResponse response, String mimeType) {
    String data = readAsString(response.originalData, mimeType);
    return data.length();
  }

  static final Pattern CHARSET_PATTERN = Pattern.compile("charset=([a-z0-9-]+)");

  static String readAsString(byte[] data, String cType) {
    String charset = "utf-8";
    if (cType != null) {
      Matcher matcher = CHARSET_PATTERN.matcher(cType.toLowerCase());
      if (matcher.find()) {
        charset = matcher.group(1);
      }
    }
    try {
      return new String(data, charset);
    } catch (UnsupportedEncodingException e) {
      LogUtils.e(TAG, "readAsString, exception=", e);
      return new String(data);
    }
  }

  private String getResponseMimeType(JSONObject responseHeaderJson) {
    String mimeType = responseHeaderJson.optString("content-type");
    // TODO 如果拿不到 从responseType拿
    return mimeType;
  }

  private JSONObject getResponseHeaderJson(HippyHttpResponse response) {
    JSONObject headerObj = new JSONObject();
    try {
      Map<String, List<String>> headerMap = response.getRspHeaderMaps();
      for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
        String key = entry.getKey();
        List<String> value = entry.getValue();
        if (value != null && value.size() > 0) {
          if (value.size() > 1) {
            headerObj.put(key, joinList(";", (List<String>) value));
          } else {
            headerObj.put(key, value.get(0));
          }
        }
      }
    } catch (Exception e) {
      LogUtils.e(TAG, "getResponseHeaderJson, exception=", e);
    }
    return headerObj;
  }

  private JSONObject getRequestHeaderJson(HippyHttpRequest request) {
    JSONObject headerObj = new JSONObject();
    try {
      Map<String, Object> headerMap = request.getHeaders();
      for (Map.Entry<String, Object> entry : headerMap.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if (value instanceof List) {
          headerObj.put(key, joinList(";", (List<String>) value));
        } else if (value instanceof String) {
          headerObj.put(key, (String) value);
        }
      }
    } catch (Exception e) {
      LogUtils.e(TAG, "getRequestHeaderJson, exception=", e);
    }
    return headerObj;
  }

  private String joinList(final String separator, List<String> list) {
    if (list.size() == 0) {
      return "";
    }

    boolean notFirst = false;
    final StringBuilder ret = new StringBuilder();
    for (final Object obj : list) {
      if(notFirst) {
        ret.append(separator);
      }
      ret.append(obj.toString());
      notFirst = true;
    }
    return ret.toString();
  }

  /**
   *  The referrer policy of the request, as defined in https://www.w3.org/TR/referrer-policy/
   *  https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Referrer-Policy
   */
  public static class ReferrerPolicy {
    // 默认值
    // 在没有指定任何策略的情况下用户代理的默认行为。
    // 在同等安全级别的情况下，引用页面的地址会被发送(HTTPS->HTTPS)，但是在降级的情况下不会被发送 (HTTPS->HTTP)。
    public static final String NOREFERRERWHENDOWNGRADE = "no-referrer-when-downgrade";
    // 无论是同源请求还是非同源请求，都发送完整的 URL（移除参数信息之后）作为引用地址。
    // 这项设置会将受 TLS 安全协议保护的资源的源和路径信息泄露给非安全的源服务器。进行此项设置的时候要慎重考虑。
    public static final String UNSAFEURL = "unsafe-url";
    // 整个 Referer  首部会被移除。访问来源信息不随着请求一起发送。
    public static final String NOREFERRER = "no-referrer";
    // 在任何情况下，仅发送文件的源作为引用地址。例如  https://example.com/page.html 会将 https://example.com/ 作为引用地址。
    public static final String ORIGIN = "origin";
    // 对于同源的请求，会发送完整的URL作为引用地址，但是对于非同源请求仅发送文件的源。
    public static final String ORIGINWHENCROSSORIGIN = "origin-when-cross-origin";
    // 对于同源的请求会发送引用地址，但是对于非同源请求则不发送引用地址信息。
    public static final String SAMEORIGIN = "same-origin";
    // 在同等安全级别的情况下，发送文件的源作为引用地址(HTTPS->HTTPS)，但是在降级的情况下不会发送 (HTTPS->HTTP)。
    public static final String STRICTORIGIN = "strict-origin";
    // 对于同源的请求，会发送完整的URL作为引用地址；在同等安全级别的情况下，发送文件的源作为引用地址(HTTPS->HTTPS)；
    // 在降级的情况下不发送此首部 (HTTPS->HTTP)。
    public static final String STRICTORIGINWHENCROSSORIGIN = "strict-origin-when-cross-origin";
  }

  /**
   *  https://chromedevtools.github.io/devtools-protocol/tot/Network/#type-ResourcePriority
   *  Loading priority of a resource request.
   */
  class ResourcePriority {
    public static final String VERYLOW = "VeryLow";
    public static final String LOW = "Low";
    public static final String MEDIUM = "Medium";
    public static final String HIGH = "High";
    public static final String VERYHIGH = "VeryHigh";
  }

  /**
   *  https://chromedevtools.github.io/devtools-protocol/tot/Security/#type-MixedContentType
   *  A description of mixed content (HTTP resources on HTTPS pages), as defined by https://www.w3.org/TR/mixed-content/#categories
   */
  public static class SecurityMixedContentType {
    public static final String BLOCKABLE = "blockable";
    public static final String OPTIONALLYBLOCKABLE = "optionally-blockable";
    public static final String NONE = "none";
  }

  /**
   *  https://chromedevtools.github.io/devtools-protocol/tot/Network/#type-ResourceType
   *  Resource type as it was perceived by the rendering engine.
   */
  public static class ResourceType {
    public static String DOCUMENT = "Document";
    public static String STYLESHEET = "Stylesheet";
    public static String IMAGE = "Image";
    public static String MEDIA = "Media";
    public static String FONT = "Font";
    public static String SCRIPT = "Script";
    public static String TEXTTRACK = "TextTrack";
    public static String XHR = "XHR";
    public static String FETCH = "Fetch";
    public static String EVENTSOURCE = "EventSource";
    public static String WEBSOCKET = "WebSocket";
    public static String MANIFEST = "Manifest";
    public static String SIGNEDEXCHANGE = "SignedExchange";
    public static String PING = "Ping";
    public static String CSPVIOLATIONREPORT = "CSPViolationReport";
    public static String PREFLIGHT = "Preflight";
    public static String OTHER = "Other";
  }

  /**
   *   https://chromedevtools.github.io/devtools-protocol/tot/Network/#type-Initiator
   */
  public static class Initiator {
    // use InitiatorType variable member
    public String type;
    // Initiator JavaScript stack trace, set for Script only.
    public RuntimeStackTrace stack;
    // Initiator URL, set for Parser type or for Script type (when script is importing module) or for SignedExchange type.
    public String url;
    // Initiator line number, set for Parser type or for Script type (when script is importing module) (0-based).
    public Integer lineNumber;
    // Initiator column number, set for Parser type or for Script type (when script is importing module) (0-based)
    public Integer columnNumber;
    // Set if another request triggered this request (e.g. preflight).
    // Unique request identifier.
    public String requestId;

    public JSONObject toJson() {
      JSONObject result = new JSONObject();
      try {
        result.put("type", type);
        if (stack != null) {
          result.put("stack", stack);
        }
        if (url != null) {
          result.put("url", url);
        }
        if (lineNumber != null) {
          result.put("lineNumber", lineNumber);
        }
        if (columnNumber != null) {
          result.put("columnNumber", columnNumber);
        }
        if (requestId != null) {
          result.put("requestId", requestId);
        }
      } catch (Exception e) {
        LogUtils.e(TAG, "Initiator, exception=", e);
      }
      return result;
    }
  }

  public static class InitiatorType {
    public static String PARSER = "parser";
    public static String SCRIPT = "script";
    public static String PRELOAD = "preload";
    public static String SIGNEDEXCHANGE = "SignedExchange";
    public static String PREFLIGHT = "preflight";
    public static String OTHER = "other";
  }

  /**
   *   https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-StackTrace
   *   Call frames for assertions or error messages.
   */
  public static class RuntimeStackTrace {
    // String label of this stack trace.
    // For async traces this may be a name of the function that initiated the async call.
    public String description;
    // JavaScript function name.
    public List<RuntimeCallFrame> callFrames;
    // Asynchronous JavaScript stack trace that preceded this stack, if available.
    public RuntimeStackTrace parent;
    // Asynchronous JavaScript stack trace that preceded this stack, if available.
    public RuntimeStackTraceId parentId;

    public JSONObject toJson() {
      JSONObject result = new JSONObject();
      try {
        result.put("callFrames", callFrames);
        if (!TextUtils.isEmpty(description)) {
          result.put("description", description);
        }
        if (parent != null) {
          result.put("parent", parent);
        }
        if (parentId != null) {
          result.put("parentId", parentId);
        }
      } catch (Exception e) {
        LogUtils.e(TAG, "RuntimeStackTrace, exception=", e);
      }
      return result;
    }
  }

  /**
   *   https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-StackTraceId
   */
  public static class RuntimeStackTraceId {
    public String id;
    // https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-UniqueDebuggerId
    // Unique identifier of current debugger.
    public String debuggerId;

    public JSONObject toJson() {
      JSONObject result = new JSONObject();
      try {
        result.put("id", id);
        if (!TextUtils.isEmpty(debuggerId)) {
          result.put("debuggerId", debuggerId);
        }
      } catch (Exception e) {
        LogUtils.e(TAG, "RuntimeStackTraceId, exception=", e);
      }
      return result;
    }
  }

  /**
   * https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-CallFrame
   * Stack entry for runtime errors and assertions.
   */
  static class RuntimeCallFrame {
    // JavaScript function name.
    public String functionName;
    // https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-ScriptId
    // Unique script identifier.
    public String scriptId;
    // JavaScript script name or url.
    public String url;
    // JavaScript script line number (0-based).
    public int lineNumber;
    // JavaScript script column number (0-based).
    public int columnNumber;

    public JSONObject toJson() {
      JSONObject result = new JSONObject();
      try {
        result.put("functionName", functionName);
        result.put("scriptId", functionName);
        result.put("url", url);
        result.put("lineNumber", lineNumber);
        result.put("columnNumber", columnNumber);
      } catch (Exception e) {
        LogUtils.e(TAG, "RuntimeCallFrame, exception=", e);
      }
      return result;
    }
  }

  /**
   *  https://chromedevtools.github.io/devtools-protocol/tot/Security/#type-SecurityState
   *  The security level of a page or resource.
   */
  static class SecurityState {
    public static final String UNKNOWN = "unknown";
    public static final String NEUTRAL = "neutral";
    public static final String INSECURE = "insecure";
    public static final String SECURE = "secure";
    public static final String INFO = "info";
    public static final String INSECUREBROKEN = "insecure-broken";
  }

}
