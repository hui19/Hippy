package com.tencent.mtt.hippy.views.wormhole;

import android.content.Context;

import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

public class TKDWormholeView extends HippyViewGroup {
  private String mWormholeId;
  private HippyMap mWormholeDataProps;


  public TKDWormholeView(Context context) {
    super(context);
  }

  public void setWormholeDataProps(HippyMap dataProps) {
    mWormholeDataProps = dataProps;
    setWormholeId(HippyWormholeManager.getInstance().getWormholeIdFromProps(dataProps));
  }

  public HippyMap getWormholeDataProps() {
    return mWormholeDataProps;
  }

  public void setWormholeId(String id) {
    mWormholeId = id;
  }

  public String getWormholeId() {
    return mWormholeId;
  }

}

