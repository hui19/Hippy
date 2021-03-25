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
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.HippyRecyclerViewBase;
import android.support.v7.widget.IHippyViewAboundListener;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.nxeasy.recyclerview.helper.skikcy.IHeaderAttachListener;
import com.tencent.mtt.nxeasy.recyclerview.helper.skikcy.IHeaderHost;
import com.tencent.mtt.nxeasy.recyclerview.helper.skikcy.StickyHeaderHelper;

/**
 * Created by niuniuyang on 2020/12/22. Description
 */
public class HippyRecyclerView<ADP extends HippyRecyclerListAdapter> extends
        HippyRecyclerViewBase implements IHeaderAttachListener,
        IHippyViewAboundListener {

    protected HippyEngineContext hippyEngineContext;
    protected ADP listAdapter;
    protected boolean isEnableScroll = true;//使能ListView的滚动功能
    protected StickyHeaderHelper stickyHeaderHelper;//支持吸顶
    protected IHeaderHost headerHost;//用于pullHeader下拉刷新
    protected LayoutManager layoutManager;
    protected RecyclerViewEventHelper recyclerViewEventHelper;//事件集合
    private NodePositionHelper nodePositionHelper;

    public HippyRecyclerView(Context context) {
        super(context);
    }

    public HippyRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HippyRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ADP getAdapter() {
        return listAdapter;
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        listAdapter = (ADP) adapter;
        super.setAdapter(adapter);
    }

    public NodePositionHelper getNodePositionHelper() {
        if (nodePositionHelper == null) {
            nodePositionHelper = new NodePositionHelper();
        }
        return nodePositionHelper;
    }

    public void setOrientation(LinearLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    public void setHeaderHost(IHeaderHost headerHost) {
        this.headerHost = headerHost;
    }

    public void setHippyEngineContext(HippyEngineContext hippyEngineContext) {
        this.hippyEngineContext = hippyEngineContext;
    }

    public void initRecyclerView() {
        setAdapter(new HippyRecyclerListAdapter<HippyRecyclerView>(this,
                this.hippyEngineContext));
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
     * 内容偏移，返回recyclerView顶部被滑出去的内容 1、找到顶部第一个View前面的逻辑内容高度 2、加上第一个View被遮住的区域
     */
    public int getContentOffsetY() {
        return computeVerticalScrollOffset();
    }

    /**
     * 内容偏移，返回recyclerView被滑出去的内容 1、找到顶部第一个View前面的逻辑内容宽度 2、加上第一个View被遮住的区域
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
    public int getTotalHeightBefore(int position) {
        int totalHeightBefore = 0;
        for (int i = 0; i < position; i++) {
            totalHeightBefore += listAdapter.getItemHeight(i);
        }
        return totalHeightBefore;
    }

    /**
     * 获取renderNodePosition前面的内容高度，不包含renderNodePosition自身的高度
     */
    public int getRenderNodeHeightBefore(int renderNodePosition) {
        int renderNodeTotalHeight = 0;
        for (int i = 0; i < renderNodePosition; i++) {
            renderNodeTotalHeight += listAdapter.getRenderNodeHeight(i);
        }
        return renderNodeTotalHeight;
    }


    /**
     * 获取position 前面的内容高度，不包含position自身的高度
     */
    public int getTotalWithBefore(int position) {
        int totalWidthBefore = 0;
        for (int i = 0; i < position; i++) {
            totalWidthBefore += listAdapter.getItemWidth(i);
        }
        return totalWidthBefore;
    }

    public RecyclerViewEventHelper getRecyclerViewEventHelper() {
        if (recyclerViewEventHelper == null) {
            recyclerViewEventHelper = createEventHelper();
        }
        return recyclerViewEventHelper;
    }

    protected RecyclerViewEventHelper createEventHelper() {
        return new RecyclerViewEventHelper(this);
    }

    /**
     * 设置recyclerView可以滚动
     */
    public void setScrollEnable(boolean enable) {
        isEnableScroll = enable;
    }

    public int getNodePositionInAdapter(int position) {
        return position;
    }

    public void scrollToIndex(int xIndex, int yPosition, boolean animated, int duration) {
        int positionInAdapter = getNodePositionInAdapter(yPosition);
        if (animated) {
            doSmoothScrollY(duration, getTotalHeightBefore(positionInAdapter) - getContentOffsetY());
        } else {
            scrollToPosition(positionInAdapter);
        }
        postDispatchLayout();
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
                postDispatchLayout();
            }
        } else {
            smoothScrollBy(0, scrollToYPos);
            postDispatchLayout();
        }
    }

    private void postDispatchLayout() {
        post(new Runnable() {
            @Override
            public void run() {
                dispatchLayout();
            }
        });
    }

    public void scrollToTop() {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager.canScrollHorizontally()) {
            smoothScrollBy(-getContentOffsetX(), 0);
        } else {
            smoothScrollBy(0, -getContentOffsetY());
        }
        postDispatchLayout();
    }

    /**
     * @param enable true ：支持Item 上滑吸顶功能
     */
    public void setRowShouldSticky(boolean enable) {
        if (enable) {
            if (stickyHeaderHelper == null) {
                stickyHeaderHelper = new StickyHeaderHelper(this, listAdapter, this, headerHost);
                addOnScrollListener(stickyHeaderHelper);
            }
        } else {
            if (stickyHeaderHelper != null) {
                removeOnScrollListener(stickyHeaderHelper);
            }
        }
    }

    /**
     * 同步删除RenderNode对应注册的View，deleteChild是递归删除RenderNode创建的所有的view
     */
    @Override
    public void onViewAbound(HippyRecyclerViewHolder viewHolder) {
        if (viewHolder.bindNode != null && !viewHolder.bindNode.isDelete()) {
            viewHolder.bindNode.setLazy(true);
            RenderNode parentNode = viewHolder.bindNode.getParent();
            if (parentNode != null) {
                hippyEngineContext.getRenderManager().getControllerManager()
                        .deleteChild(parentNode.getId(), viewHolder.bindNode.getId());
            }
            viewHolder.bindNode.setRecycleItemTypeChangeListener(null);
        }
    }

    /**
     * 当header被摘下来，需要对header进行还原或者回收对处理 遍历所有都ViewHolder，看看有没有收纳这个headerView都ViewHolder
     * 如果没有，需要把aboundHeader进行回收，并同步删除render节点对应都view
     *
     * @param aboundHeader HeaderView对应的Holder
     * @param currentHeaderView headerView的实体内容
     */
    @Override
    public void onHeaderDetached(ViewHolder aboundHeader, View currentHeaderView) {
        boolean findHostViewHolder = false;
        for (int i = 0; i < getChildCountWithCaches(); i++) {
            ViewHolder viewHolder = getChildViewHolder(getChildAtWithCaches(i));
            if (isTheSameRenderNode((HippyRecyclerViewHolder) aboundHeader,
                    (HippyRecyclerViewHolder) viewHolder)) {
                findHostViewHolder = true;
                fillContentView(currentHeaderView, viewHolder);
                break;
            }
        }
        //当header无处安放，抛弃view都同时，需要同步给Hippy进行View都删除，不然后续无法创建对应都View
        if (!findHostViewHolder) {
            onViewAbound((HippyRecyclerViewHolder) aboundHeader);
        }
    }

    private boolean fillContentView(View currentHeaderView, ViewHolder viewHolder) {
        if (viewHolder != null && viewHolder.itemView instanceof ViewGroup) {
            ViewGroup itemView = (ViewGroup) viewHolder.itemView;
            if (itemView.getChildCount() <= 0) {
                itemView.addView(currentHeaderView);
            }
        }
        return false;
    }

    private boolean isTheSameRenderNode(HippyRecyclerViewHolder aboundHeader,
            HippyRecyclerViewHolder viewHolder) {
        if (viewHolder.bindNode != null && aboundHeader.bindNode != null) {
            return viewHolder.bindNode.getId() == aboundHeader.bindNode.getId();
        }
        return false;
    }

    public void setNodePositionHelper(NodePositionHelper nodePositionHelper) {
        this.nodePositionHelper = nodePositionHelper;
    }
}
