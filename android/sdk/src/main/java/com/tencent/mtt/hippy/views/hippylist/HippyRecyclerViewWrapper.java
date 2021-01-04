package com.tencent.mtt.hippy.views.hippylist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;

/**
 * Created by niuniuyang on 2020/12/29.
 * Description
 */
public class HippyRecyclerViewWrapper extends FrameLayout implements HippyViewBase {

  private HippyRecyclerView recyclerView;
  private NativeGestureDispatcher nativeGestureDispatcher;

  public HippyRecyclerViewWrapper(@NonNull Context context, HippyRecyclerView recyclerView) {
    super(context);
    this.recyclerView = recyclerView;
    addView(recyclerView,
      new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
  }

  @Override
  public NativeGestureDispatcher getGestureDispatcher() {
    return nativeGestureDispatcher;
  }

  @Override
  public void setGestureDispatcher(NativeGestureDispatcher dispatcher) {
    nativeGestureDispatcher = dispatcher;
  }

  public int getChildCountWithCaches() {
    return recyclerView.getChildCountWithCaches();
  }

  public View getChildAtWithCaches(int index) {
    return recyclerView.getChildAtWithCaches(index);
  }

  public void setListData() {
    recyclerView.setListData();
  }

  public RecyclerViewEventHelper getRecyclerViewEventHelper() {
    return recyclerView.getRecyclerViewEventHelper();
  }

  public void setScrollEnable(boolean flag) {
    recyclerView.setScrollEnable(flag);
  }

  public void scrollToIndex(int xIndex, int yIndex, boolean animated, int duration) {
    recyclerView.scrollToIndex(xIndex, yIndex, animated, duration);
  }

  public void scrollToContentOffset(double xOffset, double yOffset, boolean animated,
    int duration) {
    recyclerView.scrollToContentOffset(xOffset, yOffset, animated, duration);
  }

  public void scrollToTop() {
    recyclerView.scrollToTop();
  }

  public void setRowShouldSticky(boolean enable) {
    recyclerView.setRowShouldSticky(enable);
  }
}
