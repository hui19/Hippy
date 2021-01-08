package com.tencent.mtt.hippy.views.hippylist;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.HippyRecyclerViewBase;
import android.support.v7.widget.LinearLayoutManager;
import android.view.MotionEvent;
import android.view.View;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.nxeasy.listview.skikcy.StickyHeaderHelper;

/**
 * Created by niuniuyang on 2020/12/22.
 * Description
 */
public class HippyRecyclerView extends HippyRecyclerViewBase {

    private HippyEngineContext hippyEngineContext;
    private HippyRecyclerListAdapter listAdapter;
    private RecyclerViewEventHelper recyclerViewEventHelper;
    private boolean isEnableScroll;
    private boolean enableSticky;
    private StickyHeaderHelper stickyHeaderHelper;//FIXME niuniuayng 后续实现

    public HippyRecyclerView(Context context, int orientation) {
        super(context);
        init(context, orientation);
        setItemAnimator(null);
        recyclerViewEventHelper = new RecyclerViewEventHelper(this);
    }

    public HippyRecyclerView(Context context) {
        super(context);
        init(context, LinearLayoutManager.VERTICAL);
    }

    private void init(Context context, int orientation) {
        hippyEngineContext = ((HippyInstanceContext) context).getEngineContext();
        this.setLayoutManager(new LinearLayoutManager(context, orientation, false));
        listAdapter = new HippyRecyclerListAdapter(this, hippyEngineContext);
        setAdapter(listAdapter);
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
        LogUtils.d("HippyRecyclerView", "itemCount =" + listAdapter.getItemCount());
        listAdapter.notifyDataSetChanged();
        //notifyDataSetChanged 本身是可以触发requestLayout的，但是Hippy框架下 HippyRootView 已经把
        //onLayout方法重载写成空方法，requestLayout不会回调孩子节点的onLayout，这里需要自己发起dispatchLayout
        dispatchLayout();
    }

    /**
     * 内容偏移，返回recyclerView顶部被滑出去的内容
     * 1、找到顶部第一个View前面的逻辑内容高度
     * 2、加上第一个View被遮住的区域
     */
    public int getContentOffsetY() {
        int firstChildPosition = getFirstChildPosition();
        int totalHeightBeforePosition = getTotalHeightBefore(firstChildPosition);
        int firstChildOffset =
                listAdapter.getItemHeight(firstChildPosition) - getVisibleHeight(getChildAt(0));
        return totalHeightBeforePosition + firstChildOffset;
    }

    /**
     * 内容偏移，返回recyclerView被滑出去的内容
     * 1、找到顶部第一个View前面的逻辑内容宽度
     * 2、加上第一个View被遮住的区域
     */
    public int getContentOffsetX() {
        int firstChildPosition = getFirstChildPosition();
        int totalWidthBeforePosition = getTotalWithBefore(firstChildPosition);
        int firstChildOffset =
                listAdapter.getItemWidth(firstChildPosition) - getVisibleWidth(getChildAt(0));
        return totalWidthBeforePosition + firstChildOffset;
    }

    /**
     * 获取一个View的可视高度，并非view本身的height，有可能部分是被滑出到屏幕外部
     */
    protected int getVisibleHeight(View firstChildView) {
        return getViewVisibleRect(firstChildView).height();
    }

    /**
     * 获取一个View的可视高度，并非view本身的height，有可能部分是被滑出到屏幕外部
     */
    protected int getVisibleWidth(View firstChildView) {
        return getViewVisibleRect(firstChildView).width();
    }

    /**
     * 获取view在父亲中的可视区域
     */
    private Rect getViewVisibleRect(View view) {
        Rect rect = new Rect();
        if (view != null) {
            view.getLocalVisibleRect(rect);
        }
        return rect;
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


    /**
     * 获取position 前面的内容高度，不包含position自身的高度
     */
    protected int getTotalWithBefore(int position) {
        int totalWidthBefore = 0;
        for (int i = 0; i < position; i++) {
            totalWidthBefore += listAdapter.getItemWidth(i);
        }
        return totalWidthBefore;
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

    /**
     * FIXME niuniuyang 这没有requestLayout的逻辑，测试需要看看会不会有问题。
     */
    public void scrollToIndex(int xIndex, int yPosition, boolean animated, int duration) {
        if (animated) {
            doSmoothScrollY(duration, getTotalHeightBefore(yPosition) - getContentOffsetY());
        } else {
            scrollToPosition(yPosition);
        }
    }

    public void scrollToContentOffset(double xOffset, double yOffset, boolean animated,
            int duration) {
        int yOffsetInPixel = (int) PixelUtil.dp2px(yOffset);
        if (animated) {
            doSmoothScrollY(duration, yOffsetInPixel - getContentOffsetY());
        } else {
            scrollBy(0, yOffsetInPixel - getContentOffsetY());
        }
    }

    private void doSmoothScrollY(int duration, int scrollToYPos) {
        if (duration != 0) {
            if (scrollToYPos != 0 && !didStructureChange()) {
                smoothScrollBy(0, scrollToYPos, duration);
            }
        } else {
            smoothScrollBy(0, scrollToYPos);
        }
    }

    public void scrollToTop() {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager.canScrollHorizontally()) {
            smoothScrollBy(-getContentOffsetX(), 0);
        } else {
            smoothScrollBy(0, -getContentOffsetY());
        }
    }

    /**
     * @param enable true ：支持Item 上滑吸顶功能
     */
    public void setRowShouldSticky(boolean enable) {
        this.enableSticky = enable;
        if (enableSticky) {
            if (stickyHeaderHelper == null) {
                stickyHeaderHelper = new StickyHeaderHelper(this, listAdapter);
                addOnScrollListener(stickyHeaderHelper);
            }
        } else {
            if (stickyHeaderHelper != null) {
                removeOnScrollListener(stickyHeaderHelper);
            }
        }
    }

    public void onHeaderRefreshFinish() {

    }

    public void onHeaderRefresh() {

    }
}
