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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.support.annotation.NonNull;
import android.support.v7.widget.HippyItemTypeHelper;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.uimanager.DiffUtils;
import com.tencent.mtt.hippy.uimanager.DiffUtils.PatchType;
import com.tencent.mtt.hippy.uimanager.ListItemRenderNode;
import com.tencent.mtt.hippy.uimanager.PullHeaderRenderNode;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.views.list.IRecycleItemTypeChange;
import com.tencent.mtt.nxeasy.recyclerview.helper.skikcy.IStickyItemsProvider;
import java.util.ArrayList;

/**
 * Created by niuniuyang on 2020/12≥/22. Description RecyclerView的子View直接是前端的RenderNode节点，没有之前包装的那层RecyclerViewItem。
 * 对于特殊的renderNode，比如header和sticky的节点，我们进行了不同的处理。
 */
public class HippyRecyclerListAdapter<HRCV extends HippyRecyclerView>
        extends Adapter<HippyRecyclerViewHolder>
        implements IRecycleItemTypeChange, IStickyItemsProvider {

    protected final HippyEngineContext hpContext;
    protected final HRCV hippyRecyclerView;
    protected final HippyItemTypeHelper hippyItemTypeHelper;
    protected int positionToCreateHolder;
    protected PullFooterEventHelper footerEventHelper;
    protected PullHeaderEventHelper headerEventHelper;
    protected PreloadHelper preloadHelper;

    public HippyRecyclerListAdapter(HRCV hippyRecyclerView, HippyEngineContext hpContext) {
        this.hpContext = hpContext;
        this.hippyRecyclerView = hippyRecyclerView;
        hippyItemTypeHelper = new HippyItemTypeHelper(hippyRecyclerView);
        preloadHelper = new PreloadHelper(hippyRecyclerView);
    }

    /**
     * 对于吸顶到RenderNode需要特殊处理 吸顶的View需要包一层ViewGroup，吸顶的时候，从ViewGroup把RenderNode的View取出来挂载到顶部
     * 当RenderNode的View已经挂载到Header位置上面，如果重新触发创建ViewHolder，renderView会创建失败，
     * 此时就只返回一个空到renderViewContainer上去，等viewHolder需要显示到时候，再把header上面的View还原到这个 ViewHolder上面。
     */
    @NonNull
    @Override
    public HippyRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ListItemRenderNode renderNode = getChildNode(positionToCreateHolder);
        renderNode.setLazy(false);
        View renderView = renderNode.createViewRecursive();
        if (isPullHeader(positionToCreateHolder)) {
            initPullHeadEventHelper((PullHeaderRenderNode) renderNode, renderView);
            return new HippyRecyclerViewHolder(headerEventHelper.getView(), renderNode);
        } else if (isStickyPosition(positionToCreateHolder)) {
            return new HippyRecyclerViewHolder(getStickyContainer(parent, renderView), renderNode);
        } else {
            return new HippyRecyclerViewHolder(renderView, renderNode);
        }
    }

    private FrameLayout getStickyContainer(ViewGroup parent, View renderView) {
        FrameLayout container = new FrameLayout(parent.getContext());
        if (renderView != null) {
            container.addView(renderView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        }
        return container;
    }

    private void initPullHeadEventHelper(PullHeaderRenderNode renderNode, View renderView) {
        if (headerEventHelper == null) {
            headerEventHelper = new PullHeaderEventHelper(hippyRecyclerView, renderNode);
        }
        headerEventHelper.setRenderNodeView(renderView);
    }

    /**
     * 绑定数据 对于全新的viewHolder，isCreated 为true，调用updateViewRecursive进行物理树的创建，以及数据的绑定
     * 对于非全新创建的viewHolder，进行view树的diff，然后在把数据绑定到view树上面
     *
     * @param hippyRecyclerViewHolder position当前的viewHolder
     * @param position 绑定数据的节点位置
     */
    @Override
    public void onBindViewHolder(HippyRecyclerViewHolder hippyRecyclerViewHolder, int position) {
        RenderNode oldNode = hippyRecyclerViewHolder.bindNode;
        setLayoutParams(hippyRecyclerViewHolder.itemView, position);
        if (hippyRecyclerViewHolder.isCreated) {
            oldNode.updateViewRecursive();
            hippyRecyclerViewHolder.isCreated = false;
        } else {
            oldNode.setLazy(true);
            ListItemRenderNode toNode = getChildNode(position);
            toNode.setLazy(false);
            //step 1: diff
            ArrayList<PatchType> patchTypes = DiffUtils.diff(oldNode, toNode);
            //step:2 delete unUseful views
            DiffUtils.deleteViews(hpContext.getRenderManager().getControllerManager(), patchTypes);
            //step:3 replace id
            DiffUtils.replaceIds(hpContext.getRenderManager().getControllerManager(), patchTypes);
            //step:4 create view is do not  reUse
            DiffUtils.createView(hpContext.getRenderManager().getControllerManager(), patchTypes);
            //step:5 patch the dif result
            DiffUtils.doPatch(hpContext.getRenderManager().getControllerManager(), patchTypes);
            hippyRecyclerViewHolder.bindNode = toNode;
        }
        hippyRecyclerViewHolder.bindNode.setRecycleItemTypeChangeListener(this);
        enablePullFooter(position, hippyRecyclerViewHolder.itemView);
    }

    /**
     * 检测最后一个item是否是footer，如果是，需要对这个itemView设置监控，footer显示就通知前端加载下一页
     */
    private void enablePullFooter(int position, View itemView) {
        if (position == getItemCount() - 1) {
            ListItemRenderNode renderNode = getChildNode(position);
            if (renderNode.isPullFooter()) {
                if (footerEventHelper == null) {
                    footerEventHelper = new PullFooterEventHelper(hippyRecyclerView);
                }
                footerEventHelper.enableFooter(itemView);
            } else {
                if (footerEventHelper != null) {
                    footerEventHelper.disableFooter();
                }
            }
        }
    }

    /**
     * 设置View的LayoutParams排版属性，宽高由render节点提供
     */
    protected void setLayoutParams(View itemView, int position) {
        LayoutParams childLp = getLayoutParams(itemView);
        RenderNode childNode = getChildNode(position);
        childLp.height = childNode.getHeight();
        childLp.width = childNode.getWidth();
        itemView.setLayoutParams(childLp);
    }

    protected LayoutParams getLayoutParams(View itemView) {
        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        LayoutParams childLp = null;
        if (params instanceof LayoutParams) {
            childLp = (LayoutParams) params;
        }
        if (childLp == null) {
            childLp = new LayoutParams(MATCH_PARENT, 0);
        }
        return childLp;
    }

    @Override
    public int getItemViewType(int position) {
        //在调用onCreateViewHolder之前，必然会调用getItemViewType，所以这里把position记下来
        //用在onCreateViewHolder的时候来创建View，不然onCreateViewHolder是无法创建RenderNode到View的
        setPositionToCreate(position);
        return getChildNode(position).getItemViewType();
    }

    protected void setPositionToCreate(int position) {
        positionToCreateHolder = position;
    }

    /**
     * 获取子节点，理论上面是不会返回空的，否则就是某个流程出了问题
     */
    public ListItemRenderNode getChildNode(int position) {
        return (ListItemRenderNode) getParentNode().getChildAt(position);
    }

    /**
     * listItemView的数量
     */
    @Override
    public int getItemCount() {
        RenderNode listNode = getParentNode();
        if (listNode != null) {
            return listNode.getChildCount();
        }
        return 0;
    }

    public int getItemHeight(int position) {
        ListItemRenderNode childNode = getChildNode(position);
        if (childNode != null) {
            return childNode.getHeight();
        }
        return 0;
    }

    public int getItemWidth(int position) {
        ListItemRenderNode childNode = getChildNode(position);
        if (childNode != null) {
            return childNode.getWidth();
        }
        return 0;
    }

    protected RenderNode getParentNode() {
        return hpContext.getRenderManager().getRenderNode(getHippyListViewId());
    }

    private int getHippyListViewId() {
        return ((View) hippyRecyclerView.getParent()).getId();
    }

    @Override
    public void onRecycleItemTypeChanged(int oldType, int newType,
            ListItemRenderNode listItemNode) {
        hippyItemTypeHelper.updateItemType(oldType, newType, listItemNode);
    }

    @Override
    public long getItemId(int position) {
        return getChildNode(position).getId();
    }

    /**
     * 该position对于的renderNode是否是吸顶的属性
     */
    @Override
    public boolean isStickyPosition(int position) {
        if (position >= 0 && position < getItemCount()) {
            return getChildNode(position).shouldSticky();
        }
        return false;
    }

    /**
     * 该position对于的renderNode是否是Header属性，值判断第一个节点
     */
    private boolean isPullHeader(int position) {
        if (position == 0) {
            return getChildNode(0).isPullHeader();
        }
        return false;
    }

    /**
     * 获取下拉刷新的事件辅助器
     */
    public PullHeaderEventHelper getHeaderEventHelper() {
        return headerEventHelper;
    }

    public void setPreloadItemNumber(int preloadItemNumber) {
        preloadHelper.setPreloadItemNumber(preloadItemNumber);
    }
}
