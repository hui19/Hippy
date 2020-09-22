package com.tencent.mtt.hippy.views.wormhole;


import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.mtt.hippy.HippyEngine;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.RenderManager;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.list.HippyListItemView;
import com.tencent.mtt.hippy.views.wormhole.node.TKDStyleNode;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HippyWormholeManager implements HippyWormholeProxy {
  public static final String WORMHOLE_TAG                       = "hippy_wormhole";

  public static final String WORMHOLE_PARAMS                    = "params";
  public static final String WORMHOLE_WORMHOLE_ID               = "wormholeId";
  public static final String WORMHOLE_CLIENT_DATA_RECEIVED      = "Wormhole.dataReceived";
  public static final String WORMHOLE_SERVER_BATCH_COMPLETE     = "onServerBatchComplete";
  public static final String EVENT_DATARECEIVED                 = "onClientMessageReceived";
  public static final String FUNCTION_SENDEVENT_TO_WORMHOLEVIEW = "sendEventToWormholeView";
  public static final String FUNCTION_ONCUSTOMEVENT             = "onCustomEvent";

  private static final AtomicInteger mWormholeIdCounter = new AtomicInteger(1000);
  private static volatile HippyWormholeManager INSTANCE;
  private HippyEngine mWormholeEngine;
  private HippyRootView mHippyRootView;
  private ConcurrentHashMap<String, ViewGroup> mTkdWormholeMap = new ConcurrentHashMap<String, ViewGroup>();
  private ConcurrentHashMap<String, ViewGroup> mTkdWormholeListitemMap = new ConcurrentHashMap<String, ViewGroup>();
  //存储业务方引擎
  private ArrayList<HippyEngine> mClientEngineList = new ArrayList<>();

  private HippyWormholeManager() {

  }

  public static HippyWormholeManager getInstance() {
    if (INSTANCE == null) {
      synchronized (HippyWormholeManager.class) {
        if (INSTANCE == null) {
          INSTANCE = new HippyWormholeManager();
        }
      }
    }
    return INSTANCE;
  }

  public void setServerEngine(HippyEngine engine, final HippyRootView hippyRootView) {
    mWormholeEngine = engine;
    mHippyRootView = hippyRootView;
  }

  public HippyEngineContext getEngineContext() {
    return mWormholeEngine != null ? mWormholeEngine.getEngineContext() : null;
  }

  public HippyRootView getHippyRootView() {
    return mHippyRootView;
  }

  private RenderNode getWormholeNode(HippyWormholeView wormhole) {
    Context context = wormhole.getContext();
    if (context instanceof HippyInstanceContext) {
      HippyEngineContext engineContext = ((HippyInstanceContext) context).getEngineContext();
      if (engineContext != null) {
        RenderManager rm = engineContext.getRenderManager();
        RenderNode node = rm.getRenderNode(wormhole.getId());
        return node;
      }
    }

    return null;
  }

  private void sendDataReceivedMessageToServer(String wormholeId, HippyMap initProps) {
    HippyMap paramsMap = initProps.getMap(WORMHOLE_PARAMS);
    if (paramsMap != null) {
      HippyMap bundle = paramsMap.copy();
      bundle.pushString(WORMHOLE_WORMHOLE_ID, wormholeId);
      JSONArray jsonArray = new JSONArray();
      jsonArray.put(bundle);
      mWormholeEngine.sendEvent(WORMHOLE_CLIENT_DATA_RECEIVED, jsonArray);
    }
  }

  private void sendBatchCompleteMessageToClient(float width, float height, View view) {
    HippyMap layoutMeasurement = new HippyMap();
    layoutMeasurement.pushDouble("width", PixelUtil.px2dp(width));
    layoutMeasurement.pushDouble("height", PixelUtil.px2dp(height));
    HippyViewEvent event = new HippyViewEvent(WORMHOLE_SERVER_BATCH_COMPLETE);
    event.send(view, layoutMeasurement);
  }

  public void onServerBatchComplete(HippyWormholeView wormholeView) {
    if (wormholeView == null) {
      return;
    }

    RenderNode node = getWormholeNode(wormholeView);
    if (node != null) {
      String businessId = wormholeView.getBusinessId();
      ViewGroup newParent = mTkdWormholeMap.get(businessId);
      if (newParent == null) {
        return;
      }
      ViewGroup oldParent = (ViewGroup) (wormholeView.getParent());
      if (oldParent != newParent) {
        oldParent.removeView(wormholeView);
        newParent.addView(wormholeView, 0);
        //这里校验一下是否需要将创建出来的wormhole及其层级关系挪到WormParentFrameLayout下面
        resetViewLevel(newParent, businessId);
      }
      float width = node.getWidth();
      float height = node.getHeight();
      sendBatchCompleteMessageToClient(width, height, newParent);
    }
  }


  public String getWormholeId() {
    int id = mWormholeIdCounter.getAndIncrement();
    return "" + id;
  }

  public String getWormholeIdFromProps(HippyMap props) {
    HippyMap paramsMap = props.getMap(WORMHOLE_PARAMS);
    if (paramsMap == null) {
      return null;
    }

    String businessId = paramsMap.getString(WORMHOLE_WORMHOLE_ID);
    return businessId;
  }

  @Override
  public void createWormhole(String wormholeId,HippyMap initProps, ViewGroup parent) {
    if (mWormholeEngine == null) {
      return;
    }

    if (initProps == null || parent == null) {
      return;
    }

    if (!TextUtils.isEmpty(wormholeId)) {
      if (!mTkdWormholeMap.containsValue(parent)) {
        mTkdWormholeMap.put(wormholeId, parent);
        sendDataReceivedMessageToServer(wormholeId,initProps);
      }
    }
  }

  public void registerClientEngine(HippyEngine hippyEngine) {
    if (!mClientEngineList.contains(hippyEngine)) {
      mClientEngineList.add(hippyEngine);
    }
  }

  public void unRegisterClientEngine(HippyEngine hippyEngine) {
    if (!mClientEngineList.contains(hippyEngine)) {
      mClientEngineList.remove(hippyEngine);
    }
  }

  //如果是业务方收到了通知之后，应该要告知虫洞
  public void sendMessageToWormhole(HippyMap data) {
    if (mWormholeEngine != null && data != null) {
      mWormholeEngine.sendEvent(EVENT_DATARECEIVED, data);
    }
  }

  //如果是虫洞引擎收到了通知之后，应该要广播给所有的业务方
  public void sendMessageToAllClient(HippyMap data) {
    for (int i = 0; i < mClientEngineList.size(); i++) {
      if (mClientEngineList.get(i) != null) {
        mClientEngineList.get(i).sendEvent(EVENT_DATARECEIVED, data);
      }
    }
  }

  public void onWormholeDestroy(String id) {
    if (!TextUtils.isEmpty(id)) {
      mTkdWormholeMap.remove(id);
      mTkdWormholeListitemMap.remove(id);
    }
  }

  public ViewGroup createListItemView(Context context, RenderNode targetNode) {
    //由于listItem和tkdWormhole的位置关系是确定的，所以这里直接根据listItem取出tkdWormhole的props
    String busid = TKDStyleNode.getWormholeId(targetNode.getChildAt(0).getChildAt(0).getProps());
    if (mTkdWormholeListitemMap.containsKey(busid)) {
      resetViewLevel(mTkdWormholeMap.get(busid),busid);
      ViewGroup result = mTkdWormholeListitemMap.get(busid);
      if (result.getChildCount() <= 0 || ((ViewGroup) result.getChildAt(0)).getChildCount() < 0) {
        //如果该itemView下面没有挂载过tkdWormholeView，触发节点的createView方法创建tkdWormholeView
        targetNode.createViewRecursive();
      }
      return result;
    }
    //如果引擎还在创建wormhole的话，这里先返回一个空viewGroup吧，等wormhole创建好了之后再将其add进来
    WormParentFrameLayout frameLayout = new WormParentFrameLayout(context);
    mTkdWormholeListitemMap.put(busid, frameLayout);
    resetViewLevel(mTkdWormholeMap.get(busid),busid);
    if (frameLayout.getChildCount() <= 0 || ((ViewGroup) frameLayout.getChildAt(0)).getChildCount() < 0) {
      //如果该itemView下面没有挂载过tkdWormholeView，触发节点的createView方法创建tkdWormholeView
      targetNode.createViewRecursive();
    }
    return frameLayout;
  }

  private void resetViewLevel(ViewGroup targetView, String busid) {
    if (targetView == null) {
      return;
    }
    //如果tkdWormHole及其child已经创建出来了，这里需要往上遍历找到HippyListItemView,将HippyListItemView都挪到WormParentFrameLayout下面
    while (targetView.getParent() instanceof ViewGroup) {
      if (targetView instanceof HippyListItemView) {
        break;
      }
      targetView = (ViewGroup) targetView.getParent();
    }
    if (targetView instanceof HippyListItemView) {
      if (targetView.getParent() != null && mTkdWormholeListitemMap.get(busid) != null) {
        ((ViewGroup) targetView.getParent()).removeView(targetView);
      }
      if (mTkdWormholeListitemMap.get(busid) != null) {
        mTkdWormholeListitemMap.get(busid).addView(targetView);
      }
    }
  }
}
