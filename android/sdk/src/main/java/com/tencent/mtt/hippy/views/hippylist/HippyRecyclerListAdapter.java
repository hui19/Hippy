package com.tencent.mtt.hippy.views.hippylist;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.support.annotation.NonNull;
import android.support.v7.widget.HippyItemTypeHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.DiffUtils;
import com.tencent.mtt.hippy.uimanager.DiffUtils.PatchType;
import com.tencent.mtt.hippy.uimanager.ListItemRenderNode;
import com.tencent.mtt.hippy.uimanager.PullFooterRenderNode;
import com.tencent.mtt.hippy.uimanager.PullHeaderRenderNode;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.views.list.IRecycleItemTypeChange;
import com.tencent.mtt.nxeasy.listview.skikcy.IStickyItemsProvider;
import java.util.ArrayList;

/**
 * Created by niuniuyang on 2020/12≥/22.
 * Description
 * RecyclerView的子View，直接是前端的RenderNode节点，没有之前包装的那层RecyclerViewItem。
 */
public class HippyRecyclerListAdapter extends Adapter<HippyRecyclerViewHolder> implements
  IRecycleItemTypeChange, IStickyItemsProvider {

  private final HippyEngineContext hpContext;
  private final HippyRecyclerView hippyRecyclerView;
  private final HippyItemTypeHelper hippyItemTypeHelper;
  private int positionToCreateHolder;

  public HippyRecyclerListAdapter(HippyRecyclerView hippyRecyclerView,
    HippyEngineContext hpContext) {
    this.hpContext = hpContext;
    this.hippyRecyclerView = hippyRecyclerView;
    hippyItemTypeHelper = new HippyItemTypeHelper(hippyRecyclerView);
  }

  /**
   * 对于吸顶到RenderNode需要特殊处理
   * 吸顶的View需要包一层ViewGroup，吸顶的时候，从ViewGroup把RenderNode的View取出来挂载到顶部
   * 当RenderNode的View已经挂载到Header位置上面，如果重新触发创建ViewHolder，renderView会创建失败，
   * 此时就只返回一个空到renderViewContainer上去，等viewHolder需要显示到时候，再把header上面的View还原到这个
   * ViewHolder上面。
   */
  @NonNull
  @Override
  public HippyRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    ListItemRenderNode renderNode = getChildNode(positionToCreateHolder);
    renderNode.setLazy(false);
    View renderView = renderNode.createViewRecursive();
    if (isStickyPosition(positionToCreateHolder)) {
      FrameLayout renderViewContainer = new FrameLayout(parent.getContext());
      if (renderView != null) {
        renderViewContainer
          .addView(renderView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
      }
      return new HippyRecyclerViewHolder(renderViewContainer, renderNode);
    } else {
      return new HippyRecyclerViewHolder(renderView, renderNode);
    }
  }

  @Override
  public void onBindViewHolder(HippyRecyclerViewHolder hippyRecyclerViewHolder, int position) {
    RenderNode oldNode = hippyRecyclerViewHolder.bindNode;
    setLayoutParams(hippyRecyclerViewHolder.itemView, position);
    if (hippyRecyclerViewHolder.isCreated) {
      oldNode.updateViewRecursive();
      hippyRecyclerViewHolder.isCreated = false;
    } else {
      oldNode.setLazy(true);
      RenderNode toNode = getChildNode(position);
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
      hippyRecyclerViewHolder.bindNode = (ListItemRenderNode) toNode;
    }
    hippyRecyclerViewHolder.bindNode.setRecycleItemTypeChangeListener(this);
  }

  /**
   * 设置View的LayoutParams排版属性
   */
  protected void setLayoutParams(View itemView, int position) {
    ViewGroup.LayoutParams params = itemView.getLayoutParams();
    RecyclerView.LayoutParams childLp = null;
    if (params instanceof RecyclerView.LayoutParams) {
      childLp = (LayoutParams) params;
    }
    if (childLp == null) {
      childLp = new RecyclerView.LayoutParams(MATCH_PARENT, 0);
    }
    RenderNode childNode = getChildNode(position);
    childLp.height = childNode.getHeight();
    childLp.width = childNode.getWidth();
    itemView.setLayoutParams(childLp);
  }

  @Override
  public int getItemViewType(int position) {
    int viewType = super.getItemViewType(position);
    RenderNode childNode = getChildNode(position);
    if (childNode != null) {
      if (childNode instanceof PullFooterRenderNode) {
        viewType = 5;//RecyclerViewBase.ViewHolder.TYPE_CUSTOM_FOOTER;
      } else if (childNode instanceof PullHeaderRenderNode) {
        viewType = 4;//RecyclerViewBase.ViewHolder.TYPE_CUSTOM_HEADERE;
      } else if (childNode.getProps() != null) {
        HippyMap listItemProps = childNode.getProps();
        if (listItemProps.get(ListItemRenderNode.ITEM_VIEW_TYPE_NEW) != null) {
          viewType = listItemProps.getInt(ListItemRenderNode.ITEM_VIEW_TYPE_NEW);
        }
      }
    }
    //在调用onCreateViewHolder之前，必然会调用getItemViewType，所以这里把position记下来
    //用在onCreateViewHolder的时候来创建View，不然onCreateViewHolder是无法创建RenderNode到View的
    positionToCreateHolder = position;
    return viewType;
  }

  private ListItemRenderNode getChildNode(int position) {
    return (ListItemRenderNode) getParentNode().getChildAt(position);
  }

  @Override
  public int getItemCount() {
    RenderNode listNode = getParentNode();
    if (listNode != null) {
      return listNode.getChildCount();
    }
    return 0;
  }

  public int getItemHeight(int position) {
    RenderNode renderNode = getChildNode(position);
    if (renderNode != null) {
      return renderNode.getHeight();
    }
    return 0;
  }

  public int getItemWidth(int position) {
    RenderNode renderNode = getChildNode(position);
    if (renderNode != null) {
      return renderNode.getWidth();
    }
    return 0;
  }

  protected RenderNode getParentNode() {
    return hpContext.getRenderManager()
      .getRenderNode(((View) hippyRecyclerView.getParent()).getId());
  }

  @Override
  public void onRecycleItemTypeChanged(int oldType, int newType, ListItemRenderNode listItemNode) {
    hippyItemTypeHelper.updateItemType(oldType, newType, listItemNode);
  }

  @Override
  public long getItemId(int position) {
    return getChildNode(position).getId();
  }

  @Override
  public boolean isStickyPosition(int position) {
    ListItemRenderNode renderNode = getChildNode(position);
    if (renderNode != null) {
      return renderNode.shouldSticky();
    }
    return false;
  }
}
