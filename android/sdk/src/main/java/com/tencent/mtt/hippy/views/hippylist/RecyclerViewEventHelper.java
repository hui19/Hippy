package com.tencent.mtt.hippy.views.hippylist;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_SETTLING;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.list.HippyListItemView;
import com.tencent.mtt.hippy.views.scroll.HippyScrollViewEventHelper;

/**
 * Created by niuniuyang on 2020/12/24.
 * Description
 */
class RecyclerViewEventHelper extends OnScrollListener implements OnLayoutChangeListener {

  private final HippyRecyclerView hippyRecyclerView;
  private boolean scrollBeginDragEventEnable;
  private boolean scrollEndDragEventEnable;
  private OnScrollDragEndedEvent onScrollDragEndedEvent;
  private boolean momentumScrollBeginEventEnable;
  private boolean momentumScrollEndEventEnable;
  private OnScrollFlingStartedEvent onScrollFlingStartedEvent;
  private OnScrollFlingEndedEvent onScrollFlingEndedEvent;
  private int currentState;
  private boolean onScrollEventEnable;
  private OnScrollEvent onScrollEvent;
  private long lastScrollEventTimeStamp;
  private int scrollEventThrottle;
  private boolean exposureEventEnable;

  public RecyclerViewEventHelper(HippyRecyclerView recyclerView) {
    this.hippyRecyclerView = recyclerView;
    hippyRecyclerView.addOnScrollListener(this);
    hippyRecyclerView.addOnLayoutChangeListener(this);
  }

  private OnScrollDragStartedEvent onScrollDragStartedEvent;

  public void setScrollBeginDragEventEnable(boolean enable) {
    scrollBeginDragEventEnable = enable;
  }

  public void setScrollEndDragEventEnable(boolean enable) {
    scrollEndDragEventEnable = enable;
  }

  public void setMomentumScrollBeginEventEnable(boolean enable) {
    momentumScrollBeginEventEnable = enable;
  }

  public void setMomentumScrollEndEventEnable(boolean enable) {
    momentumScrollEndEventEnable = enable;
  }

  public void setOnScrollEventEnable(boolean enable) {
    onScrollEventEnable = enable;
  }

  @Override
  public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
    int oldTop, int oldRight, int oldBottom) {
    if (exposureEventEnable) {
      dispatchExposureEvent();
    }
  }

  protected class OnScrollDragStartedEvent extends HippyViewEvent {

    public OnScrollDragStartedEvent(String eventName) {
      super(eventName);
    }
  }

  protected OnScrollDragStartedEvent getOnScrollDragStartedEvent() {
    if (onScrollDragStartedEvent == null) {
      onScrollDragStartedEvent = new OnScrollDragStartedEvent(
        HippyScrollViewEventHelper.EVENT_TYPE_BEGIN_DRAG);
    }
    return onScrollDragStartedEvent;
  }

  protected class OnScrollDragEndedEvent extends HippyViewEvent {

    public OnScrollDragEndedEvent(String eventName) {
      super(eventName);
    }
  }

  protected class OnScrollFlingStartedEvent extends HippyViewEvent {

    public OnScrollFlingStartedEvent(String eventName) {
      super(eventName);
    }
  }

  // scroll
  protected OnScrollEvent getOnScrollEvent() {
    if (onScrollEvent == null) {
      onScrollEvent = new OnScrollEvent(HippyScrollViewEventHelper.EVENT_TYPE_SCROLL);
    }
    return onScrollEvent;
  }

  protected class OnScrollEvent extends HippyViewEvent {

    public OnScrollEvent(String eventName) {
      super(eventName);
    }
  }

  // start fling
  protected OnScrollFlingStartedEvent getOnScrollFlingStartedEvent() {
    if (onScrollFlingStartedEvent == null) {
      onScrollFlingStartedEvent = new OnScrollFlingStartedEvent(
        HippyScrollViewEventHelper.EVENT_TYPE_MOMENTUM_BEGIN);
    }
    return onScrollFlingStartedEvent;
  }

  // end drag event
  protected OnScrollDragEndedEvent getOnScrollDragEndedEvent() {
    if (onScrollDragEndedEvent == null) {
      onScrollDragEndedEvent = new OnScrollDragEndedEvent(
        HippyScrollViewEventHelper.EVENT_TYPE_END_DRAG);
    }
    return onScrollDragEndedEvent;
  }

  @Override
  public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
    int oldState = currentState;
    currentState = newState;
    sendDragEvent(newState);
    sendDragEndEvent(oldState, newState);
    sendFlingEvent(newState);
    sendFlingEndEvent(oldState, newState);
  }

  @Override
  public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
    if (onScrollEventEnable) {
      getOnScrollEvent().send(hippyRecyclerView, generateScrollEvent());
    }
    if (onScrollEventEnable) {
      long currTime = System.currentTimeMillis();
      if (currTime - lastScrollEventTimeStamp < scrollEventThrottle) {
        return;
      }
      lastScrollEventTimeStamp = currTime;
      getOnScrollEvent().send(hippyRecyclerView, generateScrollEvent());
    }
    if (exposureEventEnable) {
      dispatchExposureEvent();
    }
  }

  protected void sendFlingEvent(int newState) {
    if (momentumScrollBeginEventEnable && newState == SCROLL_STATE_SETTLING) {
      getOnScrollFlingStartedEvent().send(hippyRecyclerView, generateScrollEvent());
    }
  }

  protected void sendDragEndEvent(int oldState, int newState) {
    if (scrollEndDragEventEnable && oldState == SCROLL_STATE_DRAGGING
      && newState == RecyclerView.SCROLL_STATE_IDLE) {
      getOnScrollDragEndedEvent().send(hippyRecyclerView, generateScrollEvent());
    }
  }

  protected void sendFlingEndEvent(int oldState, int newState) {
    if (momentumScrollEndEventEnable && oldState == SCROLL_STATE_SETTLING
      && newState != SCROLL_STATE_SETTLING) {
      getOnScrollFlingEndedEvent().send(hippyRecyclerView, generateScrollEvent());
    }
  }

  protected void sendDragEvent(int newState) {
    if (scrollBeginDragEventEnable && newState == RecyclerView.SCROLL_STATE_DRAGGING) {
      getOnScrollDragStartedEvent().send(hippyRecyclerView, generateScrollEvent());
    }
  }

  protected class OnScrollFlingEndedEvent extends HippyViewEvent {

    public OnScrollFlingEndedEvent(String eventName) {
      super(eventName);
    }
  }

  // end fling
  protected OnScrollFlingEndedEvent getOnScrollFlingEndedEvent() {
    if (onScrollFlingEndedEvent == null) {
      onScrollFlingEndedEvent = new OnScrollFlingEndedEvent(
        HippyScrollViewEventHelper.EVENT_TYPE_MOMENTUM_END);
    }
    return onScrollFlingEndedEvent;
  }

  public void setScrollEventThrottle(int scrollEventThrottle) {
    this.scrollEventThrottle = scrollEventThrottle;
  }

  protected HippyMap generateScrollEvent() {
    HippyMap contentOffset = new HippyMap();
    contentOffset.pushDouble("x", PixelUtil.px2dp(0));
    contentOffset.pushDouble("y", PixelUtil.px2dp(hippyRecyclerView.getContentOffset()));
    HippyMap event = new HippyMap();
    event.pushMap("contentOffset", contentOffset);
    return event;
  }

  public void setExposureEventEnable(boolean enable) {
    exposureEventEnable = enable;
  }

  /**
   * 可视面积小于10%，任务view当前已经不在可视区域
   */
  private boolean isViewVisible(View view) {
    if (view == null) {
      return false;
    }
    Rect rect = new Rect();
    boolean visibility = view.getGlobalVisibleRect(rect);
    if (!visibility) {
      return false;
    } else {
      float visibleArea = rect.height() * rect.width(); //可见区域的面积
      float viewArea = view.getMeasuredWidth() * view.getMeasuredHeight();//当前view的总面积
      return visibleArea > viewArea * 0.1f;
    }
  }

  protected void checkExposureView(View view) {
    if (view instanceof HippyListItemView) {
      return;
    }
    HippyListItemView itemView = (HippyListItemView) view;
    if (isViewVisible(view)) {
      if (itemView.getExposureState() != HippyListItemView.EXPOSURE_STATE_APPEAR) {
        hippyRecyclerView.sendExposureEvent(view, HippyListItemView.EXPOSURE_EVENT_APPEAR);
        itemView.setExposureState(HippyListItemView.EXPOSURE_STATE_APPEAR);
      }
    } else {
      if (itemView.getExposureState() != HippyListItemView.EXPOSURE_STATE_DISAPPEAR) {
        hippyRecyclerView.sendExposureEvent(view, HippyListItemView.EXPOSURE_EVENT_DISAPPEAR);
        itemView.setExposureState(HippyListItemView.EXPOSURE_STATE_DISAPPEAR);
      }
    }
  }

  protected void sendExposureEvent(View view, String eventName) {
    if (eventName.equals(HippyListItemView.EXPOSURE_EVENT_APPEAR)
      || eventName.equals(HippyListItemView.EXPOSURE_EVENT_DISAPPEAR)) {
      new HippyViewEvent(eventName).send(view, null);
    }
  }

  private void dispatchExposureEvent() {
    int childCount = hippyRecyclerView.getChildCount();
    for (int i = 0; i < childCount; i++) {
      checkExposureView(hippyRecyclerView.getChildAt(i));
    }
  }
}
