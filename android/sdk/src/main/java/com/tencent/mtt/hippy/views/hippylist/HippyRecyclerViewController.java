/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.mtt.hippy.views.hippylist;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.ListViewRenderNode;
import com.tencent.mtt.hippy.uimanager.RenderNode;

/**
 * Created by niuniuyang on 2020/12/22.
 */

@HippyController(name = HippyRecyclerViewController.CLASS_NAME)
public class HippyRecyclerViewController extends HippyViewController<HippyRecyclerViewWrapper> {

  public static final String CLASS_NAME = "RecyclerView";

  @Override
  public int getChildCount(HippyRecyclerViewWrapper viewGroup) {
    return viewGroup.getChildCountWithCaches();
  }

  @Override
  public View getChildAt(HippyRecyclerViewWrapper viewGroup, int index) {
    return viewGroup.getChildAtWithCaches(index);
  }

  @Override
  public void onBatchComplete(HippyRecyclerViewWrapper view) {
    super.onBatchComplete(view);
    view.setListData();
  }

  @Override
  protected View createViewImpl(Context context) {
    return new HippyRecyclerViewWrapper(context,
      new HippyRecyclerView(context, LinearLayoutManager.HORIZONTAL));
  }

  @Override
  protected View createViewImpl(Context context, HippyMap iniProps) {
    if (iniProps != null && iniProps.containsKey("horizontal")) {
      return new HippyRecyclerViewWrapper(context,
        new HippyRecyclerView(context, LinearLayoutManager.HORIZONTAL));
    } else {
      return new HippyRecyclerViewWrapper(context,
        new HippyRecyclerView(context, LinearLayoutManager.VERTICAL));
    }
  }

  @Override
  public RenderNode createRenderNode(int id, HippyMap props, String className,
    HippyRootView hippyRootView, ControllerManager controllerManager, boolean lazy) {
    return new ListViewRenderNode(id, props, className, hippyRootView, controllerManager, lazy);
  }

  @HippyControllerProps(name = "rowShouldSticky")
  public void setRowShouldSticky(HippyRecyclerViewWrapper view, boolean enable) {
    view.setRowShouldSticky(enable);
  }

  @HippyControllerProps(name = "onScrollBeginDrag", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setScrollBeginDragEventEnable(HippyRecyclerViewWrapper view, boolean flag) {
    view.getRecyclerViewEventHelper().setScrollBeginDragEventEnable(flag);
  }

  @HippyControllerProps(name = "onScrollEndDrag", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setScrollEndDragEventEnable(HippyRecyclerViewWrapper view, boolean flag) {
    view.getRecyclerViewEventHelper().setScrollEndDragEventEnable(flag);
  }

  @HippyControllerProps(name = "onMomentumScrollBegin", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setMomentumScrollBeginEventEnable(HippyRecyclerViewWrapper view, boolean flag) {
    view.getRecyclerViewEventHelper().setMomentumScrollBeginEventEnable(flag);
  }

  @HippyControllerProps(name = "onMomentumScrollEnd", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setMomentumScrollEndEventEnable(HippyRecyclerViewWrapper view, boolean flag) {
    view.getRecyclerViewEventHelper().setMomentumScrollEndEventEnable(flag);
  }

  @HippyControllerProps(name = "onScrollEnable", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setOnScrollEventEnable(HippyRecyclerViewWrapper view, boolean flag) {
    view.getRecyclerViewEventHelper().setOnScrollEventEnable(flag);
  }

  @HippyControllerProps(name = "exposureEventEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setExposureEventEnable(HippyRecyclerViewWrapper view, boolean flag) {
    view.getRecyclerViewEventHelper().setExposureEventEnable(flag);
  }

  @HippyControllerProps(name = "scrollEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setScrollEnable(HippyRecyclerViewWrapper view, boolean flag) {
    view.setScrollEnable(flag);
  }

  @HippyControllerProps(name = "scrollEventThrottle", defaultType = HippyControllerProps.NUMBER, defaultNumber = 30.0D)
  public void setscrollEventThrottle(HippyRecyclerViewWrapper view, int scrollEventThrottle) {
    view.getRecyclerViewEventHelper().setScrollEventThrottle(scrollEventThrottle);
  }

  @HippyControllerProps(name = "preloadItemNumber")
  public void setPreloadItemNumber(HippyRecyclerViewWrapper view, int preloadItemNumber) {
    //FIXME niuniuyang ,配合分页加载使用
//    RecyclerViewBase.Adapter adapter = view.getAdapter();
//    if (adapter instanceof HippyListAdapter) {
//      ((HippyListAdapter) adapter).setPreloadItemNumber(preloadItemNumber);
//    }
  }

  @Override
  public void dispatchFunction(HippyRecyclerViewWrapper view, String functionName,
    HippyArray dataArray) {
    super.dispatchFunction(view, functionName, dataArray);
    //FIXME niuniuyang
    switch (functionName) {
      case "scrollToIndex": {
        // list滑动到某个item
        int xIndex = dataArray.getInt(0);
        int yIndex = dataArray.getInt(1);
        boolean animated = dataArray.getBoolean(2);
        int duration = dataArray.getInt(3); //1.2.7 增加滚动时间 ms,animated==true时生效
        view.scrollToIndex(xIndex, yIndex, animated, duration);
        break;
      }
      case "scrollToContentOffset": {
        // list滑动到某个距离
        double xOffset = dataArray.getDouble(0);
        double yOffset = dataArray.getDouble(1);
        boolean animated = dataArray.getBoolean(2);
        int duration = dataArray.getInt(3);  //1.2.7 增加滚动时间 ms,animated==true时生效
        view.scrollToContentOffset(xOffset, yOffset, animated, duration);
        break;
      }
      case "scrollToTop": {
        view.scrollToTop();
        break;
      }
    }
  }
}
