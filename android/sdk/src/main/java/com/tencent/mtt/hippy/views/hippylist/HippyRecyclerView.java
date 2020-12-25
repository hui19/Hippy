package com.tencent.mtt.hippy.views.hippylist;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.EasyRecyclerView;
import android.support.v7.widget.LinearLayoutManager;
import android.view.MotionEvent;
import android.view.View;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;

/**
 * Created by niuniuyang on 2020/12/22.
 * Description
 */
public class HippyRecyclerView extends EasyRecyclerView implements HippyViewBase {


  private HippyEngineContext hippyEngineContext;
  private Context context;
  private HippyRecyclerListAdapter listAdapter;
  private RecyclerViewEventHelper recyclerViewEventHelper;
  private boolean isEnableScroll;

  public HippyRecyclerView(Context context, int orientation) {
    super(context);
    init(context, orientation);
    recyclerViewEventHelper = new RecyclerViewEventHelper(this);
  }

  private void init(Context context, int orientation) {
    hippyEngineContext = ((HippyInstanceContext) context).getEngineContext();
    this.setLayoutManager(new LinearLayoutManager(context, orientation, false));
    this.context = context;
//  setRepeatableSuspensionMode(false);//FIXME niuniuyang
    listAdapter = new HippyRecyclerListAdapter(this, hippyEngineContext);
    setAdapter(listAdapter);
  }

  public HippyRecyclerView(Context context) {
    super(context);
    init(context, LinearLayoutManager.VERTICAL);
  }

  @Override public NativeGestureDispatcher getGestureDispatcher() {
    return null;
  }

  @Override public void setGestureDispatcher(NativeGestureDispatcher dispatcher) {

  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (!isEnableScroll) {
      return false;
    }
    return super.onTouchEvent(e);
  }

  /**
   * 刷新数据
   */
  public void setListData() {
    listAdapter.notifyDataSetChanged();
  }

  /**
   * 内容偏移，返回recyclerView顶部被滑出去的内容
   * 1、找到顶部第一个View前面的逻辑内容高度
   * 2、加上第一个View被遮住的区域
   */
  public int getContentOffset() {
    int firstChildPosition = getFirstChildPosition();
    int totalHeightBeforePosition = getTotalHeightBefore(firstChildPosition);
    int firstChildOffset =
      listAdapter.getItemHeight(firstChildPosition) - getVisibleHeight(getChildAt(0));
    return totalHeightBeforePosition + firstChildOffset;
  }

  /**
   * 获取一个View的可视高度，并非view本身的height，有可能部分是被滑出到屏幕外部
   */
  protected int getVisibleHeight(View firstChildView) {
    Rect rect = new Rect();
    if (firstChildView != null) {
      firstChildView.getLocalVisibleRect(rect);
    }
    return rect.height();
  }

  /**
   * 获取position 前面的内容高度，不包含position自身的高度
   */
  protected int getTotalHeightBefore(int position) {
    int totalHeightBefore = 0;
    for (int i = 0; i < position; i++) {
      totalHeightBefore += listAdapter.getItemHeight(i);
    }
    return totalHeightBefore;
  }

  public RecyclerViewEventHelper getRecyclerViewEventHelper() {
    return recyclerViewEventHelper;
  }

  /**
   * 子类有继承，兼容之前的接口
   */
  protected void sendExposureEvent(View view, String eventName) {
    recyclerViewEventHelper.sendExposureEvent(view, eventName);
  }

  /**
   * 设置recyclerView可以滚动
   */
  public void setScrollEnable(boolean enable) {
    isEnableScroll = enable;
  }
}
